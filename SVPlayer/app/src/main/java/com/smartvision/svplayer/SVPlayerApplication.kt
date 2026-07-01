package com.smartvision.svplayer

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.smartvision.svplayer.core.data.AppContainer
import com.smartvision.svplayer.startup.BackgroundSyncScheduler
import com.smartvision.svplayer.startup.StartupStateStore

class SVPlayerApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        Handler(Looper.getMainLooper()).post {
            val container = appContainer
            container.anomalyReporter.installCrashHandler()
            container.anomalyReporter.flushPendingAsync()
            container.anomalyReporter.reportPreviousProcessExitAsync()
            container.deviceDiagnosticsReporter.syncLatestAsync()
            BackgroundSyncScheduler.applyPeriodicSync(this, StartupStateStore(this).isBackgroundSyncEnabled())
        }
    }
}
