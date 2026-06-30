package com.smartvision.svplayer.ui.youtube

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.smartvision.svplayer.R
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
    var youtubeFullScreen by remember { mutableStateOf(false) }
    var completedVideoId by remember { mutableStateOf<String?>(null) }
    var pendingAutoAdvanceFromVideoId by remember { mutableStateOf<String?>(null) }
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
        youtubeFullScreen = false
        playingVideo = null
        viewModel.closePlayerToTrending()
        restoreCategoryFocusAfterPlayer = true
    }

    fun openYoutubeVideo(video: YoutubeVideoUi, sourceScreen: String) {
        completedVideoId = null
        pendingAutoAdvanceFromVideoId = null
        viewModel.openYoutubeVideo(video, sourceScreen)
        playingVideo = video
    }

    fun nextYoutubeVideo(current: YoutubeVideoUi?): YoutubeVideoUi? {
        val currentId = current?.videoId ?: return state.playerSuggestions.firstOrNull()
        return state.playerSuggestions.firstOrNull { it.videoId != currentId }
            ?: state.videos.dropWhile { it.videoId != currentId }.drop(1).firstOrNull()
    }

    fun previousYoutubeVideo(current: YoutubeVideoUi?): YoutubeVideoUi? {
        val currentId = current?.videoId ?: return null
        val index = state.videos.indexOfFirst { it.videoId == currentId }
        return state.videos.getOrNull(index - 1)
    }

    LaunchedEffect(pendingAutoAdvanceFromVideoId, state.playerSuggestions.size, playingVideo?.videoId) {
        val pendingId = pendingAutoAdvanceFromVideoId ?: return@LaunchedEffect
        val currentVideo = playingVideo ?: return@LaunchedEffect
        if (currentVideo.videoId == pendingId) {
            nextYoutubeVideo(currentVideo)?.let { nextVideo ->
                pendingAutoAdvanceFromVideoId = null
                openYoutubeVideo(nextVideo, "AUTOPLAY")
            }
        }
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

    if (youtubeFullScreen && playingVideo != null) {
        YoutubeInlinePlayer(
            video = playingVideo!!,
            strings = strings,
            focusRequester = playerFocusRequester,
            leftFocusRequester = visiblePlayerSuggestionFocusRequester,
            upFocusRequester = searchFocusRequester,
            anomalyReporter = container.anomalyReporter,
            onPlayerBehavior = { eventType, video ->
                if (eventType == "VIDEO_COMPLETED" && video != null && completedVideoId != video.videoId) {
                    completedVideoId = video.videoId
                    viewModel.recordPlayerBehavior(eventType, video)
                    nextYoutubeVideo(video)?.let { nextVideo ->
                        openYoutubeVideo(nextVideo, "AUTOPLAY")
                    } ?: run {
                        pendingAutoAdvanceFromVideoId = video.videoId
                    }
                }
            },
            onPreviousVideo = {
                previousYoutubeVideo(playingVideo)?.let { openYoutubeVideo(it, "PREVIOUS") }
            },
            onNextVideo = {
                nextYoutubeVideo(playingVideo)?.let { openYoutubeVideo(it, "NEXT") }
            },
            onToggleFullScreen = { youtubeFullScreen = false },
            fullScreen = true,
            modifier = Modifier.fillMaxSize(),
        )
        return
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
                        viewModel.recordPlayerBehavior("SUGGESTION_OPENED", video, "SUGGESTION")
                        openYoutubeVideo(video, "SUGGESTION")
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
                    openYoutubeVideo(video, viewModel.currentBehaviorSource())
                },
                playingVideo = playingVideo,
                anomalyReporter = container.anomalyReporter,
                onPlayerBehavior = { eventType, video ->
                    if (eventType == "VIDEO_COMPLETED" && video != null && playingVideo?.videoId == video.videoId && completedVideoId != video.videoId) {
                        completedVideoId = video.videoId
                        viewModel.recordPlayerBehavior(eventType, video)
                        nextYoutubeVideo(video)?.let { nextVideo ->
                            openYoutubeVideo(nextVideo, "AUTOPLAY")
                        } ?: run {
                            pendingAutoAdvanceFromVideoId = video.videoId
                        }
                    }
                },
                onPreviousVideo = {
                    previousYoutubeVideo(playingVideo)?.let { openYoutubeVideo(it, "PREVIOUS") }
                },
                onNextVideo = {
                    nextYoutubeVideo(playingVideo)?.let { openYoutubeVideo(it, "NEXT") }
                },
                onToggleFullScreen = { youtubeFullScreen = true },
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
                    label = category.localizedLabel(strings),
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
                glowColor = focusStyle.accent,
                cornerRadius = 7.dp,
            )
            .onFocusChanged { if (it.isFocused) onFocused() }
            .clip(shape)
            .background(if (focusState.isFocused) focusStyle.accent.copy(alpha = 0.16f) else SmartVisionColors.Surface.copy(alpha = 0.72f))
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
    onPreviousVideo: () -> Unit,
    onNextVideo: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onLoadMore: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val firstSuggestionFocusRequester = remember { FocusRequester() }
    var searchFocused by remember { mutableStateOf(false) }
    var suggestionsFocused by remember { mutableStateOf(false) }
    var suppressSuggestionsAfterSelection by remember { mutableStateOf(false) }
    val showSuggestions = !suppressSuggestionsAfterSelection &&
        (searchFocused || suggestionsFocused) &&
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
                onQueryChange = {
                    suppressSuggestionsAfterSelection = false
                    onSearchQueryChange(it)
                },
                onSubmit = onSearchSubmit,
                onFocusChanged = {
                    searchFocused = it
                    if (it) suppressSuggestionsAfterSelection = false
                },
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
                    onPreviousVideo = onPreviousVideo,
                    onNextVideo = onNextVideo,
                    onToggleFullScreen = onToggleFullScreen,
                    fullScreen = false,
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
                onSuggestionClick = {
                    suppressSuggestionsAfterSelection = true
                    suggestionsFocused = false
                    onSuggestionClick(it)
                },
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
    onPreviousVideo: () -> Unit,
    onNextVideo: () -> Unit,
    onToggleFullScreen: () -> Unit,
    fullScreen: Boolean,
    modifier: Modifier = Modifier,
) {
    val safeVideoId = video.videoId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(32)
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    val playPauseFocusRequester = remember { FocusRequester() }
    var controlsVisible by remember(video.videoId) { mutableStateOf(true) }
    var command by remember(video.videoId) { mutableStateOf<YoutubePlayerCommand?>(null) }
    var commandSerial by remember(video.videoId) { mutableStateOf(0) }
    var isPlaying by remember(video.videoId) { mutableStateOf(false) }
    var controlsHaveFocus by remember(video.videoId) { mutableStateOf(false) }
    val controlsOffset by animateDpAsState(
        targetValue = if (controlsVisible) 0.dp else 132.dp,
        animationSpec = tween(260),
        label = "youtubeControlsOffset",
    )

    fun sendCommand(nextCommand: YoutubePlayerCommand) {
        command = nextCommand
        commandSerial += 1
        controlsVisible = true
    }

    LaunchedEffect(video.videoId) {
        controlsVisible = true
        withFrameNanos { }
        delay(120)
        runCatching { focusRequester.requestFocus() }
        delay(2_000)
        controlsVisible = false
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            withFrameNanos { }
            delay(50)
            runCatching { playPauseFocusRequester.requestFocus() }
        } else if (controlsHaveFocus) {
            controlsHaveFocus = false
            runCatching { focusRequester.requestFocus() }
        }
    }

    fun hideControlsToPlayer() {
        controlsVisible = false
        controlsHaveFocus = false
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
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.DirectionDown -> {
                        controlsVisible = true
                        runCatching { playPauseFocusRequester.requestFocus() }
                        true
                    }
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
                glowColor = focusStyle.accent,
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
                command = command,
                commandSerial = commandSerial,
                anomalyReporter = anomalyReporter,
                onPlaybackStateChanged = { playing ->
                    isPlaying = playing
                    if (playing) controlsVisible = true
                },
                onPlaybackCompleted = { onPlayerBehavior("VIDEO_COMPLETED", video) },
                modifier = Modifier.fillMaxSize(),
            )
            YoutubePlayerTopBar(
                title = video.title,
                meta = video.meta.ifBlank { "YouTube" },
                rightText = video.durationLabel,
                offsetY = -controlsOffset,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = if (fullScreen) 24.dp else 18.dp, end = if (fullScreen) 24.dp else 18.dp, top = if (fullScreen) 18.dp else 14.dp),
            )
            YoutubePlayerControlBar(
                isPlaying = isPlaying,
                fullScreen = fullScreen,
                enabled = controlsVisible,
                focusRequester = playPauseFocusRequester,
                offsetY = controlsOffset,
                playerFocusRequester = focusRequester,
                onFocusChanged = { controlsHaveFocus = it },
                onDismiss = ::hideControlsToPlayer,
                onPrevious = {
                    controlsVisible = true
                    onPreviousVideo()
                },
                onPlayPause = { sendCommand(YoutubePlayerCommand.TogglePlayback) },
                onNext = {
                    controlsVisible = true
                    onNextVideo()
                },
                onToggleFullScreen = {
                    controlsVisible = true
                    onToggleFullScreen()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 30.dp, end = 30.dp, bottom = if (fullScreen) 24.dp else 18.dp),
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
private fun YoutubePlayerTopBar(
    title: String,
    meta: String,
    rightText: String,
    offsetY: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .offset(y = offsetY)
            .clip(YoutubePlayerGlassShape)
            .background(YoutubePlayerGlassBackground)
            .border(BorderStroke(1.dp, YoutubePlayerGlassBorder), YoutubePlayerGlassShape)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.smartvision_logo_wide),
            contentDescription = "SmartVision",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(162.dp)
                .height(40.dp),
        )
        YoutubePlayerSeparator()
        YoutubePlayerBadge("YOUTUBE", YoutubePlayerNeonBlue)
        YoutubePlayerSeparator()
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f),
        )
        YoutubePlayerSeparator()
        Text(
            text = meta,
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 17.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.7f),
        )
        Text(
            text = rightText.ifBlank { "YouTube" },
            color = Color.White.copy(alpha = 0.74f),
            fontSize = 17.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.width(96.dp),
        )
    }
}

