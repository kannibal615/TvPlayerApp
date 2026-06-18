package com.smartvision.svplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun TvHeader(
    title: String,
    subtitle: String,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SmartVisionDimensions.HeaderHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmartVisionMark()
            Spacer(Modifier.width(SmartVisionDimensions.InternalSpacing))
            Column {
                Text(
                    text = title,
                    color = SmartVisionColors.TextPrimary,
                    style = SmartVisionType.TitleM,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Caption,
                    maxLines = 1,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(SmartVisionDimensions.InternalSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderClock()
            TvButton(
                text = "Parametres",
                onClick = onSettings,
                leadingIcon = Icons.Default.Settings,
                variant = TvButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun SmartVisionMark() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(SmartVisionDimensions.ButtonRadius))
            .background(
                Brush.linearGradient(
                    listOf(
                        SmartVisionColors.Primary,
                        SmartVisionColors.CyanAccent,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = buildAnnotatedString {
                append("S")
                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.86f))) {
                    append("V")
                }
            },
            color = Color.White,
            style = SmartVisionType.TitleS,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HeaderClock() {
    var time by remember { mutableStateOf(formatTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            time = formatTime()
            delay(30_000)
        }
    }

    TvPanel(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = SmartVisionDimensions.InternalSpacing,
            vertical = SmartVisionDimensions.CompactSpacing,
        ),
    ) {
        Text(
            text = time,
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.Label,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun formatTime(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
