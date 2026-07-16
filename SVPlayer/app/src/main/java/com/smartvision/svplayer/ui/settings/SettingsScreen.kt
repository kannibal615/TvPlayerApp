package com.smartvision.svplayer.ui.settings

import androidx.activity.compose.BackHandler
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.svplayer.R
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.core.config.XtreamAccount
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.data.network.NetworkActivityItem
import com.smartvision.svplayer.data.network.NetworkActivitySnapshot
import com.smartvision.svplayer.data.network.NetworkActivityStatus
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.TvSectionCard
import com.smartvision.svplayer.ui.components.TvConfirmationDialog
import com.smartvision.svplayer.ui.components.TvDialogSurface
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.TvFocusStyles
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.home.TvHeader
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.i18n.smartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import com.smartvision.svplayer.ui.update.AppUpdateUiState
import java.util.UUID
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.ui.profile.ProfileViewModel
import com.smartvision.svplayer.ui.profile.LicensePanel
import com.smartvision.svplayer.ui.profile.SmartVisionQrDialog

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
    onLicenseKey: () -> Unit,
    showLicenseKey: Boolean,
    hasNewNotifications: Boolean,
    notificationBadgeCount: Int,
    updateState: AppUpdateUiState,
    onCheckForUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val accounts by container.accountManager.accounts.collectAsStateWithLifecycle()
    val activeAccountId by container.accountManager.activeAccountId.collectAsStateWithLifecycle()
    val activeAccount = accounts.firstOrNull { it.id == activeAccountId } ?: accounts.firstOrNull()
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = com.smartvision.svplayer.domain.model.PlayerSettings(),
    )
    val networkSnapshot by container.networkActivityTracker.snapshot.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var selectedSection by remember { mutableStateOf(SettingsSection.License) }
    var showClearLocalDataConfirmation by remember { mutableStateOf(false) }
    val strings = smartVisionStrings(settings.language)
    val lastUpdateLabel = remember(context) { context.smartVisionLastUpdateLabel() }
    val homeTabFocusRequester = remember { FocusRequester() }
    val firstMenuFocusRequester = remember { FocusRequester() }
    val licenseViewModel: ProfileViewModel = viewModel(
        factory = viewModelFactory {
            ProfileViewModel(
                activationRepository = container.activationRepository,
                accountManager = container.accountManager,
                catalogRepository = container.catalogRepository,
            )
        },
    )
    val licenseState by licenseViewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        delay(180)
        runCatching { firstMenuFocusRequester.requestFocus() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        SmartVisionColors.PrimaryDark.copy(alpha = 0.4f),
                        SmartVisionColors.Background,
                        Color(0xFF01040C),
                    ),
                    radius = 1500f,
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
            onProfile = onProfile,
            onNotifications = onNotifications,
            onLicenseKey = onLicenseKey,
            showLicenseKey = showLicenseKey,
            hasNewNotifications = hasNewNotifications,
            notificationBadgeCount = notificationBadgeCount,
            homeTabFocusRequester = homeTabFocusRequester,
            contentDownFocusRequester = firstMenuFocusRequester,
            onContentDown = { runCatching { firstMenuFocusRequester.requestFocus() } },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(SmartVisionDimensions.AppHeaderContentSpacing))

        SettingsMenuLayout(
            selectedSection = selectedSection,
            onSectionSelected = { selectedSection = it },
            firstMenuFocusRequester = firstMenuFocusRequester,
            headerFocusRequester = homeTabFocusRequester,
            settings = settings,
            accountsCount = accounts.size,
            activeAccount = activeAccount,
            networkSnapshot = networkSnapshot,
            updateState = updateState,
            onCheckForUpdate = onCheckForUpdate,
            onSetLanguage = { value -> scope.launch { container.settingsRepository.setLanguage(value) } },
            onSetFocusStyle = { value -> scope.launch { container.settingsRepository.setFocusStyle(value) } },
            onSetFocusColor = { value -> scope.launch { container.settingsRepository.setFocusColor(value) } },
            onSetFocusEffect = { value -> scope.launch { container.settingsRepository.setFocusEffect(value) } },
            onSetFocusBackground = { value -> scope.launch { container.settingsRepository.setFocusBackground(value) } },
            onSetFocusSelectedColor = { value -> scope.launch { container.settingsRepository.setFocusSelectedColor(value) } },
            onSetFocusActiveColor = { value -> scope.launch { container.settingsRepository.setFocusActiveColor(value) } },
            onSetFocusParentColor = { value -> scope.launch { container.settingsRepository.setFocusParentColor(value) } },
            onSetVideoRatio = { value -> scope.launch { container.settingsRepository.setVideoRatio(value) } },
            onSetAnimations = { value -> scope.launch { container.settingsRepository.setAnimationsEnabled(value) } },
            onSetRetry = { value -> scope.launch { container.settingsRepository.setRetryEnabled(value) } },
            onClearLocalData = { showClearLocalDataConfirmation = true },
            licenseState = licenseState,
            onRefreshLicense = licenseViewModel::refresh,
            onShowLicenseQr = licenseViewModel::showLicenseQr,
            strings = strings,
            lastUpdateLabel = lastUpdateLabel,
            modifier = Modifier.fillMaxSize(),
        )
    }

    licenseState.qrDialog?.let { qr ->
        SmartVisionQrDialog(
            title = qr.title,
            subtitle = qr.subtitle,
            qrUrl = qr.url,
            tvCode = licenseState.tvCode,
            code = qr.code,
            loading = qr.loading,
            error = qr.error ?: licenseState.errorMessage,
            licenseCode = if (qr.allowsLicenseEntry) licenseState.licenseCode else "",
            onLicenseCodeChange = if (qr.allowsLicenseEntry) licenseViewModel::updateLicenseCode else null,
            onSubmitLicenseCode = if (qr.allowsLicenseEntry) licenseViewModel::activateLicense else null,
            submittingLicense = licenseState.submittingLicense,
            onDismiss = licenseViewModel::dismissQr,
        )
    }

    if (showClearLocalDataConfirmation) {
        TvConfirmationDialog(
            title = strings.clearLocalDataConfirmationTitle,
            message = strings.clearLocalDataConfirmationMessage,
            confirmText = strings.delete,
            cancelText = strings.cancel,
            onDismiss = { showClearLocalDataConfirmation = false },
            onConfirm = {
                showClearLocalDataConfirmation = false
                scope.launch {
                    container.settingsRepository.clearLocalData()
                    container.xtreamRepository.clearCaches()
                    container.catalogRepository.clearCatalogForProfileSwitch()
                }
            },
        )
    }
}

