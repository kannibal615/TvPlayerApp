package com.smartvision.svplayer.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.smartvision.svplayer.R
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.YoutubeLogoIcon
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions

data class HomeHeaderTab(
    val label: String,
    val route: String,
    val icon: ImageVector? = null,
    val useYoutubeLogo: Boolean = false,
    val locked: Boolean = false,
)

@Composable
fun TvHeader(
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
    onLicenseKey: () -> Unit,
    showLicenseKey: Boolean,
    hasNewNotifications: Boolean,
    notificationBadgeCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(SmartVisionDimensions.HomeHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmartVisionLogo()
        Spacer(Modifier.width(16.dp))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                TvButton(
                    text = tab.label,
                    onClick = { onNavigate(tab.route) },
                    selected = tab.route == currentRoute,
                    variant = if (tab.route == currentRoute) TvButtonVariant.Primary else TvButtonVariant.Text,
                    leadingIcon = tab.icon,
                    leadingContent = if (tab.useYoutubeLogo) {
                        {
                            YoutubeLogoIcon()
                        }
                    } else {
                        null
                    },
                    trailingContent = if (tab.locked) {
                        {
                            Image(
                                painter = painterResource(R.drawable.premium_crown),
                                contentDescription = "Premium",
                                modifier = Modifier
                                    .offset(x = 2.dp, y = (-6).dp)
                                    .graphicsLayer { rotationZ = 12f }
                                    .size(20.dp),
                            )
                        }
                    } else {
                        null
                    },
                    enabled = true,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .alpha(if (tab.locked) 0.22f else 1f),
                )
            }
        }
        HeaderControls(
            onNotifications = onNotifications,
            onLicenseKey = onLicenseKey,
            onProfile = onProfile,
            onSettings = onSettings,
            showLicenseKey = showLicenseKey,
            hasNewNotifications = hasNewNotifications,
            notificationBadgeCount = notificationBadgeCount,
        )
    }
}

@Composable
fun HeaderControls(
    onNotifications: () -> Unit,
    onLicenseKey: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    showLicenseKey: Boolean,
    hasNewNotifications: Boolean,
    notificationBadgeCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showLicenseKey) {
            HeaderIconButton(
                icon = Icons.Default.Key,
                contentDescription = "Acheter une licence",
                onClick = onLicenseKey,
                accent = SmartVisionColors.Warning,
            )
        }
        HeaderIconButton(
            icon = Icons.Default.Notifications,
            contentDescription = "Notifications",
            onClick = onNotifications,
            showBadge = hasNewNotifications,
            badgeCount = notificationBadgeCount,
        )
        HeaderIconButton(
            icon = Icons.Default.Person,
            contentDescription = "Profil",
            onClick = onProfile,
        )
        HeaderIconButton(
            icon = Icons.Default.Settings,
            contentDescription = "Parametres",
            onClick = onSettings,
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    accent: Color = SmartVisionColors.Primary,
    showBadge: Boolean = false,
    badgeCount: Int = 0,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(10.dp)

    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .tvFocusTarget(
                    state = focusState,
                    pressed = pressed,
                    glowColor = accent,
                    cornerRadius = 10.dp,
                )
                .clip(shape)
                .background(if (focusState.isFocused) SmartVisionColors.SurfaceElevated else Color(0xB8121B2D))
                .border(
                    BorderStroke(
                        if (focusState.isFocused) SmartVisionDimensions.FocusBorder else SmartVisionDimensions.PanelBorder,
                        when {
                            focusState.isFocused -> SmartVisionColors.FocusWhite
                            accent != SmartVisionColors.Primary -> accent.copy(alpha = 0.58f)
                            else -> SmartVisionColors.Border
                        },
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
                tint = when {
                    focusState.isFocused -> SmartVisionColors.TextPrimary
                    accent != SmartVisionColors.Primary -> accent
                    else -> SmartVisionColors.TextSecondary
                },
                modifier = Modifier.size(21.dp),
            )
        }
        if (showBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-1).dp, y = 1.dp)
                    .zIndex(3f)
                    .size(if (badgeCount > 9) 22.dp else 19.dp)
                    .background(Color(0xFFFF2034), RoundedCornerShape(50))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.92f)), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (badgeCount > 9) "9+" else badgeCount.coerceAtLeast(1).toString(),
                    color = Color.White,
                    fontSize = if (badgeCount > 9) 9.sp else 11.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun SmartVisionLogo() {
    Image(
        painter = painterResource(R.drawable.smartvision_logo_wide),
        contentDescription = "SmartVision IPTV Player",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(190.dp)
            .fillMaxHeight(),
    )
}
