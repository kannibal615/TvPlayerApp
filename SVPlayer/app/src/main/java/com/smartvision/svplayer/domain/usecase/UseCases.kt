package com.smartvision.svplayer.domain.usecase

import com.smartvision.svplayer.domain.model.PlaybackKind
import com.smartvision.svplayer.domain.repository.CatalogRepository

class SynchronizeCatalogUseCase(
    private val repository: CatalogRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.synchronize()
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
