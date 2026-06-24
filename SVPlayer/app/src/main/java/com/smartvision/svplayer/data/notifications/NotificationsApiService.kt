package com.smartvision.svplayer.data.notifications

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface NotificationsApiService {
    @GET("api/notifications.php")
    suspend fun getNotifications(
        @Query("device_id") deviceId: String,
        @Query("public_device_code") publicDeviceCode: String,
    ): NotificationsResponse
}

data class NotificationsResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("notifications") val notifications: List<RemoteNotification> = emptyList(),
    @SerializedName("error") val error: String? = null,
)

data class RemoteNotification(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("priority") val priority: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("expires_at") val expiresAt: String?,
)
