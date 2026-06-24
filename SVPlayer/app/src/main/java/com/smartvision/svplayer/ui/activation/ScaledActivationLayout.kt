package com.smartvision.svplayer.ui.activation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

internal const val ActivationContentScale = 0.88f

@Composable
internal fun ScaledActivationLayout(
    scale: Float = ActivationContentScale,
    content: @Composable () -> Unit,
) {
    require(scale > 0f) { "Scale must be greater than zero." }

    Layout(content = content) { measurables, constraints ->
        val placeable = measurables.single().measure(
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxWidth = constraints.maxWidth.scaledUpFor(scale),
                maxHeight = constraints.maxHeight.scaledUpFor(scale),
            ),
        )
        val scaledWidth = (placeable.width * scale)
            .roundToInt()
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val scaledHeight = (placeable.height * scale)
            .roundToInt()
            .coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(scaledWidth, scaledHeight) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

private fun Int.scaledUpFor(scale: Float): Int =
    if (this == Constraints.Infinity) {
        this
    } else {
        (this / scale).toInt()
    }
