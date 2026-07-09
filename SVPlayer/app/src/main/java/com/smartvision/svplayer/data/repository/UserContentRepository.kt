package com.smartvision.svplayer.data.repository

import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.data.local.dao.FavoriteDao
import com.smartvision.svplayer.data.local.dao.ProgressDao
import com.smartvision.svplayer.data.local.dao.MediaDao
import com.smartvision.svplayer.data.local.entity.FavoriteEntity
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.domain.model.CategoryHistorySignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

object UserContentType {
    const val Live = "live"
    const val Movie = "movie"
    const val Series = "series"
    const val Episode = "episode"
    const val LocalMedia = "local_media"
}

class UserContentRepository(
    private val accountManager: XtreamAccountManager,
    private val favoriteDao: FavoriteDao,
    private val progressDao: ProgressDao,
    private val mediaDao: MediaDao,
) {
    @Volatile
    private var recentProgressSnapshot: RecentProgressSnapshot? = null

    fun observeFavoriteIds(contentType: String): Flow<Set<Int>> =
        accountManager.activeProfileId.flatMapLatest {
            favoriteDao.observeByType(profileIdFor(contentType), contentType).map { favorites ->
                favorites.mapNotNull { favorite -> favorite.contentId.toIntOrNull() }.toSet()
            }
        }

    fun observeRecentProgress(limit: Int = 12): Flow<List<PlaybackProgressEntity>> =
        accountManager.activeProfileId.flatMapLatest {
            progressDao.observeRecent(profileIdFor(UserContentType.Movie), limit)
        }

    fun getCachedRecentProgressSnapshot(limit: Int = 60): List<PlaybackProgressEntity>? =
        recentProgressSnapshot
            ?.takeIf { it.limit == limit }
            ?.items

    suspend fun getRecentProgressSnapshot(limit: Int = 60): List<PlaybackProgressEntity> =
        withContext(Dispatchers.IO) {
            progressDao.observeRecent(profileIdFor(UserContentType.Movie), limit)
                .first()
                .map { enrichProgress(it) }
                .also { recentProgressSnapshot = RecentProgressSnapshot(limit, it) }
        }

    fun observeHistory(contentType: String, limit: Int = 100): Flow<List<PlaybackProgressEntity>> {
        val minimumPositionMs = if (contentType == UserContentType.Live) 4_999L else 5_000L
        return accountManager.activeProfileId.flatMapLatest {
            progressDao.observeHistory(profileIdFor(contentType), contentType, minimumPositionMs = minimumPositionMs, limit = limit)
        }
    }

    suspend fun getProgress(contentType: String, contentId: Int): PlaybackProgressEntity? =
        withContext(Dispatchers.IO) {
            progressDao.get(profileIdFor(contentType), contentType, contentId.toString())
        }

    suspend fun deleteProgress(contentType: String, contentId: Int) = withContext(Dispatchers.IO) {
        progressDao.delete(profileIdFor(contentType), contentType, contentId.toString())
        recentProgressSnapshot = null
    }

    suspend fun resolveCategorySignals(progressItems: List<PlaybackProgressEntity>): List<CategoryHistorySignal> =
        withContext(Dispatchers.IO) {
            progressItems.mapNotNull { progress ->
                val contentId = progress.contentId.toIntOrNull() ?: return@mapNotNull null
                val categoryId = when (progress.contentType) {
                    UserContentType.Live -> mediaDao.getLiveStream(progress.profileId, contentId)?.categoryId
                    UserContentType.Movie -> mediaDao.getMovie(progress.profileId, contentId)?.categoryId
                    UserContentType.Episode -> {
                        val seriesId = progress.parentContentId?.toIntOrNull()
                            ?: mediaDao.getEpisode(progress.profileId, contentId)?.seriesId
                        seriesId?.let { mediaDao.getSeries(progress.profileId, it)?.categoryId }
                    }
                    UserContentType.Series -> mediaDao.getSeries(progress.profileId, contentId)?.categoryId
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
            UserContentType.Episode -> !progress.title.isFallbackEpisodeTitle(id)
            else -> !progress.title.isNullOrBlank()
        }
        val metadataAlreadyStable = titleAlreadyStable &&
            !progress.imageUrl.isNullOrBlank() &&
            (progress.contentType != UserContentType.Episode ||
                (!progress.parentContentId.isNullOrBlank() && !progress.subtitle.isGenericEpisodeSubtitle()))
        if (metadataAlreadyStable) return@withContext progress
        when (progress.contentType) {
            UserContentType.Live -> mediaDao.getLiveStream(progress.profileId, id)?.let { stream ->
                progress.copy(
                    title = progress.title.takeUnless { it.isFallbackLiveTitle(id) } ?: stream.name,
                    subtitle = progress.subtitle ?: "Live TV",
                    imageUrl = progress.imageUrl ?: stream.logoUrl,
                )
            }
            UserContentType.Movie -> mediaDao.getMovie(progress.profileId, id)?.let { movie ->
                progress.copy(
                    title = progress.title ?: movie.title,
                    subtitle = progress.subtitle ?: "Film",
                    imageUrl = progress.imageUrl ?: movie.posterUrl,
                )
            }
            UserContentType.Episode -> mediaDao.getEpisode(progress.profileId, id)?.let { episode ->
                val series = mediaDao.getSeries(progress.profileId, episode.seriesId)
                val seasonEpisodeLabel = "S${episode.seasonNumber} E${episode.episodeNumber}"
                val episodeSubtitle = listOf(seasonEpisodeLabel, episode.title)
                    .filter { it.isNotBlank() }
                    .joinToString(" - ")
                progress.copy(
                    title = progress.title
                        .takeUnless { it.isFallbackEpisodeTitle(id, episode.title) }
                        ?: series?.title
                        ?: episode.title,
                    subtitle = progress.subtitle
                        .takeUnless { it.isGenericEpisodeSubtitle() }
                        ?: episodeSubtitle,
                    imageUrl = progress.imageUrl ?: series?.posterUrl,
                    parentContentId = progress.parentContentId ?: episode.seriesId.toString(),
                )
            } ?: progress.parentContentId?.toIntOrNull()?.let { seriesId ->
                    mediaDao.getSeries(progress.profileId, seriesId)?.let { series ->
                    progress.copy(
                        title = progress.title
                            .takeUnless { it.isFallbackEpisodeTitle(id) }
                            ?: series.title,
                        subtitle = progress.subtitle
                            .takeUnless { it.isGenericEpisodeSubtitle() }
                            ?: "Serie",
                        imageUrl = progress.imageUrl ?: series.posterUrl,
                        parentContentId = progress.parentContentId,
                    )
                }
            }
            else -> null
        } ?: progress
    }

    suspend fun toggleFavorite(contentType: String, contentId: Int) = withContext(Dispatchers.IO) {
        val key = contentId.toString()
        val profileId = profileIdFor(contentType)
        val existing = favoriteDao.get(profileId, contentType, key)
        if (existing == null) {
            favoriteDao.upsert(
                FavoriteEntity(
                    profileId = profileId,
                    contentType = contentType,
                    contentId = key,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        } else {
            favoriteDao.delete(profileId, contentType, key)
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
        val stableDurationMs = durationMs.coerceAtLeast(0L)
        val profileId = profileIdFor(contentType)
        val existing = progressDao.get(profileId, contentType, contentId.toString())
        val candidate = PlaybackProgressEntity(
            profileId = profileId,
            contentType = contentType,
            contentId = contentId.toString(),
            positionMs = stablePositionMs,
            durationMs = stableDurationMs,
            updatedAt = System.currentTimeMillis(),
            title = title?.takeIf { it.isNotBlank() } ?: existing?.title,
            subtitle = subtitle?.takeIf { it.isNotBlank() } ?: existing?.subtitle,
            imageUrl = imageUrl?.takeIf { it.isNotBlank() } ?: existing?.imageUrl,
            parentContentId = parentContentId?.toString() ?: existing?.parentContentId,
        )
        val enriched = enrichProgress(candidate)
        progressDao.upsert(
            enriched.copy(
                positionMs = stablePositionMs,
                durationMs = stableDurationMs,
                updatedAt = candidate.updatedAt,
            ),
        )
        recentProgressSnapshot = null
    }

    private fun profileIdFor(contentType: String): String =
        if (contentType == UserContentType.LocalMedia) {
            LocalMediaProfileId
        } else {
            accountManager.activeProfileIdOrDefault()
        }
}

private data class RecentProgressSnapshot(
    val limit: Int,
    val items: List<PlaybackProgressEntity>,
)

private const val LocalMediaProfileId = "local_media"

private fun String?.isFallbackEpisodeTitle(contentId: Int, episodeTitle: String? = null): Boolean {
    val value = this?.trim() ?: return true
    if (value.isBlank()) return true
    if (value.equals("Episode $contentId", ignoreCase = true)) return true
    if (value.equals("Series", ignoreCase = true) || value.equals("Serie", ignoreCase = true)) return true
    return !episodeTitle.isNullOrBlank() && value.equals(episodeTitle.trim(), ignoreCase = true)
}

private fun String?.isGenericEpisodeSubtitle(): Boolean {
    val value = this?.trim() ?: return true
    if (value.isBlank()) return true
    return value.equals("Episode", ignoreCase = true) ||
        value.equals("Series", ignoreCase = true) ||
        value.equals("Serie", ignoreCase = true)
}

private fun String?.isFallbackLiveTitle(contentId: Int): Boolean {
    val value = this?.trim() ?: return true
    if (value.isBlank()) return true
    return value.equals("Chaine $contentId", ignoreCase = true) ||
        value.equals("Chaîne $contentId", ignoreCase = true)
}
