package com.smartvision.svplayer.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.profile.ProfilePickerAvatar
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
    val kind: HomeHeaderTabKind? = null,
)

enum class HomeHeaderTabKind {
    Home,
    LiveTv,
    Movies,
    Series,
    Media,
    Youtube,
}

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
    activeProfileOverride: PlaylistProfile? = null,
    modifier: Modifier = Modifier,
    currentTabFocusRequester: FocusRequester? = null,
    homeTabFocusRequester: FocusRequester? = null,
    licenseFocusRequester: FocusRequester? = null,
    notificationsFocusRequester: FocusRequester? = null,
    profileFocusRequester: FocusRequester? = null,
    settingsFocusRequester: FocusRequester? = null,
    contentDownFocusRequester: FocusRequester? = null,
    onContentDown: (() -> Unit)? = null,
    onProfileAvatarBoundsChanged: (Rect) -> Unit = {},
) {
    val container = LocalAppContainer.current
    val profiles by container.accountManager.profiles.collectAsStateWithLifecycle()
    val activeProfileId by container.accountManager.activeProfileId.collectAsStateWithLifecycle()
    val playerSettings by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = PlayerSettings(),
    )
    val activeProfile = activeProfileOverride ?: profiles.firstOrNull { it.id == activeProfileId }
    val kidsMode = activeProfile?.type == ProfileType.KIDS
    val internalFirstFocusRequester = remember { FocusRequester() }
    val internalLastFocusRequester = remember { FocusRequester() }
    val firstTabRequester = when {
        tabs.firstOrNull()?.route == currentRoute -> currentTabFocusRequester
            ?: homeTabFocusRequester
            ?: internalFirstFocusRequester
        else -> homeTabFocusRequester ?: internalFirstFocusRequester
    }
    val lastControlRequester = when {
        kidsMode -> profileFocusRequester ?: internalLastFocusRequester
        else -> settingsFocusRequester ?: internalLastFocusRequester
    }
    Row(
        modifier = modifier.height(SmartVisionDimensions.HomeHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmartVisionLogo()
        Spacer(Modifier.width(6.dp))
        HeaderTabsRail(
            tabs = tabs,
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            firstTabRequester = firstTabRequester,
            currentTabFocusRequester = currentTabFocusRequester,
            lastControlRequester = lastControlRequester,
            contentDownFocusRequester = contentDownFocusRequester,
            onContentDown = onContentDown,
            modifier = Modifier.weight(1f),
        )
        HeaderActionSeparator()
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
            profileFocusRequester = if (kidsMode) lastControlRequester else profileFocusRequester,
            settingsFocusRequester = if (!kidsMode) lastControlRequester else settingsFocusRequester,
            activeProfile = activeProfile,
            kidsMode = kidsMode,
            showClock = playerSettings.showHeaderClock,
            showSeconds = playerSettings.showHeaderSeconds,
            downFocusRequester = contentDownFocusRequester,
            onDown = onContentDown,
            onWrapToStart = { runCatching { firstTabRequester.requestFocus() } },
            onProfileAvatarBoundsChanged = onProfileAvatarBoundsChanged,
        )
    }
}

@Composable
private fun HeaderTabsRail(
    tabs: List<HomeHeaderTab>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    firstTabRequester: FocusRequester,
    currentTabFocusRequester: FocusRequester?,
    lastControlRequester: FocusRequester,
    contentDownFocusRequester: FocusRequester?,
    onContentDown: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(start = 2.dp, end = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val railY = size.height * 0.76f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        SmartVisionColors.Primary.copy(alpha = 0.38f),
                        Color(0xFF76D9FF).copy(alpha = 0.52f),
                        SmartVisionColors.Primary.copy(alpha = 0.34f),
                        Color.Transparent,
                    ),
                ),
                topLeft = Offset(size.width * 0.06f, railY),
                size = Size(size.width * 0.88f, 1.4.dp.toPx()),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                HeaderTabButton(
                    tab = tab,
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                    height = 38.dp,
                    horizontalPadding = 4.dp,
                    focusRequester = when {
                        index == 0 -> firstTabRequester
                        tab.route == currentRoute -> currentTabFocusRequester
                        else -> null
                    },
                    onLeft = if (index == 0) {
                        { runCatching { lastControlRequester.requestFocus() } }
                    } else {
                        null
                    },
                    downFocusRequester = contentDownFocusRequester,
                    onDown = onContentDown,
                )
            }
        }
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
    showClock: Boolean = true,
    showSeconds: Boolean = true,
    onWrapToStart: (() -> Unit)? = null,
    onProfileAvatarBoundsChanged: (Rect) -> Unit = {},
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
            onRight = onWrapToStart.takeIf { kidsMode },
            onBoundsChanged = onProfileAvatarBoundsChanged,
        )
        if (!kidsMode) {
            HeaderIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Parametres",
                onClick = onSettings,
                downFocusRequester = downFocusRequester,
                onDown = onDown,
                focusRequester = settingsFocusRequester,
                onRight = onWrapToStart,
            )
        }
        if (showClock) {
            HeaderDateTime(showSeconds = showSeconds)
        }
    }
}

