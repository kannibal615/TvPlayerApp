@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.smartvision.svplayer.ui.home

import android.graphics.Color as AndroidColor
import android.os.SystemClock
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
import androidx.compose.ui.draw.alpha
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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.smartvision.svplayer.data.diagnostics.PerformanceDiagnosticRecorder
import coil.compose.AsyncImage
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.mock.HomePreviewMode
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    onUp: (() -> Unit)? = null,
    blockLeft: Boolean = false,
    enablePreview: Boolean = false,
    resumeOverlayText: String = "Resume playback",
    blocked: Boolean = false,
    blockedMessage: String = "Connection unavailable",
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
    val previewDisabledForSession = UnsupportedHomePreviewCache.isUnsupported(item.previewUrl)
    val previewActive = enablePreview &&
        focusState.isFocused &&
        !blocked &&
        !item.previewUrl.isNullOrBlank() &&
        item.previewMode != HomePreviewMode.None &&
        !previewDisabledForSession
    val previewPosterUrl = item.previewImageUrl ?: item.imageUrl
    var showPreview by remember(item.id, item.previewUrl) { mutableStateOf(false) }
    val trendPreviewVisible = showPreview && item.previewMode == HomePreviewMode.TrendSegments

    LaunchedEffect(focusState.isFocused) {
        onFocusChanged(focusState.isFocused)
        if (focusState.isFocused) onFocused()
        // PERF_DIAG: focus-to-preview marker for black-frame and delayed mini-player diagnosis.
        PerformanceDiagnosticRecorder.record(
            sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
            event = "content_card_focus_changed",
            fields = mapOf(
                "id" to item.id,
                "title" to item.title,
                "focused" to focusState.isFocused,
                "enablePreview" to enablePreview,
                "blocked" to blocked,
                "previewActive" to previewActive,
                "previewDisabledForSession" to previewDisabledForSession,
                "previewMode" to item.previewMode.name,
                "hasPreviewUrl" to !item.previewUrl.isNullOrBlank(),
                "hasPoster" to !previewPosterUrl.isNullOrBlank(),
            ),
        )
    }

    LaunchedEffect(previewActive, item.previewUrl) {
        showPreview = false
        if (previewActive) {
            // PERF_FIX: a short focus debounce avoids creating/releasing ExoPlayer during fast D-pad passes.
            delay(HomePreviewFocusDebounceMillis)
            showPreview = true
        }
        PerformanceDiagnosticRecorder.record(
            sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
            event = "content_card_preview_visibility",
            fields = mapOf(
                "id" to item.id,
                "title" to item.title,
                "previewActive" to previewActive,
                "showPreview" to showPreview,
                "previewMode" to item.previewMode.name,
                "focusDebounceMs" to HomePreviewFocusDebounceMillis,
                "urlHash" to (item.previewUrl?.hashCode() ?: 0),
            ),
        )
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
                        event.key == Key.DirectionLeft && blockLeft -> true
                        else -> false
                    }
                }
            }
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.0f,
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
        val displayedImageUrl = if (showPreview && !previewPosterUrl.isNullOrBlank()) {
            previewPosterUrl
        } else {
            item.imageUrl
        }
        if (!displayedImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = displayedImageUrl,
                contentDescription = null,
                contentScale = if (isLive && displayedImageUrl == item.imageUrl) ContentScale.Fit else ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isLive && displayedImageUrl == item.imageUrl) 16.dp else 0.dp),
            )
        }
        if (showPreview && item.previewUrl != null) {
            HomeMutedPreviewPlayer(
                url = item.previewUrl,
                mode = item.previewMode,
                startPositionMs = item.previewStartPositionMs,
                posterUrl = previewPosterUrl,
                resumeOverlayText = resumeOverlayText,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (!trendPreviewVisible) {
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
    posterUrl: String?,
    resumeOverlayText: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val showPoster = !posterUrl.isNullOrBlank()
    val playerCreatedAt = remember(url) { SystemClock.elapsedRealtime() }
    var transitionVisible by remember(url) { mutableStateOf(false) }
    var resumeOverlayVisible by remember(url, startPositionMs) { mutableStateOf(false) }
    var videoVisible by remember(url) { mutableStateOf(false) }
    var firstFrameRendered by remember(url) { mutableStateOf(false) }
    var playbackFailed by remember(url) { mutableStateOf(false) }
    val transitionAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (transitionVisible) 0.72f else 0f,
        animationSpec = tween(PreviewFadeMillis.toInt(), easing = FastOutSlowInEasing),
        label = "trendPreviewFade",
    )
    val videoAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (videoVisible) 1f else 0f,
        animationSpec = tween(PreviewCrossfadeMillis.toInt(), easing = FastOutSlowInEasing),
        label = "homePreviewVideoAlpha",
    )
    val player = remember(url) {
        // PERF_DIAG: Media3 setup timing for the Home mini-player only.
        val startedAt = SystemClock.elapsedRealtime()
        ExoPlayer.Builder(context)
            .build()
            .apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = false
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                PerformanceDiagnosticRecorder.recordDuration(
                    sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
                    event = "mini_player_created_and_prepare_called",
                    startedAtMs = startedAt,
                    fields = mapOf(
                        "mode" to mode.name,
                        "urlHash" to url.hashCode(),
                        "startPositionMs" to startPositionMs,
                        "showPoster" to showPoster,
                    ),
                )
            }
    }

    DisposableEffect(player) {
        // PERF_DIAG: Media3 callbacks explain buffering, first frame, and black-screen duration.
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrameRendered = true
                videoVisible = true
                PerformanceDiagnosticRecorder.recordDuration(
                    sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
                    event = "mini_player_first_frame",
                    startedAtMs = playerCreatedAt,
                    fields = mapOf(
                        "mode" to mode.name,
                        "positionMs" to player.currentPosition,
                        "durationMs" to player.duration,
                        "videoVisible" to videoVisible,
                        "posterVisible" to showPoster,
                    ),
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                PerformanceDiagnosticRecorder.recordDuration(
                    sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
                    event = "mini_player_playback_state",
                    startedAtMs = playerCreatedAt,
                    fields = mapOf(
                        "mode" to mode.name,
                        "state" to playbackState.toPlaybackStateLabel(),
                        "positionMs" to player.currentPosition,
                        "durationMs" to player.duration,
                        "playWhenReady" to player.playWhenReady,
                        "isPlaying" to player.isPlaying,
                        "videoVisible" to videoVisible,
                        "firstFrameRendered" to firstFrameRendered,
                    ),
                )
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                PerformanceDiagnosticRecorder.recordDuration(
                    sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
                    event = "mini_player_is_playing_changed",
                    startedAtMs = playerCreatedAt,
                    fields = mapOf(
                        "mode" to mode.name,
                        "isPlaying" to isPlaying,
                        "positionMs" to player.currentPosition,
                        "videoVisible" to videoVisible,
                    ),
                )
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackFailed = true
                if (error.errorCodeName.contains("PARSING_CONTAINER_UNSUPPORTED", ignoreCase = true)) {
                    UnsupportedHomePreviewCache.markUnsupported(url)
                }
                PerformanceDiagnosticRecorder.recordDuration(
                    sheet = PerformanceDiagnosticRecorder.SHEET_ERRORS,
                    event = "mini_player_error",
                    startedAtMs = playerCreatedAt,
                    fields = mapOf(
                        "mode" to mode.name,
                        "errorCode" to error.errorCode,
                        "errorCodeName" to error.errorCodeName,
                        "positionMs" to player.currentPosition,
                        "markedUnsupportedForSession" to UnsupportedHomePreviewCache.isUnsupported(url),
                    ),
                    error = error,
                )
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    DisposableEffect(player) {
        onDispose {
            PerformanceDiagnosticRecorder.recordDuration(
                sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
                event = "mini_player_released",
                startedAtMs = playerCreatedAt,
                fields = mapOf(
                    "mode" to mode.name,
                    "positionMs" to player.currentPosition,
                    "durationMs" to player.duration,
                    "firstFrameRendered" to firstFrameRendered,
                ),
            )
            player.release()
        }
    }

    LaunchedEffect(player, url, mode, startPositionMs) {
        var volumeFadeJob: Job? = null
        fun restartPreviewAudioFade() {
            volumeFadeJob?.cancel()
            player.volume = 0f
            volumeFadeJob = launch { player.fadeInMiniPlayerVolume() }
        }

        PerformanceDiagnosticRecorder.record(
            sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
            event = "mini_player_effect_started",
            fields = mapOf(
                "mode" to mode.name,
                "startPositionMs" to startPositionMs,
                "showPoster" to showPoster,
                "urlHash" to url.hashCode(),
            ),
        )
        try {
            when (mode) {
                HomePreviewMode.TrendSegments -> {
                    resumeOverlayVisible = false
                    videoVisible = false
                    delay(TrendPreviewPosterDelayMillis)
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
                        event = "trend_preview_poster_delay_complete",
                        startedAtMs = playerCreatedAt,
                        fields = mapOf("posterDelayMs" to TrendPreviewPosterDelayMillis),
                    )
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
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
                        event = "trend_preview_duration_ready",
                        startedAtMs = playerCreatedAt,
                        fields = mapOf("durationMs" to duration, "attempts" to attempts),
                    )
                    for (positionRatio in TrendPreviewPositions) {
                        val targetPosition = (duration * positionRatio)
                            .toLong()
                            .coerceIn(0L, (duration - 1_000L).coerceAtLeast(0L))
                        player.seekTo(targetPosition)
                        player.volume = 0f
                        player.playWhenReady = true
                        player.play()
                        restartPreviewAudioFade()
                        PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
                        event = "trend_preview_segment_started",
                        startedAtMs = playerCreatedAt,
                        fields = mapOf(
                            "positionRatio" to positionRatio,
                            "targetPositionMs" to targetPosition,
                            "videoVisible" to videoVisible,
                            "firstFrameRendered" to firstFrameRendered,
                        ),
                        )
                        if (!videoVisible) {
                            var waitAttempts = 0
                            while (!firstFrameRendered && !playbackFailed && waitAttempts < FirstFrameWaitAttempts) {
                                delay(50L)
                                waitAttempts++
                            }
                            if (!firstFrameRendered || playbackFailed) {
                                volumeFadeJob?.cancel()
                                player.volume = 0f
                                PerformanceDiagnosticRecorder.recordDuration(
                                    sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
                                    event = "trend_preview_first_frame_wait_aborted",
                                    startedAtMs = playerCreatedAt,
                                    fields = mapOf(
                                        "targetPositionMs" to targetPosition,
                                        "playbackFailed" to playbackFailed,
                                        "waitAttempts" to waitAttempts,
                                    ),
                                )
                                break
                            }
                            videoVisible = true
                            PerformanceDiagnosticRecorder.recordDuration(
                                sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
                                event = "trend_preview_video_made_visible",
                                startedAtMs = playerCreatedAt,
                                fields = mapOf("targetPositionMs" to targetPosition),
                            )
                        } else {
                            transitionVisible = true
                            delay(PreviewFadeMillis)
                        }
                        transitionVisible = false
                        delay(TrendPreviewSegmentMillis)
                    }
                    player.pause()
                    volumeFadeJob?.cancel()
                    player.volume = 0f
                    videoVisible = false
                    transitionVisible = false
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
                        event = "trend_preview_finished",
                        startedAtMs = playerCreatedAt,
                    )
                }

                HomePreviewMode.LiveImmediate -> {
                    resumeOverlayVisible = false
                    transitionVisible = false
                    videoVisible = firstFrameRendered
                    player.volume = 0f
                    player.playWhenReady = true
                    player.play()
                    restartPreviewAudioFade()
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
                        event = "live_preview_play_called",
                        startedAtMs = playerCreatedAt,
                        fields = mapOf("videoVisible" to videoVisible, "firstFrameRendered" to firstFrameRendered),
                    )
                    awaitCancellation()
                }

                HomePreviewMode.ResumeLoop -> {
                    transitionVisible = false
                    videoVisible = firstFrameRendered
                    val requestedStart = startPositionMs.coerceAtLeast(0L)
                    player.seekTo(requestedStart)
                    player.volume = 0f
                    player.playWhenReady = true
                    player.play()
                    restartPreviewAudioFade()
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
                        event = "resume_preview_play_called",
                        startedAtMs = playerCreatedAt,
                        fields = mapOf(
                            "requestedStartMs" to requestedStart,
                            "videoVisible" to videoVisible,
                            "firstFrameRendered" to firstFrameRendered,
                        ),
                    )
                    while (true) {
                        delay(ContinuePreviewLoopMillis)
                        if (playbackFailed) return@LaunchedEffect
                        resumeOverlayVisible = true
                        delay(ContinuePreviewOverlayMillis)
                        resumeOverlayVisible = false
                        val duration = player.duration
                        val safeStart = if (duration > 0L && duration != C.TIME_UNSET) {
                            requestedStart.coerceIn(0L, (duration - 1_000L).coerceAtLeast(0L))
                        } else {
                            requestedStart
                        }
                        player.seekTo(safeStart)
                        player.playWhenReady = true
                        player.play()
                        PerformanceDiagnosticRecorder.recordDuration(
                            sheet = PerformanceDiagnosticRecorder.SHEET_MINI_PLAYER,
                            event = "resume_preview_loop_restart",
                            startedAtMs = playerCreatedAt,
                            fields = mapOf("safeStartMs" to safeStart, "durationMs" to duration),
                        )
                    }
                }

                HomePreviewMode.YoutubeTrailer,
                HomePreviewMode.None -> Unit
            }
        } finally {
            volumeFadeJob?.cancel()
            player.volume = 0f
        }
    }

    Box(modifier = modifier.background(Color.Transparent)) {
        if (showPoster) {
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

private fun Int.toPlaybackStateLabel(): String =
    when (this) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "UNKNOWN_$this"
    }

private suspend fun ExoPlayer.fadeInMiniPlayerVolume() {
    delay(MiniPlayerAudioStartDelayMillis)
    repeat(MiniPlayerAudioFadeSteps) { step ->
        delay(MiniPlayerAudioFadeMillis / MiniPlayerAudioFadeSteps)
        volume = (step + 1).toFloat() / MiniPlayerAudioFadeSteps
    }
    volume = 1f
}

// PERF_FIX: session-local blacklist for Media3 containers that failed during Home preview.
// This is intentionally memory-only and easy to remove with the diagnostic/performance preview changes.
private object UnsupportedHomePreviewCache {
    private val urlHashes = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())

    fun isUnsupported(url: String?): Boolean =
        !url.isNullOrBlank() && url.hashCode() in urlHashes

    fun markUnsupported(url: String) {
        urlHashes += url.hashCode()
    }
}

private const val TrendPreviewPosterDelayMillis = 4_000L
private const val TrendPreviewSegmentMillis = 8_000L
private const val HomePreviewFocusDebounceMillis = 450L
private const val FirstFrameWaitAttempts = 80
private const val PreviewFadeMillis = 900L
private const val PreviewCrossfadeMillis = 1_100L
private const val TrendPreviewFallbackDurationMillis = 60 * 60_000L
private const val ContinuePreviewLoopMillis = 20_000L
private const val ContinuePreviewOverlayMillis = 2_000L
private const val MiniPlayerAudioStartDelayMillis = 1_000L
private const val MiniPlayerAudioFadeMillis = 1_000L
private const val MiniPlayerAudioFadeSteps = 10
private val TrendPreviewPositions = listOf(0.10f, 0.25f, 0.45f, 0.65f, 0.80f)
