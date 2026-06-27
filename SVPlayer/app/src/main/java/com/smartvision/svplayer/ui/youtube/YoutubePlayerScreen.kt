package com.smartvision.svplayer.ui.youtube

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.smartvision.svplayer.data.anomaly.AnomalyReporter
import com.smartvision.svplayer.ui.catalog.CatalogMetaStyle
import com.smartvision.svplayer.ui.catalog.MediaCatalogDimens
import com.smartvision.svplayer.ui.catalog.MediaCatalogHeader
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.theme.SmartVisionColors

@Composable
fun YoutubePlayerScreen(
    videoId: String,
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    anomalyReporter: AnomalyReporter,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
    onLicenseKey: () -> Unit,
    showLicenseKey: Boolean,
    hasNewNotifications: Boolean,
    notificationBadgeCount: Int,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SmartVisionColors.Background)
            .padding(horizontal = MediaCatalogDimens.ScreenPadding)
            .padding(top = MediaCatalogDimens.TopPadding, bottom = MediaCatalogDimens.BottomPadding),
    ) {
        MediaCatalogHeader(
            currentRoute = currentRoute,
            tabs = tabs,
            onNavigate = onNavigate,
            onSync = onSync,
            onSettings = onSettings,
            onProfile = onProfile,
            onNotifications = onNotifications,
            onLicenseKey = onLicenseKey,
            showLicenseKey = showLicenseKey,
            hasNewNotifications = hasNewNotifications,
            notificationBadgeCount = notificationBadgeCount,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(
            modifier = Modifier
                .padding(top = MediaCatalogDimens.HeaderGap)
                .fillMaxWidth()
                .weight(1f)
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
        }
    }
}