@Composable
fun HeaderActionSeparator() {
    Box(
        modifier = Modifier
            .padding(start = 4.dp, end = 10.dp)
            .height(34.dp)
            .width(1.dp)
            .background(SmartVisionColors.Border.copy(alpha = 0.78f)),
    )
}

@Composable
private fun HeaderAvatarButton(
    profile: PlaylistProfile?,
    onClick: () -> Unit,
    downFocusRequester: FocusRequester?,
    onDown: (() -> Unit)?,
    focusRequester: FocusRequester?,
    onRight: (() -> Unit)? = null,
    onBoundsChanged: (Rect) -> Unit = {},
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
                .onGloballyPositioned { coordinates ->
                    onBoundsChanged(coordinates.boundsInRoot())
                }
                .then(if (downFocusRequester != null) Modifier.focusProperties { down = downFocusRequester } else Modifier)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.key == Key.DirectionDown && onDown != null -> {
                            onDown()
                            true
                        }
                        event.key == Key.DirectionRight && onRight != null -> {
                            onRight()
                            true
                        }
                        else -> false
                    }
                }
                .tvFocusTarget(
                    state = focusState,
                    focusRequester = focusRequester,
                    pressed = pressed,
                    glowColor = SmartVisionColors.Primary,
                    cornerRadius = 10.dp,
                )
                .clip(shape)
                .background(if (profile == null) avatarColor else Color.Transparent)
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
            if (profile != null) {
                ProfilePickerAvatar(
                    profile = profile,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
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
    onRight: (() -> Unit)? = null,
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
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.key == Key.DirectionDown && onDown != null -> {
                            onDown()
                            true
                        }
                        event.key == Key.DirectionRight && onRight != null -> {
                            onRight()
                            true
                        }
                        else -> false
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
private fun HeaderDateTime(showSeconds: Boolean) {
    var dateTime by remember(showSeconds) { mutableStateOf(formatHeaderDateTime(showSeconds)) }

    LaunchedEffect(showSeconds) {
        while (true) {
            dateTime = formatHeaderDateTime(showSeconds)
            delay(if (showSeconds) 1_000 else 30_000)
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
    onLeft: (() -> Unit)? = null,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val selected = tab.route == currentRoute
    val emphasized = selected || focusState.isFocused
    val width = if (height <= 34.dp) 56.dp else 66.dp
    val iconSize = if (height <= 34.dp) 23.dp else 26.dp
    val accent = when {
        tab.warning -> SmartVisionColors.Warning
        tab.kind == HomeHeaderTabKind.Youtube || tab.useYoutubeLogo -> Color(0xFFFF2E42)
        else -> SmartVisionColors.Primary
    }

    Box(
        modifier = modifier
            .height(height + if (tab.locked) 8.dp else 0.dp)
            .width(width)
            .padding(top = if (tab.locked) 8.dp else 0.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .height(height)
                .width(width)
                .then(
                    if (downFocusRequester != null) {
                        Modifier.focusProperties { down = downFocusRequester }
                    } else {
                        Modifier
                    },
                )
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.key == Key.DirectionDown && onDown != null -> {
                            onDown()
                            true
                        }
                        event.key == Key.DirectionLeft && onLeft != null -> {
                            onLeft()
                            true
                        }
                        else -> false
                    }
                }
                .tvFocusTarget(
                    state = focusState,
                    focusRequester = focusRequester,
                    pressed = pressed,
                    focusedScale = 1.02f,
                    glowColor = accent,
                    cornerRadius = 8.dp,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onNavigate(tab.route) },
                )
                .focusable(interactionSource = interactionSource)
                .padding(horizontal = horizontalPadding)
                .alpha(if (tab.locked || tab.warning) 0.48f else 1f),
            contentAlignment = Alignment.Center,
        ) {
            HeaderTabFocusLight(
                emphasized = emphasized,
                accent = accent,
                modifier = Modifier.matchParentSize(),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                HeaderTabGlyph(
                    kind = tab.kind ?: tab.inferredKind(),
                    emphasized = emphasized,
                    warning = tab.warning,
                    modifier = Modifier.size(iconSize),
                )
                Text(
                    text = tab.label,
                    color = SmartVisionColors.TextPrimary,
                    fontSize = if (height <= 34.dp) 9.sp else 10.sp,
                    lineHeight = if (height <= 34.dp) 9.sp else 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.alpha(if (emphasized) 1f else 0f),
                )
            }
        }
        if (tab.warning) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Indisponible",
                tint = SmartVisionColors.Warning,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-7).dp, y = 2.dp)
                    .zIndex(5f)
                    .size(14.dp),
            )
        }
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

