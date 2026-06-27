package com.smartvision.svplayer.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.models.XtreamLiveCategory
import com.smartvision.svplayer.data.models.XtreamLiveStream
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.UserContentType
import com.smartvision.svplayer.data.repository.XtreamRepository
import com.smartvision.svplayer.domain.model.CategoryHistorySignal
import com.smartvision.svplayer.domain.model.sortedByHistorySignals
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveTvCategory(
    val id: String,
    val label: String,
    val count: Int?,
    val kind: LiveTvCategoryKind,
)

enum class LiveTvCategoryKind {
    France,
    Sport,
    Info,
    Cinema,
    Kids,
    Documentary,
    Generic,
}

data class LiveTvChannel(
    val streamId: Int,
    val number: String,
    val logoText: String,
    val logoUrl: String?,
    val name: String,
    val program: String,
    val genre: String,
    val timeRange: String,
    val progress: Float,
    val description: String,
    val nextProgram: String,
    val nextTimeRange: String,
    val streamUrl: String,
    val fallbackStreamUrl: String,
    val quality: String = "HD",
    val isFavorite: Boolean = false,
)

data class LiveTvUiState(
    val categoriesLoading: Boolean = true,
    val channelsLoading: Boolean = false,
    val errorMessage: String? = null,
    val categories: List<LiveTvCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val channels: List<LiveTvChannel> = emptyList(),
    val focusedChannelId: Int? = null,
    val selectedChannelId: Int? = null,
) {
    val selectedCategory: LiveTvCategory?
        get() = categories.firstOrNull { it.id == selectedCategoryId }

    val selectedChannel: LiveTvChannel?
        get() = channels.firstOrNull { it.streamId == selectedChannelId }

    val focusedChannel: LiveTvChannel?
        get() = channels.firstOrNull { it.streamId == focusedChannelId }
}

