package com.smartvision.svplayer.data.notifications

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface NotificationsApiService {
    @GET("api/notifications.php")
    suspend fun getNotifications(
        @Query("device_id") deviceId: String,
        @Query("public_device_code") publicDeviceCode: String,
    ): NotificationsResponse

    @POST("api/notifications.php")
    suspend fun markNotificationsSeen(
        @Body request: MarkNotificationsSeenRequest,
    ): MarkNotificationsSeenResponse
}

data class NotificationsResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("unread_count") val unreadCount: Int = 0,
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
    @SerializedName("seen") val seen: Boolean = false,
)

data class MarkNotificationsSeenRequest(
    @SerializedName("action") val action: String = "mark_seen",
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("public_device_code") val publicDeviceCode: String,
    @SerializedName("notification_ids") val notificationIds: List<Long> = emptyList(),
)

data class MarkNotificationsSeenResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("marked_seen") val markedSeen: Int = 0,
    @SerializedName("unread_count") val unreadCount: Int = 0,
    @SerializedName("error") val error: String? = null,
)
