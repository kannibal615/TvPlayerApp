package com.smartvision.svplayer.data.anomaly

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.data.activation.ActivationRepository
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.http.Body
import retrofit2.http.POST
import kotlin.system.exitProcess

interface AnomalyApiService {
    @POST("api/app/anomaly-events")
    suspend fun storeEvent(@Body request: AnomalyEventRequest): AnomalyEventResponse
}

data class AnomalyEventRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("appVersion") val appVersion: String,
    @SerializedName("platform") val platform: String = "ANDROID_TV",
    @SerializedName("route") val route: String?,
    @SerializedName("anomalyType") val anomalyType: String,
    @SerializedName("message") val message: String?,
    @SerializedName("stackTrace") val stackTrace: String?,
    @SerializedName("context") val context: String?,
)

data class AnomalyEventResponse(
    @SerializedName("success") val success: Boolean = false,
)

class AnomalyReporter(
    private val appContext: Context,
    private val activationRepository: ActivationRepository,
    private val api: AnomalyApiService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val queueLock = Any()
    private val queueFile = File(appContext.filesDir, "smartvision_anomaly_events.jsonl")
    private val prefs = appContext.getSharedPreferences("smartvision_anomalies", Context.MODE_PRIVATE)
    @Volatile private var currentRoute: String? = null
    @Volatile private var currentContext: String? = null

    fun setCurrentRoute(route: String?) {
        currentRoute = route
        if (!route.isNullOrBlank()) {
            prefs.edit().putString(KEY_LAST_ROUTE, route).apply()
        }
    }

    fun setCurrentContext(context: String?) {
        currentContext = context
        if (!context.isNullOrBlank()) {
            prefs.edit().putString(KEY_LAST_CONTEXT, context).apply()
        }
    }

    fun reportAsync(
        anomalyType: String,
        message: String?,
        throwable: Throwable? = null,
        context: String? = null,
    ) {
        scope.launch {
            report(
                anomalyType = anomalyType,
                message = message ?: throwable?.message,
                stackTrace = throwable?.stackTraceText(),
                context = context ?: currentContext,
            )
        }
    }

    suspend fun report(
        anomalyType: String,
        message: String?,
        stackTrace: String? = null,
        context: String? = null,
    ) = withContext(Dispatchers.IO) {
        val request = buildRequest(
            anomalyType = anomalyType,
            message = message,
            stackTrace = stackTrace,
            context = context,
        ) ?: return@withContext

        enqueue(request)
        flushPending()
    }

    fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                runBlocking {
                    withTimeout(1800L) {
                        report(
                            anomalyType = "UNCAUGHT_EXCEPTION",
                            message = throwable.message ?: throwable::class.java.simpleName,
                            stackTrace = throwable.stackTraceText(),
                            context = currentContext,
                        )
                    }
                }
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }

    fun flushPendingAsync() {
        scope.launch { flushPending() }
    }

    fun reportPreviousProcessExitAsync() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        scope.launch {
            runCatching { reportPreviousProcessExit() }
                .onFailure { Log.w(TAG, "Historique crash Android indisponible", it) }
        }
    }

    private suspend fun reportPreviousProcessExit() = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext
        val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return@withContext
        val crashExitReasons = setOf(
            ApplicationExitInfo.REASON_CRASH,
            ApplicationExitInfo.REASON_CRASH_NATIVE,
            ApplicationExitInfo.REASON_ANR,
        )
        val exit = manager.getHistoricalProcessExitReasons(appContext.packageName, 0, 8)
            .firstOrNull { it.reason in crashExitReasons }
            ?: return@withContext
        val lastReported = prefs.getLong(KEY_LAST_EXIT_REPORTED_AT, 0L)
        if (exit.timestamp <= lastReported) return@withContext

        prefs.edit().putLong(KEY_LAST_EXIT_REPORTED_AT, exit.timestamp).apply()
        val lastRoute = prefs.getString(KEY_LAST_ROUTE, null).orEmpty()
        val lastContext = prefs.getString(KEY_LAST_CONTEXT, null).orEmpty()
        val trace = runCatching {
            exit.traceInputStream?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        report(
            anomalyType = "PROCESS_EXIT_${exit.reasonName()}",
            message = exit.description?.takeIf { it.isNotBlank() } ?: "Sortie brutale detectee au demarrage",
            stackTrace = trace,
            context = listOf(
                "pid=${exit.pid}",
                "importance=${exit.importance}",
                "pss=${exit.pss}",
                "rss=${exit.rss}",
                "timestamp=${exit.timestamp}",
                "lastRoute=$lastRoute",
                "lastContext=$lastContext",
                currentContext.orEmpty(),
            ).filter { it.isNotBlank() }.joinToString(" | "),
        )
    }

    private suspend fun buildRequest(
        anomalyType: String,
        message: String?,
        stackTrace: String?,
        context: String?,
    ): AnomalyEventRequest? {
        val storedDeviceId = activationRepository.localState.first().deviceId
        val deviceId = storedDeviceId.ifBlank {
            runCatching { activationRepository.getOrCreateDeviceId() }.getOrDefault("")
        }
        if (deviceId.isBlank()) return null
        return AnomalyEventRequest(
            deviceId = deviceId,
            appVersion = BuildConfig.VERSION_NAME,
            route = currentRoute?.take(120),
            anomalyType = anomalyType.take(60),
            message = message?.take(255),
            stackTrace = stackTrace?.take(4000),
            context = context?.take(500),
        )
    }

    private fun enqueue(request: AnomalyEventRequest) {
        synchronized(queueLock) {
            queueFile.parentFile?.mkdirs()
            queueFile.appendText(gson.toJson(request) + "\n")
        }
    }

    private suspend fun flushPending() = withContext(Dispatchers.IO) {
        val pending = synchronized(queueLock) {
            if (!queueFile.exists()) {
                emptyList()
            } else {
                queueFile.readLines().also { queueFile.writeText("") }
            }
        }
        if (pending.isEmpty()) return@withContext

        val failed = mutableListOf<String>()
        pending.forEach { line ->
            val request = runCatching { gson.fromJson(line, AnomalyEventRequest::class.java) }.getOrNull()
            if (request == null) return@forEach
            val sent = runCatching { api.storeEvent(request).success }.getOrDefault(false)
            if (!sent) failed += line
        }
        if (failed.isNotEmpty()) {
            synchronized(queueLock) {
                queueFile.appendText(failed.joinToString(separator = "\n", postfix = "\n"))
            }
            Log.w(TAG, "Anomalies en attente: ${failed.size}")
        }
    }

    private companion object {
        const val TAG = "SmartVisionAnomaly"
        const val KEY_LAST_EXIT_REPORTED_AT = "last_exit_reported_at"
        const val KEY_LAST_ROUTE = "last_route"
        const val KEY_LAST_CONTEXT = "last_context"
    }
}

private fun ApplicationExitInfo.reasonName(): String =
    when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "CRASH"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        else -> reason.toString()
    }

private fun Throwable.stackTraceText(): String {
    val writer = StringWriter()
    printStackTrace(PrintWriter(writer))
    return writer.toString()
}
