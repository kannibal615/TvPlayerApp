package com.smartvision.svplayer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType

enum class TvButtonVariant {
    Primary,
    Secondary,
    Tertiary,
    Text,
    Success,
    Danger,
    Exit,
}

@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: TvButtonVariant = TvButtonVariant.Primary,
    selected: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = SmartVisionDimensions.InternalSpacing),
) {
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    val shape = RoundedCornerShape(SmartVisionDimensions.ButtonRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val targetBackground = when {
        variant == TvButtonVariant.Text -> Color.Transparent
        selected -> focusStyle.selectedBackground
        variant == TvButtonVariant.Exit && focusState.isFocused -> SmartVisionColors.Error
        variant == TvButtonVariant.Exit -> SmartVisionColors.Primary
        focusState.isFocused -> focusStyle.background
        variant == TvButtonVariant.Success -> Color(0xFF0E8F55)
        variant == TvButtonVariant.Danger -> Color(0xFFB3262F)
        variant == TvButtonVariant.Primary -> SmartVisionColors.Primary
        variant == TvButtonVariant.Secondary -> SmartVisionColors.SurfaceElevated
        else -> SmartVisionColors.Surface.copy(alpha = 0.54f)
    }
    val targetBorder = when {
        variant == TvButtonVariant.Exit && focusState.isFocused -> SmartVisionColors.Error
        focusState.isFocused -> focusStyle.accent
        selected -> focusStyle.selectedAccent
        variant == TvButtonVariant.Success -> Color(0xFF28E08A)
        variant == TvButtonVariant.Danger -> Color(0xFFFF5A65)
        variant == TvButtonVariant.Primary -> SmartVisionColors.Primary
        variant == TvButtonVariant.Text -> Color.Transparent
        else -> SmartVisionColors.Border
    }
    val targetTextColor = when {
        !enabled -> SmartVisionColors.TextSecondary.copy(alpha = 0.52f)
        (variant == TvButtonVariant.Primary || variant == TvButtonVariant.Exit || variant == TvButtonVariant.Success || variant == TvButtonVariant.Danger) && !focusState.isFocused -> Color.White
        focusState.isFocused || selected -> SmartVisionColors.TextPrimary
        else -> SmartVisionColors.TextSecondary
    }

    val background by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "tvButtonBackground",
    )
    val border by animateColorAsState(
        targetValue = targetBorder,
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "tvButtonBorder",
    )
    val textColor by animateColorAsState(
        targetValue = targetTextColor,
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "tvButtonText",
    )

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = SmartVisionDimensions.ButtonHeight)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                enabled = enabled,
                pressed = pressed,
                glowColor = if (variant == TvButtonVariant.Exit) SmartVisionColors.Error else SmartVisionColors.Primary,
                cornerRadius = SmartVisionDimensions.ButtonRadius,
            )
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .clip(shape)
            .background(background)
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else SmartVisionDimensions.PanelBorder,
                    border,
                ),
                shape,
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        // horizontalArrangement = Arrangement.Center,
    ) {
        when {
            leadingContent != null -> {
                leadingContent()
                Spacer(Modifier.width(SmartVisionDimensions.CompactSpacing))
            }
            leadingIcon != null -> {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(SmartVisionDimensions.InternalSpacing + SmartVisionDimensions.CompactSpacing),
                )
                Spacer(Modifier.width(SmartVisionDimensions.CompactSpacing))
            }
        }
        Text(
            text = text,
            color = textColor,
            style = SmartVisionType.Label,
            fontWeight = if (selected || focusState.isFocused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
        if (trailingContent != null) {
            Spacer(Modifier.width(SmartVisionDimensions.CompactSpacing))
            trailingContent()
        }
    }
}

@Composable
fun YoutubeLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 24.dp, height = 17.dp)) {
        drawRoundRect(
            color = Color(0xFFFF0033),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        )
        val triangle = Path().apply {
            moveTo(size.width * 0.43f, size.height * 0.30f)
            lineTo(size.width * 0.43f, size.height * 0.70f)
            lineTo(size.width * 0.70f, size.height * 0.50f)
            close()
        }
        drawPath(triangle, Color.White)
    }
}
