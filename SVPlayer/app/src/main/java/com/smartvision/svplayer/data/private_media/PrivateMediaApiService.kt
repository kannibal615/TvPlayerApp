package com.smartvision.svplayer.data.private_media

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface PrivateMediaApiService {
    @GET("api/media/private/libraries.php")
    suspend fun libraries(): PrivateMediaLibrariesResponse

    @GET("api/media/private/categories.php")
    suspend fun categories(): PrivateMediaCategoriesResponse

    @GET("api/media/private/items.php")
    suspend fun items(
        @Query("category_id") categoryId: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): PrivateMediaItemsResponse

    @GET("api/media/private/item.php")
    suspend fun item(@Query("id") id: String): PrivateMediaItemResponse

    @GET("api/media/private/playback.php")
    suspend fun playback(@Query("id") id: String): PrivateMediaPlaybackResponse
}

data class PrivateMediaLibrariesResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("libraries") val libraries: List<PrivateMediaLibraryDto> = emptyList(),
    @SerializedName("error") val error: String? = null,
)

data class PrivateMediaCategoriesResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("categories") val categories: List<PrivateMediaCategoryDto> = emptyList(),
    @SerializedName("error") val error: String? = null,
)

data class PrivateMediaItemsResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("page") val page: PrivateMediaPageDto? = null,
    @SerializedName("error") val error: String? = null,
)

data class PrivateMediaItemResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("item") val item: PrivateMediaDetailsDto? = null,
    @SerializedName("error") val error: String? = null,
)

data class PrivateMediaPlaybackResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("playbackType") val playbackType: String = "UNAVAILABLE",
    @SerializedName("streams") val streams: List<PrivateMediaStreamDto> = emptyList(),
    @SerializedName("error") val error: String? = null,
)

data class PrivateMediaLibraryDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("provider") val provider: String = "",
    @SerializedName("isEnabled") val isEnabled: Boolean = false,
)

data class PrivateMediaCategoryDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("parentLibraryId") val parentLibraryId: String = "",
    @SerializedName("query") val query: String = "",
    @SerializedName("order") val order: String = "",
    @SerializedName("provider") val provider: String = "",
)

data class PrivateMediaPageDto(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("perPage") val perPage: Int = 24,
    @SerializedName("totalCount") val totalCount: Int = 0,
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("items") val items: List<PrivateMediaItemDto> = emptyList(),
    @SerializedName("error") val error: String? = null,
)

data class PrivateMediaItemDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("provider") val provider: String = "",
    @SerializedName("providerVideoId") val providerVideoId: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("keywords") val keywords: List<String> = emptyList(),
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("thumbnailUrl") val thumbnailUrl: String? = null,
    @SerializedName("thumbnails") val thumbnails: List<PrivateMediaThumbnailDto> = emptyList(),
    @SerializedName("durationSeconds") val durationSeconds: Long? = null,
    @SerializedName("durationLabel") val durationLabel: String? = null,
    @SerializedName("views") val views: Long? = null,
    @SerializedName("rating") val rating: Float? = null,
    @SerializedName("addedAt") val addedAt: String? = null,
    @SerializedName("sourceUrl") val sourceUrl: String? = null,
    @SerializedName("embedUrl") val embedUrl: String? = null,
    @SerializedName("isPlayable") val isPlayable: Boolean = false,
    @SerializedName("playbackType") val playbackType: String = "UNAVAILABLE",
)

data class PrivateMediaDetailsDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("provider") val provider: String = "",
    @SerializedName("providerVideoId") val providerVideoId: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("keywords") val keywords: List<String> = emptyList(),
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("thumbnailUrl") val thumbnailUrl: String? = null,
    @SerializedName("thumbnails") val thumbnails: List<PrivateMediaThumbnailDto> = emptyList(),
    @SerializedName("durationSeconds") val durationSeconds: Long? = null,
    @SerializedName("durationLabel") val durationLabel: String? = null,
    @SerializedName("views") val views: Long? = null,
    @SerializedName("rating") val rating: Float? = null,
    @SerializedName("addedAt") val addedAt: String? = null,
    @SerializedName("sourceUrl") val sourceUrl: String? = null,
    @SerializedName("embedUrl") val embedUrl: String? = null,
    @SerializedName("streams") val streams: List<PrivateMediaStreamDto> = emptyList(),
    @SerializedName("isPlayable") val isPlayable: Boolean = false,
    @SerializedName("playbackType") val playbackType: String = "UNAVAILABLE",
)

data class PrivateMediaThumbnailDto(
    @SerializedName("url") val url: String = "",
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("size") val size: String? = null,
)

data class PrivateMediaStreamDto(
    @SerializedName("url") val url: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("mimeType") val mimeType: String? = null,
    @SerializedName("headers") val headers: Map<String, String> = emptyMap(),
    @SerializedName("expiresAt") val expiresAt: String? = null,
)
