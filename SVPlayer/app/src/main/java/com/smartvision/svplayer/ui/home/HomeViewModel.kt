package com.smartvision.svplayer.ui.home

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.diagnostics.PerformanceDiagnosticRecorder
import com.smartvision.svplayer.data.home.HomeTrendingPreparedPreview
import com.smartvision.svplayer.data.home.HomeContentRepository
import com.smartvision.svplayer.data.home.HomeTrendingPolicy
import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.home.HomeSlide
import com.smartvision.svplayer.data.home.HomeSlidesRepository
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.mock.HomePreviewMode
import com.smartvision.svplayer.data.mock.HomeVisualStyle
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.UserContentType
import com.smartvision.svplayer.data.tmdb.TmdbMatcher
import com.smartvision.svplayer.domain.model.PlaybackKind
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.domain.model.SyncStatus
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.domain.repository.CatalogContentCounts
import com.smartvision.svplayer.domain.repository.SettingsRepository
import com.smartvision.svplayer.ui.settings.allowsContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class HomeUiState(
    val profileId: String = "",
    val continueWatching: List<ContinueItem> = emptyList(),
    val trendingMovies: List<ContinueItem> = emptyList(),
    val trendingSeries: List<ContinueItem> = emptyList(),
    val slides: List<HomeSlide> = emptyList(),
    val catalogCounts: CatalogContentCounts = CatalogContentCounts(),
    val continueWatchingLoading: Boolean = false,
    val trendingLoading: Boolean = false,
    val catalogCountsLoading: Boolean = false,
    val catalogRevision: Long = 0L,
    val loadedCatalogRevision: Long = -1L,
    val syncInProgress: Boolean = false,
)

private data class HomeLoadingState(
    val profileId: String,
    val catalogRevision: Long,
    val loadedCatalogRevision: Long,
    val syncInProgress: Boolean,
    val continueWatching: Boolean,
    val trending: Boolean,
    val catalogCounts: CatalogContentCounts,
    val catalogCountsLoading: Boolean,
)

private data class HomeLoadGate(
    val profileId: String,
    val catalogRevision: Long,
    val loadedCatalogRevision: Long = -1L,
    val syncInProgress: Boolean = false,
)

private data class ScopedHomeItems<T>(
    val token: HomeLoadToken,
    val items: List<T>,
)

private data class ScopedCatalogCounts(
    val token: HomeLoadToken,
    val counts: CatalogContentCounts,
)

