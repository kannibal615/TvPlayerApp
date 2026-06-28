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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlin.math.max
import kotlin.math.min

@Composable
fun ContentProgressCard(
    item: ContinueItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(SmartVisionDimensions.HomeContentRadius)
    val focusStyle = LocalTvFocusStyle.current
    val border by animateColorAsState(
        targetValue = if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border.copy(alpha = 0.78f),
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "contentCardBorder",
    )
    val isLive = item.mediaType == "LIVE"

    LaunchedEffect(focusState.isFocused) {
        if (focusState.isFocused) onFocused()
    }

    Box(
        modifier = modifier
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .tvFocusTarget(
                state = focusState,
                pressed = pressed,
                focusedScale = 1.045f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = SmartVisionDimensions.HomeContentRadius,
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
        HomeVisualBackground(style = item.visualStyle, modifier = Modifier.fillMaxSize())
        if (!item.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = if (isLive) ContentScale.Fit else ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isLive) 16.dp else 0.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.46f),
                            Color.Black.copy(alpha = 0.10f),
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
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                    ),
                ),
        )
        MediaTypeBadge(
            text = item.mediaType,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(7.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        ) {
            Text(
                text = item.title,
                color = Color.White,
                style = SmartVisionType.Caption.copy(fontSize = 11.sp, lineHeight = 14.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            ProgressBar(progress = item.progress)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item.meta,
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Caption.copy(fontSize = 10.sp, lineHeight = 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = item.remaining,
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Caption.copy(fontSize = 10.sp, lineHeight = 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MediaTypeBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    if (text.isBlank()) return
    val accent = mediaTypeBadgeAccent(text)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(accent.copy(alpha = 0.30f))
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.86f)), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = SmartVisionType.Caption.copy(fontSize = 9.sp, lineHeight = 11.sp),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun mediaTypeBadgeAccent(text: String): Color =
    when (text.uppercase()) {
        "LIVE" -> SmartVisionColors.CyanAccent
        "FILM" -> SmartVisionColors.Warning
        "SERIE" -> SmartVisionColors.Primary
        else -> SmartVisionColors.TextSecondary
    }

@Composable
private fun ProgressBar(progress: Float) {
    val clamped = max(0f, min(1f, progress))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(4.dp)
                .background(SmartVisionColors.Primary),
        )
    }
}
