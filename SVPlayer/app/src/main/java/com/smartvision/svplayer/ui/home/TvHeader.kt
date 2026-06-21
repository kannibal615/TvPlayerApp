package com.smartvision.svplayer.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderIconButton(
                icon = Icons.Default.Notifications,
                contentDescription = "Notifications",
                onClick = {},
            )
            HeaderIconButton(
                icon = Icons.Default.Person,
                contentDescription = "Profil",
                onClick = {},
            )
            HeaderIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Parametres",
                onClick = onSettings,
            )
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .size(40.dp)
            .tvFocusTarget(
                state = focusState,
                pressed = pressed,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = 10.dp,
            )
            .clip(shape)
            .background(if (focusState.isFocused) SmartVisionColors.SurfaceElevated else Color(0xB8121B2D))
            .border(
                BorderStroke(
                    if (focusState.isFocused) SmartVisionDimensions.FocusBorder else SmartVisionDimensions.PanelBorder,
                    if (focusState.isFocused) SmartVisionColors.FocusWhite else SmartVisionColors.Border,
                ),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (focusState.isFocused) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
            modifier = Modifier.size(21.dp),
        )
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
            modifier = Modifier.size(34.dp),
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
