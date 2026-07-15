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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import com.smartvision.svplayer.data.notifications.NotificationDetails
import com.smartvision.svplayer.data.notifications.NotificationType
import com.smartvision.svplayer.data.notifications.NotificationsRepository
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.TvDialogSurface
import com.smartvision.svplayer.ui.components.TvDialogTone
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.home.TvHeader
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.i18n.smartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun NotificationsRoute(
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onLicenseKey: () -> Unit,
    showLicenseKey: Boolean,
    hasNewNotifications: Boolean,
    notificationBadgeCount: Int,
    onBack: () -> Unit,
    onNotificationsChanged: () -> Unit,
    onOpenUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = com.smartvision.svplayer.domain.model.PlayerSettings(),
    )
    val strings = smartVisionStrings(settings.language)
    val viewModel: NotificationsViewModel = viewModel(
        factory = viewModelFactory { NotificationsViewModel(container.notificationsRepository) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    NotificationsScreen(
        state = state,
        currentRoute = currentRoute,
        tabs = tabs,
        onNavigate = onNavigate,
        onSync = onSync,
        onSettings = onSettings,
        onProfile = onProfile,
        onLicenseKey = onLicenseKey,
        showLicenseKey = showLicenseKey,
        hasNewNotifications = hasNewNotifications,
        notificationBadgeCount = notificationBadgeCount,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onOpenNotification = { notification ->
            if (!notification.seen) {
                viewModel.markSeen(notification.id, onNotificationsChanged)
            }
            if (notification.type == NotificationType.AppUpdate) onOpenUpdate()
        },
        strings = strings,
        modifier = modifier,
    )
}

@Composable
private fun NotificationsScreen(
    state: NotificationsUiState,
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onLicenseKey: () -> Unit,
    showLicenseKey: Boolean,
    hasNewNotifications: Boolean,
    notificationBadgeCount: Int,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenNotification: (AppNotification) -> Unit,
    strings: SmartVisionStrings,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    var selectedSection by remember { mutableStateOf(NotificationSection.Updates) }
    var dialogNotification by remember { mutableStateOf<AppNotification?>(null) }
    val headerFocusRequester = remember { FocusRequester() }
    val refreshFocusRequester = remember { FocusRequester() }
    val firstCardFocusRequester = remember { FocusRequester() }
    val sectionRequesters = remember { NotificationSection.entries.associateWith { FocusRequester() } }
    val notificationRequesters = remember { mutableMapOf<Long, FocusRequester>() }
    var openedNotificationId by remember { mutableStateOf<Long?>(null) }
    var preferredFocusId by remember { mutableStateOf<Long?>(null) }
    val visibleNotifications = remember(state.notifications, selectedSection) {
        filterNotifications(state.notifications, selectedSection)
    }
    val counts = remember(state.notifications) { notificationCounts(state.notifications) }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        delay(90)
        runCatching { sectionRequesters.getValue(NotificationSection.Updates).requestFocus() }
    }

    LaunchedEffect(visibleNotifications.map { it.id }, dialogNotification) {
        val openedId = openedNotificationId ?: return@LaunchedEffect
        if (dialogNotification == null && visibleNotifications.none { it.id == openedId }) {
            delay(80)
            val targetId = preferredFocusId?.takeIf { candidate -> visibleNotifications.any { it.id == candidate } }
            if (targetId != null) {
                runCatching { notificationRequesters[targetId]?.requestFocus() }
            } else {
                runCatching { sectionRequesters.getValue(selectedSection).requestFocus() }
            }
            openedNotificationId = null
            preferredFocusId = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(SmartVisionColors.PrimaryDark.copy(alpha = 0.42f), SmartVisionColors.Background, Color(0xFF01040C)),
                    radius = 1550f,
                ),
            )
            .padding(horizontal = 34.dp, vertical = 18.dp),
    ) {
        TvHeader(
            currentRoute = currentRoute,
            tabs = tabs,
            onNavigate = onNavigate,
            onSync = onSync,
            onSettings = onSettings,
            onProfile = onProfile,
            onNotifications = {},
            onLicenseKey = onLicenseKey,
            showLicenseKey = showLicenseKey,
            hasNewNotifications = hasNewNotifications,
            notificationBadgeCount = notificationBadgeCount,
            notificationsFocusRequester = headerFocusRequester,
            contentDownFocusRequester = sectionRequesters.getValue(selectedSection),
            onContentDown = { runCatching { sectionRequesters.getValue(selectedSection).requestFocus() } },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LazyColumn(
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight()
                    .background(Color(0xD9091424), RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(NotificationSection.entries, key = { _, section -> section.name }) { index, section ->
                    NotificationMenuRow(
                        section = section,
                        label = section.label(strings),
                        count = counts[section] ?: 0,
                        selected = selectedSection == section,
                        focusRequester = sectionRequesters.getValue(section),
                        onClick = { selectedSection = section },
                        onMoveRight = {
                            if (visibleNotifications.isNotEmpty()) {
                                runCatching { firstCardFocusRequester.requestFocus() }
                            }
                        },
                        onMoveUpFromFirst = if (index == 0) ({ runCatching { headerFocusRequester.requestFocus() } }) else null,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xE60A1424), RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
                    .padding(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = SmartVisionColors.CyanAccent, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(9.dp))
                    Text(strings.notifications, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    TvButton(
                        text = if (state.loading) strings.refreshing else strings.refresh,
                        leadingIcon = Icons.Default.Refresh,
                        onClick = onRefresh,
                        enabled = !state.loading,
                        focusRequester = refreshFocusRequester,
                        variant = TvButtonVariant.Primary,
                        modifier = Modifier.height(38.dp).focusProperties {
                            if (visibleNotifications.isNotEmpty()) down = firstCardFocusRequester
                            left = sectionRequesters.getValue(selectedSection)
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
                when {
                    state.loading && state.notifications.isEmpty() -> CircularProgressIndicator(
                        color = SmartVisionColors.CyanAccent,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 120.dp),
                    )
                    state.errorMessage != null && state.notifications.isEmpty() -> NotificationEmptyState(
                        message = state.errorMessage,
                    )
                    visibleNotifications.isEmpty() -> NotificationEmptyState(
                        message = strings.noNotifications,
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        itemsIndexed(visibleNotifications, key = { _, item -> item.id }) { index, notification ->
                            val cardFocusRequester = if (index == 0) {
                                firstCardFocusRequester
                            } else {
                                notificationRequesters.getOrPut(notification.id) { FocusRequester() }
                            }
                            notificationRequesters[notification.id] = cardFocusRequester
                            NotificationCard(
                                notification = notification,
                                focusRequester = cardFocusRequester,
                                leftFocusRequester = sectionRequesters.getValue(selectedSection),
                                upFocusRequester = if (index == 0) refreshFocusRequester else null,
                                onClick = {
                                    openedNotificationId = notification.id
                                    preferredFocusId = nextNotificationFocusId(visibleNotifications, index)
                                    onOpenNotification(notification)
                                    if (notification.type != NotificationType.AppUpdate) dialogNotification = notification
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    dialogNotification?.let { notification ->
        NotificationDetailsDialog(notification, strings) { dialogNotification = null }
    }
}

@Composable
private fun NotificationMenuRow(
    section: NotificationSection,
    label: String,
    count: Int,
    selected: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onMoveRight: () -> Unit,
    onMoveUpFromFirst: (() -> Unit)?,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        TvButton(
            text = label,
            leadingIcon = section.icon,
            selected = selected,
            variant = if (selected) TvButtonVariant.Primary else TvButtonVariant.Text,
            onClick = onClick,
            focusRequester = focusRequester,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionRight -> { onMoveRight(); true }
                        Key.DirectionUp -> if (onMoveUpFromFirst != null) { onMoveUpFromFirst(); true } else false
                        else -> false
                    }
                },
        )
        if (count > 0 && section != NotificationSection.History) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp).size(24.dp)
                    .background(Color(0xFFE53935), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Text(count.coerceAtMost(99).toString(), color = Color.White, style = SmartVisionType.Caption, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    focusRequester: FocusRequester?,
    leftFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusProperties {
                left = leftFocusRequester
                if (upFocusRequester != null) up = upFocusRequester
            }
            .tvFocusTarget(state = focusState, pressed = pressed, glowColor = focusStyle.accent, cornerRadius = 8.dp)
            .background(if (focusState.isFocused) focusStyle.background else Color(0xFF081426), shape)
            .border(
                BorderStroke(if (focusState.isFocused) focusStyle.borderWidth else 1.dp, if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border),
                shape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(notification.type.icon(), contentDescription = null, tint = if (focusState.isFocused) focusStyle.accent else SmartVisionColors.TextSecondary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(notification.title, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(notification.message, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        Text(notification.createdAt, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 2)
    }
}

@Composable
private fun NotificationEmptyState(
    message: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 110.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Notifications, contentDescription = null, tint = SmartVisionColors.CyanAccent, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(10.dp))
        Text(message.orEmpty(), color = SmartVisionColors.TextSecondary, style = SmartVisionType.Body)
    }
}

@Composable
private fun NotificationDetailsDialog(notification: AppNotification, strings: SmartVisionStrings, onDismiss: () -> Unit) {
    val closeFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(90); runCatching { closeFocusRequester.requestFocus() } }
    TvDialogSurface(
        title = notification.title,
        onDismiss = onDismiss,
        width = 760.dp,
        tone = if (notification.priority == "urgent") TvDialogTone.Warning else TvDialogTone.Default,
        icon = notification.type.icon(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(330.dp).verticalScroll(rememberScrollState())) {
            Text(notification.message, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Body)
            Spacer(Modifier.height(14.dp))
            DetailRow(strings.notificationDate, notification.createdAt)
            DetailRow(strings.notificationPriority, notification.priority)
            if (notification.type == NotificationType.PlaylistAdded) {
                PlaylistDetails(notification.details, strings)
            }
        }
        Spacer(Modifier.height(18.dp))
        TvButton(
            text = strings.close,
            onClick = onDismiss,
            focusRequester = closeFocusRequester,
            variant = TvButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth().height(44.dp),
        )
    }
}

@Composable
private fun PlaylistDetails(details: NotificationDetails?, strings: SmartVisionStrings) {
    Spacer(Modifier.height(12.dp))
    Text(strings.playlistReceivedDetails, color = SmartVisionColors.CyanAccent, style = SmartVisionType.Label, fontWeight = FontWeight.Bold)
    if (details == null) {
        Spacer(Modifier.height(8.dp))
        Text(strings.playlistDetailsUnavailable, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
        return
    }
    DetailRow(strings.host, details.xtreamHost)
    DetailRow(strings.user, details.xtreamUsername)
    DetailRow(strings.password, details.xtreamPassword)
    DetailRow("M3U", details.m3uUrl)
    DetailRow("EPG", details.epgUrl)
    DetailRow(strings.notificationSentAt, details.submittedAt)
    DetailRow(strings.notificationIpAddress, details.ipAddress)
    DetailRow(strings.notificationCountry, details.countryCode)
    DetailRow(strings.notificationSenderDevice, details.senderDevice)
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, modifier = Modifier.width(170.dp))
        Text(value, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Caption, modifier = Modifier.weight(1f))
    }
}

internal enum class NotificationSection(val icon: ImageVector) {
    Updates(Icons.Default.SystemUpdateAlt),
    Playlists(Icons.Default.PlaylistAdd),
    Important(Icons.Default.WarningAmber),
    History(Icons.Default.History);

    fun label(strings: SmartVisionStrings): String = when (this) {
        Updates -> strings.applicationUpdates
        Playlists -> strings.playlistAddedNotifications
        Important -> strings.importantInformation
        History -> strings.notificationHistory
    }
}

internal fun filterNotifications(items: List<AppNotification>, section: NotificationSection): List<AppNotification> =
    items.filter { notification ->
        when (section) {
            NotificationSection.Updates -> !notification.seen && notification.type == NotificationType.AppUpdate
            NotificationSection.Playlists -> !notification.seen && notification.type == NotificationType.PlaylistAdded
            NotificationSection.Important -> !notification.seen && notification.type == NotificationType.ImportantInfo
            NotificationSection.History -> notification.seen
        }
    }

internal fun notificationCounts(items: List<AppNotification>): Map<NotificationSection, Int> = mapOf(
    NotificationSection.Updates to items.count { !it.seen && it.type == NotificationType.AppUpdate },
    NotificationSection.Playlists to items.count { !it.seen && it.type == NotificationType.PlaylistAdded },
    NotificationSection.Important to items.count { !it.seen && it.type == NotificationType.ImportantInfo },
    NotificationSection.History to items.count { it.seen },
)

internal fun nextNotificationFocusId(items: List<AppNotification>, openedIndex: Int): Long? =
    items.getOrNull(openedIndex + 1)?.id ?: items.getOrNull(openedIndex - 1)?.id

private fun NotificationType.icon(): ImageVector = when (this) {
    NotificationType.AppUpdate -> Icons.Default.CloudDownload
    NotificationType.PlaylistAdded -> Icons.Default.PlaylistAdd
    NotificationType.ImportantInfo -> Icons.Default.Info
}

class NotificationsViewModel(private val repository: NotificationsRepository) : ViewModel() {
    private val state = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = state

    fun refresh() {
        viewModelScope.launch {
            state.update { it.copy(loading = true, errorMessage = null) }
            runCatching { repository.getNotifications() }
                .onSuccess { snapshot -> state.update { it.copy(loading = false, notifications = snapshot.notifications, errorMessage = null) } }
                .onFailure { state.update { it.copy(loading = false, errorMessage = "Impossible de charger les notifications.") } }
        }
    }

    fun markSeen(notificationId: Long, onChanged: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.markSeen(listOf(notificationId)) }
                .onSuccess {
                    state.update { current ->
                        current.copy(notifications = current.notifications.map { if (it.id == notificationId) it.copy(seen = true) else it })
                    }
                    onChanged()
                }
                .onFailure { state.update { it.copy(errorMessage = "Impossible de marquer la notification comme lue.") } }
        }
    }
}

data class NotificationsUiState(
    val loading: Boolean = false,
    val notifications: List<AppNotification> = emptyList(),
    val errorMessage: String? = null,
)
