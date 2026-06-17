package com.smartvision.svplayer.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.domain.model.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun TopHeader(
    syncStatus: SyncStatus,
    syncIcon: ImageVector,
    settingsIcon: ImageVector,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = buildAnnotatedString {
                    append("Smart")
                    withStyle(
                        SpanStyle(
                            brush = Brush.horizontalGradient(
                                listOf(SVColors.Blue, SVColors.Purple),
                            ),
                        ),
                    ) {
                        append("Vision")
                    }
                },
                color = SVColors.TextPrimary,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "IPTV PLAYER | ANDROID TV",
                color = SVColors.TextSecondary,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FocusableButton(
                text = syncStatus.buttonLabel,
                icon = syncIcon,
                onClick = onSync,
                selected = syncStatus is SyncStatus.Running,
                accent = if (syncStatus is SyncStatus.Error) SVColors.Danger else SVColors.Cyan,
                modifier = Modifier.width(214.dp),
            )
            FocusableButton(
                text = "",
                icon = settingsIcon,
                onClick = onSettings,
                accent = SVColors.TextPrimary,
                modifier = Modifier.size(width = 64.dp, height = 54.dp),
            )
            ClockPanel()
        }
    }
}

@Composable
private fun ClockPanel() {
    var now by remember { mutableStateOf(formatTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = formatTime()
            delay(30_000)
        }
    }
    GlassPanel(modifier = Modifier.size(width = 104.dp, height = 54.dp)) {
        BoxCenter {
            Text(
                text = now,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun BoxCenter(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

private fun formatTime(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
