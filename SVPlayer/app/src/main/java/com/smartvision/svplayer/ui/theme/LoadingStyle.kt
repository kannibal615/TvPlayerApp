package com.smartvision.svplayer.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object SmartVisionLoadingColors {
    fun fromKey(key: String?): Color = when {
        key.equals("White", ignoreCase = true) -> SmartVisionColors.FocusWhite
        key.equals("ElectricBlue", ignoreCase = true) -> Color(0xFF2F6BFF)
        key.equals("Gold", ignoreCase = true) -> Color(0xFFFFC84A)
        else -> Color(0xFF19F3FF)
    }
}

val LocalLoadingColor = staticCompositionLocalOf { SmartVisionLoadingColors.fromKey("CyanNeon") }
