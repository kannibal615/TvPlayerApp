package com.smartvision.svplayer.data.home

import android.os.SystemClock
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.data.diagnostics.PerformanceDiagnosticRecorder
import com.smartvision.svplayer.data.local.dao.MediaDao
import com.smartvision.svplayer.data.local.dao.SyncStateDao
import com.smartvision.svplayer.data.local.entity.HomeTrendingPreviewCacheEntity
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.mock.HomePreviewMode
import com.smartvision.svplayer.data.mock.HomeVisualStyle
import com.smartvision.svplayer.data.models.XtreamSeriesEpisode
import com.smartvision.svplayer.data.network.NetworkActivityTracker
import com.smartvision.svplayer.data.network.NetworkActivityType
import com.smartvision.svplayer.data.remote.XtreamUrlFactory
import com.smartvision.svplayer.data.repository.XtreamRepository
import com.smartvision.svplayer.data.tmdb.TmdbRepository
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.domain.model.TrendingCatalogItem
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

data class HomeTrendingSnapshot(
    val movies: List<ContinueItem>,
    val series: List<ContinueItem>,
)

data class HomeTrendingPreparedPreview(
    val contentType: String,
    val contentId: Int,
    val posterUrl: String?,
    val backdropUrl: String?,
    val durationLabel: String?,
    val durationMs: Long?,
    val previewUrl: String?,
    val previewStartPositionMs: Long,
    val previewFallbackStartPositionMs: Long,
    val sampleLabel: String?,
    val backdropAvailable: Boolean,
    val previewAvailable: Boolean,
) {
    fun applyTo(item: ContinueItem): ContinueItem =
        item.copy(
            imageUrl = item.imageUrl ?: posterUrl,
            previewImageUrl = backdropUrl ?: posterUrl ?: item.imageUrl,
            remaining = durationLabel ?: sampleLabel ?: item.remaining,
            previewUrl = previewUrl,
            previewYoutubeKey = null,
            previewStartPositionMs = previewStartPositionMs,
            previewFallbackStartPositionMs = previewFallbackStartPositionMs,
            previewDurationLabel = durationLabel ?: sampleLabel,
            previewDurationMs = durationMs,
            previewPrepared = true,
            previewBackdropAvailable = backdropAvailable,
            previewMode = if (previewAvailable) HomePreviewMode.TrendSegments else HomePreviewMode.None,
        )
}

