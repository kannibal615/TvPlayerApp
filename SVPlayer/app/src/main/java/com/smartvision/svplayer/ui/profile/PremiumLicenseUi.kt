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
internal fun LicensePanel(
    state: ProfileUiState,
    strings: SmartVisionStrings,
    onRefresh: () -> Unit,
    onShowLicenseQr: () -> Unit,
    onShowPrivacyOptions: () -> Unit,
    privacyOptionsRequired: Boolean,
    embedded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val premiumPurchaseUrl = remember(state.publicDeviceCode, state.deviceId) {
        "${activationBaseUrl()}account/?source=tv&intent=license&" +
            "${tvDeviceQuery(state.publicDeviceCode, state.deviceId)}&plan=year_1"
    }
    val details: @Composable ColumnScope.() -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ProfileMetric("Statut", state.activationStatusLabel, Modifier.weight(1f), state.usageMode.color)
            ProfileMetric("Expiration", state.licenseExpiresAt.ifBlank { "Non disponible" }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        ProfileInfoRow("Type", state.usageMode.description)
        ProfileInfoRow("Code TV", state.tvCode)
        ProfileInfoRow("Identifiant appareil", state.deviceId.ifBlank { "Generation..." })
        ProfileInfoRow("Publicites", if (state.usageMode == UsageMode.FreeAds) "Actives" else "Desactivees")
        ProfileInfoRow("Renouvellement", state.usageMode.renewalHint)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            TvButton(
                text = state.usageMode.localizedPrimaryCta(strings),
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
            if (state.usageMode == UsageMode.FreeAds && privacyOptionsRequired) {
                TvButton(
                    text = "Confidentialite pubs",
                    onClick = onShowPrivacyOptions,
                    leadingIcon = Icons.Default.Security,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                )
            }
        }
    }
    val content: @Composable ColumnScope.() -> Unit = {
        if (embedded) {
            EmbeddedPremiumLicenseCard(
                state = state,
                strings = strings,
                purchaseUrl = premiumPurchaseUrl,
                onActivate = onShowLicenseQr,
            )
        } else {
            details()
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

@Composable
private fun EmbeddedPremiumLicenseCard(
    state: ProfileUiState,
    strings: SmartVisionStrings,
    purchaseUrl: String,
    onActivate: () -> Unit,
) {
    val gold = Color(0xFFFFD36A)
    val benefits = remember(strings) {
        listOf(
            strings.premiumBenefitNoAds,
            strings.premiumBenefitRecorder,
            strings.premiumBenefitMediaCenter,
            strings.premiumBenefitPhoneTransfer,
            strings.premiumBenefitMultiProfile,
            strings.premiumBenefitParentalControl,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF10243B),
                        Color(0xFF071426),
                        Color(0xFF030A15),
                    ),
                    center = Offset(760f, 20f),
                    radius = 1050f,
                ),
            )
            .border(
                BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.92f)),
                RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = gold,
                    modifier = Modifier.size(31.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = strings.premiumPurchaseTitle,
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.TitleS,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = strings.premiumBenefitsSubtitle,
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Caption,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.height(13.dp))
            benefits.forEach { benefit ->
                PremiumBenefitRow(text = benefit)
            }
            Spacer(Modifier.height(13.dp))

            TvButton(
                text = strings.premiumActivate,
                onClick = onActivate,
                leadingIcon = Icons.Default.Key,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
            )
        }

        PremiumPreviewQr(
            purchaseUrl = purchaseUrl,
            tvCode = state.tvCode,
            title = strings.premiumPurchaseTitle,
            subtitle = strings.premiumPreviewSubtitle,
            codeLabel = "TV CODE :",
            modifier = Modifier.width(270.dp),
        )
    }
}

