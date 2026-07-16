package com.smartvision.svplayer.ui.profile

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.CredentialsMode
import com.smartvision.svplayer.core.config.ProfileType
import com.smartvision.svplayer.core.config.PlaylistProfileStatus
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.AdminProfileAvatarId
import com.smartvision.svplayer.core.config.ProfileAvatarPresetIds
import com.smartvision.svplayer.core.config.KidsProfileAvatarPresetIds
import com.smartvision.svplayer.core.config.XtreamAccount
import com.smartvision.svplayer.core.config.canonicalProfileAvatarId
import com.smartvision.svplayer.core.config.defaultProfileAvatarId
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.data.xtream.XtreamCredentialsValidationResult
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.activation.ActivationException
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.domain.model.AccountProfile
import com.smartvision.svplayer.domain.model.ParentalControlScope
import com.smartvision.svplayer.domain.model.SyncStatus
import com.smartvision.svplayer.domain.access.PremiumFeatureGateResult
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.NumericPinDialog
import com.smartvision.svplayer.ui.components.TvConfirmationDialog
import com.smartvision.svplayer.ui.components.TvDialogSurface
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.home.HomeVisualBackground
import com.smartvision.svplayer.data.mock.HomeVisualStyle
import com.smartvision.svplayer.ui.home.TvHeader
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.settings.SynchronizationPreferencesContent
import com.smartvision.svplayer.startup.BackgroundSyncScheduler
import com.smartvision.svplayer.sync.SyncFrequencyPolicy
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileRoute(
    strings: SmartVisionStrings,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onNotifications: () -> Unit,
    onLicenseKey: () -> Unit,
    showLicenseKey: Boolean,
    hasNewNotifications: Boolean,
    notificationBadgeCount: Int,
    multiProfileAccess: PremiumFeatureGateResult,
    onLockedFeature: () -> Unit,
    onSyncCatalog: suspend () -> Result<Unit>,
    onActivationChanged: () -> Unit,
    startDestination: ProfileAreaDestination = ProfileAreaDestination.INFO,
    onOpenInfo: () -> Unit,
    onOpenManage: () -> Unit,
    onActivateProfileForSession: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val viewModel: ProfileViewModel = viewModel(
        factory = viewModelFactory {
            ProfileViewModel(
                activationRepository = container.activationRepository,
                accountManager = container.accountManager,
                catalogRepository = container.catalogRepository,
            )
        },
    )
    val parentalViewModel: ParentalControlViewModel = viewModel(
        factory = viewModelFactory {
            ParentalControlViewModel(
                settingsRepository = container.settingsRepository,
                parentalCatalogRepository = container.parentalCatalogRepository,
                activeProfileId = container.accountManager.activeProfileId,
                activeProfileIdProvider = container.accountManager::activeProfileIdOrDefault,
                profiles = container.accountManager.profiles,
                catalogRevision = container.catalogRepository.catalogRevision,
            )
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val parentalState by parentalViewModel.uiState.collectAsStateWithLifecycle()
    val syncStatus by container.catalogRepository.syncStatus.collectAsStateWithLifecycle()
    val catalogRevision by container.catalogRepository.catalogRevision.collectAsStateWithLifecycle()
    val profileSettings by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = com.smartvision.svplayer.domain.model.PlayerSettings(),
    )
    val parentalControlScope by container.settingsRepository.parentalControlScope.collectAsStateWithLifecycle(
        initialValue = ParentalControlScope(),
    )
    val privacyOptionsRequired by container.privacyConsentManager.privacyOptionsRequired
        .collectAsStateWithLifecycle()
    var parentalHiddenCount by remember { mutableStateOf<Int?>(null) }
    var parentalHiddenLoading by remember { mutableStateOf(false) }
    var parentalHiddenError by remember { mutableStateOf(false) }
    var synchronizationDue by remember { mutableStateOf(true) }

    LaunchedEffect(
        state.activePlaylistProfileId,
        catalogRevision,
        profileSettings.parentalKeywordValues,
        parentalControlScope,
        state.account.catalogSyncStatus,
    ) {
        val profileId = state.activePlaylistProfileId
        if (profileId.isBlank()) {
            parentalHiddenCount = 0
            parentalHiddenLoading = false
            parentalHiddenError = false
            synchronizationDue = true
            return@LaunchedEffect
        }
        val hiddenItemsCacheKey = ProfileHiddenItemsCacheKey(
            profileId = profileId,
            catalogRevision = catalogRevision,
            keywords = profileSettings.parentalKeywordValues,
            enabled = parentalControlScope.enabled,
            disabledProfileIds = parentalControlScope.disabledProfileIds,
        )
        val cachedHiddenCount = ProfileHiddenItemsCache.get(hiddenItemsCacheKey)
        parentalHiddenCount = cachedHiddenCount
        parentalHiddenLoading = parentalControlScope.isEnabledFor(profileId) && cachedHiddenCount == null
        parentalHiddenError = false
        parentalHiddenCount = if (parentalControlScope.isEnabledFor(profileId)) {
            runCatching {
                container.parentalCatalogRepository.counts(profileId, profileSettings.parentalKeywordValues).items
            }.onFailure { parentalHiddenError = true }.getOrNull()?.also { count ->
                ProfileHiddenItemsCache.put(hiddenItemsCacheKey, count)
            } ?: cachedHiddenCount
        } else {
            0.also { ProfileHiddenItemsCache.put(hiddenItemsCacheKey, it) }
        }
        parentalHiddenLoading = false
        val hasLocalCatalog = container.catalogRepository.hasLocalCatalogForActiveProfile()
        val activeProfile = state.playlistProfiles.firstOrNull { it.id == profileId }
        synchronizationDue = SyncFrequencyPolicy.isSynchronizationDue(
            value = profileSettings.syncFrequency,
            lastSyncAt = activeProfile?.lastSyncAt,
            hasLocalCatalog = hasLocalCatalog,
            allowRunOnStartup = false,
        )
    }

    LaunchedEffect(state.usageMode) {
        if (state.usageMode == UsageMode.Premium) {
            onActivationChanged()
        }
    }

    suspend fun synchronizeActiveProfileCatalog(): Result<Unit> = runCatching {
        container.xtreamRepository.clearCaches()
        container.catalogRepository.invalidateLocalCatalogCache()
        container.synchronizeCatalog().getOrThrow()
        container.accountManager.epgUrl.value.takeIf { it.isNotBlank() }?.let { epgUrl ->
            container.epgRepository.synchronize(epgUrl)
        }
    }

    ProfileAreaScreen(
        strings = strings,
        startDestination = startDestination,
        state = state,
        syncStatus = syncStatus,
        settings = profileSettings,
        parentalControlScope = parentalControlScope,
        parentalHiddenCount = parentalHiddenCount,
        parentalHiddenLoading = parentalHiddenLoading,
        parentalHiddenError = parentalHiddenError,
        syncDue = synchronizationDue,
        parentalState = parentalState,
        parentalViewModel = parentalViewModel,
        onBack = onBack,
        onOpenInfo = onOpenInfo,
        onOpenManage = onOpenManage,
        onSettings = onSettings,
        currentRoute = currentRoute,
        tabs = tabs,
        onNavigate = onNavigate,
        onSync = onSync,
        onNotifications = onNotifications,
        onLicenseKey = onLicenseKey,
        showLicenseKey = showLicenseKey,
        hasNewNotifications = hasNewNotifications,
        notificationBadgeCount = notificationBadgeCount,
        multiProfileAccess = multiProfileAccess,
        onLockedFeature = onLockedFeature,
        pinConfigured = container.profilePinManager.hasPin(),
        onVerifyPin = container.profilePinManager::verifyPin,
        onSetParentalPin = { pin -> scope.launch { container.settingsRepository.setParentalPin(pin) } },
        onSaveProfile = { profile ->
            val wasFirstProfile = container.accountManager.profiles.value.isEmpty()
            val wasActiveProfile = profile.id.isNotBlank() && profile.id == container.accountManager.activeProfileId.value
            val savedProfileId = container.accountManager.upsertProfile(profile)
            if (wasFirstProfile || wasActiveProfile) {
                container.xtreamRepository.clearCaches()
                scope.launch {
                    container.catalogRepository.clearCatalogForProfileSwitch()
                    if (!container.catalogRepository.hasLocalCatalogForActiveProfile()) {
                        synchronizeActiveProfileCatalog()
                    }
                }
            }
            savedProfileId
        },
        onActivateProfile = { profileId ->
            onActivateProfileForSession(profileId)
        },
        onDeleteProfile = { profileId ->
            val profileToDelete = container.accountManager.profiles.value.firstOrNull { it.id == profileId }
            val wasActiveProfile = profileId == container.accountManager.activeProfileId.value
            scope.launch {
                profileToDelete?.let { deletedProfile ->
                    runCatching { container.activationRepository.clearPlaylistConfig(deletedProfile) }
                }
                container.catalogRepository.deleteProfileData(profileId)
                container.parentalCatalogRepository.deleteProfileSnapshot(profileId)
                container.accountManager.deleteProfile(profileId)
                container.xtreamRepository.clearCaches()
                container.catalogRepository.clearCatalogForProfileSwitch()
                if (wasActiveProfile && container.accountManager.activeProfileId.value != null &&
                    !container.catalogRepository.hasLocalCatalogForActiveProfile()
                ) {
                    synchronizeActiveProfileCatalog()
                }
            }
        },
        onSynchronizeProfile = { profileId ->
            scope.launch {
                val target = container.accountManager.profiles.value.firstOrNull { it.id == profileId }
                    ?: return@launch
                container.catalogRepository.synchronize(profileId).onSuccess {
                    val resolved = container.accountManager.resolvedProfile(target)
                    resolved.epgUrl.takeIf { it.isNotBlank() }?.let { epgUrl ->
                        container.epgRepository.synchronize(epgUrl)
                    }
                }
            }
        },
        onSetAutostartEnabled = { value -> scope.launch { container.settingsRepository.setAutostartEnabled(value) } },
        onSetBackgroundSyncEnabled = { value ->
            scope.launch {
                container.settingsRepository.setBackgroundSyncEnabled(value)
                BackgroundSyncScheduler.applyPeriodicSync(context, value)
            }
        },
        onSetSyncFrequency = { value -> scope.launch { container.settingsRepository.setSyncFrequency(value) } },
    )
}

private data class ProfileHiddenItemsCacheKey(
    val profileId: String,
    val catalogRevision: Long,
    val keywords: List<String>,
    val enabled: Boolean,
    val disabledProfileIds: Set<String>,
)

private object ProfileHiddenItemsCache {
    private val values = LinkedHashMap<ProfileHiddenItemsCacheKey, Int>()

    @Synchronized
    fun get(key: ProfileHiddenItemsCacheKey): Int? = values[key]

    @Synchronized
    fun put(key: ProfileHiddenItemsCacheKey, value: Int) {
        values[key] = value
        while (values.size > 16) values.remove(values.keys.first())
    }
}

@Composable
private fun ProfileScreen(
    strings: SmartVisionStrings,
    state: ProfileUiState,
    syncStatus: SyncStatus,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onNotifications: () -> Unit,
    onLicenseKey: () -> Unit,
    showLicenseKey: Boolean,
    hasNewNotifications: Boolean,
    notificationBadgeCount: Int,
    multiProfileAccess: PremiumFeatureGateResult,
    onLockedFeature: () -> Unit,
    onRefresh: () -> Unit,
    onSyncCatalog: suspend () -> Result<Unit>,
    onShowLicenseQr: () -> Unit,
    onLicenseCodeChange: (String) -> Unit,
    onActivateLicense: () -> Unit,
    onShowPrivacyOptions: () -> Unit,
    privacyOptionsRequired: Boolean,
    onShowXtreamSetupQr: () -> Unit,
    onSaveXtreamAccount: (XtreamAccount) -> Unit,
    onSaveEpgUrl: (String) -> Unit,
    onSaveM3uUrl: (String) -> Unit,
    onSelectPlaylistSource: (PlaylistSource) -> Unit,
    onSavePlaylistProfile: (PlaylistProfile) -> Unit,
    onSelectPlaylistProfile: (String) -> Unit,
    onDeletePlaylistProfile: (String) -> Unit,
    onSynchronizePlaylistProfile: (String) -> Unit,
    onDeleteXtreamAccount: (String) -> Unit,
    onDismissQr: () -> Unit,
    parentalState: ParentalControlUiState,
    parentalViewModel: ParentalControlViewModel,
) {
    BackHandler(onBack = onBack)
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val parentalSettings by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = com.smartvision.svplayer.domain.model.PlayerSettings(),
    )
    val parentalControlScope by container.settingsRepository.parentalControlScope.collectAsStateWithLifecycle(
        initialValue = ParentalControlScope(),
    )
    var selectedSection by remember { mutableStateOf(ProfileSection.Xtream) }
    var showXtreamSyncDialog by remember { mutableStateOf(false) }
    var showParentalUnlockDialog by remember { mutableStateOf(false) }
    var showParentalCreateDialog by remember { mutableStateOf(false) }
    var pendingFocusSection by remember { mutableStateOf<ProfileSection?>(null) }
    val homeTabFocusRequester = remember { FocusRequester() }
    val xtreamSectionFocusRequester = remember { FocusRequester() }
    val parentalSectionFocusRequester = remember { FocusRequester() }
    val syncSectionFocusRequester = remember { FocusRequester() }
    val syncFirstControlFocusRequester = remember { FocusRequester() }
    val deviceCatalogFocusRequester = remember { FocusRequester() }
    val activePlaylistProfile = state.playlistProfiles.firstOrNull { it.id == state.activePlaylistProfileId }
        ?: state.playlistProfiles.firstOrNull()
    val resolvedActivePlaylistProfile = activePlaylistProfile?.let(container.accountManager::resolvedProfile)

    LaunchedEffect(selectedSection) {
        parentalViewModel.setVisible(selectedSection == ProfileSection.Parental)
    }

    LaunchedEffect(Unit) {
        delay(ProfileFocusRequestDelayMillis)
        runCatching { xtreamSectionFocusRequester.requestFocus() }
    }

    LaunchedEffect(pendingFocusSection, showXtreamSyncDialog) {
        if (!showXtreamSyncDialog) {
            delay(ProfileFocusRequestDelayMillis)
            when (pendingFocusSection) {
                ProfileSection.Xtream -> runCatching { deviceCatalogFocusRequester.requestFocus() }
                else -> Unit
            }
            pendingFocusSection = null
        }
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
            .padding(horizontal = 34.dp, vertical = 18.dp),
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
            homeTabFocusRequester = homeTabFocusRequester,
            contentDownFocusRequester = xtreamSectionFocusRequester,
            onContentDown = { runCatching { xtreamSectionFocusRequester.requestFocus() } },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
                ProfileSection.entries.forEach { section ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        TvButton(
                            text = section.label,
                            leadingIcon = section.icon,
                            selected = selectedSection == section,
                            variant = if (selectedSection == section) TvButtonVariant.Primary else TvButtonVariant.Text,
                            onClick = {
                                if (section == ProfileSection.Parental) {
                                    if (parentalSettings.parentalPin.isBlank()) {
                                        showParentalCreateDialog = true
                                    } else {
                                        showParentalUnlockDialog = true
                                    }
                                } else {
                                    selectedSection = section
                                }
                            },
                            focusRequester = when (section) {
                                ProfileSection.Xtream -> xtreamSectionFocusRequester
                                ProfileSection.Parental -> parentalSectionFocusRequester
                                ProfileSection.Sync -> syncSectionFocusRequester
                                else -> null
                            },
                            modifier = Modifier
                                .then(
                                    if (section == ProfileSection.Xtream) {
                                        Modifier.onPreviewKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                                                runCatching { homeTabFocusRequester.requestFocus() }
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                    } else {
                                        Modifier
                                    },
                                )
                                .then(if (section == ProfileSection.Xtream) Modifier.focusProperties { up = homeTabFocusRequester } else Modifier)
                                .then(
                                    if (section == ProfileSection.Sync) {
                                        Modifier.onPreviewKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                                                runCatching { syncFirstControlFocusRequester.requestFocus() }
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                    } else {
                                        Modifier
                                    },
                                )
                                .fillMaxWidth()
                                .height(46.dp),
                        )
                        if (section == ProfileSection.Parental) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 12.dp)
                                    .size(11.dp)
                                    .background(
                                        if (parentalControlScope.enabled) Color(0xFF20D878) else Color(0xFFFF4B4B),
                                        RoundedCornerShape(50),
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(50)),
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (selectedSection == ProfileSection.Parental) {
                            Modifier
                        } else {
                            Modifier.verticalScroll(rememberScrollState())
                        },
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (selectedSection) {
                    ProfileSection.Xtream -> XtreamPanel(
                        strings = strings,
                        state = state,
                        syncStatus = syncStatus,
                        multiProfileAccess = multiProfileAccess,
                        onLockedFeature = onLockedFeature,
                        onShowXtreamSetupQr = onShowXtreamSetupQr,
                        onOpenSyncDialog = { showXtreamSyncDialog = true },
                        onSaveXtreamAccount = onSaveXtreamAccount,
                        onSaveEpgUrl = onSaveEpgUrl,
                        onSaveM3uUrl = onSaveM3uUrl,
                        onSelectPlaylistSource = onSelectPlaylistSource,
                        onSavePlaylistProfile = onSavePlaylistProfile,
                        onSelectPlaylistProfile = onSelectPlaylistProfile,
                        onDeletePlaylistProfile = onDeletePlaylistProfile,
                        onSynchronizePlaylistProfile = onSynchronizePlaylistProfile,
                        onDeleteXtreamAccount = onDeleteXtreamAccount,
                        sectionFocusRequester = xtreamSectionFocusRequester,
                        deviceCatalogFocusRequester = deviceCatalogFocusRequester,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ProfileSection.Parental -> ParentalControlPanel(
                        strings = strings,
                        state = parentalState,
                        viewModel = parentalViewModel,
                        onExitToMenu = { runCatching { parentalSectionFocusRequester.requestFocus() } },
                        modifier = Modifier.fillMaxSize(),
                    )
                    ProfileSection.Sync -> ProfilePanel(
                        title = strings.sync,
                        icon = Icons.Default.CloudSync,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SynchronizationPreferencesContent(
                            settings = parentalSettings,
                            activeProfileName = activePlaylistProfile?.name ?: strings.none,
                            activeServer = when (resolvedActivePlaylistProfile?.source) {
                                PlaylistSource.M3u -> resolvedActivePlaylistProfile.m3uUrl.safeServerHost(strings.notConfigured)
                                PlaylistSource.Xtream -> resolvedActivePlaylistProfile.xtreamHost.ifBlank { strings.notConfigured }
                                null -> strings.notConfigured
                            },
                            lastSynchronization = activePlaylistProfile?.lastSyncAt
                                ?.let { it.asProfileDate() }
                                ?: strings.syncNever,
                            strings = strings,
                            firstControlFocusRequester = syncFirstControlFocusRequester,
                            menuFocusRequester = syncSectionFocusRequester,
                            onSetAutostartEnabled = { value ->
                                scope.launch { container.settingsRepository.setAutostartEnabled(value) }
                            },
                            onSetBackgroundSyncEnabled = { value ->
                                scope.launch {
                                    container.settingsRepository.setBackgroundSyncEnabled(value)
                                    BackgroundSyncScheduler.applyPeriodicSync(context, value)
                                }
                            },
                            onSetSyncFrequency = { value ->
                                scope.launch { container.settingsRepository.setSyncFrequency(value) }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showParentalUnlockDialog) {
        NumericPinDialog(
            title = strings.enterPin,
            strings = strings,
            onDismiss = { showParentalUnlockDialog = false },
            onSubmit = { pin ->
                container.settingsRepository.verifyParentalPin(pin).also { valid ->
                    if (valid) {
                        showParentalUnlockDialog = false
                        selectedSection = ProfileSection.Parental
                    }
                }
            },
        )
    }
    if (showParentalCreateDialog) {
        NumericPinDialog(
            title = strings.createPin,
            strings = strings,
            requireConfirmation = true,
            onDismiss = { showParentalCreateDialog = false },
            onSubmit = { pin ->
                scope.launch { container.settingsRepository.setParentalPin(pin) }
                showParentalCreateDialog = false
                selectedSection = ProfileSection.Parental
                true
            },
        )
    }

    state.qrDialog?.let { qr ->
        SmartVisionQrDialog(
            title = qr.title,
            subtitle = qr.subtitle,
            qrUrl = qr.url,
            tvCode = state.tvCode,
            code = qr.code,
            loading = qr.loading,
            error = qr.error ?: state.errorMessage,
            licenseCode = if (qr.allowsLicenseEntry) state.licenseCode else "",
            onLicenseCodeChange = if (qr.allowsLicenseEntry) onLicenseCodeChange else null,
            onSubmitLicenseCode = if (qr.allowsLicenseEntry) onActivateLicense else null,
            submittingLicense = state.submittingLicense,
            onDismiss = onDismissQr,
        )
    }

    if (showXtreamSyncDialog) {
        XtreamSynchronizationDialog(
            state = state,
            syncStatus = syncStatus,
            onStartSync = onSyncCatalog,
            onCancel = {
                showXtreamSyncDialog = false
                selectedSection = ProfileSection.Xtream
                pendingFocusSection = ProfileSection.Xtream
            },
            onReturn = {
                showXtreamSyncDialog = false
                selectedSection = ProfileSection.Xtream
                pendingFocusSection = ProfileSection.Xtream
            },
        )
    }
}

@Composable
internal fun LicensePanel(
    state: ProfileUiState,
    onRefresh: () -> Unit,
    onShowLicenseQr: () -> Unit,
    onShowPrivacyOptions: () -> Unit,
    privacyOptionsRequired: Boolean,
    embedded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val content: @Composable ColumnScope.() -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ProfileMetric("Statut", state.activationStatusLabel, Modifier.weight(1f), state.usageMode.color)
            ProfileMetric("Expiration", state.licenseExpiresAt.ifBlank { "Non disponible" }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        ProfileInfoRow("Type", state.usageMode.description)
        ProfileInfoRow("Code TV", state.tvCode)
        ProfileInfoRow("Identifiant appareil", state.deviceId.ifBlank { "Generation..." })
        ProfileInfoRow("Renouvellement", state.usageMode.renewalHint)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            TvButton(
                text = state.usageMode.primaryCta,
                onClick = onShowLicenseQr,
                leadingIcon = Icons.Default.Key,
                modifier = Modifier
                    .weight(1.25f)
                    .height(46.dp),
            )
            TvButton(
                text = if (state.refreshing) "Verification..." else "Actualiser",
                onClick = onRefresh,
                enabled = !state.refreshing,
                leadingIcon = Icons.Default.Refresh,
                variant = TvButtonVariant.Secondary,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            TvButton(
                text = "Saisir une licence",
                onClick = onShowLicenseQr,
                leadingIcon = Icons.Default.Verified,
                variant = TvButtonVariant.Secondary,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
            )
            if (state.usageMode == UsageMode.FreeAds && privacyOptionsRequired) {
                TvButton(
                    text = "Confidentialite pubs",
                    onClick = onShowPrivacyOptions,
                    leadingIcon = Icons.Default.Security,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                )
            }
        }
    }
    if (embedded) {
        Column(modifier = modifier, content = content)
    } else {
        ProfilePanel(
            title = "Licence SmartVision",
            icon = Icons.Default.Verified,
            modifier = modifier,
            trailingContent = {
                StatusPill(state.usageMode.label, state.usageMode.color)
            },
            content = content,
        )
    }
}

private enum class ProfileSection(
    val label: String,
    val icon: ImageVector,
) {
    Xtream("Info compte", Icons.Default.Person),
    Parental("Controle parental", Icons.Default.Lock),
    Sync("Synchronisation", Icons.Default.CloudSync),
}

@Composable
private fun XtreamPanel(
    strings: SmartVisionStrings,
    state: ProfileUiState,
    syncStatus: SyncStatus,
    multiProfileAccess: PremiumFeatureGateResult,
    onLockedFeature: () -> Unit,
    onShowXtreamSetupQr: () -> Unit,
    onOpenSyncDialog: () -> Unit,
    onSaveXtreamAccount: (XtreamAccount) -> Unit,
    onSaveEpgUrl: (String) -> Unit,
    onSaveM3uUrl: (String) -> Unit,
    onSelectPlaylistSource: (PlaylistSource) -> Unit,
    onSavePlaylistProfile: (PlaylistProfile) -> Unit,
    onSelectPlaylistProfile: (String) -> Unit,
    onDeletePlaylistProfile: (String) -> Unit,
    onSynchronizePlaylistProfile: (String) -> Unit,
    onDeleteXtreamAccount: (String) -> Unit,
    sectionFocusRequester: FocusRequester,
    deviceCatalogFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    var profileToEdit by remember { mutableStateOf<PlaylistProfile?>(null) }
    var selectedProfileId by remember { mutableStateOf<String?>(null) }
    var profileDetailVisible by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<PlaylistProfile?>(null) }
    var showProfileEditor by remember { mutableStateOf(false) }
    var profileAvatarToEdit by remember { mutableStateOf<PlaylistProfile?>(null) }
    var showEpgEditor by remember { mutableStateOf(false) }
    var showM3uEditor by remember { mutableStateOf(false) }
    var deletedProfileIdAwaitingFocus by remember { mutableStateOf<String?>(null) }
    val profileListFocusRequester = remember { FocusRequester() }
    val selectedProfile = state.playlistProfiles.firstOrNull { it.id == selectedProfileId }
        ?: state.playlistProfiles.firstOrNull { it.id == state.activePlaylistProfileId }
        ?: state.playlistProfiles.firstOrNull()

    LaunchedEffect(deletedProfileIdAwaitingFocus, state.playlistProfiles) {
        val deletedId = deletedProfileIdAwaitingFocus ?: return@LaunchedEffect
        if (state.playlistProfiles.any { it.id == deletedId }) return@LaunchedEffect
        delay(ProfileFocusRequestDelayMillis)
        if (state.playlistProfiles.isNotEmpty() || multiProfileAccess.allowed) {
            runCatching { profileListFocusRequester.requestFocus() }
        } else {
            runCatching { sectionFocusRequester.requestFocus() }
        }
        deletedProfileIdAwaitingFocus = null
    }

    ProfilePanel(
        title = "Info compte",
        icon = Icons.Default.Person,
        modifier = modifier,
        trailingContent = {
            ExpirationPill(state.xtreamExpiresAt.ifBlank { "Expiration non disponible" })
        },
    ) {
        PlaylistProfilesSection(
            profiles = state.playlistProfiles,
            activeProfileId = state.activePlaylistProfileId,
            onAdd = {
                if (multiProfileAccess.allowed) {
                    profileToEdit = null
                    showProfileEditor = true
                } else {
                    onLockedFeature()
                }
            },
            multiProfileLocked = !multiProfileAccess.allowed,
            selectedProfileId = selectedProfile?.id,
            onOpen = { profile ->
                val sameProfile = selectedProfileId == profile.id
                selectedProfileId = profile.id
                profileDetailVisible = !(sameProfile && profileDetailVisible)
            },
            onSelect = onSelectPlaylistProfile,
            onEdit = { profile ->
                if (multiProfileAccess.allowed) {
                    profileToEdit = profile
                    showProfileEditor = true
                } else {
                    onLockedFeature()
                }
            },
            restoreFocusRequester = profileListFocusRequester,
        )
        if (profileDetailVisible) selectedProfile?.let { profile ->
            Spacer(Modifier.height(8.dp))
            PlaylistProfileDetailsPanel(
                profile = profile,
                active = profile.id == state.activePlaylistProfileId,
                syncStatus = syncStatus,
                onEditProfile = {
                    if (multiProfileAccess.allowed) {
                        profileToEdit = profile
                        showProfileEditor = true
                    } else {
                        onLockedFeature()
                    }
                },
                onEditAvatar = {
                    if (multiProfileAccess.allowed) {
                        profileAvatarToEdit = profile
                    } else {
                        onLockedFeature()
                    }
                },
                onShowXtreamSetupQr = onShowXtreamSetupQr,
                onSelectSource = { source ->
                    onSavePlaylistProfile(profile.copy(source = source))
                    selectedProfileId = profile.id
                },
                onEditM3u = { showM3uEditor = true },
                onEditEpg = { showEpgEditor = true },
                onSynchronize = {
                    selectedProfileId = profile.id
                    onSynchronizePlaylistProfile(profile.id)
                },
                onToggleLock = {
                    if (container.profilePinManager.hasPin()) {
                        onSavePlaylistProfile(profile.copy(isLocked = !profile.isLocked))
                        selectedProfileId = profile.id
                    }
                },
                pinConfigured = container.profilePinManager.hasPin(),
                onDelete = {
                    if (profile.type != ProfileType.ADMIN) {
                        selectedProfileId = profile.id
                        profileToDelete = profile
                    }
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        DeviceCatalogInlineSection(
            state = state,
            syncStatus = syncStatus,
            focusRequester = deviceCatalogFocusRequester,
        )
    }

    profileAvatarToEdit?.let { profile ->
        ProfileAvatarPickerDialog(
            initialAvatarId = profile.avatarId,
            profileType = profile.type,
            onDismiss = { profileAvatarToEdit = null },
            onSave = { avatarId ->
                profileAvatarToEdit = null
                onSavePlaylistProfile(profile.copy(avatarId = avatarId))
                selectedProfileId = profile.id
                profileDetailVisible = true
            },
        )
    }

    if (showProfileEditor) {
        PlaylistProfileEditorDialog(
            initial = profileToEdit,
            adminProfile = state.playlistProfiles.firstOrNull { it.type == ProfileType.ADMIN },
            existingNames = state.playlistProfiles
                .filterNot { it.id == profileToEdit?.id }
                .map { it.name },
            onDismiss = {
                showProfileEditor = false
                profileToEdit = null
            },
            onSave = { profile ->
                showProfileEditor = false
                profileToEdit = null
                onSavePlaylistProfile(profile)
            },
        )
    }

    profileToDelete?.let { profile ->
        ConfirmPlaylistProfileDeleteDialog(
            profile = profile,
            strings = strings,
            onDismiss = { profileToDelete = null },
            onConfirm = {
                profileToDelete = null
                val deletedIndex = state.playlistProfiles.indexOfFirst { it.id == profile.id }
                selectedProfileId = state.playlistProfiles.getOrNull(deletedIndex + 1)?.id
                    ?: state.playlistProfiles.getOrNull(deletedIndex - 1)?.id
                profileDetailVisible = false
                deletedProfileIdAwaitingFocus = profile.id
                onDeletePlaylistProfile(profile.id)
            },
        )
    }

    if (showEpgEditor) {
        val profile = selectedProfile
        UrlEditorDialog(
            title = "Modifier URL EPG",
            invalidMessage = "URL EPG invalide.",
            initialUrl = profile?.epgUrl ?: state.epgUrl,
            onDismiss = { showEpgEditor = false },
            onSave = { url ->
                showEpgEditor = false
                if (profile != null) {
                    onSavePlaylistProfile(profile.copy(epgUrl = url))
                    selectedProfileId = profile.id
                } else {
                    onSaveEpgUrl(url)
                }
            },
        )
    }

    if (showM3uEditor) {
        val profile = selectedProfile
        UrlEditorDialog(
            title = "Modifier lien M3U",
            invalidMessage = "Lien M3U invalide.",
            initialUrl = profile?.m3uUrl ?: state.m3uUrl,
            onDismiss = { showM3uEditor = false },
            onSave = { url ->
                showM3uEditor = false
                if (profile != null) {
                    onSavePlaylistProfile(profile.copy(m3uUrl = url))
                    selectedProfileId = profile.id
                } else {
                    onSaveM3uUrl(url)
                }
            },
        )
    }
}

@Composable
private fun PlaylistProfilesSection(
    profiles: List<PlaylistProfile>,
    activeProfileId: String,
    selectedProfileId: String?,
    onAdd: () -> Unit,
    multiProfileLocked: Boolean,
    onOpen: (PlaylistProfile) -> Unit,
    onSelect: (String) -> Unit,
    onEdit: (PlaylistProfile) -> Unit,
    restoreFocusRequester: FocusRequester,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(SmartVisionColors.Surface.copy(alpha = 0.62f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.82f)), RoundedCornerShape(7.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            AccountInfoLine(
                icon = Icons.Default.Person,
                label = "Profils",
                value = profiles.firstOrNull { it.id == activeProfileId }?.name ?: "Aucun profil actif",
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            TvButton(
                text = "Ajouter",
                onClick = onAdd,
                leadingIcon = Icons.Default.Add,
                enabled = !multiProfileLocked,
                focusRequester = restoreFocusRequester.takeIf { profiles.isEmpty() },
                modifier = Modifier
                    .width(150.dp)
                    .height(40.dp),
            )
            if (multiProfileLocked) {
                Spacer(Modifier.width(8.dp))
                Image(
                    painter = painterResource(R.drawable.premium_crown),
                    contentDescription = "Premium",
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (profiles.isEmpty()) {
            Text(
                text = "Aucun profil. Ajoutez une source Xtream ou M3U pour configurer ce compte.",
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
            )
        } else {
            profiles.forEach { profile ->
                PlaylistProfileRow(
                    profile = profile,
                    active = profile.id == activeProfileId,
                    selected = profile.id == selectedProfileId,
                    onOpen = { onOpen(profile) },
                    onSelect = { onSelect(profile.id) },
                    onEdit = { onEdit(profile) },
                    externalFocusRequester = restoreFocusRequester.takeIf { profile.id == selectedProfileId },
                )
                Spacer(Modifier.height(7.dp))
            }
        }
    }
}

@Composable
private fun PlaylistProfileRow(
    profile: PlaylistProfile,
    active: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    externalFocusRequester: FocusRequester? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val internalRowFocusRequester = remember { FocusRequester() }
    val rowFocusRequester = externalFocusRequester ?: internalRowFocusRequester
    val toggleFocusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val focusStyle = LocalTvFocusStyle.current
    val borderColor = when {
        focused -> focusStyle.accent
        selected -> SmartVisionColors.CyanAccent.copy(alpha = 0.72f)
        active -> Color(0xFF20D46B).copy(alpha = 0.68f)
        else -> SmartVisionColors.Border.copy(alpha = 0.55f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    focused -> focusStyle.background
                    selected -> SmartVisionColors.Primary.copy(alpha = 0.20f)
                    active -> Color(0xFF0F3728).copy(alpha = 0.66f)
                    else -> Color.White.copy(alpha = 0.035f)
                },
            )
            .border(
                BorderStroke(if (focused) focusStyle.borderWidth else 1.dp, borderColor),
                RoundedCornerShape(6.dp),
            )
            .focusRequester(rowFocusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight && profile.isConfigured) {
                    runCatching { toggleFocusRequester.requestFocus() }
                    true
                } else {
                    false
                }
            }
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onOpen)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaylistProfileAvatar(profile = profile, modifier = Modifier.size(42.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileInlineEditIcon(onClick = onEdit)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = profile.name,
                    color = SmartVisionColors.TextPrimary,
                    style = SmartVisionType.Label,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${profile.source.displayLabel()}  |  ${profile.status.displayLabel()}",
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        SourceToggleButton(
            active = active,
            enabled = profile.isConfigured,
            onClick = onSelect,
            modifier = Modifier
                .focusRequester(toggleFocusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                        runCatching { rowFocusRequester.requestFocus() }
                        true
                    } else {
                        false
                    }
                },
        )
    }
}

@Composable
private fun ProfileInlineEditIcon(
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (focused) SmartVisionColors.Primary.copy(alpha = 0.32f) else Color.Transparent)
            .border(
                BorderStroke(if (focused) 1.dp else 0.dp, if (focused) SmartVisionColors.CyanAccent else Color.Transparent),
                RoundedCornerShape(5.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Modifier le profil",
            tint = if (focused) Color.White else SmartVisionColors.TextSecondary,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
fun PlaylistProfileAvatar(
    profile: PlaylistProfile,
    modifier: Modifier = Modifier,
) {
    ProfileAvatarImage(
        avatarId = profile.avatarId,
        profileType = profile.type,
        modifier = modifier,
    )
}

@Composable
private fun ProfileAvatarEditButton(
    profile: PlaylistProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) SmartVisionColors.CyanAccent else Color.White.copy(alpha = 0.22f)),
                RoundedCornerShape(7.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        PlaylistProfileAvatar(profile = profile, modifier = Modifier.matchParentSize())
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Modifier la photo",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(18.dp)
                .background(Color.Black.copy(alpha = 0.52f), RoundedCornerShape(50))
                .padding(2.dp),
        )
    }
}

@Composable
private fun PlaylistProfileDetailsPanel(
    profile: PlaylistProfile,
    active: Boolean,
    syncStatus: SyncStatus,
    onEditProfile: () -> Unit,
    onEditAvatar: () -> Unit,
    onShowXtreamSetupQr: () -> Unit,
    onSelectSource: (PlaylistSource) -> Unit,
    onEditM3u: () -> Unit,
    onEditEpg: () -> Unit,
    onSynchronize: () -> Unit,
    onToggleLock: () -> Unit,
    pinConfigured: Boolean,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.72f)), RoundedCornerShape(7.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatarEditButton(
                profile = profile,
                onClick = onEditAvatar,
                modifier = Modifier.size(50.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.Label,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    ProfileInlineEditIcon(onClick = onEditProfile)
                }
                Text(
                    text = if (active) "Profil actif" else "Profil selectionne",
                    color = if (active) Color(0xFF20D46B) else SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Caption,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(7.dp))
                .background(SmartVisionColors.Surface.copy(alpha = 0.58f))
                .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.78f)), RoundedCornerShape(7.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceToggleButton(active = profile.isLocked, enabled = pinConfigured, onClick = onToggleLock)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Profile lock", color = SmartVisionColors.TextPrimary, style = SmartVisionType.Label)
                Text(
                    if (pinConfigured) "Uses the administrator parental PIN" else "Configure the parental PIN first",
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Caption,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        XtreamAccountCard(
            account = profile.toXtreamAccountOrNull(),
            active = profile.source == PlaylistSource.Xtream,
            editEnabled = true,
            deleteEnabled = profile.type != ProfileType.ADMIN,
            onToggleSource = { onSelectSource(PlaylistSource.Xtream) },
            onEdit = onEditProfile,
            onEditQr = onShowXtreamSetupQr,
            onDelete = onDelete,
        )
        Spacer(Modifier.height(8.dp))
        M3uUrlCard(
            m3uUrl = profile.m3uUrl,
            active = profile.source == PlaylistSource.M3u,
            onToggleSource = { onSelectSource(PlaylistSource.M3u) },
            onEdit = onEditM3u,
        )
        Spacer(Modifier.height(8.dp))
        EpgUrlCard(
            epgUrl = profile.epgUrl,
            onEdit = onEditEpg,
            onEditQr = onShowXtreamSetupQr,
        )
        Spacer(Modifier.height(8.dp))
        SynchronizationCard(
            lastSync = profile.lastSyncAt.asProfileDate(),
            syncStatus = syncStatus,
            onOpenSyncDialog = onSynchronize,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun XtreamAccountCard(
    account: XtreamAccount?,
    active: Boolean,
    editEnabled: Boolean = account != null,
    deleteEnabled: Boolean = account != null,
    onToggleSource: () -> Unit,
    onEdit: () -> Unit,
    onEditQr: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(SmartVisionColors.Surface.copy(alpha = 0.58f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.78f)), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceToggleButton(active = active, enabled = account != null, onClick = onToggleSource)
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = account?.name?.ifBlank { "Compte Xtream" } ?: "Compte Xtream",
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            XtreamCredentialsLine(account)
        }
        Spacer(Modifier.width(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ProfileIconTileButton(
                icon = Icons.Default.QrCode2,
                contentDescription = "Modifier par QR",
                onClick = onEditQr,
            )
            ProfileIconTileButton(
                icon = Icons.Default.Edit,
                contentDescription = "Modifier",
                onClick = onEdit,
                enabled = editEnabled,
            )
            ProfileIconTileButton(
                icon = Icons.Default.Delete,
                contentDescription = "Supprimer",
                onClick = onDelete,
                enabled = deleteEnabled,
            )
        }
    }
}

@Composable
private fun XtreamCredentialsLine(account: XtreamAccount?) {
    Text(
        text = listOf(
            "URL ${account?.host?.ifBlank { "Non configure" } ?: "Non configure"}",
            "User ${account?.username?.ifBlank { "Non configure" } ?: "Non configure"}",
            "Pass ${if (account?.password.isNullOrBlank()) "Non configure" else "********"}",
        ).joinToString("  |  "),
        color = SmartVisionColors.TextSecondary,
        style = SmartVisionType.Caption,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun M3uUrlCard(
    m3uUrl: String,
    active: Boolean,
    onToggleSource: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(SmartVisionColors.Surface.copy(alpha = 0.58f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.78f)), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceToggleButton(active = active, enabled = m3uUrl.isNotBlank(), onClick = onToggleSource)
        Spacer(Modifier.width(12.dp))
        AccountInfoLine(
            icon = Icons.Default.Devices,
            label = "Lien M3U",
            value = m3uUrl.ifBlank { "Non configure" },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        ProfileIconTileButton(
            icon = Icons.Default.Edit,
            contentDescription = "Modifier lien M3U",
            onClick = onEdit,
        )
    }
}

@Composable
private fun EpgUrlCard(
    epgUrl: String,
    onEdit: () -> Unit,
    onEditQr: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(SmartVisionColors.Surface.copy(alpha = 0.58f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.78f)), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountInfoLine(
            icon = Icons.Default.Devices,
            label = "URL EPG",
            value = epgUrl.ifBlank { "Non configure" },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(18.dp))
        ProfileIconTileButton(
            icon = Icons.Default.Edit,
            contentDescription = "Modifier URL EPG",
            onClick = onEdit,
        )
        Spacer(Modifier.width(12.dp))
        ProfileIconTileButton(
            icon = Icons.Default.QrCode2,
            contentDescription = "Modifier URL EPG par QR",
            onClick = onEditQr,
        )
    }
}

@Composable
private fun SynchronizationCard(
    lastSync: String,
    syncStatus: SyncStatus,
    onOpenSyncDialog: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(SmartVisionColors.Surface.copy(alpha = 0.58f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.78f)), RoundedCornerShape(7.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvButton(
            text = syncStatus.buttonLabel,
            onClick = onOpenSyncDialog,
            enabled = syncStatus !is SyncStatus.Running,
            leadingIcon = Icons.Default.CloudSync,
            modifier = Modifier
                .width(if (onDelete == null) 270.dp else 138.dp)
                .height(42.dp),
        )
        if (onDelete != null) {
            Spacer(Modifier.width(8.dp))
            TvButton(
                text = "Supprimer",
                onClick = onDelete,
                enabled = syncStatus !is SyncStatus.Running,
                leadingIcon = Icons.Default.Delete,
                variant = TvButtonVariant.Danger,
                modifier = Modifier
                    .width(132.dp)
                    .height(42.dp),
            )
        }
        Spacer(Modifier.width(20.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(44.dp)
                .background(SmartVisionColors.Border.copy(alpha = 0.74f)),
        )
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            AccountInfoLine(Icons.Default.CheckCircle, "Date de synchronisation", lastSync)
        }
    }
}

@Composable
private fun SourceToggleButton(
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val color = if (active) Color(0xFF20D46B) else Color(0xFFE33A3A)
    Box(
        modifier = modifier
            .size(width = 54.dp, height = 26.dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = if (enabled) 0.88f else 0.32f))
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color.White else color.copy(alpha = 0.9f)),
                RoundedCornerShape(50),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .padding(horizontal = 6.dp),
        contentAlignment = if (active) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            text = if (active) "ON" else "OFF",
            color = Color.White,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AccountInfoLine(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProfileIconTileButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val borderColor = if (focused) SmartVisionColors.CyanAccent else Color.Transparent
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 38.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (focused) SmartVisionColors.SurfaceElevated.copy(alpha = 0.9f) else Color.Transparent)
            .border(BorderStroke(if (focused) 1.dp else 0.dp, borderColor), RoundedCornerShape(7.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(enabled = enabled, interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.34f),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun UrlEditorDialog(
    title: String,
    invalidMessage: String,
    initialUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }
    var error by remember { mutableStateOf<String?>(null) }
    val urlFocusRequester = remember { FocusRequester() }
    val saveFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(ProfileFocusRequestDelayMillis)
        runCatching { urlFocusRequester.requestFocus() }
    }

    TvDialogSurface(
        title = title,
        onDismiss = onDismiss,
        width = 600.dp,
        icon = Icons.Default.Edit,
    ) {
            ProfileEditTextField(
                label = "URL",
                value = url,
                onValueChange = {
                    url = it
                    error = null
                },
                focusRequester = urlFocusRequester,
                nextFocusRequester = saveFocusRequester,
            )
            error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = SmartVisionColors.Error, style = SmartVisionType.Caption)
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TvButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier.height(42.dp),
                )
                Spacer(Modifier.width(10.dp))
                TvButton(
                    text = "Enregistrer",
                    onClick = {
                        val normalized = url.trim()
                        if (normalized.isNotBlank() && !normalized.startsWith("http://") && !normalized.startsWith("https://")) {
                            error = invalidMessage
                        } else {
                            onSave(normalized)
                        }
                    },
                    focusRequester = saveFocusRequester,
                    modifier = Modifier.height(42.dp),
                )
            }
    }
}

private enum class XtreamSyncDialogPhase {
    Confirmation,
    Running,
    Success,
    Error,
}

@Composable
private fun XtreamSynchronizationDialog(
    state: ProfileUiState,
    syncStatus: SyncStatus,
    onStartSync: suspend () -> Result<Unit>,
    onCancel: () -> Unit,
    onReturn: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val startFocusRequester = remember { FocusRequester() }
    val returnFocusRequester = remember { FocusRequester() }
    var phase by remember { mutableStateOf(XtreamSyncDialogPhase.Confirmation) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val blocking = phase == XtreamSyncDialogPhase.Running
    val account = state.activeXtreamAccount
    val source = state.activePlaylistSource
    val sourceConfigured = when (source) {
        PlaylistSource.Xtream -> state.hasXtream
        PlaylistSource.M3u -> state.m3uUrl.isNotBlank()
    }
    val sourceTitle = if (source == PlaylistSource.M3u) "Synchronisation M3U" else "Synchronisation Xtream"
    val progress = syncStatus.catalogProgressOrDefault(state.account)
    val showProgress = phase != XtreamSyncDialogPhase.Confirmation

    fun closeAllowed() {
        if (phase == XtreamSyncDialogPhase.Confirmation) onCancel() else onReturn()
    }

    BackHandler(enabled = true) {
        if (!blocking) closeAllowed()
    }
    LaunchedEffect(Unit) {
        delay(ProfileFocusRequestDelayMillis)
        runCatching { startFocusRequester.requestFocus() }
    }
    LaunchedEffect(phase) {
        if (phase == XtreamSyncDialogPhase.Success || phase == XtreamSyncDialogPhase.Error) {
            delay(ProfileFocusRequestDelayMillis)
            runCatching { returnFocusRequester.requestFocus() }
        }
    }

    Dialog(
        onDismissRequest = { if (!blocking) closeAllowed() },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .width(860.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFA071629),
                            Color(0xF5050B16),
                        ),
                    ),
                )
                .border(BorderStroke(1.dp, SmartVisionColors.CyanAccent.copy(alpha = 0.62f)), RoundedCornerShape(8.dp))
                .onPreviewKeyEvent { blocking }
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = SmartVisionColors.CyanAccent,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sourceTitle,
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.TitleS,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (source == PlaylistSource.M3u) {
                            "Controlez le lien M3U avant de relancer le chargement Live TV."
                        } else {
                            "Controlez les donnees catalogue avant de lancer la mise a jour."
                        },
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Caption,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                XtreamSyncCountCard(
                    title = "Live TV",
                    progress = progress.live,
                    showProgress = showProgress,
                    modifier = Modifier.weight(1f),
                )
                XtreamSyncCountCard(
                    title = "Films",
                    progress = progress.movies,
                    showProgress = showProgress,
                    enabled = source == PlaylistSource.Xtream,
                    modifier = Modifier.weight(1f),
                )
                XtreamSyncCountCard(
                    title = "Series",
                    progress = progress.series,
                    showProgress = showProgress,
                    enabled = source == PlaylistSource.Xtream,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(SmartVisionColors.Surface.copy(alpha = 0.48f))
                    .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.72f)), RoundedCornerShape(7.dp))
                    .padding(14.dp),
            ) {
                SyncDialogInfoRow("Code TV", state.tvCode)
                if (source == PlaylistSource.M3u) {
                    SyncDialogInfoRow("Source active", "Lien M3U")
                    SyncDialogInfoRow("Lien M3U", state.m3uUrl.ifBlank { "Non configure" })
                    SyncDialogInfoRow("URL EPG", state.epgUrl.ifBlank { "Non configure" })
                } else {
                    SyncDialogInfoRow("Source active", "Xtream")
                    SyncDialogInfoRow("Url Xtream", account?.host?.ifBlank { state.xtreamHost } ?: state.xtreamHost.ifBlank { "Non configure" })
                    SyncDialogInfoRow("Username", account?.username?.ifBlank { state.xtreamUsername } ?: state.xtreamUsername.ifBlank { "Non configure" })
                    SyncDialogInfoRow("Password", if (account?.password.isNullOrBlank()) "********" else "********")
                }
                SyncDialogInfoRow("Derniere synchro", state.account.lastSync ?: "Jamais")
            }
            if (phase == XtreamSyncDialogPhase.Success) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = syncStatus.successMessageOrDefault(),
                    color = SmartVisionColors.Success,
                    style = SmartVisionType.Label,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(7.dp))
                        .background(SmartVisionColors.Success.copy(alpha = 0.14f))
                        .border(BorderStroke(1.dp, SmartVisionColors.Success.copy(alpha = 0.58f)), RoundedCornerShape(7.dp))
                        .padding(vertical = 10.dp),
                )
            }
            if (phase == XtreamSyncDialogPhase.Error) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = errorMessage ?: syncStatus.errorMessageOrDefault(),
                    color = SmartVisionColors.Error,
                    style = SmartVisionType.Label,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(7.dp))
                        .background(SmartVisionColors.Error.copy(alpha = 0.14f))
                        .border(BorderStroke(1.dp, SmartVisionColors.Error.copy(alpha = 0.58f)), RoundedCornerShape(7.dp))
                        .padding(vertical = 10.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TvButton(
                    text = if (phase == XtreamSyncDialogPhase.Confirmation) "Annuler" else "Retour",
                    onClick = { if (!blocking) closeAllowed() },
                    enabled = !blocking,
                    leadingIcon = Icons.Default.ArrowBack,
                    focusRequester = returnFocusRequester,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .width(210.dp)
                        .height(44.dp),
                )
                TvButton(
                    text = if (blocking) "Synchronisation en cours" else "Lancer la synchronisation",
                    onClick = {
                        if (phase == XtreamSyncDialogPhase.Confirmation) {
                            phase = XtreamSyncDialogPhase.Running
                            errorMessage = null
                            scope.launch {
                                val result = onStartSync()
                                phase = if (result.isSuccess) {
                                    XtreamSyncDialogPhase.Success
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Synchronisation impossible"
                                    XtreamSyncDialogPhase.Error
                                }
                            }
                        }
                    },
                    enabled = phase == XtreamSyncDialogPhase.Confirmation && sourceConfigured,
                    leadingIcon = if (blocking) null else Icons.Default.CloudSync,
                    leadingContent = if (blocking) {
                        {
                            CircularProgressIndicator(
                                color = SmartVisionColors.CyanAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else {
                        null
                    },
                    focusRequester = startFocusRequester,
                    modifier = Modifier
                        .width(286.dp)
                        .height(44.dp),
                )
            }
        }
    }
}

