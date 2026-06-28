package com.smartvision.svplayer.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.svplayer.R
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.core.config.XtreamAccount
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.TvFocusStyles
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.i18n.smartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import com.smartvision.svplayer.ui.update.AppUpdateUiState
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    updateState: AppUpdateUiState,
    onCheckForUpdate: () -> Unit,
    onSyncCatalog: () -> Unit,
    parentalControlAllowed: Boolean = true,
    onLockedFeature: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val accounts by container.accountManager.accounts.collectAsStateWithLifecycle()
    val activeAccountId by container.accountManager.activeAccountId.collectAsStateWithLifecycle()
    val activeAccount = accounts.firstOrNull { it.id == activeAccountId } ?: accounts.firstOrNull()
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = com.smartvision.svplayer.domain.model.PlayerSettings(),
    )
    val scope = rememberCoroutineScope()
    var selectedSection by remember { mutableStateOf(SettingsSection.Preferences) }
    val strings = smartVisionStrings(settings.language)

    BackHandler(onBack = onBack)

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
            .padding(horizontal = 34.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TvButton(
                text = strings.back,
                leadingIcon = Icons.Default.ArrowBack,
                onClick = onBack,
                variant = TvButtonVariant.Secondary,
                modifier = Modifier.height(42.dp),
            )
            Spacer(Modifier.width(20.dp))
            Text(
                text = strings.settings,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleL,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(22.dp))

        SettingsMenuLayout(
            selectedSection = selectedSection,
            onSectionSelected = { selectedSection = it },
            settings = settings,
            accountsCount = accounts.size,
            activeAccount = activeAccount,
            updateState = updateState,
            onCheckForUpdate = onCheckForUpdate,
            onSyncCatalog = onSyncCatalog,
            onSetLanguage = { value -> scope.launch { container.settingsRepository.setLanguage(value) } },
            onSetSyncFrequency = { value -> scope.launch { container.settingsRepository.setSyncFrequency(value) } },
            onSetFocusStyle = { value -> scope.launch { container.settingsRepository.setFocusStyle(value) } },
            onSetFocusColor = { value -> scope.launch { container.settingsRepository.setFocusColor(value) } },
            onSetFocusEffect = { value -> scope.launch { container.settingsRepository.setFocusEffect(value) } },
            onSetVideoRatio = { value -> scope.launch { container.settingsRepository.setVideoRatio(value) } },
            onSetAnimations = { value -> scope.launch { container.settingsRepository.setAnimationsEnabled(value) } },
            onSetRetry = { value -> scope.launch { container.settingsRepository.setRetryEnabled(value) } },
            onSetParentalEnabled = { value -> scope.launch { container.settingsRepository.setParentalControlEnabled(value) } },
            onSetParentalPin = { value -> scope.launch { container.settingsRepository.setParentalPin(value) } },
            onSetParentalKeywords = { value -> scope.launch { container.settingsRepository.setParentalKeywords(value) } },
            onClearLocalData = { scope.launch { container.settingsRepository.clearLocalData() } },
            parentalControlAllowed = parentalControlAllowed,
            onLockedFeature = onLockedFeature,
            strings = strings,
            modifier = Modifier.fillMaxSize(),
        )
        return@Column

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SettingsPanel(
                title = "Preferences generales",
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
            ) {
                SettingsChoice(
                    label = strings.language,
                    values = listOf(
                        SettingsOption("English", strings.english),
                        SettingsOption("Francais", strings.french),
                    ),
                    selected = settings.language,
                    onSelected = { value -> scope.launch { container.settingsRepository.setLanguage(value) } },
                )
                SettingsChoice(
                    label = strings.automaticSync,
                    values = syncFrequencyOptions(strings),
                    selected = settings.syncFrequency,
                    onSelected = { value -> scope.launch { container.settingsRepository.setSyncFrequency(value) } },
                )
                SettingsChoice(
                    label = strings.videoFormat,
                    values = settingsOptions("Fit", "Fill", "Zoom"),
                    selected = settings.videoRatio,
                    onSelected = { scope.launch { container.settingsRepository.setVideoRatio(it) } },
                )
                SettingsChoice(
                    label = strings.animations,
                    values = listOf(
                        SettingsOption("enabled", strings.enabled),
                        SettingsOption("reduced", strings.reduced),
                    ),
                    selected = if (settings.animationsEnabled) "enabled" else "reduced",
                    onSelected = { value -> scope.launch { container.settingsRepository.setAnimationsEnabled(value == "enabled") } },
                )
                SettingsChoice(
                    label = strings.automaticReconnect,
                    values = listOf(
                        SettingsOption("enabled", strings.enabled),
                        SettingsOption("disabled", strings.disabled),
                    ),
                    selected = if (settings.retryEnabled) "enabled" else "disabled",
                    onSelected = { value -> scope.launch { container.settingsRepository.setRetryEnabled(value == "enabled") } },
                )
                SettingsInfoRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                SettingsInfoRow(strings.activeXtreamAccount, activeAccount?.name ?: strings.none)
                SettingsInfoRow(strings.activeServer, activeAccount?.host ?: strings.notConfigured)
                Spacer(Modifier.height(12.dp))
                TvButton(
                    text = if (updateState.checking) "Recherche..." else "Chercher une mise a jour",
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
                    Text(
                        text = message,
                        color = SmartVisionColors.Error,
                        style = SmartVisionType.Caption,
                        maxLines = 2,
                    )
                }
                if (updateState.checkedOnce && updateState.update == null && updateState.errorMessage == null && !updateState.checking) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = strings.appUpToDate,
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Caption,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(18.dp))
                TvButton(
                    text = strings.clearLocalData,
                    leadingIcon = Icons.Default.Refresh,
                    onClick = { scope.launch { container.settingsRepository.clearLocalData() } },
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                )
            }

            SettingsPanel(
                title = "Maintenance et donnees",
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
            ) {
                SettingsInfoRow("Frequence actuelle", settings.syncFrequency)
                SettingsInfoRow(strings.activeAccount, activeAccount?.let { "${it.name} - ${it.username}" } ?: strings.none)
                SettingsInfoRow(strings.bufferMode, settings.bufferMode)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Les identifiants Xtream se gerent maintenant depuis Compte utilisateur afin de separer les reglages de l'application et les donnees client.",
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Body,
                )
                Spacer(Modifier.height(16.dp))
                TvButton(
                    text = "Verifier les mises a jour",
                    leadingIcon = Icons.Default.Refresh,
                    onClick = onCheckForUpdate,
                    enabled = !updateState.checking && !updateState.installing,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                )
                Spacer(Modifier.height(10.dp))
                TvButton(
                    text = strings.clearLocalData,
                    leadingIcon = Icons.Default.Delete,
                    onClick = { scope.launch { container.settingsRepository.clearLocalData() } },
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsMenuLayout(
    selectedSection: SettingsSection,
    onSectionSelected: (SettingsSection) -> Unit,
    settings: PlayerSettings,
    accountsCount: Int,
    activeAccount: XtreamAccount?,
    updateState: AppUpdateUiState,
    onCheckForUpdate: () -> Unit,
    onSyncCatalog: () -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetSyncFrequency: (String) -> Unit,
    onSetFocusStyle: (String) -> Unit,
    onSetFocusColor: (String) -> Unit,
    onSetFocusEffect: (String) -> Unit,
    onSetVideoRatio: (String) -> Unit,
    onSetAnimations: (Boolean) -> Unit,
    onSetRetry: (Boolean) -> Unit,
    onSetParentalEnabled: (Boolean) -> Unit,
    onSetParentalPin: (String) -> Unit,
    onSetParentalKeywords: (String) -> Unit,
    onClearLocalData: () -> Unit,
    parentalControlAllowed: Boolean,
    onLockedFeature: () -> Unit,
    strings: SmartVisionStrings,
    modifier: Modifier = Modifier,
) {
    var parentalUnlocked by remember { mutableStateOf(false) }
    var showCreatePinDialog by remember { mutableStateOf(false) }
    var showUnlockPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var pendingParentalEnabled by remember { mutableStateOf<Boolean?>(null) }
    var pinMessage by remember { mutableStateOf<String?>(null) }

    fun openParentalSection() {
        if (!parentalControlAllowed) {
            onLockedFeature()
            return
        }
        pinMessage = null
        if (settings.parentalPin.isBlank()) {
            showCreatePinDialog = true
        } else if (!parentalUnlocked) {
            showUnlockPinDialog = true
        } else {
            onSectionSelected(SettingsSection.Parental)
        }
    }

    fun applyParentalToggle(value: Boolean) {
        pendingParentalEnabled = value
        showUnlockPinDialog = true
    }

    LaunchedEffect(parentalControlAllowed, settings.parentalPin) {
        if (!parentalControlAllowed || settings.parentalPin.isBlank()) {
            parentalUnlocked = false
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(
            modifier = Modifier
                .width(292.dp)
                .fillMaxHeight()
                .background(Color(0xD9091424), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingsSection.entries.forEach { section ->
                val isParental = section == SettingsSection.Parental
                Box(modifier = Modifier.fillMaxWidth()) {
                    TvButton(
                        text = section.label(strings),
                        leadingIcon = section.icon,
                        selected = selectedSection == section,
                        variant = if (selectedSection == section) TvButtonVariant.Primary else TvButtonVariant.Text,
                        onClick = {
                            if (isParental) {
                                openParentalSection()
                            } else {
                                onSectionSelected(section)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .alpha(if (isParental && !parentalControlAllowed) 0.28f else 1f),
                    )
                    if (isParental) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset(x = (-10).dp)
                                .size(11.dp)
                                .background(
                                    if (settings.parentalControlEnabled) Color(0xFF20D878) else Color(0xFFFF4B4B),
                                    RoundedCornerShape(50),
                                )
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.72f)), RoundedCornerShape(50)),
                        )
                        if (!parentalControlAllowed) {
                            Image(
                                painter = painterResource(R.drawable.premium_crown),
                                contentDescription = "Premium",
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .offset(x = (-30).dp)
                                    .size(22.dp),
                            )
                        }
                    }
                }
            }
        }

        SettingsPanel(
            title = selectedSection.label(strings),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            when (selectedSection) {
                SettingsSection.Preferences -> {
                    SettingsChoice(
                        label = strings.language,
                        values = listOf(
                            SettingsOption("English", strings.english),
                            SettingsOption("Francais", strings.french),
                        ),
                        selected = settings.language,
                        onSelected = onSetLanguage,
                    )
                    SettingsChoice(
                        label = strings.videoFormat,
                        values = settingsOptions("Fit", "Fill", "Zoom"),
                        selected = settings.videoRatio,
                        onSelected = onSetVideoRatio,
                    )
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
                SettingsSection.Sync -> {
                    SettingsChoice(
                        label = strings.automaticSync,
                        values = syncFrequencyOptions(strings),
                        selected = settings.syncFrequency,
                        onSelected = onSetSyncFrequency,
                    )
                    SettingsInfoRow(strings.currentFrequency, settings.syncFrequency.localizedSyncFrequency(strings))
                    SettingsInfoRow(strings.activeAccount, activeAccount?.let { "${it.name} - ${it.username}" } ?: strings.none)
                    SettingsInfoRow(strings.activeServer, activeAccount?.host ?: strings.notConfigured)
                }
                SettingsSection.Personalization -> {
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
                    Text(
                        text = "${strings.focusStyle}: ${settings.focusStyle} | ${strings.focusColor}: ${settings.focusColor} | ${strings.focusEffect}: ${settings.focusEffect}",
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Caption,
                    )
                }
                SettingsSection.Updates -> {
                    SettingsInfoRow(strings.installedVersion, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    SettingsInfoRow(strings.portal, BuildConfig.ACTIVATION_BASE_URL.removeSuffix("/"))
                    Spacer(Modifier.height(14.dp))
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
                        Text(
                            text = message,
                            color = SmartVisionColors.Error,
                            style = SmartVisionType.Caption,
                            maxLines = 2,
                        )
                    }
                    if (updateState.checkedOnce && updateState.update == null && updateState.errorMessage == null && !updateState.checking) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = strings.appUpToDate,
                            color = SmartVisionColors.TextSecondary,
                            style = SmartVisionType.Caption,
                            maxLines = 1,
                        )
                    }
                }
                SettingsSection.Data -> {
                    SettingsInfoRow(strings.bufferMode, settings.bufferMode)
                    SettingsInfoRow(strings.localXtreamAccounts, accountsCount.toString())
                    SettingsInfoRow(strings.activeAccount, activeAccount?.name ?: strings.none)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = strings.localCredentialsInfo,
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Body,
                    )
                    Spacer(Modifier.height(16.dp))
                    TvButton(
                        text = strings.clearLocalData,
                        leadingIcon = Icons.Default.Delete,
                        onClick = onClearLocalData,
                        variant = TvButtonVariant.Secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                    )
                }
                SettingsSection.Parental -> {
                    val keywordsFocusRequester = remember { FocusRequester() }
                    when {
                        !parentalControlAllowed -> {
                            Text(
                                text = strings.lockedPremiumFeature,
                                color = SmartVisionColors.TextSecondary,
                                style = SmartVisionType.Body,
                            )
                            Spacer(Modifier.height(14.dp))
                            TvButton(
                                text = "Premium",
                                onClick = onLockedFeature,
                                variant = TvButtonVariant.Primary,
                                modifier = Modifier.height(44.dp),
                            )
                        }

                        !parentalUnlocked -> {
                            Text(
                                text = strings.unlockParentalControl,
                                color = SmartVisionColors.TextSecondary,
                                style = SmartVisionType.Body,
                            )
                            Spacer(Modifier.height(14.dp))
                            TvButton(
                                text = if (settings.parentalPin.isBlank()) strings.createPin else strings.enterPin,
                                onClick = {
                                    if (settings.parentalPin.isBlank()) showCreatePinDialog = true else showUnlockPinDialog = true
                                },
                                variant = TvButtonVariant.Primary,
                                modifier = Modifier.height(44.dp),
                            )
                        }

                        else -> {
                            Text(strings.parentalControl, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Label)
                            Spacer(Modifier.height(7.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TvButton(
                                    text = strings.enabled,
                                    onClick = { applyParentalToggle(true) },
                                    selected = settings.parentalControlEnabled,
                                    variant = if (settings.parentalControlEnabled) TvButtonVariant.Success else TvButtonVariant.Secondary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                )
                                TvButton(
                                    text = strings.disabled,
                                    onClick = { applyParentalToggle(false) },
                                    selected = !settings.parentalControlEnabled,
                                    variant = if (settings.parentalControlEnabled) TvButtonVariant.Secondary else TvButtonVariant.Danger,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(strings.pinCode, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    if (settings.parentalPin.isBlank()) strings.notConfigured else "****",
                                    color = SmartVisionColors.TextPrimary,
                                    style = SmartVisionType.Caption,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.weight(1f))
                                Spacer(Modifier.width(12.dp))
                                TvButton(
                                    text = strings.changePin,
                                    onClick = { showChangePinDialog = true },
                                    variant = TvButtonVariant.Secondary,
                                    modifier = Modifier.height(40.dp),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            SettingsTextField(
                                label = strings.hiddenKeywords,
                                value = settings.parentalKeywords,
                                onValueChange = onSetParentalKeywords,
                                focusRequester = keywordsFocusRequester,
                            )
                            TvButton(
                                text = strings.apply,
                                onClick = onSyncCatalog,
                                variant = TvButtonVariant.Primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = strings.parentalHelp,
                                color = SmartVisionColors.TextSecondary,
                                style = SmartVisionType.Caption,
                            )
                            pinMessage?.let { message ->
                                Spacer(Modifier.height(8.dp))
                                Text(message, color = SmartVisionColors.CyanAccent, style = SmartVisionType.Caption)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePinDialog) {
        ParentalCreatePinDialog(
            strings = strings,
            onDismiss = { showCreatePinDialog = false },
            onCreate = { pin ->
                onSetParentalPin(pin)
                parentalUnlocked = true
                showCreatePinDialog = false
                onSectionSelected(SettingsSection.Parental)
                pinMessage = null
            },
        )
    }

    if (showUnlockPinDialog) {
        ParentalPinDialog(
            title = strings.enterPin,
            strings = strings,
            onDismiss = {
                showUnlockPinDialog = false
                pendingParentalEnabled = null
            },
            onSubmit = { pin ->
                if (pin == settings.parentalPin) {
                    parentalUnlocked = true
                    showUnlockPinDialog = false
                    val pending = pendingParentalEnabled
                    pendingParentalEnabled = null
                    if (pending != null) {
                        onSetParentalEnabled(pending)
                    } else {
                        onSectionSelected(SettingsSection.Parental)
                    }
                    pinMessage = null
                    true
                } else {
                    pinMessage = strings.pinIncorrect
                    false
                }
            },
        )
    }

    if (showChangePinDialog) {
        ParentalCreatePinDialog(
            strings = strings,
            title = strings.changePin,
            onDismiss = { showChangePinDialog = false },
            onCreate = { pin ->
                onSetParentalPin(pin)
                showChangePinDialog = false
                pinMessage = strings.changePin
            },
        )
    }
}

private enum class SettingsSection(
    val icon: ImageVector,
) {
    Preferences(Icons.Default.Settings),
    Sync(Icons.Default.CloudSync),
    Personalization(Icons.Default.Settings),
    Updates(Icons.Default.Refresh),
    Parental(Icons.Default.Person),
    Data(Icons.Default.Delete),
}

private fun SettingsSection.label(strings: SmartVisionStrings): String = when (this) {
    SettingsSection.Preferences -> strings.generalPreferences
    SettingsSection.Sync -> strings.sync
    SettingsSection.Personalization -> strings.personalization
    SettingsSection.Updates -> strings.updates
    SettingsSection.Parental -> strings.parentalControl
    SettingsSection.Data -> strings.localData
}

private data class SettingsOption(
    val value: String,
    val label: String = value,
)

private fun settingsOptions(vararg values: String): List<SettingsOption> = values.map { SettingsOption(it) }

private fun syncFrequencyOptions(strings: SmartVisionStrings): List<SettingsOption> = listOf(
    SettingsOption("24h", strings.sync24h),
    SettingsOption("48h", strings.sync48h),
    SettingsOption("A chaque demarrage", strings.syncOnStartup),
    SettingsOption("Manuelle", strings.syncManual),
    SettingsOption("Jamais", strings.syncNever),
)

private fun String.localizedSyncFrequency(strings: SmartVisionStrings): String {
    return syncFrequencyOptions(strings).firstOrNull { it.value == this }?.label ?: this
}

@Composable
private fun SettingsPanel(
    title: String,
    modifier: Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(Color(0xE60A1424), RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
        }
        Spacer(Modifier.height(18.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            content()
            Spacer(Modifier.height(12.dp))
        }
    }
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
        nameFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(570.dp)
                .background(Color(0xFF0A1425), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, SmartVisionColors.Primary), RoundedCornerShape(8.dp))
                .padding(24.dp),
        ) {
            Text("Compte Xtream", color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
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
}

@Composable
private fun ParentalPinDialog(
    title: String,
    strings: SmartVisionStrings,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Boolean,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var recoveryMessage by remember { mutableStateOf<String?>(null) }
    val pinFocusRequester = remember { FocusRequester() }
    val submitFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        pinFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(470.dp)
                .background(Color(0xFF0A1425), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, SmartVisionColors.Primary), RoundedCornerShape(8.dp))
                .padding(24.dp),
        ) {
            Text(title, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            SettingsTextField(
                label = strings.pinCode,
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                focusRequester = pinFocusRequester,
                nextFocusRequester = submitFocusRequester,
                password = true,
            )
            error?.let { Text(it, color = SmartVisionColors.Error, style = SmartVisionType.Caption) }
            recoveryMessage?.let { Text(it, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption) }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TvButton(text = strings.cancel, onClick = onDismiss, variant = TvButtonVariant.Secondary, modifier = Modifier.height(42.dp))
                Spacer(Modifier.width(10.dp))
                TvButton(
                    text = strings.forgotPinByEmail,
                    onClick = { recoveryMessage = strings.pinEmailUnavailable },
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier.height(42.dp),
                )
                Spacer(Modifier.width(10.dp))
                TvButton(
                    text = strings.apply,
                    focusRequester = submitFocusRequester,
                    onClick = {
                        if (pin.length < 4) {
                            error = strings.pinRequired
                        } else if (!onSubmit(pin)) {
                            error = strings.pinIncorrect
                        }
                    },
                    modifier = Modifier.height(42.dp),
                )
            }
        }
    }
}

@Composable
private fun ParentalCreatePinDialog(
    strings: SmartVisionStrings,
    title: String = strings.createPin,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val pinFocusRequester = remember { FocusRequester() }
    val confirmFocusRequester = remember { FocusRequester() }
    val saveFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        pinFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(470.dp)
                .background(Color(0xFF0A1425), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, SmartVisionColors.Primary), RoundedCornerShape(8.dp))
                .padding(24.dp),
        ) {
            Text(title, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            SettingsTextField(
                label = strings.newPin,
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                focusRequester = pinFocusRequester,
                nextFocusRequester = confirmFocusRequester,
                password = true,
            )
            SettingsTextField(
                label = strings.confirmPin,
                value = confirmPin,
                onValueChange = { confirmPin = it.filter(Char::isDigit).take(8) },
                focusRequester = confirmFocusRequester,
                previousFocusRequester = pinFocusRequester,
                nextFocusRequester = saveFocusRequester,
                password = true,
            )
            error?.let { Text(it, color = SmartVisionColors.Error, style = SmartVisionType.Caption) }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TvButton(text = strings.cancel, onClick = onDismiss, variant = TvButtonVariant.Secondary, modifier = Modifier.height(42.dp))
                Spacer(Modifier.width(10.dp))
                TvButton(
                    text = strings.apply,
                    focusRequester = saveFocusRequester,
                    onClick = {
                        when {
                            pin.length < 4 -> error = strings.pinRequired
                            pin != confirmPin -> error = strings.pinsDoNotMatch
                            else -> onCreate(pin)
                        }
                    },
                    modifier = Modifier.height(42.dp),
                )
            }
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
        cursorBrush = SolidColor(SmartVisionColors.CyanAccent),
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
                        nextFocusRequester?.requestFocus()
                        nextFocusRequester != null
                    }
                    Key.DirectionUp -> {
                        editing = false
                        keyboardController?.hide()
                        previousFocusRequester?.requestFocus()
                        previousFocusRequester != null
                    }
                    else -> false
                }
            }
            .fillMaxWidth()
            .height(44.dp)
            .background(
                if (focused || editing) focusStyle.accent.copy(alpha = 0.10f) else SmartVisionColors.Surface,
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
