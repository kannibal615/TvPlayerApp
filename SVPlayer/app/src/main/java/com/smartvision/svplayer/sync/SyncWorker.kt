package com.smartvision.svplayer.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.BackoffPolicy
import com.smartvision.svplayer.SVPlayerApplication
import com.smartvision.svplayer.core.data.AppContainer
import com.smartvision.svplayer.data.xtream.XtreamConnectionStatus
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
        val container = (applicationContext as? SVPlayerApplication)?.appContainer
            ?: AppContainer(applicationContext)
        return runCatching {
            val connection = container.xtreamConnectionManager.verifyQuick("manual_or_periodic_sync")
            if (!connection.isConnected) {
                if (connection.status == XtreamConnectionStatus.NETWORK_ERROR) {
                    throw IllegalStateException(connection.message)
                }
                return Result.success()
            }
            container.xtreamRepository.clearCaches()
            container.synchronizeCatalog()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }
}

object CatalogSyncScheduler {
    private const val WORK_NAME = "catalog_sync"
    private const val STARTUP_DELAY_MINUTES = 30L
    private const val RETRY_DELAY_MINUTES = 15L
    private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun apply(context: Context, syncFrequency: String) {
        val appContext = context.applicationContext
        schedulerScope.launch {
            runCatching {
                val policy = SyncFrequencyPolicy.from(syncFrequency)
                val repeatHours = policy.repeatHours
                if (repeatHours == null) {
                    WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
                } else {
                    enqueuePeriodic(appContext, repeatHours)
                }
            }
        }
    }

    fun enqueueOnce(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_DELAY_MINUTES, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "${WORK_NAME}_startup",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun enqueuePeriodic(context: Context, repeatHours: Long) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(repeatHours, TimeUnit.HOURS)
            .setInitialDelay(STARTUP_DELAY_MINUTES, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_DELAY_MINUTES, TimeUnit.MINUTES)
            .setConstraints(networkConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun networkConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
}