@Composable
private fun XtreamSyncCountCard(
    title: String,
    progress: SyncStatus.SyncSectionProgress,
    showProgress: Boolean,
    enabled: Boolean = true,
    visualStyle: HomeVisualStyle? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(7.dp)
    val valueColor = when {
        !enabled -> SmartVisionColors.TextSecondary
        progress.completed -> Color(0xFF7CFFB2)
        else -> SmartVisionColors.TextPrimary
    }
    Box(
        modifier = modifier
            .height(126.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        SmartVisionColors.Primary.copy(alpha = 0.24f),
                        SmartVisionColors.Surface.copy(alpha = 0.70f),
                    ),
                ),
            )
            .border(
                BorderStroke(1.dp, if (enabled) SmartVisionColors.CyanAccent.copy(alpha = 0.38f) else SmartVisionColors.Border.copy(alpha = 0.44f)),
                shape,
            ),
    ) {
        visualStyle?.let { style ->
            HomeVisualBackground(style = style, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF020712).copy(alpha = 0.62f)),
            )
        }
        if (showProgress && enabled && progress.percent < 100) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(1f - progress.fraction)
                    .align(Alignment.CenterEnd)
                    .background(Color.Black.copy(alpha = 0.38f)),
            )
        }
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
                Spacer(Modifier.weight(1f))
                if (showProgress && enabled) {
                    Text(
                        text = "${progress.percent}%",
                        color = if (progress.completed) Color(0xFF7CFFB2) else SmartVisionColors.CyanAccent,
                        style = SmartVisionType.Caption,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (enabled) progress.currentItems.toString() else "N/A",
                color = valueColor,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            if (showProgress && enabled) {
                Text(
                    text = buildString {
                        append(progress.message ?: progress.phase.profileSyncPhaseLabel())
                        progress.totalItems?.takeIf { it > 0 }?.let { append(" • ").append(progress.currentItems).append("/").append(it) }
                    },
                    color = if (progress.phase == SyncStatus.SyncSectionPhase.ERROR) SmartVisionColors.Error else SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Caption,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (progress.keptItems != null || progress.excludedItems != null) {
                    Text(
                        text = listOfNotNull(
                            progress.keptItems?.let { "$it conserves" },
                            progress.excludedItems?.let { "$it exclus" },
                        ).joinToString(" • "),
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Caption,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (showProgress && enabled) {
                LinearProgressIndicator(
                    progress = { progress.fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (progress.completed) Color(0xFF7CFFB2) else SmartVisionColors.CyanAccent,
                    trackColor = Color.White.copy(alpha = 0.10f),
                )
            }
        }
    }
}

private fun SyncStatus.SyncSectionPhase.profileSyncPhaseLabel(): String = when (this) {
    SyncStatus.SyncSectionPhase.WAITING -> "En attente"
    SyncStatus.SyncSectionPhase.RUNNING -> "Telechargement"
    SyncStatus.SyncSectionPhase.FILTERING -> "Filtrage Kids"
    SyncStatus.SyncSectionPhase.IMPORTING -> "Import local"
    SyncStatus.SyncSectionPhase.LOADING_TRENDS -> "Finalisation"
    SyncStatus.SyncSectionPhase.COMPLETED -> "Termine"
    SyncStatus.SyncSectionPhase.ERROR -> "Erreur"
}

@Composable
private fun SyncDialogInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$label :", color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
        Spacer(Modifier.width(10.dp))
        Text(
            text = value,
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun SyncStatus.catalogProgressOrDefault(account: AccountProfile): SyncStatus.CatalogProgress =
    when (this) {
        is SyncStatus.Running -> catalogProgress
        is SyncStatus.Success -> catalogProgress
        is SyncStatus.Error -> catalogProgress
        SyncStatus.Idle -> SyncStatus.CatalogProgress(
            live = SyncStatus.SyncSectionProgress(currentItems = account.liveCount, previousItems = account.liveCount),
            movies = SyncStatus.SyncSectionProgress(currentItems = account.movieCount, previousItems = account.movieCount),
            series = SyncStatus.SyncSectionProgress(currentItems = account.seriesCount, previousItems = account.seriesCount),
        )
    }

private fun SyncStatus.successMessageOrDefault(): String =
    (this as? SyncStatus.Success)?.message ?: "Synchronisation terminee avec succes."

private fun SyncStatus.errorMessageOrDefault(): String =
    (this as? SyncStatus.Error)?.message ?: "Synchronisation impossible."

@Composable
fun PlaylistProfileEditorDialog(
    initial: PlaylistProfile?,
    createType: ProfileType? = null,
    adminProfile: PlaylistProfile? = null,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (PlaylistProfile) -> Unit,
) {
    val container = LocalAppContainer.current
    val validationScope = rememberCoroutineScope()
    val profileType = initial?.type ?: createType ?: ProfileType.NORMAL
    var credentialsMode by remember(initial?.id, profileType) {
        mutableStateOf(
            if (profileType == ProfileType.ADMIN) CredentialsMode.CUSTOM
            else initial?.credentialsMode ?: if (adminProfile != null) CredentialsMode.SHARED_WITH_ADMIN else CredentialsMode.CUSTOM,
        )
    }
    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var avatarId by remember(initial?.id) {
        mutableStateOf(
            initial?.avatarId
                ?.takeIf { it.isNotBlank() }
                ?.let { canonicalProfileAvatarId(it, profileType) }
                ?: defaultProfileAvatarId(profileType, initial?.id?.takeIf { it.isNotBlank() }),
        )
    }
    var source by remember(initial?.id) {
        mutableStateOf(initial?.source ?: adminProfile?.source ?: PlaylistSource.Xtream)
    }
    var host by remember(initial?.id) { mutableStateOf(initial?.xtreamHost ?: "") }
    var username by remember(initial?.id) { mutableStateOf(initial?.xtreamUsername ?: "") }
    var password by remember(initial?.id) { mutableStateOf(initial?.xtreamPassword ?: "") }
    var m3uUrl by remember(initial?.id) { mutableStateOf(initial?.m3uUrl ?: "") }
    var epgUrl by remember(initial?.id) { mutableStateOf(initial?.epgUrl ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var validatingCredentials by remember { mutableStateOf(false) }
    var expandedSource by remember(initial?.id, profileType) { mutableStateOf<PlaylistSource?>(null) }
    val nameFocusRequester = remember { FocusRequester() }
    val firstSourceFocusRequester = remember { FocusRequester() }
    val hostFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val m3uFocusRequester = remember { FocusRequester() }
    val epgFocusRequester = remember { FocusRequester() }
    val saveFocusRequester = remember { FocusRequester() }

/*     val configuration = LocalConfiguration.current
    val dialogWidth = (configuration.screenWidthDp.dp - 48.dp).coerceAtMost(820.dp)
    val requestedDialogHeight = when {
        expandedSource != null -> 610.dp
        profileType != ProfileType.ADMIN && adminProfile != null -> 500.dp
        else -> 430.dp
    }
    val dialogHeight = requestedDialogHeight.coerceAtMost(configuration.screenHeightDp.dp - 32.dp) */


val configuration = LocalConfiguration.current
val dialogWidth = (configuration.screenWidthDp.dp * 0.50f)
    .coerceIn(520.dp, 680.dp)
val requestedDialogHeight = when {
    expandedSource != null -> 500.dp
    profileType != ProfileType.ADMIN && adminProfile != null -> 350.dp
    else -> 350.dp}
val dialogHeight = requestedDialogHeight.coerceAtMost(
    configuration.screenHeightDp.dp * 0.75f)




    val avatarPresets = remember(profileType) {
        when (profileType) {
            ProfileType.ADMIN -> listOf(AdminProfileAvatarId) + ProfileAvatarPresetIds
            ProfileType.KIDS -> KidsProfileAvatarPresetIds
            ProfileType.NORMAL -> ProfileAvatarPresetIds
        }
    }

    fun buildProfile(normalizedName: String, normalizedHost: String = host): PlaylistProfile =
        PlaylistProfile(
            id = initial?.id.orEmpty(),
            name = normalizedName,
            source = if (credentialsMode == CredentialsMode.SHARED_WITH_ADMIN) adminProfile?.source ?: source else source,
            type = profileType,
            credentialsMode = credentialsMode,
            isLocked = initial?.isLocked ?: false,
            avatarId = avatarId,
            avatarColorHex = initial?.avatarColorHex.orEmpty(),
            xtreamHost = if (credentialsMode == CredentialsMode.CUSTOM) normalizedHost else "",
            xtreamUsername = if (credentialsMode == CredentialsMode.CUSTOM) username else "",
            xtreamPassword = if (credentialsMode == CredentialsMode.CUSTOM) password else "",
            m3uUrl = if (credentialsMode == CredentialsMode.CUSTOM) m3uUrl else "",
            epgUrl = if (credentialsMode == CredentialsMode.CUSTOM) epgUrl else "",
            createdAt = initial?.createdAt ?: System.currentTimeMillis(),
            lastSyncAt = initial?.lastSyncAt,
        )

    fun validateCustomXtream(onSuccess: (String) -> Unit) {
        validatingCredentials = true
        validationMessage = null
        error = null
        validationScope.launch {
            when (val result = container.xtreamCredentialsValidator.validate(host, username, password)) {
                is XtreamCredentialsValidationResult.Success -> {
                    host = result.normalizedHost
                    validationMessage = "Xtream connection successful."
                    onSuccess(result.normalizedHost)
                }
                is XtreamCredentialsValidationResult.Failure -> error = result.message
            }
            validatingCredentials = false
        }
    }

    LaunchedEffect(Unit) {
        delay(ProfileFocusRequestDelayMillis)
        runCatching { nameFocusRequester.requestFocus() }
    }

    TvDialogSurface(
        title = when {
            initial != null -> "Modifier le profil"
            profileType == ProfileType.KIDS -> "Add Kids Profile"
            else -> "Add Normal Profile"
        },
        onDismiss = onDismiss,
        width = dialogWidth,
        icon = Icons.Default.Person,
        modifier = Modifier.height(dialogHeight).imePadding(),
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(0.42f)) {
                    ProfileEditTextField(
                        label = "Nom du profil",
                        value = name,
                        onValueChange = { name = it },
                        focusRequester = nameFocusRequester,
                        nextFocusRequester = firstSourceFocusRequester,
                    )
                }
                Column(modifier = Modifier.weight(0.58f)) {
                    Text("Photo de profil", color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
                    Spacer(Modifier.height(5.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    ) {
                        avatarPresets.forEach { presetId ->
                            ProfileAvatarPresetButton(
                                avatarId = presetId,
                                profileType = profileType,
                                selected = avatarId == presetId,
                                onClick = { avatarId = presetId },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (profileType != ProfileType.ADMIN && adminProfile != null) {
                Text("Xtream credentials", color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    TvButton(
                        text = "Same as administrator",
                        onClick = { credentialsMode = CredentialsMode.SHARED_WITH_ADMIN },
                        focusRequester = firstSourceFocusRequester,
                        variant = if (credentialsMode == CredentialsMode.SHARED_WITH_ADMIN) TvButtonVariant.Primary else TvButtonVariant.Secondary,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .focusProperties {
                                if (credentialsMode == CredentialsMode.SHARED_WITH_ADMIN) down = saveFocusRequester
                            },
                    )
                    TvButton(
                        text = "Other credentials",
                        onClick = { credentialsMode = CredentialsMode.CUSTOM },
                        variant = if (credentialsMode == CredentialsMode.CUSTOM) TvButtonVariant.Primary else TvButtonVariant.Secondary,
                        modifier = Modifier.weight(1f).height(42.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            if (credentialsMode == CredentialsMode.CUSTOM) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TvButton(
                    text = "Xtream Codes",
                    onClick = {
                        source = PlaylistSource.Xtream
                        expandedSource = if (expandedSource == PlaylistSource.Xtream) null else PlaylistSource.Xtream
                    },
                    focusRequester = if (profileType == ProfileType.ADMIN || adminProfile == null) firstSourceFocusRequester else null,
                    variant = if (expandedSource == PlaylistSource.Xtream) TvButtonVariant.Primary else TvButtonVariant.Secondary,
                    trailingContent = {
                        Icon(
                            if (expandedSource == PlaylistSource.Xtream) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .focusProperties {
                            if (expandedSource == null) down = saveFocusRequester
                        },
                )
                TvButton(
                    text = "Playlist M3U",
                    onClick = {
                        source = PlaylistSource.M3u
                        expandedSource = if (expandedSource == PlaylistSource.M3u) null else PlaylistSource.M3u
                    },
                    variant = if (expandedSource == PlaylistSource.M3u) TvButtonVariant.Primary else TvButtonVariant.Secondary,
                    trailingContent = {
                        Icon(
                            if (expandedSource == PlaylistSource.M3u) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .focusProperties {
                            if (expandedSource == null) down = saveFocusRequester
                        },
                )
            }
            if (credentialsMode == CredentialsMode.CUSTOM) Spacer(Modifier.height(12.dp))
            if (credentialsMode == CredentialsMode.CUSTOM && expandedSource == PlaylistSource.Xtream) {
                ProfileEditTextField("URL serveur", host, { host = it }, hostFocusRequester, nameFocusRequester, usernameFocusRequester)
                Row(
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.Bottom,
    modifier = Modifier.fillMaxWidth()
) {
    Column(modifier = Modifier.weight(1f)) {
        ProfileEditTextField("Username", username, { username = it }, usernameFocusRequester, hostFocusRequester, passwordFocusRequester)
    }
    Column(modifier = Modifier.weight(1f)) {
        ProfileEditTextField("Password", password, { password = it }, passwordFocusRequester, usernameFocusRequester, epgFocusRequester, password = true)
    }
    TvButton(
        text = if (validatingCredentials) "Testing..." else "Test connection",
        enabled = !validatingCredentials && host.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
        onClick = { validateCustomXtream {} },
        variant = TvButtonVariant.Secondary,
        modifier = Modifier
            .width(150.dp)
            .height(42.dp),
    )
}
                ProfileEditTextField("URL EPG optionnelle", epgUrl, { epgUrl = it }, epgFocusRequester, passwordFocusRequester, saveFocusRequester)
            } else if (credentialsMode == CredentialsMode.CUSTOM && expandedSource == PlaylistSource.M3u) {
                ProfileEditTextField("Lien M3U", m3uUrl, { m3uUrl = it }, m3uFocusRequester, nameFocusRequester, epgFocusRequester)
                ProfileEditTextField("Lien EPG optionnel", epgUrl, { epgUrl = it }, epgFocusRequester, m3uFocusRequester, saveFocusRequester)
            }
            error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = SmartVisionColors.Error, style = SmartVisionType.Caption)
            }
            validationMessage?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = SmartVisionColors.Success, style = SmartVisionType.Caption)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TvButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier.height(42.dp),
                )
                Spacer(Modifier.width(10.dp))
                TvButton(
                    text = if (validatingCredentials) "Validating..." else "Enregistrer",
                    enabled = !validatingCredentials,
                    onClick = {
                        val normalizedName = name.trim()
                        error = when {
                            normalizedName.isBlank() -> "Le nom du profil est obligatoire."
                            existingNames.any { it.equals(normalizedName, ignoreCase = true) } -> "Un profil porte deja ce nom."
                            credentialsMode == CredentialsMode.CUSTOM && source == PlaylistSource.Xtream && (host.isBlank() || username.isBlank() || password.isBlank()) -> "URL, username et password sont obligatoires."
                            credentialsMode == CredentialsMode.CUSTOM && source == PlaylistSource.M3u && m3uUrl.isBlank() -> "Le lien M3U est obligatoire."
                            credentialsMode == CredentialsMode.CUSTOM && source == PlaylistSource.Xtream && !host.looksLikeUrlHost() -> "URL serveur Xtream invalide."
                            credentialsMode == CredentialsMode.CUSTOM && source == PlaylistSource.M3u && !m3uUrl.looksLikeHttpUrl() -> "Lien M3U invalide."
                            credentialsMode == CredentialsMode.CUSTOM && epgUrl.isNotBlank() && !epgUrl.looksLikeHttpUrl() -> "URL EPG invalide."
                            else -> null
                        }
                        if (error == null) {
                            if (credentialsMode == CredentialsMode.CUSTOM && source == PlaylistSource.Xtream) {
                                validateCustomXtream { normalizedHost -> onSave(buildProfile(normalizedName, normalizedHost)) }
                            } else {
                                onSave(buildProfile(normalizedName))
                            }
                        }
                    },
                    focusRequester = saveFocusRequester,
                    modifier = Modifier.height(42.dp),
                )
        }
    }
}

@Composable
private fun ProfileAvatarPickerDialog(
    initialAvatarId: String,
    profileType: ProfileType,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var selectedAvatarId by remember(initialAvatarId) {
        mutableStateOf(canonicalProfileAvatarId(initialAvatarId, profileType))
    }
    val saveFocusRequester = remember { FocusRequester() }
    val avatarPresets = when (profileType) {
        ProfileType.ADMIN -> listOf(AdminProfileAvatarId) + ProfileAvatarPresetIds
        ProfileType.KIDS -> KidsProfileAvatarPresetIds
        ProfileType.NORMAL -> ProfileAvatarPresetIds
    }

    TvDialogSurface(
        title = "Modifier la photo de profil",
        onDismiss = onDismiss,
        width = 560.dp,
        icon = Icons.Default.Person,
    ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                avatarPresets.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { presetId ->
                            ProfileAvatarPresetButton(
                                avatarId = presetId,
                                profileType = profileType,
                                selected = selectedAvatarId == presetId,
                                onClick = {
                                    selectedAvatarId = presetId
                                    runCatching { saveFocusRequester.requestFocus() }
                                },
                                modifier = Modifier.size(70.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TvButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier.height(42.dp),
                )
                Spacer(Modifier.width(10.dp))
                TvButton(
                    text = "Enregistrer",
                    onClick = { onSave(selectedAvatarId) },
                    focusRequester = saveFocusRequester,
                    modifier = Modifier.height(42.dp),
                )
            }
    }
}

@Composable
private fun ProfileAvatarPresetButton(
    avatarId: String,
    profileType: ProfileType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(48.dp),
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(
                BorderStroke(
                    if (focused || selected) 2.dp else 1.dp,
                    when {
                        focused -> SmartVisionColors.CyanAccent
                        selected -> Color.White
                        else -> Color.White.copy(alpha = 0.20f)
                    },
                ),
                CircleShape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        ProfileAvatarImage(
            avatarId = avatarId,
            profileType = profileType,
            modifier = Modifier
                .matchParentSize()
                .padding(3.dp),
        )
    }
}

@Composable
private fun ConfirmPlaylistProfileDeleteDialog(
    profile: PlaylistProfile,
    strings: SmartVisionStrings,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    TvConfirmationDialog(
        title = strings.profileDeleteTitle,
        itemLabel = profile.name,
        message = strings.profileDeleteMessage,
        confirmText = strings.delete,
        cancelText = strings.cancel,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
private fun ProfileEditTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    previousFocusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    password: Boolean = false,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val bringIntoViewScope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val focusStyle = LocalTvFocusStyle.current

    LaunchedEffect(editing) {
        if (editing) {
            keyboardController?.show()
        }
    }

    Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
    Spacer(Modifier.height(5.dp))
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = !editing,
        singleLine = true,
        textStyle = SmartVisionType.Body.copy(color = SmartVisionColors.TextPrimary),
        cursorBrush = SolidColor(focusStyle.accent),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .bringIntoViewRequester(bringIntoViewRequester)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                focused = focusState.isFocused
                if (focusState.isFocused) {
                    bringIntoViewScope.launch {
                        delay(120)
                        bringIntoViewRequester.bringIntoView()
                    }
                }
                if (!focusState.isFocused) {
                    editing = false
                    keyboardController?.hide()
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        editing = true
                        keyboardController?.show()
                        true
                    }
                    Key.Back -> {
                        if (editing) {
                            editing = false
                            keyboardController?.hide()
                            true
                        } else {
                            false
                        }
                    }
                    Key.DirectionDown -> {
                        editing = false
                        keyboardController?.hide()
                        runCatching { nextFocusRequester?.requestFocus() }
                        nextFocusRequester != null
                    }
                    Key.DirectionUp -> {
                        editing = false
                        keyboardController?.hide()
                        runCatching { previousFocusRequester?.requestFocus() }
                        previousFocusRequester != null
                    }
                    else -> false
                }
            }
            .background(
                if (focused || editing) focusStyle.background else SmartVisionColors.Surface,
                RoundedCornerShape(6.dp),
            )
            .border(
                BorderStroke(
                    if (focused) focusStyle.borderWidth else 1.dp,
                    if (editing || focused) focusStyle.accent else SmartVisionColors.Primary.copy(alpha = 0.72f),
                ),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isBlank()) {
                    Text(
                        text = label,
                        color = SmartVisionColors.TextSecondary.copy(alpha = 0.62f),
                        style = SmartVisionType.Body,
                    )
                }
                innerTextField()
            }
        },
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun DeviceCatalogInlineSection(
    state: ProfileUiState,
    syncStatus: SyncStatus,
    focusRequester: FocusRequester,
) {
    var focused by remember { mutableStateOf(false) }
    val focusStyle = LocalTvFocusStyle.current
    val shape = RoundedCornerShape(7.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clip(shape)
            .background(if (focused) focusStyle.background else SmartVisionColors.Surface.copy(alpha = 0.58f))
            .border(
                BorderStroke(
                    if (focused) focusStyle.borderWidth else 1.dp,
                    if (focused) focusStyle.accent else SmartVisionColors.Border.copy(alpha = 0.78f),
                ),
                shape,
            )
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Devices, contentDescription = null, tint = SmartVisionColors.CyanAccent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Appareil et catalogue", color = SmartVisionColors.TextPrimary, style = SmartVisionType.Label, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            DeviceCatalogSyncHeaderStatus(syncStatus)
        }
        Spacer(Modifier.height(12.dp))
        DeviceCatalogContent(state, syncStatus)
    }
}

@Composable
private fun DeviceCatalogContent(
    state: ProfileUiState,
    syncStatus: SyncStatus,
) {
    val catalogProfileName = state.playlistProfiles
        .firstOrNull { it.id == state.activePlaylistProfileId }
        ?.name
        ?.trim()
        ?.ifBlank { null }
        ?: "Profil actif"
    val progress = syncStatus.catalogProgressOrDefault(state.account)
    val showProgress = syncStatus is SyncStatus.Running || syncStatus is SyncStatus.Error
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        XtreamSyncCountCard(
            "Live TV",
            progress.live,
            showProgress,
            visualStyle = HomeVisualStyle.Signal,
            modifier = Modifier.weight(1f),
        )
        XtreamSyncCountCard(
            "Films",
            progress.movies,
            showProgress,
            enabled = state.activePlaylistSource == PlaylistSource.Xtream,
            visualStyle = HomeVisualStyle.Cinema,
            modifier = Modifier.weight(1f),
        )
        XtreamSyncCountCard(
            "Series",
            progress.series,
            showProgress,
            enabled = state.activePlaylistSource == PlaylistSource.Xtream,
            visualStyle = HomeVisualStyle.Series,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
        ProfileInfoRow("Code TV", state.tvCode, modifier = Modifier.weight(1f))
        ProfileInfoRow("Profil catalogue", catalogProfileName, modifier = Modifier.weight(1f))
    }
    if (state.errorMessage != null) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = state.errorMessage,
            color = SmartVisionColors.Error,
            style = SmartVisionType.Caption,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DeviceCatalogSyncHeaderStatus(syncStatus: SyncStatus) {
    when (syncStatus) {
        is SyncStatus.Running -> {
            Text(
                text = syncStatus.message,
                color = SmartVisionColors.CyanAccent,
                style = SmartVisionType.Caption,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        is SyncStatus.Success -> {
            Text(
                text = syncStatus.message,
                color = SmartVisionColors.Success,
                style = SmartVisionType.Caption,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        is SyncStatus.Error -> {
            Text(
                text = syncStatus.message,
                color = SmartVisionColors.Error,
                style = SmartVisionType.Caption,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        else -> Unit
    }
}

@Composable
private fun ProfilePanel(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xEA0B1526),
                        Color(0xF207101E),
                    ),
                ),
            )
            .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = SmartVisionColors.CyanAccent, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(9.dp))
            Text(title, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            trailingContent?.invoke()
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun ProfileMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = SmartVisionColors.TextPrimary,
) {
    Column(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(SmartVisionColors.Surface.copy(alpha = 0.72f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.78f)), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            color = accent,
            style = SmartVisionType.Label,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun UsageStep(
    title: String,
    text: String,
    active: Boolean,
    color: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(if (active) color.copy(alpha = 0.16f) else SmartVisionColors.Surface.copy(alpha = 0.42f))
            .border(BorderStroke(1.dp, if (active) color.copy(alpha = 0.68f) else SmartVisionColors.Border), RoundedCornerShape(7.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = if (active) Icons.Default.CheckCircle else Icons.Default.CreditCard,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Label, fontWeight = FontWeight.Bold)
            Text(text, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
    Spacer(Modifier.height(9.dp))
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.18f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.72f)), RoundedCornerShape(100.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = color, style = SmartVisionType.Caption, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExpirationPill(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(SmartVisionColors.Primary.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, SmartVisionColors.CyanAccent.copy(alpha = 0.62f)), RoundedCornerShape(100.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = SmartVisionColors.CyanAccent,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SmartVisionQrDialog(
    title: String,
    subtitle: String,
    qrUrl: String,
    tvCode: String = "",
    code: String? = null,
    loading: Boolean = false,
    error: String? = null,
    width: androidx.compose.ui.unit.Dp = 760.dp,
    licenseCode: String = "",
    onLicenseCodeChange: ((String) -> Unit)? = null,
    onSubmitLicenseCode: (() -> Unit)? = null,
    submittingLicense: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    if (onLicenseCodeChange != null && onSubmitLicenseCode != null) {
        PremiumLicenseDialog(
            qrUrl = qrUrl,
            tvCode = tvCode,
            loading = loading,
            error = error,
            licenseCode = licenseCode,
            onLicenseCodeChange = onLicenseCodeChange,
            onSubmitLicenseCode = onSubmitLicenseCode,
            submittingLicense = submittingLicense,
            onDismiss = onDismiss,
        )
        return
    }

    PremiumQrOnlyDialog(
        title = title,
        subtitle = subtitle,
        qrUrl = qrUrl,
        tvCode = tvCode,
        code = code,
        loading = loading,
        error = error,
        width = width,
        actionLabel = actionLabel,
        onAction = onAction,
        onDismiss = onDismiss,
    )
}

@Composable
private fun PremiumQrOnlyDialog(
    title: String,
    subtitle: String,
    qrUrl: String,
    tvCode: String,
    code: String?,
    loading: Boolean,
    error: String?,
    width: androidx.compose.ui.unit.Dp,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(ProfileFocusRequestDelayMillis)
        runCatching { closeFocusRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC010711))
                .padding(horizontal = 40.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(width)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF102542),
                                Color(0xFF071426),
                                Color(0xFF030A15),
                            ),
                            radius = 1100f,
                        ),
                    )
                    .border(
                        BorderStroke(1.dp, Color(0xFF2A67A7).copy(alpha = 0.86f)),
                        RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PremiumQrCard(
                        qrUrl = qrUrl,
                        tvCode = tvCode,
                        loading = loading,
                        modifier = Modifier
                            .width(336.dp)
                            .height(318.dp),
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(308.dp)
                            .background(Color(0xFF1D3553).copy(alpha = 0.72f)),
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 14.dp, end = 12.dp),
                    ) {
                        Text(
                            text = title,
                            color = SmartVisionColors.TextPrimary,
                            style = SmartVisionType.TitleM,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = subtitle,
                            color = SmartVisionColors.TextSecondary,
                            style = SmartVisionType.Body,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (error != null) {
                            Spacer(Modifier.height(12.dp))
                            Text(error, color = SmartVisionColors.Error, style = SmartVisionType.Label)
                        }
                        Spacer(Modifier.height(22.dp))
                        if (actionLabel != null && onAction != null) {
                            PremiumDialogButton(
                                text = actionLabel,
                                onClick = onAction,
                                primary = true,
                                trailingIcon = Icons.Default.ArrowForward,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        PremiumDialogButton(
                            text = "Fermer",
                            onClick = onDismiss,
                            primary = false,
                            focusRequester = closeFocusRequester,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                        )
                    }
                }

                PremiumCloseButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}

@Composable
private fun PremiumLicenseDialog(
    qrUrl: String,
    tvCode: String,
    loading: Boolean,
    error: String?,
    licenseCode: String,
    onLicenseCodeChange: (String) -> Unit,
    onSubmitLicenseCode: () -> Unit,
    submittingLicense: Boolean,
    onDismiss: () -> Unit,
) {
    val fieldFocusRequester = remember { FocusRequester() }
    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var editing by remember { mutableStateOf(false) }
    var fieldFocused by remember { mutableStateOf(false) }
    val focusStyle = LocalTvFocusStyle.current

    LaunchedEffect(Unit) {
        delay(ProfileFocusRequestDelayMillis)
        runCatching { fieldFocusRequester.requestFocus() }
    }

    LaunchedEffect(editing) {
        if (editing) {
            delay(ProfileFocusRequestDelayMillis)
            runCatching { inputFocusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC010711))
                .padding(horizontal = 40.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(760.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF102542),
                                Color(0xFF071426),
                                Color(0xFF030A15),
                            ),
                            radius = 1100f,
                        ),
                    )
                    .border(
                        BorderStroke(1.dp, Color(0xFF2A67A7).copy(alpha = 0.86f)),
                        RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PremiumQrCard(
                        qrUrl = qrUrl,
                        tvCode = tvCode,
                        loading = loading,
                        modifier = Modifier
                            .width(336.dp)
                            .height(318.dp),
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(308.dp)
                            .background(Color(0xFF1D3553).copy(alpha = 0.72f)),
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 16.dp, end = 12.dp),
                    ) {
                        Text(
                            text = "Passer à",
                            color = SmartVisionColors.TextPrimary,
                            style = SmartVisionType.TitleM,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "SmartVision Premium",
                            color = Color(0xFF1687FF),
                            style = SmartVisionType.TitleM,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Débloquez toutes les fonctionnalités premium\net profitez d’une expérience IPTV sans limites.",
                            color = SmartVisionColors.TextSecondary,
                            style = SmartVisionType.Label,
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF1687FF),
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Code licence SmartVision",
                                color = Color(0xFF1687FF),
                                style = SmartVisionType.Label,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .focusRequester(fieldFocusRequester)
                                .onFocusChanged { fieldFocused = it.isFocused }
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    when (event.key) {
                                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                            editing = true
                                            true
                                        }
                                        Key.Back -> {
                                            if (editing) {
                                                editing = false
                                                keyboardController?.hide()
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                        else -> false
                                    }
                                }
                                .focusable(enabled = !editing)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (fieldFocused || editing) {
                                        focusStyle.background
                                    } else {
                                        Color(0xFF030B18).copy(alpha = 0.92f)
                                    },
                                )
                                .border(
                                    BorderStroke(
                                        if (fieldFocused || editing) focusStyle.borderWidth else 1.dp,
                                        if (fieldFocused || editing) {
                                            focusStyle.accent
                                        } else {
                                            SmartVisionColors.Primary
                                        },
                                    ),
                                    RoundedCornerShape(10.dp),
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BasicTextField(
                                value = licenseCode,
                                onValueChange = { onLicenseCodeChange(it.uppercase().take(24)) },
                                enabled = editing,
                                singleLine = true,
                                textStyle = SmartVisionType.Body.copy(
                                    color = SmartVisionColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                cursorBrush = SolidColor(focusStyle.accent),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(inputFocusRequester)
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused && editing) {
                                            editing = false
                                        }
                                    },
                                decorationBox = { inner ->
                                    if (licenseCode.isBlank()) {
                                        Text(
                                            text = "Saisir le code",
                                            color = SmartVisionColors.TextSecondary.copy(alpha = 0.62f),
                                            style = SmartVisionType.Body,
                                        )
                                    }
                                    inner()
                                },
                            )
                        }
                        if (!error.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = error,
                                color = SmartVisionColors.Error,
                                style = SmartVisionType.Label,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        PremiumDialogButton(
                            text = if (submittingLicense) "Activation..." else "Activer ce code",
                            onClick = onSubmitLicenseCode,
                            enabled = !submittingLicense && licenseCode.isNotBlank(),
                            primary = true,
                            trailingIcon = Icons.Default.ArrowForward,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        PremiumDialogButton(
                            text = "Fermer",
                            onClick = onDismiss,
                            primary = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                        )
                    }
                }

                PremiumCloseButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}

@Composable
private fun PremiumQrCard(
    qrUrl: String,
    tvCode: String,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.smartvision_logo_wide),
            contentDescription = "SmartVision IPTV Player",
            modifier = Modifier
                .width(214.dp)
                .height(52.dp),
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .size(208.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                loading -> CircularProgressIndicator(color = SmartVisionColors.Primary)
                qrUrl.isBlank() -> Icon(
                    Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = Color(0xFF10203A),
                    modifier = Modifier.size(72.dp),
                )
                else -> {
                    val bitmap = remember(qrUrl) { createQrBitmap(qrUrl, 512) }
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR code SmartVision",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = null,
                tint = Color(0xFF1687FF),
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "CODE TV : ${tvCode.ifBlank { "GENERATION" }}",
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
private fun PremiumDialogButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    trailingIcon: ImageVector? = null,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(10.dp)
    val focusStyle = LocalTvFocusStyle.current
    val borderColor = when {
        focusState.isFocused -> focusStyle.accent
        primary -> SmartVisionColors.CyanAccent
        else -> Color(0xFF344761)
    }

    Row(
        modifier = modifier
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                enabled = enabled,
                glowColor = focusStyle.accent,
                cornerRadius = 10.dp,
            )
            .clip(shape)
            .background(
                if (primary) {
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF0EBEFF),
                            Color(0xFF0876FF),
                            Color(0xFF153DFF),
                        ),
                    )
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF0B172A), Color(0xFF07101D)))
                },
            )
            .border(BorderStroke(if (focusState.isFocused) focusStyle.borderWidth else 1.dp, borderColor), shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.42f),
            style = SmartVisionType.Body,
            fontWeight = FontWeight.SemiBold,
        )
        if (trailingIcon != null) {
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = if (enabled) Color.White else Color.White.copy(alpha = 0.42f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun PremiumCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(12.dp)
    val focusStyle = LocalTvFocusStyle.current

    Box(
        modifier = modifier
            .size(42.dp)
            .tvFocusTarget(
                state = focusState,
                glowColor = focusStyle.accent,
                cornerRadius = 12.dp,
            )
            .clip(shape)
            .background(Color(0xFF0B1728))
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else 1.dp,
                    if (focusState.isFocused) focusStyle.accent else Color(0xFF263B56),
                ),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Fermer",
            tint = SmartVisionColors.TextSecondary,
            modifier = Modifier.size(24.dp),
        )
    }
}

internal fun String.masked(): String =
    when {
        length <= 2 -> "***"
        length <= 5 -> take(1) + "***"
        else -> take(2) + "****" + takeLast(2)
    }

private fun PlaylistProfile.toXtreamAccountOrNull(): XtreamAccount? {
    if (xtreamHost.isBlank() && xtreamUsername.isBlank() && xtreamPassword.isBlank()) return null
    return XtreamAccount(
        id = id,
        name = "Compte SmartVision",
        host = xtreamHost,
        username = xtreamUsername,
        password = xtreamPassword,
        epgUrl = epgUrl,
    )
}

private fun PlaylistSource.displayLabel(): String =
    when (this) {
        PlaylistSource.Xtream -> "Xtream Codes"
        PlaylistSource.M3u -> "Playlist M3U"
    }

private fun PlaylistProfileStatus.displayLabel(): String =
    when (this) {
        PlaylistProfileStatus.Active -> "Actif"
        PlaylistProfileStatus.Inactive -> "Inactif"
        PlaylistProfileStatus.Error -> "Erreur"
        PlaylistProfileStatus.NotConfigured -> "Non configure"
    }

private fun String.looksLikeHttpUrl(): Boolean {
    val value = trim()
    return value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)
}

private fun String.looksLikeUrlHost(): Boolean {
    val value = trim()
    return value.isNotBlank() && (value.looksLikeHttpUrl() || "." in value)
}

private fun Long?.asProfileDate(): String =
    this?.let { timestamp ->
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    } ?: "Jamais"

private fun String.safeServerHost(fallback: String): String =
    runCatching { java.net.URI(trim()).host }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: fallback

internal fun activationBaseUrl(): String =
    BuildConfig.ACTIVATION_BASE_URL.ifBlank { "https://smartvisions.net/" }
        .trim()
        .trimEnd('/') + "/"

internal data class ProfileDeviceDisplay(
    val deviceId: String,
    val publicDeviceCode: String,
)

internal suspend fun ActivationRepository.currentDisplayDevice(): ProfileDeviceDisplay {
    val state = localState.first()
    val publicCode = state.publicDeviceCode.ifBlank { getOrCreateLocalPublicCode() }
    val deviceId = state.deviceId.ifBlank { getOrCreateDeviceId() }
    return ProfileDeviceDisplay(deviceId = deviceId, publicDeviceCode = publicCode)
}

internal fun tvDeviceQuery(publicDeviceCode: String, deviceId: String): String =
    if (publicDeviceCode.isNotBlank()) {
        "device=$publicDeviceCode"
    } else {
        "device_id=$deviceId"
    }

internal fun Throwable.userMessage(defaultMessage: String): String =
    when (this) {
        is ActivationException -> message ?: defaultMessage
        else -> defaultMessage
    }

private fun createQrBitmap(content: String, size: Int): Bitmap {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1,
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        val offset = y * size
        for (x in 0 until size) {
            pixels[offset + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}

private const val ProfileFocusRequestDelayMillis = 80L
