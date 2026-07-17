package com.smartvision.svplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.util.Locale

internal val VodProgressStart = Color(0xFF009DFF)
internal val VodProgressEnd = Color(0xFF12C8FF)
internal val VodTrackColor = Color(0xFF393C40).copy(alpha = 0.72f)
internal val VodFocusBlue = Color(0xFF1687FF)

internal const val VodControlBrightness = 0
internal const val VodControlPrevious = 1
internal const val VodControlSeekBack = 2
internal const val VodControlPlayPause = 3
internal const val VodControlSeekForward = 4
internal const val VodControlNext = 5
internal const val VodControlExit = 6
internal const val VodControlRestart = 7
internal const val VodControlSeriesDetails = 8

internal fun vodEnabledControlOrder(
    hasPrevious: Boolean,
    canSeek: Boolean,
    hasNext: Boolean,
    showSeriesDetails: Boolean,
): List<Int> = buildList {
    add(VodControlBrightness)
    add(VodControlRestart)
    if (hasPrevious) add(VodControlPrevious)
    if (canSeek) add(VodControlSeekBack)
    add(VodControlPlayPause)
    if (canSeek) add(VodControlSeekForward)
    if (hasNext) add(VodControlNext)
    if (showSeriesDetails) add(VodControlSeriesDetails)
    add(VodControlExit)
}

internal fun referencePlayerGradient(): Brush = Brush.verticalGradient(
    0.00f to Color.Transparent,
    0.54f to Color.Transparent,
    0.66f to Color.Black.copy(alpha = 0.18f),
    0.79f to Color.Black.copy(alpha = 0.62f),
    0.91f to Color.Black.copy(alpha = 0.88f),
    1.00f to Color.Black.copy(alpha = 0.97f),
)

/**
 * Movie/series fullscreen controls calibrated from the official 1680 x 945 reference.
 * Every position is a viewport ratio so the geometry remains identical on 16:9 TV sizes.
 */
