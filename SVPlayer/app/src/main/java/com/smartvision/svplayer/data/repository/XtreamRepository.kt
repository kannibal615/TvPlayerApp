package com.smartvision.svplayer.data.repository

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.smartvision.svplayer.data.models.XtreamLiveCategory
import com.smartvision.svplayer.data.models.XtreamLiveStream
import com.smartvision.svplayer.data.models.XtreamMovieCategory
import com.smartvision.svplayer.data.models.XtreamMovieDetails
import com.smartvision.svplayer.data.models.XtreamMovieStream
import com.smartvision.svplayer.data.models.XtreamSeriesCategory
import com.smartvision.svplayer.data.models.XtreamSeriesDetails
import com.smartvision.svplayer.data.models.XtreamSeriesEpisode
import com.smartvision.svplayer.data.models.XtreamSeriesStream
import com.smartvision.svplayer.data.remote.XtreamApiClient
import com.smartvision.svplayer.data.remote.XtreamUrlFactory
import com.smartvision.svplayer.data.remote.dto.XtreamCategoryDto
import com.smartvision.svplayer.data.remote.dto.XtreamEpisodeDto
import com.smartvision.svplayer.data.remote.dto.XtreamLiveStreamDto
import com.smartvision.svplayer.data.remote.dto.XtreamMovieDto
import com.smartvision.svplayer.data.remote.dto.XtreamSeriesDto
import com.smartvision.svplayer.data.remote.dto.XtreamSeriesInfoDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class XtreamRepository(
    private val apiClient: XtreamApiClient,
    private val urlFactory: XtreamUrlFactory,
) {
    fun clearCaches() {
        liveCategoriesCache = emptyList()
        streamsByCategory.clear()
        streamsById.clear()
        movieCategoriesCache = emptyList()
        moviesByCategory.clear()
        moviesById.clear()
        movieDetailsById.clear()
        seriesCategoriesCache = emptyList()
        seriesByCategory.clear()
        seriesById.clear()
        seriesDetailsById.clear()
        episodesBySeriesId.clear()
        episodesById.clear()
    }
    private var liveCategoriesCache: List<XtreamLiveCategory> = emptyList()
    private val streamsByCategory = mutableMapOf<String, List<XtreamLiveStream>>()
    private val streamsById = mutableMapOf<Int, XtreamLiveStream>()
    private var movieCategoriesCache: List<XtreamMovieCategory> = emptyList()
    private val moviesByCategory = mutableMapOf<String, List<XtreamMovieStream>>()
    private val moviesById = mutableMapOf<Int, XtreamMovieStream>()
    private val movieDetailsById = mutableMapOf<Int, XtreamMovieDetails>()
    private var seriesCategoriesCache: List<XtreamSeriesCategory> = emptyList()
    private val seriesByCategory = mutableMapOf<String, List<XtreamSeriesStream>>()
    private val seriesById = mutableMapOf<Int, XtreamSeriesStream>()
    private val seriesDetailsById = mutableMapOf<Int, XtreamSeriesDetails>()
    private val episodesBySeriesId = mutableMapOf<Int, List<XtreamSeriesEpisode>>()
    private val episodesById = mutableMapOf<Int, XtreamSeriesEpisode>()

    suspend fun getLiveCategories(): List<XtreamLiveCategory> = withContext(Dispatchers.IO) {
        val categories = apiClient.getLiveCategories()
            .mapNotNull { it.toLiveCategory() }
            .map { category ->
                category.copy(count = streamsByCategory[category.id]?.size)
            }
        liveCategoriesCache = categories
        categories
    }

    suspend fun getLiveStreams(categoryId: String): List<XtreamLiveStream> = withContext(Dispatchers.IO) {
        val streams = apiClient.getLiveStreams(categoryId)
            .mapNotNull { it.toLiveStream() }
        streamsByCategory[categoryId] = streams
        streams.forEach { stream -> streamsById[stream.streamId] = stream }
        liveCategoriesCache = liveCategoriesCache.map { category ->
            if (category.id == categoryId) category.copy(count = streams.size) else category
        }
        streams
    }

    suspend fun getMovieCategories(): List<XtreamMovieCategory> = withContext(Dispatchers.IO) {
        val categories = apiClient.getMovieCategories()
            .mapNotNull { it.toMovieCategory() }
            .map { category ->
                category.copy(count = moviesByCategory[category.id]?.size)
            }
        movieCategoriesCache = categories
        categories
    }

    suspend fun getMovies(categoryId: String): List<XtreamMovieStream> = withContext(Dispatchers.IO) {
        val movies = apiClient.getMovies(categoryId)
            .mapNotNull { it.toMovieStream() }
        moviesByCategory[categoryId] = movies
        movies.forEach { movie -> moviesById[movie.streamId] = movie }
        movieCategoriesCache = movieCategoriesCache.map { category ->
            if (category.id == categoryId) category.copy(count = movies.size) else category
        }
        movies
    }

    suspend fun getMovieDetails(movieId: Int): XtreamMovieDetails = withContext(Dispatchers.IO) {
        movieDetailsById[movieId]?.let { return@withContext it }
        val cachedMovie = moviesById[movieId]
        val response = apiClient.getMovieInfo(movieId)
        val info = response.info.asObjectOrNull()
        val movieData = response.movieData.asObjectOrNull()
        val details = XtreamMovieDetails(
            movieId = movieId,
            title = info.string("name", "title")
                ?: movieData.string("name", "title")
                ?: cachedMovie?.title
                ?: "Film $movieId",
            posterUrl = info.string("movie_image", "cover", "poster_path", "image")
                ?: cachedMovie?.posterUrl,
            backdropUrl = info.string("backdrop_path", "backdrop", "background_image", "cover_big")
                ?: info.firstStringFromArray("backdrop_path"),
            plot = info.string("plot", "description", "overview"),
            genre = info.string("genre"),
            releaseDate = info.string("releasedate", "releaseDate", "release_date", "year"),
            rating = info.string("rating", "rating_5based") ?: cachedMovie?.rating,
            duration = info.string("duration", "duration_secs"),
            director = info.string("director"),
            cast = info.string("cast", "actors"),
            containerExtension = movieData.string("container_extension")
                ?: cachedMovie?.containerExtension
                ?: "mp4",
            categoryId = movieData.string("category_id") ?: cachedMovie?.categoryId,
        )
        movieDetailsById[movieId] = details
        details
    }

    suspend fun getSeriesCategories(): List<XtreamSeriesCategory> = withContext(Dispatchers.IO) {
        val categories = apiClient.getSeriesCategories()
            .mapNotNull { it.toSeriesCategory() }
            .map { category ->
                category.copy(count = seriesByCategory[category.id]?.size)
            }
        seriesCategoriesCache = categories
        categories
    }

    suspend fun getSeries(categoryId: String): List<XtreamSeriesStream> = withContext(Dispatchers.IO) {
        val series = apiClient.getSeries(categoryId)
            .mapNotNull { it.toSeriesStream() }
        seriesByCategory[categoryId] = series
        series.forEach { item -> seriesById[item.seriesId] = item }
        seriesCategoriesCache = seriesCategoriesCache.map { category ->
            if (category.id == categoryId) category.copy(count = series.size) else category
        }
        series
    }

    suspend fun getSeriesDetails(seriesId: Int): XtreamSeriesDetails = withContext(Dispatchers.IO) {
        seriesDetailsById[seriesId]?.let { return@withContext it }
        val cachedSeries = seriesById[seriesId]
        val response = apiClient.getSeriesInfo(seriesId)
        val info = response.info.asObjectOrNull()
        val episodes = response.toSeriesEpisodes(seriesId)
        episodesBySeriesId[seriesId] = episodes
        episodes.forEach { episode -> episodesById[episode.episodeId] = episode }
        val details = XtreamSeriesDetails(
            seriesId = seriesId,
            title = info.string("name", "title")
                ?: cachedSeries?.title
                ?: "Serie $seriesId",
            coverUrl = info.string("cover", "movie_image", "poster_path", "image")
                ?: cachedSeries?.coverUrl,
            backdropUrl = info.string("backdrop_path", "backdrop", "background_image", "cover_big")
                ?: info.firstStringFromArray("backdrop_path"),
            plot = info.string("plot", "description", "overview") ?: cachedSeries?.plot,
            genre = info.string("genre") ?: cachedSeries?.genre,
            releaseDate = info.string("releaseDate", "releasedate", "release_date", "year")
                ?: cachedSeries?.releaseDate,
            rating = info.string("rating", "rating_5based") ?: cachedSeries?.rating,
            episodeRunTime = info.string("episode_run_time", "duration") ?: cachedSeries?.episodeRunTime,
            director = info.string("director"),
            cast = info.string("cast", "actors"),
            categoryId = info.string("category_id") ?: cachedSeries?.categoryId,
        )
        seriesDetailsById[seriesId] = details
        details
    }

    suspend fun getSeriesEpisodes(seriesId: Int): List<XtreamSeriesEpisode> = withContext(Dispatchers.IO) {
        episodesBySeriesId[seriesId]?.let { return@withContext it }
        val episodes = apiClient.getSeriesInfo(seriesId).toSeriesEpisodes(seriesId)
        episodesBySeriesId[seriesId] = episodes
        episodes.forEach { episode -> episodesById[episode.episodeId] = episode }
        episodes
    }

    fun getCachedLiveStream(streamId: Int): XtreamLiveStream? =
        streamsById[streamId]

    fun getCachedLiveStreams(): List<XtreamLiveStream> =
        streamsById.values.toList()

    fun getCachedCategories(): List<XtreamLiveCategory> =
        liveCategoriesCache

    fun getCachedMovie(movieId: Int): XtreamMovieStream? =
        moviesById[movieId]

    fun getCachedMovies(): List<XtreamMovieStream> =
        moviesById.values.toList()

    fun getCachedMovieCategories(): List<XtreamMovieCategory> =
        movieCategoriesCache

    fun getCachedSeries(seriesId: Int): XtreamSeriesStream? =
        seriesById[seriesId]

    fun getCachedSeriesList(): List<XtreamSeriesStream> =
        seriesById.values.toList()

    fun getCachedSeriesCategories(): List<XtreamSeriesCategory> =
        seriesCategoriesCache

    fun getCachedEpisode(episodeId: Int): XtreamSeriesEpisode? =
        episodesById[episodeId]

    fun getCachedNextEpisode(episodeId: Int): XtreamSeriesEpisode? {
        val current = episodesById[episodeId] ?: return null
        val ordered = episodesBySeriesId[current.seriesId].orEmpty()
            .sortedWith(compareBy<XtreamSeriesEpisode> { it.seasonNumber }.thenBy { it.episodeNumber })
        val currentIndex = ordered.indexOfFirst { it.episodeId == episodeId }
        return ordered.getOrNull(currentIndex + 1)
    }

    fun getCachedPreviousEpisode(episodeId: Int): XtreamSeriesEpisode? {
        val current = episodesById[episodeId] ?: return null
        val ordered = episodesBySeriesId[current.seriesId].orEmpty()
            .sortedWith(compareBy<XtreamSeriesEpisode> { it.seasonNumber }.thenBy { it.episodeNumber })
        val currentIndex = ordered.indexOfFirst { it.episodeId == episodeId }
        return ordered.getOrNull(currentIndex - 1)
    }

    fun getCachedNextLiveStream(streamId: Int): XtreamLiveStream? =
        adjacentLiveStream(streamId, offset = 1)

    fun getCachedPreviousLiveStream(streamId: Int): XtreamLiveStream? =
        adjacentLiveStream(streamId, offset = -1)

    fun getCachedNextMovie(movieId: Int): XtreamMovieStream? =
        adjacentMovie(movieId, offset = 1)

    fun getCachedPreviousMovie(movieId: Int): XtreamMovieStream? =
        adjacentMovie(movieId, offset = -1)

    fun buildLiveStreamUrl(stream: XtreamLiveStream): String =
        stream.directSource
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: urlFactory.live(stream.streamId)

    fun buildLiveStreamUrl(streamId: Int): String =
        streamsById[streamId]?.let(::buildLiveStreamUrl) ?: urlFactory.live(streamId)

    fun buildLiveStreamFallbackUrl(streamId: Int): String =
        urlFactory.liveHls(streamId)

    fun buildMovieStreamUrl(movie: XtreamMovieStream): String =
        urlFactory.movie(movie.streamId, movie.containerExtension)

    fun buildMovieStreamUrl(movieId: Int): String =
        moviesById[movieId]?.let(::buildMovieStreamUrl) ?: urlFactory.movie(movieId, "mp4")

    fun buildEpisodeStreamUrl(episode: XtreamSeriesEpisode): String =
        urlFactory.episode(episode.episodeId, episode.containerExtension)

    fun buildEpisodeStreamUrl(episodeId: Int): String =
        episodesById[episodeId]?.let(::buildEpisodeStreamUrl) ?: urlFactory.episode(episodeId, "mp4")

    private fun adjacentLiveStream(streamId: Int, offset: Int): XtreamLiveStream? {
        val current = streamsById[streamId] ?: return null
        val ordered = current.categoryId?.let { streamsByCategory[it] }.orEmpty()
            .ifEmpty { streamsById.values.toList() }
            .sortedWith(compareBy<XtreamLiveStream> { it.number }.thenBy { it.name })
        val currentIndex = ordered.indexOfFirst { it.streamId == streamId }
        return ordered.getOrNull(currentIndex + offset)
    }

    private fun adjacentMovie(movieId: Int, offset: Int): XtreamMovieStream? {
        val current = moviesById[movieId] ?: return null
        val ordered = current.categoryId?.let { moviesByCategory[it] }.orEmpty()
            .ifEmpty { moviesById.values.toList() }
            .sortedWith(compareBy<XtreamMovieStream> { it.number }.thenBy { it.title })
        val currentIndex = ordered.indexOfFirst { it.streamId == movieId }
        return ordered.getOrNull(currentIndex + offset)
    }
}

