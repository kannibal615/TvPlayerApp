package com.smartvision.svplayer.startup

import android.content.Context
import android.os.SystemClock
import android.os.UserManager
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.smartvision.svplayer.SVPlayerApplication
import com.smartvision.svplayer.core.data.AppContainer
import com.smartvision.svplayer.data.xtream.XtreamConnectionStatus
import kotlinx.coroutines.flow.first

class PlaylistSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val stateStore = StartupStateStore(applicationContext)
    private val container by lazy {
        (applicationContext as? SVPlayerApplication)?.appContainer ?: AppContainer(applicationContext)
    }

    override suspend fun doWork(): Result {
        val startedAt = SystemClock.elapsedRealtime()
        val source = AutoSyncSource.entries.firstOrNull { it.name == inputData.getString(KEY_SOURCE) } ?: AutoSyncSource.BOOT
        val userManager = applicationContext.getSystemService(Context.USER_SERVICE) as? UserManager
        val isUnlocked = userManager?.isUserUnlocked ?: true

        if (!stateStore.isBackgroundSyncEnabled()) {
            log(source, "disabled", startedAt, null, "background_sync_disabled")
            return Result.success()
        }

        if (!isUnlocked) {
            recordWithoutReporter(source, "waiting_unlock", startedAt, null, "credentials_unavailable_until_unlock")
            return Result.retry()
        }

        val activation = runCatching { container.activationRepository.checkStatus() }.getOrElse {
            val local = container.activationRepository.localState.first()
            if (local.activated || local.trialStatus == "active" || local.freeWithAdsStatus == "active") {
                null
            } else {
                log(source, "skipped", startedAt, null, "inactive_license_state")
                return Result.success()
            }
        }
        if (activation != null && !activation.activated && activation.trialStatus != "active" && activation.freeWithAdsStatus != "active") {
            log(source, "skipped", startedAt, null, "inactive_license_state")
            return Result.success()
        }

        if (!container.accountManager.current().isConfigured) {
            log(source, "skipped", startedAt, null, "missing_xtream_credentials")
            return Result.success()
        }

        val connection = container.xtreamConnectionManager.verifyQuick("autosync_${source.name.lowercase()}")
        if (!connection.isConnected) {
            log(source, "error", startedAt, null, connection.status.name)
            return if (connection.status == XtreamConnectionStatus.NETWORK_ERROR) {
                Result.retry()
            } else {
                Result.success()
            }
        }

        val lastSyncAt = runCatching { container.syncStateDao.get()?.lastSync }.getOrNull()
        if (lastSyncAt != null && System.currentTimeMillis() - lastSyncAt < RECENT_SYNC_WINDOW_MS) {
            log(source, "recent", startedAt, null, null)
            return Result.success()
        }

        return runCatching {
            container.xtreamRepository.clearCaches()
            container.synchronizeCatalog()
        }.fold(
            onSuccess = {
                log(source, "success", startedAt, null, null)
                Result.success()
            },
            onFailure = { error ->
                log(source, "error", startedAt, null, error.message ?: error.javaClass.simpleName)
                Result.retry()
            },
        )
    }

    private fun log(
        source: AutoSyncSource,
        result: String,
        startedAt: Long,
        sizeKb: Long?,
        error: String?,
    ) {
        stateStore.recordAutoSyncResult(
            source = source,
            result = result,
            durationMs = SystemClock.elapsedRealtime() - startedAt,
            downloadedKilobytes = sizeKb,
            error = error,
        )
        container.deviceDiagnosticsReporter.reportAutoSyncAsync()
    }

    private fun recordWithoutReporter(
        source: AutoSyncSource,
        result: String,
        startedAt: Long,
        sizeKb: Long?,
        error: String?,
    ) {
        stateStore.recordAutoSyncResult(
            source = source,
            result = result,
            durationMs = SystemClock.elapsedRealtime() - startedAt,
            downloadedKilobytes = sizeKb,
            error = error,
        )
    }

    companion object {
        private const val KEY_SOURCE = "source"
        private const val KEY_REASON = "reason"
        private const val RECENT_SYNC_WINDOW_MS = 24 * 60 * 60 * 1_000L

        fun inputData(source: AutoSyncSource, reason: String): Data =
            Data.Builder()
                .putString(KEY_SOURCE, source.name)
                .putString(KEY_REASON, reason)
                .build()
    }
}
