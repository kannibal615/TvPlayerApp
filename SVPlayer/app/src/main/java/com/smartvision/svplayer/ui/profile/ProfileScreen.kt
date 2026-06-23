package com.smartvision.svplayer.ui.profile

import android.graphics.Bitmap
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.core.config.XtreamAccount
import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.activation.ActivationException
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.data.activation.StoredActivationState
import com.smartvision.svplayer.data.repository.emptyAccountProfile
import com.smartvision.svplayer.domain.model.AccountProfile
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onSyncCatalog: () -> Unit,
) {
    val container = LocalAppContainer.current
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

    ProfileScreen(
        state = state,
        onBack = onBack,
        onSettings = onSettings,
        onRefresh = viewModel::refresh,
        onSyncCatalog = onSyncCatalog,
        onShowLicenseQr = viewModel::showLicenseQr,
        onShowXtreamSetupQr = viewModel::showXtreamSetupQr,
        onShowXtreamShopQr = viewModel::showXtreamShopQr,
        onDeleteXtreamAccount = { accountId ->
            container.accountManager.delete(accountId)
            onSyncCatalog()
        },
        onDismissQr = viewModel::dismissQr,
    )
}

@Composable
private fun ProfileScreen(
    state: ProfileUiState,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
    onSyncCatalog: () -> Unit,
    onShowLicenseQr: () -> Unit,
    onShowXtreamSetupQr: () -> Unit,
    onShowXtreamShopQr: () -> Unit,
    onDeleteXtreamAccount: (String) -> Unit,
    onDismissQr: () -> Unit,
) {
    BackHandler(onBack = onBack)

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
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                LicensePanel(
                    state = state,
                    onRefresh = onRefresh,
                    onShowLicenseQr = onShowLicenseQr,
                    modifier = Modifier.weight(1f),
                )
                XtreamPanel(
                    state = state,
                    onShowXtreamSetupQr = onShowXtreamSetupQr,
                    onShowXtreamShopQr = onShowXtreamShopQr,
                    onDeleteXtreamAccount = onDeleteXtreamAccount,
                    onSyncCatalog = onSyncCatalog,
                    modifier = Modifier.weight(1f),
                )
            }

            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ConversionPanel(
                    usageMode = state.usageMode,
                    modifier = Modifier.weight(1.1f),
                )
                DevicePanel(
                    state = state,
                    modifier = Modifier.weight(0.9f),
                )
            }
        }
    }

    state.qrDialog?.let { qr ->
        SmartVisionQrDialog(
            title = qr.title,
            subtitle = qr.subtitle,
            qrUrl = qr.url,
            code = qr.code,
            loading = qr.loading,
            error = qr.error,
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
        ProfileInfoRow("Identifiant appareil", state.deviceId.ifBlank { "Generation..." })
        ProfileInfoRow("Renouvellement", state.usageMode.renewalHint)
        Spacer(Modifier.weight(1f))
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
    }
}

