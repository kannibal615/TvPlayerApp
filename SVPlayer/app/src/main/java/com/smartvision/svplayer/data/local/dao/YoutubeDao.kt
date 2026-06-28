package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.smartvision.svplayer.data.local.entity.YoutubeBehaviorEventEntity
import com.smartvision.svplayer.data.local.entity.YoutubeSearchEntity
import com.smartvision.svplayer.data.local.entity.YoutubeSelectionEntity
import com.smartvision.svplayer.data.local.entity.YoutubeVideoHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface YoutubeDao {
    @Query("SELECT * FROM youtube_searches ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecentSearches(limit: Int = 10): Flow<List<YoutubeSearchEntity>>

    @Query("SELECT * FROM youtube_video_history ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecentVideos(limit: Int = 20): Flow<List<YoutubeVideoHistoryEntity>>

    @Query("SELECT COUNT(*) FROM youtube_video_history")
    fun observeRecentVideoCount(): Flow<Int>

    @Query("SELECT * FROM youtube_video_history ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentVideos(limit: Int = 100): List<YoutubeVideoHistoryEntity>

    @Query("SELECT * FROM youtube_selection WHERE id = 'last' LIMIT 1")
    suspend fun getLastSelection(): YoutubeSelectionEntity?

    @Query("SELECT * FROM youtube_behavior_events WHERE syncedAt IS NULL ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingBehaviorEvents(limit: Int = 40): List<YoutubeBehaviorEventEntity>

    @Upsert
    suspend fun upsertSearch(search: YoutubeSearchEntity)

    @Upsert
    suspend fun upsertVideo(video: YoutubeVideoHistoryEntity)

    @Upsert
    suspend fun upsertSelection(selection: YoutubeSelectionEntity)

    @Insert
    suspend fun insertBehaviorEvent(event: YoutubeBehaviorEventEntity): Long

    @Query("UPDATE youtube_behavior_events SET syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markBehaviorEventsSynced(ids: List<Long>, syncedAt: Long)
}
