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
import com.smartvision.svplayer.domain.model.PlaybackKind
import com.smartvision.svplayer.domain.repository.CatalogRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class HomeUiState(
    val continueWatching: List<ContinueItem> = emptyList(),
    val trendingMovies: List<ContinueItem> = emptyList(),
    val trendingSeries: List<ContinueItem> = emptyList(),
    val slides: List<HomeSlide> = emptyList(),
    val continueWatchingLoading: Boolean = false,
    val trendingLoading: Boolean = false,
)

private data class HomeLoadingState(
    val continueWatching: Boolean,
    val trending: Boolean,
)

class HomeViewModel(
    private val userContentRepository: UserContentRepository,
    private val catalogRepository: CatalogRepository,
    private val homeSlidesRepository: HomeSlidesRepository,
    private val homeContentRepository: HomeContentRepository,
    private val accountManager: XtreamAccountManager,
) : ViewModel() {
    private val cachedContinueWatching = userContentRepository
        .getCachedRecentProgressSnapshot(limit = ContinueWatchingSnapshotLimit)
        ?.toContinueItems()
        .orEmpty()
    private val cachedTrending = homeContentRepository.getLastCachedTrendingSnapshot()
    private val trendingMovies = MutableStateFlow(cachedTrending?.movies.orEmpty())
    private val trendingSeries = MutableStateFlow(cachedTrending?.series.orEmpty())
    private val slides = MutableStateFlow(homeSlidesRepository.getCachedSlides().orEmpty())
    private val continueWatchingLoading = MutableStateFlow(cachedContinueWatching.isEmpty())
    private val trendingLoading = MutableStateFlow(cachedTrending == null)
    private val loadingState = combine(
        continueWatchingLoading,
        trendingLoading,
    ) { continueLoading, trendLoading ->
        HomeLoadingState(
            continueWatching = continueLoading,
            trending = trendLoading,
        )
    }
    private var trendingRefreshJob: Job? = null
    private val trendingPreviewPrepareJobs = mutableMapOf<String, Job>()
    private val trendingPreviewPrepareSemaphore = Semaphore(TrendingPreviewPrepareConcurrency)
    private val continueWatching = userContentRepository.observeRecentProgress(limit = ContinueWatchingSnapshotLimit)
        .map { progress ->
            // PERF_DIAG: measures why Continue watching can appear after Home is already visible.
            val startedAt = SystemClock.elapsedRealtime()
            val recent = progress
                .filter { it.positionMs > 5_000L }
                .filterNot { HomeTrendingPolicy.containsAdultMarker(it.title, it.subtitle) }
                .map { userContentRepository.enrichProgress(it) }
                .distinctBy(::historyGroupingKey)
                .take(10)
            val items = recent.mapNotNull { item ->
                toContinueItemWithPreview(item, catalogRepository)
            }
            continueWatchingLoading.value = false
            PerformanceDiagnosticRecorder.recordDuration(
                sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
                event = "continue_watching_flow_mapped",
                startedAtMs = startedAt,
                fields = mapOf(
                    "rawItems" to progress.size,
                    "recentItems" to recent.size,
                    "mappedItems" to items.size,
                    "previewReadyItems" to items.count { !it.previewUrl.isNullOrBlank() || !it.previewYoutubeKey.isNullOrBlank() },
                ),
            )
            items
        }
        .onStart {
            if (cachedContinueWatching.isNotEmpty()) {
                continueWatchingLoading.value = false
                emit(cachedContinueWatching)
            }
        }

    val uiState: StateFlow<HomeUiState> = combine(
        continueWatching,
        trendingMovies,
        trendingSeries,
        slides,
        loadingState,
    ) { continueItems, trendMovieItems, trendSeriesItems, homeSlides, loading ->
        HomeUiState(
            continueWatching = continueItems,
            trendingMovies = trendMovieItems,
            trendingSeries = trendSeriesItems,
            slides = homeSlides,
            continueWatchingLoading = loading.continueWatching,
            trendingLoading = loading.trending,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        // PERF_FIX: first Home composition should not be empty when startup/repositories already hold caches.
        initialValue = HomeUiState(
            continueWatching = cachedContinueWatching,
            trendingMovies = cachedTrending?.movies.orEmpty(),
            trendingSeries = cachedTrending?.series.orEmpty(),
            slides = slides.value,
            continueWatchingLoading = cachedContinueWatching.isEmpty(),
            trendingLoading = cachedTrending == null,
        ),
    )

    init {
        // PERF_DIAG: initial cache state seen by Home before any refresh coroutine finishes.
        PerformanceDiagnosticRecorder.record(
            sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
            event = "home_viewmodel_init",
            fields = mapOf(
                "cachedContinueWatching" to cachedContinueWatching.size,
                "cachedTrendingMovies" to trendingMovies.value.size,
                "cachedTrendingSeries" to trendingSeries.value.size,
                "cachedSlides" to slides.value.size,
            ),
        )
        refreshSlides()
        observeProfileChanges()
    }

    private fun observeProfileChanges() {
        viewModelScope.launch {
            var previousProfileId = accountManager.activeProfileIdOrDefault()
            accountManager.activeProfileId.collect {
                val nextProfileId = accountManager.activeProfileIdOrDefault()
                if (nextProfileId == previousProfileId) return@collect
                previousProfileId = nextProfileId
                trendingRefreshJob?.cancel()
                trendingPreviewPrepareJobs.values.forEach { job -> job.cancel() }
                trendingPreviewPrepareJobs.clear()
                trendingMovies.value = emptyList()
                trendingSeries.value = emptyList()
                trendingLoading.value = true
                refreshTrending(forceRefresh = false)
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

    fun refreshTrending(forceRefresh: Boolean = false) {
        if (!forceRefresh && trendingRefreshJob?.isActive == true) return
        trendingRefreshJob = viewModelScope.launch {
            // PERF_DIAG: tells whether trends are consumed from startup cache or recomputed on Home.
            val startedAt = SystemClock.elapsedRealtime()
            val cached = if (forceRefresh) null else homeContentRepository.getCachedTrending()
            trendingLoading.value = cached == null
            if (cached != null) {
                trendingMovies.value = cached.movies
                trendingSeries.value = cached.series
                trendingLoading.value = false
                PerformanceDiagnosticRecorder.recordDuration(
                    sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
                    event = "home_trending_cache_hit",
                    startedAtMs = startedAt,
                    fields = mapOf(
                        "movies" to cached.movies.size,
                        "series" to cached.series.size,
                    ),
                )
                return@launch
            }
            val snapshot = runCatching { homeContentRepository.refreshTrending(forceRefresh) }
                .onFailure { error ->
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_ERRORS,
                        event = "home_trending_refresh_failed",
                        startedAtMs = startedAt,
                        fields = mapOf("forceRefresh" to forceRefresh),
                        error = error,
                    )
                }
                .getOrNull()
            if (snapshot == null) {
                trendingLoading.value = false
                return@launch
            }
            trendingMovies.value = snapshot.movies
            trendingSeries.value = snapshot.series
            trendingLoading.value = false
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

    suspend fun loadSavedTrendingMovies(forceRefresh: Boolean = false) {
        trendingLoading.value = true
        runCatching {
            trendingMovies.value = homeContentRepository.refreshTrendingMovies(forceRefresh)
        }.also {
            trendingLoading.value = false
        }.getOrThrow()
    }

    suspend fun loadSavedTrendingSeries(forceRefresh: Boolean = false) {
        trendingLoading.value = true
        runCatching {
            trendingSeries.value = homeContentRepository.refreshTrendingSeries(forceRefresh)
        }.also {
            trendingLoading.value = false
        }.getOrThrow()
    }

    fun prefetchTrendingPreviews(items: List<ContinueItem>) {
        items.forEach(::prepareTrendingPreview)
    }

    fun prepareTrendingPreview(item: ContinueItem) {
        val key = item.trendingPreviewKey() ?: return
        if (isTrendingPreviewPrepared(item.id)) return
        if (trendingPreviewPrepareJobs[key.jobKey]?.isActive == true) return
        trendingPreviewPrepareJobs[key.jobKey] = viewModelScope.launch {
            trendingPreviewPrepareSemaphore.withPermit {
                val prepared = homeContentRepository.prepareTrendingPreview(
                    contentType = key.contentType,
                    contentId = key.contentId,
                    fallbackPosterUrl = item.imageUrl,
                ) ?: return@withPermit
                applyPreparedTrendingPreview(prepared)
            }
        }
    }

    private fun isTrendingPreviewPrepared(itemId: String): Boolean =
        trendingMovies.value.any { it.id == itemId && it.previewPrepared } ||
            trendingSeries.value.any { it.id == itemId && it.previewPrepared }

    private fun applyPreparedTrendingPreview(prepared: HomeTrendingPreparedPreview) {
        val itemId = "${prepared.contentType}:${prepared.contentId}"
        when (prepared.contentType) {
            TrendingMovieType -> trendingMovies.update { items ->
                items.map { item -> if (item.id == itemId) prepared.applyTo(item) else item }
            }
            TrendingSeriesType -> trendingSeries.update { items ->
                items.map { item -> if (item.id == itemId) prepared.applyTo(item) else item }
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
    val title = progress.title?.cleanHistoryTitle() ?: when (progress.contentType) {
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
        remaining = if (duration > position) (duration - position).formatRemaining() else "Direct",
        progress = ratio.coerceIn(0f, 1f),
        visualStyle = visualStyle,
        imageUrl = imageUrl,
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
    val request = runCatching {
        catalogRepository.buildPlaybackRequest(previewKind, progress.contentId)
    }.getOrNull()
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
    return if (hours > 0L) "${hours}h ${remainingMinutes}min" else "${minutes}min"
}

private fun String.cleanHistoryTitle(): String =
    replace(Regex("\\s+"), " ")
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" 4K", "", ignoreCase = true)
        .trim()
