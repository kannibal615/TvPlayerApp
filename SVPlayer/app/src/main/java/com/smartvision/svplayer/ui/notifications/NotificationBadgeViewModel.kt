package com.smartvision.svplayer.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.data.notifications.AppNotification
import com.smartvision.svplayer.data.notifications.NotificationsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NotificationBadgeViewModel(
    private val repository: NotificationsRepository,
) : ViewModel() {
    private val state = MutableStateFlow(NotificationBadgeUiState())
    val uiState: StateFlow<NotificationBadgeUiState> = state
    private val handledPlaylistNotificationIds = mutableSetOf<Long>()

    init {
        viewModelScope.launch {
            while (isActive) {
                refreshOnce()
                delay(60_000)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshOnce()
        }
    }

    fun clearUnread() {
        state.update { it.copy(unreadCount = 0, hasUnread = false) }
        viewModelScope.launch {
            runCatching { repository.markSeen() }
        }
    }

    private suspend fun refreshOnce() {
        runCatching { repository.getNotifications() }
            .onSuccess { snapshot ->
                val installedUpdateNotifications = snapshot.notifications.filter { it.isInstalledUpdateNotification() }
                if (installedUpdateNotifications.isNotEmpty()) {
                    runCatching { repository.markSeen(installedUpdateNotifications.map { it.id }) }
                }
                val visibleNotifications = snapshot.notifications - installedUpdateNotifications.toSet()
                val playlistNotifications = visibleNotifications.filter { notification ->
                    !notification.seen &&
                        notification.isPlaylistConfigurationNotification() &&
                        handledPlaylistNotificationIds.add(notification.id)
                }
                if (playlistNotifications.isNotEmpty()) {
                    runCatching { repository.refreshDeviceStatus() }
                }
                val visibleUnread = visibleNotifications.count { !it.seen }
                state.update {
                    it.copy(
                        unreadCount = visibleUnread,
                        hasUnread = visibleUnread > 0,
                    )
                }
            }
    }
}

data class NotificationBadgeUiState(
    val unreadCount: Int = 0,
    val hasUnread: Boolean = false,
)

private fun AppNotification.isInstalledUpdateNotification(): Boolean {
    val isUpdate = title.contains("update", ignoreCase = true) ||
        title.contains("mise a jour", ignoreCase = true) ||
        message.contains("install the update", ignoreCase = true) ||
        message.contains("installer la mise a jour", ignoreCase = true)
    if (!isUpdate) return false
    val versionCode = Regex("\\((\\d+)\\)").find(message)?.groupValues?.getOrNull(1)?.toIntOrNull()
    return versionCode != null && versionCode <= BuildConfig.VERSION_CODE
}

private fun AppNotification.isPlaylistConfigurationNotification(): Boolean =
    title.contains("Configuration playlist", ignoreCase = true) ||
        message.contains("Configuration recue depuis SmartVision Playlist", ignoreCase = true)
