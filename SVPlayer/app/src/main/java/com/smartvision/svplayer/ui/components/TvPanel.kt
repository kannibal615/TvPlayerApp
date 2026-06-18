package com.smartvision.svplayer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions

@Composable
fun TvPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(SmartVisionDimensions.InternalSpacing),
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(SmartVisionDimensions.PanelRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        SmartVisionColors.SurfaceElevated.copy(alpha = 0.88f),
                        SmartVisionColors.Surface.copy(alpha = 0.96f),
                    ),
                ),
            )
            .border(
                BorderStroke(SmartVisionDimensions.PanelBorder, SmartVisionColors.Border),
                shape,
            )
            .padding(contentPadding),
    ) {
        content()
    }
}
