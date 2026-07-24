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
import androidx.compose.runtime.key
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
import com.smartvision.svplayer.domain.profile.ContentPrefixPolicy
import com.smartvision.svplayer.domain.access.PremiumFeatureGateResult
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.NumericPinDialog
import com.smartvision.svplayer.ui.components.PremiumPreviewQr
import com.smartvision.svplayer.ui.components.TvConfirmationDialog
import com.smartvision.svplayer.ui.components.TvDialogSurface
import com.smartvision.svplayer.ui.activation.ScaledActivationLayout
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
    onSynchronizeOnHome: () -> Unit,
    onActivationChanged: () -> Unit,
    startDestination: ProfileAreaDestination = ProfileAreaDestination.INFO,
    onOpenInfo: () -> Unit,
    onOpenManage: () -> Unit,
    onRequestGlobalProfilePicker: () -> Unit,
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
        synchronizationDue = activeProfile?.let {
            !container.accountManager.isCatalogCurrent(it)
        } == true || SyncFrequencyPolicy.isSynchronizationDue(
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
        onRequestGlobalProfilePicker = onRequestGlobalProfilePicker,
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
                container.catalogRepository.synchronize(profileId)
            }
        },
        onSynchronizeOnHome = onSynchronizeOnHome,
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
            strings = strings,
            title = if (qr.allowsLicenseEntry) strings.premiumPurchaseTitle else qr.title,
            subtitle = if (qr.allowsLicenseEntry) strings.premiumPurchaseSubtitle else qr.subtitle,
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
