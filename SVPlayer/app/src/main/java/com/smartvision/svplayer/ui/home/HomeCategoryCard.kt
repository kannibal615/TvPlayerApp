package com.smartvision.svplayer.ui.home

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.smartvision.svplayer.data.mock.HomeCategory
import com.smartvision.svplayer.data.mock.HomeCategoryType
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType

@Composable
fun HomeCategoryCard(
    category: HomeCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    blocked: Boolean = false,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(SmartVisionDimensions.HomeCardRadius)
    val accent = category.type.accent
    val focusStyle = LocalTvFocusStyle.current

    val border by animateColorAsState(
        targetValue = if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border,
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "homeCategoryBorder",
    )

    Box(
        modifier = modifier
            .zIndex(if (focusState.isFocused) 3f else 0f)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.055f,
                glowColor = accent,
                cornerRadius = SmartVisionDimensions.HomeCardRadius,
            )
            .clip(shape)
            .background(SmartVisionColors.Surface)
            .border(BorderStroke(if (focusState.isFocused) focusStyle.borderWidth else 1.dp, border), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource),
    ) {
        HomeVisualBackground(style = category.visualStyle, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF020712).copy(alpha = 0.88f),
                            Color(0xFF020712).copy(alpha = 0.38f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xFF020712).copy(alpha = 0.82f)),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            CategoryBadge(text = category.badge, accent = accent)
            Column {
                Text(
                    text = category.title,
                    color = SmartVisionColors.TextPrimary,
                    style = SmartVisionType.HomeCategoryTitle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = category.subtitle,
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                CategoryCta(
                    text = category.actionLabel,
                    icon = if (category.type == HomeCategoryType.Live) Icons.Default.Tv else Icons.Default.PlayArrow,
                )
            }
        }
        if (blocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF130F09).copy(alpha = 0.78f))
                        .border(BorderStroke(1.dp, SmartVisionColors.Warning.copy(alpha = 0.76f)), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = SmartVisionColors.Warning,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Connexion indisponible",
                        color = Color.White,
                        style = SmartVisionType.Caption,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBadge(
    text: String,
    accent: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(SmartVisionDimensions.HomeContentRadius))
            .background(accent.copy(alpha = 0.26f))
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.78f)), RoundedCornerShape(SmartVisionDimensions.HomeContentRadius))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun CategoryCta(
    text: String,
    icon: ImageVector,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(SmartVisionDimensions.HomeContentRadius))
            .background(Color.Black.copy(alpha = 0.28f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(SmartVisionDimensions.HomeContentRadius))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            color = Color.White,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

private val HomeCategoryType.accent: Color
    get() = when (this) {
        HomeCategoryType.Live -> SmartVisionColors.CyanAccent
        HomeCategoryType.Movies -> SmartVisionColors.Warning
        HomeCategoryType.Series -> SmartVisionColors.Primary
    }
