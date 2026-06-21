package com.smartvision.svplayer.ui.activation

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.smartvision.svplayer.data.activation.ActivationException
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.data.activation.ActivationSession
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun XtreamQrSetupPanel(
    activationRepository: ActivationRepository,
    title: String,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<ActivationSession?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        loading = true
        error = null
        scope.launch {
            runCatching { activationRepository.createPlaylistSetupSession() }
                .onSuccess {
                    session = it
                    loading = false
                }
                .onFailure {
                    error = when (it) {
                        is ActivationException -> it.message ?: "Lien indisponible."
                        else -> "Lien de configuration indisponible."
                    }
                    loading = false
                }
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    LaunchedEffect(session?.shortCode) {
        while (session != null && isActive) {
            delay(5_000L)
            runCatching { activationRepository.checkStatus() }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .width(850.dp)
                .background(Color(0xE60A1424), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
                .padding(34.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(34.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                val qr = session?.qrUrl?.let { remember(it) { createQrBitmap(it, 420) } }
                if (qr != null) {
                    Image(
                        bitmap = qr.asImageBitmap(),
                        contentDescription = "QR code configuration Xtream",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = Color(0xFF10203A),
                        modifier = Modifier.size(92.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = SmartVisionColors.TextPrimary,
                    style = SmartVisionType.TitleL,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Votre appareil est activé. Scannez ce QR code avec votre téléphone pour renseigner uniquement vos identifiants Xtream.",
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Body,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(18.dp))
                session?.let {
                    Text(
                        text = it.shortCode.chunked(3).joinToString(" "),
                        color = SmartVisionColors.CyanAccent,
                        style = SmartVisionType.TitleS,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Le lien expire à ${it.expiresAt}.",
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Caption,
                    )
                }
                error?.let {
                    Text(
                        text = it,
                        color = SmartVisionColors.Error,
                        style = SmartVisionType.Body,
                        textAlign = TextAlign.Start,
                    )
                }
                Spacer(Modifier.height(22.dp))
                TvButton(
                    text = if (loading) "Generation..." else "Generer un nouveau QR code",
                    onClick = { if (!loading) refresh() },
                    variant = TvButtonVariant.Primary,
                    modifier = Modifier.height(46.dp),
                )
            }
        }
    }
}

private fun createQrBitmap(content: String, size: Int): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
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
