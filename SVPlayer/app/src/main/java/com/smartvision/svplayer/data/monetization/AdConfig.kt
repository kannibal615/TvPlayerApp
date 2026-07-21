package com.smartvision.svplayer.data.monetization

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.smartvision.svplayer.BuildConfig
import retrofit2.http.GET

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
    suspend fun refresh(): AdConfig = current()
}

interface AdConfigApiService {
    @GET("api/app/ads-config")
    suspend fun getAdConfig(): RemoteAdConfig
}

data class RemoteAdConfig(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("adsEnabled") val adsEnabled: Boolean = true,
    @SerializedName("adsOnlyInsidePlayer") val adsOnlyInsidePlayer: Boolean = true,
    @SerializedName("minMinutesBetweenAds") val minMinutesBetweenAds: Int = 30,
    @SerializedName("maxAdsPerDay") val maxAdsPerDay: Int = 3,
    @SerializedName("showAdBeforeLiveStream") val showAdBeforeLiveStream: Boolean = true,
    @SerializedName("showAdBeforeMovie") val showAdBeforeMovie: Boolean = true,
    @SerializedName("showAdBeforeSeriesEpisode") val showAdBeforeSeriesEpisode: Boolean = true,
    @SerializedName("allowPlaybackIfAdFails") val allowPlaybackIfAdFails: Boolean = true,
    @SerializedName("vastTagUrl") val vastTagUrl: String = "",
)

class RemoteAdConfigProvider(
    private val api: AdConfigApiService,
    fallback: AdConfig = AdConfig(),
) : AdConfigProvider {
    @Volatile
    private var cached = fallback

    override fun current(): AdConfig = cached

    override suspend fun refresh(): AdConfig {
        return try {
            val response = api.getAdConfig()
            if (!response.success) {
                cached
            } else {
                response.toAdConfig(cached).also { cached = it }
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Configuration publicitaire distante indisponible", exception)
            cached
        }
    }

    private companion object {
        const val TAG = "SmartVisionAdsConfig"
    }
}

internal fun RemoteAdConfig.toAdConfig(fallback: AdConfig): AdConfig =
    AdConfig(
        adsEnabled = adsEnabled,
        adsOnlyInsidePlayer = adsOnlyInsidePlayer,
        minMinutesBetweenAds = minMinutesBetweenAds.coerceIn(1, 1440),
        maxAdsPerDay = maxAdsPerDay.coerceIn(1, 500),
        showAdBeforeLiveStream = showAdBeforeLiveStream,
        showAdBeforeMovie = showAdBeforeMovie,
        showAdBeforeSeriesEpisode = showAdBeforeSeriesEpisode,
        allowPlaybackIfAdFails = allowPlaybackIfAdFails,
        adTagUrl = vastTagUrl.trim().ifBlank { fallback.adTagUrl },
        requestTimeoutSeconds = fallback.requestTimeoutSeconds,
    )
