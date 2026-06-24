package com.smartvision.svplayer.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(60_000)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repository.getNotifications() }
                .onSuccess { snapshot ->
                    state.update {
                        it.copy(
                            unreadCount = snapshot.unreadCount,
                            hasUnread = snapshot.unreadCount > 0,
                        )
                    }
                }
        }
    }

    fun clearUnread() {
        state.update { it.copy(unreadCount = 0, hasUnread = false) }
        viewModelScope.launch {
            runCatching { repository.markSeen() }
        }
    }
}

data class NotificationBadgeUiState(
    val unreadCount: Int = 0,
    val hasUnread: Boolean = false,
)
