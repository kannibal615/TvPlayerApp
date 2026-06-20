package com.smartvision.svplayer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.mock.HomeVisualStyle
import com.smartvision.svplayer.data.models.XtreamMovieStream
import com.smartvision.svplayer.data.models.XtreamSeriesStream
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.UserContentType
import com.smartvision.svplayer.data.repository.XtreamRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val continueWatching: List<ContinueItem> = emptyList(),
    val trending: List<ContinueItem> = emptyList(),
)

class HomeViewModel(
    private val userContentRepository: UserContentRepository,
    private val xtreamRepository: XtreamRepository,
) : ViewModel() {
    private val trending = MutableStateFlow<List<ContinueItem>>(emptyList())
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
    ) { continueItems, trendItems ->
        HomeUiState(
            continueWatching = continueItems,
            trending = trendItems,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    init {
        loadTrending()
    }

    private fun loadTrending() {
        viewModelScope.launch {
            val movies = async { runCatching { loadTrendingMovies() }.getOrDefault(emptyList()) }
            val series = async { runCatching { loadTrendingSeries() }.getOrDefault(emptyList()) }
            trending.value = (movies.await() + series.await())
                .sortedByDescending { it.score }
                .take(10)
                .map { it.item }
        }
    }

    private suspend fun loadTrendingMovies(): List<ScoredTrend> {
        val categories = xtreamRepository.getMovieCategories()
        return categories.movieTrendingCandidates().flatMap { category ->
            xtreamRepository.getMovies(category.id).take(60)
        }.distinctBy { it.streamId }
            .sortedByDescending(::movieTrendScore)
            .take(6)
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
                    ),
                )
            }
    }

    private suspend fun loadTrendingSeries(): List<ScoredTrend> {
        val categories = xtreamRepository.getSeriesCategories()
        return categories.seriesTrendingCandidates().flatMap { category ->
            xtreamRepository.getSeries(category.id).take(60)
        }.distinctBy { it.seriesId }
            .sortedByDescending(::seriesTrendScore)
            .take(6)
            .map { series ->
                ScoredTrend(
                    score = seriesTrendScore(series),
                    item = ContinueItem(
                        id = "series:${series.seriesId}",
                        title = series.title.cleanHistoryTitle(),
                        meta = "Serie${series.rating?.let { "  |  $it/10" }.orEmpty()}",
                        remaining = series.releaseDate?.take(4) ?: "Tendance",
                        progress = 0f,
                        visualStyle = HomeVisualStyle.Series,
                        imageUrl = series.coverUrl,
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
            UserContentType.Live -> xtreamRepository.getCachedLiveStream(id)?.name?.cleanHistoryTitle()
                ?: "Chaine $id"
            UserContentType.Movie -> xtreamRepository.getCachedMovie(id)?.title?.cleanHistoryTitle()
                ?: "Film $id"
            UserContentType.Episode -> {
                val episode = xtreamRepository.getCachedEpisode(id)
                val seriesTitle = episode?.seriesId?.let { xtreamRepository.getCachedSeries(it)?.title }
                seriesTitle?.cleanHistoryTitle()
                    ?: episode?.title?.cleanHistoryTitle()
                    ?: "Episode $id"
            }
            else -> return null
        }
        val imageUrl = progress.imageUrl?.takeIf { it.isNotBlank() } ?: when (progress.contentType) {
            UserContentType.Live -> xtreamRepository.getCachedLiveStream(id)?.streamIcon
            UserContentType.Movie -> xtreamRepository.getCachedMovie(id)?.posterUrl
            UserContentType.Episode -> {
                val seriesId = progress.parentContentId?.toIntOrNull()
                    ?: xtreamRepository.getCachedEpisode(id)?.seriesId
                seriesId?.let { xtreamRepository.getCachedSeries(it)?.coverUrl }
            }
            else -> null
        }?.takeIf { it.isNotBlank() }
        val meta = progress.subtitle?.take(36) ?: when (progress.contentType) {
            UserContentType.Live -> "Live TV"
            UserContentType.Movie -> "Film"
            UserContentType.Episode -> xtreamRepository.getCachedEpisode(id)?.let {
                "S${it.seasonNumber} E${it.episodeNumber}"
            } ?: "Serie"
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

private fun List<com.smartvision.svplayer.data.models.XtreamMovieCategory>.movieTrendingCandidates() =
    sortedByDescending { it.name.trendCategoryScore() }.take(2)

private fun List<com.smartvision.svplayer.data.models.XtreamSeriesCategory>.seriesTrendingCandidates() =
    sortedByDescending { it.name.trendCategoryScore() }.take(2)

private fun String.trendCategoryScore(): Int {
    val value = lowercase()
    return when {
        listOf("top", "tendance", "nouveau", "new", "2026", "2025").any(value::contains) -> 3
        listOf("netflix", "prime", "cinema", "film", "serie").any(value::contains) -> 2
        else -> 1
    }
}

private fun movieTrendScore(movie: XtreamMovieStream): Float =
    movie.rating.toRating() * 10f + (movie.added?.toLongOrNull()?.rem(10_000_000L) ?: 0L) / 10_000_000f

private fun seriesTrendScore(series: XtreamSeriesStream): Float {
    val year = series.releaseDate?.take(4)?.toIntOrNull() ?: 0
    return series.rating.toRating() * 10f + (year - 2000).coerceAtLeast(0) / 100f
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
