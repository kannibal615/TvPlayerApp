package com.smartvision.svplayer.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.data.local.SVDatabase
import com.smartvision.svplayer.data.local.dao.CategoryDao
import com.smartvision.svplayer.data.local.dao.FavoriteDao
import com.smartvision.svplayer.data.local.dao.MediaDao
import com.smartvision.svplayer.data.local.dao.ProfileDao
import com.smartvision.svplayer.data.local.dao.ProgressDao
import com.smartvision.svplayer.data.local.dao.SyncStateDao
import com.smartvision.svplayer.data.local.entity.FavoriteEntity
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.local.entity.SyncStateEntity
import com.smartvision.svplayer.data.network.NetworkActivityHandle
import com.smartvision.svplayer.data.network.NetworkActivityStatus
import com.smartvision.svplayer.data.network.NetworkActivityTracker
import com.smartvision.svplayer.data.network.NetworkActivityType
import com.smartvision.svplayer.data.playlist.EpgRepository
import com.smartvision.svplayer.data.playlist.M3uPlaylistClient
import com.smartvision.svplayer.data.remote.XtreamApiService
import com.smartvision.svplayer.data.remote.XtreamUrlFactory
import com.smartvision.svplayer.domain.model.AccountProfile
import com.smartvision.svplayer.domain.model.Category
import com.smartvision.svplayer.domain.model.Episode
import com.smartvision.svplayer.domain.model.LiveChannel
import com.smartvision.svplayer.domain.model.MediaSection
import com.smartvision.svplayer.domain.model.Movie
import com.smartvision.svplayer.domain.model.PlaybackKind
import com.smartvision.svplayer.domain.model.PlaybackRequest
import com.smartvision.svplayer.domain.model.SyncStatus
import com.smartvision.svplayer.domain.model.TrendingCatalogItem
import com.smartvision.svplayer.domain.model.TvSeries
import com.smartvision.svplayer.domain.repository.CatalogContentCounts
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.domain.repository.LocalCatalogSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultCatalogRepository(
    private val database: SVDatabase,
    private val api: XtreamApiService,
    private val accountManager: XtreamAccountManager,
    private val urlFactory: XtreamUrlFactory,
    private val m3uPlaylistClient: M3uPlaylistClient,
    private val epgRepository: EpgRepository,
    private val categoryDao: CategoryDao,
    private val mediaDao: MediaDao,
    private val profileDao: ProfileDao,
    private val favoriteDao: FavoriteDao,
    private val progressDao: ProgressDao,
    private val syncStateDao: SyncStateDao,
    private val networkActivityTracker: NetworkActivityTracker,
) : CatalogRepository {
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus
    private val localCatalogSnapshotCache = LocalCatalogSnapshotCache()

    override fun observeLiveCategories(): Flow<List<Category>> =
        combine(
            categoryDao.observeByType(MediaSection.Live.storageName),
            mediaDao.observeLiveStreamCountsByCategory(),
            accountManager.activePlaylistSource,
            accountManager.m3uUrl,
            accountManager.accounts,
        ) { categories, countsByCategory, source, m3uUrl, accounts ->
            if (!source.hasConfiguredCatalog(m3uUrl, accounts.isNotEmpty())) return@combine emptyList()
            val counts = countsByCategory.associate { it.categoryId to it.count }
            localCatalogSnapshotCache.putLiveCategories(
                categories.map { it.toDomain(MediaSection.Live, counts[it.id] ?: 0) },
            )
        }

    override fun observeLiveChannels(categoryId: String?): Flow<List<LiveChannel>> =
        combine(
            categoryDao.observeByType(MediaSection.Live.storageName),
            mediaDao.observeLiveStreamsByCategory(categoryId),
            accountManager.activePlaylistSource,
            accountManager.m3uUrl,
            accountManager.accounts,
        ) { categories, streams, source, m3uUrl, accounts ->
            if (!source.hasConfiguredCatalog(m3uUrl, accounts.isNotEmpty())) return@combine emptyList()
            val names = categories.associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            streams.map { it.toDomain(names[it.categoryId] ?: "Live TV", imageBaseHost).withEpg(epgRepository) }
        }

    override fun observeMovieCategories(): Flow<List<Category>> =
        combine(
            categoryDao.observeByType(MediaSection.Movies.storageName),
            mediaDao.observeMovieCountsByCategory(),
            accountManager.activePlaylistSource,
            accountManager.accounts,
        ) { categories, countsByCategory, source, accounts ->
            if (source != PlaylistSource.Xtream || accounts.isEmpty()) return@combine emptyList()
            val counts = countsByCategory.associate { it.categoryId to it.count }
            localCatalogSnapshotCache.putMovieCategories(
                categories.map { it.toDomain(MediaSection.Movies, counts[it.id] ?: 0) },
            )
        }

    override fun observeMovies(categoryId: String?): Flow<List<Movie>> =
        combine(
            categoryDao.observeByType(MediaSection.Movies.storageName),
            mediaDao.observeMoviesByCategory(categoryId?.takeUnless { it == "all" || it == "new" || it == "favorites" }),
            accountManager.activePlaylistSource,
            accountManager.accounts,
        ) { categories, movies, source, accounts ->
            if (source != PlaylistSource.Xtream || accounts.isEmpty()) return@combine emptyList()
            val names = categories.associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            movies.map { it.toDomain(names[it.categoryId] ?: "Films", imageBaseHost) }
        }

    override fun observeSeriesCategories(): Flow<List<Category>> =
        combine(
            categoryDao.observeByType(MediaSection.Series.storageName),
            mediaDao.observeSeriesCountsByCategory(),
            accountManager.activePlaylistSource,
            accountManager.accounts,
        ) { categories, countsByCategory, source, accounts ->
            if (source != PlaylistSource.Xtream || accounts.isEmpty()) return@combine emptyList()
            val counts = countsByCategory.associate { it.categoryId to it.count }
            localCatalogSnapshotCache.putSeriesCategories(
                categories.map { it.toDomain(MediaSection.Series, counts[it.id] ?: 0) },
            )
        }

    override fun observeSeries(categoryId: String?): Flow<List<TvSeries>> =
        combine(
            categoryDao.observeByType(MediaSection.Series.storageName),
            mediaDao.observeSeriesByCategory(categoryId?.takeUnless { it == "all" || it == "new" || it == "favorites" }),
            accountManager.activePlaylistSource,
            accountManager.accounts,
        ) { categories, series, source, accounts ->
            if (source != PlaylistSource.Xtream || accounts.isEmpty()) return@combine emptyList()
            val names = categories.associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            series.map { it.toDomain(names[it.categoryId] ?: "Series", imageBaseHost) }
        }

    override fun observeAccount(): Flow<AccountProfile> =
        combine(
            profileDao.observe(),
            mediaDao.observeLiveStreamCount(),
            mediaDao.observeMovieCount(),
            mediaDao.observeSeriesCount(),
        ) { profile, liveCount, movieCount, seriesCount ->
            profile?.toDomain(
                liveCount = liveCount,
                movieCount = movieCount,
                seriesCount = seriesCount,
            ) ?: emptyAccountProfile()
        }

    override fun getCachedLiveCatalogSnapshot(): LocalCatalogSnapshot<LiveChannel>? =
        localCatalogSnapshotCache.getLive()

    override fun getCachedMovieCatalogSnapshot(): LocalCatalogSnapshot<Movie>? =
        localCatalogSnapshotCache.getMovies()

    override fun getCachedSeriesCatalogSnapshot(): LocalCatalogSnapshot<TvSeries>? =
        localCatalogSnapshotCache.getSeries()

    override fun getCachedLiveCategories(): List<Category>? =
        localCatalogSnapshotCache.getLiveCategories()

    override fun getCachedMovieCategories(): List<Category>? =
        localCatalogSnapshotCache.getMovieCategories()

    override fun getCachedSeriesCategories(): List<Category>? =
        localCatalogSnapshotCache.getSeriesCategories()

    override suspend fun getLiveCategoriesSnapshot(): List<Category> = withContext(Dispatchers.IO) {
        getCachedLiveCategories()?.let { return@withContext it }
        localCatalogSnapshotCache.putLiveCategories(observeLiveCategories().first())
    }

    override suspend fun getMovieCategoriesSnapshot(): List<Category> = withContext(Dispatchers.IO) {
        getCachedMovieCategories()?.let { return@withContext it }
        localCatalogSnapshotCache.putMovieCategories(observeMovieCategories().first())
    }

    override suspend fun getSeriesCategoriesSnapshot(): List<Category> = withContext(Dispatchers.IO) {
        getCachedSeriesCategories()?.let { return@withContext it }
        localCatalogSnapshotCache.putSeriesCategories(observeSeriesCategories().first())
    }

    override suspend fun getInitialLiveCategoriesSnapshot(limit: Int): List<Category> = withContext(Dispatchers.IO) {
        if (!isLiveCatalogConfigured()) return@withContext emptyList()
        val counts = mediaDao.observeLiveStreamCountsByCategory().first().associate { it.categoryId to it.count }
        categoryDao.getByTypeLimit(MediaSection.Live.storageName, limit)
            .map { it.toDomain(MediaSection.Live, counts[it.id] ?: 0) }
    }

    override suspend fun getInitialMovieCategoriesSnapshot(limit: Int): List<Category> = withContext(Dispatchers.IO) {
        if (!isXtreamCatalogConfigured()) return@withContext emptyList()
        val counts = mediaDao.observeMovieCountsByCategory().first().associate { it.categoryId to it.count }
        categoryDao.getByTypeLimit(MediaSection.Movies.storageName, limit)
            .map { it.toDomain(MediaSection.Movies, counts[it.id] ?: 0) }
    }

    override suspend fun getInitialSeriesCategoriesSnapshot(limit: Int): List<Category> = withContext(Dispatchers.IO) {
        if (!isXtreamCatalogConfigured()) return@withContext emptyList()
        val counts = mediaDao.observeSeriesCountsByCategory().first().associate { it.categoryId to it.count }
        categoryDao.getByTypeLimit(MediaSection.Series.storageName, limit)
            .map { it.toDomain(MediaSection.Series, counts[it.id] ?: 0) }
    }

    override suspend fun getLiveCatalogSnapshot(): LocalCatalogSnapshot<LiveChannel> = withContext(Dispatchers.IO) {
        getCachedLiveCatalogSnapshot()?.let { return@withContext it }
        val snapshot = LocalCatalogSnapshot(
            categories = observeLiveCategories().first(),
            items = observeLiveChannels(null).first(),
        )
        localCatalogSnapshotCache.putLive(snapshot)
    }

    override suspend fun getMovieCatalogSnapshot(): LocalCatalogSnapshot<Movie> = withContext(Dispatchers.IO) {
        getCachedMovieCatalogSnapshot()?.let { return@withContext it }
        val snapshot = LocalCatalogSnapshot(
            categories = observeMovieCategories().first(),
            items = observeMovies(null).first(),
        )
        localCatalogSnapshotCache.putMovies(snapshot)
    }

    override suspend fun getSeriesCatalogSnapshot(): LocalCatalogSnapshot<TvSeries> = withContext(Dispatchers.IO) {
        getCachedSeriesCatalogSnapshot()?.let { return@withContext it }
        val snapshot = LocalCatalogSnapshot(
            categories = observeSeriesCategories().first(),
            items = observeSeries(null).first(),
        )
        localCatalogSnapshotCache.putSeries(snapshot)
    }

    override suspend fun getLiveChannelsPage(categoryId: String?, offset: Int, limit: Int): List<LiveChannel> =
        withContext(Dispatchers.IO) {
            if (!isLiveCatalogConfigured()) return@withContext emptyList()
            val safeLimit = limit.coerceIn(1, CatalogPageMaxLimit)
            val safeOffset = offset.coerceAtLeast(0)
            localCatalogSnapshotCache.getLivePage(categoryId, safeOffset, safeLimit)?.let { return@withContext it }
            val categoryNames = categoryDao.getByType(MediaSection.Live.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            val streams = categoryId
                ?.takeIf { it.isNotBlank() }
                ?.let { mediaDao.getLiveStreamsByCategoryPage(it, safeLimit, safeOffset) }
                ?: mediaDao.getLiveStreamsPage(safeLimit, safeOffset)
            localCatalogSnapshotCache.putLivePage(
                categoryId = categoryId,
                offset = safeOffset,
                limit = safeLimit,
                items = streams.map { stream ->
                    stream.toDomain(categoryNames[stream.categoryId] ?: "Live TV", imageBaseHost).withEpg(epgRepository)
                },
            )
        }

    override suspend fun searchLiveChannelsPage(categoryId: String?, query: String, offset: Int, limit: Int): List<LiveChannel> =
        withContext(Dispatchers.IO) {
            if (!isLiveCatalogConfigured()) return@withContext emptyList()
            val cleanQuery = query.trim()
            if (cleanQuery.isBlank()) return@withContext getLiveChannelsPage(categoryId, offset, limit)
            val safeLimit = limit.coerceIn(1, CatalogPageMaxLimit)
            val safeOffset = offset.coerceAtLeast(0)
            val pattern = cleanQuery.toSqlLikeContainsPattern()
            val categoryNames = categoryDao.getByType(MediaSection.Live.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            val streams = categoryId
                ?.takeIf { it.isNotBlank() }
                ?.let { mediaDao.searchLiveStreamsByCategoryPage(it, pattern, safeLimit, safeOffset) }
                ?: mediaDao.searchLiveStreamsPage(pattern, safeLimit, safeOffset)
            streams.map { stream ->
                stream.toDomain(categoryNames[stream.categoryId] ?: "Live TV", imageBaseHost).withEpg(epgRepository)
            }
        }

    override suspend fun getLiveChannelById(streamId: Int): LiveChannel? =
        withContext(Dispatchers.IO) {
            if (!isLiveCatalogConfigured()) return@withContext null
            val categoryNames = categoryDao.getByType(MediaSection.Live.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            mediaDao.getLiveStream(streamId)
                ?.let { stream -> stream.toDomain(categoryNames[stream.categoryId] ?: "Live TV", imageBaseHost) }
                ?.withEpg(epgRepository)
        }

    override suspend fun getPreviousLiveChannel(streamId: Int): LiveChannel? =
        withContext(Dispatchers.IO) {
            if (!isLiveCatalogConfigured()) return@withContext null
            val current = mediaDao.getLiveStream(streamId) ?: return@withContext null
            val categoryNames = categoryDao.getByType(MediaSection.Live.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            mediaDao.getPreviousLiveStream(current.categoryId, current.number, current.name, current.streamId)
                ?.let { stream -> stream.toDomain(categoryNames[stream.categoryId] ?: "Live TV", imageBaseHost) }
                ?.withEpg(epgRepository)
        }

    override suspend fun getNextLiveChannel(streamId: Int): LiveChannel? =
        withContext(Dispatchers.IO) {
            if (!isLiveCatalogConfigured()) return@withContext null
            val current = mediaDao.getLiveStream(streamId) ?: return@withContext null
            val categoryNames = categoryDao.getByType(MediaSection.Live.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            mediaDao.getNextLiveStream(current.categoryId, current.number, current.name, current.streamId)
                ?.let { stream -> stream.toDomain(categoryNames[stream.categoryId] ?: "Live TV", imageBaseHost) }
                ?.withEpg(epgRepository)
        }

    override suspend fun getMoviesPage(categoryId: String?, offset: Int, limit: Int): List<Movie> =
        withContext(Dispatchers.IO) {
            if (!isXtreamCatalogConfigured()) return@withContext emptyList()
            val safeLimit = limit.coerceIn(1, CatalogPageMaxLimit)
            val safeOffset = offset.coerceAtLeast(0)
            localCatalogSnapshotCache.getMoviePage(categoryId, safeOffset, safeLimit)?.let { return@withContext it }
            val categoryNames = categoryDao.getByType(MediaSection.Movies.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            val movies = categoryId
                ?.takeIf { it.isNotBlank() }
                ?.let { mediaDao.getMoviesByCategoryPage(it, safeLimit, safeOffset) }
                ?: mediaDao.getMoviesPage(safeLimit, safeOffset)
            localCatalogSnapshotCache.putMoviePage(
                categoryId = categoryId,
                offset = safeOffset,
                limit = safeLimit,
                items = movies.map { movie -> movie.toDomain(categoryNames[movie.categoryId] ?: "Films", imageBaseHost) },
            )
        }

    override suspend fun getSeriesPage(categoryId: String?, offset: Int, limit: Int): List<TvSeries> =
        withContext(Dispatchers.IO) {
            if (!isXtreamCatalogConfigured()) return@withContext emptyList()
            val safeLimit = limit.coerceIn(1, CatalogPageMaxLimit)
            val safeOffset = offset.coerceAtLeast(0)
            localCatalogSnapshotCache.getSeriesPage(categoryId, safeOffset, safeLimit)?.let { return@withContext it }
            val categoryNames = categoryDao.getByType(MediaSection.Series.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            val series = categoryId
                ?.takeIf { it.isNotBlank() }
                ?.let { mediaDao.getSeriesByCategoryPage(it, safeLimit, safeOffset) }
                ?: mediaDao.getSeriesPage(safeLimit, safeOffset)
            localCatalogSnapshotCache.putSeriesPage(
                categoryId = categoryId,
                offset = safeOffset,
                limit = safeLimit,
                items = series.map { item -> item.toDomain(categoryNames[item.categoryId] ?: "Series", imageBaseHost) },
            )
        }

    override suspend fun getAllLiveChannelsPage(offset: Int, limit: Int): List<LiveChannel> =
        getLiveChannelsPage(categoryId = null, offset = offset, limit = limit)

    override suspend fun getAllMoviesPage(offset: Int, limit: Int): List<Movie> =
        getMoviesPage(categoryId = null, offset = offset, limit = limit)

    override suspend fun getAllSeriesPage(offset: Int, limit: Int): List<TvSeries> =
        getSeriesPage(categoryId = null, offset = offset, limit = limit)

    override suspend fun getLiveChannelsByIds(streamIds: List<Int>): List<LiveChannel> =
        withContext(Dispatchers.IO) {
            if (streamIds.isEmpty() || !isLiveCatalogConfigured()) return@withContext emptyList()
            val distinctIds = streamIds.distinct()
            val order = distinctIds.withIndex().associate { it.value to it.index }
            val categoryNames = categoryDao.getByType(MediaSection.Live.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            mediaDao.getLiveStreamsByIds(distinctIds)
                .sortedBy { order[it.streamId] ?: Int.MAX_VALUE }
                .map { stream -> stream.toDomain(categoryNames[stream.categoryId] ?: "Live TV", imageBaseHost).withEpg(epgRepository) }
        }

    override suspend fun getMoviesByIds(streamIds: List<Int>): List<Movie> =
        withContext(Dispatchers.IO) {
            if (streamIds.isEmpty() || !isXtreamCatalogConfigured()) return@withContext emptyList()
            val distinctIds = streamIds.distinct()
            val order = distinctIds.withIndex().associate { it.value to it.index }
            val categoryNames = categoryDao.getByType(MediaSection.Movies.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            mediaDao.getMoviesByIds(distinctIds)
                .sortedBy { order[it.streamId] ?: Int.MAX_VALUE }
                .map { movie -> movie.toDomain(categoryNames[movie.categoryId] ?: "Films", imageBaseHost) }
        }

    override suspend fun getSeriesByIds(seriesIds: List<Int>): List<TvSeries> =
        withContext(Dispatchers.IO) {
            if (seriesIds.isEmpty() || !isXtreamCatalogConfigured()) return@withContext emptyList()
            val distinctIds = seriesIds.distinct()
            val order = distinctIds.withIndex().associate { it.value to it.index }
            val categoryNames = categoryDao.getByType(MediaSection.Series.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            mediaDao.getSeriesByIds(distinctIds)
                .sortedBy { order[it.seriesId] ?: Int.MAX_VALUE }
                .map { series -> series.toDomain(categoryNames[series.categoryId] ?: "Series", imageBaseHost) }
        }

    override suspend fun getTrendingMovies(limit: Int): List<Movie> = withContext(Dispatchers.IO) {
        if (accountManager.activePlaylistSource.value != PlaylistSource.Xtream || accountManager.accounts.value.isEmpty()) {
            return@withContext emptyList()
        }
        val categoryNames = categoryDao.getByType(MediaSection.Movies.storageName).associate { it.id to it.name }
        val imageBaseHost = imageBaseHost()
        mediaDao.getTrendingMovies(limit)
            .map { movie -> movie.toDomain(categoryNames[movie.categoryId] ?: "Films", imageBaseHost) }
    }

    override suspend fun getTrendingSeries(limit: Int): List<TvSeries> = withContext(Dispatchers.IO) {
        if (accountManager.activePlaylistSource.value != PlaylistSource.Xtream || accountManager.accounts.value.isEmpty()) {
            return@withContext emptyList()
        }
        val categoryNames = categoryDao.getByType(MediaSection.Series.storageName).associate { it.id to it.name }
        val imageBaseHost = imageBaseHost()
        mediaDao.getTrendingSeries(limit)
            .map { series -> series.toDomain(categoryNames[series.categoryId] ?: "Series", imageBaseHost) }
    }

    override suspend fun getTrendingMovieItems(limit: Int): List<TrendingCatalogItem> = withContext(Dispatchers.IO) {
        if (accountManager.activePlaylistSource.value != PlaylistSource.Xtream || accountManager.accounts.value.isEmpty()) {
            return@withContext emptyList()
        }
        val categoryNames = categoryDao.getByType(MediaSection.Movies.storageName).associate { it.id to it.name }
        val imageBaseHost = imageBaseHost()
        mediaDao.getTrendingMovies(randomTrendCandidateLimit(limit))
            .asSequence()
            .map { movie ->
                TrendingCatalogItem(
                    contentType = TrendingMovieType,
                    contentId = movie.streamId,
                    title = movie.title,
                    categoryName = categoryNames[movie.categoryId] ?: "Films",
                    posterUrl = normalizeCatalogImageUrl(movie.posterUrl, imageBaseHost),
                    rating = movie.rating,
                    year = movie.year,
                    previewUrl = null,
                )
            }
            .filterNot { it.containsAdultMarker() }
            .take(limit)
            .toList()
    }

    override suspend fun getTrendingSeriesItems(limit: Int): List<TrendingCatalogItem> = withContext(Dispatchers.IO) {
        if (accountManager.activePlaylistSource.value != PlaylistSource.Xtream || accountManager.accounts.value.isEmpty()) {
            return@withContext emptyList()
        }
        val categoryNames = categoryDao.getByType(MediaSection.Series.storageName).associate { it.id to it.name }
        val imageBaseHost = imageBaseHost()
        mediaDao.getTrendingSeries(randomTrendCandidateLimit(limit))
            .asSequence()
            .map { series ->
                TrendingCatalogItem(
                    contentType = TrendingSeriesType,
                    contentId = series.seriesId,
                    title = series.title,
                    categoryName = categoryNames[series.categoryId] ?: "Series",
                    posterUrl = normalizeCatalogImageUrl(series.posterUrl, imageBaseHost),
                    rating = series.rating,
                    year = series.year,
                    previewUrl = null,
                )
            }
            .filterNot { it.containsAdultMarker() }
            .take(limit)
            .toList()
    }

    override suspend fun getCatalogContentCounts(): CatalogContentCounts = withContext(Dispatchers.IO) {
        CatalogContentCounts(
            live = mediaDao.countLiveStreams(),
            movies = mediaDao.countMovies(),
            series = mediaDao.countSeries(),
        )
    }

    override fun invalidateLocalCatalogCache() {
        localCatalogSnapshotCache.invalidate()
    }

    private fun isLiveCatalogConfigured(): Boolean =
        accountManager.activePlaylistSource.value.hasConfiguredCatalog(
            m3uUrl = accountManager.m3uUrl.value,
            hasXtream = accountManager.accounts.value.isNotEmpty(),
        )

    private fun isXtreamCatalogConfigured(): Boolean =
        accountManager.activePlaylistSource.value == PlaylistSource.Xtream &&
            accountManager.accounts.value.isNotEmpty()

    private fun imageBaseHost(): String =
        accountManager.current().normalizedHost

    override suspend fun synchronize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (accountManager.activePlaylistSource.value == PlaylistSource.M3u) {
            return@withContext synchronizeM3u()
        }

        val credentials = accountManager.current()
        if (!credentials.isConfigured) {
            val message = "Aucun compte Xtream configure"
            _syncStatus.value = SyncStatus.Error(message)
            return@withContext Result.failure(IllegalStateException(message))
        }

        val previousCatalogProgress = SyncStatus.CatalogProgress(
            live = SyncStatus.SyncSectionProgress(previousItems = mediaDao.countLiveStreams()),
            movies = SyncStatus.SyncSectionProgress(previousItems = mediaDao.countMovies()),
            series = SyncStatus.SyncSectionProgress(previousItems = mediaDao.countSeries()),
        )
        val syncWorkId = "catalog-sync-${System.currentTimeMillis()}"
        val syncWork = networkActivityTracker.begin(
            id = syncWorkId,
            title = "Catalog synchronization",
            type = NetworkActivityType.Catalog,
            message = "Preparing catalog synchronization",
            source = "Xtream",
            progressPercent = 0,
        )
        val liveWork = networkActivityTracker.beginCatalogSectionWork(syncWorkId, "Live TV")
        val moviesWork = networkActivityTracker.beginCatalogSectionWork(syncWorkId, "Movies")
        val seriesWork = networkActivityTracker.beginCatalogSectionWork(syncWorkId, "Series")
        var liveItems = 0
        var movieItems = 0
        var seriesItems = 0
        var liveCompleted = false
        var moviesCompleted = false
        var seriesCompleted = false
        var livePhase = SyncStatus.SyncSectionPhase.WAITING
        var moviesPhase = SyncStatus.SyncSectionPhase.WAITING
        var seriesPhase = SyncStatus.SyncSectionPhase.WAITING
        var livePercent = 0
        var moviesPercent = 0
        var seriesPercent = 0
        fun currentCatalogProgress(): SyncStatus.CatalogProgress =
            SyncStatus.CatalogProgress(
                live = previousCatalogProgress.live.copy(
                    currentItems = liveItems,
                    completed = liveCompleted,
                    phase = livePhase,
                    progressPercent = livePercent,
                ),
                movies = previousCatalogProgress.movies.copy(
                    currentItems = movieItems,
                    completed = moviesCompleted,
                    phase = moviesPhase,
                    progressPercent = moviesPercent,
                ),
                series = previousCatalogProgress.series.copy(
                    currentItems = seriesItems,
                    completed = seriesCompleted,
                    phase = seriesPhase,
                    progressPercent = seriesPercent,
                ),
            )

        _syncStatus.value = SyncStatus.Running(catalogProgress = previousCatalogProgress)
        try {
            logSyncMemory(
                stage = "xtream_sync_start",
                previousLive = previousCatalogProgress.live.previousItems,
                previousMovies = previousCatalogProgress.movies.previousItems,
                previousSeries = previousCatalogProgress.series.previousItems,
            )
            val now = System.currentTimeMillis()
            var completedItems = 0
            var totalItems = 6
            fun updateProgress(message: String, fetched: Int = 0, remainingSteps: Int = 0) {
                completedItems += fetched
                totalItems = maxOf(totalItems, completedItems + remainingSteps)
                val percent = if (totalItems > 0) {
                    ((completedItems.toFloat() / totalItems.toFloat()) * 100).toInt().coerceIn(0, 99)
                } else {
                    0
                }
                syncWork.update(
                    message = message,
                    progressPercent = percent,
                    currentItems = completedItems,
                    totalItems = totalItems,
                )
                _syncStatus.value = SyncStatus.Running(
                    message = message,
                    completedItems = completedItems,
                    totalItems = totalItems,
                    catalogProgress = currentCatalogProgress(),
                )
            }
            suspend fun updateSectionProgress(
                message: String,
                section: MediaSection,
                phase: SyncStatus.SyncSectionPhase,
                percent: Int,
                currentItems: Int? = null,
                fetched: Int = 0,
                remainingSteps: Int = 0,
            ) {
                when (section) {
                    MediaSection.Live -> {
                        livePhase = phase
                        livePercent = percent.coerceIn(0, 100)
                        currentItems?.let { liveItems = it }
                        liveCompleted = phase == SyncStatus.SyncSectionPhase.COMPLETED
                        liveWork.update(
                            status = phase.toNetworkActivityStatus(),
                            message = message,
                            progressPercent = percent,
                            currentItems = currentItems,
                        )
                        if (liveCompleted) liveWork.complete("Live TV ready")
                    }
                    MediaSection.Movies -> {
                        moviesPhase = phase
                        moviesPercent = percent.coerceIn(0, 100)
                        currentItems?.let { movieItems = it }
                        moviesCompleted = phase == SyncStatus.SyncSectionPhase.COMPLETED
                        moviesWork.update(
                            status = phase.toNetworkActivityStatus(),
                            message = message,
                            progressPercent = percent,
                            currentItems = currentItems,
                        )
                        if (moviesCompleted) moviesWork.complete("Movies ready")
                    }
                    MediaSection.Series -> {
                        seriesPhase = phase
                        seriesPercent = percent.coerceIn(0, 100)
                        currentItems?.let { seriesItems = it }
                        seriesCompleted = phase == SyncStatus.SyncSectionPhase.COMPLETED
                        seriesWork.update(
                            status = phase.toNetworkActivityStatus(),
                            message = message,
                            progressPercent = percent,
                            currentItems = currentItems,
                        )
                        if (seriesCompleted) seriesWork.complete("Series ready")
                    }
                }
                updateProgress(message = message, fetched = fetched, remainingSteps = remainingSteps)
            }

            database.withTransaction {
                logSyncMemory(stage = "before_get_account")
                val account = api.getAccount(credentials.username, credentials.password)
                logSyncMemory(stage = "after_get_account")
                updateProgress("Verification du compte Xtream...", fetched = 1, remainingSteps = 5)
                profileDao.upsert(account.toProfileEntity(credentials, now))

                liveItems = synchronizeLiveSection(
                    username = credentials.username,
                    password = credentials.password,
                    updateSectionProgress = ::updateSectionProgress,
                )

                movieItems = synchronizeMovieSection(
                    username = credentials.username,
                    password = credentials.password,
                    liveItems = liveItems,
                    updateSectionProgress = ::updateSectionProgress,
                )

                seriesItems = synchronizeSeriesSection(
                    username = credentials.username,
                    password = credentials.password,
                    liveItems = liveItems,
                    movieItems = movieItems,
                    updateSectionProgress = ::updateSectionProgress,
                )

                logSyncMemory(stage = "before_sync_state_write", live = liveItems, movies = movieItems, series = seriesItems)
                syncStateDao.upsert(
                    SyncStateEntity(
                        id = "catalog",
                        lastSync = now,
                        status = "success",
                        message = "Synchronisation terminee",
                    ),
                )
            }
            logSyncMemory(stage = "after_room_write", live = liveItems, movies = movieItems, series = seriesItems)
            invalidateLocalCatalogCache()
            logSyncMemory(stage = "after_cache_invalidation", live = liveItems, movies = movieItems, series = seriesItems)
            _syncStatus.value = SyncStatus.Success(
                message = "Synchronisation terminee",
                catalogProgress = currentCatalogProgress(),
            )
            syncWork.update(currentItems = liveItems + movieItems + seriesItems, progressPercent = 100)
            syncWork.complete("Catalog ready")
            logSyncMemory(stage = "xtream_sync_success", live = liveItems, movies = movieItems, series = seriesItems)
            Result.success(Unit)
        } catch (error: Exception) {
            logSyncMemory(stage = "xtream_sync_error_${error.javaClass.simpleName}")
            if (!liveCompleted && livePhase != SyncStatus.SyncSectionPhase.WAITING) {
                livePhase = SyncStatus.SyncSectionPhase.ERROR
            } else if (!moviesCompleted && moviesPhase != SyncStatus.SyncSectionPhase.WAITING) {
                moviesPhase = SyncStatus.SyncSectionPhase.ERROR
            } else if (!seriesCompleted && seriesPhase != SyncStatus.SyncSectionPhase.WAITING) {
                seriesPhase = SyncStatus.SyncSectionPhase.ERROR
            }
            syncStateDao.upsert(
                SyncStateEntity(
                    id = "catalog",
                    lastSync = syncStateDao.get()?.lastSync,
                    status = "error",
                    message = "Erreur reseau",
                ),
            )
            _syncStatus.value = SyncStatus.Error(
                message = "Erreur reseau",
                catalogProgress = currentCatalogProgress(),
            )
            val errorMessage = error.message ?: error.javaClass.simpleName
            syncWork.fail(errorMessage)
            if (!liveCompleted) liveWork.fail(errorMessage)
            if (!moviesCompleted) moviesWork.fail(errorMessage)
            if (!seriesCompleted) seriesWork.fail(errorMessage)
            Result.failure(error)
        }
    }

    private suspend fun synchronizeLiveSection(
        username: String,
        password: String,
        updateSectionProgress: suspend (String, MediaSection, SyncStatus.SyncSectionPhase, Int, Int?, Int, Int) -> Unit,
    ): Int {
        updateSectionProgress(
            "Telechargement des categories Live TV...",
            MediaSection.Live,
            SyncStatus.SyncSectionPhase.RUNNING,
            8,
            null,
            0,
            4,
        )
        logSyncMemory(stage = "before_get_live_categories")
        val liveCategories = api.getCategories(username, password, "get_live_categories")
        logSyncMemory(stage = "after_get_live_categories", liveCategories = liveCategories.size)
        updateSectionProgress(
            "Import des categories Live TV...",
            MediaSection.Live,
            SyncStatus.SyncSectionPhase.IMPORTING,
            24,
            null,
            liveCategories.size,
            4,
        )
        categoryDao.deleteByType(MediaSection.Live.storageName)
        upsertMappedInBatches(liveCategories, { it.toEntity(MediaSection.Live) }) { entities ->
            categoryDao.upsertAll(entities)
        }

        updateSectionProgress(
            "Telechargement des chaines Live TV...",
            MediaSection.Live,
            SyncStatus.SyncSectionPhase.RUNNING,
            38,
            null,
            0,
            3,
        )
        logSyncMemory(stage = "before_get_live_streams", liveCategories = liveCategories.size)
        val liveStreams = api.getLiveStreams(username, password)
        val liveItems = liveStreams.size
        logSyncMemory(stage = "after_get_live_streams", live = liveItems, liveCategories = liveCategories.size)
        updateSectionProgress(
            "Import des chaines Live TV...",
            MediaSection.Live,
            SyncStatus.SyncSectionPhase.IMPORTING,
            78,
            liveItems,
            liveItems,
            3,
        )
        mediaDao.clearLiveStreams()
        val imageBaseHost = imageBaseHost()
        upsertMappedInBatches(liveStreams, { it.toEntity(imageBaseHost) }) { entities ->
            mediaDao.upsertLiveStreams(entities)
        }
        logSyncMemory(stage = "after_live_room_write", live = liveItems)
        updateSectionProgress(
            "Live TV terminee",
            MediaSection.Live,
            SyncStatus.SyncSectionPhase.COMPLETED,
            100,
            liveItems,
            0,
            3,
        )
        return liveItems
    }

    private suspend fun synchronizeMovieSection(
        username: String,
        password: String,
        liveItems: Int,
        updateSectionProgress: suspend (String, MediaSection, SyncStatus.SyncSectionPhase, Int, Int?, Int, Int) -> Unit,
    ): Int {
        updateSectionProgress(
            "Telechargement des categories Films...",
            MediaSection.Movies,
            SyncStatus.SyncSectionPhase.RUNNING,
            8,
            null,
            0,
            2,
        )
        logSyncMemory(stage = "before_get_movie_categories", live = liveItems)
        val movieCategories = api.getCategories(username, password, "get_vod_categories")
        logSyncMemory(stage = "after_get_movie_categories", live = liveItems, movieCategories = movieCategories.size)
        updateSectionProgress(
            "Import des categories Films...",
            MediaSection.Movies,
            SyncStatus.SyncSectionPhase.IMPORTING,
            24,
            null,
            movieCategories.size,
            2,
        )
        categoryDao.deleteByType(MediaSection.Movies.storageName)
        upsertMappedInBatches(movieCategories, { it.toEntity(MediaSection.Movies) }) { entities ->
            categoryDao.upsertAll(entities)
        }

        updateSectionProgress(
            "Telechargement des films...",
            MediaSection.Movies,
            SyncStatus.SyncSectionPhase.RUNNING,
            38,
            null,
            0,
            1,
        )
        logSyncMemory(stage = "before_get_movies", live = liveItems, movieCategories = movieCategories.size)
        var movies = api.getMovies(username, password)
        if (movies.isEmpty() && movieCategories.isNotEmpty()) {
            updateSectionProgress(
                "Telechargement des films par categories...",
                MediaSection.Movies,
                SyncStatus.SyncSectionPhase.RUNNING,
                44,
                null,
                0,
                1,
            )
            logSyncMemory(stage = "before_get_movies_by_category", live = liveItems, movieCategories = movieCategories.size)
            movies = movieCategories
                .mapNotNull { category -> category.id?.takeIf { it.isNotBlank() } }
                .flatMap { categoryId ->
                    api.getMovies(username = username, password = password, categoryId = categoryId)
                        .map { movie ->
                            if (movie.categoryId.isNullOrBlank()) movie.copy(categoryId = categoryId) else movie
                        }
                }
                .distinctBy { it.streamId }
            logSyncMemory(stage = "after_get_movies_by_category", live = liveItems, movies = movies.size, movieCategories = movieCategories.size)
        }
        val movieItems = movies.size
        logSyncMemory(stage = "after_get_movies", live = liveItems, movies = movieItems, movieCategories = movieCategories.size)
        updateSectionProgress(
            "Import des films...",
            MediaSection.Movies,
            SyncStatus.SyncSectionPhase.IMPORTING,
            72,
            movieItems,
            movieItems,
            1,
        )
        mediaDao.clearMovies()
        val imageBaseHost = imageBaseHost()
        upsertMappedInBatches(movies, { it.toEntity(imageBaseHost) }) { entities ->
            mediaDao.upsertMovies(entities)
        }
        updateSectionProgress(
            "Films termines",
            MediaSection.Movies,
            SyncStatus.SyncSectionPhase.COMPLETED,
            100,
            movieItems,
            0,
            1,
        )
        logSyncMemory(stage = "after_movies_room_write", live = liveItems, movies = movieItems)
        return movieItems
    }

    private suspend fun synchronizeSeriesSection(
        username: String,
        password: String,
        liveItems: Int,
        movieItems: Int,
        updateSectionProgress: suspend (String, MediaSection, SyncStatus.SyncSectionPhase, Int, Int?, Int, Int) -> Unit,
    ): Int {
        updateSectionProgress(
            "Telechargement des categories Series...",
            MediaSection.Series,
            SyncStatus.SyncSectionPhase.RUNNING,
            8,
            null,
            0,
            1,
        )
        logSyncMemory(stage = "before_get_series_categories", live = liveItems, movies = movieItems)
        val seriesCategories = api.getCategories(username, password, "get_series_categories")
        logSyncMemory(
            stage = "after_get_series_categories",
            live = liveItems,
            movies = movieItems,
            seriesCategories = seriesCategories.size,
        )
        updateSectionProgress(
            "Import des categories Series...",
            MediaSection.Series,
            SyncStatus.SyncSectionPhase.IMPORTING,
            24,
            null,
            seriesCategories.size,
            1,
        )
        categoryDao.deleteByType(MediaSection.Series.storageName)
        upsertMappedInBatches(seriesCategories, { it.toEntity(MediaSection.Series) }) { entities ->
            categoryDao.upsertAll(entities)
        }

        updateSectionProgress(
            "Telechargement des series...",
            MediaSection.Series,
            SyncStatus.SyncSectionPhase.RUNNING,
            38,
            null,
            0,
            0,
        )
        logSyncMemory(stage = "before_get_series", live = liveItems, movies = movieItems, seriesCategories = seriesCategories.size)
        val series = api.getSeries(username, password)
        val seriesItems = series.size
        logSyncMemory(stage = "after_get_series", live = liveItems, movies = movieItems, series = seriesItems)
        updateSectionProgress(
            "Import des series...",
            MediaSection.Series,
            SyncStatus.SyncSectionPhase.IMPORTING,
            70,
            seriesItems,
            seriesItems,
            0,
        )
        mediaDao.clearSeries()
        val imageBaseHost = imageBaseHost()
        upsertMappedInBatches(series, { it.toEntity(imageBaseHost) }) { entities ->
            mediaDao.upsertSeries(entities)
        }
        updateSectionProgress(
            "Series terminees",
            MediaSection.Series,
            SyncStatus.SyncSectionPhase.COMPLETED,
            100,
            seriesItems,
            0,
            0,
        )
        logSyncMemory(stage = "after_series_room_write", live = liveItems, movies = movieItems, series = seriesItems)
        return seriesItems
    }

    override suspend fun toggleFavorite(contentType: String, contentId: String) = withContext(Dispatchers.IO) {
        val existing = favoriteDao.get(contentType, contentId)
        if (existing == null) {
            favoriteDao.upsert(FavoriteEntity(contentType, contentId, System.currentTimeMillis()))
        } else {
            favoriteDao.delete(contentType, contentId)
        }
    }

    override suspend fun getSeriesEpisodes(seriesId: Int): List<Episode> = withContext(Dispatchers.IO) {
        mediaDao.getEpisodes(seriesId).map { it.toDomain() }
    }

    override suspend fun buildPlaybackRequest(kind: PlaybackKind, id: String): PlaybackRequest? =
        withContext(Dispatchers.IO) {
            when (kind) {
                PlaybackKind.Live -> {
                    val streamId = id.toIntOrNull() ?: return@withContext null
                    val local = mediaDao.getLiveStream(streamId)
                    val title = local?.name ?: return@withContext null
                    val subtitle = local.categoryId ?: "Live TV"
                    PlaybackRequest(kind, id, title, subtitle, local.directStreamUrl?.takeIf { it.isNotBlank() } ?: urlFactory.live(streamId))
                }

                PlaybackKind.Movie -> {
                    val streamId = id.toIntOrNull() ?: return@withContext null
                    val local = mediaDao.getMovie(streamId)
                    val title = local?.title ?: return@withContext null
                    val extension = local.containerExtension ?: "mp4"
                    val progress = progressDao.get(kind.routeName, id)
                    PlaybackRequest(kind, id, title, local.categoryId ?: "Film", urlFactory.movie(streamId, extension), progress?.positionMs ?: 0L)
                }

                PlaybackKind.Episode -> {
                    val episodeId = id.toIntOrNull() ?: return@withContext null
                    val local = mediaDao.getEpisode(episodeId)
                    val title = local?.title ?: return@withContext null
                    val extension = local.containerExtension ?: "mp4"
                    val progress = progressDao.get(kind.routeName, id)
                    PlaybackRequest(kind, id, title, "Episode", urlFactory.episode(episodeId, extension), progress?.positionMs ?: 0L)
                }
            }
        }

    override suspend fun savePlaybackProgress(
        kind: PlaybackKind,
        id: String,
        positionMs: Long,
        durationMs: Long,
    ) = withContext(Dispatchers.IO) {
        if (kind == PlaybackKind.Live) return@withContext
        progressDao.upsert(
            PlaybackProgressEntity(
                contentType = kind.routeName,
                contentId = id,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun synchronizeM3u(): Result<Unit> {
        val m3uUrl = accountManager.m3uUrl.value
        if (m3uUrl.isBlank()) {
            val message = "Aucun lien M3U configure"
            _syncStatus.value = SyncStatus.Error(message)
            return Result.failure(IllegalStateException(message))
        }
        val syncWorkId = "catalog-m3u-${System.currentTimeMillis()}"
        val syncWork = networkActivityTracker.begin(
            id = syncWorkId,
            title = "Catalog synchronization",
            type = NetworkActivityType.Catalog,
            message = "Synchronisation M3U...",
            source = "M3U",
            progressPercent = 8,
        )
        val liveWork = networkActivityTracker.beginCatalogSectionWork(syncWorkId, "Live TV")
        liveWork.update(
            status = NetworkActivityStatus.Running,
            message = "Downloading M3U playlist",
            progressPercent = 8,
        )
        _syncStatus.value = SyncStatus.Running(
            message = "Synchronisation M3U...",
            totalItems = 2,
            catalogProgress = SyncStatus.CatalogProgress(
                live = SyncStatus.SyncSectionProgress(
                    phase = SyncStatus.SyncSectionPhase.RUNNING,
                    progressPercent = 8,
                ),
            ),
        )
        return try {
            val now = System.currentTimeMillis()
            val playlist = m3uPlaylistClient.fetch(m3uUrl)
            syncWork.update(message = "Chargement catalogue M3U...", progressPercent = 50, currentItems = 1, totalItems = 2)
            liveWork.update(
                status = NetworkActivityStatus.Importing,
                message = "Importing M3U channels",
                progressPercent = 72,
                currentItems = playlist.channels.size,
            )
            _syncStatus.value = SyncStatus.Running(
                message = "Chargement catalogue M3U...",
                completedItems = 1,
                totalItems = 2,
                catalogProgress = SyncStatus.CatalogProgress(
                    live = SyncStatus.SyncSectionProgress(
                        currentItems = playlist.channels.size,
                        phase = SyncStatus.SyncSectionPhase.IMPORTING,
                        progressPercent = 72,
                    ),
                ),
            )
            categoryDao.deleteByType(MediaSection.Live.storageName)
            categoryDao.upsertAll(playlist.categories)
            mediaDao.clearLiveStreams()
            mediaDao.upsertLiveStreams(playlist.channels.map { it.toEntity() })
            categoryDao.deleteByType(MediaSection.Movies.storageName)
            categoryDao.deleteByType(MediaSection.Series.storageName)
            mediaDao.clearMovies()
            mediaDao.clearSeries()
            accountManager.epgUrl.value.takeIf { it.isNotBlank() }?.let { epgRepository.synchronize(it) }
            syncStateDao.upsert(
                SyncStateEntity(
                    id = "catalog",
                    lastSync = now,
                    status = "success",
                    message = "Synchronisation M3U terminee",
                ),
            )
            invalidateLocalCatalogCache()
            _syncStatus.value = SyncStatus.Success(
                message = "Synchronisation M3U terminee",
                catalogProgress = SyncStatus.CatalogProgress(
                    live = SyncStatus.SyncSectionProgress(
                        currentItems = playlist.channels.size,
                        completed = true,
                        phase = SyncStatus.SyncSectionPhase.COMPLETED,
                        progressPercent = 100,
                    ),
                ),
            )
            liveWork.update(progressPercent = 100, currentItems = playlist.channels.size)
            liveWork.complete("Live TV ready")
            syncWork.update(progressPercent = 100, currentItems = playlist.channels.size)
            syncWork.complete("Catalog ready")
            Result.success(Unit)
        } catch (error: Exception) {
            _syncStatus.value = SyncStatus.Error(
                "Synchronisation M3U indisponible",
                catalogProgress = SyncStatus.CatalogProgress(
                    live = SyncStatus.SyncSectionProgress(
                        phase = SyncStatus.SyncSectionPhase.ERROR,
                    ),
                ),
            )
            val errorMessage = error.message ?: error.javaClass.simpleName
            liveWork.fail(errorMessage)
            syncWork.fail(errorMessage)
            Result.failure(error)
        }
    }
}

private fun PlaylistSource.hasConfiguredCatalog(m3uUrl: String, hasXtream: Boolean): Boolean =
    when (this) {
        PlaylistSource.Xtream -> hasXtream
        PlaylistSource.M3u -> m3uUrl.isNotBlank()
    }

private fun NetworkActivityTracker.beginCatalogSectionWork(syncWorkId: String, section: String): NetworkActivityHandle =
    begin(
        id = "$syncWorkId-${section.lowercase().replace(' ', '-')}",
        title = "Catalog $section",
        type = NetworkActivityType.Catalog,
        section = section,
        message = "Waiting",
        status = NetworkActivityStatus.Queued,
        progressPercent = 0,
    )

private fun SyncStatus.SyncSectionPhase.toNetworkActivityStatus(): NetworkActivityStatus =
    when (this) {
        SyncStatus.SyncSectionPhase.WAITING -> NetworkActivityStatus.Queued
        SyncStatus.SyncSectionPhase.RUNNING,
        SyncStatus.SyncSectionPhase.LOADING_TRENDS -> NetworkActivityStatus.Running
        SyncStatus.SyncSectionPhase.IMPORTING -> NetworkActivityStatus.Importing
        SyncStatus.SyncSectionPhase.COMPLETED -> NetworkActivityStatus.Completed
        SyncStatus.SyncSectionPhase.ERROR -> NetworkActivityStatus.Error
    }

private fun TrendingCatalogItem.containsAdultMarker(): Boolean =
    listOf(title, categoryName)
        .any { value -> AdultContentPattern.containsMatchIn(value) }

private fun randomTrendCandidateLimit(limit: Int): Int =
    (limit * RandomTrendCandidateMultiplier)
        .coerceAtLeast(limit)
        .coerceAtMost(RandomTrendCandidateMax)

private suspend fun <Remote, Local> upsertMappedInBatches(
    items: List<Remote>,
    mapper: (Remote) -> Local?,
    upsert: suspend (List<Local>) -> Unit,
) {
    var index = 0
    while (index < items.size) {
        val end = minOf(index + SyncInsertBatchSize, items.size)
        val mapped = ArrayList<Local>(end - index)
        for (itemIndex in index until end) {
            mapper(items[itemIndex])?.let(mapped::add)
        }
        if (mapped.isNotEmpty()) {
            upsert(mapped)
        }
        index = end
    }
}

private fun logSyncMemory(
    stage: String,
    live: Int? = null,
    movies: Int? = null,
    series: Int? = null,
    liveCategories: Int? = null,
    movieCategories: Int? = null,
    seriesCategories: Int? = null,
    previousLive: Int? = null,
    previousMovies: Int? = null,
    previousSeries: Int? = null,
) {
    val runtime = Runtime.getRuntime()
    val total = runtime.totalMemory()
    val free = runtime.freeMemory()
    val used = total - free
    Log.i(
        SyncMemoryTag,
        buildString {
            append("stage=").append(stage)
            append(" usedMb=").append(used.toMiB())
            append(" freeMb=").append(free.toMiB())
            append(" totalMb=").append(total.toMiB())
            append(" maxMb=").append(runtime.maxMemory().toMiB())
            live?.let { append(" live=").append(it) }
            movies?.let { append(" movies=").append(it) }
            series?.let { append(" series=").append(it) }
            liveCategories?.let { append(" liveCategories=").append(it) }
            movieCategories?.let { append(" movieCategories=").append(it) }
            seriesCategories?.let { append(" seriesCategories=").append(it) }
            previousLive?.let { append(" previousLive=").append(it) }
            previousMovies?.let { append(" previousMovies=").append(it) }
            previousSeries?.let { append(" previousSeries=").append(it) }
        },
    )
}

private fun Long.toMiB(): Long = this / (1024L * 1024L)

private fun String.toSqlLikeContainsPattern(): String =
    buildString {
        append('%')
        this@toSqlLikeContainsPattern.forEach { char ->
            when (char) {
                '\\', '%', '_' -> {
                    append('\\')
                    append(char)
                }
                else -> append(char)
            }
        }
        append('%')
    }

private const val SyncInsertBatchSize = 500
private const val CatalogPageMaxLimit = 500
private const val SyncMemoryTag = "SVSyncMemory"
private const val TrendingMovieType = "movie"
private const val TrendingSeriesType = "series"
private const val RandomTrendCandidateMultiplier = 5
private const val RandomTrendCandidateMax = 100
private val AdultContentPattern = Regex(
    "(^|[^a-z0-9])(adult|adults|adulte|porn|porno|xxx|erotic|erotique|sex|sexy|18\\+)([^a-z0-9]|$)",
    RegexOption.IGNORE_CASE,
)

private fun LiveChannel.withEpg(epgRepository: EpgRepository): LiveChannel {
    val programs = epgRepository.loadPrograms(epgChannelId, name)
    val now = System.currentTimeMillis()
    val current = programs.firstOrNull { program ->
        val start = program.startMillis
        val stop = program.stopMillis
        start != null && stop != null && now in start..stop
    } ?: programs.firstOrNull()
    return if (current == null) {
        this
    } else {
        copy(
            currentProgram = current.title,
            timeRange = current.timeRange,
        )
    }
}

internal fun emptyAccountProfile() = AccountProfile(
    id = "primary",
    name = "Aucun compte",
    host = "",
    usernameMasked = "",
    status = "Non configure",
    expirationDate = null,
    activeConnections = null,
    maxConnections = null,
    lastSync = null,
    liveCount = 0,
    movieCount = 0,
    seriesCount = 0,
)
