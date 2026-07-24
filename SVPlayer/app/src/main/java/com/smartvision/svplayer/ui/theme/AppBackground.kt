package com.smartvision.svplayer.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.smartvision.svplayer.R
import com.smartvision.svplayer.domain.model.PlayerSettings

val LocalAppBackgroundActive = staticCompositionLocalOf { false }

@Composable
fun AppBackgroundSurface(
    settings: PlayerSettings,
    modifier: Modifier = Modifier,
) {
    val model = appBackgroundModel(settings.appBackgroundType, settings.appBackgroundValue)
    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                listOf(Color(0xFF0A2444), SmartVisionColors.Background, Color(0xFF01040C)),
                radius = 1550f,
            ),
        ),
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

fun appBackgroundModel(type: String, value: String): Any? = when (type) {
    "Preset" -> when (value) {
        "Neon" -> R.drawable.startup_neon_background
        "Cinema" -> R.drawable.startup_cinema_background
        "Aurora" -> R.drawable.home_hero_slide_3
        else -> null
    }
    "Local", "Url" -> value.trim().takeIf(String::isNotBlank)
    else -> null
}

@Composable
fun Modifier.appScreenBackground(defaultBrush: Brush): Modifier =
    if (LocalAppBackgroundActive.current) background(Color.Transparent) else background(defaultBrush)
