package com.smartvision.svplayer.data.youtube

import com.smartvision.svplayer.data.local.dao.YoutubeDao
import com.smartvision.svplayer.data.local.entity.YoutubeSearchEntity
import com.smartvision.svplayer.data.local.entity.YoutubeSelectionEntity
import com.smartvision.svplayer.data.local.entity.YoutubeVideoHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class YoutubeCategory(
    val id: String,
    val label: String,
    val query: String,
)

data class YoutubeVideo(
    val videoId: String,
    val title: String,
    val channelId: String?,
    val channelTitle: String,
    val description: String?,
    val thumbnailUrl: String?,
    val publishedAt: String?,
    val viewCount: Long?,
    val durationIso: String?,
    val durationSeconds: Long?,
    val categoryId: String?,
    val tags: List<String>,
)

data class YoutubePage(
    val videos: List<YoutubeVideo>,
    val nextPageToken: String?,
)

class YoutubeRepository(
    private val api: YoutubeApiService,
    private val dao: YoutubeDao,
    private val apiKey: String,
    private val behaviorReporter: YoutubeBehaviorReporter,
) {
    val categories: List<YoutubeCategory> = listOf(
        YoutubeCategory("history", "Historique", ""),
        YoutubeCategory("trending", "Tendances", ""),
        YoutubeCategory("music", "Musique", "musique"),
        YoutubeCategory("sport", "Sport", "sport"),
        YoutubeCategory("gaming", "Gaming", "gaming"),
        YoutubeCategory("news", "Actualites", "actualites"),
        YoutubeCategory("movies", "Films", "films"),
        YoutubeCategory("documentaries", "Documentaires", "documentaires"),
        YoutubeCategory("kids", "Enfants", "enfants"),
    )

    fun observeRecentSearches(): Flow<List<YoutubeSearchEntity>> = dao.observeRecentSearches()

    fun observeRecentVideos(): Flow<List<YoutubeVideoHistoryEntity>> = dao.observeRecentVideos()

    fun observeRecentVideoCount(): Flow<Int> = dao.observeRecentVideoCount()

    suspend fun loadCategory(categoryId: String, pageToken: String? = null): YoutubePage = withContext(Dispatchers.IO) {
        val category = categories.firstOrNull { it.id == categoryId }
            ?: categories.first { it.id == "trending" }
        when (category.id) {
            "history" -> YoutubePage(
                videos = dao.getRecentVideos().map { it.toVideo() },
                nextPageToken = null,
            )
            "trending" -> {
                ensureApiKey()
                personalizedTrending(pageToken)
            }
            else -> searchPage(category.query, pageToken = pageToken, recordSearch = false)
        }
    }

    suspend fun search(query: String, pageToken: String? = null): YoutubePage = withContext(Dispatchers.IO) {
        searchPage(query, pageToken = pageToken, recordSearch = pageToken == null)
    }

    private suspend fun searchPage(
        query: String?,
        pageToken: String?,
        recordSearch: Boolean,
        channelId: String? = null,
        order: String = "relevance",
    ): YoutubePage {
        val clean = query?.trim().orEmpty()
        val cleanChannelId = channelId?.trim().orEmpty()
        require(clean.isNotBlank() || cleanChannelId.isNotBlank()) { "Recherche YouTube vide." }
        ensureApiKey()
        if (recordSearch && clean.isNotBlank()) {
            dao.upsertSearch(YoutubeSearchEntity(query = clean.take(120), updatedAt = System.currentTimeMillis()))
        }
        val response = api.searchVideos(
            apiKey = apiKey,
            query = clean.takeIf { it.isNotBlank() },
            channelId = cleanChannelId.takeIf { it.isNotBlank() },
            order = order,
            pageToken = pageToken,
        )
        val ids = response.items.mapNotNull { it.id?.videoId?.takeIf(String::isNotBlank) }.distinct()
        return YoutubePage(
            videos = enrichVideos(ids),
            nextPageToken = response.nextPageToken,
        )
    }

    suspend fun recordVideoSelected(video: YoutubeVideo, sourceScreen: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.upsertVideo(
            YoutubeVideoHistoryEntity(
                videoId = video.videoId,
                title = video.title.take(220),
                channelTitle = video.channelTitle.take(160),
                thumbnailUrl = video.thumbnailUrl,
                publishedAt = video.publishedAt,
                channelId = video.channelId,
                viewCount = video.viewCount,
                durationIso = video.durationIso,
                durationSeconds = video.durationSeconds,
                categoryId = video.categoryId,
                tags = video.tags.toTagStorage(),
                updatedAt = now,
            ),
        )
        dao.upsertSelection(YoutubeSelectionEntity(id = "last", videoId = video.videoId, updatedAt = now))
        behaviorReporter.report("VIDEO_OPENED", video, sourceScreen)
    }

    suspend fun recordBehavior(eventType: String, video: YoutubeVideo?, sourceScreen: String = "YOUTUBE") = withContext(Dispatchers.IO) {
        behaviorReporter.report(eventType, video, sourceScreen)
    }

    suspend fun suggestVideos(video: YoutubeVideo): List<YoutubeVideo> = withContext(Dispatchers.IO) {
        ensureApiKey()
        val sameChannel = video.channelId
            ?.takeIf { it.isNotBlank() }
            ?.let { channelId ->
                runCatching {
                    searchPage(
                        query = null,
                        pageToken = null,
                        recordSearch = false,
                        channelId = channelId,
                        order = "date",
                    ).videos
                }.getOrDefault(emptyList())
            }.orEmpty()
            .filterNot { it.videoId == video.videoId }

        if (sameChannel.size >= 8) return@withContext sameChannel.take(20)

        val keywordQuery = buildSuggestionQuery(video)
        val related = if (keywordQuery.isNotBlank()) {
            runCatching { searchPage(keywordQuery, pageToken = null, recordSearch = false).videos }
                .getOrDefault(emptyList())
        } else {
            emptyList()
        }

        (sameChannel + related + popularVideos(null).videos)
            .filterNot { it.videoId == video.videoId }
            .distinctBy { it.videoId }
            .take(24)
    }

    private fun ensureApiKey() {
        check(apiKey.isNotBlank()) { "Cle API YouTube absente. Configurez YOUTUBE_API_KEY dans local.properties." }
    }

    private suspend fun personalizedTrending(pageToken: String?): YoutubePage {
        val history = dao.getRecentVideos(limit = 30)
        val query = history.buildHistoryQuery()
        return if (query.isBlank()) {
            popularVideos(pageToken)
        } else {
            runCatching { searchPage(query, pageToken = pageToken, recordSearch = false) }
                .getOrElse { popularVideos(pageToken) }
        }
    }

    private suspend fun popularVideos(pageToken: String?): YoutubePage {
        val response = api.mostPopularVideos(apiKey = apiKey, pageToken = pageToken)
        return YoutubePage(
            videos = response.items
                .filter { it.status?.embeddable != false }
                .mapNotNull { it.toVideo() },
            nextPageToken = response.nextPageToken,
        )
    }

    private suspend fun enrichVideos(ids: List<String>): List<YoutubeVideo> {
        if (ids.isEmpty()) return emptyList()
        val response = api.videosByIds(apiKey = apiKey, ids = ids.take(50).joinToString(","))
        val byId = response.items
            .filter { it.status?.embeddable != false }
            .mapNotNull { it.toVideo() }
            .associateBy { it.videoId }
        return ids.mapNotNull { byId[it] }
    }
}

