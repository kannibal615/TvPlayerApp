package com.smartvision.svplayer.ui.home

import android.content.Context
import android.view.LayoutInflater
import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomePreviewController(context: Context) {
    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null
    private val _activePreviewId = MutableStateFlow<String?>(null)
    val activePreviewId = _activePreviewId.asStateFlow()
    private val _firstFramePreviewId = MutableStateFlow<String?>(null)
    val firstFramePreviewId = _firstFramePreviewId.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var audioFadeJob: Job? = null
    private var segmentJob: Job? = null

    @MainThread
    fun play(
        previewId: String,
        url: String,
        startPositionMs: Long = 0L,
        mode: HomePreviewMode = HomePreviewMode.None,
    ) {
        if (_activePreviewId.value == previewId && player?.isPlaying == true) return
        stop()
        val nextPlayer = ExoPlayer.Builder(appContext).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    if (_activePreviewId.value == previewId) {
                        _firstFramePreviewId.value = previewId
                        startAudioFade()
                    }
                }
                override fun onPlayerError(error: PlaybackException) {
                    if (_activePreviewId.value == previewId) stop(previewId)
                }
            })
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            if (startPositionMs > 0L) seekTo(startPositionMs)
            playWhenReady = true
        }
        player = nextPlayer
        _activePreviewId.value = previewId
        _firstFramePreviewId.value = null
        if (mode == HomePreviewMode.TrendSegments) startTrendingSegments(nextPlayer, previewId)
    }

    @MainThread
    fun stop(previewId: String? = null) {
        if (previewId != null && _activePreviewId.value != previewId) return
        _activePreviewId.value = null
        _firstFramePreviewId.value = null
        audioFadeJob?.cancel()
        segmentJob?.cancel()
        audioFadeJob = null
        segmentJob = null
        player?.run {
            playWhenReady = false
            stop()
            clearMediaItems()
            release()
        }
        player = null
    }

    @MainThread
    fun release() {
        stop()
        scope.cancel()
    }

    @MainThread
    fun playerFor(previewId: String): Player? =
        player.takeIf { _activePreviewId.value == previewId }

    private fun startAudioFade() {
        audioFadeJob?.cancel()
        val current = player ?: return
        current.volume = 0f
        audioFadeJob = scope.launch {
            repeat(20) { step ->
                delay(100)
                if (player !== current) return@launch
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
            while (_activePreviewId.value == previewId && player === current) {
                val start = (duration * ratios[index]).toLong().coerceIn(0L, (duration - 1_000L).coerceAtLeast(0L))
                current.seekTo(start)
                val available = (duration - start).coerceAtLeast(0L)
                delay(minOf(25_000L, available).coerceAtLeast(1_000L))
                index = (index + 1) % ratios.size
            }
        }
    }
}

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
    val firstFramePreviewId by controller.firstFramePreviewId.collectAsStateWithLifecycle()
    if (activePreviewId != previewId) return
    AndroidView(
        factory = { context ->
            (LayoutInflater.from(context).inflate(R.layout.home_preview_player_view, null, false) as PlayerView).apply {
                player = controller.playerFor(previewId)
            }
        },
        update = { view -> view.player = controller.playerFor(previewId) },
        modifier = modifier.alpha(if (firstFramePreviewId == previewId) 1f else 0f),
    )
}
