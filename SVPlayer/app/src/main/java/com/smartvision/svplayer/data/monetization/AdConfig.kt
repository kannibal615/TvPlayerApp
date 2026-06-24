package com.smartvision.svplayer.data.monetization

import com.smartvision.svplayer.BuildConfig

data class AdConfig(
    val adsEnabled: Boolean = true,
    val adsOnlyInsidePlayer: Boolean = true,
    val minMinutesBetweenAds: Int = 30,
    val maxAdsPerDay: Int = 3,
    val showAdBeforeLiveStream: Boolean = true,
    val showAdBeforeMovie: Boolean = true,
    val showAdBeforeSeriesEpisode: Boolean = true,
    val allowPlaybackIfAdFails: Boolean = true,
    val adTagUrl: String = BuildConfig.VIDEO_AD_TAG_URL,
    val requestTimeoutSeconds: Long = 12,
)

interface AdConfigProvider {
    fun current(): AdConfig
}

class StaticAdConfigProvider(
    private val config: AdConfig = AdConfig(),
) : AdConfigProvider {
    override fun current(): AdConfig = config
}