private fun XtreamCategoryDto.toLiveCategory(): XtreamLiveCategory? {
    val safeId = id?.takeIf { it.isNotBlank() } ?: return null
    return XtreamLiveCategory(
        id = safeId,
        name = name.orEmpty().ifBlank { "Sans categorie" },
        parentId = parentId,
    )
}

private fun XtreamLiveStreamDto.toLiveStream(): XtreamLiveStream? {
    val safeStreamId = streamId ?: return null
    return XtreamLiveStream(
        number = number ?: safeStreamId,
        name = name.orEmpty().ifBlank { "Chaine $safeStreamId" },
        streamType = streamType,
        streamId = safeStreamId,
        streamIcon = icon?.takeIf { it.isNotBlank() },
        epgChannelId = epgChannelId,
        added = added,
        categoryId = categoryId,
        customSid = customSid,
        tvArchive = tvArchive,
        directSource = directSource,
    )
}

private fun XtreamCategoryDto.toMovieCategory(): XtreamMovieCategory? {
    val safeId = id?.takeIf { it.isNotBlank() } ?: return null
    return XtreamMovieCategory(
        id = safeId,
        name = name.orEmpty().ifBlank { "Sans categorie" },
        parentId = parentId,
    )
}

private fun XtreamMovieDto.toMovieStream(): XtreamMovieStream? {
    val safeStreamId = streamId ?: return null
    return XtreamMovieStream(
        number = number ?: safeStreamId,
        title = name.orEmpty().ifBlank { "Film $safeStreamId" },
        streamId = safeStreamId,
        posterUrl = icon?.takeIf { it.isNotBlank() },
        categoryId = categoryId,
        containerExtension = containerExtension.orEmpty().ifBlank { "mp4" },
        rating = ratingFiveBased ?: rating,
        added = added,
    )
}

