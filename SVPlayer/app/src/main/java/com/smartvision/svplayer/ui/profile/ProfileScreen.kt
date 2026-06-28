package com.smartvision.svplayer.ui.profile

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.focus.FocusRequester
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.config.XtreamAccount
import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.activation.ActivationException
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.data.activation.StoredActivationState
import com.smartvision.svplayer.data.monetization.MonetizationStatus
import com.smartvision.svplayer.data.monetization.monetizationStatus
import com.smartvision.svplayer.data.repository.emptyAccountProfile
import com.smartvision.svplayer.domain.model.AccountProfile
import com.smartvision.svplayer.domain.model.SyncStatus
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onSyncCatalog: () -> Unit,
    onActivationChanged: () -> Unit,
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val syncStatus by container.catalogRepository.syncStatus.collectAsStateWithLifecycle()
    val privacyOptionsRequired by container.privacyConsentManager.privacyOptionsRequired
        .collectAsStateWithLifecycle()

    LaunchedEffect(state.usageMode) {
        if (state.usageMode == UsageMode.Premium) {
            onActivationChanged()
        }
    }

    ProfileScreen(
        state = state,
        syncStatus = syncStatus,
        onBack = onBack,
        onSettings = onSettings,
        onRefresh = viewModel::refresh,
        onSyncCatalog = onSyncCatalog,
        onShowLicenseQr = viewModel::showLicenseQr,
        onLicenseCodeChange = viewModel::updateLicenseCode,
        onActivateLicense = viewModel::activateLicense,
        onShowPrivacyOptions = {
            activity?.let {
                scope.launch { container.privacyConsentManager.showPrivacyOptions(it) }
            }
        },
        privacyOptionsRequired = privacyOptionsRequired,
        onShowXtreamSetupQr = viewModel::showXtreamSetupQr,
        onSaveXtreamAccount = { account ->
            val accountId = container.accountManager.upsert(account)
            container.accountManager.select(accountId)
            container.xtreamRepository.clearCaches()
            onSyncCatalog()
        },
        onDeleteXtreamAccount = { accountId ->
            container.accountManager.delete(accountId)
            container.xtreamRepository.clearCaches()
            if (container.accountManager.accounts.value.isNotEmpty()) {
                onSyncCatalog()
            }
        },
        onDismissQr = viewModel::dismissQr,
    )
}

