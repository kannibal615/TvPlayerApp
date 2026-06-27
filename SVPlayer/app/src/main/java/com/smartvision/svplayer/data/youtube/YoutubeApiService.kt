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
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 50,
        @Query("safeSearch") safeSearch: String = "moderate",
        @Query("pageToken") pageToken: String? = null,
    ): YoutubeSearchResponse

    @GET("videos")
    suspend fun mostPopularVideos(
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet",
        @Query("chart") chart: String = "mostPopular",
        @Query("maxResults") maxResults: Int = 50,
        @Query("regionCode") regionCode: String = "FR",
        @Query("pageToken") pageToken: String? = null,
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
)

data class YoutubeSnippetDto(
    @SerializedName("title") val title: String?,
    @SerializedName("channelTitle") val channelTitle: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("publishedAt") val publishedAt: String?,
    @SerializedName("thumbnails") val thumbnails: YoutubeThumbnailsDto?,
)

data class YoutubeThumbnailsDto(
    @SerializedName("medium") val medium: YoutubeThumbnailDto?,
    @SerializedName("high") val high: YoutubeThumbnailDto?,
    @SerializedName("default") val default: YoutubeThumbnailDto?,
)

data class YoutubeThumbnailDto(
    @SerializedName("url") val url: String?,
)
