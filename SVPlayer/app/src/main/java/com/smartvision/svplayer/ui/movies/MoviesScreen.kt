package com.smartvision.svplayer.ui.movies

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
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Theaters
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
                MovieCategoryList(
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
                MovieList(
                    state = state,
                    onMovieFocused = viewModel::focusMovie,
                    onMovieClick = { movie ->
                        if (inputReady) {
                            viewModel.activateMovie(movie)
                            onOpenMovieDetails(movie.streamId)
                        }
                    },
                    onRetry = viewModel::retryCurrentCategory,
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                )
                MoviePreviewPanel(
                    movie = state.selectedMovie,
                    onWatch = {
                        if (inputReady) {
                            state.selectedMovie?.streamId?.let(onWatchMovie)
                        }
                    },
                    onFavorite = { state.selectedMovie?.let(viewModel::toggleFavorite) },
                    modifier = Modifier
                        .weight(0.34f)
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
    onCategory: (MovieCategoryUi) -> Unit,
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
                    icon = movieCategoryIcon(category.label),
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
private fun MovieList(
    state: MoviesScreenState,
    onMovieFocused: (MovieItemUi) -> Unit,
    onMovieClick: (MovieItemUi) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MediaCatalogPanel(
        title = "Films",
        modifier = modifier,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val movieCount = state.selectedCategory?.count ?: state.movies.size
                Text(
                    text = if (movieCount > 0) "$movieCount films" else "films",
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
            state.moviesLoading -> CatalogLoading(
                title = "Chargement des films",
                modifier = Modifier.fillMaxSize(),
            )

            state.errorMessage != null -> CatalogError(
                message = state.errorMessage,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )

            state.movies.isEmpty() -> CatalogEmpty(
                title = "Aucun film",
                subtitle = "Selectionnez une autre categorie.",
                modifier = Modifier.fillMaxSize(),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MediaCatalogDimens.ListGap),
                contentPadding = PaddingValues(bottom = MediaCatalogDimens.ListGap),
            ) {
                items(state.movies, key = { it.streamId }) { movie ->
                    CatalogContentRow(
                        number = movie.number,
                        title = movie.title,
                        subtitle = movie.subtitle,
                        meta = movie.rating?.let { "$it/10" } ?: movie.containerExtension.uppercase(),
                        imageUrl = movie.posterUrl,
                        fallbackText = movie.title.take(2).uppercase(),
                        selected = movie.streamId == state.selectedMovieId,
                        onFocused = { onMovieFocused(movie) },
                        onClick = { onMovieClick(movie) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MoviePreviewPanel(
    movie: MovieItemUi?,
    onWatch: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MediaCatalogPanel(
        title = "Apercu",
        modifier = modifier,
    ) {
        if (movie == null) {
            CatalogEmpty(
                title = "Selectionnez un film",
                subtitle = "Aucun apercu actif.",
                modifier = Modifier.fillMaxSize(),
            )
            return@MediaCatalogPanel
        }

        Column(modifier = Modifier.fillMaxSize()) {
            CatalogPosterFrame(
                imageUrl = movie.posterUrl,
                title = movie.title,
                badge = "VOD",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.88f),
            )

            Spacer(Modifier.height(8.dp))

            MovieInfoCard(
                movie = movie,
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
                    selected = movie.isFavorite,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            CatalogActionButton(
                text = "Infos film",
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
private fun MovieInfoCard(
    movie: MovieItemUi,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CatalogBadge(text = movie.containerExtension.uppercase(), color = SmartVisionColors.Primary)
            Spacer(Modifier.weight(1f))
            Text(
                text = movie.year ?: movie.categoryLabel,
                color = SmartVisionColors.TextSecondary,
                style = CatalogMetaStyle,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = movie.title,
            color = SmartVisionColors.TextPrimary,
            style = CatalogPreviewTitleStyle,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(5.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = movie.categoryLabel,
                color = SmartVisionColors.TextSecondary,
                style = CatalogMetaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            movie.rating?.let {
                CatalogBadge(text = "$it/10", color = SmartVisionColors.PrimaryDark)
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Film VOD disponible dans ${movie.categoryLabel}.",
            color = SmartVisionColors.TextSecondary,
            style = CatalogMetaStyle,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SmartVisionColors.Border.copy(alpha = 0.72f)),
        )

        Spacer(Modifier.height(9.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            CatalogBadge(text = "XTREAM", color = SmartVisionColors.PrimaryDark)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Catalogue films",
                color = SmartVisionColors.TextSecondary,
                style = CatalogMetaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun movieCategoryIcon(label: String): ImageVector {
    val normalized = label.lowercase()
    return when {
        "favoris" in normalized -> Icons.Default.Favorite
        "action" in normalized || "thriller" in normalized -> Icons.Default.LocalMovies
        "classic" in normalized || "cinema" in normalized -> Icons.Default.Movie
        "top" in normalized || "new" in normalized || "nouveau" in normalized -> Icons.Default.Star
        else -> Icons.Default.Theaters
    }
}
