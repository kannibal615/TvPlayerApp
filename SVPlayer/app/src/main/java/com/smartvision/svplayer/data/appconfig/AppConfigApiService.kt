package com.smartvision.svplayer.data.appconfig

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AppConfigApiService {
    @GET("api/app_config.php")
    suspend fun getAppConfig(
        @Query("device_id") deviceId: String,
        @Query("public_device_code") publicDeviceCode: String,
    ): AppConfigResponse

    @POST("api/app_config.php")
    suspend fun acceptConsent(
        @Body request: AcceptConsentRequest,
    ): AcceptConsentResponse
}

data class AppConfigResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("consent") val consent: RemoteConsentConfig? = null,
    @SerializedName("accepted_consent_version") val acceptedConsentVersion: String? = null,
    @SerializedName("features") val features: List<RemoteFeatureAccess> = emptyList(),
    @SerializedName("trending") val trending: RemoteTrendingConfig? = null,
    @SerializedName("error") val error: String? = null,
)

data class AcceptConsentRequest(
    @SerializedName("action") val action: String = "accept_consent",
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("public_device_code") val publicDeviceCode: String,
    @SerializedName("consent_version") val consentVersion: String,
    @SerializedName("app_version") val appVersion: String,
)

data class AcceptConsentResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("accepted_consent_version") val acceptedConsentVersion: String? = null,
    @SerializedName("error") val error: String? = null,
)

data class RemoteConsentConfig(
    @SerializedName("version") val version: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("variables") val variables: Map<String, String> = emptyMap(),
)

data class RemoteFeatureAccess(
    @SerializedName("key") val key: String = "",
    @SerializedName("label") val label: String = "",
    @SerializedName("premium") val premium: Boolean = false,
    @SerializedName("trial") val trial: Boolean = false,
    @SerializedName("free_ads") val freeAds: Boolean = false,
)

data class RemoteTrendingConfig(
    @SerializedName("require_landscape_image") val requireLandscapeImage: Boolean? = null,
    @SerializedName("exclude_adult") val excludeAdult: Boolean? = null,
    @SerializedName("use_rating_filter") val useRatingFilter: Boolean? = null,
    @SerializedName("minimum_rating") val minimumRating: Float? = null,
    @SerializedName("candidate_limit") val candidateLimit: Int? = null,
    @SerializedName("section_limit") val sectionLimit: Int? = null,
)
