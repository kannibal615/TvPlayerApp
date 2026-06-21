package com.smartvision.svplayer.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.ui.activation.XtreamQrSetupPanel
import com.smartvision.svplayer.ui.catalog.CatalogCategoryRow
import com.smartvision.svplayer.ui.catalog.CatalogEmpty
import com.smartvision.svplayer.ui.catalog.CatalogError
import com.smartvision.svplayer.ui.catalog.CatalogLoading
import com.smartvision.svplayer.ui.catalog.CatalogMediaCard
import com.smartvision.svplayer.ui.catalog.CatalogMetaStyle
import com.smartvision.svplayer.ui.catalog.MediaCatalogDimens
import com.smartvision.svplayer.ui.catalog.MediaCatalogHeader
import com.smartvision.svplayer.ui.catalog.MediaCatalogPanel
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import kotlinx.coroutines.delay

@Suppress("UNUSED_PARAMETER")
@Composable
fun SeriesScreen(
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    onOpenSeriesDetails: (Int) -> Unit,
    onWatchEpisode: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: SeriesViewModel = viewModel(
        factory = viewModelFactory {
            SeriesViewModel(
                xtreamRepository = container.xtreamRepository,
                userContentRepository = container.userContentRepository,
            )
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by container.accountManager.accounts.collectAsStateWithLifecycle()
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    val firstSeriesFocusRequester = remember { FocusRequester() }
    var inputReady by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        delay(260)
        inputReady = true
    }

    LaunchedEffect(state.selectedCategoryId, state.categoriesLoading, accounts.isNotEmpty()) {
        if (accounts.isNotEmpty() && !state.categoriesLoading) {
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
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(MediaCatalogDimens.HeaderGap))

        if (accounts.isEmpty()) {
            XtreamQrSetupPanel(
                activationRepository = container.activationRepository,
                title = "Configurer votre catalogue de séries",
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }

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
                    firstSeriesFocusRequester = firstSeriesFocusRequester,
                    searchQuery = searchQuery,
                    onCategory = { category ->
                        if (inputReady) viewModel.selectCategory(category)
                    },
                    modifier = Modifier
                        .weight(0.22f)
                        .fillMaxHeight(),
                )
                SeriesGrid(
                    state = state,
                    firstSeriesFocusRequester = firstSeriesFocusRequester,
                    selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                    searchQuery = searchQuery,
                    onSeriesFocused = viewModel::focusSeries,
                    onSeriesClick = { series ->
                        if (inputReady) {
                            viewModel.focusSeries(series)
                            onOpenSeriesDetails(series.seriesId)
                        }
                    },
                    onRetry = viewModel::retryCurrentCategory,
                    modifier = Modifier
                        .weight(0.78f)
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
    firstSeriesFocusRequester: FocusRequester,
    searchQuery: String,
    onCategory: (SeriesCategoryUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    MediaCatalogPanel(title = "Categories", modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(MediaCatalogDimens.ListGap),
            contentPadding = PaddingValues(bottom = MediaCatalogDimens.ListGap),
        ) {
            items(
                state.categories.filter { searchQuery.isBlank() || it.label.contains(searchQuery, ignoreCase = true) },
                key = { it.id },
            ) { category ->
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
                    rightFocusRequester = firstSeriesFocusRequester.takeIf {
                        !state.seriesLoading && state.series.any {
                            searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
                        }
                    },
                    onClick = { onCategory(category) },
                )
            }
        }
    }
}

@Composable
private fun SeriesGrid(
    state: SeriesScreenState,
    firstSeriesFocusRequester: FocusRequester,
    selectedCategoryFocusRequester: FocusRequester,
    searchQuery: String,
    onSeriesFocused: (SeriesItemUi) -> Unit,
    onSeriesClick: (SeriesItemUi) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val visibleSeries = state.series.filter { series ->
        searchQuery.isBlank() ||
            series.title.contains(searchQuery, ignoreCase = true) ||
            series.categoryLabel.contains(searchQuery, ignoreCase = true)
    }

    LaunchedEffect(state.selectedCategoryId) {
        if (gridState.layoutInfo.totalItemsCount > 0) gridState.scrollToItem(0)
    }

    MediaCatalogPanel(
        title = state.selectedCategory?.label ?: "Series",
        modifier = modifier,
        trailing = {
            val seriesCount = if (searchQuery.isBlank()) {
                state.selectedCategory?.count ?: state.series.size
            } else {
                visibleSeries.size
            }
            Text(
                text = if (seriesCount > 0) "$seriesCount series" else "Series",
                color = SmartVisionColors.TextSecondary,
                style = CatalogMetaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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

            visibleSeries.isEmpty() -> CatalogEmpty(
                title = if (searchQuery.isBlank()) "Aucune serie" else "Aucun resultat",
                subtitle = if (searchQuery.isBlank()) "Selectionnez une autre categorie." else "Modifiez votre recherche.",
                modifier = Modifier.fillMaxSize(),
            )

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(MediaCatalogDimens.MediaGridColumns),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(MediaCatalogDimens.MediaGridGap),
                verticalArrangement = Arrangement.spacedBy(MediaCatalogDimens.MediaGridGap),
                contentPadding = PaddingValues(bottom = MediaCatalogDimens.MediaGridGap),
            ) {
                itemsIndexed(
                    items = visibleSeries,
                    key = { _, series -> series.seriesId },
                ) { index, series ->
                    CatalogMediaCard(
                        title = series.title,
                        meta = seriesCardMeta(series),
                        imageUrl = series.coverUrl,
                        fallbackText = series.title.take(2).uppercase(),
                        selected = series.seriesId == state.selectedSeriesId,
                        favorite = series.isFavorite,
                        focusRequester = firstSeriesFocusRequester.takeIf { index == 0 },
                        leftFocusRequester = selectedCategoryFocusRequester.takeIf {
                            index % MediaCatalogDimens.MediaGridColumns == 0
                        },
                        onFocused = { onSeriesFocused(series) },
                        onClick = { onSeriesClick(series) },
                    )
                }
            }
        }
    }
}

private fun seriesCardMeta(series: SeriesItemUi): String =
    listOfNotNull(
        series.releaseDate?.take(4),
        series.rating?.let { "$it/10" },
        series.seasonsCount?.let { "$it S" },
        series.episodesCount?.let { "$it ep" },
    ).joinToString("  |  ").ifBlank { series.categoryLabel }

private fun seriesCategoryIcon(label: String): ImageVector {
    val normalized = label.lowercase()
    return when {
        "favoris" in normalized -> Icons.Default.Favorite
        "histor" in normalized -> Icons.Default.History
        "kids" in normalized || "jeunesse" in normalized -> Icons.Default.Tv
        "anime" in normalized || "animation" in normalized -> Icons.Default.Slideshow
        "top" in normalized || "new" in normalized || "nouveau" in normalized -> Icons.Default.Star
        else -> Icons.Default.Theaters
    }
}