private data class ContinueLoadInput(
    val token: HomeLoadToken,
    val progress: List<PlaybackProgressEntity>,
    val settings: PlayerSettings,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val userContentRepository: UserContentRepository,
    private val catalogRepository: CatalogRepository,
    private val homeSlidesRepository: HomeSlidesRepository,
    private val homeContentRepository: HomeContentRepository,
    private val accountManager: XtreamAccountManager,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val cachedContinueWatching = userContentRepository
        .getCachedRecentProgressSnapshot(limit = ContinueWatchingSnapshotLimit)
        ?.toContinueItems()
        .orEmpty()
    private val continueWatchingColdStartAtMs = SystemClock.elapsedRealtime()
    private val initialProfileId = accountManager.activeProfileIdOrDefault()
    private val initialCatalogRevision = catalogRepository.catalogRevision.value
    private val initialToken = HomeLoadToken(initialProfileId, initialCatalogRevision)
    private val cachedTrending = homeContentRepository.getLastCachedTrendingSnapshot(initialProfileId)
    private val trendingMovies = MutableStateFlow(
        ScopedHomeItems(initialToken, cachedTrending?.movies.orEmpty()),
    )
    private val trendingSeries = MutableStateFlow(
        ScopedHomeItems(initialToken, cachedTrending?.series.orEmpty()),
    )
    private val slides = MutableStateFlow(homeSlidesRepository.getCachedSlides().orEmpty())
    private val continueWatchingLoading = MutableStateFlow(cachedContinueWatching.isEmpty())
    private val trendingLoading = MutableStateFlow(cachedTrending == null)
    private val catalogCounts = MutableStateFlow(
        ScopedCatalogCounts(initialToken, CatalogContentCounts()),
    )
    private val catalogCountsLoading = MutableStateFlow(true)
    private val loadGate = MutableStateFlow(
        HomeLoadGate(
            profileId = initialProfileId,
            catalogRevision = initialCatalogRevision,
            syncInProgress = catalogRepository.syncStatus.value is SyncStatus.Running,
        ),
    )
    private val loadingState = combine(
        continueWatchingLoading,
        trendingLoading,
        catalogCounts,
        catalogCountsLoading,
        loadGate,
    ) { continueLoading, trendLoading, scopedCounts, countsLoading, gate ->
        val counts = scopedCounts
            .takeIf {
                shouldApplyHomeLoadResult(
                    token = it.token,
                    activeProfileId = gate.profileId,
                    catalogRevision = gate.catalogRevision,
                )
            }
            ?.counts
            ?: CatalogContentCounts()
        HomeLoadingState(
            profileId = gate.profileId,
            catalogRevision = gate.catalogRevision,
            loadedCatalogRevision = gate.loadedCatalogRevision,
            syncInProgress = gate.syncInProgress,
            continueWatching = continueLoading,
            trending = trendLoading,
            catalogCounts = counts,
            catalogCountsLoading = countsLoading,
        )
    }
    private var trendingRefreshJob: Job? = null
    private var catalogCountsRefreshJob: Job? = null
    private var catalogCountsLoadedToken: HomeLoadToken? = null
    private var trendingLoadedToken: HomeLoadToken? = null
    private val trendingPreviewPrepareJobs = mutableMapOf<String, Job>()
    private val trendingPreviewPrepareSemaphore = Semaphore(TrendingPreviewPrepareConcurrency)
    private val continueWatching = accountManager.activeProfileId
        .map { accountManager.activeProfileIdOrDefault() }
        .distinctUntilChanged()
        .flatMapLatest { profileId ->
            combine(
                userContentRepository.observeRecentProgress(
                    profileId = profileId,
                    limit = ContinueWatchingSnapshotLimit,
                ),
                settingsRepository.settings,
                catalogRepository.catalogRevision,
            ) { progress, settings, revision ->
                ContinueLoadInput(
                    token = HomeLoadToken(profileId, revision),
                    progress = progress,
                    settings = settings,
                )
            }
                .onStart {
                    if (accountManager.activeProfileIdOrDefault() == profileId) {
                        continueWatchingLoading.value = true
                    }
                }
                .mapLatest { input ->
            // PERF_DIAG: measures why Continue watching can appear after Home is already visible.
            val startedAt = SystemClock.elapsedRealtime()
            ensureCurrent(input.token)
            val recent = input.progress
                .filter { it.positionMs > 5_000L }
                .filterNot { HomeTrendingPolicy.containsAdultMarker(it.title, it.subtitle) }
                .mapNotNull { progress ->
                    ensureCurrent(input.token)
                    try {
                        resolveContinueWatchingProgress(
                            progress = progress,
                            profileId = input.token.profileId,
                            userContentRepository = userContentRepository,
                            catalogRepository = catalogRepository,
                            activeProfileId = accountManager::activeProfileIdOrDefault,
                        )
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Throwable) {
                        null
                    }
                }
                .filter { it.isAllowedByParentalControl(input.settings, catalogRepository) }
                .distinctBy(::historyGroupingKey)
                .take(10)
            val items = recent.mapNotNull { item ->
                ensureCurrent(input.token)
                toContinueItemWithPreview(item, catalogRepository)
            }
            if (cachedContinueWatching.isEmpty()) {
                awaitMinimumHomeLoading(continueWatchingColdStartAtMs)
            }
            ensureCurrent(input.token)
            continueWatchingLoading.value = false
            PerformanceDiagnosticRecorder.recordDuration(
                sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
                event = "continue_watching_flow_mapped",
                startedAtMs = startedAt,
                fields = mapOf(
                    "profileId" to input.token.profileId,
                    "catalogRevision" to input.token.catalogRevision,
                    "rawItems" to input.progress.size,
                    "recentItems" to recent.size,
                    "mappedItems" to items.size,
                    "previewReadyItems" to items.count { !it.previewUrl.isNullOrBlank() || !it.previewYoutubeKey.isNullOrBlank() },
                ),
            )
            ScopedHomeItems(input.token, items)
        }.catch { error ->
            if (error is CancellationException) throw error
            val token = currentHomeLoadToken()
            if (token.profileId == profileId) {
                continueWatchingLoading.value = false
                emit(ScopedHomeItems(token, emptyList()))
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        continueWatching,
        trendingMovies,
        trendingSeries,
        slides,
        loadingState,
    ) { scopedContinue, scopedTrendMovies, scopedTrendSeries, homeSlides, loading ->
        val activeToken = HomeLoadToken(loading.profileId, loading.catalogRevision)
        val continueItems = scopedContinue.items.takeIf { scopedContinue.token == activeToken }.orEmpty()
        val trendMovieItems = scopedTrendMovies.items.takeIf { scopedTrendMovies.token == activeToken }.orEmpty()
        val trendSeriesItems = scopedTrendSeries.items.takeIf { scopedTrendSeries.token == activeToken }.orEmpty()
        HomeUiState(
            profileId = loading.profileId,
            continueWatching = continueItems,
            trendingMovies = trendMovieItems,
            trendingSeries = trendSeriesItems,
            slides = homeSlides,
            catalogCounts = loading.catalogCounts,
            continueWatchingLoading = loading.continueWatching,
            trendingLoading = loading.trending,
            catalogCountsLoading = loading.catalogCountsLoading,
            catalogRevision = loading.catalogRevision,
            loadedCatalogRevision = loading.loadedCatalogRevision,
            syncInProgress = loading.syncInProgress,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        // PERF_FIX: first Home composition should not be empty when startup/repositories already hold caches.
        initialValue = HomeUiState(
            profileId = initialProfileId,
            continueWatching = cachedContinueWatching,
            trendingMovies = cachedTrending?.movies.orEmpty(),
            trendingSeries = cachedTrending?.series.orEmpty(),
            slides = slides.value,
            continueWatchingLoading = cachedContinueWatching.isEmpty(),
            trendingLoading = cachedTrending == null,
            catalogCountsLoading = true,
            catalogRevision = initialCatalogRevision,
            loadedCatalogRevision = -1L,
            syncInProgress = catalogRepository.syncStatus.value is SyncStatus.Running,
        ),
    )

    init {
        // PERF_DIAG: initial cache state seen by Home before any refresh coroutine finishes.
        PerformanceDiagnosticRecorder.record(
            sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
            event = "home_viewmodel_init",
            fields = mapOf(
                "cachedContinueWatching" to cachedContinueWatching.size,
                "cachedTrendingMovies" to trendingMovies.value.items.size,
                "cachedTrendingSeries" to trendingSeries.value.items.size,
                "cachedSlides" to slides.value.size,
            ),
        )
        observeProfileChanges()
        observeCatalogRevision()
        observeSyncStatus()
        refreshSlides()
        refreshCatalogCounts()
        refreshTrending(forceRefresh = false)
    }

    private fun observeProfileChanges() {
        viewModelScope.launch {
            var previousProfileId = accountManager.activeProfileIdOrDefault()
            accountManager.activeProfileId.collect {
                val nextProfileId = accountManager.activeProfileIdOrDefault()
                if (nextProfileId == previousProfileId) return@collect
                previousProfileId = nextProfileId
                val token = HomeLoadToken(nextProfileId, catalogRepository.catalogRevision.value)
                resetHomeData(token)
                refreshCatalogCounts()
                refreshTrending(forceRefresh = false)
            }
        }
    }

    private fun observeCatalogRevision() {
        viewModelScope.launch {
            var previousRevision = catalogRepository.catalogRevision.value
            catalogRepository.catalogRevision.collect { revision ->
                if (revision == previousRevision) return@collect
                previousRevision = revision
                val token = HomeLoadToken(accountManager.activeProfileIdOrDefault(), revision)
                resetHomeData(token)
                refreshCatalogCounts()
                refreshTrending(forceRefresh = false)
            }
        }
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            catalogRepository.syncStatus.collect { status ->
                loadGate.update { gate ->
                    gate.copy(syncInProgress = status is SyncStatus.Running)
                }
            }
        }
    }

    private fun resetHomeData(token: HomeLoadToken) {
        trendingRefreshJob?.cancel()
        catalogCountsRefreshJob?.cancel()
        trendingPreviewPrepareJobs.values.forEach(Job::cancel)
        trendingPreviewPrepareJobs.clear()
        catalogCountsLoadedToken = null
        trendingLoadedToken = null
        loadGate.value = HomeLoadGate(
            profileId = token.profileId,
            catalogRevision = token.catalogRevision,
            loadedCatalogRevision = -1L,
            syncInProgress = catalogRepository.syncStatus.value is SyncStatus.Running,
        )
        continueWatchingLoading.value = true
        trendingMovies.value = ScopedHomeItems(token, emptyList())
        trendingSeries.value = ScopedHomeItems(token, emptyList())
        catalogCounts.value = ScopedCatalogCounts(token, CatalogContentCounts())
        catalogCountsLoading.value = true
        trendingLoading.value = true
    }

    private fun currentHomeLoadToken(): HomeLoadToken =
        HomeLoadToken(
            profileId = accountManager.activeProfileIdOrDefault(),
            catalogRevision = catalogRepository.catalogRevision.value,
        )

    private fun isCurrent(token: HomeLoadToken): Boolean =
        shouldApplyHomeLoadResult(
            token = token,
            activeProfileId = accountManager.activeProfileIdOrDefault(),
            catalogRevision = catalogRepository.catalogRevision.value,
        )

    private fun ensureCurrent(token: HomeLoadToken) {
        if (!isCurrent(token)) {
            throw CancellationException("Home load superseded by another profile or catalog revision")
        }
    }

    private fun markCatalogCountsLoaded(token: HomeLoadToken) {
        if (!isCurrent(token)) return
        catalogCountsLoadedToken = token
        markCatalogRevisionLoadedIfComplete(token)
    }

    private fun markTrendingLoaded(token: HomeLoadToken) {
        if (!isCurrent(token)) return
        trendingLoadedToken = token
        markCatalogRevisionLoadedIfComplete(token)
    }

    private fun markCatalogRevisionLoadedIfComplete(token: HomeLoadToken) {
        if (
            !isCurrent(token) ||
            catalogCountsLoadedToken != token ||
            trendingLoadedToken != token
        ) {
            return
        }
        loadGate.update { gate ->
            if (gate.profileId == token.profileId && gate.catalogRevision == token.catalogRevision) {
                gate.copy(loadedCatalogRevision = token.catalogRevision)
            } else {
                gate
            }
        }
    }

    fun refreshSlides(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // PERF_DIAG: tells whether Home uses cached slides or waits for network refresh.
            val startedAt = SystemClock.elapsedRealtime()
            val cached = homeSlidesRepository.getCachedSlides()
            if (!forceRefresh && !cached.isNullOrEmpty()) {
                slides.value = cached
                PerformanceDiagnosticRecorder.recordDuration(
                    sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
                    event = "home_slides_cache_hit",
                    startedAtMs = startedAt,
                    fields = mapOf("items" to cached.size),
                )
                return@launch
            }
            runCatching { homeSlidesRepository.refresh() }
                .onSuccess { refreshed ->
                    slides.value = refreshed
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
                        event = "home_slides_refreshed",
                        startedAtMs = startedAt,
                        fields = mapOf("items" to refreshed.size, "forceRefresh" to forceRefresh),
                    )
                }
                .onFailure { error ->
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_ERRORS,
                        event = "home_slides_refresh_failed",
                        startedAtMs = startedAt,
                        fields = mapOf("forceRefresh" to forceRefresh),
                        error = error,
                    )
                }
        }
    }

    fun refreshCatalogCounts() {
        val token = currentHomeLoadToken()
        catalogCountsRefreshJob?.cancel()
        catalogCountsLoadedToken = null
        loadGate.update { gate ->
            if (gate.profileId == token.profileId && gate.catalogRevision == token.catalogRevision) {
                gate.copy(loadedCatalogRevision = -1L)
            } else {
                gate
            }
        }
        catalogCounts.value = ScopedCatalogCounts(token, CatalogContentCounts())
        catalogCountsLoading.value = true
        catalogCountsRefreshJob = viewModelScope.launch {
            try {
                val counts = catalogRepository.getCatalogContentCounts()
                if (isCurrent(token)) {
                    catalogCounts.value = ScopedCatalogCounts(token, counts)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                PerformanceDiagnosticRecorder.record(
                    sheet = PerformanceDiagnosticRecorder.SHEET_ERRORS,
                    event = "home_catalog_counts_refresh_failed",
                    fields = mapOf(
                        "profileId" to token.profileId,
                        "catalogRevision" to token.catalogRevision,
                        "error" to error.javaClass.simpleName,
                    ),
                )
            } finally {
                if (isCurrent(token)) {
                    catalogCountsLoading.value = false
                    markCatalogCountsLoaded(token)
                }
            }
        }
    }

    fun refreshTrending(forceRefresh: Boolean = false) {
        if (!forceRefresh && trendingRefreshJob?.isActive == true) return
        val token = currentHomeLoadToken()
        trendingRefreshJob?.cancel()
        trendingLoadedToken = null
        loadGate.update { gate ->
            if (gate.profileId == token.profileId && gate.catalogRevision == token.catalogRevision) {
                gate.copy(loadedCatalogRevision = -1L)
            } else {
                gate
            }
        }
        trendingRefreshJob = viewModelScope.launch {
            // PERF_DIAG: tells whether trends are consumed from startup cache or recomputed on Home.
            val startedAt = SystemClock.elapsedRealtime()
            val cached = if (forceRefresh) null else homeContentRepository.getCachedTrending()
            trendingLoading.value = cached == null
            if (cached != null) {
                ensureCurrent(token)
                trendingMovies.value = ScopedHomeItems(token, cached.movies)
                trendingSeries.value = ScopedHomeItems(token, cached.series)
                PerformanceDiagnosticRecorder.recordDuration(
                    sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
                    event = "home_trending_cache_hit",
                    startedAtMs = startedAt,
                    fields = mapOf(
                        "movies" to cached.movies.size,
                        "series" to cached.series.size,
                    ),
                )
            } else {
                val snapshot = try {
                    homeContentRepository.refreshTrending(forceRefresh)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_ERRORS,
                        event = "home_trending_refresh_failed",
                        startedAtMs = startedAt,
                        fields = mapOf("forceRefresh" to forceRefresh),
                        error = error,
                    )
                    null
                }
                if (snapshot != null) {
                    ensureCurrent(token)
                    trendingMovies.value = ScopedHomeItems(token, snapshot.movies)
                    trendingSeries.value = ScopedHomeItems(token, snapshot.series)
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
                        event = "home_trending_refreshed",
                        startedAtMs = startedAt,
                        fields = mapOf(
                            "movies" to snapshot.movies.size,
                            "series" to snapshot.series.size,
                            "forceRefresh" to forceRefresh,
                        ),
                    )
                }
            }
            awaitMinimumHomeLoading(startedAt)
            ensureCurrent(token)
            trendingLoading.value = false
            markTrendingLoaded(token)
        }
    }

    suspend fun loadSavedTrendingMovies(forceRefresh: Boolean = false) {
        val token = currentHomeLoadToken()
        val items = homeContentRepository.refreshTrendingMovies(forceRefresh)
        if (isCurrent(token)) {
            trendingMovies.value = ScopedHomeItems(token, items)
        }
    }

    suspend fun loadSavedTrendingSeries(forceRefresh: Boolean = false) {
        val token = currentHomeLoadToken()
        val items = homeContentRepository.refreshTrendingSeries(forceRefresh)
        if (isCurrent(token)) {
            trendingSeries.value = ScopedHomeItems(token, items)
        }
    }

    fun prefetchTrendingPreviews(items: List<ContinueItem>) {
        items.forEach(::prepareTrendingPreview)
    }

    fun prepareTrendingPreview(item: ContinueItem) {
        val key = item.trendingPreviewKey() ?: return
        if (isTrendingPreviewPrepared(item.id)) return
        if (trendingPreviewPrepareJobs[key.jobKey]?.isActive == true) return
        val token = currentHomeLoadToken()
        trendingPreviewPrepareJobs[key.jobKey] = viewModelScope.launch {
            trendingPreviewPrepareSemaphore.withPermit {
                val prepared = homeContentRepository.prepareTrendingPreview(
                    contentType = key.contentType,
                    contentId = key.contentId,
                    fallbackPosterUrl = item.imageUrl,
                ) ?: return@withPermit
                ensureCurrent(token)
                applyPreparedTrendingPreview(prepared)
            }
        }
    }

    private fun isTrendingPreviewPrepared(itemId: String): Boolean =
        trendingMovies.value.items.any { it.id == itemId && it.previewPrepared } ||
            trendingSeries.value.items.any { it.id == itemId && it.previewPrepared }

    private fun applyPreparedTrendingPreview(prepared: HomeTrendingPreparedPreview) {
        val itemId = "${prepared.contentType}:${prepared.contentId}"
        when (prepared.contentType) {
            TrendingMovieType -> trendingMovies.update { items ->
                items.copy(
                    items = items.items.map { item ->
                        if (item.id == itemId) prepared.applyTo(item) else item
                    },
                )
            }
            TrendingSeriesType -> trendingSeries.update { items ->
                items.copy(
                    items = items.items.map { item ->
                        if (item.id == itemId) prepared.applyTo(item) else item
                    },
                )
            }
        }
        PerformanceDiagnosticRecorder.record(
            sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
            event = "home_trending_preview_applied",
            fields = mapOf(
                "contentType" to prepared.contentType,
                "contentId" to prepared.contentId,
                "hasBackdrop" to prepared.backdropAvailable,
                "hasPreview" to prepared.previewAvailable,
            ),
        )
    }
}

