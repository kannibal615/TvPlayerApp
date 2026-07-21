package com.smartvision.svplayer.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
import com.smartvision.svplayer.ui.focus.LocalTvAnimationsEnabled
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
    @DrawableRes val iconRes: Int,
    val iconStyle: HeaderIconStyle = HeaderIconStyle.Monochrome,
    val locked: Boolean = false,
    val warning: Boolean = false,
)

sealed interface HeaderIconStyle {
    data object Monochrome : HeaderIconStyle
    data object OriginalColors : HeaderIconStyle
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(HeaderTabSpacing, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                key(tab.route) {
                    HeaderTabButton(
                        tab = tab,
                        onNavigate = onNavigate,
                        height = SmartVisionDimensions.HomeHeaderHeight,
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

private val HeaderTabWidth = 58.dp
private val HeaderTabSpacing = 12.dp
private val HeaderTabIconSize = 34.dp
private val HeaderFocusSurfaceSize = 40.dp

@Composable
fun HeaderTabButton(
    tab: HomeHeaderTab,
    onNavigate: (String) -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    onDown: (() -> Unit)? = null,
    onLeft: (() -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val animationsEnabled = LocalTvAnimationsEnabled.current
    val density = LocalDensity.current
    val transition = updateTransition(
        targetState = isFocused,
        label = "headerMenuFocus",
    )
    val scale by transition.animateFloat(
        transitionSpec = {
            if (animationsEnabled) {
                tween(
                    durationMillis = if (targetState) 200 else 170,
                    easing = FastOutSlowInEasing,
                )
            } else {
                snap()
            }
        },
        label = "headerMenuScale",
    ) { focused ->
        if (focused) 1.03f else 1f
    }
    val iconAlpha by transition.animateFloat(
        transitionSpec = {
            if (animationsEnabled) tween(if (targetState) 180 else 160) else snap()
        },
        label = "headerIconAlpha",
    ) { focused ->
        if (focused) 1f else 0.82f
    }
    val focusSurfaceAlpha by transition.animateFloat(
        transitionSpec = {
            if (animationsEnabled) tween(if (targetState) 180 else 150) else snap()
        },
        label = "headerFocusSurfaceAlpha",
    ) { focused ->
        if (focused) 1f else 0f
    }
    val labelAlpha by transition.animateFloat(
        transitionSpec = {
            if (animationsEnabled) tween(if (targetState) 180 else 140) else snap()
        },
        label = "headerMenuLabelAlpha",
    ) { focused ->
        if (focused) 1f else 0f
    }
    val labelOffset by transition.animateDp(
        transitionSpec = {
            if (animationsEnabled) tween(if (targetState) 180 else 140, easing = FastOutSlowInEasing) else snap()
        },
        label = "headerMenuLabelOffset",
    ) { focused ->
        if (focused) 0.dp else 6.dp
    }
    val labelOffsetPx = with(density) { labelOffset.toPx() }
    val requesterModifier = focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
    val muted = tab.locked || tab.warning

    Box(
        modifier = modifier
            .height(height + if (tab.locked) 8.dp else 0.dp)
            .width(HeaderTabWidth)
            .padding(top = if (tab.locked) 8.dp else 0.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .height(height)
                .width(HeaderTabWidth),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = if (muted) 0.48f else 1f
                    }
                    .then(requesterModifier)
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
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused || focusState.hasFocus
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onNavigate(tab.route) },
                    )
                    .focusable(interactionSource = interactionSource),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-1).dp)
                        .size(HeaderFocusSurfaceSize),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = focusSurfaceAlpha }
                            .background(
                                Color.White.copy(alpha = 0.09f),
                                RoundedCornerShape(10.dp),
                            )
                            .border(
                                BorderStroke(1.25.dp, Color.White.copy(alpha = 0.88f)),
                                RoundedCornerShape(10.dp),
                            ),
                    )
                    HeaderTabGlyph(
                        tab = tab,
                        iconAlpha = iconAlpha,
                        modifier = Modifier.size(HeaderTabIconSize),
                    )
                }
                Text(
                    text = tab.label,
                    color = SmartVisionColors.TextPrimary,
                    fontSize = 9.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .width(HeaderTabWidth)
                        .height(10.dp)
                        .graphicsLayer {
                            alpha = labelAlpha
                            translationY = labelOffsetPx
                        },
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
                    .offset(x = (-5).dp, y = 2.dp)
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
private fun HeaderTabGlyph(
    tab: HomeHeaderTab,
    iconAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(tab.iconRes),
        contentDescription = tab.label,
        tint = when (tab.iconStyle) {
            HeaderIconStyle.Monochrome -> Color.White.copy(alpha = iconAlpha)
            HeaderIconStyle.OriginalColors -> Color.Unspecified
        },
        modifier = modifier,
    )
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
