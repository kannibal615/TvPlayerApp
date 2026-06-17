package com.smartvision.svplayer.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.core.designsystem.Badge
import com.smartvision.svplayer.core.designsystem.FocusableButton
import com.smartvision.svplayer.core.designsystem.FocusableCard
import com.smartvision.svplayer.core.designsystem.GlassPanel
import com.smartvision.svplayer.core.designsystem.SVColors

@Composable
fun HomeRoute(
    openLive: () -> Unit,
    openMovies: () -> Unit,
    openSeries: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        HeroBanner()
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Categories",
            color = SVColors.TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            HomeCategoryCard(
                title = "LIVE TV",
                subtitle = "Regardez vos chaines en direct",
                action = "Regarder maintenant",
                icon = Icons.Default.Tv,
                accent = SVColors.Cyan,
                background = listOf(Color(0xFF035E5F), Color(0xFF062239), Color(0xFF03101D)),
                onClick = openLive,
                modifier = Modifier.weight(1f),
            )
            HomeCategoryCard(
                title = "FILMS",
                subtitle = "Decouvrez notre bibliotheque de films",
                action = "Explorer maintenant",
                icon = Icons.Default.Movie,
                accent = SVColors.Purple,
                background = listOf(Color(0xFF4C1D95), Color(0xFF160B35), Color(0xFF050714)),
                onClick = openMovies,
                modifier = Modifier.weight(1f),
            )
            HomeCategoryCard(
                title = "SERIES",
                subtitle = "Explorez vos series preferees",
                action = "Decouvrir maintenant",
                icon = Icons.Default.VideoLibrary,
                accent = SVColors.Blue,
                background = listOf(Color(0xFF0B4A86), Color(0xFF061D39), Color(0xFF030A15)),
                onClick = openSeries,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeroBanner() {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(238.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1B75BC), Color(0xFF071426), Color(0xFF020817)),
                        radius = 1200f,
                        center = androidx.compose.ui.geometry.Offset(1100f, 0f),
                    ),
                ),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(460.dp)
                    .clip(RoundedCornerShape(230.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF3BA7FF).copy(alpha = 0.65f), Color.Transparent),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 46.dp),
            ) {
                Badge("ANNONCE", SVColors.Cyan.copy(alpha = 0.45f))
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Annonces SmartVision",
                    color = SVColors.TextPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Publicites, informations et nouveautes pour votre ecran TV.",
                    color = SVColors.TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == 0) 12.dp else 10.dp)
                            .background(
                                if (index == 0) Color.White else SVColors.TextSecondary.copy(alpha = 0.55f),
                                RoundedCornerShape(50),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeCategoryCard(
    title: String,
    subtitle: String,
    action: String,
    icon: ImageVector,
    accent: Color,
    background: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCard(
        onClick = onClick,
        accent = accent,
        modifier = modifier.height(360.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(background))
                .border(BorderStroke(1.dp, accent.copy(alpha = 0.45f)), RoundedCornerShape(12.dp)),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(150.dp)
                    .background(accent.copy(alpha = 0.18f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(92.dp))
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(58.dp))
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 28.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(title, color = Color.White, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(subtitle, color = SVColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(20.dp))
                FocusableButton(text = action, onClick = onClick, accent = accent, modifier = Modifier.fillMaxWidth(0.72f))
            }
        }
    }
}
