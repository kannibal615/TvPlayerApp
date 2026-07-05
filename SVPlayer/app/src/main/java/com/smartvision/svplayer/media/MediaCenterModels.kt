package com.smartvision.svplayer.media

import android.net.Uri
import java.io.InputStream

data class MediaCenterFolder(
    val id: Long,
    val name: String,
    val relativePath: String,
    val parentId: Long?,
    val updatedAt: Long,
)

data class MediaCenterFile(
    val id: Long,
    val folderId: Long?,
    val displayName: String,
    val relativePath: String,
    val mimeType: String?,
    val mediaType: MediaCenterFileType,
    val source: MediaCenterSource,
    val sizeBytes: Long,
    val durationMs: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class MediaCenterStorageInfo(
    val rootPath: String,
    val availableBytes: Long,
)

data class MediaCenterPlayback(
    val id: Long,
    val displayName: String,
    val relativePath: String,
    val uri: Uri,
    val mimeType: String?,
    val mediaType: MediaCenterFileType,
    val source: MediaCenterSource,
    val sizeBytes: Long,
    val updatedAt: Long,
)

data class MediaCenterDownload(
    val displayName: String,
    val relativePath: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val openStream: () -> InputStream,
)

enum class MediaCenterFileType(val key: String) {
    Video("video"),
    Photo("photo"),
    Audio("audio"),
    Other("other"),
}

enum class MediaCenterSource(val key: String) {
    Recording("recording"),
    Import("import"),
    Transfer("transfer"),
    Local("local"),
}

internal data class ScannedMediaFolder(
    val name: String,
    val relativePath: String,
    val parentRelativePath: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

internal data class ScannedMediaFile(
    val displayName: String,
    val relativePath: String,
    val folderRelativePath: String?,
    val mimeType: String?,
    val mediaType: MediaCenterFileType,
    val source: MediaCenterSource,
    val sizeBytes: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

internal data class MediaStorageScan(
    val storageInfo: MediaCenterStorageInfo,
    val folders: List<ScannedMediaFolder>,
    val files: List<ScannedMediaFile>,
)
