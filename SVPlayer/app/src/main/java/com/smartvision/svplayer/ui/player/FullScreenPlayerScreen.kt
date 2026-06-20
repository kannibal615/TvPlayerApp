package com.smartvision.svplayer.ui.player

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.repository.XtreamRepository
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class FullScreenPlayback(
    val streamId: Int,
    val title: String,
    val subtitle: String,
    val url: String,
    val fallbackUrl: String,
)

class FullScreenPlayerViewModel(
    streamId: Int,
    xtreamRepository: XtreamRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        xtreamRepository.getCachedLiveStream(streamId)?.let { stream ->
            val categoryName = xtreamRepository.getCachedCategories()
                .firstOrNull { it.id == stream.categoryId }
                ?.name
                ?: "Live TV"
            FullScreenPlayback(
                streamId = stream.streamId,
                title = stream.name.cleanTitle(),
                subtitle = categoryName,
                url = xtreamRepository.buildLiveStreamUrl(stream),
                fallbackUrl = xtreamRepository.buildLiveStreamFallbackUrl(stream.streamId),
            )
        } ?: FullScreenPlayback(
            streamId = streamId,
            title = "Chaine $streamId",
            subtitle = "Live TV",
            url = xtreamRepository.buildLiveStreamUrl(streamId),
            fallbackUrl = xtreamRepository.buildLiveStreamFallbackUrl(streamId),
        ),
    )
    val uiState: StateFlow<FullScreenPlayback> = _uiState
}

@Composable
fun FullScreenPlayerRoute(
    streamId: Int,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: FullScreenPlayerViewModel = viewModel(
        key = "fullscreen-live-$streamId",
        factory = viewModelFactory {
            FullScreenPlayerViewModel(
                streamId = streamId,
                xtreamRepository = container.xtreamRepository,
            )
        },
    )
    val playback by viewModel.uiState.collectAsStateWithLifecycle()
    FullScreenPlayerScreen(playback = playback, onBack = onBack)
}

