package com.smartvision.svplayer.ui.catalog

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.smartvision.svplayer.ui.components.YoutubeLogoIcon
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.home.HeaderControls
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType

val CatalogPanelTitleStyle = TextStyle(
    fontSize = 16.sp,
    lineHeight = 22.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.sp,
)

val CatalogItemTitleStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.sp,
)

val CatalogMetaStyle = TextStyle(
    fontSize = 10.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
)

val CatalogPreviewTitleStyle = TextStyle(
    fontSize = 18.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.sp,
)

@Composable
fun MediaCatalogHeader(
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
        modifier = modifier.height(MediaCatalogDimens.HeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaCatalogLogo()

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
                    trailingContent = if (tab.warning) {
                        {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Indisponible",
                                tint = SmartVisionColors.Warning,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else if (tab.locked) {
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
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .alpha(if (tab.locked || tab.warning) 0.42f else 1f),
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
fun CatalogSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Rechercher",
) {
    var focused by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusStyle = LocalTvFocusStyle.current
    val shape = RoundedCornerShape(7.dp)
    val borderColor by animateColorAsState(
        targetValue = if (focused) focusStyle.accent else SmartVisionColors.Border,
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "catalogSearchBorder",
    )

    LaunchedEffect(editing) {
        if (editing) {
            inputFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        readOnly = !editing,
        cursorBrush = SolidColor(focusStyle.accent),
        textStyle = CatalogMetaStyle.copy(color = SmartVisionColors.TextPrimary),
        modifier = modifier
            .height(34.dp)
            .focusRequester(inputFocusRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (!it.isFocused) {
                    editing = false
                    keyboardController?.hide()
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                        editing = true
                        keyboardController?.show()
                        true
                    }
                    event.key == Key.Back && editing -> {
                        editing = false
                        keyboardController?.hide()
                        true
                    }
                    else -> false
                }
            }
            .clip(shape)
            .background(
                if (focused || editing) focusStyle.background else SmartVisionColors.Surface.copy(alpha = 0.86f),
            )
            .border(
                BorderStroke(if (focused) focusStyle.borderWidth else 1.dp, borderColor),
                shape,
            )
            .padding(horizontal = 10.dp),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (focused) focusStyle.accent else SmartVisionColors.TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(7.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isBlank()) {
                        Text(
                            text = placeholder,
                            color = SmartVisionColors.TextSecondary,
                            style = CatalogMetaStyle,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun MediaCatalogLogo() {
    Image(
        painter = painterResource(R.drawable.smartvision_logo_wide),
        contentDescription = "SmartVision IPTV Player",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(190.dp)
            .fillMaxHeight(),
    )
}

@Composable
fun MediaCatalogPanel(
    title: String,
    modifier: Modifier = Modifier,
    titleContent: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(MediaCatalogDimens.PanelRadius),
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xE80A1323),
                        Color(0xEE07101E),
                    ),
                ),
            )
            .border(BorderStroke(1.dp, SmartVisionColors.Border), shape)
            .padding(MediaCatalogDimens.PanelPadding),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (titleContent != null) {
                titleContent()
            } else {
                Text(
                    text = title,
                    color = SmartVisionColors.TextPrimary,
                    style = CatalogPanelTitleStyle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
        }

        Spacer(Modifier.height(12.dp))

        content()
    }
}

@Composable
fun CatalogCategoryRow(
    label: String,
    count: Int?,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val active = selected || focusState.isFocused
    val shape = RoundedCornerShape(MediaCatalogDimens.ItemRadius)
    val focusStyle = LocalTvFocusStyle.current
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> focusStyle.accent
            selected -> SmartVisionColors.Primary
            else -> SmartVisionColors.Border
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "catalogCategoryBorder",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MediaCatalogDimens.CategoryRowHeight)
            .then(
                if (rightFocusRequester != null) {
                    Modifier.focusProperties { right = rightFocusRequester }
                } else {
                    Modifier
                },
            )
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.04f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = MediaCatalogDimens.ItemRadius,
            )
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .clip(shape)
            .background(
                if (active) {
                    Brush.horizontalGradient(
                        listOf(
                            SmartVisionColors.Primary.copy(alpha = 0.82f),
                            SmartVisionColors.PrimaryDark.copy(alpha = 0.72f),
                        ),
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            SmartVisionColors.Surface.copy(alpha = 0.36f),
                            SmartVisionColors.Surface.copy(alpha = 0.20f),
                        ),
                    )
                },
            )
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else SmartVisionDimensions.PanelBorder,
                    borderColor,
                ),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = label,
            color = SmartVisionColors.TextPrimary,
            style = CatalogItemTitleStyle,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count?.toString().orEmpty(),
            color = if (active) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
            style = CatalogMetaStyle,
            maxLines = 1,
        )
    }
}

