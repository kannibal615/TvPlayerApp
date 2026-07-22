package com.smartvision.svplayer.ui.home

import android.media.MediaPlayer
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.ProfileType
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.data.diagnostics.PerformanceDiagnosticRecorder
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.mock.HomeCategory
import com.smartvision.svplayer.data.mock.HomeCategoryType
import com.smartvision.svplayer.data.mock.HomeVisualStyle
import com.smartvision.svplayer.domain.model.MediaSection
import com.smartvision.svplayer.domain.model.SyncStatus
import com.smartvision.svplayer.startup.StartupCatalogWorkKind
import com.smartvision.svplayer.sync.SyncFrequencyPolicy
import com.smartvision.svplayer.ui.focus.awaitItemVisible
import com.smartvision.svplayer.ui.focus.remoteMultiPressShortcuts
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.appScreenBackground
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    state: HomeUiState,
    activeProfile: PlaylistProfile?,
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
    xtreamCatalogNavigationBlocked: Boolean,
    onXtreamBlocked: () -> Unit,
    onContentClick: (ContinueItem) -> Unit,
    onTrendingContentClick: (ContinueItem) -> Unit,
    modifier: Modifier = Modifier,
    headerFocusRequest: Int = 0,
    headerFocusTarget: HomeHeaderFocusTarget = HomeHeaderFocusTarget.CurrentTab,
    visibleToUser: Boolean = true,
    onProfileAvatarBoundsChanged: (Rect) -> Unit = {},
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current.applicationContext
    val previewController = rememberHomePreviewController(context)
    val focusScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    var verticalFocusJob by remember { mutableStateOf<Job?>(null) }
    val startupWorkRequest by container.startupCatalogWork.collectAsStateWithLifecycle()
    val activePlaylistSource by container.accountManager.activePlaylistSource.collectAsStateWithLifecycle()
    val activeM3uUrl by container.accountManager.m3uUrl.collectAsStateWithLifecycle()
    val activeProfileId = state.profileId
    val kidsMode = activeProfile?.type == ProfileType.KIDS
    var catalogWorkUiState by remember { mutableStateOf(HomeCatalogWorkUiState.Idle) }
    var initialHomeVisibilityHandled by remember { mutableStateOf(false) }
    val catalogWorkActive = catalogWorkUiState.active
    val catalogSyncActive = catalogWorkActive && catalogWorkUiState.kind == StartupCatalogWorkKind.Synchronize
    val homeTabFocusRequester = remember { FocusRequester() }
    val licenseFocusRequester = remember { FocusRequester() }
    val notificationsFocusRequester = remember { FocusRequester() }
    val profileFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    val liveFocusRequester = remember { FocusRequester() }
    val moviesFocusRequester = remember { FocusRequester() }
    val seriesFocusRequester = remember { FocusRequester() }
    val continueFirstFocusRequester = remember { FocusRequester() }
    val movieTrendFirstFocusRequester = remember { FocusRequester() }
    val seriesTrendFirstFocusRequester = remember { FocusRequester() }
    val hasContinueWatching = state.continueWatching.isNotEmpty()
    val hasMovieTrends = state.trendingMovies.isNotEmpty()
    val hasSeriesTrends = state.trendingSeries.isNotEmpty()
    val showContinueSkeleton = state.continueWatchingLoading
    val showMovieTrendSkeleton = state.trendingLoading
    val showSeriesTrendSkeleton = state.trendingLoading
    val continueRowState = rememberHomeRowState(state.continueWatching)
    val movieTrendRowState = rememberHomeRowState(state.trendingMovies)
    val seriesTrendRowState = rememberHomeRowState(state.trendingSeries)
    val continueItemFocusRequesters = rememberHomeItemFocusRequesters(
        state.continueWatching,
        continueFirstFocusRequester,
    )
    val movieTrendItemFocusRequesters = rememberHomeItemFocusRequesters(
        state.trendingMovies,
        movieTrendFirstFocusRequester,
    )
    val seriesTrendItemFocusRequesters = rememberHomeItemFocusRequesters(
        state.trendingSeries,
        seriesTrendFirstFocusRequester,
    )
    val homeCategories = remember(strings) { homeCategories(strings) }

    LaunchedEffect(activeProfileId) {
        previewController.stop()
    }

    // PERF_DIAG: Home lifecycle and visible section counts for the splash-to-home handoff.
    DisposableEffect(Unit) {
        PerformanceDiagnosticRecorder.recordMemory(
            sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
            event = "home_screen_composed",
            fields = mapOf("currentRoute" to currentRoute),
        )
        onDispose {
            PerformanceDiagnosticRecorder.record(
                sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
                event = "home_screen_disposed",
            )
        }
    }

    LaunchedEffect(
        state.continueWatching.size,
        state.trendingMovies.size,
        state.trendingSeries.size,
        state.slides.size,
        state.continueWatchingLoading,
        state.trendingLoading,
    ) {
        // PERF_DIAG: records when history/trends/slides actually become visible to Compose.
        PerformanceDiagnosticRecorder.recordMemory(
            sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
            event = "home_state_visible",
            fields = mapOf(
                "continueWatching" to state.continueWatching.size,
                "trendingMovies" to state.trendingMovies.size,
                "trendingSeries" to state.trendingSeries.size,
                "slides" to state.slides.size,
                "hasContinueWatching" to hasContinueWatching,
                "hasMovieTrends" to hasMovieTrends,
                "hasSeriesTrends" to hasSeriesTrends,
                "continueWatchingLoading" to state.continueWatchingLoading,
                "trendingLoading" to state.trendingLoading,
            ),
        )
    }

    val categoryScrollPx = 0
    val homeHeroBlockHeight =
        SmartVisionDimensions.HomeHeroHeight +
            SmartVisionDimensions.HomeHeaderToHeroSpacing
    val continueScrollPx = with(density) { homeHeroBlockHeight.roundToPx() }
        .coerceAtLeast(0)
    val movieTrendScrollPx = with(density) {
        val continueBlockHeight = if (hasContinueWatching) {
            SmartVisionDimensions.HomeContentRowHeight + SmartVisionDimensions.HomeContentSectionSpacing
        } else {
            0.dp
        }
        (homeHeroBlockHeight + continueBlockHeight).roundToPx()
    }.coerceAtLeast(0)
    val seriesTrendScrollPx = with(density) {
        val continueBlockHeight = if (hasContinueWatching) {
            SmartVisionDimensions.HomeContentRowHeight + SmartVisionDimensions.HomeContentSectionSpacing
        } else {
            0.dp
        }
        (
            homeHeroBlockHeight +
                continueBlockHeight +
                SmartVisionDimensions.HomeContentRowHeight +
                SmartVisionDimensions.HomeContentSectionSpacing
            ).roundToPx()
    }.coerceAtLeast(0)

    DisposableEffect(Unit) {
        onDispose { verticalFocusJob?.cancel() }
    }

    BackHandler(enabled = catalogSyncActive) {
        // A real synchronization owns the remote until it finishes or fails.
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
        targetItemIndex: Int = 0,
        resetRowToFirst: Boolean = false,
    ) {
        verticalFocusJob?.cancel()
        verticalFocusJob = focusScope.launch {
            val clampedScroll = targetScrollPx.coerceIn(0, scrollState.maxValue)
            try {
                Log.i(
                    HomeFocusLogTag,
                    "scroll start target=$targetName from=${scrollState.value} to=$clampedScroll max=${scrollState.maxValue}",
                )
                // PERF_DIAG: vertical focus route timing helps diagnose non-Netflix-like transitions.
                PerformanceDiagnosticRecorder.record(
                    sheet = PerformanceDiagnosticRecorder.SHEET_ROW_FOCUS,
                    event = "home_vertical_scroll_start",
                    fields = mapOf(
                        "targetName" to targetName,
                        "from" to scrollState.value,
                        "to" to clampedScroll,
                        "max" to scrollState.maxValue,
                    ),
                )
                if (rowState != null) {
                    try {
                        val safeTargetIndex = targetItemIndex
                            .coerceIn(0, (rowState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
                        if (resetRowToFirst) {
                            animateRowToFirst(targetName, rowState)
                        } else if (rowState.layoutInfo.visibleItemsInfo.none { it.index == safeTargetIndex }) {
                            rowState.scrollToItem(safeTargetIndex)
                            rowState.awaitItemVisible(safeTargetIndex)
                        }
                        Log.i(
                            HomeFocusLogTag,
                            "requestRowFocus target=$targetName item=$safeTargetIndex reset=$resetRowToFirst",
                        )
                        PerformanceDiagnosticRecorder.record(
                            sheet = PerformanceDiagnosticRecorder.SHEET_ROW_FOCUS,
                            event = "home_row_target_prepared",
                            fields = mapOf(
                                "targetName" to targetName,
                                "targetItemIndex" to safeTargetIndex,
                                "resetRowToFirst" to resetRowToFirst,
                                "firstVisibleItemIndex" to rowState.firstVisibleItemIndex,
                                "firstVisibleItemScrollOffset" to rowState.firstVisibleItemScrollOffset,
                            ),
                        )
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Throwable) {
                        Log.w(HomeFocusLogTag, "requestRowFocus target=$targetName rowScrollFailed", error)
                        PerformanceDiagnosticRecorder.record(
                            sheet = PerformanceDiagnosticRecorder.SHEET_ERRORS,
                            event = "home_row_scroll_to_first_failed",
                            fields = mapOf("targetName" to targetName),
                            error = error,
                        )
                    }
                }
                scrollState.animateScrollTo(clampedScroll)
                Log.i(HomeFocusLogTag, "scroll end target=$targetName value=${scrollState.value}")
                PerformanceDiagnosticRecorder.record(
                    sheet = PerformanceDiagnosticRecorder.SHEET_ROW_FOCUS,
                    event = "home_vertical_scroll_end",
                    fields = mapOf("targetName" to targetName, "value" to scrollState.value),
                )
                withFrameNanos { }
                runCatching { focusRequester.requestFocus() }
                    .onSuccess {
                        Log.i(HomeFocusLogTag, "focus requested target=$targetName")
                        PerformanceDiagnosticRecorder.record(
                            sheet = PerformanceDiagnosticRecorder.SHEET_ROW_FOCUS,
                            event = "home_focus_requested",
                            fields = mapOf("targetName" to targetName),
                        )
                    }
                    .onFailure { error ->
                        Log.w(HomeFocusLogTag, "focus request failed target=$targetName", error)
                        PerformanceDiagnosticRecorder.record(
                            sheet = PerformanceDiagnosticRecorder.SHEET_ERRORS,
                            event = "home_focus_request_failed",
                            fields = mapOf("targetName" to targetName),
                            error = error,
                        )
                    }
            } catch (cancellation: CancellationException) {
                Log.i(HomeFocusLogTag, "scroll canceled target=$targetName")
                PerformanceDiagnosticRecorder.record(
                    sheet = PerformanceDiagnosticRecorder.SHEET_ROW_FOCUS,
                    event = "home_vertical_scroll_canceled",
                    fields = mapOf("targetName" to targetName),
                )
                throw cancellation
            }
        }
    }

    fun requestClosestRowFocus(
        targetName: String,
        targetItems: List<ContinueItem>,
        targetRowState: LazyListState,
        targetRequesters: Map<String, FocusRequester>,
        targetScrollPx: Int,
        sourceRowState: LazyListState,
        sourceIndex: Int,
    ) {
        if (targetItems.isEmpty()) return
        val targetIndex = closestVisibleHomeItemIndex(
            sourceRowState = sourceRowState,
            sourceIndex = sourceIndex,
            targetRowState = targetRowState,
            targetItemCount = targetItems.size,
        )
        val targetItem = targetItems[targetIndex]
        val targetRequester = targetRequesters[targetItem.id] ?: return
        requestVerticalFocus(
            targetName = targetName,
            focusRequester = targetRequester,
            targetScrollPx = targetScrollPx,
            rowState = targetRowState,
            targetItemIndex = targetIndex,
            resetRowToFirst = false,
        )
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
                targetItemIndex = 0,
                resetRowToFirst = true,
            )
            hasMovieTrends -> requestVerticalFocus(
                targetName = "trending_movies",
                focusRequester = movieTrendFirstFocusRequester,
                targetScrollPx = movieTrendScrollPx,
                rowState = movieTrendRowState,
                targetItemIndex = 0,
                resetRowToFirst = true,
            )
            hasSeriesTrends -> requestVerticalFocus(
                targetName = "trending_series",
                focusRequester = seriesTrendFirstFocusRequester,
                targetScrollPx = seriesTrendScrollPx,
                rowState = seriesTrendRowState,
                targetItemIndex = 0,
                resetRowToFirst = true,
            )
        }
    }

    fun requestMovieTrendFocus(
        sourceRowState: LazyListState,
        sourceIndex: Int,
    ) {
        when {
            hasMovieTrends -> requestClosestRowFocus(
                targetName = "trending_movies",
                targetItems = state.trendingMovies,
                targetRowState = movieTrendRowState,
                targetRequesters = movieTrendItemFocusRequesters,
                targetScrollPx = movieTrendScrollPx,
                sourceRowState = sourceRowState,
                sourceIndex = sourceIndex,
            )
            hasSeriesTrends -> requestClosestRowFocus(
                targetName = "trending_series",
                targetItems = state.trendingSeries,
                targetRowState = seriesTrendRowState,
                targetRequesters = seriesTrendItemFocusRequesters,
                targetScrollPx = seriesTrendScrollPx,
                sourceRowState = sourceRowState,
                sourceIndex = sourceIndex,
            )
        }
    }

    fun requestContinueFocus(
        sourceRowState: LazyListState,
        sourceIndex: Int,
    ) {
        if (hasContinueWatching) {
            requestClosestRowFocus(
                targetName = "continue_watching",
                targetItems = state.continueWatching,
                targetRowState = continueRowState,
                targetRequesters = continueItemFocusRequesters,
                targetScrollPx = continueScrollPx,
                sourceRowState = sourceRowState,
                sourceIndex = sourceIndex,
            )
        }
    }

    fun requestPreviousBeforeMovieTrend(sourceIndex: Int) {
        if (hasContinueWatching) {
            requestContinueFocus(movieTrendRowState, sourceIndex)
        } else {
            requestMainCategoryFocus()
        }
    }

    fun requestPreviousBeforeSeriesTrend(sourceIndex: Int) {
        when {
            hasMovieTrends -> requestClosestRowFocus(
                targetName = "trending_movies",
                targetItems = state.trendingMovies,
                targetRowState = movieTrendRowState,
                targetRequesters = movieTrendItemFocusRequesters,
                targetScrollPx = movieTrendScrollPx,
                sourceRowState = seriesTrendRowState,
                sourceIndex = sourceIndex,
            )
            hasContinueWatching -> requestContinueFocus(seriesTrendRowState, sourceIndex)
            else -> requestMainCategoryFocus()
        }
    }

    fun refreshHomeFromHeader() {
        verticalFocusJob?.cancel()
        verticalFocusJob = focusScope.launch {
            // PERF_DIAG: distinguishes HOME-header refresh from cold-start Home load.
            PerformanceDiagnosticRecorder.record(
                sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
                event = "home_header_refresh_started",
            )
            viewModel.refreshSlides(forceRefresh = true)
            viewModel.refreshTrending(forceRefresh = false, preserveReadyState = true)
            viewModel.refreshCatalogCounts(preserveReadyState = true)
            continueRowState.scrollToItem(0)
            movieTrendRowState.scrollToItem(0)
            seriesTrendRowState.scrollToItem(0)
            scrollState.animateScrollTo(0)
            withFrameNanos { }
            runCatching { liveFocusRequester.requestFocus() }
            withFrameNanos { }
            if (scrollState.value != 0) scrollState.scrollTo(0)
            PerformanceDiagnosticRecorder.record(
                sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
                event = "home_header_refresh_focus_restored",
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshSlides()
    }

    LaunchedEffect(
        visibleToUser,
        activeProfile?.id,
        activeProfile?.isConfigured,
        activePlaylistSource,
        activeM3uUrl,
    ) {
        if (!visibleToUser) {
            verticalFocusJob?.cancel()
            previewController.stop()
            return@LaunchedEffect
        }
        val firstVisibleEntry = !initialHomeVisibilityHandled
        initialHomeVisibilityHandled = true
        PerformanceDiagnosticRecorder.record(
            sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
            event = "home_first_launched_effect",
        )
        if (firstVisibleEntry) {
            playStartupChimeOnHome(context)
        }
        scrollState.scrollTo(0)
        withFrameNanos { }
        delay(80)
        runCatching { liveFocusRequester.requestFocus() }
        withFrameNanos { }
        if (scrollState.value != 0) scrollState.scrollTo(0)
        PerformanceDiagnosticRecorder.record(
            sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
            event = "home_initial_focus_requested",
        )
        if (shouldRequestPostHomeCatalogSync(container)) {
            PerformanceDiagnosticRecorder.record(
                sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
                event = "home_post_render_sync_requested",
            )
            container.requestStartupCatalogWork(StartupCatalogWorkKind.Synchronize)
        }
    }

    LaunchedEffect(headerFocusRequest, headerFocusTarget, visibleToUser) {
        if (visibleToUser && headerFocusRequest > 0) {
            scrollState.scrollTo(0)
            withFrameNanos { }
            delay(90)
            val requester = when (headerFocusTarget) {
                HomeHeaderFocusTarget.CurrentTab -> homeTabFocusRequester
                HomeHeaderFocusTarget.License -> licenseFocusRequester
                HomeHeaderFocusTarget.Notifications -> notificationsFocusRequester
                HomeHeaderFocusTarget.Profile -> profileFocusRequester
                HomeHeaderFocusTarget.Settings -> settingsFocusRequester
            }
            runCatching { requester.requestFocus() }
        }
    }

    LaunchedEffect(startupWorkRequest.requestedAtMs, visibleToUser) {
        if (!visibleToUser || !startupWorkRequest.active) return@LaunchedEffect
        val request = startupWorkRequest
        catalogWorkUiState = HomeCatalogWorkUiState.initial(request.kind, request.source)
        runCatching {
            when (request.kind) {
                StartupCatalogWorkKind.Synchronize -> {
                    val statusJob = launch {
                        container.catalogRepository.syncStatus.collect { status ->
                            val nextState = status.toHomeCatalogWorkUiState(request.kind, request.source)
                            if (nextState != null) {
                                catalogWorkUiState = nextState
                            }
                        }
                    }
                    try {
                        if (container.accountManager.activePlaylistSource.value == PlaylistSource.Xtream) {
                            val connection = container.xtreamConnectionManager.verifyQuick("home_startup_sync")
                            if (!connection.isConnected) {
                                throw IllegalStateException(connection.message.ifBlank { "Xtream unavailable" })
                            }
                        }
                        container.xtreamRepository.clearCaches()
                        container.catalogRepository.invalidateLocalCatalogCache()
                        container.synchronizeCatalog().getOrThrow()
                    } finally {
                        statusJob.cancel()
                    }
                }
                StartupCatalogWorkKind.LoadLocal -> {
                    Unit
                }
                StartupCatalogWorkKind.None -> Unit
            }
        }.onSuccess {
            catalogWorkUiState = catalogWorkUiState.completed()
            container.clearStartupCatalogWork(request.requestedAtMs)
            delay(2_500L)
            if (!catalogWorkUiState.active && catalogWorkUiState.errorMessage == null) {
                catalogWorkUiState = HomeCatalogWorkUiState.Idle
            }
        }.onFailure { error ->
            Log.w(HomeFocusLogTag, "startup catalog work failed: ${error.javaClass.simpleName}", error)
            catalogWorkUiState = catalogWorkUiState.failed(strings.catalogWorkFailed)
            container.clearStartupCatalogWork(request.requestedAtMs)
            delay(4_000L)
            if (catalogWorkUiState.errorMessage != null) {
                catalogWorkUiState = HomeCatalogWorkUiState.Idle
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .remoteMultiPressShortcuts(
                enabled = visibleToUser && !catalogSyncActive,
                onStart = {
                    focusScope.launch {
                        scrollState.animateScrollTo(0)
                        withFrameNanos { }
                        runCatching { liveFocusRequester.requestFocus() }
                    }
                },
                onEnd = {
                    focusScope.launch {
                        val target = when {
                            state.trendingSeries.isNotEmpty() -> {
                                scrollState.animateScrollTo(scrollState.maxValue)
                                seriesTrendItemFocusRequesters[state.trendingSeries.last().id]
                            }
                            state.trendingMovies.isNotEmpty() -> {
                                scrollState.animateScrollTo(scrollState.maxValue)
                                movieTrendItemFocusRequesters[state.trendingMovies.last().id]
                            }
                            state.continueWatching.isNotEmpty() -> {
                                scrollState.animateScrollTo(scrollState.maxValue)
                                continueItemFocusRequesters[state.continueWatching.last().id]
                            }
                            else -> seriesFocusRequester
                        }
                        withFrameNanos { }
                        runCatching { target?.requestFocus() }
                    }
                },
                onHeader = {
                    focusScope.launch {
                        scrollState.animateScrollTo(0)
                        withFrameNanos { }
                        runCatching { homeTabFocusRequester.requestFocus() }
                    }
                },
            )
            .onPreviewKeyEvent { !visibleToUser || catalogSyncActive }
            .appScreenBackground(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF123B70).copy(alpha = 0.72f),
                        Color(0xFF09182B),
                        Color(0xFF030914),
                    ),
                    radius = 1500f,
                ),
            )
            .padding(horizontal = SmartVisionDimensions.HomeScreenPadding)
            .padding(
                top = SmartVisionDimensions.HomeHeaderTopPadding,
                bottom = SmartVisionDimensions.AppScreenVerticalPadding,
            ),
    ) {
        TvHeader(
            currentRoute = currentRoute,
            tabs = tabs,
            onNavigate = { route ->
                if (route == currentRoute) {
                    refreshHomeFromHeader()
                } else {
                    onNavigate(route)
                }
            },
            onSync = onSync,
            onSettings = onSettings,
            onProfile = onProfile,
            onNotifications = onNotifications,
            onLicenseKey = onLicenseKey,
            showLicenseKey = showLicenseKey,
            hasNewNotifications = hasNewNotifications,
            notificationBadgeCount = notificationBadgeCount,
            activeProfileOverride = activeProfile,
            currentTabFocusRequester = homeTabFocusRequester,
            licenseFocusRequester = licenseFocusRequester,
            notificationsFocusRequester = notificationsFocusRequester,
            profileFocusRequester = profileFocusRequester,
            settingsFocusRequester = settingsFocusRequester,
            modifier = Modifier.fillMaxWidth(),
            onProfileAvatarBoundsChanged = onProfileAvatarBoundsChanged,
        )

        Spacer(Modifier.height(SmartVisionDimensions.HomeHeaderContentClearance))

        HomeVerticalScrollColumn(
            scrollState = scrollState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            HomeHeroBanner(
                strings = strings,
                remoteSlides = state.slides,
                kidsMode = kidsMode,
                profileKey = activeProfileId,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(SmartVisionDimensions.HomeHeaderToHeroSpacing))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SmartVisionDimensions.HomeCategoryHeight),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                homeCategories.forEachIndexed { index, category ->
                    HomeCategoryCard(
                        category = category,
                        onClick = {
                            if (catalogSyncActive) {
                                Unit
                            } else if (xtreamCatalogNavigationBlocked) {
                                onXtreamBlocked()
                            } else {
                                onNavigate(category.routeName)
                            }
                        },
                        blocked = xtreamCatalogBlocked,
                        blockedMessage = strings.connectionUnavailable,
                        workOverlay = catalogWorkUiState.overlayFor(category.id, strings),
                        kidsMode = kidsMode,
                        itemCount = when (category.type) {
                            HomeCategoryType.Live -> state.catalogCounts.live
                            HomeCategoryType.Movies -> state.catalogCounts.movies
                            HomeCategoryType.Series -> state.catalogCounts.series
                        },
                        focusRequester = when (category.id) {
                            "live" -> liveFocusRequester
                            "movies" -> moviesFocusRequester
                            "series" -> seriesFocusRequester
                            else -> null
                        },
                        onLeft = if (index == 0) {
                            { runCatching { seriesFocusRequester.requestFocus() } }
                        } else {
                            null
                        },
                        onRight = if (index == homeCategories.lastIndex) {
                            { runCatching { liveFocusRequester.requestFocus() } }
                        } else {
                            null
                        },
                        onDown = if (hasContinueWatching || hasMovieTrends || hasSeriesTrends) {
                            { requestFirstHomeRowFocus() }
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(SmartVisionDimensions.HomeContentSectionSpacing))

            if (showContinueSkeleton) {
                HomeSkeletonRow(
                    title = strings.continueWatching,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(SmartVisionDimensions.HomeContentSectionSpacing))
            } else if (hasContinueWatching) {
                ContinueWatchingRow(
                    title = strings.continueWatching,
                    items = state.continueWatching,
                    previewController = previewController,
                    onItemClick = onContentClick,
                    lazyListState = continueRowState,
                    firstItemFocusRequester = continueFirstFocusRequester,
                    itemFocusRequesters = continueItemFocusRequesters,
                    onDownFromRow = if (hasMovieTrends || hasSeriesTrends) {
                        { sourceIndex -> requestMovieTrendFocus(continueRowState, sourceIndex) }
                    } else {
                        null
                    },
                    onUpFromRow = { _ -> requestMainCategoryFocus() },
                    enablePreview = true,
                    timeRemainingFormat = strings.timeRemainingFormat,
                    resumeOverlayText = strings.resumePlayback,
                    blocked = xtreamCatalogBlocked,
                    blockedMessage = strings.connectionUnavailable,
                    onBlockedClick = onXtreamBlocked,
                    modifier = Modifier
                        .fillMaxWidth(),
                )
                Spacer(Modifier.height(SmartVisionDimensions.HomeContentSectionSpacing))
            }

            if (showMovieTrendSkeleton) {
                HomeSkeletonRow(
                    title = strings.trendingMovies,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (hasMovieTrends) {
                TrendingContentRow(
                    title = strings.trendingMovies,
                    previewNamespace = "trending-movies",
                    items = state.trendingMovies,
                    previewController = previewController,
                    onItemClick = onTrendingContentClick,
                    onPrepareItems = viewModel::prefetchTrendingPreviews,
                    lazyListState = movieTrendRowState,
                    firstItemFocusRequester = movieTrendFirstFocusRequester,
                    itemFocusRequesters = movieTrendItemFocusRequesters,
                    onDownFromRow = if (hasSeriesTrends) {
                        { sourceIndex ->
                            requestClosestRowFocus(
                                targetName = "trending_series",
                                targetItems = state.trendingSeries,
                                targetRowState = seriesTrendRowState,
                                targetRequesters = seriesTrendItemFocusRequesters,
                                targetScrollPx = seriesTrendScrollPx,
                                sourceRowState = movieTrendRowState,
                                sourceIndex = sourceIndex,
                            )
                        }
                    } else {
                        null
                    },
                    onUpFromRow = { sourceIndex -> requestPreviousBeforeMovieTrend(sourceIndex) },
                    blocked = xtreamCatalogBlocked,
                    blockedMessage = strings.connectionUnavailable,
                    onBlockedClick = onXtreamBlocked,
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            }

            if ((hasMovieTrends || showMovieTrendSkeleton) && (hasSeriesTrends || showSeriesTrendSkeleton)) {
                Spacer(Modifier.height(SmartVisionDimensions.HomeContentSectionSpacing))
            }

            if (showSeriesTrendSkeleton) {
                HomeSkeletonRow(
                    title = strings.trendingSeries,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (hasSeriesTrends) {
                TrendingContentRow(
                    title = strings.trendingSeries,
                    previewNamespace = "trending-series",
                    items = state.trendingSeries,
                    previewController = previewController,
                    onItemClick = onTrendingContentClick,
                    onPrepareItems = viewModel::prefetchTrendingPreviews,
                    lazyListState = seriesTrendRowState,
                    firstItemFocusRequester = seriesTrendFirstFocusRequester,
                    itemFocusRequesters = seriesTrendItemFocusRequesters,
                    onUpFromRow = { sourceIndex -> requestPreviousBeforeSeriesTrend(sourceIndex) },
                    blocked = xtreamCatalogBlocked,
                    blockedMessage = strings.connectionUnavailable,
                    onBlockedClick = onXtreamBlocked,
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private object HomeManualBringIntoViewSpec : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float,
    ): Float = 0f
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun HomeVerticalScrollColumn(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val platformBringIntoViewSpec = LocalBringIntoViewSpec.current
    CompositionLocalProvider(LocalBringIntoViewSpec provides HomeManualBringIntoViewSpec) {
        Column(modifier = modifier.verticalScroll(scrollState)) {
            val columnScope = this
            // Restore the platform policy for the nested LazyRows so horizontal
            // focus reveal keeps working; only the parent vertical scroll is manual.
            CompositionLocalProvider(LocalBringIntoViewSpec provides platformBringIntoViewSpec) {
                content(columnScope)
            }
        }
    }
}

@Composable
private fun rememberHomeRowState(items: List<ContinueItem>): LazyListState {
    val firstId = items.firstOrNull()?.id.orEmpty()
    return remember(firstId, items.size) { LazyListState() }
}

@Composable
private fun rememberHomeItemFocusRequesters(
    items: List<ContinueItem>,
    firstItemFocusRequester: FocusRequester,
): Map<String, FocusRequester> {
    val signature = items.joinToString("|") { it.id }
    return remember(signature, firstItemFocusRequester) {
        items.mapIndexed { index, item ->
            item.id to if (index == 0) firstItemFocusRequester else FocusRequester()
        }.toMap()
    }
}

private fun closestVisibleHomeItemIndex(
    sourceRowState: LazyListState,
    sourceIndex: Int,
    targetRowState: LazyListState,
    targetItemCount: Int,
): Int {
    if (targetItemCount <= 0) return 0
    val sourceInfo = sourceRowState.layoutInfo.visibleItemsInfo
        .firstOrNull { it.index == sourceIndex }
    val sourceCenter = sourceInfo
        ?.let { it.offset + (it.size / 2) }
        ?: (
            sourceRowState.layoutInfo.viewportStartOffset +
                sourceRowState.layoutInfo.viewportEndOffset
            ) / 2
    return targetRowState.layoutInfo.visibleItemsInfo
        .asSequence()
        .filter { it.index in 0 until targetItemCount }
        .minByOrNull { item ->
            abs((item.offset + (item.size / 2)) - sourceCenter)
        }
        ?.index
        ?: sourceIndex.coerceIn(0, targetItemCount - 1)
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

private fun homeCategories(strings: SmartVisionStrings): List<HomeCategory> = listOf(
    HomeCategory(
        id = "live",
        type = HomeCategoryType.Live,
        badge = "LIVE",
        title = strings.liveTv,
        subtitle = strings.emptySubtitle,
        actionLabel = strings.liveTvSubtitle,
        visualStyle = HomeVisualStyle.Signal,
    ),
    HomeCategory(
        id = "movies",
        type = HomeCategoryType.Movies,
        badge = "VOD",
        title = strings.movies,
        subtitle = strings.emptySubtitle,
        actionLabel = strings.moviesSubtitle,
        visualStyle = HomeVisualStyle.Cinema,
    ),
    HomeCategory(
        id = "series",
        type = HomeCategoryType.Series,
        badge = "SERIES",
        title = strings.series,
        subtitle = strings.emptySubtitle,
        actionLabel = strings.seriesSubtitle,
        visualStyle = HomeVisualStyle.Series,
    ),
)

private val HomeCategory.routeName: String
    get() = when (id) {
        "live" -> "live_tv"
        "movies" -> "movies"
        else -> "series"
    }

private fun String.isHomeXtreamRoute(): Boolean =
    this == "live_tv" || this == "movies" || this == "series"

private data class HomeCatalogWorkUiState(
    val kind: StartupCatalogWorkKind = StartupCatalogWorkKind.None,
    val source: PlaylistSource = PlaylistSource.Xtream,
    val active: Boolean = false,
    val live: HomeCatalogSectionUiState = HomeCatalogSectionUiState(),
    val movies: HomeCatalogSectionUiState = HomeCatalogSectionUiState(),
    val series: HomeCatalogSectionUiState = HomeCatalogSectionUiState(),
    val errorMessage: String? = null,
    val profileName: String = "",
    val kidsMode: Boolean = false,
    val startedAtMs: Long? = null,
) {
    fun withSection(
        section: MediaSection,
        phase: SyncStatus.SyncSectionPhase,
        progress: Int,
        message: String? = null,
    ): HomeCatalogWorkUiState =
        when (section) {
            MediaSection.Live -> copy(live = live.copy(phase = phase, progress = progress, message = message))
            MediaSection.Movies -> copy(movies = movies.copy(phase = phase, progress = progress, message = message))
            MediaSection.Series -> copy(series = series.copy(phase = phase, progress = progress, message = message))
        }

    fun completed(): HomeCatalogWorkUiState =
        copy(
            active = false,
            live = live.takeIf { source == PlaylistSource.Xtream || it.phase != SyncStatus.SyncSectionPhase.WAITING }
                ?.copy(phase = SyncStatus.SyncSectionPhase.COMPLETED, progress = 100)
                ?: live,
            movies = movies.copy(phase = SyncStatus.SyncSectionPhase.COMPLETED, progress = 100),
            series = series.copy(phase = SyncStatus.SyncSectionPhase.COMPLETED, progress = 100),
        )

    fun failed(message: String): HomeCatalogWorkUiState {
        fun HomeCatalogSectionUiState.markFailedIfRunning(): HomeCatalogSectionUiState =
            if (phase != SyncStatus.SyncSectionPhase.COMPLETED && phase != SyncStatus.SyncSectionPhase.WAITING) {
                copy(phase = SyncStatus.SyncSectionPhase.ERROR, message = message)
            } else {
                this
            }
        return copy(
            active = false,
            live = live.markFailedIfRunning(),
            movies = movies.markFailedIfRunning(),
            series = series.markFailedIfRunning(),
            errorMessage = message,
        )
    }

    fun overlayFor(categoryId: String, strings: SmartVisionStrings): HomeCategoryWorkOverlay? {
        val section = when (categoryId) {
            "live" -> live
            "movies" -> movies
            "series" -> series
            else -> return null
        }
        if (section.phase == SyncStatus.SyncSectionPhase.WAITING ||
            section.phase == SyncStatus.SyncSectionPhase.COMPLETED
        ) return null
        val running = section.phase != SyncStatus.SyncSectionPhase.WAITING &&
            section.phase != SyncStatus.SyncSectionPhase.ERROR &&
            section.phase != SyncStatus.SyncSectionPhase.COMPLETED
        if (!running && section.phase != SyncStatus.SyncSectionPhase.ERROR) return null
        val label = when {
            section.phase == SyncStatus.SyncSectionPhase.ERROR -> section.message ?: strings.catalogWorkFailed
            section.phase == SyncStatus.SyncSectionPhase.COMPLETED -> section.message ?: strings.catalogWorkCompleted
            kind == StartupCatalogWorkKind.LoadLocal -> strings.catalogLoadInProgress
            else -> strings.catalogDownloadInProgress
        }
        return HomeCategoryWorkOverlay(
            progress = section.progress.toFloat() / 100f,
            active = active && running,
            error = section.phase == SyncStatus.SyncSectionPhase.ERROR,
            label = label,
            detail = buildString {
                if (profileName.isNotBlank()) append(profileName)
                if (kidsMode) { if (isNotEmpty()) append(" • "); append("Kids") }
                startedAtMs?.let { started ->
                    if (isNotEmpty()) append(" • ")
                    append(((System.currentTimeMillis() - started).coerceAtLeast(0L) / 1_000L)).append("s")
                }
                if (section.currentItems > 0 && isNotEmpty()) append(" • ")
                if (section.currentItems > 0) append(section.currentItems)
                section.totalItems?.takeIf { it > 0 }?.let { append(" / ").append(it) }
                section.keptItems?.let {
                    if (isNotEmpty()) append(" • ")
                    append(it).append(" conserves")
                }
                section.excludedItems?.takeIf { it > 0 }?.let {
                    append(" • ").append(it).append(" exclus")
                }
            }.ifBlank { null },
        )
    }

    companion object {
        val Idle = HomeCatalogWorkUiState()

        fun initial(kind: StartupCatalogWorkKind, source: PlaylistSource): HomeCatalogWorkUiState =
            HomeCatalogWorkUiState(
                kind = kind,
                source = source,
                active = true,
                live = HomeCatalogSectionUiState(),
                movies = if (source == PlaylistSource.Xtream) HomeCatalogSectionUiState() else HomeCatalogSectionUiState(
                    phase = SyncStatus.SyncSectionPhase.COMPLETED,
                    progress = 100,
                ),
                series = if (source == PlaylistSource.Xtream) HomeCatalogSectionUiState() else HomeCatalogSectionUiState(
                    phase = SyncStatus.SyncSectionPhase.COMPLETED,
                    progress = 100,
                ),
            )
    }
}

private data class HomeCatalogSectionUiState(
    val phase: SyncStatus.SyncSectionPhase = SyncStatus.SyncSectionPhase.WAITING,
    val progress: Int = 0,
    val message: String? = null,
    val currentItems: Int = 0,
    val totalItems: Int? = null,
    val keptItems: Int? = null,
    val excludedItems: Int? = null,
)

private fun SyncStatus.toHomeCatalogWorkUiState(
    kind: StartupCatalogWorkKind,
    source: PlaylistSource,
): HomeCatalogWorkUiState? =
    when (this) {
        SyncStatus.Idle -> null
        is SyncStatus.Running -> HomeCatalogWorkUiState(
            kind = kind,
            source = source,
            active = true,
            profileName = profileName,
            kidsMode = kidsMode,
            startedAtMs = startedAtMs,
            live = catalogProgress.live.toHomeSectionUiState(),
            movies = if (source == PlaylistSource.Xtream) catalogProgress.movies.toHomeSectionUiState() else HomeCatalogSectionUiState(
                phase = SyncStatus.SyncSectionPhase.COMPLETED,
                progress = 100,
            ),
            series = if (source == PlaylistSource.Xtream) catalogProgress.series.toHomeSectionUiState() else HomeCatalogSectionUiState(
                phase = SyncStatus.SyncSectionPhase.COMPLETED,
                progress = 100,
            ),
        )
        is SyncStatus.Success -> HomeCatalogWorkUiState(
            kind = kind,
            source = source,
            active = false,
            live = catalogProgress.live.toHomeSectionUiState().copy(phase = SyncStatus.SyncSectionPhase.COMPLETED, progress = 100),
            movies = catalogProgress.movies.toHomeSectionUiState().copy(phase = SyncStatus.SyncSectionPhase.COMPLETED, progress = 100),
            series = catalogProgress.series.toHomeSectionUiState().copy(phase = SyncStatus.SyncSectionPhase.COMPLETED, progress = 100),
        )
        is SyncStatus.Error -> HomeCatalogWorkUiState(
            kind = kind,
            source = source,
            active = false,
            live = catalogProgress.live.toHomeSectionUiState(),
            movies = catalogProgress.movies.toHomeSectionUiState(),
            series = catalogProgress.series.toHomeSectionUiState(),
            errorMessage = message,
        ).failed(message)
    }

private fun SyncStatus.SyncSectionProgress.toHomeSectionUiState(): HomeCatalogSectionUiState =
    HomeCatalogSectionUiState(
        phase = phase,
        progress = percent,
        message = message,
        currentItems = currentItems,
        totalItems = totalItems,
        keptItems = keptItems,
        excludedItems = excludedItems,
    )

private const val HomeFocusLogTag = "SVHomeFocus"

private suspend fun shouldRequestPostHomeCatalogSync(
    container: com.smartvision.svplayer.core.data.AppContainer,
): Boolean {
    if (container.catalogRepository.syncStatus.value is SyncStatus.Running) return false
    val activeProfile = container.accountManager.activeProfile() ?: return false
    val source = container.accountManager.activePlaylistSource.value
    if (source == PlaylistSource.Xtream && !container.accountManager.current().isConfigured) return false
    if (source == PlaylistSource.M3u && container.accountManager.m3uUrl.value.isBlank()) return false
    if (!container.accountManager.isCatalogCurrent(activeProfile)) return true
    val hasLocalCatalog = container.catalogRepository.hasLocalCatalogForActiveProfile()
    val settings = container.settingsRepository.settings.first()
    val lastSync = container.syncStateDao.get(container.accountManager.activeProfileIdOrDefault())?.lastSync
    val policy = SyncFrequencyPolicy.from(settings.syncFrequency)
    val allowStartupRun = if (policy.runOnStartup) {
        container.claimStartupSyncPolicyEvaluation()
    } else {
        container.claimStartupSyncPolicyEvaluation()
        false
    }
    return SyncFrequencyPolicy.isSynchronizationDue(
        value = settings.syncFrequency,
        lastSyncAt = lastSync,
        hasLocalCatalog = hasLocalCatalog,
        allowRunOnStartup = allowStartupRun,
    )
}
