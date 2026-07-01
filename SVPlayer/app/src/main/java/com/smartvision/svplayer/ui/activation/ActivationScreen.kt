package com.smartvision.svplayer.ui.activation

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tv
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.smartvision.svplayer.R
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType

@Composable
fun ActivationScreen(
    state: ActivationUiState,
    onRetry: () -> Unit,
    onRefreshSession: () -> Unit,
    onCheckNow: () -> Unit,
    onActivateLicense: (String) -> Unit,
    onStartTrial: () -> Unit,
    onContinueFreeWithAds: () -> Unit,
    onShowActivationForm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPurchaseQr by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SmartVisionActivationBackground()),
    ) {
        ActivationContent(
            state = state,
            onActivateLicense = onActivateLicense,
            onStartTrial = onStartTrial,
            onCheckNow = onCheckNow,
            onRetry = onRetry,
            onContinueFreeWithAds = onContinueFreeWithAds,
            onShowActivationForm = onShowActivationForm,
            onBuyPremium = { showPurchaseQr = true },
        )
    }
    if (showPurchaseQr) {
        ActivationPurchaseDialog(
            purchaseUrl = state.purchaseUrl,
            onDismiss = { showPurchaseQr = false },
        )
    }
}

@Composable
private fun ActivationContent(
    state: ActivationUiState,
    onActivateLicense: (String) -> Unit,
    onStartTrial: () -> Unit,
    onCheckNow: () -> Unit,
    onRetry: () -> Unit,
    onContinueFreeWithAds: () -> Unit,
    onShowActivationForm: () -> Unit,
    onBuyPremium: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        ScaledActivationLayout(
            scale = ActivationContentScale,
        ) {
            if (state.showFreeWithAdsChoice) {
                TrialExpiredPanel(
                    state = state,
                    onContinueFreeWithAds = onContinueFreeWithAds,
                    onBuyLicense = onBuyPremium,
                    onEnterLicense = onShowActivationForm,
                )
            } else {
                ActivationMainPanel(
                    state = state,
                    onActivateLicense = onActivateLicense,
                    onStartTrial = onStartTrial,
                    onCheckNow = onCheckNow,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun ActivationMainPanel(
    state: ActivationUiState,
    onActivateLicense: (String) -> Unit,
    onStartTrial: () -> Unit,
    onCheckNow: () -> Unit,
    onRetry: () -> Unit,
) {
    var licenseCode by remember { mutableStateOf("") }
    val licenseFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { licenseFocus.requestFocus() }

    GlassShell(width = 1000.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
            ) {
                SmartVisionLogo()
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Activer SmartVision",
                    color = SmartVisionColors.TextPrimary,
                    style = ActivationTitleStyle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Activez votre appareil ou demarrez un essai gratuit.",
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Label,
                )
                Spacer(Modifier.height(20.dp))
                DeviceCodeCard(
                    label = "Identifiant appareil",
                    code = state.publicDeviceCode.ifBlank { state.shortCode.ifBlank { "------" } },
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Saisir votre code de licence",
                    color = SmartVisionColors.TextPrimary,
                    style = SmartVisionType.Label,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                LicenseInput(
                    value = licenseCode,
                    onValueChange = { licenseCode = it },
                    focusRequester = licenseFocus,
                    enabled = !state.activationBusy,
                )
                Spacer(Modifier.height(8.dp))
                TvButton(
                    text = if (state.activationBusy) "Activation..." else "Activer la licence",
                    onClick = { onActivateLicense(licenseCode) },
                    enabled = !state.activationBusy,
                    leadingIcon = Icons.Default.Key,
                    contentPadding = PaddingValues(horizontal = 26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "ou",
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.TitleS,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(7.dp))
                TvButton(
                    text = if (state.activationBusy && state.statusLabel.contains("essai", ignoreCase = true)) {
                        "Demarrage de l essai..."
                    } else {
                        "Essai gratuit 7 jours"
                    },
                    onClick = onStartTrial,
                    enabled = !state.activationBusy,
                    leadingIcon = Icons.Default.CardGiftcard,
                    variant = TvButtonVariant.Secondary,
                    contentPadding = PaddingValues(horizontal = 26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                )
                if (state.errorMessage != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = state.errorMessage,
                        color = SmartVisionColors.Error,
                        style = SmartVisionType.Label,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Besoin d'aide ? Rendez-vous sur smartvisions.net/support",
                    color = SmartVisionColors.TextSecondary.copy(alpha = 0.82f),
                    style = SmartVisionType.Caption,
                )
            }

            VerticalDivider()

            ActivationQrColumn(
                state = state,
            )
        }
        SecureFooter()
    }
}

@Composable
private fun ActivationQrColumn(
    state: ActivationUiState,
) {
    Column(
        modifier = Modifier.width(282.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = buildAnnotatedString {
                append("Scannez pour ")
                withStyle(SpanStyle(color = SmartVisionColors.Primary, fontWeight = FontWeight.Bold)) {
                    append("acheter")
                }
                append(" une licence")
            },
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Label,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        QrCard(content = state.purchaseUrl, size = 205)
        Spacer(Modifier.height(22.dp))
        StepsCard(
            steps = listOf(
                ActivationStep("1", "Scanner", "ce QR code", Icons.Default.QrCode2),
                ActivationStep("2", "Acheter", "une licence", Icons.Default.ShoppingCart),
                ActivationStep("3", "Activation", "automatique", Icons.Default.CheckCircle),
            ),
        )
    }
}

@Composable
private fun TrialExpiredPanel(
    state: ActivationUiState,
    onContinueFreeWithAds: () -> Unit,
    onBuyLicense: () -> Unit,
    onEnterLicense: () -> Unit,
) {
    val continueFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { continueFocus.requestFocus() }

    GlassShell(width = 1000.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
            ) {
                SmartVisionLogo()
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Continuer avec SmartVision",
                    color = SmartVisionColors.TextPrimary,
                    style = ActivationTitleStyle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Votre acces gratuit sans publicite est termine. Vous pouvez activer SmartVision Premium ou continuer gratuitement avec des publicites video raisonnables.",
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Label,
                )
                Spacer(Modifier.height(26.dp))
                ChoiceButton(
                    text = "Continuer gratuit avec pubs",
                    subtitle = "Publicites video uniquement dans le lecteur",
                    icon = Icons.Default.Tv,
                    dangerFocus = true,
                    focusRequester = continueFocus,
                    enabled = !state.activationBusy,
                    onClick = onContinueFreeWithAds,
                )
                Spacer(Modifier.height(14.dp))
                ChoiceButton(
                    text = "Acheter Premium",
                    icon = Icons.Default.ShoppingCart,
                    enabled = !state.activationBusy,
                    onClick = onBuyLicense,
                )
                Spacer(Modifier.height(14.dp))
                ChoiceButton(
                    text = "Saisir une licence",
                    icon = Icons.Default.Key,
                    enabled = !state.activationBusy,
                    onClick = onEnterLicense,
                )
                state.errorMessage?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = SmartVisionColors.Error, style = SmartVisionType.Label)
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Vous pourrez acheter une licence a tout moment pour supprimer les publicites.",
                    color = SmartVisionColors.TextSecondary.copy(alpha = 0.82f),
                    style = SmartVisionType.Caption,
                )
            }

            VerticalDivider()

            Column(
                modifier = Modifier.width(282.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Acheter sans attendre",
                    color = SmartVisionColors.Primary,
                    style = SmartVisionType.TitleS,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Scannez pour ")
                        withStyle(SpanStyle(color = SmartVisionColors.Primary, fontWeight = FontWeight.Bold)) {
                            append("acheter")
                        }
                        append(" une licence\net supprimer les publicites.")
                    },
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Label,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                QrCard(content = state.purchaseUrl, size = 205)
                Spacer(Modifier.height(22.dp))
                StepsCard(
                    steps = listOf(
                        ActivationStep("1", "Scanner", "ce QR code", Icons.Default.QrCode2),
                        ActivationStep("2", "Acheter", "une licence", Icons.Default.ShoppingCart),
                        ActivationStep("3", "Activer", "", Icons.Default.CheckCircle),
                    ),
                )
            }
        }
    }
}

