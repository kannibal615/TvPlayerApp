package com.smartvision.svplayer.ui.notifications

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.notifications.AppNotification
import com.smartvision.svplayer.data.notifications.NotificationsRepository
import com.smartvision.svplayer.data.update.AppUpdateInfo
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.i18n.smartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun NotificationsRoute(
    onBack: () -> Unit,
    onNotificationsSeen: () -> Unit,
    updateNotification: AppUpdateInfo? = null,
    onOpenUpdate: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = com.smartvision.svplayer.domain.model.PlayerSettings(),
    )
    val strings = smartVisionStrings(settings.language)
    val viewModel: NotificationsViewModel = viewModel(
        factory = viewModelFactory {
            NotificationsViewModel(container.notificationsRepository)
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refresh(markSeen = true, onSeen = onNotificationsSeen)
    }

    NotificationsScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        updateNotification = updateNotification,
        onOpenUpdate = onOpenUpdate,
        strings = strings,
        modifier = modifier,
    )
}

@Composable
private fun NotificationsScreen(
    state: NotificationsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    updateNotification: AppUpdateInfo?,
    onOpenUpdate: () -> Unit,
    strings: SmartVisionStrings,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        SmartVisionColors.PrimaryDark.copy(alpha = 0.38f),
                        SmartVisionColors.Background,
                        Color(0xFF01040C),
                    ),
                    radius = 1500f,
                ),
            )
            .padding(horizontal = 34.dp, vertical = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvButton(
                text = strings.back,
                leadingIcon = Icons.Default.ArrowBack,
                onClick = onBack,
                variant = TvButtonVariant.Secondary,
                contentPadding = PaddingValues(horizontal = 18.dp),
                modifier = Modifier.height(42.dp),
            )
            Spacer(Modifier.width(18.dp))
            Icon(Icons.Default.Notifications, contentDescription = null, tint = SmartVisionColors.CyanAccent)
            Spacer(Modifier.width(10.dp))
            Text(
                text = strings.notifications,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleL,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            TvButton(
                text = if (state.loading) strings.refreshing else strings.refresh,
                leadingIcon = Icons.Default.Refresh,
                onClick = onRefresh,
                enabled = !state.loading,
                variant = TvButtonVariant.Secondary,
                modifier = Modifier.height(42.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD9091424), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
                .padding(20.dp),
        ) {
            when {
                state.loading && state.notifications.isEmpty() && updateNotification == null -> {
                    CircularProgressIndicator(
                        color = SmartVisionColors.CyanAccent,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                state.errorMessage != null -> {
                    Text(
                        text = state.errorMessage,
                        color = SmartVisionColors.Error,
                        style = SmartVisionType.Body,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                state.notifications.isEmpty() && updateNotification == null -> {
                    Text(
                        text = strings.noNotifications,
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Body,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (updateNotification != null) {
                            item("app-update") {
                                UpdateNotificationRow(updateNotification, onOpenUpdate, strings)
                            }
                        }
                        items(state.notifications, key = { it.id }) { notification ->
                            NotificationRow(notification, onClick = {})
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateNotificationRow(
    update: AppUpdateInfo,
    onClick: () -> Unit,
    strings: SmartVisionStrings,
) {
    NotificationCard(
        title = strings.updateAvailableTitle,
        message = strings.updateAvailableMessage.format(update.versionName),
        createdAt = "Version ${update.versionCode}",
        priority = if (update.mandatory) "urgent" else "important",
        onClick = onClick,
    )
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    onClick: () -> Unit,
) {
    NotificationCard(
        title = notification.title,
        message = notification.message,
        createdAt = notification.createdAt,
        priority = notification.priority,
        onClick = onClick,
    )
}

@Composable
private fun NotificationCard(
    title: String,
    message: String,
    createdAt: String,
    priority: String,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusTarget(
                state = focusState,
                pressed = pressed,
                glowColor = focusStyle.accent,
                cornerRadius = 8.dp,
            )
            .background(
                if (focusState.isFocused) SmartVisionColors.CyanAccent.copy(alpha = 0.14f) else Color(0xFF0B1728),
                shape,
            )
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else 1.dp,
                    when {
                        focusState.isFocused -> focusStyle.accent
                        priority == "urgent" -> SmartVisionColors.Warning
                        else -> SmartVisionColors.Border
                    },
                ),
                shape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = createdAt,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Body,
        )
    }
}

class NotificationsViewModel(
    private val repository: NotificationsRepository,
) : ViewModel() {
    private val state = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = state

    fun refresh(
        markSeen: Boolean = false,
        onSeen: (() -> Unit)? = null,
    ) {
        viewModelScope.launch {
            state.update { it.copy(loading = true, errorMessage = null) }
            runCatching { repository.getNotifications() }
                .onSuccess { snapshot ->
                    val notifications = snapshot.notifications
                    if (markSeen && notifications.isNotEmpty()) {
                        runCatching { repository.markSeen(notifications.map { it.id }) }
                        onSeen?.invoke()
                    }
                    state.update {
                        it.copy(
                            loading = false,
                            notifications = if (markSeen) {
                                notifications.map { notification -> notification.copy(seen = true) }
                            } else {
                                notifications
                            },
                            errorMessage = null,
                        )
                    }
                }
                .onFailure {
                    state.update {
                        it.copy(
                            loading = false,
                            errorMessage = "Impossible de charger les notifications.",
                        )
                    }
                }
        }
    }
}

data class NotificationsUiState(
    val loading: Boolean = false,
    val notifications: List<AppNotification> = emptyList(),
    val errorMessage: String? = null,
)
