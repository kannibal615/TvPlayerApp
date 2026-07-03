package com.smartvision.svplayer.data.diagnostics

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.smartvision.svplayer.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import org.json.JSONObject

/**
 * PERF_DIAG: local-only recorder for Splash/Home performance investigations.
 *
 * Removal guide:
 * - remove calls to PerformanceDiagnosticRecorder from startup/home files;
 * - remove the releaseDiagnostic BuildConfig fields/build type if no longer needed;
 * - this class has no product-side behavior when PERF_DIAGNOSTICS_ENABLED is false.
 */
object PerformanceDiagnosticRecorder {
    const val SHEET_RUN_SUMMARY = "RunSummary"
    const val SHEET_STARTUP_STEPS = "StartupSteps"
    const val SHEET_LOADED_DATA = "LoadedData"
    const val SHEET_HOME_STATE = "HomeState"
    const val SHEET_ROW_FOCUS = "RowFocus"
    const val SHEET_MINI_PLAYER = "MiniPlayer"
    const val SHEET_FRAME_STATS = "FrameStats"
    const val SHEET_MEMORY = "Memory"
    const val SHEET_ERRORS = "Errors"

    private const val TAG = "SVPerf"
    private val sheets = listOf(
        SHEET_RUN_SUMMARY,
        SHEET_STARTUP_STEPS,
        SHEET_LOADED_DATA,
        SHEET_HOME_STATE,
        SHEET_ROW_FOCUS,
        SHEET_MINI_PLAYER,
        SHEET_FRAME_STATS,
        SHEET_MEMORY,
        SHEET_ERRORS,
    )
    private val formatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
    private val lock = Any()
    // PERF_DIAG: keep file IO off the UI thread so the diagnostic build does not distort jank metrics.
    private val writer = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SVPerfWriter").apply { isDaemon = true }
    }
    private var runDir: File? = null
    private var startedAtMs: Long = 0L

    val enabled: Boolean
        get() = BuildConfig.PERF_DIAGNOSTICS_ENABLED

    fun init(context: Context) {
        if (!enabled) return
        synchronized(lock) {
            if (runDir != null) return
            // PERF_DIAG: all artifacts stay under the app external files directory for adb pull.
            startedAtMs = SystemClock.elapsedRealtime()
            val root = File(context.getExternalFilesDir("diagnostics"), "splash-home-${formatter.format(Date())}")
            root.mkdirs()
            runDir = root
            sheets.forEach { sheet ->
                File(root, "$sheet.csv").writeText(CsvHeader)
            }
            File(root, "events.jsonl").writeText("")
            record(
                sheet = SHEET_RUN_SUMMARY,
                event = "diagnostic_run_started",
                fields = mapOf(
                    "runDir" to root.absolutePath,
                    "packageName" to context.packageName,
                    "versionName" to BuildConfig.VERSION_NAME,
                    "versionCode" to BuildConfig.VERSION_CODE,
                    "diagnosticLabel" to BuildConfig.PERF_DIAGNOSTICS_LABEL,
                    "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
                    "sdk" to Build.VERSION.SDK_INT,
                    "androidRelease" to Build.VERSION.RELEASE,
                ),
            )
        }
    }

    fun runDirectoryPath(): String? = runDir?.absolutePath

    fun record(
        sheet: String,
        event: String,
        fields: Map<String, Any?> = emptyMap(),
        error: Throwable? = null,
    ) {
        if (!enabled) return
        val dir = runDir ?: return
        val now = SystemClock.elapsedRealtime()
        val safeSheet = sheet.takeIf { it in sheets } ?: SHEET_RUN_SUMMARY
        val sanitizedFields = fields.mapValues { (key, value) -> sanitizeValue(key, value) }
        val json = JSONObject().apply {
            put("sheet", safeSheet)
            put("event", event)
            put("elapsed_ms", now - startedAtMs)
            put("wall_time_ms", System.currentTimeMillis())
            put("thread", Thread.currentThread().name)
            sanitizedFields.forEach { (key, value) -> put(key, value) }
            if (error != null) {
                put("error_type", error.javaClass.simpleName)
                put("error_message", sanitizeText(error.message.orEmpty()))
            }
        }
        val row = listOf(
            safeSheet,
            event,
            (now - startedAtMs).toString(),
            System.currentTimeMillis().toString(),
            Thread.currentThread().name,
            sanitizedFields["message"]?.toString().orEmpty(),
            json.toString(),
        ).joinToString(",") { it.csvEscape() } + "\n"
        writer.execute {
            synchronized(lock) {
                runCatching {
                    File(dir, "$safeSheet.csv").appendText(row)
                    File(dir, "events.jsonl").appendText(json.toString() + "\n")
                }.onFailure {
                    Log.w(TAG, "diagnostic write failed: ${it.javaClass.simpleName}")
                }
            }
        }
        val logMessage = "$event ${json.toString().take(900)}"
        if (error == null) {
            Log.i(TAG, logMessage)
        } else {
            Log.w(TAG, logMessage, error)
        }
    }

    fun recordMemory(sheet: String, event: String, fields: Map<String, Any?> = emptyMap()) {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()
        record(
            sheet = sheet,
            event = event,
            fields = fields + mapOf(
                "runtimeUsedBytes" to used,
                "runtimeFreeBytes" to runtime.freeMemory(),
                "runtimeTotalBytes" to runtime.totalMemory(),
                "runtimeMaxBytes" to runtime.maxMemory(),
            ),
        )
    }

    fun recordDuration(
        sheet: String,
        event: String,
        startedAtMs: Long,
        fields: Map<String, Any?> = emptyMap(),
        error: Throwable? = null,
    ) {
        record(
            sheet = sheet,
            event = event,
            fields = fields + ("durationMs" to (SystemClock.elapsedRealtime() - startedAtMs)),
            error = error,
        )
    }

    private fun sanitizeValue(key: String, value: Any?): String =
        when {
            value == null -> ""
            key.contains("password", ignoreCase = true) -> "<redacted>"
            key.contains("token", ignoreCase = true) -> "<redacted>"
            key.contains("secret", ignoreCase = true) -> "<redacted>"
            else -> sanitizeText(value.toString())
        }

    private fun sanitizeText(input: String): String =
        input
            .replace(Regex("(?i)(password|token|key)=([^&\\s]+)"), "$1=<redacted>")
            .replace(Regex("(?i)/(live|movie|series)/[^/\\s]+/[^/\\s]+/"), "/$1/<user>/<pass>/")
            .replace(Regex("(?i)://([^:/\\s]+):([^@/\\s]+)@"), "://<user>:<pass>@")

    private fun String.csvEscape(): String =
        "\"" + replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\""

    private const val CsvHeader = "sheet,event,elapsed_ms,wall_time_ms,thread,message,details_json\n"
}