private fun XtreamCategoryDto.toSeriesCategory(): XtreamSeriesCategory? {
    val safeId = id?.takeIf { it.isNotBlank() } ?: return null
    return XtreamSeriesCategory(
        id = safeId,
        name = name.orEmpty().ifBlank { "Sans categorie" },
        parentId = parentId,
    )
}

private fun XtreamSeriesDto.toSeriesStream(): XtreamSeriesStream? {
    val safeSeriesId = seriesId ?: return null
    return XtreamSeriesStream(
        number = number ?: safeSeriesId,
        title = name.orEmpty().ifBlank { "Serie $safeSeriesId" },
        seriesId = safeSeriesId,
        coverUrl = cover?.takeIf { it.isNotBlank() },
        plot = plot?.takeIf { it.isNotBlank() },
        genre = genre?.takeIf { it.isNotBlank() },
        releaseDate = releaseDate?.takeIf { it.isNotBlank() },
        rating = rating?.takeIf { it.isNotBlank() },
        episodeRunTime = episodeRunTime?.takeIf { it.isNotBlank() },
        categoryId = categoryId,
    )
}

private fun XtreamEpisodeDto.toSeriesEpisode(seriesId: Int, seasonNumber: Int): XtreamSeriesEpisode? {
    val safeEpisodeId = id?.toIntOrNull() ?: return null
    return XtreamSeriesEpisode(
        episodeId = safeEpisodeId,
        seriesId = seriesId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber ?: safeEpisodeId,
        title = title.orEmpty().ifBlank { "Episode $safeEpisodeId" },
        containerExtension = containerExtension.orEmpty().ifBlank { "mp4" },
        duration = info?.duration?.takeIf { it.isNotBlank() },
        plot = info?.plot?.takeIf { it.isNotBlank() },
    )
}

