package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartvision.svplayer.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE profileId = :profileId AND type = :type ORDER BY name")
    fun observeByType(profileId: String, type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE profileId = :profileId AND type = :type ORDER BY name")
    suspend fun getByType(profileId: String, type: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE profileId = :profileId AND type = :type ORDER BY name LIMIT :limit")
    suspend fun getByTypeLimit(profileId: String, type: String, limit: Int): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories WHERE profileId = :profileId AND type = :type")
    suspend fun countByType(profileId: String, type: String): Int

    @Query("DELETE FROM categories WHERE profileId = :profileId AND type = :type")
    suspend fun deleteByType(profileId: String, type: String)

    @Query("DELETE FROM categories WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: String)

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)
}
