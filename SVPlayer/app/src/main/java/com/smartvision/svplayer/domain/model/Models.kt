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
    val epgChannelId: String? = null,
    val directStreamUrl: String? = null,
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

data class TrendingCatalogItem(
    val contentType: String,
    val contentId: Int,
    val title: String,
    val categoryName: String,
    val posterUrl: String?,
    val rating: String?,
    val year: String?,
    val previewUrl: String?,
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

    enum class SyncSectionPhase {
        WAITING,
        RUNNING,
        IMPORTING,
        LOADING_TRENDS,
        COMPLETED,
        ERROR,
    }

    data class SyncSectionProgress(
        val currentItems: Int = 0,
        val previousItems: Int = 0,
        val completed: Boolean = false,
        val phase: SyncSectionPhase = SyncSectionPhase.WAITING,
        val progressPercent: Int? = null,
    ) {
        val percent: Int
            get() = when {
                progressPercent != null -> progressPercent.coerceIn(0, 100)
                completed -> 100
                previousItems > 0 -> ((currentItems.toFloat() / previousItems.toFloat()) * 100)
                    .toInt()
                    .coerceIn(0, 99)
                currentItems > 0 -> 70
                else -> 0
            }

        val fraction: Float
            get() = percent.toFloat() / 100f
    }

    data class CatalogProgress(
        val live: SyncSectionProgress = SyncSectionProgress(),
        val movies: SyncSectionProgress = SyncSectionProgress(),
        val series: SyncSectionProgress = SyncSectionProgress(),
    )

    data class Running(
        val message: String = "Telechargement de la playlist...",
        val completedItems: Int = 0,
        val totalItems: Int = 0,
        val catalogProgress: CatalogProgress = CatalogProgress(),
    ) : SyncStatus {
        val percent: Int =
            if (totalItems > 0) ((completedItems.toFloat() / totalItems.toFloat()) * 100).toInt().coerceIn(0, 100) else 0
    }
    data class Success(
        val message: String,
        val catalogProgress: CatalogProgress = CatalogProgress(),
    ) : SyncStatus

    data class Error(
        val message: String,
        val catalogProgress: CatalogProgress = CatalogProgress(),
    ) : SyncStatus

    val buttonLabel: String
        get() = when (this) {
            Idle -> "Synchroniser"
            is Running -> "Synchronisation..."
            is Success -> "Synchroniser"
            is Error -> "Synchroniser"
        }
}

data class PlayerSettings(
    val displaySize: String = "Normal",
    val language: String = "English",
    val syncFrequency: String = "24h",
    val autostartEnabled: Boolean = true,
    val backgroundSyncEnabled: Boolean = true,
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