@Composable
fun CatalogMediaCard(
    title: String,
    meta: String,
    imageUrl: String?,
    fallbackText: String,
    selected: Boolean,
    favorite: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val active = selected || focusState.isFocused
    val shape = RoundedCornerShape(MediaCatalogDimens.ItemRadius)
    val focusStyle = LocalTvFocusStyle.current
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> focusStyle.accent
            selected -> SmartVisionColors.Primary
            else -> SmartVisionColors.Border.copy(alpha = 0.72f)
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "catalogMediaCardBorder",
    )

    Box(
        modifier = modifier
            .aspectRatio(MediaCatalogDimens.MediaCardAspectRatio)
            .then(
                if (leftFocusRequester != null || rightFocusRequester != null) {
                    Modifier.focusProperties {
                        if (leftFocusRequester != null) left = leftFocusRequester
                        if (rightFocusRequester != null) right = rightFocusRequester
                    }
                } else {
                    Modifier
                },
            )
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.045f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = MediaCatalogDimens.ItemRadius,
            )
            .onFocusChanged { focus ->
                if (focus.isFocused) onFocused()
            }
            .zIndex(if (focusState.isFocused) 3f else 0f)
            .clip(shape)
            .background(SmartVisionColors.SurfaceElevated)
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else SmartVisionDimensions.PanelBorder,
                    borderColor,
                ),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource),
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                SmartVisionColors.Primary.copy(alpha = 0.38f),
                                SmartVisionColors.SurfaceElevated,
                                SmartVisionColors.Background,
                            ),
                            radius = 420f,
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = fallbackText,
                    color = SmartVisionColors.TextPrimary.copy(alpha = 0.82f),
                    style = CatalogPanelTitleStyle,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.04f),
                            Color.Black.copy(alpha = 0.16f),
                            Color.Black.copy(alpha = 0.94f),
                        ),
                    ),
                ),
        )

        if (favorite) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF5D78),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .size(15.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = meta,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 8.sp,
                lineHeight = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun CatalogContentRow(
    number: String,
    title: String,
    subtitle: String,
    meta: String,
    imageUrl: String?,
    fallbackText: String,
    selected: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val active = selected || focusState.isFocused
    val shape = RoundedCornerShape(MediaCatalogDimens.ItemRadius)
    val focusStyle = LocalTvFocusStyle.current
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> focusStyle.accent
            selected -> SmartVisionColors.Primary
            else -> SmartVisionColors.Border
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "catalogContentBorder",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MediaCatalogDimens.ContentRowHeight)
            .tvFocusTarget(
                state = focusState,
                pressed = pressed,
                focusedScale = 1.035f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = MediaCatalogDimens.ItemRadius,
            )
            .onFocusChanged { focus ->
                if (focus.isFocused) {
                    onFocused()
                }
            }
            .then(
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            )
            .focusable(interactionSource = interactionSource)
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .clip(shape)
            .background(
                if (active) {
                    Brush.horizontalGradient(
                        listOf(
                            SmartVisionColors.PrimaryDark.copy(alpha = 0.58f),
                            SmartVisionColors.SurfaceElevated.copy(alpha = 0.94f),
                        ),
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            SmartVisionColors.SurfaceElevated.copy(alpha = 0.70f),
                            SmartVisionColors.Surface.copy(alpha = 0.52f),
                        ),
                    )
                },
            )
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else SmartVisionDimensions.PanelBorder,
                    borderColor,
                ),
                shape,
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = number,
            color = if (active) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
            style = CatalogMetaStyle,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.width(34.dp),
        )

        CatalogThumb(
            imageUrl = imageUrl,
            fallbackText = fallbackText,
            modifier = Modifier.size(width = 56.dp, height = 34.dp),
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SmartVisionColors.TextPrimary,
                style = CatalogItemTitleStyle,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = SmartVisionColors.TextSecondary,
                style = CatalogMetaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = meta,
            color = SmartVisionColors.TextSecondary,
            style = CatalogMetaStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(72.dp),
        )
    }
}

