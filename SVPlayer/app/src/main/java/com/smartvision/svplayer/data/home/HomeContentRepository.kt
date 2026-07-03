package com.smartvision.svplayer.data.home

import android.os.SystemClock
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.data.diagnostics.PerformanceDiagnosticRecorder
import com.smartvision.svplayer.data.local.dao.SyncStateDao
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.mock.HomePreviewMode
import com.smartvision.svplayer.data.mock.HomeVisualStyle
import com.smartvision.svplayer.domain.model.TrendingCatalogItem
import com.smartvision.svplayer.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class HomeTrendingSnapshot(
    val movies: List<ContinueItem>,
    val series: List<ContinueItem>,
)

class HomeContentRepository(
    private val catalogRepository: CatalogRepository,
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
            runCatching { catalogRepository.getTrendingMovieItems(HomeTrendingSectionLimit) }
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
                .take(HomeTrendingSectionLimit)
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
            runCatching { catalogRepository.getTrendingSeriesItems(HomeTrendingSectionLimit) }
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
                .take(HomeTrendingSectionLimit)
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
        return withContext(Dispatchers.IO) {
            coroutineScope {
                val movies = async {
                    val candidatesStart = SystemClock.elapsedRealtime()
                    runCatching { catalogRepository.getTrendingMovieItems(HomeTrendingSectionLimit) }
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
                        .take(HomeTrendingSectionLimit)
                }
                val series = async {
                    val candidatesStart = SystemClock.elapsedRealtime()
                    runCatching { catalogRepository.getTrendingSeriesItems(HomeTrendingSectionLimit) }
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
                        .take(HomeTrendingSectionLimit)
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
            previewImageUrl = posterUrl,
            mediaType = "FILM",
            previewUrl = previewUrl,
            previewMode = HomePreviewMode.TrendSegments,
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
            previewImageUrl = posterUrl,
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

private fun String.cleanHistoryTitle(): String =
    replace(Regex("\\s+"), " ")
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" 4K", "", ignoreCase = true)
        .trim()

private const val HomeTrendingSectionLimit = 10
