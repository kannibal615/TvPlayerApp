package com.smartvision.svplayer.data.home

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface HomeSlidesApiService {
    @GET("api/home_slides.php")
    suspend fun getHomeSlides(
        @Query("refresh") refreshToken: Long = System.currentTimeMillis(),
    ): HomeSlidesResponse
}

data class HomeSlidesResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("slides") val slides: List<HomeSlide> = emptyList(),
)

data class HomeSlide(
    @SerializedName("title") val title: String = "",
    @SerializedName("subtitle") val subtitle: String = "",
    @SerializedName("button_label") val buttonLabel: String = "En savoir plus",
    @SerializedName("button_route") val buttonRoute: String = "home",
    @SerializedName("image_url") val imageUrl: String = "",
)

class HomeSlidesRepository(
    private val api: HomeSlidesApiService,
) {
    @Volatile
    private var cachedSlides: List<HomeSlide>? = null

    fun getCachedSlides(): List<HomeSlide>? = cachedSlides

    suspend fun refresh(): List<HomeSlide> {
        val response = api.getHomeSlides()
        check(response.success) { "Slides indisponibles." }
        return response.slides
            .filter { it.title.isNotBlank() }
            .also { cachedSlides = it }
    }
}
