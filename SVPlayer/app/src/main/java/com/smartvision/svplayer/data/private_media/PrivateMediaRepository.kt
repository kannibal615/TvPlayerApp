package com.smartvision.svplayer.data.private_media

class PrivateMediaRepository(
    private val api: PrivateMediaApiService,
) {
    suspend fun loadLibraries(): Result<List<PrivateMediaLibraryDto>> = runCatching {
        val response = api.libraries()
        if (!response.success) error(response.error ?: "Private media libraries unavailable.")
        response.libraries
    }

    suspend fun loadCategories(): Result<List<PrivateMediaCategoryDto>> = runCatching {
        val response = api.categories()
        if (!response.success) error(response.error ?: "Private media categories unavailable.")
        response.categories
    }

    suspend fun loadItems(
        categoryId: String,
        page: Int = 1,
        perPage: Int = 24,
        query: String = "",
    ): Result<PrivateMediaPageDto> = runCatching {
        val response = api.items(categoryId, page, perPage, query.takeIf { it.isNotBlank() })
        if (!response.success) error(response.error ?: "Private media unavailable.")
        response.page ?: PrivateMediaPageDto(error = response.error)
    }

    suspend fun loadDetails(id: String): Result<PrivateMediaDetailsDto> = runCatching {
        val response = api.item(id)
        if (!response.success) error(response.error ?: "Private media details unavailable.")
        response.item ?: error("Private media item unavailable.")
    }

    suspend fun loadPlayback(id: String): Result<PrivateMediaPlaybackResponse> = runCatching {
        val response = api.playback(id)
        if (!response.success && response.embedUrl.isNullOrBlank() && response.streams.isEmpty()) {
            error(response.error ?: "Private media playback unavailable.")
        }
        response
    }
}
