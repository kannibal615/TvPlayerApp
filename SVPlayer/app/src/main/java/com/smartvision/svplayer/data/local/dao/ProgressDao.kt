package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity

@Dao
interface ProgressDao {
    @Query("SELECT * FROM playback_progress WHERE contentType = :contentType AND contentId = :contentId")
    suspend fun get(contentType: String, contentId: String): PlaybackProgressEntity?

    @Upsert
    suspend fun upsert(progress: PlaybackProgressEntity)
}
