package com.smartvision.svplayer.data.youtube

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.data.local.dao.YoutubeDao
import com.smartvision.svplayer.data.local.entity.YoutubeBehaviorEventEntity
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.http.Body
import retrofit2.http.POST

interface YoutubeBehaviorApiService {
    @POST("api/app/behavior-events")
    suspend fun storeEvent(@Body request: YoutubeBehaviorEventRequest): YoutubeBehaviorEventResponse
}

data class YoutubeBehaviorEventRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("appVersion") val appVersion: String,
    @SerializedName("platform") val platform: String = "ANDROID_TV",
    @SerializedName("eventType") val eventType: String,
    @SerializedName("contentType") val contentType: String = "YOUTUBE",
    @SerializedName("contentIdHash") val contentIdHash: String?,
    @SerializedName("contentTitleHash") val contentTitleHash: String?,
    @SerializedName("contentTitle") val contentTitle: String?,
    @SerializedName("videoIdHash") val videoIdHash: String?,
    @SerializedName("channelId") val channelId: String?,
    @SerializedName("categoryId") val categoryId: String?,
    @SerializedName("categoryLabel") val categoryLabel: String?,
    @SerializedName("durationSeconds") val durationSeconds: Long?,
    @SerializedName("engagementScore") val engagementScore: Int?,
    @SerializedName("sourceScreen") val sourceScreen: String?,
    @SerializedName("context") val context: String?,
    @SerializedName("tags") val tags: String?,
)

data class YoutubeBehaviorEventResponse(
    @SerializedName("success") val success: Boolean = false,
)

class YoutubeBehaviorReporter(
    private val activationRepository: ActivationRepository,
    private val api: YoutubeBehaviorApiService,
    private val dao: YoutubeDao,
) {
    suspend fun report(eventType: String, video: YoutubeVideo?, sourceScreen: String? = null) = withContext(Dispatchers.IO) {
        val cleanEventType = eventType.cleanEventType()
        if (cleanEventType in IgnoredEventTypes) return@withContext
        val categoryLabel = video?.inferBehaviorCategory()
        val source = sourceScreen?.cleanEventType()?.takeIf { it.isNotBlank() } ?: "YOUTUBE"
        dao.insertBehaviorEvent(
            YoutubeBehaviorEventEntity(
                eventType = cleanEventType,
                videoIdHash = video?.videoId?.sha256(),
                channelId = video?.channelId?.take(80),
                categoryId = video?.categoryId?.take(40),
                contentTitle = video?.title?.cleanText(180),
                categoryLabel = categoryLabel,
                sourceScreen = source,
                durationSeconds = video?.durationSeconds?.coerceIn(0, 86_400),
                engagementScore = cleanEventType.youtubeEngagementScore(),
                context = video?.behaviorContext(source),
                tags = video?.tags?.toBehaviorTags(),
                createdAt = System.currentTimeMillis(),
            ),
        )
        flushPending()
    }

    suspend fun flushPending() = withContext(Dispatchers.IO) {
        val activation = activationRepository.localState.first()
        if (activation.deviceId.isBlank()) return@withContext
        val pending = dao.getPendingBehaviorEvents()
        if (pending.isEmpty()) return@withContext
        val synced = mutableListOf<Long>()
        for (event in pending) {
            val sent = runCatching {
                api.storeEvent(
                    YoutubeBehaviorEventRequest(
                        deviceId = activation.deviceId,
                        appVersion = BuildConfig.VERSION_NAME,
                        eventType = event.eventType,
                        contentIdHash = event.videoIdHash,
                        contentTitleHash = event.contentTitle?.normalizeForHash()?.takeIf { it.isNotBlank() }?.sha256(),
                        contentTitle = event.contentTitle,
                        videoIdHash = event.videoIdHash,
                        channelId = event.channelId,
                        categoryId = event.categoryId,
                        categoryLabel = event.categoryLabel,
                        durationSeconds = event.durationSeconds,
                        engagementScore = event.engagementScore,
                        sourceScreen = event.sourceScreen,
                        context = event.context,
                        tags = event.tags,
                    ),
                ).success
            }.onFailure {
                Log.w(TAG, "Evenement comportemental YouTube non envoye")
            }.getOrDefault(false)
            if (sent) synced += event.id
        }
        if (synced.isNotEmpty()) {
            dao.markBehaviorEventsSynced(synced, System.currentTimeMillis())
        }
    }

    private fun String.cleanEventType(): String =
        uppercase().filter { it.isLetterOrDigit() || it == '_' }.take(40).ifBlank { "UNKNOWN" }

    private fun String.cleanText(maxLength: Int): String? =
        replace(Regex("\\s+"), " ").trim().take(maxLength).ifBlank { null }

    private fun String.normalizeForHash(): String =
        lowercase().replace(Regex("\\s+"), " ").trim()

    private fun String.sha256(): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun List<String>.toBehaviorTags(): String? =
        asSequence()
            .map { it.trim().lowercase() }
            .filter { it.length in 2..32 }
            .map { it.filter { char -> char.isLetterOrDigit() || char == '-' || char == '_' } }
            .filter { it.isNotBlank() }
            .distinct()
            .take(12)
            .joinToString(",")
            .ifBlank { null }

    private fun YoutubeVideo.behaviorContext(sourceScreen: String): String? =
        listOfNotNull(
            "source=$sourceScreen",
            channelTitle.takeIf { it.isNotBlank() }?.let { "channel=${it.cleanText(60)}" },
            description?.cleanText(80)?.let { "description=$it" },
        ).joinToString(";").ifBlank { null }

    private fun YoutubeVideo.inferBehaviorCategory(): String {
        val haystack = listOf(title, description.orEmpty(), channelTitle, tags.joinToString(" "))
            .joinToString(" ")
            .lowercase()
        val rules = listOf(
            "music" to listOf("music", "musique", "clip", "song", "album", "concert", "dj", "lyrics"),
            "news" to listOf("news", "info", "actualite", "journal", "breaking", "politique", "politics"),
            "sports" to listOf("sport", "football", "soccer", "fifa", "nba", "tennis", "match", "goal"),
            "tutorial" to listOf("tutorial", "tuto", "how to", "guide", "course", "formation", "learn"),
            "kids" to listOf("kids", "enfant", "children", "cartoon", "dessin anime", "nursery"),
            "cinema" to listOf("movie", "film", "cinema", "trailer", "teaser", "episode"),
            "documentaries" to listOf("documentary", "documentaire", "history", "nature", "science"),
            "gaming" to listOf("gaming", "gameplay", "playstation", "xbox", "minecraft", "fortnite"),
            "technology" to listOf("tech", "android", "software", "ai", "gadget", "review"),
        )
        return rules.firstOrNull { (_, needles) -> needles.any { haystack.contains(it) } }?.first
            ?: when (categoryId) {
                "10" -> "music"
                "17" -> "sports"
                "20" -> "gaming"
                "24" -> "entertainment"
                "25" -> "news"
                "26", "27", "28" -> "tutorial"
                else -> "youtube"
            }
    }

    private fun String.youtubeEngagementScore(): Int =
        when (this) {
            "VIDEO_OPENED" -> 45
            "SUGGESTION_OPENED" -> 55
            "VIDEO_COMPLETED" -> 85
            else -> 40
        }

    private companion object {
        const val TAG = "SmartVisionYoutubeBehavior"
        val IgnoredEventTypes = setOf("PLAYER_READY")
    }
}
