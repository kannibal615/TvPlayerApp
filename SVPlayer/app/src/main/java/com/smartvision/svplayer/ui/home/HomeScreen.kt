package com.smartvision.svplayer.ui.home

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.mock.HomeNavigationData
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
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
    strings: SmartVisionStrings,
    xtreamCatalogBlocked: Boolean,
    onXtreamBlocked: () -> Unit,
    onContentClick: (ContinueItem) -> Unit,
    onContinueViewAll: () -> Unit,
    onTrendingViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current.applicationContext
    val focusScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    var verticalFocusJob by remember { mutableStateOf<Job?>(null) }
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            HomeViewModel(
                userContentRepository = container.userContentRepository,
                catalogRepository = container.catalogRepository,
                xtreamRepository = container.xtreamRepository,
                appConfigRepository = container.appConfigRepository,
                homeSlidesRepository = container.homeSlidesRepository,
            )
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val liveFocusRequester = remember { FocusRequester() }
    val continueFirstFocusRequester = remember { FocusRequester() }
    val movieTrendFirstFocusRequester = remember { FocusRequester() }
    val seriesTrendFirstFocusRequester = remember { FocusRequester() }
    val continueRowState = rememberLazyListState()
    val movieTrendRowState = rememberLazyListState()
    val seriesTrendRowState = rememberLazyListState()
    val hasContinueWatching = state.continueWatching.isNotEmpty()
    val hasMovieTrends = state.trendingMovies.isNotEmpty()
    val hasSeriesTrends = state.trendingSeries.isNotEmpty()

    val categoryScrollPx = 0
    val continueScrollPx = with(density) { (SmartVisionDimensions.HomeHeroHeight + 16.dp).roundToPx() }
        .coerceAtLeast(0)
    val movieTrendScrollPx = with(density) {
        val continueBlockHeight = if (hasContinueWatching) {
            SmartVisionDimensions.HomeContentRowHeight + SmartVisionDimensions.HomeTrendFoldOffset
        } else {
            0.dp
        }
        (SmartVisionDimensions.HomeHeroHeight + 16.dp + continueBlockHeight).roundToPx()
    }.coerceAtLeast(0)
    val seriesTrendScrollPx = with(density) {
        val continueBlockHeight = if (hasContinueWatching) {
            SmartVisionDimensions.HomeContentRowHeight + SmartVisionDimensions.HomeTrendFoldOffset
        } else {
            0.dp
        }
        (
            SmartVisionDimensions.HomeHeroHeight +
                16.dp +
                continueBlockHeight +
                SmartVisionDimensions.HomeContentRowHeight +
                16.dp
            ).roundToPx()
    }.coerceAtLeast(0)

    DisposableEffect(Unit) {
        onDispose { verticalFocusJob?.cancel() }
    }

    suspend fun animateRowToFirst(targetName: String, rowState: LazyListState) {
        if (rowState.firstVisibleItemIndex == 0 && rowState.firstVisibleItemScrollOffset == 0) {
            Log.i(HomeFocusLogTag, "requestRowFocus target=$targetName rowAlreadyAtFirst")
            return
        }
        rowState.animateScrollToItem(0)
    }

    fun requestVerticalFocus(
        targetName: String,
        focusRequester: FocusRequester,
        targetScrollPx: Int,
        rowState: LazyListState? = null,
    ) {
        verticalFocusJob?.cancel()
        verticalFocusJob = focusScope.launch {
            val clampedScroll = targetScrollPx.coerceIn(0, scrollState.maxValue)
            try {
                Log.i(
                    HomeFocusLogTag,
                    "scroll start target=$targetName from=${scrollState.value} to=$clampedScroll max=${scrollState.maxValue}",
                )
                if (rowState != null) {
                    try {
                        animateRowToFirst(targetName, rowState)
                        Log.i(HomeFocusLogTag, "requestRowFocus target=$targetName rowScrolledToFirst")
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Throwable) {
                        Log.w(HomeFocusLogTag, "requestRowFocus target=$targetName rowScrollFailed", error)
                    }
                }
                scrollState.animateScrollTo(clampedScroll)
                Log.i(HomeFocusLogTag, "scroll end target=$targetName value=${scrollState.value}")
                withFrameNanos { }
                runCatching { focusRequester.requestFocus() }
                    .onSuccess { Log.i(HomeFocusLogTag, "focus requested target=$targetName") }
                    .onFailure { error -> Log.w(HomeFocusLogTag, "focus request failed target=$targetName", error) }
            } catch (cancellation: CancellationException) {
                Log.i(HomeFocusLogTag, "scroll canceled target=$targetName")
                throw cancellation
            }
        }
    }

    fun requestMainCategoryFocus() {
        requestVerticalFocus(
            targetName = "categories",
            focusRequester = liveFocusRequester,
            targetScrollPx = categoryScrollPx,
        )
    }

    fun requestFirstHomeRowFocus() {
        when {
            hasContinueWatching -> requestVerticalFocus(
                targetName = "continue_watching",
                focusRequester = continueFirstFocusRequester,
                targetScrollPx = continueScrollPx,
                rowState = continueRowState,
            )
            hasMovieTrends -> requestVerticalFocus(
                targetName = "trending_movies",
                focusRequester = movieTrendFirstFocusRequester,
                targetScrollPx = movieTrendScrollPx,
                rowState = movieTrendRowState,
            )
            hasSeriesTrends -> requestVerticalFocus(
                targetName = "trending_series",
                focusRequester = seriesTrendFirstFocusRequester,
                targetScrollPx = seriesTrendScrollPx,
                rowState = seriesTrendRowState,
            )
        }
    }

    fun requestMovieTrendFocus() {
        when {
            hasMovieTrends -> requestVerticalFocus(
                targetName = "trending_movies",
                focusRequester = movieTrendFirstFocusRequester,
                targetScrollPx = movieTrendScrollPx,
                rowState = movieTrendRowState,
            )
            hasSeriesTrends -> requestVerticalFocus(
                targetName = "trending_series",
                focusRequester = seriesTrendFirstFocusRequester,
                targetScrollPx = seriesTrendScrollPx,
                rowState = seriesTrendRowState,
            )
        }
    }

    fun requestContinueFocus() {
        if (hasContinueWatching) {
            requestVerticalFocus(
                targetName = "continue_watching",
                focusRequester = continueFirstFocusRequester,
                targetScrollPx = continueScrollPx,
                rowState = continueRowState,
            )
        }
    }

    LaunchedEffect(Unit) {
        playStartupChimeOnHome(context)
        viewModel.refreshSlides()
        viewModel.refreshTrending()
        withFrameNanos { }
        liveFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SmartVisionColors.PrimaryDark.copy(alpha = 0.38f),
                        SmartVisionColors.Background,
                        Color(0xFF01040C),
                    ),
                    radius = 1500f,
                ),
            )
            .padding(horizontal = SmartVisionDimensions.HomeScreenPadding)
            .padding(top = SmartVisionDimensions.HomeHeaderTopPadding),
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

        Spacer(Modifier.height(SmartVisionDimensions.HomeHeaderToHeroSpacing))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            HomeHeroBanner(
                remoteSlides = state.slides,
                onNavigate = { route ->
                    if (xtreamCatalogBlocked && route.isHomeXtreamRoute()) {
                        onXtreamBlocked()
                    } else {
                        onNavigate(route)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SmartVisionDimensions.HomeCategoryHeight),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HomeNavigationData.categories.forEach { category ->
                    HomeCategoryCard(
                        category = category,
                        onClick = {
                            if (xtreamCatalogBlocked) onXtreamBlocked() else onNavigate(category.routeName)
                        },
                        blocked = xtreamCatalogBlocked,
                        focusRequester = if (category.id == "live") liveFocusRequester else null,
                        onDown = { requestFirstHomeRowFocus() },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (hasContinueWatching) {
                ContinueWatchingRow(
                    title = strings.continueWatching,
                    items = state.continueWatching,
                    showViewAll = true,
                    viewAllText = strings.viewAll,
                    onViewAll = onContinueViewAll,
                    onItemClick = onContentClick,
                    lazyListState = continueRowState,
                    firstItemFocusRequester = continueFirstFocusRequester,
                    onDownFromRow = { requestMovieTrendFocus() },
                    onUpFromRow = { requestMainCategoryFocus() },
                    enablePreview = true,
                    resumeOverlayText = strings.resumePlayback,
                    blocked = xtreamCatalogBlocked,
                    onBlockedClick = onXtreamBlocked,
                    modifier = Modifier
                        .fillMaxWidth(),
                )
                Spacer(Modifier.height(SmartVisionDimensions.HomeTrendFoldOffset))
            }

            ContinueWatchingRow(
                title = strings.trendingMovies,
                items = state.trendingMovies,
                showViewAll = true,
                viewAllText = strings.viewAll,
                onViewAll = onTrendingViewAll,
                onItemClick = onContentClick,
                lazyListState = movieTrendRowState,
                firstItemFocusRequester = movieTrendFirstFocusRequester,
                onDownFromRow = {
                    if (hasSeriesTrends) {
                        requestVerticalFocus(
                            targetName = "trending_series",
                            focusRequester = seriesTrendFirstFocusRequester,
                            targetScrollPx = seriesTrendScrollPx,
                            rowState = seriesTrendRowState,
                        )
                    }
                },
                onUpFromRow = { requestContinueFocus() },
                enablePreview = true,
                blocked = xtreamCatalogBlocked,
                onBlockedClick = onXtreamBlocked,
                modifier = Modifier
                    .fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            ContinueWatchingRow(
                title = strings.trendingSeries,
                items = state.trendingSeries,
                showViewAll = true,
                viewAllText = strings.viewAll,
                onViewAll = onTrendingViewAll,
                onItemClick = onContentClick,
                lazyListState = seriesTrendRowState,
                firstItemFocusRequester = seriesTrendFirstFocusRequester,
                onUpFromRow = { requestMovieTrendFocus() },
                enablePreview = true,
                blocked = xtreamCatalogBlocked,
                onBlockedClick = onXtreamBlocked,
                modifier = Modifier
                    .fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun playStartupChimeOnHome(context: android.content.Context) {
    if (HomeStartupSoundState.played) return
    HomeStartupSoundState.played = true
    runCatching {
        MediaPlayer.create(context, R.raw.startup_chime)?.apply {
            setOnCompletionListener { player -> player.release() }
            start()
        }
    }
}

private object HomeStartupSoundState {
    var played: Boolean = false
}

private val com.smartvision.svplayer.data.mock.HomeCategory.routeName: String
    get() = when (id) {
        "live" -> "live_tv"
        "movies" -> "movies"
        else -> "series"
    }

private fun String.isHomeXtreamRoute(): Boolean =
    this == "live_tv" || this == "movies" || this == "series"

private const val HomeFocusLogTag = "SVHomeFocus"
