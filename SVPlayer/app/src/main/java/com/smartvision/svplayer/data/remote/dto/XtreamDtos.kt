package com.smartvision.svplayer.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class XtreamAccountDto(
    @SerializedName("user_info") val userInfo: UserInfoDto?,
    @SerializedName("server_info") val serverInfo: ServerInfoDto?,
)

data class UserInfoDto(
    @SerializedName("username") val username: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("exp_date") val expirationDate: String?,
    @SerializedName("active_cons") val activeConnections: String?,
    @SerializedName("max_connections") val maxConnections: String?,
)

data class ServerInfoDto(
    @SerializedName("url") val url: String?,
    @SerializedName("port") val port: String?,
    @SerializedName("server_protocol") val protocol: String?,
)

data class XtreamCategoryDto(
    @SerializedName("category_id") val id: String?,
    @SerializedName("category_name") val name: String?,
    @SerializedName("parent_id") val parentId: Int?,
)

data class XtreamLiveStreamDto(
    @SerializedName("num") val number: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("stream_type") val streamType: String?,
    @SerializedName("stream_id") val streamId: Int?,
    @SerializedName("stream_icon") val icon: String?,
    @SerializedName("epg_channel_id") val epgChannelId: String?,
    @SerializedName("added") val added: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("custom_sid") val customSid: String?,
    @SerializedName("tv_archive") val tvArchive: Int?,
    @SerializedName("direct_source") val directSource: String?,
)

data class XtreamMovieDto(
    @SerializedName("num") val number: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("stream_id") val streamId: Int?,
    @SerializedName("stream_icon") val icon: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("container_extension") val containerExtension: String?,
    @SerializedName("rating") val rating: String?,
    @SerializedName("rating_5based") val ratingFiveBased: String?,
    @SerializedName("added") val added: String?,
)

data class XtreamMovieInfoDto(
    @SerializedName("info") val info: JsonElement? = null,
    @SerializedName("movie_data") val movieData: JsonElement? = null,
)

data class XtreamSeriesDto(
    @SerializedName("num") val number: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("series_id") val seriesId: Int?,
    @SerializedName("cover") val cover: String?,
    @SerializedName("plot") val plot: String?,
    @SerializedName("genre") val genre: String?,
    @SerializedName("releaseDate") val releaseDate: String?,
    @SerializedName("rating") val rating: String?,
    @SerializedName("episode_run_time") val episodeRunTime: String?,
    @SerializedName("category_id") val categoryId: String?,
)

data class XtreamSeriesInfoDto(
    @SerializedName("episodes") val episodes: Map<String, List<XtreamEpisodeDto>>? = null,
    @SerializedName("info") val info: JsonElement? = null,
)

data class XtreamEpisodeDto(
    @SerializedName("id") val id: String?,
    @SerializedName("episode_num") val episodeNumber: Int?,
    @SerializedName("title") val title: String?,
    @SerializedName("container_extension") val containerExtension: String?,
    @SerializedName("info") val info: XtreamEpisodeInfoDto?,
)

data class XtreamEpisodeInfoDto(
    @SerializedName("duration") val duration: String?,
    @SerializedName("plot") val plot: String?,
)
