@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.smartvision.svplayer.ui.home

import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import com.smartvision.svplayer.ui.youtube.YoutubePlaybackMode
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

@Composable
fun TrendingContentRow(
    title: String,
    items: List<ContinueItem>,
    onItemClick: (ContinueItem) -> Unit,
    onPrepareItems: (List<ContinueItem>) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState? = null,
    firstItemFocusRequester: FocusRequester? = null,
    itemFocusRequesters: Map<String, FocusRequester> = emptyMap(),
    onDownFromRow: ((Int) -> Unit)? = null,
    onUpFromRow: ((Int) -> Unit)? = null,
    blocked: Boolean = false,
    blockedMessage: String = "Connection unavailable",
    onBlockedClick: () -> Unit = {},
    previewController: HomePreviewController,
) {
    val fallbackRowState = rememberLazyListState()
    val rowState = lazyListState ?: fallbackRowState
    val rowScope = rememberCoroutineScope()
    val internalFirstItemFocusRequester = remember { FocusRequester() }
    val resolvedFirstItemFocusRequester = firstItemFocusRequester ?: internalFirstItemFocusRequester
    var focusedItemId by remember { mutableStateOf<String?>(null) }
    var focusedIndex by remember { mutableIntStateOf(-1) }
    var activePreviewId by remember { mutableStateOf<String?>(null) }
    val totalFocusableItems = items.size
    val itemsSignature = remember(items) {
        items.joinToString("|") { it.id }
    }
    val lastItemFocusRequester = remember(itemsSignature) { FocusRequester() }
    val resolvedLastItemFocusRequester = items.lastOrNull()
        ?.let { itemFocusRequesters[it.id] }
        ?: lastItemFocusRequester

    LaunchedEffect(itemsSignature) {
        focusedItemId = null
        focusedIndex = -1
        activePreviewId = null
        if (rowState.firstVisibleItemIndex != 0 || rowState.firstVisibleItemScrollOffset != 0) {
            rowState.scrollToItem(0)
        }
    }

    LaunchedEffect(itemsSignature, rowState) {
        snapshotFlow {
            val visible = rowState.layoutInfo.visibleItemsInfo
            val first = visible.firstOrNull()?.index ?: rowState.firstVisibleItemIndex
            val last = visible.lastOrNull()?.index ?: first
            first to last
        }.collect { (first, last) ->
            if (items.isEmpty()) return@collect
            val safeFirst = first.coerceIn(0, items.lastIndex)
            val safeLast = (last + TrendingPrefetchAhead)
                .coerceIn(safeFirst, items.lastIndex)
            onPrepareItems(items.subList(safeFirst, safeLast + 1))
        }
    }

    LaunchedEffect(focusedItemId, focusedIndex) {
        activePreviewId = null
        val pendingId = focusedItemId ?: return@LaunchedEffect
        if (focusedIndex < 0) return@LaunchedEffect
        delay(TrendingFocusStabilityMillis)
        if (focusedItemId != pendingId) return@LaunchedEffect
        if (focusedItemId == pendingId) {
            activePreviewId = pendingId
        }
    }

    fun wrapFocus(targetIndex: Int, targetRequester: FocusRequester) {
        if (items.isEmpty()) return
        rowScope.launch {
            rowState.scrollToItem(targetIndex.coerceIn(items.indices))
            withFrameNanos { }
            runCatching { targetRequester.requestFocus() }
        }
    }

    Column(modifier = modifier.height(SmartVisionDimensions.HomeContentRowHeight)) {
        Text(
            text = title,
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.HomeSectionTitle,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(
            state = rowState,
            modifier = Modifier
                .fillMaxWidth()
                .height(SmartVisionDimensions.HomeContentFocusedCardHeight),
            contentPadding = PaddingValues(horizontal = SmartVisionDimensions.HomeRowEdgePadding),
            horizontalArrangement = Arrangement.spacedBy(SmartVisionDimensions.HomeContentCardSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                TrendingPreviewCard(
                    item = item,
                    enablePreview = activePreviewId == item.id &&
                        item.previewPrepared &&
                        !item.previewUrl.isNullOrBlank(),
                    onClick = { if (blocked) onBlockedClick() else onItemClick(item) },
                    focusRequester = when {
                        item.id in itemFocusRequesters -> itemFocusRequesters.getValue(item.id)
                        index == 0 -> resolvedFirstItemFocusRequester
                        index == items.lastIndex -> lastItemFocusRequester
                        else -> null
                    },
                    onFocusChanged = { focused ->
                        if (focused) {
                            focusedItemId = item.id
                            focusedIndex = index
                        } else if (focusedItemId == item.id) {
                            focusedItemId = null
                            focusedIndex = -1
                        }
                    },
                    onDown = onDownFromRow?.let { callback -> { callback(index) } },
                    onUp = onUpFromRow?.let { callback -> { callback(index) } },
                    onLeft = if (index == 0) {
                        {
                            wrapFocus(
                                items.lastIndex,
                                if (items.size == 1) resolvedFirstItemFocusRequester else resolvedLastItemFocusRequester,
                            )
                        }
                    } else {
                        null
                    },
                    onRight = if (index == items.lastIndex) {
                        { wrapFocus(0, resolvedFirstItemFocusRequester) }
                    } else {
                        null
                    },
                    blocked = blocked,
                    blockedMessage = blockedMessage,
                    previewController = previewController,
                    modifier = Modifier
                        .width(SmartVisionDimensions.HomeContentPreviewCardWidth)
                        .height(SmartVisionDimensions.HomeContentCardHeight),
                )
            }
            item(key = "trending_tail_spacer_$title") {
                Spacer(
                    modifier = Modifier
                        .width(SmartVisionDimensions.HomeContentPreviewCardWidth * 3)
                        .height(SmartVisionDimensions.HomeContentCardHeight),
                )
            }
        }
    }
}

@Composable
private fun TrendingPreviewCard(
    item: ContinueItem,
    enablePreview: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {},
    onDown: (() -> Unit)? = null,
    onUp: (() -> Unit)? = null,
    blockLeft: Boolean = false,
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    blocked: Boolean = false,
    blockedMessage: String = "Connection unavailable",
    previewController: HomePreviewController,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focusStyle = LocalTvFocusStyle.current
    val shape = RoundedCornerShape(SmartVisionDimensions.HomeContentRadius)
    val border by animateColorAsState(
        targetValue = if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border.copy(alpha = 0.72f),
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "trendingCardBorder",
    )
    val previewUrl = item.previewUrl
    val displayedPosterUrl = item.imageUrl
    val firstFramePreviewId by previewController.firstFramePreviewId.collectAsStateWithLifecycle()
    val videoVisible = firstFramePreviewId == item.id
    val detailLabel = if (item.mediaType == "SERIE") item.secondaryLabel else item.previewDurationLabel
    var compactTitle by remember(item.id) { mutableStateOf(false) }

    LaunchedEffect(focusState.isFocused) {
        onFocusChanged(focusState.isFocused)
        if (!focusState.isFocused) previewController.stop(item.id)
    }

    LaunchedEffect(enablePreview, previewUrl, focusState.isFocused) {
        if (enablePreview && focusState.isFocused && previewUrl != null) {
            previewController.play(item.id, previewUrl, item.previewStartPositionMs, item.previewMode)
        } else {
            previewController.stop(item.id)
        }
    }

    DisposableEffect(item.id, previewController) {
        onDispose { previewController.stop(item.id) }
    }

    Box(
        modifier = modifier
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when {
                        event.key == Key.DirectionDown && onDown != null -> {
                            onDown()
                            true
                        }
                        event.key == Key.DirectionUp && onUp != null -> {
                            onUp()
                            true
                        }
                        event.key == Key.DirectionLeft && onLeft != null -> {
                            onLeft()
                            true
                        }
                        event.key == Key.DirectionRight && onRight != null -> {
                            onRight()
                            true
                        }
                        event.key == Key.DirectionLeft && blockLeft -> true
                        else -> false
                    }
                }
            }
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = if (videoVisible) HomeCardVideoFocusedScale else HomeCardFocusedScale,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = SmartVisionDimensions.HomeContentRadius,
            )
            .shadow(
                elevation = if (focusState.isFocused) 10.dp else 3.dp,
                shape = shape,
                ambientColor = SmartVisionColors.CardFocusGlow,
                spotColor = SmartVisionColors.CardFocusGlow,
            )
            .clip(shape)
            .background(SmartVisionColors.Surface)
            .border(BorderStroke(if (focusState.isFocused) focusStyle.borderWidth else 1.dp, border), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource),
    ) {
        HomeVisualBackground(style = item.visualStyle, modifier = Modifier.fillMaxSize())
        if (!displayedPosterUrl.isNullOrBlank()) {
            AsyncImage(
                model = displayedPosterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            )
        }
        HomePreviewSurface(
            controller = previewController,
            previewId = item.id,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .fillMaxHeight(if (videoVisible) 0.28f else 0.62f)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = if (videoVisible) 0.80f else 0.96f),
                        ),
                    ),
                ),
        )
        if (!videoVisible && !item.ratingLabel.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.84f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.ratingLabel.removeSuffix("*"),
                        color = Color.White,
                        style = SmartVisionType.Caption.copy(fontSize = 10.sp, lineHeight = 12.sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = TrendRatingStarColor,
                        modifier = Modifier.size(10.dp),
                    )
                }
            }
        }
        if (!videoVisible && !detailLabel.isNullOrBlank() && !detailLabel.isZeroDurationLabel()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.84f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Text(
                    text = detailLabel,
                    color = Color.White,
                    style = SmartVisionType.Caption.copy(fontSize = 9.sp, lineHeight = 11.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 7.dp),
        ) {
            Text(
                text = item.title,
                color = Color.White,
                style = SmartVisionType.Caption.copy(
                    fontSize = when {
                        videoVisible -> 11.sp
                        compactTitle -> 12.sp
                        else -> 15.sp
                    },
                    lineHeight = when {
                        videoVisible -> 13.sp
                        compactTitle -> 14.sp
                        else -> 17.sp
                    },
                ),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result ->
                    if (!videoVisible && result.hasVisualOverflow) compactTitle = true
                },
            )
        }
        if (blocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = SmartVisionColors.Warning,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = blockedMessage,
                        color = Color.White,
                        style = SmartVisionType.Caption.copy(fontSize = 9.sp, lineHeight = 11.sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendingMutedPreviewPlayer(
    url: String,
    startPositionMs: Long,
    fallbackStartPositionMs: Long,
    knownDurationMs: Long?,
    posterUrl: String?,
    onPreviewUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var videoVisible by remember(url) { mutableStateOf(false) }
    var firstFrameRendered by remember(url) { mutableStateOf(false) }
    var playbackFailed by remember(url) { mutableStateOf(false) }
    val videoAlpha by animateFloatAsState(
        targetValue = if (videoVisible) 1f else 0f,
        animationSpec = tween(TrendingVideoCrossfadeMillis.toInt(), easing = FastOutSlowInEasing),
        label = "trendingPreviewVideoAlpha",
    )
    val player = remember(url) {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = false
                setMediaItem(MediaItem.fromUri(url))
                prepare()
            }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrameRendered = true
                videoVisible = true
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackFailed = true
                onPreviewUnavailable()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, url, startPositionMs, fallbackStartPositionMs, knownDurationMs) {
        var volumeFadeJob: Job? = null
        fun restartPreviewAudioFade() {
            volumeFadeJob?.cancel()
            player.volume = 0f
            volumeFadeJob = launch { player.fadeInMiniPlayerVolume() }
        }

        try {
            delay(TrendingVideoPrepareDelayMillis)
            val firstStart = resolvePreviewStart(
                player = player,
                requestedStartPositionMs = startPositionMs,
                knownDurationMs = knownDurationMs,
                ratio = TrendingPreviewStartRatio,
            )
            val firstOk = player.playPreviewAt(firstStart, ::restartPreviewAudioFade) {
                firstFrameRendered || playbackFailed
            }
            if (!firstOk && !playbackFailed) {
                firstFrameRendered = false
                val fallbackStart = resolvePreviewStart(
                    player = player,
                    requestedStartPositionMs = fallbackStartPositionMs,
                    knownDurationMs = knownDurationMs,
                    ratio = TrendingPreviewFallbackRatio,
                )
                player.playPreviewAt(fallbackStart, ::restartPreviewAudioFade) {
                    firstFrameRendered || playbackFailed
                }
            }
            if (!firstFrameRendered || playbackFailed) {
                videoVisible = false
                player.pause()
                onPreviewUnavailable()
            } else {
                awaitCancellation()
            }
        } finally {
            volumeFadeJob?.cancel()
            player.volume = 0f
        }
    }

    Box(modifier = modifier.background(Color.Transparent)) {
        if (!posterUrl.isNullOrBlank()) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            )
        }
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .alpha(videoAlpha),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    setShutterBackgroundColor(AndroidColor.TRANSPARENT)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    isFocusable = false
                    isFocusableInTouchMode = false
                    isClickable = false
                    isLongClickable = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                    this.player = player
                }
            },
            update = { playerView ->
                if (playerView.player !== player) {
                    playerView.player = player
                }
            },
        )
    }
}

