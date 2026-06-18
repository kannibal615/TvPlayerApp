package com.smartvision.svplayer.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(SVColors.SurfaceSoft)
            .border(BorderStroke(1.dp, SVColors.Border), shape),
        content = { content() },
    )
}

@Composable
fun FocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accent: Color = SVColors.Cyan,
    shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable (focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.025f else 1f, label = "cardScale")
    val border by animateColorAsState(
        when {
            focused -> accent
            selected -> accent.copy(alpha = 0.75f)
            else -> SVColors.Border
        },
        label = "cardBorder",
    )
    val background by animateColorAsState(
        when {
            selected -> accent.copy(alpha = 0.17f)
            focused -> SVColors.SurfaceLight.copy(alpha = 0.92f)
            else -> SVColors.Surface.copy(alpha = 0.78f)
        },
        label = "cardBackground",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                if (focused) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(accent.copy(alpha = 0.25f), Color.Transparent),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension * 0.72f,
                        ),
                    )
                }
            }
            .clip(shape)
            .background(background)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, border), shape)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .focusable()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content(focused)
    }
}

@Composable
fun FocusableButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    accent: Color = SVColors.Cyan,
    minHeight: Dp = 48.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    shape: Shape = RoundedCornerShape(10.dp),
) {
    FocusableCard(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = minHeight),
        selected = selected,
        accent = accent,
        shape = shape,
        contentPadding = contentPadding,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected || it) accent else SVColors.TextPrimary,
                    modifier = Modifier.size(22.dp),
                )
                if (text.isNotBlank()) {
                    Spacer(Modifier.width(10.dp))
                }
            }
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    color = SVColors.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
