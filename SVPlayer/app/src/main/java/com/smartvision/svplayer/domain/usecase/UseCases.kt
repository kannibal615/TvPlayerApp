package com.smartvision.svplayer.domain.usecase

import com.smartvision.svplayer.domain.model.PlaybackKind
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.domain.model.SyncStatus
import kotlinx.coroutines.sync.Mutex

class SynchronizeCatalogUseCase(
    private val repository: CatalogRepository,
) {
    private val invocationMutex = Mutex()

    suspend operator fun invoke(): Result<Unit> {
        if (repository.syncStatus.value is SyncStatus.Running || !invocationMutex.tryLock()) {
            return Result.success(Unit)
        }
        return try {
            repository.synchronize()
        } finally {
            invocationMutex.unlock()
        }
    }
}

class ToggleFavoriteUseCase(
    private val repository: CatalogRepository,
) {
    suspend operator fun invoke(contentType: String, contentId: String) {
        repository.toggleFavorite(contentType, contentId)
    }
}

class BuildPlaybackRequestUseCase(
    private val repository: CatalogRepository,
) {
    suspend operator fun invoke(kind: PlaybackKind, id: String) =
        repository.buildPlaybackRequest(kind, id)
}

class SavePlaybackProgressUseCase(
    private val repository: CatalogRepository,
) {
    suspend operator fun invoke(kind: PlaybackKind, id: String, positionMs: Long, durationMs: Long) {
        repository.savePlaybackProgress(kind, id, positionMs, durationMs)
    }
}
