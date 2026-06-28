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
    val effect: TvFocusEffect = TvFocusEffect.Frame,
)

enum class TvFocusEffect {
    Frame,
    NeonGlow,
    GoldSweep,
}

object TvFocusStyles {
    val Default = TvFocusStyle(
        key = "Default",
        label = "Default",
        scale = 1.06f,
        borderWidth = 2.dp,
        glowAlpha = 0.18f,
        accent = SmartVisionColors.FocusWhite,
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

    fun fromKeys(styleKey: String?, colorKey: String?, effectKey: String?): TvFocusStyle {
        val base = fromKey(styleKey)
        val effect = when {
            effectKey.equals("NeonGlow", ignoreCase = true) -> TvFocusEffect.NeonGlow
            effectKey.equals("GoldSweep", ignoreCase = true) -> TvFocusEffect.GoldSweep
            else -> TvFocusEffect.Frame
        }
        val color = when {
            effect == TvFocusEffect.GoldSweep -> GoldAccent
            colorKey.equals("CyanNeon", ignoreCase = true) -> Color(0xFF19F3FF)
            colorKey.equals("ElectricBlue", ignoreCase = true) -> Color(0xFF2F6BFF)
            else -> SmartVisionColors.FocusWhite
        }
        return base.copy(
            accent = color,
            effect = effect,
            glowAlpha = when (effect) {
                TvFocusEffect.Frame -> 0.10f
                TvFocusEffect.NeonGlow -> 0.46f
                TvFocusEffect.GoldSweep -> 0.20f
            },
        )
    }

    val FocusColors = listOf("White", "CyanNeon", "ElectricBlue")
    val FocusEffects = listOf("Frame", "NeonGlow", "GoldSweep")

    private val GoldAccent = Color(0xFFFFC84A)
}

val LocalTvFocusStyle = staticCompositionLocalOf { TvFocusStyles.Default }
