package com.smartvision.svplayer.data.network

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer

enum class NetworkActivityType {
    Catalog,
    Epg,
    Home,
    Http,
    Notifications,
    Config,
    Updates,
    Xtream,
    License,
    Diagnostics,
    Recorder,
    Other,
}

enum class NetworkActivityStatus {
    Queued,
    Running,
    Importing,
    Completed,
    Error,
}

data class NetworkActivityItem(
    val id: String,
    val title: String,
    val type: NetworkActivityType,
    val status: NetworkActivityStatus,
    val message: String = "",
    val section: String? = null,
    val source: String? = null,
    val progressPercent: Int? = null,
    val currentItems: Int? = null,
    val totalItems: Int? = null,
    val bytesRead: Long? = null,
    val totalBytes: Long? = null,
    val bytesPerSecond: Long? = null,
    val startedAtMs: Long = SystemClock.elapsedRealtime(),
    val updatedAtMs: Long = SystemClock.elapsedRealtime(),
    val finishedAtMs: Long? = null,
    val errorMessage: String? = null,
) {
    val active: Boolean = status != NetworkActivityStatus.Completed && status != NetworkActivityStatus.Error
    val durationMs: Long
        get() = (finishedAtMs ?: SystemClock.elapsedRealtime()) - startedAtMs
}

data class NetworkActivitySnapshot(
    val active: List<NetworkActivityItem> = emptyList(),
    val recent: List<NetworkActivityItem> = emptyList(),
    val activeCount: Int = 0,
    val errorCount: Int = 0,
    val bytesPerSecond: Long = 0L,
) {
    val hasActivity: Boolean = active.isNotEmpty() || recent.isNotEmpty()
}

class NetworkActivityTracker {
    private val lock = Any()
    private val items = linkedMapOf<String, NetworkActivityItem>()
    private val _snapshot = MutableStateFlow(NetworkActivitySnapshot())
    val snapshot: StateFlow<NetworkActivitySnapshot> = _snapshot.asStateFlow()

    fun begin(
        id: String,
        title: String,
        type: NetworkActivityType,
        message: String = "",
        section: String? = null,
        source: String? = null,
        status: NetworkActivityStatus = NetworkActivityStatus.Running,
        progressPercent: Int? = null,
        currentItems: Int? = null,
        totalItems: Int? = null,
        totalBytes: Long? = null,
    ): NetworkActivityHandle {
        val now = SystemClock.elapsedRealtime()
        synchronized(lock) {
            items[id] = NetworkActivityItem(
                id = id,
                title = title,
                type = type,
                status = status,
                message = message,
                section = section,
                source = source,
                progressPercent = progressPercent,
                currentItems = currentItems,
                totalItems = totalItems,
                totalBytes = totalBytes?.takeIf { it > 0L },
                startedAtMs = now,
                updatedAtMs = now,
            )
            publishLocked()
        }
        return NetworkActivityHandle(this, id)
    }

    fun update(
        id: String,
        status: NetworkActivityStatus? = null,
        message: String? = null,
        progressPercent: Int? = null,
        currentItems: Int? = null,
        totalItems: Int? = null,
        bytesRead: Long? = null,
        totalBytes: Long? = null,
    ) {
        synchronized(lock) {
            val existing = items[id] ?: return
            val now = SystemClock.elapsedRealtime()
            val read = bytesRead ?: existing.bytesRead
            val total = totalBytes?.takeIf { it > 0L } ?: existing.totalBytes
            val elapsedSeconds = ((now - existing.startedAtMs).coerceAtLeast(1L)).toDouble() / 1000.0
            items[id] = existing.copy(
                status = status ?: existing.status,
                message = message ?: existing.message,
                progressPercent = progressPercent?.coerceIn(0, 100) ?: read.toPercent(total) ?: existing.progressPercent,
                currentItems = currentItems ?: existing.currentItems,
                totalItems = totalItems ?: existing.totalItems,
                bytesRead = read,
                totalBytes = total,
                bytesPerSecond = read?.let { (it / elapsedSeconds).toLong().coerceAtLeast(0L) } ?: existing.bytesPerSecond,
                updatedAtMs = now,
            )
            publishLocked()
        }
    }

    fun complete(id: String, message: String? = null) {
        finish(id, NetworkActivityStatus.Completed, message, null)
    }

    fun fail(id: String, errorMessage: String?) {
        finish(id, NetworkActivityStatus.Error, errorMessage, errorMessage)
    }

    private fun finish(
        id: String,
        status: NetworkActivityStatus,
        message: String?,
        errorMessage: String?,
    ) {
        synchronized(lock) {
            val existing = items[id] ?: return
            val now = SystemClock.elapsedRealtime()
            items[id] = existing.copy(
                status = status,
                message = message ?: existing.message,
                progressPercent = if (status == NetworkActivityStatus.Completed) 100 else existing.progressPercent,
                updatedAtMs = now,
                finishedAtMs = now,
                errorMessage = errorMessage,
            )
            trimLocked()
            publishLocked()
        }
    }

