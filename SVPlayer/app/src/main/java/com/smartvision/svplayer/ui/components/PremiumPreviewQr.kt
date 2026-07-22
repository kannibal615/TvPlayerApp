package com.smartvision.svplayer.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType

/** Canonical Premium QR panel shared by Live TV, Movies and Series previews. */
@Composable
fun PremiumPreviewQr(
    purchaseUrl: String,
    tvCode: String,
    title: String,
    subtitle: String,
    codeLabel: String,
    modifier: Modifier = Modifier,
) {
    val qrContent = purchaseUrl.ifBlank { "https://smartvisions.net" }
    val bitmap = remember(qrContent) { createPremiumPreviewQrBitmap(qrContent, 384) }
    val gold = Color(0xFFFFD47A)
    val shape = RoundedCornerShape(5.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false }
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0A1828), Color(0xFF020914), Color.Black),
                    center = Offset(220f, 0f),
                    radius = 560f,
                ),
            )
            .border(BorderStroke(0.5.dp, gold.copy(alpha = 0.58f)), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        PremiumDivider(gold = gold, showCrown = true)
        androidx.compose.material3.Text(
            text = title,
            color = gold,
            style = SmartVisionType.TitleS.copy(fontSize = 19.sp, lineHeight = 23.sp),
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        androidx.compose.material3.Text(
            text = subtitle,
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.Body.copy(fontSize = 13.sp, lineHeight = 17.sp),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(shape)
                .background(Color.White)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)), RoundedCornerShape(10.dp))
                .padding(5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR code SmartVision Premium",
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(7.dp))
        PremiumDivider(
            gold = gold,
            centerContent = {
                androidx.compose.material3.Text(
                    text = codeLabel,
                    color = gold.copy(alpha = 0.94f),
                    style = SmartVisionType.Body.copy(fontSize = 13.sp, lineHeight = 17.sp),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 10.dp, end = 6.dp),
                )
                androidx.compose.material3.Text(
                    text = tvCode.ifBlank { "------" },
                    color = gold,
                    style = SmartVisionType.TitleS.copy(fontSize = 20.sp, lineHeight = 23.sp),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 10.dp),
                )
            },
        )
    }
}

@Composable
private fun PremiumDivider(
    gold: Color,
    showCrown: Boolean = false,
    centerContent: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .weight(if (showCrown) 1f else 0.7f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, gold.copy(alpha = 0.52f)))),
        )
        if (showCrown) {
            PremiumCrown(
                color = gold,
                modifier = Modifier.padding(horizontal = 12.dp).size(width = 32.dp, height = 22.dp),
            )
        } else {
            centerContent()
        }
        Box(
            modifier = Modifier
                .weight(if (showCrown) 1f else 0.7f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(gold.copy(alpha = 0.52f), Color.Transparent))),
        )
    }
}

@Composable
private fun PremiumCrown(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.focusProperties { canFocus = false }) {
        val crown = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.72f)
            lineTo(size.width * 0.27f, size.height * 0.36f)
            lineTo(size.width * 0.42f, size.height * 0.72f)
            lineTo(size.width * 0.50f, size.height * 0.18f)
            lineTo(size.width * 0.58f, size.height * 0.72f)
            lineTo(size.width * 0.73f, size.height * 0.36f)
            lineTo(size.width * 0.88f, size.height * 0.72f)
            lineTo(size.width * 0.80f, size.height * 0.88f)
            lineTo(size.width * 0.20f, size.height * 0.88f)
            close()
        }
        drawPath(crown, color.copy(alpha = 0.92f))
        drawPath(crown, Color.White.copy(alpha = 0.36f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.7.dp.toPx()))
        listOf(0.27f, 0.50f, 0.73f).forEach { x ->
            drawCircle(
                color = color,
                radius = size.minDimension * 0.07f,
                center = Offset(size.width * x, size.height * if (x == 0.50f) 0.15f else 0.32f),
            )
        }
    }
}

private fun createPremiumPreviewQrBitmap(content: String, size: Int): Bitmap {
    val matrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        size,
        size,
        mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
        ),
    )
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
