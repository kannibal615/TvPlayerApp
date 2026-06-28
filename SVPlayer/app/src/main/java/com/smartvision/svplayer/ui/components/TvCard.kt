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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType

@Composable
fun TvCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    meta: String? = null,
    badge: String? = null,
    selected: Boolean = false,
    accent: Color = SmartVisionColors.Primary,
    focusRequester: FocusRequester? = null,
    contentPadding: PaddingValues = PaddingValues(SmartVisionDimensions.InternalSpacing),
) {
    val focusState = rememberTvFocusState()
    val shape = RoundedCornerShape(SmartVisionDimensions.CardRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focusStyle = LocalTvFocusStyle.current

    val border by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> focusStyle.accent
            selected -> accent
            else -> SmartVisionColors.Border
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "tvCardBorder",
    )
    val topAccent by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> focusStyle.background
            selected -> accent.copy(alpha = 0.26f)
            else -> SmartVisionColors.SurfaceElevated
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "tvCardAccent",
    )

    Box(
        modifier = modifier
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                glowColor = accent,
                cornerRadius = SmartVisionDimensions.CardRadius,
            )
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        topAccent,
                        SmartVisionColors.SurfaceElevated.copy(alpha = 0.84f),
                        SmartVisionColors.Surface.copy(alpha = 0.98f),
                    ),
                ),
            )
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else SmartVisionDimensions.PanelBorder,
                    border,
                ),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(contentPadding),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (badge != null) {
                CardBadge(text = badge, accent = accent)
            } else {
                Spacer(Modifier.height(SmartVisionDimensions.InternalSpacing))
            }

            Column {
                Text(
                    text = title,
                    color = SmartVisionColors.TextPrimary,
                    style = SmartVisionType.TitleS,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(SmartVisionDimensions.CompactSpacing))
                    Text(
                        text = subtitle,
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Label,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (meta != null) {
                    Spacer(Modifier.height(SmartVisionDimensions.InternalSpacing))
                    Text(
                        text = meta,
                        color = if (focusState.isFocused || selected) accent else SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Caption,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardBadge(
    text: String,
    accent: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(SmartVisionDimensions.BadgeRadius))
            .background(accent.copy(alpha = 0.18f))
            .border(
                BorderStroke(SmartVisionDimensions.PanelBorder, accent.copy(alpha = 0.72f)),
                RoundedCornerShape(SmartVisionDimensions.BadgeRadius),
            )
            .padding(
                horizontal = SmartVisionDimensions.CompactSpacing,
                vertical = SmartVisionDimensions.CompactSpacing / 2,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
