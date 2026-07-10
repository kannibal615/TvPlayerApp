package com.smartvision.svplayer.data.notifications

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
        )
        if (!response.success) {
            throw ActivationException(response.error ?: "Notifications indisponibles.")
        }
        return NotificationsSnapshot(
            unreadCount = response.unreadCount.coerceAtLeast(0),
            notifications = response.notifications.map {
                AppNotification(
                    id = it.id,
                    title = it.title,
                    message = it.message,
                    priority = it.priority,
                    createdAt = it.createdAt,
                    expiresAt = it.expiresAt,
                    seen = it.seen,
                )
            },
        )
    }

    suspend fun markSeen(notificationIds: List<Long> = emptyList()): Int {
        val access = currentAccess()
        val response = api.markNotificationsSeen(
            MarkNotificationsSeenRequest(
                deviceId = access.deviceId,
                publicDeviceCode = access.publicDeviceCode,
                notificationIds = notificationIds,
            )
        )
        if (!response.success) {
            throw ActivationException(response.error ?: "Marquage notifications impossible.")
        }
        return response.unreadCount.coerceAtLeast(0)
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
        )
    }
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
)

private data class NotificationDeviceAccess(
    val deviceId: String,
    val publicDeviceCode: String,
)