private suspend fun resolveContinueWatchingProgress(
    progress: PlaybackProgressEntity,
    profileId: String,
    userContentRepository: UserContentRepository,
    catalogRepository: CatalogRepository,
    activeProfileId: () -> String,
): PlaybackProgressEntity {
    if (activeProfileId() != profileId || progress.profileId != profileId) {
        throw CancellationException("Continue watching profile changed")
    }
    var enriched = userContentRepository.enrichProgress(progress)
    if (progress.contentType != UserContentType.Episode) return enriched
    val episodeId = progress.contentId.toIntOrNull() ?: return enriched
    if (catalogRepository.getEpisodeById(episodeId) != null) return enriched
    val seriesId = enriched.parentContentId?.toIntOrNull()
        ?: progress.parentContentId?.toIntOrNull()
        ?: return enriched
    catalogRepository.getOrFetchSeriesEpisodes(seriesId)
    if (activeProfileId() != profileId) {
        throw CancellationException("Continue watching profile changed while loading episodes")
    }
    enriched = userContentRepository.enrichProgress(enriched)
    return enriched
}

private suspend fun PlaybackProgressEntity.isAllowedByParentalControl(
    settings: PlayerSettings,
    catalogRepository: CatalogRepository,
): Boolean {
    if (!settings.parentalControlEnabled) return true
    val id = contentId.toIntOrNull() ?: return settings.allowsContent(title, subtitle)
    return when (contentType) {
        UserContentType.Live -> catalogRepository.getLiveChannelById(id)?.let { channel ->
            settings.allowsContent(channel.name, channel.categoryName, title, subtitle)
        } ?: settings.allowsContent(title, subtitle)
        UserContentType.Movie -> catalogRepository.getMovieById(id)?.let { movie ->
            settings.allowsContent(movie.title, movie.plot, movie.genre, movie.categoryName, title, subtitle)
        } ?: settings.allowsContent(title, subtitle)
        UserContentType.Episode -> {
            val episode = catalogRepository.getEpisodeById(id)
            val seriesId = episode?.seriesId ?: parentContentId?.toIntOrNull()
            val series = seriesId?.let { catalogRepository.getSeriesByIds(listOf(it)).firstOrNull() }
            settings.allowsContent(
                episode?.title,
                episode?.plot,
                series?.title,
                series?.plot,
                series?.genre,
                series?.categoryName,
                title,
                subtitle,
            )
        }
        else -> settings.allowsContent(title, subtitle)
    }
}

