package com.smartvision.svplayer.data.notifications

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface NotificationsApiService {
    @Headers("Cache-Control: no-cache", "Pragma: no-cache")
    @GET("api/notifications.php")
    suspend fun getNotifications(
        @Query("device_id") deviceId: String,
        @Query("public_device_code") publicDeviceCode: String,
        @Query("device_token") deviceToken: String,
        @Query("app_version_code") appVersionCode: Int,
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
    @SerializedName("seen_at") val seenAt: String? = null,
    @SerializedName("type") val type: String = "important_info",
    @SerializedName("source_version_code") val sourceVersionCode: Int? = null,
    @SerializedName("details") val details: RemoteNotificationDetails? = null,
)

data class RemoteNotificationDetails(
    @SerializedName("xtream_host") val xtreamHost: String? = null,
    @SerializedName("xtream_username") val xtreamUsername: String? = null,
    @SerializedName("xtream_password") val xtreamPassword: String? = null,
    @SerializedName("m3u_url") val m3uUrl: String? = null,
    @SerializedName("epg_url") val epgUrl: String? = null,
    @SerializedName("submitted_at") val submittedAt: String? = null,
    @SerializedName("ip_address") val ipAddress: String? = null,
    @SerializedName("country_code") val countryCode: String? = null,
    @SerializedName("sender_device") val senderDevice: String? = null,
    @SerializedName("source") val source: String? = null,
)

data class MarkNotificationsSeenRequest(
    @SerializedName("action") val action: String = "mark_seen",
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("public_device_code") val publicDeviceCode: String,
    @SerializedName("device_token") val deviceToken: String,
    @SerializedName("app_version_code") val appVersionCode: Int,
    @SerializedName("notification_ids") val notificationIds: List<Long> = emptyList(),
)

data class MarkNotificationsSeenResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("marked_seen") val markedSeen: Int = 0,
    @SerializedName("unread_count") val unreadCount: Int = 0,
    @SerializedName("error") val error: String? = null,
)
