package com.smartvision.svplayer

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.smartvision.svplayer.core.data.AppContainer
import com.smartvision.svplayer.data.diagnostics.PerformanceDiagnosticRecorder
import com.smartvision.svplayer.data.playlist.EpgSyncScheduler
import com.smartvision.svplayer.startup.BackgroundSyncScheduler
import com.smartvision.svplayer.startup.StartupStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class SVPlayerApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // PERF_DIAG: no-op outside releaseDiagnostic; initializes the local artifact directory early.
        PerformanceDiagnosticRecorder.init(this)
        Handler(Looper.getMainLooper()).postDelayed({
            startupScope.launch {
                val container = appContainer
                startupScope.launch {
                    container.accountManager.profiles
                        .collectLatest { profiles ->
                            delay(ProfileInventoryDebounceMillis)
                            runCatching { container.activationRepository.publishProfileInventory(profiles) }
                        }
                }
                container.anomalyReporter.installCrashHandler()
                container.anomalyReporter.flushPendingAsync()
                container.anomalyReporter.reportPreviousProcessExitAsync()
                container.deviceDiagnosticsReporter.syncLatestAsync()
                BackgroundSyncScheduler.applyPeriodicSync(
                    this@SVPlayerApplication,
                    StartupStateStore(this@SVPlayerApplication).isBackgroundSyncEnabled(),
                )
                EpgSyncScheduler.apply(this@SVPlayerApplication)
            }
        }, DeferredStartupDiagnosticsDelayMillis)
    }

    private companion object {
        const val DeferredStartupDiagnosticsDelayMillis = 1_200L
        const val ProfileInventoryDebounceMillis = 500L
    }
}