private const val ContinueWatchingSnapshotLimit = 10
private const val TrendingPreviewPrepareConcurrency = 2
private const val TrendingMovieType = "movie"
private const val TrendingSeriesType = "series"

private data class TrendingPreviewKey(
    val contentType: String,
    val contentId: Int,
) {
    val jobKey: String = "$contentType:$contentId"
}

private fun ContinueItem.trendingPreviewKey(): TrendingPreviewKey? {
    val parts = id.split(":", limit = 2)
    if (parts.size != 2) return null
    val contentType = parts[0].takeIf { it == TrendingMovieType || it == TrendingSeriesType } ?: return null
    val contentId = parts[1].toIntOrNull() ?: return null
    return TrendingPreviewKey(contentType, contentId)
}

private fun toContinueItem(progress: PlaybackProgressEntity): ContinueItem? {
    val id = progress.contentId.toIntOrNull() ?: return null
    val duration = progress.durationMs
    val position = progress.positionMs.coerceAtLeast(0L)
    val visualStyle = when (progress.contentType) {
        UserContentType.Live -> HomeVisualStyle.Signal
        UserContentType.Movie -> HomeVisualStyle.Cinema
        UserContentType.Episode -> HomeVisualStyle.Series
        else -> HomeVisualStyle.Mystery
    }
    val title = progress.title?.cleanHomeTitle() ?: when (progress.contentType) {
        UserContentType.Live -> "Chaine $id"
        UserContentType.Movie -> "Film $id"
        UserContentType.Episode -> "Episode $id"
        else -> return null
    }
    val imageUrl = progress.imageUrl?.takeIf { it.isNotBlank() }
    val meta = progress.subtitle?.take(36) ?: when (progress.contentType) {
        UserContentType.Live -> "Live TV"
        UserContentType.Movie -> "Film"
        UserContentType.Episode -> "Serie"
        else -> "Media"
    }
    val ratio = if (duration > 0L) position.toFloat() / duration.toFloat() else 0f
    return ContinueItem(
        id = "${progress.contentType}:$id",
        title = title,
        meta = meta,
        remaining = if (
            progress.contentType != UserContentType.Live &&
            duration > position
        ) {
            (duration - position).formatRemaining()
        } else {
            ""
        },
        progress = ratio.coerceIn(0f, 1f),
        visualStyle = visualStyle,
        imageUrl = imageUrl,
        secondaryLabel = progress.episodeBadge(),
        mediaType = when (progress.contentType) {
            UserContentType.Live -> "LIVE"
            UserContentType.Movie -> "FILM"
            UserContentType.Episode -> "SERIE"
            else -> "MEDIA"
        },
    )
}

