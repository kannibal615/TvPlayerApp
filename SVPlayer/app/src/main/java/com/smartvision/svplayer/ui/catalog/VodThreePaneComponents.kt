package com.smartvision.svplayer.ui.catalog

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Theaters
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class VodPreviewContent(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val backdropUrl: String?,
    val streamUrl: String?,
    val durationLabel: String?,
    val sideLabel: String?,
    val year: String?,
    val rating: String?,
    val genre: String?,
    val plot: String?,
    val creditLabel: String?,
    val cast: String?,
    val isFavorite: Boolean,
    val loading: Boolean = false,
)

@Composable
fun VodContentRow(
    title: String,
    subtitle: String,
    sideLabel: String,
    imageUrl: String?,
    fallbackText: String,
    selected: Boolean,
    focusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val active = selected || focusState.isFocused
    val shape = RoundedCornerShape(MediaCatalogDimens.ItemRadius)
    val focusStyle = LocalTvFocusStyle.current
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> focusStyle.accent
            selected -> SmartVisionColors.Primary
            else -> SmartVisionColors.Border
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "vodContentRowBorder",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .then(
                if (rightFocusRequester != null) {
                    Modifier.focusProperties { right = rightFocusRequester }
                } else {
                    Modifier
                },
            )
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.025f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = MediaCatalogDimens.ItemRadius,
            )
            .onFocusChanged { focus ->
                if (focus.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .clip(shape)
            .background(
                if (active) {
                    Brush.horizontalGradient(
                        listOf(
                            SmartVisionColors.PrimaryDark.copy(alpha = 0.70f),
                            SmartVisionColors.SurfaceElevated.copy(alpha = 0.94f),
                        ),
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            SmartVisionColors.SurfaceElevated.copy(alpha = 0.70f),
                            SmartVisionColors.Surface.copy(alpha = 0.50f),
                        ),
                    )
                },
            )
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else SmartVisionDimensions.PanelBorder,
                    borderColor,
                ),
                shape,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CatalogThumb(
            imageUrl = imageUrl,
            fallbackText = fallbackText,
            modifier = Modifier
                .width(82.dp)
                .aspectRatio(16f / 9f),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SmartVisionColors.TextPrimary,
                style = CatalogItemTitleStyle,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = SmartVisionColors.TextSecondary,
                style = CatalogMetaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = sideLabel,
            color = SmartVisionColors.TextSecondary,
            style = CatalogMetaStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(76.dp),
        )
    }
}