@Composable
private fun YoutubePlayerControlBar(
    isPlaying: Boolean,
    fullScreen: Boolean,
    enabled: Boolean,
    focusRequester: FocusRequester,
    offsetY: Dp,
    playerFocusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val previousFocusRequester = remember { FocusRequester() }
    val nextFocusRequester = remember { FocusRequester() }
    val fullScreenFocusRequester = remember { FocusRequester() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(128.dp)
            .offset(y = offsetY)
            .clip(YoutubePlayerGlassShape)
            .background(YoutubePlayerGlassBackground)
            .border(BorderStroke(1.dp, YoutubePlayerGlassBorder), YoutubePlayerGlassShape)
            .padding(horizontal = 28.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(46.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        YoutubeControlButton(
            label = "Previous",
            icon = Icons.Default.SkipPrevious,
            onClick = onPrevious,
            enabled = enabled,
            focusRequester = previousFocusRequester,
            leftFocusRequester = previousFocusRequester,
            rightFocusRequester = focusRequester,
            upFocusRequester = playerFocusRequester,
            onDismiss = onDismiss,
            onFocusChanged = onFocusChanged,
        )
        YoutubeControlButton(
            label = if (isPlaying) "Pause" else "Play",
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            onClick = onPlayPause,
            enabled = enabled,
            focusRequester = focusRequester,
            leftFocusRequester = previousFocusRequester,
            rightFocusRequester = nextFocusRequester,
            upFocusRequester = playerFocusRequester,
            onDismiss = onDismiss,
            onFocusChanged = onFocusChanged,
            primary = true,
        )
        YoutubeControlButton(
            label = "Next",
            icon = Icons.Default.SkipNext,
            onClick = onNext,
            enabled = enabled,
            focusRequester = nextFocusRequester,
            leftFocusRequester = focusRequester,
            rightFocusRequester = fullScreenFocusRequester,
            upFocusRequester = playerFocusRequester,
            onDismiss = onDismiss,
            onFocusChanged = onFocusChanged,
        )
        YoutubeControlButton(
            label = if (fullScreen) "Exit full screen" else "Full screen",
            icon = if (fullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
            onClick = onToggleFullScreen,
            enabled = enabled,
            focusRequester = fullScreenFocusRequester,
            leftFocusRequester = nextFocusRequester,
            rightFocusRequester = fullScreenFocusRequester,
            upFocusRequester = playerFocusRequester,
            onDismiss = onDismiss,
            onFocusChanged = onFocusChanged,
        )
    }
}

@Composable
private fun YoutubeControlButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    focusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    onDismiss: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    primary: Boolean = false,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focusStyle = LocalTvFocusStyle.current
    val circleSize = if (primary) 88.dp else 68.dp
    Column(
        modifier = Modifier
            .width(if (primary) 104.dp else 86.dp)
            .height(if (primary) 116.dp else 98.dp)
            .focusProperties {
                leftFocusRequester?.let { left = it }
                rightFocusRequester?.let { right = it }
                upFocusRequester?.let { up = it }
            }
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp, Key.Back -> {
                        onDismiss()
                        true
                    }
                    Key.DirectionDown -> true
                    else -> false
                }
            }
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                enabled = enabled,
                pressed = pressed,
                focusedScale = if (primary) 1.08f else 1.04f,
                glowColor = YoutubePlayerNeonBlue,
                cornerRadius = 50.dp,
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(enabled = enabled, interactionSource = interactionSource),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .background(
                    when {
                        primary && focusState.isFocused -> YoutubePlayerNeonBlue.copy(alpha = 0.25f)
                        primary -> Color.Black.copy(alpha = 0.34f)
                        focusState.isFocused -> YoutubePlayerNeonBlue.copy(alpha = 0.16f)
                        else -> Color.White.copy(alpha = 0.05f)
                    },
                )
                .border(
                    BorderStroke(
                        if (focusState.isFocused || primary) 2.dp else 1.dp,
                        if (focusState.isFocused || primary) YoutubePlayerNeonBlue else Color.White.copy(alpha = 0.22f),
                    ),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(if (primary) 34.dp else 25.dp),
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = label,
            color = if (focusState.isFocused || primary) Color.White else SmartVisionColors.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun YoutubePlayerBadge(text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.16f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.72f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun YoutubePlayerSeparator() {
    Box(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .width(1.dp)
            .height(28.dp)
            .background(Color.White.copy(alpha = 0.24f)),
    )
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
        cursorBrush = SolidColor(focusStyle.accent),
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
                    focusStyle.background
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
    val focusStyle = LocalTvFocusStyle.current
    Column(
        modifier = modifier
            .width(430.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xF20A111F))
            .border(BorderStroke(1.dp, focusStyle.accent.copy(alpha = 0.58f)), RoundedCornerShape(8.dp))
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
                glowColor = focusStyle.accent,
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
        else -> null
    }

private fun YoutubeCategoryUi.localizedLabel(strings: SmartVisionStrings): String =
    when (id) {
        "history" -> strings.youtubeCategoryHistory
        "trending" -> strings.trending
        "music" -> strings.youtubeCategoryMusic
        "sport" -> strings.youtubeCategorySport
        "gaming" -> strings.youtubeCategoryGaming
        "news" -> strings.youtubeCategoryNews
        "movies" -> strings.movies
        "documentaries" -> strings.youtubeCategoryDocumentaries
        "kids" -> strings.youtubeCategoryKids
        else -> label
    }

private val YoutubePlayerGlassShape = RoundedCornerShape(28.dp)
private val YoutubePlayerGlassBackground = Color(0x66040E20)
private val YoutubePlayerGlassBorder = Color.White.copy(alpha = 0.22f)
private val YoutubePlayerNeonBlue = Color(0xFF0A84FF)

private const val YoutubeVideoCardAspectRatio = 16f / 9f
private const val YoutubeGridColumns = 4
