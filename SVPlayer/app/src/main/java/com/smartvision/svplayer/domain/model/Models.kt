package com.smartvision.svplayer.domain.model

enum class MediaSection(val storageName: String) {
    Live("live"),
    Movies("movies"),
    Series("series"),
}

enum class PlaybackKind(val routeName: String) {
    Live("live"),
    Movie("movie"),
    Episode("episode");

    companion object {
        fun fromRoute(value: String?): PlaybackKind =
            entries.firstOrNull { it.routeName == value } ?: Live
    }
}

data class Category(
    val id: String,
    val name: String,
    val type: MediaSection,
    val count: Int = 0,
)

data class LiveChannel(
    val streamId: Int,
    val number: Int,
    val name: String,
    val categoryId: String?,
    val categoryName: String,
    val logoUrl: String?,
    val currentProgram: String?,
    val timeRange: String?,
    val isFavorite: Boolean = false,
)

data class Movie(
    val streamId: Int,
    val number: Int,
    val title: String,
    val categoryId: String?,
    val categoryName: String,
    val posterUrl: String?,
    val year: String?,
    val genre: String?,
    val rating: String?,
    val duration: String?,
    val plot: String?,
    val containerExtension: String,
    val isFavorite: Boolean = false,
)

data class TvSeries(
    val seriesId: Int,
    val number: Int,
    val title: String,
    val categoryId: String?,
    val categoryName: String,
    val posterUrl: String?,
    val year: String?,
    val genre: String?,
    val rating: String?,
    val seasonsCount: Int?,
    val plot: String?,
    val isFavorite: Boolean = false,
)

data class Episode(
    val episodeId: Int,
    val seriesId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val containerExtension: String,
    val duration: String?,
    val plot: String?,
)

data class AccountProfile(
    val id: String,
    val name: String,
    val host: String,
    val usernameMasked: String,
    val status: String,
    val expirationDate: String?,
    val activeConnections: Int?,
    val maxConnections: Int?,
    val lastSync: String?,
    val liveCount: Int,
    val movieCount: Int,
    val seriesCount: Int,
)

data class PlaybackRequest(
    val kind: PlaybackKind,
    val contentId: String,
    val title: String,
    val subtitle: String,
    val url: String,
    val resumePositionMs: Long = 0L,
)

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Running : SyncStatus
    data class Success(val message: String) : SyncStatus
    data class Error(val message: String) : SyncStatus

    val buttonLabel: String
        get() = when (this) {
            Idle -> "Synchroniser"
            Running -> "Synchronisation..."
            is Success -> "Synchroniser"
            is Error -> "Synchroniser"
        }
}

data class PlayerSettings(
    val displaySize: String = "Normal",
    val language: String = "English",
    val syncFrequency: String = "A chaque demarrage",
    val focusStyle: String = "Default",
    val focusColor: String = "White",
    val focusEffect: String = "Frame",
    val focusBackground: String = "BlueTransparent",
    val animationsEnabled: Boolean = true,
    val videoRatio: String = "Fit",
    val bufferMode: String = "Standard",
    val retryEnabled: Boolean = true,
    val parentalControlEnabled: Boolean = false,
    val parentalPin: String = "",
    val parentalKeywords: String = "adults; porn; xxx",
)
