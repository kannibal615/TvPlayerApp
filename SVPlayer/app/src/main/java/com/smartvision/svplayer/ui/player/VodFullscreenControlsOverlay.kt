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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import java.util.Locale

private val VodProgressStart = Color(0xFF009DFF)
private val VodProgressEnd = Color(0xFF12C8FF)
private val VodTrackColor = Color(0xFF393C40).copy(alpha = 0.72f)
private val VodFocusBlue = Color(0xFF1687FF)

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
    focusedControlIndex: Int,
    onFocusedControlChange: (Int) -> Unit,
    playFocusRequester: FocusRequester,
    brightnessFocusRequester: FocusRequester,
    brightnessMode: Boolean,
    brightnessValue: Float,
    onOpenBrightness: () -> Unit,
    onChangeBrightness: (Float) -> Unit,
    onCloseBrightness: () -> Unit,
    onPlayPrevious: () -> Unit,
    onSeekBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onPlayNext: () -> Unit,
    onExitFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val previousFocusRequester = remember { FocusRequester() }
    val seekBackFocusRequester = remember { FocusRequester() }
    val seekForwardFocusRequester = remember { FocusRequester() }
    val nextFocusRequester = remember { FocusRequester() }
    val exitFocusRequester = remember { FocusRequester() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.00f to Color.Transparent,
                    0.54f to Color.Transparent,
                    0.66f to Color.Black.copy(alpha = 0.18f),
                    0.79f to Color.Black.copy(alpha = 0.62f),
                    0.91f to Color.Black.copy(alpha = 0.88f),
                    1.00f to Color.Black.copy(alpha = 0.97f),
                ),
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

        VodFocusIconButton(
            icon = Icons.Outlined.WbSunny,
            contentDescription = "Brightness",
            focusRequester = brightnessFocusRequester,
            leftFocusRequester = exitFocusRequester,
            rightFocusRequester = previousFocusRequester,
            onClick = onOpenBrightness,
            hitSize = controlHitSize,
            iconSize = normalIconSize,
            modifier = Modifier.offset(x = maxWidth * 0.0545f, y = controlsTop),
            forceFocused = focusedControlIndex == 0,
            onFocused = { onFocusedControlChange(0) },
        )

        VodFocusIconButton(
            icon = Icons.Default.SkipPrevious,
            contentDescription = "Previous",
            focusRequester = previousFocusRequester,
            leftFocusRequester = brightnessFocusRequester,
            rightFocusRequester = seekBackFocusRequester,
            onClick = onPlayPrevious,
            hitSize = controlHitSize,
            iconSize = normalIconSize,
            forceFocused = focusedControlIndex == 1,
            onFocused = { onFocusedControlChange(1) },
            modifier = Modifier.offset(
                x = maxWidth * 0.338f - controlHitSize / 2f,
                y = controlsTop,
            ),
        )
        VodFocusIconButton(
            icon = Icons.Default.Replay10,
            contentDescription = "Back 10 seconds",
            focusRequester = seekBackFocusRequester,
            leftFocusRequester = previousFocusRequester,
            rightFocusRequester = playFocusRequester,
            onClick = onSeekBack,
            hitSize = controlHitSize,
            iconSize = normalIconSize,
            forceFocused = focusedControlIndex == 2,
            onFocused = { onFocusedControlChange(2) },
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
            onClick = onPlayPause,
            hitSize = controlHitSize,
            iconSize = playIconSize,
            prominent = true,
            forceFocused = focusedControlIndex == 3,
            onFocused = { onFocusedControlChange(3) },
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
            onClick = onSeekForward,
            hitSize = controlHitSize,
            iconSize = normalIconSize,
            forceFocused = focusedControlIndex == 4,
            onFocused = { onFocusedControlChange(4) },
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
            rightFocusRequester = exitFocusRequester,
            onClick = onPlayNext,
            hitSize = controlHitSize,
            iconSize = normalIconSize,
            forceFocused = focusedControlIndex == 5,
            onFocused = { onFocusedControlChange(5) },
            modifier = Modifier.offset(
                x = maxWidth * 0.652f - controlHitSize / 2f,
                y = controlsTop,
            ),
        )

        VodFocusIconButton(
            icon = Icons.Default.FullscreenExit,
            contentDescription = "Exit fullscreen",
            focusRequester = exitFocusRequester,
            leftFocusRequester = nextFocusRequester,
            rightFocusRequester = brightnessFocusRequester,
            onClick = onExitFullscreen,
            hitSize = controlHitSize,
            iconSize = normalIconSize,
            modifier = Modifier.offset(x = maxWidth * 0.9035f, y = controlsTop),
            forceFocused = focusedControlIndex == 6,
            onFocused = { onFocusedControlChange(6) },
        )

        if (brightnessMode) {
            PlayerBrightnessSlider(
                value = brightnessValue,
                onChange = onChangeBrightness,
                onClose = onCloseBrightness,
                modifier = Modifier
                    .offset(x = maxWidth * 0.054f, y = maxHeight * 0.824f)
                    .width(maxWidth * 0.25f),
            )
        }

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
private fun VodReferenceProgressBar(
    positionMs: Long,
    durationMs: Long,
    timeStyle: TextStyle,
    trackHeight: Dp,
    thumbSize: Dp,
    elapsedWidth: Dp,
    endGap: Dp,
    durationWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val progress = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = positionMs.formatPlaybackTime(),
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
                Box(
                    modifier = Modifier
                        .size(thumbSize)
                        .clip(CircleShape)
                        .background(Color(0xFFF7F7F7)),
                )
            }
        }
        Spacer(Modifier.width(endGap))
        Text(
            text = durationMs.formatPlaybackTime(),
            color = Color.White.copy(alpha = 0.94f),
            style = timeStyle,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(durationWidth),
        )
    }
}

@Composable
private fun VodFocusIconButton(
    icon: ImageVector,
    contentDescription: String,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester,
    onClick: () -> Unit,
    hitSize: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    forceFocused: Boolean = false,
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
                left = leftFocusRequester
                right = rightFocusRequester
            }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        val displayFocused = forceFocused
        if (displayFocused) {
            Box(
                modifier = Modifier
                    .size(hitSize * 1.35f)
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
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (displayFocused || prominent) Color.White else Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(iconSize),
        )
    }
}
