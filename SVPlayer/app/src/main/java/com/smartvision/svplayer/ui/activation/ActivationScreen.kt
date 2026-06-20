package com.smartvision.svplayer.ui.activation

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.smartvision.svplayer.R
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType

@Composable
fun ActivationScreen(
    state: ActivationUiState,
    onRetry: () -> Unit,
    onRefreshSession: () -> Unit,
    onCheckNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SmartVisionColors.PrimaryDark.copy(alpha = 0.46f),
                        SmartVisionColors.Background,
                        Color(0xFF01040C),
                    ),
                    radius = 1450f,
                ),
            )
            .padding(horizontal = 72.dp, vertical = 48.dp),
    ) {
        HeaderLogo(Modifier.align(Alignment.TopStart))

        ActivationPanel(
            state = state,
            onRetry = onRetry,
            onRefreshSession = onRefreshSession,
            onCheckNow = onCheckNow,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun HeaderLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(50.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.smartvision_mark),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(42.dp),
        )
        Spacer(Modifier.width(12.dp))
        Image(
            painter = painterResource(R.drawable.smartvision_wordmark),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(178.dp)
                .height(38.dp),
        )
    }
}

@Composable
private fun ActivationPanel(
    state: ActivationUiState,
    onRetry: () -> Unit,
    onRefreshSession: () -> Unit,
    onCheckNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .width(1120.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        SmartVisionColors.SurfaceElevated.copy(alpha = 0.92f),
                        SmartVisionColors.Surface.copy(alpha = 0.96f),
                    ),
                ),
            )
            .border(BorderStroke(1.dp, SmartVisionColors.Border), shape)
            .padding(34.dp),
        horizontalArrangement = Arrangement.spacedBy(34.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivationInfo(
            state = state,
            onRetry = onRetry,
            onRefreshSession = onRefreshSession,
            onCheckNow = onCheckNow,
            modifier = Modifier.weight(1f),
        )
        ActivationQrBlock(
            state = state,
            modifier = Modifier.width(360.dp),
        )
    }
}

@Composable
private fun ActivationInfo(
    state: ActivationUiState,
    onRetry: () -> Unit,
    onRefreshSession: () -> Unit,
    onCheckNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val verifyFocusRequester = remember { FocusRequester() }
    val newCodeFocusRequester = remember { FocusRequester() }
    val verifyEnabled = !state.checking && !state.creatingSession && !state.blocked
    var initialFocusRequested by remember { mutableStateOf(false) }

    LaunchedEffect(verifyEnabled, state.hasSession) {
        if (!initialFocusRequested && verifyEnabled && state.hasSession) {
            verifyFocusRequester.requestFocus()
            initialFocusRequested = true
        }
    }

    Column(modifier = modifier) {
        StatusBadge(state)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Activer SmartVision",
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.TitleL,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Scannez le QR code ou ouvrez le lien d activation, puis validez cet appareil depuis le portail SmartVision.",
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Body,
        )
        Spacer(Modifier.height(18.dp))

        ActivationCodeBlock(state)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TvButton(
                text = "Verifier",
                onClick = onCheckNow,
                enabled = verifyEnabled,
                leadingIcon = Icons.Default.CheckCircle,
                focusRequester = verifyFocusRequester,
                contentPadding = PaddingValues(horizontal = 22.dp),
                modifier = Modifier
                    .focusProperties { right = newCodeFocusRequester }
                    .height(48.dp),
            )
            TvButton(
                text = "Nouveau code",
                onClick = onRefreshSession,
                enabled = !state.creatingSession && !state.blocked,
                leadingIcon = Icons.Default.Refresh,
                focusRequester = newCodeFocusRequester,
                variant = TvButtonVariant.Secondary,
                contentPadding = PaddingValues(horizontal = 22.dp),
                modifier = Modifier
                    .focusProperties { left = verifyFocusRequester }
                    .height(48.dp),
            )
            if (!state.hasSession || state.errorMessage != null) {
                TvButton(
                    text = "Reessayer",
                    onClick = onRetry,
                    enabled = !state.checking && !state.creatingSession,
                    leadingIcon = Icons.Default.Sync,
                    variant = TvButtonVariant.Tertiary,
                    contentPadding = PaddingValues(horizontal = 22.dp),
                    modifier = Modifier.height(48.dp),
                )
            }
        }
        if (state.errorMessage != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = state.errorMessage,
                color = SmartVisionColors.Error,
                style = SmartVisionType.Label,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun StatusBadge(state: ActivationUiState) {
    val (label, color) = when {
        state.activated -> "ACTIVE" to SmartVisionColors.Success
        state.blocked -> "BLOQUE" to SmartVisionColors.Error
        state.creatingSession || state.checking -> "VERIFICATION" to SmartVisionColors.Warning
        state.polling -> "EN ATTENTE" to SmartVisionColors.Primary
        else -> "INACTIF" to SmartVisionColors.TextSecondary
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(SmartVisionDimensions.BadgeRadius))
            .background(color.copy(alpha = 0.18f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.62f)), RoundedCornerShape(SmartVisionDimensions.BadgeRadius))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.checking || state.creatingSession) {
            CircularProgressIndicator(
                color = color,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            color = color,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ActivationCodeBlock(state: ActivationUiState) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .width(450.dp)
            .clip(shape)
            .background(Color(0xFF081528).copy(alpha = 0.86f))
            .border(BorderStroke(1.dp, SmartVisionColors.Primary.copy(alpha = 0.46f)), shape)
            .padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
        Text(
            text = "Code d activation",
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Label,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = state.shortCode.ifBlank { "------" }.chunked(3).joinToString(" "),
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.TitleL,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = state.statusLabel,
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Label,
        )
        if (state.expiresAt.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Expire le ${state.expiresAt}",
                color = SmartVisionColors.TextSecondary.copy(alpha = 0.82f),
                style = SmartVisionType.Caption,
            )
        }
    }
}

@Composable
private fun ActivationQrBlock(
    state: ActivationUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.creatingSession || state.checking -> CircularProgressIndicator(color = SmartVisionColors.Primary)
                state.qrUrl.isBlank() -> Text(
                    text = "QR indisponible",
                    color = Color(0xFF1D2431),
                    style = SmartVisionType.Label,
                    textAlign = TextAlign.Center,
                )
                else -> QrCodeImage(content = state.qrUrl, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Le statut est verifie toutes les ${state.pollingIntervalSeconds} secondes.",
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Label,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(content) { createQrBitmap(content, 512) }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR code activation",
        modifier = modifier,
    )
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
