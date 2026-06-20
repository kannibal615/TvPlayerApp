package com.smartvision.svplayer.data.repository

import com.smartvision.svplayer.data.local.dao.FavoriteDao
import com.smartvision.svplayer.data.local.dao.ProgressDao
import com.smartvision.svplayer.data.local.entity.FavoriteEntity
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

object UserContentType {
    const val Live = "live"
    const val Movie = "movie"
    const val Series = "series"
    const val Episode = "episode"
}

class UserContentRepository(
    private val favoriteDao: FavoriteDao,
    private val progressDao: ProgressDao,
) {
    fun observeFavoriteIds(contentType: String): Flow<Set<Int>> =
        favoriteDao.observeByType(contentType).map { favorites ->
            favorites.mapNotNull { it.contentId.toIntOrNull() }.toSet()
        }

    fun observeRecentProgress(limit: Int = 12): Flow<List<PlaybackProgressEntity>> =
        progressDao.observeRecent(limit)

    suspend fun getProgress(contentType: String, contentId: Int): PlaybackProgressEntity? =
        withContext(Dispatchers.IO) {
            progressDao.get(contentType, contentId.toString())
        }

    suspend fun toggleFavorite(contentType: String, contentId: Int) = withContext(Dispatchers.IO) {
        val key = contentId.toString()
        val existing = favoriteDao.get(contentType, key)
        if (existing == null) {
            favoriteDao.upsert(
                FavoriteEntity(
                    contentType = contentType,
                    contentId = key,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        } else {
            favoriteDao.delete(contentType, key)
        }
    }

    suspend fun savePlaybackProgress(
        contentType: String,
        contentId: Int,
        positionMs: Long,
        durationMs: Long,
    ) = withContext(Dispatchers.IO) {
        progressDao.upsert(
            PlaybackProgressEntity(
                contentType = contentType,
                contentId = contentId.toString(),
                positionMs = positionMs.coerceAtLeast(0L),
                durationMs = durationMs.coerceAtLeast(0L),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}
