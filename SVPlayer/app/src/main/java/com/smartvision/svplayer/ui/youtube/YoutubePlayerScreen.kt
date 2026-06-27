package com.smartvision.svplayer.ui.youtube

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.data.anomaly.AnomalyReporter
import com.smartvision.svplayer.ui.catalog.CatalogMetaStyle
import com.smartvision.svplayer.ui.theme.SmartVisionColors

@Composable
fun YoutubePlayerScreen(
    videoId: String,
    anomalyReporter: AnomalyReporter,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeVideoId = videoId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(32)
    BackHandler(onBack = onBack)

    LaunchedEffect(safeVideoId) {
        if (safeVideoId.isBlank()) {
            anomalyReporter.reportAsync(
                anomalyType = "YOUTUBE_PLAYER_ERROR",
                message = "Identifiant video YouTube invalide",
                context = "route=youtube_player",
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (safeVideoId.isNotBlank()) {
            YoutubeWebPlayer(
                videoId = safeVideoId,
                mode = YoutubePlaybackMode.Fullscreen,
                anomalyReporter = anomalyReporter,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = "Video YouTube indisponible",
                color = SmartVisionColors.TextPrimary,
                style = CatalogMetaStyle.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.32f),
                        0.18f to Color.Transparent,
                    ),
                ),
        )

        Text(
            text = "YouTube",
            color = Color.White.copy(alpha = 0.88f),
            style = CatalogMetaStyle.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 18.dp),
        )
    }
}