@Composable
fun VodPreviewPanel(
    title: String,
    content: VodPreviewContent?,
    playFocusRequester: FocusRequester,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    onFavorite: () -> Unit,
    onDeleteHistory: () -> Unit,
    modifier: Modifier = Modifier,
    showDeleteHistory: Boolean = false,
) {
    MediaCatalogPanel(
        title = title,
        modifier = modifier,
        trailing = {
            if (content != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PreviewIconButton(
                        icon = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        focusRequester = playFocusRequester,
                        onClick = onPlay,
                        primary = true,
                    )
                    PreviewIconButton(
                        icon = Icons.Default.Info,
                        contentDescription = "Details",
                        onClick = onDetails,
                    )
                    if (showDeleteHistory) {
                        PreviewIconButton(
                            icon = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            onClick = onDeleteHistory,
                            danger = true,
                        )
                    } else {
                        PreviewIconButton(
                            icon = if (content.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favori",
                            onClick = onFavorite,
                            selected = content.isFavorite,
                        )
                    }
                }
            }
        },
    ) {
        if (content == null) {
            CatalogEmpty(
                title = "Aucun contenu selectionne",
                subtitle = "Selectionnez une ligne pour afficher l'apercu.",
                modifier = Modifier.fillMaxSize(),
            )
            return@MediaCatalogPanel
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 10.dp),
        ) {
            item(key = "${content.id}-player") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(MediaCatalogDimens.ItemRadius))
                        .background(Color.Black)
                        .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(MediaCatalogDimens.ItemRadius)),
                ) {
                    SegmentedVodMiniPlayer(
                        content = content,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }

            item(key = "${content.id}-details") {
                Row(modifier = Modifier.fillMaxWidth()) {
                    CatalogPosterFrame(
                        imageUrl = content.imageUrl,
                        title = content.title,
                        badge = content.durationLabel ?: content.sideLabel ?: "VOD",
                        modifier = Modifier
                            .width(118.dp)
                            .aspectRatio(2f / 3f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = content.title,
                            color = SmartVisionColors.TextPrimary,
                            style = CatalogPreviewTitleStyle,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = listOfNotNull(content.year, content.durationLabel, content.rating?.let { "$it/10" })
                                .joinToString("  |  ")
                                .ifBlank { content.subtitle },
                            color = SmartVisionColors.TextSecondary,
                            style = CatalogMetaStyle,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!content.genre.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = content.genre,
                                color = SmartVisionColors.Primary,
                                style = CatalogMetaStyle,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (content.loading) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Chargement de l'episode...",
                                color = SmartVisionColors.TextSecondary,
                                style = CatalogMetaStyle,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            item(key = "${content.id}-plot") {
                Text(
                    text = content.plot?.takeIf { it.isNotBlank() } ?: "Aucun resume disponible.",
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Body,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            content.creditLabel?.takeIf { it.isNotBlank() }?.let { creditLabel ->
                item(key = "${content.id}-credit") {
                    PreviewInfoLine(label = creditLabel.substringBefore(':'), value = creditLabel.substringAfter(':', creditLabel))
                }
            }
            content.cast?.takeIf { it.isNotBlank() }?.let { cast ->
                item(key = "${content.id}-cast") {
                    PreviewInfoLine(label = "Cast", value = cast)
                }
            }
        }
    }
}

@Composable
private fun PreviewIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    primary: Boolean = false,
    selected: Boolean = false,
    danger: Boolean = false,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(5.dp)
    val focusStyle = LocalTvFocusStyle.current
    val active = focusState.isFocused || selected || primary
    val background = when {
        danger && focusState.isFocused -> SmartVisionColors.Error.copy(alpha = 0.34f)
        primary -> SmartVisionColors.Primary.copy(alpha = 0.86f)
        active -> SmartVisionColors.SurfaceElevated
        else -> SmartVisionColors.Surface.copy(alpha = 0.70f)
    }
    val borderColor = when {
        danger && focusState.isFocused -> SmartVisionColors.Error
        focusState.isFocused -> focusStyle.accent
        selected -> SmartVisionColors.Primary
        else -> SmartVisionColors.Border
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.08f,
                glowColor = if (danger) SmartVisionColors.Error else SmartVisionColors.Primary,
                cornerRadius = 5.dp,
            )
            .clip(shape)
            .background(background)
            .border(BorderStroke(if (focusState.isFocused) focusStyle.borderWidth else 1.dp, borderColor), shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SegmentedVodMiniPlayer(
    content: VodPreviewContent,
    modifier: Modifier = Modifier,
) {
    val streamUrl = content.streamUrl
    val posterUrl = content.backdropUrl ?: content.imageUrl
    if (streamUrl.isNullOrBlank()) {
        LandscapePosterFallback(
            imageUrl = posterUrl,
            title = content.title,
            message = "Preview indisponible",
            modifier = modifier,
        )
        return
    }

    val context = LocalContext.current
    val audioScope = rememberCoroutineScope()
    var buffering by remember(content.id) { mutableStateOf(true) }
    var showPoster by remember(content.id) { mutableStateOf(false) }
    var errorText by remember(content.id) { mutableStateOf<String?>(null) }
    var readyNonce by remember(content.id) { mutableIntStateOf(0) }
    var sequenceStarted by remember(content.id) { mutableStateOf(false) }
    val volumeFadeJob = remember { arrayOfNulls<Job>(1) }
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    fun restartPreviewAudioFade() {
        volumeFadeJob[0]?.cancel()
        player.volume = 0f
        volumeFadeJob[0] = audioScope.launch { player.fadeInVodMiniPlayerVolume() }
    }

    LaunchedEffect(content.id, streamUrl) {
        volumeFadeJob[0]?.cancel()
        showPoster = false
        errorText = null
        buffering = true
        readyNonce = 0
        sequenceStarted = false
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
    }

    LaunchedEffect(content.id, readyNonce) {
        if (readyNonce == 0 || errorText != null) return@LaunchedEffect
        var durationMs = player.duration
        var attempts = 0
        while ((durationMs <= 0 || durationMs == C.TIME_UNSET) && attempts < 20) {
            delay(250)
            durationMs = player.duration
            attempts += 1
        }

        val validDuration = durationMs.takeIf { it > 0 && it != C.TIME_UNSET }
        if (validDuration == null) {
            buffering = false
            player.play()
            restartPreviewAudioFade()
            delay(VodPreviewSegmentMillis)
            player.pause()
            player.volume = 0f
            showPoster = true
            return@LaunchedEffect
        }

        VodPreviewPercents.forEach { percent ->
            player.seekTo((validDuration * percent / 100f).toLong().coerceIn(0L, validDuration))
            player.play()
            restartPreviewAudioFade()
            buffering = false
            delay(VodPreviewSegmentMillis)
        }
        volumeFadeJob[0]?.cancel()
        player.pause()
        player.volume = 0f
        showPoster = true
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_READY) {
                    errorText = null
                    if (!sequenceStarted) {
                        sequenceStarted = true
                        readyNonce += 1
                    }
                }
                if (playbackState == Player.STATE_ENDED) {
                    showPoster = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                volumeFadeJob[0]?.cancel()
                buffering = false
                showPoster = true
                errorText = "Preview indisponible"
            }
        }
        player.addListener(listener)
        onDispose {
            volumeFadeJob[0]?.cancel()
            player.volume = 0f
            player.removeListener(listener)
            player.release()
        }
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    this.player = player
                }
            },
            update = {
                it.player = player
                it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            },
            modifier = Modifier.matchParentSize(),
        )

        if (showPoster || errorText != null) {
            LandscapePosterFallback(
                imageUrl = posterUrl,
                title = content.title,
                message = errorText,
                modifier = Modifier.matchParentSize(),
            )
        }

        if (buffering && !showPoster && errorText == null) {
            CircularProgressIndicator(
                color = SmartVisionColors.Primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun LandscapePosterFallback(
    imageUrl: String?,
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.22f)),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Theaters,
                contentDescription = null,
                tint = SmartVisionColors.Primary,
                modifier = Modifier.size(52.dp),
            )
        }
        message?.let {
            Text(
                text = it,
                color = Color.White,
                style = CatalogMetaStyle,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun PreviewInfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label:",
            color = SmartVisionColors.Primary,
            style = CatalogMetaStyle,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.width(70.dp),
        )
        Text(
            text = value,
            color = SmartVisionColors.TextSecondary,
            style = CatalogMetaStyle,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private suspend fun ExoPlayer.fadeInVodMiniPlayerVolume() {
    delay(VodMiniPlayerAudioStartDelayMillis)
    repeat(VodMiniPlayerAudioFadeSteps) { step ->
        delay(VodMiniPlayerAudioFadeMillis / VodMiniPlayerAudioFadeSteps)
        volume = (step + 1).toFloat() / VodMiniPlayerAudioFadeSteps
    }
    volume = 1f
}

private val VodPreviewPercents = listOf(10, 30, 50, 80)
private const val VodPreviewSegmentMillis = 15_000L
private const val VodMiniPlayerAudioStartDelayMillis = 1_000L
private const val VodMiniPlayerAudioFadeMillis = 1_000L
private const val VodMiniPlayerAudioFadeSteps = 10
