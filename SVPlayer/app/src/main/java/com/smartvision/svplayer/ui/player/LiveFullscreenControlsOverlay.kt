package com.smartvision.svplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun LiveFullscreenControlsOverlay(
    playback: FullScreenPlayback,
    isPlaying: Boolean,
    errorText: String?,
    positionMs: Long,
    durationMs: Long,
    wallClockMs: Long,
    isSeekable: Boolean,
    isAtLiveEdge: Boolean,
    focusedControlIndex: Int,
    progressFocused: Boolean,
    onFocusedControlChange: (Int) -> Unit,
    onProgressFocused: () -> Unit,
    playFocusRequester: FocusRequester,
    brightnessFocusRequester: FocusRequester,
    progressFocusRequester: FocusRequester,
    brightnessMode: Boolean,
    brightnessValue: Float,
    epgUnavailableLabel: String,
    nextLabel: String,
    directLabel: String,
    onOpenBrightness: () -> Unit,
    onChangeBrightness: (Float) -> Unit,
    onCloseBrightness: () -> Unit,
    onPreviousChannel: () -> Unit,
    onSeekBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onNextChannel: () -> Unit,
    onExitFullscreen: () -> Unit,
) {
    val currentIndex = playback.epgPrograms.indexOfFirst { program ->
        val start = program.startMillis
        val stop = program.stopMillis
        start != null && stop != null && wallClockMs in start until stop
    }.takeIf { it >= 0 } ?: playback.epgPrograms.indexOfFirst { it.isCurrent }
    val currentProgram = playback.epgPrograms.getOrNull(currentIndex)
    val nextProgram = playback.epgPrograms.getOrNull(currentIndex + 1)
    val epgStart = currentProgram?.startMillis
    val epgStop = currentProgram?.stopMillis
    val epgProgress = if (epgStart != null && epgStop != null && epgStop > epgStart) {
        ((wallClockMs - epgStart).toFloat() / (epgStop - epgStart).toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val leftTime = epgStart?.let { timeFormatter.format(Date(it)) } ?: "--:--"
    val rightTime = epgStop?.let { timeFormatter.format(Date(it)) } ?: "--:--"

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(referencePlayerGradient()),
    ) {
        val density = LocalDensity.current
        val viewportHeight = maxHeight
        val textShadow = Shadow(Color.Black.copy(alpha = 0.90f), blurRadius = 5f)
        val channelStyle = TextStyle(
            fontSize = with(density) { (maxHeight * 0.050f).toSp() },
            lineHeight = with(density) { (maxHeight * 0.058f).toSp() },
            fontWeight = FontWeight.Bold,
            shadow = textShadow,
        )
        val currentProgramStyle = TextStyle(
            fontSize = with(density) { (maxHeight * 0.029f).toSp() },
            lineHeight = with(density) { (maxHeight * 0.036f).toSp() },
            shadow = textShadow,
        )
        val nextProgramStyle = currentProgramStyle.copy(
            fontSize = with(density) { (maxHeight * 0.024f).toSp() },
        )
        val timeStyle = TextStyle(
            fontSize = with(density) { (maxHeight * 0.021f).toSp() },
            lineHeight = with(density) { (maxHeight * 0.027f).toSp() },
            shadow = textShadow,
        )
        val logoSize = maxHeight * 0.105f
        Box(
            modifier = Modifier
                .offset(x = maxWidth * 0.0605f, y = maxHeight * 0.700f)
                .size(logoSize),
            contentAlignment = Alignment.Center,
        ) {
            if (!playback.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = playback.imageUrl,
                    contentDescription = playback.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.size(logoSize * 0.62f),
                )
            }
        }
        Box(
            modifier = Modifier
                .offset(x = maxWidth * 0.132f, y = maxHeight * 0.690f)
                .width(maxWidth * 0.76f)
                .height(maxHeight * 0.145f),
        ) {
            Text(
                text = playback.title,
                color = Color.White,
                style = channelStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = currentProgram?.let { "$leftTime–$rightTime | ${it.title}" } ?: epgUnavailableLabel,
                color = Color.White.copy(alpha = 0.94f),
                style = currentProgramStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.offset(y = viewportHeight * 0.060f),
            )
            nextProgram?.let { program ->
                val nextTime = program.startMillis?.let { timeFormatter.format(Date(it)) } ?: "--:--"
                Text(
                    text = "${nextLabel.uppercase(Locale.getDefault())} · $nextTime | ${program.title}",
                    color = VodProgressEnd.copy(alpha = 0.90f),
                    style = nextProgramStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.offset(y = viewportHeight * 0.103f),
                )
            }
        }

        VodReferenceProgressBar(
            positionMs = positionMs,
            durationMs = durationMs,
            timeStyle = timeStyle,
            trackHeight = maxHeight * 0.0043f,
            thumbSize = maxHeight * 0.0145f,
            elapsedWidth = maxWidth * 0.044f,
            endGap = maxWidth * 0.012f,
            durationWidth = maxWidth * 0.050f,
            enabled = isSeekable,
            focused = progressFocused && isSeekable,
            focusRequester = progressFocusRequester,
            downFocusRequester = playFocusRequester,
            onFocused = onProgressFocused,
            onSeekBy = { delta -> if (delta < 0L) onSeekBack() else onSeekForward() },
            leftLabel = leftTime,
            rightLabel = rightTime,
            progressOverride = if (isSeekable && durationMs > 0L) null else epgProgress,
            trailingLabel = "● ${directLabel.uppercase(Locale.getDefault())}",
            trailingColor = if (isAtLiveEdge) VodProgressEnd else Color.White.copy(alpha = 0.52f),
            trailingWidth = maxWidth * 0.075f,
            modifier = Modifier
                .offset(y = maxHeight * 0.837f)
                .fillMaxWidth()
                .height(maxHeight * 0.058f)
                .padding(start = maxWidth * 0.0605f, end = maxWidth * 0.060f),
        )

        val previousRequester = remember { FocusRequester() }
        val seekBackRequester = remember { FocusRequester() }
        val seekForwardRequester = remember { FocusRequester() }
        val nextRequester = remember { FocusRequester() }
        val exitRequester = remember { FocusRequester() }
        val hitSize = maxHeight * 0.064f
        val iconSize = maxHeight * 0.032f
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
                    .height(hitSize),
            )
        } else {
            VodFocusIconButton(
                icon = Icons.Outlined.WbSunny,
                contentDescription = "Brightness",
                focusRequester = brightnessFocusRequester,
                leftFocusRequester = exitRequester,
                rightFocusRequester = previousRequester,
                upFocusRequester = progressFocusRequester,
                onClick = onOpenBrightness,
                hitSize = hitSize,
                iconSize = iconSize,
                modifier = Modifier.offset(x = maxWidth * 0.0545f, y = controlsTop),
                forceFocused = focusedControlIndex == 0,
                onFocused = { onFocusedControlChange(0) },
            )
        }
        val previousX = if (isSeekable) 0.338f else 0.413f
        val nextX = if (isSeekable) 0.652f else 0.583f
        VodFocusIconButton(
            icon = Icons.Default.SkipPrevious,
            contentDescription = "Previous channel",
            focusRequester = previousRequester,
            leftFocusRequester = brightnessFocusRequester,
            rightFocusRequester = if (isSeekable) seekBackRequester else playFocusRequester,
            upFocusRequester = progressFocusRequester,
            onClick = onPreviousChannel,
            hitSize = hitSize,
            iconSize = iconSize,
            modifier = Modifier.offset(x = maxWidth * previousX - hitSize / 2f, y = controlsTop),
            enabled = playback.previousItem != null,
            forceFocused = focusedControlIndex == 1,
            onFocused = { onFocusedControlChange(1) },
        )
        if (isSeekable) {
            VodFocusIconButton(
                icon = Icons.Default.Replay10,
                contentDescription = "Back 10 seconds",
                focusRequester = seekBackRequester,
                leftFocusRequester = previousRequester,
                rightFocusRequester = playFocusRequester,
                upFocusRequester = progressFocusRequester,
                onClick = onSeekBack,
                hitSize = hitSize,
                iconSize = iconSize,
                modifier = Modifier.offset(x = maxWidth * 0.413f - hitSize / 2f, y = controlsTop),
                forceFocused = focusedControlIndex == 2,
                onFocused = { onFocusedControlChange(2) },
            )
        }
        VodFocusIconButton(
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            focusRequester = playFocusRequester,
            leftFocusRequester = if (isSeekable) seekBackRequester else previousRequester,
            rightFocusRequester = if (isSeekable) seekForwardRequester else nextRequester,
            upFocusRequester = progressFocusRequester,
            onClick = onPlayPause,
            hitSize = hitSize,
            iconSize = maxHeight * 0.047f,
            modifier = Modifier.offset(x = maxWidth * 0.498f - hitSize / 2f, y = controlsTop),
            prominent = true,
            forceFocused = focusedControlIndex == 3,
            onFocused = { onFocusedControlChange(3) },
        )
        if (isSeekable) {
            VodFocusIconButton(
                icon = Icons.Default.Forward10,
                contentDescription = "Forward 10 seconds",
                focusRequester = seekForwardRequester,
                leftFocusRequester = playFocusRequester,
                rightFocusRequester = nextRequester,
                upFocusRequester = progressFocusRequester,
                onClick = onSeekForward,
                hitSize = hitSize,
                iconSize = iconSize,
                modifier = Modifier.offset(x = maxWidth * 0.583f - hitSize / 2f, y = controlsTop),
                forceFocused = focusedControlIndex == 4,
                onFocused = { onFocusedControlChange(4) },
            )
        }
        VodFocusIconButton(
            icon = Icons.Default.SkipNext,
            contentDescription = "Next channel",
            focusRequester = nextRequester,
            leftFocusRequester = if (isSeekable) seekForwardRequester else playFocusRequester,
            rightFocusRequester = exitRequester,
            upFocusRequester = progressFocusRequester,
            onClick = onNextChannel,
            hitSize = hitSize,
            iconSize = iconSize,
            modifier = Modifier.offset(x = maxWidth * nextX - hitSize / 2f, y = controlsTop),
            enabled = playback.nextItem != null,
            forceFocused = focusedControlIndex == 5,
            onFocused = { onFocusedControlChange(5) },
        )
        VodFocusIconButton(
            icon = Icons.Outlined.FullscreenExit,
            contentDescription = "Exit fullscreen",
            focusRequester = exitRequester,
            leftFocusRequester = nextRequester,
            rightFocusRequester = brightnessFocusRequester,
            upFocusRequester = progressFocusRequester,
            onClick = onExitFullscreen,
            hitSize = hitSize,
            iconSize = iconSize,
            modifier = Modifier.offset(x = maxWidth * 0.9035f, y = controlsTop),
            forceFocused = focusedControlIndex == 6,
            onFocused = { onFocusedControlChange(6) },
        )

        errorText?.let { message ->
            Text(
                text = message,
                color = Color.White,
                style = currentProgramStyle.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
