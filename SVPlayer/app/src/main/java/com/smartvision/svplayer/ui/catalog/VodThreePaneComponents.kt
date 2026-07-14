@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.smartvision.svplayer.ui.catalog

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.smartvision.svplayer.data.monetization.IdleVastAdLoader
import com.smartvision.svplayer.data.monetization.IdleVastCreative
import com.smartvision.svplayer.data.monetization.MonetizationManager
import com.smartvision.svplayer.data.monetization.smartVisionMediaSourceFactory
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class VodPreviewContent(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val backdropUrl: String?,
    val streamUrl: String?,
    val durationLabel: String?,
    val sideLabel: String?,
    val year: String?,
    val rating: String?,
    val genre: String?,
    val plot: String?,
    val creditLabel: String?,
    val cast: String?,
    val isFavorite: Boolean,
    val loading: Boolean = false,
)

data class VodPreviewEpisode(
    val id: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val durationLabel: String?,
    val progressPercent: Int = 0,
    val resume: Boolean = false,
)

@Composable
fun VodContentRow(
    title: String,
    subtitle: String,
    genre: String?,
    rating: String?,
    sideLabel: String,
    imageUrl: String?,
    fallbackText: String,
    selected: Boolean,
    focusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    upFocusRequester: FocusRequester? = null,
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ratingFirst: Boolean = false,
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
            selected -> focusStyle.selectedAccent
            else -> SmartVisionColors.Border
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "vodContentRowBorder",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(VodContentRowHeight)
            .then(
                if (rightFocusRequester != null || upFocusRequester != null) {
                    Modifier.focusProperties {
                        if (rightFocusRequester != null) right = rightFocusRequester
                        if (upFocusRequester != null) up = upFocusRequester
                    }
                } else {
                    Modifier
                },
            )
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.025f,
                glowColor = focusStyle.accent,
                cornerRadius = MediaCatalogDimens.ItemRadius,
            )
            .onFocusChanged { focus ->
                if (focus.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.key == Key.DirectionLeft && onLeft != null -> {
                        onLeft()
                        true
                    }
                    event.key == Key.DirectionRight && onRight != null -> {
                        onRight()
                        true
                    }
                    event.key == Key.DirectionUp && upFocusRequester != null -> {
                        runCatching { upFocusRequester.requestFocus() }
                        true
                    }
                    event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                        onClick()
                        true
                    }
                    else -> false
                }
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .clip(shape)
            .background(
                if (focusState.isFocused || selected) {
                    val roleBackground = if (focusState.isFocused) focusStyle.background else focusStyle.selectedBackground
                    Brush.horizontalGradient(
                        listOf(
                            roleBackground,
                            SmartVisionColors.SurfaceElevated.copy(alpha = 0.94f),
                        ),
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            SmartVisionColors.SurfaceElevated.copy(alpha = 0.70f),
                            SmartVisionColors.Surface.copy(alpha = 0.50f),
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
            .padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CatalogThumb(
            imageUrl = imageUrl,
            fallbackText = fallbackText,
            modifier = Modifier
                .height(VodContentRowHeight)
                .aspectRatio(16f / 9f),
            crop = true,
            framed = false,
        )
        Spacer(Modifier.width(9.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 3.dp),
        ) {
            Text(
                text = title,
                color = SmartVisionColors.TextPrimary,
                style = CatalogItemTitleStyle,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val ratingContent: @Composable () -> Unit = {
                    rating?.takeIf { it.isNotBlank() }?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("\u2605", color = Color(0xFFFFD54F), style = CatalogMetaStyle, maxLines = 1)
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = it,
                                color = SmartVisionColors.TextPrimary,
                                style = CatalogMetaStyle,
                                maxLines = 1,
                            )
                        }
                    }
                }
                val genreContent: @Composable () -> Unit = {
                    genre?.takeIf { it.isNotBlank() }?.let {
                        VodLineBadge(text = it.substringBefore(",").trim().ifBlank { it })
                    }
                }
                if (ratingFirst) ratingContent() else genreContent()
                if (ratingFirst) genreContent() else ratingContent()
                Text(
                    text = subtitle,
                    color = SmartVisionColors.TextSecondary,
                    style = CatalogMetaStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                sideLabel.takeIf { it.isNotBlank() && it !in subtitle }?.let {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = it,
                        color = SmartVisionColors.TextSecondary,
                        style = CatalogMetaStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun VodLineBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SmartVisionColors.Primary.copy(alpha = 0.22f))
            .border(BorderStroke(1.dp, SmartVisionColors.Primary.copy(alpha = 0.42f)), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = SmartVisionColors.TextPrimary,
            style = CatalogMetaStyle,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun VodCatalogLoadingSkeleton(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "vodCatalogSkeleton")
    val shimmerOffset by transition.animateFloat(
        initialValue = -360f,
        targetValue = 920f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vodCatalogSkeletonOffset",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            SmartVisionColors.Surface.copy(alpha = 0.34f),
            SmartVisionColors.SurfaceElevated.copy(alpha = 0.74f),
            SmartVisionColors.Surface.copy(alpha = 0.34f),
        ),
        start = androidx.compose.ui.geometry.Offset(shimmerOffset, 0f),
        end = androidx.compose.ui.geometry.Offset(shimmerOffset + 280f, 0f),
    )

    Row(
        modifier = modifier.focusProperties { canFocus = false },
        horizontalArrangement = Arrangement.spacedBy(MediaCatalogDimens.PanelGap),
    ) {
        VodSkeletonPanel(
            titleWidth = 104.dp,
            rows = 10,
            rowHeight = 58.dp,
            shimmerBrush = shimmerBrush,
            modifier = Modifier.weight(0.24f),
        )
        VodSkeletonPanel(
            titleWidth = 160.dp,
            rows = 9,
            rowHeight = VodContentRowHeight,
            shimmerBrush = shimmerBrush,
            headerTrailing = true,
            modifier = Modifier.weight(0.42f),
        )
        VodSkeletonPreviewPanel(
            shimmerBrush = shimmerBrush,
            modifier = Modifier.weight(0.34f),
        )
    }
}

@Composable
fun VodContentListLoadingSkeleton(
    rows: Int = 8,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "vodContentListSkeleton")
    val shimmerOffset by transition.animateFloat(
        initialValue = -220f,
        targetValue = 760f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1180, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vodContentListSkeletonOffset",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            SmartVisionColors.Surface.copy(alpha = 0.38f),
            SmartVisionColors.SurfaceElevated.copy(alpha = 0.82f),
            SmartVisionColors.Surface.copy(alpha = 0.38f),
        ),
        start = androidx.compose.ui.geometry.Offset(shimmerOffset, 0f),
        end = androidx.compose.ui.geometry.Offset(shimmerOffset + 260f, 0f),
    )
    Column(
        modifier = modifier
            .focusProperties { canFocus = false }
            .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(MediaCatalogDimens.ListGap),
    ) {
        repeat(rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VodContentRowHeight)
                    .clip(RoundedCornerShape(MediaCatalogDimens.ItemRadius))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                SmartVisionColors.SurfaceElevated.copy(alpha = 0.58f),
                                SmartVisionColors.Surface.copy(alpha = 0.44f),
                            ),
                        ),
                    )
                    .border(
                        BorderStroke(SmartVisionDimensions.PanelBorder, SmartVisionColors.Border.copy(alpha = 0.48f)),
                        RoundedCornerShape(MediaCatalogDimens.ItemRadius),
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(122.dp)
                        .height(VodContentRowHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush),
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(15.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(shimmerBrush),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.48f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(shimmerBrush),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(shimmerBrush),
                )
                Spacer(Modifier.width(10.dp))
            }
        }
    }
}

