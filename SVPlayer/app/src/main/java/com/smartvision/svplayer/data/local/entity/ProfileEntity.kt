package com.smartvision.svplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val host: String,
    val usernameMasked: String,
    val status: String,
    val expirationDate: String?,
    val activeConnections: Int?,
    val maxConnections: Int?,
    val lastSync: Long?,
)
