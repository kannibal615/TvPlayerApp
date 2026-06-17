package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartvision.svplayer.data.local.entity.FavoriteEntity

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE contentType = :contentType AND contentId = :contentId")
    suspend fun get(contentType: String, contentId: String): FavoriteEntity?

    @Upsert
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE contentType = :contentType AND contentId = :contentId")
    suspend fun delete(contentType: String, contentId: String)
}
