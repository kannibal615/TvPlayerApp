package com.smartvision.svplayer.ui.home

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.smartvision.svplayer.R
import com.smartvision.svplayer.data.mock.HomePreviewMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomePreviewController(context: Context) {
    private val appContext = context.applicationContext
    private val _activePreviewId = MutableStateFlow<String?>(null)
    val activePreviewId = _activePreviewId.asStateFlow()
    private val _firstFramePreviewId = MutableStateFlow<String?>(null)
    val firstFramePreviewId = _firstFramePreviewId.asStateFlow()
    private var scope = newPreviewScope()
    private var audioFadeJob: Job? = null
    private var segmentJob: Job? = null
    private var activeUrlFingerprint: Int? = null
    private val player = ExoPlayer.Builder(appContext).build().apply {
        volume = 0f
        repeatMode = Player.REPEAT_MODE_OFF
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(
                    HomePreviewLogTag,
                    "state preview=${_activePreviewId.value.orEmpty()} state=$playbackState playing=$isPlaying",
                )
            }

            override fun onRenderedFirstFrame() {
                val previewId = _activePreviewId.value ?: return
                _firstFramePreviewId.value = previewId
                Log.d(HomePreviewLogTag, "first_frame preview=$previewId")
                startAudioFade()
            }

            override fun onPlayerError(error: PlaybackException) {
                val previewId = _activePreviewId.value ?: return
                Log.w(
                    HomePreviewLogTag,
                    "error preview=$previewId code=${error.errorCode} type=${error.javaClass.simpleName}",
                )
                stop(previewId)
            }
        })
    }

    @MainThread
    fun play(
        previewId: String,
        url: String,
        startPositionMs: Long = 0L,
        mode: HomePreviewMode = HomePreviewMode.None,
    ) {
        if (url.isBlank()) {
            Log.w(HomePreviewLogTag, "ignored_empty_url preview=$previewId mode=${mode.name}")
            stop(previewId)
            return
        }
        if (!scope.isActive) scope = newPreviewScope()
        val urlFingerprint = url.hashCode()
        if (
            _activePreviewId.value == previewId &&
            activeUrlFingerprint == urlFingerprint &&
            player.mediaItemCount > 0
        ) {
            player.playWhenReady = true
            Log.d(HomePreviewLogTag, "resume preview=$previewId mode=${mode.name} state=${player.playbackState}")
            return
        }
        stop()
        _activePreviewId.value = previewId
        _firstFramePreviewId.value = null
        activeUrlFingerprint = urlFingerprint
        player.volume = 0f
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        if (startPositionMs > 0L) player.seekTo(startPositionMs)
        player.playWhenReady = true
        Log.d(HomePreviewLogTag, "prepare preview=$previewId mode=${mode.name} resume=${startPositionMs > 0L}")
        if (mode == HomePreviewMode.TrendSegments) startTrendingSegments(player, previewId)
    }

    @MainThread
    fun stop(previewId: String? = null) {
        if (previewId != null && _activePreviewId.value != previewId) return
        _activePreviewId.value = null
        _firstFramePreviewId.value = null
        activeUrlFingerprint = null
        audioFadeJob?.cancel()
        segmentJob?.cancel()
        audioFadeJob = null
        segmentJob = null
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
    }

    @MainThread
    fun release() {
        stop()
        player.release()
        scope.cancel()
    }

    @MainThread
    fun playerFor(previewId: String): Player? =
        player.takeIf { _activePreviewId.value == previewId }

    private fun startAudioFade() {
        audioFadeJob?.cancel()
        val current = player
        current.volume = 0f
        audioFadeJob = scope.launch {
            repeat(20) { step ->
                delay(100)
                if (_activePreviewId.value == null) return@launch
                current.volume = (step + 1) / 20f
            }
        }
    }

    private fun startTrendingSegments(current: ExoPlayer, previewId: String) {
        segmentJob?.cancel()
        segmentJob = scope.launch {
            while (current.duration <= 0L || current.duration == androidx.media3.common.C.TIME_UNSET) {
                if (_activePreviewId.value != previewId) return@launch
                delay(150)
            }
            val duration = current.duration
            val ratios = doubleArrayOf(0.15, 0.35, 0.55, 0.70, 0.90)
            var index = 0
            while (_activePreviewId.value == previewId) {
                val start = (duration * ratios[index]).toLong().coerceIn(0L, (duration - 1_000L).coerceAtLeast(0L))
                current.seekTo(start)
                val available = (duration - start).coerceAtLeast(0L)
                delay(minOf(25_000L, available).coerceAtLeast(1_000L))
                index = (index + 1) % ratios.size
            }
        }
    }

    private fun newPreviewScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}

private const val HomePreviewLogTag = "SVHomePreview"

internal fun homePreviewSessionId(namespace: String, contentId: String): String =
    "${namespace.trim().ifBlank { "home" }}:$contentId"

@Composable
fun rememberHomePreviewController(context: Context): HomePreviewController {
    val controller = remember(context.applicationContext) { HomePreviewController(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) controller.stop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.release()
        }
    }
    return controller
}

@Composable
fun HomePreviewSurface(
    controller: HomePreviewController,
    previewId: String,
    modifier: Modifier = Modifier,
) {
    val activePreviewId by controller.activePreviewId.collectAsStateWithLifecycle()
    if (activePreviewId != previewId) return
    AndroidView(
        factory = { context ->
            (LayoutInflater.from(context).inflate(R.layout.home_preview_player_view, null, false) as PlayerView).apply {
                player = controller.playerFor(previewId)
            }
        },
        update = { view -> view.player = controller.playerFor(previewId) },
        // SurfaceView alpha transitions are unreliable on several Fire TV chipsets:
        // decoding succeeds but the surface can remain transparent after first frame.
        // Poster/overlay visibility is still driven by firstFramePreviewId in the card.
        modifier = modifier,
    )
}
