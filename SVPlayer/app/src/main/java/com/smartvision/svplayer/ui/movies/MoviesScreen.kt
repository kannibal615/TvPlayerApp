package com.smartvision.svplayer.ui.movies

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Theaters
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
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

@Composable
fun MoviesScreen(
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    onOpenMovieDetails: (Int) -> Unit,
    onWatchMovie: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: MoviesViewModel = viewModel(
        factory = viewModelFactory {
            MoviesViewModel(
                xtreamRepository = container.xtreamRepository,
                userContentRepository = container.userContentRepository,
            )
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    val firstMovieFocusRequester = remember { FocusRequester() }
    var inputReady by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

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
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
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
                MovieCategoryList(
                    state = state,
                    selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                    firstMovieFocusRequester = firstMovieFocusRequester,
                    searchQuery = searchQuery,
                    onCategory = { category ->
                        if (inputReady) viewModel.selectCategory(category)
                    },
                    modifier = Modifier
                        .weight(0.22f)
                        .fillMaxHeight(),
                )
                MovieGrid(
                    state = state,
                    firstMovieFocusRequester = firstMovieFocusRequester,
                    selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                    searchQuery = searchQuery,
                    onMovieFocused = viewModel::focusMovie,
                    onMovieClick = { movie ->
                        if (inputReady) {
                            viewModel.activateMovie(movie)
                            onOpenMovieDetails(movie.streamId)
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
private fun MovieCategoryList(
    state: MoviesScreenState,
    selectedCategoryFocusRequester: FocusRequester,
    firstMovieFocusRequester: FocusRequester,
    searchQuery: String,
    onCategory: (MovieCategoryUi) -> Unit,
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
                    icon = movieCategoryIcon(category.label),
                    selected = category.id == state.selectedCategoryId,
                    focusRequester = if (category.id == state.selectedCategoryId) {
                        selectedCategoryFocusRequester
                    } else {
                        null
                    },
                    rightFocusRequester = firstMovieFocusRequester.takeIf {
                        !state.moviesLoading && state.movies.any {
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
private fun MovieGrid(
    state: MoviesScreenState,
    firstMovieFocusRequester: FocusRequester,
    selectedCategoryFocusRequester: FocusRequester,
    searchQuery: String,
    onMovieFocused: (MovieItemUi) -> Unit,
    onMovieClick: (MovieItemUi) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val visibleMovies = state.movies.filter { movie ->
        searchQuery.isBlank() ||
            movie.title.contains(searchQuery, ignoreCase = true) ||
            movie.categoryLabel.contains(searchQuery, ignoreCase = true)
    }

    LaunchedEffect(state.selectedCategoryId) {
        if (gridState.layoutInfo.totalItemsCount > 0) gridState.scrollToItem(0)
    }

    MediaCatalogPanel(
        title = state.selectedCategory?.label ?: "Films",
        modifier = modifier,
        trailing = {
            val movieCount = if (searchQuery.isBlank()) {
                state.selectedCategory?.count ?: state.movies.size
            } else {
                visibleMovies.size
            }
            Text(
                text = if (movieCount > 0) "$movieCount films" else "Films",
                color = SmartVisionColors.TextSecondary,
                style = CatalogMetaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    ) {
        when {
            state.moviesLoading -> CatalogLoading(
                title = "Chargement des films",
                modifier = Modifier.fillMaxSize(),
            )

            state.errorMessage != null -> CatalogError(
                message = state.errorMessage,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )

            visibleMovies.isEmpty() -> CatalogEmpty(
                title = if (searchQuery.isBlank()) "Aucun film" else "Aucun resultat",
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
                    items = visibleMovies,
                    key = { _, movie -> movie.streamId },
                ) { index, movie ->
                    CatalogMediaCard(
                        title = movie.title,
                        meta = movieCardMeta(movie),
                        imageUrl = movie.posterUrl,
                        fallbackText = movie.title.take(2).uppercase(),
                        selected = movie.streamId == state.selectedMovieId,
                        favorite = movie.isFavorite,
                        focusRequester = firstMovieFocusRequester.takeIf { index == 0 },
                        leftFocusRequester = selectedCategoryFocusRequester.takeIf {
                            index % MediaCatalogDimens.MediaGridColumns == 0
                        },
                        onFocused = { onMovieFocused(movie) },
                        onClick = { onMovieClick(movie) },
                    )
                }
            }
        }
    }
}

private fun movieCardMeta(movie: MovieItemUi): String =
    listOfNotNull(
        movie.year,
        movie.rating?.let { "$it/10" },
        movie.containerExtension.uppercase().takeIf { it.isNotBlank() },
    ).joinToString("  |  ").ifBlank { movie.categoryLabel }

private fun movieCategoryIcon(label: String): ImageVector {
    val normalized = label.lowercase()
    return when {
        "favoris" in normalized -> Icons.Default.Favorite
        "histor" in normalized -> Icons.Default.History
        "action" in normalized || "thriller" in normalized -> Icons.Default.LocalMovies
        "classic" in normalized || "cinema" in normalized -> Icons.Default.Movie
        "top" in normalized || "new" in normalized || "nouveau" in normalized -> Icons.Default.Star
        else -> Icons.Default.Theaters
    }
}
