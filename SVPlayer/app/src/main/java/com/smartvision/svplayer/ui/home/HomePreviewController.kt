package com.smartvision.svplayer.ui.home

import android.content.Context
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomePreviewController(context: Context) {
    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null
    private val _activePreviewId = MutableStateFlow<String?>(null)
    val activePreviewId = _activePreviewId.asStateFlow()

    @MainThread
    fun play(previewId: String, url: String, startPositionMs: Long = 0L) {
        if (_activePreviewId.value == previewId && player?.isPlaying == true) return
        stop()
        val nextPlayer = ExoPlayer.Builder(appContext).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
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
    }

    @MainThread
    fun stop(previewId: String? = null) {
        if (previewId != null && _activePreviewId.value != previewId) return
        _activePreviewId.value = null
        player?.run {
            playWhenReady = false
            stop()
            clearMediaItems()
            release()
        }
        player = null
    }

    @MainThread
    fun playerFor(previewId: String): Player? =
        player.takeIf { _activePreviewId.value == previewId }
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
            controller.stop()
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
        modifier = modifier,
    )
}