@Composable
internal fun VodFullscreenControlsOverlay(
    title: String,
    subtitle: String?,
    isPlaying: Boolean,
    errorText: String?,
    positionMs: Long,
    durationMs: Long,
    canSeek: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    showSeriesDetails: Boolean,
    seriesDetailsLabel: String,
    focusedControlIndex: Int,
    progressFocused: Boolean,
    onFocusedControlChange: (Int) -> Unit,
    onProgressFocused: () -> Unit,
    onSeekBy: (Long) -> Unit,
    playFocusRequester: FocusRequester,
    brightnessFocusRequester: FocusRequester,
    progressFocusRequester: FocusRequester,
    brightnessMode: Boolean,
    brightnessValue: Float,
    onOpenBrightness: () -> Unit,
    onChangeBrightness: (Float) -> Unit,
    onCloseBrightness: () -> Unit,
    onPlayPrevious: () -> Unit,
    onSeekBack: () -> Unit,
    onPlayPause: () -> Unit,
    onRestart: () -> Unit,
    onSeekForward: () -> Unit,
    onPlayNext: () -> Unit,
    onOpenSeriesDetails: () -> Unit,
    onExitFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val previousFocusRequester = remember { FocusRequester() }
    val restartFocusRequester = remember { FocusRequester() }
    val seekBackFocusRequester = remember { FocusRequester() }
    val seekForwardFocusRequester = remember { FocusRequester() }
    val nextFocusRequester = remember { FocusRequester() }
    val seriesDetailsFocusRequester = remember { FocusRequester() }
    val exitFocusRequester = remember { FocusRequester() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                referencePlayerGradient(),
            ),
    ) {
        val density = LocalDensity.current
        val titleFontSize = with(density) { (maxHeight * 0.046f).toSp() }
        val titleLineHeight = with(density) { (maxHeight * 0.056f).toSp() }
        val subtitleFontSize = with(density) { (maxHeight * 0.030f).toSp() }
        val subtitleLineHeight = with(density) { (maxHeight * 0.038f).toSp() }
        val subtitleOffset = maxHeight * 0.075f
        val timeFontSize = with(density) { (maxHeight * 0.021f).toSp() }
        val timeLineHeight = with(density) { (maxHeight * 0.027f).toSp() }
        val textShadow = Shadow(
            color = Color.Black.copy(alpha = 0.90f),
            blurRadius = with(density) { (maxHeight * 0.006f).toPx() },
        )

        Box(
            modifier = Modifier
                .offset(x = maxWidth * 0.108f, y = maxHeight * 0.714f)
                .width(maxWidth * 0.70f)
                .height(maxHeight * 0.106f),
        ) {
            Text(
                text = title.uppercase(Locale.getDefault()),
                color = Color.White,
                style = TextStyle(
                    fontSize = titleFontSize,
                    lineHeight = titleLineHeight,
                    fontWeight = FontWeight.Bold,
                    shadow = textShadow,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.94f),
                    style = TextStyle(
                        fontSize = subtitleFontSize,
                        lineHeight = subtitleLineHeight,
                        fontWeight = FontWeight.Normal,
                        shadow = textShadow,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.offset(y = subtitleOffset),
                )
            }
        }

        VodReferenceProgressBar(
            positionMs = positionMs,
            durationMs = durationMs,
            timeStyle = TextStyle(
                fontSize = timeFontSize,
                lineHeight = timeLineHeight,
                fontWeight = FontWeight.Normal,
                shadow = textShadow,
            ),
            trackHeight = maxHeight * 0.0043f,
            thumbSize = maxHeight * 0.0145f,
            elapsedWidth = maxWidth * 0.044f,
            endGap = maxWidth * 0.018f,
            durationWidth = maxWidth * 0.057f,
            enabled = canSeek,
            focused = progressFocused,
            focusRequester = progressFocusRequester,
            downFocusRequester = playFocusRequester,
            onFocused = onProgressFocused,
            onSeekBy = onSeekBy,
            modifier = Modifier
                .offset(y = maxHeight * 0.837f)
                .fillMaxWidth()
                .height(maxHeight * 0.058f)
                .padding(start = maxWidth * 0.0605f, end = maxWidth * 0.060f),
        )

        val controlHitSize = maxHeight * 0.064f
        val normalIconSize = maxHeight * 0.032f
        val playIconSize = maxHeight * 0.047f
        val controlsTop = maxHeight * 0.894f

        if (brightnessMode) {
            VodInlineBrightnessSlider(
                value = brightnessValue,
                focusRequester = brightnessFocusRequester,
                onChange = onChangeBrightness,
                onClose = onCloseBrightness,
                modifier = Modifier
                    .offset(x = maxWidth * 0.0545f, y = controlsTop)
                    .width(maxWidth * 0.22f)
                    .height(controlHitSize),
            )
        } else {
            VodFocusIconButton(
                icon = Icons.Outlined.WbSunny,
                contentDescription = "Brightness",
                focusRequester = brightnessFocusRequester,
                leftFocusRequester = exitFocusRequester,
                rightFocusRequester = restartFocusRequester,
                upFocusRequester = progressFocusRequester,
                onClick = onOpenBrightness,
                hitSize = controlHitSize,
                iconSize = normalIconSize,
                modifier = Modifier.offset(x = maxWidth * 0.0545f, y = controlsTop),
                forceFocused = focusedControlIndex == VodControlBrightness,
                onFocused = { onFocusedControlChange(VodControlBrightness) },
            )
        }

        VodFocusIconButton(
            icon = Icons.Default.RestartAlt,
            contentDescription = "Restart from beginning",
            focusRequester = restartFocusRequester,
            leftFocusRequester = brightnessFocusRequester,
            rightFocusRequester = if (hasPrevious) previousFocusRequester else seekBackFocusRequester,
            upFocusRequester = progressFocusRequester,
            onClick = onRestart,
            hitSize = controlHitSize,
            iconSize = normalIconSize,
            forceFocused = focusedControlIndex == VodControlRestart,
            onFocused = { onFocusedControlChange(VodControlRestart) },
            modifier = Modifier.offset(
                x = maxWidth * 0.263f - controlHitSize / 2f,
                y = controlsTop,
            ),
        )

        VodFocusIconButton(
            icon = Icons.Default.SkipPrevious,
            contentDescription = "Previous",
            focusRequester = previousFocusRequester,
            leftFocusRequester = restartFocusRequester,
            rightFocusRequester = seekBackFocusRequester,
            upFocusRequester = progressFocusRequester,
            onClick = onPlayPrevious,
            enabled = hasPrevious,
            hitSize = controlHitSize,
            iconSize = normalIconSize,
            forceFocused = focusedControlIndex == VodControlPrevious,
            onFocused = { onFocusedControlChange(VodControlPrevious) },
            modifier = Modifier.offset(
                x = maxWidth * 0.338f - controlHitSize / 2f,
                y = controlsTop,
            ),
        )
        VodFocusIconButton(
            icon = Icons.Default.Replay10,
            contentDescription = "Back 10 seconds",
            focusRequester = seekBackFocusRequester,
            leftFocusRequester = if (hasPrevious) previousFocusRequester else restartFocusRequester,
            rightFocusRequester = playFocusRequester,
            upFocusRequester = progressFocusRequester,
            onClick = onSeekBack,
            hitSize = controlHitSize,
            iconSize = normalIconSize,
            forceFocused = focusedControlIndex == VodControlSeekBack,
            onFocused = { onFocusedControlChange(VodControlSeekBack) },
            modifier = Modifier.offset(
                x = maxWidth * 0.413f - controlHitSize / 2f,
                y = controlsTop,
            ),
        )
        VodFocusIconButton(
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            focusRequester = playFocusRequester,
            leftFocusRequester = seekBackFocusRequester,
            rightFocusRequester = seekForwardFocusRequester,
            upFocusRequester = progressFocusRequester,
            onClick = onPlayPause,
            hitSize = controlHitSize,
            iconSize = playIconSize,
            prominent = true,
            forceFocused = focusedControlIndex == VodControlPlayPause,
            onFocused = { onFocusedControlChange(VodControlPlayPause) },
            modifier = Modifier.offset(
                x = maxWidth * 0.498f - controlHitSize / 2f,
                y = controlsTop,
            ),
        )
        VodFocusIconButton(
            icon = Icons.Default.Forward10,
            contentDescription = "Forward 10 seconds",
            focusRequester = seekForwardFocusRequester,
            leftFocusRequester = playFocusRequester,
            rightFocusRequester = nextFocusRequester,
            upFocusRequester = progressFocusRequester,
            onClick = onSeekForward,
            enabled = canSeek,
            hitSize = controlHitSize,
            iconSize = normalIconSize,
            forceFocused = focusedControlIndex == VodControlSeekForward,
            onFocused = { onFocusedControlChange(VodControlSeekForward) },
            modifier = Modifier.offset(
                x = maxWidth * 0.583f - controlHitSize / 2f,
                y = controlsTop,
            ),
        )
        VodFocusIconButton(
            icon = Icons.Default.SkipNext,
            contentDescription = "Next",
            focusRequester = nextFocusRequester,
            leftFocusRequester = seekForwardFocusRequester,
            rightFocusRequester = if (showSeriesDetails) seriesDetailsFocusRequester else exitFocusRequester,
            upFocusRequester = progressFocusRequester,
            onClick = onPlayNext,
            enabled = hasNext,
            hitSize = controlHitSize,
            iconSize = normalIconSize,
            forceFocused = focusedControlIndex == VodControlNext,
            onFocused = { onFocusedControlChange(VodControlNext) },
            modifier = Modifier.offset(
                x = maxWidth * 0.652f - controlHitSize / 2f,
                y = controlsTop,
            ),
        )

        if (showSeriesDetails) {
            VodFocusIconButton(
                icon = Icons.Default.Info,
                contentDescription = seriesDetailsLabel,
                focusRequester = seriesDetailsFocusRequester,
                leftFocusRequester = if (hasNext) nextFocusRequester else seekForwardFocusRequester,
                rightFocusRequester = exitFocusRequester,
                upFocusRequester = progressFocusRequester,
                onClick = onOpenSeriesDetails,
                hitSize = controlHitSize,
                iconSize = normalIconSize,
                modifier = Modifier.offset(
                    x = maxWidth * 0.727f - controlHitSize / 2f,
                    y = controlsTop,
                ),
                forceFocused = focusedControlIndex == VodControlSeriesDetails,
                onFocused = { onFocusedControlChange(VodControlSeriesDetails) },
                showFocusedLabel = true,
            )
        }

        VodFocusIconButton(
            icon = Icons.Default.FullscreenExit,
            contentDescription = "Exit fullscreen",
            focusRequester = exitFocusRequester,
            leftFocusRequester = if (showSeriesDetails) seriesDetailsFocusRequester else nextFocusRequester,
            rightFocusRequester = brightnessFocusRequester,
            upFocusRequester = progressFocusRequester,
            onClick = onExitFullscreen,
            hitSize = controlHitSize,
            iconSize = normalIconSize,
            modifier = Modifier.offset(x = maxWidth * 0.9035f, y = controlsTop),
            forceFocused = focusedControlIndex == VodControlExit,
            onFocused = { onFocusedControlChange(VodControlExit) },
        )

        errorText?.let { message ->
            Text(
                text = message,
                color = Color.White,
                style = TextStyle(
                    fontSize = subtitleFontSize,
                    lineHeight = subtitleLineHeight,
                    fontWeight = FontWeight.Bold,
                    shadow = textShadow,
                ),
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
internal fun VodReferenceProgressBar(
    positionMs: Long,
    durationMs: Long,
    timeStyle: TextStyle,
    trackHeight: Dp,
    thumbSize: Dp,
    elapsedWidth: Dp,
    endGap: Dp,
    durationWidth: Dp,
    enabled: Boolean,
    focused: Boolean,
    focusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    onFocused: () -> Unit,
    onSeekBy: (Long) -> Unit,
    leftLabel: String = positionMs.formatPlaybackTime(),
    rightLabel: String = durationMs.formatPlaybackTime(),
    progressOverride: Float? = null,
    trailingLabel: String? = null,
    trailingColor: Color = VodProgressEnd,
    trailingWidth: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val progress = (progressOverride ?: (positionMs.toFloat() / safeDuration.toFloat())).coerceIn(0f, 1f)
    Row(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusProperties {
                canFocus = enabled
                down = downFocusRequester
            }
            .onFocusChanged { if (it.isFocused || it.hasFocus) onFocused() }
            .focusable(enabled = enabled)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> if (enabled) {
                        onSeekBy(-10_000L)
                        true
                    } else false
                    Key.DirectionRight -> if (enabled) {
                        onSeekBy(10_000L)
                        true
                    } else false
                    else -> false
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = leftLabel,
            color = Color.White.copy(alpha = 0.94f),
            style = timeStyle,
            maxLines = 1,
            modifier = Modifier.width(elapsedWidth),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(thumbSize),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(VodTrackColor),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(VodProgressStart, VodProgressEnd),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceAtLeast(0.001f))
                    .height(thumbSize),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (focused) {
                    VodFocusHalo(thumbSize * 3.6f)
                }
                Box(
                    modifier = Modifier
                        .size(if (focused) thumbSize * 1.12f else thumbSize)
                        .clip(CircleShape)
                        .background(Color(0xFFF7F7F7)),
                )
            }
        }
        Spacer(Modifier.width(endGap))
        Text(
            text = rightLabel,
            color = Color.White.copy(alpha = 0.94f),
            style = timeStyle,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(durationWidth),
        )
        if (!trailingLabel.isNullOrBlank()) {
            Spacer(Modifier.width(endGap))
            Text(
                text = trailingLabel,
                color = trailingColor,
                style = timeStyle,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(trailingWidth),
            )
        }
    }
}

