package com.smartvision.svplayer.startup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BackgroundSyncScheduler {
    private const val BOOT_SYNC_WORK_NAME = "playlist_boot_sync"
    private const val PERIODIC_SYNC_WORK_NAME = "playlist_periodic_sync"
    private const val AUTOSTART_FALLBACK_WORK_NAME = "autostart_worker_fallback"

    fun scheduleBootSync(context: Context, source: AutostartSource) {
        val request = OneTimeWorkRequestBuilder<PlaylistSyncWorker>()
            .setInputData(PlaylistSyncWorker.inputData(AutoSyncSource.BOOT, source.name))
            .setInitialDelay(30, TimeUnit.SECONDS)
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            BOOT_SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun applyPeriodicSync(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<PlaylistSyncWorker>(24, TimeUnit.HOURS)
            .setInputData(PlaylistSyncWorker.inputData(AutoSyncSource.PERIODIC, "periodic"))
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun enqueueAutostartFallback(context: Context) {
        val request = OneTimeWorkRequestBuilder<AutostartFallbackWorker>()
            .setInitialDelay(15, TimeUnit.SECONDS)
            .setConstraints(networkConstraints())
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            AUTOSTART_FALLBACK_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun networkConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
}

class AutostartFallbackWorker(
    appContext: Context,
    params: androidx.work.WorkerParameters,
) : androidx.work.CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        AutostartManager(applicationContext).attemptAutostart(applicationContext, AutostartSource.WORKER_FALLBACK)
        return Result.success()
    }
}
