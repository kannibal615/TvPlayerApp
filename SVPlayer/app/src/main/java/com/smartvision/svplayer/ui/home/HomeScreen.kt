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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.mock.HomeNavigationData
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions

@Composable
fun HomeScreen(
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onLicenseKey: () -> Unit,
    showLicenseKey: Boolean,
    onContentClick: (ContinueItem) -> Unit,
    onContinueViewAll: () -> Unit,
    onTrendingViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            HomeViewModel(
                userContentRepository = container.userContentRepository,
                xtreamRepository = container.xtreamRepository,
                homeSlidesRepository = container.homeSlidesRepository,
            )
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val liveFocusRequester = remember { FocusRequester() }
    val hasContinueWatching = state.continueWatching.isNotEmpty()

    LaunchedEffect(Unit) {
        viewModel.refreshSlides()
        viewModel.refreshTrending()
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
            onProfile = onProfile,
            onLicenseKey = onLicenseKey,
            showLicenseKey = showLicenseKey,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(SmartVisionDimensions.HomeHeaderToHeroSpacing))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            HomeHeroBanner(
                remoteSlides = state.slides,
                onNavigate = onNavigate,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SmartVisionDimensions.HomeCategoryHeight),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HomeNavigationData.categories.forEach { category ->
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
                title = if (hasContinueWatching) "Reprendre la lecture" else "Tendances",
                items = if (hasContinueWatching) state.continueWatching else state.trending,
                showViewAll = true,
                onViewAll = if (hasContinueWatching) onContinueViewAll else onTrendingViewAll,
                onItemClick = onContentClick,
                modifier = Modifier.fillMaxWidth(),
            )

            if (hasContinueWatching) {
                Spacer(Modifier.height(SmartVisionDimensions.HomeTrendFoldOffset))

                ContinueWatchingRow(
                    title = "Tendances",
                    items = state.trending,
                    onItemClick = onContentClick,
                    showViewAll = true,
                    onViewAll = onTrendingViewAll,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

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