@Composable
internal fun VodFocusIconButton(
    icon: ImageVector,
    contentDescription: String,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    onClick: () -> Unit,
    hitSize: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    enabled: Boolean = true,
    forceFocused: Boolean = false,
    showFocusedLabel: Boolean = false,
    onFocused: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(hitSize)
            .zIndex(if (focused || forceFocused) 2f else 0f)
            .focusRequester(focusRequester)
            .focusProperties {
                canFocus = enabled
                left = leftFocusRequester
                right = rightFocusRequester
                up = upFocusRequester
            }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(enabled = enabled, interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        val displayFocused = enabled && (focused || forceFocused)
        if (displayFocused) {
            VodFocusHalo(hitSize * 1.35f)
            if (showFocusedLabel) {
                Text(
                    text = contentDescription,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-24).dp)
                        .width(hitSize * 3.4f),
                )
            }
        }
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> Color.White.copy(alpha = 0.28f)
                displayFocused || prominent -> Color.White
                else -> Color.White.copy(alpha = 0.92f)
            },
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
internal fun VodInlineBrightnessSlider(
    value: Float,
    focusRequester: FocusRequester,
    onChange: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = (value / 100f).coerceIn(0f, 1f)
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
    Row(
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onChange(-5f)
                        true
                    }
                    Key.DirectionRight -> {
                        onChange(5f)
                        true
                    }
                    Key.Enter, Key.DirectionCenter, Key.Back -> {
                        onClose()
                        true
                    }
                    else -> false
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.WbSunny,
            contentDescription = "Brightness",
            tint = Color.White.copy(alpha = 0.94f),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(34.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(VodTrackColor),
            )
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(VodProgressStart, VodProgressEnd))),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceAtLeast(0.001f))
                    .height(34.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (focused) VodFocusHalo(34.dp)
                Box(
                    Modifier
                        .size(if (focused) 13.dp else 12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF7F7F7)),
                )
            }
        }
    }
}

@Composable
internal fun VodFocusHalo(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(
                Brush.radialGradient(
                    0.00f to VodFocusBlue.copy(alpha = 0.72f),
                    0.34f to VodFocusBlue.copy(alpha = 0.38f),
                    0.68f to VodFocusBlue.copy(alpha = 0.13f),
                    1.00f to Color.Transparent,
                ),
            ),
    )
}