private fun XtreamSeriesInfoDto.toSeriesEpisodes(seriesId: Int): List<XtreamSeriesEpisode> =
    episodes
        .orEmpty()
        .flatMap { (season, remoteEpisodes) ->
            val seasonNumber = season.toIntOrNull() ?: 0
            remoteEpisodes.mapNotNull { it.toSeriesEpisode(seriesId, seasonNumber) }
        }
        .sortedWith(compareBy<XtreamSeriesEpisode> { it.seasonNumber }.thenBy { it.episodeNumber })

private fun JsonElement?.asObjectOrNull(): JsonObject? =
    this
        ?.takeIf { !it.isJsonNull && it.isJsonObject }
        ?.asJsonObject

private fun JsonObject?.string(vararg names: String): String? {
    if (this == null) return null
    return names.asSequence()
        .mapNotNull { name -> get(name).asCleanString() }
        .firstOrNull()
}

private fun JsonObject?.firstStringFromArray(name: String): String? {
    val element = this?.get(name)?.takeIf { !it.isJsonNull && it.isJsonArray } ?: return null
    return element.asJsonArray.asSequence()
        .mapNotNull { it.asCleanString() }
        .firstOrNull()
}

private fun JsonElement?.asCleanString(): String? {
    val element = this?.takeIf { !it.isJsonNull } ?: return null
    return when {
        element.isJsonPrimitive -> element.asString
        element.isJsonArray -> element.asJsonArray.asSequence()
            .mapNotNull { it.asCleanString() }
            .firstOrNull()
        else -> null
    }?.trim()?.takeIf { it.isNotBlank() && it != "null" }
}
