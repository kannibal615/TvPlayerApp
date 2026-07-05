package com.smartvision.svplayer.media

import android.content.Context
import android.webkit.MimeTypeMap
import java.io.File
import java.util.Locale

class MediaStorageManager(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun ensureStorage(): MediaCenterStorageInfo {
        val root = rootDirectory()
        root.mkdirs()
        DefaultFolders.forEach { folder -> File(root, folder).mkdirs() }
        return MediaCenterStorageInfo(
            rootPath = root.absolutePath,
            availableBytes = root.usableSpace,
        )
    }

    internal fun scan(): MediaStorageScan {
        val storageInfo = ensureStorage()
        val root = rootDirectory().canonicalFile
        val folders = root.walkTopDown()
            .filter { it.isDirectory && it.canonicalFile != root }
            .mapNotNull { directory ->
                val relativePath = directory.toRelativeMediaPath(root) ?: return@mapNotNull null
                ScannedMediaFolder(
                    name = directory.name,
                    relativePath = relativePath,
                    parentRelativePath = directory.parentFile?.takeUnless { it.canonicalFile == root }?.toRelativeMediaPath(root),
                    createdAt = directory.lastModified().coerceAtLeast(0L),
                    updatedAt = directory.lastModified().coerceAtLeast(0L),
                )
            }
            .sortedBy { it.relativePath }
            .toList()
        val files = root.walkTopDown()
            .filter { it.isFile }
            .mapNotNull { file ->
                val relativePath = file.toRelativeMediaPath(root) ?: return@mapNotNull null
                val folderRelativePath = file.parentFile?.takeUnless { it.canonicalFile == root }?.toRelativeMediaPath(root)
                val mimeType = file.mimeType()
                ScannedMediaFile(
                    displayName = file.name,
                    relativePath = relativePath,
                    folderRelativePath = folderRelativePath,
                    mimeType = mimeType,
                    mediaType = file.mediaType(mimeType),
                    source = sourceFor(relativePath),
                    sizeBytes = file.length().coerceAtLeast(0L),
                    createdAt = file.lastModified().coerceAtLeast(0L),
                    updatedAt = file.lastModified().coerceAtLeast(0L),
                )
            }
            .sortedBy { it.relativePath }
            .toList()
        return MediaStorageScan(storageInfo, folders, files)
    }

    internal fun rename(relativePath: String, requestedName: String): ScannedMediaFile {
        val source = resolve(relativePath)
        require(source.isFile) { "File not found." }
        val destination = uniqueDestination(
            parent = source.parentFile ?: rootDirectory(),
            requestedName = sanitizeFileName(requestedName, source.name),
            current = source,
        )
        moveFile(source, destination)
        return destination.toScannedFile()
    }

    internal fun move(relativePath: String, targetFolderRelativePath: String?): ScannedMediaFile {
        val source = resolve(relativePath)
        require(source.isFile) { "File not found." }
        val targetFolder = if (targetFolderRelativePath.isNullOrBlank()) {
            rootDirectory()
        } else {
            resolveDirectory(targetFolderRelativePath)
        }
        targetFolder.mkdirs()
        val destination = uniqueDestination(targetFolder, source.name, source)
        moveFile(source, destination)
        return destination.toScannedFile()
    }

    fun delete(relativePath: String) {
        val file = resolve(relativePath)
        require(file.isFile) { "File not found." }
        check(file.delete()) { "Unable to delete file." }
    }

    private fun rootDirectory(): File {
        val external = appContext.getExternalFilesDir(null)
        return File(external ?: appContext.filesDir, RootFolderName)
    }

    private fun resolve(relativePath: String): File {
        val cleanPath = normalizeRelativePath(relativePath)
        val root = rootDirectory().canonicalFile
        val target = File(root, cleanPath).canonicalFile
        require(target.path == root.path || target.path.startsWith(root.path + File.separator)) {
            "Path outside media storage."
        }
        return target
    }

    private fun resolveDirectory(relativePath: String): File {
        val directory = resolve(relativePath)
        require(!directory.exists() || directory.isDirectory) { "Target folder is not a directory." }
        return directory
    }

    private fun File.toScannedFile(): ScannedMediaFile {
        val root = rootDirectory().canonicalFile
        val relativePath = toRelativeMediaPath(root) ?: error("Path outside media storage.")
        val folderRelativePath = parentFile?.takeUnless { it.canonicalFile == root }?.toRelativeMediaPath(root)
        val mimeType = mimeType()
        return ScannedMediaFile(
            displayName = name,
            relativePath = relativePath,
            folderRelativePath = folderRelativePath,
            mimeType = mimeType,
            mediaType = mediaType(mimeType),
            source = sourceFor(relativePath),
            sizeBytes = length().coerceAtLeast(0L),
            createdAt = lastModified().coerceAtLeast(0L),
            updatedAt = lastModified().coerceAtLeast(0L),
        )
    }

    private fun moveFile(source: File, destination: File) {
        if (source.canonicalPath == destination.canonicalPath) return
        destination.parentFile?.mkdirs()
        if (!source.renameTo(destination)) {
            source.inputStream().use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            if (!source.delete()) {
                destination.delete()
                error("Unable to finalize file move.")
            }
        }
    }

    private fun uniqueDestination(parent: File, requestedName: String, current: File): File {
        val safeName = requestedName.ifBlank { current.name }
        val baseName = safeName.substringBeforeLast('.', safeName)
        val extension = safeName.substringAfterLast('.', "").takeIf { it.isNotBlank() && it != safeName }
        var candidate = File(parent, safeName).canonicalFile
        var index = 2
        while (candidate.exists() && candidate.canonicalPath != current.canonicalPath) {
            val nextName = if (extension == null) "$baseName ($index)" else "$baseName ($index).$extension"
            candidate = File(parent, nextName).canonicalFile
            index++
        }
        val root = rootDirectory().canonicalFile
        require(candidate.path.startsWith(root.path + File.separator)) { "Destination outside media storage." }
        return candidate
    }

    private fun sanitizeFileName(requestedName: String, fallbackName: String): String {
        val trimmed = requestedName.trim()
        val base = trimmed
            .replace(InvalidFileNameChars, "_")
            .trim('.', ' ')
            .ifBlank { fallbackName }
        val hasExtension = base.substringAfterLast('.', "").let { it.isNotBlank() && it != base }
        val fallbackExtension = fallbackName.substringAfterLast('.', "").takeIf { it.isNotBlank() && it != fallbackName }
        return if (hasExtension || fallbackExtension == null) base else "$base.$fallbackExtension"
    }

    private fun normalizeRelativePath(relativePath: String): String {
        val normalized = relativePath.replace('\\', '/').trim().trim('/')
        require(normalized.isNotBlank() && normalized.split('/').none { it == ".." || it.isBlank() }) {
            "Invalid media path."
        }
        return normalized
    }

    private fun File.toRelativeMediaPath(root: File): String? {
        val base = root.canonicalPath
        val target = canonicalPath
        if (target == base) return ""
        if (!target.startsWith(base + File.separator)) return null
        return target.removePrefix(base + File.separator).replace(File.separatorChar, '/')
    }

    private fun File.mimeType(): String? {
        val extension = extension.lowercase(Locale.US).takeIf { it.isNotBlank() } ?: return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    private fun File.mediaType(mimeType: String?): MediaCenterFileType {
        val lowerName = name.lowercase(Locale.US)
        return when {
            mimeType?.startsWith("video/") == true || lowerName.endsWithAny(VideoExtensions) -> MediaCenterFileType.Video
            mimeType?.startsWith("image/") == true || lowerName.endsWithAny(PhotoExtensions) -> MediaCenterFileType.Photo
            mimeType?.startsWith("audio/") == true || lowerName.endsWithAny(AudioExtensions) -> MediaCenterFileType.Audio
            else -> MediaCenterFileType.Other
        }
    }

    private fun sourceFor(relativePath: String): MediaCenterSource {
        val normalized = relativePath.lowercase(Locale.US)
        return when {
            normalized.startsWith("recordings/") -> MediaCenterSource.Recording
            normalized.startsWith("imports/") -> MediaCenterSource.Import
            normalized.startsWith("transfers/") -> MediaCenterSource.Transfer
            else -> MediaCenterSource.Local
        }
    }

    private fun String.endsWithAny(extensions: Set<String>): Boolean =
        extensions.any { endsWith(it) }

    private companion object {
        const val RootFolderName = "SmartVisionMedia"
        val DefaultFolders = listOf("Recordings", "Imports", "Transfers")
        val InvalidFileNameChars = Regex("""[\\/:*?"<>|]""")
        val VideoExtensions = setOf(".mp4", ".mkv", ".avi", ".mov", ".ts", ".m3u8", ".webm")
        val PhotoExtensions = setOf(".jpg", ".jpeg", ".png", ".webp", ".gif")
        val AudioExtensions = setOf(".mp3", ".aac", ".m4a", ".wav", ".ogg", ".flac")
    }
}
