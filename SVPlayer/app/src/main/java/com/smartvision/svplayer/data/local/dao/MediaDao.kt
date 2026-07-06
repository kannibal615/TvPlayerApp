package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartvision.svplayer.data.local.entity.EpisodeEntity
import com.smartvision.svplayer.data.local.entity.HomeTrendingPreviewCacheEntity
import com.smartvision.svplayer.data.local.entity.LiveStreamEntity
import com.smartvision.svplayer.data.local.entity.MovieEntity
import com.smartvision.svplayer.data.local.entity.SeriesEntity
import com.smartvision.svplayer.data.local.entity.TrendingMediaEntity
import kotlinx.coroutines.flow.Flow

data class CategoryItemCount(
    val categoryId: String?,
    val count: Int,
)

@Dao
interface MediaDao {
    @Query("SELECT * FROM live_streams ORDER BY number, name")
    fun observeLiveStreams(): Flow<List<LiveStreamEntity>>

    @Query("SELECT COUNT(*) FROM live_streams")
    fun observeLiveStreamCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM live_streams")
    suspend fun countLiveStreams(): Int

    @Query("SELECT categoryId, COUNT(*) AS count FROM live_streams GROUP BY categoryId")
    fun observeLiveStreamCountsByCategory(): Flow<List<CategoryItemCount>>

    @Query("SELECT * FROM live_streams WHERE (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY number, name")
    fun observeLiveStreamsByCategory(categoryId: String?): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_streams ORDER BY number, name LIMIT :limit OFFSET :offset")
    suspend fun getLiveStreamsPage(limit: Int, offset: Int): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE categoryId = :categoryId ORDER BY number, name LIMIT :limit OFFSET :offset")
    suspend fun getLiveStreamsByCategoryPage(categoryId: String, limit: Int, offset: Int): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE name LIKE :pattern ESCAPE '\\' ORDER BY number, name LIMIT :limit OFFSET :offset")
    suspend fun searchLiveStreamsPage(pattern: String, limit: Int, offset: Int): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE categoryId = :categoryId AND name LIKE :pattern ESCAPE '\\' ORDER BY number, name LIMIT :limit OFFSET :offset")
    suspend fun searchLiveStreamsByCategoryPage(categoryId: String, pattern: String, limit: Int, offset: Int): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE streamId IN (:streamIds)")
    suspend fun getLiveStreamsByIds(streamIds: List<Int>): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE streamId = :streamId")
    suspend fun getLiveStream(streamId: Int): LiveStreamEntity?

    @Query(
        "SELECT * FROM live_streams " +
            "WHERE ((:categoryId IS NULL AND categoryId IS NULL) OR categoryId = :categoryId) " +
            "AND (number < :number OR (number = :number AND name < :name) OR (number = :number AND name = :name AND streamId < :streamId)) " +
            "ORDER BY number DESC, name DESC, streamId DESC LIMIT 1",
    )
    suspend fun getPreviousLiveStream(categoryId: String?, number: Int, name: String, streamId: Int): LiveStreamEntity?

    @Query(
        "SELECT * FROM live_streams " +
            "WHERE ((:categoryId IS NULL AND categoryId IS NULL) OR categoryId = :categoryId) " +
            "AND (number > :number OR (number = :number AND name > :name) OR (number = :number AND name = :name AND streamId > :streamId)) " +
            "ORDER BY number ASC, name ASC, streamId ASC LIMIT 1",
    )
    suspend fun getNextLiveStream(categoryId: String?, number: Int, name: String, streamId: Int): LiveStreamEntity?

    @Query("DELETE FROM live_streams")
    suspend fun clearLiveStreams()

    @Upsert
    suspend fun upsertLiveStreams(streams: List<LiveStreamEntity>)

    @Query("SELECT * FROM movies ORDER BY number, title")
    fun observeMovies(): Flow<List<MovieEntity>>

    @Query("SELECT COUNT(*) FROM movies")
    fun observeMovieCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM movies")
    suspend fun countMovies(): Int

    @Query("SELECT categoryId, COUNT(*) AS count FROM movies GROUP BY categoryId")
    fun observeMovieCountsByCategory(): Flow<List<CategoryItemCount>>

    @Query("SELECT * FROM movies WHERE (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY number, title")
    fun observeMoviesByCategory(categoryId: String?): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies ORDER BY number, title LIMIT :limit OFFSET :offset")
    suspend fun getMoviesPage(limit: Int, offset: Int): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE categoryId = :categoryId ORDER BY number, title LIMIT :limit OFFSET :offset")
    suspend fun getMoviesByCategoryPage(categoryId: String, limit: Int, offset: Int): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE streamId IN (:streamIds)")
    suspend fun getMoviesByIds(streamIds: List<Int>): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE streamId = :streamId")
    suspend fun getMovie(streamId: Int): MovieEntity?

