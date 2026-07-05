package com.smartvision.svplayer.media.transfer

import com.smartvision.svplayer.media.MediaCenterDownload
import com.smartvision.svplayer.media.MediaRepository
import java.io.BufferedInputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.security.SecureRandom
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

enum class MediaTransferMode {
    Import,
    Export,
}

data class MediaTransferSession(
    val mode: MediaTransferMode,
    val url: String,
    val port: Int,
    val expiresAtMs: Long,
    val fileName: String? = null,
)

data class MediaTransferUploadResult(
    val fileId: Long?,
    val fileName: String,
)

class MediaTransferServer(
    private val repository: MediaRepository,
) {
    private val lock = Any()
    private val random = SecureRandom()
    private var scope: CoroutineScope? = null
    private var socket: ServerSocket? = null
    private var session: ActiveSession? = null

    fun startImportSession(
        onUploadCompleted: (MediaTransferUploadResult) -> Unit,
    ): Result<MediaTransferSession> = runCatching {
        startSession(
            mode = MediaTransferMode.Import,
            exportFileId = null,
            onUploadCompleted = onUploadCompleted,
        )
    }

    suspend fun startExportSession(fileId: Long): Result<MediaTransferSession> = runCatching {
        val download = repository.getDownload(fileId) ?: error("File not found.")
        startSession(
            mode = MediaTransferMode.Export,
            exportFileId = fileId,
            exportDownload = download,
            onUploadCompleted = null,
        )
    }

    fun stop() {
        synchronized(lock) {
            stopLocked()
        }
    }

    private fun startSession(
        mode: MediaTransferMode,
        exportFileId: Long?,
        exportDownload: MediaCenterDownload? = null,
        onUploadCompleted: ((MediaTransferUploadResult) -> Unit)?,
    ): MediaTransferSession {
        synchronized(lock) {
            stopLocked()
            val token = newToken()
            val serverSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(0))
            }
            val host = localIpv4Address() ?: error("Unable to find a local network IP address.")
            val port = serverSocket.localPort
            val expiresAt = System.currentTimeMillis() + SessionTtlMs
            val publicUrl = when (mode) {
                MediaTransferMode.Import -> "http://$host:$port/u/$token"
                MediaTransferMode.Export -> "http://$host:$port/d/$token"
            }
            val active = ActiveSession(
                mode = mode,
                token = token,
                exportFileId = exportFileId,
                exportDownload = exportDownload,
                expiresAtMs = expiresAt,
                onUploadCompleted = onUploadCompleted,
            )
            val newScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            socket = serverSocket
            session = active
            scope = newScope
            newScope.launch { acceptLoop(serverSocket) }
            return MediaTransferSession(
                mode = mode,
                url = publicUrl,
                port = port,
                expiresAtMs = expiresAt,
                fileName = exportDownload?.displayName,
            )
        }
    }

    private fun stopLocked() {
        runCatching { socket?.close() }
        socket = null
        session = null
        scope?.cancel()
        scope = null
    }

    private suspend fun acceptLoop(serverSocket: ServerSocket) {
        while (!serverSocket.isClosed) {
            val client = runCatching { serverSocket.accept() }.getOrNull() ?: break
            scope?.launch {
                client.use { handleClient(it) }
            }
        }
    }

    private suspend fun handleClient(client: Socket) {
        client.soTimeout = ClientTimeoutMs
        val active = synchronized(lock) { session } ?: return
        val input = BufferedInputStream(client.getInputStream())
        val output = client.getOutputStream()
        val request = readRequest(input) ?: run {
            output.writeTextResponse(400, "text/plain", "Bad request")
            return
        }
        if (System.currentTimeMillis() > active.expiresAtMs) {
            output.writeTextResponse(410, "text/plain", "Transfer session expired")
            return
        }
        when {
            request.method == "GET" && request.path == "/u/${active.token}" && active.mode == MediaTransferMode.Import ->
                output.writeTextResponse(200, "text/html; charset=utf-8", uploadPage(active.token))

            request.method == "PUT" &&
                request.path.startsWith("/upload/${active.token}/") &&
                active.mode == MediaTransferMode.Import ->
                handleUpload(request, input, output, active)

            request.method == "GET" && request.path == "/d/${active.token}" && active.mode == MediaTransferMode.Export ->
                output.writeTextResponse(200, "text/html; charset=utf-8", downloadPage(active.token, active.exportDownload))

            request.method == "GET" && request.path == "/download/${active.token}" && active.mode == MediaTransferMode.Export ->
                handleDownload(output, active)

            else -> output.writeTextResponse(404, "text/plain", "Not found")
        }
    }

    private suspend fun handleUpload(
        request: HttpRequest,
        input: BufferedInputStream,
        output: OutputStream,
        active: ActiveSession,
    ) {
        val encodedName = request.path.removePrefix("/upload/${active.token}/")
        val fileName = URLDecoder.decode(encodedName, Charsets.UTF_8.name()).ifBlank { "SmartVision transfer" }
        val contentLength = request.headers["content-length"]?.toLongOrNull()
            ?: run {
                output.writeTextResponse(411, "text/plain", "Content-Length required")
                return
            }
        val contentType = request.headers["content-type"]?.takeIf { it.isNotBlank() }
        val fileId = runCatching {
            repository.importTransferredFile(
                requestedName = fileName,
                mimeType = contentType,
                input = input,
                expectedBytes = contentLength,
            )
        }.getOrElse { throwable ->
            output.writeTextResponse(500, "text/plain", throwable.message ?: "Upload failed")
            return
        }
        active.onUploadCompleted?.invoke(MediaTransferUploadResult(fileId, fileName))
        output.writeTextResponse(200, "application/json; charset=utf-8", """{"success":true}""")
    }

    private fun handleDownload(output: OutputStream, active: ActiveSession) {
        val download = active.exportDownload ?: run {
            output.writeTextResponse(404, "text/plain", "File not found")
            return
        }
        val safeFileName = download.displayName.replace("\"", "'")
        val mimeType = download.mimeType ?: "application/octet-stream"
        output.writeAscii(
            "HTTP/1.1 200 OK\r\n" +
                "Content-Type: $mimeType\r\n" +
                "Content-Length: ${download.sizeBytes}\r\n" +
                "Content-Disposition: attachment; filename=\"$safeFileName\"\r\n" +
                "Cache-Control: no-store\r\n" +
                "Connection: close\r\n\r\n",
        )
        download.openStream().use { input ->
            input.copyTo(output)
        }
    }

    private fun readRequest(input: BufferedInputStream): HttpRequest? {
        val requestLine = input.readAsciiLine() ?: return null
        val parts = requestLine.split(' ')
        if (parts.size < 2) return null
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = input.readAsciiLine() ?: return null
            if (line.isEmpty()) break
            val separator = line.indexOf(':')
            if (separator > 0) {
                headers[line.substring(0, separator).trim().lowercase(Locale.US)] =
                    line.substring(separator + 1).trim()
            }
        }
        return HttpRequest(parts[0].uppercase(Locale.US), parts[1].substringBefore('?'), headers)
    }

    private fun BufferedInputStream.readAsciiLine(): String? {
        val builder = StringBuilder()
        while (true) {
            val value = read()
            if (value == -1) return if (builder.isEmpty()) null else builder.toString()
            if (value == '\n'.code) return builder.toString().trimEnd('\r')
            builder.append(value.toChar())
            if (builder.length > MaxHeaderLineLength) return null
        }
    }

    private fun OutputStream.writeTextResponse(status: Int, contentType: String, body: String) {
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val reason = when (status) {
            200 -> "OK"
            400 -> "Bad Request"
            404 -> "Not Found"
            410 -> "Gone"
            411 -> "Length Required"
            500 -> "Internal Server Error"
            else -> "OK"
        }
        writeAscii(
            "HTTP/1.1 $status $reason\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${bodyBytes.size}\r\n" +
                "Cache-Control: no-store\r\n" +
                "Connection: close\r\n\r\n",
        )
        write(bodyBytes)
    }

    private fun OutputStream.writeAscii(value: String) {
        write(value.toByteArray(Charsets.US_ASCII))
    }

    private fun uploadPage(token: String): String =
        """
        <!doctype html>
        <html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>SmartVision Upload</title>
        <style>body{font-family:sans-serif;background:#07101e;color:#fff;padding:24px}button,input{font-size:18px;margin-top:16px;width:100%;padding:12px}.box{max-width:520px;margin:auto}</style>
        </head><body><main class="box">
        <h1>Send to SmartVision</h1>
        <p>Select one file. Keep this page open until the transfer completes.</p>
        <input id="file" type="file">
        <button onclick="send()">Send file</button>
        <p id="status"></p>
        <script>
        async function send(){
          const file=document.getElementById('file').files[0];
          const status=document.getElementById('status');
          if(!file){status.textContent='Choose a file first.';return;}
          status.textContent='Uploading...';
          const res=await fetch('/upload/$token/'+encodeURIComponent(file.name),{method:'PUT',headers:{'Content-Type':file.type||'application/octet-stream'},body:file});
          status.textContent=res.ok?'Upload complete. You can return to the TV.':'Upload failed: '+res.status;
        }
        </script></main></body></html>
        """.trimIndent()

    private fun downloadPage(token: String, download: MediaCenterDownload?): String {
        val name = htmlEscape(download?.displayName ?: "SmartVision file")
        return """
            <!doctype html>
            <html><head><meta name="viewport" content="width=device-width,initial-scale=1">
            <title>SmartVision Download</title>
            <style>body{font-family:sans-serif;background:#07101e;color:#fff;padding:24px}a{display:block;background:#14b8d4;color:#001018;text-align:center;text-decoration:none;font-size:18px;margin-top:16px;padding:14px;border-radius:8px}.box{max-width:520px;margin:auto}</style>
            </head><body><main class="box">
            <h1>Download from SmartVision</h1>
            <p>$name</p>
            <a href="/download/$token">Download file</a>
            </main></body></html>
        """.trimIndent()
    }

    private fun htmlEscape(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    private fun newToken(): String {
        val bytes = ByteArray(18)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun localIpv4Address(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces().toList()
        return interfaces
            .asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList().asSequence() }
            .filterIsInstance<Inet4Address>()
            .filter { !it.isLoopbackAddress && it.isSiteLocalAddress }
            .map { it.hostAddress }
            .firstOrNull()
    }

    private data class ActiveSession(
        val mode: MediaTransferMode,
        val token: String,
        val exportFileId: Long?,
        val exportDownload: MediaCenterDownload?,
        val expiresAtMs: Long,
        val onUploadCompleted: ((MediaTransferUploadResult) -> Unit)?,
    )

    private data class HttpRequest(
        val method: String,
        val path: String,
        val headers: Map<String, String>,
    )

    private companion object {
        const val SessionTtlMs = 15L * 60L * 1000L
        const val ClientTimeoutMs = 120_000
        const val MaxHeaderLineLength = 8192
    }
}
