package com.smartvision.svplayer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.mock.MockHomeData
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions

@Composable
fun HomeScreen(
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    onContentClick: (ContinueItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val liveFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        liveFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SmartVisionColors.PrimaryDark.copy(alpha = 0.38f),
                        SmartVisionColors.Background,
                        Color(0xFF01040C),
                    ),
                    radius = 1500f,
                ),
            )
            .padding(horizontal = SmartVisionDimensions.HomeScreenPadding)
            .padding(top = SmartVisionDimensions.HomeHeaderTopPadding),
    ) {
        TvHeader(
            currentRoute = currentRoute,
            tabs = tabs,
            onNavigate = onNavigate,
            onSync = onSync,
            onSettings = onSettings,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(SmartVisionDimensions.HomeHeaderToHeroSpacing))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            HomeHeroBanner(modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SmartVisionDimensions.HomeCategoryHeight),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MockHomeData.categories.forEach { category ->
                    HomeCategoryCard(
                        category = category,
                        onClick = { onNavigate(category.routeName) },
                        focusRequester = if (category.id == "live") liveFocusRequester else null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            ContinueWatchingRow(
                title = "Reprendre la lecture",
                items = MockHomeData.continueWatching,
                showViewAll = true,
                onViewAll = {},
                onItemClick = onContentClick,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(SmartVisionDimensions.HomeTrendFoldOffset))

            ContinueWatchingRow(
                title = "Tendances",
                items = MockHomeData.trending,
                onItemClick = onContentClick,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

private val com.smartvision.svplayer.data.mock.HomeCategory.routeName: String
    get() = when (id) {
        "live" -> "live_tv"
        "movies" -> "movies"
        else -> "series"
    }