class LiveTvViewModel(
    private val xtreamRepository: XtreamRepository,
    private val userContentRepository: UserContentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LiveTvUiState())
    val uiState: StateFlow<LiveTvUiState> = _uiState.asStateFlow()

    private var channelsJob: Job? = null
    private var favoriteIds: Set<Int> = emptySet()
    private var historyProgress: List<PlaybackProgressEntity> = emptyList()
    private var historyCategorySignals: List<CategoryHistorySignal> = emptyList()

    init {
        observeFavorites()
        observeHistory()
        loadCategories()
    }

    fun loadCategories() {
        channelsJob?.cancel()
        viewModelScope.launch {
            _uiState.value = LiveTvUiState(categoriesLoading = true)
            runCatching {
                xtreamRepository.getLiveCategories().map { it.toUiCategory() }
            }.onSuccess { categories ->
                if (categories.isEmpty()) {
                    _uiState.value = LiveTvUiState(
                        categoriesLoading = false,
                        errorMessage = "Aucune categorie Live TV retournee par Xtream.",
                    )
                    return@onSuccess
                }

                _uiState.update {
                    val visibleCategories = categories.withSpecialCategories(
                        allCount = xtreamRepository.getCachedLiveStreams().size.takeIf { it > 0 },
                        favoriteCount = favoriteIds.size,
                        historyCount = historyProgress.size,
                        historySignals = historyCategorySignals,
                    )
                    it.copy(
                        categoriesLoading = false,
                        errorMessage = null,
                        categories = visibleCategories,
                        selectedCategoryId = HistoryLiveCategoryId,
                    )
                }
                loadHistoryChannels()
            }.onFailure { error ->
                _uiState.value = LiveTvUiState(
                    categoriesLoading = false,
                    errorMessage = error.userMessage("Impossible de charger les categories Xtream."),
                )
            }
        }
    }

    fun selectCategory(category: LiveTvCategory) {
        val current = _uiState.value
        if (current.selectedCategoryId == category.id && (current.channels.isNotEmpty() || current.channelsLoading)) {
            return
        }
        if (category.id == FavoriteLiveCategoryId) {
            loadFavoriteChannels()
            return
        }
        if (category.id == HistoryLiveCategoryId) {
            loadHistoryChannels()
            return
        }
        if (category.id == AllLiveCategoryId) {
            loadAllChannels()
            return
        }
        loadChannels(category.id)
    }

    fun focusChannel(channel: LiveTvChannel) {
        _uiState.update { state ->
            state.copy(focusedChannelId = channel.streamId)
        }
    }

    fun activateChannel(channel: LiveTvChannel): Boolean {
        val shouldOpenFullPlayer = _uiState.value.selectedChannelId == channel.streamId
        startPreview(channel)
        return shouldOpenFullPlayer
    }

    fun startPreview(channel: LiveTvChannel) {
        _uiState.update { state ->
            state.copy(
                selectedChannelId = channel.streamId,
                focusedChannelId = channel.streamId,
                errorMessage = null,
            )
        }
    }

    fun toggleFavorite(channel: LiveTvChannel) {
        viewModelScope.launch {
            userContentRepository.toggleFavorite(UserContentType.Live, channel.streamId)
        }
    }

    fun retryCurrentCategory() {
        val categoryId = _uiState.value.selectedCategoryId
        if (categoryId == null) {
            loadCategories()
        } else if (categoryId == FavoriteLiveCategoryId) {
            loadFavoriteChannels()
        } else if (categoryId == HistoryLiveCategoryId) {
            loadHistoryChannels()
        } else if (categoryId == AllLiveCategoryId) {
            loadAllChannels()
        } else {
            loadChannels(categoryId)
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            userContentRepository.observeFavoriteIds(UserContentType.Live).collect { ids ->
                favoriteIds = ids
                _uiState.update { state ->
                    val refreshedChannels = if (state.selectedCategoryId == FavoriteLiveCategoryId) {
                        favoriteChannels()
                    } else {
                        state.channels.map { it.copy(isFavorite = it.streamId in ids) }
                    }
                    state.copy(
                        categories = state.categories.withSpecialCategories(
                            allCount = xtreamRepository.getCachedLiveStreams().size.takeIf { it > 0 },
                            favoriteCount = ids.size,
                            historyCount = historyProgress.size,
                            historySignals = historyCategorySignals,
                        ),
                        channels = refreshedChannels,
                        focusedChannelId = state.focusedChannelId
                            ?.takeIf { focusedId -> refreshedChannels.any { it.streamId == focusedId } }
                            ?: refreshedChannels.firstOrNull()?.streamId,
                        selectedChannelId = state.selectedChannelId
                            ?.takeIf { selectedId -> refreshedChannels.any { it.streamId == selectedId } },
                    )
                }
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            userContentRepository.observeHistory(UserContentType.Live).collect { progress ->
                historyProgress = progress.map { userContentRepository.enrichProgress(it) }
                    .distinctBy { it.contentId }
                historyCategorySignals = userContentRepository.resolveCategorySignals(historyProgress)
                _uiState.update { state ->
                    val history = historyChannels()
                    state.copy(
                        categories = state.categories.withSpecialCategories(
                            allCount = xtreamRepository.getCachedLiveStreams().size.takeIf { it > 0 },
                            favoriteCount = favoriteIds.size,
                            historyCount = history.size,
                            historySignals = historyCategorySignals,
                        ),
                        channels = if (state.selectedCategoryId == HistoryLiveCategoryId) history else state.channels,
                        focusedChannelId = if (state.selectedCategoryId == HistoryLiveCategoryId) {
                            state.focusedChannelId
                                ?.takeIf { focusedId -> history.any { it.streamId == focusedId } }
                                ?: history.firstOrNull()?.streamId
                        } else {
                            state.focusedChannelId
                        },
                    )
                }
            }
        }
    }

    private fun loadFavoriteChannels() {
        channelsJob?.cancel()
        val channels = favoriteChannels()
        _uiState.update { state ->
            state.copy(
                channelsLoading = false,
                errorMessage = null,
                selectedCategoryId = FavoriteLiveCategoryId,
                categories = state.categories.withSpecialCategories(
                    allCount = xtreamRepository.getCachedLiveStreams().size.takeIf { it > 0 },
                    favoriteCount = favoriteIds.size,
                    historyCount = historyProgress.size,
                    historySignals = historyCategorySignals,
                ),
                channels = channels,
                focusedChannelId = channels.firstOrNull()?.streamId,
                selectedChannelId = null,
            )
        }
    }

    private fun loadHistoryChannels() {
        channelsJob?.cancel()
        val channels = historyChannels()
        _uiState.update { state ->
            state.copy(
                channelsLoading = false,
                errorMessage = null,
                selectedCategoryId = HistoryLiveCategoryId,
                categories = state.categories.withSpecialCategories(
                    allCount = xtreamRepository.getCachedLiveStreams().size.takeIf { it > 0 },
                    favoriteCount = favoriteIds.size,
                    historyCount = channels.size,
                    historySignals = historyCategorySignals,
                ),
                channels = channels,
                focusedChannelId = channels.firstOrNull()?.streamId,
                selectedChannelId = null,
            )
        }
    }

    private fun loadAllChannels() {
        channelsJob?.cancel()
        channelsJob = viewModelScope.launch {
            val categories = _uiState.value.categories
                .filterNot { it.id in SpecialLiveCategoryIds }
                .sortedByHistorySignals(historyCategorySignals) { it.id }
            val collected = mutableListOf<LiveTvChannel>()
            val seen = mutableSetOf<Int>()
            _uiState.update { state ->
                state.copy(
                    channelsLoading = true,
                    errorMessage = null,
                    selectedCategoryId = AllLiveCategoryId,
                    channels = emptyList(),
                    focusedChannelId = null,
                    selectedChannelId = null,
                )
            }

            runCatching {
                categories.forEach { category ->
                    xtreamRepository.getLiveStreams(category.id).forEach { stream ->
                        if (!seen.add(stream.streamId)) return@forEach
                        collected += stream.toUiChannel(
                            index = collected.size,
                            categoryLabel = category.label,
                            xtreamRepository = xtreamRepository,
                            favoriteIds = favoriteIds,
                        )
                    }
                    _uiState.update { state ->
                        state.copy(
                            categories = state.categories.withSpecialCategories(
                                allCount = collected.size,
                                favoriteCount = favoriteIds.size,
                                historyCount = historyProgress.size,
                                historySignals = historyCategorySignals,
                            ),
                            channels = collected.toList(),
                            focusedChannelId = state.focusedChannelId ?: collected.firstOrNull()?.streamId,
                        )
                    }
                }
                collected.toList()
            }.onSuccess { channels ->
                _uiState.update { state ->
                    state.copy(
                        channelsLoading = false,
                        categories = state.categories.withSpecialCategories(
                            allCount = channels.size,
                            favoriteCount = favoriteIds.size,
                            historyCount = historyProgress.size,
                            historySignals = historyCategorySignals,
                        ),
                        channels = channels,
                        focusedChannelId = channels.firstOrNull()?.streamId,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        channelsLoading = false,
                        channels = collected.toList(),
                        focusedChannelId = collected.firstOrNull()?.streamId,
                        selectedChannelId = null,
                        errorMessage = error.userMessage("Impossible de charger toutes les chaines Xtream."),
                    )
                }
            }
        }
    }

    fun deleteHistoryChannel(channel: LiveTvChannel) {
        viewModelScope.launch {
            userContentRepository.deleteProgress(UserContentType.Live, channel.streamId)
        }
    }

    private fun historyChannels(): List<LiveTvChannel> =
        historyProgress.mapIndexedNotNull { index, progress ->
            val id = progress.contentId.toIntOrNull() ?: return@mapIndexedNotNull null
            val cachedStream = xtreamRepository.getCachedLiveStream(id)
            val name = cachedStream?.name?.cleanedChannelName()
                ?: progress.title?.takeUnless { it.isGeneratedLiveTitle(id) }
                ?: "Chaine $id"
            val categoryLabel = cachedStream?.categoryId
                ?.let { categoryId -> xtreamRepository.getCachedCategories().firstOrNull { it.id == categoryId }?.name }
                ?: progress.subtitle
                ?: "Historique"
            LiveTvChannel(
                streamId = id,
                number = (index + 1).toString().padStart(3, '0'),
                logoText = name.logoFallback(),
                logoUrl = cachedStream?.streamIcon?.takeIf { it.isNotBlank() } ?: progress.imageUrl,
                name = name,
                program = "Direct",
                genre = categoryLabel,
                timeRange = "Live",
                progress = 0f,
                description = "Chaine deja regardee.",
                nextProgram = "EPG non disponible",
                nextTimeRange = "A suivre",
                streamUrl = cachedStream?.let { xtreamRepository.buildLiveStreamUrl(it) }
                    ?: xtreamRepository.buildLiveStreamUrl(id),
                fallbackStreamUrl = xtreamRepository.buildLiveStreamFallbackUrl(id),
                isFavorite = id in favoriteIds,
            )
        }

    private fun favoriteChannels(): List<LiveTvChannel> =
        xtreamRepository.getCachedLiveStreams()
            .filter { it.streamId in favoriteIds }
            .sortedBy { it.name }
            .mapIndexed { index, stream ->
                val categoryLabel = xtreamRepository.getCachedCategories()
                    .firstOrNull { it.id == stream.categoryId }
                    ?.name
                    ?: "Favoris"
                stream.toUiChannel(
                    index = index,
                    categoryLabel = categoryLabel,
                    xtreamRepository = xtreamRepository,
                    favoriteIds = favoriteIds,
                )
            }

    private fun loadChannels(categoryId: String) {
        channelsJob?.cancel()
        channelsJob = viewModelScope.launch {
            val categoryLabel = _uiState.value.categories.firstOrNull { it.id == categoryId }?.label ?: "Live TV"
            _uiState.update { state ->
                state.copy(
                    channelsLoading = true,
                    errorMessage = null,
                    selectedCategoryId = categoryId,
                    channels = emptyList(),
                    focusedChannelId = null,
                    selectedChannelId = null,
                )
            }

            runCatching {
                xtreamRepository.getLiveStreams(categoryId).mapIndexed { index, stream ->
                    stream.toUiChannel(
                        index = index,
                        categoryLabel = categoryLabel,
                        xtreamRepository = xtreamRepository,
                        favoriteIds = favoriteIds,
                    )
                }
            }.onSuccess { channels ->
                _uiState.update { state ->
                    val refreshedCategories = state.categories.map { category ->
                        if (category.id == categoryId) category.copy(count = channels.size) else category
                    }.withSpecialCategories(
                        allCount = xtreamRepository.getCachedLiveStreams().size.takeIf { it > 0 },
                        favoriteCount = favoriteIds.size,
                        historyCount = historyProgress.size,
                        historySignals = historyCategorySignals,
                    )
                    state.copy(
                        channelsLoading = false,
                        categories = refreshedCategories,
                        channels = channels,
                        focusedChannelId = channels.firstOrNull()?.streamId,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        channelsLoading = false,
                        channels = emptyList(),
                        focusedChannelId = null,
                        selectedChannelId = null,
                        errorMessage = error.userMessage("Impossible de charger les chaines Xtream."),
                    )
                }
            }
        }
    }
}

