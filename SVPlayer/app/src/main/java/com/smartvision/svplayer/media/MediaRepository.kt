package com.smartvision.svplayer.media

import com.smartvision.svplayer.data.local.dao.MediaCenterDao
import com.smartvision.svplayer.data.local.entity.MediaFileEntity
import com.smartvision.svplayer.data.local.entity.MediaFolderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MediaRepository(
    private val dao: MediaCenterDao,
    private val storageManager: MediaStorageManager,
) {
    fun observeFiles(): Flow<List<MediaCenterFile>> =
        dao.observeActiveFiles().map { entities -> entities.map { it.toDomain() } }

    fun observeFolders(): Flow<List<MediaCenterFolder>> =
        dao.observeFolders().map { entities -> entities.map { it.toDomain() } }

    suspend fun refreshStorage(): MediaCenterStorageInfo = withContext(Dispatchers.IO) {
        val scan = storageManager.scan()
        upsertFolders(scan.folders)
        val folders = dao.getFoldersSnapshot().associateBy { it.relativePath }
        val now = System.currentTimeMillis()
        scan.files.forEach { scanned ->
            val existing = dao.getFileByRelativePath(scanned.relativePath)
            val folderId = scanned.folderRelativePath?.let { folders[it]?.id }
            if (existing == null) {
                dao.insertFile(scanned.toEntity(folderId, now))
            } else {
                dao.updateFileLocationFromScan(existing.id, folderId, scanned)
            }
        }
        val activePaths = scan.files.map { it.relativePath }
        if (activePaths.isEmpty()) {
            dao.markAllActiveFilesDeleted(now)
        } else {
            dao.markMissingFilesDeleted(activePaths, now)
        }
        scan.storageInfo
    }

    suspend fun renameFile(fileId: Long, requestedName: String): Result<Unit> =
        runCatching {
            withContext(Dispatchers.IO) {
                val file = dao.getFile(fileId) ?: error("File not found.")
                val scanned = storageManager.rename(file.relativePath, requestedName)
                val folderId = dao.getFolderByRelativePath(scanned.folderRelativePath.orEmpty())?.id
                dao.updateFileLocationFromScan(file.id, folderId, scanned)
            }
        }

    suspend fun moveFile(fileId: Long, targetFolderId: Long?): Result<Unit> =
        runCatching {
            withContext(Dispatchers.IO) {
                val file = dao.getFile(fileId) ?: error("File not found.")
                val targetFolder = targetFolderId?.let { dao.getFolder(it) ?: error("Target folder not found.") }
                val scanned = storageManager.move(file.relativePath, targetFolder?.relativePath)
                dao.updateFileLocationFromScan(file.id, targetFolder?.id, scanned)
            }
        }

    suspend fun deleteFile(fileId: Long): Result<Unit> =
        runCatching {
            withContext(Dispatchers.IO) {
                val file = dao.getFile(fileId) ?: error("File not found.")
                storageManager.delete(file.relativePath)
                dao.markFileDeleted(file.id, System.currentTimeMillis())
            }
        }

    private suspend fun upsertFolders(folders: List<ScannedMediaFolder>) {
        val createdIds = mutableMapOf<String, Long>()
        folders.forEach { folder ->
            val parentId = folder.parentRelativePath?.let { parentPath ->
                createdIds[parentPath] ?: dao.getFolderByRelativePath(parentPath)?.id
            }
            val existing = dao.getFolderByRelativePath(folder.relativePath)
            if (existing == null) {
                val id = dao.insertFolder(folder.toEntity(parentId))
                if (id > 0) createdIds[folder.relativePath] = id
            } else {
                dao.updateFolder(
                    existing.copy(
                        name = folder.name,
                        parentId = parentId,
                        updatedAt = folder.updatedAt,
                    ),
                )
                createdIds[folder.relativePath] = existing.id
            }
        }
    }

    private suspend fun MediaCenterDao.updateFileLocationFromScan(
        id: Long,
        folderId: Long?,
        scanned: ScannedMediaFile,
    ) {
        updateFileLocation(
            id = id,
            folderId = folderId,
            displayName = scanned.displayName,
            relativePath = scanned.relativePath,
            mimeType = scanned.mimeType,
            mediaType = scanned.mediaType.key,
            source = scanned.source.key,
            sizeBytes = scanned.sizeBytes,
            updatedAt = scanned.updatedAt,
            lastIndexedAt = System.currentTimeMillis(),
        )
    }

    private fun ScannedMediaFolder.toEntity(parentId: Long?): MediaFolderEntity =
        MediaFolderEntity(
            name = name,
            relativePath = relativePath,
            parentId = parentId,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun ScannedMediaFile.toEntity(folderId: Long?, now: Long): MediaFileEntity =
        MediaFileEntity(
            folderId = folderId,
            displayName = displayName,
            relativePath = relativePath,
            mimeType = mimeType,
            mediaType = mediaType.key,
            source = source.key,
            sizeBytes = sizeBytes,
            durationMs = null,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastIndexedAt = now,
            deletedAt = null,
        )

    private fun MediaFileEntity.toDomain(): MediaCenterFile =
        MediaCenterFile(
            id = id,
            folderId = folderId,
            displayName = displayName,
            relativePath = relativePath,
            mimeType = mimeType,
            mediaType = MediaCenterFileType.entries.firstOrNull { it.key == mediaType } ?: MediaCenterFileType.Other,
            source = MediaCenterSource.entries.firstOrNull { it.key == source } ?: MediaCenterSource.Local,
            sizeBytes = sizeBytes,
            durationMs = durationMs,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun MediaFolderEntity.toDomain(): MediaCenterFolder =
        MediaCenterFolder(
            id = id,
            name = name,
            relativePath = relativePath,
            parentId = parentId,
            updatedAt = updatedAt,
        )
}
