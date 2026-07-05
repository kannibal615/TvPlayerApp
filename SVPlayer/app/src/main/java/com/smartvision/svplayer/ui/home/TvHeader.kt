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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.smartvision.svplayer.R
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.YoutubeLogoIcon
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

data class HomeHeaderTab(
    val label: String,
    val route: String,
    val icon: ImageVector? = null,
    val useYoutubeLogo: Boolean = false,
    val locked: Boolean = false,
    val warning: Boolean = false,
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
        Spacer(Modifier.width(6.dp))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                HeaderTabButton(
                    tab = tab,
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                    height = 38.dp,
                    horizontalPadding = 6.dp,
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
        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
        HeaderDateTime()
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
    val focusStyle = LocalTvFocusStyle.current
    val shape = RoundedCornerShape(10.dp)

    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(38.dp)
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
                        if (focusState.isFocused) focusStyle.borderWidth else SmartVisionDimensions.PanelBorder,
                        when {
                            focusState.isFocused -> focusStyle.accent
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
                modifier = Modifier.size(20.dp),
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
private fun HeaderDateTime() {
    var dateTime by remember { mutableStateOf(formatHeaderDateTime()) }

    LaunchedEffect(Unit) {
        while (true) {
            dateTime = formatHeaderDateTime()
            delay(1_000)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Box(
            modifier = Modifier
                .height(30.dp)
                .width(1.dp)
                .background(SmartVisionColors.Border.copy(alpha = 0.72f)),
        )
        Column(
            modifier = Modifier.width(75.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = dateTime.time,
                color = SmartVisionColors.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
            Text(
                text = dateTime.date,
                color = SmartVisionColors.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
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
            .offset(x = (-6).dp)
            .width(172.dp)
            .fillMaxHeight(),
    )
}

@Composable
fun HeaderTabButton(
    tab: HomeHeaderTab,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    height: Dp,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(height + if (tab.locked) 8.dp else 0.dp)
            .padding(top = if (tab.locked) 8.dp else 0.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
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
            trailingContent = if (tab.warning) {
                {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Indisponible",
                        tint = SmartVisionColors.Warning,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                null
            },
            enabled = true,
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            modifier = Modifier
                .height(height)
                .alpha(if (tab.locked || tab.warning) 0.42f else 1f),
        )
        if (tab.locked) {
            Image(
                painter = painterResource(R.drawable.premium_crown),
                contentDescription = "Premium",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-2).dp)
                    .graphicsLayer { rotationZ = 10f }
                    .zIndex(4f)
                    .size(18.dp),
            )
        }
    }
}

private data class HeaderDateTimeText(
    val time: String,
    val date: String,
)

private fun formatHeaderDateTime(): HeaderDateTimeText {
    val now = Date()
    return HeaderDateTimeText(
        time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now),
        date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now),
    )
}
