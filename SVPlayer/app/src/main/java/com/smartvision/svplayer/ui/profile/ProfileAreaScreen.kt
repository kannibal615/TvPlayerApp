package com.smartvision.svplayer.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.config.CredentialsMode
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.ProfileType
import com.smartvision.svplayer.domain.access.PremiumFeatureGateResult
import com.smartvision.svplayer.domain.model.ParentalControlScope
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.domain.model.SyncStatus
import com.smartvision.svplayer.ui.components.NumericPinDialog
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.TvConfirmationDialog
import com.smartvision.svplayer.ui.components.TvSectionCard
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.home.TvHeader
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.settings.SynchronizationPreferencesContent
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ProfileAreaFocusDelay = 90L

@Composable
internal fun ProfileAreaScreen(
    strings: SmartVisionStrings,
    startDestination: ProfileAreaDestination,
    state: ProfileUiState,
    syncStatus: SyncStatus,
    settings: PlayerSettings,
    parentalControlScope: ParentalControlScope,
    parentalHiddenCount: Int?,
    parentalHiddenLoading: Boolean,
    parentalHiddenError: Boolean,
    syncDue: Boolean,
    parentalState: ParentalControlUiState,
    parentalViewModel: ParentalControlViewModel,
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    showLicenseKey: Boolean,
    hasNewNotifications: Boolean,
    notificationBadgeCount: Int,
    multiProfileAccess: PremiumFeatureGateResult,
    pinConfigured: Boolean,
    onVerifyPin: (String) -> Boolean,
    onSetParentalPin: (String) -> Unit,
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenManage: () -> Unit,
    onSettings: () -> Unit,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onNotifications: () -> Unit,
    onLicenseKey: () -> Unit,
    onLockedFeature: () -> Unit,
    onSaveProfile: (PlaylistProfile) -> String,
    onActivateProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onSynchronizeProfile: (String) -> Unit,
    onSetAutostartEnabled: (Boolean) -> Unit,
    onSetBackgroundSyncEnabled: (Boolean) -> Unit,
    onSetSyncFrequency: (String) -> Unit,
) {
    var selectedDestination by rememberSaveable(startDestination) { mutableStateOf(startDestination) }
    var showParentalPin by remember { mutableStateOf(false) }
    var showParentalPinCreation by remember { mutableStateOf(false) }
    val headerRequester = remember { FocusRequester() }
    val menuRequesters = remember { ProfileManagementPolicy.destinations.associateWith { FocusRequester() } }
    val infoChangeRequester = remember { FocusRequester() }
    val infoSyncRequester = remember { FocusRequester() }
    val manageFirstCardRequester = remember { FocusRequester() }
    val parentalFirstRequester = remember { FocusRequester() }
    val syncFirstRequester = remember { FocusRequester() }
    val activeProfile = state.playlistProfiles.firstOrNull { it.id == state.activePlaylistProfileId }
        ?: state.playlistProfiles.firstOrNull()

    BackHandler(onBack = onBack)

    LaunchedEffect(startDestination) {
        selectedDestination = startDestination
    }
    LaunchedEffect(selectedDestination) {
        parentalViewModel.setVisible(selectedDestination == ProfileAreaDestination.PARENTAL)
    }
    LaunchedEffect(Unit) {
        delay(ProfileAreaFocusDelay)
        menuRequesters[startDestination]?.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        SmartVisionColors.PrimaryDark.copy(alpha = 0.42f),
                        SmartVisionColors.Background,
                        Color(0xFF01040C),
                    ),
                    radius = 1550f,
                ),
            )
            .padding(
                horizontal = SmartVisionDimensions.AppScreenHorizontalPadding,
                vertical = SmartVisionDimensions.AppScreenVerticalPadding,
            ),
    ) {
        TvHeader(
            currentRoute = currentRoute,
            tabs = tabs,
            onNavigate = onNavigate,
            onSync = onSync,
            onSettings = onSettings,
            onProfile = {},
            onNotifications = onNotifications,
            onLicenseKey = onLicenseKey,
            showLicenseKey = showLicenseKey,
            hasNewNotifications = hasNewNotifications,
            notificationBadgeCount = notificationBadgeCount,
            homeTabFocusRequester = headerRequester,
            contentDownFocusRequester = menuRequesters[startDestination],
            onContentDown = { menuRequesters[startDestination]?.requestFocus() },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SmartVisionDimensions.AppHeaderContentSpacing))
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight()
                    .background(Color(0xD9091424), RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProfileManagementPolicy.destinations.forEachIndexed { index, destination ->
                    val selected = selectedDestination == destination
                    Box(Modifier.fillMaxWidth()) {
                        TvButton(
                            text = destination.label(strings),
                            leadingIcon = destination.icon(),
                            selected = selected,
                            variant = if (selected) TvButtonVariant.Primary else TvButtonVariant.Text,
                            onClick = {
                                when (destination) {
                                    ProfileAreaDestination.INFO -> {
                                        selectedDestination = destination
                                        onOpenInfo()
                                    }
                                    ProfileAreaDestination.MANAGE -> onOpenManage()
                                    ProfileAreaDestination.PARENTAL -> {
                                        if (pinConfigured) showParentalPin = true else showParentalPinCreation = true
                                    }
                                    else -> selectedDestination = destination
                                }
                            },
                            focusRequester = menuRequesters[destination],
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .focusProperties {
                                    if (index == 0) up = headerRequester
                                    right = when (destination) {
                                        ProfileAreaDestination.INFO -> infoChangeRequester
                                        ProfileAreaDestination.MANAGE -> manageFirstCardRequester
                                    ProfileAreaDestination.PARENTAL -> parentalFirstRequester
                                        ProfileAreaDestination.SYNCHRONIZATION -> syncFirstRequester
                                        else -> FocusRequester.Default
                                    }
                                },
                        )
                        if (destination == ProfileAreaDestination.PARENTAL) {
                            Box(
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
                                    .size(11.dp)
                                    .background(
                                        if (parentalControlScope.enabled) Color(0xFF20D878) else Color(0xFFFF4B4B),
                                        CircleShape,
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.72f), CircleShape),
                            )
                        }
                    }
                }
            }

            when (selectedDestination) {
                ProfileAreaDestination.INFO -> AreaPanel(
                    title = "",
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    ProfileInfoContent(
                        strings = strings,
                        state = state,
                        syncStatus = syncStatus,
                        parentalHiddenCount = parentalHiddenCount,
                        parentalHiddenLoading = parentalHiddenLoading,
                        parentalHiddenError = parentalHiddenError,
                        syncDue = syncDue,
                        menuRequester = menuRequesters.getValue(ProfileAreaDestination.INFO),
                        changeRequester = infoChangeRequester,
                        syncRequester = infoSyncRequester,
                        onActivateProfile = onActivateProfile,
                        onVerifyPin = onVerifyPin,
                        onOpenManage = onOpenManage,
                        onSynchronizeProfile = onSynchronizeProfile,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                ProfileAreaDestination.MANAGE -> AreaPanel(
                    title = "",
                    icon = Icons.Default.Groups,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    ManageProfilesContent(
                        strings = strings,
                        profiles = state.playlistProfiles,
                        activeProfileId = state.activePlaylistProfileId,
                        parentalControlScope = parentalControlScope,
                        multiProfileAccess = multiProfileAccess,
                        pinConfigured = pinConfigured,
                        menuRequester = menuRequesters.getValue(ProfileAreaDestination.MANAGE),
                        firstCardRequester = manageFirstCardRequester,
                        onVerifyPin = onVerifyPin,
                        onLockedFeature = onLockedFeature,
                        onSaveProfile = onSaveProfile,
                        onDeleteProfile = onDeleteProfile,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                ProfileAreaDestination.PARENTAL -> ParentalControlPanel(
                    strings = strings,
                    state = parentalState,
                    viewModel = parentalViewModel,
                    initialFocusRequester = parentalFirstRequester,
                    onExitToMenu = { menuRequesters[ProfileAreaDestination.PARENTAL]?.requestFocus() },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                ProfileAreaDestination.SYNCHRONIZATION -> AreaPanel(
                    title = strings.sync,
                    icon = Icons.Default.CloudSync,
                    modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                ) {
                    SynchronizationPreferencesContent(
                        settings = settings,
                        activeProfileName = activeProfile?.name ?: strings.none,
                        activeServer = activeProfile?.source?.displayName(strings) ?: strings.notConfigured,
                        lastSynchronization = activeProfile?.lastSyncAt?.asProfileAreaDate() ?: strings.syncNever,
                        strings = strings,
                        firstControlFocusRequester = syncFirstRequester,
                        menuFocusRequester = menuRequesters.getValue(ProfileAreaDestination.SYNCHRONIZATION),
                        onSetAutostartEnabled = onSetAutostartEnabled,
                        onSetBackgroundSyncEnabled = onSetBackgroundSyncEnabled,
                        onSetSyncFrequency = onSetSyncFrequency,
                    )
                }
                ProfileAreaDestination.HELP -> PlaceholderArea(
                    strings.help,
                    Icons.Default.HelpOutline,
                    Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }

    if (showParentalPin) {
        NumericPinDialog(
            title = strings.enterPin,
            strings = strings,
            onDismiss = { showParentalPin = false },
            onSubmit = { pin ->
                onVerifyPin(pin).also { valid ->
                    if (valid) {
                        showParentalPin = false
                        selectedDestination = ProfileAreaDestination.PARENTAL
                    }
                }
            },
        )
    }
    if (showParentalPinCreation) {
        NumericPinDialog(
            title = strings.createPin,
            strings = strings,
            requireConfirmation = true,
            onDismiss = { showParentalPinCreation = false },
            onSubmit = { pin ->
                onSetParentalPin(pin)
                showParentalPinCreation = false
                selectedDestination = ProfileAreaDestination.PARENTAL
                true
            },
        )
    }
}

@Composable
private fun ProfileInfoContent(
    strings: SmartVisionStrings,
    state: ProfileUiState,
    syncStatus: SyncStatus,
    parentalHiddenCount: Int?,
    parentalHiddenLoading: Boolean,
    parentalHiddenError: Boolean,
    syncDue: Boolean,
    menuRequester: FocusRequester,
    changeRequester: FocusRequester,
    syncRequester: FocusRequester,
    onActivateProfile: (String) -> Unit,
    onVerifyPin: (String) -> Boolean,
    onOpenManage: () -> Unit,
    onSynchronizeProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeProfile = state.playlistProfiles.firstOrNull { it.id == state.activePlaylistProfileId }
    var showPicker by remember { mutableStateOf(false) }
    var lockedProfile by remember { mutableStateOf<PlaylistProfile?>(null) }
    val syncing = syncStatus is SyncStatus.Running
    val scrollState = rememberScrollState()

    Column(modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Default.Person, null, tint = SmartVisionColors.CyanAccent, modifier = Modifier.size(23.dp))
            Text(strings.profileInfo, style = SmartVisionType.TitleS, color = SmartVisionColors.TextPrimary, fontWeight = FontWeight.SemiBold)
        }
        
        if (activeProfile == null) {
            TvSectionCard(strings.activeProfile, Icons.Default.Person, Modifier.fillMaxWidth()) {
                Text(strings.noProfilesAvailable, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Body)
                Spacer(Modifier.height(10.dp))
                TvButton(
                    text = strings.openManageProfiles,
                    onClick = onOpenManage,
                    focusRequester = changeRequester,
                    modifier = Modifier.focusProperties { left = menuRequester }.height(46.dp),
                )
            }
            return@Column
        }
        TvSectionCard(strings.activeProfile, Icons.Default.Person, Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlaylistProfileAvatar(activeProfile, Modifier.size(50.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Text(strings.activeProfile, color = SmartVisionColors.CyanAccent, style = SmartVisionType.Body)
                    Text(activeProfile.name, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(strings.active, Color(0xFF159B57))
                        Text(activeProfile.source.displayName(strings), color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
                    }
                }
                TvButton(
                    text = strings.changeProfile,
                    onClick = { showPicker = true },
                    focusRequester = changeRequester,
                    modifier = Modifier.width(160.dp).height(40.dp).focusProperties {
                        left = menuRequester
                        down = syncRequester
                    },
                )
            }
        }
        TvSectionCard(strings.catalog, Icons.Default.PlaylistPlay, Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CatalogMetric(strings.liveTv, state.account.liveCount, Icons.Default.LiveTv, SmartVisionColors.CyanAccent, Modifier.weight(1f))
                CatalogMetric(strings.movies, state.account.movieCount, Icons.Default.Movie, Color(0xFFFFB340), Modifier.weight(1f))
                CatalogMetric(strings.series, state.account.seriesCount, Icons.Default.Tv, Color(0xFFB46CFF), Modifier.weight(1f))
                val hiddenValue = when {
                    parentalHiddenLoading -> null
                    parentalHiddenError -> null
                    else -> ProfileManagementPolicy.hiddenItemCount(
                        state.account.kidsExcludedCount,
                        parentalHiddenCount ?: 0,
                    )
                }
                CatalogMetric(
                    strings.hiddenItems,
                    hiddenValue,
                    Icons.Default.VisibilityOff,
                    Color(0xFFFF668F),
                    Modifier.weight(1f),
                    unavailable = parentalHiddenError,
                )
            }
            if (!state.refreshing && state.account.liveCount + state.account.movieCount + state.account.seriesCount == 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (state.account.catalogSyncStatus == "error") strings.catalogUnavailable else strings.catalogEmpty,
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Caption,
                )
            }
        }
        TvSectionCard(strings.sync, Icons.Default.CloudSync, Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(strings.lastSynchronization, color = SmartVisionColors.CyanAccent, style = SmartVisionType.Caption)
                    Text(activeProfile.lastSyncAt?.asProfileAreaDate() ?: strings.syncNever, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Body)
                    val status = when {
                        syncing -> strings.synchronizationInProgress
                        syncStatus is SyncStatus.Error || state.account.catalogSyncStatus == "error" -> strings.synchronizationError
                        syncDue -> strings.synchronizationNeeded
                        else -> strings.synchronizationUpToDate
                    }
                    Text(status, color = if (syncDue) Color(0xFFFFB340) else Color(0xFF20D878), style = SmartVisionType.Caption)
                    if (syncStatus is SyncStatus.Running && syncStatus.message.isNotBlank()) {
                        Text(syncStatus.message, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
                    }
                }
                if (syncStatus is SyncStatus.Running) {
                    val progress = if (syncStatus.totalItems > 0) syncStatus.completedItems.toFloat() / syncStatus.totalItems else 0f
                    Column(Modifier.width(150.dp).padding(end = 14.dp)) {
                        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Text("${(progress * 100).toInt()}%", color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
                    }
                }
                TvButton(
                    text = if (syncing) strings.synchronizationInProgress else strings.synchronizeProfile,
                    onClick = { onSynchronizeProfile(activeProfile.id) },
                    enabled = !syncing && activeProfile.isConfigured,
                    focusRequester = syncRequester,
                    modifier = Modifier.width(160.dp).height(40.dp).focusProperties {
                        left = menuRequester
                        up = changeRequester
                    },
                )
            }
        }
    }

    if (showPicker) {
        ProfileActivationDialog(
            strings = strings,
            profiles = state.playlistProfiles,
            activeProfileId = state.activePlaylistProfileId,
            onDismiss = { showPicker = false },
            onActivate = { profile ->
                if (!profile.isConfigured) return@ProfileActivationDialog
                if (profile.isLocked) lockedProfile = profile else {
                    showPicker = false
                    onActivateProfile(profile.id)
                }
            },
        )
    }
    lockedProfile?.let { profile ->
        NumericPinDialog(
            title = strings.enterPin,
            strings = strings,
            onDismiss = { lockedProfile = null },
            onSubmit = { pin ->
                onVerifyPin(pin).also { valid ->
                    if (valid) {
                        lockedProfile = null
                        showPicker = false
                        onActivateProfile(profile.id)
                    }
                }
            },
        )
    }
}

@Composable
private fun ProfileActivationDialog(
    strings: SmartVisionStrings,
    profiles: List<PlaylistProfile>,
    activeProfileId: String,
    onDismiss: () -> Unit,
    onActivate: (PlaylistProfile) -> Unit,
) {
    val firstRequester = remember { FocusRequester() }
    LaunchedEffect(profiles) {
        if (profiles.isNotEmpty()) {
            delay(ProfileAreaFocusDelay)
            firstRequester.requestFocus()
        }
    }
    com.smartvision.svplayer.ui.components.TvDialogSurface(
        title = strings.changeProfile,
        onDismiss = onDismiss,
        width = 500.dp,
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(4.dp)) {
            items(profiles, key = { it.id }) { profile ->
                ProfileCard(
                    profile = profile,
                    active = profile.id == activeProfileId,
                    strings = strings,
                    onClick = { onActivate(profile) },
                    modifier = Modifier.width(130.dp).height(150.dp)
                        .then(if (profile.id == profiles.firstOrNull()?.id) Modifier.focusRequester(firstRequester) else Modifier),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        TvButton(strings.cancel, onDismiss, variant = TvButtonVariant.Secondary, modifier = Modifier.align(Alignment.End).height(35.dp))
    }
}

@Composable
private fun ManageProfilesContent(
    strings: SmartVisionStrings,
    profiles: List<PlaylistProfile>,
    activeProfileId: String,
    parentalControlScope: ParentalControlScope,
    multiProfileAccess: PremiumFeatureGateResult,
    pinConfigured: Boolean,
    menuRequester: FocusRequester,
    firstCardRequester: FocusRequester,
    onVerifyPin: (String) -> Boolean,
    onLockedFeature: () -> Unit,
    onSaveProfile: (PlaylistProfile) -> String,
    onDeleteProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val profileIds = profiles.map { it.id }
    val profileListState = rememberLazyListState()
    val cardRequesters = remember(profileIds) {
        profileIds.mapIndexed { index, id -> id to if (index == 0) firstCardRequester else FocusRequester() }.toMap()
    }
    var selectedProfileId by rememberSaveable { mutableStateOf<String?>(null) }
    var focusedProfileId by remember { mutableStateOf<String?>(null) }
    var editorProfile by remember { mutableStateOf<PlaylistProfile?>(null) }
    var editorCreateType by remember { mutableStateOf<ProfileType?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var lockProfile by remember { mutableStateOf<PlaylistProfile?>(null) }
    var deleteProfile by remember { mutableStateOf<PlaylistProfile?>(null) }
    var restoreProfileId by remember { mutableStateOf<String?>(null) }
    val editRequester = remember { FocusRequester() }
    val lockRequester = remember { FocusRequester() }
    val deleteRequester = remember { FocusRequester() }
    val addKidsRequester = remember { FocusRequester() }
    val addStandardRequester = remember { FocusRequester() }
    val detailsId = ProfileManagementPolicy.detailsProfileId(focusedProfileId, selectedProfileId, activeProfileId, profileIds)
    val detailsProfile = profiles.firstOrNull { it.id == detailsId }
    val adminProfile = profiles.firstOrNull { it.type == ProfileType.ADMIN }

    LaunchedEffect(profiles, restoreProfileId, showEditor, lockProfile, deleteProfile) {
        val target = restoreProfileId ?: return@LaunchedEffect
        if (showEditor || lockProfile != null || deleteProfile != null) return@LaunchedEffect
        delay(ProfileAreaFocusDelay)
        cardRequesters[target]?.requestFocus() ?: firstCardRequester.requestFocus()
        restoreProfileId = null
    }
    LaunchedEffect(profileIds) {
        if (profileIds.isNotEmpty()) profileListState.scrollToItem(0)
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Default.Groups, null, tint = SmartVisionColors.CyanAccent, modifier = Modifier.size(23.dp))
            Text(strings.manageProfiles, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.SemiBold)
        }
        TvSectionCard(strings.manageProfiles, Icons.Default.Groups, Modifier.fillMaxWidth()) {
            LazyRow(
                state = profileListState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                itemsIndexed(profiles, key = { _, profile -> "profile:${profile.id}" }) { index, profile ->
                    val requester = cardRequesters[profile.id] ?: firstCardRequester
                    ProfileCard(
                        profile = profile,
                        active = profile.id == activeProfileId,
                        strings = strings,
                        onClick = {
                            selectedProfileId = profile.id
                            scope.launch { delay(ProfileAreaFocusDelay); editRequester.requestFocus() }
                        },
                        modifier = Modifier
                            .width(120.dp).height(150.dp)
                            .focusRequester(requester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    focusedProfileId = profile.id
                                    scope.launch { profileListState.animateScrollToItem(index) }
                                }
                            }
                            .focusProperties {
                                if (profile.id == profiles.firstOrNull()?.id) left = menuRequester
                                down = editRequester
                            }
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                                    selectedProfileId = profile.id
                                    editRequester.requestFocus()
                                    true
                                } else false
                            },
                    )
                }
                item(key = "add:kids") {
                    AddProfileCard(
                        strings.addKidsProfile,
                        R.drawable.avatar_action_add_kids,
                        onClick = {
                            if (!multiProfileAccess.allowed) onLockedFeature() else {
                                editorProfile = null
                                editorCreateType = ProfileType.KIDS
                                showEditor = true
                            }
                        },
                        modifier = Modifier
                            .focusRequester(if (profiles.isEmpty()) firstCardRequester else addKidsRequester)
                            .focusProperties {
                                if (profiles.isEmpty()) left = menuRequester
                                right = addStandardRequester
                            },
                    )
                }
                item(key = "add:standard") {
                    AddProfileCard(
                        strings.addProfile,
                        R.drawable.avatar_action_add_profile,
                        onClick = {
                            if (!multiProfileAccess.allowed) onLockedFeature() else {
                                editorProfile = null
                                editorCreateType = ProfileType.NORMAL
                                showEditor = true
                            }
                        },
                        modifier = Modifier
                            .focusRequester(addStandardRequester)
                            .focusProperties { left = if (profiles.isEmpty()) firstCardRequester else addKidsRequester },
                    )
                }
            }
        }
        TvSectionCard(strings.profileInfo, Icons.Default.Person, Modifier.fillMaxWidth().weight(1f)) {
            if (detailsProfile == null) {
                Text(strings.noProfilesAvailable, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Body)
            } else Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PlaylistProfileAvatar(detailsProfile, Modifier.size(58.dp))
                        Text(detailsProfile.name, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.Bold)
                    }
                    DetailLine(strings.profileType, detailsProfile.type.displayName(strings))
                    DetailLine(strings.profileSource, detailsProfile.source.displayName(strings, detailsProfile.credentialsMode))
                    DetailLine(strings.parentalControl, if (parentalControlScope.isEnabledFor(detailsProfile.id)) strings.active else strings.inactive)
                    DetailLine(strings.catalog, if (detailsProfile.isConfigured) strings.configured else strings.notConfigured)
                }
                Column(Modifier.width(220.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvButton(
                        text = strings.edit,
                        onClick = {
                            if (!multiProfileAccess.allowed) onLockedFeature() else {
                                selectedProfileId = detailsProfile.id
                                restoreProfileId = detailsProfile.id
                                editorProfile = detailsProfile
                                editorCreateType = null
                                showEditor = true
                            }
                        },
                        leadingIcon = Icons.Default.Edit,
                        focusRequester = editRequester,
                        variant = TvButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth().height(45.dp).focusProperties {
                            up = cardRequesters[detailsProfile.id] ?: firstCardRequester
                            left = menuRequester
                            down = lockRequester
                        },
                    )
                    TvButton(
                        text = if (detailsProfile.isLocked) strings.unlockProfile else strings.lockProfile,
                        onClick = {
                            if (pinConfigured) {
                                selectedProfileId = detailsProfile.id
                                restoreProfileId = detailsProfile.id
                                lockProfile = detailsProfile
                            }
                        },
                        enabled = pinConfigured,
                        leadingIcon = Icons.Default.Lock,
                        focusRequester = lockRequester,
                        variant = TvButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth().height(45.dp).focusProperties {
                            up = editRequester
                            down = deleteRequester
                            left = menuRequester
                        },
                    )
                    TvButton(
                        text = strings.delete,
                        onClick = {
                            if (detailsProfile.type != ProfileType.ADMIN) {
                                selectedProfileId = detailsProfile.id
                                deleteProfile = detailsProfile
                            }
                        },
                        enabled = detailsProfile.type != ProfileType.ADMIN,
                        leadingIcon = Icons.Default.Delete,
                        focusRequester = deleteRequester,
                        variant = TvButtonVariant.Danger,
                        modifier = Modifier.fillMaxWidth().height(45.dp).focusProperties {
                            up = lockRequester
                            left = menuRequester
                        },
                    )
                    if (!pinConfigured) Text(strings.pinRequiredForLock, color = Color(0xFFFFB340), style = SmartVisionType.Caption)
                }
            }
        }
    }

    if (showEditor) {
        PlaylistProfileEditorDialog(
            initial = editorProfile,
            createType = editorCreateType,
            adminProfile = adminProfile,
            existingNames = profiles.filterNot { it.id == editorProfile?.id }.map { it.name },
            onDismiss = {
                showEditor = false
                editorProfile?.id?.let { restoreProfileId = it }
            },
            onSave = { profile ->
                val savedProfileId = onSaveProfile(profile)
                showEditor = false
                selectedProfileId = savedProfileId
                restoreProfileId = savedProfileId
            },
        )
    }
    lockProfile?.let { profile ->
        NumericPinDialog(
            title = strings.enterPin,
            strings = strings,
            onDismiss = { lockProfile = null },
            onSubmit = { pin ->
                onVerifyPin(pin).also { valid ->
                    if (valid) {
                        onSaveProfile(profile.copy(isLocked = !profile.isLocked))
                        lockProfile = null
                        restoreProfileId = profile.id
                    }
                }
            },
        )
    }
    deleteProfile?.let { profile ->
        TvConfirmationDialog(
            title = strings.profileDeleteTitle,
            itemLabel = profile.name,
            message = strings.profileDeleteMessage,
            confirmText = strings.deletePermanently,
            cancelText = strings.cancel,
            onDismiss = {
                deleteProfile = null
                restoreProfileId = profile.id
            },
            onConfirm = {
                val target = ProfileManagementPolicy.focusTargetAfterDeletion(profile.id, profileIds, profileIds - profile.id)
                onDeleteProfile(profile.id)
                selectedProfileId = target
                focusedProfileId = null
                restoreProfileId = target
                deleteProfile = null
            },
        )
    }
}

