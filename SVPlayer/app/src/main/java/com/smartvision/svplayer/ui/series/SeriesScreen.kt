package com.smartvision.svplayer.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.ui.catalog.CatalogActionButton
import com.smartvision.svplayer.ui.catalog.CatalogBadge
import com.smartvision.svplayer.ui.catalog.CatalogCategoryRow
import com.smartvision.svplayer.ui.catalog.CatalogContentRow
import com.smartvision.svplayer.ui.catalog.CatalogEmpty
import com.smartvision.svplayer.ui.catalog.CatalogError
import com.smartvision.svplayer.ui.catalog.CatalogLoading
import com.smartvision.svplayer.ui.catalog.CatalogMetaStyle
import com.smartvision.svplayer.ui.catalog.CatalogPosterFrame
import com.smartvision.svplayer.ui.catalog.CatalogPreviewTitleStyle
import com.smartvision.svplayer.ui.catalog.MediaCatalogDimens
import com.smartvision.svplayer.ui.catalog.MediaCatalogHeader
import com.smartvision.svplayer.ui.catalog.MediaCatalogPanel
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import kotlinx.coroutines.delay

@Composable
fun SeriesScreen(
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    onWatchEpisode: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: SeriesViewModel = viewModel(
        factory = viewModelFactory {
            SeriesViewModel(container.xtreamRepository)
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    var inputReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(260)
        inputReady = true
    }

    LaunchedEffect(state.selectedCategoryId, state.categoriesLoading) {
        if (!state.categoriesLoading) {
            withFrameNanos { }
            delay(120)
            selectedCategoryFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SmartVisionColors.PrimaryDark.copy(alpha = 0.34f),
                        SmartVisionColors.Background,
                        Color(0xFF01040C),
                    ),
                    center = Offset(980f, 120f),
                    radius = 1500f,
                ),
            )
            .padding(horizontal = MediaCatalogDimens.ScreenPadding)
            .padding(top = MediaCatalogDimens.TopPadding, bottom = MediaCatalogDimens.BottomPadding),
    ) {
        MediaCatalogHeader(
            currentRoute = currentRoute,
            tabs = tabs,
            onNavigate = onNavigate,
            onSync = onSync,
            onSettings = onSettings,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(MediaCatalogDimens.HeaderGap))

        if (state.categoriesLoading) {
            CatalogLoading(
                title = "Chargement des categories",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(MediaCatalogDimens.PanelGap),
            ) {
                SeriesCategoryList(
                    state = state,
                    selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                    onCategory = { category ->
                        if (inputReady) {
                            viewModel.selectCategory(category)
                        }
                    },
                    modifier = Modifier
                        .weight(0.24f)
                        .fillMaxHeight(),
                )
                SeriesList(
                    state = state,
                    onSeriesFocused = viewModel::focusSeries,
                    onSeriesClick = { series ->
                        if (inputReady) {
                            viewModel.activateSeries(series)?.let(onWatchEpisode)
                        }
                    },
                    onRetry = viewModel::retryCurrentCategory,
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                )
                SeriesPreviewPanel(
                    state = state,
                    onWatch = {
                        if (inputReady) {
                            state.firstEpisode?.episodeId?.let(onWatchEpisode)
                        }
                    },
                    onFavorite = { state.selectedSeries?.let(viewModel::toggleFavorite) },
                    modifier = Modifier
                        .weight(0.34f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun SeriesCategoryList(
    state: SeriesScreenState,
    selectedCategoryFocusRequester: FocusRequester,
    onCategory: (SeriesCategoryUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    MediaCatalogPanel(
        title = "Categories",
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(MediaCatalogDimens.ListGap),
            contentPadding = PaddingValues(bottom = MediaCatalogDimens.ListGap),
        ) {
            items(state.categories, key = { it.id }) { category ->
                CatalogCategoryRow(
                    label = category.label,
                    count = category.count,
                    icon = seriesCategoryIcon(category.label),
                    selected = category.id == state.selectedCategoryId,
                    focusRequester = if (category.id == state.selectedCategoryId) {
                        selectedCategoryFocusRequester
                    } else {
                        null
                    },
                    onClick = { onCategory(category) },
                )
            }
        }
    }
}

@Composable
private fun SeriesList(
    state: SeriesScreenState,
    onSeriesFocused: (SeriesItemUi) -> Unit,
    onSeriesClick: (SeriesItemUi) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MediaCatalogPanel(
        title = "Series",
        modifier = modifier,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val seriesCount = state.selectedCategory?.count ?: state.series.size
                Text(
                    text = if (seriesCount > 0) "$seriesCount series" else "series",
                    color = SmartVisionColors.TextSecondary,
                    style = CatalogMetaStyle,
                    maxLines = 1,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = SmartVisionColors.TextSecondary,
                    modifier = Modifier.width(16.dp),
                )
            }
        },
    ) {
        when {
            state.seriesLoading -> CatalogLoading(
                title = "Chargement des series",
                modifier = Modifier.fillMaxSize(),
            )

            state.errorMessage != null && state.series.isEmpty() -> CatalogError(
                message = state.errorMessage,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )

            state.series.isEmpty() -> CatalogEmpty(
                title = "Aucune serie",
                subtitle = "Selectionnez une autre categorie.",
                modifier = Modifier.fillMaxSize(),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MediaCatalogDimens.ListGap),
                contentPadding = PaddingValues(bottom = MediaCatalogDimens.ListGap),
            ) {
                items(state.series, key = { it.seriesId }) { series ->
                    CatalogContentRow(
                        number = series.number,
                        title = series.title,
                        subtitle = series.subtitle,
                        meta = series.rating?.let { "$it/10" } ?: series.episodeRunTime ?: "Serie",
                        imageUrl = series.coverUrl,
                        fallbackText = series.title.take(2).uppercase(),
                        selected = series.seriesId == state.selectedSeriesId,
                        onFocused = { onSeriesFocused(series) },
                        onClick = { onSeriesClick(series) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesPreviewPanel(
    state: SeriesScreenState,
    onWatch: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val series = state.selectedSeries
    MediaCatalogPanel(
        title = "Apercu",
        modifier = modifier,
    ) {
        if (series == null) {
            CatalogEmpty(
                title = "Selectionnez une serie",
                subtitle = "Aucun apercu actif.",
                modifier = Modifier.fillMaxSize(),
            )
            return@MediaCatalogPanel
        }

        Column(modifier = Modifier.fillMaxSize()) {
            CatalogPosterFrame(
                imageUrl = series.coverUrl,
                title = series.title,
                badge = "SERIE",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.88f),
            )

            Spacer(Modifier.height(8.dp))

            SeriesInfoCard(
                series = series,
                episodes = state.episodes,
                episodesLoading = state.episodesLoading,
                errorMessage = state.errorMessage,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CatalogActionButton(
                    text = "Regarder",
                    onClick = onWatch,
                    icon = Icons.Default.PlayArrow,
                    primary = true,
                    modifier = Modifier
                        .weight(1.25f)
                        .height(32.dp),
                )
                CatalogActionButton(
                    text = "Favori",
                    onClick = onFavorite,
                    icon = Icons.Default.Favorite,
                    selected = series.isFavorite,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            CatalogActionButton(
                text = "Infos serie",
                onClick = {},
                icon = Icons.Default.Info,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
            )
        }
    }
}

@Composable
private fun SeriesInfoCard(
    series: SeriesItemUi,
    episodes: List<SeriesEpisodeUi>,
    episodesLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CatalogBadge(text = "EN COURS", color = SmartVisionColors.Primary)
            Spacer(Modifier.weight(1f))
            Text(
                text = series.releaseDate?.take(4) ?: series.categoryLabel,
                color = SmartVisionColors.TextSecondary,
                style = CatalogMetaStyle,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = series.title,
            color = SmartVisionColors.TextPrimary,
            style = CatalogPreviewTitleStyle,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(5.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = series.genre ?: series.categoryLabel,
                color = SmartVisionColors.TextSecondary,
                style = CatalogMetaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            series.rating?.let {
                CatalogBadge(text = "$it/10", color = SmartVisionColors.PrimaryDark)
            }
        }

        Spacer(Modifier.height(7.dp))

        Text(
            text = series.plot ?: "Episodes disponibles depuis le catalogue Xtream.",
            color = SmartVisionColors.TextSecondary,
            style = CatalogMetaStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SmartVisionColors.Border.copy(alpha = 0.72f)),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Episodes",
            color = SmartVisionColors.TextPrimary,
            style = CatalogMetaStyle,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Spacer(Modifier.height(5.dp))

        when {
            episodesLoading -> Text(
                text = "Chargement...",
                color = SmartVisionColors.TextSecondary,
                style = CatalogMetaStyle,
                maxLines = 1,
            )

            errorMessage != null && episodes.isEmpty() -> Text(
                text = "Episodes indisponibles",
                color = SmartVisionColors.Error,
                style = CatalogMetaStyle,
                maxLines = 1,
            )

            episodes.isEmpty() -> Text(
                text = "Aucun episode",
                color = SmartVisionColors.TextSecondary,
                style = CatalogMetaStyle,
                maxLines = 1,
            )

            else -> episodes.take(2).forEach { episode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = episode.number,
                        color = SmartVisionColors.Primary,
                        style = CatalogMetaStyle,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.width(52.dp),
                    )
                    Text(
                        text = episode.title,
                        color = SmartVisionColors.TextPrimary,
                        style = CatalogMetaStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = episode.duration.orEmpty(),
                        color = SmartVisionColors.TextSecondary,
                        style = CatalogMetaStyle,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun seriesCategoryIcon(label: String): ImageVector {
    val normalized = label.lowercase()
    return when {
        "kids" in normalized || "jeunesse" in normalized -> Icons.Default.Tv
        "anime" in normalized || "animation" in normalized -> Icons.Default.Slideshow
        "top" in normalized || "new" in normalized || "nouveau" in normalized -> Icons.Default.Star
        else -> Icons.Default.Theaters
    }
}