@Composable
fun CatalogThumb(
    imageUrl: String?,
    fallbackText: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
            )
        } else {
            Text(
                text = fallbackText,
                color = SmartVisionColors.TextPrimary,
                style = CatalogItemTitleStyle,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun CatalogPosterFrame(
    imageUrl: String?,
    title: String,
    badge: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(MediaCatalogDimens.ItemRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Black)
            .border(BorderStroke(1.dp, SmartVisionColors.Border), shape),
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                SmartVisionColors.Primary.copy(alpha = 0.32f),
                                SmartVisionColors.Surface,
                                Color.Black,
                            ),
                            radius = 600f,
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Theaters,
                    contentDescription = null,
                    tint = SmartVisionColors.TextSecondary,
                    modifier = Modifier.size(52.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f)),
                    ),
                ),
        )

        CatalogBadge(
            text = badge,
            color = SmartVisionColors.Primary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        )

        Text(
            text = title,
            color = Color.White,
            style = CatalogItemTitleStyle,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
        )
    }
}

@Composable
fun CatalogActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    selected: Boolean = false,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(MediaCatalogDimens.ItemRadius)
    val active = focusState.isFocused || selected
    val focusStyle = LocalTvFocusStyle.current
    val backgroundColor by animateColorAsState(
        targetValue = when {
            primary -> SmartVisionColors.Primary
            active -> SmartVisionColors.SurfaceElevated
            else -> SmartVisionColors.Surface.copy(alpha = 0.70f)
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "catalogActionBackground",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> focusStyle.accent
            selected -> SmartVisionColors.Primary
            else -> SmartVisionColors.Border
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "catalogActionBorder",
    )
    val contentColor = if (primary || active) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary

    Row(
        modifier = modifier
            .tvFocusTarget(
                state = focusState,
                pressed = pressed,
                focusedScale = 1.04f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = MediaCatalogDimens.ItemRadius,
            )
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .clip(shape)
            .background(backgroundColor)
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else SmartVisionDimensions.PanelBorder,
                    borderColor,
                ),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            color = contentColor,
            style = CatalogMetaStyle,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun CatalogBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = CatalogMetaStyle,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
fun CatalogLoading(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = SmartVisionColors.Primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(38.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.Body,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun CatalogEmpty(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Theaters,
                contentDescription = null,
                tint = SmartVisionColors.TextSecondary,
                modifier = Modifier.size(46.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun CatalogError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                color = SmartVisionColors.Error,
                style = SmartVisionType.Body,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            TvButton(
                text = "Reessayer",
                onClick = onRetry,
                variant = TvButtonVariant.Secondary,
                modifier = Modifier.height(42.dp),
            )
        }
    }
}

object MediaCatalogDimens {
    val ScreenPadding = 14.dp
    val TopPadding = 4.dp
    val BottomPadding = 16.dp
    val HeaderHeight = 44.dp
    val HeaderGap = 16.dp
    val PanelGap = 8.dp
    val PanelPadding = 14.dp
    val PanelRadius = 8.dp
    val ItemRadius = 7.dp
    val ListGap = 5.dp
    val CategoryRowHeight = 42.dp
    val ContentRowHeight = 66.dp
    val MediaGridGap = 8.dp
    const val MediaGridColumns = 5
    const val MediaCardAspectRatio = 1.42f
}
