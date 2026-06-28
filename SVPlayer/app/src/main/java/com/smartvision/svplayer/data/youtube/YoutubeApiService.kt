package com.smartvision.svplayer.data.youtube

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface YoutubeApiService {
    @GET("search")
    suspend fun searchVideos(
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("q") query: String? = null,
        @Query("channelId") channelId: String? = null,
        @Query("order") order: String = "relevance",
        @Query("maxResults") maxResults: Int = 50,
        @Query("safeSearch") safeSearch: String = "moderate",
        @Query("videoEmbeddable") videoEmbeddable: String = "true",
        @Query("pageToken") pageToken: String? = null,
    ): YoutubeSearchResponse

    @GET("videos")
    suspend fun mostPopularVideos(
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet,contentDetails,statistics,status",
        @Query("chart") chart: String = "mostPopular",
        @Query("maxResults") maxResults: Int = 50,
        @Query("regionCode") regionCode: String = "FR",
        @Query("pageToken") pageToken: String? = null,
    ): YoutubeVideosResponse

    @GET("videos")
    suspend fun videosByIds(
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet,contentDetails,statistics,status",
        @Query("id") ids: String,
        @Query("maxResults") maxResults: Int = 50,
    ): YoutubeVideosResponse
}

data class YoutubeSearchResponse(
    @SerializedName("nextPageToken") val nextPageToken: String? = null,
    @SerializedName("items") val items: List<YoutubeSearchItemDto> = emptyList(),
)

data class YoutubeVideosResponse(
    @SerializedName("nextPageToken") val nextPageToken: String? = null,
    @SerializedName("items") val items: List<YoutubeVideoDto> = emptyList(),
)

data class YoutubeSearchItemDto(
    @SerializedName("id") val id: YoutubeSearchIdDto?,
    @SerializedName("snippet") val snippet: YoutubeSnippetDto?,
)

data class YoutubeSearchIdDto(
    @SerializedName("videoId") val videoId: String?,
)

data class YoutubeVideoDto(
    @SerializedName("id") val id: String?,
    @SerializedName("snippet") val snippet: YoutubeSnippetDto?,
    @SerializedName("contentDetails") val contentDetails: YoutubeContentDetailsDto? = null,
    @SerializedName("statistics") val statistics: YoutubeStatisticsDto? = null,
    @SerializedName("status") val status: YoutubeStatusDto? = null,
)

data class YoutubeStatusDto(
    @SerializedName("embeddable") val embeddable: Boolean? = null,
)

data class YoutubeSnippetDto(
    @SerializedName("title") val title: String?,
    @SerializedName("channelId") val channelId: String?,
    @SerializedName("channelTitle") val channelTitle: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("publishedAt") val publishedAt: String?,
    @SerializedName("categoryId") val categoryId: String?,
    @SerializedName("tags") val tags: List<String>?,
    @SerializedName("thumbnails") val thumbnails: YoutubeThumbnailsDto?,
)

data class YoutubeContentDetailsDto(
    @SerializedName("duration") val duration: String?,
)

data class YoutubeStatisticsDto(
    @SerializedName("viewCount") val viewCount: String?,
)

data class YoutubeThumbnailsDto(
    @SerializedName("medium") val medium: YoutubeThumbnailDto?,
    @SerializedName("high") val high: YoutubeThumbnailDto?,
    @SerializedName("default") val default: YoutubeThumbnailDto?,
)

data class YoutubeThumbnailDto(
    @SerializedName("url") val url: String?,
)
