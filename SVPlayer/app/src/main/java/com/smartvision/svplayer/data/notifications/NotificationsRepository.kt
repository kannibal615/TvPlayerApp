package com.smartvision.svplayer.data.notifications

import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.data.activation.ActivationException
import com.smartvision.svplayer.data.activation.ActivationRepository
import kotlinx.coroutines.flow.first

class NotificationsRepository(
    private val activationRepository: ActivationRepository,
    private val api: NotificationsApiService,
) {
    suspend fun getNotifications(): NotificationsSnapshot {
        val access = currentAccess()
        val response = api.getNotifications(
            deviceId = access.deviceId,
            publicDeviceCode = access.publicDeviceCode,
            deviceToken = access.deviceToken,
            appVersionCode = BuildConfig.VERSION_CODE,
        )
        if (!response.success) {
            throw ActivationException(response.error ?: "Notifications indisponibles.")
        }
        val notifications = response.notifications.map {
                AppNotification(
                    id = it.id,
                    title = it.title,
                    message = it.message,
                    priority = it.priority,
                    createdAt = it.createdAt,
                    expiresAt = it.expiresAt,
                    seen = it.seen,
                    seenAt = it.seenAt,
                    type = NotificationType.fromWire(it.type),
                    sourceVersionCode = it.sourceVersionCode,
                    details = it.details?.let { details ->
                        NotificationDetails(
                            xtreamHost = details.xtreamHost,
                            xtreamUsername = details.xtreamUsername,
                            xtreamPassword = details.xtreamPassword,
                            m3uUrl = details.m3uUrl,
                            epgUrl = details.epgUrl,
                            submittedAt = details.submittedAt,
                            ipAddress = details.ipAddress,
                            countryCode = details.countryCode,
                            senderDevice = details.senderDevice,
                            source = details.source,
                        )
                    },
                )
            }
        val visibleNotifications = excludeInstalledAppUpdates(notifications, BuildConfig.VERSION_CODE)
        return NotificationsSnapshot(
            unreadCount = visibleNotifications.count { !it.seen },
            notifications = visibleNotifications,
        )
    }

    suspend fun markSeen(notificationIds: List<Long> = emptyList()): Int {
        val access = currentAccess()
        val response = api.markNotificationsSeen(
            MarkNotificationsSeenRequest(
                deviceId = access.deviceId,
                publicDeviceCode = access.publicDeviceCode,
                deviceToken = access.deviceToken,
                appVersionCode = BuildConfig.VERSION_CODE,
                notificationIds = notificationIds,
            )
        )
        if (!response.success) {
            throw ActivationException(response.error ?: "Marquage notifications impossible.")
        }
        return response.unreadCount.coerceAtLeast(0)
    }

    suspend fun markAllSeen(): Int = markSeen(emptyList())

    suspend fun clearHistory(): Int {
        val access = currentAccess()
        val response = api.markNotificationsSeen(
            MarkNotificationsSeenRequest(
                action = "clear_history",
                deviceId = access.deviceId,
                publicDeviceCode = access.publicDeviceCode,
                deviceToken = access.deviceToken,
                appVersionCode = BuildConfig.VERSION_CODE,
            )
        )
        if (!response.success) {
            throw ActivationException(response.error ?: "Suppression historique notifications impossible.")
        }
        return response.clearedHistory.coerceAtLeast(0)
    }

    suspend fun refreshDeviceStatus() {
        activationRepository.checkStatus()
    }

    private suspend fun currentAccess(): NotificationDeviceAccess {
        val createdDeviceId = activationRepository.getOrCreateDeviceId()
        val state = activationRepository.localState.first()
        return NotificationDeviceAccess(
            deviceId = state.deviceId.ifBlank { createdDeviceId },
            publicDeviceCode = state.publicDeviceCode,
            deviceToken = activationRepository.getDeviceToken(),
        )
    }
}

internal fun excludeInstalledAppUpdates(
    notifications: List<AppNotification>,
    installedVersionCode: Int,
): List<AppNotification> = notifications.filterNot { notification ->
    notification.type == NotificationType.AppUpdate &&
        notification.sourceVersionCode?.let { it <= installedVersionCode } == true
}

data class NotificationsSnapshot(
    val unreadCount: Int,
    val notifications: List<AppNotification>,
)

data class AppNotification(
    val id: Long,
    val title: String,
    val message: String,
    val priority: String,
    val createdAt: String,
    val expiresAt: String?,
    val seen: Boolean,
    val seenAt: String?,
    val type: NotificationType,
    val sourceVersionCode: Int?,
    val details: NotificationDetails?,
)

enum class NotificationType(val wireValue: String) {
    AppUpdate("app_update"),
    PlaylistAdded("playlist_added"),
    ImportantInfo("important_info");

    companion object {
        fun fromWire(value: String?): NotificationType =
            entries.firstOrNull { it.wireValue == value } ?: ImportantInfo
    }
}

data class NotificationDetails(
    val xtreamHost: String?,
    val xtreamUsername: String?,
    val xtreamPassword: String?,
    val m3uUrl: String?,
    val epgUrl: String?,
    val submittedAt: String?,
    val ipAddress: String?,
    val countryCode: String?,
    val senderDevice: String?,
    val source: String?,
)

private data class NotificationDeviceAccess(
    val deviceId: String,
    val publicDeviceCode: String,
    val deviceToken: String,
)
