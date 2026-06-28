package com.smartvision.svplayer.ui.youtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.youtube.YoutubeRepository
import com.smartvision.svplayer.data.youtube.YoutubeVideo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class YoutubeCategoryUi(
    val id: String,
    val label: String,
    val count: Int? = null,
)

data class YoutubeVideoUi(
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
) {
    val meta: String = listOfNotNull(formatViews(viewCount), channelTitle, publishedAt?.take(10)).joinToString(" | ")
    val durationLabel: String = formatDuration(durationSeconds)
}

data class YoutubeScreenState(
    val categories: List<YoutubeCategoryUi> = emptyList(),
    val selectedCategoryId: String = "trending",
    val videos: List<YoutubeVideoUi> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val nextPageToken: String? = null,
    val errorMessage: String? = null,
    val focusedVideoId: String? = null,
    val selectedVideoId: String? = null,
    val searchQuery: String = "",
    val recentSearches: List<String> = emptyList(),
    val searchSuggestions: List<String> = emptyList(),
    val playerSuggestions: List<YoutubeVideoUi> = emptyList(),
    val suggestionsLoading: Boolean = false,
) {
    val selectedCategory: YoutubeCategoryUi?
        get() = categories.firstOrNull { it.id == selectedCategoryId }
}

class YoutubeViewModel(
    private val repository: YoutubeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        YoutubeScreenState(
            categories = repository.categories.map { YoutubeCategoryUi(it.id, it.label) },
        ),
    )
    val uiState: StateFlow<YoutubeScreenState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var activeSearchQuery: String? = null

    init {
        observeSearches()
        observeRecentVideos()
        loadCategory("trending")
    }

    fun selectCategory(category: YoutubeCategoryUi) {
        if (_uiState.value.selectedCategoryId == category.id && _uiState.value.videos.isNotEmpty()) return
        loadCategory(category.id)
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            val clean = query.take(120)
            state.copy(
                searchQuery = clean,
                searchSuggestions = buildSearchSuggestions(clean, state.recentSearches, state.categories),
            )
        }
    }

    fun submitSearch(queryOverride: String? = null) {
        val query = (queryOverride ?: _uiState.value.searchQuery).trim()
        if (query.isBlank()) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            activeSearchQuery = query
            _uiState.update {
                it.copy(
                    loading = true,
                    loadingMore = false,
                    endReached = false,
                    nextPageToken = null,
                    errorMessage = null,
                    selectedCategoryId = "search",
                    videos = emptyList(),
                    focusedVideoId = null,
                    selectedVideoId = null,
                    searchQuery = query,
                    searchSuggestions = emptyList(),
                )
            }
            runCatching { repository.search(query) }
                .onSuccess { page ->
                    val videos = page.videos.map { it.toUi() }
                    _uiState.update {
                        it.copy(
                            loading = false,
                            videos = videos,
                            focusedVideoId = videos.firstOrNull()?.videoId,
                            nextPageToken = page.nextPageToken,
                            endReached = page.nextPageToken == null,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            errorMessage = error.userMessage("Recherche YouTube impossible."),
                        )
                    }
                }
        }
    }

    fun selectSuggestion(suggestion: String) {
        _uiState.update { it.copy(searchSuggestions = emptyList()) }
        submitSearch(suggestion)
    }

    fun focusVideo(video: YoutubeVideoUi) {
        _uiState.update { it.copy(focusedVideoId = video.videoId) }
    }

    fun openYoutubeVideo(video: YoutubeVideoUi) {
        _uiState.update {
            it.copy(
                selectedVideoId = video.videoId,
                focusedVideoId = video.videoId,
                playerSuggestions = emptyList(),
                suggestionsLoading = true,
            )
        }
        viewModelScope.launch {
            val domain = video.toDomain()
            repository.recordVideoSelected(domain)
            val suggestions = repository.suggestVideos(domain).map { it.toUi() }
            _uiState.update {
                it.copy(
                    playerSuggestions = suggestions,
                    suggestionsLoading = false,
                )
            }
        }
    }

    fun closePlayerToTrending() {
        _uiState.update { it.copy(playerSuggestions = emptyList(), suggestionsLoading = false) }
        loadCategory("trending")
    }

    fun recordPlayerBehavior(eventType: String, video: YoutubeVideoUi?) {
        viewModelScope.launch {
            repository.recordBehavior(eventType, video?.toDomain())
        }
    }

    fun retry() {
        val state = _uiState.value
        if (state.selectedCategoryId == "search") {
            submitSearch(activeSearchQuery ?: state.searchQuery)
        } else {
            loadCategory(state.selectedCategoryId)
        }
    }

    fun loadMoreIfNeeded(lastVisibleIndex: Int) {
        val state = _uiState.value
        if (lastVisibleIndex < state.videos.size - 10) return
        if (state.loading || state.loadingMore || state.endReached || state.nextPageToken.isNullOrBlank()) return
        val token = state.nextPageToken
        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true, errorMessage = null) }
            val result = if (state.selectedCategoryId == "search") {
                val query = activeSearchQuery ?: state.searchQuery.trim()
                runCatching { repository.search(query, pageToken = token) }
            } else {
                runCatching { repository.loadCategory(state.selectedCategoryId, pageToken = token) }
            }
            result
                .onSuccess { page ->
                    val appended = (_uiState.value.videos + page.videos.map { it.toUi() })
                        .distinctBy { it.videoId }
                    _uiState.update {
                        it.copy(
                            videos = appended,
                            loadingMore = false,
                            nextPageToken = page.nextPageToken,
                            endReached = page.nextPageToken == null,
                            categories = it.categories.withCategoryCount(it.selectedCategoryId, appended.size),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loadingMore = false,
                            errorMessage = error.userMessage("Chargement YouTube impossible."),
                        )
                    }
                }
        }
    }

    private fun loadCategory(categoryId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            activeSearchQuery = null
            _uiState.update {
                it.copy(
                    loading = true,
                    loadingMore = false,
                    endReached = false,
                    nextPageToken = null,
                    errorMessage = null,
                    selectedCategoryId = categoryId,
                    videos = emptyList(),
                    focusedVideoId = null,
                    selectedVideoId = null,
                    playerSuggestions = emptyList(),
                    suggestionsLoading = false,
                )
            }
            runCatching { repository.loadCategory(categoryId) }
                .onSuccess { page ->
                    val videos = page.videos.map { it.toUi() }
                    _uiState.update {
                        it.copy(
                            loading = false,
                            videos = videos,
                            focusedVideoId = videos.firstOrNull()?.videoId,
                            nextPageToken = page.nextPageToken,
                            endReached = page.nextPageToken == null,
                            categories = it.categories.withCategoryCount(categoryId, videos.size),
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            errorMessage = error.userMessage("Chargement YouTube impossible."),
                        )
                    }
                }
        }
    }

    private fun observeSearches() {
        viewModelScope.launch {
            repository.observeRecentSearches().collect { searches ->
                _uiState.update { state ->
                    val recent = searches.map { it.query }
                    state.copy(
                        recentSearches = recent,
                        searchSuggestions = if (
                            state.loading ||
                            state.searchQuery.trim().equals(activeSearchQuery, ignoreCase = true)
                        ) {
                            emptyList()
                        } else {
                            buildSearchSuggestions(state.searchQuery, recent, state.categories)
                        },
                    )
                }
            }
        }
    }

    private fun observeRecentVideos() {
        viewModelScope.launch {
            repository.observeRecentVideoCount().collect { count ->
                _uiState.update { state ->
                    state.copy(categories = state.categories.withCategoryCount("history", count))
                }
            }
        }
    }
}

