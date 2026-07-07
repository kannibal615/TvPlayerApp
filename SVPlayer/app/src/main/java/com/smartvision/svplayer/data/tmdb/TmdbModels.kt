package com.smartvision.svplayer.data.tmdb

import com.smartvision.svplayer.data.local.entity.TmdbMovieMetadataEntity
import com.smartvision.svplayer.data.local.entity.TmdbSeriesMetadataEntity

data class TmdbPersonCredit(
    val name: String,
    val role: String? = null,
    val profileUrl: String? = null,
)

data class TmdbVideoItem(
    val key: String,
    val name: String,
    val type: String,
    val official: Boolean = false,
    val language: String? = null,
)

data class TmdbRecommendation(
    val tmdbId: Int,
    val title: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val releaseDate: String? = null,
    val voteAverage: Double? = null,
)

data class TmdbMovieMetadata(
    val tmdbId: Int,
    val language: String,
    val title: String,
    val originalTitle: String?,
    val overview: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val logoUrl: String?,
    val releaseDate: String?,
    val runtimeMinutes: Int?,
    val genres: String?,
    val voteAverage: Double?,
    val voteCount: Int?,
    val popularity: Double?,
    val cast: String?,
    val castMembers: List<TmdbPersonCredit>,
    val director: String?,
    val directors: List<TmdbPersonCredit>,
    val trailerKey: String?,
    val videos: List<TmdbVideoItem>,
    val recommendations: List<TmdbRecommendation>,
    val collectionName: String?,
    val certification: String?,
    val providersSummary: String?,
    val adult: Boolean,
)

data class TmdbSeriesMetadata(
    val tmdbId: Int,
    val language: String,
    val name: String,
    val originalName: String?,
    val overview: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val logoUrl: String?,
    val firstAirDate: String?,
    val episodeRunTimeMinutes: Int?,
    val genres: String?,
    val voteAverage: Double?,
    val voteCount: Int?,
    val popularity: Double?,
    val cast: String?,
    val castMembers: List<TmdbPersonCredit>,
    val createdBy: String?,
    val creators: List<TmdbPersonCredit>,
    val trailerKey: String?,
    val videos: List<TmdbVideoItem>,
    val recommendations: List<TmdbRecommendation>,
    val certification: String?,
    val providersSummary: String?,
    val adult: Boolean,
)

fun TmdbMovieMetadataEntity.toTmdbMovieMetadata(): TmdbMovieMetadata =
    TmdbMovieMetadata(
        tmdbId = tmdbId,
        language = language,
        title = title,
        originalTitle = originalTitle,
        overview = overview,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        logoUrl = logoUrl,
        releaseDate = releaseDate,
        runtimeMinutes = runtimeMinutes,
        genres = genres,
        voteAverage = voteAverage,
        voteCount = voteCount,
        popularity = popularity,
        cast = cast,
        castMembers = castJson.decodeTmdbList(),
        director = director,
        directors = directorJson.decodeTmdbList(),
        trailerKey = trailerKey,
        videos = videosJson.decodeTmdbList(),
        recommendations = recommendationsJson.decodeTmdbList(),
        collectionName = collectionName,
        certification = certification,
        providersSummary = providersSummary,
        adult = adult,
    )

fun TmdbSeriesMetadataEntity.toTmdbSeriesMetadata(): TmdbSeriesMetadata =
    TmdbSeriesMetadata(
        tmdbId = tmdbId,
        language = language,
        name = name,
        originalName = originalName,
        overview = overview,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        logoUrl = logoUrl,
        firstAirDate = firstAirDate,
        episodeRunTimeMinutes = episodeRunTimeMinutes,
        genres = genres,
        voteAverage = voteAverage,
        voteCount = voteCount,
        popularity = popularity,
        cast = cast,
        castMembers = castJson.decodeTmdbList(),
        createdBy = createdBy,
        creators = createdByJson.decodeTmdbList(),
        trailerKey = trailerKey,
        videos = videosJson.decodeTmdbList(),
        recommendations = recommendationsJson.decodeTmdbList(),
        certification = certification,
        providersSummary = providersSummary,
        adult = adult,
    )

private val tmdbModelGson = com.google.gson.Gson()

private inline fun <reified T> String?.decodeTmdbList(): List<T> {
    val json = this?.takeIf { it.isNotBlank() } ?: return emptyList()
    return runCatching {
        val type = object : com.google.gson.reflect.TypeToken<List<T>>() {}.type
        tmdbModelGson.fromJson<List<T>>(json, type).orEmpty()
    }.getOrDefault(emptyList())
}
