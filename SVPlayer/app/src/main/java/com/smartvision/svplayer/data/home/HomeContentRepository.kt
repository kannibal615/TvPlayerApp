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
import com.smartvision.svplayer.data.remote.XtreamUrlFactory
import com.smartvision.svplayer.data.repository.XtreamRepository
import com.smartvision.svplayer.data.tmdb.TmdbMatcher
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
    fun applyTo(
        item: ContinueItem,
        promoteCardArtwork: Boolean = false,
    ): ContinueItem =
        item.copy(
            imageUrl = if (promoteCardArtwork) {
                backdropUrl.takeIf { backdropAvailable }
                    ?: posterUrl
                    ?: item.imageUrl
            } else {
                item.imageUrl
            },
            previewImageUrl = backdropUrl.takeIf { backdropAvailable },
            remaining = durationLabel ?: item.remaining,
            previewUrl = previewUrl,
            previewYoutubeKey = null,
            previewStartPositionMs = previewStartPositionMs,
            previewFallbackStartPositionMs = previewFallbackStartPositionMs,
            previewDurationLabel = durationLabel,
            secondaryLabel = sampleLabel ?: item.secondaryLabel,
            previewDurationMs = durationMs,
            previewPrepared = true,
            previewBackdropAvailable = backdropAvailable && !backdropUrl.isNullOrBlank(),
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
) {
    @Volatile
    private var cachedTrending: CachedHomeTrending? = null
    @Volatile
    private var previewCacheCleanedForKey: Pair<String, Long>? = null

    fun getLastCachedTrendingSnapshot(profileId: String): HomeTrendingSnapshot? = cachedTrending
        ?.takeIf { it.key.profileId == profileId }
        ?.snapshot

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
            runCatching { catalogRepository.getTrendingMovieItems(HomeTrendingPolicy.CandidateLimit, key.profileId) }
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
                .let { candidates ->
                    val eligible = prepareEligibleTrendingMovies(candidates)
                    updateCachedTrending(key = key, movies = eligible).movies
                }
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
            runCatching { catalogRepository.getTrendingSeriesItems(HomeTrendingPolicy.SectionLimit, key.profileId) }
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
                .let { series ->
                    val prepared = prepareInitialTrendingSeries(key, series)
                    updateCachedTrending(key = key, series = prepared).series
                }
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
        return withContext(Dispatchers.IO) {
            coroutineScope {
                    val movies = async {
                        val candidatesStart = SystemClock.elapsedRealtime()
                        runCatching { catalogRepository.getTrendingMovieItems(HomeTrendingPolicy.CandidateLimit, key.profileId) }
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
                    }
                    val series = async {
                        val candidatesStart = SystemClock.elapsedRealtime()
                        runCatching { catalogRepository.getTrendingSeriesItems(HomeTrendingPolicy.SectionLimit, key.profileId) }
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
                    val baseSnapshot = HomeTrendingSnapshot(
                        movies = movies.await(),
                        series = series.await(),
                    )
                    val preparedSnapshot = HomeTrendingSnapshot(
                        movies = prepareEligibleTrendingMovies(baseSnapshot.movies),
                        series = prepareInitialTrendingSeries(key, baseSnapshot.series),
                    )
                    preparedSnapshot.also {
                        cachedTrending = CachedHomeTrending(key, preparedSnapshot)
                        PerformanceDiagnosticRecorder.recordDuration(
                            sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                            event = "home_trending_snapshot_ready",
                            startedAtMs = startedAt,
                            fields = mapOf("movies" to it.movies.size, "series" to it.series.size),
                        )
                    }
            }
        }
    }

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
        if (key.tmdbApiEnabled) cleanPreviewCacheIfNeeded(key.lastSync)
        if (key.tmdbApiEnabled) mediaDao.getHomeTrendingPreviewCache(key.profileId, contentType, contentId, key.lastSync)
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

        val settings = settingsRepository.settings.first()
        val entity = runCatching {
            when (contentType) {
                TrendingMovieType -> buildMoviePreviewCache(contentId, fallbackPosterUrl, key.lastSync, settings)
                TrendingSeriesType -> buildSeriesPreviewCache(contentId, fallbackPosterUrl, key.lastSync, settings)
                else -> null
            }
        }.onFailure { error ->
            PerformanceDiagnosticRecorder.recordDuration(
                sheet = PerformanceDiagnosticRecorder.SHEET_ERRORS,
                event = "home_trending_preview_prepare_failed",
                startedAtMs = startedAt,
                fields = mapOf("contentType" to contentType, "contentId" to contentId),
                error = error,
            )
        }.getOrNull() ?: return@withContext null

        if (key.tmdbApiEnabled) mediaDao.upsertHomeTrendingPreviewCache(entity)
        entity.toPreparedPreview().also { prepared ->
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
            tmdbApiEnabled = settingsRepository.settings.first().tmdbApiEnabled,
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
        val profileId = accountManager.activeProfileIdOrDefault()
        val key = profileId to lastSync
        if (previewCacheCleanedForKey == key) return
        mediaDao.deleteStaleHomeTrendingPreviewCache(profileId, lastSync)
        previewCacheCleanedForKey = key
    }

    private suspend fun hydrateCachedTrendingItems(
        key: HomeTrendingCacheKey,
        contentType: String,
        items: List<ContinueItem>,
    ): List<ContinueItem> {
        if (items.isEmpty()) return items
        if (!key.tmdbApiEnabled) return items
        cleanPreviewCacheIfNeeded(key.lastSync)
        return items.map { item ->
            val contentId = item.id.substringAfter(':', "").toIntOrNull()
                ?: return@map item
            mediaDao.getHomeTrendingPreviewCache(
                key.profileId,
                contentType,
                contentId,
                key.lastSync,
            )
                ?.takeIf { it.hasReusableXtreamPreviewCache() }
                ?.toPreparedPreview()
                ?.applyTo(item, promoteCardArtwork = true)
                ?: item
        }
    }

    private suspend fun prepareInitialTrendingSeries(
        key: HomeTrendingCacheKey,
        items: List<ContinueItem>,
    ): List<ContinueItem> =
        // Home readiness must only depend on catalog data and reusable local artwork.
        // Missing previews are prepared asynchronously when their cards become visible.
        hydrateCachedTrendingItems(
            key = key,
            contentType = TrendingSeriesType,
            items = items,
        )

    private suspend fun prepareEligibleTrendingMovies(
        candidates: List<ContinueItem>,
    ): List<ContinueItem> = coroutineScope {
        val selected = ArrayList<ContinueItem>(HomeTrendingPolicy.SectionLimit)
        for (batch in candidates.chunked(TrendingMovieDurationBatchSize)) {
            val preparedBatch = batch.map { item ->
                async {
                    val contentId = item.id.substringAfter(':', "").toIntOrNull() ?: return@async null
                    prepareTrendingPreview(
                        contentType = TrendingMovieType,
                        contentId = contentId,
                        fallbackPosterUrl = item.imageUrl,
                    )
                        ?.takeIf { HomeTrendingPolicy.isEligibleMovieDuration(it.durationMs) }
                        ?.applyTo(item, promoteCardArtwork = true)
                }
            }.map { it.await() }
            preparedBatch.filterNotNullTo(selected)
            if (selected.size >= HomeTrendingPolicy.SectionLimit) break
        }
        selected.take(HomeTrendingPolicy.SectionLimit)
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
            durationLabel = durationMs
                ?.takeIf { it > 0L }
                ?.formatDurationLabel()
                ?: details.duration?.takeIf { it.isMeaningfulDurationLabel() },
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
        val episodes = xtreamRepository.getSeriesEpisodes(contentId)
            .sortedWith(compareBy<XtreamSeriesEpisode> { it.seasonNumber }.thenBy { it.episodeNumber })
        val firstEpisode = episodes
            .firstOrNull()
        val durationMs = tmdb?.episodeRunTimeMinutes?.takeIf { it > 0 }?.times(60_000L)
            ?: details.episodeRunTime.parseDurationMs()
            ?: firstEpisode?.duration.parseDurationMs()
        val seasonsCount = episodes.map { it.seasonNumber }.filter { it > 0 }.distinct().size
        val sampleLabel = episodes.takeIf { it.isNotEmpty() }?.let {
            "${seasonsCount.coerceAtLeast(1)}S ${it.size.toString().padStart(2, '0')}E"
        }
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
            sampleLabel = sampleLabel,
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
        (previewKind == PreviewKindMovie || previewKind == PreviewKindEpisode ||
            (previewKind == PreviewKindNone && previewContentId == null && previewExtension == null)) &&
            (
                contentType != TrendingSeriesType ||
                    previewKind == PreviewKindNone ||
                    sampleLabel?.matches(Regex("\\d+S \\d+E")) == true
                )

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
            title = title.cleanHomeTitle(),
            meta = categoryName.takeIf { it.isNotBlank() } ?: mediaTypeLabel,
            remaining = rating?.let { "$it/10" } ?: year?.take(4).orEmpty(),
            progress = 0f,
            visualStyle = HomeVisualStyle.Cinema,
            imageUrl = posterUrl,
            previewImageUrl = null,
            ratingLabel = rating.toRatingLabel(),
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
            title = title.cleanHomeTitle(),
            meta = categoryName.takeIf { it.isNotBlank() } ?: mediaTypeLabel,
            remaining = year?.take(4) ?: rating?.let { "$it/10" }.orEmpty(),
            progress = 0f,
            visualStyle = HomeVisualStyle.Series,
            imageUrl = posterUrl,
            previewImageUrl = null,
            ratingLabel = rating.toRatingLabel(),
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
    val tmdbApiEnabled: Boolean,
)

private fun String.cleanHomeTitle(): String =
    TmdbMatcher.cleanDisplayTitle(this)
        .replace(Regex("\\s+"), " ")
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" 4K", "", ignoreCase = true)
        .trim()

private fun String?.toRatingLabel(): String? =
    this
        ?.trim()
        ?.substringBefore('/')
        ?.replace(',', '.')
        ?.trimEnd('0')
        ?.trimEnd('.')
        ?.takeIf { it.isNotBlank() && it != "0" }

private fun String?.parseDurationMs(): Long? {
    return HomeTrendingPolicy.parseDurationMs(this)
}

private fun String.isMeaningfulDurationLabel(): Boolean =
    isNotBlank() && parseDurationMs()?.let { it > 0L } == true

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
private const val TrendingPreviewPrepareConcurrency = 2
private const val PreviewKindMovie = "movie"
private const val PreviewKindEpisode = "episode"
private const val PreviewKindNone = "none"
private const val BackdropAvailable = "available"
private const val BackdropMissing = "missing"
private const val PreviewAvailable = "available"
private const val PreviewUnavailable = "unavailable"
private const val PreviewStartRatio = 0.15
private const val PreviewFallbackStartRatio = 0.30
private const val TrendingMovieDurationBatchSize = 3
