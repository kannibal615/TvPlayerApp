package com.smartvision.svplayer.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.private_media.PrivateMediaItemDto
import com.smartvision.svplayer.data.private_media.PrivateMediaPlaybackResponse
import com.smartvision.svplayer.data.private_media.PrivateMediaRepository
import com.smartvision.svplayer.media.MediaCenterFile
import com.smartvision.svplayer.media.MediaCenterFileType
import com.smartvision.svplayer.media.MediaCenterFolder
import com.smartvision.svplayer.media.MediaCenterSource
import com.smartvision.svplayer.media.MediaCenterStorageInfo
import com.smartvision.svplayer.media.MediaRepository
import com.smartvision.svplayer.media.transfer.MediaTransferServer
import com.smartvision.svplayer.media.transfer.MediaTransferSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MediaViewModel(
    private val repository: MediaRepository,
    private val privateMediaRepository: PrivateMediaRepository,
    private val transferServer: MediaTransferServer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MediaScreenState())
    val uiState: StateFlow<MediaScreenState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.observeFiles(), repository.observeFolders()) { files, folders ->
                MediaSnapshot(files, folders)
            }.collect { snapshot ->
                _uiState.update { current ->
                    val folderCounts = snapshot.files.groupingBy { it.folderId }.eachCount()
                    val fileItems = snapshot.files.map { file ->
                        file.toUi(folderName = snapshot.folders.firstOrNull { it.id == file.folderId }?.name)
                    }
                    val folderItems = snapshot.folders.map { folder ->
                        folder.toUi(fileCount = folderCounts[folder.id] ?: 0)
                    }
                    val selectedFileId = current.selectedFileId
                        ?.takeIf { id -> fileItems.any { it.id == id } }
                        ?: fileItems.firstOrNull()?.id
                    current.copy(
                        loading = false,
                        files = fileItems,
                        folders = folderItems,
                        selectedFileId = selectedFileId,
                    )
                }
            }
        }
        refreshStorage()
        loadPrivateMedia()
    }

    fun selectArea(area: MediaArea) {
        _uiState.update { current ->
            val nextSelection = if (area == MediaArea.Folders) {
                null
            } else {
                current.files.firstOrNull { it.matches(area) }?.id
                    ?: current.files.firstOrNull()?.id
            }
            current.copy(
                selectedSource = MediaSource.Local,
                selectedArea = area,
                selectedFileId = nextSelection,
            )
        }
    }

    fun toggleLocalGroup() {
        _uiState.update {
            it.copy(
                selectedSource = MediaSource.Local,
                localExpanded = !it.localExpanded,
            )
        }
    }

    fun selectPrivateMedia() {
        val state = _uiState.value
        if (!state.privateProviderEnabled) {
            _uiState.update { it.copy(selectedSource = MediaSource.Private) }
            return
        }
        _uiState.update { it.copy(selectedSource = MediaSource.Private, privateExpanded = true) }
        if (state.privateItems.isEmpty() && !state.privateLoading) {
            loadPrivateMedia()
        }
    }

    fun togglePrivateGroup() {
        _uiState.update {
            it.copy(
                selectedSource = MediaSource.Private,
                privateExpanded = !it.privateExpanded,
            )
        }
        if (!_uiState.value.privateBootstrapLoaded) {
            loadPrivateMedia()
        }
    }

    fun selectPrivateCategory(categoryId: String) {
        _uiState.update {
            it.copy(
                selectedSource = MediaSource.Private,
                selectedPrivateCategoryId = categoryId,
                selectedPrivateItemId = null,
                privatePreviewPlayback = null,
                privatePreviewError = null,
            )
        }
        loadPrivateMedia(force = true)
    }

    fun updateSearchQuery(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                selectedPrivateItemId = if (it.selectedSource == MediaSource.Private) null else it.selectedPrivateItemId,
                privatePreviewPlayback = if (it.selectedSource == MediaSource.Private) null else it.privatePreviewPlayback,
                privatePreviewError = if (it.selectedSource == MediaSource.Private) null else it.privatePreviewError,
            )
        }
        if (_uiState.value.selectedSource == MediaSource.Private) {
            loadPrivateMedia(force = true)
        }
    }

    fun selectPrivateItem(itemId: String) {
        _uiState.update {
            it.copy(
                selectedPrivateItemId = itemId,
                privatePreviewLoading = true,
                privatePreviewPlayback = null,
                privatePreviewError = null,
            )
        }
        viewModelScope.launch {
            privateMediaRepository.loadPlayback(itemId)
                .onSuccess { playback ->
                    _uiState.update {
                        it.copy(
                            privatePreviewLoading = false,
                            privatePreviewPlayback = playback.toPreviewUi(itemId),
                            privatePreviewError = playback.error,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            privatePreviewLoading = false,
                            privatePreviewError = throwable.message ?: "Private media playback unavailable.",
                        )
                    }
                }
        }
    }

    fun refreshPrivateMedia() {
        loadPrivateMedia(force = true)
    }

    fun selectFile(fileId: Long) {
        _uiState.update { it.copy(selectedFileId = fileId) }
    }

    fun refreshStorage() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true, errorMessage = null) }
            runCatching { repository.refreshStorage() }
                .onSuccess { storageInfo ->
                    _uiState.update {
                        it.copy(
                            refreshing = false,
                            storageInfo = storageInfo,
                            message = null,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            refreshing = false,
                            loading = false,
                            errorMessage = throwable.message ?: "Media storage refresh failed.",
                        )
                    }
                }
        }
    }

    private fun loadPrivateMedia(force: Boolean = false) {
        viewModelScope.launch {
            val current = _uiState.value
            if (!force && current.privateBootstrapLoaded && current.privateItems.isNotEmpty()) {
                return@launch
            }
            _uiState.update {
                it.copy(
                    privateLoading = true,
                    privateErrorMessage = null,
                    privateBootstrapLoaded = true,
                )
            }
            val librariesResult = privateMediaRepository.loadLibraries()
            val libraries = librariesResult.getOrElse { throwable ->
                _uiState.update {
                    it.copy(
                        privateLoading = false,
                        privateProviderEnabled = false,
                        privateErrorMessage = throwable.message ?: "Private media unavailable.",
                    )
                }
                return@launch
            }
            val enabled = libraries.any { it.id == "private_media" && it.isEnabled }
            if (!enabled) {
                _uiState.update {
                    it.copy(
                        privateLoading = false,
                        privateProviderEnabled = false,
                        privateCategories = emptyList(),
                        privateItems = emptyList(),
                        selectedPrivateItemId = null,
                        privateErrorMessage = null,
                    )
                }
                return@launch
            }

            val categories = privateMediaRepository.loadCategories().getOrElse { throwable ->
                _uiState.update {
                    it.copy(
                        privateLoading = false,
                        privateProviderEnabled = true,
                        privateErrorMessage = throwable.message ?: "Private media categories unavailable.",
                    )
                }
                return@launch
            }
            val selectedCategoryId = current.selectedPrivateCategoryId
                ?.takeIf { id -> categories.any { it.id == id } }
                ?: categories.firstOrNull()?.id
            if (selectedCategoryId == null) {
                _uiState.update {
                    it.copy(
                        privateLoading = false,
                        privateProviderEnabled = true,
                        privateCategories = emptyList(),
                        privateItems = emptyList(),
                        selectedPrivateItemId = null,
                    )
                }
                return@launch
            }

            val page = privateMediaRepository.loadItems(selectedCategoryId, query = current.searchQuery).getOrElse { throwable ->
                _uiState.update {
                    it.copy(
                        privateLoading = false,
                        privateProviderEnabled = true,
                        privateCategories = categories.map { category ->
                            PrivateMediaCategoryUi(category.id, category.title)
                        },
                        selectedPrivateCategoryId = selectedCategoryId,
                        privateErrorMessage = throwable.message ?: "Private media unavailable.",
                    )
                }
                return@launch
            }
            val items = page.items.map { it.toPrivateUi() }
            _uiState.update {
                it.copy(
                    privateLoading = false,
                    privateProviderEnabled = true,
                    privateCategories = categories.map { category -> PrivateMediaCategoryUi(category.id, category.title) },
                    selectedPrivateCategoryId = selectedCategoryId,
                    privateItems = items,
                    selectedPrivateItemId = it.selectedPrivateItemId?.takeIf { id -> items.any { item -> item.id == id } }
                        ?: items.firstOrNull()?.id,
                    privatePreviewPlayback = it.privatePreviewPlayback?.takeIf { playback -> items.any { item -> item.id == playback.itemId } },
                    privateErrorMessage = page.error,
                )
            }
        }
    }

    fun renameSelected(requestedName: String) {
        val selectedId = _uiState.value.selectedFileId ?: return
        renameFile(selectedId, requestedName)
    }

    fun renameFile(fileId: Long, requestedName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(operationInProgress = true, errorMessage = null, message = null) }
            repository.renameFile(fileId, requestedName)
                .onSuccess {
                    _uiState.update { it.copy(operationInProgress = false, message = MediaActionMessage.Renamed) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            operationInProgress = false,
                            errorMessage = throwable.message ?: "Unable to rename media file.",
                        )
                    }
                }
        }
    }

    fun moveSelected(targetFolderId: Long?) {
        val selectedId = _uiState.value.selectedFileId ?: return
        moveFile(selectedId, targetFolderId)
    }

    fun moveFile(fileId: Long, targetFolderId: Long?) {
        viewModelScope.launch {
            _uiState.update { it.copy(operationInProgress = true, errorMessage = null, message = null) }
            repository.moveFile(fileId, targetFolderId)
                .onSuccess {
                    _uiState.update { it.copy(operationInProgress = false, message = MediaActionMessage.Moved) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            operationInProgress = false,
                            errorMessage = throwable.message ?: "Unable to move media file.",
                        )
                    }
                }
        }
    }

    fun deleteSelected() {
        val selectedId = _uiState.value.selectedFileId ?: return
        deleteFile(selectedId)
    }

    fun deleteFile(fileId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(operationInProgress = true, errorMessage = null, message = null) }
            repository.deleteFile(fileId)
                .onSuccess {
                    _uiState.update { current ->
                        val nextFiles = current.files.filterNot { it.id == fileId }
                        current.copy(
                            operationInProgress = false,
                            selectedFileId = nextFiles.firstOrNull { it.matches(current.selectedArea) }?.id
                                ?: nextFiles.firstOrNull()?.id,
                            message = MediaActionMessage.Deleted,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            operationInProgress = false,
                            errorMessage = throwable.message ?: "Unable to delete media file.",
                        )
                    }
                }
        }
    }

    fun startPhoneImport() {
        _uiState.update { it.copy(transferInProgress = true, errorMessage = null, message = null) }
        transferServer.startImportSession { result ->
            viewModelScope.launch {
                refreshStorage()
                transferServer.stop()
                _uiState.update {
                    it.copy(
                        transferInProgress = false,
                        transferSession = null,
                        selectedArea = MediaArea.Transfers,
                        selectedFileId = result.fileId ?: it.selectedFileId,
                        message = MediaActionMessage.TransferUploaded,
                    )
                }
            }
        }.onSuccess { session ->
            _uiState.update {
                it.copy(
                    transferInProgress = false,
                    transferSession = session,
                    selectedArea = MediaArea.Transfers,
                )
            }
        }.onFailure { throwable ->
            _uiState.update {
                it.copy(
                    transferInProgress = false,
                    transferSession = null,
                    errorMessage = throwable.message ?: "Unable to start phone import.",
                )
            }
        }
    }

    fun startPhoneExport() {
        val selectedId = _uiState.value.selectedFileId ?: return
        _uiState.update { it.copy(transferInProgress = true, errorMessage = null, message = null) }
        viewModelScope.launch {
            transferServer.startExportSession(selectedId)
                .onSuccess { session ->
                    _uiState.update {
                        it.copy(
                            transferInProgress = false,
                            transferSession = session,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            transferInProgress = false,
                            transferSession = null,
                            errorMessage = throwable.message ?: "Unable to start phone export.",
                        )
                    }
                }
        }
    }

    fun dismissTransferSession() {
        transferServer.stop()
        _uiState.update { it.copy(transferSession = null, transferInProgress = false) }
    }

    fun clearTransientMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    override fun onCleared() {
        transferServer.stop()
        super.onCleared()
    }

    private fun MediaCenterFile.toUi(folderName: String?): MediaFileUi =
        MediaFileUi(
            id = id,
            folderId = folderId,
            displayName = displayName,
            relativePath = relativePath,
            folderName = folderName,
            mimeType = mimeType,
            mediaType = mediaType,
            source = source,
            sizeBytes = sizeBytes,
            sizeLabel = formatBytes(sizeBytes),
            updatedLabel = formatDate(updatedAt),
        )

    private fun MediaCenterFolder.toUi(fileCount: Int): MediaFolderUi =
        MediaFolderUi(
            id = id,
            name = name,
            relativePath = relativePath,
            parentId = parentId,
            fileCount = fileCount,
            updatedLabel = formatDate(updatedAt),
        )

    private fun formatDate(timestamp: Long): String =
        if (timestamp <= 0L) {
            "-"
        } else {
            DateFormatter.format(Date(timestamp))
        }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val units = listOf("KB", "MB", "GB", "TB")
        var value = bytes / 1024.0
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < units.lastIndex) {
            value /= 1024.0
            unitIndex++
        }
        return String.format(Locale.US, "%.1f %s", value, units[unitIndex])
    }

    private data class MediaSnapshot(
        val files: List<MediaCenterFile>,
        val folders: List<MediaCenterFolder>,
    )

    private companion object {
        val DateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }
}

