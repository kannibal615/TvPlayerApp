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
import com.smartvision.svplayer.domain.model.TrendingCatalogItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class LocalCatalogSnapshot<T>(
    val categories: List<Category>,
    val items: List<T>,
    val fromCache: Boolean = false,
)

data class CatalogContentCounts(
    val live: Int = 0,
    val movies: Int = 0,
    val series: Int = 0,
) {
    val hasAnyContent: Boolean
        get() = live > 0 || movies > 0 || series > 0
}

interface CatalogRepository {
    val syncStatus: StateFlow<SyncStatus>

    fun observeLiveCategories(): Flow<List<Category>>
    fun observeLiveChannels(categoryId: String?): Flow<List<LiveChannel>>
    fun observeMovieCategories(): Flow<List<Category>>
    fun observeMovies(categoryId: String?): Flow<List<Movie>>
    fun observeSeriesCategories(): Flow<List<Category>>
    fun observeSeries(categoryId: String?): Flow<List<TvSeries>>
    fun observeAccount(): Flow<AccountProfile>
    fun getCachedLiveCatalogSnapshot(): LocalCatalogSnapshot<LiveChannel>?
    fun getCachedMovieCatalogSnapshot(): LocalCatalogSnapshot<Movie>?
    fun getCachedSeriesCatalogSnapshot(): LocalCatalogSnapshot<TvSeries>?
    fun getCachedLiveCategories(): List<Category>?
    fun getCachedMovieCategories(): List<Category>?
    fun getCachedSeriesCategories(): List<Category>?

    suspend fun getLiveCategoriesSnapshot(): List<Category>
    suspend fun getMovieCategoriesSnapshot(): List<Category>
    suspend fun getSeriesCategoriesSnapshot(): List<Category>
    suspend fun getInitialLiveCategoriesSnapshot(limit: Int): List<Category>
    suspend fun getInitialMovieCategoriesSnapshot(limit: Int): List<Category>
    suspend fun getInitialSeriesCategoriesSnapshot(limit: Int): List<Category>
    suspend fun getLiveCatalogSnapshot(): LocalCatalogSnapshot<LiveChannel>
    suspend fun getMovieCatalogSnapshot(): LocalCatalogSnapshot<Movie>
    suspend fun getSeriesCatalogSnapshot(): LocalCatalogSnapshot<TvSeries>
    suspend fun getLiveChannelsPage(categoryId: String?, offset: Int, limit: Int): List<LiveChannel>
    suspend fun getMoviesPage(categoryId: String?, offset: Int, limit: Int): List<Movie>
    suspend fun getSeriesPage(categoryId: String?, offset: Int, limit: Int): List<TvSeries>
    suspend fun getAllLiveChannelsPage(offset: Int, limit: Int): List<LiveChannel>
    suspend fun getAllMoviesPage(offset: Int, limit: Int): List<Movie>
    suspend fun getAllSeriesPage(offset: Int, limit: Int): List<TvSeries>
    suspend fun getLiveChannelsByIds(streamIds: List<Int>): List<LiveChannel>
    suspend fun getMoviesByIds(streamIds: List<Int>): List<Movie>
    suspend fun getSeriesByIds(seriesIds: List<Int>): List<TvSeries>
    suspend fun getTrendingMovies(limit: Int): List<Movie>
    suspend fun getTrendingSeries(limit: Int): List<TvSeries>
    suspend fun getTrendingMovieItems(limit: Int): List<TrendingCatalogItem>
    suspend fun getTrendingSeriesItems(limit: Int): List<TrendingCatalogItem>
    suspend fun getCatalogContentCounts(): CatalogContentCounts
    fun invalidateLocalCatalogCache()
    suspend fun synchronize(): Result<Unit>
    suspend fun toggleFavorite(contentType: String, contentId: String)
    suspend fun getSeriesEpisodes(seriesId: Int): List<Episode>
    suspend fun buildPlaybackRequest(kind: PlaybackKind, id: String): PlaybackRequest?
    suspend fun savePlaybackProgress(kind: PlaybackKind, id: String, positionMs: Long, durationMs: Long)
}
