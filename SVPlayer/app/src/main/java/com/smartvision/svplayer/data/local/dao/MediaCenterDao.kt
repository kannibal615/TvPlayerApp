package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartvision.svplayer.data.local.entity.MediaFileEntity
import com.smartvision.svplayer.data.local.entity.MediaFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaCenterDao {
    @Query("SELECT * FROM media_folders ORDER BY relativePath")
    fun observeFolders(): Flow<List<MediaFolderEntity>>

    @Query("SELECT * FROM media_folders ORDER BY relativePath")
    suspend fun getFoldersSnapshot(): List<MediaFolderEntity>

    @Query("SELECT * FROM media_folders WHERE id = :id LIMIT 1")
    suspend fun getFolder(id: Long): MediaFolderEntity?

    @Query("SELECT * FROM media_folders WHERE relativePath = :relativePath LIMIT 1")
    suspend fun getFolderByRelativePath(relativePath: String): MediaFolderEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolder(folder: MediaFolderEntity): Long

    @Update
    suspend fun updateFolder(folder: MediaFolderEntity)

    @Query("SELECT * FROM media_files WHERE deletedAt IS NULL ORDER BY updatedAt DESC, displayName")
    fun observeActiveFiles(): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files WHERE deletedAt IS NULL ORDER BY updatedAt DESC, displayName")
    suspend fun getActiveFilesSnapshot(): List<MediaFileEntity>

    @Query("SELECT * FROM media_files WHERE id = :id LIMIT 1")
    suspend fun getFile(id: Long): MediaFileEntity?

    @Query("SELECT * FROM media_files WHERE relativePath = :relativePath LIMIT 1")
    suspend fun getFileByRelativePath(relativePath: String): MediaFileEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFile(file: MediaFileEntity): Long

    @Query(
        "UPDATE media_files SET " +
            "folderId = :folderId, " +
            "displayName = :displayName, " +
            "relativePath = :relativePath, " +
            "mimeType = :mimeType, " +
            "mediaType = :mediaType, " +
            "source = :source, " +
            "sizeBytes = :sizeBytes, " +
            "updatedAt = :updatedAt, " +
            "lastIndexedAt = :lastIndexedAt, " +
            "deletedAt = NULL " +
            "WHERE id = :id",
    )
    suspend fun updateFileLocation(
        id: Long,
        folderId: Long?,
        displayName: String,
        relativePath: String,
        mimeType: String?,
        mediaType: String,
        source: String,
        sizeBytes: Long,
        updatedAt: Long,
        lastIndexedAt: Long,
    )

    @Query("UPDATE media_files SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun markFileDeleted(id: Long, deletedAt: Long)

    @Query("UPDATE media_files SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE deletedAt IS NULL")
    suspend fun markAllActiveFilesDeleted(deletedAt: Long)

    @Query("UPDATE media_files SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE deletedAt IS NULL AND relativePath NOT IN (:activePaths)")
    suspend fun markMissingFilesDeleted(activePaths: List<String>, deletedAt: Long)
}
