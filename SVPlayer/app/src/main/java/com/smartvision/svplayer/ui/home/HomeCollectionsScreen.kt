package com.smartvision.svplayer.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.i18n.smartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType

enum class HomeCollectionKind {
    ContinueWatching,
    Trending,
}

private data class CollectionSection(
    val title: String,
    val items: List<ContinueItem>,
)

@Composable
fun HomeCollectionsScreen(
    kind: HomeCollectionKind,
    onBack: () -> Unit,
    onItemClick: (ContinueItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(
        key = "home-collection-${kind.name}",
        factory = viewModelFactory {
            HomeViewModel(
                userContentRepository = container.userContentRepository,
                catalogRepository = container.catalogRepository,
                homeSlidesRepository = container.homeSlidesRepository,
                homeContentRepository = container.homeContentRepository,
            )
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = PlayerSettings())
    val strings = smartVisionStrings(settings.language)
    val sections = when (kind) {
        HomeCollectionKind.ContinueWatching -> listOf(
            CollectionSection(strings.liveTv, state.continueWatching.filter { it.id.startsWith("live:") }),
            CollectionSection(strings.movies, state.continueWatching.filter { it.id.startsWith("movie:") }),
            CollectionSection(strings.series, state.continueWatching.filter { it.id.startsWith("episode:") }),
        )
        HomeCollectionKind.Trending -> listOf(
            CollectionSection(strings.trendingMovies, state.trendingMovies),
            CollectionSection(strings.trendingSeries, state.trendingSeries),
        )
    }.filter { it.items.isNotEmpty() }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        SmartVisionColors.PrimaryDark.copy(alpha = 0.38f),
                        SmartVisionColors.Background,
                        Color(0xFF01040C),
                    ),
                    radius = 1500f,
                ),
            )
            .padding(horizontal = 34.dp, vertical = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvButton(
                text = strings.back,
                leadingIcon = Icons.Default.ArrowBack,
                onClick = onBack,
                variant = TvButtonVariant.Secondary,
                modifier = Modifier.height(42.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (kind == HomeCollectionKind.ContinueWatching) strings.continueWatching else strings.trending,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleL,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            items(sections, key = { it.title }) { section ->
                ContinueWatchingRow(
                    title = section.title,
                    items = section.items,
                    onItemClick = onItemClick,
                    showViewAll = false,
                    blockedMessage = strings.connectionUnavailable,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
