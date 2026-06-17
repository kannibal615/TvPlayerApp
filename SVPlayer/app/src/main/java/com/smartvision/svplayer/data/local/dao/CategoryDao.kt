package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartvision.svplayer.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name")
    fun observeByType(type: String): Flow<List<CategoryEntity>>

    @Query("DELETE FROM categories WHERE type = :type")
    suspend fun deleteByType(type: String)

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)
}
