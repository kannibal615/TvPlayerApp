package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartvision.svplayer.data.local.entity.EpisodeEntity
import com.smartvision.svplayer.data.local.entity.HomeTrendingPreviewCacheEntity
import com.smartvision.svplayer.data.local.entity.LiveStreamEntity
import com.smartvision.svplayer.data.local.entity.MovieEntity
import com.smartvision.svplayer.data.local.entity.SeriesEntity
import com.smartvision.svplayer.data.local.entity.TmdbContentMappingEntity
import com.smartvision.svplayer.data.local.entity.TmdbMovieMetadataEntity
import com.smartvision.svplayer.data.local.entity.TmdbSeriesMetadataEntity
import com.smartvision.svplayer.data.local.entity.TrendingMediaEntity
import kotlinx.coroutines.flow.Flow

data class CategoryItemCount(
    val categoryId: String?,
    val count: Int,
)

@Dao
interface MediaDao {
    @Query("DELETE FROM episodes WHERE profileId = :profileId")
    suspend fun clearEpisodesByProfile(profileId: String)

    @Query("DELETE FROM trending_media WHERE profileId = :profileId")
    suspend fun clearTrendingByProfile(profileId: String)

    @Query("DELETE FROM home_trending_preview_cache WHERE profileId = :profileId")
    suspend fun clearHomePreviewCacheByProfile(profileId: String)

    @Query("DELETE FROM tmdb_content_mapping WHERE profileId = :profileId")
    suspend fun clearTmdbMappingsByProfile(profileId: String)

    @Query("SELECT * FROM live_streams WHERE profileId = :profileId ORDER BY number, name")
    fun observeLiveStreams(profileId: String): Flow<List<LiveStreamEntity>>

