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
    @SerializedName("videoIdHash") val videoIdHash: String?,
    @SerializedName("channelId") val channelId: String?,
    @SerializedName("categoryId") val categoryId: String?,
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
    suspend fun report(eventType: String, video: YoutubeVideo?) = withContext(Dispatchers.IO) {
        dao.insertBehaviorEvent(
            YoutubeBehaviorEventEntity(
                eventType = eventType.cleanEventType(),
                videoIdHash = video?.videoId?.sha256(),
                channelId = video?.channelId?.take(80),
                categoryId = video?.categoryId?.take(40),
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
                        videoIdHash = event.videoIdHash,
                        channelId = event.channelId,
                        categoryId = event.categoryId,
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

    private companion object {
        const val TAG = "SmartVisionYoutubeBehavior"
    }
}
