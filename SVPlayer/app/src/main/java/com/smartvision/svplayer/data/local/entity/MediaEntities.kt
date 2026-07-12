package com.smartvision.svplayer.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "live_streams",
    primaryKeys = ["profileId", "streamId"],
    indices = [
        Index(value = ["profileId", "categoryId"]),
        Index(value = ["profileId", "categoryId", "number", "name"]),
        Index(value = ["profileId", "source"]),
    ],
)
data class LiveStreamEntity(
    val profileId: String,
    val streamId: Int,
    val number: Int,
    val name: String,
    val categoryId: String?,
    val logoUrl: String?,
    val epgChannelId: String?,
    val directStreamUrl: String? = null,
    val source: String = "xtream",
)

@Entity(
    tableName = "movies",
    primaryKeys = ["profileId", "streamId"],
    indices = [
        Index(value = ["profileId", "categoryId"]),
        Index(value = ["profileId", "categoryId", "number", "title"]),
        Index(value = ["profileId", "addedAt"]),
    ],
)
data class MovieEntity(
    val profileId: String,
    val streamId: Int,
    val number: Int,
    val title: String,
    val categoryId: String?,
    val posterUrl: String?,
    val year: String?,
    val genre: String?,
    val rating: String?,
    val duration: String?,
    val plot: String?,
    val containerExtension: String,
    val addedAt: Long = 0L,
)

@Entity(
    tableName = "series",
    primaryKeys = ["profileId", "seriesId"],
    indices = [
        Index(value = ["profileId", "categoryId"]),
        Index(value = ["profileId", "categoryId", "number", "title"]),
        Index(value = ["profileId", "addedAt"]),
    ],
)
data class SeriesEntity(
    val profileId: String,
    val seriesId: Int,
    val number: Int,
    val title: String,
    val categoryId: String?,
    val posterUrl: String?,
    val year: String?,
    val genre: String?,
    val rating: String?,
    val seasonsCount: Int?,
    val plot: String?,
    val addedAt: Long = 0L,
)

@Entity(
    tableName = "trending_media",
    primaryKeys = ["profileId", "contentType", "contentId"],
    indices = [
        Index(value = ["profileId", "contentType", "rating"]),
        Index(value = ["profileId", "contentType", "updatedAt"]),
    ],
)
data class TrendingMediaEntity(
    val profileId: String,
    val contentType: String,
    val contentId: Int,
    val sampleContentId: Int?,
    val sampleExtension: String?,
    val rating: Float,
    val updatedAt: Long,
)

@Entity(
    tableName = "home_trending_preview_cache",
    primaryKeys = ["profileId", "contentType", "contentId"],
    indices = [
        Index(value = ["profileId", "contentType", "preparedAt"]),
        Index(value = ["profileId", "lastSync"]),
    ],
)
data class HomeTrendingPreviewCacheEntity(
    val profileId: String,
    val contentType: String,
    val contentId: Int,
    val posterUrl: String?,
    val backdropUrl: String?,
    val durationLabel: String?,
    val durationMs: Long?,
    val previewKind: String,
    val previewContentId: Int?,
    val previewExtension: String?,
    val trailerKey: String?,
    val previewStartPositionMs: Long,
    val sampleLabel: String?,
    val backdropState: String,
    val previewState: String,
    val preparedAt: Long,
    val lastSync: Long,
)

@Entity(
    tableName = "tmdb_content_mapping",
    primaryKeys = ["profileId", "contentType", "contentId"],
    indices = [
        Index(value = ["tmdbId"]),
        Index(value = ["profileId", "contentType", "confidence"]),
        Index(value = ["profileId", "updatedAt"]),
    ],
)
data class TmdbContentMappingEntity(
    val profileId: String,
    val contentType: String,
    val contentId: Int,
    val tmdbId: Int?,
    val mediaType: String,
    val matchedTitle: String?,
    val originalTitle: String?,
    val matchedYear: String?,
    val confidence: Int,
    val matchSource: String,
    val language: String,
    val adult: Boolean,
    val lastError: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "tmdb_movie_metadata",
    primaryKeys = ["tmdbId", "language"],
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["releaseDate"]),
    ],
)
data class TmdbMovieMetadataEntity(
    val tmdbId: Int,
    val language: String,
    val title: String,
    val originalTitle: String?,
    val overview: String?,
    val posterPath: String?,
    val posterUrl: String?,
    val backdropPath: String?,
    val backdropUrl: String?,
    val logoPath: String?,
    val logoUrl: String?,
    val releaseDate: String?,
    val runtimeMinutes: Int?,
    val genres: String?,
    val voteAverage: Double?,
    val voteCount: Int?,
    val popularity: Double?,
    val cast: String?,
    val castJson: String?,
    val director: String?,
    val directorJson: String?,
    val trailerKey: String?,
    val videosJson: String?,
    val recommendationsJson: String?,
    val collectionName: String?,
    val certification: String?,
    val providersSummary: String?,
    val homepage: String?,
    val status: String?,
    val adult: Boolean,
    val originCountry: String?,
    val originalLanguage: String?,
    val updatedAt: Long,
)

@Entity(
    tableName = "tmdb_series_metadata",
    primaryKeys = ["tmdbId", "language"],
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["firstAirDate"]),
    ],
)
data class TmdbSeriesMetadataEntity(
    val tmdbId: Int,
    val language: String,
    val name: String,
    val originalName: String?,
    val overview: String?,
    val posterPath: String?,
    val posterUrl: String?,
    val backdropPath: String?,
    val backdropUrl: String?,
    val logoPath: String?,
    val logoUrl: String?,
    val firstAirDate: String?,
    val episodeRunTimeMinutes: Int?,
    val genres: String?,
    val voteAverage: Double?,
    val voteCount: Int?,
    val popularity: Double?,
    val cast: String?,
    val castJson: String?,
    val createdBy: String?,
    val createdByJson: String?,
    val trailerKey: String?,
    val videosJson: String?,
    val recommendationsJson: String?,
    val certification: String?,
    val providersSummary: String?,
    val homepage: String?,
    val status: String?,
    val adult: Boolean,
    val originCountry: String?,
    val originalLanguage: String?,
    val updatedAt: Long,
)

@Entity(
    tableName = "episodes",
    primaryKeys = ["profileId", "episodeId"],
    indices = [
        Index(value = ["profileId", "seriesId"]),
    ],
)
data class EpisodeEntity(
    val profileId: String,
    val episodeId: Int,
    val seriesId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val containerExtension: String,
    val duration: String?,
    val plot: String?,
)

@Entity(tableName = "favorites", primaryKeys = ["profileId", "contentType", "contentId"])
data class FavoriteEntity(
    val profileId: String,
    val contentType: String,
    val contentId: String,
    val createdAt: Long,
)

@Entity(tableName = "playback_progress", primaryKeys = ["profileId", "contentType", "contentId"])
data class PlaybackProgressEntity(
    val profileId: String,
    val contentType: String,
    val contentId: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val title: String? = null,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val parentContentId: String? = null,
)

@Entity(tableName = "sync_state", primaryKeys = ["profileId", "id"])
data class SyncStateEntity(
    val profileId: String,
    val id: String,
    val lastSync: Long?,
    val status: String,
    val message: String?,
)
