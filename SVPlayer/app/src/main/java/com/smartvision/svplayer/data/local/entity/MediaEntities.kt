package com.smartvision.svplayer.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "live_streams",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["categoryId", "number", "name"]),
        Index(value = ["source"]),
    ],
)
data class LiveStreamEntity(
    @PrimaryKey val streamId: Int,
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
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["categoryId", "number", "title"]),
    ],
)
data class MovieEntity(
    @PrimaryKey val streamId: Int,
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
)

@Entity(
    tableName = "series",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["categoryId", "number", "title"]),
    ],
)
data class SeriesEntity(
    @PrimaryKey val seriesId: Int,
    val number: Int,
    val title: String,
    val categoryId: String?,
    val posterUrl: String?,
    val year: String?,
    val genre: String?,
    val rating: String?,
    val seasonsCount: Int?,
    val plot: String?,
)

@Entity(
    tableName = "trending_media",
    primaryKeys = ["contentType", "contentId"],
    indices = [
        Index(value = ["contentType", "rating"]),
        Index(value = ["contentType", "updatedAt"]),
    ],
)
data class TrendingMediaEntity(
    val contentType: String,
    val contentId: Int,
    val sampleContentId: Int?,
    val sampleExtension: String?,
    val rating: Float,
    val updatedAt: Long,
)

@Entity(
    tableName = "home_trending_preview_cache",
    primaryKeys = ["contentType", "contentId"],
    indices = [
        Index(value = ["contentType", "preparedAt"]),
        Index(value = ["lastSync"]),
    ],
)
data class HomeTrendingPreviewCacheEntity(
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
    primaryKeys = ["contentType", "contentId"],
    indices = [
        Index(value = ["tmdbId"]),
        Index(value = ["contentType", "confidence"]),
        Index(value = ["updatedAt"]),
    ],
)
data class TmdbContentMappingEntity(
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
    indices = [
        Index(value = ["seriesId"]),
    ],
)
data class EpisodeEntity(
    @PrimaryKey val episodeId: Int,
    val seriesId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val containerExtension: String,
    val duration: String?,
    val plot: String?,
)

@Entity(tableName = "favorites", primaryKeys = ["contentType", "contentId"])
data class FavoriteEntity(
    val contentType: String,
    val contentId: String,
    val createdAt: Long,
)

@Entity(tableName = "playback_progress", primaryKeys = ["contentType", "contentId"])
data class PlaybackProgressEntity(
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

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: String,
    val lastSync: Long?,
    val status: String,
    val message: String?,
)
