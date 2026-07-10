@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.smartvision.svplayer.data.monetization

import android.content.Context
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

const val SMARTVISION_AD_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 12; Android TV) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

fun smartVisionMediaSourceFactory(context: Context): DefaultMediaSourceFactory {
    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(SMARTVISION_AD_USER_AGENT)
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(10_000)
        .setReadTimeoutMs(15_000)

    return DefaultMediaSourceFactory(
        DefaultDataSource.Factory(context.applicationContext, httpDataSourceFactory),
    )
}
