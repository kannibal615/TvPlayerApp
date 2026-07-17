package com.smartvision.svplayer.data.home

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.annotations.SerializedName
import com.smartvision.svplayer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
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
    appContext: Context,
    private val api: HomeSlidesApiService,
    private val okHttpClient: OkHttpClient,
) {
    private val context = appContext.applicationContext
    private val preferences = context.getSharedPreferences("home_hero_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val cacheDirectory = File(context.filesDir, "home_hero").apply { mkdirs() }

    @Volatile
    private var cachedSlides: List<HomeSlide>? = loadPersistedSlides()

    fun getCachedSlides(): List<HomeSlide>? = cachedSlides

    suspend fun refresh(): List<HomeSlide> {
        val currentCache = cachedSlides
        val lastRefreshAt = preferences.getLong(KEY_LAST_REFRESH_AT, 0L)
        if (!currentCache.isNullOrEmpty() && System.currentTimeMillis() - lastRefreshAt < CACHE_MAX_AGE_MS) {
            return currentCache
        }
        return try {
            val response = api.getHomeSlides()
            check(response.success) { "Slides indisponibles." }
            val remoteSlides = response.slides
                .filter { it.title.isNotBlank() }
            val localSlides = cacheSlideImages(remoteSlides, currentCache.orEmpty())
            localSlides.also {
                    cachedSlides = it
                    persistSlides(it)
                    preferences.edit { putLong(KEY_LAST_REFRESH_AT, System.currentTimeMillis()) }
                }
        } catch (error: Throwable) {
            throw error
        }
    }

    private suspend fun cacheSlideImages(
        remoteSlides: List<HomeSlide>,
        previousSlides: List<HomeSlide>,
    ): List<HomeSlide> = withContext(Dispatchers.IO) {
        remoteSlides.mapIndexed { index, slide ->
            if (slide.imageUrl.isBlank()) return@mapIndexed slide
            val target = File(cacheDirectory, "hero_${index}_${System.currentTimeMillis()}.img")
            val temporary = File(cacheDirectory, "hero_$index.tmp")
            val absoluteUrl = slide.imageUrl.toAbsoluteUrl()
            val downloaded = runCatching {
                okHttpClient.newCall(Request.Builder().url(absoluteUrl).build()).execute().use { response ->
                    check(response.isSuccessful)
                    val body = response.body ?: error("Empty hero image")
                    temporary.outputStream().use { output -> body.byteStream().use { it.copyTo(output) } }
                }
                check(BitmapFactory.decodeFile(temporary.absolutePath) != null) { "Invalid hero image" }
                check(temporary.renameTo(target)) { "Unable to publish hero image" }
                cacheDirectory.listFiles()
                    ?.filter { it.name.startsWith("hero_${index}_") && it != target }
                    ?.forEach(File::delete)
                true
            }.getOrElse {
                temporary.delete()
                false
            }
            when {
                downloaded || target.exists() -> slide.copy(imageUrl = target.toURI().toString())
                else -> previousSlides.getOrNull(index)?.takeIf { it.imageUrl.localFileExists() }
                    ?: slide
            }
        }
    }

    private fun loadPersistedSlides(): List<HomeSlide>? = runCatching {
        val json = preferences.getString(KEY_SLIDES, null) ?: return@runCatching null
        gson.fromJson<List<HomeSlide>>(json, object : TypeToken<List<HomeSlide>>() {}.type)
            ?.takeIf { slides -> slides.isNotEmpty() }
    }.getOrNull()

    private fun persistSlides(slides: List<HomeSlide>) {
        preferences.edit { putString(KEY_SLIDES, gson.toJson(slides)) }
    }

    private fun String.toAbsoluteUrl(): String = when {
        startsWith("http://") || startsWith("https://") -> this
        else -> BuildConfig.ACTIVATION_BASE_URL.trimEnd('/') + "/" + trimStart('/')
    }

    private fun String.localFileExists(): Boolean = runCatching {
        startsWith("file:") && File(java.net.URI(this)).exists()
    }.getOrDefault(false)

    private companion object {
        const val KEY_LAST_REFRESH_AT = "last_refresh_at"
        const val KEY_SLIDES = "slides"
        const val CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1_000L
    }
}
