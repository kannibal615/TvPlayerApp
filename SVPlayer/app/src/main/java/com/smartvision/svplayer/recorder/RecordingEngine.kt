package com.smartvision.svplayer.recorder

import java.io.File
import java.io.OutputStream
import java.net.URI
import java.util.Locale
import kotlinx.coroutines.delay
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class RecordingEngine(
    private val okHttpClient: OkHttpClient,
) {
    @Volatile
    private var currentCall: Call? = null

    suspend fun record(
        request: RecordingEngineRequest,
        shouldStop: () -> Boolean,
        onProgress: suspend () -> Unit,
    ) {
        try {
            val outputFile = File(request.output.tempAbsolutePath)
            outputFile.parentFile?.mkdirs()
            val deadlineMs = System.currentTimeMillis() + request.durationMs
            outputFile.outputStream().buffered().use { output ->
                if (request.streamUrl.lowercase(Locale.US).contains(".m3u8")) {
                    recordHls(request.streamUrl, output, deadlineMs, shouldStop, onProgress)
                } else {
                    recordProgressiveOrDetectedHls(request.streamUrl, output, deadlineMs, shouldStop, onProgress)
                }
            }
        } finally {
            currentCall = null
        }
    }

    fun cancel() {
        currentCall?.cancel()
    }

    private suspend fun recordProgressiveOrDetectedHls(
        url: String,
        output: OutputStream,
        deadlineMs: Long,
        shouldStop: () -> Boolean,
        onProgress: suspend () -> Unit,
    ) {
        execute(url).use { response ->
            if (!response.isSuccessful) throw RecordingException("Stream request failed: HTTP ${response.code}")
            val body = response.body ?: throw RecordingException("Stream response is empty.")
            val contentType = body.contentType()?.toString().orEmpty().lowercase(Locale.US)
            if (contentType.contains("mpegurl") || contentType.contains("m3u")) {
                recordHls(url, output, deadlineMs, shouldStop, onProgress, firstPlaylist = body.string())
                return
            }
            body.byteStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (!shouldStop() && System.currentTimeMillis() < deadlineMs) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    onProgress()
                }
            }
        }
    }

    private suspend fun recordHls(
        playlistUrl: String,
        output: OutputStream,
        deadlineMs: Long,
        shouldStop: () -> Boolean,
        onProgress: suspend () -> Unit,
        firstPlaylist: String? = null,
    ) {
        val downloadedSegments = LinkedHashSet<String>()
        var initSegmentDownloaded = false
        var nextPlaylist = firstPlaylist
        while (!shouldStop() && System.currentTimeMillis() < deadlineMs) {
            val playlistText = nextPlaylist ?: downloadText(playlistUrl)
            nextPlaylist = null
            val playlist = parseHlsPlaylist(playlistText)
            if (playlist.encrypted) {
                throw RecordingException("Encrypted HLS streams are not supported by the recorder yet.")
            }
            val initSegment = playlist.mapUri?.let { resolveUrl(playlistUrl, it) }
            if (initSegment != null && !initSegmentDownloaded) {
                downloadBinary(initSegment, output, deadlineMs, shouldStop, onProgress)
                initSegmentDownloaded = true
            }
            var addedSegment = false
            playlist.segments.forEach { segment ->
                if (shouldStop() || System.currentTimeMillis() >= deadlineMs) return@forEach
                val segmentUrl = resolveUrl(playlistUrl, segment)
                if (downloadedSegments.add(segmentUrl)) {
                    downloadBinary(segmentUrl, output, deadlineMs, shouldStop, onProgress)
                    addedSegment = true
                }
            }
            if (playlist.endList && !addedSegment) break
            if (!addedSegment) delay(HLS_POLL_DELAY_MS)
        }
    }

    private fun downloadText(url: String): String =
        execute(url).use { response ->
            if (!response.isSuccessful) throw RecordingException("Playlist request failed: HTTP ${response.code}")
            response.body?.string() ?: throw RecordingException("Playlist response is empty.")
        }

    private suspend fun downloadBinary(
        url: String,
        output: OutputStream,
        deadlineMs: Long,
        shouldStop: () -> Boolean,
        onProgress: suspend () -> Unit,
    ) {
        execute(url).use { response ->
            if (!response.isSuccessful) throw RecordingException("Segment request failed: HTTP ${response.code}")
            val body = response.body ?: throw RecordingException("Segment response is empty.")
            body.byteStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (!shouldStop() && System.currentTimeMillis() < deadlineMs) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    onProgress()
                }
            }
        }
    }

    private fun execute(url: String): Response {
        val call = okHttpClient.newCall(
            Request.Builder()
                .url(url)
                .header("User-Agent", "SmartVision Recorder")
                .build(),
        )
        currentCall = call
        return call.execute()
    }

    private fun parseHlsPlaylist(text: String): HlsPlaylist {
        val segments = mutableListOf<String>()
        var mapUri: String? = null
        var encrypted = false
        var endList = false
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                when {
                    line.startsWith("#EXT-X-KEY", ignoreCase = true) &&
                        !line.contains("METHOD=NONE", ignoreCase = true) -> encrypted = true
                    line.startsWith("#EXT-X-MAP", ignoreCase = true) -> mapUri = line.extractQuotedUri()
                    line.startsWith("#EXT-X-ENDLIST", ignoreCase = true) -> endList = true
                    !line.startsWith("#") -> segments += line
                }
            }
        if (segments.isEmpty()) throw RecordingException("HLS playlist contains no media segments.")
        return HlsPlaylist(segments = segments, mapUri = mapUri, encrypted = encrypted, endList = endList)
    }

    private fun String.extractQuotedUri(): String? =
        Regex("URI=\"([^\"]+)\"").find(this)?.groupValues?.getOrNull(1)

    private fun resolveUrl(baseUrl: String, value: String): String =
        runCatching { URI(baseUrl).resolve(value).toString() }.getOrElse { value }

    private data class HlsPlaylist(
        val segments: List<String>,
        val mapUri: String?,
        val encrypted: Boolean,
        val endList: Boolean,
    )

    private companion object {
        const val HLS_POLL_DELAY_MS = 2_000L
        const val DEFAULT_BUFFER_SIZE = 64 * 1024
    }
}
