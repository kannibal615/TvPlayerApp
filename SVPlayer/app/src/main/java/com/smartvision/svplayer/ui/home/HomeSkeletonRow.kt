package com.smartvision.svplayer.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType

@Composable
fun HomeSkeletonRow(
    title: String,
    modifier: Modifier = Modifier,
    itemCount: Int = 8,
) {
    val transition = rememberInfiniteTransition(label = "homeSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "homeSkeletonAlpha",
    )

    Column(modifier = modifier.height(SmartVisionDimensions.HomeContentRowHeight)) {
        Text(
            text = title,
            color = SmartVisionColors.TextPrimary.copy(alpha = 0.78f),
            style = SmartVisionType.HomeSectionTitle,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(SmartVisionDimensions.HomeContentCardHeight),
            contentPadding = PaddingValues(horizontal = SmartVisionDimensions.HomeRowEdgePadding),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            userScrollEnabled = false,
        ) {
            items(itemCount) {
                HomeSkeletonCard(
                    alpha = alpha,
                    modifier = Modifier
                        .width(SmartVisionDimensions.HomeContentPreviewCardWidth)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun HomeSkeletonCard(
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF17243A).copy(alpha = alpha))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.72f)
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.12f)),
        )
    }
}
