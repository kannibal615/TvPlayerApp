package com.smartvision.svplayer.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.models.XtreamLiveCategory
import com.smartvision.svplayer.data.models.XtreamLiveStream
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.playlist.EpgRepository
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.UserContentType
import com.smartvision.svplayer.data.repository.XtreamRepository
import com.smartvision.svplayer.domain.model.Category
import com.smartvision.svplayer.domain.model.CategoryHistorySignal
import com.smartvision.svplayer.domain.model.LiveChannel as LocalLiveChannel
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.domain.model.sortedByHistorySignals
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.domain.repository.SettingsRepository
import com.smartvision.svplayer.ui.settings.allowsContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LiveTvCategory(
    val id: String,
    val label: String,
    val count: Int?,
    val kind: LiveTvCategoryKind,
    val hasEpg: Boolean = false,
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
    val epgPrograms: List<LiveTvProgram> = emptyList(),
    val streamUrl: String,
    val fallbackStreamUrl: String,
    val quality: String = "HD",
    val isFavorite: Boolean = false,
)

data class LiveTvProgram(
    val title: String,
    val timeRange: String,
    val description: String,
)

data class LiveTvUiState(
    val categoriesLoading: Boolean = true,
    val itemsLoading: Boolean = false,
    val channelsLoading: Boolean = false,
    val nextPageLoading: Boolean = false,
    val hasMoreItems: Boolean = false,
    val currentOffset: Int = 0,
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
    private val catalogRepository: CatalogRepository,
    private val userContentRepository: UserContentRepository,
    private val settingsRepository: SettingsRepository,
    private val epgRepository: EpgRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LiveTvUiState())
    val uiState: StateFlow<LiveTvUiState> = _uiState.asStateFlow()

    private var channelsJob: Job? = null
    private var favoriteIds: Set<Int> = emptySet()
    private var historyProgress: List<PlaybackProgressEntity> = emptyList()
    private var historyCategorySignals: List<CategoryHistorySignal> = emptyList()
    private var playerSettings = PlayerSettings()
    private var localCategories: List<Category> = emptyList()

    init {
        observeSettings()
        observeFavorites()
        observeHistory()
        loadCategories()
    }

    fun loadCategories() {
        channelsJob?.cancel()
        val cachedCategories = catalogRepository.getCachedLiveCategories()
        if (!cachedCategories.isNullOrEmpty()) {
            applyCategories(cachedCategories)
            return
        }
        viewModelScope.launch {
            _uiState.value = LiveTvUiState(categoriesLoading = true)
            var initialApplied = false
            runCatching { catalogRepository.getInitialLiveCategoriesSnapshot(InitialCategoryLimit) }
                .onSuccess { categories ->
                    if (categories.isNotEmpty()) {
                        initialApplied = true
                        applyCategories(categories)
                    }
                }
            runCatching { catalogRepository.getLiveCategoriesSnapshot() }
                .onSuccess { categories -> applyCategories(categories) }
                .onFailure { error ->
                    if (!initialApplied) {
                        _uiState.value = LiveTvUiState(
                            categoriesLoading = false,
                            errorMessage = error.userMessage("Impossible de charger les categories Xtream."),
                        )
                    }
                }
            }
    }

    private fun applyCategories(categoriesSnapshot: List<Category>) {
        localCategories = categoriesSnapshot
        val categories = localCategories.map { it.toUiCategory() }
            .filter { category -> playerSettings.allowsContent(category.label) }
        if (categories.isEmpty()) {
            _uiState.value = LiveTvUiState(
                categoriesLoading = false,
                errorMessage = "Aucune categorie Live TV retournee par Xtream.",
            )
            return
        }

        _uiState.update {
            val visibleCategories = categories.withSpecialCategories(
                allCount = catalogTotalCount(),
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
    }

    fun refreshEpgCategoryAvailability() {
        if (localCategories.isEmpty()) return
        viewModelScope.launch {
            val epgCategoryIds = withContext(Dispatchers.IO) {
                val ids = mutableSetOf<String>()
                var offset = 0
                do {
                    val page = catalogRepository.getAllLiveChannelsPage(offset, EpgCategoryScanPageSize)
                    page.forEach { stream ->
                        if (epgRepository.hasPrograms(stream.epgChannelId, stream.name)) {
                            stream.categoryId?.let(ids::add)
                        }
                    }
                    offset += page.size
                } while (page.size == EpgCategoryScanPageSize)
                ids
            }
            _uiState.update { state ->
                state.copy(
                    categories = state.categories.map { category ->
                        if (category.id in SpecialLiveCategoryIds) {
                            category.copy(hasEpg = false)
                        } else {
                            category.copy(hasEpg = category.id in epgCategoryIds)
                        }
                    },
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

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val changed = playerSettings.parentalControlEnabled != settings.parentalControlEnabled ||
                    playerSettings.parentalKeywords != settings.parentalKeywords
                playerSettings = settings
                if (changed && !_uiState.value.categoriesLoading) {
                    loadCategories()
                }
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            userContentRepository.observeFavoriteIds(UserContentType.Live).collect { ids ->
                favoriteIds = ids
                val favoriteSelection = if (_uiState.value.selectedCategoryId == FavoriteLiveCategoryId) {
                    favoriteChannels()
                } else {
                    null
                }
                _uiState.update { state ->
                    val refreshedChannels = favoriteSelection
                        ?: state.channels.map { it.copy(isFavorite = it.streamId in ids) }
                    state.copy(
                        categories = state.categories.withSpecialCategories(
                            allCount = catalogTotalCount(),
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
                            allCount = catalogTotalCount(),
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
        channelsJob = viewModelScope.launch {
            val channels = favoriteChannels()
            _uiState.update { state ->
                state.copy(
                    itemsLoading = false,
                    channelsLoading = false,
                    nextPageLoading = false,
                    hasMoreItems = false,
                    currentOffset = channels.size,
                    errorMessage = null,
                    selectedCategoryId = FavoriteLiveCategoryId,
                    categories = state.categories.withSpecialCategories(
                        allCount = catalogTotalCount(),
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
    }

    private fun loadHistoryChannels() {
        channelsJob?.cancel()
        channelsJob = viewModelScope.launch {
            val channels = historyChannels()
            _uiState.update { state ->
                state.copy(
                    channelsLoading = false,
                    itemsLoading = false,
                    nextPageLoading = false,
                    hasMoreItems = false,
                    currentOffset = channels.size,
                    errorMessage = null,
                    selectedCategoryId = HistoryLiveCategoryId,
                    categories = state.categories.withSpecialCategories(
                        allCount = catalogTotalCount(),
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
    }

    private fun loadAllChannels() {
        loadChannelPage(categoryId = null, selectedCategoryId = AllLiveCategoryId, categoryLabel = "Live TV", replace = true)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.categoriesLoading || state.itemsLoading || state.nextPageLoading || !state.hasMoreItems) return
        val selectedCategoryId = state.selectedCategoryId ?: return
        if (selectedCategoryId in setOf(FavoriteLiveCategoryId, HistoryLiveCategoryId)) return
        val categoryId = selectedCategoryId.takeUnless { it == AllLiveCategoryId }
        val categoryLabel = state.selectedCategory?.label ?: "Live TV"
        loadChannelPage(
            categoryId = categoryId,
            selectedCategoryId = selectedCategoryId,
            categoryLabel = categoryLabel,
            replace = false,
        )
    }

    fun deleteHistoryChannel(channel: LiveTvChannel) {
        viewModelScope.launch {
            userContentRepository.deleteProgress(UserContentType.Live, channel.streamId)
        }
    }

    private suspend fun historyChannels(): List<LiveTvChannel> {
        val historyIds = historyProgress.mapNotNull { it.contentId.toIntOrNull() }
        val localById = catalogRepository.getLiveChannelsByIds(historyIds).associateBy { it.streamId }
        return historyProgress.mapIndexedNotNull { index, progress ->
            val id = progress.contentId.toIntOrNull() ?: return@mapIndexedNotNull null
            val local = localById[id]
            if (local != null) {
                val categoryLabel = local.categoryName.ifBlank { progress.subtitle ?: "Historique" }
                return@mapIndexedNotNull local.toUiChannel(
                    index = index,
                    categoryLabel = categoryLabel,
                    xtreamRepository = xtreamRepository,
                    epgRepository = epgRepository,
                    favoriteIds = favoriteIds,
                )
            }
            val name = progress.title?.takeUnless { it.isGeneratedLiveTitle(id) }
                ?: "Chaine $id"
            val categoryLabel = progress.subtitle
                ?: "Historique"
            if (!playerSettings.allowsContent(name, categoryLabel, progress.title, progress.subtitle)) {
                return@mapIndexedNotNull null
            }
            LiveTvChannel(
                streamId = id,
                number = (index + 1).toString().padStart(3, '0'),
                logoText = name.logoFallback(),
                logoUrl = progress.imageUrl,
                name = name,
                program = "Direct",
                genre = categoryLabel,
                timeRange = "Live",
                progress = 0f,
                description = "Chaine deja regardee.",
                nextProgram = "EPG non disponible",
                nextTimeRange = "A suivre",
                streamUrl = xtreamRepository.buildLiveStreamUrl(id),
                fallbackStreamUrl = xtreamRepository.buildLiveStreamFallbackUrl(id),
                isFavorite = id in favoriteIds,
            )
        }
    }

    private suspend fun favoriteChannels(): List<LiveTvChannel> =
        catalogRepository.getLiveChannelsByIds(favoriteIds.toList())
            .filter { stream ->
                playerSettings.allowsContent(stream.name, stream.categoryName)
            }
            .sortedBy { it.name }
            .mapIndexed { index, stream ->
                stream.toUiChannel(
                    index = index,
                    categoryLabel = stream.categoryName.ifBlank { "Favoris" },
                    xtreamRepository = xtreamRepository,
                    epgRepository = epgRepository,
                    favoriteIds = favoriteIds,
                )
            }

    private fun loadChannels(categoryId: String) {
        val categoryLabel = _uiState.value.categories.firstOrNull { it.id == categoryId }?.label ?: "Live TV"
        loadChannelPage(categoryId = categoryId, selectedCategoryId = categoryId, categoryLabel = categoryLabel, replace = true)
    }

    private fun loadChannelPage(
        categoryId: String?,
        selectedCategoryId: String,
        categoryLabel: String,
        replace: Boolean,
    ) {
        if (replace) channelsJob?.cancel()
        channelsJob = viewModelScope.launch {
            val startOffset = if (replace) 0 else _uiState.value.currentOffset
            val previousChannels = if (replace) emptyList() else _uiState.value.channels
            _uiState.update { state ->
                state.copy(
                    itemsLoading = replace,
                    channelsLoading = replace,
                    nextPageLoading = !replace,
                    errorMessage = null,
                    selectedCategoryId = selectedCategoryId,
                    channels = if (replace) emptyList() else state.channels,
                    focusedChannelId = if (replace) null else state.focusedChannelId,
                    selectedChannelId = if (replace) null else state.selectedChannelId,
                )
            }

            runCatching {
                val page = if (categoryId == null) {
                    catalogRepository.getAllLiveChannelsPage(startOffset, LiveItemsPageSize)
                } else {
                    catalogRepository.getLiveChannelsPage(categoryId, startOffset, LiveItemsPageSize)
                }
                val visiblePage = page
                    .filter { stream -> playerSettings.allowsContent(stream.name, categoryLabel) }
                    .mapIndexed { index, stream ->
                        stream.toUiChannel(
                            index = startOffset + index,
                            categoryLabel = stream.categoryName.ifBlank { categoryLabel },
                            xtreamRepository = xtreamRepository,
                            epgRepository = epgRepository,
                            favoriteIds = favoriteIds,
                        )
                    }
                val merged = (previousChannels + visiblePage).distinctBy { it.streamId }
                PageLoadResult(items = merged, rawPageSize = page.size)
            }.onSuccess { result ->
                _uiState.update { state ->
                    val refreshedCategories = state.categories.withSpecialCategories(
                        allCount = catalogTotalCount(),
                        favoriteCount = favoriteIds.size,
                        historyCount = historyProgress.size,
                        historySignals = historyCategorySignals,
                    )
                    state.copy(
                        itemsLoading = false,
                        channelsLoading = false,
                        nextPageLoading = false,
                        hasMoreItems = result.rawPageSize == LiveItemsPageSize,
                        currentOffset = startOffset + result.rawPageSize,
                        categories = refreshedCategories,
                        channels = result.items,
                        focusedChannelId = state.focusedChannelId ?: result.items.firstOrNull()?.streamId,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        itemsLoading = false,
                        channelsLoading = false,
                        nextPageLoading = false,
                        channels = if (replace) emptyList() else state.channels,
                        focusedChannelId = if (replace) null else state.focusedChannelId,
                        selectedChannelId = if (replace) null else state.selectedChannelId,
                        errorMessage = error.userMessage("Impossible de charger les chaines Xtream."),
                    )
                }
            }
        }
    }

    private fun catalogTotalCount(): Int? =
        localCategories.sumOf { it.count }.takeIf { it > 0 }
}

private data class PageLoadResult<T>(
    val items: List<T>,
    val rawPageSize: Int,
)

private const val LiveItemsPageSize = 96
private const val EpgCategoryScanPageSize = 500
private const val InitialCategoryLimit = 20
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

private fun Category.toUiCategory(): LiveTvCategory =
    LiveTvCategory(
        id = id,
        label = name,
        count = count,
        kind = name.categoryKind(),
    )

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

private fun LocalLiveChannel.toUiChannel(
    index: Int,
    categoryLabel: String,
    xtreamRepository: XtreamRepository,
    epgRepository: EpgRepository,
    favoriteIds: Set<Int>,
): LiveTvChannel {
    val displayName = name.cleanedChannelName()
    val epgPrograms = epgRepository.loadPrograms(epgChannelId, name).map {
        LiveTvProgram(
            title = it.title,
            timeRange = it.timeRange,
            description = it.description,
        )
    }
    val current = epgPrograms.firstOrNull()
    val next = epgPrograms.drop(1).firstOrNull()
    return LiveTvChannel(
        streamId = streamId,
        number = (number.takeIf { it > 0 } ?: (index + 1)).toString().padStart(3, '0'),
        logoText = displayName.logoFallback(),
        logoUrl = logoUrl,
        name = displayName,
        program = current?.title ?: currentProgram ?: "Direct",
        genre = categoryLabel,
        timeRange = current?.timeRange ?: timeRange ?: "Live",
        progress = 0f,
        description = current?.description?.takeIf { it.isNotBlank() } ?: "Chaine issue de la derniere synchronisation locale.",
        nextProgram = next?.title ?: "EPG non disponible",
        nextTimeRange = next?.timeRange ?: "A suivre",
        epgPrograms = epgPrograms,
        streamUrl = directStreamUrl?.takeIf { it.isNotBlank() } ?: xtreamRepository.buildLiveStreamUrl(streamId),
        fallbackStreamUrl = directStreamUrl?.takeIf { it.isNotBlank() } ?: xtreamRepository.buildLiveStreamFallbackUrl(streamId),
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
