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
    val channelTitle: String,
    val description: String?,
    val thumbnailUrl: String?,
    val publishedAt: String?,
)

data class YoutubePage(
    val videos: List<YoutubeVideo>,
    val nextPageToken: String?,
)

class YoutubeRepository(
    private val api: YoutubeApiService,
    private val dao: YoutubeDao,
    private val apiKey: String,
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
                val response = api.mostPopularVideos(apiKey = apiKey, pageToken = pageToken)
                YoutubePage(
                    videos = response.items
                        .filter { it.status?.embeddable != false }
                        .mapNotNull { it.toVideo() },
                    nextPageToken = response.nextPageToken,
                )
            }
            else -> searchPage(category.query, pageToken = pageToken, recordSearch = false)
        }
    }

    suspend fun search(query: String, pageToken: String? = null): YoutubePage = withContext(Dispatchers.IO) {
        searchPage(query, pageToken = pageToken, recordSearch = pageToken == null)
    }

    private suspend fun searchPage(
        query: String,
        pageToken: String?,
        recordSearch: Boolean,
    ): YoutubePage {
        val clean = query.trim()
        require(clean.isNotBlank()) { "Recherche YouTube vide." }
        ensureApiKey()
        if (recordSearch) {
            dao.upsertSearch(YoutubeSearchEntity(query = clean.take(120), updatedAt = System.currentTimeMillis()))
        }
        val response = api.searchVideos(apiKey = apiKey, query = clean, pageToken = pageToken)
        return YoutubePage(
            videos = response.items.mapNotNull { item ->
            val videoId = item.id?.videoId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            item.snippet?.toVideo(videoId)
            },
            nextPageToken = response.nextPageToken,
        )
    }

    suspend fun recordVideoSelected(video: YoutubeVideo) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.upsertVideo(
            YoutubeVideoHistoryEntity(
                videoId = video.videoId,
                title = video.title.take(220),
                channelTitle = video.channelTitle.take(160),
                thumbnailUrl = video.thumbnailUrl,
                publishedAt = video.publishedAt,
                updatedAt = now,
            ),
        )
        dao.upsertSelection(YoutubeSelectionEntity(id = "last", videoId = video.videoId, updatedAt = now))
    }

    private fun ensureApiKey() {
        check(apiKey.isNotBlank()) { "Cle API YouTube absente. Configurez YOUTUBE_API_KEY dans local.properties." }
    }
}

private fun YoutubeVideoHistoryEntity.toVideo(): YoutubeVideo =
    YoutubeVideo(
        videoId = videoId,
        title = title,
        channelTitle = channelTitle,
        description = null,
        thumbnailUrl = thumbnailUrl,
        publishedAt = publishedAt,
    )

private fun YoutubeVideoDto.toVideo(): YoutubeVideo? {
    val cleanId = id?.takeIf { it.isNotBlank() } ?: return null
    return snippet?.toVideo(cleanId)
}

private fun YoutubeSnippetDto.toVideo(videoId: String): YoutubeVideo =
    YoutubeVideo(
        videoId = videoId,
        title = title?.htmlClean().orEmpty().ifBlank { "Video YouTube" },
        channelTitle = channelTitle?.htmlClean().orEmpty().ifBlank { "YouTube" },
        description = description?.htmlClean(),
        thumbnailUrl = thumbnails?.high?.url ?: thumbnails?.medium?.url ?: thumbnails?.default?.url,
        publishedAt = publishedAt,
    )

private fun String.htmlClean(): String =
    replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
