package com.smartvision.svplayer.data.local.entity

import androidx.room.Entity

@Entity(tableName = "categories", primaryKeys = ["profileId", "id", "type"])
data class CategoryEntity(
    val profileId: String,
    val id: String,
    val type: String,
    val name: String,
)
