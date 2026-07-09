package com.smartvision.svplayer.ui.series

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import com.smartvision.svplayer.ui.catalog.CatalogSearchField
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
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun SeriesScreen(
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
    onOpenSeriesDetails: (Int) -> Unit,
    onWatchEpisode: (Int) -> Unit,
    modifier: Modifier = Modifier,
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
    val firstSeriesFocusRequester = remember { FocusRequester() }
    val previewPlayFocusRequester = remember { FocusRequester() }
    val behaviorScope = rememberCoroutineScope()
    val focusScope = rememberCoroutineScope()
    var inputReady by remember { mutableStateOf(false) }
    var seriesToDelete by remember { mutableStateOf<SeriesItemUi?>(null) }
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

    LaunchedEffect(state.categoriesLoading, accounts.isNotEmpty(), m3uActive, state.categories.isNotEmpty()) {
        if (!m3uActive && accounts.isNotEmpty() && !state.categoriesLoading && state.categories.isNotEmpty()) {
            withFrameNanos { }
            delay(120)
            runCatching { selectedCategoryFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(state.selectedSeriesId) {
        if (state.selectedSeriesId != null) {
            withFrameNanos { }
            delay(80)
            runCatching { previewPlayFocusRequester.requestFocus() }
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
        )

        Spacer(Modifier.height(MediaCatalogDimens.HeaderGap))

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
                    onCategory = { category ->
                        if (inputReady) viewModel.selectCategory(category)
                    },
                    modifier = Modifier
                        .weight(0.22f)
                        .fillMaxHeight(),
                )
                SeriesList(
                    state = state,
                    firstSeriesFocusRequester = firstSeriesFocusRequester,
                    rightFocusRequester = previewPlayFocusRequester,
                    onSearchQueryChange = viewModel::updateContentSearchQuery,
                    onSeriesFocused = viewModel::focusSeries,
                    onSeriesClick = { series ->
                        if (inputReady) {
                            container.behaviorReporter.reportAsync(
                                behaviorScope,
                                "CONTENT_OPENED",
                                series.toBehaviorContent(),
                            )
                            val episodeId = viewModel.activateSeries(series)
                            if (episodeId != null) {
                                onWatchEpisode(episodeId)
                            } else {
                                focusScope.launch {
                                    withFrameNanos { }
                                    delay(80)
                                    runCatching { previewPlayFocusRequester.requestFocus() }
                                }
                            }
                        }
                    },
                    onLoadNextPage = viewModel::loadNextPage,
                    onRetry = viewModel::retryCurrentCategory,
                    modifier = Modifier
                        .weight(0.44f)
                        .fillMaxHeight(),
                )
                VodPreviewPanel(
                    title = "Preview",
                    content = state.selectedSeries?.toPreviewContent(
                        episode = state.selectedPreviewEpisode,
                        loading = state.episodesLoading,
                    ),
                    playFocusRequester = previewPlayFocusRequester,
                    onPlay = { state.selectedPreviewEpisode?.let { onWatchEpisode(it.episodeId) } },
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
                    modifier = Modifier
                        .weight(0.34f)
                        .fillMaxHeight(),
                )
            }
        }
    }

    seriesToDelete?.let { series ->
        ConfirmHistoryDeleteDialog(
            title = "Supprimer cette serie de l'historique ?",
            itemName = series.title,
            onDismiss = { seriesToDelete = null },
            onConfirm = {
                seriesToDelete = null
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
    onCategory: (SeriesCategoryUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusCategoryId = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.id
        ?: state.categories.firstOrNull()?.id

    MediaCatalogPanel(
        title = "Categories",
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(MediaCatalogDimens.ListGap),
            contentPadding = PaddingValues(bottom = MediaCatalogDimens.ListGap),
        ) {
            itemsIndexed(
                state.categories,
                key = { _, category -> category.id },
            ) { _, category ->
                CatalogCategoryRow(
                    label = category.label,
                    count = category.count,
                    icon = seriesCategoryIcon(category.label),
                    selected = category.id == state.selectedCategoryId,
                    focusRequester = if (category.id == focusCategoryId) selectedCategoryFocusRequester else null,
                    onClick = { onCategory(category) },
                )
            }
        }
    }
}

@Composable
private fun SeriesList(
    state: SeriesScreenState,
    firstSeriesFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onSeriesFocused: (SeriesItemUi) -> Unit,
    onSeriesClick: (SeriesItemUi) -> Unit,
    onLoadNextPage: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.selectedCategoryId, state.contentSearchQuery) {
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
        title = state.selectedCategory?.label ?: "Series",
        modifier = modifier,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.contentSearchQuery.isBlank()) {
                        state.selectedCategory?.count?.let { "$it series" } ?: "${state.series.size} series"
                    } else {
                        "${state.series.size} resultats"
                    },
                    color = SmartVisionColors.TextSecondary,
                    style = CatalogMetaStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(10.dp))
                CatalogSearchField(
                    query = state.contentSearchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = "Serie",
                    modifier = Modifier.width(190.dp),
                )
            }
        },
    ) {
        when {
            state.seriesLoading && state.series.isEmpty() -> VodContentListLoadingSkeleton(
                modifier = Modifier.fillMaxSize(),
            )

            state.errorMessage != null && state.series.isEmpty() -> CatalogError(
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
                    items = state.series,
                    key = { _, series -> series.seriesId },
                ) { index, series ->
                    val itemFocusRequester = remember(series.seriesId) { FocusRequester() }
                    VodContentRow(
                        title = series.title,
                        subtitle = series.subtitle,
                        genre = series.genre,
                        rating = series.rating,
                        sideLabel = series.sideLabel(),
                        imageUrl = series.backdropUrl ?: series.coverUrl,
                        fallbackText = series.title.take(2).uppercase(),
                        selected = series.seriesId == state.selectedSeriesId,
                        focusRequester = if (index == 0) firstSeriesFocusRequester else itemFocusRequester,
                        rightFocusRequester = rightFocusRequester,
                        onFocused = { onSeriesFocused(series) },
                        onClick = { onSeriesClick(series) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmHistoryDeleteDialog(
    title: String,
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .background(Color(0xFF0A1425), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, SmartVisionColors.Error.copy(alpha = 0.78f)), RoundedCornerShape(8.dp))
                .padding(22.dp),
        ) {
            Text(title, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS)
            Spacer(Modifier.height(10.dp))
            Text(itemName, color = SmartVisionColors.TextSecondary, style = SmartVisionType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TvButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier.height(42.dp),
                )
                Spacer(Modifier.width(10.dp))
                TvButton(
                    text = "Supprimer",
                    onClick = onConfirm,
                    leadingIcon = Icons.Default.Delete,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier.height(42.dp),
                )
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
        imageUrl = coverUrl,
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
