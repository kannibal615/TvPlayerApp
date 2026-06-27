package com.smartvision.svplayer

import android.app.Application
import com.smartvision.svplayer.core.data.AppContainer

class SVPlayerApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        appContainer.anomalyReporter.installCrashHandler()
        appContainer.anomalyReporter.flushPendingAsync()
        appContainer.anomalyReporter.reportPreviousProcessExitAsync()
    }
}
