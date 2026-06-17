package com.smartvision.svplayer.data.local.entity

import androidx.room.Entity

@Entity(tableName = "categories", primaryKeys = ["id", "type"])
data class CategoryEntity(
    val id: String,
    val type: String,
    val name: String,
)