@Composable
private fun FullScreenPlayerScreen(
    playback: FullScreenPlayback,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val latestUrl by rememberUpdatedState(playback.url)
    val latestFallbackUrl by rememberUpdatedState(playback.fallbackUrl)
    val playFocusRequester = remember { FocusRequester() }
    var overlayVisible by remember { mutableStateOf(true) }
    var overlayTick by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var buffering by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var fallbackTried by remember(playback.streamId) { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    fun showOverlay() {
        overlayVisible = true
        overlayTick += 1
    }

    BackHandler {
        if (overlayVisible) {
            overlayVisible = false
        } else {
            onBack()
        }
    }

    LaunchedEffect(playback.url) {
        fallbackTried = false
        errorText = null
        buffering = true
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(playback.url))
        player.prepare()
        player.playWhenReady = true
    }

    LaunchedEffect(overlayVisible, overlayTick) {
        if (overlayVisible) {
            delay(120)
            playFocusRequester.requestFocus()
            delay(4_800)
            overlayVisible = false
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                isPlaying = isPlayingValue
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_READY) {
                    errorText = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val fallback = latestFallbackUrl
                if (!fallbackTried && fallback.isNotBlank() && fallback != latestUrl) {
                    fallbackTried = true
                    errorText = null
                    buffering = true
                    player.stop()
                    player.clearMediaItems()
                    player.setMediaItem(MediaItem.fromUri(fallback))
                    player.prepare()
                    player.playWhenReady = true
                    return
                }

                buffering = false
                errorText = "Flux indisponible"
                showOverlay()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == AndroidKeyEvent.ACTION_DOWN &&
                    event.nativeKeyEvent.keyCode in overlayKeyCodes
                ) {
                    showOverlay()
                }
                false
            },
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    this.player = player
                    isFocusable = true
                    isFocusableInTouchMode = true
                    setOnKeyListener { _, keyCode, event ->
                        when {
                            keyCode == AndroidKeyEvent.KEYCODE_BACK && event.action == AndroidKeyEvent.ACTION_UP -> {
                                if (overlayVisible) {
                                    overlayVisible = false
                                } else {
                                    onBack()
                                }
                                true
                            }

                            event.action == AndroidKeyEvent.ACTION_DOWN && keyCode in overlayKeyCodes -> {
                                showOverlay()
                                false
                            }

                            else -> false
                        }
                    }
                    requestFocus()
                }
            },
            update = {
                it.player = player
                it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                it.setOnKeyListener { _, keyCode, event ->
                    when {
                        keyCode == AndroidKeyEvent.KEYCODE_BACK && event.action == AndroidKeyEvent.ACTION_UP -> {
                            if (overlayVisible) {
                                overlayVisible = false
                            } else {
                                onBack()
                            }
                            true
                        }

                        event.action == AndroidKeyEvent.ACTION_DOWN && keyCode in overlayKeyCodes -> {
                            showOverlay()
                            false
                        }

                        else -> false
                    }
                }
            },
            modifier = Modifier.matchParentSize(),
        )

        if (buffering) {
            CircularProgressIndicator(
                color = SmartVisionColors.Primary,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(46.dp),
            )
        }

        if (overlayVisible) {
            FullPlayerOverlay(
                playback = playback,
                isPlaying = isPlaying,
                errorText = errorText,
                playFocusRequester = playFocusRequester,
                onPlayPause = {
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                    showOverlay()
                },
                onReplay = {
                    player.play()
                    showOverlay()
                },
                onSeekBack = {
                    if (player.isCurrentMediaItemSeekable) {
                        player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
                    }
                    showOverlay()
                },
                onSeekForward = {
                    if (player.isCurrentMediaItemSeekable) {
                        player.seekTo(player.currentPosition + 10_000L)
                    }
                    showOverlay()
                },
                onNoOp = ::showOverlay,
            )
        }
    }
}

@Composable
private fun FullPlayerOverlay(
    playback: FullScreenPlayback,
    isPlaying: Boolean,
    errorText: String?,
    playFocusRequester: FocusRequester,
    onPlayPause: () -> Unit,
    onReplay: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onNoOp: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.42f),
                        Color.Transparent,
                        Color(0xF0010612),
                    ),
                ),
            ),
    ) {
        PlayerLogo(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 30.dp, top = 23.dp),
        )

        PlayerInfoCard(
            playback = playback,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 30.dp, bottom = 176.dp)
                .width(348.dp),
        )

        errorText?.let { message ->
            Text(
                text = message,
                color = Color.White,
                style = PlayerMetaStyle,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.66f))
                    .border(BorderStroke(1.dp, SmartVisionColors.Error), RoundedCornerShape(6.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }

        PlayerProgressBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 42.dp, end = 42.dp, bottom = 118.dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 30.dp, end = 30.dp, bottom = 31.dp)
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xD6071123))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(8.dp))
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerControlButton("Guide TV", Icons.Default.Menu, onNoOp)
            PlayerControlButton("Reprendre", Icons.Default.Replay, onReplay)
            PlayerControlButton("- 10 sec", Icons.Default.Replay10, onSeekBack)
            PlayerControlButton(
                label = if (isPlaying) "Pause" else "Lecture",
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                onClick = onPlayPause,
                focusRequester = playFocusRequester,
                primary = true,
                width = 92.dp,
                height = 64.dp,
                iconSize = 28.dp,
            )
            PlayerControlButton("+ 10 sec", Icons.Default.Forward10, onSeekForward)
            PlayerControlButton("Audio", Icons.Default.VolumeUp, onNoOp)
            PlayerControlButton("Sous-titres", Icons.Default.Subtitles, onNoOp)
            PlayerControlButton("Parametres", Icons.Default.Settings, onNoOp)
        }
    }
}