private fun YoutubeVideoHistoryEntity.toVideo(): YoutubeVideo =
    YoutubeVideo(
        videoId = videoId,
        title = title,
        channelId = channelId,
        channelTitle = channelTitle,
        description = null,
        thumbnailUrl = thumbnailUrl,
        publishedAt = publishedAt,
        viewCount = viewCount,
        durationIso = durationIso,
        durationSeconds = durationSeconds,
        categoryId = categoryId,
        tags = tags.fromTagStorage(),
    )

private fun YoutubeVideoDto.toVideo(): YoutubeVideo? {
    val cleanId = id?.takeIf { it.isNotBlank() } ?: return null
    val snippet = snippet ?: return null
    return YoutubeVideo(
        videoId = cleanId,
        title = snippet.title?.htmlClean().orEmpty().ifBlank { "Video YouTube" },
        channelId = snippet.channelId?.takeIf { it.isNotBlank() },
        channelTitle = snippet.channelTitle?.htmlClean().orEmpty().ifBlank { "YouTube" },
        description = snippet.description?.htmlClean(),
        thumbnailUrl = snippet.thumbnails?.high?.url ?: snippet.thumbnails?.medium?.url ?: snippet.thumbnails?.default?.url,
        publishedAt = snippet.publishedAt,
        viewCount = statistics?.viewCount?.toLongOrNull(),
        durationIso = contentDetails?.duration,
        durationSeconds = contentDetails?.duration?.parseYoutubeDurationSeconds(),
        categoryId = snippet.categoryId?.takeIf { it.isNotBlank() },
        tags = snippet.tags.orEmpty().map { it.htmlClean() },
    )
}

private fun List<YoutubeVideoHistoryEntity>.buildHistoryQuery(): String {
    val tagSignals = flatMap { it.tags.fromTagStorage() }
    val channelSignals = mapNotNull { it.channelTitle.takeIf(String::isNotBlank) }
    return (tagSignals + channelSignals)
        .map { it.trim() }
        .filter { it.length in 2..40 }
        .groupingBy { it.lowercase() }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(4)
        .joinToString(" ") { it.key }
}

private fun buildSuggestionQuery(video: YoutubeVideo): String =
    (video.tags.take(4) + listOfNotNull(video.categoryId, video.channelTitle))
        .map { it.trim() }
        .filter { it.length in 2..40 }
        .distinctBy { it.lowercase() }
        .take(5)
        .joinToString(" ")

private fun List<String>.toTagStorage(): String? =
    asSequence()
        .map { it.trim().lowercase() }
        .filter { it.length in 2..32 }
        .map { tag -> tag.filter { it.isLetterOrDigit() || it == '-' || it == '_' } }
        .filter { it.isNotBlank() }
        .distinct()
        .take(16)
        .joinToString(",")
        .ifBlank { null }

private fun String?.fromTagStorage(): List<String> =
    orEmpty()
        .split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }

private fun String.parseYoutubeDurationSeconds(): Long? {
    val match = Regex("""PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?""").matchEntire(this) ?: return null
    val hours = match.groupValues.getOrNull(1)?.toLongOrNull() ?: 0L
    val minutes = match.groupValues.getOrNull(2)?.toLongOrNull() ?: 0L
    val seconds = match.groupValues.getOrNull(3)?.toLongOrNull() ?: 0L
    return hours * 3600L + minutes * 60L + seconds
}

private fun String.htmlClean(): String =
    replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
