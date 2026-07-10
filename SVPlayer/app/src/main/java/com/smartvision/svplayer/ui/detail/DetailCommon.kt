package com.smartvision.svplayer.ui.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.smartvision.svplayer.R
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.home.HeaderControls
import com.smartvision.svplayer.ui.home.HeaderTabButton
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import kotlinx.coroutines.launch

val DetailHeroTitleStyle = TextStyle(
    fontSize = 42.sp,
    lineHeight = 48.sp,
    fontWeight = FontWeight.Black,
    letterSpacing = 0.sp,
)

val DetailTitleStyle = TextStyle(
    fontSize = 24.sp,
    lineHeight = 30.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.sp,
)

val DetailBodyStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
)

val DetailMetaStyle = TextStyle(
    fontSize = 11.sp,
    lineHeight = 15.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
)

object DetailDimens {
    val ScreenPadding = 30.dp
    val HeaderHeight = 42.dp
    val HeaderTop = 18.dp
    val ActionHeight = 42.dp
    val ItemRadius = 7.dp
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.detailBringIntoViewOnFocus(): Modifier {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    return bringIntoViewRequester(bringIntoViewRequester)
        .onFocusChanged { state ->
            if (state.isFocused) {
                scope.launch { bringIntoViewRequester.bringIntoView() }
            }
        }
}

@Composable
fun DetailBackground(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020713)),
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFB020713),
                            Color(0xE9020713),
                            Color(0x83020713),
                            Color(0xEA020713),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xB6020713),
                            Color.Transparent,
                            Color(0xF5020713),
                        ),
                    ),
                ),
        )
        content()
    }
}

@Composable
fun DetailHeader(
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
    contentDownFocusRequester: FocusRequester? = null,
    onContentDown: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.height(DetailDimens.HeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DetailLogo()
        Spacer(Modifier.width(28.dp))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                HeaderTabButton(
                    tab = tab,
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                    height = 34.dp,
                    horizontalPadding = 6.dp,
                    focusRequester = currentTabFocusRequester.takeIf { tab.route == currentRoute },
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
            downFocusRequester = contentDownFocusRequester,
            onDown = onContentDown,
        )
    }
}

@Composable
private fun DetailLogo() {
    Image(
        painter = painterResource(R.drawable.smartvision_logo_wide),
        contentDescription = "SmartVision IPTV Player",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(178.dp)
            .fillMaxHeight(),
    )
}

@Composable
fun DetailActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    bringIntoViewOnFocus: Boolean = true,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(DetailDimens.ItemRadius)
    val active = selected || focusState.isFocused
    val backgroundColor by animateColorAsState(
        targetValue = when {
            primary -> SmartVisionColors.Primary
            active -> Color(0xCC152033)
            else -> Color(0x94111A2A)
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "detailActionBackground",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> SmartVisionColors.FocusWhite
            selected -> SmartVisionColors.Primary
            else -> Color.White.copy(alpha = 0.12f)
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "detailActionBorder",
    )
    val contentColor = when {
        !enabled -> SmartVisionColors.TextSecondary.copy(alpha = 0.48f)
        primary || active -> Color.White
        else -> SmartVisionColors.TextSecondary
    }

    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.56f)
            .then(if (bringIntoViewOnFocus) Modifier.detailBringIntoViewOnFocus() else Modifier)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.04f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = DetailDimens.ItemRadius,
            )
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .clip(shape)
            .background(backgroundColor)
            .border(
                BorderStroke(if (focusState.isFocused) 2.dp else 1.dp, borderColor),
                shape,
            )
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            color = contentColor,
            style = DetailMetaStyle,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun DetailIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailActionButton(
        text = "",
        icon = icon,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun DetailBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = SmartVisionColors.PrimaryDark,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = DetailMetaStyle,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
