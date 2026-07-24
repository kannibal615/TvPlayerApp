package com.smartvision.svplayer.ui.activation

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.smartvision.svplayer.R
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay

@Composable
fun ActivationScreen(
    state: ActivationUiState,
    strings: SmartVisionStrings,
    onActivateLicense: (String) -> Unit,
    onStartTrial: () -> Unit,
    onContinueFreeWithAds: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.startup_cinema_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF020A18).copy(alpha = 0.30f),
                            Color(0xFF021127).copy(alpha = 0.58f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            ScaledActivationLayout {
                UnifiedActivationPanel(
                    state = state,
                    strings = strings,
                    onActivateLicense = onActivateLicense,
                    onStartTrial = onStartTrial,
                    onContinueFreeWithAds = onContinueFreeWithAds,
                )
            }
        }
    }
}

@Composable
private fun UnifiedActivationPanel(
    state: ActivationUiState,
    strings: SmartVisionStrings,
    onActivateLicense: (String) -> Unit,
    onStartTrial: () -> Unit,
    onContinueFreeWithAds: () -> Unit,
) {
    var licenseCode by remember { mutableStateOf("") }
    var lastSubmittedAction by remember { mutableStateOf<ActivationAction?>(null) }
    val offerFocus = remember { FocusRequester() }
    val licenseFocus = remember { FocusRequester() }
    val activateFocus = remember { FocusRequester() }
    val blocked = state.offerMode == ActivationOfferMode.Blocked
    val offerBusy = state.activeAction == ActivationAction.Trial ||
        state.activeAction == ActivationAction.FreeWithAds
    val licenseBusy = state.activeAction == ActivationAction.License

    LaunchedEffect(state.offerMode, state.activationBusy, state.error) {
        if (!blocked && !state.activationBusy) {
            withFrameNanos { }
            delay(80)
            val target = if (
                state.error != null &&
                lastSubmittedAction == ActivationAction.License
            ) {
                activateFocus
            } else {
                offerFocus
            }
            runCatching { target.requestFocus() }
        }
    }

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
            .padding(horizontal = 26.dp, vertical = 18.dp)
            .imePadding(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            SmartVisionLogo()
            Spacer(Modifier.height(20.dp))
            Text(
                text = strings.activationTitle,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleM,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = strings.activationSubtitle,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            ActivationOfferButton(
                text = when {
                    blocked -> strings.activationDeviceBlocked
                    offerBusy && state.offerMode == ActivationOfferMode.FreeWithAds -> strings.activationEnablingAds
                    offerBusy -> strings.activationStartingTrial
                    state.offerMode == ActivationOfferMode.FreeWithAds -> strings.activationContinueWithAds
                    else -> strings.activationTryTrial
                },
                icon = when (state.offerMode) {
                    ActivationOfferMode.TrialAvailable -> Icons.Default.CardGiftcard
                    ActivationOfferMode.FreeWithAds -> Icons.Default.Tv
                    ActivationOfferMode.Blocked -> Icons.Default.Lock
                },
                enabled = !state.activationBusy && !blocked,
                loading = offerBusy,
                focusRequester = offerFocus,
                next = licenseFocus,
                onClick = {
                    when (state.offerMode) {
                        ActivationOfferMode.TrialAvailable -> {
                            lastSubmittedAction = ActivationAction.Trial
                            onStartTrial()
                        }
                        ActivationOfferMode.FreeWithAds -> {
                            lastSubmittedAction = ActivationAction.FreeWithAds
                            onContinueFreeWithAds()
                        }
                        ActivationOfferMode.Blocked -> Unit
                    }
                },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = strings.activationOr,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = strings.activationLicenseLabel,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Caption,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(5.dp))
            LicenseInput(
                value = licenseCode,
                onValueChange = { licenseCode = it },
                placeholder = strings.activationLicensePlaceholder,
                focusRequester = licenseFocus,
                previous = offerFocus,
                next = activateFocus,
                enabled = !state.activationBusy && !blocked,
            )
            Spacer(Modifier.height(10.dp))
            ActivationPrimaryButton(
                text = if (licenseBusy) strings.activationActivatingLicense else strings.activationActivateLicense,
                enabled = !state.activationBusy && !blocked,
                loading = licenseBusy,
                focusRequester = activateFocus,
                previous = licenseFocus,
                onClick = {
                    lastSubmittedAction = ActivationAction.License
                    onActivateLicense(licenseCode)
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .padding(top = 5.dp),
            ) {
                state.error?.let { error ->
                    Text(
                        text = error.localizedMessage(strings),
                        color = SmartVisionColors.Error,
                        style = SmartVisionType.Caption,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(340.dp)
                .background(Color(0xFF31547C).copy(alpha = 0.72f)),
        )

        Column(
            modifier = Modifier
                .width(220.dp)
                .height(340.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
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
            QrCard(
                content = state.purchaseUrl.takeIf { state.publicDeviceCode.isNotBlank() }.orEmpty(),
                description = strings.activationQrDescription,
                size = 174,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = strings.activationTvCode,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Label,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = state.publicDeviceCode.ifBlank { "------" },
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleM,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActivationOfferButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    loading: Boolean,
    focusRequester: FocusRequester,
    next: FocusRequester,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val outerShape = RoundedCornerShape(10.dp)
    val innerShape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(outerShape)
            .background(if (focused) Color(0xFF1478FF).copy(alpha = 0.58f) else Color.Transparent)
            .border(BorderStroke(1.dp, if (focused) Color(0xFF2E8CFF) else Color.Transparent), outerShape)
            .padding(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusProperties { down = next }
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> {
                            runCatching { next.requestFocus() }
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (enabled) onClick()
                            true
                        }
                        else -> false
                    }
                }
                .clip(innerShape)
                .background(
                    if (enabled) {
                        Brush.horizontalGradient(listOf(Color(0xFF39A66F), Color(0xFF2C8E5E)))
                    } else {
                        Brush.horizontalGradient(listOf(Color(0xFF315B4A), Color(0xFF27483C)))
                    },
                )
                .border(
                    BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0xFF51C58A)),
                    innerShape,
                )
                .clickable(enabled = enabled, onClick = onClick)
                .focusable(enabled = enabled)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                color = Color.White,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LicenseInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester,
    previous: FocusRequester,
    next: FocusRequester,
    enabled: Boolean,
) {
    val fieldFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusStyle = LocalTvFocusStyle.current
    var editing by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val outerShape = RoundedCornerShape(10.dp)
    val innerShape = RoundedCornerShape(8.dp)

    LaunchedEffect(editing) {
        if (editing) {
            withFrameNanos { }
            delay(40)
            runCatching { fieldFocusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(outerShape)
            .background(if (focused || editing) Color(0xFF1478FF).copy(alpha = 0.58f) else Color.Transparent)
            .border(BorderStroke(1.dp, if (focused || editing) Color(0xFF2E8CFF) else Color.Transparent), outerShape)
            .padding(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusProperties {
                    up = previous
                    down = next
                }
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (!enabled || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                            editing = true
                            true
                        }
                        event.key == Key.Back && editing -> {
                            editing = false
                            keyboardController?.hide()
                            runCatching { focusRequester.requestFocus() }
                            true
                        }
                        event.key == Key.DirectionUp && !editing -> {
                            runCatching { previous.requestFocus() }
                            true
                        }
                        event.key == Key.DirectionDown && !editing -> {
                            runCatching { next.requestFocus() }
                            true
                        }
                        else -> false
                    }
                }
                .focusable(enabled = enabled && !editing)
                .clip(innerShape)
                .background(if (focused || editing) Color(0xFF102D5B) else Color(0xFF030C19).copy(alpha = 0.88f))
                .border(
                    BorderStroke(if (focused || editing) 2.dp else 1.dp, if (focused || editing) Color.White else Color(0xFF2C4A71)),
                    innerShape,
                )
                .padding(horizontal = 12.dp)
                .semantics { contentDescription = placeholder },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = { nextValue ->
                    onValueChange(nextValue.replace(Regex("[\\s-]+"), "").uppercase().take(10))
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
                    .onFocusChanged {
                        if (!it.isFocused && editing) editing = false
                    },
                decorationBox = { inner ->
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            color = SmartVisionColors.TextSecondary.copy(alpha = 0.72f),
                            style = SmartVisionType.Caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
}

@Composable
private fun ActivationPrimaryButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    focusRequester: FocusRequester,
    previous: FocusRequester,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val outerShape = RoundedCornerShape(10.dp)
    val innerShape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(outerShape)
            .background(if (focused) Color(0xFF1478FF).copy(alpha = 0.58f) else Color.Transparent)
            .border(BorderStroke(1.dp, if (focused) Color(0xFF2E8CFF) else Color.Transparent), outerShape)
            .padding(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusProperties { up = previous }
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> {
                            runCatching { previous.requestFocus() }
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (enabled) onClick()
                            true
                        }
                        else -> false
                    }
                }
                .clip(innerShape)
                .background(
                    if (enabled) {
                        Brush.horizontalGradient(listOf(Color(0xFF2C8CFF), Color(0xFF1766F2)))
                    } else {
                        Brush.horizontalGradient(listOf(Color(0xFF31547C), Color(0xFF243B5B)))
                    },
                )
                .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0xFF2B83FF)), innerShape)
                .clickable(enabled = enabled, onClick = onClick)
                .focusable(enabled = enabled)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Default.Key, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                color = Color.White,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SmartVisionLogo(
    modifier: Modifier = Modifier
        .width(200.dp)
        .height(62.dp),
) {
    Image(
        painter = painterResource(R.drawable.smartvision_logo_1),
        contentDescription = "SmartVision IPTV Player",
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@Composable
private fun QrCard(
    content: String,
    description: String,
    size: Int,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color.White), RoundedCornerShape(14.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (content.isBlank()) {
            Icon(
                Icons.Default.QrCode2,
                contentDescription = null,
                tint = Color(0xFF10203A),
                modifier = Modifier.size(92.dp),
            )
        } else {
            val bitmap = remember(content) { createQrBitmap(content, 520) }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = description,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

internal fun ActivationUiError.localizedMessage(strings: SmartVisionStrings): String =
    when (this) {
        ActivationUiError.InvalidLicenseCode -> strings.activationInvalidLicenseError
        ActivationUiError.LicenseActivationFailed -> strings.activationLicenseFailedError
        ActivationUiError.FreeWithAdsUnavailable -> strings.activationAdsUnavailableError
        ActivationUiError.NetworkUnavailable -> strings.activationNetworkError
        ActivationUiError.TrialUnavailable -> strings.activationTrialUnavailableError
        ActivationUiError.SessionUnavailable -> strings.activationSessionUnavailableError
        ActivationUiError.StatusUnavailable -> strings.activationStatusUnavailableError
        ActivationUiError.DeviceBlocked -> strings.activationDeviceBlockedError
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
        val row = y * size
        for (x in 0 until size) {
            pixels[row + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}
