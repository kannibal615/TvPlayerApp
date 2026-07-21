package com.smartvision.svplayer.ui.youtube

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.smartvision.svplayer.ui.components.TvConfirmationDialog
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
    val currentTabFocusRequester = remember { FocusRequester() }
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    val categoryListState = rememberLazyListState()
    val firstVideoFocusRequester = remember { FocusRequester() }
    val playerFocusRequester = remember { FocusRequester() }
    val firstPlayerSuggestionFocusRequester = remember { FocusRequester() }
    val visiblePlayerSuggestionFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    val miniPlayerBackFocusRequester = remember { FocusRequester() }
    val favoriteFocusRequester = remember { FocusRequester() }
    val youtubeSettingsFocusRequester = remember { FocusRequester() }
    var playingVideo by remember { mutableStateOf<YoutubeVideoUi?>(null) }
    var youtubeFullScreen by remember { mutableStateOf(false) }
    var youtubeResumeSeconds by remember { mutableStateOf(0.0) }
    var showYoutubeSettings by remember { mutableStateOf(false) }
    var pendingYoutubeClear by remember { mutableStateOf<YoutubeClearTarget?>(null) }
    var completedVideoId by remember { mutableStateOf<String?>(null) }
    var pendingAutoAdvanceFromVideoId by remember { mutableStateOf<String?>(null) }
    var restoreVideoFocusId by remember { mutableStateOf<String?>(null) }
    val contentFocusRequester = when {
        playingVideo != null -> playerFocusRequester
        state.videos.isNotEmpty() -> firstVideoFocusRequester
        else -> null
    }

    LaunchedEffect(state.categories.size, state.selectedCategoryId, playingVideo?.videoId, restoreVideoFocusId) {
        if (playingVideo == null && state.categories.isNotEmpty() && restoreVideoFocusId == null) {
            val selectedIndex = state.categories.indexOfFirst { it.id == state.selectedCategoryId }
                .coerceAtLeast(0)
            categoryListState.scrollToItem(selectedIndex)
            withFrameNanos { }
            delay(120)
            runCatching { selectedCategoryFocusRequester.requestFocus() }
        } else if (playingVideo == null) {
            withFrameNanos { }
            delay(80)
            runCatching { currentTabFocusRequester.requestFocus() }
        }
    }

    BackHandler(enabled = playingVideo != null) {
        if (youtubeFullScreen) {
            youtubeFullScreen = false
            runCatching { playerFocusRequester.requestFocus() }
        } else {
            restoreVideoFocusId = playingVideo?.videoId
            youtubeFullScreen = false
            playingVideo = null
            youtubeResumeSeconds = 0.0
            viewModel.closePlayer()
        }
    }

    fun openYoutubeVideo(video: YoutubeVideoUi, sourceScreen: String, refreshSuggestions: Boolean = true) {
        completedVideoId = null
        pendingAutoAdvanceFromVideoId = null
        youtubeResumeSeconds = 0.0
        viewModel.openYoutubeVideo(video, sourceScreen, refreshSuggestions)
        playingVideo = video
    }

    fun nextYoutubeVideo(current: YoutubeVideoUi?): YoutubeVideoUi? {
        val currentId = current?.videoId ?: return state.playerSuggestions.firstOrNull()
        return state.playerSuggestions.firstOrNull { it.videoId != currentId }
            ?: state.videos.dropWhile { it.videoId != currentId }.drop(1).firstOrNull()
    }

    fun previousYoutubeVideo(current: YoutubeVideoUi?): YoutubeVideoUi? {
        val currentId = current?.videoId ?: return null
        val historyIndex = state.recentVideos.indexOfFirst { it.videoId == currentId }
        state.recentVideos.getOrNull(historyIndex + 1)?.let { return it }
        val index = state.videos.indexOfFirst { it.videoId == currentId }
        return state.videos.getOrNull(index - 1)
    }

    fun openPreviousYoutubeVideo() {
        val current = playingVideo ?: return
        previousYoutubeVideo(current)?.let {
            openYoutubeVideo(it, "PREVIOUS")
            return
        }
        viewModel.previousFromHistory(current.videoId) { previous ->
            previous?.let { openYoutubeVideo(it, "PREVIOUS") }
        }
    }

    fun closeMiniPlayer() {
        restoreVideoFocusId = playingVideo?.videoId
        youtubeFullScreen = false
        playingVideo = null
        youtubeResumeSeconds = 0.0
        viewModel.closePlayer()
    }

    LaunchedEffect(pendingAutoAdvanceFromVideoId, state.playerSuggestions.size, playingVideo?.videoId) {
        val pendingId = pendingAutoAdvanceFromVideoId ?: return@LaunchedEffect
        val currentVideo = playingVideo ?: return@LaunchedEffect
        if (state.youtubeAutoplayEnabled && currentVideo.videoId == pendingId) {
            nextYoutubeVideo(currentVideo)?.let { nextVideo ->
                pendingAutoAdvanceFromVideoId = null
                openYoutubeVideo(nextVideo, "AUTOPLAY", refreshSuggestions = false)
            }
        }
    }

    LaunchedEffect(state.youtubeAutoplayEnabled, playingVideo?.videoId, state.playerSuggestions.size) {
        if (state.youtubeAutoplayEnabled && playingVideo != null && state.playerSuggestions.size < YoutubeAutoplayQueueTarget) {
            viewModel.ensureAutoplayQueue(playingVideo)
        }
    }

    if (youtubeFullScreen && playingVideo != null) {
        YoutubeInlinePlayer(
            video = playingVideo!!,
            strings = strings,
            focusRequester = playerFocusRequester,
            leftFocusRequester = visiblePlayerSuggestionFocusRequester,
            rightFocusRequester = favoriteFocusRequester,
            upFocusRequester = miniPlayerBackFocusRequester,
            anomalyReporter = container.anomalyReporter,
            initialStartSeconds = youtubeResumeSeconds,
            onProgress = { position, _ -> youtubeResumeSeconds = position },
            onPlayerBehavior = { eventType, video ->
                if (eventType == "VIDEO_COMPLETED" && video != null && completedVideoId != video.videoId) {
                    completedVideoId = video.videoId
                    viewModel.recordPlayerBehavior(eventType, video)
                    if (state.youtubeAutoplayEnabled) {
                        nextYoutubeVideo(video)?.let { nextVideo ->
                            openYoutubeVideo(nextVideo, "AUTOPLAY", refreshSuggestions = false)
                        } ?: run {
                            pendingAutoAdvanceFromVideoId = video.videoId
                        }
                    } else {
                        pendingAutoAdvanceFromVideoId = null
                    }
                }
            },
            onPreviousVideo = {
                openPreviousYoutubeVideo()
            },
            onReloadVideo = {
                youtubeResumeSeconds = 0.0
            },
            onNextVideo = {
                nextYoutubeVideo(playingVideo)?.let { openYoutubeVideo(it, "NEXT", refreshSuggestions = false) }
            },
            onToggleFullScreen = { youtubeFullScreen = false },
            onBackToInline = {
                youtubeFullScreen = false
            },
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
            currentTabFocusRequester = currentTabFocusRequester,
            contentDownFocusRequester = selectedCategoryFocusRequester,
            onContentDown = { runCatching { selectedCategoryFocusRequester.requestFocus() } },
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
                    playingVideo = playingVideo,
                    firstSuggestionFocusRequester = firstPlayerSuggestionFocusRequester,
                    visibleSuggestionFocusRequester = visiblePlayerSuggestionFocusRequester,
                    contentFocusRequester = playerFocusRequester,
                    upFocusRequester = miniPlayerBackFocusRequester,
                    onSuggestionFocused = viewModel::focusVideo,
                    onSuggestionClick = { video ->
                        viewModel.recordPlayerBehavior("SUGGESTION_OPENED", video, "SUGGESTION")
                        openYoutubeVideo(video, "SUGGESTION", refreshSuggestions = false)
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
                    headerFocusRequester = currentTabFocusRequester,
                    listState = categoryListState,
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
                restoreVideoFocusId = restoreVideoFocusId,
                onRestoreVideoFocusConsumed = { restoreVideoFocusId = null },
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
                favoriteVideo = playingVideo ?: state.videos.firstOrNull { it.videoId == state.focusedVideoId },
                favoriteVideoIds = state.favoriteVideoIds,
                miniPlayerBackFocusRequester = miniPlayerBackFocusRequester,
                favoriteFocusRequester = favoriteFocusRequester,
                youtubeSettingsFocusRequester = youtubeSettingsFocusRequester,
                onCloseMiniPlayer = ::closeMiniPlayer,
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenYoutubeSettings = { showYoutubeSettings = true },
                anomalyReporter = container.anomalyReporter,
                initialStartSeconds = youtubeResumeSeconds,
                onPlayerProgress = { position, _ -> youtubeResumeSeconds = position },
                onPlayerBehavior = { eventType, video ->
                    if (eventType == "VIDEO_COMPLETED" && video != null && playingVideo?.videoId == video.videoId && completedVideoId != video.videoId) {
                        completedVideoId = video.videoId
                        viewModel.recordPlayerBehavior(eventType, video)
                        if (state.youtubeAutoplayEnabled) {
                            nextYoutubeVideo(video)?.let { nextVideo ->
                                openYoutubeVideo(nextVideo, "AUTOPLAY", refreshSuggestions = false)
                            } ?: run {
                                pendingAutoAdvanceFromVideoId = video.videoId
                            }
                        } else {
                            pendingAutoAdvanceFromVideoId = null
                        }
                    }
                },
                onPreviousVideo = {
                    openPreviousYoutubeVideo()
                },
                onReloadVideo = {
                    youtubeResumeSeconds = 0.0
                },
                onNextVideo = {
                    nextYoutubeVideo(playingVideo)?.let { openYoutubeVideo(it, "NEXT", refreshSuggestions = false) }
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

    if (showYoutubeSettings) {
        YoutubeSettingsDialog(
            strings = strings,
            autoplayEnabled = state.youtubeAutoplayEnabled,
            queuedSuggestions = state.playerSuggestions.size,
            watchedCount = state.recentVideos.size,
            favoritesCount = state.favoriteVideoIds.size,
            searchCount = state.recentSearches.size,
            onToggleAutoplay = { viewModel.setYoutubeAutoplayEnabled(!state.youtubeAutoplayEnabled) },
            onClearSearchHistory = {
                showYoutubeSettings = false
                pendingYoutubeClear = YoutubeClearTarget.SearchHistory
            },
            onClearVideoHistory = {
                showYoutubeSettings = false
                pendingYoutubeClear = YoutubeClearTarget.WatchHistory
            },
            onClearFavorites = {
                showYoutubeSettings = false
                pendingYoutubeClear = YoutubeClearTarget.Favorites
            },
            onDismiss = { showYoutubeSettings = false },
        )
    }

    pendingYoutubeClear?.let { target ->
        TvConfirmationDialog(
            title = target.title(strings),
            message = strings.destructiveActionWarning,
            confirmText = strings.delete,
            cancelText = strings.cancel,
            onDismiss = {
                pendingYoutubeClear = null
                showYoutubeSettings = true
            },
            onConfirm = {
                when (target) {
                    YoutubeClearTarget.SearchHistory -> viewModel.clearSearchHistory()
                    YoutubeClearTarget.WatchHistory -> viewModel.clearVideoHistory()
                    YoutubeClearTarget.Favorites -> viewModel.clearYoutubeFavorites()
                }
                pendingYoutubeClear = null
                showYoutubeSettings = true
            },
        )
    }
}

private enum class YoutubeClearTarget {
    SearchHistory,
    WatchHistory,
    Favorites;

    fun title(strings: SmartVisionStrings): String = when (this) {
        SearchHistory -> strings.youtubeClearSearchHistory
        WatchHistory -> strings.youtubeClearWatchHistory
        Favorites -> strings.youtubeClearFavorites
    }
}

@Composable
private fun YoutubeCategoryList(
    state: YoutubeScreenState,
    strings: SmartVisionStrings,
    selectedCategoryFocusRequester: FocusRequester,
    headerFocusRequester: FocusRequester,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onCategory: (YoutubeCategoryUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    MediaCatalogPanel(
        title = strings.youtubeCategories,
        modifier = modifier,
        trailing = { YoutubeLogoMark() },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(MediaCatalogDimens.ListGap),
            contentPadding = PaddingValues(bottom = MediaCatalogDimens.ListGap),
        ) {
            itemsIndexed(state.categories, key = { _, category -> category.id }) { index, category ->
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
                    upFocusRequester = headerFocusRequester.takeIf { index == 0 },
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
    playingVideo: YoutubeVideoUi?,
    firstSuggestionFocusRequester: FocusRequester,
    visibleSuggestionFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    onSuggestionFocused: (YoutubeVideoUi) -> Unit,
    onSuggestionClick: (YoutubeVideoUi) -> Unit,
    onLoadMoreSuggestions: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var firstVisibleIndex by remember { mutableStateOf(0) }
    val displaySuggestions = remember(playingVideo?.videoId, state.playerSuggestions) {
        listOfNotNull(playingVideo) + state.playerSuggestions.filter { it.videoId != playingVideo?.videoId }
    }
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

            displaySuggestions.isEmpty() -> CatalogEmpty(
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
                itemsIndexed(displaySuggestions, key = { _, video -> video.videoId }) { index, video ->
                    YoutubePlayerSuggestionRow(
                        video = video,
                        focusRequester = when {
                            index == firstVisibleIndex -> visibleSuggestionFocusRequester
                            index == 0 -> firstSuggestionFocusRequester
                            else -> null
                        },
                        rightFocusRequester = contentFocusRequester,
                        upFocusRequester = upFocusRequester.takeIf { index == 0 },
                        isCurrent = video.videoId == playingVideo?.videoId,
                        isFirst = index == 0,
                        isLast = index == displaySuggestions.lastIndex,
                        onFocused = {
                            onSuggestionFocused(video)
                            if (index >= displaySuggestions.size - 4) onLoadMoreSuggestions(index)
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
    upFocusRequester: FocusRequester?,
    isCurrent: Boolean,
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
                event.key == Key.DirectionDown && isLast
            }
            .focusProperties {
                right = rightFocusRequester
                if (upFocusRequester != null) up = upFocusRequester
            }
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
            if (isCurrent) {
                YoutubeNowPlayingBars(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.Black.copy(alpha = 0.56f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
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
    restoreVideoFocusId: String?,
    onRestoreVideoFocusConsumed: () -> Unit,
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
    favoriteVideo: YoutubeVideoUi?,
    favoriteVideoIds: Set<String>,
    miniPlayerBackFocusRequester: FocusRequester,
    favoriteFocusRequester: FocusRequester,
    youtubeSettingsFocusRequester: FocusRequester,
    onCloseMiniPlayer: () -> Unit,
    onToggleFavorite: (YoutubeVideoUi) -> Unit,
    onOpenYoutubeSettings: () -> Unit,
    anomalyReporter: AnomalyReporter,
    initialStartSeconds: Double,
    onPlayerProgress: (Double, Double) -> Unit,
    onPlayerBehavior: (String, YoutubeVideoUi?) -> Unit,
    onPreviousVideo: () -> Unit,
    onReloadVideo: () -> Unit,
    onNextVideo: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onLoadMore: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val videoFocusRequesters = remember(state.videos.map { it.videoId }) {
        state.videos.mapIndexed { index, video ->
            video.videoId to if (index == 0) firstVideoFocusRequester else FocusRequester()
        }.toMap()
    }
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

    LaunchedEffect(restoreVideoFocusId, state.videos.map { it.videoId }, playingVideo) {
        if (playingVideo != null) return@LaunchedEffect
        val videoId = restoreVideoFocusId ?: return@LaunchedEffect
        val index = state.videos.indexOfFirst { it.videoId == videoId }
        val requester = videoFocusRequesters[videoId]
        if (index < 0 || requester == null) return@LaunchedEffect
        gridState.scrollToItem(index)
        withFrameNanos { }
        delay(100)
        runCatching { requester.requestFocus() }
        onRestoreVideoFocusConsumed()
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (playingVideo != null) {
                    YoutubeHeaderIconButton(
                        icon = Icons.Default.ArrowBack,
                        contentDescription = strings.back,
                        focusRequester = miniPlayerBackFocusRequester,
                        rightFocusRequester = searchFocusRequester,
                        downFocusRequester = playerFocusRequester,
                        onClick = onCloseMiniPlayer,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                if (playingVideo != null) {
                    Spacer(Modifier.weight(0.7f))
                }
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
                Spacer(Modifier.weight(1f))
                YoutubeFavoriteButton(
                    selected = favoriteVideo?.videoId?.let { it in favoriteVideoIds } == true,
                    enabled = favoriteVideo != null,
                    focusRequester = favoriteFocusRequester,
                    leftFocusRequester = if (playingVideo != null) playerFocusRequester else searchFocusRequester,
                    rightFocusRequester = youtubeSettingsFocusRequester,
                    label = strings.youtubeFavorite,
                    onClick = { favoriteVideo?.let(onToggleFavorite) },
                )
                Spacer(Modifier.width(8.dp))
                YoutubeHeaderIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = strings.youtubeSettings,
                    focusRequester = youtubeSettingsFocusRequester,
                    leftFocusRequester = favoriteFocusRequester,
                    downFocusRequester = contentDownFocusRequester,
                    onClick = onOpenYoutubeSettings,
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
                    rightFocusRequester = favoriteFocusRequester,
                    upFocusRequester = miniPlayerBackFocusRequester,
                    anomalyReporter = anomalyReporter,
                    initialStartSeconds = initialStartSeconds,
                    onProgress = onPlayerProgress,
                    onPlayerBehavior = onPlayerBehavior,
                    onPreviousVideo = onPreviousVideo,
                    onReloadVideo = onReloadVideo,
                    onNextVideo = onNextVideo,
                    onToggleFullScreen = onToggleFullScreen,
                    onBackToInline = {},
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
                            focusRequester = videoFocusRequesters[video.videoId],
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
    rightFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    anomalyReporter: AnomalyReporter,
    initialStartSeconds: Double,
    onProgress: (Double, Double) -> Unit,
    onPlayerBehavior: (String, YoutubeVideoUi?) -> Unit,
    onPreviousVideo: () -> Unit,
    onReloadVideo: () -> Unit,
    onNextVideo: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onBackToInline: () -> Unit,
    fullScreen: Boolean,
    modifier: Modifier = Modifier,
) {
    val safeVideoId = video.videoId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(32)
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    val playPauseFocusRequester = remember { FocusRequester() }
    var controlsVisible by remember(video.videoId, fullScreen) { mutableStateOf(false) }
    var command by remember(video.videoId) { mutableStateOf<YoutubePlayerCommand?>(null) }
    var commandSerial by remember(video.videoId) { mutableStateOf(0) }
    var isPlaying by remember(video.videoId) { mutableStateOf(false) }
    var hasStarted by remember(video.videoId) { mutableStateOf(false) }
    var controlsHaveFocus by remember(video.videoId) { mutableStateOf(false) }
    var playerContainerHasFocus by remember(video.videoId) { mutableStateOf(false) }
    var progressPosition by remember(video.videoId) { mutableStateOf(initialStartSeconds) }
    var progressDuration by remember(video.videoId) { mutableStateOf(video.durationSeconds?.toDouble() ?: 0.0) }
    var controlsActivitySerial by remember(video.videoId, fullScreen) { mutableStateOf(0) }
    var backButtonVisible by remember(video.videoId, fullScreen) { mutableStateOf(false) }
    val controlsOffset by animateDpAsState(
        targetValue = if (controlsVisible) 0.dp else 150.dp,
        animationSpec = tween(260),
        label = "youtubeControlsOffset",
    )

    fun showControls() {
        controlsVisible = true
        if (fullScreen) backButtonVisible = true
        controlsActivitySerial += 1
    }

    fun showBackButtonOnly() {
        if (fullScreen) {
            backButtonVisible = true
            controlsActivitySerial += 1
        }
    }

    fun sendCommand(nextCommand: YoutubePlayerCommand) {
        command = nextCommand
        commandSerial += 1
        showControls()
    }

    LaunchedEffect(video.videoId) {
        withFrameNanos { }
        delay(120)
        runCatching { focusRequester.requestFocus() }
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            withFrameNanos { }
            delay(50)
            runCatching { playPauseFocusRequester.requestFocus() }
        } else if (!backButtonVisible && (controlsHaveFocus || !playerContainerHasFocus)) {
            controlsHaveFocus = false
            runCatching { focusRequester.requestFocus() }
        }
    }

    LaunchedEffect(backButtonVisible, controlsVisible, fullScreen) {
        if (fullScreen && backButtonVisible && !controlsVisible) {
            withFrameNanos { }
            delay(50)
            runCatching { upFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(controlsVisible, backButtonVisible, controlsActivitySerial) {
        if (!controlsVisible && !backButtonVisible) return@LaunchedEffect
        delay(7_000)
        controlsVisible = false
        backButtonVisible = false
    }

    fun hideControlsToPlayer() {
        controlsVisible = false
        backButtonVisible = false
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
                if (!playerContainerHasFocus) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.DirectionDown -> {
                        showControls()
                        runCatching { playPauseFocusRequester.requestFocus() }
                        true
                    }
                    Key.DirectionLeft -> {
                        runCatching { leftFocusRequester.requestFocus() }
                        true
                    }
                    Key.DirectionRight -> {
                        runCatching { rightFocusRequester.requestFocus() }
                        true
                    }
                    Key.DirectionUp -> {
                        if (fullScreen) {
                            showBackButtonOnly()
                            runCatching { upFocusRequester.requestFocus() }
                        } else {
                            runCatching { upFocusRequester.requestFocus() }
                        }
                        true
                    }
                    else -> false
                }
            }
            .onFocusChanged { playerContainerHasFocus = it.isFocused }
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
                keyboardControlsEnabled = false,
                initialStartSeconds = initialStartSeconds,
                anomalyReporter = anomalyReporter,
                onPlaybackStateChanged = { playing ->
                    isPlaying = playing
                    if (playing && !hasStarted) {
                        hasStarted = true
                        showControls()
                    }
                },
                onPlaybackProgress = { position, duration ->
                    progressPosition = position
                    progressDuration = duration.takeIf { it > 0.0 } ?: video.durationSeconds?.toDouble() ?: 0.0
                    onProgress(position, progressDuration)
                },
                onPlaybackCompleted = { onPlayerBehavior("VIDEO_COMPLETED", video) },
                modifier = Modifier.fillMaxSize(),
            )
            if (fullScreen && (controlsVisible || backButtonVisible)) {
                YoutubeHeaderIconButton(
                    icon = Icons.Default.ArrowBack,
                    contentDescription = strings.back,
                    focusRequester = upFocusRequester,
                    downFocusRequester = focusRequester,
                    onClick = onBackToInline,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 28.dp, top = 24.dp),
                )
            }
            YoutubePlayerControlBar(
                strings = strings,
                isPlaying = isPlaying,
                fullScreen = fullScreen,
                enabled = controlsVisible,
                progressPosition = progressPosition,
                progressDuration = progressDuration,
                focusRequester = playPauseFocusRequester,
                offsetY = controlsOffset,
                playerFocusRequester = focusRequester,
                onFocusChanged = { controlsHaveFocus = it },
                onUserActivity = ::showControls,
                onDismiss = ::hideControlsToPlayer,
                onPrevious = {
                    showControls()
                    onPreviousVideo()
                },
                onReload = {
                    progressPosition = 0.0
                    onReloadVideo()
                    sendCommand(YoutubePlayerCommand.Reload)
                },
                onPlayPause = {
                    isPlaying = !isPlaying
                    sendCommand(YoutubePlayerCommand.TogglePlayback)
                },
                onNext = {
                    showControls()
                    onNextVideo()
                },
                onToggleFullScreen = {
                    showControls()
                    onToggleFullScreen()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 30.dp, end = 30.dp, bottom = if (fullScreen) 24.dp else 14.dp),
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
            .background(YoutubePlayerNeonBlue.copy(alpha = 0.10f), YoutubePlayerGlassShape)
            .border(BorderStroke(2.dp, YoutubePlayerGlassGlowBorder), YoutubePlayerGlassShape)
            .padding(1.dp)
            .clip(YoutubePlayerGlassShape)
            .background(YoutubePlayerGlassBackground)
            .border(BorderStroke(2.dp, YoutubePlayerGlassBorder), YoutubePlayerGlassShape)
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
    strings: SmartVisionStrings,
    isPlaying: Boolean,
    fullScreen: Boolean,
    enabled: Boolean,
    progressPosition: Double,
    progressDuration: Double,
    focusRequester: FocusRequester,
    offsetY: Dp,
    playerFocusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onUserActivity: () -> Unit,
    onDismiss: () -> Unit,
    onPrevious: () -> Unit,
    onReload: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reloadFocusRequester = remember { FocusRequester() }
    val previousFocusRequester = remember { FocusRequester() }
    val nextFocusRequester = remember { FocusRequester() }
    val fullScreenFocusRequester = remember { FocusRequester() }

    fun requestPlayFocus() {
        runCatching { focusRequester.requestFocus() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = offsetY)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                onUserActivity()
                when (event.key) {
                    Key.DirectionUp, Key.Back -> {
                        onDismiss()
                        true
                    }
                    Key.DirectionDown -> true
                    else -> false
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        YoutubeProgressBar(
            position = progressPosition,
            duration = progressDuration,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        )
        Spacer(Modifier.height(7.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(YoutubePlayerNeonBlue.copy(alpha = 0.08f), YoutubePlayerGlassShape)
                .border(BorderStroke(2.dp, YoutubePlayerGlassGlowBorder), YoutubePlayerGlassShape)
                .padding(1.dp)
                .clip(YoutubePlayerGlassShape)
                .background(YoutubePlayerGlassBackground)
                .border(BorderStroke(2.dp, YoutubePlayerGlassBorder), YoutubePlayerGlassShape)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(34.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            YoutubeControlButton(
                label = strings.refresh,
                icon = Icons.Default.Refresh,
                onClick = onReload,
                enabled = enabled,
                focusRequester = reloadFocusRequester,
                leftFocusRequester = reloadFocusRequester,
                rightFocusRequester = previousFocusRequester,
                upFocusRequester = playerFocusRequester,
                onLeft = { runCatching { reloadFocusRequester.requestFocus() } },
                onRight = { runCatching { previousFocusRequester.requestFocus() } },
                onDismiss = onDismiss,
                onUserActivity = onUserActivity,
                onFocusChanged = onFocusChanged,
            )
            YoutubeControlButton(
                label = "Previous",
                icon = Icons.Default.SkipPrevious,
                onClick = onPrevious,
                enabled = enabled,
                focusRequester = previousFocusRequester,
                leftFocusRequester = reloadFocusRequester,
                rightFocusRequester = focusRequester,
                upFocusRequester = playerFocusRequester,
                onLeft = { runCatching { reloadFocusRequester.requestFocus() } },
                onRight = { requestPlayFocus() },
                onDismiss = onDismiss,
                onUserActivity = onUserActivity,
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
                onLeft = { runCatching { previousFocusRequester.requestFocus() } },
                onRight = { runCatching { nextFocusRequester.requestFocus() } },
                onDismiss = onDismiss,
                onUserActivity = onUserActivity,
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
                onLeft = { requestPlayFocus() },
                onRight = { runCatching { fullScreenFocusRequester.requestFocus() } },
                onDismiss = onDismiss,
                onUserActivity = onUserActivity,
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
                onLeft = { runCatching { nextFocusRequester.requestFocus() } },
                onRight = { runCatching { fullScreenFocusRequester.requestFocus() } },
                onDismiss = onDismiss,
                onUserActivity = onUserActivity,
                onFocusChanged = onFocusChanged,
            )
        }
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
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onDismiss: () -> Unit,
    onUserActivity: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    primary: Boolean = false,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focusStyle = LocalTvFocusStyle.current
    val circleSize = if (primary) 58.dp else 46.dp
    val outerSize = circleSize + if (primary) 12.dp else 10.dp
    Column(
        modifier = Modifier
            .width(if (primary) 74.dp else 58.dp)
            .height(if (primary) 68.dp else 56.dp)
            .focusProperties {
                leftFocusRequester?.let { left = it }
                rightFocusRequester?.let { right = it }
                upFocusRequester?.let { up = it }
            }
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                onUserActivity()
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (enabled) onClick()
                        true
                    }
                    Key.DirectionLeft -> {
                        onLeft()
                        true
                    }
                    Key.DirectionRight -> {
                        onRight()
                        true
                    }
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
                glowColor = focusStyle.accent,
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
                .size(outerSize)
                .background(
                    if (focusState.isFocused) focusStyle.background else Color.Transparent,
                    CircleShape,
                )
                .padding(if (focusState.isFocused) 7.dp else 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(
                        when {
                            primary && focusState.isFocused -> focusStyle.background.copy(alpha = 0.92f)
                            primary -> YoutubePlayerButtonBackground.copy(alpha = 0.80f)
                            focusState.isFocused -> focusStyle.background.copy(alpha = 0.82f)
                            else -> YoutubePlayerButtonBackground.copy(alpha = 0.72f)
                        },
                    )
                    .border(
                        BorderStroke(
                            if (focusState.isFocused) focusStyle.borderWidth else 1.5.dp,
                            if (focusState.isFocused) focusStyle.accent else Color.White.copy(alpha = 0.30f),
                        ),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(if (primary) 28.dp else 21.dp),
                )
            }
        }
    }
}

@Composable
private fun YoutubeProgressBar(
    position: Double,
    duration: Double,
    modifier: Modifier = Modifier,
) {
    val fraction = if (duration > 0.0) {
        (position / duration).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }
    Row(
        modifier = modifier.height(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = formatPlaybackClock(position),
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(42.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.22f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(YoutubePlayerNeonBlue),
            )
        }
        Text(
            text = formatPlaybackClock(duration),
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(42.dp),
        )
    }
}

@Composable
private fun YoutubeHeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    focusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focusStyle = LocalTvFocusStyle.current
    Box(
        modifier = modifier
            .size(34.dp)
            .focusProperties {
                if (leftFocusRequester != null) left = leftFocusRequester
                if (rightFocusRequester != null) right = rightFocusRequester
                if (downFocusRequester != null) down = downFocusRequester
            }
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.05f,
                glowColor = focusStyle.accent,
                cornerRadius = 50.dp,
            )
            .clip(CircleShape)
            .background(if (focusState.isFocused) focusStyle.background else SmartVisionColors.Surface.copy(alpha = 0.82f))
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else 1.dp,
                    if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border,
                ),
                CircleShape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun YoutubeFavoriteButton(
    selected: Boolean,
    enabled: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester? = null,
    label: String,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focusStyle = LocalTvFocusStyle.current
    Box(
        modifier = Modifier
            .size(34.dp)
            .focusProperties {
                left = leftFocusRequester
                if (rightFocusRequester != null) right = rightFocusRequester
            }
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                enabled = enabled,
                pressed = pressed,
                focusedScale = 1.05f,
                glowColor = focusStyle.accent,
                cornerRadius = 50.dp,
            )
            .clip(CircleShape)
            .background(if (focusState.isFocused) focusStyle.background else SmartVisionColors.Surface.copy(alpha = 0.82f))
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else 1.dp,
                    if (focusState.isFocused) focusStyle.accent else Color.White.copy(alpha = 0.56f),
                ),
                CircleShape,
            )
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(enabled = enabled, interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (selected) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = label,
            tint = if (selected) Color(0xFFFF3B5F) else Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun YoutubeSettingsDialog(
    strings: SmartVisionStrings,
    autoplayEnabled: Boolean,
    queuedSuggestions: Int,
    watchedCount: Int,
    favoritesCount: Int,
    searchCount: Int,
    onToggleAutoplay: () -> Unit,
    onClearSearchHistory: () -> Unit,
    onClearVideoHistory: () -> Unit,
    onClearFavorites: () -> Unit,
    onDismiss: () -> Unit,
) {
    val autoplayFocusRequester = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LaunchedEffect(Unit) {
            withFrameNanos { }
            delay(80)
            runCatching { autoplayFocusRequester.requestFocus() }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.44f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(620.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xF20A1323))
                    .border(BorderStroke(1.5.dp, YoutubePlayerGlassBorder), RoundedCornerShape(18.dp))
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = YoutubePlayerNeonBlue,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = strings.youtubeSettings,
                        color = SmartVisionColors.TextPrimary,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = strings.youtubeQueueStatus.format(
                        queuedSuggestions,
                        watchedCount,
                        favoritesCount,
                        searchCount,
                    ),
                    color = SmartVisionColors.TextSecondary,
                    style = CatalogMetaStyle,
                )
                YoutubeSettingsActionRow(
                    title = strings.youtubeAutoplay,
                    subtitle = strings.youtubeAutoplayHint,
                    value = if (autoplayEnabled) strings.enabled else strings.disabled,
                    focusRequester = autoplayFocusRequester,
                    onClick = onToggleAutoplay,
                )
                YoutubeSettingsActionRow(
                    title = strings.youtubeClearSearchHistory,
                    subtitle = strings.youtubeSearchPlaceholder,
                    value = strings.delete,
                    onClick = onClearSearchHistory,
                )
                YoutubeSettingsActionRow(
                    title = strings.youtubeClearWatchHistory,
                    subtitle = strings.youtubeCategoryHistory,
                    value = strings.delete,
                    onClick = onClearVideoHistory,
                )
                YoutubeSettingsActionRow(
                    title = strings.youtubeClearFavorites,
                    subtitle = strings.youtubeCategoryFavorites,
                    value = strings.delete,
                    onClick = onClearFavorites,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    YoutubeSettingsPillButton(
                        text = strings.cancel,
                        onClick = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun YoutubeSettingsActionRow(
    title: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focusStyle = LocalTvFocusStyle.current
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.015f,
                glowColor = focusStyle.accent,
                cornerRadius = 10.dp,
            )
            .clip(shape)
            .background(if (focusState.isFocused) focusStyle.background else SmartVisionColors.Surface.copy(alpha = 0.74f))
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else 1.dp,
                    if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border,
                ),
                shape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SmartVisionColors.TextPrimary,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = SmartVisionColors.TextSecondary,
                style = CatalogMetaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = value,
            color = if (focusState.isFocused) focusStyle.accent else Color.White.copy(alpha = 0.82f),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun YoutubeSettingsPillButton(
    text: String,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focusStyle = LocalTvFocusStyle.current
    Box(
        modifier = Modifier
            .height(36.dp)
            .width(116.dp)
            .tvFocusTarget(
                state = focusState,
                pressed = pressed,
                focusedScale = 1.04f,
                glowColor = focusStyle.accent,
                cornerRadius = 9.dp,
            )
            .clip(RoundedCornerShape(9.dp))
            .background(if (focusState.isFocused) focusStyle.background else SmartVisionColors.Surface.copy(alpha = 0.82f))
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else 1.dp,
                    if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border,
                ),
                RoundedCornerShape(9.dp),
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = SmartVisionColors.TextPrimary,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
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
    val visibleSuggestions = suggestions.take(3)
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
            if (focusState.isFocused) {
                Text(
                    text = video.meta,
                    color = Color.White.copy(alpha = 0.72f),
                    style = CatalogMetaStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (focusState.isFocused && video.durationLabel.isNotBlank()) {
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

        if (focusState.isFocused) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.58f)),
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

@Composable
private fun YoutubeNowPlayingBars(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "youtubeNowPlaying")
    val first by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "youtubeNowPlayingFirst",
    )
    val second by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(640), RepeatMode.Reverse),
        label = "youtubeNowPlayingSecond",
    )
    val third by transition.animateFloat(
        initialValue = 0.48f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(460), RepeatMode.Reverse),
        label = "youtubeNowPlayingThird",
    )
    Row(
        modifier = modifier.height(22.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(first, second, third).forEach { scale ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((18f * scale).dp)
                    .clip(RoundedCornerShape(50))
                    .background(YoutubePlayerNeonBlue),
            )
        }
    }
}

private fun youtubeCategoryIcon(id: String): ImageVector? =
    when (id) {
        "history" -> Icons.Default.Menu
        "favorites" -> Icons.Default.Favorite
        "trending" -> Icons.Default.PlayArrow
        else -> null
    }

private fun YoutubeCategoryUi.localizedLabel(strings: SmartVisionStrings): String =
    when (id) {
        "history" -> strings.youtubeCategoryHistory
        "favorites" -> strings.youtubeCategoryFavorites
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

private fun formatPlaybackClock(seconds: Double): String {
    val totalSeconds = seconds.toLong().coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val remainingSeconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, remainingSeconds)
}

private val YoutubePlayerGlassShape = RoundedCornerShape(18.dp)
private val YoutubePlayerGlassBackground = Color(0x300A2A66)
private val YoutubePlayerGlassBorder = Color.White.copy(alpha = 0.34f)
private val YoutubePlayerNeonBlue = Color(0xFF0A84FF)
private val YoutubePlayerGlassGlowBorder = YoutubePlayerNeonBlue.copy(alpha = 0.24f)
private val YoutubePlayerButtonBackground = Color(0xB30A1B38)

private const val YoutubeVideoCardAspectRatio = 16f / 9f
private const val YoutubeGridColumns = 4
private const val YoutubeAutoplayQueueTarget = 20
