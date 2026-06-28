package com.smartvision.svplayer.ui.focus

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.ui.theme.SmartVisionColors

data class TvFocusStyle(
    val key: String,
    val label: String,
    val scale: Float,
    val borderWidth: Dp,
    val glowAlpha: Float,
    val accent: Color,
)

object TvFocusStyles {
    val Default = TvFocusStyle(
        key = "Default",
        label = "Default",
        scale = 1.06f,
        borderWidth = 2.dp,
        glowAlpha = 0.34f,
        accent = SmartVisionColors.CyanAccent,
    )
    val Soft = TvFocusStyle(
        key = "Soft",
        label = "Soft",
        scale = 1.035f,
        borderWidth = 2.dp,
        glowAlpha = 0.22f,
        accent = Color(0xFF7DE7FF),
    )
    val Compact = TvFocusStyle(
        key = "Compact",
        label = "Compact",
        scale = 1.015f,
        borderWidth = 1.dp,
        glowAlpha = 0.12f,
        accent = SmartVisionColors.FocusWhite,
    )

    val All = listOf(Default, Soft, Compact)

    fun fromKey(key: String?): TvFocusStyle =
        All.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: Default
}

val LocalTvFocusStyle = staticCompositionLocalOf { TvFocusStyles.Default }