private fun buildSearchSuggestions(
    query: String,
    recentSearches: List<String>,
    categories: List<YoutubeCategoryUi>,
): List<String> {
    val clean = query.trim()
    val categorySuggestions = categories
        .filterNot { it.id == "history" }
        .map { it.label }
    val candidates = (recentSearches + categorySuggestions)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
    return if (clean.isBlank()) {
        recentSearches.take(5)
    } else {
        candidates
            .filter { it.contains(clean, ignoreCase = true) }
            .take(5)
    }
}

private fun List<YoutubeCategoryUi>.withCategoryCount(categoryId: String, count: Int): List<YoutubeCategoryUi> =
    map { category ->
        if (category.id == categoryId) {
            category.copy(count = count.takeIf { it > 0 })
        } else {
            category
        }
    }

private fun YoutubeVideo.toUi(): YoutubeVideoUi =
    YoutubeVideoUi(
        videoId = videoId,
        title = title,
        channelId = channelId,
        channelTitle = channelTitle,
        description = description,
        thumbnailUrl = thumbnailUrl,
        publishedAt = publishedAt,
        viewCount = viewCount,
        durationIso = durationIso,
        durationSeconds = durationSeconds,
        categoryId = categoryId,
        tags = tags,
    )

private fun YoutubeVideoUi.toDomain(): YoutubeVideo =
    YoutubeVideo(
        videoId = videoId,
        title = title,
        channelId = channelId,
        channelTitle = channelTitle,
        description = description,
        thumbnailUrl = thumbnailUrl,
        publishedAt = publishedAt,
        viewCount = viewCount,
        durationIso = durationIso,
        durationSeconds = durationSeconds,
        categoryId = categoryId,
        tags = tags,
    )

private fun Throwable.userMessage(defaultMessage: String): String =
    message?.takeIf { it.isNotBlank() } ?: defaultMessage

private fun formatViews(viewCount: Long?): String? {
    val views = viewCount ?: return null
    return when {
        views >= 1_000_000_000L -> "${views / 1_000_000_000L} Md vues"
        views >= 1_000_000L -> "${views / 1_000_000L} M vues"
        views >= 1_000L -> "${views / 1_000L} k vues"
        else -> "$views vues"
    }
}

private fun formatDuration(durationSeconds: Long?): String {
    val total = durationSeconds ?: return ""
    val hours = total / 3600L
    val minutes = (total % 3600L) / 60L
    val seconds = total % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