    @Query(
        "SELECT * FROM movies ORDER BY RANDOM() LIMIT :limit",
    )
    suspend fun getTrendingMovies(limit: Int): List<MovieEntity>

    @Query(
        "SELECT * FROM movies " +
            "WHERE CAST(REPLACE(COALESCE(rating, '0'), ',', '.') AS REAL) >= 9.0 " +
            "ORDER BY CAST(REPLACE(COALESCE(rating, '0'), ',', '.') AS REAL) DESC, RANDOM() " +
            "LIMIT :limit",
    )
    suspend fun getBestRatedMovies(limit: Int): List<MovieEntity>

    @Query("DELETE FROM movies")
    suspend fun clearMovies()

    @Upsert
    suspend fun upsertMovies(movies: List<MovieEntity>)

    @Query("SELECT * FROM series ORDER BY number, title")
    fun observeSeries(): Flow<List<SeriesEntity>>

    @Query("SELECT COUNT(*) FROM series")
    fun observeSeriesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM series")
    suspend fun countSeries(): Int

    @Query("SELECT categoryId, COUNT(*) AS count FROM series GROUP BY categoryId")
    fun observeSeriesCountsByCategory(): Flow<List<CategoryItemCount>>

    @Query("SELECT * FROM series WHERE (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY number, title")
    fun observeSeriesByCategory(categoryId: String?): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series ORDER BY number, title LIMIT :limit OFFSET :offset")
    suspend fun getSeriesPage(limit: Int, offset: Int): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE categoryId = :categoryId ORDER BY number, title LIMIT :limit OFFSET :offset")
    suspend fun getSeriesByCategoryPage(categoryId: String, limit: Int, offset: Int): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE seriesId IN (:seriesIds)")
    suspend fun getSeriesByIds(seriesIds: List<Int>): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE seriesId = :seriesId")
    suspend fun getSeries(seriesId: Int): SeriesEntity?

    @Query(
        "SELECT * FROM series ORDER BY RANDOM() LIMIT :limit",
    )
    suspend fun getTrendingSeries(limit: Int): List<SeriesEntity>

    @Query(
        "SELECT * FROM series " +
            "WHERE CAST(REPLACE(COALESCE(rating, '0'), ',', '.') AS REAL) >= 9.0 " +
            "ORDER BY CAST(REPLACE(COALESCE(rating, '0'), ',', '.') AS REAL) DESC, RANDOM() " +
            "LIMIT :limit",
    )
    suspend fun getBestRatedSeries(limit: Int): List<SeriesEntity>

    @Query("SELECT * FROM trending_media WHERE contentType = :contentType")
    suspend fun getTrendingMedia(contentType: String): List<TrendingMediaEntity>

    @Query("SELECT contentId FROM trending_media WHERE contentType = :contentType")
    suspend fun getTrendingContentIds(contentType: String): List<Int>

    @Query("DELETE FROM trending_media WHERE contentType = :contentType")
    suspend fun clearTrendingMedia(contentType: String)

    @Upsert
    suspend fun upsertTrendingMedia(items: List<TrendingMediaEntity>)

    @Query(
        "SELECT * FROM home_trending_preview_cache " +
            "WHERE contentType = :contentType AND contentId = :contentId AND lastSync = :lastSync",
    )
    suspend fun getHomeTrendingPreviewCache(
        contentType: String,
        contentId: Int,
        lastSync: Long,
    ): HomeTrendingPreviewCacheEntity?

    @Upsert
    suspend fun upsertHomeTrendingPreviewCache(item: HomeTrendingPreviewCacheEntity)

    @Query("DELETE FROM home_trending_preview_cache WHERE lastSync != :lastSync")
    suspend fun deleteStaleHomeTrendingPreviewCache(lastSync: Long)

    @Query("DELETE FROM series")
    suspend fun clearSeries()

    @Upsert
    suspend fun upsertSeries(series: List<SeriesEntity>)

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY seasonNumber, episodeNumber")
    suspend fun getEpisodes(seriesId: Int): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE episodeId = :episodeId")
    suspend fun getEpisode(episodeId: Int): EpisodeEntity?

    @Query("DELETE FROM episodes WHERE seriesId = :seriesId")
    suspend fun deleteEpisodes(seriesId: Int)

    @Upsert
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)
}
