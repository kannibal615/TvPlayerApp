package com.smartvision.svplayer.ui.live

import android.net.Uri
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.monetization.MonetizationManager
import androidx.media3.exoplayer.ima.ImaAdsLoader
import com.smartvision.svplayer.ui.activation.XtreamQrSetupPanel
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.catalog.CatalogSearchField
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.home.HeaderControls
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay

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
            )
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by container.accountManager.accounts.collectAsStateWithLifecycle()
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    val firstChannelFocusRequester = remember { FocusRequester() }
    var inputReady by remember { mutableStateOf(false) }
    var categorySearchQuery by remember { mutableStateOf("") }
    var contentSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        delay(260)
        inputReady = true
    }

    LaunchedEffect(state.categoriesLoading, accounts.isNotEmpty()) {
        if (accounts.isNotEmpty() && !state.categoriesLoading) {
            withFrameNanos { }
            delay(120)
            selectedCategoryFocusRequester.requestFocus()
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
        LiveTvHeader(
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
                    onChannelFocused = viewModel::focusChannel,
                    onChannelClick = { channel ->
                        if (inputReady) {
                            val openFullPlayer = viewModel.activateChannel(channel)
                            if (openFullPlayer) {
                                onWatch(channel.streamId)
                            }
                        }
                    },
                    onRetry = viewModel::retryCurrentCategory,
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                )
                PreviewPanel(
                    channel = state.selectedChannel,
                    idleAdContentUrl = state.focusedChannel?.streamUrl,
                    monetizationManager = container.monetizationManager,
                    onWatch = {
                        if (inputReady) {
                            state.selectedChannel?.streamId?.let(onWatch)
                        }
                    },
                    onFavorite = { state.selectedChannel?.let(viewModel::toggleFavorite) },
                    modifier = Modifier
                        .weight(0.34f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun LiveTvHeader(
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
        modifier = modifier.height(LiveTvDimens.HeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveTvLogo()

        Spacer(Modifier.width(86.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                TvButton(
                    text = tab.label,
                    onClick = { onNavigate(tab.route) },
                    selected = tab.route == currentRoute,
                    variant = if (tab.route == currentRoute) TvButtonVariant.Primary else TvButtonVariant.Text,
                    contentPadding = PaddingValues(horizontal = 13.dp),
                    modifier = Modifier.height(36.dp),
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
private fun LiveTvLogo() {
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
                state.categories.filter { searchQuery.isBlank() || it.label.contains(searchQuery, ignoreCase = true) },
                key = { it.id },
            ) { category ->
                CategoryRow(
                    category = category,
                    selected = category.id == state.selectedCategoryId,
                    focusRequester = if (category.id == state.selectedCategoryId) {
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
    onChannelFocused: (LiveTvChannel) -> Unit,
    onChannelClick: (LiveTvChannel) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleChannels = state.channels.filter { channel ->
        searchQuery.isBlank() || channel.name.contains(searchQuery, ignoreCase = true)
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
            state.channelsLoading -> LoadingState(
                title = "Chargement des chaines",
                modifier = Modifier.fillMaxSize(),
            )

            state.errorMessage != null -> ErrorState(
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
                        focusRequester = firstChannelFocusRequester.takeIf { index == 0 },
                        leftFocusRequester = selectedCategoryFocusRequester,
                        onFocused = { onChannelFocused(channel) },
                        onClick = { onChannelClick(channel) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewPanel(
    channel: LiveTvChannel?,
    idleAdContentUrl: String?,
    monetizationManager: MonetizationManager,
    onWatch: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiveTvPanel(
        title = "Apercu",
        modifier = modifier,
    ) {
        if (channel == null) {
            Column(modifier = Modifier.fillMaxSize()) {
                IdlePreviewAdFrame(
                    contentUrl = idleAdContentUrl,
                    monetizationManager = monetizationManager,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.88f),
                )
                Spacer(Modifier.height(10.dp))
                EmptyState(
                    title = "Selectionnez une chaine",
                    subtitle = "Appuyez sur OK pour lancer l'apercu.",
                    modifier = Modifier.weight(1f),
                )
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
private fun IdlePreviewAdFrame(
    contentUrl: String?,
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
        if (tagUrl != null && !contentUrl.isNullOrBlank()) {
            var adCycle by remember(tagUrl, contentUrl) { mutableIntStateOf(0) }
            key(adCycle) {
                IdlePreviewImaPlayer(
                    adTagUrl = tagUrl,
                    contentUrl = contentUrl,
                    monetizationManager = monetizationManager,
                    onCycleFinished = { adCycle += 1 },
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
private fun IdlePreviewImaPlayer(
    adTagUrl: String,
    contentUrl: String,
    monetizationManager: MonetizationManager,
    onCycleFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var loading by remember(adTagUrl) { mutableStateOf(true) }
    var adStarted by remember(adTagUrl) { mutableStateOf(false) }
    var adFinished by remember(adTagUrl) { mutableStateOf(false) }
    var adFailed by remember(adTagUrl) { mutableStateOf(false) }
    var cycleRestarted by remember(adTagUrl) { mutableStateOf(false) }
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
    val adsLoader = remember(adTagUrl) {
        ImaAdsLoader.Builder(context.applicationContext)
            .setImaSdkSettings(
                ImaSdkFactory.getInstance().createImaSdkSettings().apply {
                    playerType = "SmartVision Android TV Idle Preview"
                    playerVersion = com.smartvision.svplayer.BuildConfig.VERSION_NAME
                },
            )
            .setFocusSkipButtonWhenAvailable(false)
            .setEnableContinuousPlayback(false)
            .setAdPreloadTimeoutMs(8_000)
            .setVastLoadTimeoutMs(8_000)
            .setMediaLoadTimeoutMs(8_000)
            .setAdEventListener { event ->
                when (event.type) {
                    AdEvent.AdEventType.LOADED -> loading = false
                    AdEvent.AdEventType.STARTED -> {
                        loading = false
                        adStarted = true
                    }
                    AdEvent.AdEventType.COMPLETED,
                    AdEvent.AdEventType.SKIPPED,
                    AdEvent.AdEventType.ALL_ADS_COMPLETED,
                    AdEvent.AdEventType.CONTENT_RESUME_REQUESTED,
                    -> adFinished = true
                    else -> Unit
                }
            }
            .setAdErrorListener {
                loading = false
                adFailed = true
            }
            .build()
    }
    val mediaSourceFactory = remember(playerView, adsLoader) {
        DefaultMediaSourceFactory(context)
            .setLocalAdInsertionComponents({ adsLoader }, playerView)
    }
    val player = remember(mediaSourceFactory) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                volume = 0f
                playWhenReady = true
            }
    }

    LaunchedEffect(adTagUrl, contentUrl) {
        loading = true
        adStarted = false
        adFinished = false
        adFailed = false
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(contentUrl)
                .setAdsConfiguration(
                    MediaItem.AdsConfiguration.Builder(Uri.parse(adTagUrl)).build(),
                )
                .build(),
        )
        player.prepare()
        player.playWhenReady = true
    }

    LaunchedEffect(adFinished, adFailed) {
        if ((adFinished || adFailed) && !cycleRestarted) {
            cycleRestarted = true
            player.pause()
            player.stop()
            delay(300)
            onCycleFinished()
        }
    }

    LaunchedEffect(adStarted) {
        if (adStarted) {
            monetizationManager.onIdleLivePreviewAdStarted()
        }
    }

    DisposableEffect(player, adsLoader) {
        adsLoader.setPlayer(player)
        onDispose {
            adsLoader.setPlayer(null)
            player.release()
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

        if (adFinished || adFailed) {
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

@Composable
private fun PreviewActionButton(
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
    val shape = RoundedCornerShape(LiveTvDimens.ItemRadius)
    val active = focusState.isFocused || selected
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
            focusState.isFocused -> SmartVisionColors.FocusWhite
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
                    if (focusState.isFocused) SmartVisionDimensions.FocusBorder else SmartVisionDimensions.PanelBorder,
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
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> SmartVisionColors.FocusWhite
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
                    if (focusState.isFocused) SmartVisionDimensions.FocusBorder else SmartVisionDimensions.PanelBorder,
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
        CategoryIcon(category = category, active = active)
        Spacer(Modifier.width(10.dp))
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
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val active = selected || focusState.isFocused
    val shape = RoundedCornerShape(LiveTvDimens.ItemRadius)
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> SmartVisionColors.FocusWhite
            selected -> SmartVisionColors.Primary
            else -> SmartVisionColors.Border
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "channelBorder",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(LiveTvDimens.ChannelRowHeight)
            .focusProperties { left = leftFocusRequester }
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
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
                    if (focusState.isFocused) SmartVisionDimensions.FocusBorder else SmartVisionDimensions.PanelBorder,
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

@Composable
private fun CategoryIcon(
    category: LiveTvCategory,
    active: Boolean,
) {
    if (category.label.equals("Favoris", ignoreCase = true)) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = if (active) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        return
    }

    if (category.label.equals("Historiques", ignoreCase = true)) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = if (active) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        return
    }

    if (category.kind == LiveTvCategoryKind.France) {
        FranceFlagIcon(active = active)
        return
    }

    val icon = when (category.kind) {
        LiveTvCategoryKind.Sport -> Icons.Default.EmojiEvents
        LiveTvCategoryKind.Info -> Icons.Default.Info
        LiveTvCategoryKind.Cinema -> Icons.Default.Movie
        LiveTvCategoryKind.Kids -> Icons.Default.Tv
        LiveTvCategoryKind.Documentary -> Icons.Default.VideoLibrary
        LiveTvCategoryKind.France -> Icons.Default.Tv
        LiveTvCategoryKind.Generic -> Icons.Default.Tv
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (active) SmartVisionColors.TextPrimary else SmartVisionColors.TextSecondary,
        modifier = Modifier.size(20.dp),
    )
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
