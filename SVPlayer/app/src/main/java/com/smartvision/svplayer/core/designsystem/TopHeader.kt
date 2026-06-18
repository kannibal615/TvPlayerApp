package com.smartvision.svplayer.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvision.svplayer.core.navigation.SVRoute
import com.smartvision.svplayer.domain.model.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun TopHeader(
    currentRoute: SVRoute,
    items: List<NavItem>,
    onNavigate: (SVRoute) -> Unit,
    syncStatus: SyncStatus,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LogoSection()

        Spacer(Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Menu items moved to the right, before the action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.filter { it.route != SVRoute.Account && it.route != SVRoute.Settings }.forEach { item ->
                    val selected = item.route.topLevelRoute == currentRoute.topLevelRoute
                    TopNavItem(
                        item = item,
                        selected = selected,
                        onClick = { onNavigate(item.route) }
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Action buttons reduced in size
            FocusableButton(
                text = "Synchroniser",
                icon = Icons.Default.Sync,
                onClick = onSync,
                selected = syncStatus is SyncStatus.Running,
                accent = SVColors.Blue,
                modifier = Modifier
                    .width(136.dp)
                    .height(36.dp),
                minHeight = 36.dp,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            )
            FocusableButton(
                text = "Paramètres",
                icon = Icons.Default.Settings,
                onClick = onSettings,
                accent = SVColors.SurfaceLight,
                modifier = Modifier
                    .width(124.dp)
                    .height(36.dp),
                minHeight = 36.dp,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun LogoSection() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = SVColors.Blue,
                modifier = Modifier.size(32.dp)
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = SVColors.Cyan.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp).padding(start = 4.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White)) {
                    append("Smart")
                }
                withStyle(SpanStyle(color = SVColors.Blue)) {
                    append("Vision")
                }
            },
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp),
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun TopNavItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Color(0xFF1E293B) else Color.Transparent)
            .border(BorderStroke(if (focused) 1.dp else 0.dp, if (focused) SVColors.Cyan else Color.Transparent), shape)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .focusable()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = item.label,
            color = if (selected || focused) Color.White else SVColors.TextSecondary,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium,
        )
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
    GlassPanel(modifier = Modifier.size(width = 76.dp, height = 40.dp)) {
        BoxCenter {
            Text(
                text = now,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 20.sp),
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
