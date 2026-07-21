package com.smartvision.svplayer.data.mock

enum class HomeCategoryType {
    Live,
    Movies,
    Series,
}

enum class HomeVisualStyle {
    Signal,
    Cinema,
    Series,
    Sport,
    Nature,
    People,
    Desert,
    City,
    Fire,
    Mystery,
}

enum class HomePreviewMode {
    None,
    TrendSegments,
    LiveImmediate,
    ResumeLoop,
    YoutubeTrailer,
}

data class HomeCategory(
    val id: String,
    val type: HomeCategoryType,
    val badge: String,
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val visualStyle: HomeVisualStyle,
)

data class ContinueItem(
    val id: String,
    val title: String,
    val meta: String,
    val remaining: String,
    val progress: Float,
    val visualStyle: HomeVisualStyle,
    val imageUrl: String? = null,
    val previewImageUrl: String? = null,
    val mediaType: String = "",
    val previewUrl: String? = null,
    val previewMode: HomePreviewMode = HomePreviewMode.None,
    val previewStartPositionMs: Long = 0L,
    val previewFallbackStartPositionMs: Long = 0L,
    val previewDurationLabel: String? = null,
    val ratingLabel: String? = null,
    val secondaryLabel: String? = null,
    val previewDurationMs: Long? = null,
    val previewPrepared: Boolean = false,
    val previewBackdropAvailable: Boolean = false,
    val previewYoutubeKey: String? = null,
)