data class MediaScreenState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val operationInProgress: Boolean = false,
    val selectedArea: MediaArea = MediaArea.AllFiles,
    val selectedSource: MediaSource = MediaSource.Local,
    val localExpanded: Boolean = true,
    val privateExpanded: Boolean = true,
    val searchQuery: String = "",
    val selectedFileId: Long? = null,
    val files: List<MediaFileUi> = emptyList(),
    val folders: List<MediaFolderUi> = emptyList(),
    val storageInfo: MediaCenterStorageInfo? = null,
    val transferSession: MediaTransferSession? = null,
    val transferInProgress: Boolean = false,
    val privateBootstrapLoaded: Boolean = false,
    val privateProviderEnabled: Boolean = false,
    val privateLoading: Boolean = false,
    val privateCategories: List<PrivateMediaCategoryUi> = emptyList(),
    val selectedPrivateCategoryId: String? = null,
    val privateItems: List<PrivateMediaItemUi> = emptyList(),
    val selectedPrivateItemId: String? = null,
    val privatePreviewLoading: Boolean = false,
    val privatePreviewPlayback: PrivateMediaPlaybackUi? = null,
    val privatePreviewError: String? = null,
    val privateErrorMessage: String? = null,
    val message: MediaActionMessage? = null,
    val errorMessage: String? = null,
) {
    val selectedFile: MediaFileUi?
        get() = files.firstOrNull { it.id == selectedFileId }

    val selectedPrivateItem: PrivateMediaItemUi?
        get() = privateItems.firstOrNull { it.id == selectedPrivateItemId }

    val visibleFiles: List<MediaFileUi>
        get() = files
            .filter { it.matches(selectedArea) }
            .filter { file ->
                searchQuery.isBlank() ||
                    file.displayName.contains(searchQuery, ignoreCase = true) ||
                    file.relativePath.contains(searchQuery, ignoreCase = true)
            }

    fun countFor(area: MediaArea): Int =
        when (area) {
            MediaArea.Folders -> folders.size
            else -> files.count { it.matches(area) }
        }
}

