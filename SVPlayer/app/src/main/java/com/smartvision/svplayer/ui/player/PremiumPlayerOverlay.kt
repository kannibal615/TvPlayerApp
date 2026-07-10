package com.smartvision.svplayer.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.ui.theme.SmartVisionColors

private val PremiumOverlayGlass = Color(0xD90A1220)
private val PremiumOverlayEdge = Color.White.copy(alpha = 0.16f)

@Composable
internal fun PremiumPlayerOverlayFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.18f),
                0.38f to Color.Transparent,
                0.68f to Color.Black.copy(alpha = 0.28f),
                1f to Color.Black.copy(alpha = 0.92f),
            ),
        ),
        content = content,
    )
}

internal fun Modifier.premiumPlayerGlassSurface(): Modifier {
    val shape = RoundedCornerShape(18.dp)
    return clip(shape)
        .background(
            Brush.horizontalGradient(
                listOf(
                    PremiumOverlayGlass,
                    Color(0xE30B1628),
                    Color(0xD808101D),
                ),
            ),
        )
        .border(BorderStroke(1.dp, PremiumOverlayEdge), shape)
}

@Composable
internal fun PremiumPlayerContextPill(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label.uppercase(),
        color = SmartVisionColors.CyanAccent,
        style = PlayerMetaStyle,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(SmartVisionColors.Primary.copy(alpha = 0.20f))
            .border(
                BorderStroke(1.dp, SmartVisionColors.CyanAccent.copy(alpha = 0.42f)),
                RoundedCornerShape(50),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
