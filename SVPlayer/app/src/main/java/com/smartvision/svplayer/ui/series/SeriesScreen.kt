package com.smartvision.svplayer.ui.series

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.behavior.BehaviorContent
import com.smartvision.svplayer.ui.activation.XtreamQrSetupPanel
import com.smartvision.svplayer.ui.catalog.CatalogCategoryRow
import com.smartvision.svplayer.ui.catalog.CatalogEmpty
import com.smartvision.svplayer.ui.catalog.CatalogError
import com.smartvision.svplayer.ui.catalog.CatalogLoading
import com.smartvision.svplayer.ui.catalog.CatalogMediaCard
import com.smartvision.svplayer.ui.catalog.CatalogMetaStyle
import com.smartvision.svplayer.ui.catalog.CatalogSearchField
import com.smartvision.svplayer.ui.catalog.MediaCatalogDimens
import com.smartvision.svplayer.ui.catalog.MediaCatalogHeader
import com.smartvision.svplayer.ui.catalog.MediaCatalogPanel
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay

@Suppress("UNUSED_PARAMETER")
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
                userContentRepository = container.userContentRepository,
                settingsRepository = container.settingsRepository,
            )
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by container.accountManager.accounts.collectAsStateWithLifecycle()
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    val firstSeriesFocusRequester = remember { FocusRequester() }
    val behaviorScope = rememberCoroutineScope()
    var inputReady by remember { mutableStateOf(false) }
    var categorySearchQuery by remember { mutableStateOf("") }
    var contentSearchQuery by remember { mutableStateOf("") }
    var seriesToDelete by remember { mutableStateOf<SeriesItemUi?>(null) }

    LaunchedEffect(Unit) {
        delay(260)
        inputReady = true
    }

    val selectedCategoryVisible = state.categories.any { category ->
        category.id == state.selectedCategoryId &&
            (categorySearchQuery.isBlank() || category.label.contains(categorySearchQuery, ignoreCase = true))
    }
    val categoryFocusTargetAvailable = selectedCategoryVisible || state.categories.any { category ->
        categorySearchQuery.isBlank() || category.label.contains(categorySearchQuery, ignoreCase = true)
    }

    LaunchedEffect(state.categoriesLoading, accounts.isNotEmpty(), categoryFocusTargetAvailable) {
        if (accounts.isNotEmpty() && !state.categoriesLoading && categoryFocusTargetAvailable) {
            withFrameNanos { }
            delay(120)
            runCatching { selectedCategoryFocusRequester.requestFocus() }
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

        if (accounts.isEmpty()) {
            XtreamQrSetupPanel(
                activationRepository = container.activationRepository,
                title = "Configurer votre catalogue de séries",
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
                    searchQuery = categorySearchQuery,
                    onSearchQueryChange = { categorySearchQuery = it },
                    contentSearchQuery = contentSearchQuery,
                    onCategory = { category ->
                        if (inputReady) {
                            container.behaviorReporter.reportAsync(
                                behaviorScope,
                                "CATEGORY_OPENED",
                                BehaviorContent(
                                    contentType = "SERIES",
                                    contentId = category.id,
                                    title = category.label,
                                    categoryId = category.id,
                                    categoryLabel = category.label,
                                    sourceScreen = "SERIES",
                                    engagementScore = 30,
                                ),
                            )
                            viewModel.selectCategory(category)
                        }
                    },
                    modifier = Modifier
                        .weight(0.22f)
                        .fillMaxHeight(),
                )
                SeriesGrid(
                    state = state,
                    firstSeriesFocusRequester = firstSeriesFocusRequester,
                    selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                    searchQuery = contentSearchQuery,
                    onSearchQueryChange = { contentSearchQuery = it },
                    onSeriesFocused = viewModel::focusSeries,
                    onSeriesClick = { series ->
                        if (inputReady) {
                            container.behaviorReporter.reportAsync(
                                behaviorScope,
                                "CONTENT_OPENED",
                                series.toBehaviorContent(),
                            )
                            viewModel.focusSeries(series)
                            onOpenSeriesDetails(series.seriesId)
                        }
                    },
                    showHistoryDelete = state.selectedCategory?.label == "Historique",
                    onDeleteHistorySeries = { series -> seriesToDelete = series },
                    onRetry = viewModel::retryCurrentCategory,
                    modifier = Modifier
                        .weight(0.78f)
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
    firstSeriesFocusRequester: FocusRequester,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    contentSearchQuery: String,
    onCategory: (SeriesCategoryUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleCategories = state.categories.filter {
        searchQuery.isBlank() || it.label.contains(searchQuery, ignoreCase = true)
    }
    val focusCategoryId = visibleCategories.firstOrNull { it.id == state.selectedCategoryId }?.id
        ?: visibleCategories.firstOrNull()?.id

    MediaCatalogPanel(
        title = "Categories",
        modifier = modifier,
        trailing = {
            CatalogSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Dossier",
                modifier = Modifier.width(118.dp),
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(MediaCatalogDimens.ListGap),
            contentPadding = PaddingValues(bottom = MediaCatalogDimens.ListGap),
        ) {
            items(
                visibleCategories,
                key = { it.id },
            ) { category ->
                CatalogCategoryRow(
                    label = category.label,
                    count = category.count,
                    icon = seriesCategoryIcon(category.label),
                    selected = category.id == state.selectedCategoryId,
                    focusRequester = if (category.id == focusCategoryId) {
                        selectedCategoryFocusRequester
                    } else {
                        null
                    },
                    rightFocusRequester = firstSeriesFocusRequester.takeIf {
                        !state.seriesLoading && state.series.any {
                            contentSearchQuery.isBlank() || it.title.contains(contentSearchQuery, ignoreCase = true)
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
    onSearchQueryChange: (String) -> Unit,
    onSeriesFocused: (SeriesItemUi) -> Unit,
    onSeriesClick: (SeriesItemUi) -> Unit,
    showHistoryDelete: Boolean,
    onDeleteHistorySeries: (SeriesItemUi) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val visibleSeries = state.series.filter { series ->
        searchQuery.isBlank() || series.title.contains(searchQuery, ignoreCase = true)
    }

    LaunchedEffect(state.selectedCategoryId) {
        if (gridState.layoutInfo.totalItemsCount > 0) gridState.scrollToItem(0)
    }

    MediaCatalogPanel(
        title = state.selectedCategory?.label ?: "Series",
        modifier = modifier,
        trailing = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
                Spacer(Modifier.width(10.dp))
                CatalogSearchField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = "Serie",
                    modifier = Modifier.width(190.dp),
                )
            }
        },
    ) {
        when {
            state.seriesLoading && visibleSeries.isEmpty() -> CatalogLoading(
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
                    val itemFocusRequester = remember(series.seriesId) { FocusRequester() }
                    val cardFocusRequester = if (index == 0) firstSeriesFocusRequester else itemFocusRequester
                    val deleteFocusRequester = remember(series.seriesId) { FocusRequester() }
                    Box {
                        CatalogMediaCard(
                            title = series.title,
                            meta = seriesCardMeta(series),
                            imageUrl = series.coverUrl,
                            fallbackText = series.title.take(2).uppercase(),
                            selected = series.seriesId == state.selectedSeriesId,
                            favorite = series.isFavorite,
                            focusRequester = cardFocusRequester,
                            leftFocusRequester = selectedCategoryFocusRequester.takeIf {
                                index % MediaCatalogDimens.MediaGridColumns == 0
                            },
                            rightFocusRequester = deleteFocusRequester.takeIf { showHistoryDelete },
                            onFocused = { onSeriesFocused(series) },
                            onClick = { onSeriesClick(series) },
                        )
                        if (showHistoryDelete) {
                            HistoryDeleteButton(
                                focusRequester = deleteFocusRequester,
                                leftFocusRequester = cardFocusRequester,
                                onClick = { onDeleteHistorySeries(series) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(7.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryDeleteButton(
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = modifier
            .size(36.dp)
            .focusProperties { left = leftFocusRequester }
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.08f,
                glowColor = SmartVisionColors.Error,
                cornerRadius = 7.dp,
            )
            .clip(shape)
            .background(if (focusState.isFocused) SmartVisionColors.Error.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.56f))
            .border(
                BorderStroke(
                    if (focusState.isFocused) 2.dp else 1.dp,
                    if (focusState.isFocused) SmartVisionColors.Error else Color.White.copy(alpha = 0.30f),
                ),
                shape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Supprimer",
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
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

private fun seriesCardMeta(series: SeriesItemUi): String =
    listOfNotNull(
        series.releaseDate?.take(4),
        series.rating?.let { "$it/10" },
        series.seasonsCount?.let { "$it S" },
        series.episodesCount?.let { "$it ep" },
    ).joinToString("  |  ").ifBlank { series.categoryLabel }

private fun seriesCategoryIcon(label: String): ImageVector? {
    val normalized = label.lowercase()
    return when {
        normalized == "all" -> Icons.Default.Menu
        "favoris" in normalized -> Icons.Default.Favorite
        "histor" in normalized -> Icons.Default.History
        else -> null
    }
}
