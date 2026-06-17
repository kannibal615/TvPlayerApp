package com.smartvision.svplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "live_streams")
data class LiveStreamEntity(
    @PrimaryKey val streamId: Int,
    val number: Int,
    val name: String,
    val categoryId: String?,
    val logoUrl: String?,
    val epgChannelId: String?,
)

@Entity(tableName = "movies")
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

@Entity(tableName = "series")
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

@Entity(tableName = "episodes")
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
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: String,
    val lastSync: Long?,
    val status: String,
    val message: String?,
)
