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
import com.smartvision.svplayer.data.local.entity.TrendingMediaEntity
import com.smartvision.svplayer.data.playlist.EpgRepository
import com.smartvision.svplayer.data.playlist.M3uPlaylistClient
import com.smartvision.svplayer.data.remote.XtreamApiService
import com.smartvision.svplayer.data.remote.XtreamUrlFactory
import com.smartvision.svplayer.data.remote.dto.XtreamCategoryDto
import com.smartvision.svplayer.data.remote.dto.XtreamEpisodeDto
import com.smartvision.svplayer.data.remote.dto.XtreamMovieDto
import com.smartvision.svplayer.data.remote.dto.XtreamSeriesDto
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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

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
            categories.map { it.toDomain(MediaSection.Live, counts[it.id] ?: 0) }
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
            streams.map { it.toDomain(names[it.categoryId] ?: "Live TV").withEpg(epgRepository) }
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
            categories.map { it.toDomain(MediaSection.Movies, counts[it.id] ?: 0) }
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
            movies.map { it.toDomain(names[it.categoryId] ?: "Films") }
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
            categories.map { it.toDomain(MediaSection.Series, counts[it.id] ?: 0) }
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
            series.map { it.toDomain(names[it.categoryId] ?: "Series") }
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
            val categoryNames = categoryDao.getByType(MediaSection.Live.storageName).associate { it.id to it.name }
            val streams = categoryId
                ?.takeIf { it.isNotBlank() }
                ?.let { mediaDao.getLiveStreamsByCategoryPage(it, safeLimit, safeOffset) }
                ?: mediaDao.getLiveStreamsPage(safeLimit, safeOffset)
            streams.map { stream ->
                stream.toDomain(categoryNames[stream.categoryId] ?: "Live TV").withEpg(epgRepository)
            }
        }

    override suspend fun getMoviesPage(categoryId: String?, offset: Int, limit: Int): List<Movie> =
        withContext(Dispatchers.IO) {
            if (!isXtreamCatalogConfigured()) return@withContext emptyList()
            val safeLimit = limit.coerceIn(1, CatalogPageMaxLimit)
            val safeOffset = offset.coerceAtLeast(0)
            val categoryNames = categoryDao.getByType(MediaSection.Movies.storageName).associate { it.id to it.name }
            val movies = categoryId
                ?.takeIf { it.isNotBlank() }
                ?.let { mediaDao.getMoviesByCategoryPage(it, safeLimit, safeOffset) }
                ?: mediaDao.getMoviesPage(safeLimit, safeOffset)
            movies.map { movie -> movie.toDomain(categoryNames[movie.categoryId] ?: "Films") }
        }

    override suspend fun getSeriesPage(categoryId: String?, offset: Int, limit: Int): List<TvSeries> =
        withContext(Dispatchers.IO) {
            if (!isXtreamCatalogConfigured()) return@withContext emptyList()
            val safeLimit = limit.coerceIn(1, CatalogPageMaxLimit)
            val safeOffset = offset.coerceAtLeast(0)
            val categoryNames = categoryDao.getByType(MediaSection.Series.storageName).associate { it.id to it.name }
            val series = categoryId
                ?.takeIf { it.isNotBlank() }
                ?.let { mediaDao.getSeriesByCategoryPage(it, safeLimit, safeOffset) }
                ?: mediaDao.getSeriesPage(safeLimit, safeOffset)
            series.map { item -> item.toDomain(categoryNames[item.categoryId] ?: "Series") }
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
            mediaDao.getLiveStreamsByIds(distinctIds)
                .sortedBy { order[it.streamId] ?: Int.MAX_VALUE }
                .map { stream -> stream.toDomain(categoryNames[stream.categoryId] ?: "Live TV").withEpg(epgRepository) }
        }

    override suspend fun getMoviesByIds(streamIds: List<Int>): List<Movie> =
        withContext(Dispatchers.IO) {
            if (streamIds.isEmpty() || !isXtreamCatalogConfigured()) return@withContext emptyList()
            val distinctIds = streamIds.distinct()
            val order = distinctIds.withIndex().associate { it.value to it.index }
            val categoryNames = categoryDao.getByType(MediaSection.Movies.storageName).associate { it.id to it.name }
            mediaDao.getMoviesByIds(distinctIds)
                .sortedBy { order[it.streamId] ?: Int.MAX_VALUE }
                .map { movie -> movie.toDomain(categoryNames[movie.categoryId] ?: "Films") }
        }

    override suspend fun getSeriesByIds(seriesIds: List<Int>): List<TvSeries> =
        withContext(Dispatchers.IO) {
            if (seriesIds.isEmpty() || !isXtreamCatalogConfigured()) return@withContext emptyList()
            val distinctIds = seriesIds.distinct()
            val order = distinctIds.withIndex().associate { it.value to it.index }
            val categoryNames = categoryDao.getByType(MediaSection.Series.storageName).associate { it.id to it.name }
            mediaDao.getSeriesByIds(distinctIds)
                .sortedBy { order[it.seriesId] ?: Int.MAX_VALUE }
                .map { series -> series.toDomain(categoryNames[series.categoryId] ?: "Series") }
        }

    override suspend fun getTrendingMovies(limit: Int): List<Movie> = withContext(Dispatchers.IO) {
        if (accountManager.activePlaylistSource.value != PlaylistSource.Xtream || accountManager.accounts.value.isEmpty()) {
            return@withContext emptyList()
        }
        val categoryNames = categoryDao.getByType(MediaSection.Movies.storageName).associate { it.id to it.name }
        mediaDao.getTrendingMovies(limit)
            .map { movie -> movie.toDomain(categoryNames[movie.categoryId] ?: "Films") }
    }

    override suspend fun getTrendingSeries(limit: Int): List<TvSeries> = withContext(Dispatchers.IO) {
        if (accountManager.activePlaylistSource.value != PlaylistSource.Xtream || accountManager.accounts.value.isEmpty()) {
            return@withContext emptyList()
        }
        val categoryNames = categoryDao.getByType(MediaSection.Series.storageName).associate { it.id to it.name }
        mediaDao.getTrendingSeries(limit)
            .map { series -> series.toDomain(categoryNames[series.categoryId] ?: "Series") }
    }

    override suspend fun getTrendingMovieItems(limit: Int): List<TrendingCatalogItem> = withContext(Dispatchers.IO) {
        if (accountManager.activePlaylistSource.value != PlaylistSource.Xtream || accountManager.accounts.value.isEmpty()) {
            return@withContext emptyList()
        }
        val categoryNames = categoryDao.getByType(MediaSection.Movies.storageName).associate { it.id to it.name }
        val movies = mediaDao.getTrendingMovies(limit)
        movies.map { movie ->
            TrendingCatalogItem(
                contentType = TrendingMovieType,
                contentId = movie.streamId,
                title = movie.title,
                categoryName = categoryNames[movie.categoryId] ?: "Films",
                posterUrl = movie.posterUrl,
                rating = movie.rating,
                year = movie.year,
                previewUrl = urlFactory.movie(movie.streamId, movie.containerExtension),
            )
        }
    }

    override suspend fun getTrendingSeriesItems(limit: Int): List<TrendingCatalogItem> = withContext(Dispatchers.IO) {
        if (accountManager.activePlaylistSource.value != PlaylistSource.Xtream || accountManager.accounts.value.isEmpty()) {
            return@withContext emptyList()
        }
        val categoryNames = categoryDao.getByType(MediaSection.Series.storageName).associate { it.id to it.name }
        val previewBySeriesId = mediaDao.getTrendingMedia(TrendingSeriesType).associateBy { it.contentId }
        val seriesItems = mediaDao.getTrendingSeries(limit)
        seriesItems.map { series ->
            val preview = previewBySeriesId[series.seriesId]
            TrendingCatalogItem(
                contentType = TrendingSeriesType,
                contentId = series.seriesId,
                title = series.title,
                categoryName = categoryNames[series.categoryId] ?: "Series",
                posterUrl = series.posterUrl,
                rating = series.rating,
                year = series.year,
                previewUrl = preview?.sampleContentId?.let { episodeId ->
                    urlFactory.episode(episodeId, preview.sampleExtension.orEmpty().ifBlank { "mp4" })
                },
            )
        }
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
        var liveItems = 0
        var movieItems = 0
        var seriesItems = 0
        var liveCompleted = false
        var moviesCompleted = false
        var seriesCompleted = false
        fun currentCatalogProgress(): SyncStatus.CatalogProgress =
            SyncStatus.CatalogProgress(
                live = previousCatalogProgress.live.copy(currentItems = liveItems, completed = liveCompleted),
                movies = previousCatalogProgress.movies.copy(currentItems = movieItems, completed = moviesCompleted),
                series = previousCatalogProgress.series.copy(currentItems = seriesItems, completed = seriesCompleted),
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
                _syncStatus.value = SyncStatus.Running(
                    message = message,
                    completedItems = completedItems,
                    totalItems = totalItems,
                    catalogProgress = currentCatalogProgress(),
                )
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
                    updateProgress = ::updateProgress,
                )
                liveCompleted = true

                movieItems = synchronizeMovieSection(
                    username = credentials.username,
                    password = credentials.password,
                    liveItems = liveItems,
                    updateProgress = ::updateProgress,
                )
                moviesCompleted = true

                seriesItems = synchronizeSeriesSection(
                    username = credentials.username,
                    password = credentials.password,
                    liveItems = liveItems,
                    movieItems = movieItems,
                    updateProgress = ::updateProgress,
                )
                seriesCompleted = true

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
            logSyncMemory(stage = "xtream_sync_success", live = liveItems, movies = movieItems, series = seriesItems)
            Result.success(Unit)
        } catch (error: Exception) {
            logSyncMemory(stage = "xtream_sync_error_${error.javaClass.simpleName}")
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
            Result.failure(error)
        }
    }

    private suspend fun synchronizeLiveSection(
        username: String,
        password: String,
        updateProgress: suspend (String, Int, Int) -> Unit,
    ): Int {
        logSyncMemory(stage = "before_get_live_categories")
        val liveCategories = api.getCategories(username, password, "get_live_categories")
        logSyncMemory(stage = "after_get_live_categories", liveCategories = liveCategories.size)
        updateProgress("Telechargement des categories Live TV...", liveCategories.size, 4)
        categoryDao.deleteByType(MediaSection.Live.storageName)
        upsertMappedInBatches(liveCategories, { it.toEntity(MediaSection.Live) }) { entities ->
            categoryDao.upsertAll(entities)
        }

        logSyncMemory(stage = "before_get_live_streams", liveCategories = liveCategories.size)
        val liveStreams = api.getLiveStreams(username, password)
        val liveItems = liveStreams.size
        logSyncMemory(stage = "after_get_live_streams", live = liveItems, liveCategories = liveCategories.size)
        updateProgress("Telechargement des chaines Live TV...", liveItems, 3)
        mediaDao.clearLiveStreams()
        upsertMappedInBatches(liveStreams, { it.toEntity() }) { entities ->
            mediaDao.upsertLiveStreams(entities)
        }
        logSyncMemory(stage = "after_live_room_write", live = liveItems)
        return liveItems
    }

    private suspend fun synchronizeMovieSection(
        username: String,
        password: String,
        liveItems: Int,
        updateProgress: suspend (String, Int, Int) -> Unit,
    ): Int {
        logSyncMemory(stage = "before_get_movie_categories", live = liveItems)
        val movieCategories = api.getCategories(username, password, "get_vod_categories")
        logSyncMemory(stage = "after_get_movie_categories", live = liveItems, movieCategories = movieCategories.size)
        updateProgress("Telechargement des categories Films...", movieCategories.size, 2)
        categoryDao.deleteByType(MediaSection.Movies.storageName)
        upsertMappedInBatches(movieCategories, { it.toEntity(MediaSection.Movies) }) { entities ->
            categoryDao.upsertAll(entities)
        }

        logSyncMemory(stage = "before_get_movies", live = liveItems, movieCategories = movieCategories.size)
        val movies = api.getMovies(username, password)
        val movieItems = movies.size
        logSyncMemory(stage = "after_get_movies", live = liveItems, movies = movieItems, movieCategories = movieCategories.size)
        updateProgress("Telechargement des films...", movieItems, 1)
        val previousTrendingIds = mediaDao.getTrendingContentIds(TrendingMovieType).toSet()
        mediaDao.clearMovies()
        upsertMappedInBatches(movies, { it.toEntity() }) { entities ->
            mediaDao.upsertMovies(entities)
        }
        val trendingItems = updateTrendingMovies(
            categories = movieCategories,
            movies = movies,
            previousTrendingIds = previousTrendingIds,
            now = System.currentTimeMillis(),
        )
        updateProgress("Mise a jour tendances films...", trendingItems, 1)
        logSyncMemory(stage = "after_movies_room_write", live = liveItems, movies = movieItems)
        return movieItems
    }

    private suspend fun synchronizeSeriesSection(
        username: String,
        password: String,
        liveItems: Int,
        movieItems: Int,
        updateProgress: suspend (String, Int, Int) -> Unit,
    ): Int {
        logSyncMemory(stage = "before_get_series_categories", live = liveItems, movies = movieItems)
        val seriesCategories = api.getCategories(username, password, "get_series_categories")
        logSyncMemory(
            stage = "after_get_series_categories",
            live = liveItems,
            movies = movieItems,
            seriesCategories = seriesCategories.size,
        )
        updateProgress("Telechargement des categories Series...", seriesCategories.size, 1)
        categoryDao.deleteByType(MediaSection.Series.storageName)
        upsertMappedInBatches(seriesCategories, { it.toEntity(MediaSection.Series) }) { entities ->
            categoryDao.upsertAll(entities)
        }

        logSyncMemory(stage = "before_get_series", live = liveItems, movies = movieItems, seriesCategories = seriesCategories.size)
        val series = api.getSeries(username, password)
        val seriesItems = series.size
        logSyncMemory(stage = "after_get_series", live = liveItems, movies = movieItems, series = seriesItems)
        updateProgress("Telechargement des series...", seriesItems, 0)
        val previousTrendingIds = mediaDao.getTrendingContentIds(TrendingSeriesType).toSet()
        mediaDao.clearSeries()
        upsertMappedInBatches(series, { it.toEntity() }) { entities ->
            mediaDao.upsertSeries(entities)
        }
        val trendingItems = updateTrendingSeries(
            username = username,
            password = password,
            categories = seriesCategories,
            series = series,
            previousTrendingIds = previousTrendingIds,
            now = System.currentTimeMillis(),
        )
        updateProgress("Mise a jour tendances series...", trendingItems, 0)
        logSyncMemory(stage = "after_series_room_write", live = liveItems, movies = movieItems, series = seriesItems)
        return seriesItems
    }

    private suspend fun updateTrendingMovies(
        categories: List<XtreamCategoryDto>,
        movies: List<XtreamMovieDto>,
        previousTrendingIds: Set<Int>,
        now: Long,
    ): Int {
        val categoryNames = categories.categoryNameById()
        val candidates = movies
            .mapNotNull { movie -> movie.toMovieTrendCandidate(categoryNames) }
            .bestRatedTrendCandidates(previousTrendingIds)
        val verified = candidates
            .asSequence()
            .take(TrendValidationScanLimit)
            .mapNotNull { candidate ->
                val url = urlFactory.movie(candidate.contentId, candidate.extension.orEmpty().ifBlank { "mp4" })
                candidate.takeIf { isPlayableMediaUrl(url) }
            }
            .take(TrendStorageLimit)
            .toList()

        if (verified.isNotEmpty()) {
            mediaDao.clearTrendingMedia(TrendingMovieType)
            mediaDao.upsertTrendingMedia(
                verified.map { candidate ->
                    candidate.toEntity(contentType = TrendingMovieType, now = now)
                },
            )
        }
        logSyncMemory(stage = "after_trending_movies_update", movies = verified.size)
        return verified.size
    }

    private suspend fun updateTrendingSeries(
        username: String,
        password: String,
        categories: List<XtreamCategoryDto>,
        series: List<XtreamSeriesDto>,
        previousTrendingIds: Set<Int>,
        now: Long,
    ): Int {
        val categoryNames = categories.categoryNameById()
        val candidates = series
            .mapNotNull { item -> item.toSeriesTrendCandidate(categoryNames) }
            .bestRatedTrendCandidates(previousTrendingIds)
        val verified = mutableListOf<TrendingMediaEntity>()

        for (candidate in candidates.take(TrendValidationScanLimit)) {
            if (verified.size >= TrendStorageLimit) break
            val previewEpisode = runCatching {
                api.getSeriesInfo(username, password, seriesId = candidate.contentId)
                    .episodes
                    .firstPlayableEpisode()
            }.getOrNull() ?: continue
            val url = urlFactory.episode(
                previewEpisode.episodeId,
                previewEpisode.extension.ifBlank { "mp4" },
            )
            if (isPlayableMediaUrl(url)) {
                verified += candidate.toEntity(
                    contentType = TrendingSeriesType,
                    now = now,
                    sampleContentId = previewEpisode.episodeId,
                    sampleExtension = previewEpisode.extension,
                )
            }
        }

        if (verified.isNotEmpty()) {
            mediaDao.clearTrendingMedia(TrendingSeriesType)
            mediaDao.upsertTrendingMedia(verified)
        }
        logSyncMemory(stage = "after_trending_series_update", series = verified.size)
        return verified.size
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
        _syncStatus.value = SyncStatus.Running(message = "Synchronisation M3U...", totalItems = 2)
        return try {
            val now = System.currentTimeMillis()
            val playlist = m3uPlaylistClient.fetch(m3uUrl)
            _syncStatus.value = SyncStatus.Running(
                message = "Chargement catalogue M3U...",
                completedItems = 1,
                totalItems = 2,
                catalogProgress = SyncStatus.CatalogProgress(
                    live = SyncStatus.SyncSectionProgress(currentItems = playlist.channels.size),
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
                    live = SyncStatus.SyncSectionProgress(currentItems = playlist.channels.size, completed = true),
                ),
            )
            Result.success(Unit)
        } catch (error: Exception) {
            _syncStatus.value = SyncStatus.Error("Synchronisation M3U indisponible")
            Result.failure(error)
        }
    }
}