    @Query("SELECT COUNT(*) FROM live_streams WHERE profileId = :profileId")
    fun observeLiveStreamCount(profileId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM live_streams WHERE profileId = :profileId")
    suspend fun countLiveStreams(profileId: String): Int

    @Query("SELECT categoryId, COUNT(*) AS count FROM live_streams WHERE profileId = :profileId GROUP BY categoryId")
    fun observeLiveStreamCountsByCategory(profileId: String): Flow<List<CategoryItemCount>>

    @Query("SELECT * FROM live_streams WHERE profileId = :profileId AND (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY number, name")
    fun observeLiveStreamsByCategory(profileId: String, categoryId: String?): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_streams WHERE profileId = :profileId ORDER BY number, name LIMIT :limit OFFSET :offset")
    suspend fun getLiveStreamsPage(profileId: String, limit: Int, offset: Int): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE profileId = :profileId AND categoryId = :categoryId ORDER BY number, name LIMIT :limit OFFSET :offset")
    suspend fun getLiveStreamsByCategoryPage(profileId: String, categoryId: String, limit: Int, offset: Int): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE profileId = :profileId AND name LIKE :pattern ESCAPE '\\' ORDER BY number, name LIMIT :limit OFFSET :offset")
    suspend fun searchLiveStreamsPage(profileId: String, pattern: String, limit: Int, offset: Int): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE profileId = :profileId AND categoryId = :categoryId AND name LIKE :pattern ESCAPE '\\' ORDER BY number, name LIMIT :limit OFFSET :offset")
    suspend fun searchLiveStreamsByCategoryPage(profileId: String, categoryId: String, pattern: String, limit: Int, offset: Int): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE profileId = :profileId AND streamId IN (:streamIds)")
    suspend fun getLiveStreamsByIds(profileId: String, streamIds: List<Int>): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE profileId = :profileId AND streamId = :streamId")
    suspend fun getLiveStream(profileId: String, streamId: Int): LiveStreamEntity?

    @Query(
        "SELECT * FROM live_streams " +
            "WHERE profileId = :profileId " +
            "AND ((:categoryId IS NULL AND categoryId IS NULL) OR categoryId = :categoryId) " +
            "AND (number < :number OR (number = :number AND name < :name) OR (number = :number AND name = :name AND streamId < :streamId)) " +
            "ORDER BY number DESC, name DESC, streamId DESC LIMIT 1",
    )
    suspend fun getPreviousLiveStream(profileId: String, categoryId: String?, number: Int, name: String, streamId: Int): LiveStreamEntity?

    @Query(
        "SELECT * FROM live_streams " +
            "WHERE profileId = :profileId " +
            "AND ((:categoryId IS NULL AND categoryId IS NULL) OR categoryId = :categoryId) " +
            "AND (number > :number OR (number = :number AND name > :name) OR (number = :number AND name = :name AND streamId > :streamId)) " +
            "ORDER BY number ASC, name ASC, streamId ASC LIMIT 1",
    )
    suspend fun getNextLiveStream(profileId: String, categoryId: String?, number: Int, name: String, streamId: Int): LiveStreamEntity?

    @Query("DELETE FROM live_streams WHERE profileId = :profileId")
    suspend fun clearLiveStreams(profileId: String)

    @Upsert
    suspend fun upsertLiveStreams(streams: List<LiveStreamEntity>)

    @Query("SELECT * FROM movies WHERE profileId = :profileId ORDER BY number, title")
    fun observeMovies(profileId: String): Flow<List<MovieEntity>>

    @Query("SELECT COUNT(*) FROM movies WHERE profileId = :profileId")
    fun observeMovieCount(profileId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM movies WHERE profileId = :profileId")
    suspend fun countMovies(profileId: String): Int

    @Query("SELECT categoryId, COUNT(*) AS count FROM movies WHERE profileId = :profileId GROUP BY categoryId")
    fun observeMovieCountsByCategory(profileId: String): Flow<List<CategoryItemCount>>

    @Query("SELECT * FROM movies WHERE profileId = :profileId AND (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY number, title")
    fun observeMoviesByCategory(profileId: String, categoryId: String?): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE profileId = :profileId ORDER BY number, title LIMIT :limit OFFSET :offset")
    suspend fun getMoviesPage(profileId: String, limit: Int, offset: Int): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE profileId = :profileId AND categoryId = :categoryId ORDER BY number, title LIMIT :limit OFFSET :offset")
    suspend fun getMoviesByCategoryPage(profileId: String, categoryId: String, limit: Int, offset: Int): List<MovieEntity>

    @Query(
        "SELECT * FROM movies WHERE profileId = :profileId " +
            "AND (title LIKE :pattern ESCAPE '\\' OR COALESCE(genre, '') LIKE :pattern ESCAPE '\\' OR COALESCE(year, '') LIKE :pattern ESCAPE '\\') " +
            "ORDER BY number, title LIMIT :limit OFFSET :offset",
    )
    suspend fun searchMoviesPage(profileId: String, pattern: String, limit: Int, offset: Int): List<MovieEntity>

    @Query(
        "SELECT * FROM movies WHERE profileId = :profileId AND categoryId = :categoryId " +
            "AND (title LIKE :pattern ESCAPE '\\' OR COALESCE(genre, '') LIKE :pattern ESCAPE '\\' OR COALESCE(year, '') LIKE :pattern ESCAPE '\\') " +
            "ORDER BY number, title LIMIT :limit OFFSET :offset",
    )
    suspend fun searchMoviesByCategoryPage(profileId: String, categoryId: String, pattern: String, limit: Int, offset: Int): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE profileId = :profileId AND streamId IN (:streamIds)")
    suspend fun getMoviesByIds(profileId: String, streamIds: List<Int>): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE profileId = :profileId AND streamId = :streamId")
    suspend fun getMovie(profileId: String, streamId: Int): MovieEntity?

    @Query(
        "SELECT * FROM movies WHERE profileId = :profileId AND categoryId IS :categoryId " +
            "AND (number < :number OR (number = :number AND title < :title) " +
            "OR (number = :number AND title = :title AND streamId < :streamId)) " +
            "ORDER BY number DESC, title DESC, streamId DESC LIMIT 1",
    )
    suspend fun getPreviousMovie(
        profileId: String,
        categoryId: String?,
        number: Int,
        title: String,
        streamId: Int,
    ): MovieEntity?

    @Query(
        "SELECT * FROM movies WHERE profileId = :profileId AND categoryId IS :categoryId " +
            "AND (number > :number OR (number = :number AND title > :title) " +
            "OR (number = :number AND title = :title AND streamId > :streamId)) " +
            "ORDER BY number, title, streamId LIMIT 1",
    )
    suspend fun getNextMovie(
        profileId: String,
        categoryId: String?,
        number: Int,
        title: String,
        streamId: Int,
    ): MovieEntity?

    @Query("SELECT * FROM movies WHERE profileId = :profileId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getTrendingMovies(profileId: String, limit: Int): List<MovieEntity>

    @Query(
        "SELECT * FROM movies " +
            "WHERE profileId = :profileId " +
            "AND CAST(REPLACE(COALESCE(rating, '0'), ',', '.') AS REAL) >= 9.0 " +
            "ORDER BY CAST(REPLACE(COALESCE(rating, '0'), ',', '.') AS REAL) DESC, RANDOM() " +
            "LIMIT :limit",
    )
    suspend fun getBestRatedMovies(profileId: String, limit: Int): List<MovieEntity>

    @Query("DELETE FROM movies WHERE profileId = :profileId")
    suspend fun clearMovies(profileId: String)

    @Upsert
    suspend fun upsertMovies(movies: List<MovieEntity>)

    @Query("SELECT * FROM series WHERE profileId = :profileId ORDER BY number, title")
    fun observeSeries(profileId: String): Flow<List<SeriesEntity>>

    @Query("SELECT COUNT(*) FROM series WHERE profileId = :profileId")
    fun observeSeriesCount(profileId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM series WHERE profileId = :profileId")
    suspend fun countSeries(profileId: String): Int

    @Query("SELECT categoryId, COUNT(*) AS count FROM series WHERE profileId = :profileId GROUP BY categoryId")
    fun observeSeriesCountsByCategory(profileId: String): Flow<List<CategoryItemCount>>

    @Query("SELECT * FROM series WHERE profileId = :profileId AND (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY number, title")
    fun observeSeriesByCategory(profileId: String, categoryId: String?): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE profileId = :profileId ORDER BY number, title LIMIT :limit OFFSET :offset")
    suspend fun getSeriesPage(profileId: String, limit: Int, offset: Int): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE profileId = :profileId AND categoryId = :categoryId ORDER BY number, title LIMIT :limit OFFSET :offset")
    suspend fun getSeriesByCategoryPage(profileId: String, categoryId: String, limit: Int, offset: Int): List<SeriesEntity>

    @Query(
        "SELECT * FROM series WHERE profileId = :profileId " +
            "AND (title LIKE :pattern ESCAPE '\\' OR COALESCE(genre, '') LIKE :pattern ESCAPE '\\' OR COALESCE(year, '') LIKE :pattern ESCAPE '\\') " +
            "ORDER BY number, title LIMIT :limit OFFSET :offset",
    )
    suspend fun searchSeriesPage(profileId: String, pattern: String, limit: Int, offset: Int): List<SeriesEntity>

    @Query(
        "SELECT * FROM series WHERE profileId = :profileId AND categoryId = :categoryId " +
            "AND (title LIKE :pattern ESCAPE '\\' OR COALESCE(genre, '') LIKE :pattern ESCAPE '\\' OR COALESCE(year, '') LIKE :pattern ESCAPE '\\') " +
            "ORDER BY number, title LIMIT :limit OFFSET :offset",
    )
    suspend fun searchSeriesByCategoryPage(profileId: String, categoryId: String, pattern: String, limit: Int, offset: Int): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE profileId = :profileId AND seriesId IN (:seriesIds)")
    suspend fun getSeriesByIds(profileId: String, seriesIds: List<Int>): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE profileId = :profileId AND seriesId = :seriesId")
    suspend fun getSeries(profileId: String, seriesId: Int): SeriesEntity?

    @Query("SELECT * FROM series WHERE profileId = :profileId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getTrendingSeries(profileId: String, limit: Int): List<SeriesEntity>

    @Query(
        "SELECT * FROM series " +
            "WHERE profileId = :profileId " +
            "AND CAST(REPLACE(COALESCE(rating, '0'), ',', '.') AS REAL) >= 9.0 " +
            "ORDER BY CAST(REPLACE(COALESCE(rating, '0'), ',', '.') AS REAL) DESC, RANDOM() " +
            "LIMIT :limit",
    )
    suspend fun getBestRatedSeries(profileId: String, limit: Int): List<SeriesEntity>

    @Query("SELECT * FROM trending_media WHERE profileId = :profileId AND contentType = :contentType")
    suspend fun getTrendingMedia(profileId: String, contentType: String): List<TrendingMediaEntity>

    @Query("SELECT contentId FROM trending_media WHERE profileId = :profileId AND contentType = :contentType")
    suspend fun getTrendingContentIds(profileId: String, contentType: String): List<Int>

    @Query("DELETE FROM trending_media WHERE profileId = :profileId AND contentType = :contentType")
    suspend fun clearTrendingMedia(profileId: String, contentType: String)

    @Upsert
    suspend fun upsertTrendingMedia(items: List<TrendingMediaEntity>)

    @Query(
        "SELECT * FROM home_trending_preview_cache " +
            "WHERE profileId = :profileId AND contentType = :contentType AND contentId = :contentId AND lastSync = :lastSync",
    )
    suspend fun getHomeTrendingPreviewCache(
        profileId: String,
        contentType: String,
        contentId: Int,
        lastSync: Long,
    ): HomeTrendingPreviewCacheEntity?

    @Upsert
    suspend fun upsertHomeTrendingPreviewCache(item: HomeTrendingPreviewCacheEntity)

    @Query("DELETE FROM home_trending_preview_cache WHERE profileId = :profileId AND lastSync != :lastSync")
    suspend fun deleteStaleHomeTrendingPreviewCache(profileId: String, lastSync: Long)

    @Query(
        "SELECT * FROM tmdb_content_mapping " +
            "WHERE profileId = :profileId AND contentType = :contentType AND contentId = :contentId LIMIT 1",
    )
    suspend fun getTmdbContentMapping(profileId: String, contentType: String, contentId: Int): TmdbContentMappingEntity?

    @Upsert
    suspend fun upsertTmdbContentMapping(mapping: TmdbContentMappingEntity)

    @Query("SELECT * FROM tmdb_movie_metadata WHERE tmdbId = :tmdbId AND language = :language LIMIT 1")
    suspend fun getTmdbMovieMetadata(tmdbId: Int, language: String): TmdbMovieMetadataEntity?

    @Query("SELECT * FROM tmdb_movie_metadata WHERE tmdbId = :tmdbId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getAnyTmdbMovieMetadata(tmdbId: Int): TmdbMovieMetadataEntity?

    @Upsert
    suspend fun upsertTmdbMovieMetadata(metadata: TmdbMovieMetadataEntity)

    @Query("DELETE FROM tmdb_movie_metadata WHERE updatedAt < :minUpdatedAt")
    suspend fun deleteStaleTmdbMovieMetadata(minUpdatedAt: Long)

    @Query("SELECT * FROM tmdb_series_metadata WHERE tmdbId = :tmdbId AND language = :language LIMIT 1")
    suspend fun getTmdbSeriesMetadata(tmdbId: Int, language: String): TmdbSeriesMetadataEntity?

    @Query("SELECT * FROM tmdb_series_metadata WHERE tmdbId = :tmdbId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getAnyTmdbSeriesMetadata(tmdbId: Int): TmdbSeriesMetadataEntity?

    @Upsert
    suspend fun upsertTmdbSeriesMetadata(metadata: TmdbSeriesMetadataEntity)

    @Query("DELETE FROM tmdb_series_metadata WHERE updatedAt < :minUpdatedAt")
    suspend fun deleteStaleTmdbSeriesMetadata(minUpdatedAt: Long)

    @Query("DELETE FROM series WHERE profileId = :profileId")
    suspend fun clearSeries(profileId: String)

    @Upsert
    suspend fun upsertSeries(series: List<SeriesEntity>)

    @Query("SELECT * FROM episodes WHERE profileId = :profileId AND seriesId = :seriesId ORDER BY seasonNumber, episodeNumber")
    suspend fun getEpisodes(profileId: String, seriesId: Int): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE profileId = :profileId AND episodeId = :episodeId")
    suspend fun getEpisode(profileId: String, episodeId: Int): EpisodeEntity?

    @Query("DELETE FROM episodes WHERE profileId = :profileId AND seriesId = :seriesId")
    suspend fun deleteEpisodes(profileId: String, seriesId: Int)

    @Upsert
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)
}
