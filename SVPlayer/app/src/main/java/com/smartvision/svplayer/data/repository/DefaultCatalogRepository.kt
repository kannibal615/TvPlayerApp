package com.smartvision.svplayer.data.repository

import com.smartvision.svplayer.core.config.XtreamCredentialsProvider
import com.smartvision.svplayer.data.local.dao.CategoryDao
import com.smartvision.svplayer.data.local.dao.FavoriteDao
import com.smartvision.svplayer.data.local.dao.MediaDao
import com.smartvision.svplayer.data.local.dao.ProfileDao
import com.smartvision.svplayer.data.local.dao.ProgressDao
import com.smartvision.svplayer.data.local.dao.SyncStateDao
import com.smartvision.svplayer.data.local.entity.FavoriteEntity
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.local.entity.SyncStateEntity
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultCatalogRepository(
    private val api: XtreamApiService,
    private val credentialsProvider: XtreamCredentialsProvider,
    private val urlFactory: XtreamUrlFactory,
    private val categoryDao: CategoryDao,
    private val mediaDao: MediaDao,
    private val profileDao: ProfileDao,
    private val favoriteDao: FavoriteDao,
    private val progressDao: ProgressDao,
    private val syncStateDao: SyncStateDao,
) : CatalogRepository {
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus

    override fun observeLiveCategories(): Flow<List<Category>> =
        combine(
            categoryDao.observeByType(MediaSection.Live.storageName),
            mediaDao.observeLiveStreams(),
        ) { categories, streams ->
            if (!credentialsProvider.current().isConfigured) return@combine emptyList()
            val counts = streams.groupingBy { it.categoryId }.eachCount()
            categories.map { it.toDomain(MediaSection.Live, counts[it.id] ?: 0) }
        }

    override fun observeLiveChannels(categoryId: String?): Flow<List<LiveChannel>> =
        combine(
            categoryDao.observeByType(MediaSection.Live.storageName),
            mediaDao.observeLiveStreamsByCategory(categoryId),
        ) { categories, streams ->
            if (!credentialsProvider.current().isConfigured) return@combine emptyList()
            val names = categories.associate { it.id to it.name }
            streams.map { it.toDomain(names[it.categoryId] ?: "Live TV") }
        }

    override fun observeMovieCategories(): Flow<List<Category>> =
        combine(
            categoryDao.observeByType(MediaSection.Movies.storageName),
            mediaDao.observeMovies(),
        ) { categories, movies ->
            if (!credentialsProvider.current().isConfigured) return@combine emptyList()
            val counts = movies.groupingBy { it.categoryId }.eachCount()
            categories.map { it.toDomain(MediaSection.Movies, counts[it.id] ?: 0) }
        }

    override fun observeMovies(categoryId: String?): Flow<List<Movie>> =
        combine(
            categoryDao.observeByType(MediaSection.Movies.storageName),
            mediaDao.observeMoviesByCategory(categoryId?.takeUnless { it == "all" || it == "new" || it == "favorites" }),
        ) { categories, movies ->
            if (!credentialsProvider.current().isConfigured) return@combine emptyList()
            val names = categories.associate { it.id to it.name }
            movies.map { it.toDomain(names[it.categoryId] ?: "Films") }
        }

    override fun observeSeriesCategories(): Flow<List<Category>> =
        combine(
            categoryDao.observeByType(MediaSection.Series.storageName),
            mediaDao.observeSeries(),
        ) { categories, series ->
            if (!credentialsProvider.current().isConfigured) return@combine emptyList()
            val counts = series.groupingBy { it.categoryId }.eachCount()
            categories.map { it.toDomain(MediaSection.Series, counts[it.id] ?: 0) }
        }

    override fun observeSeries(categoryId: String?): Flow<List<TvSeries>> =
        combine(
            categoryDao.observeByType(MediaSection.Series.storageName),
            mediaDao.observeSeriesByCategory(categoryId?.takeUnless { it == "all" || it == "new" || it == "favorites" }),
        ) { categories, series ->
            if (!credentialsProvider.current().isConfigured) return@combine emptyList()
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

    override suspend fun synchronize(): Result<Unit> = withContext(Dispatchers.IO) {
        val credentials = credentialsProvider.current()
        if (!credentials.isConfigured) {
            val message = "Aucun compte Xtream configure"
            _syncStatus.value = SyncStatus.Error(message)
            return@withContext Result.failure(IllegalStateException(message))
        }

        _syncStatus.value = SyncStatus.Running
        try {
            val now = System.currentTimeMillis()
            val account = api.getAccount(credentials.username, credentials.password)
            profileDao.upsert(account.toProfileEntity(credentials, now))

            val liveCategories = api.getCategories(credentials.username, credentials.password, "get_live_categories")
            val liveStreams = api.getLiveStreams(credentials.username, credentials.password)
            categoryDao.deleteByType(MediaSection.Live.storageName)
            categoryDao.upsertAll(liveCategories.mapNotNull { it.toEntity(MediaSection.Live) })
            mediaDao.clearLiveStreams()
            mediaDao.upsertLiveStreams(liveStreams.mapNotNull { it.toEntity() })

            val movieCategories = api.getCategories(credentials.username, credentials.password, "get_vod_categories")
            val movies = api.getMovies(credentials.username, credentials.password)
            categoryDao.deleteByType(MediaSection.Movies.storageName)
            categoryDao.upsertAll(movieCategories.mapNotNull { it.toEntity(MediaSection.Movies) })
            mediaDao.clearMovies()
            mediaDao.upsertMovies(movies.mapNotNull { it.toEntity() })

            val seriesCategories = api.getCategories(credentials.username, credentials.password, "get_series_categories")
            val series = api.getSeries(credentials.username, credentials.password)
            categoryDao.deleteByType(MediaSection.Series.storageName)
            categoryDao.upsertAll(seriesCategories.mapNotNull { it.toEntity(MediaSection.Series) })
            mediaDao.clearSeries()
            mediaDao.upsertSeries(series.mapNotNull { it.toEntity() })

            syncStateDao.upsert(
                SyncStateEntity(
                    id = "catalog",
                    lastSync = now,
                    status = "success",
                    message = "Synchronisation terminee",
                ),
            )
            _syncStatus.value = SyncStatus.Success("Synchronisation terminee")
            Result.success(Unit)
        } catch (error: Exception) {
            syncStateDao.upsert(
                SyncStateEntity(
                    id = "catalog",
                    lastSync = syncStateDao.get()?.lastSync,
                    status = "error",
                    message = "Erreur reseau",
                ),
            )
            _syncStatus.value = SyncStatus.Error("Erreur reseau")
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
        val local = mediaDao.getEpisodes(seriesId)
        if (local.isNotEmpty()) return@withContext local.map { it.toDomain() }

        val credentials = credentialsProvider.current()
        if (credentials.isConfigured) {
            runCatching {
                val remote = api.getSeriesInfo(
                    username = credentials.username,
                    password = credentials.password,
                    seriesId = seriesId,
                )
                val entities = remote.episodes.orEmpty().flatMap { (season, episodes) ->
                    val seasonNumber = season.toIntOrNull() ?: 0
                    episodes.mapNotNull { it.toEntity(seriesId, seasonNumber) }
                }
                if (entities.isNotEmpty()) {
                    mediaDao.deleteEpisodes(seriesId)
                    mediaDao.upsertEpisodes(entities)
                }
                entities.map { it.toDomain() }
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
    }

    override suspend fun buildPlaybackRequest(kind: PlaybackKind, id: String): PlaybackRequest? =
        withContext(Dispatchers.IO) {
            when (kind) {
                PlaybackKind.Live -> {
                    val streamId = id.toIntOrNull() ?: return@withContext null
                    val local = mediaDao.getLiveStream(streamId)
                    val title = local?.name ?: return@withContext null
                    val subtitle = local.categoryId ?: "Live TV"
                    PlaybackRequest(kind, id, title, subtitle, urlFactory.live(streamId))
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
