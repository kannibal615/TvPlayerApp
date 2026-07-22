package com.smartvision.svplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.smartvision.svplayer.ui.theme.SmartVisionColors

@Stable
class LiveZapGuideState {
    var channels by mutableStateOf<List<LiveZapGuideItem>>(emptyList())
        private set
    var currentStreamId by mutableIntStateOf(-1)
        private set
    var visible by mutableStateOf(false)
        private set
    private var requestId by mutableIntStateOf(0)

    fun updateChannels(value: List<LiveZapGuideItem>) {
        if (value.isNotEmpty()) channels = value
    }

    fun show(streamId: Int): Int {
        currentStreamId = streamId
        visible = channels.isNotEmpty()
        requestId += 1
        return requestId
    }

    fun hide(request: Int) {
        if (request == requestId) visible = false
    }
}

@Composable
internal fun LiveZapGuide(
    channels: List<LiveZapGuideItem>,
    currentStreamId: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(236.dp)
            .focusProperties { canFocus = false },
        verticalArrangement = Arrangement.spacedBy(2.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                if (selected) SmartVisionColors.Primary.copy(alpha = 0.46f)
                else Color.Black.copy(alpha = 0.22f),
            )
            .focusProperties { canFocus = false },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(if (selected) SmartVisionColors.CyanAccent else Color.Transparent),
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier.size(width = 48.dp, height = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (channel.imageUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.62f),
                    modifier = Modifier.size(18.dp),
                )
            } else {
                AsyncImage(
                    model = channel.imageUrl,
                    contentDescription = channel.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = channel.title,
            color = Color.White.copy(alpha = if (selected) 1f else 0.76f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(164.dp),
        )
    }
}
