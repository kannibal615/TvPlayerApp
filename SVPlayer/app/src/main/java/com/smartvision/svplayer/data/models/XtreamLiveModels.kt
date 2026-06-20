package com.smartvision.svplayer.data.models

data class XtreamLiveCategory(
    val id: String,
    val name: String,
    val parentId: Int? = null,
    val count: Int? = null,
)

data class XtreamLiveStream(
    val number: Int,
    val name: String,
    val streamType: String?,
    val streamId: Int,
    val streamIcon: String?,
    val epgChannelId: String?,
    val added: String?,
    val categoryId: String?,
    val customSid: String?,
    val tvArchive: Int?,
    val directSource: String?,
)

data class XtreamMovieCategory(
    val id: String,
    val name: String,
    val parentId: Int? = null,
    val count: Int? = null,
)

data class XtreamMovieStream(
    val number: Int,
    val title: String,
    val streamId: Int,
    val posterUrl: String?,
    val categoryId: String?,
    val containerExtension: String,
    val rating: String?,
    val added: String?,
)

data class XtreamSeriesCategory(
    val id: String,
    val name: String,
    val parentId: Int? = null,
    val count: Int? = null,
)

data class XtreamSeriesStream(
    val number: Int,
    val title: String,
    val seriesId: Int,
    val coverUrl: String?,
    val plot: String?,
    val genre: String?,
    val releaseDate: String?,
    val rating: String?,
    val episodeRunTime: String?,
    val categoryId: String?,
)

data class XtreamSeriesEpisode(
    val episodeId: Int,
    val seriesId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val containerExtension: String,
    val duration: String?,
    val plot: String?,
)