@Composable
private fun ProfileCard(
    profile: PlaylistProfile,
    active: Boolean,
    strings: SmartVisionStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .background(if (focused) Color(0xFF344050) else SmartVisionColors.Surface, RoundedCornerShape(10.dp))
            .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color.White else SmartVisionColors.Border), RoundedCornerShape(10.dp))
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key in setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)) {
                    onClick(); true
                } else false
            }
            .focusable()
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PlaylistProfileAvatar(profile, Modifier.size(66.dp))
            Text(profile.name, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Body, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (active) StatusBadge(strings.active, Color(0xFF159B57))
            else Text(profile.source.displayName(strings), color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
        }
    }
}

@Composable
private fun AddProfileCard(label: String, avatarDrawableRes: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.width(120.dp).height(150.dp)
            .background(if (focused) Color(0xFF344050) else SmartVisionColors.Surface, RoundedCornerShape(10.dp))
            .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color.White else SmartVisionColors.Border), RoundedCornerShape(10.dp))
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key in setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)) { onClick(); true } else false
            }
            .focusable().padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painter = painterResource(avatarDrawableRes), contentDescription = null, modifier = Modifier.size(66.dp))
            Text(label, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Body, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, maxLines = 2)
        }
    }
}

@Composable
private fun CatalogMetric(label: String, value: Int?, icon: ImageVector, accent: Color, modifier: Modifier, unavailable: Boolean = false) {
    Column(
        modifier.background(SmartVisionColors.Surface, RoundedCornerShape(8.dp))
            .border(1.dp, SmartVisionColors.Border, RoundedCornerShape(8.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            Text(label, color = accent, style = SmartVisionType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (value == null && !unavailable) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = accent)
        else Text(if (unavailable) "--" else "%,d".format(value ?: 0), color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.Bold, fontSize = 18.sp,)
    }
}

@Composable
private fun AreaPanel(title: String, icon: ImageVector, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.background(Color(0xD9091424), RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp)).padding(10.dp),
    ) {
        if (title.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(icon, null, tint = SmartVisionColors.CyanAccent, modifier = Modifier.size(23.dp))
                Text(title, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
        }
        content()
    }
}

@Composable
private fun PlaceholderArea(title: String, icon: ImageVector, modifier: Modifier = Modifier) {
    AreaPanel(title, icon, modifier) {
        Text(title, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Body)
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Text(text, color = Color.White, style = SmartVisionType.Caption, modifier = Modifier.background(color, RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 3.dp))
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, modifier = Modifier.width(108.dp))
        Text(value, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun ProfileAreaDestination.label(strings: SmartVisionStrings): String = when (this) {
    ProfileAreaDestination.INFO -> strings.profileInfo
    ProfileAreaDestination.MANAGE -> strings.manageProfiles
    ProfileAreaDestination.PARENTAL -> strings.parentalControl
    ProfileAreaDestination.SYNCHRONIZATION -> strings.sync
    ProfileAreaDestination.HELP -> strings.help
}

private fun ProfileAreaDestination.icon(): ImageVector = when (this) {
    ProfileAreaDestination.INFO -> Icons.Default.Person
    ProfileAreaDestination.MANAGE -> Icons.Default.Groups
    ProfileAreaDestination.PARENTAL -> Icons.Default.Security
    ProfileAreaDestination.SYNCHRONIZATION -> Icons.Default.CloudSync
    ProfileAreaDestination.HELP -> Icons.Default.HelpOutline
}

private fun PlaylistSource.displayName(strings: SmartVisionStrings, mode: CredentialsMode = CredentialsMode.CUSTOM): String =
    if (mode == CredentialsMode.SHARED_WITH_ADMIN) strings.smartVisionAccount else when (this) {
        PlaylistSource.Xtream -> "Xtream Codes"
        PlaylistSource.M3u -> "M3U"
    }

private fun ProfileType.displayName(strings: SmartVisionStrings): String = when (this) {
    ProfileType.ADMIN -> strings.administrator
    ProfileType.NORMAL -> strings.standardProfile
    ProfileType.KIDS -> strings.kidsProfile
}

private fun Long.asProfileAreaDate(): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(this))
