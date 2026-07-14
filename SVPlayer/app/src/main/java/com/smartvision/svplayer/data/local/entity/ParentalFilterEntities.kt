package com.smartvision.svplayer.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "parental_filter_snapshots")
data class ParentalFilterSnapshotEntity(
    @androidx.room.PrimaryKey val profileId: String,
    val keywordFingerprint: String,
    val catalogLastSync: Long,
    val generatedAt: Long,
)

@Entity(
    tableName = "parental_hidden_items",
    primaryKeys = ["profileId", "contentType", "contentId"],
    indices = [
        Index(value = ["profileId", "section", "folderId"]),
        Index(value = ["profileId", "title"]),
    ],
)
data class ParentalHiddenItemEntity(
    val profileId: String,
    val section: String,
    val folderId: String,
    val folderName: String,
    val contentType: String,
    val contentId: String,
    val title: String,
    val imageUrl: String?,
    val secondaryLabel: String,
    val duration: String?,
)