@Composable
private fun HeaderTabFocusLight(
    emphasized: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    if (!emphasized) return
    Canvas(modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    accent.copy(alpha = 0.32f),
                    accent.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
            ),
            topLeft = Offset(size.width * 0.32f, 0f),
            size = Size(size.width * 0.36f, size.height * 0.72f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.66f), Color.Transparent),
                center = Offset(size.width / 2f, size.height * 0.82f),
                radius = size.width * 0.48f,
            ),
            radius = size.width * 0.36f,
            center = Offset(size.width / 2f, size.height * 0.82f),
        )
    }
}

@Composable
private fun HeaderTabGlyph(
    kind: HomeHeaderTabKind,
    emphasized: Boolean,
    warning: Boolean,
    modifier: Modifier = Modifier,
) {
    val cyan = if (warning) SmartVisionColors.Warning else Color(0xFF50D6FF)
    val glass = if (warning) Color(0xFFFFD28A) else Color(0xFFA7F2FF)
    val deep = if (warning) Color(0xFF6B3E06) else Color(0xFF0B4F85)
    val red = Color(0xFFFF2340)
    val alphaBoost = if (emphasized) 1f else 0.76f

    Canvas(modifier) {
        val strokeThin = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        val glassBrush = Brush.linearGradient(
            colors = listOf(
                glass.copy(alpha = 0.86f * alphaBoost),
                Color.White.copy(alpha = 0.54f * alphaBoost),
                deep.copy(alpha = 0.82f * alphaBoost),
            ),
            start = Offset(w * 0.18f, h * 0.04f),
            end = Offset(w * 0.82f, h),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(cyan.copy(alpha = 0.28f * alphaBoost), Color.Transparent),
                center = Offset(w / 2f, h * 0.82f),
                radius = w * 0.58f,
            ),
            radius = w * 0.5f,
            center = Offset(w / 2f, h * 0.82f),
        )

        when (kind) {
            HomeHeaderTabKind.Home -> {
                val roof = Path().apply {
                    moveTo(w * 0.16f, h * 0.48f)
                    lineTo(w * 0.50f, h * 0.16f)
                    lineTo(w * 0.84f, h * 0.48f)
                    lineTo(w * 0.76f, h * 0.48f)
                    lineTo(w * 0.76f, h * 0.83f)
                    lineTo(w * 0.24f, h * 0.83f)
                    lineTo(w * 0.24f, h * 0.48f)
                    close()
                }
                drawPath(roof, brush = glassBrush)
                drawPath(roof, color = cyan.copy(alpha = 0.78f * alphaBoost), style = strokeThin)
                drawRoundRect(
                    color = Color(0xFF061C3A).copy(alpha = 0.62f),
                    topLeft = Offset(w * 0.41f, h * 0.56f),
                    size = Size(w * 0.18f, h * 0.27f),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
                drawLine(cyan, Offset(w * 0.5f, h * 0.58f), Offset(w * 0.5f, h * 0.82f), 1.dp.toPx(), StrokeCap.Round)
            }
            HomeHeaderTabKind.LiveTv -> {
                drawRoundRect(
                    brush = glassBrush,
                    topLeft = Offset(w * 0.16f, h * 0.28f),
                    size = Size(w * 0.58f, h * 0.44f),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                )
                drawRoundRect(
                    color = Color(0xFF041D3B).copy(alpha = 0.66f),
                    topLeft = Offset(w * 0.22f, h * 0.35f),
                    size = Size(w * 0.46f, h * 0.30f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
                val wave = Path().apply {
                    moveTo(w * 0.25f, h * 0.50f)
                    lineTo(w * 0.34f, h * 0.50f)
                    lineTo(w * 0.39f, h * 0.42f)
                    lineTo(w * 0.46f, h * 0.60f)
                    lineTo(w * 0.53f, h * 0.43f)
                    lineTo(w * 0.61f, h * 0.50f)
                }
                drawPath(wave, color = cyan.copy(alpha = alphaBoost), style = stroke)
                drawLine(cyan, Offset(w * 0.36f, h * 0.22f), Offset(w * 0.28f, h * 0.11f), 1.dp.toPx(), StrokeCap.Round)
                drawLine(cyan, Offset(w * 0.54f, h * 0.22f), Offset(w * 0.64f, h * 0.11f), 1.dp.toPx(), StrokeCap.Round)
                drawLine(cyan.copy(alpha = 0.78f), Offset(w * 0.82f, h * 0.37f), Offset(w * 0.92f, h * 0.30f), 1.5.dp.toPx(), StrokeCap.Round)
                drawLine(cyan.copy(alpha = 0.54f), Offset(w * 0.82f, h * 0.50f), Offset(w * 0.96f, h * 0.50f), 1.5.dp.toPx(), StrokeCap.Round)
                drawLine(cyan.copy(alpha = 0.78f), Offset(w * 0.82f, h * 0.63f), Offset(w * 0.92f, h * 0.70f), 1.5.dp.toPx(), StrokeCap.Round)
            }
            HomeHeaderTabKind.Movies -> {
                drawRoundRect(
                    brush = glassBrush,
                    topLeft = Offset(w * 0.18f, h * 0.34f),
                    size = Size(w * 0.64f, h * 0.43f),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                )
                drawRoundRect(
                    color = Color(0xFF071B32).copy(alpha = 0.7f),
                    topLeft = Offset(w * 0.22f, h * 0.44f),
                    size = Size(w * 0.56f, h * 0.25f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.72f * alphaBoost),
                    topLeft = Offset(w * 0.22f, h * 0.30f),
                    size = Size(w * 0.12f, h * 0.08f),
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.52f * alphaBoost),
                    topLeft = Offset(w * 0.43f, h * 0.30f),
                    size = Size(w * 0.12f, h * 0.08f),
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.72f * alphaBoost),
                    topLeft = Offset(w * 0.64f, h * 0.30f),
                    size = Size(w * 0.12f, h * 0.08f),
                )
                drawCircle(color = cyan.copy(alpha = 0.78f * alphaBoost), radius = w * 0.13f, center = Offset(w * 0.68f, h * 0.57f), style = strokeThin)
                drawCircle(color = Color.White.copy(alpha = 0.72f * alphaBoost), radius = w * 0.025f, center = Offset(w * 0.68f, h * 0.57f))
            }
            HomeHeaderTabKind.Series -> {
                drawRoundRect(
                    brush = glassBrush,
                    topLeft = Offset(w * 0.30f, h * 0.20f),
                    size = Size(w * 0.50f, h * 0.38f),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                )
                drawRoundRect(
                    color = deep.copy(alpha = 0.76f),
                    topLeft = Offset(w * 0.20f, h * 0.34f),
                    size = Size(w * 0.50f, h * 0.38f),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                    style = strokeThin,
                )
                val star = Path().apply {
                    moveTo(w * 0.50f, h * 0.34f)
                    lineTo(w * 0.55f, h * 0.46f)
                    lineTo(w * 0.68f, h * 0.46f)
                    lineTo(w * 0.57f, h * 0.54f)
                    lineTo(w * 0.61f, h * 0.66f)
                    lineTo(w * 0.50f, h * 0.59f)
                    lineTo(w * 0.39f, h * 0.66f)
                    lineTo(w * 0.43f, h * 0.54f)
                    lineTo(w * 0.32f, h * 0.46f)
                    lineTo(w * 0.45f, h * 0.46f)
                    close()
                }
                drawPath(star, color = Color.White.copy(alpha = 0.82f * alphaBoost))
                drawLine(cyan, Offset(w * 0.28f, h * 0.78f), Offset(w * 0.72f, h * 0.78f), 1.3.dp.toPx(), StrokeCap.Round)
            }
            HomeHeaderTabKind.Media -> {
                val folder = Path().apply {
                    moveTo(w * 0.15f, h * 0.34f)
                    lineTo(w * 0.38f, h * 0.34f)
                    lineTo(w * 0.45f, h * 0.25f)
                    lineTo(w * 0.80f, h * 0.25f)
                    quadraticTo(w * 0.86f, h * 0.25f, w * 0.86f, h * 0.32f)
                    lineTo(w * 0.86f, h * 0.74f)
                    quadraticTo(w * 0.86f, h * 0.82f, w * 0.78f, h * 0.82f)
                    lineTo(w * 0.18f, h * 0.82f)
                    quadraticTo(w * 0.12f, h * 0.82f, w * 0.12f, h * 0.74f)
                    lineTo(w * 0.12f, h * 0.42f)
                    quadraticTo(w * 0.12f, h * 0.34f, w * 0.15f, h * 0.34f)
                    close()
                }
                drawPath(folder, brush = glassBrush)
                drawPath(folder, color = cyan.copy(alpha = 0.76f * alphaBoost), style = strokeThin)
                val play = Path().apply {
                    moveTo(w * 0.43f, h * 0.47f)
                    lineTo(w * 0.43f, h * 0.68f)
                    lineTo(w * 0.63f, h * 0.575f)
                    close()
                }
                drawPath(play, color = Color.White.copy(alpha = 0.88f * alphaBoost))
            }
            HomeHeaderTabKind.Youtube -> {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(
                            red.copy(alpha = 0.96f * alphaBoost),
                            Color(0xFFFF6A78).copy(alpha = 0.8f * alphaBoost),
                            Color(0xFF9D0016).copy(alpha = 0.94f * alphaBoost),
                        ),
                    ),
                    topLeft = Offset(w * 0.15f, h * 0.34f),
                    size = Size(w * 0.70f, h * 0.40f),
                    cornerRadius = CornerRadius(7.dp.toPx()),
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.35f * alphaBoost),
                    topLeft = Offset(w * 0.22f, h * 0.39f),
                    size = Size(w * 0.56f, h * 0.06f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
                val play = Path().apply {
                    moveTo(w * 0.44f, h * 0.44f)
                    lineTo(w * 0.44f, h * 0.64f)
                    lineTo(w * 0.64f, h * 0.54f)
                    close()
                }
                drawPath(play, color = Color.White.copy(alpha = 0.94f * alphaBoost))
            }
        }
    }
}

private fun HomeHeaderTab.inferredKind(): HomeHeaderTabKind = when {
    useYoutubeLogo || label.equals("YouTube", ignoreCase = true) -> HomeHeaderTabKind.Youtube
    route.contains("live", ignoreCase = true) -> HomeHeaderTabKind.LiveTv
    route.contains("movie", ignoreCase = true) || route.contains("film", ignoreCase = true) -> HomeHeaderTabKind.Movies
    route.contains("series", ignoreCase = true) -> HomeHeaderTabKind.Series
    route.contains("media", ignoreCase = true) -> HomeHeaderTabKind.Media
    else -> HomeHeaderTabKind.Home
}

private data class HeaderDateTimeText(
    val time: String,
    val date: String,
)

private fun formatHeaderDateTime(showSeconds: Boolean): HeaderDateTimeText {
    val now = Date()
    return HeaderDateTimeText(
        time = SimpleDateFormat(if (showSeconds) "HH:mm:ss" else "HH:mm", Locale.getDefault()).format(now),
        date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now),
    )
}