class HomeContentRepository(
    private val catalogRepository: CatalogRepository,
    private val accountManager: XtreamAccountManager,
    private val syncStateDao: SyncStateDao,
    private val mediaDao: MediaDao,
    private val xtreamRepository: XtreamRepository,
    private val tmdbRepository: TmdbRepository,
    private val settingsRepository: SettingsRepository,
    private val urlFactory: XtreamUrlFactory,
    private val networkActivityTracker: NetworkActivityTracker,
) {
    @Volatile
    private var cachedTrending: CachedHomeTrending? = null
    @Volatile
    private var previewCacheCleanedForLastSync: Long? = null

    fun getLastCachedTrendingSnapshot(): HomeTrendingSnapshot? = cachedTrending?.snapshot

    suspend fun getCachedTrending(): HomeTrendingSnapshot? {
        val key = currentCacheKey()
        return cachedTrending
            ?.takeIf { it.key == key }
            ?.snapshot
            ?.also {
                PerformanceDiagnosticRecorder.record(
                    sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
                    event = "home_content_cache_hit",
                    fields = mapOf("movies" to it.movies.size, "series" to it.series.size),
                )
            }
    }

    fun invalidateTrending() {
        cachedTrending = null
    }

    suspend fun cacheEmptyTrending() {
        cachedTrending = CachedHomeTrending(
            key = currentCacheKey(),
            snapshot = HomeTrendingSnapshot(movies = emptyList(), series = emptyList()),
        )
        PerformanceDiagnosticRecorder.record(
            sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
            event = "home_content_empty_trending_cached",
        )
    }

    suspend fun preloadTrending(): HomeTrendingSnapshot = refreshTrending(forceRefresh = true)

    suspend fun refreshTrendingMovies(forceRefresh: Boolean = false): List<ContinueItem> {
        val startedAt = SystemClock.elapsedRealtime()
        val key = currentCacheKey()
        if (!forceRefresh) {
            cachedTrending
                ?.takeIf { it.key == key }
                ?.snapshot
                ?.movies
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }
        if (key.source != PlaylistSource.Xtream.storageValue || key.accountSignature.isBlank()) {
            return updateCachedTrending(key = key, movies = emptyList()).movies
        }
        return withContext(Dispatchers.IO) {
            runCatching { catalogRepository.getTrendingMovieItems(HomeTrendingPolicy.SectionLimit) }
                .getOrDefault(emptyList())
                .also { candidates ->
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                        event = "home_trending_movie_saved_candidates",
                        startedAtMs = startedAt,
                        fields = mapOf("candidates" to candidates.size),
                    )
                }
                .mapNotNull { it.toMovieTrendItem() }
                .take(HomeTrendingPolicy.SectionLimit)
                .let { movies -> updateCachedTrending(key = key, movies = movies).movies }
        }
    }

    suspend fun refreshTrendingSeries(forceRefresh: Boolean = false): List<ContinueItem> {
        val startedAt = SystemClock.elapsedRealtime()
        val key = currentCacheKey()
        if (!forceRefresh) {
            cachedTrending
                ?.takeIf { it.key == key }
                ?.snapshot
                ?.series
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }
        if (key.source != PlaylistSource.Xtream.storageValue || key.accountSignature.isBlank()) {
            return updateCachedTrending(key = key, series = emptyList()).series
        }
        return withContext(Dispatchers.IO) {
            runCatching { catalogRepository.getTrendingSeriesItems(HomeTrendingPolicy.SectionLimit) }
                .getOrDefault(emptyList())
                .also { candidates ->
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                        event = "home_trending_series_saved_candidates",
                        startedAtMs = startedAt,
                        fields = mapOf("candidates" to candidates.size),
                    )
                }
                .mapNotNull { it.toSeriesTrendItem() }
                .take(HomeTrendingPolicy.SectionLimit)
                .let { series -> updateCachedTrending(key = key, series = series).series }
        }
    }

    suspend fun refreshTrending(forceRefresh: Boolean = false): HomeTrendingSnapshot {
        // PERF_DIAG: captures the expensive Home trends path: config, candidates, details and filters.
        val startedAt = SystemClock.elapsedRealtime()
        val key = currentCacheKey()
        if (!forceRefresh) {
            cachedTrending
                ?.takeIf { it.key == key }
                ?.snapshot
                ?.let {
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_HOME_STATE,
                        event = "home_content_refresh_reused_cache",
                        startedAtMs = startedAt,
                        fields = mapOf("movies" to it.movies.size, "series" to it.series.size),
                    )
                    return it
                }
        }
        if (key.source != PlaylistSource.Xtream.storageValue || key.accountSignature.isBlank()) {
            return HomeTrendingSnapshot(emptyList(), emptyList())
                .also {
                    cachedTrending = CachedHomeTrending(key, it)
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                        event = "home_content_refresh_skipped",
                        startedAtMs = startedAt,
                        fields = mapOf("source" to key.source, "hasAccount" to key.accountSignature.isNotBlank()),
                    )
                }
        }
        val work = networkActivityTracker.begin(
            id = "home-trending-${System.currentTimeMillis()}",
            title = "Home trends",
            type = NetworkActivityType.Home,
            message = "Preparing trending rows",
            progressPercent = 0,
        )
        return withContext(Dispatchers.IO) {
            try {
                coroutineScope {
                    val movies = async {
                        val candidatesStart = SystemClock.elapsedRealtime()
                        runCatching { catalogRepository.getTrendingMovieItems(HomeTrendingPolicy.SectionLimit) }
                            .getOrDefault(emptyList())
                            .also { candidates ->
                                PerformanceDiagnosticRecorder.recordDuration(
                                    sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                                    event = "home_trending_movie_candidates",
                                    startedAtMs = candidatesStart,
                                    fields = mapOf("candidates" to candidates.size),
                                )
                            }
                            .mapNotNull { it.toMovieTrendItem() }
                            .take(HomeTrendingPolicy.SectionLimit)
                    }
                    val series = async {
                        val candidatesStart = SystemClock.elapsedRealtime()
                        runCatching { catalogRepository.getTrendingSeriesItems(HomeTrendingPolicy.SectionLimit) }
                            .getOrDefault(emptyList())
                            .also { candidates ->
                                PerformanceDiagnosticRecorder.recordDuration(
                                    sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                                    event = "home_trending_series_candidates",
                                    startedAtMs = candidatesStart,
                                    fields = mapOf("candidates" to candidates.size),
                                )
                            }
                            .mapNotNull { it.toSeriesTrendItem() }
                            .take(HomeTrendingPolicy.SectionLimit)
                    }
                    HomeTrendingSnapshot(
                        movies = movies.await(),
                        series = series.await(),
                    ).also {
                        cachedTrending = CachedHomeTrending(key, it)
                        val total = it.movies.size + it.series.size
                        work.update(currentItems = total, totalItems = HomeTrendingPolicy.SectionLimit * 2, progressPercent = 100)
                        work.complete("Home trends ready")
                        PerformanceDiagnosticRecorder.recordDuration(
                            sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                            event = "home_trending_snapshot_ready",
                            startedAtMs = startedAt,
                            fields = mapOf("movies" to it.movies.size, "series" to it.series.size),
                        )
                    }
                }
            } catch (error: Throwable) {
                work.fail(error.message ?: error.javaClass.simpleName)
                throw error
            }
        }
    }

    private fun beginTrendingPreviewWork(contentType: String, contentId: Int) =
        networkActivityTracker.begin(
            id = "home-trending-preview-$contentType-$contentId-${System.currentTimeMillis()}",
            title = "Trending preview",
            type = NetworkActivityType.Home,
            section = contentType,
            message = "Preparing preview metadata",
            progressPercent = 0,
        )

    suspend fun prepareTrendingPreview(
        contentType: String,
        contentId: Int,
        fallbackPosterUrl: String?,
    ): HomeTrendingPreparedPreview? = withContext(Dispatchers.IO) {
        val startedAt = SystemClock.elapsedRealtime()
        val key = currentCacheKey()
        if (key.source != PlaylistSource.Xtream.storageValue || key.accountSignature.isBlank()) {
            return@withContext null
        }
        cleanPreviewCacheIfNeeded(key.lastSync)
        mediaDao.getHomeTrendingPreviewCache(key.profileId, contentType, contentId, key.lastSync)
            ?.takeIf { it.hasReusableXtreamPreviewCache() }
            ?.toPreparedPreview()
            ?.also { prepared ->
                PerformanceDiagnosticRecorder.recordDuration(
                    sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                    event = "home_trending_preview_cache_hit",
                    startedAtMs = startedAt,
                    fields = mapOf(
                        "contentType" to contentType,
                        "contentId" to contentId,
                        "hasBackdrop" to prepared.backdropAvailable,
                        "hasPreview" to prepared.previewAvailable,
                    ),
                )
            }
            ?.let { return@withContext it }

        val previewWork = beginTrendingPreviewWork(contentType, contentId)
        val entity = runCatching {
            val settings = settingsRepository.settings.first()
            when (contentType) {
                TrendingMovieType -> buildMoviePreviewCache(contentId, fallbackPosterUrl, key.lastSync, settings)
                TrendingSeriesType -> buildSeriesPreviewCache(contentId, fallbackPosterUrl, key.lastSync, settings)
                else -> null
            }
        }.onFailure { error ->
            previewWork.fail(error.message ?: error.javaClass.simpleName)
            PerformanceDiagnosticRecorder.recordDuration(
                sheet = PerformanceDiagnosticRecorder.SHEET_ERRORS,
                event = "home_trending_preview_prepare_failed",
                startedAtMs = startedAt,
                fields = mapOf("contentType" to contentType, "contentId" to contentId),
                error = error,
            )
        }.getOrNull() ?: run {
            previewWork.complete("Preview unavailable")
            return@withContext null
        }

        mediaDao.upsertHomeTrendingPreviewCache(entity)
        entity.toPreparedPreview().also { prepared ->
            previewWork.update(progressPercent = 100)
            previewWork.complete("Preview metadata ready")
            PerformanceDiagnosticRecorder.recordDuration(
                sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                event = "home_trending_preview_prepared",
                startedAtMs = startedAt,
                fields = mapOf(
                    "contentType" to contentType,
                    "contentId" to contentId,
                    "hasBackdrop" to prepared.backdropAvailable,
                    "hasPreview" to prepared.previewAvailable,
                    "durationMs" to (prepared.durationMs ?: 0L),
                ),
            )
        }
    }

    private suspend fun currentCacheKey(): HomeTrendingCacheKey =
        HomeTrendingCacheKey(
            source = accountManager.activePlaylistSource.value.storageValue,
            accountSignature = accountManager.accounts.value.joinToString("|") { account ->
                "${account.id}:${account.host}:${account.username}:${account.password.hashCode()}"
            },
            profileId = accountManager.activeProfileIdOrDefault(),
            lastSync = syncStateDao.get(accountManager.activeProfileIdOrDefault())?.lastSync ?: 0L,
        )

    private fun updateCachedTrending(
        key: HomeTrendingCacheKey,
        movies: List<ContinueItem>? = null,
        series: List<ContinueItem>? = null,
    ): HomeTrendingSnapshot {
        val existing = cachedTrending
            ?.takeIf { it.key == key }
            ?.snapshot
        val snapshot = HomeTrendingSnapshot(
            movies = movies ?: existing?.movies.orEmpty(),
            series = series ?: existing?.series.orEmpty(),
        )
        cachedTrending = CachedHomeTrending(key, snapshot)
        return snapshot
    }

    private suspend fun cleanPreviewCacheIfNeeded(lastSync: Long) {
        if (previewCacheCleanedForLastSync == lastSync) return
        mediaDao.deleteStaleHomeTrendingPreviewCache(accountManager.activeProfileIdOrDefault(), lastSync)
        previewCacheCleanedForLastSync = lastSync
    }

    private suspend fun buildMoviePreviewCache(
        contentId: Int,
        fallbackPosterUrl: String?,
        lastSync: Long,
        settings: PlayerSettings,
    ): HomeTrendingPreviewCacheEntity {
        val details = xtreamRepository.getMovieDetails(contentId)
        val tmdb = tmdbRepository.enrichMovie(
            contentId = contentId,
            title = details.title,
            year = details.releaseDate,
            language = settings.language,
            includeAdult = !settings.parentalControlEnabled,
        )
        val durationMs = tmdb?.runtimeMinutes?.takeIf { it > 0 }?.times(60_000L)
            ?: details.duration.parseDurationMs()
        val startPositionMs = durationMs.previewStartAt(PreviewStartRatio)
        val posterUrl = tmdb?.posterUrl ?: details.posterUrl ?: fallbackPosterUrl
        val backdropUrl = tmdb?.backdropUrl ?: details.backdropUrl?.takeIf { it.isNotBlank() }
        val localMovie = mediaDao.getMovie(accountManager.activeProfileIdOrDefault(), contentId)
        return HomeTrendingPreviewCacheEntity(
            profileId = accountManager.activeProfileIdOrDefault(),
            contentType = TrendingMovieType,
            contentId = contentId,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            durationLabel = durationMs?.formatDurationLabel() ?: details.duration?.takeIf { it.isNotBlank() },
            durationMs = durationMs,
            previewKind = if (localMovie != null) PreviewKindMovie else PreviewKindNone,
            previewContentId = localMovie?.streamId,
            previewExtension = localMovie?.containerExtension,
            trailerKey = null,
            previewStartPositionMs = startPositionMs,
            sampleLabel = null,
            backdropState = if (!backdropUrl.isNullOrBlank()) BackdropAvailable else BackdropMissing,
            previewState = if (localMovie != null) PreviewAvailable else PreviewUnavailable,
            preparedAt = System.currentTimeMillis(),
            lastSync = lastSync,
        )
    }

    private suspend fun buildSeriesPreviewCache(
        contentId: Int,
        fallbackPosterUrl: String?,
        lastSync: Long,
        settings: PlayerSettings,
    ): HomeTrendingPreviewCacheEntity {
        val details = xtreamRepository.getSeriesDetails(contentId)
        val tmdb = tmdbRepository.enrichSeries(
            contentId = contentId,
            title = details.title,
            year = details.releaseDate,
            language = settings.language,
            includeAdult = !settings.parentalControlEnabled,
        )
        val firstEpisode = xtreamRepository.getSeriesEpisodes(contentId)
            .sortedWith(compareBy<XtreamSeriesEpisode> { it.seasonNumber }.thenBy { it.episodeNumber })
            .firstOrNull()
        val durationMs = tmdb?.episodeRunTimeMinutes?.takeIf { it > 0 }?.times(60_000L)
            ?: details.episodeRunTime.parseDurationMs()
            ?: firstEpisode?.duration.parseDurationMs()
        val sampleLabel = firstEpisode?.let { "S${it.seasonNumber} E${it.episodeNumber}" }
        val posterUrl = tmdb?.posterUrl ?: details.coverUrl ?: fallbackPosterUrl
        val backdropUrl = tmdb?.backdropUrl ?: details.backdropUrl?.takeIf { it.isNotBlank() }
        return HomeTrendingPreviewCacheEntity(
            profileId = accountManager.activeProfileIdOrDefault(),
            contentType = TrendingSeriesType,
            contentId = contentId,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            durationLabel = durationMs?.formatDurationLabel(),
            durationMs = durationMs,
            previewKind = if (firstEpisode != null) PreviewKindEpisode else PreviewKindNone,
            previewContentId = firstEpisode?.episodeId,
            previewExtension = firstEpisode?.containerExtension,
            trailerKey = null,
            previewStartPositionMs = durationMs.previewStartAt(PreviewStartRatio),
            sampleLabel = durationMs?.formatDurationLabel() ?: sampleLabel,
            backdropState = if (!backdropUrl.isNullOrBlank()) BackdropAvailable else BackdropMissing,
            previewState = if (firstEpisode != null) PreviewAvailable else PreviewUnavailable,
            preparedAt = System.currentTimeMillis(),
            lastSync = lastSync,
        )
    }

    private fun HomeTrendingPreviewCacheEntity.toPreparedPreview(): HomeTrendingPreparedPreview {
        val previewUrl = when (previewKind) {
            PreviewKindMovie -> previewContentId?.let { id ->
                urlFactory.movie(id, previewExtension.orEmpty().ifBlank { "mp4" })
            }
            PreviewKindEpisode -> previewContentId?.let { id ->
                urlFactory.episode(id, previewExtension.orEmpty().ifBlank { "mp4" })
            }
            else -> null
        }.takeIf { previewState == PreviewAvailable }
        val fallbackStart = durationMs.previewStartAt(PreviewFallbackStartRatio)
        return HomeTrendingPreparedPreview(
            contentType = contentType,
            contentId = contentId,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            durationLabel = durationLabel,
            durationMs = durationMs,
            previewUrl = previewUrl,
            previewStartPositionMs = previewStartPositionMs,
            previewFallbackStartPositionMs = fallbackStart,
            sampleLabel = sampleLabel,
            backdropAvailable = backdropState == BackdropAvailable && !backdropUrl.isNullOrBlank(),
            previewAvailable = previewUrl != null,
        )
    }

    private fun HomeTrendingPreviewCacheEntity.hasReusableXtreamPreviewCache(): Boolean =
        previewKind == PreviewKindMovie || previewKind == PreviewKindEpisode ||
            (previewKind == PreviewKindNone && previewContentId == null && previewExtension == null)

    private fun TrendingCatalogItem.toMovieTrendItem(): ContinueItem? {
        // PERF_DIAG: per-candidate details are intentionally data-only; URLs/secrets are not written.
        val startedAt = SystemClock.elapsedRealtime()
        PerformanceDiagnosticRecorder.recordDuration(
            sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
            event = "home_trending_movie_accepted",
            startedAtMs = startedAt,
            fields = mapOf(
                "contentId" to contentId,
                "title" to title,
                "category" to categoryName,
                "rating" to rating,
                "year" to year,
                "hasPoster" to !posterUrl.isNullOrBlank(),
                "hasPreviewUrl" to false,
            ),
        )
        return ContinueItem(
            id = "movie:$contentId",
            title = title.cleanHistoryTitle(),
            meta = categoryName.takeIf { it.isNotBlank() } ?: mediaTypeLabel,
            remaining = rating?.let { "$it/10" } ?: year?.take(4).orEmpty(),
            progress = 0f,
            visualStyle = HomeVisualStyle.Cinema,
            imageUrl = posterUrl,
            previewImageUrl = posterUrl,
            mediaType = "FILM",
            previewUrl = null,
            previewMode = HomePreviewMode.None,
        )
    }

    private fun TrendingCatalogItem.toSeriesTrendItem(): ContinueItem? {
        // PERF_DIAG: per-candidate details are intentionally data-only; URLs/secrets are not written.
        val startedAt = SystemClock.elapsedRealtime()
        PerformanceDiagnosticRecorder.recordDuration(
            sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
            event = "home_trending_series_accepted",
            startedAtMs = startedAt,
            fields = mapOf(
                "contentId" to contentId,
                "title" to title,
                "category" to categoryName,
                "rating" to rating,
                "year" to year,
                "hasPoster" to !posterUrl.isNullOrBlank(),
                "hasPreviewUrl" to false,
            ),
        )
        return ContinueItem(
            id = "series:$contentId",
            title = title.cleanHistoryTitle(),
            meta = categoryName.takeIf { it.isNotBlank() } ?: mediaTypeLabel,
            remaining = year?.take(4) ?: rating?.let { "$it/10" }.orEmpty(),
            progress = 0f,
            visualStyle = HomeVisualStyle.Series,
            imageUrl = posterUrl,
            previewImageUrl = posterUrl,
            mediaType = "SERIE",
            previewUrl = null,
            previewMode = HomePreviewMode.None,
        )
    }
}