@Composable
private fun XtreamPanel(
    state: ProfileUiState,
    onShowXtreamSetupQr: () -> Unit,
    onShowXtreamShopQr: () -> Unit,
    onDeleteXtreamAccount: (String) -> Unit,
    onSyncCatalog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfilePanel(
        title = "Identifiants Xtream",
        icon = Icons.Default.CloudSync,
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ProfileMetric("Compte", if (state.hasXtream) "Configure" else "Absent", Modifier.weight(1f), if (state.hasXtream) SmartVisionColors.Success else SmartVisionColors.Warning)
            ProfileMetric("Expiration Xtream", state.xtreamExpiresAt.ifBlank { "A synchroniser" }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        ProfileInfoRow("Serveur", state.xtreamHost.ifBlank { "Aucun serveur configure" })
        ProfileInfoRow("Utilisateur", state.xtreamUsername.ifBlank { "Non configure" })
        ProfileInfoRow("Connexions", state.xtreamConnections.ifBlank { "Non disponible" })
        ProfileInfoRow("Comptes locaux", state.xtreamAccounts.size.toString())
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            TvButton(
                text = if (state.hasXtream) "Modifier par QR" else "Configurer par QR",
                onClick = onShowXtreamSetupQr,
                leadingIcon = Icons.Default.QrCode2,
                modifier = Modifier
                    .weight(1.1f)
                    .height(46.dp),
            )
            TvButton(
                text = "Achat Xtream",
                onClick = onShowXtreamShopQr,
                leadingIcon = Icons.Default.ShoppingCart,
                variant = TvButtonVariant.Secondary,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            TvButton(
                text = "Synchroniser",
                onClick = onSyncCatalog,
                leadingIcon = Icons.Default.CloudSync,
                variant = TvButtonVariant.Secondary,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
            )
            TvButton(
                text = "Supprimer local",
                onClick = { onDeleteXtreamAccount(state.activeXtreamAccountId) },
                enabled = state.activeXtreamAccountId.isNotBlank(),
                leadingIcon = Icons.Default.Delete,
                variant = TvButtonVariant.Secondary,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
            )
        }
    }
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
        Spacer(Modifier.weight(1f))
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
    code: String? = null,
    loading: Boolean = false,
    error: String? = null,
    width: androidx.compose.ui.unit.Dp = 760.dp,
    licenseCode: String = "",
    onLicenseCodeChange: ((String) -> Unit)? = null,
    onSubmitLicenseCode: (() -> Unit)? = null,
    submittingLicense: Boolean = false,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .width(width)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xF2081324))
                .border(BorderStroke(1.dp, SmartVisionColors.Primary.copy(alpha = 0.62f)), RoundedCornerShape(12.dp))
                .padding(28.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(252.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    loading -> CircularProgressIndicator(color = SmartVisionColors.Primary)
                    qrUrl.isBlank() -> Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color(0xFF10203A), modifier = Modifier.size(82.dp))
                    else -> {
                        val bitmap = androidx.compose.runtime.remember(qrUrl) { createQrBitmap(qrUrl, 512) }
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR code SmartVision", modifier = Modifier.fillMaxSize())
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleM, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                Text(subtitle, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Body, maxLines = 6, overflow = TextOverflow.Ellipsis)
                code?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(14.dp))
                    Text(it.chunked(3).joinToString(" "), color = SmartVisionColors.CyanAccent, style = SmartVisionType.TitleS, fontWeight = FontWeight.Bold)
                }
                if (onLicenseCodeChange != null && onSubmitLicenseCode != null) {
                    val fieldFocusRequester = remember { FocusRequester() }
                    val inputFocusRequester = remember { FocusRequester() }
                    val keyboardController = LocalSoftwareKeyboardController.current
                    var editing by remember { mutableStateOf(false) }

                    LaunchedEffect(editing) {
                        if (editing) {
                            inputFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Code licence SmartVision",
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Caption,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .focusRequester(fieldFocusRequester)
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
                            .background(SmartVisionColors.Surface, RoundedCornerShape(6.dp))
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (editing) SmartVisionColors.CyanAccent else SmartVisionColors.Primary.copy(alpha = 0.72f),
                                ),
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicTextField(
                            value = licenseCode,
                            onValueChange = { onLicenseCodeChange(it.uppercase().take(24)) },
                            enabled = editing,
                            singleLine = true,
                            textStyle = SmartVisionType.Body.copy(color = SmartVisionColors.TextPrimary),
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
                                        text = "Saisir le code achete",
                                        color = SmartVisionColors.TextSecondary.copy(alpha = 0.72f),
                                        style = SmartVisionType.Body,
                                    )
                                }
                                inner()
                            },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    TvButton(
                        text = if (submittingLicense) "Activation..." else "Activer ce code",
                        onClick = onSubmitLicenseCode,
                        enabled = !submittingLicense && licenseCode.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                    )
                }
                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(error, color = SmartVisionColors.Error, style = SmartVisionType.Label)
                }
                Spacer(Modifier.height(22.dp))
                TvButton(
                    text = "Fermer",
                    onClick = onDismiss,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier.height(44.dp),
                )
            }
        }
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
        buildProfileState(activation, accounts, activeAccountId.orEmpty(), activeAccount, account, transient)
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
            runCatching {
                activationRepository.getOrCreateDeviceId()
                activationRepository.checkStatus()
            }.onFailure { error ->
                transient.update {
                    it.copy(errorMessage = error.userMessage("Impossible de verifier le compte."))
                }
            }
            transient.update { it.copy(refreshing = false) }
        }
    }

    fun showLicenseQr() {
        viewModelScope.launch {
            val deviceId = activationRepository.getOrCreateDeviceId()
            transient.update {
                it.copy(
                    qrDialog = ProfileQrState(
                        title = "Acheter ou prolonger la licence",
                        subtitle = "Scannez ce QR code avec votre telephone pour choisir une licence SmartVision. Le portail associera l'achat a cet appareil.",
                        url = "${activationBaseUrl()}account/?source=tv&intent=license&device_id=$deviceId&plan=year_1",
                    ),
                )
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
            val deviceId = activationRepository.getOrCreateDeviceId()
            transient.update {
                it.copy(
                    qrDialog = ProfileQrState(
                        title = "Acheter ou prolonger Xtream",
                        subtitle = "Scannez ce QR code pour continuer sur le portail. Le parcours Xtream sera finalise sur mobile, pas avec la telecommande.",
                        url = "${activationBaseUrl()}account/?source=tv&intent=xtream&device_id=$deviceId",
                    ),
                )
            }
        }
    }

    fun dismissQr() {
        transient.update { it.copy(qrDialog = null) }
    }
}

data class ProfileUiState(
    val deviceId: String = "",
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
    val errorMessage: String? = null,
    val qrDialog: ProfileQrState? = null,
)

data class ProfileQrState(
    val title: String,
    val subtitle: String,
    val url: String,
    val code: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

private data class ProfileTransientState(
    val refreshing: Boolean = false,
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
    val usageMode = when (activation.activationType) {
        "trial_demo" -> UsageMode.Trial
        "smartvision_code", "own_xtream" -> UsageMode.Premium
        "free_ads" -> UsageMode.FreeAds
        else -> UsageMode.Unknown
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
        activationStatusLabel = activation.status.ifBlank { "pending" }.replaceFirstChar { it.uppercase() },
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
    BuildConfig.ACTIVATION_BASE_URL.ifBlank { "https://app.smartvisions.net/" }
        .trim()
        .trimEnd('/') + "/"

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