private suspend fun toContinueItemWithPreview(
    progress: PlaybackProgressEntity,
    catalogRepository: CatalogRepository,
): ContinueItem? {
    val base = toContinueItem(progress) ?: return null
    val previewKind = progress.contentType.toPreviewPlaybackKind() ?: return base
    val request = try {
        catalogRepository.buildPlaybackRequest(previewKind, progress.contentId)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Throwable) {
        null
    }
    val url = request?.url?.takeIf { it.isNotBlank() } ?: return base
    val previewMode = when (progress.contentType) {
        UserContentType.Live -> HomePreviewMode.LiveImmediate
        UserContentType.Movie, UserContentType.Episode -> HomePreviewMode.ResumeLoop
        else -> HomePreviewMode.None
    }
    return base.copy(
        previewUrl = url,
        previewImageUrl = base.previewImageUrl,
        previewMode = previewMode,
        previewStartPositionMs = if (previewMode == HomePreviewMode.ResumeLoop) {
            (request.resumePositionMs.takeIf { it > 0L } ?: progress.positionMs).coerceAtLeast(0L)
        } else {
            0L
        },
    )
}

private fun List<PlaybackProgressEntity>.toContinueItems(): List<ContinueItem> =
    filter { it.positionMs > 5_000L }
        .distinctBy(::historyGroupingKey)
        .take(10)
        .mapNotNull(::toContinueItem)