@Composable
private fun TrendingViewAllButton(
    text: String,
    onClick: () -> Unit,
    onDown: (() -> Unit)? = null,
    onUp: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(SmartVisionDimensions.HomeContentRadius)
    val focusStyle = LocalTvFocusStyle.current

    Column(
        modifier = modifier
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when {
                        event.key == Key.DirectionDown && onDown != null -> {
                            onDown()
                            true
                        }
                        event.key == Key.DirectionUp && onUp != null -> {
                            onUp()
                            true
                        }
                        else -> false
                    }
                }
            }
            .tvFocusTarget(
                state = focusState,
                pressed = pressed,
                focusedScale = 1.0f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = SmartVisionDimensions.HomeContentRadius,
            )
            .clip(shape)
            .background(SmartVisionColors.SurfaceElevated.copy(alpha = 0.82f))
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else 1.dp,
                    if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border,
                ),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(PaddingValues(horizontal = 8.dp, vertical = 10.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = SmartVisionColors.TextPrimary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = text,
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private suspend fun resolvePreviewStart(
    player: Player,
    requestedStartPositionMs: Long,
    knownDurationMs: Long?,
    ratio: Double,
): Long {
    if (requestedStartPositionMs > 0L) return requestedStartPositionMs
    var duration = knownDurationMs ?: player.duration
    var attempts = 0
    while ((duration <= 0L || duration == C.TIME_UNSET) && attempts < DurationProbeAttempts) {
        delay(DurationProbeDelayMillis)
        duration = player.duration
        attempts++
    }
    return duration.previewStartAt(ratio)
}

private suspend fun ExoPlayer.playPreviewAt(
    startPositionMs: Long,
    startAudioFade: () -> Unit,
    done: () -> Boolean,
): Boolean {
    seekTo(startPositionMs.coerceAtLeast(0L))
    volume = 0f
    playWhenReady = true
    play()
    startAudioFade()
    var attempts = 0
    while (!done() && attempts < PreviewFirstFrameAttempts) {
        delay(PreviewFirstFrameDelayMillis)
        attempts++
    }
    return done()
}

private suspend fun ExoPlayer.fadeInMiniPlayerVolume() {
    delay(MiniPlayerAudioStartDelayMillis)
    repeat(MiniPlayerAudioFadeSteps) { step ->
        delay(MiniPlayerAudioFadeMillis / MiniPlayerAudioFadeSteps)
        volume = (step + 1).toFloat() / MiniPlayerAudioFadeSteps
    }
    volume = 1f
}

private fun Long.previewStartAt(ratio: Double): Long =
    takeIf { it > 0L && it != C.TIME_UNSET }
        ?.let { (it * ratio).roundToLong().coerceIn(0L, (it - 1_000L).coerceAtLeast(0L)) }
        ?: 0L

private object UnsupportedTrendingPreviewCache {
    private val urlHashes = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())

    fun isUnsupported(url: String?): Boolean =
        !url.isNullOrBlank() && url.hashCode() in urlHashes

    fun markUnsupported(url: String) {
        urlHashes += url.hashCode()
    }
}

private const val TrendingFocusStabilityMillis = 550L
private const val HomeCardFocusedScale = 1.04f
private const val HomeCardVideoFocusedScale = 1.2f
private val TrendRatingStarColor = Color(0xFFFFD54F)

private fun String.isZeroDurationLabel(): Boolean =
    trim().matches(Regex("""(?i)^0+\s*(?:h|m|min)?(?:\s*0+\s*(?:m|min))?$"""))
private const val TrendingVideoPrepareDelayMillis = 900L
private const val TrendingVideoCrossfadeMillis = 650L
private const val TrendingPrefetchAhead = 3
private const val TrendingPreviewStartRatio = 0.15
private const val TrendingPreviewFallbackRatio = 0.30
private const val DurationProbeAttempts = 16
private const val DurationProbeDelayMillis = 100L
private const val PreviewFirstFrameAttempts = 50
private const val PreviewFirstFrameDelayMillis = 50L
private const val MiniPlayerAudioStartDelayMillis = 1_000L
private const val MiniPlayerAudioFadeMillis = 1_000L
private const val MiniPlayerAudioFadeSteps = 10
