package com.smartvision.svplayer

import android.app.Application
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory

class SVPlayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.ADS_RUNTIME_CONFIGURED) {
            val factory = ImaSdkFactory.getInstance()
            val settings = factory.createImaSdkSettings().apply {
                playerType = "SmartVision Android TV"
                playerVersion = BuildConfig.VERSION_NAME
                language = "fr"
                isDebugMode = BuildConfig.DEBUG
            }
            factory.initialize(this, settings)
        }
    }
}