@Composable
private fun ChoiceButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    selected: Boolean = false,
    dangerFocus: Boolean = false,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
) {
    TvButton(
        text = if (subtitle == null) text else "$text   $subtitle",
        onClick = onClick,
        leadingIcon = icon,
        selected = selected,
        enabled = enabled,
        focusRequester = focusRequester,
        variant = when {
            dangerFocus -> TvButtonVariant.Exit
            selected -> TvButtonVariant.Primary
            else -> TvButtonVariant.Secondary
        },
        contentPadding = PaddingValues(horizontal = 28.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
    )
}

@Composable
private fun ActivationPurchaseDialog(
    purchaseUrl: String,
    onDismiss: () -> Unit,
) {
    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { closeFocus.requestFocus() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f)),
            contentAlignment = Alignment.Center,
        ) {
            GlassShell(width = 720.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Acheter SmartVision Premium",
                        color = SmartVisionColors.TextPrimary,
                        style = ActivationTitleStyle,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Scannez ce QR code avec votre telephone. Premium supprime immediatement toutes les publicites.",
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Body,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(22.dp))
                    QrCard(content = purchaseUrl, size = 250)
                    Spacer(Modifier.height(22.dp))
                    TvButton(
                        text = "Fermer",
                        onClick = onDismiss,
                        focusRequester = closeFocus,
                        variant = TvButtonVariant.Secondary,
                        modifier = Modifier.height(48.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LicenseInput(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    enabled: Boolean,
) {
    val fieldFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var editing by remember { mutableStateOf(false) }
    var containerFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(7.dp)
    val focusStyle = LocalTvFocusStyle.current
    val borderColor = if (editing || containerFocused) {
        focusStyle.accent
    } else {
        SmartVisionColors.Primary.copy(alpha = 0.72f)
    }

    LaunchedEffect(editing) {
        if (editing) {
            fieldFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { containerFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (!enabled) return@onPreviewKeyEvent false
                when {
                    event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) -> {
                        editing = true
                        true
                    }
                    event.type == KeyEventType.KeyDown && event.key == Key.Back && editing -> {
                        editing = false
                        keyboardController?.hide()
                        true
                    }
                    else -> false
                }
            }
            .focusable(enabled = enabled && !editing)
            .clip(shape)
            .background(
                if (editing || containerFocused) {
                    focusStyle.background
                } else {
                    Color(0xFF050D1A).copy(alpha = 0.82f)
                },
            )
            .border(BorderStroke(if (editing || containerFocused) focusStyle.borderWidth else 1.dp, borderColor), shape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = { next ->
                onValueChange(next.replace(Regex("[\\s-]+"), "").uppercase().take(10))
            },
            enabled = enabled && editing,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            textStyle = SmartVisionType.Body.copy(
                color = SmartVisionColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            ),
            cursorBrush = SolidColor(focusStyle.accent),
            modifier = Modifier
                .weight(1f)
                .focusRequester(fieldFocusRequester)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && editing) {
                        editing = false
                    }
                },
            decorationBox = { inner ->
                if (value.isBlank()) {
                    Text(
                        text = "Entrez le code de licence achete",
                        color = SmartVisionColors.TextSecondary.copy(alpha = 0.72f),
                        style = SmartVisionType.Label,
                    )
                }
                inner()
            },
        )
        Icon(
            imageVector = Icons.Default.Keyboard,
            contentDescription = null,
            tint = SmartVisionColors.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun DeviceCodeCard(
    label: String,
    code: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0A182B).copy(alpha = 0.82f))
            .border(BorderStroke(1.dp, Color(0xFF29476E)), RoundedCornerShape(10.dp))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SmartVisionColors.Primary.copy(alpha = 0.16f))
                .border(BorderStroke(2.dp, SmartVisionColors.Primary), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = SmartVisionColors.Primary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(20.dp))
        Text(
            text = label,
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.Label,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = code.chunked(3).joinToString(""),
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.TitleM,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StepsCard(steps: List<ActivationStep>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF071629).copy(alpha = 0.78f))
            .border(BorderStroke(1.dp, Color(0xFF2B4364)), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        steps.forEach { step ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(27.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(SmartVisionColors.Primary)
                        .border(BorderStroke(1.dp, SmartVisionColors.CyanAccent), RoundedCornerShape(99.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(step.number, color = Color.White, style = SmartVisionType.Caption, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Icon(step.icon, contentDescription = null, tint = SmartVisionColors.TextPrimary, modifier = Modifier.size(21.dp))
                Spacer(Modifier.height(4.dp))
                Text(step.label, color = SmartVisionColors.TextPrimary, style = SmartVisionType.Caption, fontWeight = FontWeight.SemiBold)
                if (step.subtitle.isNotBlank()) {
                    Text(step.subtitle, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Caption)
                }
            }
        }
    }
}

@Composable
private fun GlassShell(
    width: androidx.compose.ui.unit.Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .width(width)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .clip(RoundedCornerShape(20.dp))
            .background(GlassPanelBrush())
            .border(BorderStroke(1.dp, Color(0xFF2A3B58)), RoundedCornerShape(20.dp))
            .padding(horizontal = 28.dp, vertical = 18.dp),
        content = content,
    )
}

@Composable
private fun SmartVisionLogo(
    modifier: Modifier = Modifier
        .width(190.dp)
        .height(48.dp),
) {
    Image(
        painter = painterResource(R.drawable.smartvision_logo_wide),
        contentDescription = "SmartVision IPTV Player",
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@Composable
private fun QrCard(
    content: String,
    size: Int,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFDDE8FF)), RoundedCornerShape(16.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (content.isBlank()) {
            CircularProgressIndicator(color = SmartVisionColors.Primary)
        } else {
            val bitmap = remember(content) { createQrBitmap(content, 520) }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR code SmartVision",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(430.dp)
            .background(Color(0xFF2D4263).copy(alpha = 0.82f)),
    )
}

@Composable
private fun SecureFooter() {
    Spacer(Modifier.height(14.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = SmartVisionColors.TextSecondary.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Connexion securisee - Vos donnees sont protegees.",
            color = SmartVisionColors.TextSecondary.copy(alpha = 0.72f),
            style = SmartVisionType.Caption,
        )
    }
}

private val ActivationTitleStyle = TextStyle(
    fontSize = 32.sp,
    lineHeight = 38.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.sp,
)

private data class ActivationStep(
    val number: String,
    val label: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
private fun SmartVisionActivationBackground(): Brush =
    Brush.radialGradient(
        colors = listOf(
            SmartVisionColors.PrimaryDark.copy(alpha = 0.52f),
            Color(0xFF06101F),
            Color(0xFF01040B),
        ),
        radius = 1500f,
    )

@Composable
private fun GlassPanelBrush(): Brush =
    Brush.verticalGradient(
        listOf(
            Color(0xFF102038).copy(alpha = 0.88f),
            Color(0xFF071322).copy(alpha = 0.96f),
        ),
    )

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
