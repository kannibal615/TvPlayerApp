package com.smartvision.svplayer

import android.app.Application
import com.smartvision.svplayer.core.data.AppContainer
import com.smartvision.svplayer.sync.CatalogSyncScheduler

class SVPlayerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CatalogSyncScheduler.schedule(this)
    }
}