@Composable
private fun ProfileScreen(
    state: ProfileUiState,
    syncStatus: SyncStatus,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
    onSyncCatalog: () -> Unit,
    onShowLicenseQr: () -> Unit,
    onLicenseCodeChange: (String) -> Unit,
    onActivateLicense: () -> Unit,
    onShowPrivacyOptions: () -> Unit,
    privacyOptionsRequired: Boolean,
    onShowXtreamSetupQr: () -> Unit,
    onSaveXtreamAccount: (XtreamAccount) -> Unit,
    onDeleteXtreamAccount: (String) -> Unit,
    onDismissQr: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var selectedSection by remember { mutableStateOf(ProfileSection.License) }

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
            .padding(horizontal = 34.dp, vertical = 24.dp),
    ) {
        ProfileTopBar(
            modeLabel = state.usageMode.label,
            modeColor = state.usageMode.color,
            onBack = onBack,
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                ProfileSection.entries.forEach { section ->
                    TvButton(
                        text = section.label,
                        leadingIcon = section.icon,
                        selected = selectedSection == section,
                        variant = if (selectedSection == section) TvButtonVariant.Primary else TvButtonVariant.Text,
                        onClick = {
                            if (section == ProfileSection.SettingsShortcut) {
                                onSettings()
                            } else {
                                selectedSection = section
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (selectedSection) {
                    ProfileSection.License -> LicensePanel(
                        state = state,
                        onRefresh = onRefresh,
                        onShowLicenseQr = onShowLicenseQr,
                        onShowPrivacyOptions = onShowPrivacyOptions,
                        privacyOptionsRequired = privacyOptionsRequired,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ProfileSection.Xtream -> XtreamPanel(
                        state = state,
                        syncStatus = syncStatus,
                        onShowXtreamSetupQr = onShowXtreamSetupQr,
                        onSyncCatalog = onSyncCatalog,
                        onSaveXtreamAccount = onSaveXtreamAccount,
                        onDeleteXtreamAccount = onDeleteXtreamAccount,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ProfileSection.Device -> DevicePanel(state = state, modifier = Modifier.fillMaxWidth())
                    ProfileSection.Usage -> ConversionPanel(usageMode = state.usageMode, modifier = Modifier.fillMaxWidth())
                    ProfileSection.History -> ProfileHistoryPanel(state = state, modifier = Modifier.fillMaxWidth())
                    ProfileSection.Help -> ProfileHelpPanel(modifier = Modifier.fillMaxWidth())
                    ProfileSection.SettingsShortcut -> Unit
                }
            }
        }
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
}

@Composable
private fun ProfileTopBar(
    modeLabel: String,
    modeColor: Color,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvButton(
            text = "Retour",
            onClick = onBack,
            leadingIcon = Icons.Default.ArrowBack,
            variant = TvButtonVariant.Secondary,
            contentPadding = PaddingValues(horizontal = 18.dp),
            modifier = Modifier.height(42.dp),
        )
        Spacer(Modifier.width(18.dp))
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = SmartVisionColors.CyanAccent,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Profil client",
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.TitleL,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        StatusPill(modeLabel, modeColor)
    }
}

@Composable
private fun LicensePanel(
    state: ProfileUiState,
    onRefresh: () -> Unit,
    onShowLicenseQr: () -> Unit,
    onShowPrivacyOptions: () -> Unit,
    privacyOptionsRequired: Boolean,
    modifier: Modifier = Modifier,
) {
    ProfilePanel(
        title = "Licence SmartVision",
        icon = Icons.Default.Verified,
        modifier = modifier,
    ) {
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
}

private enum class ProfileSection(
    val label: String,
    val icon: ImageVector,
) {
    License("Licence SmartVision", Icons.Default.Verified),
    Xtream("Identifiants Xtream", Icons.Default.CloudSync),
    Device("Appareil et catalogue", Icons.Default.Devices),
    Usage("Mode d'utilisation", Icons.Default.CreditCard),
    History("Historique", Icons.Default.History),
    Help("Aide", Icons.Default.HelpOutline),
    SettingsShortcut("Parametres", Icons.Default.Settings),
}

@Composable
private fun XtreamPanel(
    state: ProfileUiState,
    syncStatus: SyncStatus,
    onShowXtreamSetupQr: () -> Unit,
    onSyncCatalog: () -> Unit,
    onSaveXtreamAccount: (XtreamAccount) -> Unit,
    onDeleteXtreamAccount: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDetails by remember(state.activeXtreamAccount?.id) { mutableStateOf(true) }
    var accountToEdit by remember { mutableStateOf<XtreamAccount?>(null) }
    var accountToDelete by remember { mutableStateOf<XtreamAccount?>(null) }
    val activeAccount = state.activeXtreamAccount

    ProfilePanel(
        title = "Identifiants Xtream",
        icon = Icons.Default.CloudSync,
        modifier = modifier,
    ) {
        TvButton(
            text = syncStatus.buttonLabel,
            onClick = onSyncCatalog,
            enabled = syncStatus !is SyncStatus.Running,
            leadingIcon = Icons.Default.CloudSync,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
        )
        Spacer(Modifier.height(10.dp))
        when (syncStatus) {
            SyncStatus.Running -> {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50)),
                    color = SmartVisionColors.CyanAccent,
                    trackColor = SmartVisionColors.Surface.copy(alpha = 0.84f),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Synchronisation du catalogue Xtream en cours...",
                    color = SmartVisionColors.CyanAccent,
                    style = SmartVisionType.Caption,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            is SyncStatus.Success -> Text(
                text = syncStatus.message,
                color = SmartVisionColors.Success,
                style = SmartVisionType.Caption,
                fontWeight = FontWeight.SemiBold,
            )
            is SyncStatus.Error -> Text(
                text = syncStatus.message,
                color = SmartVisionColors.Error,
                style = SmartVisionType.Caption,
                fontWeight = FontWeight.SemiBold,
            )
            else -> Unit
        }
        Spacer(Modifier.height(14.dp))
        XtreamConnectedAccountSection(
            account = activeAccount,
            showDetails = showDetails,
            onToggleDetails = { showDetails = !showDetails },
            onEdit = { account -> accountToEdit = account },
            onDelete = { account -> accountToDelete = account },
        )
        if (state.xtreamExpiresAt.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            ProfileInfoRow("Expiration Xtream", state.xtreamExpiresAt)
        }
        Spacer(Modifier.height(12.dp))
        TvButton(
            text = if (state.hasXtream) "Modifier par QR" else "Configurer par QR",
            onClick = onShowXtreamSetupQr,
            leadingIcon = Icons.Default.QrCode2,
            variant = TvButtonVariant.Secondary,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
        )
    }

    accountToEdit?.let { account ->
        XtreamAccountEditorDialog(
            initial = account,
            onDismiss = { accountToEdit = null },
            onSave = { updated ->
                accountToEdit = null
                onSaveXtreamAccount(updated)
            },
        )
    }

    accountToDelete?.let { account ->
        ConfirmXtreamDeleteDialog(
            account = account,
            onDismiss = { accountToDelete = null },
            onConfirm = {
                accountToDelete = null
                onDeleteXtreamAccount(account.id)
            },
        )
    }
}

@Composable
private fun XtreamConnectedAccountSection(
    account: XtreamAccount?,
    showDetails: Boolean,
    onToggleDetails: () -> Unit,
    onEdit: (XtreamAccount) -> Unit,
    onDelete: (XtreamAccount) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(SmartVisionColors.Surface.copy(alpha = 0.64f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.82f)), RoundedCornerShape(7.dp))
            .padding(12.dp),
    ) {
        Text(
            text = "Compte Xtream connecte",
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.Label,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(9.dp))
        if (account == null) {
            Text(
                text = "Aucun compte Xtream local n'est configure.",
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
            )
        } else {
            if (showDetails) {
                ProfileInfoRow("URL", account.host.ifBlank { "Non configure" })
                ProfileInfoRow("Username", account.username.ifBlank { "Non configure" })
                ProfileInfoRow("Password", account.password.ifBlank { "Non configure" })
                Spacer(Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TvButton(
                    text = if (showDetails) "Masquer" else "Voir",
                    onClick = onToggleDetails,
                    leadingIcon = Icons.Default.Visibility,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                )
                TvButton(
                    text = "Modifier",
                    onClick = { onEdit(account) },
                    leadingIcon = Icons.Default.Edit,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                )
                TvButton(
                    text = "Supprimer",
                    onClick = { onDelete(account) },
                    leadingIcon = Icons.Default.Delete,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                )
            }
        }
    }
}

@Composable
private fun XtreamAccountEditorDialog(
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
    val saveFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        nameFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(600.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0A1425))
                .border(BorderStroke(1.dp, SmartVisionColors.CyanAccent), RoundedCornerShape(8.dp))
                .padding(22.dp),
        ) {
            Text(
                text = "Modifier le compte Xtream",
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(14.dp))
            ProfileEditTextField(
                label = "Nom",
                value = name,
                onValueChange = { name = it },
                focusRequester = nameFocusRequester,
                nextFocusRequester = hostFocusRequester,
            )
            ProfileEditTextField(
                label = "URL",
                value = host,
                onValueChange = { host = it },
                focusRequester = hostFocusRequester,
                previousFocusRequester = nameFocusRequester,
                nextFocusRequester = usernameFocusRequester,
            )
            ProfileEditTextField(
                label = "Username",
                value = username,
                onValueChange = { username = it },
                focusRequester = usernameFocusRequester,
                previousFocusRequester = hostFocusRequester,
                nextFocusRequester = passwordFocusRequester,
            )
            ProfileEditTextField(
                label = "Password",
                value = password,
                onValueChange = { password = it },
                focusRequester = passwordFocusRequester,
                previousFocusRequester = usernameFocusRequester,
                nextFocusRequester = saveFocusRequester,
                password = false,
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
                        if (host.isBlank() || username.isBlank() || password.isBlank()) {
                            error = "URL, username et password sont obligatoires."
                        } else {
                            onSave(
                                initial.copy(
                                    name = name,
                                    host = host,
                                    username = username,
                                    password = password,
                                ),
                            )
                        }
                    },
                    focusRequester = saveFocusRequester,
                    modifier = Modifier.height(42.dp),
                )
            }
        }
    }
}