private fun PlaylistSource.hasConfiguredCatalog(m3uUrl: String, hasXtream: Boolean): Boolean =
    when (this) {
        PlaylistSource.Xtream -> hasXtream
        PlaylistSource.M3u -> m3uUrl.isNotBlank()
    }

private data class TrendCandidate(
    val contentId: Int,
    val title: String,
    val categoryName: String,
    val genre: String?,
    val plot: String?,
    val rating: Float,
    val extension: String? = null,
)

private data class EpisodePreview(
    val episodeId: Int,
    val extension: String,
)

private fun List<XtreamCategoryDto>.categoryNameById(): Map<String, String> =
    mapNotNull { category ->
        val id = category.id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        id to category.name.orEmpty()
    }.toMap()

private fun XtreamMovieDto.toMovieTrendCandidate(categoryNames: Map<String, String>): TrendCandidate? {
    val safeId = streamId ?: return null
    return TrendCandidate(
        contentId = safeId,
        title = name.orEmpty(),
        categoryName = categoryNames[categoryId].orEmpty(),
        genre = null,
        plot = null,
        rating = movieRatingOutOf10(),
        extension = containerExtension.orEmpty().ifBlank { "mp4" },
    )
}

private fun XtreamSeriesDto.toSeriesTrendCandidate(categoryNames: Map<String, String>): TrendCandidate? {
    val safeId = seriesId ?: return null
    return TrendCandidate(
        contentId = safeId,
        title = name.orEmpty(),
        categoryName = categoryNames[categoryId].orEmpty(),
        genre = genre,
        plot = plot,
        rating = rating.toRatingValue(),
    )
}

