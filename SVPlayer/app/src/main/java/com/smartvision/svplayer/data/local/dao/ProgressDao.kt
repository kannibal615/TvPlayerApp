package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM playback_progress WHERE contentType = :contentType AND contentId = :contentId")
    suspend fun get(contentType: String, contentId: String): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PlaybackProgressEntity>>

    @Upsert
    suspend fun upsert(progress: PlaybackProgressEntity)
}
