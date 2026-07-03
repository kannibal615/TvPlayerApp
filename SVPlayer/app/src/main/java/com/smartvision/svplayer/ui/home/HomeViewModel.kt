package com.smartvision.svplayer.ui.home

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.diagnostics.PerformanceDiagnosticRecorder
import com.smartvision.svplayer.data.home.HomeContentRepository
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.home.HomeSlide
import com.smartvision.svplayer.data.home.HomeSlidesRepository
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.mock.HomePreviewMode
import com.smartvision.svplayer.data.mock.HomeVisualStyle
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.UserContentType
import com.smartvision.svplayer.data.repository.XtreamRepository
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
import kotlinx.coroutines.launch

data class HomeUiState(
    val continueWatching: List<ContinueItem> = emptyList(),
    val trendingMovies: List<ContinueItem> = emptyList(),
    val trendingSeries: List<ContinueItem> = emptyList(),
    val slides: List<HomeSlide> = emptyList(),
)

class HomeViewModel(
    private val userContentRepository: UserContentRepository,
    private val catalogRepository: CatalogRepository,
    private val xtreamRepository: XtreamRepository,
    private val homeSlidesRepository: HomeSlidesRepository,
    private val homeContentRepository: HomeContentRepository,
) : ViewModel() {
    private val cachedContinueWatching = userContentRepository
        .getCachedRecentProgressSnapshot(limit = ContinueWatchingSnapshotLimit)
        ?.toContinueItems()
        .orEmpty()
    private val cachedTrending = homeContentRepository.getLastCachedTrendingSnapshot()
    private val trendingMovies = MutableStateFlow(cachedTrending?.movies.orEmpty())
    private val trendingSeries = MutableStateFlow(cachedTrending?.series.orEmpty())
    private val slides = MutableStateFlow(homeSlidesRepository.getCachedSlides().orEmpty())
    private var trendingRefreshJob: Job? = null
    private val continueWatching = userContentRepository.observeRecentProgress(limit = ContinueWatchingSnapshotLimit)
        .map { progress ->
            // PERF_DIAG: measures why Continue watching can appear after Home is already visible.
            val startedAt = SystemClock.elapsedRealtime()
            val recent = progress
                .filter { it.positionMs > 5_000L }
                .map { userContentRepository.enrichProgress(it) }
                .distinctBy(::historyGroupingKey)
                .take(10)
            val items = recent.mapNotNull { item ->
                toContinueItemWithPreview(item, catalogRepository, xtreamRepository)
            }
            PerformanceDiagnosticRecorder.recordDuration(
                sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
                event = "continue_watching_flow_mapped",
                startedAtMs = startedAt,
                fields = mapOf(
                    "rawItems" to progress.size,
                    "recentItems" to recent.size,
                    "mappedItems" to items.size,
                    "previewReadyItems" to items.count { !it.previewUrl.isNullOrBlank() },
                ),
            )
            items
        }
        .onStart {
            if (cachedContinueWatching.isNotEmpty()) {
                emit(cachedContinueWatching)
            }
        }

    val uiState: StateFlow<HomeUiState> = combine(
        continueWatching,
        trendingMovies,
        trendingSeries,
        slides,
    ) { continueItems, trendMovieItems, trendSeriesItems, homeSlides ->
        HomeUiState(
            continueWatching = continueItems,
            trendingMovies = trendMovieItems,
            trendingSeries = trendSeriesItems,
            slides = homeSlides,
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
        refreshTrending()
        refreshSlides()
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
            if (cached != null) {
                trendingMovies.value = cached.movies
                trendingSeries.value = cached.series
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
                ?: return@launch
            trendingMovies.value = snapshot.movies
            trendingSeries.value = snapshot.series
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
}

private const val ContinueWatchingSnapshotLimit = 10

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
    xtreamRepository: XtreamRepository,
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
    val previewImageUrl = when (progress.contentType) {
        UserContentType.Movie -> progress.contentId.toIntOrNull()?.let { movieId ->
            runCatching { xtreamRepository.getMovieDetails(movieId).backdropUrl }.getOrNull()
        }
        UserContentType.Episode -> progress.parentContentId?.toIntOrNull()?.let { seriesId ->
            runCatching { xtreamRepository.getSeriesDetails(seriesId).backdropUrl }.getOrNull()
        }
        else -> null
    }?.takeIf { it.isNotBlank() }
    return base.copy(
        previewUrl = url,
        previewImageUrl = previewImageUrl ?: base.previewImageUrl,
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
