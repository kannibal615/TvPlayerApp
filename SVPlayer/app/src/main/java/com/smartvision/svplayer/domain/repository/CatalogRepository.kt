package com.smartvision.svplayer.domain.repository

import com.smartvision.svplayer.domain.model.AccountProfile
import com.smartvision.svplayer.domain.model.Category
import com.smartvision.svplayer.domain.model.Episode
import com.smartvision.svplayer.domain.model.LiveChannel
import com.smartvision.svplayer.domain.model.Movie
import com.smartvision.svplayer.domain.model.PlaybackKind
import com.smartvision.svplayer.domain.model.PlaybackRequest
import com.smartvision.svplayer.domain.model.SyncStatus
import com.smartvision.svplayer.domain.model.TvSeries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CatalogRepository {
    val syncStatus: StateFlow<SyncStatus>

    fun observeLiveCategories(): Flow<List<Category>>
    fun observeLiveChannels(categoryId: String?): Flow<List<LiveChannel>>
    fun observeMovieCategories(): Flow<List<Category>>
    fun observeMovies(categoryId: String?): Flow<List<Movie>>
    fun observeSeriesCategories(): Flow<List<Category>>
    fun observeSeries(categoryId: String?): Flow<List<TvSeries>>
    fun observeAccount(): Flow<AccountProfile>

    suspend fun synchronize(): Result<Unit>
    suspend fun toggleFavorite(contentType: String, contentId: String)
    suspend fun getSeriesEpisodes(seriesId: Int): List<Episode>
    suspend fun buildPlaybackRequest(kind: PlaybackKind, id: String): PlaybackRequest?
    suspend fun savePlaybackProgress(kind: PlaybackKind, id: String, positionMs: Long, durationMs: Long)
}
