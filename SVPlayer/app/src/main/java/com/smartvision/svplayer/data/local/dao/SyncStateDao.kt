package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartvision.svplayer.data.local.entity.SyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE profileId = :profileId AND id = :id")
    suspend fun get(profileId: String, id: String = "catalog"): SyncStateEntity?

    @Query("SELECT * FROM sync_state WHERE profileId = :profileId AND id = :id")
    fun observe(profileId: String, id: String = "catalog"): Flow<SyncStateEntity?>

    @Upsert
    suspend fun upsert(state: SyncStateEntity)

    @Query("DELETE FROM sync_state WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: String)
}
