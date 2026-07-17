package com.smartvision.svplayer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType

@Composable
internal fun TvSectionCard(
    title: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xB8081628),
    borderColor: Color = SmartVisionColors.Border.copy(alpha = 0.86f),
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp,
    verticalPadding: androidx.compose.ui.unit.Dp = 14.dp,
    headerTrailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                BorderStroke(borderWidth, borderColor),
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        if (!title.isNullOrBlank() && icon != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SmartVisionColors.CyanAccent,
                    modifier = Modifier.size(23.dp),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = title,
                    color = SmartVisionColors.TextPrimary,
                    style = SmartVisionType.Label,
                    fontWeight = FontWeight.Bold,
                )
                if (headerTrailing != null) {
                    Spacer(Modifier.weight(1f))
                    headerTrailing()
                }
            }
            Spacer(Modifier.size(12.dp))
        }
        content()
    }
}
