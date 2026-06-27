package com.smartvision.svplayer.data.repository

import com.smartvision.svplayer.data.local.dao.FavoriteDao
import com.smartvision.svplayer.data.local.dao.ProgressDao
import com.smartvision.svplayer.data.local.dao.MediaDao
import com.smartvision.svplayer.data.local.entity.FavoriteEntity
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.domain.model.CategoryHistorySignal
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

    fun observeHistory(contentType: String, limit: Int = 100): Flow<List<PlaybackProgressEntity>> {
        val minimumPositionMs = if (contentType == UserContentType.Live) 4_999L else 5_000L
        return progressDao.observeHistory(contentType, minimumPositionMs = minimumPositionMs, limit = limit)
    }

    suspend fun getProgress(contentType: String, contentId: Int): PlaybackProgressEntity? =
        withContext(Dispatchers.IO) {
            progressDao.get(contentType, contentId.toString())
        }

    suspend fun deleteProgress(contentType: String, contentId: Int) = withContext(Dispatchers.IO) {
        progressDao.delete(contentType, contentId.toString())
    }

    suspend fun resolveCategorySignals(progressItems: List<PlaybackProgressEntity>): List<CategoryHistorySignal> =
        withContext(Dispatchers.IO) {
            progressItems.mapNotNull { progress ->
                val contentId = progress.contentId.toIntOrNull() ?: return@mapNotNull null
                val categoryId = when (progress.contentType) {
                    UserContentType.Live -> mediaDao.getLiveStream(contentId)?.categoryId
                    UserContentType.Movie -> mediaDao.getMovie(contentId)?.categoryId
                    UserContentType.Episode -> {
                        val seriesId = progress.parentContentId?.toIntOrNull()
                            ?: mediaDao.getEpisode(contentId)?.seriesId
                        seriesId?.let { mediaDao.getSeries(it)?.categoryId }
                    }
                    UserContentType.Series -> mediaDao.getSeries(contentId)?.categoryId
                    else -> null
                }
                categoryId?.takeIf { it.isNotBlank() }?.let {
                    CategoryHistorySignal(categoryId = it, updatedAt = progress.updatedAt)
                }
            }
        }

    suspend fun enrichProgress(progress: PlaybackProgressEntity): PlaybackProgressEntity = withContext(Dispatchers.IO) {
        val id = progress.contentId.toIntOrNull() ?: return@withContext progress
        val titleAlreadyStable = when (progress.contentType) {
            UserContentType.Live -> !progress.title.isFallbackLiveTitle(id)
            else -> !progress.title.isNullOrBlank()
        }
        if (titleAlreadyStable && !progress.imageUrl.isNullOrBlank()) return@withContext progress
        when (progress.contentType) {
            UserContentType.Live -> mediaDao.getLiveStream(id)?.let { stream ->
                progress.copy(
                    title = progress.title.takeUnless { it.isFallbackLiveTitle(id) } ?: stream.name,
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
        if (contentType == UserContentType.Live && positionMs < 5_000L) {
            return@withContext
        }
        val stablePositionMs = positionMs.coerceAtLeast(0L)
        progressDao.upsert(
            PlaybackProgressEntity(
                contentType = contentType,
                contentId = contentId.toString(),
                positionMs = stablePositionMs,
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

private fun String?.isFallbackLiveTitle(contentId: Int): Boolean {
    val value = this?.trim() ?: return true
    if (value.isBlank()) return true
    return value.equals("Chaine $contentId", ignoreCase = true) ||
        value.equals("Chaîne $contentId", ignoreCase = true)
}
