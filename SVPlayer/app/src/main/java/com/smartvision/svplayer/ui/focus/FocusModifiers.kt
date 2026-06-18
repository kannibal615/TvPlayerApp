package com.smartvision.svplayer.ui.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import kotlin.math.max

@Stable
class TvFocusState internal constructor() {
    var isFocused by mutableStateOf(false)
        internal set
}

@Composable
fun rememberTvFocusState(): TvFocusState = remember { TvFocusState() }

fun Modifier.tvFocusTarget(
    state: TvFocusState,
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
    pressed: Boolean = false,
    focusedScale: Float = SmartVisionDimensions.FocusScale,
    glowColor: Color = SmartVisionColors.CyanAccent,
    cornerRadius: Dp = SmartVisionDimensions.CardRadius,
): Modifier = composed {
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }
    val scale by animateFloatAsState(
        targetValue = when {
            pressed && enabled -> 0.97f
            state.isFocused && enabled -> focusedScale
            else -> 1f
        },
        animationSpec = tween(durationMillis = SmartVisionDimensions.FocusAnimationMillis),
        label = "tvFocusScale",
    )

    val requesterModifier = if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }

    this
        .then(requesterModifier)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawBehind {
            if (state.isFocused && enabled) {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = 0.34f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = max(size.width, size.height) * 0.72f,
                    ),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                )
            }
        }
        .onFocusChanged { focusState ->
            state.isFocused = focusState.isFocused || focusState.hasFocus
        }
}

fun Modifier.tvFocusable(
    state: TvFocusState,
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
    pressed: Boolean = false,
    focusedScale: Float = SmartVisionDimensions.FocusScale,
    glowColor: Color = SmartVisionColors.CyanAccent,
    cornerRadius: Dp = SmartVisionDimensions.CardRadius,
): Modifier = tvFocusTarget(
    state = state,
    focusRequester = focusRequester,
    enabled = enabled,
    pressed = pressed,
    focusedScale = focusedScale,
    glowColor = glowColor,
    cornerRadius = cornerRadius,
).focusable(enabled = enabled)
