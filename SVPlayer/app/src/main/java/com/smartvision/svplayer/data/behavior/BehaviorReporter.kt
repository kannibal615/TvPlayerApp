package com.smartvision.svplayer.data.behavior

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.data.activation.ActivationRepository
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.http.Body
import retrofit2.http.POST

interface BehaviorApiService {
    @POST("api/app/behavior-events")
    suspend fun storeEvents(@Body request: BehaviorEventBatchRequest): BehaviorEventResponse
}

data class BehaviorEventBatchRequest(
    @SerializedName("events") val events: List<BehaviorEventRequest>,
)

data class BehaviorEventRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("appVersion") val appVersion: String,
    @SerializedName("platform") val platform: String = "ANDROID_TV",
    @SerializedName("eventType") val eventType: String,
    @SerializedName("contentType") val contentType: String,
    @SerializedName("contentIdHash") val contentIdHash: String?,
    @SerializedName("contentTitleHash") val contentTitleHash: String?,
    @SerializedName("contentTitle") val contentTitle: String?,
    @SerializedName("categoryId") val categoryId: String?,
    @SerializedName("categoryLabel") val categoryLabel: String?,
    @SerializedName("language") val language: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("durationSeconds") val durationSeconds: Long?,
    @SerializedName("positionSeconds") val positionSeconds: Long?,
    @SerializedName("engagementScore") val engagementScore: Int?,
    @SerializedName("sourceScreen") val sourceScreen: String?,
    @SerializedName("tags") val tags: String?,
    @SerializedName("context") val context: String?,
)

data class BehaviorEventResponse(
    @SerializedName("success") val success: Boolean = false,
)

data class BehaviorContent(
    val contentType: String,
    val contentId: String?,
    val title: String?,
    val categoryId: String? = null,
    val categoryLabel: String? = null,
    val language: String? = null,
    val country: String? = null,
    val durationSeconds: Long? = null,
    val positionSeconds: Long? = null,
    val engagementScore: Int? = null,
    val sourceScreen: String? = null,
    val tags: List<String> = emptyList(),
    val context: Map<String, String> = emptyMap(),
)

class BehaviorReporter(
    private val activationRepository: ActivationRepository,
    private val api: BehaviorApiService,
) {
    suspend fun report(eventType: String, content: BehaviorContent) = withContext(Dispatchers.IO) {
        val activation = activationRepository.localState.first()
        if (activation.deviceId.isBlank()) return@withContext
        val request = BehaviorEventRequest(
            deviceId = activation.deviceId,
            appVersion = BuildConfig.VERSION_NAME,
            eventType = eventType.cleanToken(40),
            contentType = content.contentType.cleanToken(30).ifBlank { "UNKNOWN" },
            contentIdHash = content.contentId?.sha256(),
            contentTitleHash = content.title?.normalizeForHash()?.takeIf { it.isNotBlank() }?.sha256(),
            contentTitle = content.title?.cleanText(180),
            categoryId = content.categoryId?.cleanId(80),
            categoryLabel = content.categoryLabel?.cleanText(120),
            language = content.language?.cleanToken(16),
            country = content.country?.cleanToken(16),
            durationSeconds = content.durationSeconds?.coerceIn(0, 86_400),
            positionSeconds = content.positionSeconds?.coerceIn(0, 86_400),
            engagementScore = content.engagementScore?.coerceIn(0, 100),
            sourceScreen = content.sourceScreen?.cleanToken(40),
            tags = content.tags.toBehaviorTags(),
            context = content.context.toBehaviorContext(),
        )
        runCatching {
            api.storeEvents(BehaviorEventBatchRequest(listOf(request))).success
        }.onFailure {
            Log.w(TAG, "Evenement comportemental non envoye")
        }
    }

    fun reportAsync(scope: kotlinx.coroutines.CoroutineScope, eventType: String, content: BehaviorContent) {
        scope.launchCatching { report(eventType, content) }
    }

    private fun kotlinx.coroutines.CoroutineScope.launchCatching(block: suspend () -> Unit) {
        launch {
            runCatching { block() }.onFailure {
                Log.w(TAG, "Tracking comportemental ignore")
            }
        }
    }

    private fun String.cleanToken(maxLength: Int): String =
        uppercase().filter { it.isLetterOrDigit() || it == '_' }.take(maxLength)

    private fun String.cleanId(maxLength: Int): String? =
        filter { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' || it == ':' }
            .take(maxLength)
            .ifBlank { null }

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
            .map { tag -> tag.filter { it.isLetterOrDigit() || it == '-' || it == '_' } }
            .filter { it.isNotBlank() }
            .distinct()
            .take(16)
            .joinToString(",")
            .ifBlank { null }

    private fun Map<String, String>.toBehaviorContext(): String? =
        entries.asSequence()
            .mapNotNull { (key, value) ->
                val cleanKey = key.lowercase().filter { it.isLetterOrDigit() || it == '_' }.take(32)
                val cleanValue = value.replace(Regex("\\s+"), " ").trim().take(80)
                if (cleanKey.isBlank() || cleanValue.isBlank()) null else "$cleanKey=$cleanValue"
            }
            .take(12)
            .joinToString(";")
            .ifBlank { null }

    private companion object {
        const val TAG = "SmartVisionBehavior"
    }
}
