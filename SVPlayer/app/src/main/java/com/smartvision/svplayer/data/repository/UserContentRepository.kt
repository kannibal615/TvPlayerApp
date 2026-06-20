package com.smartvision.svplayer.data.repository

import com.smartvision.svplayer.data.local.dao.FavoriteDao
import com.smartvision.svplayer.data.local.dao.ProgressDao
import com.smartvision.svplayer.data.local.dao.MediaDao
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
    private val mediaDao: MediaDao,
) {
    fun observeFavoriteIds(contentType: String): Flow<Set<Int>> =
        favoriteDao.observeByType(contentType).map { favorites ->
            favorites.mapNotNull { it.contentId.toIntOrNull() }.toSet()
        }

    fun observeRecentProgress(limit: Int = 12): Flow<List<PlaybackProgressEntity>> =
        progressDao.observeRecent(limit)

    fun observeHistory(contentType: String, limit: Int = 100): Flow<List<PlaybackProgressEntity>> =
        progressDao.observeHistory(contentType, minimumPositionMs = 5_000L, limit = limit)

    suspend fun getProgress(contentType: String, contentId: Int): PlaybackProgressEntity? =
        withContext(Dispatchers.IO) {
            progressDao.get(contentType, contentId.toString())
        }

    suspend fun enrichProgress(progress: PlaybackProgressEntity): PlaybackProgressEntity = withContext(Dispatchers.IO) {
        if (!progress.title.isNullOrBlank() && !progress.imageUrl.isNullOrBlank()) return@withContext progress
        val id = progress.contentId.toIntOrNull() ?: return@withContext progress
        when (progress.contentType) {
            UserContentType.Live -> mediaDao.getLiveStream(id)?.let { stream ->
                progress.copy(
                    title = progress.title ?: stream.name,
                    subtitle = progress.subtitle ?: "Live TV",
                    imageUrl = progress.imageUrl ?: stream.logoUrl,
                )
            }
            UserContentType.Movie -> mediaDao.getMovie(id)?.let { movie ->
                progress.copy(
                    title = progress.title ?: movie.title,
                    subtitle = progress.subtitle ?: "Film",
                    imageUrl = progress.imageUrl ?: movie.posterUrl,
                )
            }
            UserContentType.Episode -> mediaDao.getEpisode(id)?.let { episode ->
                val series = mediaDao.getSeries(episode.seriesId)
                progress.copy(
                    title = progress.title ?: series?.title ?: episode.title,
                    subtitle = progress.subtitle ?: "S${episode.seasonNumber} E${episode.episodeNumber}",
                    imageUrl = progress.imageUrl ?: series?.posterUrl,
                    parentContentId = progress.parentContentId ?: episode.seriesId.toString(),
                )
            }
            else -> null
        } ?: progress
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
        title: String? = null,
        subtitle: String? = null,
        imageUrl: String? = null,
        parentContentId: Int? = null,
    ) = withContext(Dispatchers.IO) {
        progressDao.upsert(
            PlaybackProgressEntity(
                contentType = contentType,
                contentId = contentId.toString(),
                positionMs = positionMs.coerceAtLeast(0L),
                durationMs = durationMs.coerceAtLeast(0L),
                updatedAt = System.currentTimeMillis(),
                title = title?.takeIf { it.isNotBlank() },
                subtitle = subtitle?.takeIf { it.isNotBlank() },
                imageUrl = imageUrl?.takeIf { it.isNotBlank() },
                parentContentId = parentContentId?.toString(),
            ),
        )
    }
}