@Composable
private fun PlayerLogo(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = SmartVisionColors.Primary,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = buildAnnotatedString {
                append("Smart")
                withStyle(SpanStyle(color = SmartVisionColors.Primary)) {
                    append("Vision")
                }
            },
            color = Color.White,
            style = SmartVisionType.Label,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun PlayerInfoCard(
    playback: FullScreenPlayback,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xB4071123))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), RoundedCornerShape(6.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LivePill()
            Spacer(Modifier.width(10.dp))
            Text(
                text = playback.title,
                color = Color.White,
                style = PlayerTitleStyle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = playback.subtitle,
                color = SmartVisionColors.TextSecondary,
                style = PlayerMetaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Direct",
                color = SmartVisionColors.TextSecondary,
                style = PlayerMetaStyle,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            InfoPill("16+")
            InfoPill("HD")
            InfoPill("5.1")
        }
    }
}

@Composable
private fun LivePill() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(SmartVisionColors.Primary)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(10.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text("LIVE", color = Color.White, style = PlayerTinyStyle, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun InfoPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color.Black.copy(alpha = 0.36f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = Color.White, style = PlayerTinyStyle, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlayerProgressBar(modifier: Modifier = Modifier) {
    val now = remember {
        val start = Date()
        val end = Date(start.time + 60 * 60 * 1000L)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        format.format(start) to format.format(end)
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(now.first, color = Color.White, style = PlayerMetaStyle)
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.22f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.76f)
                    .fillMaxHeight()
                    .background(SmartVisionColors.Primary),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 140.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(SmartVisionColors.Primary),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(now.second, color = Color.White, style = PlayerMetaStyle)
    }
}

@Composable
private fun PlayerControlButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    primary: Boolean = false,
    width: Dp = 84.dp,
    height: Dp = 54.dp,
    iconSize: Dp = 23.dp,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(if (primary) 8.dp else 6.dp)
    val backgroundColor by animateColorAsState(
        targetValue = when {
            primary && focusState.isFocused -> SmartVisionColors.PrimaryDark
            primary -> Color(0xD9113E91)
            focusState.isFocused -> SmartVisionColors.SurfaceElevated
            else -> Color.Transparent
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "playerControlBackground",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> SmartVisionColors.FocusWhite
            primary -> SmartVisionColors.Primary
            else -> Color.Transparent
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "playerControlBorder",
    )

    Column(
        modifier = modifier
            .width(width)
            .height(height)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = if (primary) 1.08f else 1.04f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = if (primary) 8.dp else 6.dp,
            )
            .zIndex(if (focusState.isFocused) 3f else 0f)
            .clip(shape)
            .background(backgroundColor)
            .border(
                BorderStroke(
                    if (focusState.isFocused) 2.dp else 1.dp,
                    borderColor,
                ),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(PaddingValues(horizontal = 4.dp, vertical = 4.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = label,
            color = if (focusState.isFocused || primary) Color.White else SmartVisionColors.TextSecondary,
            style = PlayerTinyStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val overlayKeyCodes = setOf(
    AndroidKeyEvent.KEYCODE_DPAD_UP,
    AndroidKeyEvent.KEYCODE_DPAD_DOWN,
    AndroidKeyEvent.KEYCODE_DPAD_LEFT,
    AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
    AndroidKeyEvent.KEYCODE_DPAD_CENTER,
    AndroidKeyEvent.KEYCODE_ENTER,
)

private val PlayerTitleStyle = TextStyle(
    fontSize = 16.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.sp,
)

private val PlayerMetaStyle = TextStyle(
    fontSize = 10.sp,
    lineHeight = 13.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
)

private val PlayerTinyStyle = TextStyle(
    fontSize = 8.sp,
    lineHeight = 10.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
)

private fun String.cleanTitle(): String =
    replace(Regex("\\s+"), " ")
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" SD", "", ignoreCase = true)
        .trim()
        .ifBlank { "Live TV" }
