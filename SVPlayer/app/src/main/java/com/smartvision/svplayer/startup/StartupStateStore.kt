package com.smartvision.svplayer.startup

import android.content.Context
import android.os.Build
import android.provider.Settings

class StartupStateStore(context: Context) {
    private val appContext = context.applicationContext
    private val storageContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        appContext.createDeviceProtectedStorageContext() ?: appContext
    } else {
        appContext
    }
    private val prefs = storageContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isAutostartEnabled(): Boolean = prefs.getBoolean(KEY_AUTOSTART_ENABLED, true)

    fun setAutostartEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOSTART_ENABLED, enabled).apply()
    }

    fun isBackgroundSyncEnabled(): Boolean = prefs.getBoolean(KEY_BACKGROUND_SYNC_ENABLED, true)

    fun setBackgroundSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND_SYNC_ENABLED, enabled).apply()
    }

    fun bootId(): String {
        val bootCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching {
                Settings.Global.getInt(storageContext.contentResolver, Settings.Global.BOOT_COUNT)
            }.getOrNull()
        } else {
            null
        }
        val approximateBootHour = (System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()) / 3_600_000L
        return bootCount?.toString() ?: "boot_hour:$approximateBootHour"
    }

    fun markAutostartAttempt(source: AutostartSource): Int {
        val bootId = bootId()
        val editor = prefs.edit()
        val currentAttempts = if (prefs.getString(KEY_AUTOSTART_BOOT_ID, null) == bootId) {
            prefs.getInt(KEY_AUTOSTART_ATTEMPTS, 0) + 1
        } else {
            1
        }
        editor.putString(KEY_AUTOSTART_BOOT_ID, bootId)
        editor.putInt(KEY_AUTOSTART_ATTEMPTS, currentAttempts)
        editor.putString(KEY_AUTOSTART_LAST_SOURCE, source.name)
        editor.putLong(KEY_AUTOSTART_LAST_ATTEMPT_AT, System.currentTimeMillis())
        editor.apply()
        return currentAttempts
    }

    fun autostartAttemptsForCurrentBoot(): Int {
        val bootId = bootId()
        return if (prefs.getString(KEY_AUTOSTART_BOOT_ID, null) == bootId) {
            prefs.getInt(KEY_AUTOSTART_ATTEMPTS, 0)
        } else {
            0
        }
    }

    fun recordAutostartResult(
        source: AutostartSource,
        success: Boolean,
        error: String?,
        bootCompleted: Boolean,
    ) {
        prefs.edit()
            .putString(KEY_AUTOSTART_LAST_SOURCE, source.name)
            .putLong(KEY_AUTOSTART_LAST_ATTEMPT_AT, System.currentTimeMillis())
            .putBoolean(KEY_AUTOSTART_LAST_SUCCESS, success)
            .putString(KEY_AUTOSTART_LAST_ERROR, error?.take(MAX_ERROR_LENGTH))
            .putBoolean(KEY_AUTOSTART_LAST_BOOT_COMPLETED, bootCompleted)
            .apply()
    }

    fun autostartSnapshot(): Map<String, Any?> = mapOf(
        "enabled" to isAutostartEnabled(),
        "last_source" to prefs.getString(KEY_AUTOSTART_LAST_SOURCE, null),
        "last_attempt_at" to prefs.getLong(KEY_AUTOSTART_LAST_ATTEMPT_AT, 0L).takeIf { it > 0L },
        "last_success" to prefs.getBoolean(KEY_AUTOSTART_LAST_SUCCESS, false),
        "last_error" to prefs.getString(KEY_AUTOSTART_LAST_ERROR, null),
        "attempts_this_boot" to autostartAttemptsForCurrentBoot(),
        "boot_id" to bootId(),
        "boot_completed" to prefs.getBoolean(KEY_AUTOSTART_LAST_BOOT_COMPLETED, false),
    )

    fun recordAutoSyncResult(
        source: AutoSyncSource,
        result: String,
        durationMs: Long,
        downloadedKilobytes: Long?,
        error: String?,
    ) {
        prefs.edit()
            .putString(KEY_AUTOSYNC_LAST_SOURCE, source.name)
            .putString(KEY_AUTOSYNC_LAST_RESULT, result.take(40))
            .putLong(KEY_AUTOSYNC_LAST_AT, System.currentTimeMillis())
            .putLong(KEY_AUTOSYNC_LAST_DURATION_MS, durationMs.coerceAtLeast(0L))
            .putLong(KEY_AUTOSYNC_LAST_SIZE_KB, downloadedKilobytes ?: -1L)
            .putString(KEY_AUTOSYNC_LAST_ERROR, error?.take(MAX_ERROR_LENGTH))
            .apply()
    }

    fun autoSyncSnapshot(): Map<String, Any?> = mapOf(
        "enabled" to isBackgroundSyncEnabled(),
        "last_source" to prefs.getString(KEY_AUTOSYNC_LAST_SOURCE, null),
        "last_sync_at" to prefs.getLong(KEY_AUTOSYNC_LAST_AT, 0L).takeIf { it > 0L },
        "last_result" to prefs.getString(KEY_AUTOSYNC_LAST_RESULT, null),
        "last_duration_ms" to prefs.getLong(KEY_AUTOSYNC_LAST_DURATION_MS, 0L).takeIf { it > 0L },
        "last_size_kb" to prefs.getLong(KEY_AUTOSYNC_LAST_SIZE_KB, -1L).takeIf { it >= 0L },
        "last_error" to prefs.getString(KEY_AUTOSYNC_LAST_ERROR, null),
    )

    private companion object {
        const val PREFS_NAME = "smartvision_startup_state"
        const val MAX_ERROR_LENGTH = 240
        const val KEY_AUTOSTART_ENABLED = "autostart_enabled"
        const val KEY_BACKGROUND_SYNC_ENABLED = "background_sync_enabled"
        const val KEY_AUTOSTART_BOOT_ID = "autostart_boot_id"
        const val KEY_AUTOSTART_ATTEMPTS = "autostart_attempts"
        const val KEY_AUTOSTART_LAST_SOURCE = "autostart_last_source"
        const val KEY_AUTOSTART_LAST_ATTEMPT_AT = "autostart_last_attempt_at"
        const val KEY_AUTOSTART_LAST_SUCCESS = "autostart_last_success"
        const val KEY_AUTOSTART_LAST_ERROR = "autostart_last_error"
        const val KEY_AUTOSTART_LAST_BOOT_COMPLETED = "autostart_last_boot_completed"
        const val KEY_AUTOSYNC_LAST_SOURCE = "autosync_last_source"
        const val KEY_AUTOSYNC_LAST_AT = "autosync_last_at"
        const val KEY_AUTOSYNC_LAST_RESULT = "autosync_last_result"
        const val KEY_AUTOSYNC_LAST_DURATION_MS = "autosync_last_duration_ms"
        const val KEY_AUTOSYNC_LAST_SIZE_KB = "autosync_last_size_kb"
        const val KEY_AUTOSYNC_LAST_ERROR = "autosync_last_error"
    }
}

enum class AutostartSource {
    BOOT_COMPLETED,
    LOCKED_BOOT_COMPLETED,
    QUICKBOOT_POWERON,
    MY_PACKAGE_REPLACED,
    USER_PRESENT,
    ALARM_FALLBACK,
    WORKER_FALLBACK,
}

enum class AutoSyncSource {
    BOOT,
    PERIODIC,
    MANUAL,
}
