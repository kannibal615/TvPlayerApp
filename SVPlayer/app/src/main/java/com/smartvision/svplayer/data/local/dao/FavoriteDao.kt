package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartvision.svplayer.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE profileId = :profileId AND contentType = :contentType AND contentId = :contentId")
    suspend fun get(profileId: String, contentType: String, contentId: String): FavoriteEntity?

    @Query("SELECT * FROM favorites WHERE profileId = :profileId AND contentType = :contentType ORDER BY createdAt DESC")
    fun observeByType(profileId: String, contentType: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE profileId = :profileId AND contentType = :contentType ORDER BY createdAt DESC")
    suspend fun getByType(profileId: String, contentType: String): List<FavoriteEntity>

    @Upsert
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE profileId = :profileId AND contentType = :contentType AND contentId = :contentId")
    suspend fun delete(profileId: String, contentType: String, contentId: String)

    @Query("DELETE FROM favorites WHERE profileId = :profileId AND contentType = :contentType")
    suspend fun deleteByType(profileId: String, contentType: String)

    @Query("DELETE FROM favorites WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: String)
}
