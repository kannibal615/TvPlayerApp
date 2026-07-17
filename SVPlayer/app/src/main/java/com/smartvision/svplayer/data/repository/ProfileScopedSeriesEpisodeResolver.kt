package com.smartvision.svplayer.data.repository

internal suspend fun <T> getOrFetchProfileScopedSeriesEpisodes(
    capturedProfileId: String,
    seriesId: Int,
    activeProfileId: () -> String,
    loadLocal: suspend (profileId: String, seriesId: Int) -> List<T>,
    fetchRemote: suspend (profileId: String, seriesId: Int) -> List<T>,
    persist: suspend (profileId: String, seriesId: Int, episodes: List<T>) -> Unit,
): List<T> {
    val local = loadLocal(capturedProfileId, seriesId)
    if (local.isNotEmpty()) {
        return local.takeIf { activeProfileId() == capturedProfileId }.orEmpty()
    }
    val remote = fetchRemote(capturedProfileId, seriesId)
    if (remote.isNotEmpty()) {
        persist(capturedProfileId, seriesId, remote)
    }
    return remote.takeIf { activeProfileId() == capturedProfileId }.orEmpty()
}
