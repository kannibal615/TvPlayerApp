package com.smartvision.svplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "youtube_searches")
data class YoutubeSearchEntity(
    @PrimaryKey val query: String,
    val updatedAt: Long,
)

@Entity(tableName = "youtube_video_history")
data class YoutubeVideoHistoryEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String?,
    val publishedAt: String?,
    val channelId: String?,
    val viewCount: Long?,
    val durationIso: String?,
    val durationSeconds: Long?,
    val categoryId: String?,
    val tags: String?,
    val updatedAt: Long,
)

@Entity(tableName = "youtube_selection")
data class YoutubeSelectionEntity(
    @PrimaryKey val id: String,
    val videoId: String?,
    val updatedAt: Long,
)

@Entity(tableName = "youtube_behavior_events")
data class YoutubeBehaviorEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val videoIdHash: String?,
    val channelId: String?,
    val categoryId: String?,
    val tags: String?,
    val createdAt: Long,
    val syncedAt: Long? = null,
)