data class PrivateMediaCategoryUi(
    val id: String,
    val title: String,
)

data class PrivateMediaItemUi(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val durationLabel: String,
    val viewsLabel: String,
    val ratingLabel: String,
    val tags: List<String>,
    val playbackType: String,
    val isPlayable: Boolean,
)

data class PrivateMediaPlaybackUi(
    val itemId: String,
    val title: String,
    val thumbnailUrl: String?,
    val playbackType: String,
    val streamUrl: String?,
    val streamType: String?,
    val embedUrl: String?,
    val pageUrl: String?,
    val error: String?,
) {
    val canPlay: Boolean
        get() = !streamUrl.isNullOrBlank() || !embedUrl.isNullOrBlank()
}

data class MediaFileUi(
    val id: Long,
    val folderId: Long?,
    val displayName: String,
    val relativePath: String,
    val folderName: String?,
    val mimeType: String?,
    val mediaType: MediaCenterFileType,
    val source: MediaCenterSource,
    val sizeBytes: Long,
    val sizeLabel: String,
    val updatedLabel: String,
)

data class MediaFolderUi(
    val id: Long,
    val name: String,
    val relativePath: String,
    val parentId: Long?,
    val fileCount: Int,
    val updatedLabel: String,
)

enum class MediaArea {
    AllFiles,
    Recordings,
    Imports,
    Folders,
    Transfers,
}

