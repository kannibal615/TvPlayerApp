package com.smartvision.svplayer.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onDown: (() -> Unit)? = null,
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    blocked: Boolean = false,
    blockedMessage: String = "Connection unavailable",
    workOverlay: HomeCategoryWorkOverlay? = null,
    kidsMode: Boolean = false,
    itemCount: Int = 0,
    transitionSurfaceModifier: Modifier = Modifier,
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
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.key == Key.DirectionDown && onDown != null -> {
                        onDown()
                        true
                    }
                    event.key == Key.DirectionLeft && onLeft != null -> {
                        onLeft()
                        true
                    }
                    event.key == Key.DirectionRight && onRight != null -> {
                        onRight()
                        true
                    }
                    else -> false
                }
            }
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                // HOME category cards keep a stable footprint: focus is expressed by
                // the shared frame/glow instead of overlapping neighbouring content.
                focusedScale = 1.0f,
                glowColor = accent,
                cornerRadius = SmartVisionDimensions.HomeCardRadius,
            )
            .shadow(
                elevation = if (focusState.isFocused) 20.dp else 3.dp,
                shape = shape,
                ambientColor = SmartVisionColors.CardFocusGlow,
                spotColor = SmartVisionColors.CardFocusGlow,
            )
            .clip(shape)
            .background(SmartVisionColors.Surface)
            .border(BorderStroke(if (focusState.isFocused) focusStyle.borderWidth else 1.dp, border), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = workOverlay?.active != true,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource),
    ) {
        Box(
            modifier = transitionSurfaceModifier
                .fillMaxSize()
                .clip(shape),
        ) {
            HomeVisualBackground(style = category.visualStyle, kidsMode = kidsMode, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF020712).copy(alpha = 0.72f),
                                Color(0xFF020712).copy(alpha = 0.24f),
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
                            listOf(Color.Transparent, Color(0xFF020712).copy(alpha = 0.68f)),
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = HomeCategoryCardLayout.HorizontalPadding,
                    top = HomeCategoryCardLayout.TopPadding,
                    end = HomeCategoryCardLayout.HorizontalPadding,
                    bottom = HomeCategoryCardLayout.BottomPadding,
                ),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = itemCount.coerceAtLeast(0).toString(),
                color = Color.White.copy(alpha = 0.92f),
                style = SmartVisionType.HomeCategoryTitle.copy(
                    fontSize = HomeCategoryCardLayout.CountFontSize,
                    lineHeight = HomeCategoryCardLayout.CountLineHeight,
                ),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.height(HomeCategoryCardLayout.CountTitleSpacing))
            Text(
                text = category.title,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.HomeCategoryTitle.copy(
                    fontSize = HomeCategoryCardLayout.TitleFontSize,
                    lineHeight = HomeCategoryCardLayout.TitleLineHeight,
                ),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(HomeCategoryCardLayout.TitleSubtitleSpacing))
            Text(
                text = category.subtitle,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(HomeCategoryCardLayout.SubtitleActionSpacing))
            if (workOverlay?.active == true || workOverlay?.error == true) {
                CategoryWorkProgress(workOverlay)
            } else {
                CategoryActionRow(
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
                        text = blockedMessage,
                        color = Color.White,
                        style = SmartVisionType.Caption,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
        if (workOverlay?.active == true) {
            CategoryPerimeterLoader(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
            )
        }
    }
}

data class HomeCategoryWorkOverlay(
    val progress: Float,
    val active: Boolean,
    val error: Boolean,
    val label: String,
    val detail: String? = null,
)

@Composable
private fun CategoryPerimeterLoader(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "home-category-loader")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "home-category-loader-phase",
    )
    Canvas(modifier = modifier) {
        val perimeter = 2f * (size.width + size.height)
        val segment = perimeter * HomeCategoryCardLayout.LoaderSegmentFraction
        val gap = (perimeter - segment).coerceAtLeast(1f)
        drawRoundRect(
            color = SmartVisionColors.CyanAccent,
            cornerRadius = CornerRadius(SmartVisionDimensions.HomeCardRadius.toPx()),
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(segment, gap),
                    phase = -phase * perimeter,
                ),
            ),
        )
    }
}

@Composable
private fun CategoryWorkProgress(workOverlay: HomeCategoryWorkOverlay) {
    val progress = workOverlay.progress.coerceIn(0f, 1f)
    val statusColor = if (workOverlay.error) SmartVisionColors.Warning else SmartVisionColors.CyanAccent
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = workOverlay.label,
                color = if (workOverlay.error) SmartVisionColors.Warning else Color.White,
                style = SmartVisionType.Caption,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(progress * 100f).toInt()}%",
                color = statusColor,
                style = SmartVisionType.Caption,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50)),
            color = statusColor,
            trackColor = Color.White.copy(alpha = 0.16f),
        )
        workOverlay.detail?.let { detail ->
            Spacer(Modifier.height(3.dp))
            Text(
                text = detail,
                color = Color.White.copy(alpha = 0.80f),
                style = SmartVisionType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CategoryActionRow(
    text: String,
    icon: ImageVector,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(SmartVisionDimensions.HomeContentRadius))
                .background(Color.Black.copy(alpha = 0.28f))
                .border(
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    RoundedCornerShape(SmartVisionDimensions.HomeContentRadius),
                )
                .padding(
                    horizontal = HomeCategoryCardLayout.ActionHorizontalPadding,
                    vertical = HomeCategoryCardLayout.ActionVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HomeCategoryCardLayout.ActionIconSpacing),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(HomeCategoryCardLayout.ActionIconSize),
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
}

private object HomeCategoryCardLayout {
    const val LoaderSegmentFraction = 0.650f
    val HorizontalPadding = 20.dp
    val TopPadding = 12.dp
    val BottomPadding = 18.dp
    val TitleFontSize = 42.sp
    val TitleLineHeight = 51.sp
    val CountFontSize = 27.sp
    val CountLineHeight = 32.sp
    val CountTitleSpacing = 0.dp
    val TitleSubtitleSpacing = 1.dp
    val SubtitleActionSpacing = 8.dp
    val ActionHorizontalPadding = 12.dp
    val ActionVerticalPadding = 5.dp
    val ActionIconSpacing = 8.dp
    val ActionIconSize = 14.dp
}

private val HomeCategoryType.accent: Color
    get() = when (this) {
        HomeCategoryType.Live -> SmartVisionColors.CyanAccent
        HomeCategoryType.Movies -> SmartVisionColors.Warning
        HomeCategoryType.Series -> SmartVisionColors.Primary
    }
