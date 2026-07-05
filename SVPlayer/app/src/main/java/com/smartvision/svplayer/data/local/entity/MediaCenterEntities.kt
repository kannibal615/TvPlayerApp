package com.smartvision.svplayer.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_folders",
    indices = [
        Index(value = ["relativePath"], unique = true),
        Index(value = ["parentId"]),
    ],
)
data class MediaFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relativePath: String,
    val parentId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "media_files",
    indices = [
        Index(value = ["relativePath"], unique = true),
        Index(value = ["folderId"]),
        Index(value = ["mediaType"]),
        Index(value = ["source"]),
        Index(value = ["deletedAt"]),
    ],
)
data class MediaFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long?,
    val displayName: String,
    val relativePath: String,
    val mimeType: String?,
    val mediaType: String,
    val source: String,
    val sizeBytes: Long,
    val durationMs: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastIndexedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "recording_jobs",
    indices = [
        Index(value = ["status"]),
        Index(value = ["mediaFileId"]),
        Index(value = ["updatedAt"]),
    ],
)
data class RecordingJobEntity(
    @PrimaryKey val id: String,
    val mediaFileId: Long?,
    val status: String,
    val sourceType: String,
    val title: String,
    val outputRelativePath: String?,
    val startedAt: Long?,
    val endedAt: Long?,
    val updatedAt: Long,
    val errorMessage: String?,
)
