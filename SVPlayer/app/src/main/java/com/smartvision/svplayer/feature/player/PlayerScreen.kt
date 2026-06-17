package com.smartvision.svplayer.feature.player

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.designsystem.FocusableButton
import com.smartvision.svplayer.core.designsystem.SVColors
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.domain.model.PlaybackKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerRoute(
    kind: PlaybackKind,
    id: String,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: PlayerViewModel = viewModel(
        key = "player-${kind.routeName}-$id",
        factory = viewModelFactory {
            PlayerViewModel(
                kind = kind,
                id = id,
                buildPlaybackRequest = container.buildPlaybackRequest,
                savePlaybackProgress = container.savePlaybackProgress,
            )
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    PlayerScreen(state = state, onRetry = viewModel::load, onBack = onBack, saveProgress = viewModel::saveProgress)
}

@Composable
private fun PlayerScreen(
    state: PlayerUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    saveProgress: (Long, Long) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.loading -> Text("Chargement du player...", color = Color.White, style = MaterialTheme.typography.titleLarge)
            state.error != null -> PlayerError(message = state.error, onRetry = onRetry, onBack = onBack)
            state.request != null -> ExoPlayerSurface(
                request = state.request,
                onBack = onBack,
                saveProgress = saveProgress,
            )
        }
    }
}

@Composable
private fun ExoPlayerSurface(
    request: com.smartvision.svplayer.domain.model.PlaybackRequest,
    onBack: () -> Unit,
    saveProgress: (Long, Long) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var overlayVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var resizeIndex by remember { mutableIntStateOf(0) }
    var failedRetries by remember { mutableIntStateOf(0) }
    val retryDelays = listOf(2_000L, 5_000L, 10_000L)
    val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    )

    val player = remember(request.url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(request.url))
            prepare()
            if (request.resumePositionMs > 0L) seekTo(request.resumePositionMs)
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                isPlaying = isPlayingValue
            }

            override fun onPlayerError(error: PlaybackException) {
                val delayMs = retryDelays.getOrNull(failedRetries)
                if (delayMs != null) {
                    failedRetries += 1
                    scope.launch {
                        delay(delayMs)
                        player.prepare()
                        player.play()
                    }
                } else {
                    overlayVisible = true
                }
            }
        }
        player.addListener(listener)
        onDispose {
            if (request.kind != PlaybackKind.Live) {
                saveProgress(player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
            }
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(overlayVisible) {
        if (overlayVisible) {
            delay(5_000)
            overlayVisible = false
        }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                useController = false
                this.player = player
                resizeMode = resizeModes[resizeIndex]
            }
        },
        update = {
            it.player = player
            it.resizeMode = resizeModes[resizeIndex]
        },
        modifier = Modifier
            .fillMaxSize()
            .focusable()
            .onKeyEvent {
                if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP &&
                    (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)
                ) {
                    overlayVisible = !overlayVisible
                    true
                } else {
                    false
                }
            },
    )

    if (overlayVisible) {
        PlayerOverlay(
            title = request.title,
            subtitle = request.subtitle,
            isPlaying = isPlaying,
            failedRetries = failedRetries,
            onPlayPause = {
                if (player.isPlaying) player.pause() else player.play()
                overlayVisible = true
            },
            onRatio = {
                resizeIndex = (resizeIndex + 1) % resizeModes.size
                overlayVisible = true
            },
            onRetry = {
                failedRetries = 0
                player.prepare()
                player.play()
                overlayVisible = true
            },
            onBack = onBack,
        )
    }
}

@Composable
private fun PlayerOverlay(
    title: String,
    subtitle: String,
    isPlaying: Boolean,
    failedRetries: Int,
    onPlayPause: () -> Unit,
    onRatio: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(48.dp),
        ) {
            Text(title, color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Text(subtitle, color = SVColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.width(1.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 22.dp)) {
                FocusableButton(
                    text = if (isPlaying) "Pause" else "Lecture",
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    onClick = onPlayPause,
                    modifier = Modifier.width(150.dp),
                )
                FocusableButton("Favori", onClick = {}, icon = Icons.Default.Favorite, accent = SVColors.Purple, modifier = Modifier.width(140.dp))
                FocusableButton("Ratio", onClick = onRatio, icon = Icons.Default.AspectRatio, accent = SVColors.Blue, modifier = Modifier.width(130.dp))
                if (failedRetries >= 3) {
                    FocusableButton("Reessayer", onClick = onRetry, icon = Icons.Default.Refresh, accent = SVColors.Danger, modifier = Modifier.width(150.dp))
                }
                FocusableButton("Retour", onClick = onBack, icon = Icons.Default.ArrowBack, modifier = Modifier.width(130.dp))
            }
        }
    }
}

@Composable
private fun PlayerError(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(message, color = Color.White, style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FocusableButton("Reessayer", onClick = onRetry, icon = Icons.Default.Refresh)
            FocusableButton("Retour", onClick = onBack, icon = Icons.Default.ArrowBack)
        }
    }
}
