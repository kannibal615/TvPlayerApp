package com.smartvision.svplayer.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.smartvision.svplayer.ui.theme.SmartVisionColors

@Composable
internal fun LiveZapGuide(
    channels: List<LiveZapGuideItem>,
    currentStreamId: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(320.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.58f))
            .border(
                BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.72f)),
                RoundedCornerShape(12.dp),
            )
            .focusProperties { canFocus = false }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        channels.forEach { channel ->
            LiveZapGuideRow(
                channel = channel,
                selected = channel.streamId == currentStreamId,
            )
        }
    }
}

@Composable
private fun LiveZapGuideRow(
    channel: LiveZapGuideItem,
    selected: Boolean,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(
                if (selected) SmartVisionColors.Primary.copy(alpha = 0.72f)
                else SmartVisionColors.Surface.copy(alpha = 0.48f),
            )
            .border(
                BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) SmartVisionColors.CyanAccent
                    else SmartVisionColors.Border.copy(alpha = 0.44f),
                ),
                shape,
            )
            .focusProperties { canFocus = false }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 36.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            if (channel.imageUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.size(22.dp),
                )
            } else {
                AsyncImage(
                    model = channel.imageUrl,
                    contentDescription = channel.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.width(214.dp)) {
            Text(
                text = channel.label,
                color = Color.White.copy(alpha = if (selected) 0.92f else 0.66f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = channel.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