@Composable
private fun SettingsMenuLayout(
    selectedSection: SettingsSection,
    onSectionSelected: (SettingsSection) -> Unit,
    firstMenuFocusRequester: FocusRequester,
    headerFocusRequester: FocusRequester,
    settings: PlayerSettings,
    accountsCount: Int,
    activeAccount: XtreamAccount?,
    networkSnapshot: NetworkActivitySnapshot,
    updateState: AppUpdateUiState,
    onCheckForUpdate: () -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetFocusStyle: (String) -> Unit,
    onSetFocusColor: (String) -> Unit,
    onSetFocusEffect: (String) -> Unit,
    onSetFocusBackground: (String) -> Unit,
    onSetFocusSelectedColor: (String) -> Unit,
    onSetFocusActiveColor: (String) -> Unit,
    onSetFocusParentColor: (String) -> Unit,
    onSetVideoRatio: (String) -> Unit,
    onSetAnimations: (Boolean) -> Unit,
    onSetRetry: (Boolean) -> Unit,
    onClearLocalData: () -> Unit,
    licenseState: com.smartvision.svplayer.ui.profile.ProfileUiState,
    onRefreshLicense: () -> Unit,
    onShowLicenseQr: () -> Unit,
    strings: SmartVisionStrings,
    lastUpdateLabel: String,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LazyColumn(
            modifier = Modifier
                .width(250.dp)
                .fillMaxHeight()
                .background(Color(0xD9091424), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(SettingsSection.entries, key = { it.name }) { section ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    TvButton(
                        text = section.label(strings),
                        leadingIcon = section.icon,
                        selected = selectedSection == section,
                        variant = if (selectedSection == section) TvButtonVariant.Primary else TvButtonVariant.Text,
                        onClick = { onSectionSelected(section) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .then(
                                if (section == SettingsSection.License) {
                                    Modifier.onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                                            runCatching { headerFocusRequester.requestFocus() }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                } else {
                                    Modifier
                                },
                            )
                            .then(
                                if (section == SettingsSection.License) {
                                    Modifier.focusProperties { up = headerFocusRequester }
                                } else {
                                    Modifier
                                },
                            )
                            .then(if (section == SettingsSection.License) Modifier.focusRequester(firstMenuFocusRequester) else Modifier),
                    )
                }
            }
        }

        SettingsPanel(
            title = selectedSection.label(strings),
            icon = selectedSection.icon,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentFocusable = selectedSection == SettingsSection.Network,
        ) {
            when (selectedSection) {
                SettingsSection.License -> {
                    LicensePanel(
                        state = licenseState,
                        onRefresh = onRefreshLicense,
                        onShowLicenseQr = onShowLicenseQr,
                        onShowPrivacyOptions = {},
                        privacyOptionsRequired = false,
                        embedded = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SettingsSection.Preferences -> {
                    TvSectionCard(strings.language, Icons.Default.Settings, Modifier.fillMaxWidth()) {
                        SettingsChoice(
                            label = strings.language,
                            values = listOf(
                                SettingsOption("English", strings.english),
                                SettingsOption("Francais", strings.french),
                            ),
                            selected = settings.language,
                            onSelected = onSetLanguage,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    TvSectionCard(strings.videoFormat, Icons.Default.Settings, Modifier.fillMaxWidth()) {
                        SettingsChoice(
                            label = strings.videoFormat,
                            values = settingsOptions("Fit", "Fill", "Zoom"),
                            selected = settings.videoRatio,
                            onSelected = onSetVideoRatio,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    TvSectionCard(strings.animations, Icons.Default.Settings, Modifier.fillMaxWidth()) {
                        SettingsChoice(
                            label = strings.animations,
                            values = listOf(
                                SettingsOption("enabled", strings.enabled),
                                SettingsOption("reduced", strings.reduced),
                            ),
                            selected = if (settings.animationsEnabled) "enabled" else "reduced",
                            onSelected = { value -> onSetAnimations(value == "enabled") },
                        )
                        SettingsChoice(
                            label = strings.automaticReconnect,
                            values = listOf(
                                SettingsOption("enabled", strings.enabled),
                                SettingsOption("disabled", strings.disabled),
                            ),
                            selected = if (settings.retryEnabled) "enabled" else "disabled",
                            onSelected = { value -> onSetRetry(value == "enabled") },
                        )
                    }
                }
                SettingsSection.Network -> {
                    NetworkActivityPanel(
                        snapshot = networkSnapshot,
                        strings = strings,
                    )
                }
                SettingsSection.Tmdb -> {
                    TvSectionCard(strings.tmdbTokenStatus, Icons.Default.Settings, Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.tmdbAttributionSubtitle,
                            color = SmartVisionColors.TextSecondary,
                            style = SmartVisionType.Body,
                        )
                        Spacer(Modifier.height(14.dp))
                        SettingsInfoRow(
                            label = strings.tmdbTokenStatus,
                            value = if (BuildConfig.TMDB_READ_ACCESS_TOKEN.isNotBlank()) strings.active else strings.notConfigured,
                        )
                        SettingsInfoRow(strings.language, settings.language)
                    }
                    Spacer(Modifier.height(10.dp))
                    TvSectionCard(strings.tmdbAttribution, Icons.Default.Info, Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.tmdbAttributionBody,
                            color = SmartVisionColors.TextSecondary,
                            style = SmartVisionType.Body,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = strings.tmdbProvidersBody,
                            color = SmartVisionColors.TextSecondary,
                            style = SmartVisionType.Body,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = strings.tmdbLicenseNote,
                            color = SmartVisionColors.TextSecondary,
                            style = SmartVisionType.Caption,
                        )
                    }
                }
                SettingsSection.Personalization -> {
                    TvSectionCard(strings.focusStyle, Icons.Default.Settings, Modifier.fillMaxWidth()) {
                        SettingsChoice(
                        label = strings.focusStyle,
                        values = listOf(
                            SettingsOption(TvFocusStyles.Default.key, strings.focusDefault),
                            SettingsOption(TvFocusStyles.Soft.key, strings.focusSoft),
                            SettingsOption(TvFocusStyles.Compact.key, strings.focusCompact),
                        ),
                        selected = settings.focusStyle,
                        onSelected = onSetFocusStyle,
                    )
                        SettingsChoice(
                        label = strings.focusColor,
                        values = listOf(
                            SettingsOption("White", strings.focusWhite),
                            SettingsOption("CyanNeon", strings.focusCyanNeon),
                            SettingsOption("ElectricBlue", strings.focusElectricBlue),
                        ),
                        selected = settings.focusColor,
                        onSelected = onSetFocusColor,
                    )
                        SettingsChoice(
                        label = strings.focusEffect,
                        values = listOf(
                            SettingsOption("Frame", strings.focusFrame),
                            SettingsOption("NeonGlow", strings.focusNeonGlow),
                            SettingsOption("GoldSweep", strings.focusGoldSweep),
                        ),
                        selected = settings.focusEffect,
                        onSelected = onSetFocusEffect,
                    )
                        SettingsChoice(
                        label = strings.focusBackground,
                        values = listOf(
                            SettingsOption("BlueTransparent", strings.focusBackgroundBlue),
                            SettingsOption("GoldTransparent", strings.focusBackgroundGold),
                            SettingsOption("WhiteTransparent", strings.focusBackgroundWhite),
                        ),
                        selected = settings.focusBackground,
                        onSelected = onSetFocusBackground,
                    )
                    }
                    Spacer(Modifier.height(10.dp))
                    TvSectionCard(strings.focusSelectedElement, Icons.Default.CheckCircle, Modifier.fillMaxWidth()) {
                        SettingsChoice(
                        label = strings.focusSelectedElement,
                        values = focusRoleColorOptions(strings),
                        selected = settings.focusSelectedColor,
                        onSelected = onSetFocusSelectedColor,
                    )
                        SettingsChoice(
                        label = strings.focusActiveElement,
                        values = focusRoleColorOptions(strings),
                        selected = settings.focusActiveColor,
                        onSelected = onSetFocusActiveColor,
                    )
                        SettingsChoice(
                        label = strings.focusParentElement,
                        values = focusRoleColorOptions(strings),
                        selected = settings.focusParentColor,
                        onSelected = onSetFocusParentColor,
                    )
                    }
                    Spacer(Modifier.height(10.dp))
                    TvSectionCard(strings.focusLivePreview, Icons.Default.Info, Modifier.fillMaxWidth()) {
                        FocusStatePreview(strings = strings)
                    }
                }
                SettingsSection.Updates -> {
                    TvSectionCard(strings.installedVersion, Icons.Default.Info, Modifier.fillMaxWidth()) {
                        SettingsInfoRow(strings.installedVersion, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        SettingsInfoRow(strings.lastUpdate, lastUpdateLabel)
                        SettingsInfoRow(strings.portal, BuildConfig.ACTIVATION_BASE_URL.removeSuffix("/"))
                    }
                    Spacer(Modifier.height(10.dp))
                    TvSectionCard(strings.checkForUpdate, Icons.Default.Refresh, Modifier.fillMaxWidth()) {
                        TvButton(
                            text = if (updateState.checking) strings.checking else strings.checkForUpdate,
                            leadingIcon = Icons.Default.Refresh,
                            onClick = onCheckForUpdate,
                            enabled = !updateState.checking && !updateState.installing,
                            variant = TvButtonVariant.Secondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                        )
                        updateState.errorMessage?.let { message ->
                            Spacer(Modifier.height(8.dp))
                            Text(text = message, color = SmartVisionColors.Error, style = SmartVisionType.Caption, maxLines = 2)
                        }
                        if (updateState.checkedOnce && updateState.update == null && updateState.errorMessage == null && !updateState.checking) {
                            Spacer(Modifier.height(8.dp))
                            Text(text = strings.appUpToDate, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
                        }
                    }
                }
                SettingsSection.Data -> {
                    TvSectionCard(strings.localData, Icons.Default.Info, Modifier.fillMaxWidth()) {
                        SettingsInfoRow(strings.bufferMode, settings.bufferMode)
                        SettingsInfoRow(strings.localXtreamAccounts, accountsCount.toString())
                        SettingsInfoRow(strings.activeAccount, activeAccount?.name ?: strings.none)
                        Spacer(Modifier.height(10.dp))
                        Text(text = strings.localCredentialsInfo, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Body)
                    }
                    Spacer(Modifier.height(10.dp))
                    TvSectionCard(strings.clearLocalData, Icons.Default.Delete, Modifier.fillMaxWidth()) {
                        TvButton(
                            text = strings.clearLocalData,
                            leadingIcon = Icons.Default.Delete,
                            onClick = onClearLocalData,
                            variant = TvButtonVariant.Danger,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                        )
                    }
                }
            }
        }
    }
}

private enum class SettingsSection(
    val icon: ImageVector,
) {
    License(Icons.Default.Verified),
    Preferences(Icons.Default.Settings),
    Network(Icons.Default.CloudSync),
    Tmdb(Icons.Default.Info),
    Personalization(Icons.Default.Settings),
    Updates(Icons.Default.Refresh),
    Data(Icons.Default.Delete),
}

private fun SettingsSection.label(strings: SmartVisionStrings): String = when (this) {
    SettingsSection.License -> "Licence SmartVision"
    SettingsSection.Preferences -> strings.generalPreferences
    SettingsSection.Network -> strings.networkActivity
    SettingsSection.Tmdb -> strings.tmdbAttribution
    SettingsSection.Personalization -> strings.personalization
    SettingsSection.Updates -> strings.updates
    SettingsSection.Data -> strings.localData
}

private data class SettingsOption(
    val value: String,
    val label: String = value,
)

private fun focusRoleColorOptions(strings: SmartVisionStrings): List<SettingsOption> = listOf(
    SettingsOption("White", strings.focusWhite),
    SettingsOption("CyanNeon", strings.focusCyanNeon),
    SettingsOption("ElectricBlue", strings.focusElectricBlue),
    SettingsOption("Gold", strings.focusGold),
)

@Composable
private fun PersonalizationSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        color = SmartVisionColors.CyanAccent,
        style = SmartVisionType.Caption,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun FocusStatePreview(strings: SmartVisionStrings) {
    val style = LocalTvFocusStyle.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF081526), RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
            .padding(14.dp),
    ) {
        Text(
            text = strings.focusLivePreview.uppercase(),
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            FocusPreviewRoleCard(
                label = strings.focusFocusedElement,
                accent = style.accent,
                background = style.background,
                modifier = Modifier.weight(1f),
            )
            FocusPreviewRoleCard(
                label = strings.focusSelectedElement,
                accent = style.selectedAccent,
                background = style.selectedBackground,
                modifier = Modifier.weight(1f),
            )
            FocusPreviewRoleCard(
                label = strings.focusActiveElement,
                accent = style.activeAccent,
                background = style.activeBackground,
                modifier = Modifier.weight(1f),
            )
            FocusPreviewRoleCard(
                label = strings.focusParentElement,
                accent = style.parentAccent,
                background = style.parentBackground,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FocusPreviewRoleCard(
    label: String,
    accent: Color,
    background: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .background(background, RoundedCornerShape(7.dp))
            .border(BorderStroke(2.dp, accent), RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

private fun settingsOptions(vararg values: String): List<SettingsOption> = values.map { SettingsOption(it) }

private fun Context.smartVisionLastUpdateLabel(): String {
    return runCatching {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
        val updatedAt = Date(packageInfo.lastUpdateTime)
        DateFormat.getDateFormat(this).format(updatedAt) + " " + DateFormat.getTimeFormat(this).format(updatedAt)
    }.getOrDefault("-")
}

@Composable
private fun NetworkActivityPanel(
    snapshot: NetworkActivitySnapshot,
    strings: SmartVisionStrings,
) {
    TvSectionCard(strings.networkActivity, Icons.Default.CloudSync, Modifier.fillMaxWidth()) {
        Text(
            text = strings.networkActivitySubtitle,
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Body,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NetworkMetricCard(strings.networkActive, snapshot.activeCount.toString(), Modifier.weight(1f))
            NetworkMetricCard(strings.networkThroughput, snapshot.bytesPerSecond.formatByteRate(), Modifier.weight(1f))
            NetworkMetricCard(strings.networkErrors, snapshot.errorCount.toString(), Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(10.dp))

    TvSectionCard(strings.networkActive, Icons.Default.CloudSync, Modifier.fillMaxWidth()) {
        if (snapshot.active.isEmpty()) {
            Text(strings.networkNoActivity, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                snapshot.active.take(NetworkActivityVisibleLimit).forEach { item ->
                    NetworkActivityRow(item = item, strings = strings)
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    TvSectionCard(strings.networkRecent, Icons.Default.Refresh, Modifier.fillMaxWidth()) {
        if (snapshot.recent.isEmpty()) {
            Text(strings.networkNoActivity, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                snapshot.recent.take(NetworkActivityVisibleLimit).forEach { item ->
                    NetworkActivityRow(item = item, strings = strings)
                }
            }
        }
    }
}

@Composable
private fun NetworkMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(SmartVisionColors.Surface.copy(alpha = 0.72f), RoundedCornerShape(7.dp))
            .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Label, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun NetworkActivityRow(
    item: NetworkActivityItem,
    strings: SmartVisionStrings,
) {
    val statusColor = item.status.statusColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xB80D1828), RoundedCornerShape(7.dp))
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.82f)), RoundedCornerShape(7.dp))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.title,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Caption,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = item.status.name,
                color = statusColor,
                style = SmartVisionType.Caption,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        if (item.message.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(item.message, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
        }
        Spacer(Modifier.height(7.dp))
        NetworkProgressBar(item.progressPercent ?: 0, statusColor)
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            NetworkTinyInfo(strings.networkProgress, "${item.progressPercent ?: 0}%", Modifier.weight(1f))
            NetworkTinyInfo(strings.networkData, item.formatData(), Modifier.weight(1f))
            NetworkTinyInfo(strings.networkDuration, item.durationMs.formatDuration(), Modifier.weight(1f))
        }
        val section = item.section
        val source = item.source
        if (!section.isNullOrBlank() || !source.isNullOrBlank() || item.currentItems != null) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = listOfNotNull(
                    section?.let { "${strings.networkSection}: $it" },
                    source?.let { "${strings.networkSource}: $it" },
                    item.currentItems?.let { current ->
                        "${strings.networkItems}: " + item.totalItems?.let { "$current/$it" }.orEmpty().ifBlank { current.toString() }
                    },
                ).joinToString("  |  "),
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
                maxLines = 1,
            )
        }
        item.errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
            Spacer(Modifier.height(5.dp))
            Text(error, color = SmartVisionColors.Error, style = SmartVisionType.Caption, maxLines = 1)
        }
    }
}

@Composable
private fun NetworkTinyInfo(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
        Text(value, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Caption, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun NetworkProgressBar(percent: Int, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(50)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                .height(5.dp)
                .background(color, RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    icon: ImageVector,
    modifier: Modifier,
    trailing: @Composable (() -> Unit)? = null,
    contentFocusable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val scrollStep = 116
    Column(
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = SmartVisionColors.CyanAccent, modifier = Modifier.size(23.dp))
            Spacer(Modifier.width(9.dp))
            Text(
                text = title,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
        }
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xE60A1424), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
                .padding(10.dp),
        ) {
            val contentModifier = if (contentFocusable) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionDown -> {
                                scope.launch {
                                    scrollState.animateScrollTo(
                                        (scrollState.value + scrollStep).coerceAtMost(scrollState.maxValue),
                                    )
                                }
                                true
                            }
                            Key.DirectionUp -> {
                                scope.launch {
                                    scrollState.animateScrollTo(
                                        (scrollState.value - scrollStep).coerceAtLeast(0),
                                    )
                                }
                                true
                            }
                            else -> false
                        }
                    }
                    .focusable()
                    .verticalScroll(scrollState)
            } else {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
            }
            Column(
                modifier = contentModifier,
            ) {
                content()
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private const val NetworkActivityVisibleLimit = 10

private fun NetworkActivityStatus.statusColor(): Color =
    when (this) {
        NetworkActivityStatus.Queued -> SmartVisionColors.TextSecondary
        NetworkActivityStatus.Running -> SmartVisionColors.CyanAccent
        NetworkActivityStatus.Importing -> SmartVisionColors.Primary
        NetworkActivityStatus.Completed -> SmartVisionColors.Success
        NetworkActivityStatus.Error -> SmartVisionColors.Error
    }

private fun NetworkActivityItem.formatData(): String {
    val read = bytesRead
    val total = totalBytes
    return when {
        read != null && total != null -> "${read.formatBytes()}/${total.formatBytes()}"
        read != null -> read.formatBytes()
        currentItems != null && totalItems != null -> "$currentItems/$totalItems"
        currentItems != null -> currentItems.toString()
        else -> "-"
    }
}

private fun Long.formatByteRate(): String =
    if (this <= 0L) "-" else "${formatBytes()}/s"

private fun Long.formatBytes(): String =
    when {
        this >= 1024L * 1024L -> "${this / (1024L * 1024L)} MB"
        this >= 1024L -> "${this / 1024L} KB"
        else -> "$this B"
    }

private fun Long.formatDuration(): String =
    when {
        this < 1_000L -> "<1s"
        this < 60_000L -> "${this / 1_000L}s"
        else -> "${this / 60_000L}m ${(this % 60_000L) / 1_000L}s"
    }

@Composable
private fun SettingsChoice(
    label: String,
    values: List<SettingsOption>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Label)
    Spacer(Modifier.height(7.dp))
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        values.chunked(3).forEach { rowValues ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                rowValues.forEach { value ->
                    TvButton(
                        text = value.label,
                        onClick = { onSelected(value.value) },
                        selected = value.value == selected,
                        variant = if (value.value == selected) TvButtonVariant.Primary else TvButtonVariant.Secondary,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                    )
                }
                repeat(3 - rowValues.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
        Spacer(Modifier.weight(1f))
        Text(value, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Caption, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AccountRow(
    account: XtreamAccount,
    active: Boolean,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(if (active) SmartVisionColors.PrimaryDark.copy(alpha = 0.5f) else SmartVisionColors.Surface, RoundedCornerShape(7.dp))
            .border(BorderStroke(1.dp, if (active) SmartVisionColors.Primary else SmartVisionColors.Border), RoundedCornerShape(7.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (active) Icons.Default.CheckCircle else Icons.Default.Person,
            contentDescription = null,
            tint = if (active) SmartVisionColors.CyanAccent else SmartVisionColors.TextSecondary,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(account.name, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Label, fontWeight = FontWeight.Bold)
            Text("${account.host}  |  ${account.username}", color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption, maxLines = 1)
        }
        if (!active) {
            TvButton("Connecter", onClick = onConnect, modifier = Modifier.height(38.dp))
            Spacer(Modifier.width(7.dp))
        }
        TvButton("Modifier", onClick = onEdit, leadingIcon = Icons.Default.Edit, variant = TvButtonVariant.Secondary, modifier = Modifier.height(38.dp))
        Spacer(Modifier.width(7.dp))
        TvButton("Supprimer", onClick = onDelete, leadingIcon = Icons.Default.Delete, variant = TvButtonVariant.Secondary, modifier = Modifier.height(38.dp))
    }
}

@Composable
private fun AccountEditorDialog(
    initial: XtreamAccount,
    onDismiss: () -> Unit,
    onSave: (XtreamAccount) -> Unit,
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var host by remember(initial.id) { mutableStateOf(initial.host) }
    var username by remember(initial.id) { mutableStateOf(initial.username) }
    var password by remember(initial.id) { mutableStateOf(initial.password) }
    var error by remember { mutableStateOf<String?>(null) }
    val nameFocusRequester = remember { FocusRequester() }
    val hostFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val cancelFocusRequester = remember { FocusRequester() }
    val saveFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        delay(80)
        runCatching { nameFocusRequester.requestFocus() }
    }

    TvDialogSurface(
        title = "Compte Xtream",
        onDismiss = onDismiss,
        width = 570.dp,
        icon = Icons.Default.Person,
    ) {
            SettingsTextField(
                label = "Nom du compte",
                value = name,
                onValueChange = { name = it },
                focusRequester = nameFocusRequester,
                nextFocusRequester = hostFocusRequester,
            )
            SettingsTextField(
                label = "Hote",
                value = host,
                onValueChange = { host = it },
                focusRequester = hostFocusRequester,
                previousFocusRequester = nameFocusRequester,
                nextFocusRequester = usernameFocusRequester,
            )
            SettingsTextField(
                label = "Utilisateur",
                value = username,
                onValueChange = { username = it },
                focusRequester = usernameFocusRequester,
                previousFocusRequester = hostFocusRequester,
                nextFocusRequester = passwordFocusRequester,
            )
            SettingsTextField(
                label = "Mot de passe",
                value = password,
                onValueChange = { password = it },
                focusRequester = passwordFocusRequester,
                previousFocusRequester = usernameFocusRequester,
                nextFocusRequester = saveFocusRequester,
                password = true,
            )
            error?.let {
                Text(it, color = SmartVisionColors.Error, style = SmartVisionType.Caption)
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TvButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    focusRequester = cancelFocusRequester,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier.height(42.dp),
                )
                Spacer(Modifier.width(10.dp))
                TvButton(
                    text = "Enregistrer et connecter",
                    focusRequester = saveFocusRequester,
                    onClick = {
                        if (host.isBlank() || username.isBlank() || password.isBlank()) {
                            error = "Hote, utilisateur et mot de passe sont obligatoires."
                        } else {
                            onSave(initial.copy(name = name, host = host, username = username, password = password))
                        }
                    },
                    modifier = Modifier.height(42.dp),
                )
            }
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    previousFocusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    password: Boolean = false,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var editing by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    var localValue by remember { mutableStateOf(value) }
    val focusStyle = LocalTvFocusStyle.current
    val shape = RoundedCornerShape(6.dp)

    LaunchedEffect(value, editing) {
        if (!editing && localValue != value) {
            localValue = value
        }
    }

    LaunchedEffect(editing) {
        if (editing) {
            runCatching { focusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

    Text(label, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
    Spacer(Modifier.height(5.dp))
    BasicTextField(
        value = localValue,
        onValueChange = { next ->
            localValue = next
            onValueChange(next)
        },
        singleLine = true,
        readOnly = !editing,
        textStyle = SmartVisionType.Body.copy(color = SmartVisionColors.TextPrimary),
        cursorBrush = SolidColor(focusStyle.accent),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (!it.isFocused) {
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
            .fillMaxWidth()
            .height(44.dp)
            .background(
                if (focused || editing) focusStyle.background else SmartVisionColors.Surface,
                shape,
            )
            .border(
                BorderStroke(
                    if (focused || editing) focusStyle.borderWidth else 1.dp,
                    if (focused || editing) focusStyle.accent else SmartVisionColors.Border,
                ),
                shape,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
    Spacer(Modifier.height(12.dp))
}
