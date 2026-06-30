package com.smartvision.svplayer.ui.live

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
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

private val LiveTvItemMetaStyle = TextStyle(
    fontSize = 10.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
)

private val LiveTvPreviewTitleStyle = TextStyle(
    fontSize = 18.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.sp,
)

private val LiveTvButtonTextStyle = TextStyle(
    fontSize = 11.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.sp,
)

@Composable
fun LiveTvScreen(
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
    onWatch: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: LiveTvViewModel = viewModel(
        factory = viewModelFactory {
            LiveTvViewModel(
                xtreamRepository = container.xtreamRepository,
                userContentRepository = container.userContentRepository,
                settingsRepository = container.settingsRepository,
            )
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by container.accountManager.accounts.collectAsStateWithLifecycle()
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    val firstChannelFocusRequester = remember { FocusRequester() }
    val firstPreviewActionFocusRequester = remember { FocusRequester() }
    val behaviorScope = rememberCoroutineScope()
    var inputReady by remember { mutableStateOf(false) }
    var categorySearchQuery by remember { mutableStateOf("") }
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

    val selectedCategoryVisible = state.categories.any { category ->
        category.id == state.selectedCategoryId &&
            (categorySearchQuery.isBlank() || category.label.contains(categorySearchQuery, ignoreCase = true))
    }
    val categoryFocusTargetAvailable = selectedCategoryVisible || state.categories.any { category ->
        categorySearchQuery.isBlank() || category.label.contains(categorySearchQuery, ignoreCase = true)
    }

    LaunchedEffect(state.categoriesLoading, accounts.isNotEmpty(), categoryFocusTargetAvailable) {
        if (accounts.isNotEmpty() && !state.categoriesLoading && categoryFocusTargetAvailable) {
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
        )

        Spacer(Modifier.height(LiveTvDimens.HeaderGap))

        if (accounts.isEmpty()) {
            XtreamQrSetupPanel(
                activationRepository = container.activationRepository,
                title = "Configurer vos chaînes Live TV",
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

        if (state.categoriesLoading) {
            LoadingState(
                title = "Chargement des categories",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(LiveTvDimens.PanelGap),
            ) {
                CategoryList(
                    state = state,
                    selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                    firstChannelFocusRequester = firstChannelFocusRequester,
                    searchQuery = categorySearchQuery,
                    onSearchQueryChange = { categorySearchQuery = it },
                    contentSearchQuery = contentSearchQuery,
                    onCategory = { category ->
                        if (inputReady) {
                            viewModel.selectCategory(category)
                        }
                    },
                    modifier = Modifier
                        .weight(0.24f)
                        .fillMaxHeight(),
                )
                ChannelList(
                    state = state,
                    firstChannelFocusRequester = firstChannelFocusRequester,
                    selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                    searchQuery = contentSearchQuery,
                    onSearchQueryChange = { contentSearchQuery = it },
                    previewActionFocusRequester = firstPreviewActionFocusRequester.takeIf {
                        state.selectedChannel != null
                    },
                    onChannelFocused = viewModel::focusChannel,
                    onChannelClick = { channel ->
                        if (inputReady) {
                            val openFullPlayer = viewModel.activateChannel(channel)
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
                    showHistoryDelete = state.selectedCategory?.label == "Historique",
                    onDeleteHistoryChannel = { channel -> channelToDelete = channel },
                    onRetry = viewModel::retryCurrentCategory,
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                )
                PreviewPanel(
                    channel = state.selectedChannel,
                    showFreeAdsPreview = showFreeAdsPreview,
                    idleAdContentUrl = state.channels.firstOrNull()?.streamUrl,
                    premiumPurchaseUrl = premiumPurchaseUrl,
                    tvCode = tvCode,
                    idleVastAdLoader = container.idleVastAdLoader,
                    monetizationManager = container.monetizationManager,
                    firstActionFocusRequester = firstPreviewActionFocusRequester,
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
                    modifier = Modifier
                        .weight(0.34f)
                        .fillMaxHeight(),
                )
            }
        }
    }

    channelToDelete?.let { channel ->
        ConfirmHistoryDeleteDialog(
            title = "Supprimer cette chaine de l'historique ?",
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
    selectedCategoryFocusRequester: FocusRequester,
    firstChannelFocusRequester: FocusRequester,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    contentSearchQuery: String,
    onCategory: (LiveTvCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleCategories = state.categories.filter {
        searchQuery.isBlank() || it.label.contains(searchQuery, ignoreCase = true)
    }
    val focusCategoryId = visibleCategories.firstOrNull { it.id == state.selectedCategoryId }?.id
        ?: visibleCategories.firstOrNull()?.id

    LiveTvPanel(
        title = "Categories",
        modifier = modifier,
        trailing = {
            CatalogSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Dossier",
                modifier = Modifier.width(118.dp),
            )
        },
    ) {
        LazyColumn(
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
                    rightFocusRequester = firstChannelFocusRequester.takeIf {
                        !state.channelsLoading && state.channels.any {
                            contentSearchQuery.isBlank() || it.name.contains(contentSearchQuery, ignoreCase = true)
                        }
                    },
                    onClick = { onCategory(category) },
                )
            }
        }
    }
}

@Composable
private fun ChannelList(
    state: LiveTvUiState,
    firstChannelFocusRequester: FocusRequester,
    selectedCategoryFocusRequester: FocusRequester,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    previewActionFocusRequester: FocusRequester?,
    onChannelFocused: (LiveTvChannel) -> Unit,
    onChannelClick: (LiveTvChannel) -> Unit,
    showHistoryDelete: Boolean,
    onDeleteHistoryChannel: (LiveTvChannel) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleChannels = state.channels.filter { channel ->
        searchQuery.isBlank() || channel.name.contains(searchQuery, ignoreCase = true)
    }
    val focusChannelId = state.selectedChannelId
        ?.takeIf { selectedId -> visibleChannels.any { it.streamId == selectedId } }
        ?: state.focusedChannelId?.takeIf { focusedId -> visibleChannels.any { it.streamId == focusedId } }
        ?: visibleChannels.firstOrNull()?.streamId
    LaunchedEffect(focusChannelId, state.channelsLoading, visibleChannels.size) {
        if (!state.channelsLoading && focusChannelId != null) {
            withFrameNanos { }
            delay(80)
            runCatching { firstChannelFocusRequester.requestFocus() }
        }
    }
    LiveTvPanel(
        title = "Chaines",
        modifier = modifier,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val channelCount = if (searchQuery.isBlank()) {
                    state.selectedCategory?.count ?: state.channels.size
                } else {
                    visibleChannels.size
                }
                Text(
                    text = if (channelCount > 0) "$channelCount chaines" else "chaines",
                    color = SmartVisionColors.TextSecondary,
                    style = LiveTvItemMetaStyle,
                    maxLines = 1,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = SmartVisionColors.TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(10.dp))
                CatalogSearchField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = "Chaine",
                    modifier = Modifier.width(190.dp),
                )
            }
        },
    ) {
        when {
            state.channelsLoading && visibleChannels.isEmpty() -> LoadingState(
                title = "Chargement des chaines",
                modifier = Modifier.fillMaxSize(),
            )

            state.errorMessage != null && visibleChannels.isEmpty() -> ErrorState(
                message = state.errorMessage,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )

            visibleChannels.isEmpty() -> EmptyState(
                title = if (searchQuery.isBlank()) "Aucune chaine" else "Aucun resultat",
                subtitle = if (searchQuery.isBlank()) "Selectionnez une autre categorie." else "Modifiez votre recherche.",
                modifier = Modifier.fillMaxSize(),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(LiveTvDimens.ListGap),
                contentPadding = PaddingValues(bottom = LiveTvDimens.ListGap),
            ) {
                itemsIndexed(visibleChannels, key = { _, channel -> channel.streamId }) { index, channel ->
                    ChannelRow(
                        channel = channel,
                        selected = channel.streamId == state.selectedChannel?.streamId,
                        focusRequester = firstChannelFocusRequester.takeIf { channel.streamId == focusChannelId },
                        leftFocusRequester = selectedCategoryFocusRequester,
                        rightFocusRequester = previewActionFocusRequester,
                        onFocused = { onChannelFocused(channel) },
                        onClick = { onChannelClick(channel) },
                        showDelete = showHistoryDelete,
                        onDelete = { onDeleteHistoryChannel(channel) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewPanel(
    channel: LiveTvChannel?,
    showFreeAdsPreview: Boolean,
    idleAdContentUrl: String?,
    premiumPurchaseUrl: String,
    tvCode: String,
    idleVastAdLoader: IdleVastAdLoader,
    monetizationManager: MonetizationManager,
    firstActionFocusRequester: FocusRequester,
    onWatch: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiveTvPanel(
        title = "Apercu",
        modifier = modifier,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.88f),
            )

            Spacer(Modifier.height(8.dp))

            ProgramInfoCard(
                channel = channel,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PreviewActionButton(
                    text = "Regarder",
                    onClick = onWatch,
                    icon = Icons.Default.PlayArrow,
                    primary = true,
                    focusRequester = firstActionFocusRequester,
                    modifier = Modifier
                        .weight(1.25f)
                        .height(32.dp),
                )
                PreviewActionButton(
                    text = "Favori",
                    onClick = onFavorite,
                    selected = channel.isFavorite,
                    icon = Icons.Default.Favorite,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                )
            }
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
private fun PreviewActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    selected: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(LiveTvDimens.ItemRadius)
    val active = focusState.isFocused || selected
    val focusStyle = LocalTvFocusStyle.current
    val backgroundColor by animateColorAsState(
        targetValue = when {
            primary -> SmartVisionColors.Primary
            active -> SmartVisionColors.SurfaceElevated
            else -> SmartVisionColors.Surface.copy(alpha = 0.70f)
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "previewActionBackground",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> focusStyle.accent
            selected -> SmartVisionColors.Primary
            else -> SmartVisionColors.Border
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "previewActionBorder",
    )
    val contentColor = if (primary || active) {
        SmartVisionColors.TextPrimary
    } else {
        SmartVisionColors.TextSecondary
    }

    Row(
        modifier = modifier
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
            style = LiveTvButtonTextStyle,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CategoryRow(
    category: LiveTvCategory,
    selected: Boolean,
    focusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
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
        Text(
            text = category.count?.toString().orEmpty(),
            color = if (active) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
            style = LiveTvItemMetaStyle,
            maxLines = 1,
        )
    }
}

@Composable
private fun ChannelRow(
    channel: LiveTvChannel,
    selected: Boolean,
    focusRequester: FocusRequester?,
    leftFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    showDelete: Boolean,
    onDelete: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val fallbackRowFocusRequester = remember { FocusRequester() }
    val rowFocusRequester = focusRequester ?: fallbackRowFocusRequester
    val deleteFocusRequester = remember { FocusRequester() }
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
                .focusProperties {
                    left = leftFocusRequester
                    if (showDelete) {
                        right = deleteFocusRequester
                    } else if (rightFocusRequester != null) {
                        right = rightFocusRequester
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
                    style = LiveTvItemTitleStyle,
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
        }

        if (showDelete) {
            Spacer(Modifier.width(8.dp))
            HistoryDeleteButton(
                focusRequester = deleteFocusRequester,
                leftFocusRequester = rowFocusRequester,
                rightFocusRequester = rightFocusRequester,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun HistoryDeleteButton(
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focusStyle = LocalTvFocusStyle.current
    Box(
        modifier = Modifier
            .size(34.dp)
            .focusProperties {
                left = leftFocusRequester
                if (rightFocusRequester != null) right = rightFocusRequester
            }
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.08f,
                glowColor = SmartVisionColors.Error,
                cornerRadius = 6.dp,
            )
            .clip(RoundedCornerShape(6.dp))
            .background(if (focusState.isFocused) SmartVisionColors.Error.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.08f))
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else 1.dp,
                    if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Error.copy(alpha = 0.58f),
                ),
                RoundedCornerShape(6.dp),
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Supprimer de l'historique",
            tint = SmartVisionColors.TextPrimary,
            modifier = Modifier.size(18.dp),
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
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(LiveTvDimens.ItemRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Black)
            .border(BorderStroke(1.dp, SmartVisionColors.Border), shape),
    ) {
        MiniPreviewPlayer(
            streamUrl = channel.streamUrl,
            fallbackStreamUrl = channel.fallbackStreamUrl,
            modifier = Modifier.matchParentSize(),
        )

        LiveBadge(
            text = "LIVE",
            color = SmartVisionColors.Error,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        )

        ChannelLogo(
            channel = channel,
            active = true,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(width = 44.dp, height = 24.dp),
        )
    }
}

@Composable
private fun MiniPreviewPlayer(
    streamUrl: String,
    fallbackStreamUrl: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val latestStreamUrl by rememberUpdatedState(streamUrl)
    val latestFallbackStreamUrl by rememberUpdatedState(fallbackStreamUrl)
    var buffering by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var fallbackTried by remember(streamUrl) { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            playWhenReady = true
        }
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
                    return
                }

                buffering = false
                errorText = "Flux indisponible"
            }
        }
        player.addListener(listener)
        onDispose {
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

@Composable
private fun ProgramInfoCard(
    channel: LiveTvChannel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LiveBadge(text = "EN COURS", color = SmartVisionColors.Primary)
            Spacer(Modifier.weight(1f))
            Text(
                text = channel.timeRange,
                color = SmartVisionColors.TextSecondary,
                style = LiveTvItemMetaStyle,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = if (channel.program == "Direct") channel.name else channel.program,
            color = SmartVisionColors.TextPrimary,
            style = LiveTvPreviewTitleStyle,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = channel.genre,
                color = SmartVisionColors.TextSecondary,
                style = LiveTvItemMetaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            LiveBadge(text = channel.quality, color = SmartVisionColors.PrimaryDark)
        }

        Spacer(Modifier.height(7.dp))

        Text(
            text = channel.description,
            color = SmartVisionColors.TextSecondary,
            style = LiveTvItemMetaStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SmartVisionColors.Border.copy(alpha = 0.72f)),
        )

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "A suivre",
                    color = SmartVisionColors.TextPrimary,
                    style = LiveTvItemMetaStyle,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = channel.nextProgram,
                    color = SmartVisionColors.TextPrimary,
                    style = LiveTvItemMetaStyle,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = channel.genre,
                    color = SmartVisionColors.TextSecondary,
                    style = LiveTvItemMetaStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = channel.nextTimeRange,
                color = SmartVisionColors.TextSecondary,
                style = LiveTvItemMetaStyle,
                maxLines = 1,
            )
        }
    }
}

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
private fun FranceFlagIcon(active: Boolean) {
    val shape = RoundedCornerShape(3.dp)
    Row(
        modifier = Modifier
            .size(width = 22.dp, height = 16.dp)
            .clip(shape)
            .border(
                BorderStroke(1.dp, if (active) Color.White.copy(alpha = 0.72f) else SmartVisionColors.Border),
                shape,
            ),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF1B4BFF)),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.White),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFFFF3048)),
        )
    }
}

@Composable
private fun ChannelLogo(
    channel: LiveTvChannel,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                when (channel.logoText) {
                    "2" -> Color.Transparent
                    "3" -> Color.Transparent
                    "arte" -> Color.Transparent
                    "W9" -> Color.Transparent
                    else -> Color.White.copy(alpha = if (active) 0.18f else 0.12f)
                },
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (!channel.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp),
            )
            return@Box
        }

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
                .height(34.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = SmartVisionColors.TextPrimary,
                style = LiveTvPanelTitleStyle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
        }

        Spacer(Modifier.height(12.dp))

        content()
    }
}

private object LiveTvDimens {
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
    val ChannelRowHeight = 46.dp
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
