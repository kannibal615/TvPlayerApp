package com.smartvision.svplayer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
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
    focusRequester: FocusRequester? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = SmartVisionDimensions.InternalSpacing),
) {
    val focusState = rememberTvFocusState()
    val shape = RoundedCornerShape(SmartVisionDimensions.ButtonRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val targetBackground = when {
        variant == TvButtonVariant.Text -> Color.Transparent
        selected -> SmartVisionColors.Primary.copy(alpha = 0.34f)
        focusState.isFocused -> SmartVisionColors.SurfaceElevated
        variant == TvButtonVariant.Primary -> SmartVisionColors.Primary
        variant == TvButtonVariant.Secondary -> SmartVisionColors.SurfaceElevated
        else -> SmartVisionColors.Surface.copy(alpha = 0.54f)
    }
    val targetBorder = when {
        focusState.isFocused -> SmartVisionColors.FocusWhite
        selected -> SmartVisionColors.CyanAccent
        variant == TvButtonVariant.Primary -> SmartVisionColors.Primary
        variant == TvButtonVariant.Text -> Color.Transparent
        else -> SmartVisionColors.Border
    }
    val targetTextColor = when {
        !enabled -> SmartVisionColors.TextSecondary.copy(alpha = 0.52f)
        variant == TvButtonVariant.Primary && !focusState.isFocused -> Color.White
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
                glowColor = SmartVisionColors.Primary,
                cornerRadius = SmartVisionDimensions.ButtonRadius,
            )
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .clip(shape)
            .background(background)
            .border(
                BorderStroke(
                    if (focusState.isFocused) SmartVisionDimensions.FocusBorder else SmartVisionDimensions.PanelBorder,
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
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(SmartVisionDimensions.InternalSpacing + SmartVisionDimensions.CompactSpacing),
            )
            Spacer(Modifier.width(SmartVisionDimensions.CompactSpacing))
        }
        Text(
            text = text,
            color = textColor,
            style = SmartVisionType.Label,
            fontWeight = if (selected || focusState.isFocused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}
