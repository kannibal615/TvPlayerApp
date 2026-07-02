package com.smartvision.svplayer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.home.HomeSlide
import com.smartvision.svplayer.data.home.HomeSlidesRepository
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.mock.HomeVisualStyle
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.UserContentType
import com.smartvision.svplayer.domain.model.TrendingCatalogItem
import com.smartvision.svplayer.domain.repository.CatalogRepository
import kotlinx.coroutines.async
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
    private val homeSlidesRepository: HomeSlidesRepository,
) : ViewModel() {
    private val cachedContinueWatching = userContentRepository
        .getCachedRecentProgressSnapshot(limit = ContinueWatchingSnapshotLimit)
        ?.toContinueItems()
        .orEmpty()
    private val trendingMovies = MutableStateFlow<List<ContinueItem>>(emptyList())
    private val trendingSeries = MutableStateFlow<List<ContinueItem>>(emptyList())
    private val slides = MutableStateFlow(homeSlidesRepository.getCachedSlides().orEmpty())
    private val continueWatching = userContentRepository.observeRecentProgress(limit = ContinueWatchingSnapshotLimit)
        .map { progress ->
            progress
                .filter { it.positionMs > 5_000L }
                .map { userContentRepository.enrichProgress(it) }
                .distinctBy(::historyGroupingKey)
                .take(10)
                .mapNotNull(::toContinueItem)
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
        initialValue = HomeUiState(),
    )

    init {
        refreshTrending()
        refreshSlides()
    }

    fun refreshSlides(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val cached = homeSlidesRepository.getCachedSlides()
            if (!forceRefresh && !cached.isNullOrEmpty()) {
                slides.value = cached
                return@launch
            }
            runCatching { homeSlidesRepository.refresh() }
                .onSuccess { refreshed -> slides.value = refreshed }
        }
    }

    fun refreshTrending(forceRefresh: Boolean = false) {
        if (!forceRefresh && trendingMovies.value.isNotEmpty() && trendingSeries.value.isNotEmpty()) return
        viewModelScope.launch {
            val movies = async {
                runCatching { catalogRepository.getTrendingMovieItems(HomeTrendingSectionLimit) }
                    .getOrDefault(emptyList())
                    .map { it.toMovieTrendItem() }
            }
            val series = async {
                runCatching { catalogRepository.getTrendingSeriesItems(HomeTrendingSectionLimit) }
                    .getOrDefault(emptyList())
                    .map { it.toSeriesTrendItem() }
            }
            trendingMovies.value = movies.await()
            trendingSeries.value = series.await()
        }
    }
}

private const val ContinueWatchingSnapshotLimit = 10
private const val HomeTrendingSectionLimit = 10

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

private fun List<PlaybackProgressEntity>.toContinueItems(): List<ContinueItem> =
    filter { it.positionMs > 5_000L }
        .distinctBy(::historyGroupingKey)
        .take(10)
        .mapNotNull(::toContinueItem)

private fun TrendingCatalogItem.toMovieTrendItem(): ContinueItem =
    ContinueItem(
        id = "movie:$contentId",
        title = title.cleanHistoryTitle(),
        meta = "Film${rating?.let { "  |  $it/10" }.orEmpty()}",
        remaining = "Tendance",
        progress = 0f,
        visualStyle = HomeVisualStyle.Cinema,
        imageUrl = posterUrl,
        mediaType = "FILM",
        previewUrl = previewUrl,
    )

private fun TrendingCatalogItem.toSeriesTrendItem(): ContinueItem =
    ContinueItem(
        id = "series:$contentId",
        title = title.cleanHistoryTitle(),
        meta = "Serie${rating?.let { "  |  $it/10" }.orEmpty()}",
        remaining = year?.take(4) ?: "Tendance",
        progress = 0f,
        visualStyle = HomeVisualStyle.Series,
        imageUrl = posterUrl,
        mediaType = "SERIE",
        previewUrl = previewUrl,
    )

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
