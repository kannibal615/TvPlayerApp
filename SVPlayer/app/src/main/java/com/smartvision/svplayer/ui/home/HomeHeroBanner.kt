package com.smartvision.svplayer.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.R
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType

@Composable
fun HomeHeroBanner(
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(SmartVisionDimensions.HomePanelRadius)
    Box(
        modifier = modifier
            .height(SmartVisionDimensions.HomeHeroHeight)
            .clip(shape)
            .background(SmartVisionColors.Surface)
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.82f)), shape),
    ) {
        Image(
            painter = painterResource(R.drawable.home_hero_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF051020).copy(alpha = 0.96f),
                            Color(0xFF051020).copy(alpha = 0.58f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.18f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp, end = 430.dp),
        ) {
            Text(
                text = "Bienvenue sur SmartVision",
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.HomeHeroTitle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = "Profitez du meilleur du divertissement avec une qualité de streaming exceptionnelle.",
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Label,
                maxLines = 2,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HeroBadge("HD", "Haute qualité")
                HeroBadge("○", "Streaming stable")
                HeroBadge("✓", "Sécurisé & privé")
            }
        }
    }
}

@Composable
private fun HeroBadge(
    prefix: String,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(BorderStroke(1.dp, SmartVisionColors.Primary), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = prefix,
                color = SmartVisionColors.CyanAccent,
                style = SmartVisionType.Caption,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Caption,
            maxLines = 1,
        )
    }
}