private fun XtreamMovieDto.movieRatingOutOf10(): Float {
    val regular = rating.toRatingValue()
    val fiveBased = ratingFiveBased.toRatingValue().let { value ->
        if (value in 0.01f..5.0f) value * 2f else value
    }
    return maxOf(regular, fiveBased).coerceIn(0f, 10f)
}

private fun String?.toRatingValue(): Float =
    this
        ?.replace(',', '.')
        ?.toFloatOrNull()
        ?.coerceIn(0f, 10f)
        ?: 0f

private fun List<TrendCandidate>.bestRatedTrendCandidates(previousTrendingIds: Set<Int>): List<TrendCandidate> {
    val clean = filter { candidate ->
        candidate.rating >= TrendMinimumFallbackRating && !candidate.containsAdultMarker()
    }
    val perfect = clean.filter { it.rating >= TrendPerfectRatingFloor }
    val fallback = clean.filter { it.rating >= TrendMinimumFallbackRating && it.rating < TrendPerfectRatingFloor }
    return perfect.preferFresh(previousTrendingIds) + fallback.preferFresh(previousTrendingIds)
}

private fun List<TrendCandidate>.preferFresh(previousTrendingIds: Set<Int>): List<TrendCandidate> {
    val shuffled = shuffled()
    val (fresh, existing) = shuffled.partition { it.contentId !in previousTrendingIds }
    return fresh + existing
}

