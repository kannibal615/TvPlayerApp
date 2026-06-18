package com.smartvision.svplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme as ComposeMaterialTheme
import androidx.compose.material3.Shapes as ComposeShapes
import androidx.compose.material3.darkColorScheme as composeDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Shapes as TvShapes
import androidx.tv.material3.darkColorScheme as tvDarkColorScheme

private val SmartVisionComposeColorScheme = composeDarkColorScheme(
    background = SmartVisionColors.Background,
    surface = SmartVisionColors.Surface,
    surfaceVariant = SmartVisionColors.SurfaceElevated,
    primary = SmartVisionColors.Primary,
    secondary = SmartVisionColors.CyanAccent,
    tertiary = SmartVisionColors.PrimaryDark,
    outline = SmartVisionColors.Border,
    error = SmartVisionColors.Error,
    onBackground = SmartVisionColors.TextPrimary,
    onSurface = SmartVisionColors.TextPrimary,
    onPrimary = Color.White,
    onSecondary = Color.Black,
)

private val SmartVisionTvColorScheme = tvDarkColorScheme(
    background = SmartVisionColors.Background,
    surface = SmartVisionColors.Surface,
    surfaceVariant = SmartVisionColors.SurfaceElevated,
    primary = SmartVisionColors.Primary,
    secondary = SmartVisionColors.CyanAccent,
    tertiary = SmartVisionColors.PrimaryDark,
    border = SmartVisionColors.Border,
    error = SmartVisionColors.Error,
    onBackground = SmartVisionColors.TextPrimary,
    onSurface = SmartVisionColors.TextPrimary,
    onPrimary = Color.White,
    onSecondary = Color.Black,
)

private val SmartVisionComposeShapes = ComposeShapes(
    small = RoundedCornerShape(SmartVisionDimensions.BadgeRadius),
    medium = RoundedCornerShape(SmartVisionDimensions.CardRadius),
    large = RoundedCornerShape(SmartVisionDimensions.PanelRadius),
)

private val SmartVisionTvShapes = TvShapes(
    small = RoundedCornerShape(SmartVisionDimensions.BadgeRadius),
    medium = RoundedCornerShape(SmartVisionDimensions.CardRadius),
    large = RoundedCornerShape(SmartVisionDimensions.PanelRadius),
)

@Composable
fun SmartVisionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    TvMaterialTheme(
        colorScheme = SmartVisionTvColorScheme,
        typography = SmartVisionTvTypography,
        shapes = SmartVisionTvShapes,
    ) {
        ComposeMaterialTheme(
            colorScheme = SmartVisionComposeColorScheme,
            typography = SmartVisionComposeTypography,
            shapes = SmartVisionComposeShapes,
            content = content,
        )
    }
}