private const val FavoriteLiveCategoryId = "__favorites_live__"
private const val HistoryLiveCategoryId = "__history_live__"
private const val AllLiveCategoryId = "__all_live__"
private val SpecialLiveCategoryIds = setOf(AllLiveCategoryId, FavoriteLiveCategoryId, HistoryLiveCategoryId)

private fun List<LiveTvCategory>.withSpecialCategories(
    allCount: Int?,
    favoriteCount: Int,
    historyCount: Int,
    historySignals: List<CategoryHistorySignal> = emptyList(),
): List<LiveTvCategory> =
    listOf(
        LiveTvCategory(
            id = AllLiveCategoryId,
            label = "ALL",
            count = allCount,
            kind = LiveTvCategoryKind.Generic,
        ),
        LiveTvCategory(
            id = FavoriteLiveCategoryId,
            label = "Favoris",
            count = favoriteCount,
            kind = LiveTvCategoryKind.Generic,
        ),
        LiveTvCategory(
            id = HistoryLiveCategoryId,
            label = "Historique",
            count = historyCount,
            kind = LiveTvCategoryKind.Generic,
        ),
    ) + filterNot { it.id in SpecialLiveCategoryIds }
        .sortedByHistorySignals(historySignals) { it.id }

private fun XtreamLiveCategory.toUiCategory(): LiveTvCategory =
    LiveTvCategory(
        id = id,
        label = name,
        count = count,
        kind = name.categoryKind(),
    )

