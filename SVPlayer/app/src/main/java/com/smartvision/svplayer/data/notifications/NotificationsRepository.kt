package com.smartvision.svplayer.data.notifications

import com.smartvision.svplayer.data.activation.ActivationException
import com.smartvision.svplayer.data.activation.ActivationRepository
import kotlinx.coroutines.flow.first

class NotificationsRepository(
    private val activationRepository: ActivationRepository,
    private val api: NotificationsApiService,
) {
    suspend fun getNotifications(): List<AppNotification> {
        val deviceId = activationRepository.getOrCreateDeviceId()
        val state = activationRepository.localState.first()
        val response = api.getNotifications(
            deviceId = state.deviceId.ifBlank { deviceId },
            publicDeviceCode = state.publicDeviceCode,
        )
        if (!response.success) {
            throw ActivationException(response.error ?: "Notifications indisponibles.")
        }
        return response.notifications.map {
            AppNotification(
                id = it.id,
                title = it.title,
                message = it.message,
                priority = it.priority,
                createdAt = it.createdAt,
                expiresAt = it.expiresAt,
            )
        }
    }
}

data class AppNotification(
    val id: Long,
    val title: String,
    val message: String,
    val priority: String,
    val createdAt: String,
    val expiresAt: String?,
)
