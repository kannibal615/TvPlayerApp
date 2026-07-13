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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.ProfileType
import com.smartvision.svplayer.core.data.LocalAppContainer
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

enum class HomeHeaderFocusTarget {
    CurrentTab,
    License,
    Notifications,
    Profile,
    Settings,
}

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
    currentTabFocusRequester: FocusRequester? = null,
    homeTabFocusRequester: FocusRequester? = null,
    licenseFocusRequester: FocusRequester? = null,
    notificationsFocusRequester: FocusRequester? = null,
    profileFocusRequester: FocusRequester? = null,
    settingsFocusRequester: FocusRequester? = null,
    contentDownFocusRequester: FocusRequester? = null,
    onContentDown: (() -> Unit)? = null,
) {
    val container = LocalAppContainer.current
    val profiles by container.accountManager.profiles.collectAsStateWithLifecycle()
    val activeProfileId by container.accountManager.activeProfileId.collectAsStateWithLifecycle()
    val activeProfile = profiles.firstOrNull { it.id == activeProfileId }
    val kidsMode = activeProfile?.type == ProfileType.KIDS
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
            tabs.forEachIndexed { index, tab ->
                HeaderTabButton(
                    tab = tab,
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                    height = 38.dp,
                    horizontalPadding = 6.dp,
                    focusRequester = when {
                        tab.route == currentRoute -> currentTabFocusRequester
                        index == 0 -> homeTabFocusRequester
                        else -> null
                    },
                    downFocusRequester = contentDownFocusRequester,
                    onDown = onContentDown,
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
            licenseFocusRequester = licenseFocusRequester,
            notificationsFocusRequester = notificationsFocusRequester,
            profileFocusRequester = profileFocusRequester,
            settingsFocusRequester = settingsFocusRequester,
            activeProfile = activeProfile,
            kidsMode = kidsMode,
            downFocusRequester = contentDownFocusRequester,
            onDown = onContentDown,
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
    downFocusRequester: FocusRequester? = null,
    onDown: (() -> Unit)? = null,
    licenseFocusRequester: FocusRequester? = null,
    notificationsFocusRequester: FocusRequester? = null,
    profileFocusRequester: FocusRequester? = null,
    settingsFocusRequester: FocusRequester? = null,
    activeProfile: PlaylistProfile? = null,
    kidsMode: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showLicenseKey && !kidsMode) {
            HeaderIconButton(
                icon = Icons.Default.Key,
            contentDescription = "Acheter une licence",
            onClick = onLicenseKey,
            accent = SmartVisionColors.Warning,
            downFocusRequester = downFocusRequester,
            onDown = onDown,
            focusRequester = licenseFocusRequester,
        )
        }
        if (!kidsMode) {
            HeaderIconButton(
                icon = Icons.Default.Notifications,
                contentDescription = "Notifications",
                onClick = onNotifications,
                showBadge = hasNewNotifications,
                badgeCount = notificationBadgeCount,
                downFocusRequester = downFocusRequester,
                onDown = onDown,
                focusRequester = notificationsFocusRequester,
            )
        }
        HeaderAvatarButton(
            profile = activeProfile,
            onClick = onProfile,
            downFocusRequester = downFocusRequester,
            onDown = onDown,
            focusRequester = profileFocusRequester,
        )
        if (!kidsMode) {
            HeaderIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Parametres",
                onClick = onSettings,
                downFocusRequester = downFocusRequester,
                onDown = onDown,
                focusRequester = settingsFocusRequester,
            )
        }
        HeaderDateTime()
    }
}

@Composable
private fun HeaderAvatarButton(
    profile: PlaylistProfile?,
    onClick: () -> Unit,
    downFocusRequester: FocusRequester?,
    onDown: (() -> Unit)?,
    focusRequester: FocusRequester?,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focusStyle = LocalTvFocusStyle.current
    val shape = RoundedCornerShape(10.dp)
    val avatarColor = remember(profile?.avatarColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(profile?.avatarColorHex.orEmpty())) }
            .getOrDefault(SmartVisionColors.Primary)
    }
    val initials = profile?.name.orEmpty().trim().split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { "P" }
    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .then(if (downFocusRequester != null) Modifier.focusProperties { down = downFocusRequester } else Modifier)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown && onDown != null) {
                        onDown()
                        true
                    } else false
                }
                .tvFocusTarget(
                    state = focusState,
                    focusRequester = focusRequester,
                    pressed = pressed,
                    glowColor = SmartVisionColors.Primary,
                    cornerRadius = 10.dp,
                )
                .clip(shape)
                .background(avatarColor)
                .border(
                    BorderStroke(
                        if (focusState.isFocused) focusStyle.borderWidth else SmartVisionDimensions.PanelBorder,
                        if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border,
                    ),
                    shape,
                )
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .focusable(interactionSource = interactionSource),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
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
    downFocusRequester: FocusRequester? = null,
    onDown: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
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
                .then(
                    if (downFocusRequester != null) {
                        Modifier.focusProperties { down = downFocusRequester }
                    } else {
                        Modifier
                    },
                )
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown && onDown != null) {
                        onDown()
                        true
                    } else {
                        false
                    }
                }
                .tvFocusTarget(
                    state = focusState,
                    focusRequester = focusRequester,
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
    focusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    onDown: (() -> Unit)? = null,
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
                .then(
                    if (downFocusRequester != null) {
                        Modifier.focusProperties { down = downFocusRequester }
                    } else {
                        Modifier
                    },
                )
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown && onDown != null) {
                        onDown()
                        true
                    } else {
                        false
                    }
                }
                .alpha(if (tab.locked || tab.warning) 0.42f else 1f),
            focusRequester = focusRequester,
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
