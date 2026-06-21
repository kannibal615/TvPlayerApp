package com.smartvision.svplayer.data.activation

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ActivationApiService {
    @POST("api/create_activation_session.php")
    suspend fun createActivationSession(
        @Body request: CreateActivationSessionRequest,
    ): CreateActivationSessionResponse

    @GET("api/device_status.php")
    suspend fun getDeviceStatus(
        @Query("device_id") deviceId: String,
        @Query("device_token") deviceToken: String?,
    ): DeviceStatusResponse
}

data class CreateActivationSessionRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_name") val deviceName: String,
    @SerializedName("app_version") val appVersion: String,
)

data class CreateActivationSessionResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("device_id") val deviceId: String? = null,
    @SerializedName("short_code") val shortCode: String? = null,
    @SerializedName("qr_url") val qrUrl: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("polling_interval") val pollingInterval: Int? = null,
    @SerializedName("device_token") val deviceToken: String? = null,
    @SerializedName("error") val error: String? = null,
)

data class DeviceStatusResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("status") val status: String? = null,
    @SerializedName("activated") val activated: Boolean = false,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("activation_type") val activationType: String? = null,
    @SerializedName("playlist_configured") val playlistConfigured: Boolean = false,
    @SerializedName("playlist_config") val playlistConfig: PlaylistConfigResponse? = null,
    @SerializedName("error") val error: String? = null,
)

data class PlaylistConfigResponse(
    @SerializedName("host") val host: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("password") val password: String? = null,
)
