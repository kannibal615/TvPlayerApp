package com.smartvision.svplayer.data.monetization

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.data.activation.ActivationRepository
import kotlinx.coroutines.flow.first
import retrofit2.http.Body
import retrofit2.http.POST

interface AdsEventsApiService {
    @POST("api/app/ads-events")
    suspend fun storeEvent(@Body request: AdsEventRequest): AdsEventResponse
}

data class AdsEventRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("appVersion") val appVersion: String,
    @SerializedName("platform") val platform: String = "ANDROID_TV",
    @SerializedName("userStatus") val userStatus: String,
    @SerializedName("contentType") val contentType: String,
    @SerializedName("provider") val provider: String = "UNKNOWN",
    @SerializedName("eventType") val eventType: String,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("errorCode") val errorCode: String? = null,
    @SerializedName("errorMessage") val errorMessage: String? = null,
)

data class AdsEventResponse(
    @SerializedName("success") val success: Boolean = false,
)

class AdsEventReporter(
    private val activationRepository: ActivationRepository,
    private val api: AdsEventsApiService,
) {
    suspend fun report(
        contentType: PlayerContentType,
        eventType: String,
        reason: String? = null,
        errorCode: String? = null,
        errorMessage: String? = null,
    ) {
        val activation = activationRepository.localState.first()
        if (activation.deviceId.isBlank()) return

        runCatching {
            api.storeEvent(
                AdsEventRequest(
                    deviceId = activation.deviceId,
                    appVersion = BuildConfig.VERSION_NAME,
                    userStatus = activation.monetizationStatus()?.name ?: "UNKNOWN",
                    contentType = contentType.name,
                    provider = "HILLTOPADS_VAST",
                    eventType = eventType,
                    reason = reason,
                    errorCode = errorCode,
                    errorMessage = errorMessage?.take(255),
                ),
            )
        }.onFailure {
            Log.w(TAG, "Evenement publicitaire non envoye")
        }
    }

    suspend fun reportStarted(contentType: PlayerContentType) =
        report(contentType, eventType = "AD_STARTED")

    private companion object {
        const val TAG = "SmartVisionAdsEvents"
    }
}
