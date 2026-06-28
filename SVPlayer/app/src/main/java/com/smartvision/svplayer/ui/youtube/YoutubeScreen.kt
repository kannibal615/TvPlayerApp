package com.smartvision.svplayer.ui.youtube

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.anomaly.AnomalyReporter
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.ui.catalog.CatalogCategoryRow
import com.smartvision.svplayer.ui.catalog.CatalogEmpty
import com.smartvision.svplayer.ui.catalog.CatalogError
import com.smartvision.svplayer.ui.catalog.CatalogLoading
import com.smartvision.svplayer.ui.catalog.CatalogMetaStyle
import com.smartvision.svplayer.ui.catalog.MediaCatalogDimens
import com.smartvision.svplayer.ui.catalog.MediaCatalogHeader
import com.smartvision.svplayer.ui.catalog.MediaCatalogPanel
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.i18n.smartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun YoutubeScreen(
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
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: YoutubeViewModel = viewModel(
        factory = viewModelFactory {
            YoutubeViewModel(container.youtubeRepository)
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playerSettings by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = PlayerSettings(),
    )
    val strings = smartVisionStrings(playerSettings.language)
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    val firstVideoFocusRequester = remember { FocusRequester() }
    val playerFocusRequester = remember { FocusRequester() }
    val firstPlayerSuggestionFocusRequester = remember { FocusRequester() }
    val visiblePlayerSuggestionFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    var playingVideo by remember { mutableStateOf<YoutubeVideoUi?>(null) }
    var restoreVideoFocusAfterPlayer by remember { mutableStateOf(false) }
    var restoreCategoryFocusAfterPlayer by remember { mutableStateOf(false) }
    val contentFocusRequester = when {
        playingVideo != null -> playerFocusRequester
        state.videos.isNotEmpty() -> firstVideoFocusRequester
        else -> null
    }

    LaunchedEffect(state.categories.isNotEmpty(), playingVideo?.videoId) {
        if (state.categories.isNotEmpty() && playingVideo == null) {
            withFrameNanos { }
            delay(120)
            runCatching { selectedCategoryFocusRequester.requestFocus() }
        }
    }

    BackHandler(enabled = playingVideo != null) {
        playingVideo = null
        viewModel.closePlayerToTrending()
        restoreCategoryFocusAfterPlayer = true
    }

    LaunchedEffect(restoreVideoFocusAfterPlayer, state.videos.size) {
        if (restoreVideoFocusAfterPlayer && playingVideo == null && state.videos.isNotEmpty()) {
            withFrameNanos { }
            delay(80)
            runCatching { firstVideoFocusRequester.requestFocus() }
            restoreVideoFocusAfterPlayer = false
        }
    }

    LaunchedEffect(restoreCategoryFocusAfterPlayer, state.selectedCategoryId, playingVideo) {
        if (restoreCategoryFocusAfterPlayer && playingVideo == null && state.selectedCategoryId == "trending") {
            withFrameNanos { }
            delay(120)
            runCatching { selectedCategoryFocusRequester.requestFocus() }
            restoreCategoryFocusAfterPlayer = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3A1020).copy(alpha = 0.42f),
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

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(MediaCatalogDimens.PanelGap),
        ) {
            if (playingVideo != null) {
                YoutubePlayerSuggestionsList(
                    state = state,
                    strings = strings,
                    firstSuggestionFocusRequester = firstPlayerSuggestionFocusRequester,
                    visibleSuggestionFocusRequester = visiblePlayerSuggestionFocusRequester,
                    contentFocusRequester = playerFocusRequester,
                    onSuggestionFocused = viewModel::focusVideo,
                    onSuggestionClick = { video ->
                        viewModel.recordPlayerBehavior("SUGGESTION_OPENED", video)
                        viewModel.openYoutubeVideo(video)
                        playingVideo = video
                    },
                    onLoadMoreSuggestions = viewModel::loadMorePlayerSuggestionsIfNeeded,
                    modifier = Modifier
                        .weight(0.22f)
                        .fillMaxHeight(),
                )
            } else {
                YoutubeCategoryList(
                    state = state,
                    strings = strings,
                    selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                    contentFocusRequester = contentFocusRequester,
                    onCategory = viewModel::selectCategory,
                    modifier = Modifier
                        .weight(0.22f)
                        .fillMaxHeight(),
                )
            }
                YoutubeVideoGrid(
                    state = state,
                    strings = strings,
                firstVideoFocusRequester = firstVideoFocusRequester,
                playerFocusRequester = playerFocusRequester,
                playerSuggestionFocusRequester = visiblePlayerSuggestionFocusRequester,
                searchFocusRequester = searchFocusRequester,
                selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onSearchSubmit = { viewModel.submitSearch() },
                onSuggestionClick = viewModel::selectSuggestion,
                onVideoFocused = viewModel::focusVideo,
                onVideoClick = { video ->
                    viewModel.openYoutubeVideo(video)
                    playingVideo = video
                },
                playingVideo = playingVideo,
                anomalyReporter = container.anomalyReporter,
                onPlayerBehavior = { eventType, video ->
                    viewModel.recordPlayerBehavior(eventType, video)
                    if (eventType == "VIDEO_COMPLETED" && video != null && playingVideo?.videoId == video.videoId) {
                        state.playerSuggestions.firstOrNull { it.videoId != video.videoId }?.let { nextVideo ->
                            viewModel.openYoutubeVideo(nextVideo)
                            playingVideo = nextVideo
                        }
                    }
                },
                onLoadMore = viewModel::loadMoreIfNeeded,
                onRetry = viewModel::retry,
                modifier = Modifier
                    .weight(0.78f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun YoutubeCategoryList(
    state: YoutubeScreenState,
    strings: SmartVisionStrings,
    selectedCategoryFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester?,
    onCategory: (YoutubeCategoryUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    MediaCatalogPanel(
        title = strings.youtubeCategories,
        modifier = modifier,
        trailing = { YoutubeLogoMark() },
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
                    icon = youtubeCategoryIcon(category.id),
                    selected = category.id == state.selectedCategoryId,
                    focusRequester = if (category.id == state.selectedCategoryId) {
                        selectedCategoryFocusRequester
                    } else {
                        null
                    },
                    rightFocusRequester = contentFocusRequester,
                    onClick = { onCategory(category) },
                )
            }
        }
    }
}

@Composable
private fun YoutubePlayerSuggestionsList(
    state: YoutubeScreenState,
    strings: SmartVisionStrings,
    firstSuggestionFocusRequester: FocusRequester,
    visibleSuggestionFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester,
    onSuggestionFocused: (YoutubeVideoUi) -> Unit,
    onSuggestionClick: (YoutubeVideoUi) -> Unit,
    onLoadMoreSuggestions: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var firstVisibleIndex by remember { mutableStateOf(0) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { onLoadMoreSuggestions(it) }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { firstVisibleIndex = it }
    }
    MediaCatalogPanel(
        title = strings.youtubeSuggestions,
        modifier = modifier,
        trailing = { YoutubeLogoMark() },
    ) {
        when {
            state.suggestionsLoading && state.playerSuggestions.isEmpty() -> CatalogLoading(
                title = strings.youtubeSuggestions,
                modifier = Modifier.fillMaxSize(),
            )

            state.playerSuggestions.isEmpty() -> CatalogEmpty(
                title = strings.youtubeNoVideo,
                subtitle = strings.youtubeNoVideoSubtitle,
                modifier = Modifier.fillMaxSize(),
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MediaCatalogDimens.ListGap),
                contentPadding = PaddingValues(bottom = MediaCatalogDimens.ListGap),
            ) {
                itemsIndexed(state.playerSuggestions, key = { _, video -> video.videoId }) { index, video ->
                    YoutubePlayerSuggestionRow(
                        video = video,
                        focusRequester = when {
                            index == firstVisibleIndex -> visibleSuggestionFocusRequester
                            index == 0 -> firstSuggestionFocusRequester
                            else -> null
                        },
                        rightFocusRequester = contentFocusRequester,
                        isFirst = index == 0,
                        isLast = index == state.playerSuggestions.lastIndex,
                        onFocused = {
                            onSuggestionFocused(video)
                            if (index >= state.playerSuggestions.size - 4) onLoadMoreSuggestions(index)
                        },
                        onClick = { onSuggestionClick(video) },
                    )
                }
            }
        }
    }
}

@Composable
private fun YoutubePlayerSuggestionRow(
    video: YoutubeVideoUi,
    focusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester,
    isFirst: Boolean,
    isLast: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focusStyle = LocalTvFocusStyle.current
    val shape = RoundedCornerShape(7.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                (event.key == Key.DirectionUp && isFirst) || (event.key == Key.DirectionDown && isLast)
            }
            .focusProperties { right = rightFocusRequester }
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.025f,
                glowColor = SmartVisionColors.CyanAccent,
                cornerRadius = 7.dp,
            )
            .onFocusChanged { if (it.isFocused) onFocused() }
            .clip(shape)
            .background(if (focusState.isFocused) SmartVisionColors.CyanAccent.copy(alpha = 0.16f) else SmartVisionColors.Surface.copy(alpha = 0.72f))
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else 1.dp,
                    if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border,
                ),
                shape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(78.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(5.dp))
                .background(Color.Black),
        ) {
            if (!video.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
            if (video.durationLabel.isNotBlank()) {
                Text(
                    text = video.durationLabel,
                    color = Color.White,
                    style = CatalogMetaStyle,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color.Black.copy(alpha = 0.74f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                color = SmartVisionColors.TextPrimary,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = video.meta,
                color = SmartVisionColors.TextSecondary,
                style = CatalogMetaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun YoutubeVideoGrid(
    state: YoutubeScreenState,
    strings: SmartVisionStrings,
    firstVideoFocusRequester: FocusRequester,
    playerFocusRequester: FocusRequester,
    playerSuggestionFocusRequester: FocusRequester,
    searchFocusRequester: FocusRequester,
    selectedCategoryFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onVideoFocused: (YoutubeVideoUi) -> Unit,
    onVideoClick: (YoutubeVideoUi) -> Unit,
    playingVideo: YoutubeVideoUi?,
    anomalyReporter: AnomalyReporter,
    onPlayerBehavior: (String, YoutubeVideoUi?) -> Unit,
    onLoadMore: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val firstSuggestionFocusRequester = remember { FocusRequester() }
    var searchFocused by remember { mutableStateOf(false) }
    var suggestionsFocused by remember { mutableStateOf(false) }
    val showSuggestions = (searchFocused || suggestionsFocused || state.searchQuery.isNotBlank()) &&
        state.searchSuggestions.isNotEmpty()
    val isVideoGridVisible = playingVideo == null && state.videos.isNotEmpty()
    val contentDownFocusRequester = when {
        playingVideo != null -> playerFocusRequester
        isVideoGridVisible -> firstVideoFocusRequester
        else -> null
    }

    LaunchedEffect(state.selectedCategoryId) {
        if (gridState.layoutInfo.totalItemsCount > 0) gridState.scrollToItem(0)
    }

    LaunchedEffect(gridState, state.videos.size, state.nextPageToken, playingVideo?.videoId) {
        if (playingVideo != null) return@LaunchedEffect
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: 0 }
            .distinctUntilChanged()
            .collect { onLoadMore(it) }
    }

    MediaCatalogPanel(
        title = "",
        modifier = modifier,
        titleContent = {
            YoutubeSearchInput(
                query = state.searchQuery,
                placeholder = strings.youtubeSearchPlaceholder,
                focusRequester = searchFocusRequester,
                downFocusRequester = when {
                    showSuggestions -> firstSuggestionFocusRequester
                    else -> contentDownFocusRequester
                },
                onQueryChange = onSearchQueryChange,
                onSubmit = onSearchSubmit,
                onFocusChanged = { searchFocused = it },
                onSelected = { onSearchQueryChange(state.searchQuery) },
                modifier = Modifier.width(430.dp),
            )
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.videos.isNotEmpty()) strings.youtubeVideosLoaded.format(state.videos.size) else "YouTube",
                    color = SmartVisionColors.TextSecondary,
                    style = CatalogMetaStyle,
                    maxLines = 1,
                )
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.loading && state.videos.isEmpty() -> CatalogLoading(
                    title = strings.youtubeLoading,
                    modifier = Modifier.fillMaxSize(),
                )

                state.errorMessage != null && state.videos.isEmpty() -> CatalogError(
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize(),
                )

                state.videos.isEmpty() -> CatalogEmpty(
                    title = strings.youtubeNoVideo,
                    subtitle = strings.youtubeNoVideoSubtitle,
                    modifier = Modifier.fillMaxSize(),
                )

                playingVideo != null -> YoutubeInlinePlayer(
                    video = playingVideo,
                    strings = strings,
                    focusRequester = playerFocusRequester,
                    leftFocusRequester = playerSuggestionFocusRequester,
                    upFocusRequester = searchFocusRequester,
                    anomalyReporter = anomalyReporter,
                    onPlayerBehavior = onPlayerBehavior,
                    modifier = Modifier.fillMaxSize(),
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(YoutubeGridColumns),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(MediaCatalogDimens.MediaGridGap),
                    verticalArrangement = Arrangement.spacedBy(MediaCatalogDimens.MediaGridGap),
                    contentPadding = PaddingValues(bottom = MediaCatalogDimens.MediaGridGap),
                ) {
                    itemsIndexed(state.videos, key = { _, video -> video.videoId }) { index, video ->
                        YoutubeVideoCard(
                            video = video,
                            selected = video.videoId == state.selectedVideoId,
                            focusRequester = firstVideoFocusRequester.takeIf { index == 0 },
                            leftFocusRequester = selectedCategoryFocusRequester.takeIf {
                                index % YoutubeGridColumns == 0
                            },
                            upFocusRequester = searchFocusRequester.takeIf {
                                index < YoutubeGridColumns
                            },
                            onFocused = { onVideoFocused(video) },
                            onClick = { onVideoClick(video) },
                        )
                    }
                }
            }

            YoutubeSuggestionsDropdown(
                suggestions = if (showSuggestions) state.searchSuggestions else emptyList(),
                firstSuggestionFocusRequester = firstSuggestionFocusRequester,
                upFocusRequester = searchFocusRequester,
                downFocusRequester = contentDownFocusRequester,
                onSuggestionClick = onSuggestionClick,
                onSuggestionFocusChanged = { suggestionsFocused = it },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .zIndex(8f),
            )

            if (state.loadingMore) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.68f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = strings.youtubeLoadingMore,
                        color = SmartVisionColors.TextPrimary,
                        style = CatalogMetaStyle,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun YoutubeInlinePlayer(
    video: YoutubeVideoUi,
    strings: SmartVisionStrings,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    anomalyReporter: AnomalyReporter,
    onPlayerBehavior: (String, YoutubeVideoUi?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeVideoId = video.videoId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(32)
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current

    LaunchedEffect(video.videoId) {
        withFrameNanos { }
        delay(120)
        runCatching { focusRequester.requestFocus() }
    }

    Box(
        modifier = modifier
            .focusProperties {
                left = leftFocusRequester
                up = upFocusRequester
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        runCatching { leftFocusRequester.requestFocus() }
                        true
                    }
                    Key.DirectionUp -> {
                        runCatching { upFocusRequester.requestFocus() }
                        true
                    }
                    else -> false
                }
            }
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                focusedScale = 1.006f,
                glowColor = SmartVisionColors.CyanAccent,
                cornerRadius = MediaCatalogDimens.ItemRadius,
            )
            .clip(RoundedCornerShape(MediaCatalogDimens.ItemRadius))
            .background(Color.Black)
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else 1.dp,
                    if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border,
                ),
                RoundedCornerShape(MediaCatalogDimens.ItemRadius),
            )
            .focusable(),
    ) {
        if (safeVideoId.isNotBlank()) {
            YoutubeWebPlayer(
                videoId = safeVideoId,
                mode = YoutubePlaybackMode.Fullscreen,
                anomalyReporter = anomalyReporter,
                onPlayerReady = { onPlayerBehavior("PLAYER_READY", video) },
                onPlaybackCompleted = { onPlayerBehavior("VIDEO_COMPLETED", video) },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = strings.youtubeVideoUnavailable,
                color = SmartVisionColors.TextPrimary,
                style = CatalogMetaStyle.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun YoutubeSearchInput(
    query: String,
    placeholder: String,
    focusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    onSelected: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val fallbackFocusRequester = remember { FocusRequester() }
    val inputFocusRequester = focusRequester ?: fallbackFocusRequester
    var focused by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val focusStyle = LocalTvFocusStyle.current
    val shape = RoundedCornerShape(7.dp)
    val borderColor by animateColorAsState(
        targetValue = if (focused || editing) focusStyle.accent else SmartVisionColors.Border,
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "youtubeSearchBorder",
    )

    LaunchedEffect(editing) {
        if (editing) {
            runCatching { inputFocusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        readOnly = !editing,
        cursorBrush = SolidColor(SmartVisionColors.CyanAccent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
                editing = false
                onSubmit()
            },
            onDone = {
                keyboardController?.hide()
                editing = false
                onSubmit()
            },
        ),
        textStyle = CatalogMetaStyle.copy(color = SmartVisionColors.TextPrimary),
        modifier = modifier
            .height(34.dp)
            .focusRequester(inputFocusRequester)
            .focusProperties {
                if (downFocusRequester != null) down = downFocusRequester
            }
            .onFocusChanged {
                focused = it.isFocused
                onFocusChanged(it.isFocused)
                if (it.isFocused) {
                    onSelected()
                }
                if (!it.isFocused) {
                    editing = false
                    keyboardController?.hide()
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (editing) {
                            keyboardController?.hide()
                            editing = false
                            onSubmit()
                        } else {
                            editing = true
                            keyboardController?.show()
                        }
                        true
                    }
                    Key.Back -> {
                        if (editing) {
                            editing = false
                            keyboardController?.hide()
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            }
            .clip(shape)
            .background(
                if (focused || editing) {
                    focusStyle.accent.copy(alpha = 0.10f)
                } else {
                    SmartVisionColors.Surface.copy(alpha = 0.86f)
                },
            )
            .border(BorderStroke(if (focused) focusStyle.borderWidth else 1.dp, borderColor), shape)
            .padding(horizontal = 10.dp),
        decorationBox = { inner ->
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (focused || editing) focusStyle.accent else SmartVisionColors.TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(7.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isBlank()) {
                        Text(
                            text = placeholder,
                            color = SmartVisionColors.TextSecondary,
                            style = CatalogMetaStyle,
                            maxLines = 1,
                        )
                    }
                    inner()
                }
            }
        },
    )
}

@Composable
private fun YoutubeSuggestionsDropdown(
    suggestions: List<String>,
    firstSuggestionFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester?,
    onSuggestionClick: (String) -> Unit,
    onSuggestionFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty()) return
    val visibleSuggestions = suggestions.take(5)
    Column(
        modifier = modifier
            .width(430.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xF20A111F))
            .border(BorderStroke(1.dp, SmartVisionColors.CyanAccent.copy(alpha = 0.58f)), RoundedCornerShape(8.dp))
            .padding(vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        visibleSuggestions.forEachIndexed { index, suggestion ->
            YoutubeSuggestionRow(
                text = suggestion,
                focusRequester = firstSuggestionFocusRequester.takeIf { index == 0 },
                upFocusRequester = upFocusRequester.takeIf { index == 0 },
                downFocusRequester = downFocusRequester.takeIf { index == visibleSuggestions.lastIndex },
                onClick = { onSuggestionClick(suggestion) },
                onSuggestionFocusChanged = onSuggestionFocusChanged,
            )
        }
    }
}

@Composable
private fun YoutubeSuggestionRow(
    text: String,
    focusRequester: FocusRequester?,
    upFocusRequester: FocusRequester?,
    downFocusRequester: FocusRequester?,
    onClick: () -> Unit,
    onSuggestionFocusChanged: (Boolean) -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focusStyle = LocalTvFocusStyle.current
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .focusProperties {
                if (upFocusRequester != null) up = upFocusRequester
                if (downFocusRequester != null) down = downFocusRequester
            }
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.02f,
                glowColor = SmartVisionColors.CyanAccent,
                cornerRadius = 6.dp,
            )
            .onFocusChanged { onSuggestionFocusChanged(it.isFocused) }
            .clip(shape)
            .background(if (focusState.isFocused) focusStyle.accent.copy(alpha = 0.22f) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
                    tint = if (focusState.isFocused) focusStyle.accent else SmartVisionColors.TextSecondary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = SmartVisionColors.TextPrimary,
            style = CatalogMetaStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun YoutubeVideoCard(
    video: YoutubeVideoUi,
    selected: Boolean,
    focusRequester: FocusRequester?,
    leftFocusRequester: FocusRequester?,
    upFocusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val active = selected || focusState.isFocused
    val shape = RoundedCornerShape(MediaCatalogDimens.ItemRadius)
    val focusStyle = LocalTvFocusStyle.current
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> focusStyle.accent
            selected -> Color(0xFFFF3B3B)
            else -> SmartVisionColors.Border.copy(alpha = 0.72f)
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "youtubeVideoBorder",
    )

    Box(
        modifier = modifier
            .aspectRatio(YoutubeVideoCardAspectRatio)
            .then(
                if (leftFocusRequester != null) {
                    Modifier.focusProperties {
                        left = leftFocusRequester
                        if (upFocusRequester != null) up = upFocusRequester
                    }
                } else if (upFocusRequester != null) {
                    Modifier.focusProperties { up = upFocusRequester }
                } else {
                    Modifier
                },
            )
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.045f,
                glowColor = Color(0xFFFF3B3B),
                cornerRadius = MediaCatalogDimens.ItemRadius,
            )
            .onFocusChanged { focus -> if (focus.isFocused) onFocused() }
            .zIndex(if (focusState.isFocused) 3f else 0f)
            .clip(shape)
            .background(SmartVisionColors.SurfaceElevated)
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else SmartVisionDimensions.PanelBorder,
                    borderColor,
                ),
                shape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
    ) {
        if (!video.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF3B3B).copy(alpha = 0.34f),
                                SmartVisionColors.SurfaceElevated,
                                SmartVisionColors.Background,
                            ),
                            radius = 420f,
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                YoutubeLogoMark(modifier = Modifier.size(width = 54.dp, height = 36.dp))
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.02f),
                            Color.Black.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.94f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
        ) {
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = video.meta,
                color = Color.White.copy(alpha = 0.72f),
                style = CatalogMetaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (video.durationLabel.isNotBlank()) {
            Text(
                text = video.durationLabel,
                color = Color.White,
                style = CatalogMetaStyle.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(34.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = if (active) 0.58f else 0.34f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun YoutubeLogoMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 32.dp, height = 22.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFFFF0033)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun youtubeCategoryIcon(id: String): ImageVector? =
    when (id) {
        "history" -> Icons.Default.Menu
        "trending" -> Icons.Default.PlayArrow
        "movies" -> Icons.Default.Menu
        else -> null
    }

private const val YoutubeVideoCardAspectRatio = 16f / 9f
private const val YoutubeGridColumns = 4
