package com.smartvision.svplayer.ui.live

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.behavior.BehaviorContent
import com.smartvision.svplayer.data.monetization.IdleVastAdLoader
import com.smartvision.svplayer.data.monetization.IdleVastCreative
import com.smartvision.svplayer.data.monetization.MonetizationStatus
import com.smartvision.svplayer.data.monetization.MonetizationManager
import com.smartvision.svplayer.data.monetization.monetizationStatus
import com.smartvision.svplayer.data.monetization.smartVisionMediaSourceFactory
import com.smartvision.svplayer.ui.activation.XtreamQrSetupPanel
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.catalog.CatalogSearchField
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.home.TvHeader
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val LiveTvPanelTitleStyle = TextStyle(
    fontSize = 16.sp,
    lineHeight = 22.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.sp,
)

private val LiveTvItemTitleStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.sp,
)

private val LiveTvChannelTitleStyle = TextStyle(
    fontSize = 15.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.sp,
)

private val LiveTvItemMetaStyle = TextStyle(
    fontSize = 10.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
)

private val LiveTvEpgTitleStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 17.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.sp,
)

@Composable
fun LiveTvScreen(
    strings: SmartVisionStrings,
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
    returnFocusChannelId: Int? = null,
    onReturnFocusConsumed: () -> Unit = {},
    onWatch: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: LiveTvViewModel = viewModel(
        factory = viewModelFactory {
            LiveTvViewModel(
                xtreamRepository = container.xtreamRepository,
                catalogRepository = container.catalogRepository,
                userContentRepository = container.userContentRepository,
                settingsRepository = container.settingsRepository,
                epgRepository = container.epgRepository,
            )
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by container.accountManager.accounts.collectAsStateWithLifecycle()
    val m3uUrl by container.accountManager.m3uUrl.collectAsStateWithLifecycle()
    val activePlaylistSource by container.accountManager.activePlaylistSource.collectAsStateWithLifecycle()
    val hasPlayableSource = accounts.isNotEmpty() || (activePlaylistSource == PlaylistSource.M3u && m3uUrl.isNotBlank())
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    val firstChannelFocusRequester = remember { FocusRequester() }
    val epgDetailsFocusRequester = remember { FocusRequester() }
    val firstPreviewActionFocusRequester = remember { FocusRequester() }
    val headerLiveFocusRequester = remember { FocusRequester() }
    val behaviorScope = rememberCoroutineScope()
    var inputReady by remember { mutableStateOf(false) }
    var minimumLoadingComplete by remember { mutableStateOf(false) }
    var contentSearchQuery by remember { mutableStateOf("") }
    var showFreeAdsPreview by remember { mutableStateOf(false) }
    var tvCode by remember { mutableStateOf("") }
    var channelToDelete by remember { mutableStateOf<LiveTvChannel?>(null) }
    var premiumPurchaseUrl by remember {
        mutableStateOf(
            BuildConfig.ACTIVATION_BASE_URL.trimEnd('/') +
                "/account/?source=tv&intent=license&plan=year_1",
        )
    }

    LaunchedEffect(Unit) {
        delay(260)
        inputReady = true
    }

    LaunchedEffect(Unit) {
        delay(1_000)
        minimumLoadingComplete = true
    }

    LaunchedEffect(returnFocusChannelId) {
        val channelId = returnFocusChannelId ?: return@LaunchedEffect
        viewModel.restoreFocusToChannel(channelId)
        onReturnFocusConsumed()
    }

    LaunchedEffect(container.activationRepository) {
        val activation = container.activationRepository.localState.first()
        tvCode = activation.publicDeviceCode.ifBlank { activation.deviceId.take(8).uppercase() }
        val deviceQuery = when {
            activation.publicDeviceCode.isNotBlank() -> "device=${activation.publicDeviceCode}"
            activation.deviceId.isNotBlank() -> "device_id=${activation.deviceId}"
            else -> ""
        }
        premiumPurchaseUrl = BuildConfig.ACTIVATION_BASE_URL.trimEnd('/') +
            "/account/?source=tv&intent=license" +
            if (deviceQuery.isBlank()) {
                "&plan=year_1"
            } else {
                "&$deviceQuery&plan=year_1"
            }
    }

    LaunchedEffect(container.activationRepository) {
        container.activationRepository.localState.collect { activation ->
            showFreeAdsPreview = activation.monetizationStatus() == MonetizationStatus.FREE_WITH_ADS
            tvCode = activation.publicDeviceCode.ifBlank { activation.deviceId.take(8).uppercase() }
        }
    }

    val selectedCategoryVisible = state.categories.any { category -> category.id == state.selectedCategoryId }
    val categoryFocusTargetAvailable = selectedCategoryVisible || state.categories.isNotEmpty()

    LaunchedEffect(state.categoriesLoading, hasPlayableSource, categoryFocusTargetAvailable) {
        if (hasPlayableSource && !state.categoriesLoading && categoryFocusTargetAvailable) {
            withFrameNanos { }
            delay(120)
            runCatching { selectedCategoryFocusRequester.requestFocus() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SmartVisionColors.PrimaryDark.copy(alpha = 0.36f),
                        SmartVisionColors.Background,
                        Color(0xFF01040C),
                    ),
                    center = Offset(980f, 120f),
                    radius = 1500f,
                ),
            )
            .padding(horizontal = LiveTvDimens.ScreenPadding)
            .padding(top = LiveTvDimens.TopPadding, bottom = LiveTvDimens.BottomPadding),
    ) {
        TvHeader(
            currentRoute = currentRoute,
            tabs = tabs,
            onNavigate = onNavigate,
            onSync = onSync,
            onSettings = onSettings,
            onProfile = onProfile,
            onNotifications = onNotifications,
            onLicenseKey = onLicenseKey,
            showLicenseKey = showLicenseKey,
            hasNewNotifications = hasNewNotifications,
            notificationBadgeCount = notificationBadgeCount,
            modifier = Modifier.fillMaxWidth(),
            currentTabFocusRequester = headerLiveFocusRequester,
            contentDownFocusRequester = selectedCategoryFocusRequester,
        )

        Spacer(Modifier.height(LiveTvDimens.HeaderGap))

        if (!hasPlayableSource) {
            XtreamQrSetupPanel(
                activationRepository = container.activationRepository,
                title = strings.liveTvConfigureChannels,
                onManualAccount = { account ->
                    val accountId = container.accountManager.upsert(account)
                    container.accountManager.select(accountId)
                    container.xtreamRepository.clearCaches()
                    container.xtreamRepository.getLiveCategories()
                    container.synchronizeCatalog()
                },
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }

        if (state.categoriesLoading || !minimumLoadingComplete) {
            LiveTvLoadingSkeleton(
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(LiveTvDimens.PanelGap),
            ) {
                CategoryList(
                    state = state,
                    strings = strings,
                    selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                    headerFocusRequester = headerLiveFocusRequester,
                    contentSearchQuery = contentSearchQuery,
                    onCategory = { category ->
                        if (inputReady) {
                            viewModel.selectCategory(
                                category = category,
                                autoPreviewFirstChannel = !showFreeAdsPreview,
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(0.24f)
                        .fillMaxHeight(),
                )
                ChannelList(
                    state = state,
                    strings = strings,
                    firstChannelFocusRequester = firstChannelFocusRequester,
                    selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                    headerFocusRequester = headerLiveFocusRequester,
                    searchQuery = contentSearchQuery,
                    onSearchQueryChange = { contentSearchQuery = it },
                    previewActionFocusRequester = firstPreviewActionFocusRequester.takeIf {
                        state.selectedChannel != null
                    },
                    onChannelFocused = viewModel::focusChannel,
                    onChannelClick = { channel ->
                        if (inputReady) {
                            val openFullPlayer = viewModel.activateChannel(channel)
                            viewModel.refreshChannelEpg(channel, container.accountManager.epgUrl.value)
                            if (openFullPlayer) {
                                container.behaviorReporter.reportAsync(
                                    behaviorScope,
                                    "CONTENT_OPENED",
                                    channel.toBehaviorContent(state.selectedCategory?.label),
                                )
                                onWatch(channel.streamId)
                            }
                        }
                    },
                    onLoadNextPage = viewModel::loadNextPage,
                    onRetry = viewModel::retryCurrentCategory,
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                )
                PreviewPanel(
                    channel = state.selectedChannel,
                    strings = strings,
                    selectedCategoryLabel = state.selectedCategory?.label,
                    showHistoryDelete = state.isHistoryCategory && state.selectedChannel != null,
                    showFreeAdsPreview = showFreeAdsPreview,
                    idleAdContentUrl = state.channels.firstOrNull()?.streamUrl,
                    premiumPurchaseUrl = premiumPurchaseUrl,
                    tvCode = tvCode,
                    idleVastAdLoader = container.idleVastAdLoader,
                    monetizationManager = container.monetizationManager,
                    firstActionFocusRequester = firstPreviewActionFocusRequester,
                    epgDetailsFocusRequester = epgDetailsFocusRequester,
                    selectedChannelFocusRequester = firstChannelFocusRequester,
                    headerFocusRequester = headerLiveFocusRequester,
                    onWatch = {
                        if (inputReady) {
                            state.selectedChannel?.let { channel ->
                                container.behaviorReporter.reportAsync(
                                    behaviorScope,
                                    "CONTENT_OPENED",
                                    channel.toBehaviorContent(state.selectedCategory?.label),
                                )
                                onWatch(channel.streamId)
                            }
                        }
                    },
                    onFavorite = {
                        state.selectedChannel?.let { channel ->
                            container.behaviorReporter.reportAsync(
                                behaviorScope,
                                if (channel.isFavorite) "FAVORITE_REMOVED" else "FAVORITE_ADDED",
                                channel.toBehaviorContent(state.selectedCategory?.label),
                            )
                            viewModel.toggleFavorite(channel)
                        }
                    },
                    onDeleteHistory = {
                        state.selectedChannel?.let { channel -> channelToDelete = channel }
                    },
                    modifier = Modifier
                        .weight(0.34f)
                        .fillMaxHeight(),
                )
            }
        }
    }

    channelToDelete?.let { channel ->
        ConfirmHistoryDeleteDialog(
            title = strings.liveTvDeleteHistoryTitle,
            itemName = channel.name,
            onDismiss = { channelToDelete = null },
            onConfirm = {
                channelToDelete = null
                viewModel.deleteHistoryChannel(channel)
            },
        )
    }
}

@Composable
private fun CategoryList(
    state: LiveTvUiState,
    strings: SmartVisionStrings,
    selectedCategoryFocusRequester: FocusRequester,
    headerFocusRequester: FocusRequester,
    contentSearchQuery: String,
    onCategory: (LiveTvCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleCategories = state.categories
    val focusCategoryId = visibleCategories.firstOrNull { it.id == state.selectedCategoryId }?.id
        ?: visibleCategories.firstOrNull()?.id
    val listState = rememberLazyListState()
    LaunchedEffect(focusCategoryId, visibleCategories.size) {
        val index = visibleCategories.indexOfFirst { it.id == focusCategoryId }
        if (index >= 0) {
            runCatching { listState.animateScrollToItem(index) }
        }
    }

    LiveTvPanel(
        title = strings.liveTvCategories,
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(LiveTvDimens.ListGap),
            contentPadding = PaddingValues(bottom = LiveTvDimens.ListGap),
        ) {
            items(
                visibleCategories,
                key = { it.id },
            ) { category ->
                CategoryRow(
                    category = category,
                    selected = category.id == state.selectedCategoryId,
                    focusRequester = if (category.id == focusCategoryId) {
                        selectedCategoryFocusRequester
                    } else {
                        null
                    },
                    upFocusRequester = headerFocusRequester.takeIf { category.id == visibleCategories.firstOrNull()?.id },
                    onClick = { onCategory(category) },
                )
            }
        }
    }
}

@Composable
private fun ChannelList(
    state: LiveTvUiState,
    strings: SmartVisionStrings,
    firstChannelFocusRequester: FocusRequester,
    selectedCategoryFocusRequester: FocusRequester,
    headerFocusRequester: FocusRequester,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    previewActionFocusRequester: FocusRequester?,
    onChannelFocused: (LiveTvChannel) -> Unit,
    onChannelClick: (LiveTvChannel) -> Unit,
    onLoadNextPage: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val visibleChannels = state.channels.filter { channel ->
        searchQuery.isBlank() || channel.name.contains(searchQuery, ignoreCase = true)
    }
    val focusChannelId = state.selectedChannelId
        ?.takeIf { selectedId -> visibleChannels.any { it.streamId == selectedId } }
        ?: state.focusedChannelId?.takeIf { focusedId -> visibleChannels.any { it.streamId == focusedId } }
        ?: visibleChannels.firstOrNull()?.streamId
    LaunchedEffect(focusChannelId, state.channelsLoading, visibleChannels.size) {
        if (!state.channelsLoading && focusChannelId != null) {
            visibleChannels.indexOfFirst { it.streamId == focusChannelId }
                .takeIf { it >= 0 }
                ?.let { index -> listState.animateScrollToItem((index - 2).coerceAtLeast(0)) }
            withFrameNanos { }
            delay(80)
            runCatching { firstChannelFocusRequester.requestFocus() }
        }
    }
    LaunchedEffect(listState, visibleChannels.size, state.hasMoreItems, state.nextPageLoading, searchQuery) {
        if (searchQuery.isNotBlank()) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastVisibleIndex ->
                val remaining = visibleChannels.lastIndex - lastVisibleIndex
                if (state.hasMoreItems && !state.nextPageLoading && remaining <= LiveTvNextPageThreshold) {
                    onLoadNextPage()
                }
            }
    }
    LiveTvPanel(
        title = strings.liveTvChannels,
        modifier = modifier,
        trailing = {
            CatalogSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = strings.liveTvSearchPlaceholder,
                modifier = Modifier
                    .width(190.dp)
                    .focusProperties { up = headerFocusRequester },
            )
        },
    ) {
        when {
            state.channelsLoading && visibleChannels.isEmpty() -> LoadingState(
                title = strings.liveTvLoadingChannels,
                modifier = Modifier.fillMaxSize(),
            )

            state.errorMessage != null && visibleChannels.isEmpty() -> ErrorState(
                message = state.errorMessage,
                retryLabel = strings.liveTvRetry,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )

            visibleChannels.isEmpty() -> EmptyState(
                title = if (searchQuery.isBlank()) strings.liveTvNoChannels else strings.liveTvNoResults,
                subtitle = if (searchQuery.isBlank()) strings.liveTvSelectAnotherCategory else strings.liveTvChangeSearch,
                modifier = Modifier.fillMaxSize(),
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(LiveTvDimens.ListGap),
                contentPadding = PaddingValues(top = LiveTvDimens.ChannelListTopPadding, bottom = LiveTvDimens.ListGap),
            ) {
                itemsIndexed(visibleChannels, key = { _, channel -> channel.streamId }) { index, channel ->
                    ChannelRow(
                        channel = channel.copy(number = (index + 1).toString().padStart(3, '0')),
                        selected = channel.streamId == state.selectedChannel?.streamId,
                        focusRequester = firstChannelFocusRequester.takeIf { channel.streamId == focusChannelId },
                        leftFocusRequester = selectedCategoryFocusRequester,
                        rightFocusRequester = previewActionFocusRequester,
                        upFocusRequester = headerFocusRequester.takeIf { index == 0 },
                        onFocused = { onChannelFocused(channel) },
                        onClick = { onChannelClick(channel) },
                    )
                }
                if (state.nextPageLoading) {
                    item(key = "live-next-page-loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = SmartVisionColors.Primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewPanel(
    channel: LiveTvChannel?,
    strings: SmartVisionStrings,
    selectedCategoryLabel: String?,
    showHistoryDelete: Boolean,
    showFreeAdsPreview: Boolean,
    idleAdContentUrl: String?,
    premiumPurchaseUrl: String,
    tvCode: String,
    idleVastAdLoader: IdleVastAdLoader,
    monetizationManager: MonetizationManager,
    firstActionFocusRequester: FocusRequester,
    epgDetailsFocusRequester: FocusRequester,
    selectedChannelFocusRequester: FocusRequester,
    headerFocusRequester: FocusRequester,
    onWatch: () -> Unit,
    onFavorite: () -> Unit,
    onDeleteHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiveTvPanel(
        title = strings.liveTvPreview,
        modifier = modifier,
        trailing = {
            if (channel != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (channel.epgPrograms.isNotEmpty()) {
                        EpgHeaderIndicator()
                    }
                    PreviewIconButton(
                        contentDescription = strings.liveTvWatch,
                        onClick = onWatch,
                        icon = Icons.Default.PlayArrow,
                        primary = true,
                        focusRequester = firstActionFocusRequester,
                        downFocusRequester = epgDetailsFocusRequester.takeIf { channel.epgPrograms.isNotEmpty() },
                        leftFocusRequester = selectedChannelFocusRequester,
                        upFocusRequester = headerFocusRequester,
                    )
                    PreviewIconButton(
                        contentDescription = if (channel.isFavorite) strings.liveTvRemoveFavorite else strings.liveTvFavorite,
                        onClick = onFavorite,
                        icon = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        selected = channel.isFavorite,
                        downFocusRequester = epgDetailsFocusRequester.takeIf { channel.epgPrograms.isNotEmpty() },
                        leftFocusRequester = selectedChannelFocusRequester,
                        upFocusRequester = headerFocusRequester,
                    )
                    if (showHistoryDelete) {
                        PreviewIconButton(
                            contentDescription = strings.liveTvDeleteHistory,
                            onClick = onDeleteHistory,
                            icon = Icons.Default.Delete,
                            danger = true,
                            downFocusRequester = epgDetailsFocusRequester.takeIf { channel.epgPrograms.isNotEmpty() },
                            leftFocusRequester = selectedChannelFocusRequester,
                            upFocusRequester = headerFocusRequester,
                        )
                    }
                }
            }
        },
    ) {
        if (channel == null) {
            if (showFreeAdsPreview) {
                Column(modifier = Modifier.fillMaxSize()) {
                    IdlePreviewAdFrame(
                        enabled = !idleAdContentUrl.isNullOrBlank(),
                        idleVastAdLoader = idleVastAdLoader,
                        monetizationManager = monetizationManager,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.88f),
                    )
                    Spacer(Modifier.height(10.dp))
                    PremiumPreviewQr(
                        purchaseUrl = premiumPurchaseUrl,
                        tvCode = tvCode,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                IdlePreviewSmartVisionPrompt(modifier = Modifier.fillMaxSize())
            }
            return@LiveTvPanel
        }

        Column(modifier = Modifier.fillMaxSize()) {
            VideoPreviewFrame(
                channel = channel,
                categoryLabel = selectedCategoryLabel.orEmpty(),
                streamUnavailableLabel = strings.liveTvStreamUnavailable,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.88f),
            )

            Spacer(Modifier.height(6.dp))

            EpgProgramList(
                channel = channel,
                strings = strings,
                categoryLabel = selectedCategoryLabel.orEmpty(),
                focusRequester = epgDetailsFocusRequester,
                leftFocusRequester = selectedChannelFocusRequester,
                upFocusRequester = headerFocusRequester,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun IdlePreviewSmartVisionPrompt(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .focusProperties { canFocus = false }
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.smartvision_logo_wide),
            contentDescription = "SmartVision IPTV Player",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(58.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Selectionnez une chaine pour lancer l'apercu...",
            color = SmartVisionColors.TextSecondary,
            style = LiveTvItemMetaStyle,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PremiumPreviewQr(
    purchaseUrl: String,
    tvCode: String,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(purchaseUrl) { createLivePreviewQrBitmap(purchaseUrl, 384) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(138.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color.White)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR code SmartVision Premium",
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "CODE TV : ${tvCode.ifBlank { "GENERATION" }}",
            color = SmartVisionColors.CyanAccent,
            style = LiveTvItemTitleStyle.copy(fontSize = 18.sp, lineHeight = 21.sp),
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = "Supprimez les pubs",
            color = SmartVisionColors.TextPrimary,
            style = LiveTvItemMetaStyle.copy(fontSize = 12.sp, lineHeight = 15.sp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = "Passez a Premium",
            color = SmartVisionColors.Primary,
            style = LiveTvItemMetaStyle.copy(fontSize = 12.sp, lineHeight = 15.sp),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun IdlePreviewAdFrame(
    enabled: Boolean,
    idleVastAdLoader: IdleVastAdLoader,
    monetizationManager: MonetizationManager,
    modifier: Modifier = Modifier,
) {
    var adTagUrl by remember { mutableStateOf<String?>(null) }
    var configLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(monetizationManager) {
        adTagUrl = monetizationManager.idleLivePreviewAdTagUrl()
        configLoaded = true
    }

    val shape = RoundedCornerShape(LiveTvDimens.ItemRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Black)
            .border(BorderStroke(1.dp, SmartVisionColors.Border), shape)
            .focusProperties { canFocus = false },
        contentAlignment = Alignment.Center,
    ) {
        val tagUrl = adTagUrl
        if (tagUrl != null && enabled) {
            var adCycle by remember(tagUrl) { mutableIntStateOf(0) }
            key(adCycle) {
                IdlePreviewVastPlayer(
                    adTagUrl = tagUrl,
                    idleVastAdLoader = idleVastAdLoader,
                    monetizationManager = monetizationManager,
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
                IdlePreviewPlaceholder()
            }
        }
    }
}

@Composable
private fun IdlePreviewVastPlayer(
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
            LiveBadge(
                text = "PUBLICITE",
                color = SmartVisionColors.PrimaryDark,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            )
        }

        if (adFailed) {
            IdlePreviewPlaceholder()
        }
    }
}

@Composable
private fun IdlePreviewPlaceholder() {
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
            style = LiveTvItemMetaStyle,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

private fun createLivePreviewQrBitmap(content: String, size: Int): Bitmap {
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
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}

@Composable
private fun PreviewIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    selected: Boolean = false,
    danger: Boolean = false,
    focusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focusStyle = LocalTvFocusStyle.current
    val shape = RoundedCornerShape(6.dp)
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> focusStyle.accent
            primary -> SmartVisionColors.Primary
            danger -> SmartVisionColors.Error
            else -> Color.White.copy(alpha = 0.62f)
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "previewIconBorder",
    )
    val backgroundColor by animateColorAsState(
        targetValue = when {
            primary -> SmartVisionColors.Primary
            focusState.isFocused -> SmartVisionColors.SurfaceElevated.copy(alpha = 0.88f)
            else -> Color.Transparent
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "previewIconBackground",
    )
    val tint = when {
        selected -> SmartVisionColors.Error
        danger -> SmartVisionColors.TextPrimary
        else -> SmartVisionColors.TextPrimary
    }

    Box(
        modifier = modifier
            .size(34.dp)
            .then(
                if (downFocusRequester != null || leftFocusRequester != null || upFocusRequester != null) {
                    Modifier.focusProperties {
                        if (downFocusRequester != null) down = downFocusRequester
                        if (leftFocusRequester != null) left = leftFocusRequester
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
                focusedScale = 1.08f,
                glowColor = if (danger) SmartVisionColors.Error else SmartVisionColors.Primary,
                cornerRadius = 6.dp,
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
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun CategoryRow(
    category: LiveTvCategory,
    selected: Boolean,
    focusRequester: FocusRequester?,
    upFocusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val active = selected || focusState.isFocused
    val shape = RoundedCornerShape(LiveTvDimens.ItemRadius)
    val focusStyle = LocalTvFocusStyle.current
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> focusStyle.accent
            selected -> SmartVisionColors.Primary
            else -> SmartVisionColors.Border
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "categoryBorder",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(LiveTvDimens.CategoryRowHeight)
            .then(
                if (upFocusRequester != null) {
                    Modifier.focusProperties { up = upFocusRequester }
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
                cornerRadius = LiveTvDimens.ItemRadius,
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
        val icon = category.specialIcon()
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
            text = category.label,
            color = SmartVisionColors.TextPrimary,
            style = LiveTvItemTitleStyle,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        val countText = category.count?.toString().orEmpty()
        if (category.hasEpg && countText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0876FF).copy(alpha = if (active) 0.95f else 0.72f))
                    .border(BorderStroke(1.dp, Color(0xFF1FDDFF).copy(alpha = 0.76f)), RoundedCornerShape(4.dp))
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = countText,
                    color = Color.White,
                    style = LiveTvItemMetaStyle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        } else {
            Text(
                text = countText,
                color = if (active) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
                style = LiveTvItemMetaStyle,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ChannelRow(
    channel: LiveTvChannel,
    selected: Boolean,
    focusRequester: FocusRequester?,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    upFocusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val fallbackRowFocusRequester = remember { FocusRequester() }
    val rowFocusRequester = focusRequester ?: fallbackRowFocusRequester
    val pressed by interactionSource.collectIsPressedAsState()
    val active = selected || focusState.isFocused
    val shape = RoundedCornerShape(LiveTvDimens.ItemRadius)
    val focusStyle = LocalTvFocusStyle.current
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> focusStyle.accent
            selected -> SmartVisionColors.Primary
            else -> SmartVisionColors.Border
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "channelBorder",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(LiveTvDimens.ChannelRowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (rightFocusRequester != null) {
                        Modifier.focusProperties {
                            right = rightFocusRequester
                            if (upFocusRequester != null) {
                                up = upFocusRequester
                            }
                        }
                    } else if (upFocusRequester != null) {
                        Modifier.focusProperties {
                            up = upFocusRequester
                        }
                    } else {
                        Modifier
                    },
                )
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft && leftFocusRequester != null) {
                        runCatching { leftFocusRequester.requestFocus() }
                        true
                    } else {
                        false
                    }
                }
                .tvFocusTarget(
                    state = focusState,
                    focusRequester = rowFocusRequester,
                    pressed = pressed,
                    focusedScale = 1.035f,
                    glowColor = SmartVisionColors.Primary,
                    cornerRadius = LiveTvDimens.ItemRadius,
                )
                .onFocusChanged { focus ->
                    if (focus.isFocused) {
                        onFocused()
                    }
                }
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
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .focusable(interactionSource = interactionSource)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = channel.number,
                color = if (active) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
                style = LiveTvItemMetaStyle,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.width(30.dp),
            )

            ChannelLogo(
                channel = channel,
                active = active,
                modifier = Modifier.size(width = 68.dp, height = 40.dp),
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    color = SmartVisionColors.TextPrimary,
                    style = LiveTvChannelTitleStyle,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (channel.program != "Direct") {
                    Text(
                        text = channel.program,
                        color = SmartVisionColors.TextSecondary,
                        style = LiveTvItemMetaStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (channel.epgPrograms.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                EpgAvailabilityBadge()
            }
        }
    }
}

@Composable
private fun EpgAvailabilityBadge() {
    Image(
        painter = painterResource(R.drawable.ic_epg_premium),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(width = 20.dp, height = 17.dp),
    )
}

@Composable
private fun EpgHeaderIndicator() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .focusProperties { canFocus = false }
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xA80A1B35))
            .border(BorderStroke(1.dp, SmartVisionColors.Primary.copy(alpha = 0.72f)), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_epg_premium),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width = 28.dp, height = 24.dp),
        )
    }
}

@Composable
private fun ConfirmHistoryDeleteDialog(
    title: String,
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0A1425))
                .border(BorderStroke(1.dp, SmartVisionColors.Error.copy(alpha = 0.78f)), RoundedCornerShape(8.dp))
                .padding(22.dp),
        ) {
            Text(title, color = SmartVisionColors.TextPrimary, style = SmartVisionType.TitleS, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                text = itemName,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TvButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier.height(42.dp),
                )
                Spacer(Modifier.width(10.dp))
                TvButton(
                    text = "Supprimer",
                    onClick = onConfirm,
                    leadingIcon = Icons.Default.Delete,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier.height(42.dp),
                )
            }
        }
    }
}

@Composable
private fun VideoPreviewFrame(
    channel: LiveTvChannel,
    categoryLabel: String,
    streamUnavailableLabel: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Black)
            .border(BorderStroke(1.dp, SmartVisionColors.Border), shape),
    ) {
        MiniPreviewPlayer(
            streamUrl = channel.streamUrl,
            fallbackStreamUrl = channel.fallbackStreamUrl,
            streamUnavailableLabel = streamUnavailableLabel,
            modifier = Modifier.matchParentSize(),
        )

        MiniPlayerInfoOverlay(
            channel = channel,
            categoryLabel = categoryLabel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun MiniPlayerInfoOverlay(
    channel: LiveTvChannel,
    categoryLabel: String,
    modifier: Modifier = Modifier,
) {
    val currentProgram = channel.epgPrograms.firstOrNull()
    Row(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x2D102A52),
                        Color(0xD0081427),
                    ),
                ),
            )
            .border(BorderStroke(1.dp, SmartVisionColors.Primary.copy(alpha = 0.26f)), RoundedCornerShape(0.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChannelLogo(
            channel = channel,
            active = true,
            modifier = Modifier.size(width = 42.dp, height = 24.dp),
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 9.dp)
                .width(1.dp)
                .height(28.dp)
                .background(Color.White.copy(alpha = 0.34f)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                color = SmartVisionColors.TextPrimary,
                style = LiveTvItemTitleStyle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = currentProgram?.title ?: categoryLabel.ifBlank { channel.genre },
                color = SmartVisionColors.TextSecondary,
                style = LiveTvItemMetaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        currentProgram?.timeRange?.takeIf { it.isNotBlank() }?.let { timeRange ->
            Spacer(Modifier.width(8.dp))
            Text(
                text = timeRange,
                color = SmartVisionColors.TextPrimary,
                style = LiveTvItemMetaStyle,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EpgProgramList(
    channel: LiveTvChannel,
    strings: SmartVisionStrings,
    categoryLabel: String,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val programs = channel.epgPrograms
    var expandedIndex by remember(channel.streamId, programs.size) { mutableIntStateOf(-1) }
    if (programs.isEmpty()) {
        ChannelAboutPanel(
            channel = channel,
            strings = strings,
            categoryLabel = categoryLabel.ifBlank { channel.genre },
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 6.dp),
    ) {
        itemsIndexed(programs, key = { index, program -> "${channel.streamId}-$index-${program.timeRange}" }) { index, program ->
            EpgProgramRow(
                program = program,
                expanded = expandedIndex == index,
                focusRequester = focusRequester.takeIf { index == 0 },
                leftFocusRequester = leftFocusRequester,
                upFocusRequester = upFocusRequester.takeIf { index == 0 },
                onClick = {
                    expandedIndex = if (expandedIndex == index) -1 else index
                },
            )
        }
    }
}

@Composable
private fun ChannelAboutPanel(
    channel: LiveTvChannel,
    strings: SmartVisionStrings,
    categoryLabel: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(7.dp)
    val country = remember(channel.name, categoryLabel) {
        detectLiveChannelCountry(channel.name, categoryLabel)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xB40B2344),
                        Color(0xD0051124),
                    ),
                ),
            )
            .border(BorderStroke(1.dp, SmartVisionColors.Primary.copy(alpha = 0.58f)), shape)
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Text(
            text = strings.liveTvAboutChannelTitle,
            color = SmartVisionColors.TextPrimary,
            style = LiveTvPanelTitleStyle,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp, bottom = 8.dp)
                .height(1.dp)
                .background(SmartVisionColors.Primary.copy(alpha = 0.45f)),
        )
        AboutInfoRow(
            icon = AboutInfoIconKind.Channel,
            label = strings.liveTvAboutChannelName,
            valueContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChannelLogo(
                        channel = channel,
                        active = true,
                        modifier = Modifier.size(width = 40.dp, height = 23.dp),
                    )
                    Text(
                        text = channel.name,
                        color = SmartVisionColors.TextPrimary,
                        style = LiveTvItemTitleStyle,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
        AboutInfoRow(
            icon = AboutInfoIconKind.Number,
            label = strings.liveTvAboutNumber,
            value = channel.number,
        )
        AboutInfoRow(
            icon = AboutInfoIconKind.Category,
            label = strings.liveTvAboutCategory,
            value = categoryLabel.ifBlank { channel.genre },
        )
        AboutInfoRow(
            icon = AboutInfoIconKind.Source,
            label = strings.liveTvAboutSource,
            value = strings.liveTv,
        )
        AboutInfoRow(
            icon = AboutInfoIconKind.Epg,
            label = strings.liveTvAboutEpg,
            value = strings.liveTvAboutEpgUnavailable,
            disabled = true,
        )
        AboutInfoRow(
            icon = AboutInfoIconKind.Country,
            label = strings.liveTvAboutCountry,
            value = country ?: strings.liveTvAboutUnknownCountry,
            disabled = country == null,
        )
    }
}

@Composable
private fun AboutInfoRow(
    icon: AboutInfoIconKind,
    label: String,
    value: String? = null,
    disabled: Boolean = false,
    valueContent: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp)
            .clip(shape)
            .background(Color(0x61102542))
            .border(BorderStroke(1.dp, SmartVisionColors.Primary.copy(alpha = 0.28f)), shape)
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(38.dp)
                .background(Color(0x65113B76)),
            contentAlignment = Alignment.Center,
        ) {
            AboutInfoIcon(icon)
        }
        Text(
            text = label,
            color = SmartVisionColors.TextPrimary,
            style = LiveTvItemTitleStyle,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        )
        Box(
            modifier = Modifier
                .weight(1.25f)
                .padding(end = 10.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (valueContent != null) {
                valueContent()
            } else {
                Text(
                    text = value.orEmpty(),
                    color = if (disabled) SmartVisionColors.TextSecondary.copy(alpha = 0.72f) else SmartVisionColors.TextPrimary,
                    style = LiveTvItemTitleStyle,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun AboutInfoIcon(kind: AboutInfoIconKind) {
    Canvas(
        modifier = Modifier
            .size(20.dp)
            .focusProperties { canFocus = false },
    ) {
        val stroke = 2.3.dp.toPx()
        val white = Color.White
        when (kind) {
            AboutInfoIconKind.Channel -> {
                drawRoundRect(
                    color = white,
                    topLeft = Offset(size.width * 0.08f, size.height * 0.26f),
                    size = Size(size.width * 0.74f, size.height * 0.54f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                )
                drawLine(white, Offset(size.width * 0.32f, size.height * 0.26f), Offset(size.width * 0.18f, size.height * 0.08f), stroke)
                drawLine(white, Offset(size.width * 0.58f, size.height * 0.26f), Offset(size.width * 0.72f, size.height * 0.08f), stroke)
                drawPath(
                    Path().apply {
                        moveTo(size.width * 0.42f, size.height * 0.42f)
                        lineTo(size.width * 0.42f, size.height * 0.64f)
                        lineTo(size.width * 0.61f, size.height * 0.53f)
                        close()
                    },
                    white,
                )
            }
            AboutInfoIconKind.Number -> {
                listOf(0.22f to "1", 0.50f to "2", 0.78f to "3").forEach { (y, text) ->
                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        size.width * 0.12f,
                        size.height * y,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 8.5.dp.toPx()
                            isFakeBoldText = true
                            isAntiAlias = true
                        },
                    )
                }
                repeat(3) { row ->
                    val y = size.height * (0.18f + row * 0.28f)
                    drawLine(white, Offset(size.width * 0.45f, y), Offset(size.width * 0.90f, y), stroke)
                }
            }
            AboutInfoIconKind.Category -> {
                drawRoundRect(
                    color = white,
                    topLeft = Offset(size.width * 0.10f, size.height * 0.30f),
                    size = Size(size.width * 0.80f, size.height * 0.48f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                )
                drawRoundRect(
                    color = white,
                    topLeft = Offset(size.width * 0.12f, size.height * 0.20f),
                    size = Size(size.width * 0.36f, size.height * 0.17f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                )
            }
            AboutInfoIconKind.Source -> {
                drawCircle(white, radius = size.minDimension * 0.09f, center = Offset(size.width * 0.50f, size.height * 0.50f))
                drawArc(white, 130f, 100f, false, topLeft = Offset(size.width * 0.18f, size.height * 0.22f), size = Size(size.width * 0.64f, size.height * 0.56f), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                drawArc(white, -50f, 100f, false, topLeft = Offset(size.width * 0.18f, size.height * 0.22f), size = Size(size.width * 0.64f, size.height * 0.56f), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            }
            AboutInfoIconKind.Epg -> {
                drawRoundRect(
                    color = white,
                    topLeft = Offset(size.width * 0.16f, size.height * 0.20f),
                    size = Size(size.width * 0.68f, size.height * 0.62f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                )
                repeat(2) { row ->
                    repeat(3) { col ->
                        drawCircle(white, radius = 1.5.dp.toPx(), center = Offset(size.width * (0.34f + col * 0.16f), size.height * (0.48f + row * 0.17f)))
                    }
                }
            }
            AboutInfoIconKind.Country -> {
                drawLine(white, Offset(size.width * 0.22f, size.height * 0.16f), Offset(size.width * 0.22f, size.height * 0.86f), stroke)
                drawPath(
                    Path().apply {
                        moveTo(size.width * 0.25f, size.height * 0.20f)
                        cubicTo(size.width * 0.45f, size.height * 0.10f, size.width * 0.58f, size.height * 0.32f, size.width * 0.78f, size.height * 0.22f)
                        lineTo(size.width * 0.78f, size.height * 0.58f)
                        cubicTo(size.width * 0.58f, size.height * 0.68f, size.width * 0.45f, size.height * 0.46f, size.width * 0.25f, size.height * 0.56f)
                        close()
                    },
                    white,
                )
            }
        }
    }
}

private enum class AboutInfoIconKind {
    Channel,
    Number,
    Category,
    Source,
    Epg,
    Country,
}

private fun detectLiveChannelCountry(channelName: String, categoryLabel: String): String? {
    val text = "$channelName $categoryLabel".lowercase()
    val tokens = text.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.toSet()
    return when {
        "france" in text || "french" in text || "fr" in tokens -> "France"
        "espagne" in text || "spain" in text || "es" in tokens -> "Espagne"
        "italia" in text || "italy" in text || "italie" in text || "it" in tokens -> "Italie"
        "united kingdom" in text || "royaume uni" in text || "uk" in tokens || "gb" in tokens -> "Royaume-Uni"
        else -> null
    }
}

@Composable
private fun EpgProgramRow(
    program: LiveTvProgram,
    expanded: Boolean,
    focusRequester: FocusRequester?,
    leftFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focusStyle = LocalTvFocusStyle.current
    val borderColor by animateColorAsState(
        targetValue = if (focusState.isFocused) focusStyle.accent else Color.Transparent,
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "epgRowBorder",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusProperties {
                left = leftFocusRequester
                if (upFocusRequester != null) {
                    up = upFocusRequester
                }
            }
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.015f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = 5.dp,
            )
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .clip(RoundedCornerShape(5.dp))
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else 0.dp,
                    borderColor,
                ),
                RoundedCornerShape(5.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 7.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = program.title,
                color = SmartVisionColors.TextPrimary,
                style = LiveTvEpgTitleStyle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = program.timeRange,
                color = SmartVisionColors.TextSecondary,
                style = LiveTvItemMetaStyle,
                maxLines = 1,
            )
        }
        AnimatedVisibility(
            visible = expanded && program.description.isNotBlank(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Text(
                text = program.description,
                color = SmartVisionColors.TextSecondary,
                style = LiveTvItemMetaStyle,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(1.dp)
                .background(SmartVisionColors.Border.copy(alpha = 0.46f)),
        )
    }
}

@Composable
private fun MiniPreviewPlayer(
    streamUrl: String,
    fallbackStreamUrl: String,
    streamUnavailableLabel: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val latestStreamUrl by rememberUpdatedState(streamUrl)
    val latestFallbackStreamUrl by rememberUpdatedState(fallbackStreamUrl)
    val audioScope = rememberCoroutineScope()
    var buffering by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var fallbackTried by remember(streamUrl) { mutableStateOf(false) }
    val volumeFadeJob = remember { arrayOfNulls<Job>(1) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            playWhenReady = true
        }
    }

    fun restartPreviewAudioFade() {
        volumeFadeJob[0]?.cancel()
        player.volume = 0f
        volumeFadeJob[0] = audioScope.launch { player.fadeInMiniPlayerVolume() }
    }

    LaunchedEffect(streamUrl) {
        fallbackTried = false
        errorText = null
        buffering = true
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
        player.playWhenReady = true
        player.volume = 0f
        restartPreviewAudioFade()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_READY) {
                    errorText = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val fallback = latestFallbackStreamUrl
                if (!fallbackTried && fallback.isNotBlank() && fallback != latestStreamUrl) {
                    fallbackTried = true
                    errorText = null
                    buffering = true
                    player.stop()
                    player.clearMediaItems()
                    player.setMediaItem(MediaItem.fromUri(fallback))
                    player.prepare()
                    player.playWhenReady = true
                    restartPreviewAudioFade()
                    return
                }

                buffering = false
                volumeFadeJob[0]?.cancel()
                player.volume = 0f
                errorText = streamUnavailableLabel
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

        if (buffering) {
            CircularProgressIndicator(
                color = SmartVisionColors.Primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(30.dp),
            )
        }

        errorText?.let { message ->
            Text(
                text = message,
                color = SmartVisionColors.TextPrimary,
                style = LiveTvItemMetaStyle,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            )
        }
    }
}

private suspend fun ExoPlayer.fadeInMiniPlayerVolume() {
    delay(MiniPlayerAudioStartDelayMillis)
    repeat(MiniPlayerAudioFadeSteps) { step ->
        delay(MiniPlayerAudioFadeMillis / MiniPlayerAudioFadeSteps)
        volume = (step + 1).toFloat() / MiniPlayerAudioFadeSteps
    }
    volume = 1f
}

private const val MiniPlayerAudioStartDelayMillis = 1_000L
private const val MiniPlayerAudioFadeMillis = 1_000L
private const val MiniPlayerAudioFadeSteps = 10

private fun LiveTvCategory.specialIcon(): ImageVector? {
    val normalized = label.lowercase()
    return when {
        normalized == "all" -> Icons.Default.Menu
        "favoris" in normalized -> Icons.Default.Favorite
        "histor" in normalized -> Icons.Default.History
        else -> null
    }
}

@Composable
private fun ChannelLogo(
    channel: LiveTvChannel,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!channel.logoUrl.isNullOrBlank()) {
        AsyncImage(
            model = channel.logoUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
        return
    }

    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = if (active) 0.10f else 0.06f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), shape),
        contentAlignment = Alignment.Center,
    ) {
        when (channel.logoText) {
            "TF1" -> Tf1Logo()
            else -> {
                if (channel.logoText == "2" || channel.logoText == "3") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 5.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (channel.logoText == "2") SmartVisionColors.Error else SmartVisionColors.Primary),
                    )
                }

                Text(
                    text = channel.logoText,
                    color = when (channel.logoText) {
                        "arte" -> Color(0xFFFF6B2D)
                        "W9" -> Color(0xFFA855F7)
                        else -> SmartVisionColors.TextPrimary
                    },
                    style = when {
                        channel.logoText.length <= 2 -> TextStyle(
                            fontSize = 20.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.sp,
                        )
                        else -> LiveTvItemTitleStyle
                    },
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Tf1Logo() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tf1LogoBlock(text = "T", color = Color(0xFF254BCA), modifier = Modifier.weight(1f))
        Tf1LogoBlock(text = "F", color = Color.White, textColor = Color(0xFF254BCA), modifier = Modifier.weight(1f))
        Tf1LogoBlock(text = "1", color = Color(0xFFE3263E), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Tf1LogoBlock(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
) {
    Box(
        modifier = modifier
            .height(14.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            style = LiveTvItemMetaStyle,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun LiveBadge(
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
            style = LiveTvItemMetaStyle,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ProgressLine(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(3.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.14f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(SmartVisionColors.Primary),
        )
    }
}

@Composable
private fun LiveTvLoadingSkeleton(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "liveTvSkeleton")
    val shimmerOffset by transition.animateFloat(
        initialValue = -360f,
        targetValue = 920f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "liveTvSkeletonOffset",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            SmartVisionColors.Surface.copy(alpha = 0.34f),
            SmartVisionColors.SurfaceElevated.copy(alpha = 0.74f),
            SmartVisionColors.Surface.copy(alpha = 0.34f),
        ),
        start = Offset(shimmerOffset, 0f),
        end = Offset(shimmerOffset + 280f, 0f),
    )

    Row(
        modifier = modifier.focusProperties { canFocus = false },
        horizontalArrangement = Arrangement.spacedBy(LiveTvDimens.PanelGap),
    ) {
        LiveTvSkeletonPanel(
            titleWidth = 92.dp,
            rows = 11,
            rowHeight = LiveTvDimens.CategoryRowHeight,
            shimmerBrush = shimmerBrush,
            modifier = Modifier
                .weight(0.24f)
                .fillMaxHeight(),
        )
        LiveTvSkeletonPanel(
            titleWidth = 82.dp,
            rows = 10,
            rowHeight = LiveTvDimens.ChannelRowHeight,
            shimmerBrush = shimmerBrush,
            headerTrailing = true,
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight(),
        )
        LiveTvSkeletonPreviewPanel(
            shimmerBrush = shimmerBrush,
            modifier = Modifier
                .weight(0.34f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun LiveTvSkeletonPanel(
    titleWidth: androidx.compose.ui.unit.Dp,
    rows: Int,
    rowHeight: androidx.compose.ui.unit.Dp,
    shimmerBrush: Brush,
    modifier: Modifier = Modifier,
    headerTrailing: Boolean = false,
) {
    LiveTvPanel(
        title = "",
        modifier = modifier,
        trailing = if (headerTrailing) {
            {
                Box(
                    modifier = Modifier
                        .width(180.dp)
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
        Column(verticalArrangement = Arrangement.spacedBy(LiveTvDimens.ListGap)) {
            repeat(rows) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .clip(RoundedCornerShape(LiveTvDimens.ItemRadius))
                        .background(shimmerBrush),
                )
            }
        }
    }
}

@Composable
private fun LiveTvSkeletonPreviewPanel(
    shimmerBrush: Brush,
    modifier: Modifier = Modifier,
) {
    LiveTvPanel(
        title = "",
        modifier = modifier,
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(2) {
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
                .width(80.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush),
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.88f)
                .clip(RoundedCornerShape(5.dp))
                .background(shimmerBrush),
        )
        Spacer(Modifier.height(8.dp))
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(shimmerBrush),
            )
            Spacer(Modifier.height(5.dp))
        }
    }
}

@Composable
private fun LoadingState(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .border(BorderStroke(3.dp, SmartVisionColors.Primary), CircleShape),
            )
            Spacer(Modifier.height(14.dp))
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
private fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Tv,
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
private fun ErrorState(
    message: String,
    retryLabel: String,
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
                text = retryLabel,
                onClick = onRetry,
                variant = TvButtonVariant.Secondary,
                modifier = Modifier.height(42.dp),
            )
        }
    }
}

@Composable
private fun LiveTvPanel(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(LiveTvDimens.PanelRadius),
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
            .padding(LiveTvDimens.PanelPadding),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = SmartVisionColors.TextPrimary,
                style = LiveTvPanelTitleStyle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(start = LiveTvDimens.PanelHeaderTitleStartPadding),
            )
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
        }

        Spacer(Modifier.height(6.dp))

        content()
    }
}

private const val LiveTvNextPageThreshold = 12

private object LiveTvDimens {
    val ScreenPadding = 14.dp
    val TopPadding = 4.dp
    val BottomPadding = 16.dp
    val HeaderHeight = 44.dp
    val HeaderGap = 16.dp
    val PanelGap = 8.dp
    val PanelPadding = 8.dp
    val PanelRadius = 8.dp
    val ItemRadius = 7.dp
    val ListGap = 5.dp
    val CategoryRowHeight = 36.dp
    val ChannelRowHeight = 42.dp
    val ChannelListTopPadding = 12.dp
    val PanelHeaderTitleStartPadding = 8.dp
}

private fun LiveTvChannel.toBehaviorContent(categoryLabel: String?): BehaviorContent =
    BehaviorContent(
        contentType = "LIVE_TV",
        contentId = streamId.toString(),
        title = name,
        categoryLabel = categoryLabel,
        sourceScreen = "LIVE",
        engagementScore = 45,
        tags = listOfNotNull("live", if (isFavorite) "favorite" else null),
        context = mapOf("number" to number.toString()),
    )
