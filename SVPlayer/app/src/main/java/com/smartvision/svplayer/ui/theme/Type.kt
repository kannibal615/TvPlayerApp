package com.smartvision.svplayer.ui.theme

import androidx.compose.material3.Typography as ComposeTypography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography as TvTypography

object SmartVisionType {
    val TitleXL = TextStyle(
        fontSize = 48.sp,
        lineHeight = 56.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    )

    val TitleL = TextStyle(
        fontSize = 34.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    )

    val TitleM = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    )

    val TitleS = TextStyle(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    )

    val HomeHeroTitle = TextStyle(
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    )

    val HomeCategoryTitle = TextStyle(
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    )

    val HomeSectionTitle = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    )

    val Body = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    )

    val Label = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    )

    val Caption = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    )
}

val SmartVisionComposeTypography = ComposeTypography(
    displayLarge = SmartVisionType.TitleXL,
    headlineLarge = SmartVisionType.TitleL,
    headlineMedium = SmartVisionType.TitleM,
    titleLarge = SmartVisionType.TitleM,
    titleMedium = SmartVisionType.TitleS,
    bodyLarge = SmartVisionType.Body,
    bodyMedium = SmartVisionType.Label,
    labelLarge = SmartVisionType.Label.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = SmartVisionType.Caption,
)

val SmartVisionTvTypography = TvTypography(
    displayLarge = SmartVisionType.TitleXL,
    displayMedium = SmartVisionType.TitleL,
    displaySmall = SmartVisionType.TitleM,
    headlineLarge = SmartVisionType.TitleL,
    headlineMedium = SmartVisionType.TitleM,
    headlineSmall = SmartVisionType.TitleS,
    titleLarge = SmartVisionType.TitleM,
    titleMedium = SmartVisionType.TitleS,
    titleSmall = SmartVisionType.Label.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = SmartVisionType.Body,
    bodyMedium = SmartVisionType.Label,
    bodySmall = SmartVisionType.Caption,
    labelLarge = SmartVisionType.Label.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = SmartVisionType.Caption,
    labelSmall = SmartVisionType.Caption,
)
