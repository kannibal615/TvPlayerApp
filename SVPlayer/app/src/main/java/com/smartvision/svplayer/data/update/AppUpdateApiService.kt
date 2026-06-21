package com.smartvision.svplayer.data.update

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface AppUpdateApiService {
    @GET("api/app_update.php")
    suspend fun checkUpdate(
        @Query("version_code") versionCode: Int,
        @Query("version_name") versionName: String,
        @Query("platform") platform: String = "android_tv",
    ): AppUpdateResponse
}

data class AppUpdateResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("update_available") val updateAvailable: Boolean = false,
    @SerializedName("latest_version_code") val latestVersionCode: Int = 0,
    @SerializedName("latest_version_name") val latestVersionName: String? = null,
    @SerializedName("apk_url") val apkUrl: String? = null,
    @SerializedName("apk_sha256") val apkSha256: String? = null,
    @SerializedName("apk_size") val apkSize: Long? = null,
    @SerializedName("mandatory") val mandatory: Boolean = false,
    @SerializedName("release_notes") val releaseNotes: String? = null,
    @SerializedName("error") val error: String? = null,
)

