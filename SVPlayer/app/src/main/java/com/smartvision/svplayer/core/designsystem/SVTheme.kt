package com.smartvision.svplayer.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object SVColors {
    val Background = Color(0xFF020817)
    val BackgroundDeep = Color(0xFF01040C)
    val Surface = Color(0xFF071426)
    val SurfaceLight = Color(0xFF0B1F35)
    val SurfaceSoft = Color(0xCC10243D)
    val Cyan = Color(0xFF00E5FF)
    val Purple = Color(0xFF8B5CF6)
    val Blue = Color(0xFF2563EB)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB8C2D6)
    val Border = Color(0xFF1E314D)
    val Danger = Color(0xFFEF4444)
    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
}

private val SVTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
)

@Composable
fun SVPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = SVColors.Background,
            surface = SVColors.Surface,
            primary = SVColors.Cyan,
            secondary = SVColors.Purple,
            tertiary = SVColors.Blue,
            onBackground = SVColors.TextPrimary,
            onSurface = SVColors.TextPrimary,
            onPrimary = Color(0xFF001014),
            onSecondary = Color.White,
        ),
        typography = SVTypography,
        content = content,
    )
}
