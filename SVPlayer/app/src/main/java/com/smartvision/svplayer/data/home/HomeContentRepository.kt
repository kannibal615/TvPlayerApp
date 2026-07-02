package com.smartvision.svplayer.data.home

import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.data.appconfig.AppConfigRepository
import com.smartvision.svplayer.data.appconfig.TrendingConfig
import com.smartvision.svplayer.data.appconfig.defaultTrendingConfig
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
    }

    fun invalidateTrending() {
        cachedTrending = null
    }

    suspend fun cacheEmptyTrending() {
        cachedTrending = CachedHomeTrending(
            key = currentCacheKey(),
            snapshot = HomeTrendingSnapshot(movies = emptyList(), series = emptyList()),
        )
    }

    suspend fun preloadTrending(): HomeTrendingSnapshot = refreshTrending(forceRefresh = true)

    suspend fun refreshTrending(forceRefresh: Boolean = false): HomeTrendingSnapshot {
        val key = currentCacheKey()
        if (!forceRefresh) {
            cachedTrending
                ?.takeIf { it.key == key }
                ?.snapshot
                ?.let { return it }
        }
        if (key.source != PlaylistSource.Xtream.storageValue || key.accountSignature.isBlank()) {
            return HomeTrendingSnapshot(emptyList(), emptyList())
                .also { cachedTrending = CachedHomeTrending(key, it) }
        }
        return withContext(Dispatchers.IO) {
            val trendingConfig = runCatching { appConfigRepository.loadConfig().trending }
                .getOrDefault(defaultTrendingConfig())
            coroutineScope {
                val movies = async {
                    runCatching { catalogRepository.getTrendingMovieItems(trendingConfig.candidateLimit) }
                        .getOrDefault(emptyList())
                        .mapTrendsUntilLimit(trendingConfig) { it.toMovieTrendItem(trendingConfig) }
                }
                val series = async {
                    runCatching { catalogRepository.getTrendingSeriesItems(trendingConfig.candidateLimit) }
                        .getOrDefault(emptyList())
                        .mapTrendsUntilLimit(trendingConfig) { it.toSeriesTrendItem(trendingConfig) }
                }
                HomeTrendingSnapshot(
                    movies = movies.await(),
                    series = series.await(),
                ).also { cachedTrending = CachedHomeTrending(key, it) }
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
        if (config.useRatingFilter && rating.toTrendRatingValue() < config.minimumRating) return null
        val backdropUrl = runCatching { xtreamRepository.getMovieDetails(contentId).backdropUrl }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
        if (config.requireLandscapeImage && backdropUrl.isNullOrBlank()) return null
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
        if (config.useRatingFilter && rating.toTrendRatingValue() < config.minimumRating) return null
        val backdropUrl = runCatching { xtreamRepository.getSeriesDetails(contentId).backdropUrl }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
        if (config.requireLandscapeImage && backdropUrl.isNullOrBlank()) return null
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
