package com.smartvision.svplayer.ui.home

import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.mock.HomePreviewMode
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

@Composable
fun ContentProgressCard(
    item: ContinueItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    onFocusChanged: (Boolean) -> Unit = {},
    onDown: (() -> Unit)? = null,
    enablePreview: Boolean = false,
    resumeOverlayText: String = "Resume playback",
    blocked: Boolean = false,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(SmartVisionDimensions.HomeContentRadius)
    val focusStyle = LocalTvFocusStyle.current
    val border by animateColorAsState(
        targetValue = if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border.copy(alpha = 0.78f),
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "contentCardBorder",
    )
    val isLive = item.mediaType == "LIVE"
    val previewActive = enablePreview &&
        focusState.isFocused &&
        !blocked &&
        !item.previewUrl.isNullOrBlank() &&
        item.previewMode != HomePreviewMode.None
    var showPreview by remember(item.id, item.previewUrl) { mutableStateOf(false) }

    LaunchedEffect(focusState.isFocused) {
        onFocusChanged(focusState.isFocused)
        if (focusState.isFocused) onFocused()
    }

    LaunchedEffect(previewActive, item.previewUrl) {
        showPreview = false
        if (previewActive) {
            val startDelay = when (item.previewMode) {
                HomePreviewMode.TrendSegments -> TrendPreviewStartDelayMillis
                HomePreviewMode.LiveImmediate,
                HomePreviewMode.ResumeLoop,
                HomePreviewMode.None,
                -> 0L
            }
            if (startDelay > 0L) delay(startDelay)
            showPreview = true
        }
    }

    Box(
        modifier = modifier
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown && onDown != null) {
                    onDown()
                    true
                } else {
                    false
                }
            }
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.045f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = SmartVisionDimensions.HomeContentRadius,
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
        if (!item.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = if (isLive) ContentScale.Fit else ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isLive) 16.dp else 0.dp),
            )
        }
        if (showPreview && item.previewUrl != null) {
            HomeMutedPreviewPlayer(
                url = item.previewUrl,
                mode = item.previewMode,
                startPositionMs = item.previewStartPositionMs,
                resumeOverlayText = resumeOverlayText,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.46f),
                            Color.Black.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                    ),
                ),
        )
        MediaTypeBadge(
            text = item.mediaType,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(7.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        ) {
            Text(
                text = item.title,
                color = Color.White,
                style = SmartVisionType.Caption.copy(fontSize = 11.sp, lineHeight = 14.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            ProgressBar(progress = item.progress)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item.meta,
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Caption.copy(fontSize = 10.sp, lineHeight = 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = item.remaining,
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Caption.copy(fontSize = 10.sp, lineHeight = 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
                        text = "Connexion indisponible",
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
private fun MediaTypeBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    if (text.isBlank()) return
    val accent = mediaTypeBadgeAccent(text)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(accent.copy(alpha = 0.30f))
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.86f)), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = SmartVisionType.Caption.copy(fontSize = 9.sp, lineHeight = 11.sp),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun mediaTypeBadgeAccent(text: String): Color =
    when (text.uppercase()) {
        "LIVE" -> SmartVisionColors.CyanAccent
        "FILM" -> SmartVisionColors.Warning
        "SERIE" -> SmartVisionColors.Primary
        else -> SmartVisionColors.TextSecondary
    }

@Composable
private fun ProgressBar(progress: Float) {
    val clamped = max(0f, min(1f, progress))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(4.dp)
                .background(SmartVisionColors.Primary),
        )
    }
}

@Composable
private fun HomeMutedPreviewPlayer(
    url: String,
    mode: HomePreviewMode,
    startPositionMs: Long,
    resumeOverlayText: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var transitionVisible by remember(url) { mutableStateOf(true) }
    var resumeOverlayVisible by remember(url, startPositionMs) { mutableStateOf(false) }
    val transitionAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (transitionVisible) 0.72f else 0f,
        animationSpec = tween(TrendPreviewFadeMillis.toInt(), easing = FastOutSlowInEasing),
        label = "trendPreviewFade",
    )
    val player = remember(url) {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
                setMediaItem(MediaItem.fromUri(url))
                prepare()
            }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LaunchedEffect(player, url, mode, startPositionMs) {
        when (mode) {
            HomePreviewMode.TrendSegments -> {
                resumeOverlayVisible = false
                var duration = player.duration
                var attempts = 0
                while ((duration <= 0L || duration == C.TIME_UNSET) && attempts < 35) {
                    delay(200L)
                    duration = player.duration
                    attempts++
                }
                if (duration <= 0L || duration == C.TIME_UNSET) {
                    duration = TrendPreviewFallbackDurationMillis
                }
                while (true) {
                    for (positionRatio in TrendPreviewPositions) {
                        transitionVisible = true
                        delay(TrendPreviewFadeMillis)
                        val targetPosition = (duration * positionRatio)
                            .toLong()
                            .coerceIn(0L, (duration - 1_000L).coerceAtLeast(0L))
                        player.seekTo(targetPosition)
                        player.volume = 0f
                        player.playWhenReady = true
                        player.play()
                        transitionVisible = false
                        delay(TrendPreviewSegmentMillis)
                    }
                }
            }

            HomePreviewMode.LiveImmediate -> {
                resumeOverlayVisible = false
                transitionVisible = false
                player.volume = 0f
                player.playWhenReady = true
                player.play()
            }

            HomePreviewMode.ResumeLoop -> {
                var duration = player.duration
                var attempts = 0
                while ((duration <= 0L || duration == C.TIME_UNSET) && attempts < 30) {
                    delay(200L)
                    duration = player.duration
                    attempts++
                }
                val safeStart = if (duration > 0L && duration != C.TIME_UNSET) {
                    startPositionMs.coerceIn(0L, (duration - 1_000L).coerceAtLeast(0L))
                } else {
                    startPositionMs.coerceAtLeast(0L)
                }
                transitionVisible = false
                while (true) {
                    resumeOverlayVisible = false
                    player.seekTo(safeStart)
                    player.volume = 0f
                    player.playWhenReady = true
                    player.play()
                    delay(ContinuePreviewLoopMillis)
                    resumeOverlayVisible = true
                    delay(ContinuePreviewOverlayMillis)
                }
            }

            HomePreviewMode.None -> Unit
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setBackgroundColor(AndroidColor.BLACK)
                    setShutterBackgroundColor(AndroidColor.BLACK)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = transitionAlpha)),
        )
        if (resumeOverlayVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.46f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = resumeOverlayText,
                    color = Color.White,
                    style = SmartVisionType.Caption.copy(fontSize = 10.sp, lineHeight = 12.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private const val TrendPreviewStartDelayMillis = 650L
private const val TrendPreviewSegmentMillis = 8_000L
private const val TrendPreviewFadeMillis = 220L
private const val TrendPreviewFallbackDurationMillis = 60 * 60_000L
private const val ContinuePreviewLoopMillis = 20_000L
private const val ContinuePreviewOverlayMillis = 2_000L
private val TrendPreviewPositions = listOf(0.10f, 0.25f, 0.45f, 0.65f, 0.80f)