private fun TrendCandidate.containsAdultMarker(): Boolean =
    listOf(title, categoryName, genre.orEmpty(), plot.orEmpty())
        .any { value -> AdultContentPattern.containsMatchIn(value) }

private fun TrendCandidate.toEntity(
    contentType: String,
    now: Long,
    sampleContentId: Int? = null,
    sampleExtension: String? = null,
): TrendingMediaEntity =
    TrendingMediaEntity(
        contentType = contentType,
        contentId = contentId,
        sampleContentId = sampleContentId,
        sampleExtension = sampleExtension,
        rating = rating,
        updatedAt = now,
    )

private fun Map<String, List<XtreamEpisodeDto>>?.firstPlayableEpisode(): EpisodePreview? =
    orEmpty()
        .toList()
        .sortedBy { (season, _) -> season.toIntOrNull() ?: Int.MAX_VALUE }
        .flatMap { (_, episodes) ->
            episodes.sortedBy { episode -> episode.episodeNumber ?: Int.MAX_VALUE }
        }
        .firstNotNullOfOrNull { episode ->
            val episodeId = episode.id?.toIntOrNull() ?: return@firstNotNullOfOrNull null
            EpisodePreview(
                episodeId = episodeId,
                extension = episode.containerExtension.orEmpty().ifBlank { "mp4" },
            )
        }

private fun isPlayableMediaUrl(url: String): Boolean =
    runCatching {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=0-1")
            .header("User-Agent", "SmartVision-TV")
            .get()
            .build()
        TrendingMediaCheckClient.newCall(request).execute().use { response ->
            response.isSuccessful || response.code in 300..399
        }
    }.getOrDefault(false)

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

private const val SyncInsertBatchSize = 500
private const val CatalogPageMaxLimit = 500
private const val SyncMemoryTag = "SVSyncMemory"
private const val TrendingMovieType = "movie"
private const val TrendingSeriesType = "series"
private const val TrendStorageLimit = 50
private const val TrendValidationScanLimit = 90
private const val TrendMinimumFallbackRating = 9.0f
private const val TrendPerfectRatingFloor = 9.95f
private val AdultContentPattern = Regex(
    "(^|[^a-z0-9])(adult|adults|adulte|porn|porno|xxx|erotic|erotique|sex|sexy|18\\+)([^a-z0-9]|$)",
    RegexOption.IGNORE_CASE,
)
private val TrendingMediaCheckClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(2, TimeUnit.SECONDS)
    .readTimeout(2, TimeUnit.SECONDS)
    .writeTimeout(2, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

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
