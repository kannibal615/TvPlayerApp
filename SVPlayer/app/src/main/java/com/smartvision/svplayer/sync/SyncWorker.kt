package com.smartvision.svplayer.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.BackoffPolicy
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return Result.success()
    }
}

object CatalogSyncScheduler {
    private const val WORK_NAME = "catalog_sync"
    private const val SYNC_REPEAT_HOURS = 6L
    private const val STARTUP_DELAY_MINUTES = 30L
    private const val RETRY_DELAY_MINUTES = 15L
    private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun schedule(context: Context) {
        val appContext = context.applicationContext
        schedulerScope.launch {
            runCatching { enqueue(appContext) }
        }
    }

    private fun enqueue(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(SYNC_REPEAT_HOURS, TimeUnit.HOURS)
            .setInitialDelay(STARTUP_DELAY_MINUTES, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_DELAY_MINUTES, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
