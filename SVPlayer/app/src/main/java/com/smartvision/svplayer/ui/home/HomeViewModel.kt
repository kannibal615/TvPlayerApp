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
import com.smartvision.svplayer.domain.model.Movie
import com.smartvision.svplayer.domain.model.TvSeries
import com.smartvision.svplayer.domain.repository.CatalogRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val continueWatching: List<ContinueItem> = emptyList(),
    val trending: List<ContinueItem> = emptyList(),
    val slides: List<HomeSlide> = emptyList(),
)

class HomeViewModel(
    private val userContentRepository: UserContentRepository,
    private val catalogRepository: CatalogRepository,
    private val homeSlidesRepository: HomeSlidesRepository,
) : ViewModel() {
    private val trending = MutableStateFlow<List<ContinueItem>>(emptyList())
    private val slides = MutableStateFlow<List<HomeSlide>>(emptyList())
    private val continueWatching = userContentRepository.observeRecentProgress(limit = 60)
        .map { progress ->
            progress
                .filter { it.positionMs > 5_000L }
                .map { userContentRepository.enrichProgress(it) }
                .distinctBy(::historyGroupingKey)
                .take(20)
                .mapNotNull(::toContinueItem)
        }

    val uiState: StateFlow<HomeUiState> = combine(
        continueWatching,
        trending,
        slides,
    ) { continueItems, trendItems, homeSlides ->
        HomeUiState(
            continueWatching = continueItems,
            trending = trendItems,
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

    fun refreshSlides() {
        viewModelScope.launch {
            runCatching { homeSlidesRepository.refresh() }
                .onSuccess { refreshed -> slides.value = refreshed }
        }
    }

    fun refreshTrending() {
        viewModelScope.launch {
            val movies = async { runCatching { loadTrendingMovies() }.getOrDefault(emptyList()) }
            val series = async { runCatching { loadTrendingSeries() }.getOrDefault(emptyList()) }
            trending.value = (movies.await() + series.await())
                .shuffled()
                .take(10)
                .map { it.item }
        }
    }

    private suspend fun loadTrendingMovies(): List<ScoredTrend> {
        return catalogRepository.observeMovies(null).first()
            .distinctBy { it.streamId }
            .shuffled()
            .take(12)
            .map { movie ->
                ScoredTrend(
                    score = movieTrendScore(movie),
                    item = ContinueItem(
                        id = "movie:${movie.streamId}",
                        title = movie.title.cleanHistoryTitle(),
                        meta = "Film${movie.rating?.let { "  |  $it/10" }.orEmpty()}",
                        remaining = "Tendance",
                        progress = 0f,
                        visualStyle = HomeVisualStyle.Cinema,
                        imageUrl = movie.posterUrl,
                        mediaType = "FILM",
                    ),
                )
            }
    }

    private suspend fun loadTrendingSeries(): List<ScoredTrend> {
        return catalogRepository.observeSeries(null).first()
            .distinctBy { it.seriesId }
            .shuffled()
            .take(12)
            .map { series ->
                ScoredTrend(
                    score = seriesTrendScore(series),
                    item = ContinueItem(
                        id = "series:${series.seriesId}",
                        title = series.title.cleanHistoryTitle(),
                        meta = "Serie${series.rating?.let { "  |  $it/10" }.orEmpty()}",
                        remaining = series.year?.take(4) ?: "Tendance",
                        progress = 0f,
                        visualStyle = HomeVisualStyle.Series,
                        imageUrl = series.posterUrl,
                        mediaType = "SERIE",
                    ),
                )
            }
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
}

private fun historyGroupingKey(progress: PlaybackProgressEntity): String =
    if (progress.contentType == UserContentType.Episode) {
        "series:${progress.parentContentId ?: progress.title.orEmpty().lowercase()}"
    } else {
        "${progress.contentType}:${progress.contentId}"
    }

private data class ScoredTrend(val score: Float, val item: ContinueItem)

private fun String.trendCategoryScore(): Int {
    val value = lowercase()
    return when {
        listOf("top", "tendance", "nouveau", "new", "2026", "2025").any(value::contains) -> 3
        listOf("netflix", "prime", "cinema", "film", "serie").any(value::contains) -> 2
        else -> 1
    }
}

private fun movieTrendScore(movie: Movie): Float =
    movie.rating.toRating() * 10f + movie.categoryName.trendCategoryScore()

private fun seriesTrendScore(series: TvSeries): Float {
    val year = series.year?.take(4)?.toIntOrNull() ?: 0
    return series.rating.toRating() * 10f + series.categoryName.trendCategoryScore() + (year - 2000).coerceAtLeast(0) / 100f
}

private fun String?.toRating(): Float =
    this?.replace(',', '.')?.toFloatOrNull()?.coerceIn(0f, 10f) ?: 0f

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
