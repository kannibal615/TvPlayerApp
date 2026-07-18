package com.smartvision.svplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Brush
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
    val currentProgram = playback.epgPrograms.firstOrNull { program ->
        val start = program.startMillis
        val stop = program.stopMillis
        start != null && stop != null && wallClockMs in start until stop
    } ?: playback.epgPrograms.firstOrNull { it.isCurrent }
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val epgLine = currentProgram?.let { program ->
        val start = program.startMillis?.let { formatter.format(Date(it)) }
        val stop = program.stopMillis?.let { formatter.format(Date(it)) }
        if (start != null && stop != null) {
            "$start-$stop | ${program.title}"
        } else {
            program.title
        }
    } ?: epgUnavailableLabel

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.53f to Color.Transparent,
                        0.75f to Color.Black.copy(alpha = 0.44f),
                        1f to Color.Black.copy(alpha = 0.98f),
                    ),
                ),
            ),
    ) {
        val density = LocalDensity.current
        val shadow = Shadow(Color.Black.copy(alpha = 0.96f), blurRadius = 7f)
        val logoWidth = maxWidth * 0.108f
        val logoHeight = maxHeight * 0.118f
        val hitSize = maxHeight * 0.070f
        val iconSize = maxHeight * 0.038f
        val controlsY = maxHeight * 0.805f
        val epgOffsetY = maxHeight * 0.066f
        val playIconSize = maxHeight * 0.047f
        val titleStyle = TextStyle(
            fontSize = with(density) { (maxHeight * 0.052f).toSp() },
            lineHeight = with(density) { (maxHeight * 0.060f).toSp() },
            fontWeight = FontWeight.Bold,
            shadow = shadow,
        )
        val epgStyle = TextStyle(
            fontSize = with(density) { (maxHeight * 0.027f).toSp() },
            lineHeight = with(density) { (maxHeight * 0.034f).toSp() },
            fontWeight = FontWeight.SemiBold,
            shadow = shadow,
        )

        Box(
            modifier = Modifier
                .offset(x = maxWidth * 0.037f, y = maxHeight * 0.786f)
                .size(logoWidth, logoHeight),
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
                    tint = Color.White.copy(alpha = 0.80f),
                    modifier = Modifier.size(logoHeight * 0.62f),
                )
            }
        }

        Box(
            modifier = Modifier
                .offset(x = maxWidth * 0.158f, y = maxHeight * 0.796f)
                .width(maxWidth * 0.51f)
                .height(maxHeight * 0.135f),
        ) {
            Text(
                text = playback.title,
                color = Color.White,
                style = titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = epgLine,
                color = Color.White.copy(alpha = 0.94f),
                style = epgStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.offset(y = epgOffsetY),
            )
        }

        val previousRequester = remember { FocusRequester() }
        val nextRequester = remember { FocusRequester() }
        val exitRequester = remember { FocusRequester() }
        VodFocusIconButton(
            icon = Icons.Default.SkipPrevious,
            contentDescription = "Previous channel",
            focusRequester = previousRequester,
            leftFocusRequester = exitRequester,
            rightFocusRequester = playFocusRequester,
            upFocusRequester = previousRequester,
            onClick = onPreviousChannel,
            enabled = playback.previousItem != null,
            hitSize = hitSize,
            iconSize = iconSize,
            modifier = Modifier.offset(x = maxWidth * 0.744f - hitSize / 2f, y = controlsY),
            forceFocused = focusedControlIndex == 1,
            onFocused = { onFocusedControlChange(1) },
        )
        VodFocusIconButton(
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            focusRequester = playFocusRequester,
            leftFocusRequester = previousRequester,
            rightFocusRequester = nextRequester,
            upFocusRequester = playFocusRequester,
            onClick = onPlayPause,
            hitSize = hitSize,
            iconSize = playIconSize,
            prominent = true,
            solidFocusedCircle = true,
            modifier = Modifier.offset(x = maxWidth * 0.793f - hitSize / 2f, y = controlsY),
            forceFocused = focusedControlIndex == 3,
            onFocused = { onFocusedControlChange(3) },
        )
        VodFocusIconButton(
            icon = Icons.Default.SkipNext,
            contentDescription = "Next channel",
            focusRequester = nextRequester,
            leftFocusRequester = playFocusRequester,
            rightFocusRequester = brightnessFocusRequester,
            upFocusRequester = nextRequester,
            onClick = onNextChannel,
            enabled = playback.nextItem != null,
            hitSize = hitSize,
            iconSize = iconSize,
            modifier = Modifier.offset(x = maxWidth * 0.842f - hitSize / 2f, y = controlsY),
            forceFocused = focusedControlIndex == 5,
            onFocused = { onFocusedControlChange(5) },
        )
        if (brightnessMode) {
            VodInlineBrightnessSlider(
                value = brightnessValue,
                focusRequester = brightnessFocusRequester,
                onChange = onChangeBrightness,
                onClose = onCloseBrightness,
                modifier = Modifier
                    .offset(x = maxWidth * 0.825f, y = controlsY)
                    .width(maxWidth * 0.13f)
                    .height(hitSize),
            )
        } else {
            VodFocusIconButton(
                icon = Icons.Outlined.WbSunny,
                contentDescription = "Brightness",
                focusRequester = brightnessFocusRequester,
                leftFocusRequester = nextRequester,
                rightFocusRequester = exitRequester,
                upFocusRequester = brightnessFocusRequester,
                onClick = onOpenBrightness,
                hitSize = hitSize,
                iconSize = iconSize,
                modifier = Modifier.offset(x = maxWidth * 0.896f - hitSize / 2f, y = controlsY),
                forceFocused = focusedControlIndex == 0,
                onFocused = { onFocusedControlChange(0) },
            )
        }
        VodFocusIconButton(
            icon = Icons.Outlined.FullscreenExit,
            contentDescription = "Exit fullscreen",
            focusRequester = exitRequester,
            leftFocusRequester = brightnessFocusRequester,
            rightFocusRequester = previousRequester,
            upFocusRequester = exitRequester,
            onClick = onExitFullscreen,
            hitSize = hitSize,
            iconSize = iconSize,
            modifier = Modifier.offset(x = maxWidth * 0.944f - hitSize / 2f, y = controlsY),
            forceFocused = focusedControlIndex == 6,
            onFocused = { onFocusedControlChange(6) },
        )

        errorText?.let { message ->
            Text(
                text = message,
                color = Color.White,
                style = epgStyle,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