    private fun trimLocked() {
        val finished = items.values
            .filter { !it.active }
            .sortedByDescending { it.finishedAtMs ?: it.updatedAtMs }
        finished.drop(RecentHistoryLimit).forEach { items.remove(it.id) }
    }

    private fun publishLocked() {
        val all = items.values.sortedByDescending { it.updatedAtMs }
        val active = all.filter { it.active }
        val recent = all.filter { !it.active }.take(RecentHistoryLimit)
        _snapshot.value = NetworkActivitySnapshot(
            active = active,
            recent = recent,
            activeCount = active.size,
            errorCount = recent.count { it.status == NetworkActivityStatus.Error },
            bytesPerSecond = active.sumOf { it.bytesPerSecond ?: 0L },
        )
    }

    companion object {
        private const val RecentHistoryLimit = 30
    }
}

class NetworkActivityHandle internal constructor(
    private val tracker: NetworkActivityTracker,
    private val id: String,
) {
    fun update(
        status: NetworkActivityStatus? = null,
        message: String? = null,
        progressPercent: Int? = null,
        currentItems: Int? = null,
        totalItems: Int? = null,
        bytesRead: Long? = null,
        totalBytes: Long? = null,
    ) {
        tracker.update(
            id = id,
            status = status,
            message = message,
            progressPercent = progressPercent,
            currentItems = currentItems,
            totalItems = totalItems,
            bytesRead = bytesRead,
            totalBytes = totalBytes,
        )
    }

    fun complete(message: String? = null) {
        tracker.complete(id, message)
    }

    fun fail(errorMessage: String?) {
        tracker.fail(id, errorMessage)
    }
}

class NetworkActivityInterceptor(
    private val tracker: NetworkActivityTracker,
    private val source: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val id = "http-${SystemClock.elapsedRealtimeNanos()}"
        val title = request.toSafeActivityTitle(source)
        val handle = tracker.begin(
            id = id,
            title = title,
            type = request.toActivityType(source),
            message = request.method,
            source = source,
        )
        return try {
            val response = chain.proceed(request)
            val body = response.body
            if (body == null) {
                if (response.isSuccessful) {
                    handle.complete("HTTP ${response.code}")
                } else {
                    handle.fail("HTTP ${response.code}")
                }
                response
            } else {
                tracker.update(id, totalBytes = body.contentLength().takeIf { it > 0L })
                response.newBuilder()
                    .body(TrackingResponseBody(body, tracker, id, response.code, response.isSuccessful))
                    .build()
            }
        } catch (error: Throwable) {
            handle.fail(error.javaClass.simpleName)
            throw error
        }
    }
}

private class TrackingResponseBody(
    private val delegate: ResponseBody,
    private val tracker: NetworkActivityTracker,
    private val id: String,
    private val code: Int,
    private val successful: Boolean,
) : ResponseBody() {
    private var readBytes = 0L
    private var finished = false

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun source(): BufferedSource = object : ForwardingSource(delegate.source()) {
        override fun read(sink: Buffer, byteCount: Long): Long {
            val read = super.read(sink, byteCount)
            if (read > 0L) {
                readBytes += read
                tracker.update(id, bytesRead = readBytes, totalBytes = contentLength().takeIf { it > 0L })
            } else if (read == -1L) {
                finish()
            }
            return read
        }

        override fun close() {
            runCatching { finish() }
            super.close()
        }
    }.buffer()

    private fun finish() {
        if (finished) return
        finished = true
        if (successful) {
            tracker.complete(id, "HTTP $code")
        } else {
            tracker.fail(id, "HTTP $code")
        }
    }
}

private fun Long?.toPercent(total: Long?): Int? =
    if (this != null && total != null && total > 0L) {
        ((this.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)
    } else {
        null
    }

private fun okhttp3.Request.toSafeActivityTitle(source: String): String {
    val path = url.encodedPath
        .trim('/')
        .ifBlank { url.host }
        .replace('/', ' ')
        .replace('-', ' ')
        .replace('_', ' ')
    return "$source: $path"
}

private fun okhttp3.Request.toActivityType(source: String): NetworkActivityType =
    when {
        source.contains("Xtream", ignoreCase = true) -> NetworkActivityType.Xtream
        url.encodedPath.contains("notifications", ignoreCase = true) -> NetworkActivityType.Notifications
        url.encodedPath.contains("app_update", ignoreCase = true) -> NetworkActivityType.Updates
        url.encodedPath.contains("app_config", ignoreCase = true) -> NetworkActivityType.Config
        url.encodedPath.contains("device-diagnostics", ignoreCase = true) -> NetworkActivityType.Diagnostics
        url.encodedPath.contains("device_status", ignoreCase = true) -> NetworkActivityType.License
        url.encodedPath.contains("home_slides", ignoreCase = true) -> NetworkActivityType.Home
        url.encodedPath.contains("epg", ignoreCase = true) -> NetworkActivityType.Epg
        else -> NetworkActivityType.Http
    }
