package com.smartvision.svplayer.ui.theme

import androidx.compose.ui.unit.dp

object SmartVisionDimensions {
    val AppScreenHorizontalPadding = 25.dp
    val AppScreenVerticalPadding = 14.dp
    val AppHeaderContentSpacing = 12.dp

    val ScreenPadding = 64.dp
    val SectionSpacing = 24.dp
    val InternalSpacing = 16.dp
    val CompactSpacing = 8.dp

    val PanelRadius = 20.dp
    val CardRadius = 16.dp
    val ButtonRadius = 10.dp
    val BadgeRadius = 8.dp

    val PanelBorder = 1.dp
    val FocusBorder = 2.dp
    val FocusGlowRadius = 28.dp

    val SideNavWidth = 228.dp
    val HeaderHeight = 72.dp
    val ButtonHeight = 40.dp
    val NavButtonHeight = 56.dp

    const val FocusScale = 1.06f
    const val FocusAnimationMillis = 150

    val HomeScreenPadding = AppScreenHorizontalPadding
    val HomeHeaderTopPadding = AppScreenVerticalPadding
    val HomeHeaderHeight = 44.dp
    val HomeHeaderContentClearance = 24.dp
    val HomeHeaderToHeroSpacing = AppHeaderContentSpacing
    val HomeHeroHeight = 132.dp
    val HomeCategoryHeight = 170.dp
    val HomeContentCardWidth = 112.dp
    // Fire TV reports 1920 px at density 2 (960 dp logical width). With the
    // Home side paddings, row edge paddings and 6 dp gaps, 94.5 dp keeps five
    // complete 16:9 cards visible without clipping the focused edge.
    val HomeContentCardHeight = 94.5.dp
    val HomeContentPreviewCardWidth = HomeContentCardHeight * (16f / 9f)
    // 24 dp section-title line + 6 dp title/card gap + the 94.5 dp card.
    // Keeping the row height equal to its visible content makes the following
    // section start exactly HomeContentSectionSpacing below the card.
    val HomeContentRowHeight = HomeContentCardHeight + 30.dp
    // LazyRow clips drawing at its viewport edges. Keep enough inset for the
    // focused card scale + shadow while preserving five complete 16:9 cards.
    val HomeRowEdgePadding = 20.dp
    val HomeContentCardSpacing = 6.dp
    val HomeContentSectionSpacing = HomeHeaderToHeroSpacing
    val HomePanelRadius = 8.dp
    val HomeCardRadius = 8.dp
    val HomeContentRadius = 6.dp
}
