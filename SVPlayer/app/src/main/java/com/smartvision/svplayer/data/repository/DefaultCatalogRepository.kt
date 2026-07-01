package com.smartvision.svplayer.data.repository

import android.util.Log
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.XtreamAccountManager
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
            mediaDao.observeLiveStreams(),
            accountManager.activePlaylistSource,
            accountManager.m3uUrl,
            accountManager.accounts,
        ) { categories, streams, source, m3uUrl, accounts ->
            if (!source.hasConfiguredCatalog(m3uUrl, accounts.isNotEmpty())) return@combine emptyList()
            val counts = streams.groupingBy { it.categoryId }.eachCount()
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
            mediaDao.observeMovies(),
            accountManager.activePlaylistSource,
            accountManager.accounts,
        ) { categories, movies, source, accounts ->
            if (source != PlaylistSource.Xtream || accounts.isEmpty()) return@combine emptyList()
            val counts = movies.groupingBy { it.categoryId }.eachCount()
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
            mediaDao.observeSeries(),
            accountManager.activePlaylistSource,
            accountManager.accounts,
        ) { categories, series, source, accounts ->
            if (source != PlaylistSource.Xtream || accounts.isEmpty()) return@combine emptyList()
            val counts = series.groupingBy { it.categoryId }.eachCount()
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
            mediaDao.observeLiveStreams(),
            mediaDao.observeMovies(),
            mediaDao.observeSeries(),
        ) { profile, live, movies, series ->
            profile?.toDomain(
                liveCount = live.size,
                movieCount = movies.size,
                seriesCount = series.size,
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
            live = SyncStatus.SyncSectionProgress(previousItems = mediaDao.observeLiveStreams().first().size),
            movies = SyncStatus.SyncSectionProgress(previousItems = mediaDao.observeMovies().first().size),
            series = SyncStatus.SyncSectionProgress(previousItems = mediaDao.observeSeries().first().size),
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

            logSyncMemory(stage = "before_get_account")
            val account = api.getAccount(credentials.username, credentials.password)
            logSyncMemory(stage = "after_get_account")
            updateProgress("Verification du compte Xtream...", fetched = 1, remainingSteps = 5)

            logSyncMemory(stage = "before_get_live_categories")
            val liveCategories = api.getCategories(credentials.username, credentials.password, "get_live_categories")
            logSyncMemory(stage = "after_get_live_categories", liveCategories = liveCategories.size)
            updateProgress("Telechargement des categories Live TV...", fetched = liveCategories.size, remainingSteps = 4)
            profileDao.upsert(account.toProfileEntity(credentials, now))

            logSyncMemory(stage = "before_get_live_streams", liveCategories = liveCategories.size)
            val liveStreams = api.getLiveStreams(credentials.username, credentials.password)
            liveItems = liveStreams.size
            liveCompleted = true
            logSyncMemory(stage = "after_get_live_streams", live = liveStreams.size, liveCategories = liveCategories.size)
            updateProgress("Telechargement des chaines Live TV...", fetched = liveStreams.size, remainingSteps = 3)

            logSyncMemory(stage = "before_get_movie_categories", live = liveStreams.size)
            val movieCategories = api.getCategories(credentials.username, credentials.password, "get_vod_categories")
            logSyncMemory(stage = "after_get_movie_categories", live = liveStreams.size, movieCategories = movieCategories.size)
            updateProgress("Telechargement des categories Films...", fetched = movieCategories.size, remainingSteps = 2)
            logSyncMemory(stage = "before_get_movies", live = liveStreams.size, movieCategories = movieCategories.size)
            val movies = api.getMovies(credentials.username, credentials.password)
            movieItems = movies.size
            moviesCompleted = true
            logSyncMemory(stage = "after_get_movies", live = liveStreams.size, movies = movies.size, movieCategories = movieCategories.size)
            updateProgress("Telechargement des films...", fetched = movies.size, remainingSteps = 1)

            logSyncMemory(stage = "before_get_series_categories", live = liveStreams.size, movies = movies.size)
            val seriesCategories = api.getCategories(credentials.username, credentials.password, "get_series_categories")
            logSyncMemory(
                stage = "after_get_series_categories",
                live = liveStreams.size,
                movies = movies.size,
                seriesCategories = seriesCategories.size,
            )
            updateProgress("Telechargement des categories Series...", fetched = seriesCategories.size, remainingSteps = 1)
            logSyncMemory(stage = "before_get_series", live = liveStreams.size, movies = movies.size, seriesCategories = seriesCategories.size)
            val series = api.getSeries(credentials.username, credentials.password)
            seriesItems = series.size
            seriesCompleted = true
            logSyncMemory(stage = "after_get_series", live = liveStreams.size, movies = movies.size, series = series.size)
            updateProgress("Telechargement des series...", fetched = series.size, remainingSteps = 0)

            logSyncMemory(stage = "before_room_write", live = liveStreams.size, movies = movies.size, series = series.size)
            categoryDao.deleteByType(MediaSection.Live.storageName)
            categoryDao.upsertAll(liveCategories.mapNotNull { it.toEntity(MediaSection.Live) })
            mediaDao.clearLiveStreams()
            mediaDao.upsertLiveStreams(liveStreams.mapNotNull { it.toEntity() })

            categoryDao.deleteByType(MediaSection.Movies.storageName)
            categoryDao.upsertAll(movieCategories.mapNotNull { it.toEntity(MediaSection.Movies) })
            mediaDao.clearMovies()
            mediaDao.upsertMovies(movies.mapNotNull { it.toEntity() })

            categoryDao.deleteByType(MediaSection.Series.storageName)
            categoryDao.upsertAll(seriesCategories.mapNotNull { it.toEntity(MediaSection.Series) })
            mediaDao.clearSeries()
            mediaDao.upsertSeries(series.mapNotNull { it.toEntity() })
            logSyncMemory(stage = "after_room_write", live = liveStreams.size, movies = movies.size, series = series.size)

            syncStateDao.upsert(
                SyncStateEntity(
                    id = "catalog",
                    lastSync = now,
                    status = "success",
                    message = "Synchronisation terminee",
                ),
            )
            invalidateLocalCatalogCache()
            logSyncMemory(stage = "after_cache_invalidation", live = liveStreams.size, movies = movies.size, series = series.size)
            _syncStatus.value = SyncStatus.Success(
                message = "Synchronisation terminee",
                catalogProgress = currentCatalogProgress(),
            )
            logSyncMemory(stage = "xtream_sync_success", live = liveStreams.size, movies = movies.size, series = series.size)
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
