package com.smartvision.svplayer.data.activation

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ActivationApiService {
    @POST("api/devices/register.php")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest,
    ): DeviceRegistrationResponse

    @POST("api/create_activation_session.php")
    suspend fun createActivationSession(
        @Body request: CreateActivationSessionRequest,
    ): CreateActivationSessionResponse

    @GET("api/device_status.php")
    suspend fun getDeviceStatus(
        @Query("device_id") deviceId: String,
        @Query("device_token") deviceToken: String?,
    ): DeviceStatusResponse

    @POST("api/create_playlist_setup_session.php")
    suspend fun createPlaylistSetupSession(
        @Body request: PlaylistSetupSessionRequest,
    ): CreateActivationSessionResponse

    @POST("api/licenses/activate.php")
    suspend fun activateLicense(
        @Body request: ActivateLicenseRequest,
    ): DeviceStatusResponse

    @POST("api/devices/start_trial.php")
    suspend fun startTrial(
        @Body request: DeviceAccessRequest,
    ): DeviceStatusResponse

    @POST("api/devices/enable_free_with_ads.php")
    suspend fun enableFreeWithAds(
        @Body request: DeviceAccessRequest,
    ): DeviceStatusResponse
}

data class RegisterDeviceRequest(
    @SerializedName("platform") val platform: String,
    @SerializedName("androidIdHash") val androidIdHash: String,
    @SerializedName("deviceFingerprintHash") val deviceFingerprintHash: String,
    @SerializedName("appPackage") val appPackage: String,
    @SerializedName("appVersion") val appVersion: String,
    @SerializedName("deviceManufacturer") val deviceManufacturer: String,
    @SerializedName("deviceModel") val deviceModel: String,
)

data class CreateActivationSessionRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_name") val deviceName: String,
    @SerializedName("app_version") val appVersion: String,
)

data class PlaylistSetupSessionRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_token") val deviceToken: String,
)

data class ActivateLicenseRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("publicDeviceCode") val publicDeviceCode: String,
    @SerializedName("licenseCode") val licenseCode: String,
)

data class DeviceAccessRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("publicDeviceCode") val publicDeviceCode: String,
)

data class DeviceRegistrationResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("serverDeviceId") val serverDeviceId: String? = null,
    @SerializedName("device_id") val legacyDeviceId: String? = null,
    @SerializedName("publicDeviceCode") val publicDeviceCode: String? = null,
    @SerializedName("activationStatus") val activationStatus: String? = null,
    @SerializedName("licenseStatus") val licenseStatus: String? = null,
    @SerializedName("trialStatus") val trialStatus: String? = null,
    @SerializedName("freeWithAdsStatus") val freeWithAdsStatus: String? = null,
    @SerializedName("xtreamStatus") val xtreamStatus: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("activation_type") val activationType: String? = null,
    @SerializedName("playlist_configured") val playlistConfigured: Boolean = false,
    @SerializedName("device_token") val deviceToken: String? = null,
    @SerializedName("polling_interval") val pollingInterval: Int? = null,
    @SerializedName("error") val error: String? = null,
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
    @SerializedName("serverDeviceId") val serverDeviceId: String? = null,
    @SerializedName("device_id") val legacyDeviceId: String? = null,
    @SerializedName("publicDeviceCode") val publicDeviceCode: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("activation_type") val activationType: String? = null,
    @SerializedName("licenseStatus") val licenseStatus: String? = null,
    @SerializedName("trialStatus") val trialStatus: String? = null,
    @SerializedName("freeWithAdsStatus") val freeWithAdsStatus: String? = null,
    @SerializedName("xtreamStatus") val xtreamStatus: String? = null,
    @SerializedName("playlist_configured") val playlistConfigured: Boolean = false,
    @SerializedName("playlist_config") val playlistConfig: PlaylistConfigResponse? = null,
    @SerializedName("error") val error: String? = null,
)

data class PlaylistConfigResponse(
    @SerializedName("host") val host: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("password") val password: String? = null,
)
