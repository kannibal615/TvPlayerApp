package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartvision.svplayer.data.local.entity.SyncStateEntity

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE profileId = :profileId AND id = :id")
    suspend fun get(profileId: String, id: String = "catalog"): SyncStateEntity?

    @Upsert
    suspend fun upsert(state: SyncStateEntity)
}
