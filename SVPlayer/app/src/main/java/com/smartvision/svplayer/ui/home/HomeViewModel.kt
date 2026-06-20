package com.smartvision.svplayer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.mock.HomeVisualStyle
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.UserContentType
import com.smartvision.svplayer.data.repository.XtreamRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val continueWatching: List<ContinueItem> = emptyList(),
)

class HomeViewModel(
    userContentRepository: UserContentRepository,
    private val xtreamRepository: XtreamRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = userContentRepository.observeRecentProgress(limit = 8)
        .map { progress ->
            HomeUiState(
                continueWatching = progress.mapNotNull(::toContinueItem),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

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
        val title = when (progress.contentType) {
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
        val imageUrl = when (progress.contentType) {
            UserContentType.Movie -> xtreamRepository.getCachedMovie(id)?.posterUrl
            UserContentType.Episode -> {
                val episode = xtreamRepository.getCachedEpisode(id)
                episode?.seriesId?.let { xtreamRepository.getCachedSeries(it)?.coverUrl }
            }
            else -> null
        }?.takeIf { it.isNotBlank() }
        val meta = when (progress.contentType) {
            UserContentType.Live -> "Live TV"
            UserContentType.Movie -> "Film"
            UserContentType.Episode -> xtreamRepository.getCachedEpisode(id)?.let {
                "S${it.seasonNumber} E${it.episodeNumber}"
            } ?: "Serie"
            else -> "Media"
        }
        val ratio = if (duration > 0L) {
            position.toFloat() / duration.toFloat()
        } else {
            0f
        }
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

private fun Long.formatRemaining(): String {
    val minutes = (this / 60_000L).coerceAtLeast(1L)
    val hours = minutes / 60L
    val remainingMinutes = minutes % 60L
    return if (hours > 0L) {
        "${hours}h ${remainingMinutes}min"
    } else {
        "${minutes}min"
    }
}

private fun String.cleanHistoryTitle(): String =
    replace(Regex("\\s+"), " ")
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" 4K", "", ignoreCase = true)
        .trim()
