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

    override fun invalidateLocalCatalogCache() {
        localCatalogSnapshotCache.invalidate()
    }

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
        mediaDao.clearMovies()
        upsertMappedInBatches(movies, { it.toEntity() }) { entities ->
            mediaDao.upsertMovies(entities)
        }
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
        mediaDao.clearSeries()
        upsertMappedInBatches(series, { it.toEntity() }) { entities ->
            mediaDao.upsertSeries(entities)
        }
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
private const val SyncMemoryTag = "SVSyncMemory"

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