private val TrendingCatalogItem.mediaTypeLabel: String
    get() = when (contentType) {
        "movie" -> "Movie"
        "series" -> "Series"
        else -> "Media"
    }

private data class CachedHomeTrending(
    val key: HomeTrendingCacheKey,
    val snapshot: HomeTrendingSnapshot,
)

private data class HomeTrendingCacheKey(
    val profileId: String,
    val source: String,
    val accountSignature: String,
    val lastSync: Long,
)

private fun String.cleanHistoryTitle(): String =
    replace(Regex("\\s+"), " ")
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" 4K", "", ignoreCase = true)
        .trim()

private fun String?.parseDurationMs(): Long? {
    val value = this?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
    if (":" in value) {
        val parts = value.split(":")
            .mapNotNull { it.trim().toLongOrNull() }
        return when (parts.size) {
            3 -> ((parts[0] * 3_600L) + (parts[1] * 60L) + parts[2]) * 1_000L
            2 -> ((parts[0] * 60L) + parts[1]) * 1_000L
            else -> null
        }
    }
    val hours = Regex("(\\d+)\\s*h").find(value)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
    val minutes = Regex("(\\d+)\\s*(m|min)").find(value)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
    val seconds = Regex("(\\d+)\\s*s").find(value)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
    if (hours > 0L || minutes > 0L || seconds > 0L) {
        return ((hours * 3_600L) + (minutes * 60L) + seconds) * 1_000L
    }
    val numeric = value.filter { it.isDigit() }.toLongOrNull() ?: return null
    val secondsValue = if (numeric <= 360L) numeric * 60L else numeric
    return secondsValue * 1_000L
}

private fun Long?.previewStartAt(ratio: Double): Long =
    this?.takeIf { it > 0L }
        ?.let { (it * ratio).roundToLong().coerceIn(0L, (it - 1_000L).coerceAtLeast(0L)) }
        ?: 0L

private fun Long.formatDurationLabel(): String {
    val totalMinutes = (this / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}min"
        hours > 0L -> "${hours}h"
        else -> "${minutes}min"
    }
}

private const val TrendingMovieType = "movie"
private const val TrendingSeriesType = "series"
private const val PreviewKindMovie = "movie"
private const val PreviewKindEpisode = "episode"
private const val PreviewKindNone = "none"
private const val BackdropAvailable = "available"
private const val BackdropMissing = "missing"
private const val PreviewAvailable = "available"
private const val PreviewUnavailable = "unavailable"
private const val PreviewStartRatio = 0.15
private const val PreviewFallbackStartRatio = 0.30