@Composable
private fun VodSkeletonPanel(
    titleWidth: Dp,
    rows: Int,
    rowHeight: Dp,
    shimmerBrush: Brush,
    modifier: Modifier = Modifier,
    headerTrailing: Boolean = false,
) {
    MediaCatalogPanel(
        title = "",
        modifier = modifier,
        trailing = if (headerTrailing) {
            {
                Box(
                    modifier = Modifier
                        .width(190.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(shimmerBrush),
                )
            }
        } else {
            null
        },
    ) {
        Box(
            modifier = Modifier
                .width(titleWidth)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush),
        )
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(MediaCatalogDimens.ListGap)) {
            repeat(rows) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .clip(RoundedCornerShape(MediaCatalogDimens.ItemRadius))
                        .background(shimmerBrush),
                )
            }
        }
    }
}

@Composable
private fun VodSkeletonPreviewPanel(
    shimmerBrush: Brush,
    modifier: Modifier = Modifier,
) {
    MediaCatalogPanel(
        title = "",
        modifier = modifier,
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush),
                    )
                }
            }
        },
    ) {
        Box(
            modifier = Modifier
                .width(82.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush),
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(5.dp))
                .background(shimmerBrush),
        )
        Spacer(Modifier.height(10.dp))
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(shimmerBrush),
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
fun VodPreviewPanel(
    title: String,
    content: VodPreviewContent?,
    playFocusRequester: FocusRequester,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    onFavorite: () -> Unit,
    onDeleteHistory: () -> Unit,
    modifier: Modifier = Modifier,
    showDeleteHistory: Boolean = false,
    showFreeAdsPreview: Boolean = false,
    idleAdEnabled: Boolean = false,
    idleVastAdLoader: IdleVastAdLoader? = null,
    monetizationManager: MonetizationManager? = null,
    premiumPurchaseUrl: String = "",
    tvCode: String = "",
    seriesEpisodes: List<VodPreviewEpisode> = emptyList(),
    selectedSeriesEpisodeId: Int? = null,
    onSeriesEpisodeSelected: (Int) -> Unit = {},
    seasonsLabel: String = "Seasons",
    episodesLoadingLabel: String = "Loading...",
    episodesEmptyLabel: String = "No episodes available.",
    resumeLabel: String = "Resume",
    progressLabel: String = "Progress",
    onNavigateLeft: (() -> Unit)? = null,
) {
    val miniPlayerFocusRequester = remember { FocusRequester() }
    val isSeriesPreview = content?.id?.startsWith("series-") == true
    val availableSeasons = remember(seriesEpisodes) {
        seriesEpisodes.map { it.seasonNumber }.distinct().sorted()
    }
    var selectedSeason by remember(content?.id, selectedSeriesEpisodeId, availableSeasons) {
        mutableIntStateOf(
            seriesEpisodes.firstOrNull { it.id == selectedSeriesEpisodeId }?.seasonNumber
                ?: availableSeasons.firstOrNull()
                ?: 1,
        )
    }
    val visibleSeriesEpisodes = remember(seriesEpisodes, selectedSeason) {
        seriesEpisodes.filter { it.seasonNumber == selectedSeason }
    }
    MediaCatalogPanel(
        title = title,
        modifier = modifier,
        trailing = {
            if (content != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PreviewIconButton(
                        icon = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        focusRequester = playFocusRequester,
                        downFocusRequester = miniPlayerFocusRequester,
                        onClick = onPlay,
                        primary = true,
                        onLeft = onNavigateLeft,
                    )
                    PreviewIconButton(
                        icon = Icons.Default.Info,
                        contentDescription = "Details",
                        downFocusRequester = miniPlayerFocusRequester,
                        onClick = onDetails,
                    )
                    if (showDeleteHistory) {
                        PreviewIconButton(
                            icon = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            downFocusRequester = miniPlayerFocusRequester,
                            onClick = onDeleteHistory,
                            danger = true,
                        )
                    } else {
                        PreviewIconButton(
                            icon = if (content.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favori",
                            downFocusRequester = miniPlayerFocusRequester,
                            onClick = onFavorite,
                            selected = content.isFavorite,
                        )
                    }
                }
            }
        },
    ) {
        if (content == null) {
            if (showFreeAdsPreview) {
                Column(modifier = Modifier.fillMaxSize()) {
                    VodIdlePreviewAdFrame(
                        enabled = idleAdEnabled,
                        idleVastAdLoader = idleVastAdLoader,
                        monetizationManager = monetizationManager,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                    )
                    Spacer(Modifier.height(10.dp))
                    VodPremiumPreviewCard(
                        purchaseUrl = premiumPurchaseUrl,
                        tvCode = tvCode,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                VodIdlePreviewPrompt(modifier = Modifier.fillMaxSize())
            }
            return@MediaCatalogPanel
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft && onNavigateLeft != null) {
                        onNavigateLeft()
                        true
                    } else {
                        false
                    }
                },
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 10.dp),
        ) {
            item(key = "${content.id}-player") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .focusRequester(miniPlayerFocusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                                runCatching { playFocusRequester.requestFocus() }
                                true
                            } else {
                                false
                            }
                        }
                        .focusable()
                        .clip(RoundedCornerShape(MediaCatalogDimens.ItemRadius))
                        .background(Color.Black)
                        .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(MediaCatalogDimens.ItemRadius)),
                ) {
                    SegmentedVodMiniPlayer(
                        content = content,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }

            item(key = "${content.id}-details") {
                PreviewDetailItem {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        CatalogPosterFrame(
                            imageUrl = content.imageUrl,
                            title = content.title,
                            badge = content.durationLabel ?: content.sideLabel ?: "VOD",
                            modifier = Modifier
                                .width(104.dp)
                                .aspectRatio(2f / 3f),
                            showTitle = isSeriesPreview,
                            showBadge = isSeriesPreview,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = content.title,
                                color = SmartVisionColors.TextPrimary,
                                style = CatalogPreviewTitleStyle,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!isSeriesPreview) {
                                Spacer(Modifier.height(8.dp))
                                CatalogActionButton(
                                    text = "Play",
                                    icon = Icons.Default.PlayArrow,
                                    onClick = onPlay,
                                    primary = true,
                                    modifier = Modifier
                                        .width(112.dp)
                                        .height(36.dp),
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = listOfNotNull(content.year, content.durationLabel, content.rating?.let { "\u2605 $it" })
                                    .joinToString("  |  ")
                                    .ifBlank { content.subtitle },
                                color = SmartVisionColors.TextSecondary,
                                style = CatalogMetaStyle,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!content.genre.isNullOrBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = content.genre,
                                    color = SmartVisionColors.Primary,
                                    style = CatalogMetaStyle,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (content.loading) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "Chargement de l'episode...",
                                    color = SmartVisionColors.TextSecondary,
                                    style = CatalogMetaStyle,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            if (isSeriesPreview) {
                content.plot?.takeIf { it.isNotBlank() }?.let { plot ->
                    item(key = "${content.id}-series-plot") {
                        PreviewInfoLine(label = "Resume", value = plot)
                    }
                }
                item(key = "${content.id}-season-selector") {
                    SeriesSeasonSelector(
                        seasons = availableSeasons,
                        selectedSeason = selectedSeason,
                        onSeasonSelected = { selectedSeason = it },
                        label = seasonsLabel,
                    )
                }
                if (content.loading) {
                    item(key = "${content.id}-episodes-loading") {
                        PreviewInfoLine(label = "Episodes", value = episodesLoadingLabel)
                    }
                } else if (visibleSeriesEpisodes.isEmpty()) {
                    item(key = "${content.id}-episodes-empty") {
                        PreviewInfoLine(label = "Episodes", value = episodesEmptyLabel)
                    }
                } else {
                    items(
                        items = visibleSeriesEpisodes,
                        key = { episode -> "${content.id}-episode-${episode.id}" },
                    ) { episode ->
                        SeriesPreviewEpisodeRow(
                            episode = episode,
                            selected = episode.id == selectedSeriesEpisodeId,
                            onClick = { onSeriesEpisodeSelected(episode.id) },
                            resumeLabel = resumeLabel,
                            progressLabel = progressLabel,
                        )
                    }
                }
            }

            if (!isSeriesPreview) {
                item(key = "${content.id}-plot") {
                    PreviewInfoLine(label = "Resume", value = content.plot?.takeIf { it.isNotBlank() } ?: "Aucun resume disponible.")
                }

                content.durationLabel?.takeIf { it.isNotBlank() }?.let { duration ->
                    item(key = "${content.id}-duration") {
                        PreviewInfoLine(label = "Duree", value = duration)
                    }
                }
                content.sideLabel?.takeIf { it.isNotBlank() && it != content.durationLabel }?.let { sideLabel ->
                    item(key = "${content.id}-side") {
                        PreviewInfoLine(label = "Info", value = sideLabel)
                    }
                }
                content.year?.takeIf { it.isNotBlank() }?.let { year ->
                    item(key = "${content.id}-year") {
                        PreviewInfoLine(label = "Annee", value = year)
                    }
                }
                content.rating?.takeIf { it.isNotBlank() }?.let { rating ->
                    item(key = "${content.id}-rating") {
                        PreviewInfoLine(label = "Note", value = "\u2605 $rating/10")
                    }
                }
                content.genre?.takeIf { it.isNotBlank() }?.let { genre ->
                    item(key = "${content.id}-genre") {
                        PreviewInfoLine(label = "Genre", value = genre)
                    }
                }

                content.creditLabel?.takeIf { it.isNotBlank() }?.let { creditLabel ->
                    item(key = "${content.id}-credit") {
                        PreviewInfoLine(label = creditLabel.substringBefore(':'), value = creditLabel.substringAfter(':', creditLabel))
                    }
                }
                content.cast?.takeIf { it.isNotBlank() }?.let { cast ->
                    item(key = "${content.id}-cast") {
                        PreviewInfoLine(label = "Cast", value = cast)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesSeasonSelector(
    seasons: List<Int>,
    selectedSeason: Int,
    onSeasonSelected: (Int) -> Unit,
    label: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SmartVisionColors.Surface.copy(alpha = 0.72f))
            .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
            .padding(10.dp),
    ) {
        Text(
            text = label.uppercase(),
            color = SmartVisionColors.TextSecondary,
            style = CatalogMetaStyle,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(seasons, key = { it }) { season ->
                val focusState = rememberTvFocusState()
                val active = season == selectedSeason
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .width(72.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            when {
                                focusState.isFocused -> LocalTvFocusStyle.current.background
                                active -> LocalTvFocusStyle.current.parentBackground
                                else -> Color.White.copy(alpha = 0.05f)
                            },
                        )
                        .border(
                            BorderStroke(
                                if (focusState.isFocused) 2.dp else 1.dp,
                                when {
                                    focusState.isFocused -> LocalTvFocusStyle.current.accent
                                    active -> LocalTvFocusStyle.current.parentAccent
                                    else -> SmartVisionColors.Border
                                },
                            ),
                            RoundedCornerShape(7.dp),
                        )
                        .tvFocusTarget(state = focusState, focusedScale = 1.03f)
                        .clickable(onClick = { onSeasonSelected(season) })
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "S${season.toString().padStart(2, '0')}",
                        color = SmartVisionColors.TextPrimary,
                        style = CatalogMetaStyle,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesPreviewEpisodeRow(
    episode: VodPreviewEpisode,
    selected: Boolean,
    onClick: () -> Unit,
    resumeLabel: String,
    progressLabel: String,
) {
    val focusState = rememberTvFocusState()
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                when {
                    focusState.isFocused -> LocalTvFocusStyle.current.background
                    selected -> LocalTvFocusStyle.current.selectedBackground
                    else -> SmartVisionColors.Surface.copy(alpha = 0.64f)
                },
            )
            .border(
                BorderStroke(
                    if (focusState.isFocused) 2.dp else 1.dp,
                    when {
                        focusState.isFocused -> LocalTvFocusStyle.current.accent
                        selected -> LocalTvFocusStyle.current.selectedAccent
                        else -> SmartVisionColors.Border
                    },
                ),
                shape,
            )
            .tvFocusTarget(state = focusState, focusedScale = 1.015f)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "S${episode.seasonNumber.toString().padStart(2, '0')}E${episode.episodeNumber.toString().padStart(2, '0')}",
                color = if (selected) LocalTvFocusStyle.current.accent else SmartVisionColors.CyanAccent,
                style = CatalogMetaStyle,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(68.dp),
            )
            Text(
                text = episode.title,
                color = SmartVisionColors.TextPrimary,
                style = CatalogMetaStyle,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            episode.durationLabel?.takeIf { it.isNotBlank() }?.let { duration ->
                Text(text = duration, color = SmartVisionColors.TextSecondary, style = CatalogMetaStyle)
            }
        }
        if (episode.resume || episode.progressPercent > 0) {
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (episode.resume) resumeLabel.uppercase() else progressLabel.uppercase(),
                    color = SmartVisionColors.CyanAccent,
                    style = CatalogMetaStyle,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(78.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.10f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(episode.progressPercent.coerceIn(0, 100) / 100f)
                            .height(3.dp)
                            .background(SmartVisionColors.CyanAccent),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("${episode.progressPercent}%", color = SmartVisionColors.TextSecondary, style = CatalogMetaStyle)
            }
        }
    }
}

@Composable
private fun VodIdlePreviewPrompt(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .focusProperties { canFocus = false }
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Theaters,
            contentDescription = null,
            tint = SmartVisionColors.Primary,
            modifier = Modifier.size(54.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Selectionnez un contenu pour lancer l'apercu.",
            color = SmartVisionColors.TextSecondary,
            style = CatalogMetaStyle,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VodPremiumPreviewCard(
    purchaseUrl: String,
    tvCode: String,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(purchaseUrl) { createVodPreviewQrBitmap(purchaseUrl.ifBlank { "https://smartvisions.net" }, 384) }
    val gold = Color(0xFFFFD47A)
    val shape = RoundedCornerShape(5.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false }
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0A1828),
                        Color(0xFF020914),
                        Color.Black,
                    ),
                    center = androidx.compose.ui.geometry.Offset(220f, 0f),
                    radius = 560f,
                ),
            )
            .border(BorderStroke(0.5.dp, gold.copy(alpha = 0.58f)), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, gold.copy(alpha = 0.52f)))),
            )
            VodCrown(
                color = gold,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(width = 32.dp, height = 22.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(gold.copy(alpha = 0.52f), Color.Transparent))),
            )
        }
        Text(
            text = "Passer Premium",
            color = gold,
            style = CatalogPreviewTitleStyle.copy(fontSize = 19.sp, lineHeight = 23.sp),
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Scannez le QR code depuis votre telephone",
            color = SmartVisionColors.TextPrimary,
            style = CatalogMetaStyle.copy(fontSize = 13.sp, lineHeight = 17.sp),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White)
                .padding(5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR code SmartVision Premium",
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(7.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Code TV",
                color = gold.copy(alpha = 0.94f),
                style = CatalogMetaStyle.copy(fontSize = 13.sp, lineHeight = 17.sp),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = tvCode.ifBlank { "------" },
                color = gold,
                style = CatalogPreviewTitleStyle.copy(fontSize = 20.sp, lineHeight = 23.sp),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun VodCrown(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.focusProperties { canFocus = false }) {
        val stroke = 1.7.dp.toPx()
        val baseTop = size.height * 0.72f
        val baseBottom = size.height * 0.88f
        val crown = Path().apply {
            moveTo(size.width * 0.12f, baseTop)
            lineTo(size.width * 0.27f, size.height * 0.36f)
            lineTo(size.width * 0.42f, baseTop)
            lineTo(size.width * 0.50f, size.height * 0.18f)
            lineTo(size.width * 0.58f, baseTop)
            lineTo(size.width * 0.73f, size.height * 0.36f)
            lineTo(size.width * 0.88f, baseTop)
            lineTo(size.width * 0.80f, baseBottom)
            lineTo(size.width * 0.20f, baseBottom)
            close()
        }
        drawPath(crown, color.copy(alpha = 0.92f))
        drawPath(
            crown,
            Color.White.copy(alpha = 0.36f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        listOf(0.27f, 0.50f, 0.73f).forEach { x ->
            drawCircle(
                color = color,
                radius = size.minDimension * 0.07f,
                center = androidx.compose.ui.geometry.Offset(size.width * x, size.height * if (x == 0.50f) 0.15f else 0.32f),
            )
        }
    }
}

@Composable
private fun VodIdlePreviewAdFrame(
    enabled: Boolean,
    idleVastAdLoader: IdleVastAdLoader?,
    monetizationManager: MonetizationManager?,
    modifier: Modifier = Modifier,
) {
    var adTagUrl by remember { mutableStateOf<String?>(null) }
    var configLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(monetizationManager) {
        adTagUrl = monetizationManager?.idleLivePreviewAdTagUrl()
        configLoaded = true
    }

    val shape = RoundedCornerShape(MediaCatalogDimens.ItemRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Black)
            .border(BorderStroke(1.dp, SmartVisionColors.Border), shape)
            .focusProperties { canFocus = false },
        contentAlignment = Alignment.Center,
    ) {
        val loader = idleVastAdLoader
        val manager = monetizationManager
        val tagUrl = adTagUrl
        if (tagUrl != null && enabled && loader != null && manager != null) {
            var adCycle by remember(tagUrl) { mutableIntStateOf(0) }
            key(adCycle) {
                VodIdlePreviewVastPlayer(
                    adTagUrl = tagUrl,
                    idleVastAdLoader = loader,
                    monetizationManager = manager,
                    onCycleFinished = { retryDelayMillis ->
                        delay(retryDelayMillis)
                        adCycle += 1
                    },
                    modifier = Modifier.matchParentSize(),
                )
            }
        } else {
            if (!configLoaded) {
                CircularProgressIndicator(
                    color = SmartVisionColors.Primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(30.dp),
                )
            } else {
                VodIdlePreviewPlaceholder()
            }
        }
    }
}

@Composable
private fun VodIdlePreviewVastPlayer(
    adTagUrl: String,
    idleVastAdLoader: IdleVastAdLoader,
    monetizationManager: MonetizationManager,
    onCycleFinished: suspend (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var loading by remember(adTagUrl) { mutableStateOf(true) }
    var adStarted by remember(adTagUrl) { mutableStateOf(false) }
    var adFinished by remember(adTagUrl) { mutableStateOf(false) }
    var adFailed by remember(adTagUrl) { mutableStateOf(false) }
    var cycleRestarted by remember(adTagUrl) { mutableStateOf(false) }
    var creative by remember(adTagUrl) { mutableStateOf<IdleVastCreative?>(null) }
    var firedEvents by remember(adTagUrl) { mutableStateOf(emptySet<String>()) }
    val playerView = remember {
        PlayerView(context).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false
            isLongClickable = false
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
    }
    val adMediaSourceFactory = remember(context) { smartVisionMediaSourceFactory(context) }
    val player = remember(adMediaSourceFactory) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(adMediaSourceFactory)
            .build()
            .apply {
                volume = 0f
                playWhenReady = true
            }
    }

    LaunchedEffect(adTagUrl) {
        loading = true
        adStarted = false
        adFinished = false
        adFailed = false
        firedEvents = emptySet()
        player.stop()
        player.clearMediaItems()
        creative = idleVastAdLoader.load(adTagUrl)
        val mediaUrl = creative?.mediaUrl
        if (mediaUrl.isNullOrBlank()) {
            loading = false
            adFailed = true
            monetizationManager.onIdleLivePreviewAdFailed("creative VAST indisponible")
        } else {
            monetizationManager.onIdleLivePreviewAdLoaded()
            player.setMediaItem(MediaItem.fromUri(mediaUrl))
            player.prepare()
            player.playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && !adStarted) {
                    loading = false
                    adStarted = true
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    adFinished = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                loading = false
                adFailed = true
                coroutineScope.launch {
                    monetizationManager.onIdleLivePreviewAdFailed(error.message.orEmpty())
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            playerView.player = null
            player.clearMediaItems()
            player.release()
        }
    }

    LaunchedEffect(adStarted) {
        if (adStarted) {
            idleVastAdLoader.ping(creative?.impressionUrls.orEmpty())
            idleVastAdLoader.ping(creative?.trackingUrls?.get("start").orEmpty())
            monetizationManager.onIdleLivePreviewAdStarted()
        }
    }

    LaunchedEffect(adStarted, adFinished, adFailed, creative) {
        while (adStarted && !adFinished && !adFailed) {
            val duration = player.duration.takeIf { it > 0L } ?: 0L
            if (duration > 0L) {
                val progress = player.currentPosition.toDouble() / duration.toDouble()
                val event = when {
                    progress >= 0.75 && "thirdQuartile" !in firedEvents -> "thirdQuartile"
                    progress >= 0.50 && "midpoint" !in firedEvents -> "midpoint"
                    progress >= 0.25 && "firstQuartile" !in firedEvents -> "firstQuartile"
                    else -> null
                }
                if (event != null) {
                    firedEvents = firedEvents + event
                    idleVastAdLoader.ping(creative?.trackingUrls?.get(event).orEmpty())
                }
            }
            delay(500)
        }
    }

    LaunchedEffect(adFinished, adFailed) {
        if ((adFinished || adFailed) && !cycleRestarted) {
            cycleRestarted = true
            if (adFinished) {
                idleVastAdLoader.ping(creative?.trackingUrls?.get("complete").orEmpty())
            }
            player.pause()
            player.stop()
            player.clearMediaItems()
            onCycleFinished(if (adFailed) 5_000L else 1_000L)
        }
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = {
                playerView.apply {
                    this.player = player
                    clearFocus()
                }
            },
            update = {
                it.player = player
                it.clearFocus()
                it.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            },
            modifier = Modifier
                .matchParentSize()
                .focusProperties { canFocus = false },
        )

        if (loading && !adFailed) {
            CircularProgressIndicator(
                color = SmartVisionColors.Primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(30.dp),
            )
        }

        if (adStarted && !adFinished && !adFailed) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SmartVisionColors.PrimaryDark.copy(alpha = 0.82f))
                    .border(BorderStroke(1.dp, SmartVisionColors.Primary.copy(alpha = 0.65f)), RoundedCornerShape(4.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "PUBLICITE",
                    color = Color.White,
                    style = CatalogMetaStyle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }

        if (adFailed) {
            VodIdlePreviewPlaceholder()
        }
    }
}

@Composable
private fun VodIdlePreviewPlaceholder() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.Tv,
            contentDescription = null,
            tint = SmartVisionColors.Primary,
            modifier = Modifier.size(34.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Apercu SmartVision",
            color = SmartVisionColors.TextPrimary,
            style = CatalogMetaStyle,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

private fun createVodPreviewQrBitmap(content: String, size: Int): Bitmap {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1,
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        val offset = y * size
        for (x in 0 until size) {
            pixels[offset + x] = if (matrix[x, y]) {
                android.graphics.Color.BLACK
            } else {
                android.graphics.Color.WHITE
            }
        }
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}

@Composable
private fun PreviewIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    primary: Boolean = false,
    selected: Boolean = false,
    danger: Boolean = false,
    onLeft: (() -> Unit)? = null,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(5.dp)
    val focusStyle = LocalTvFocusStyle.current
    val active = focusState.isFocused || selected || primary
    val background = when {
        danger && focusState.isFocused -> SmartVisionColors.Error.copy(alpha = 0.34f)
        primary -> SmartVisionColors.Primary.copy(alpha = 0.86f)
        active -> SmartVisionColors.SurfaceElevated
        else -> SmartVisionColors.Surface.copy(alpha = 0.70f)
    }
    val borderColor = when {
        danger && focusState.isFocused -> SmartVisionColors.Error
        focusState.isFocused -> focusStyle.accent
        selected -> SmartVisionColors.Primary
        else -> SmartVisionColors.Border
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.08f,
                glowColor = if (danger) SmartVisionColors.Error else SmartVisionColors.Primary,
                cornerRadius = 5.dp,
            )
            .then(
                if (downFocusRequester != null) {
                    Modifier.focusProperties { down = downFocusRequester }
                } else {
                    Modifier
                },
            )
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft && onLeft != null) {
                    onLeft()
                    true
                } else {
                    false
                }
            }
            .clip(shape)
            .background(background)
            .border(BorderStroke(if (focusState.isFocused) focusStyle.borderWidth else 1.dp, borderColor), shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SegmentedVodMiniPlayer(
    content: VodPreviewContent,
    modifier: Modifier = Modifier,
) {
    val streamUrl = content.streamUrl
    val posterUrl = content.backdropUrl ?: content.imageUrl
    if (streamUrl.isNullOrBlank()) {
        LandscapePosterFallback(
            imageUrl = posterUrl,
            title = content.title,
            message = "Preview indisponible",
            modifier = modifier,
        )
        return
    }

    val context = LocalContext.current
    val audioScope = rememberCoroutineScope()
    var buffering by remember(content.id) { mutableStateOf(true) }
    var showPoster by remember(content.id) { mutableStateOf(false) }
    var errorText by remember(content.id) { mutableStateOf<String?>(null) }
    var readyNonce by remember(content.id) { mutableIntStateOf(0) }
    var sequenceStarted by remember(content.id) { mutableStateOf(false) }
    val volumeFadeJob = remember { arrayOfNulls<Job>(1) }
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    fun restartPreviewAudioFade() {
        volumeFadeJob[0]?.cancel()
        player.volume = 0f
        volumeFadeJob[0] = audioScope.launch { player.fadeInVodMiniPlayerVolume() }
    }

    LaunchedEffect(content.id, streamUrl) {
        volumeFadeJob[0]?.cancel()
        showPoster = false
        errorText = null
        buffering = true
        readyNonce = 0
        sequenceStarted = false
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
    }

    LaunchedEffect(content.id, readyNonce) {
        if (readyNonce == 0 || errorText != null) return@LaunchedEffect
        buffering = false
        player.play()
        restartPreviewAudioFade()
        delay(VodPreviewWarmupMillis)

        var durationMs = player.duration
        var attempts = 0
        while ((durationMs <= 0 || durationMs == C.TIME_UNSET) && attempts < 20) {
            delay(250)
            durationMs = player.duration
            attempts += 1
        }

        val validDuration = durationMs.takeIf { it > 0 && it != C.TIME_UNSET }
        if (validDuration == null) {
            delay(VodPreviewSegmentMillis)
            player.pause()
            player.volume = 0f
            showPoster = true
            return@LaunchedEffect
        }

        VodPreviewPercents.forEach { percent ->
            runCatching {
                player.seekTo((validDuration * percent / 100f).toLong().coerceIn(0L, validDuration))
            }
            player.play()
            restartPreviewAudioFade()
            buffering = false
            delay(VodPreviewSegmentMillis)
        }
        volumeFadeJob[0]?.cancel()
        player.pause()
        player.volume = 0f
        showPoster = true
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_READY) {
                    errorText = null
                    if (!sequenceStarted) {
                        sequenceStarted = true
                        readyNonce += 1
                    }
                }
                if (playbackState == Player.STATE_ENDED) {
                    showPoster = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                volumeFadeJob[0]?.cancel()
                buffering = false
                showPoster = true
                errorText = if (posterUrl.isNullOrBlank()) "Preview indisponible" else null
            }
        }
        player.addListener(listener)
        onDispose {
            volumeFadeJob[0]?.cancel()
            player.volume = 0f
            player.removeListener(listener)
            player.release()
        }
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    this.player = player
                }
            },
            update = {
                it.player = player
                it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            },
            modifier = Modifier.matchParentSize(),
        )

        if (showPoster || errorText != null) {
            LandscapePosterFallback(
                imageUrl = posterUrl,
                title = content.title,
                message = errorText,
                modifier = Modifier.matchParentSize(),
            )
        }

        if (buffering && !showPoster && errorText == null) {
            CircularProgressIndicator(
                color = SmartVisionColors.Primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun LandscapePosterFallback(
    imageUrl: String?,
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.22f)),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Theaters,
                contentDescription = null,
                tint = SmartVisionColors.Primary,
                modifier = Modifier.size(52.dp),
            )
        }
        message?.let {
            Text(
                text = it,
                color = Color.White,
                style = CatalogMetaStyle,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun PreviewInfoLine(label: String, value: String) {
    PreviewDetailItem {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$label:",
                color = SmartVisionColors.Primary,
                style = CatalogMetaStyle,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.width(70.dp),
            )
            Text(
                text = value,
                color = SmartVisionColors.TextSecondary,
                style = CatalogMetaStyle,
                maxLines = if (label == "Resume") 7 else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PreviewDetailItem(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val shape = RoundedCornerShape(5.dp)
    val focusStyle = LocalTvFocusStyle.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusTarget(
                state = focusState,
                pressed = false,
                focusedScale = 1.01f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = 5.dp,
            )
            .clip(shape)
            .background(
                if (focusState.isFocused) {
                    SmartVisionColors.PrimaryDark.copy(alpha = 0.34f)
                } else {
                    SmartVisionColors.Surface.copy(alpha = 0.22f)
                },
            )
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else 1.dp,
                    if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border.copy(alpha = 0.42f),
                ),
                shape,
            )
            .focusable()
            .padding(horizontal = 8.dp, vertical = 7.dp),
    ) {
        content()
    }
}

private suspend fun ExoPlayer.fadeInVodMiniPlayerVolume() {
    delay(VodMiniPlayerAudioStartDelayMillis)
    repeat(VodMiniPlayerAudioFadeSteps) { step ->
        delay(VodMiniPlayerAudioFadeMillis / VodMiniPlayerAudioFadeSteps)
        volume = (step + 1).toFloat() / VodMiniPlayerAudioFadeSteps
    }
    volume = 1f
}

private val VodPreviewPercents = listOf(10, 30, 50, 80)
private val VodContentRowHeight = 56.dp
private const val VodPreviewWarmupMillis = 3_000L
private const val VodPreviewSegmentMillis = 15_000L
private const val VodMiniPlayerAudioStartDelayMillis = 1_000L
private const val VodMiniPlayerAudioFadeMillis = 1_000L
private const val VodMiniPlayerAudioFadeSteps = 10