@Composable
private fun PremiumBenefitRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(29.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SmartVisionColors.Success,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.Body,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SmartVisionQrDialog(
    strings: SmartVisionStrings,
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
            strings = strings,
            title = title,
            subtitle = subtitle,
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
        strings = strings,
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
    strings: SmartVisionStrings,
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
    val actionFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(actionLabel, onAction) {
        delay(ProfileFocusRequestDelayMillis)
        runCatching {
            if (actionLabel != null && onAction != null) actionFocusRequester.requestFocus()
            else closeFocusRequester.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 64.dp, vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                ScaledActivationLayout {
                    Row(
                        modifier = Modifier
                            .width(680.dp)
                            .height(410.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF0A1A31).copy(alpha = 0.98f),
                                        Color(0xFF030D1C).copy(alpha = 0.99f),
                                    ),
                                ),
                            )
                            .border(BorderStroke(1.dp, Color(0xFF2C568C)), RoundedCornerShape(18.dp))
                            .padding(horizontal = 26.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        ) {
                            PremiumDialogLogo()
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text = title,
                                color = SmartVisionColors.TextPrimary,
                                style = SmartVisionType.TitleM,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = subtitle,
                                color = SmartVisionColors.TextSecondary,
                                style = SmartVisionType.Caption,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (error != null) {
                                Spacer(Modifier.height(10.dp))
                                Text(error, color = SmartVisionColors.Error, style = SmartVisionType.Caption)
                            }
                            Spacer(Modifier.weight(1f))
                            if (actionLabel != null && onAction != null) {
                                PremiumDialogButton(
                                    text = actionLabel,
                                    onClick = onAction,
                                    primary = true,
                                    trailingIcon = Icons.Default.ArrowForward,
                                    focusRequester = actionFocusRequester,
                                    nextFocusRequester = closeFocusRequester,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            PremiumDialogButton(
                                text = strings.close,
                                onClick = onDismiss,
                                primary = false,
                                leadingIcon = Icons.Default.Close,
                                focusRequester = closeFocusRequester,
                                previousFocusRequester = if (actionLabel != null && onAction != null) actionFocusRequester else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(340.dp)
                                .background(Color(0xFF31547C).copy(alpha = 0.72f)),
                        )
                        PremiumQrCard(
                            strings = strings,
                            qrUrl = qrUrl,
                            tvCode = tvCode,
                            loading = loading,
                            modifier = Modifier
                                .width(220.dp)
                                .height(340.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumLicenseDialog(
    strings: SmartVisionStrings,
    title: String,
    subtitle: String,
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
    val activateFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var editing by remember { mutableStateOf(false) }
    var fieldFocused by remember { mutableStateOf(false) }
    val focusStyle = LocalTvFocusStyle.current
    val canSubmitLicense = !submittingLicense && licenseCode.isNotBlank()

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
                .background(Color.Black.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center,
        ) {
            ScaledActivationLayout {
            Row(
                modifier = Modifier
                    .width(680.dp)
                    .height(410.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0A1A31).copy(alpha = 0.98f),
                                Color(0xFF030D1C).copy(alpha = 0.99f),
                            ),
                        ),
                    )
                    .border(
                        BorderStroke(1.dp, Color(0xFF2C568C)),
                        RoundedCornerShape(18.dp),
                    )
                    .padding(horizontal = 26.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    PremiumDialogLogo()
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = title,
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.TitleM,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Caption,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = strings.premiumLicenseLabel,
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.Caption,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .focusRequester(fieldFocusRequester)
                            .focusProperties {
                                down = if (canSubmitLicense) activateFocusRequester else closeFocusRequester
                            }
                            .onFocusChanged { fieldFocused = it.isFocused }
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (event.key) {
                                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                        editing = true
                                        true
                                    }
                                    Key.DirectionDown -> {
                                        editing = false
                                        keyboardController?.hide()
                                        runCatching {
                                            if (canSubmitLicense) activateFocusRequester.requestFocus()
                                            else closeFocusRequester.requestFocus()
                                        }
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
                            .clip(RoundedCornerShape(8.dp))
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
                                    if (fieldFocused || editing) focusStyle.accent else Color(0xFF31547C),
                                ),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 13.dp),
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
                                }
                            ,
                            decorationBox = { inner ->
                                if (licenseCode.isBlank()) {
                                    Text(
                                        text = strings.premiumLicenseHint,
                                        color = SmartVisionColors.TextSecondary.copy(alpha = 0.62f),
                                        style = SmartVisionType.Body,
                                    )
                                }
                                inner()
                            },
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .padding(top = 5.dp),
                    ) {
                        if (!error.isNullOrBlank()) {
                            Text(
                                text = error,
                                color = SmartVisionColors.Error,
                                style = SmartVisionType.Caption,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    
                    PremiumDialogButton(
                        text = if (submittingLicense) strings.premiumActivating else strings.premiumActivateCode,
                        onClick = onSubmitLicenseCode,
                        enabled = canSubmitLicense,
                        primary = true,
                        leadingIcon = Icons.Default.Key,
                        focusRequester = activateFocusRequester,
                        previousFocusRequester = fieldFocusRequester,
                        nextFocusRequester = closeFocusRequester,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                    )
                    
                    Spacer(Modifier.weight(1f))
                    PremiumDialogButton(
                        text = strings.close,
                        onClick = onDismiss,
                        primary = false,
                        focusRequester = closeFocusRequester,
                        previousFocusRequester = if (canSubmitLicense) activateFocusRequester else fieldFocusRequester,
                        leadingIcon = Icons.Default.Close,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(340.dp)
                        .background(Color(0xFF31547C).copy(alpha = 0.72f)),
                )

                PremiumQrCard(
                    strings = strings,
                    qrUrl = qrUrl,
                    tvCode = tvCode,
                    loading = loading,
                    modifier = Modifier
                        .width(220.dp)
                        .height(340.dp),
                )
                    }
            }
        }
    }
}

@Composable
private fun PremiumQrCard(
    strings: SmartVisionStrings,
    qrUrl: String,
    tvCode: String,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = strings.activationBuyNow,
            color = Color(0xFF2F82FF),
            style = SmartVisionType.TitleS,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = strings.activationScanToBuy,
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Caption,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .size(174.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                loading -> CircularProgressIndicator(color = com.smartvision.svplayer.ui.theme.LocalLoadingColor.current)
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
        Spacer(Modifier.height(20.dp))
        Text(
            text = strings.activationTvCode,
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.Label,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = tvCode.ifBlank { "------" },
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.TitleM,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PremiumDialogLogo() {
    Image(
        painter = painterResource(R.drawable.smartvision_logo_1),
        contentDescription = "SmartVision IPTV Player",
        modifier = Modifier
            .width(176.dp)
            .height(43.dp),
    )
}

@Composable
private fun PremiumDialogButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    previousFocusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(10.dp)
    val focusStyle = LocalTvFocusStyle.current
    val borderColor = when {
        focusState.isFocused -> focusStyle.accent
        primary -> Color(0xFF2B83FF)
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
            .focusProperties {
                if (previousFocusRequester != null) up = previousFocusRequester
                if (nextFocusRequester != null) down = nextFocusRequester
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> {
                        previousFocusRequester?.let { runCatching { it.requestFocus() } }
                        previousFocusRequester != null
                    }
                    Key.DirectionDown -> {
                        nextFocusRequester?.let { runCatching { it.requestFocus() } }
                        nextFocusRequester != null
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (enabled) onClick()
                        true
                    }
                    else -> false
                }
            }
            .clip(shape)
            .background(
                if (primary) {
                    Brush.horizontalGradient(
                        listOf(
                            if (enabled) Color(0xFF2C8CFF) else Color(0xFF31547C),
                            if (enabled) Color(0xFF1766F2) else Color(0xFF243B5B),
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
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (enabled) Color.White else Color.White.copy(alpha = 0.42f),
                modifier = Modifier.size(21.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.42f),
            style = SmartVisionType.Body,
            fontWeight = FontWeight.SemiBold,
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = if (enabled) Color.White else Color.White.copy(alpha = 0.42f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
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

internal const val ProfileFocusRequestDelayMillis = 80L
