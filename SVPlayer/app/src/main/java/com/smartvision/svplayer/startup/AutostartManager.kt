package com.smartvision.svplayer.startup

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import com.smartvision.svplayer.MainActivity
import com.smartvision.svplayer.SVPlayerApplication
import com.smartvision.svplayer.core.data.AppContainer

class AutostartManager(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val stateStore = StartupStateStore(appContext)

    fun attemptAutostart(context: Context = appContext, source: AutostartSource) {
        val bootCompleted = source != AutostartSource.LOCKED_BOOT_COMPLETED
        if (!stateStore.isAutostartEnabled()) {
            stateStore.recordAutostartResult(source, success = false, error = "autostart_disabled", bootCompleted = bootCompleted)
            reportDiagnosticsIfUnlocked()
            return
        }

        if (stateStore.autostartAttemptsForCurrentBoot() >= MAX_ATTEMPTS_PER_BOOT) {
            stateStore.recordAutostartResult(source, success = false, error = "max_attempts_reached", bootCompleted = bootCompleted)
            reportDiagnosticsIfUnlocked()
            return
        }

        val attempts = stateStore.markAutostartAttempt(source)
        if (isAppAlreadyOpen()) {
            stateStore.recordAutostartResult(source, success = true, error = "already_open", bootCompleted = bootCompleted)
            reportDiagnosticsIfUnlocked()
            return
        }

        if (!canLaunchUi(source)) {
            stateStore.recordAutostartResult(source, success = false, error = "launch_not_allowed_yet", bootCompleted = bootCompleted)
            scheduleFallback(source, attempts)
            reportDiagnosticsIfUnlocked()
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_AUTOSTART_SOURCE, source.name)
        }

        val failure = runCatching { context.startActivity(intent) }.exceptionOrNull()
        stateStore.recordAutostartResult(
            source = source,
            success = failure == null,
            error = failure?.javaClass?.simpleName ?: failure?.message,
            bootCompleted = bootCompleted,
        )
        if (failure != null) {
            scheduleFallback(source, attempts)
        }
        reportDiagnosticsIfUnlocked()
    }

    private fun canLaunchUi(source: AutostartSource): Boolean {
        val userManager = appContext.getSystemService(Context.USER_SERVICE) as? UserManager
        val isUnlocked = userManager?.isUserUnlocked ?: true
        return when (source) {
            AutostartSource.LOCKED_BOOT_COMPLETED -> isUnlocked
            AutostartSource.USER_PRESENT,
            AutostartSource.ALARM_FALLBACK,
            AutostartSource.WORKER_FALLBACK,
            AutostartSource.BOOT_COMPLETED,
            AutostartSource.QUICKBOOT_POWERON,
            AutostartSource.MY_PACKAGE_REPLACED -> isUnlocked
        }
    }

    private fun isAppAlreadyOpen(): Boolean {
        val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        val packageName = appContext.packageName
        val hasTask = runCatching {
            manager.appTasks.any { task ->
                task.taskInfo.baseIntent.component?.packageName == packageName
            }
        }.getOrDefault(false)
        if (hasTask) return true
        return manager.runningAppProcesses.orEmpty().any { process ->
            process.processName == packageName &&
                process.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
        }
    }

    private fun scheduleFallback(source: AutostartSource, attempts: Int) {
        if (attempts >= MAX_ATTEMPTS_PER_BOOT) return
        val alarmScheduled = runCatching {
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return@runCatching false
            val intent = Intent(appContext, AutostartAlarmReceiver::class.java).apply {
                setPackage(appContext.packageName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                10,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + FALLBACK_DELAY_MS,
                pendingIntent,
            )
            true
        }.getOrDefault(false)
        if (!alarmScheduled && source != AutostartSource.WORKER_FALLBACK) {
            BackgroundSyncScheduler.enqueueAutostartFallback(appContext)
        }
    }

    private fun reportDiagnosticsIfUnlocked() {
        val userManager = appContext.getSystemService(Context.USER_SERVICE) as? UserManager
        if (userManager?.isUserUnlocked == false) return
        runCatching {
            ((appContext as? SVPlayerApplication)?.appContainer ?: AppContainer(appContext))
                .deviceDiagnosticsReporter
                .reportAutostartAsync()
        }
    }

    companion object {
        const val EXTRA_AUTOSTART_SOURCE = "autostart_source"
        private const val MAX_ATTEMPTS_PER_BOOT = 4
        private const val FALLBACK_DELAY_MS = 15_000L
    }
}
