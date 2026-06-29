package com.smartvision.svplayer

import android.app.Application
import com.smartvision.svplayer.core.data.AppContainer
import com.smartvision.svplayer.startup.BackgroundSyncScheduler
import com.smartvision.svplayer.startup.StartupStateStore

class SVPlayerApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        appContainer.anomalyReporter.installCrashHandler()
        appContainer.anomalyReporter.flushPendingAsync()
        appContainer.anomalyReporter.reportPreviousProcessExitAsync()
        appContainer.deviceDiagnosticsReporter.syncLatestAsync()
        BackgroundSyncScheduler.applyPeriodicSync(this, StartupStateStore(this).isBackgroundSyncEnabled())
    }
}
