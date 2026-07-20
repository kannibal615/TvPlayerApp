package com.smartvision.svplayer.ui.movies

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.behavior.BehaviorContent
import com.smartvision.svplayer.data.monetization.MonetizationStatus
import com.smartvision.svplayer.data.monetization.monetizationStatus
import com.smartvision.svplayer.ui.activation.XtreamQrSetupPanel
import com.smartvision.svplayer.ui.catalog.CatalogCategoryRow
import com.smartvision.svplayer.ui.catalog.CatalogEmpty
import com.smartvision.svplayer.ui.catalog.CatalogError
import com.smartvision.svplayer.ui.catalog.CatalogMetaStyle
import com.smartvision.svplayer.ui.catalog.CatalogPanelTitleWithCount
import com.smartvision.svplayer.ui.catalog.CatalogSearchField
import com.smartvision.svplayer.ui.catalog.CatalogSortButton
import com.smartvision.svplayer.ui.catalog.MediaCatalogDimens
import com.smartvision.svplayer.ui.catalog.MediaCatalogHeader
import com.smartvision.svplayer.ui.catalog.MediaCatalogPanel
import com.smartvision.svplayer.ui.catalog.VodCatalogLoadingSkeleton
import com.smartvision.svplayer.ui.catalog.VodContentListLoadingSkeleton
import com.smartvision.svplayer.ui.catalog.VodContentRow
import com.smartvision.svplayer.ui.catalog.VodPreviewContent
import com.smartvision.svplayer.ui.catalog.VodPreviewPanel
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.TvConfirmationDialog
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.focus.awaitItemVisible
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun MoviesScreen(
    strings: SmartVisionStrings,
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
    onLicenseKey: () -> Unit,
    showLicenseKey: Boolean,
    hasNewNotifications: Boolean,
    notificationBadgeCount: Int,
    returnFocusMovieId: Int? = null,
    onReturnFocusConsumed: () -> Unit = {},
    onOpenMovieDetails: (Int) -> Unit,
    onWatchMovie: (Int) -> Unit,
    onPreviewBoundsChanged: (Rect) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: MoviesViewModel = viewModel(
        factory = viewModelFactory {
            MoviesViewModel(
                xtreamRepository = container.xtreamRepository,
                catalogRepository = container.catalogRepository,
                userContentRepository = container.userContentRepository,
                settingsRepository = container.settingsRepository,
                tmdbRepository = container.tmdbRepository,
            )
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by container.accountManager.accounts.collectAsStateWithLifecycle()
    val m3uUrl by container.accountManager.m3uUrl.collectAsStateWithLifecycle()
    val activePlaylistSource by container.accountManager.activePlaylistSource.collectAsStateWithLifecycle()
    val m3uActive = activePlaylistSource == PlaylistSource.M3u && m3uUrl.isNotBlank()
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    val currentTabFocusRequester = remember { FocusRequester() }
    val categoryListState = rememberLazyListState()
    val movieListState = rememberLazyListState()
    val firstMovieFocusRequester = remember { FocusRequester() }
    val focusedMovieFocusRequester = remember { FocusRequester() }
    val returnMovieFocusRequester = remember { FocusRequester() }
    val movieSearchFocusRequester = remember { FocusRequester() }
    val previewPlayFocusRequester = remember { FocusRequester() }
    val behaviorScope = rememberCoroutineScope()
    var inputReady by remember { mutableStateOf(false) }
    var returnFocusHandled by remember { mutableStateOf(false) }
    var pendingFirstMovieFocusCategoryId by remember { mutableStateOf<String?>(null) }
    var movieToDelete by remember { mutableStateOf<MovieItemUi?>(null) }
    var deletedMovieIdAwaitingFocus by remember { mutableStateOf<Int?>(null) }
    var pendingPreviewMovieId by remember { mutableStateOf<Int?>(null) }
    var showFreeAdsPreview by remember { mutableStateOf(false) }
    var tvCode by remember { mutableStateOf("") }
    var premiumPurchaseUrl by remember {
        mutableStateOf(
            BuildConfig.ACTIVATION_BASE_URL.trimEnd('/') +
                "/account/?source=tv&intent=license&plan=year_1",
        )
    }

    LaunchedEffect(Unit) {
        delay(260)
        inputReady = true
    }

    LaunchedEffect(container.activationRepository) {
        container.activationRepository.localState.collect { activation ->
            showFreeAdsPreview = activation.monetizationStatus() == MonetizationStatus.FREE_WITH_ADS
            tvCode = activation.publicDeviceCode.ifBlank { activation.deviceId.take(8).uppercase() }
            val deviceQuery = when {
                activation.publicDeviceCode.isNotBlank() -> "device=${activation.publicDeviceCode}"
                activation.deviceId.isNotBlank() -> "device_id=${activation.deviceId}"
                else -> ""
            }
            premiumPurchaseUrl = BuildConfig.ACTIVATION_BASE_URL.trimEnd('/') +
                "/account/?source=tv&intent=license" +
                if (deviceQuery.isBlank()) "&plan=year_1" else "&$deviceQuery&plan=year_1"
        }
    }

    suspend fun focusSelectedCategory() {
        val selectedIndex = state.categories.indexOfFirst { it.id == state.selectedCategoryId }
            .takeIf { it >= 0 }
            ?: 0
        if (state.categories.isEmpty()) return
        categoryListState.scrollToItem(selectedIndex)
        if (!categoryListState.awaitItemVisible(selectedIndex)) return
        withFrameNanos { }
        runCatching { selectedCategoryFocusRequester.requestFocus() }
    }

    suspend fun focusMovieColumn() {
        val movies = state.displayedMovies
        if (movies.isEmpty()) {
            runCatching { movieSearchFocusRequester.requestFocus() }
            return
        }
        val targetIndex = movies.indexOfFirst { it.streamId == state.focusedMovieId }
            .takeIf { it >= 0 }
            ?: 0
        movieListState.scrollToItem((targetIndex - 2).coerceAtLeast(0))
        if (!movieListState.awaitItemVisible(targetIndex)) return
        withFrameNanos { }
        runCatching {
            when {
                state.focusedMovieId == returnFocusMovieId -> returnMovieFocusRequester.requestFocus()
                targetIndex == 0 -> firstMovieFocusRequester.requestFocus()
                else -> focusedMovieFocusRequester.requestFocus()
            }
        }
    }

    fun enterMoviePreview(movie: MovieItemUi) {
        pendingPreviewMovieId = movie.streamId
        if (state.selectedMovieId != movie.streamId) viewModel.activateMovie(movie)
    }

    LaunchedEffect(pendingPreviewMovieId, state.selectedMovieId) {
        val movieId = pendingPreviewMovieId ?: return@LaunchedEffect
        if (state.selectedMovieId != movieId) return@LaunchedEffect
        withFrameNanos { }
        runCatching { previewPlayFocusRequester.requestFocus() }
        pendingPreviewMovieId = null
    }

    LaunchedEffect(deletedMovieIdAwaitingFocus, state.movies) {
        val deletedId = deletedMovieIdAwaitingFocus ?: return@LaunchedEffect
        if (state.movies.any { it.streamId == deletedId }) return@LaunchedEffect
        focusSelectedCategory()
        deletedMovieIdAwaitingFocus = null
    }

    LaunchedEffect(state.categoriesLoading, accounts.isNotEmpty(), m3uActive, state.categories.size, returnFocusMovieId) {
        if (accounts.isEmpty() || returnFocusMovieId != null || returnFocusHandled) return@LaunchedEffect
        withFrameNanos { }
        delay(90)
        if (!m3uActive && !state.categoriesLoading && state.categories.isNotEmpty()) {
            focusSelectedCategory()
        } else {
            runCatching { currentTabFocusRequester.requestFocus() }
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
            onProfile = onProfile,
            onNotifications = onNotifications,
            onLicenseKey = onLicenseKey,
            showLicenseKey = showLicenseKey,
            hasNewNotifications = hasNewNotifications,
            notificationBadgeCount = notificationBadgeCount,
            modifier = Modifier.fillMaxWidth(),
            currentTabFocusRequester = currentTabFocusRequester,
            onContentDown = if (!m3uActive && accounts.isNotEmpty() && state.categories.isNotEmpty()) {
                { behaviorScope.launch { focusSelectedCategory() } }
            } else {
                null
            },
        )

        Spacer(Modifier.height(MediaCatalogDimens.HeaderGap))

        Box(modifier = Modifier.fillMaxSize()) {
        if (m3uActive) {
            CatalogEmpty(
                title = "Films non disponibles en M3U",
                subtitle = "La source M3U active alimente Live TV. Passez sur Xtream pour les films.",
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }

        if (accounts.isEmpty()) {
            XtreamQrSetupPanel(
                activationRepository = container.activationRepository,
                title = "Configurer votre catalogue de films",
                onManualAccount = { account ->
                    val accountId = container.accountManager.upsert(account)
                    container.accountManager.select(accountId)
                    container.xtreamRepository.clearCaches()
                    container.xtreamRepository.getLiveCategories()
                    container.synchronizeCatalog()
                },
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }

        if (state.categoriesLoading) {
            VodCatalogLoadingSkeleton(
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
                    currentTabFocusRequester = currentTabFocusRequester,
                    listState = categoryListState,
                    onCategory = { category ->
                        if (inputReady) {
                            pendingFirstMovieFocusCategoryId = category.id
                            viewModel.selectCategory(category)
                        }
                    },
                    onRight = { behaviorScope.launch { focusMovieColumn() } },
                    modifier = Modifier
                        .weight(0.24f)
                        .fillMaxHeight(),
                )
                MovieList(
                    state = state,
                    listState = movieListState,
                    searchFocusRequester = movieSearchFocusRequester,
                    firstMovieFocusRequester = firstMovieFocusRequester,
                    focusedMovieFocusRequester = focusedMovieFocusRequester,
                    returnMovieFocusRequester = returnMovieFocusRequester,
                    returnFocusMovieId = returnFocusMovieId,
                    focusFirstAfterCategoryId = pendingFirstMovieFocusCategoryId,
                    onFirstAfterCategoryFocused = { pendingFirstMovieFocusCategoryId = null },
                    onReturnFocusConsumed = {
                        returnFocusHandled = true
                        onReturnFocusConsumed()
                    },
                    onSearchQueryChange = viewModel::updateContentSearchQuery,
                    onSortSelected = viewModel::setSortMode,
                    onMovieFocused = viewModel::focusMovie,
                    onRestoreCategoryFocus = { behaviorScope.launch { focusSelectedCategory() } },
                    onRestoreMovieFocus = { behaviorScope.launch { focusMovieColumn() } },
                    onEnterPreview = ::enterMoviePreview,
                    onMovieClick = { movie ->
                        if (inputReady) {
                            container.behaviorReporter.reportAsync(
                                behaviorScope,
                                "CONTENT_OPENED",
                                movie.toBehaviorContent(),
                            )
                            val openFullPlayer = viewModel.activateMovie(movie)
                            if (openFullPlayer) {
                                onWatchMovie(movie.streamId)
                            }
                        }
                    },
                    onLoadNextPage = viewModel::loadNextPage,
                    onRetry = viewModel::retryCurrentCategory,
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                )
                VodPreviewPanel(
                    title = "Preview",
                    content = state.selectedMovie?.toPreviewContent(),
                    playFocusRequester = previewPlayFocusRequester,
                    onPlay = { state.selectedMovie?.let { onWatchMovie(it.streamId) } },
                    onDetails = { state.selectedMovie?.let { onOpenMovieDetails(it.streamId) } },
                    onFavorite = { state.selectedMovie?.let(viewModel::toggleFavorite) },
                    onDeleteHistory = { state.selectedMovie?.let { movieToDelete = it } },
                    showDeleteHistory = state.selectedCategory?.label == "Historique",
                    showFreeAdsPreview = showFreeAdsPreview,
                    idleAdEnabled = state.movies.isNotEmpty(),
                    idleVastAdLoader = container.idleVastAdLoader,
                    monetizationManager = container.monetizationManager,
                    premiumPurchaseUrl = premiumPurchaseUrl,
                    tvCode = tvCode,
                    seasonsLabel = strings.seasonsLabel,
                    episodesLoadingLabel = strings.episodesLoading,
                    episodesEmptyLabel = strings.episodesEmpty,
                    resumeLabel = strings.resumePlayback,
                    progressLabel = strings.progressLabel,
                    onNavigateLeft = { behaviorScope.launch { focusMovieColumn() } },
                    onPreviewBoundsChanged = onPreviewBoundsChanged,
                    modifier = Modifier
                        .weight(0.34f)
                        .fillMaxHeight(),
                )
            }
        }
        }
    }

    movieToDelete?.let { movie ->
        TvConfirmationDialog(
            title = strings.moviesDeleteHistoryTitle,
            itemLabel = movie.title,
            message = strings.destructiveActionWarning,
            confirmText = strings.delete,
            cancelText = strings.cancel,
            onDismiss = { movieToDelete = null },
            onConfirm = {
                movieToDelete = null
                deletedMovieIdAwaitingFocus = movie.streamId
                viewModel.deleteHistoryMovie(movie)
            },
        )
    }
}

private fun MovieItemUi.toBehaviorContent(): BehaviorContent =
    BehaviorContent(
        contentType = "MOVIE",
        contentId = streamId.toString(),
        title = title,
        categoryLabel = categoryLabel,
        sourceScreen = "MOVIES",
        engagementScore = 40,
        tags = listOfNotNull(containerExtension, year, rating, if (isFavorite) "favorite" else null),
        context = mapOf("number" to number),
    )

@Composable
private fun MovieCategoryList(
    state: MoviesScreenState,
    selectedCategoryFocusRequester: FocusRequester,
    currentTabFocusRequester: FocusRequester,
    listState: LazyListState,
    onCategory: (MovieCategoryUi) -> Unit,
    onRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusCategoryId = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.id
        ?: state.categories.firstOrNull()?.id

    MediaCatalogPanel(
        title = "Categories",
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(MediaCatalogDimens.ListGap),
            contentPadding = PaddingValues(bottom = MediaCatalogDimens.ListGap),
        ) {
            itemsIndexed(
                state.categories,
                key = { _, category -> category.id },
            ) { index, category ->
                CatalogCategoryRow(
                    label = category.label,
                    count = category.count,
                    icon = movieCategoryIcon(category.label),
                    selected = category.id == state.selectedCategoryId,
                    focusRequester = if (category.id == focusCategoryId) selectedCategoryFocusRequester else null,
                    upFocusRequester = currentTabFocusRequester.takeIf { index == 0 },
                    onRight = onRight,
                    onClick = { onCategory(category) },
                )
            }
        }
    }
}

@Composable
private fun MovieList(
    state: MoviesScreenState,
    listState: LazyListState,
    searchFocusRequester: FocusRequester,
    firstMovieFocusRequester: FocusRequester,
    focusedMovieFocusRequester: FocusRequester,
    returnMovieFocusRequester: FocusRequester,
    returnFocusMovieId: Int?,
    focusFirstAfterCategoryId: String?,
    onFirstAfterCategoryFocused: () -> Unit,
    onReturnFocusConsumed: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortSelected: (MovieSortMode) -> Unit,
    onMovieFocused: (MovieItemUi) -> Unit,
    onRestoreCategoryFocus: () -> Unit,
    onRestoreMovieFocus: () -> Unit,
    onEnterPreview: (MovieItemUi) -> Unit,
    onMovieClick: (MovieItemUi) -> Unit,
    onLoadNextPage: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sortFocusRequester = remember { FocusRequester() }

    LaunchedEffect(
        focusFirstAfterCategoryId,
        state.selectedCategoryId,
        state.moviesLoading,
        state.itemsLoading,
        state.movies.size,
    ) {
        val categoryId = focusFirstAfterCategoryId ?: return@LaunchedEffect
        if (state.selectedCategoryId != categoryId || state.moviesLoading || state.itemsLoading) return@LaunchedEffect
        if (state.movies.isEmpty()) {
            onFirstAfterCategoryFocused()
            return@LaunchedEffect
        }
        listState.scrollToItem(0)
        if (!listState.awaitItemVisible(0)) return@LaunchedEffect
        withFrameNanos { }
        runCatching { firstMovieFocusRequester.requestFocus() }
        onFirstAfterCategoryFocused()
    }

    LaunchedEffect(returnFocusMovieId, state.displayedMovies) {
        val movieId = returnFocusMovieId ?: return@LaunchedEffect
        val targetIndex = state.displayedMovies.indexOfFirst { it.streamId == movieId }
        if (targetIndex < 0) {
            return@LaunchedEffect
        }
        listState.scrollToItem((targetIndex - 2).coerceAtLeast(0))
        if (!listState.awaitItemVisible(targetIndex)) return@LaunchedEffect
        withFrameNanos { }
        runCatching { returnMovieFocusRequester.requestFocus() }
        onReturnFocusConsumed()
    }

    LaunchedEffect(state.selectedCategoryId, state.contentSearchQuery, state.sortMode) {
        if (returnFocusMovieId != null) return@LaunchedEffect
        if (listState.layoutInfo.totalItemsCount > 0) listState.scrollToItem(0)
    }
    LaunchedEffect(listState, state.movies.size, state.hasMoreItems, state.nextPageLoading, state.contentSearchQuery) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: 0 }
            .collect { lastVisibleIndex ->
                val remaining = state.movies.lastIndex - lastVisibleIndex
                if (state.hasMoreItems && !state.nextPageLoading && remaining <= MovieNextPageThreshold) {
                    onLoadNextPage()
                }
            }
    }

    MediaCatalogPanel(
        title = "Movies",
        modifier = modifier,
        titleContent = {
            CatalogPanelTitleWithCount(
                title = "Movies",
                count = state.selectedCategory?.count ?: state.displayedMovies.size,
            )
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CatalogSearchField(
                    query = state.contentSearchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = "Film",
                    modifier = Modifier
                        .focusRequester(searchFocusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionDown -> {
                                    onRestoreMovieFocus()
                                    true
                                }
                                Key.DirectionLeft -> {
                                    onRestoreCategoryFocus()
                                    true
                                }
                                Key.DirectionRight -> {
                                    runCatching { sortFocusRequester.requestFocus() }
                                    true
                                }
                                else -> false
                            }
                        },
                )
                Spacer(Modifier.width(6.dp))
                CatalogSortButton(
                    options = MovieSortMode.entries.map { it.label },
                    selectedIndex = state.sortMode.ordinal,
                    onSelected = { onSortSelected(MovieSortMode.entries[it]) },
                    focusRequester = sortFocusRequester,
                    leftFocusRequester = searchFocusRequester,
                )
            }
        },
    ) {
        when {
            state.moviesLoading && state.movies.isEmpty() -> VodContentListLoadingSkeleton(
                modifier = Modifier.fillMaxSize(),
            )

            state.errorMessage != null && state.movies.isEmpty() && !state.categoriesLoading && !state.moviesLoading && !state.itemsLoading -> CatalogError(
                message = state.errorMessage,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )

            state.movies.isEmpty() -> CatalogEmpty(
                title = if (state.contentSearchQuery.isBlank()) "Aucun film" else "Aucun resultat",
                subtitle = if (state.contentSearchQuery.isBlank()) "Selectionnez une autre categorie." else "Modifiez votre recherche.",
                modifier = Modifier.fillMaxSize(),
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MediaCatalogDimens.ListGap),
                contentPadding = PaddingValues(bottom = MediaCatalogDimens.ListGap),
            ) {
                itemsIndexed(
                    items = state.displayedMovies,
                    key = { _, movie -> movie.streamId },
                ) { index, movie ->
                    val itemFocusRequester = remember(movie.streamId) { FocusRequester() }
                    VodContentRow(
                        title = movie.title,
                        subtitle = movie.year?.take(4)?.takeIf { it.all(Char::isDigit) }
                            ?: movie.categoryLabel,
                        genre = movie.genre
                            ?.substringBefore('/')
                            ?.substringBefore(',')
                            ?.trim()
                            ?.takeIf(String::isNotEmpty),
                        rating = movie.rating,
                        sideLabel = movie.duration.orEmpty(),
                        imageUrl = movie.backdropUrl ?: movie.posterUrl,
                        fallbackText = movie.title.take(2).uppercase(),
                        selected = movie.streamId == state.selectedMovieId,
                        focusRequester = when {
                            movie.streamId == returnFocusMovieId -> returnMovieFocusRequester
                            index == 0 -> firstMovieFocusRequester
                            movie.streamId == state.focusedMovieId -> focusedMovieFocusRequester
                            else -> itemFocusRequester
                        },
                        rightFocusRequester = null,
                        upFocusRequester = sortFocusRequester.takeIf { index == 0 },
                        onLeft = onRestoreCategoryFocus,
                        onRight = { onEnterPreview(movie) },
                        onFocused = { onMovieFocused(movie) },
                        onClick = { onMovieClick(movie) },
                        ratingFirst = true,
                    )
                }
            }
        }
    }
}

private fun MovieItemUi.toPreviewContent(): VodPreviewContent =
    VodPreviewContent(
        id = "movie-$streamId",
        title = title,
        subtitle = subtitle,
        imageUrl = posterUrl,
        backdropUrl = backdropUrl,
        streamUrl = streamUrl,
        durationLabel = duration,
        sideLabel = duration,
        year = year,
        rating = rating,
        genre = genre,
        plot = plot,
        creditLabel = director?.let { "Director: $it" },
        cast = cast,
        isFavorite = isFavorite,
    )

private const val MovieNextPageThreshold = 15

private fun movieCategoryIcon(label: String): ImageVector? {
    val normalized = label.lowercase()
    return when {
        normalized == "all" -> Icons.Default.Menu
        "favoris" in normalized -> Icons.Default.Favorite
        "histor" in normalized -> Icons.Default.History
        else -> null
    }
}
