package com.smartvision.svplayer.ui.series

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
import com.smartvision.svplayer.ui.catalog.VodPreviewEpisode
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
fun SeriesScreen(
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
    returnFocusSeriesId: Int? = null,
    onReturnFocusConsumed: () -> Unit = {},
    onOpenSeriesDetails: (Int) -> Unit,
    onWatchEpisode: (episodeId: Int, seriesId: Int) -> Unit,
    onPreviewBoundsChanged: (Rect) -> Unit = {},
    modifier: Modifier = Modifier,
    headerTransitionModifier: Modifier = Modifier,
    contentTransitionSurfaceModifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: SeriesViewModel = viewModel(
        factory = viewModelFactory {
            SeriesViewModel(
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
    val seriesListState = rememberLazyListState()
    val firstSeriesFocusRequester = remember { FocusRequester() }
    val focusedSeriesFocusRequester = remember { FocusRequester() }
    val returnSeriesFocusRequester = remember { FocusRequester() }
    val seriesSearchFocusRequester = remember { FocusRequester() }
    val previewPlayFocusRequester = remember { FocusRequester() }
    val behaviorScope = rememberCoroutineScope()
    var inputReady by remember { mutableStateOf(false) }
    var returnFocusHandled by remember { mutableStateOf(false) }
    var pendingFirstSeriesFocusCategoryId by remember { mutableStateOf<String?>(null) }
    var seriesToDelete by remember { mutableStateOf<SeriesItemUi?>(null) }
    var deletedSeriesIdAwaitingFocus by remember { mutableStateOf<Int?>(null) }
    var pendingPreviewSeriesId by remember { mutableStateOf<Int?>(null) }
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

    suspend fun focusSeriesColumn() {
        val seriesItems = state.displayedSeries
        if (seriesItems.isEmpty()) {
            runCatching { seriesSearchFocusRequester.requestFocus() }
            return
        }
        val targetIndex = seriesItems.indexOfFirst { it.seriesId == state.focusedSeriesId }
            .takeIf { it >= 0 }
            ?: 0
        seriesListState.scrollToItem((targetIndex - 2).coerceAtLeast(0))
        if (!seriesListState.awaitItemVisible(targetIndex)) return
        withFrameNanos { }
        runCatching {
            when {
                state.focusedSeriesId == returnFocusSeriesId -> returnSeriesFocusRequester.requestFocus()
                targetIndex == 0 -> firstSeriesFocusRequester.requestFocus()
                else -> focusedSeriesFocusRequester.requestFocus()
            }
        }
    }

    fun enterSeriesPreview(series: SeriesItemUi) {
        pendingPreviewSeriesId = series.seriesId
        if (state.selectedSeriesId != series.seriesId) viewModel.activateSeries(series)
    }

    LaunchedEffect(pendingPreviewSeriesId, state.selectedSeriesId) {
        val seriesId = pendingPreviewSeriesId ?: return@LaunchedEffect
        if (state.selectedSeriesId != seriesId) return@LaunchedEffect
        withFrameNanos { }
        runCatching { previewPlayFocusRequester.requestFocus() }
        pendingPreviewSeriesId = null
    }

    LaunchedEffect(deletedSeriesIdAwaitingFocus, state.series) {
        val deletedId = deletedSeriesIdAwaitingFocus ?: return@LaunchedEffect
        if (state.series.any { it.seriesId == deletedId }) return@LaunchedEffect
        focusSelectedCategory()
        deletedSeriesIdAwaitingFocus = null
    }

    LaunchedEffect(state.categoriesLoading, accounts.isNotEmpty(), m3uActive, state.categories.size, returnFocusSeriesId) {
        if (accounts.isEmpty() || returnFocusSeriesId != null || returnFocusHandled) return@LaunchedEffect
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
            modifier = headerTransitionModifier.fillMaxWidth(),
            currentTabFocusRequester = currentTabFocusRequester,
            onContentDown = if (!m3uActive && accounts.isNotEmpty() && state.categories.isNotEmpty()) {
                { behaviorScope.launch { focusSelectedCategory() } }
            } else {
                null
            },
        )

        Spacer(Modifier.height(MediaCatalogDimens.HeaderGap))

        Box(
            modifier = contentTransitionSurfaceModifier
                .fillMaxSize(),
        ) {
        if (m3uActive) {
            CatalogEmpty(
                title = "Series non disponibles en M3U",
                subtitle = "La source M3U active alimente Live TV. Passez sur Xtream pour les series.",
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }

        if (accounts.isEmpty()) {
            XtreamQrSetupPanel(
                activationRepository = container.activationRepository,
                title = "Configurer votre catalogue de series",
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
                SeriesCategoryList(
                    state = state,
                    selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                    currentTabFocusRequester = currentTabFocusRequester,
                    listState = categoryListState,
                    onCategory = { category ->
                        if (inputReady) {
                            pendingFirstSeriesFocusCategoryId = category.id
                            viewModel.selectCategory(category)
                        }
                    },
                    onRight = { behaviorScope.launch { focusSeriesColumn() } },
                    modifier = Modifier
                        .weight(0.24f)
                        .fillMaxHeight(),
                )
                SeriesList(
                    state = state,
                    listState = seriesListState,
                    searchFocusRequester = seriesSearchFocusRequester,
                    firstSeriesFocusRequester = firstSeriesFocusRequester,
                    focusedSeriesFocusRequester = focusedSeriesFocusRequester,
                    returnSeriesFocusRequester = returnSeriesFocusRequester,
                    returnFocusSeriesId = returnFocusSeriesId,
                    focusFirstAfterCategoryId = pendingFirstSeriesFocusCategoryId,
                    onFirstAfterCategoryFocused = { pendingFirstSeriesFocusCategoryId = null },
                    onReturnFocusConsumed = {
                        returnFocusHandled = true
                        onReturnFocusConsumed()
                    },
                    onSearchQueryChange = viewModel::updateContentSearchQuery,
                    onSortSelected = viewModel::setSortMode,
                    onSeriesFocused = viewModel::focusSeries,
                    onRestoreCategoryFocus = { behaviorScope.launch { focusSelectedCategory() } },
                    onRestoreSeriesFocus = { behaviorScope.launch { focusSeriesColumn() } },
                    onEnterPreview = ::enterSeriesPreview,
                    onSeriesClick = { series ->
                        if (inputReady) {
                            container.behaviorReporter.reportAsync(
                                behaviorScope,
                                "CONTENT_OPENED",
                                series.toBehaviorContent(),
                            )
                            val openDetails = viewModel.activateSeries(series)
                            if (openDetails) {
                                onOpenSeriesDetails(series.seriesId)
                            }
                        }
                    },
                    onLoadNextPage = viewModel::loadNextPage,
                    onRetry = viewModel::retryCurrentCategory,
                    seasonsLabel = strings.seasonsLabel,
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                )
                VodPreviewPanel(
                    title = "Preview",
                    content = state.selectedSeries?.toPreviewContent(
                        episode = state.selectedPreviewEpisode,
                        loading = state.episodesLoading,
                    ),
                    playFocusRequester = previewPlayFocusRequester,
                    onPlay = {
                        val series = state.selectedSeries
                        val episode = state.selectedPreviewEpisode
                        if (series != null && episode != null) onWatchEpisode(episode.episodeId, series.seriesId)
                    },
                    onDetails = { state.selectedSeries?.let { onOpenSeriesDetails(it.seriesId) } },
                    onFavorite = { state.selectedSeries?.let(viewModel::toggleFavorite) },
                    onDeleteHistory = { state.selectedSeries?.let { seriesToDelete = it } },
                    showDeleteHistory = state.selectedCategory?.label == "Historique",
                    showFreeAdsPreview = showFreeAdsPreview,
                    idleAdEnabled = state.series.isNotEmpty(),
                    idleVastAdLoader = container.idleVastAdLoader,
                    monetizationManager = container.monetizationManager,
                    premiumPurchaseUrl = premiumPurchaseUrl,
                    tvCode = tvCode,
                    seriesEpisodes = state.episodes.map { episode ->
                        VodPreviewEpisode(
                            id = episode.episodeId,
                            seasonNumber = episode.seasonNumber,
                            episodeNumber = episode.episodeNumber,
                            title = episode.title,
                            durationLabel = episode.duration,
                            progressPercent = episode.progressPercent,
                            resume = episode.episodeId == state.selectedPreviewEpisode?.episodeId && episode.resumePositionMs > 0L,
                        )
                    },
                    selectedSeriesEpisodeId = state.selectedPreviewEpisode?.episodeId,
                    onSeriesEpisodeSelected = viewModel::selectPreviewEpisode,
                    seasonsLabel = strings.seasonsLabel,
                    episodesLoadingLabel = strings.episodesLoading,
                    episodesEmptyLabel = strings.episodesEmpty,
                    resumeLabel = strings.resumePlayback,
                    progressLabel = strings.progressLabel,
                    onNavigateLeft = { behaviorScope.launch { focusSeriesColumn() } },
                    onPreviewBoundsChanged = onPreviewBoundsChanged,
                    modifier = Modifier
                        .weight(0.34f)
                        .fillMaxHeight(),
                )
            }
        }
        }
    }

    seriesToDelete?.let { series ->
        TvConfirmationDialog(
            title = strings.seriesDeleteHistoryTitle,
            itemLabel = series.title,
            message = strings.destructiveActionWarning,
            confirmText = strings.delete,
            cancelText = strings.cancel,
            onDismiss = { seriesToDelete = null },
            onConfirm = {
                seriesToDelete = null
                deletedSeriesIdAwaitingFocus = series.seriesId
                viewModel.deleteHistorySeries(series)
            },
        )
    }
}

private fun SeriesItemUi.toBehaviorContent(): BehaviorContent =
    BehaviorContent(
        contentType = "SERIES",
        contentId = seriesId.toString(),
        title = title,
        categoryLabel = categoryLabel,
        durationSeconds = episodeRunTime?.toLongOrNull()?.times(60),
        engagementScore = 40,
        sourceScreen = "SERIES",
        tags = listOfNotNull(genre, releaseDate?.take(4), rating, if (isFavorite) "favorite" else null),
        context = mapOf(
            "seasons" to (seasonsCount?.toString() ?: "-"),
            "episodes" to (episodesCount?.toString() ?: "-"),
        ),
    )

@Composable
private fun SeriesCategoryList(
    state: SeriesScreenState,
    selectedCategoryFocusRequester: FocusRequester,
    currentTabFocusRequester: FocusRequester,
    listState: LazyListState,
    onCategory: (SeriesCategoryUi) -> Unit,
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
                    icon = seriesCategoryIcon(category.label),
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
private fun SeriesList(
    state: SeriesScreenState,
    listState: LazyListState,
    searchFocusRequester: FocusRequester,
    firstSeriesFocusRequester: FocusRequester,
    focusedSeriesFocusRequester: FocusRequester,
    returnSeriesFocusRequester: FocusRequester,
    returnFocusSeriesId: Int?,
    focusFirstAfterCategoryId: String?,
    onFirstAfterCategoryFocused: () -> Unit,
    onReturnFocusConsumed: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortSelected: (SeriesSortMode) -> Unit,
    onSeriesFocused: (SeriesItemUi) -> Unit,
    onRestoreCategoryFocus: () -> Unit,
    onRestoreSeriesFocus: () -> Unit,
    onEnterPreview: (SeriesItemUi) -> Unit,
    onSeriesClick: (SeriesItemUi) -> Unit,
    onLoadNextPage: () -> Unit,
    onRetry: () -> Unit,
    seasonsLabel: String,
    modifier: Modifier = Modifier,
) {
    val sortFocusRequester = remember { FocusRequester() }

    LaunchedEffect(
        focusFirstAfterCategoryId,
        state.selectedCategoryId,
        state.seriesLoading,
        state.itemsLoading,
        state.series.size,
    ) {
        val categoryId = focusFirstAfterCategoryId ?: return@LaunchedEffect
        if (state.selectedCategoryId != categoryId || state.seriesLoading || state.itemsLoading) return@LaunchedEffect
        if (state.series.isEmpty()) {
            onFirstAfterCategoryFocused()
            return@LaunchedEffect
        }
        listState.scrollToItem(0)
        if (!listState.awaitItemVisible(0)) return@LaunchedEffect
        withFrameNanos { }
        runCatching { firstSeriesFocusRequester.requestFocus() }
        onFirstAfterCategoryFocused()
    }

    LaunchedEffect(returnFocusSeriesId, state.displayedSeries) {
        val seriesId = returnFocusSeriesId ?: return@LaunchedEffect
        val targetIndex = state.displayedSeries.indexOfFirst { it.seriesId == seriesId }
        if (targetIndex < 0) {
            return@LaunchedEffect
        }
        listState.scrollToItem((targetIndex - 2).coerceAtLeast(0))
        if (!listState.awaitItemVisible(targetIndex)) return@LaunchedEffect
        withFrameNanos { }
        runCatching { returnSeriesFocusRequester.requestFocus() }
        onReturnFocusConsumed()
    }

    LaunchedEffect(state.selectedCategoryId, state.contentSearchQuery, state.sortMode) {
        if (returnFocusSeriesId != null) return@LaunchedEffect
        if (listState.layoutInfo.totalItemsCount > 0) listState.scrollToItem(0)
    }
    LaunchedEffect(listState, state.series.size, state.hasMoreItems, state.nextPageLoading, state.contentSearchQuery) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: 0 }
            .collect { lastVisibleIndex ->
                val remaining = state.series.lastIndex - lastVisibleIndex
                if (state.hasMoreItems && !state.nextPageLoading && remaining <= SeriesNextPageThreshold) {
                    onLoadNextPage()
                }
            }
    }

    MediaCatalogPanel(
        title = "Series",
        modifier = modifier,
        titleContent = {
            CatalogPanelTitleWithCount(
                title = "Series",
                count = state.selectedCategory?.count ?: state.displayedSeries.size,
            )
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CatalogSearchField(
                    query = state.contentSearchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = "Serie",
                    modifier = Modifier
                        .focusRequester(searchFocusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionDown -> {
                                    onRestoreSeriesFocus()
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
                    options = SeriesSortMode.entries.map { it.label },
                    selectedIndex = state.sortMode.ordinal,
                    onSelected = { onSortSelected(SeriesSortMode.entries[it]) },
                    focusRequester = sortFocusRequester,
                    leftFocusRequester = searchFocusRequester,
                )
            }
        },
    ) {
        when {
            state.seriesLoading && state.series.isEmpty() -> VodContentListLoadingSkeleton(
                modifier = Modifier.fillMaxSize(),
            )

            state.errorMessage != null && state.series.isEmpty() && !state.categoriesLoading && !state.seriesLoading && !state.itemsLoading && !state.episodesLoading -> CatalogError(
                message = state.errorMessage,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )

            state.series.isEmpty() -> CatalogEmpty(
                title = if (state.contentSearchQuery.isBlank()) "Aucune serie" else "Aucun resultat",
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
                    items = state.displayedSeries,
                    key = { _, series -> series.seriesId },
                ) { index, series ->
                    val itemFocusRequester = remember(series.seriesId) { FocusRequester() }
                    VodContentRow(
                        title = series.title,
                        subtitle = listOfNotNull(
                            series.episodeRunTime?.let { "${it}m/ep" },
                            series.releaseDate?.take(4),
                        ).joinToString(" | ").ifBlank { series.subtitle },
                        genre = series.genre
                            ?.substringBefore('/')
                            ?.substringBefore(',')
                            ?.trim()
                            ?.takeIf(String::isNotEmpty),
                        rating = series.rating,
                        sideLabel = series.listSideLabel(),
                        titleSideLabel = series.seasonsCount?.let { "$it $seasonsLabel" },
                        imageUrl = series.backdropUrl ?: series.coverUrl,
                        fallbackText = series.title.take(2).uppercase(),
                        selected = series.seriesId == state.selectedSeriesId,
                        focusRequester = when {
                            series.seriesId == returnFocusSeriesId -> returnSeriesFocusRequester
                            index == 0 -> firstSeriesFocusRequester
                            series.seriesId == state.focusedSeriesId -> focusedSeriesFocusRequester
                            else -> itemFocusRequester
                        },
                        rightFocusRequester = null,
                        upFocusRequester = sortFocusRequester.takeIf { index == 0 },
                        onLeft = onRestoreCategoryFocus,
                        onRight = { onEnterPreview(series) },
                        onFocused = { onSeriesFocused(series) },
                        onClick = { onSeriesClick(series) },
                        ratingFirst = true,
                    )
                }
            }
        }
    }
}

private fun SeriesItemUi.toPreviewContent(
    episode: SeriesEpisodeUi?,
    loading: Boolean,
): VodPreviewContent =
    VodPreviewContent(
        id = "series-$seriesId-${episode?.episodeId ?: "loading"}",
        title = title,
        subtitle = subtitle,
        imageUrl = backdropUrl,
        backdropUrl = backdropUrl,
        streamUrl = episode?.streamUrl,
        durationLabel = episode?.duration ?: episodeRunTime?.let { "${it}m/ep" },
        sideLabel = sideLabel(),
        year = releaseDate?.take(4),
        rating = rating,
        genre = genre,
        plot = plot ?: episode?.plot,
        creditLabel = createdBy?.let { "Creator: $it" },
        cast = cast,
        isFavorite = isFavorite,
        loading = loading,
    )

private fun SeriesItemUi.sideLabel(): String =
    when {
        seasonsCount != null && episodesCount != null -> "${seasonsCount}S / ${episodesCount}E"
        seasonsCount != null -> "${seasonsCount} saisons"
        episodesCount != null -> "${episodesCount} episodes"
        else -> episodeRunTime?.let { "${it}m/ep" }.orEmpty()
    }

private fun SeriesItemUi.listSideLabel(): String =
    when {
        episodesCount != null -> "${episodesCount}E"
        episodeRunTime != null -> "${episodeRunTime}m/ep"
        else -> ""
    }

private const val SeriesNextPageThreshold = 15

private fun seriesCategoryIcon(label: String): ImageVector? {
    val normalized = label.lowercase()
    return when {
        normalized == "all" -> Icons.Default.Menu
        "favoris" in normalized -> Icons.Default.Favorite
        "histor" in normalized -> Icons.Default.History
        else -> null
    }
}