private fun XtreamLiveStream.toUiChannel(
    index: Int,
    categoryLabel: String,
    xtreamRepository: XtreamRepository,
    favoriteIds: Set<Int>,
): LiveTvChannel {
    val displayName = name.cleanedChannelName()
    return LiveTvChannel(
        streamId = streamId,
        number = (number.takeIf { it > 0 } ?: (index + 1)).toString().padStart(3, '0'),
        logoText = displayName.logoFallback(),
        logoUrl = streamIcon?.takeIf { it.isNotBlank() },
        name = displayName,
        program = "Direct",
        genre = categoryLabel,
        timeRange = "Live",
        progress = 0f,
        description = "Flux live ${categoryLabel.lowercase()} fourni par Xtream.",
        nextProgram = "EPG non disponible",
        nextTimeRange = "A suivre",
        streamUrl = xtreamRepository.buildLiveStreamUrl(this),
        fallbackStreamUrl = xtreamRepository.buildLiveStreamFallbackUrl(streamId),
        isFavorite = streamId in favoriteIds,
    )
}

private fun String.categoryKind(): LiveTvCategoryKind {
    val normalized = lowercase()
    return when {
        "france" in normalized || "french" in normalized -> LiveTvCategoryKind.France
        "sport" in normalized || "foot" in normalized -> LiveTvCategoryKind.Sport
        "info" in normalized || "news" in normalized || "actual" in normalized -> LiveTvCategoryKind.Info
        "cin" in normalized || "movie" in normalized || "film" in normalized -> LiveTvCategoryKind.Cinema
        "kids" in normalized || "jeunesse" in normalized || "enfant" in normalized -> LiveTvCategoryKind.Kids
        "doc" in normalized || "decouverte" in normalized -> LiveTvCategoryKind.Documentary
        else -> LiveTvCategoryKind.Generic
    }
}

private fun String.cleanedChannelName(): String =
    replace(Regex("\\s+"), " ")
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" SD", "", ignoreCase = true)
        .trim()
        .ifBlank { "Chaine TV" }

private fun String.isGeneratedLiveTitle(streamId: Int): Boolean {
    val normalized = trim()
    return normalized.equals("Chaine $streamId", ignoreCase = true) ||
        normalized.equals("Chaîne $streamId", ignoreCase = true) ||
        normalized.equals("ChaÃ®ne $streamId", ignoreCase = true) ||
        normalized.matches(Regex("(?i)^chaine\\s+\\d+$"))
}

private fun String.logoFallback(): String {
    val compact = replace(Regex("[^A-Za-z0-9+]"), "")
    return when {
        compact.isBlank() -> "TV"
        compact.length <= 5 -> compact.uppercase()
        else -> split(" ", "-", "_")
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .take(3)
            .joinToString("")
            .ifBlank { compact.take(3).uppercase() }
    }
}

private fun Throwable.userMessage(fallback: String): String =
    when (this) {
        is IllegalStateException -> message ?: fallback
        else -> fallback
    }
