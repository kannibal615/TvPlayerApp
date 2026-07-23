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

    @POST("api/device_profiles.php")
    suspend fun syncDeviceProfiles(@Body request: DeviceProfilesRequest): DeviceProfilesResponse

    @POST("api/clear_playlist_config.php")
    suspend fun clearPlaylistConfig(
        @Body request: ClearPlaylistConfigRequest,
    ): ClearPlaylistConfigResponse

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
    @SerializedName("localPublicDeviceCode") val localPublicDeviceCode: String,
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

data class ClearPlaylistConfigRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_token") val deviceToken: String,
    @SerializedName("host") val host: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("password") val password: String? = null,
    @SerializedName("epg_url") val epgUrl: String? = null,
    @SerializedName("m3u_url") val m3uUrl: String? = null,
)

data class ActivateLicenseRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("publicDeviceCode") val publicDeviceCode: String,
    @SerializedName("licenseCode") val licenseCode: String,
)

data class DeviceAccessRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("publicDeviceCode") val publicDeviceCode: String,
    @SerializedName("playlistConfigured") val playlistConfigured: Boolean = false,
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

data class ClearPlaylistConfigResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("cleared") val cleared: Boolean = false,
    @SerializedName("playlist_configured") val playlistConfigured: Boolean = false,
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
    @SerializedName("epg_url") val epgUrl: String? = null,
    @SerializedName("m3u_url") val m3uUrl: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("provided_fields") val providedFields: List<String> = emptyList(),
    @SerializedName("config_id") val configId: String? = null,
    @SerializedName("target_profile_ids") val targetProfileIds: List<String> = emptyList(),
    @SerializedName("new_profile_name") val newProfileName: String? = null,
)

data class DeviceProfilesRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_token") val deviceToken: String,
    @SerializedName("capability_version") val capabilityVersion: Int = 1,
    @SerializedName("profiles") val profiles: List<DeviceProfileSummary>,
)

data class DeviceProfileSummary(
    @SerializedName("profile_id") val profileId: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
)

data class DeviceProfilesResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("profile_count") val profileCount: Int = 0,
    @SerializedName("error") val error: String? = null,
)
