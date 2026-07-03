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
    val previewStartPositionMs: Long,
    val sampleLabel: String?,
    val backdropState: String,
    val previewState: String,
    val preparedAt: Long,
    val lastSync: Long,
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
