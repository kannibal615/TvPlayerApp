package com.smartvision.svplayer.data.home

import android.os.SystemClock
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.data.appconfig.AppConfigRepository
import com.smartvision.svplayer.data.appconfig.TrendingConfig
import com.smartvision.svplayer.data.appconfig.defaultTrendingConfig
import com.smartvision.svplayer.data.diagnostics.PerformanceDiagnosticRecorder
import com.smartvision.svplayer.data.local.dao.SyncStateDao
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.mock.HomePreviewMode
import com.smartvision.svplayer.data.mock.HomeVisualStyle
import com.smartvision.svplayer.data.repository.XtreamRepository
import com.smartvision.svplayer.domain.model.TrendingCatalogItem
import com.smartvision.svplayer.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class HomeTrendingSnapshot(
    val movies: List<ContinueItem>,
    val series: List<ContinueItem>,
)

class HomeContentRepository(
    private val catalogRepository: CatalogRepository,
    private val xtreamRepository: XtreamRepository,
    private val appConfigRepository: AppConfigRepository,
    private val accountManager: XtreamAccountManager,
    private val syncStateDao: SyncStateDao,
) {
    @Volatile
    private var cachedTrending: CachedHomeTrending? = null

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
            val trendingConfig = runCatching { appConfigRepository.loadConfig().trending }
                .getOrDefault(defaultTrendingConfig())
            PerformanceDiagnosticRecorder.record(
                sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                event = "home_trending_config_loaded",
                fields = mapOf(
                    "candidateLimit" to trendingConfig.candidateLimit,
                    "sectionLimit" to trendingConfig.sectionLimit,
                    "useRatingFilter" to trendingConfig.useRatingFilter,
                    "minimumRating" to trendingConfig.minimumRating,
                    "requireLandscapeImage" to trendingConfig.requireLandscapeImage,
                ),
            )
            coroutineScope {
                val movies = async {
                    val candidatesStart = SystemClock.elapsedRealtime()
                    runCatching { catalogRepository.getTrendingMovieItems(trendingConfig.candidateLimit) }
                        .getOrDefault(emptyList())
                        .also { candidates ->
                            PerformanceDiagnosticRecorder.recordDuration(
                                sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                                event = "home_trending_movie_candidates",
                                startedAtMs = candidatesStart,
                                fields = mapOf("candidates" to candidates.size),
                            )
                        }
                        .mapTrendsUntilLimit(trendingConfig) { it.toMovieTrendItem(trendingConfig) }
                }
                val series = async {
                    val candidatesStart = SystemClock.elapsedRealtime()
                    runCatching { catalogRepository.getTrendingSeriesItems(trendingConfig.candidateLimit) }
                        .getOrDefault(emptyList())
                        .also { candidates ->
                            PerformanceDiagnosticRecorder.recordDuration(
                                sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                                event = "home_trending_series_candidates",
                                startedAtMs = candidatesStart,
                                fields = mapOf("candidates" to candidates.size),
                            )
                        }
                        .mapTrendsUntilLimit(trendingConfig) { it.toSeriesTrendItem(trendingConfig) }
                }
                HomeTrendingSnapshot(
                    movies = movies.await(),
                    series = series.await(),
                ).also {
                    cachedTrending = CachedHomeTrending(key, it)
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

    private suspend fun currentCacheKey(): HomeTrendingCacheKey =
        HomeTrendingCacheKey(
            source = accountManager.activePlaylistSource.value.storageValue,
            accountSignature = accountManager.accounts.value.joinToString("|") { account ->
                "${account.id}:${account.host}:${account.username}:${account.password.hashCode()}"
            },
            lastSync = syncStateDao.get()?.lastSync ?: 0L,
        )

    private suspend fun TrendingCatalogItem.toMovieTrendItem(
        config: TrendingConfig,
    ): ContinueItem? {
        // PERF_DIAG: per-candidate details are intentionally data-only; URLs/secrets are not written.
        val startedAt = SystemClock.elapsedRealtime()
        if (config.useRatingFilter && rating.toTrendRatingValue() < config.minimumRating) {
            PerformanceDiagnosticRecorder.recordDuration(
                sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                event = "home_trending_movie_rejected",
                startedAtMs = startedAt,
                fields = mapOf("contentId" to contentId, "title" to title, "reason" to "rating_filter", "rating" to rating),
            )
            return null
        }
        val backdropUrl = runCatching { xtreamRepository.getMovieDetails(contentId).backdropUrl }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
        if (config.requireLandscapeImage && backdropUrl.isNullOrBlank()) {
            PerformanceDiagnosticRecorder.recordDuration(
                sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                event = "home_trending_movie_rejected",
                startedAtMs = startedAt,
                fields = mapOf("contentId" to contentId, "title" to title, "reason" to "missing_backdrop"),
            )
            return null
        }
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
                "hasBackdrop" to !backdropUrl.isNullOrBlank(),
                "hasPreviewUrl" to !previewUrl.isNullOrBlank(),
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
            previewImageUrl = backdropUrl,
            mediaType = "FILM",
            previewUrl = previewUrl,
            previewMode = HomePreviewMode.TrendSegments,
        )
    }

    private suspend fun TrendingCatalogItem.toSeriesTrendItem(
        config: TrendingConfig,
    ): ContinueItem? {
        // PERF_DIAG: per-candidate details are intentionally data-only; URLs/secrets are not written.
        val startedAt = SystemClock.elapsedRealtime()
        if (config.useRatingFilter && rating.toTrendRatingValue() < config.minimumRating) {
            PerformanceDiagnosticRecorder.recordDuration(
                sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                event = "home_trending_series_rejected",
                startedAtMs = startedAt,
                fields = mapOf("contentId" to contentId, "title" to title, "reason" to "rating_filter", "rating" to rating),
            )
            return null
        }
        val backdropUrl = runCatching { xtreamRepository.getSeriesDetails(contentId).backdropUrl }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
        if (config.requireLandscapeImage && backdropUrl.isNullOrBlank()) {
            PerformanceDiagnosticRecorder.recordDuration(
                sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                event = "home_trending_series_rejected",
                startedAtMs = startedAt,
                fields = mapOf("contentId" to contentId, "title" to title, "reason" to "missing_backdrop"),
            )
            return null
        }
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
                "hasBackdrop" to !backdropUrl.isNullOrBlank(),
                "hasPreviewUrl" to !previewUrl.isNullOrBlank(),
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
            previewImageUrl = backdropUrl,
            mediaType = "SERIE",
            previewUrl = previewUrl,
            previewMode = HomePreviewMode.TrendSegments,
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
    val source: String,
    val accountSignature: String,
    val lastSync: Long,
)

private suspend fun List<TrendingCatalogItem>.mapTrendsUntilLimit(
    config: TrendingConfig,
    transform: suspend (TrendingCatalogItem) -> ContinueItem?,
): List<ContinueItem> {
    if (isEmpty()) return emptyList()
    val results = mutableListOf<ContinueItem>()
    for (chunk in chunked(TrendingDetailParallelism)) {
        val mapped = coroutineScope {
            chunk.map { item -> async { transform(item) } }.awaitAll()
        }.filterNotNull()
        results += mapped
        if (results.size >= config.sectionLimit) break
    }
    return results.take(config.sectionLimit)
}

private fun String?.toTrendRatingValue(): Float =
    this
        ?.replace(',', '.')
        ?.toFloatOrNull()
        ?.coerceIn(0f, 10f)
        ?: 0f

private fun String.cleanHistoryTitle(): String =
    replace(Regex("\\s+"), " ")
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" 4K", "", ignoreCase = true)
        .trim()

private const val TrendingDetailParallelism = 6
