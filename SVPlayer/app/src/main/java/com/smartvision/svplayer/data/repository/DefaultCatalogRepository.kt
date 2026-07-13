package com.smartvision.svplayer.data.repository

import android.os.SystemClock
import android.util.Log
import androidx.room.withTransaction
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.PlaylistProfileStatus
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.ProfileType
import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.data.local.SVDatabase
import com.smartvision.svplayer.data.local.dao.CategoryDao
import com.smartvision.svplayer.data.local.dao.FavoriteDao
import com.smartvision.svplayer.data.local.dao.MediaDao
import com.smartvision.svplayer.data.local.dao.KidsFilterDao
import com.smartvision.svplayer.data.local.dao.ProfileDao
import com.smartvision.svplayer.data.local.dao.ProgressDao
import com.smartvision.svplayer.data.local.dao.SyncStateDao
import com.smartvision.svplayer.data.local.dao.YoutubeDao
import com.smartvision.svplayer.data.home.HomeTrendingPolicy
import com.smartvision.svplayer.data.local.entity.FavoriteEntity
import com.smartvision.svplayer.data.local.entity.KidsCategoryDecisionEntity
import com.smartvision.svplayer.data.local.entity.KidsItemDecisionEntity
import com.smartvision.svplayer.data.local.entity.MovieEntity
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.local.entity.SeriesEntity
import com.smartvision.svplayer.data.local.entity.SyncStateEntity
import com.smartvision.svplayer.data.local.entity.TrendingMediaEntity
import com.smartvision.svplayer.data.network.NetworkActivityHandle
import com.smartvision.svplayer.data.network.NetworkActivityStatus
import com.smartvision.svplayer.data.network.NetworkActivityTracker
import com.smartvision.svplayer.data.network.NetworkActivityType
import com.smartvision.svplayer.data.playlist.EpgRepository
import com.smartvision.svplayer.data.playlist.M3uPlaylistClient
import com.smartvision.svplayer.data.remote.XtreamApiService
import com.smartvision.svplayer.data.remote.XtreamUrlFactory
import com.smartvision.svplayer.data.remote.dto.XtreamCategoryDto
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
import com.smartvision.svplayer.domain.repository.CatalogContentCounts
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.domain.repository.LocalCatalogSnapshot
import com.smartvision.svplayer.domain.profile.KidsContentFilter
import com.smartvision.svplayer.domain.profile.CachedKidsCategoryDecision
import com.smartvision.svplayer.domain.profile.CachedKidsItemDecision
import com.smartvision.svplayer.domain.profile.KidsCatalogFilterEngine
import com.smartvision.svplayer.domain.profile.KidsCategoryClassification
import com.smartvision.svplayer.domain.profile.KidsCategoryDecision
import com.smartvision.svplayer.domain.profile.KidsCategoryInput
import com.smartvision.svplayer.domain.profile.KidsContentClassification
import com.smartvision.svplayer.domain.profile.KidsContentDecision
import com.smartvision.svplayer.domain.profile.KidsContentKind
import com.smartvision.svplayer.domain.profile.KidsContentMetadata
import com.smartvision.svplayer.domain.profile.KidsDecisionSource
import com.smartvision.svplayer.domain.profile.KidsFilterFingerprint
import com.smartvision.svplayer.domain.profile.KidsFilterMetrics
import com.smartvision.svplayer.domain.profile.KidsItemInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.coroutines.withTimeout

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
    private val youtubeDao: YoutubeDao,
    private val kidsFilterDao: KidsFilterDao,
    private val networkActivityTracker: NetworkActivityTracker,
) : CatalogRepository {
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus
    private val _catalogRevision = MutableStateFlow(0L)
    override val catalogRevision: StateFlow<Long> = _catalogRevision
    private val localCatalogSnapshotCache = LocalCatalogSnapshotCache()
    private val syncMutex = Mutex()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val kidsContentFilter = KidsContentFilter()
    private val kidsCatalogFilterEngine = KidsCatalogFilterEngine(kidsContentFilter)

    init {
        observeActiveProfileChanges()
    }

    override fun observeLiveCategories(): Flow<List<Category>> =
        accountManager.activeProfileId.flatMapLatest {
            val profileId = activeProfileId()
            combine(
                categoryDao.observeByType(profileId, MediaSection.Live.storageName),
                mediaDao.observeLiveStreamCountsByCategory(profileId),
                accountManager.activePlaylistSource,
                accountManager.m3uUrl,
                accountManager.accounts,
            ) { categories, countsByCategory, source, m3uUrl, accounts ->
                if (!source.hasConfiguredCatalog(m3uUrl, accounts.isNotEmpty())) return@combine emptyList()
                val counts = countsByCategory.associate { it.categoryId to it.count }
                localCatalogSnapshotCache.putLiveCategories(
                    profileId,
                    categories.map { it.toDomain(MediaSection.Live, counts[it.id] ?: 0) },
                )
            }
        }

    override fun observeLiveChannels(categoryId: String?): Flow<List<LiveChannel>> =
        accountManager.activeProfileId.flatMapLatest {
            val profileId = activeProfileId()
            combine(
                categoryDao.observeByType(profileId, MediaSection.Live.storageName),
                mediaDao.observeLiveStreamsByCategory(profileId, categoryId),
                accountManager.activePlaylistSource,
                accountManager.m3uUrl,
                accountManager.accounts,
            ) { categories, streams, source, m3uUrl, accounts ->
                if (!source.hasConfiguredCatalog(m3uUrl, accounts.isNotEmpty())) return@combine emptyList()
                val names = categories.associate { it.id to it.name }
                val imageBaseHost = imageBaseHost()
                streams.map { it.toDomain(names[it.categoryId] ?: "Live TV", imageBaseHost).withEpg(epgRepository) }
            }
        }

    override fun observeMovieCategories(): Flow<List<Category>> =
        accountManager.activeProfileId.flatMapLatest {
            val profileId = activeProfileId()
            combine(
                categoryDao.observeByType(profileId, MediaSection.Movies.storageName),
                mediaDao.observeMovieCountsByCategory(profileId),
                accountManager.activePlaylistSource,
                accountManager.accounts,
            ) { categories, countsByCategory, source, accounts ->
                if (source != PlaylistSource.Xtream || accounts.isEmpty()) return@combine emptyList()
                val counts = countsByCategory.associate { it.categoryId to it.count }
                localCatalogSnapshotCache.putMovieCategories(
                    profileId,
                    categories.map { it.toDomain(MediaSection.Movies, counts[it.id] ?: 0) },
                )
            }
        }

    override fun observeMovies(categoryId: String?): Flow<List<Movie>> =
        accountManager.activeProfileId.flatMapLatest {
            val profileId = activeProfileId()
            combine(
                categoryDao.observeByType(profileId, MediaSection.Movies.storageName),
                mediaDao.observeMoviesByCategory(profileId, categoryId?.takeUnless { it == "all" || it == "new" || it == "favorites" }),
                accountManager.activePlaylistSource,
                accountManager.accounts,
            ) { categories, movies, source, accounts ->
                if (source != PlaylistSource.Xtream || accounts.isEmpty()) return@combine emptyList()
                val names = categories.associate { it.id to it.name }
                val imageBaseHost = imageBaseHost()
                movies.map { it.toDomain(names[it.categoryId] ?: "Films", imageBaseHost) }
            }
        }

    override fun observeSeriesCategories(): Flow<List<Category>> =
        accountManager.activeProfileId.flatMapLatest {
            val profileId = activeProfileId()
            combine(
                categoryDao.observeByType(profileId, MediaSection.Series.storageName),
                mediaDao.observeSeriesCountsByCategory(profileId),
                accountManager.activePlaylistSource,
                accountManager.accounts,
            ) { categories, countsByCategory, source, accounts ->
                if (source != PlaylistSource.Xtream || accounts.isEmpty()) return@combine emptyList()
                val counts = countsByCategory.associate { it.categoryId to it.count }
                localCatalogSnapshotCache.putSeriesCategories(
                    profileId,
                    categories.map { it.toDomain(MediaSection.Series, counts[it.id] ?: 0) },
                )
            }
        }

    override fun observeSeries(categoryId: String?): Flow<List<TvSeries>> =
        accountManager.activeProfileId.flatMapLatest {
            val profileId = activeProfileId()
            combine(
                categoryDao.observeByType(profileId, MediaSection.Series.storageName),
                mediaDao.observeSeriesByCategory(profileId, categoryId?.takeUnless { it == "all" || it == "new" || it == "favorites" }),
                accountManager.activePlaylistSource,
                accountManager.accounts,
            ) { categories, series, source, accounts ->
                if (source != PlaylistSource.Xtream || accounts.isEmpty()) return@combine emptyList()
                val names = categories.associate { it.id to it.name }
                val imageBaseHost = imageBaseHost()
                series.map { it.toDomain(names[it.categoryId] ?: "Series", imageBaseHost) }
            }
        }

    override fun observeAccount(): Flow<AccountProfile> =
        accountManager.activeProfileId.flatMapLatest {
            val profileId = activeProfileId()
            combine(
                profileDao.observe(profileId),
                accountManager.profiles,
                mediaDao.observeLiveStreamCount(profileId),
                mediaDao.observeMovieCount(profileId),
                mediaDao.observeSeriesCount(profileId),
            ) { profile, playlistProfiles, liveCount, movieCount, seriesCount ->
                profile?.toDomain(
                    liveCount = liveCount,
                    movieCount = movieCount,
                    seriesCount = seriesCount,
                ) ?: playlistProfiles
                    .firstOrNull { it.id == profileId }
                    ?.toAccountProfile(liveCount, movieCount, seriesCount)
                ?: emptyAccountProfile().copy(
                    id = profileId,
                    liveCount = liveCount,
                    movieCount = movieCount,
                    seriesCount = seriesCount,
                )
            }
        }

    override fun getCachedLiveCatalogSnapshot(): LocalCatalogSnapshot<LiveChannel>? =
        localCatalogSnapshotCache.getLive(activeProfileId())

    override fun getCachedMovieCatalogSnapshot(): LocalCatalogSnapshot<Movie>? =
        localCatalogSnapshotCache.getMovies(activeProfileId())

    override fun getCachedSeriesCatalogSnapshot(): LocalCatalogSnapshot<TvSeries>? =
        localCatalogSnapshotCache.getSeries(activeProfileId())

    override fun getCachedLiveCategories(): List<Category>? =
        localCatalogSnapshotCache.getLiveCategories(activeProfileId())

    override fun getCachedMovieCategories(): List<Category>? =
        localCatalogSnapshotCache.getMovieCategories(activeProfileId())

    override fun getCachedSeriesCategories(): List<Category>? =
        localCatalogSnapshotCache.getSeriesCategories(activeProfileId())

    override suspend fun getLiveCategoriesSnapshot(): List<Category> = withContext(Dispatchers.IO) {
        val profileId = activeProfileId()
        localCatalogSnapshotCache.getLiveCategories(profileId)?.let { return@withContext it }
        localCatalogSnapshotCache.putLiveCategories(profileId, observeLiveCategories().first())
    }

    override suspend fun getMovieCategoriesSnapshot(): List<Category> = withContext(Dispatchers.IO) {
        val profileId = activeProfileId()
        localCatalogSnapshotCache.getMovieCategories(profileId)?.let { return@withContext it }
        localCatalogSnapshotCache.putMovieCategories(profileId, observeMovieCategories().first())
    }

    override suspend fun getSeriesCategoriesSnapshot(): List<Category> = withContext(Dispatchers.IO) {
        val profileId = activeProfileId()
        localCatalogSnapshotCache.getSeriesCategories(profileId)?.let { return@withContext it }
        localCatalogSnapshotCache.putSeriesCategories(profileId, observeSeriesCategories().first())
    }

    override suspend fun getInitialLiveCategoriesSnapshot(limit: Int): List<Category> = withContext(Dispatchers.IO) {
        if (!isLiveCatalogConfigured()) return@withContext emptyList()
        val profileId = activeProfileId()
        val counts = mediaDao.observeLiveStreamCountsByCategory(profileId).first().associate { it.categoryId to it.count }
        categoryDao.getByTypeLimit(profileId, MediaSection.Live.storageName, limit)
            .map { it.toDomain(MediaSection.Live, counts[it.id] ?: 0) }
    }

    override suspend fun getInitialMovieCategoriesSnapshot(limit: Int): List<Category> = withContext(Dispatchers.IO) {
        if (!isXtreamCatalogConfigured()) return@withContext emptyList()
        val profileId = activeProfileId()
        val counts = mediaDao.observeMovieCountsByCategory(profileId).first().associate { it.categoryId to it.count }
        categoryDao.getByTypeLimit(profileId, MediaSection.Movies.storageName, limit)
            .map { it.toDomain(MediaSection.Movies, counts[it.id] ?: 0) }
    }

    override suspend fun getInitialSeriesCategoriesSnapshot(limit: Int): List<Category> = withContext(Dispatchers.IO) {
        if (!isXtreamCatalogConfigured()) return@withContext emptyList()
        val profileId = activeProfileId()
        val counts = mediaDao.observeSeriesCountsByCategory(profileId).first().associate { it.categoryId to it.count }
        categoryDao.getByTypeLimit(profileId, MediaSection.Series.storageName, limit)
            .map { it.toDomain(MediaSection.Series, counts[it.id] ?: 0) }
    }

    override suspend fun getLiveCatalogSnapshot(): LocalCatalogSnapshot<LiveChannel> = withContext(Dispatchers.IO) {
        val profileId = activeProfileId()
        localCatalogSnapshotCache.getLive(profileId)?.let { return@withContext it }
        val snapshot = LocalCatalogSnapshot(
            categories = observeLiveCategories().first(),
            items = observeLiveChannels(null).first(),
        )
        localCatalogSnapshotCache.putLive(profileId, snapshot)
    }

    override suspend fun getMovieCatalogSnapshot(): LocalCatalogSnapshot<Movie> = withContext(Dispatchers.IO) {
        val profileId = activeProfileId()
        localCatalogSnapshotCache.getMovies(profileId)?.let { return@withContext it }
        val snapshot = LocalCatalogSnapshot(
            categories = observeMovieCategories().first(),
            items = observeMovies(null).first(),
        )
        localCatalogSnapshotCache.putMovies(profileId, snapshot)
    }

    override suspend fun getSeriesCatalogSnapshot(): LocalCatalogSnapshot<TvSeries> = withContext(Dispatchers.IO) {
        val profileId = activeProfileId()
        localCatalogSnapshotCache.getSeries(profileId)?.let { return@withContext it }
        val snapshot = LocalCatalogSnapshot(
            categories = observeSeriesCategories().first(),
            items = observeSeries(null).first(),
        )
        localCatalogSnapshotCache.putSeries(profileId, snapshot)
    }

    override suspend fun getLiveChannelsPage(categoryId: String?, offset: Int, limit: Int): List<LiveChannel> =
        withContext(Dispatchers.IO) {
            if (!isLiveCatalogConfigured()) return@withContext emptyList()
            val safeLimit = limit.coerceIn(1, CatalogPageMaxLimit)
            val safeOffset = offset.coerceAtLeast(0)
            val profileId = activeProfileId()
            localCatalogSnapshotCache.getLivePage(profileId, categoryId, safeOffset, safeLimit)?.let { return@withContext it }
            val categoryNames = categoryDao.getByType(profileId, MediaSection.Live.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            val streams = categoryId
                ?.takeIf { it.isNotBlank() }
                ?.let { mediaDao.getLiveStreamsByCategoryPage(profileId, it, safeLimit, safeOffset) }
                ?: mediaDao.getLiveStreamsPage(profileId, safeLimit, safeOffset)
            localCatalogSnapshotCache.putLivePage(
                profileId = profileId,
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
            val profileId = activeProfileId()
            val categoryNames = categoryDao.getByType(profileId, MediaSection.Live.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            val streams = categoryId
                ?.takeIf { it.isNotBlank() }
                ?.let { mediaDao.searchLiveStreamsByCategoryPage(profileId, it, pattern, safeLimit, safeOffset) }
                ?: mediaDao.searchLiveStreamsPage(profileId, pattern, safeLimit, safeOffset)
            streams.map { stream ->
                stream.toDomain(categoryNames[stream.categoryId] ?: "Live TV", imageBaseHost).withEpg(epgRepository)
            }
        }

    override suspend fun getLiveChannelsByCategoryIdsPage(
        categoryIds: List<String>,
        query: String,
        offset: Int,
        limit: Int,
    ): List<LiveChannel> = withContext(Dispatchers.IO) {
        if (categoryIds.isEmpty() || !isLiveCatalogConfigured()) return@withContext emptyList()
        val profileId = activeProfileId()
        val safeLimit = limit.coerceIn(1, CatalogPageMaxLimit)
        val safeOffset = offset.coerceAtLeast(0)
        val categoryNames = categoryDao.getByType(profileId, MediaSection.Live.storageName).associate { it.id to it.name }
        val streams = query.trim().takeIf { it.isNotBlank() }?.let { cleanQuery ->
            mediaDao.searchLiveStreamsByCategoriesPage(
                profileId,
                categoryIds,
                cleanQuery.toSqlLikeContainsPattern(),
                safeLimit,
                safeOffset,
            )
        } ?: mediaDao.getLiveStreamsByCategoriesPage(profileId, categoryIds, safeLimit, safeOffset)
        val imageBaseHost = imageBaseHost()
        streams.map { stream ->
            stream.toDomain(categoryNames[stream.categoryId] ?: "Live TV", imageBaseHost).withEpg(epgRepository)
        }
    }

    override suspend fun getLiveChannelById(streamId: Int): LiveChannel? =
        withContext(Dispatchers.IO) {
            if (!isLiveCatalogConfigured()) return@withContext null
            val profileId = activeProfileId()
            val categoryNames = categoryDao.getByType(profileId, MediaSection.Live.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            mediaDao.getLiveStream(profileId, streamId)
                ?.let { stream -> stream.toDomain(categoryNames[stream.categoryId] ?: "Live TV", imageBaseHost) }
                ?.withEpg(epgRepository)
        }

    override suspend fun getPreviousLiveChannel(streamId: Int): LiveChannel? =
        withContext(Dispatchers.IO) {
            if (!isLiveCatalogConfigured()) return@withContext null
            val profileId = activeProfileId()
            val current = mediaDao.getLiveStream(profileId, streamId) ?: return@withContext null
            val categoryNames = categoryDao.getByType(profileId, MediaSection.Live.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            mediaDao.getPreviousLiveStream(profileId, current.categoryId, current.number, current.name, current.streamId)
                ?.let { stream -> stream.toDomain(categoryNames[stream.categoryId] ?: "Live TV", imageBaseHost) }
                ?.withEpg(epgRepository)
        }

    override suspend fun getNextLiveChannel(streamId: Int): LiveChannel? =
        withContext(Dispatchers.IO) {
            if (!isLiveCatalogConfigured()) return@withContext null
            val profileId = activeProfileId()
            val current = mediaDao.getLiveStream(profileId, streamId) ?: return@withContext null
            val categoryNames = categoryDao.getByType(profileId, MediaSection.Live.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            mediaDao.getNextLiveStream(profileId, current.categoryId, current.number, current.name, current.streamId)
                ?.let { stream -> stream.toDomain(categoryNames[stream.categoryId] ?: "Live TV", imageBaseHost) }
                ?.withEpg(epgRepository)
        }

    override suspend fun getMoviesPage(categoryId: String?, offset: Int, limit: Int): List<Movie> =
        withContext(Dispatchers.IO) {
            if (!isXtreamCatalogConfigured()) return@withContext emptyList()
            val safeLimit = limit.coerceIn(1, CatalogPageMaxLimit)
            val safeOffset = offset.coerceAtLeast(0)
            val profileId = activeProfileId()
            localCatalogSnapshotCache.getMoviePage(profileId, categoryId, safeOffset, safeLimit)?.let { return@withContext it }
            val categoryNames = categoryDao.getByType(profileId, MediaSection.Movies.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            val movies = categoryId
                ?.takeIf { it.isNotBlank() }
                ?.let { mediaDao.getMoviesByCategoryPage(profileId, it, safeLimit, safeOffset) }
                ?: mediaDao.getMoviesPage(profileId, safeLimit, safeOffset)
            localCatalogSnapshotCache.putMoviePage(
                profileId = profileId,
                categoryId = categoryId,
                offset = safeOffset,
                limit = safeLimit,
                items = movies.map { movie -> movie.toDomain(categoryNames[movie.categoryId] ?: "Films", imageBaseHost) },
            )
        }

    override suspend fun getMovieById(streamId: Int): Movie? = withContext(Dispatchers.IO) {
        if (!isXtreamCatalogConfigured()) return@withContext null
        val profileId = activeProfileId()
        val categoryNames = categoryDao.getByType(profileId, MediaSection.Movies.storageName).associate { it.id to it.name }
        mediaDao.getMovie(profileId, streamId)
            ?.let { it.toDomain(categoryNames[it.categoryId] ?: "Films", imageBaseHost()) }
    }

    override suspend fun getPreviousMovie(streamId: Int): Movie? = adjacentMovie(streamId, previous = true)

    override suspend fun getNextMovie(streamId: Int): Movie? = adjacentMovie(streamId, previous = false)

    private suspend fun adjacentMovie(streamId: Int, previous: Boolean): Movie? = withContext(Dispatchers.IO) {
        if (!isXtreamCatalogConfigured()) return@withContext null
        val profileId = activeProfileId()
        val current = mediaDao.getMovie(profileId, streamId) ?: return@withContext null
        val adjacent = if (previous) {
            mediaDao.getPreviousMovie(profileId, current.categoryId, current.number, current.title, current.streamId)
        } else {
            mediaDao.getNextMovie(profileId, current.categoryId, current.number, current.title, current.streamId)
        } ?: return@withContext null
        val categoryName = categoryDao.getByType(profileId, MediaSection.Movies.storageName)
            .firstOrNull { it.id == adjacent.categoryId }
            ?.name
            ?: "Films"
        adjacent.toDomain(categoryName, imageBaseHost())
    }

    override suspend fun searchMoviesPage(categoryId: String?, query: String, offset: Int, limit: Int): List<Movie> =
        withContext(Dispatchers.IO) {
            if (!isXtreamCatalogConfigured()) return@withContext emptyList()
            val cleanQuery = query.trim()
            if (cleanQuery.isBlank()) return@withContext getMoviesPage(categoryId, offset, limit)
            val safeLimit = limit.coerceIn(1, CatalogPageMaxLimit)
            val safeOffset = offset.coerceAtLeast(0)
            val pattern = cleanQuery.toSqlLikeContainsPattern()
            val profileId = activeProfileId()
            val categoryNames = categoryDao.getByType(profileId, MediaSection.Movies.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            val movies = categoryId
                ?.takeIf { it.isNotBlank() }
                ?.let { mediaDao.searchMoviesByCategoryPage(profileId, it, pattern, safeLimit, safeOffset) }
                ?: mediaDao.searchMoviesPage(profileId, pattern, safeLimit, safeOffset)
            movies.map { movie -> movie.toDomain(categoryNames[movie.categoryId] ?: "Films", imageBaseHost) }
        }

    override suspend fun getSeriesPage(categoryId: String?, offset: Int, limit: Int): List<TvSeries> =
        withContext(Dispatchers.IO) {
            if (!isXtreamCatalogConfigured()) return@withContext emptyList()
            val safeLimit = limit.coerceIn(1, CatalogPageMaxLimit)
            val safeOffset = offset.coerceAtLeast(0)
            val profileId = activeProfileId()
            localCatalogSnapshotCache.getSeriesPage(profileId, categoryId, safeOffset, safeLimit)?.let { return@withContext it }
            val categoryNames = categoryDao.getByType(profileId, MediaSection.Series.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            val series = categoryId
                ?.takeIf { it.isNotBlank() }
                ?.let { mediaDao.getSeriesByCategoryPage(profileId, it, safeLimit, safeOffset) }
                ?: mediaDao.getSeriesPage(profileId, safeLimit, safeOffset)
            localCatalogSnapshotCache.putSeriesPage(
                profileId = profileId,
                categoryId = categoryId,
                offset = safeOffset,
                limit = safeLimit,
                items = series.map { item -> item.toDomain(categoryNames[item.categoryId] ?: "Series", imageBaseHost) },
            )
        }

    override suspend fun searchSeriesPage(categoryId: String?, query: String, offset: Int, limit: Int): List<TvSeries> =
        withContext(Dispatchers.IO) {
            if (!isXtreamCatalogConfigured()) return@withContext emptyList()
            val cleanQuery = query.trim()
            if (cleanQuery.isBlank()) return@withContext getSeriesPage(categoryId, offset, limit)
            val safeLimit = limit.coerceIn(1, CatalogPageMaxLimit)
            val safeOffset = offset.coerceAtLeast(0)
            val pattern = cleanQuery.toSqlLikeContainsPattern()
            val profileId = activeProfileId()
            val categoryNames = categoryDao.getByType(profileId, MediaSection.Series.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            val series = categoryId
                ?.takeIf { it.isNotBlank() }
                ?.let { mediaDao.searchSeriesByCategoryPage(profileId, it, pattern, safeLimit, safeOffset) }
                ?: mediaDao.searchSeriesPage(profileId, pattern, safeLimit, safeOffset)
            series.map { item -> item.toDomain(categoryNames[item.categoryId] ?: "Series", imageBaseHost) }
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
            val profileId = activeProfileId()
            val distinctIds = streamIds.distinct()
            val order = distinctIds.withIndex().associate { it.value to it.index }
            val categoryNames = categoryDao.getByType(profileId, MediaSection.Live.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            mediaDao.getLiveStreamsByIds(profileId, distinctIds)
                .sortedBy { order[it.streamId] ?: Int.MAX_VALUE }
                .map { stream -> stream.toDomain(categoryNames[stream.categoryId] ?: "Live TV", imageBaseHost).withEpg(epgRepository) }
        }

    override suspend fun getMoviesByIds(streamIds: List<Int>): List<Movie> =
        withContext(Dispatchers.IO) {
            if (streamIds.isEmpty() || !isXtreamCatalogConfigured()) return@withContext emptyList()
            val profileId = activeProfileId()
            val distinctIds = streamIds.distinct()
            val order = distinctIds.withIndex().associate { it.value to it.index }
            val categoryNames = categoryDao.getByType(profileId, MediaSection.Movies.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            mediaDao.getMoviesByIds(profileId, distinctIds)
                .sortedBy { order[it.streamId] ?: Int.MAX_VALUE }
                .map { movie -> movie.toDomain(categoryNames[movie.categoryId] ?: "Films", imageBaseHost) }
        }

    override suspend fun getSeriesByIds(seriesIds: List<Int>): List<TvSeries> =
        withContext(Dispatchers.IO) {
            if (seriesIds.isEmpty() || !isXtreamCatalogConfigured()) return@withContext emptyList()
            val profileId = activeProfileId()
            val distinctIds = seriesIds.distinct()
            val order = distinctIds.withIndex().associate { it.value to it.index }
            val categoryNames = categoryDao.getByType(profileId, MediaSection.Series.storageName).associate { it.id to it.name }
            val imageBaseHost = imageBaseHost()
            mediaDao.getSeriesByIds(profileId, distinctIds)
                .sortedBy { order[it.seriesId] ?: Int.MAX_VALUE }
                .map { series -> series.toDomain(categoryNames[series.categoryId] ?: "Series", imageBaseHost) }
        }

    override suspend fun getTrendingMovieItems(limit: Int): List<TrendingCatalogItem> = withContext(Dispatchers.IO) {
        if (accountManager.activePlaylistSource.value != PlaylistSource.Xtream || accountManager.accounts.value.isEmpty()) {
            return@withContext emptyList()
        }
        val profileId = activeProfileId()
        val categoryNames = categoryDao.getByType(profileId, MediaSection.Movies.storageName).associate { it.id to it.name }
        val imageBaseHost = imageBaseHost()
        val persisted = mediaDao.getTrendingMedia(profileId, TrendingMovieType)
        val order = persisted.mapIndexed { index, item -> item.contentId to index }.toMap()
        mediaDao.getMoviesByIds(profileId, persisted.map { it.contentId })
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
            .sortedBy { order[it.contentId] ?: Int.MAX_VALUE }
            .take(limit.coerceAtMost(HomeTrendingPolicy.SectionLimit))
            .toList()
    }

    override suspend fun getTrendingSeriesItems(limit: Int): List<TrendingCatalogItem> = withContext(Dispatchers.IO) {
        if (accountManager.activePlaylistSource.value != PlaylistSource.Xtream || accountManager.accounts.value.isEmpty()) {
            return@withContext emptyList()
        }
        val profileId = activeProfileId()
        val categoryNames = categoryDao.getByType(profileId, MediaSection.Series.storageName).associate { it.id to it.name }
        val imageBaseHost = imageBaseHost()
        val persisted = mediaDao.getTrendingMedia(profileId, TrendingSeriesType)
        val order = persisted.mapIndexed { index, item -> item.contentId to index }.toMap()
        mediaDao.getSeriesByIds(profileId, persisted.map { it.contentId })
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
            .sortedBy { order[it.contentId] ?: Int.MAX_VALUE }
            .take(limit.coerceAtMost(HomeTrendingPolicy.SectionLimit))
            .toList()
    }

    override suspend fun getCatalogContentCounts(): CatalogContentCounts = withContext(Dispatchers.IO) {
        val profileId = activeProfileId()
        CatalogContentCounts(
            live = mediaDao.countLiveStreams(profileId),
            movies = mediaDao.countMovies(profileId),
            series = mediaDao.countSeries(profileId),
        )
    }

    override suspend fun hasLocalCatalogForActiveProfile(): Boolean = withContext(Dispatchers.IO) {
        val profileId = activeProfileId()
        val syncState = syncStateDao.get(profileId)
        syncState?.status == "success" &&
            when (accountManager.activePlaylistSource.value) {
                PlaylistSource.Xtream -> mediaDao.countLiveStreams(profileId) > 0 ||
                    mediaDao.countMovies(profileId) > 0 ||
                    mediaDao.countSeries(profileId) > 0
                PlaylistSource.M3u -> mediaDao.countLiveStreams(profileId) > 0
            }
    }

    override fun invalidateLocalCatalogCache() {
        localCatalogSnapshotCache.invalidate()
    }

    override suspend fun clearCatalogForProfileSwitch() = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncStatus.Idle
        invalidateLocalCatalogCache()
        bumpCatalogRevision()
    }

    override suspend fun deleteProfileData(profileId: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            categoryDao.deleteByProfile(profileId)
            mediaDao.clearLiveStreams(profileId)
            mediaDao.clearMovies(profileId)
            mediaDao.clearSeries(profileId)
            mediaDao.clearEpisodesByProfile(profileId)
            mediaDao.clearTrendingByProfile(profileId)
            mediaDao.clearHomePreviewCacheByProfile(profileId)
            mediaDao.clearTmdbMappingsByProfile(profileId)
            favoriteDao.deleteByProfile(profileId)
            progressDao.deleteByProfile(profileId)
            syncStateDao.deleteByProfile(profileId)
            profileDao.delete(profileId)
            youtubeDao.clearSearchHistory(profileId)
            youtubeDao.clearVideoHistory(profileId)
            youtubeDao.clearSelections(profileId)
        }
        invalidateLocalCatalogCache()
        bumpCatalogRevision()
    }

    private fun observeActiveProfileChanges() {
        repositoryScope.launch {
            var currentProfileId = activeProfileId()
            accountManager.activeProfileId.collect {
                val nextProfileId = activeProfileId()
                if (nextProfileId != currentProfileId) {
                    currentProfileId = nextProfileId
                    invalidateLocalCatalogCache()
                    bumpCatalogRevision()
                }
            }
        }
    }

    private fun isLiveCatalogConfigured(): Boolean =
        accountManager.activePlaylistSource.value.hasConfiguredCatalog(
            m3uUrl = accountManager.m3uUrl.value,
            hasXtream = accountManager.accounts.value.isNotEmpty(),
        )

    private fun isXtreamCatalogConfigured(): Boolean =
        accountManager.activePlaylistSource.value == PlaylistSource.Xtream &&
            accountManager.accounts.value.isNotEmpty()

    private fun activeProfileId(): String =
        accountManager.activeProfileIdOrDefault()

    private fun isKidsProfile(profileId: String): Boolean =
        accountManager.profiles.value.firstOrNull { it.id == profileId }?.type == ProfileType.KIDS

    private fun imageBaseHost(): String =
        accountManager.current().normalizedHost

    private fun bumpCatalogRevision() {
        _catalogRevision.value += 1L
    }

    override suspend fun synchronize(profileId: String?): Result<Unit> = syncMutex.withLock {
        withContext(Dispatchers.IO) {
        val requestedProfile = profileId
            ?.let { requestedId -> accountManager.profiles.value.firstOrNull { it.id == requestedId } }
            ?: accountManager.activeProfile()
        val resolvedProfile = requestedProfile?.let(accountManager::resolvedProfile)
        if (resolvedProfile?.source == PlaylistSource.M3u) {
            return@withContext synchronizeM3u(resolvedProfile)
        }

        val credentials = resolvedProfile?.let {
            com.smartvision.svplayer.core.config.XtreamCredentials(
                host = it.xtreamHost,
                username = it.xtreamUsername,
                password = it.xtreamPassword,
            )
        } ?: accountManager.current()
        if (!credentials.isConfigured) {
            val message = "Aucun compte Xtream configure"
            _syncStatus.value = SyncStatus.Error(message)
            return@withContext Result.failure(IllegalStateException(message))
        }

        val profileId = requestedProfile?.id ?: activeProfileId()
        val previousCatalogProgress = SyncStatus.CatalogProgress(
            live = mediaDao.countLiveStreams(profileId).let { count ->
                SyncStatus.SyncSectionProgress(currentItems = count, previousItems = count)
            },
            movies = mediaDao.countMovies(profileId).let { count ->
                SyncStatus.SyncSectionProgress(currentItems = count, previousItems = count)
            },
            series = mediaDao.countSeries(profileId).let { count ->
                SyncStatus.SyncSectionProgress(currentItems = count, previousItems = count)
            },
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
        var liveMessage: String? = null
        var moviesMessage: String? = null
        var seriesMessage: String? = null
        var liveTotal: Int? = null
        var moviesTotal: Int? = null
        var seriesTotal: Int? = null
        var liveKept: Int? = null
        var moviesKept: Int? = null
        var seriesKept: Int? = null
        var liveExcluded: Int? = null
        var moviesExcluded: Int? = null
        var seriesExcluded: Int? = null
        val activeProfile = requestedProfile ?: accountManager.profiles.value.firstOrNull { it.id == profileId }
        val syncStartedAt = System.currentTimeMillis()
        fun currentCatalogProgress(): SyncStatus.CatalogProgress =
            SyncStatus.CatalogProgress(
                live = previousCatalogProgress.live.copy(
                    currentItems = liveItems,
                    completed = liveCompleted,
                    phase = livePhase,
                    progressPercent = livePercent,
                    message = liveMessage,
                    totalItems = liveTotal,
                    keptItems = liveKept,
                    excludedItems = liveExcluded,
                ),
                movies = previousCatalogProgress.movies.copy(
                    currentItems = movieItems,
                    completed = moviesCompleted,
                    phase = moviesPhase,
                    progressPercent = moviesPercent,
                    message = moviesMessage,
                    totalItems = moviesTotal,
                    keptItems = moviesKept,
                    excludedItems = moviesExcluded,
                ),
                series = previousCatalogProgress.series.copy(
                    currentItems = seriesItems,
                    completed = seriesCompleted,
                    phase = seriesPhase,
                    progressPercent = seriesPercent,
                    message = seriesMessage,
                    totalItems = seriesTotal,
                    keptItems = seriesKept,
                    excludedItems = seriesExcluded,
                ),
            )

        _syncStatus.value = SyncStatus.Running(
            catalogProgress = previousCatalogProgress,
            profileName = activeProfile?.name.orEmpty(),
            kidsMode = activeProfile?.type == com.smartvision.svplayer.core.config.ProfileType.KIDS,
            startedAtMs = syncStartedAt,
        )
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
                    profileName = activeProfile?.name.orEmpty(),
                    kidsMode = activeProfile?.type == com.smartvision.svplayer.core.config.ProfileType.KIDS,
                    startedAtMs = syncStartedAt,
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
                keptItems: Int? = null,
                excludedItems: Int? = null,
            ) {
                when (section) {
                    MediaSection.Live -> {
                        liveMessage = message
                        if ((phase == SyncStatus.SyncSectionPhase.FILTERING || (phase == SyncStatus.SyncSectionPhase.IMPORTING && livePhase != phase)) && currentItems != null) liveTotal = currentItems
                        livePhase = phase
                        livePercent = percent.coerceIn(0, 100)
                        currentItems?.let { liveItems = it }
                        keptItems?.let { liveKept = it }
                        excludedItems?.let { liveExcluded = it }
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
                        moviesMessage = message
                        if ((phase == SyncStatus.SyncSectionPhase.FILTERING || (phase == SyncStatus.SyncSectionPhase.IMPORTING && moviesPhase != phase)) && currentItems != null) moviesTotal = currentItems
                        moviesPhase = phase
                        moviesPercent = percent.coerceIn(0, 100)
                        currentItems?.let { movieItems = it }
                        keptItems?.let { moviesKept = it }
                        excludedItems?.let { moviesExcluded = it }
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
                        seriesMessage = message
                        if ((phase == SyncStatus.SyncSectionPhase.FILTERING || (phase == SyncStatus.SyncSectionPhase.IMPORTING && seriesPhase != phase)) && currentItems != null) seriesTotal = currentItems
                        seriesPhase = phase
                        seriesPercent = percent.coerceIn(0, 100)
                        currentItems?.let { seriesItems = it }
                        keptItems?.let { seriesKept = it }
                        excludedItems?.let { seriesExcluded = it }
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

            // Keep remote Xtream calls outside Room transactions. Some providers take
            // several seconds per endpoint; holding a database transaction during that
            // time blocks local catalog reads and makes the progress UI look frozen.
            logSyncMemory(stage = "before_get_account")
            val syncHost = credentials.normalizedHost
            val kidsSourceKey = KidsFilterFingerprint.source("xtream", syncHost.lowercase(), credentials.username)
            if (isKidsProfile(profileId)) {
                kidsFilterDao.deleteObsoleteCategoryRules(KidsContentFilter.RuleVersion)
                kidsFilterDao.deleteObsoleteItemRules(KidsContentFilter.RuleVersion)
            }
            val account = api.getAccount(credentials.username, credentials.password, syncHost)
            logSyncMemory(stage = "after_get_account")
            updateProgress("Verification du compte Xtream...", fetched = 1, remainingSteps = 5)
            profileDao.upsert(
                account.toProfileEntity(
                    profileId = profileId,
                    profileName = accountManager.profiles.value.firstOrNull { it.id == profileId }?.name.orEmpty(),
                    credentials = credentials,
                    now = now,
                ),
            )

            liveItems = synchronizeLiveSection(
                    profileId = profileId,
                    host = syncHost,
                    username = credentials.username,
                    password = credentials.password,
                    kidsSourceKey = kidsSourceKey,
                    updateSectionProgress = ::updateSectionProgress,
                )

            movieItems = synchronizeMovieSection(
                    profileId = profileId,
                    host = syncHost,
                    username = credentials.username,
                    password = credentials.password,
                    kidsSourceKey = kidsSourceKey,
                    liveItems = liveItems,
                    updateSectionProgress = ::updateSectionProgress,
                )

            seriesItems = synchronizeSeriesSection(
                    profileId = profileId,
                    host = syncHost,
                    username = credentials.username,
                    password = credentials.password,
                    kidsSourceKey = kidsSourceKey,
                    liveItems = liveItems,
                    movieItems = movieItems,
                    updateSectionProgress = ::updateSectionProgress,
                )

            updateProgress("Calcul des tendances du profil...", remainingSteps = 1)
            persistTrendingSelections(profileId = profileId, synchronizedAt = now)

            logSyncMemory(stage = "before_sync_state_write", live = liveItems, movies = movieItems, series = seriesItems)
            syncStateDao.upsert(
                SyncStateEntity(
                    profileId = profileId,
                    id = "catalog",
                    lastSync = now,
                    status = "success",
                    message = "Synchronisation terminee",
                ),
            )
            logSyncMemory(stage = "after_room_write", live = liveItems, movies = movieItems, series = seriesItems)
            accountManager.markProfileSynced(profileId, now)
            if (activeProfileId() == profileId) {
                invalidateLocalCatalogCache()
                bumpCatalogRevision()
            }
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
                    profileId = profileId,
                    id = "catalog",
                    lastSync = syncStateDao.get(profileId)?.lastSync,
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
    }

    private suspend fun persistTrendingSelections(profileId: String, synchronizedAt: Long) {
        val movieCategories = categoryDao.getByType(profileId, MediaSection.Movies.storageName)
        val seriesCategories = categoryDao.getByType(profileId, MediaSection.Series.storageName)
        val movieCategoryNames = movieCategories.associate { it.id to it.name }
        val seriesCategoryNames = seriesCategories.associate { it.id to it.name }
        val movieNoveltyIds = movieCategories.filter { HomeTrendingPolicy.isNoveltyCategory(it.name) }.map { it.id }
        val seriesNoveltyIds = seriesCategories.filter { HomeTrendingPolicy.isNoveltyCategory(it.name) }.map { it.id }

        val ratedMovies = mediaDao.getTopRatedMovieCandidates(profileId, HomeTrendingPolicy.CandidateLimit)
        val newestMovies = if (movieNoveltyIds.isNotEmpty()) {
            mediaDao.getNewestMovieCandidatesByCategory(profileId, movieNoveltyIds, HomeTrendingPolicy.CandidateLimit)
        } else {
            mediaDao.getNewestMovieCandidates(profileId, HomeTrendingPolicy.CandidateLimit)
        }
        val ratedSeries = mediaDao.getTopRatedSeriesCandidates(profileId, HomeTrendingPolicy.CandidateLimit)
        val newestSeries = if (seriesNoveltyIds.isNotEmpty()) {
            mediaDao.getNewestSeriesCandidatesByCategory(profileId, seriesNoveltyIds, HomeTrendingPolicy.CandidateLimit)
        } else {
            mediaDao.getNewestSeriesCandidates(profileId, HomeTrendingPolicy.CandidateLimit)
        }

        val movies = HomeTrendingPolicy.selectDeterministic(
            ratedCandidates = ratedMovies,
            newestCandidates = newestMovies,
            idOf = MovieEntity::streamId,
            ratingOf = { it.rating.toTrendingRating().takeIf { rating -> rating > 0f } },
            addedAtOf = MovieEntity::addedAt,
            yearOf = { it.year?.take(4)?.toIntOrNull() },
            allowed = { movie -> !containsAdultMarker(movie.title, movieCategoryNames[movie.categoryId]) },
        )
            .mapIndexed { index, movie -> movie.toTrendingEntity(profileId, synchronizedAt - index) }
        val series = HomeTrendingPolicy.selectDeterministic(
            ratedCandidates = ratedSeries,
            newestCandidates = newestSeries,
            idOf = SeriesEntity::seriesId,
            ratingOf = { it.rating.toTrendingRating().takeIf { rating -> rating > 0f } },
            addedAtOf = SeriesEntity::addedAt,
            yearOf = { it.year?.take(4)?.toIntOrNull() },
            allowed = { item -> !containsAdultMarker(item.title, seriesCategoryNames[item.categoryId]) },
        )
            .mapIndexed { index, item -> item.toTrendingEntity(profileId, synchronizedAt - index) }

        database.withTransaction {
            mediaDao.clearTrendingMedia(profileId, TrendingMovieType)
            mediaDao.clearTrendingMedia(profileId, TrendingSeriesType)
            if (movies.isNotEmpty()) mediaDao.upsertTrendingMedia(movies)
            if (series.isNotEmpty()) mediaDao.upsertTrendingMedia(series)
        }
    }

    private suspend fun <Remote, Local> filterAndImportKidsSection(
        profileId: String,
        sourceKey: String,
        kind: KidsContentKind,
        section: MediaSection,
        categories: List<XtreamCategoryDto>,
        items: List<Remote>,
        toFilterInput: (Remote) -> KidsItemInput<Remote>?,
        toLocalEntity: (Remote) -> Local?,
        clearItems: suspend () -> Unit,
        upsertItems: suspend (List<Local>) -> Unit,
        onProgress: suspend (processed: Int, total: Int, kept: Int, metrics: KidsFilterMetrics) -> Unit,
    ): KidsSectionImportResult {
        val filterStartedAt = SystemClock.elapsedRealtime()
        val categoryInputs = categories.mapNotNull { category ->
            val id = category.id?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            KidsCategoryInput(id = id, name = category.name.orEmpty())
        }
        val categoryCache = kidsFilterDao.getCategoryDecisions(sourceKey, kind.storageName)
            .associate { it.categoryId to it.toCachedDecision() }
        val categoryBatch = kidsCatalogFilterEngine.evaluateCategories(categoryInputs, categoryCache)
        val now = System.currentTimeMillis()
        if (categoryBatch.cacheUpdates.isNotEmpty()) {
            kidsFilterDao.upsertCategoryDecisions(
                categoryBatch.cacheUpdates.map { it.toEntity(sourceKey, kind.storageName, now) },
            )
        }

        var metrics = categoryBatch.metrics
        var keptItems = 0
        var processedItems = 0
        val acceptedCategoryIds = linkedSetOf<String>()
        database.withTransaction {
            clearItems()
            items.chunked(KidsFilterImportBatchSize).forEach { remoteBatch ->
                val inputs = remoteBatch.mapNotNull(toFilterInput)
                val cacheableIds = inputs.asSequence()
                    .filter { input ->
                        input.metadata.manualOverride != null ||
                            input.categoryId?.let(categoryBatch.decisions::get)?.safe != true
                    }
                    .map { it.id }
                    .distinct()
                    .toList()
                val itemCache = if (cacheableIds.isEmpty()) {
                    emptyMap()
                } else {
                    kidsFilterDao.getItemDecisions(sourceKey, kind.storageName, cacheableIds)
                        .associate { it.contentId to it.toCachedDecision() }
                }
                val filtered = kidsCatalogFilterEngine.filterItems(
                    items = inputs,
                    categoryDecisions = categoryBatch.decisions,
                    cached = itemCache,
                )
                if (filtered.cacheUpdates.isNotEmpty()) {
                    kidsFilterDao.upsertItemDecisions(
                        filtered.cacheUpdates.map { it.toEntity(sourceKey, kind.storageName, now) },
                    )
                }
                val mapped = filtered.acceptedItems.mapNotNull(toLocalEntity)
                if (mapped.isNotEmpty()) upsertItems(mapped)
                keptItems += mapped.size
                processedItems += remoteBatch.size
                acceptedCategoryIds += filtered.acceptedCategoryIds
                metrics += filtered.metrics
                onProgress(processedItems, items.size, keptItems, metrics)
                yield()
            }

            categoryDao.deleteByType(profileId, section.storageName)
            val visibleCategories = categories.filter { it.id in acceptedCategoryIds }
            upsertMappedInBatches(
                visibleCategories,
                { it.toEntity(profileId, section) },
            ) { entities ->
                categoryDao.upsertAll(entities)
            }
        }
        metrics = metrics.copy(durationMs = SystemClock.elapsedRealtime() - filterStartedAt)
        Log.i(
            KidsFilterLogTag,
            "KidsFilter[${kind.storageName}]: ${metrics.categoriesProcessed} categories traitees, " +
                "${metrics.categoriesEvaluated} evaluees, ${metrics.safeCategories} Kids, " +
                "$keptItems conserves, ${metrics.inheritedItems} herites, " +
                "${metrics.individuallyAnalyzedItems} analyses individuellement, " +
                "${metrics.cacheHits} depuis cache, ${metrics.ambiguousItems} ambigus, " +
                "${metrics.rejectedItems} rejetes, networkRequests=0, duree=${metrics.durationMs}ms",
        )
        return KidsSectionImportResult(keptItems = keptItems, metrics = metrics)
    }

    private suspend fun synchronizeLiveSection(
        profileId: String,
        host: String,
        username: String,
        password: String,
        kidsSourceKey: String,
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
        val liveCategories = api.getCategories(username, password, "get_live_categories", host)
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
        val kidsProfile = isKidsProfile(profileId)
        if (!kidsProfile) database.withTransaction {
            categoryDao.deleteByType(profileId, MediaSection.Live.storageName)
            upsertMappedInBatches(liveCategories, { it.toEntity(profileId, MediaSection.Live) }) { entities -> categoryDao.upsertAll(entities) }
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
        val downloadedLiveStreams = api.getLiveStreams(username = username, password = password, host = host)
        if (kidsProfile) updateSectionProgress(
            "Analyse des categories Kids Live TV...",
            MediaSection.Live,
            SyncStatus.SyncSectionPhase.FILTERING,
            62,
            downloadedLiveStreams.size,
            0,
            0,
        )
        val imageBaseHost = host
        val kidsImport = if (kidsProfile) {
            filterAndImportKidsSection(
                profileId = profileId,
                sourceKey = kidsSourceKey,
                kind = KidsContentKind.LIVE_CHANNEL,
                section = MediaSection.Live,
                categories = liveCategories,
                items = downloadedLiveStreams,
                toFilterInput = { stream ->
                    val id = stream.streamId?.toString() ?: return@filterAndImportKidsSection null
                    KidsItemInput(
                        value = stream,
                        id = id,
                        categoryId = stream.categoryId,
                        metadata = KidsContentMetadata(
                            kind = KidsContentKind.LIVE_CHANNEL,
                            title = stream.name,
                        ),
                    )
                },
                toLocalEntity = { it.toEntity(profileId, imageBaseHost) },
                clearItems = { mediaDao.clearLiveStreams(profileId) },
                upsertItems = mediaDao::upsertLiveStreams,
                onProgress = { processed, total, kept, metrics ->
                    updateSectionProgress(
                        "Kids Live TV: $kept conserves, ${metrics.inheritedItems} herites, " +
                            "${metrics.individuallyAnalyzedItems} analyses, ${metrics.cacheHits} cache",
                        MediaSection.Live,
                        SyncStatus.SyncSectionPhase.FILTERING,
                        60 + (processed * 34 / total.coerceAtLeast(1)),
                        processed,
                        0,
                        0,
                    )
                },
            )
        } else {
            null
        }
        val liveItems = kidsImport?.keptItems ?: downloadedLiveStreams.size
        logSyncMemory(stage = "after_get_live_streams", live = liveItems, liveCategories = liveCategories.size)
        if (!kidsProfile) updateSectionProgress(
            "Import des chaines Live TV...",
            MediaSection.Live,
            SyncStatus.SyncSectionPhase.IMPORTING,
            78,
            liveItems,
            liveItems,
            3,
        )
        if (!kidsProfile) database.withTransaction {
            mediaDao.clearLiveStreams(profileId)
            upsertMappedInBatches(
                items = downloadedLiveStreams,
                mapper = { it.toEntity(profileId, imageBaseHost) },
                upsert = mediaDao::upsertLiveStreams,
                onBatchCommitted = { processed, total ->
                    updateSectionProgress(
                        "Import des chaines Live TV...",
                        MediaSection.Live,
                        SyncStatus.SyncSectionPhase.IMPORTING,
                        78 + ((processed.toFloat() / total.coerceAtLeast(1)) * 20).toInt(),
                        processed,
                        0,
                        0,
                    )
                },
            )
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
        profileId: String,
        host: String,
        username: String,
        password: String,
        kidsSourceKey: String,
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
        val movieCategories = api.getCategories(username, password, "get_vod_categories", host)
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
        val kidsProfile = isKidsProfile(profileId)
        if (!kidsProfile) database.withTransaction {
            categoryDao.deleteByType(profileId, MediaSection.Movies.storageName)
            upsertMappedInBatches(movieCategories, { it.toEntity(profileId, MediaSection.Movies) }) { entities -> categoryDao.upsertAll(entities) }
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
        var movies = api.getMovies(username = username, password = password, host = host)
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
                    api.getMovies(username = username, password = password, categoryId = categoryId, host = host)
                        .map { movie ->
                            if (movie.categoryId.isNullOrBlank()) movie.copy(categoryId = categoryId) else movie
                        }
                }
                .distinctBy { it.streamId }
            logSyncMemory(stage = "after_get_movies_by_category", live = liveItems, movies = movies.size, movieCategories = movieCategories.size)
        }
        if (kidsProfile) updateSectionProgress(
            "Analyse des categories Kids Films...",
            MediaSection.Movies,
            SyncStatus.SyncSectionPhase.FILTERING,
            60,
            movies.size,
            0,
            0,
        )
        val imageBaseHost = host
        val kidsImport = if (kidsProfile) {
            filterAndImportKidsSection(
                profileId = profileId,
                sourceKey = kidsSourceKey,
                kind = KidsContentKind.MOVIE,
                section = MediaSection.Movies,
                categories = movieCategories,
                items = movies,
                toFilterInput = { movie ->
                    val id = movie.streamId?.toString() ?: return@filterAndImportKidsSection null
                    KidsItemInput(
                        value = movie,
                        id = id,
                        categoryId = movie.categoryId,
                        metadata = KidsContentMetadata(
                            kind = KidsContentKind.MOVIE,
                            title = movie.name,
                        ),
                    )
                },
                toLocalEntity = { it.toEntity(profileId, imageBaseHost) },
                clearItems = { mediaDao.clearMovies(profileId) },
                upsertItems = mediaDao::upsertMovies,
                onProgress = { processed, total, kept, metrics ->
                    updateSectionProgress(
                        "Kids Films: $kept conserves, ${metrics.inheritedItems} herites, " +
                            "${metrics.individuallyAnalyzedItems} analyses, ${metrics.cacheHits} cache",
                        MediaSection.Movies,
                        SyncStatus.SyncSectionPhase.FILTERING,
                        58 + (processed * 36 / total.coerceAtLeast(1)),
                        processed,
                        0,
                        0,
                    )
                },
            )
        } else {
            null
        }
        val movieItems = kidsImport?.keptItems ?: movies.size
        logSyncMemory(stage = "after_get_movies", live = liveItems, movies = movieItems, movieCategories = movieCategories.size)
        if (!kidsProfile) updateSectionProgress(
            "Import des films...",
            MediaSection.Movies,
            SyncStatus.SyncSectionPhase.IMPORTING,
            72,
            movieItems,
            movieItems,
            1,
        )
        if (!kidsProfile) database.withTransaction {
            mediaDao.clearMovies(profileId)
            upsertMappedInBatches(
                items = movies,
                mapper = { it.toEntity(profileId, imageBaseHost) },
                upsert = mediaDao::upsertMovies,
                onBatchCommitted = { processed, total ->
                    updateSectionProgress(
                        "Import des films...",
                        MediaSection.Movies,
                        SyncStatus.SyncSectionPhase.IMPORTING,
                        72 + ((processed.toFloat() / total.coerceAtLeast(1)) * 26).toInt(),
                        processed,
                        0,
                        0,
                    )
                },
            )
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
        profileId: String,
        host: String,
        username: String,
        password: String,
        kidsSourceKey: String,
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
        val seriesCategories = api.getCategories(username, password, "get_series_categories", host)
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
        val kidsProfile = isKidsProfile(profileId)
        if (!kidsProfile) database.withTransaction {
            categoryDao.deleteByType(profileId, MediaSection.Series.storageName)
            upsertMappedInBatches(seriesCategories, { it.toEntity(profileId, MediaSection.Series) }) { entities -> categoryDao.upsertAll(entities) }
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
        val downloadedSeries = fetchSeriesWithFallback(
            host = host,
            username = username,
            password = password,
            categories = seriesCategories,
            liveItems = liveItems,
            movieItems = movieItems,
            updateSectionProgress = updateSectionProgress,
        )
        if (kidsProfile) updateSectionProgress(
            "Analyse des categories Kids Series...",
            MediaSection.Series,
            SyncStatus.SyncSectionPhase.FILTERING,
            60,
            downloadedSeries.size,
            0,
            0,
        )
        val imageBaseHost = host
        val kidsImport = if (kidsProfile) {
            filterAndImportKidsSection(
                profileId = profileId,
                sourceKey = kidsSourceKey,
                kind = KidsContentKind.SERIES,
                section = MediaSection.Series,
                categories = seriesCategories,
                items = downloadedSeries,
                toFilterInput = { item ->
                    val id = item.seriesId?.toString() ?: return@filterAndImportKidsSection null
                    KidsItemInput(
                        value = item,
                        id = id,
                        categoryId = item.categoryId,
                        metadata = KidsContentMetadata(
                            kind = KidsContentKind.SERIES,
                            title = item.name,
                            description = item.plot,
                            genres = item.genre,
                        ),
                    )
                },
                toLocalEntity = { it.toEntity(profileId, imageBaseHost) },
                clearItems = { mediaDao.clearSeries(profileId) },
                upsertItems = mediaDao::upsertSeries,
                onProgress = { processed, total, kept, metrics ->
                    updateSectionProgress(
                        "Kids Series: $kept conservees, ${metrics.inheritedItems} heritees, " +
                            "${metrics.individuallyAnalyzedItems} analysees, ${metrics.cacheHits} cache",
                        MediaSection.Series,
                        SyncStatus.SyncSectionPhase.FILTERING,
                        58 + (processed * 36 / total.coerceAtLeast(1)),
                        processed,
                        0,
                        0,
                    )
                },
            )
        } else {
            null
        }
        val seriesItems = kidsImport?.keptItems ?: downloadedSeries.size
        logSyncMemory(stage = "after_get_series", live = liveItems, movies = movieItems, series = seriesItems)
        if (!kidsProfile) updateSectionProgress(
            "Import des series...",
            MediaSection.Series,
            SyncStatus.SyncSectionPhase.IMPORTING,
            70,
            seriesItems,
            seriesItems,
            0,
        )
        if (!kidsProfile) database.withTransaction {
            mediaDao.clearSeries(profileId)
            upsertMappedInBatches(
                items = downloadedSeries,
                mapper = { it.toEntity(profileId, imageBaseHost) },
                upsert = mediaDao::upsertSeries,
                onBatchCommitted = { processed, total ->
                    updateSectionProgress(
                        "Import des series...",
                        MediaSection.Series,
                        SyncStatus.SyncSectionPhase.IMPORTING,
                        70 + ((processed.toFloat() / total.coerceAtLeast(1)) * 28).toInt(),
                        processed,
                        0,
                        0,
                    )
                },
            )
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

    private suspend fun fetchSeriesWithFallback(
        host: String,
        username: String,
        password: String,
        categories: List<XtreamCategoryDto>,
        liveItems: Int,
        movieItems: Int,
        updateSectionProgress: suspend (String, MediaSection, SyncStatus.SyncSectionPhase, Int, Int?, Int, Int) -> Unit,
    ): List<XtreamSeriesDto> {
        return try {
            withTimeout(GlobalSeriesFetchTimeoutMs) {
                api.getSeries(username = username, password = password, host = host)
            }
        } catch (timeout: TimeoutCancellationException) {
            val categoryIds = categories.mapNotNull { category -> category.id?.takeIf { it.isNotBlank() } }
            if (categoryIds.isEmpty()) throw timeout
            updateSectionProgress(
                "Telechargement des series par categories...",
                MediaSection.Series,
                SyncStatus.SyncSectionPhase.RUNNING,
                44,
                null,
                0,
                categoryIds.size,
            )
            logSyncMemory(stage = "series_global_timeout_fallback", live = liveItems, movies = movieItems, seriesCategories = categories.size)
            val fetched = mutableListOf<XtreamSeriesDto>()
            categoryIds
                .forEachIndexed { index, categoryId ->
                    val categorySeries = withTimeout(CategorySeriesFetchTimeoutMs) {
                        api.getSeries(
                            username = username,
                            password = password,
                            categoryId = categoryId,
                            host = host,
                        )
                    }.map { series ->
                        if (series.categoryId.isNullOrBlank()) series.copy(categoryId = categoryId) else series
                    }
                    fetched += categorySeries
                    val percent = 44 + (((index + 1).toFloat() / categoryIds.size.toFloat()) * 22).toInt()
                    updateSectionProgress(
                        "Telechargement des series par categories...",
                        MediaSection.Series,
                        SyncStatus.SyncSectionPhase.RUNNING,
                        percent.coerceIn(44, 66),
                        fetched.size,
                        categorySeries.size,
                        categoryIds.size - index - 1,
                    )
                }
            fetched.distinctBy { it.seriesId }
        }
    }

    override suspend fun toggleFavorite(contentType: String, contentId: String) = withContext(Dispatchers.IO) {
        val profileId = activeProfileId()
        val existing = favoriteDao.get(profileId, contentType, contentId)
        if (existing == null) {
            favoriteDao.upsert(FavoriteEntity(profileId, contentType, contentId, System.currentTimeMillis()))
        } else {
            favoriteDao.delete(profileId, contentType, contentId)
        }
    }

    override suspend fun getSeriesEpisodes(seriesId: Int): List<Episode> = withContext(Dispatchers.IO) {
        mediaDao.getEpisodes(activeProfileId(), seriesId).map { it.toDomain() }
    }

    override suspend fun getEpisodeById(episodeId: Int): Episode? = withContext(Dispatchers.IO) {
        mediaDao.getEpisode(activeProfileId(), episodeId)?.toDomain()
    }

    override suspend fun buildPlaybackRequest(kind: PlaybackKind, id: String): PlaybackRequest? =
        withContext(Dispatchers.IO) {
            when (kind) {
                PlaybackKind.Live -> {
                    val profileId = activeProfileId()
                    val streamId = id.toIntOrNull() ?: return@withContext null
                    val local = mediaDao.getLiveStream(profileId, streamId)
                    val title = local?.name ?: return@withContext null
                    val subtitle = local.categoryId ?: "Live TV"
                    PlaybackRequest(kind, id, title, subtitle, local.directStreamUrl?.takeIf { it.isNotBlank() } ?: urlFactory.live(streamId))
                }

                PlaybackKind.Movie -> {
                    val profileId = activeProfileId()
                    val streamId = id.toIntOrNull() ?: return@withContext null
                    val local = mediaDao.getMovie(profileId, streamId)
                    val title = local?.title ?: return@withContext null
                    val extension = local.containerExtension ?: "mp4"
                    val progress = progressDao.get(profileId, kind.routeName, id)
                    PlaybackRequest(kind, id, title, local.categoryId ?: "Film", urlFactory.movie(streamId, extension), progress?.positionMs ?: 0L)
                }

                PlaybackKind.Episode -> {
                    val profileId = activeProfileId()
                    val episodeId = id.toIntOrNull() ?: return@withContext null
                    val local = mediaDao.getEpisode(profileId, episodeId)
                    val title = local?.title ?: return@withContext null
                    val extension = local.containerExtension ?: "mp4"
                    val progress = progressDao.get(profileId, kind.routeName, id)
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
                profileId = activeProfileId(),
                contentType = kind.routeName,
                contentId = id,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun synchronizeM3u(profile: PlaylistProfile): Result<Unit> {
        val m3uUrl = profile.m3uUrl
        val epgUrl = profile.epgUrl
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
            val profileId = profile.id
            val downloadedPlaylist = m3uPlaylistClient.fetch(m3uUrl)
            val kidsProfile = isKidsProfile(profileId)
            val kidsImport: KidsSectionImportResult? = if (kidsProfile) {
                kidsFilterDao.deleteObsoleteCategoryRules(KidsContentFilter.RuleVersion)
                kidsFilterDao.deleteObsoleteItemRules(KidsContentFilter.RuleVersion)
                val sourceKey = KidsFilterFingerprint.source("m3u", m3uUrl)
                val categories = downloadedPlaylist.channels
                    .distinctBy { it.categoryId }
                    .map { channel ->
                        XtreamCategoryDto(id = channel.categoryId, name = channel.group, parentId = null)
                    }
                filterAndImportKidsSection(
                    profileId = profileId,
                    sourceKey = sourceKey,
                    kind = KidsContentKind.LIVE_CHANNEL,
                    section = MediaSection.Live,
                    categories = categories,
                    items = downloadedPlaylist.channels,
                    toFilterInput = { channel ->
                        KidsItemInput(
                            value = channel,
                            id = channel.id.toString(),
                            categoryId = channel.categoryId,
                            metadata = KidsContentMetadata(
                                kind = KidsContentKind.LIVE_CHANNEL,
                                title = channel.name,
                            ),
                        )
                    },
                    toLocalEntity = { it.toEntity(profileId) },
                    clearItems = { mediaDao.clearLiveStreams(profileId) },
                    upsertItems = mediaDao::upsertLiveStreams,
                    onProgress = { processed, total, kept, metrics ->
                        val percent = 35 + (processed * 50 / total.coerceAtLeast(1))
                        syncWork.update(
                            message = "Kids M3U: $kept conserves, ${metrics.inheritedItems} herites, " +
                                "${metrics.cacheHits} cache",
                            progressPercent = percent,
                            currentItems = processed,
                            totalItems = total,
                        )
                        liveWork.update(
                            status = NetworkActivityStatus.Importing,
                            message = "Filtering and importing Kids channels",
                            progressPercent = percent,
                            currentItems = processed,
                            totalItems = total,
                        )
                    },
                )
            } else null
            val importedChannelCount = kidsImport?.keptItems ?: downloadedPlaylist.channels.size
            val importedPercent = if (kidsProfile) 88 else 72
            syncWork.update(
                message = "Chargement catalogue M3U...",
                progressPercent = if (kidsProfile) 88 else 50,
                currentItems = 1,
                totalItems = 2,
            )
            liveWork.update(
                status = NetworkActivityStatus.Importing,
                message = "Importing M3U channels",
                progressPercent = importedPercent,
                currentItems = importedChannelCount,
            )
            _syncStatus.value = SyncStatus.Running(
                message = "Chargement catalogue M3U...",
                completedItems = 1,
                totalItems = 2,
                catalogProgress = SyncStatus.CatalogProgress(
                    live = SyncStatus.SyncSectionProgress(
                        currentItems = importedChannelCount,
                        phase = SyncStatus.SyncSectionPhase.IMPORTING,
                        progressPercent = importedPercent,
                    ),
                ),
            )
            if (!kidsProfile) {
                categoryDao.deleteByType(profileId, MediaSection.Live.storageName)
                categoryDao.upsertAll(downloadedPlaylist.categories(profileId))
                mediaDao.clearLiveStreams(profileId)
                mediaDao.upsertLiveStreams(downloadedPlaylist.channels.map { it.toEntity(profileId) })
            }
            categoryDao.deleteByType(profileId, MediaSection.Movies.storageName)
            categoryDao.deleteByType(profileId, MediaSection.Series.storageName)
            mediaDao.clearMovies(profileId)
            mediaDao.clearSeries(profileId)
            epgUrl.takeIf { it.isNotBlank() }?.let { epgRepository.synchronize(it) }
            syncStateDao.upsert(
                SyncStateEntity(
                    profileId = profileId,
                    id = "catalog",
                    lastSync = now,
                    status = "success",
                    message = "Synchronisation M3U terminee",
                ),
            )
            accountManager.markProfileSynced(profileId, now)
            if (activeProfileId() == profileId) {
                invalidateLocalCatalogCache()
                bumpCatalogRevision()
            }
            _syncStatus.value = SyncStatus.Success(
                message = "Synchronisation M3U terminee",
                catalogProgress = SyncStatus.CatalogProgress(
                    live = SyncStatus.SyncSectionProgress(
                        currentItems = importedChannelCount,
                        completed = true,
                        phase = SyncStatus.SyncSectionPhase.COMPLETED,
                        progressPercent = 100,
                    ),
                ),
            )
            liveWork.update(progressPercent = 100, currentItems = importedChannelCount)
            liveWork.complete("Live TV ready")
            syncWork.update(progressPercent = 100, currentItems = importedChannelCount)
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

private data class KidsSectionImportResult(
    val keptItems: Int,
    val metrics: KidsFilterMetrics,
)

private fun KidsCategoryDecisionEntity.toCachedDecision(): CachedKidsCategoryDecision =
    CachedKidsCategoryDecision(
        categoryId = categoryId,
        normalizedName = normalizedName,
        metadataFingerprint = metadataFingerprint,
        ruleVersion = ruleVersion,
        decision = KidsCategoryDecision(
            classification = enumValueOrDefault(decision, KidsCategoryClassification.NOT_KIDS_CATEGORY),
            score = score,
            source = enumValueOrDefault(source, KidsDecisionSource.ITEM_SCORE),
            reason = reason,
        ),
    )

private fun KidsItemDecisionEntity.toCachedDecision(): CachedKidsItemDecision =
    CachedKidsItemDecision(
        contentId = contentId,
        categoryId = categoryId,
        metadataFingerprint = metadataFingerprint,
        ruleVersion = ruleVersion,
        decision = KidsContentDecision(
            classification = enumValueOrDefault(decision, KidsContentClassification.NOT_KIDS_CONTENT),
            score = score,
            source = enumValueOrDefault(source, KidsDecisionSource.ITEM_SCORE),
            reason = reason,
            inheritedCategoryId = inheritedCategoryId,
        ),
    )

private fun CachedKidsCategoryDecision.toEntity(
    sourceKey: String,
    contentType: String,
    updatedAt: Long,
): KidsCategoryDecisionEntity = KidsCategoryDecisionEntity(
    sourceKey = sourceKey,
    contentType = contentType,
    categoryId = categoryId,
    normalizedName = normalizedName,
    decision = decision.classification.name,
    score = decision.score,
    source = decision.source.name,
    reason = decision.reason,
    ruleVersion = ruleVersion,
    metadataFingerprint = metadataFingerprint,
    updatedAt = updatedAt,
)

private fun CachedKidsItemDecision.toEntity(
    sourceKey: String,
    contentType: String,
    updatedAt: Long,
): KidsItemDecisionEntity = KidsItemDecisionEntity(
    sourceKey = sourceKey,
    contentType = contentType,
    contentId = contentId,
    categoryId = categoryId,
    allowed = decision.allowed,
    decision = decision.classification.name,
    score = decision.score,
    source = decision.source.name,
    reason = decision.reason,
    inheritedCategoryId = decision.inheritedCategoryId,
    ruleVersion = ruleVersion,
    metadataFingerprint = metadataFingerprint,
    updatedAt = updatedAt,
)

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)

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
        SyncStatus.SyncSectionPhase.FILTERING,
        SyncStatus.SyncSectionPhase.LOADING_TRENDS -> NetworkActivityStatus.Running
        SyncStatus.SyncSectionPhase.IMPORTING -> NetworkActivityStatus.Importing
        SyncStatus.SyncSectionPhase.COMPLETED -> NetworkActivityStatus.Completed
        SyncStatus.SyncSectionPhase.ERROR -> NetworkActivityStatus.Error
    }

private const val KidsFilterImportBatchSize = 256
private const val KidsFilterLogTag = "SVKidsFilter"

private fun TrendingCatalogItem.containsAdultMarker(): Boolean =
    containsAdultMarker(title, categoryName)

private fun containsAdultMarker(vararg values: String?): Boolean =
    HomeTrendingPolicy.containsAdultMarker(*values)

private fun MovieEntity.toTrendingEntity(profileId: String, rankTimestamp: Long): TrendingMediaEntity =
    TrendingMediaEntity(
        profileId = profileId,
        contentType = TrendingMovieType,
        contentId = streamId,
        sampleContentId = streamId,
        sampleExtension = containerExtension,
        rating = rating.toTrendingRating(),
        updatedAt = rankTimestamp,
    )

private fun SeriesEntity.toTrendingEntity(profileId: String, rankTimestamp: Long): TrendingMediaEntity =
    TrendingMediaEntity(
        profileId = profileId,
        contentType = TrendingSeriesType,
        contentId = seriesId,
        sampleContentId = null,
        sampleExtension = null,
        rating = rating.toTrendingRating(),
        updatedAt = rankTimestamp,
    )

private fun String?.toTrendingRating(): Float =
    this?.trim()?.replace(',', '.')?.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f } ?: 0f

private suspend fun <Remote, Local> upsertMappedInBatches(
    items: List<Remote>,
    mapper: (Remote) -> Local?,
    onBatchCommitted: suspend (processed: Int, total: Int) -> Unit = { _, _ -> },
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
        onBatchCommitted(index, items.size)
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
private const val GlobalSeriesFetchTimeoutMs = 120_000L
private const val CategorySeriesFetchTimeoutMs = 60_000L
private const val SyncMemoryTag = "SVSyncMemory"
private const val TrendingMovieType = "movie"
private const val TrendingSeriesType = "series"

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

private fun PlaylistProfile.toAccountProfile(
    liveCount: Int,
    movieCount: Int,
    seriesCount: Int,
): AccountProfile =
    AccountProfile(
        id = id,
        name = name.ifBlank { "Profil SmartVision" },
        host = when (source) {
            PlaylistSource.Xtream -> xtreamHost
            PlaylistSource.M3u -> m3uUrl
        },
        usernameMasked = xtreamUsername.maskProfileUsername(),
        status = when {
            !isConfigured -> "Non configure"
            status == PlaylistProfileStatus.Active -> "Actif"
            status == PlaylistProfileStatus.Error -> "Erreur"
            else -> "Inactif"
        },
        expirationDate = null,
        activeConnections = null,
        maxConnections = null,
        lastSync = lastSyncAt?.let { formatDateTime(it) },
        liveCount = liveCount,
        movieCount = movieCount,
        seriesCount = seriesCount,
    )

private fun String.maskProfileUsername(): String =
    when {
        isBlank() -> ""
        length <= 2 -> "***"
        length <= 5 -> take(1) + "***"
        else -> take(2) + "****" + takeLast(2)
    }