enum class MediaSource {
    Local,
    Private,
}

enum class MediaActionMessage {
    Renamed,
    Moved,
    Deleted,
    TransferUploaded,
}

fun MediaFileUi.matches(area: MediaArea): Boolean =
    when (area) {
        MediaArea.AllFiles -> true
        MediaArea.Recordings -> source == MediaCenterSource.Recording
        MediaArea.Imports -> source == MediaCenterSource.Import
        MediaArea.Transfers -> source == MediaCenterSource.Transfer
        MediaArea.Folders -> false
    }

private fun PrivateMediaItemDto.toPrivateUi(): PrivateMediaItemUi =
    PrivateMediaItemUi(
        id = id,
        title = title.ifBlank { "Untitled" },
        thumbnailUrl = thumbnailUrl,
        durationLabel = durationLabel?.takeIf { it.isNotBlank() } ?: durationSeconds.formatDurationLabel(),
        viewsLabel = views?.let { "%,d views".format(Locale.US, it) }.orEmpty(),
        ratingLabel = rating?.let { "%.1f".format(Locale.US, it) }.orEmpty(),
        tags = tags.ifEmpty { keywords }.take(5),
        playbackType = playbackType,
        isPlayable = isPlayable,
    )

private fun PrivateMediaPlaybackResponse.toPreviewUi(fallbackItemId: String): PrivateMediaPlaybackUi {
    val stream = streams.firstOrNull { stream ->
        stream.url.isNotBlank() && (stream.type.equals("HLS", ignoreCase = true) || stream.type.equals("MP4", ignoreCase = true))
    }
    return PrivateMediaPlaybackUi(
        itemId = id.ifBlank { fallbackItemId },
        title = title,
        thumbnailUrl = thumbnailUrl,
        playbackType = playbackType,
        streamUrl = stream?.url,
        streamType = stream?.type,
        embedUrl = embedUrl,
        pageUrl = pageUrl,
        error = error,
    )
}

private fun Long?.formatDurationLabel(): String =
    this?.takeIf { it > 0 }?.let { seconds ->
        val minutes = seconds / 60
        val remaining = seconds % 60
        "%d:%02d".format(Locale.US, minutes, remaining)
    }.orEmpty()