private fun String.toPreviewPlaybackKind(): PlaybackKind? =
    when (this) {
        UserContentType.Live -> PlaybackKind.Live
        UserContentType.Movie -> PlaybackKind.Movie
        UserContentType.Episode -> PlaybackKind.Episode
        else -> null
    }

private fun historyGroupingKey(progress: PlaybackProgressEntity): String =
    if (progress.contentType == UserContentType.Episode) {
        "series:${progress.parentContentId ?: progress.title.orEmpty().lowercase()}"
    } else {
        "${progress.contentType}:${progress.contentId}"
    }

private fun Long.formatRemaining(): String {
    val minutes = (this / 60_000L).coerceAtLeast(1L)
    val hours = minutes / 60L
    val remainingMinutes = minutes % 60L
    return if (hours > 0L) {
        "${hours} h ${remainingMinutes.toString().padStart(2, '0')} min"
    } else {
        "$minutes min"
    }
}

private fun String.cleanHomeTitle(): String =
    TmdbMatcher.cleanDisplayTitle(this)
        .replace(Regex("\\s+"), " ")
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" 4K", "", ignoreCase = true)
        .trim()

private fun PlaybackProgressEntity.episodeBadge(): String? {
    if (contentType != UserContentType.Episode) return null
    val source = listOfNotNull(subtitle, title).joinToString(" ")
    val match = EpisodeBadgeRegex.find(source) ?: return null
    val season = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
    val episode = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return null
    return "S$season E$episode"
}

private suspend fun awaitMinimumHomeLoading(startedAtMs: Long) {
    val remainingMs = MinimumHomeSkeletonMillis - (SystemClock.elapsedRealtime() - startedAtMs)
    if (remainingMs > 0L) delay(remainingMs)
}

private val EpisodeBadgeRegex =
    Regex("""(?i)\bS(?:aison)?\s*0*(\d{1,3})\s*[-_. ]*E(?:pisode)?\s*0*(\d{1,3})\b""")
private const val MinimumHomeSkeletonMillis = 650L