@Composable
private fun ConfirmXtreamDeleteDialog(
    account: XtreamAccount,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(500.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0A1425))
                .border(BorderStroke(1.dp, SmartVisionColors.Error.copy(alpha = 0.78f)), RoundedCornerShape(8.dp))
                .padding(22.dp),
        ) {
            Text(
                text = "Supprimer ce compte Xtream ?",
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = account.username.ifBlank { account.host },
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
                    text = "Supprimer",
                    onClick = onConfirm,
                    leadingIcon = Icons.Default.Delete,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier.height(42.dp),
                )
            }
        }
    }
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
        cursorBrush = SolidColor(SmartVisionColors.CyanAccent),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                focused = focusState.isFocused
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
            .background(SmartVisionColors.Surface, RoundedCornerShape(6.dp))
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
private fun ConversionPanel(
    usageMode: UsageMode,
    modifier: Modifier = Modifier,
) {
    ProfilePanel(
        title = "Mode d'utilisation",
        icon = Icons.Default.CreditCard,
        modifier = modifier,
    ) {
        UsageStep(
            title = "1. Essai gratuit",
            text = "7 jours sans publicite pour tester l'application avec vos propres identifiants.",
            active = usageMode == UsageMode.Trial,
            color = SmartVisionColors.CyanAccent,
        )
        UsageStep(
            title = "2. Premium",
            text = "Licence 1 mois, 12 mois ou a vie, sans publicite pendant toute la duree achetee.",
            active = usageMode == UsageMode.Premium,
            color = SmartVisionColors.Success,
        )
        UsageStep(
            title = "3. Gratuit avec pubs",
            text = "Apres l'essai, l'utilisateur peut continuer gratuitement avec publicites ou acheter une licence.",
            active = usageMode == UsageMode.FreeAds,
            color = SmartVisionColors.Warning,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "SmartVision ne fournit aucune chaine, film ou serie. Les contenus viennent uniquement du compte Xtream configure par l'utilisateur.",
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Caption,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DevicePanel(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
) {
    ProfilePanel(
        title = "Appareil et catalogue",
        icon = Icons.Default.Devices,
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ProfileMetric("Live TV", state.account.liveCount.toString(), Modifier.weight(1f))
            ProfileMetric("Films", state.account.movieCount.toString(), Modifier.weight(1f))
            ProfileMetric("Series", state.account.seriesCount.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        ProfileInfoRow("Code TV", state.tvCode)
        ProfileInfoRow("Identifiant appareil", state.deviceId.ifBlank { "Generation..." })
        ProfileInfoRow("Derniere sync", state.account.lastSync ?: "Jamais")
        ProfileInfoRow("Version", BuildConfig.VERSION_NAME)
        ProfileInfoRow("Portail", BuildConfig.ACTIVATION_BASE_URL.removeSuffix("/"))
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
}

@Composable
private fun XtreamSummaryPanel(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
) {
    ProfilePanel(
        title = "Identifiants Xtream",
        icon = Icons.Default.CloudSync,
        modifier = modifier,
    ) {
        ProfileMetric(
            "Compte",
            if (state.hasXtream) "Configure" else "Absent",
            Modifier.fillMaxWidth(),
            if (state.hasXtream) SmartVisionColors.Success else SmartVisionColors.Warning,
        )
        Spacer(Modifier.height(12.dp))
        ProfileInfoRow("Serveur", state.xtreamHost.ifBlank { "Non configure" })
        ProfileInfoRow("Utilisateur", state.xtreamUsername.ifBlank { "Non configure" })
        ProfileInfoRow("Expiration", state.xtreamExpiresAt.ifBlank { "A synchroniser" })
        ProfileInfoRow("Connexions", state.xtreamConnections.ifBlank { "Non disponible" })
    }
}

@Composable
private fun ProfileHistoryPanel(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
) {
    ProfilePanel(
        title = "Historique",
        icon = Icons.Default.History,
        modifier = modifier,
    ) {
        ProfileInfoRow("Premiere activation", state.account.lastSync ?: "Non disponible")
        ProfileInfoRow("Derniere synchronisation", state.account.lastSync ?: "Jamais")
        ProfileInfoRow("Compte actif", state.xtreamUsername.ifBlank { "Non configure" })
        Text(
            text = "Les reprises de lecture, favoris et historiques de contenu restent accessibles depuis les sections Live TV, Films et Series.",
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Body,
        )
    }
}

@Composable
private fun ProfileHelpPanel(
    modifier: Modifier = Modifier,
) {
    ProfilePanel(
        title = "Aide",
        icon = Icons.Default.HelpOutline,
        modifier = modifier,
    ) {
        ProfileInfoRow("Activation", "Scannez le QR code depuis la TV ou saisissez un code licence.")
        ProfileInfoRow("Xtream", "Configurez vos propres identifiants depuis le portail SmartVision.")
        ProfileInfoRow("Support", "support@smartvisions.net")
        Text(
            text = "SmartVision est un lecteur IPTV. L'application ne vend, ne fournit et n'heberge aucun contenu TV, film ou serie.",
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Body,
        )
    }
}

@Composable
private fun ProfilePanel(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
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
            Icon(icon, contentDescription = null, tint = SmartVisionColors.CyanAccent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(9.dp))
            Text(title, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.Bold)
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
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
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
private fun ProfileXtreamAccountRow(
    account: XtreamAccount,
    active: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(if (active) SmartVisionColors.PrimaryDark.copy(alpha = 0.54f) else SmartVisionColors.Surface.copy(alpha = 0.62f))
            .border(
                BorderStroke(1.dp, if (active) SmartVisionColors.CyanAccent else SmartVisionColors.Border),
                RoundedCornerShape(7.dp),
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name.ifBlank { "Compte Xtream" },
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = "${account.host}  •  ${account.username.masked()}",
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TvButton(
            text = if (active) "Actif" else "Utiliser",
            onClick = onSelect,
            selected = active,
            enabled = !active,
            variant = if (active) TvButtonVariant.Primary else TvButtonVariant.Secondary,
            modifier = Modifier.height(38.dp),
        )
        Spacer(Modifier.width(7.dp))
        TvButton(
            text = "Supprimer",
            onClick = onDelete,
            leadingIcon = Icons.Default.Delete,
            variant = TvButtonVariant.Secondary,
            modifier = Modifier.height(38.dp),
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
        closeFocusRequester.requestFocus()
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

    LaunchedEffect(Unit) {
        fieldFocusRequester.requestFocus()
    }

    LaunchedEffect(editing) {
        if (editing) {
            inputFocusRequester.requestFocus()
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
                                .background(Color(0xFF030B18).copy(alpha = 0.92f))
                                .border(
                                    BorderStroke(
                                        if (fieldFocused || editing) 2.dp else 1.dp,
                                        if (fieldFocused || editing) {
                                            SmartVisionColors.CyanAccent
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
                                cursorBrush = SolidColor(SmartVisionColors.CyanAccent),
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

class ProfileViewModel(
    private val activationRepository: ActivationRepository,
    private val accountManager: XtreamAccountManager,
    catalogRepository: CatalogRepository,
) : ViewModel() {
    private val transient = MutableStateFlow(ProfileTransientState())

    val uiState = combine(
        activationRepository.localState,
        accountManager.accounts,
        accountManager.activeAccountId,
        catalogRepository.observeAccount(),
        transient,
    ) { activation, accounts, activeAccountId, account, transient ->
        val activeAccount = accounts.firstOrNull { it.id == activeAccountId } ?: accounts.firstOrNull()
        runCatching {
            buildProfileState(activation, accounts, activeAccountId.orEmpty(), activeAccount, account, transient)
        }.getOrElse {
            ProfileUiState(
                deviceId = activation.deviceId,
                refreshing = transient.refreshing,
                licenseCode = transient.licenseCode,
                submittingLicense = transient.submittingLicense,
                errorMessage = "Impossible d'afficher toutes les informations du compte. Actualisez le statut.",
                qrDialog = transient.qrDialog,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProfileUiState(),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            transient.update { it.copy(refreshing = true, errorMessage = null) }
            try {
                runCatching {
                    activationRepository.getOrCreateDeviceId()
                    activationRepository.checkStatus()
                }.onFailure { error ->
                    transient.update {
                        it.copy(errorMessage = error.userMessage("Impossible de verifier le compte."))
                    }
                }
            } finally {
                transient.update { it.copy(refreshing = false) }
            }
        }
    }

    fun showLicenseQr() {
        viewModelScope.launch {
            val device = activationRepository.currentDisplayDevice()
            transient.update {
                it.copy(
                    qrDialog = ProfileQrState(
                        title = "Acheter ou prolonger la licence",
                        subtitle = "Scannez ce QR code avec votre telephone pour choisir une licence SmartVision. Le portail associera l'achat a cet appareil.",
                        url = "${activationBaseUrl()}account/?source=tv&intent=license&${tvDeviceQuery(device.publicDeviceCode, device.deviceId)}&plan=year_1",
                        allowsLicenseEntry = true,
                    ),
                )
            }
        }
    }

    fun updateLicenseCode(value: String) {
        transient.update {
            it.copy(
                licenseCode = value.replace(Regex("[\\s-]+"), "").uppercase().take(10),
                errorMessage = null,
            )
        }
    }

    fun activateLicense() {
        val code = transient.value.licenseCode
        if (!Regex("^[A-Z0-9]{10}$").matches(code)) {
            transient.update {
                it.copy(errorMessage = "Le code licence doit contenir exactement 10 caracteres.")
            }
            return
        }
        viewModelScope.launch {
            transient.update { it.copy(submittingLicense = true, errorMessage = null) }
            runCatching { activationRepository.activateLicense(code) }
                .onSuccess {
                    transient.update {
                        it.copy(
                            submittingLicense = false,
                            licenseCode = "",
                            qrDialog = null,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    transient.update {
                        it.copy(
                            submittingLicense = false,
                            errorMessage = error.userMessage("Activation de la licence impossible."),
                        )
                    }
                }
        }
    }

    fun showXtreamSetupQr() {
        transient.update {
            it.copy(
                qrDialog = ProfileQrState(
                    title = "Configuration Xtream",
                    subtitle = "Generation d'un lien securise pour renseigner ou remplacer les identifiants Xtream de cette TV.",
                    url = "",
                    loading = true,
                ),
            )
        }
        viewModelScope.launch {
            runCatching { activationRepository.createPlaylistSetupSession() }
                .onSuccess { session ->
                    transient.update {
                        it.copy(
                            qrDialog = ProfileQrState(
                                title = "Configurer Xtream sur telephone",
                                subtitle = "Scannez le QR code, saisissez host, utilisateur et mot de passe. La TV recevra les identifiants chiffres automatiquement.",
                                url = session.qrUrl,
                                code = session.shortCode,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    transient.update {
                        it.copy(
                            qrDialog = ProfileQrState(
                                title = "Configuration Xtream",
                                subtitle = "Le lien de configuration n'a pas pu etre genere.",
                                url = "",
                                error = error.userMessage("Lien de configuration indisponible."),
                            ),
                        )
                    }
                }
        }
    }

    fun showXtreamShopQr() {
        viewModelScope.launch {
            val device = activationRepository.currentDisplayDevice()
            transient.update {
                it.copy(
                    qrDialog = ProfileQrState(
                        title = "Acheter ou prolonger Xtream",
                        subtitle = "Scannez ce QR code pour continuer sur le portail. Le parcours Xtream sera finalise sur mobile, pas avec la telecommande.",
                        url = "${activationBaseUrl()}account/?source=tv&intent=xtream&${tvDeviceQuery(device.publicDeviceCode, device.deviceId)}",
                    ),
                )
            }
        }
    }

    fun dismissQr() {
        transient.update { it.copy(qrDialog = null, errorMessage = null) }
    }
}

data class ProfileUiState(
    val deviceId: String = "",
    val publicDeviceCode: String = "",
    val activationStatusLabel: String = "Verification",
    val licenseExpiresAt: String = "",
    val usageMode: UsageMode = UsageMode.Unknown,
    val xtreamHost: String = "",
    val xtreamUsername: String = "",
    val xtreamExpiresAt: String = "",
    val xtreamConnections: String = "",
    val hasXtream: Boolean = false,
    val xtreamAccounts: List<XtreamAccount> = emptyList(),
    val activeXtreamAccountId: String = "",
    val account: AccountProfile = emptyAccountProfile(),
    val refreshing: Boolean = false,
    val licenseCode: String = "",
    val submittingLicense: Boolean = false,
    val errorMessage: String? = null,
    val qrDialog: ProfileQrState? = null,
) {
    val tvCode: String = publicDeviceCode.ifBlank { "Generation..." }
    val activeXtreamAccount: XtreamAccount?
        get() = xtreamAccounts.firstOrNull { it.id == activeXtreamAccountId } ?: xtreamAccounts.firstOrNull()
}

data class ProfileQrState(
    val title: String,
    val subtitle: String,
    val url: String,
    val code: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val allowsLicenseEntry: Boolean = false,
)

private data class ProfileTransientState(
    val refreshing: Boolean = false,
    val licenseCode: String = "",
    val submittingLicense: Boolean = false,
    val errorMessage: String? = null,
    val qrDialog: ProfileQrState? = null,
)

enum class UsageMode(
    val label: String,
    val description: String,
    val renewalHint: String,
    val primaryCta: String,
    val color: Color,
) {
    Trial(
        label = "ESSAI GRATUIT",
        description = "Essai gratuit 7 jours sans publicite",
        renewalHint = "Acheter une licence avant la fin de l'essai pour rester sans publicite.",
        primaryCta = "Passer Premium",
        color = SmartVisionColors.CyanAccent,
    ),
    Premium(
        label = "PREMIUM",
        description = "Licence SmartVision payante active",
        renewalHint = "Prolongez depuis le portail si la licence approche de l'expiration.",
        primaryCta = "Prolonger",
        color = SmartVisionColors.Success,
    ),
    FreeAds(
        label = "GRATUIT AVEC PUBS",
        description = "Acces gratuit finance par la publicite",
        renewalHint = "Acheter une licence supprime les publicites.",
        primaryCta = "Supprimer les pubs",
        color = SmartVisionColors.Warning,
    ),
    Unknown(
        label = "COMPTE",
        description = "Statut de compte en verification",
        renewalHint = "Actualisez le statut si les informations semblent incorrectes.",
        primaryCta = "Acheter licence",
        color = SmartVisionColors.TextSecondary,
    ),
}

private fun buildProfileState(
    activation: StoredActivationState,
    accounts: List<XtreamAccount>,
    activeAccountId: String,
    activeAccount: XtreamAccount?,
    account: AccountProfile,
    transient: ProfileTransientState,
): ProfileUiState {
    val usageMode = when (activation.monetizationStatus()) {
        MonetizationStatus.TRIAL_ACTIVE -> UsageMode.Trial
        MonetizationStatus.PREMIUM_ACTIVE -> UsageMode.Premium
        MonetizationStatus.FREE_WITH_ADS -> UsageMode.FreeAds
        MonetizationStatus.TRIAL_EXPIRED,
        MonetizationStatus.LICENSE_EXPIRED,
        null,
        -> UsageMode.Unknown
    }
    val activationStatusLabel = when (activation.monetizationStatus()) {
        MonetizationStatus.TRIAL_ACTIVE -> "Essai gratuit actif"
        MonetizationStatus.PREMIUM_ACTIVE -> "Premium actif"
        MonetizationStatus.FREE_WITH_ADS -> "Gratuit avec pubs"
        MonetizationStatus.TRIAL_EXPIRED -> "Essai expire"
        MonetizationStatus.LICENSE_EXPIRED -> "Licence expiree"
        null -> "Verification"
    }
    val hasXtream = activeAccount != null
    val xtreamUsername = account.usernameMasked.takeIf { it.isNotBlank() }
        ?: activeAccount?.username?.masked().orEmpty()
    val connections = if (account.activeConnections != null || account.maxConnections != null) {
        "${account.activeConnections ?: 0}/${account.maxConnections ?: 0}"
    } else {
        ""
    }
    return ProfileUiState(
        deviceId = activation.deviceId,
        publicDeviceCode = activation.publicDeviceCode,
        activationStatusLabel = activationStatusLabel,
        licenseExpiresAt = activation.expiresAt.orEmpty(),
        usageMode = usageMode,
        xtreamHost = account.host.ifBlank { activeAccount?.host.orEmpty() },
        xtreamUsername = xtreamUsername,
        xtreamExpiresAt = account.expirationDate.orEmpty(),
        xtreamConnections = connections,
        hasXtream = hasXtream,
        xtreamAccounts = accounts,
        activeXtreamAccountId = activeAccountId,
        account = account,
        refreshing = transient.refreshing,
        licenseCode = transient.licenseCode,
        submittingLicense = transient.submittingLicense,
        errorMessage = transient.errorMessage,
        qrDialog = transient.qrDialog,
    )
}

private fun String.masked(): String =
    when {
        length <= 2 -> "***"
        length <= 5 -> take(1) + "***"
        else -> take(2) + "****" + takeLast(2)
    }

private fun activationBaseUrl(): String =
    BuildConfig.ACTIVATION_BASE_URL.ifBlank { "https://smartvisions.net/" }
        .trim()
        .trimEnd('/') + "/"

private data class ProfileDeviceDisplay(
    val deviceId: String,
    val publicDeviceCode: String,
)

private suspend fun ActivationRepository.currentDisplayDevice(): ProfileDeviceDisplay {
    val state = localState.first()
    val publicCode = state.publicDeviceCode.ifBlank { getOrCreateLocalPublicCode() }
    val deviceId = state.deviceId.ifBlank { getOrCreateDeviceId() }
    return ProfileDeviceDisplay(deviceId = deviceId, publicDeviceCode = publicCode)
}

private fun tvDeviceQuery(publicDeviceCode: String, deviceId: String): String =
    if (publicDeviceCode.isNotBlank()) {
        "device=$publicDeviceCode"
    } else {
        "device_id=$deviceId"
    }

private fun Throwable.userMessage(defaultMessage: String): String =
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
