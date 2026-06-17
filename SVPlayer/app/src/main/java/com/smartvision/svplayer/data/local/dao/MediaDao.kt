package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartvision.svplayer.data.local.entity.EpisodeEntity
import com.smartvision.svplayer.data.local.entity.LiveStreamEntity
import com.smartvision.svplayer.data.local.entity.MovieEntity
import com.smartvision.svplayer.data.local.entity.SeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM live_streams ORDER BY number, name")
    fun observeLiveStreams(): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_streams WHERE (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY number, name")
    fun observeLiveStreamsByCategory(categoryId: String?): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_streams WHERE streamId = :streamId")
    suspend fun getLiveStream(streamId: Int): LiveStreamEntity?

    @Query("DELETE FROM live_streams")
    suspend fun clearLiveStreams()

    @Upsert
    suspend fun upsertLiveStreams(streams: List<LiveStreamEntity>)

    @Query("SELECT * FROM movies ORDER BY number, title")
    fun observeMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY number, title")
    fun observeMoviesByCategory(categoryId: String?): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE streamId = :streamId")
    suspend fun getMovie(streamId: Int): MovieEntity?

    @Query("DELETE FROM movies")
    suspend fun clearMovies()

    @Upsert
    suspend fun upsertMovies(movies: List<MovieEntity>)

    @Query("SELECT * FROM series ORDER BY number, title")
    fun observeSeries(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY number, title")
    fun observeSeriesByCategory(categoryId: String?): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE seriesId = :seriesId")
    suspend fun getSeries(seriesId: Int): SeriesEntity?

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
