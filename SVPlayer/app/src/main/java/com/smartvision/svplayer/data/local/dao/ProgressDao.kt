package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM playback_progress WHERE profileId = :profileId AND contentType = :contentType AND contentId = :contentId")
    suspend fun get(profileId: String, contentType: String, contentId: String): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress WHERE profileId = :profileId ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(profileId: String, limit: Int): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE profileId = :profileId AND contentType = :contentType AND positionMs > :minimumPositionMs ORDER BY updatedAt DESC LIMIT :limit")
    fun observeHistory(profileId: String, contentType: String, minimumPositionMs: Long, limit: Int): Flow<List<PlaybackProgressEntity>>

    @Upsert
    suspend fun upsert(progress: PlaybackProgressEntity)

    @Query("DELETE FROM playback_progress WHERE profileId = :profileId AND contentType = :contentType AND contentId = :contentId")
    suspend fun delete(profileId: String, contentType: String, contentId: String)

    @Query("DELETE FROM playback_progress WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: String)
}
