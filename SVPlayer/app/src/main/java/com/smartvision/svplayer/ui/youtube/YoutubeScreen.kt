package com.smartvision.svplayer.ui.youtube

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
import com.smartvision.svplayer.ui.catalog.CatalogCategoryRow
import com.smartvision.svplayer.ui.catalog.CatalogEmpty
import com.smartvision.svplayer.ui.catalog.CatalogError
import com.smartvision.svplayer.ui.catalog.CatalogLoading
import com.smartvision.svplayer.ui.catalog.CatalogMetaStyle
import com.smartvision.svplayer.ui.catalog.MediaCatalogDimens
import com.smartvision.svplayer.ui.catalog.MediaCatalogHeader
import com.smartvision.svplayer.ui.catalog.MediaCatalogPanel
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.home.HomeHeaderTab
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
    onOpenYoutubeVideo: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: YoutubeViewModel = viewModel(
        factory = viewModelFactory {
            YoutubeViewModel(container.youtubeRepository)
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    val firstVideoFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state.categories.isNotEmpty()) {
        if (state.categories.isNotEmpty()) {
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
            YoutubeCategoryList(
                state = state,
                selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                firstVideoFocusRequester = firstVideoFocusRequester,
                onCategory = viewModel::selectCategory,
                modifier = Modifier
                    .weight(0.22f)
                    .fillMaxHeight(),
            )
            YoutubeVideoGrid(
                state = state,
                firstVideoFocusRequester = firstVideoFocusRequester,
                searchFocusRequester = searchFocusRequester,
                selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onSearchSubmit = { viewModel.submitSearch() },
                onSuggestionClick = viewModel::selectSuggestion,
                onVideoFocused = viewModel::focusVideo,
                onVideoClick = { video ->
                    viewModel.openYoutubeVideo(video)
                    onOpenYoutubeVideo(video.videoId)
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
    selectedCategoryFocusRequester: FocusRequester,
    firstVideoFocusRequester: FocusRequester,
    onCategory: (YoutubeCategoryUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    MediaCatalogPanel(
        title = "Categories",
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
                    rightFocusRequester = firstVideoFocusRequester.takeIf { state.videos.isNotEmpty() },
                    onClick = { onCategory(category) },
                )
            }
        }
    }
}

@Composable
private fun YoutubeVideoGrid(
    state: YoutubeScreenState,
    firstVideoFocusRequester: FocusRequester,
    searchFocusRequester: FocusRequester,
    selectedCategoryFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onVideoFocused: (YoutubeVideoUi) -> Unit,
    onVideoClick: (YoutubeVideoUi) -> Unit,
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

    LaunchedEffect(state.selectedCategoryId) {
        if (gridState.layoutInfo.totalItemsCount > 0) gridState.scrollToItem(0)
    }

    LaunchedEffect(gridState, state.videos.size, state.nextPageToken) {
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
                focusRequester = searchFocusRequester,
                downFocusRequester = when {
                    showSuggestions -> firstSuggestionFocusRequester
                    state.videos.isNotEmpty() -> firstVideoFocusRequester
                    else -> null
                },
                onQueryChange = onSearchQueryChange,
                onSubmit = onSearchSubmit,
                onFocusChanged = { searchFocused = it },
                modifier = Modifier.width(430.dp),
            )
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.videos.isNotEmpty()) "${state.videos.size} videos chargees" else "YouTube",
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
                    title = "Chargement YouTube",
                    modifier = Modifier.fillMaxSize(),
                )

                state.errorMessage != null && state.videos.isEmpty() -> CatalogError(
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize(),
                )

                state.videos.isEmpty() -> CatalogEmpty(
                    title = "Aucune video",
                    subtitle = "Selectionnez une categorie ou lancez une recherche.",
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
                    itemsIndexed(state.videos, key = { _, video -> video.videoId }) { index, video ->
                        YoutubeVideoCard(
                            video = video,
                            selected = video.videoId == state.selectedVideoId,
                            focusRequester = firstVideoFocusRequester.takeIf { index == 0 },
                            leftFocusRequester = selectedCategoryFocusRequester.takeIf {
                                index % MediaCatalogDimens.MediaGridColumns == 0
                            },
                            upFocusRequester = searchFocusRequester.takeIf {
                                index < MediaCatalogDimens.MediaGridColumns
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
                downFocusRequester = firstVideoFocusRequester.takeIf { state.videos.isNotEmpty() },
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
                        text = "Chargement...",
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
private fun YoutubeSearchInput(
    query: String,
    focusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val fallbackFocusRequester = remember { FocusRequester() }
    val inputFocusRequester = focusRequester ?: fallbackFocusRequester
    var focused by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(7.dp)
    val borderColor by animateColorAsState(
        targetValue = if (focused || editing) Color(0xFFFF3B3B) else SmartVisionColors.Border,
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "youtubeSearchBorder",
    )

    LaunchedEffect(editing) {
        if (editing) {
            inputFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        readOnly = !editing,
        cursorBrush = SolidColor(Color(0xFFFF3B3B)),
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
            .background(SmartVisionColors.Surface.copy(alpha = 0.86f))
            .border(BorderStroke(if (focused) 2.dp else 1.dp, borderColor), shape)
            .padding(horizontal = 10.dp),
        decorationBox = { inner ->
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (focused || editing) Color(0xFFFF3B3B) else SmartVisionColors.TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(7.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isBlank()) {
                        Text(
                            text = "Rechercher sur YouTube",
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
            .border(BorderStroke(1.dp, Color(0xFFFF3B3B).copy(alpha = 0.58f)), RoundedCornerShape(8.dp))
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
                glowColor = Color(0xFFFF3B3B),
                cornerRadius = 6.dp,
            )
            .onFocusChanged { onSuggestionFocusChanged(it.isFocused) }
            .clip(shape)
            .background(if (focusState.isFocused) Color(0xFFFF3B3B).copy(alpha = 0.26f) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = if (focusState.isFocused) Color.White else SmartVisionColors.TextSecondary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = if (focusState.isFocused) Color.White else SmartVisionColors.TextPrimary,
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
    val isFocused = focusState.isFocused
    var showPreview by remember(video.videoId) { mutableStateOf(false) }
    val shape = RoundedCornerShape(MediaCatalogDimens.ItemRadius)
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> SmartVisionColors.FocusWhite
            selected -> Color(0xFFFF3B3B)
            else -> SmartVisionColors.Border.copy(alpha = 0.72f)
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "youtubeVideoBorder",
    )

    LaunchedEffect(isFocused, video.videoId) {
        showPreview = false
        if (isFocused) {
            delay(1_000)
            showPreview = true
        }
    }

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
                    if (focusState.isFocused) SmartVisionDimensions.FocusBorder else SmartVisionDimensions.PanelBorder,
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

        if (showPreview) {
            YoutubeInlinePreview(
                videoId = video.videoId,
                modifier = Modifier.matchParentSize(),
            )
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
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
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
private fun YoutubeInlinePreview(
    videoId: String,
    modifier: Modifier = Modifier,
) {
    YoutubeWebPlayer(
        videoId = videoId,
        mode = YoutubePlaybackMode.Preview,
        modifier = modifier,
    )
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
