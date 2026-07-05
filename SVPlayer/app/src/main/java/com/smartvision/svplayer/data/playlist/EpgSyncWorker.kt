package com.smartvision.svplayer.data.playlist

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smartvision.svplayer.SVPlayerApplication
import com.smartvision.svplayer.core.data.AppContainer
import java.util.concurrent.TimeUnit

class EpgSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as? SVPlayerApplication)?.appContainer
            ?: AppContainer(applicationContext)
        val epgUrl = container.accountManager.epgUrl.value
        if (epgUrl.isBlank()) return Result.success()

        return container.epgRepository
            .synchronizeIfStale(epgUrl, EpgMinRefreshAgeMs)
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
    }
}

object EpgSyncScheduler {
    private const val WORK_NAME = "epg_hourly_sync"

    fun apply(context: Context) {
        val request = PeriodicWorkRequestBuilder<EpgSyncWorker>(1, TimeUnit.HOURS)
            .setInitialDelay(5, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}

private val EpgMinRefreshAgeMs = TimeUnit.HOURS.toMillis(1)
