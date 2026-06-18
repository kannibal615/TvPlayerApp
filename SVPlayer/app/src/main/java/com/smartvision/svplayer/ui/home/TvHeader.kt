package com.smartvision.svplayer.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType

data class HomeHeaderTab(
    val label: String,
    val route: String,
)

@Composable
fun TvHeader(
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(SmartVisionDimensions.HomeHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmartVisionLogo()
        Spacer(Modifier.width(24.dp))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                TvButton(
                    text = tab.label,
                    onClick = { onNavigate(tab.route) },
                    selected = tab.route == currentRoute,
                    variant = if (tab.route == currentRoute) TvButtonVariant.Primary else TvButtonVariant.Text,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(40.dp),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvButton(
                text = "Synchroniser",
                onClick = onSync,
                leadingIcon = Icons.Default.Sync,
                variant = TvButtonVariant.Primary,
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier
                    .width(146.dp)
                    .height(40.dp),
            )
            TvButton(
                text = "Paramètres",
                onClick = onSettings,
                leadingIcon = Icons.Default.Settings,
                variant = TvButtonVariant.Secondary,
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier
                    .width(136.dp)
                    .height(40.dp),
            )
        }
    }
}

@Composable
private fun SmartVisionLogo() {
    Row(
        modifier = Modifier
            .width(184.dp)
            .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = SmartVisionColors.Primary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = buildAnnotatedString {
                append("Smart")
                withStyle(SpanStyle(color = SmartVisionColors.Primary)) {
                    append("Vision")
                }
            },
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.TitleS,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
