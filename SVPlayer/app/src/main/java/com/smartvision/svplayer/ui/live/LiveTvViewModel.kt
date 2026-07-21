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
import com.smartvision.svplayer.ui.catalog.AllCategoryPolicy
import com.smartvision.svplayer.ui.catalog.CatalogCategoryFilterEntry
import com.smartvision.svplayer.ui.catalog.CategoryFilter
import com.smartvision.svplayer.ui.catalog.CategoryFilterResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

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

enum class LiveSortMode(val label: String) {
    DEFAULT("Ordre du fournisseur"), NAME_ASC("Nom A - Z"), NAME_DESC("Nom Z - A"), FAVORITES_FIRST("Favoris en premier"),
}

data class LiveTvUiState(
    val categoriesLoading: Boolean = true,
    val itemsLoading: Boolean = false,
    val channelsLoading: Boolean = false,
    val nextPageLoading: Boolean = false,
    val hasMoreItems: Boolean = false,
    val currentOffset: Int = 0,
    val errorMessage: String? = null,
    val categories: List<LiveTvCategory> = emptyList(),
    val categoryFilters: List<CategoryFilter> = emptyList(),
    val activeCategoryFilterCode: String? = null,
    val selectedCategoryId: String? = null,
    val channels: List<LiveTvChannel> = emptyList(),
    val matchingChannelCount: Int = 0,
    val channelSearchQuery: String = "",
    val focusedChannelId: Int? = null,
    val selectedChannelId: Int? = null,
    val sortMode: LiveSortMode = LiveSortMode.DEFAULT,
) {
    val displayedChannels: List<LiveTvChannel>
        get() = if (sortMode == LiveSortMode.DEFAULT) channels else channels.sortedWith(sortMode.comparator())
    val visibleCategories: List<LiveTvCategory>
        get() {
            val filteredEntries = CategoryFilterResolver.filterEntries(
                categories = categories.map(LiveTvCategory::toFilterEntry),
                normalizedCode = activeCategoryFilterCode,
                allCategoryId = AllLiveCategoryId,
            )
            val byId = categories.associateBy(LiveTvCategory::id)
            return filteredEntries.mapNotNull { entry -> byId[entry.id]?.copy(count = entry.count) }
        }

    val selectedCategory: LiveTvCategory?
        get() = categories.firstOrNull { it.id == selectedCategoryId }

    val selectedChannel: LiveTvChannel?
        get() = channels.firstOrNull { it.streamId == selectedChannelId }

    val focusedChannel: LiveTvChannel?
        get() = channels.firstOrNull { it.streamId == focusedChannelId }

    val isHistoryCategory: Boolean
        get() = selectedCategoryId == HistoryLiveCategoryId
}

class LiveTvViewModel(
    private val xtreamRepository: XtreamRepository,
    private val catalogRepository: CatalogRepository,
    private val userContentRepository: UserContentRepository,
    private val settingsRepository: SettingsRepository,
    private val epgRepository: EpgRepository,
    private val epgUrlProvider: () -> String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LiveTvUiState())
    val uiState: StateFlow<LiveTvUiState> = _uiState.asStateFlow()

    private var channelsJob: Job? = null
    private var categoriesJob: Job? = null
    private var favoriteIds: Set<Int> = emptySet()
    private var historyProgress: List<PlaybackProgressEntity> = emptyList()
    private var historyCategorySignals: List<CategoryHistorySignal> = emptyList()
    private var playerSettings = PlayerSettings()
    private var localCategories: List<Category> = emptyList()
    private var pendingHistoryFocusAfterDelete: Int? = null
    private var userSelectedCategory = false
    private var autoSelectedCategoryId: String? = null
    private var loadedChannelsCategoryId: String? = null
    private var observedCatalogRevision: Long? = null

    fun setSortMode(mode: LiveSortMode) {
        _uiState.update { it.copy(sortMode = mode) }
    }

    init {
        observeCatalogRevision()
        observeSettings()
        observeFavorites()
        observeHistory()
        loadCategories()
    }

    private fun observeCatalogRevision() {
        viewModelScope.launch {
            catalogRepository.catalogRevision.collect { revision ->
                val previous = observedCatalogRevision
                observedCatalogRevision = revision
                if (previous != null && previous != revision) {
                    reloadCatalogAfterRevision()
                }
            }
        }
    }

    private fun reloadCatalogAfterRevision() {
        val previousFilterCode = _uiState.value.activeCategoryFilterCode
        categoriesJob?.cancel()
        cancelChannelsLoad(clearChannels = true)
        localCategories = emptyList()
        pendingHistoryFocusAfterDelete = null
        userSelectedCategory = false
        autoSelectedCategoryId = null
        loadedChannelsCategoryId = null
        _uiState.value = LiveTvUiState(
            categoriesLoading = true,
            activeCategoryFilterCode = previousFilterCode,
        )
        loadCategories()
    }

    fun loadCategories(reloadChannels: Boolean = false) {
        categoriesJob?.cancel()
        if (reloadChannels) cancelChannelsLoad(clearChannels = true)
        val cachedCategories = catalogRepository.getCachedLiveCategories()
        if (!cachedCategories.isNullOrEmpty()) {
            applyCategories(cachedCategories)
            return
        }
        categoriesJob = viewModelScope.launch {
            _uiState.update { it.copy(categoriesLoading = true, errorMessage = null) }
            var initialApplied = false
            runCatchingNonCancellation { catalogRepository.getInitialLiveCategoriesSnapshot(InitialCategoryLimit) }
                .onSuccess { categories ->
                    if (categories.isNotEmpty()) {
                        initialApplied = true
                        applyCategories(categories)
                    }
                }
            runCatchingNonCancellation { catalogRepository.getLiveCategoriesSnapshot() }
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

        val previousSelectedCategoryId = _uiState.value.selectedCategoryId
        _uiState.update {
            val visibleCategories = categories.withSpecialCategories(
                allCount = catalogTotalCount(),
                favoriteCount = favoriteIds.size,
                historyCount = historyProgress.size,
                historySignals = historyCategorySignals,
            )
            val filters = CategoryFilterResolver.buildFilters(visibleCategories.map { category -> category.toFilterEntry() })
            val activeFilterCode = it.activeCategoryFilterCode
                ?.takeIf { code -> filters.any { filter -> filter.identity.normalizedCode == code } }
            val filteredEntries = CategoryFilterResolver.filterEntries(
                categories = visibleCategories.map(LiveTvCategory::toFilterEntry),
                normalizedCode = activeFilterCode,
                allCategoryId = AllLiveCategoryId,
            )
            val categoriesById = visibleCategories.associateBy(LiveTvCategory::id)
            val filteredCategories = filteredEntries.mapNotNull { entry ->
                categoriesById[entry.id]?.copy(count = entry.count)
            }
            val existingSelection = it.selectedCategoryId
                ?.takeIf { selectedId -> userSelectedCategory && filteredCategories.any { category -> category.id == selectedId } }
            val initialCategory = existingSelection
                ?.let { selectedId -> filteredCategories.firstOrNull { category -> category.id == selectedId } }
                ?: filteredCategories.firstOrNull { category -> category.id == AllLiveCategoryId }
                ?: filteredCategories.firstOrNull()
            if (!userSelectedCategory) {
                autoSelectedCategoryId = initialCategory?.id
            }
            it.copy(
                categoriesLoading = false,
                errorMessage = null,
                categories = visibleCategories,
                categoryFilters = filters,
                activeCategoryFilterCode = activeFilterCode,
                selectedCategoryId = initialCategory?.id,
            )
        }
        val state = _uiState.value
        val selectedCategoryChanged = state.selectedCategoryId != previousSelectedCategoryId
        val selectedCategoryLoadActive =
            channelsJob?.isActive == true && loadedChannelsCategoryId == state.selectedCategoryId
        if (selectedCategoryChanged || (state.channels.isEmpty() && !selectedCategoryLoadActive)) {
            state.selectedCategory?.let { category -> selectCategory(category, userInitiated = false) }
        }
    }

    fun applyCategoryFilter(normalizedCode: String?): LiveTvCategory? {
        val current = _uiState.value
        val validCode = normalizedCode?.takeIf { code ->
            current.categoryFilters.any { it.identity.normalizedCode == code }
        }
        val firstCategory = current.categories.firstOrNull { it.id == AllLiveCategoryId }
        _uiState.update { state ->
            state.copy(
                activeCategoryFilterCode = validCode,
                selectedCategoryId = null,
                channels = emptyList(),
                focusedChannelId = null,
                selectedChannelId = null,
            )
        }
        loadedChannelsCategoryId = null
        firstCategory?.let { selectCategory(it, userInitiated = true) }
        return firstCategory
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
                        if (epgRepository.hasPrograms(epgUrlProvider(), stream.epgChannelId, stream.name)) {
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

    fun selectCategory(
        category: LiveTvCategory,
        autoPreviewFirstChannel: Boolean = false,
        userInitiated: Boolean = true,
    ) {
        if (userInitiated) {
            userSelectedCategory = true
        } else {
            autoSelectedCategoryId = category.id
        }
        val current = _uiState.value
        if (
            current.selectedCategoryId == category.id &&
            loadedChannelsCategoryId == category.id &&
            (current.channels.isNotEmpty() || channelsJob?.isActive == true)
        ) {
            return
        }
        if (category.id == FavoriteLiveCategoryId) {
            loadFavoriteChannels(autoPreviewFirstChannel)
            return
        }
        if (category.id == HistoryLiveCategoryId) {
            loadHistoryChannels(autoPreviewFirstChannel)
            return
        }
        if (category.id == AllLiveCategoryId) {
            loadAllChannels(autoPreviewFirstChannel)
            return
        }
        loadChannels(category.id, autoPreviewFirstChannel)
    }

    fun focusChannel(channel: LiveTvChannel) {
        _uiState.update { state ->
            state.copy(focusedChannelId = channel.streamId)
        }
    }

    fun updateChannelSearchQuery(query: String) {
        val current = _uiState.value
        if (current.channelSearchQuery == query) return
        _uiState.update { state ->
            state.copy(
                channelSearchQuery = query,
                focusedChannelId = null,
                selectedChannelId = null,
            )
        }
        reloadCurrentChannelList()
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

    fun restoreFocusToChannel(streamId: Int) {
        viewModelScope.launch {
            val existing = _uiState.value.channels.firstOrNull { it.streamId == streamId }
            if (existing != null) {
                startPreview(existing)
                return@launch
            }
            val local = catalogRepository.getLiveChannelById(streamId) ?: return@launch
            if (!playerSettings.allowsContent(local.name, local.categoryName)) return@launch
            val channel = local.toUiChannel(
                index = _uiState.value.channels.size,
                categoryLabel = local.categoryName.ifBlank { _uiState.value.selectedCategory?.label ?: "Live TV" },
                xtreamRepository = xtreamRepository,
                epgRepository = epgRepository,
                epgUrl = epgUrlProvider(),
                favoriteIds = favoriteIds,
            )
            _uiState.update { state ->
                state.copy(
                    channels = (state.channels + channel).distinctBy { it.streamId },
                    selectedChannelId = channel.streamId,
                    focusedChannelId = channel.streamId,
                    errorMessage = null,
                )
            }
        }
    }

    fun toggleFavorite(channel: LiveTvChannel) {
        viewModelScope.launch {
            userContentRepository.toggleFavorite(UserContentType.Live, channel.streamId)
        }
    }

    fun refreshChannelEpg(channel: LiveTvChannel, epgUrl: String) {
        if (epgUrl.isBlank()) return
        viewModelScope.launch {
            epgRepository.synchronizeIfStale(epgUrl, EpgRefreshMinAgeMs)
            val local = catalogRepository.getLiveChannelById(channel.streamId) ?: return@launch
            val refreshed = local.toUiChannel(
                index = _uiState.value.channels.indexOfFirst { it.streamId == channel.streamId }.coerceAtLeast(0),
                categoryLabel = local.categoryName.ifBlank { _uiState.value.selectedCategory?.label ?: channel.genre },
                xtreamRepository = xtreamRepository,
                epgRepository = epgRepository,
                epgUrl = epgUrl,
                favoriteIds = favoriteIds,
            )
            _uiState.update { state ->
                state.copy(
                    channels = state.channels.map { current ->
                        if (current.streamId == refreshed.streamId) refreshed.copy(number = current.number) else current
                    },
                )
            }
        }
    }

    fun retryCurrentCategory() {
        reloadCurrentChannelList()
    }

    private fun reloadCurrentChannelList() {
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
                if (changed && (localCategories.isNotEmpty() || categoriesJob?.isActive == true)) {
                    loadCategories(reloadChannels = true)
                }
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            userContentRepository.observeFavoriteIds(UserContentType.Live).collect { ids ->
                favoriteIds = ids
                val emptySelectedFavorites = ids.isEmpty() && _uiState.value.selectedCategoryId == FavoriteLiveCategoryId
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
                if (emptySelectedFavorites) loadAllChannels()
                refreshAutomaticInitialSelectionIfNeeded()
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            userContentRepository.observeHistory(UserContentType.Live).collect { progress ->
                historyProgress = progress.map { userContentRepository.enrichProgress(it) }
                    .distinctBy { it.contentId }
                historyCategorySignals = userContentRepository.resolveCategorySignals(historyProgress)
                val emptySelectedHistory = historyProgress.isEmpty() && _uiState.value.selectedCategoryId == HistoryLiveCategoryId
                _uiState.update { state ->
                    val history = historyChannels()
                    val pendingFocusId = pendingHistoryFocusAfterDelete?.takeIf { id ->
                        history.any { it.streamId == id }
                    }
                    if (state.selectedCategoryId == HistoryLiveCategoryId) {
                        pendingHistoryFocusAfterDelete = null
                    }
                    val selectedHistoryId = when {
                        state.selectedCategoryId != HistoryLiveCategoryId -> state.selectedChannelId
                        pendingFocusId != null -> pendingFocusId
                        state.selectedChannelId?.let { selectedId -> history.any { it.streamId == selectedId } } == true -> state.selectedChannelId
                        else -> null
                    }
                    state.copy(
                        categories = state.categories.withSpecialCategories(
                            allCount = catalogTotalCount(),
                            favoriteCount = favoriteIds.size,
                            historyCount = history.size,
                            historySignals = historyCategorySignals,
                        ),
                        channels = if (state.selectedCategoryId == HistoryLiveCategoryId) history else state.channels,
                        focusedChannelId = if (state.selectedCategoryId == HistoryLiveCategoryId) {
                            pendingFocusId
                                ?: state.focusedChannelId?.takeIf { focusedId -> history.any { it.streamId == focusedId } }
                                ?: history.firstOrNull()?.streamId
                        } else {
                            state.focusedChannelId
                        },
                        selectedChannelId = selectedHistoryId,
                    )
                }
                if (emptySelectedHistory) loadAllChannels()
                refreshAutomaticInitialSelectionIfNeeded()
            }
        }
    }

    private fun refreshAutomaticInitialSelectionIfNeeded() {
        if (userSelectedCategory) return
        val state = _uiState.value
        val target = state.visibleCategories.firstOrNull { it.id == AllLiveCategoryId } ?: return
        if (target.id != state.selectedCategoryId) {
            selectCategory(target, userInitiated = false)
        }
    }

    private fun loadFavoriteChannels(autoPreviewFirstChannel: Boolean = false) {
        cancelChannelsLoad(clearChannels = true)
        loadedChannelsCategoryId = FavoriteLiveCategoryId
        channelsJob = viewModelScope.launch {
            _uiState.update {
                it.copy(itemsLoading = true, channelsLoading = true, selectedCategoryId = FavoriteLiveCategoryId)
            }
            val allChannels = favoriteChannels()
            val channels = allChannels.filterBySearch(_uiState.value.channelSearchQuery)
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
                    matchingChannelCount = channels.size,
                    focusedChannelId = channels.firstOrNull()?.streamId,
                    selectedChannelId = if (autoPreviewFirstChannel) channels.firstOrNull()?.streamId else null,
                )
            }
        }
    }

    private fun loadHistoryChannels(autoPreviewFirstChannel: Boolean = false) {
        cancelChannelsLoad(clearChannels = true)
        loadedChannelsCategoryId = HistoryLiveCategoryId
        channelsJob = viewModelScope.launch {
            _uiState.update {
                it.copy(itemsLoading = true, channelsLoading = true, selectedCategoryId = HistoryLiveCategoryId)
            }
            val allChannels = historyChannels()
            val channels = allChannels.filterBySearch(_uiState.value.channelSearchQuery)
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
                        historyCount = allChannels.size,
                        historySignals = historyCategorySignals,
                    ),
                    channels = channels,
                    matchingChannelCount = channels.size,
                    focusedChannelId = channels.firstOrNull()?.streamId,
                    selectedChannelId = if (autoPreviewFirstChannel) channels.firstOrNull()?.streamId else null,
                )
            }
        }
    }

    private fun loadAllChannels(autoPreviewFirstChannel: Boolean = false) {
        loadChannelPage(categoryId = null, selectedCategoryId = AllLiveCategoryId, categoryLabel = "Live TV", replace = true, autoPreviewFirstChannel = autoPreviewFirstChannel)
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
            autoPreviewFirstChannel = false,
        )
    }

    fun deleteHistoryChannel(channel: LiveTvChannel) {
        val channels = _uiState.value.channels
        val index = channels.indexOfFirst { it.streamId == channel.streamId }
        pendingHistoryFocusAfterDelete = channels.getOrNull(index + 1)?.streamId
            ?: channels.getOrNull(index - 1)?.streamId
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
                if (!playerSettings.allowsContent(
                        local.name,
                        local.categoryName,
                        progress.title,
                        progress.subtitle,
                    )
                ) {
                    return@mapIndexedNotNull null
                }
                return@mapIndexedNotNull local.toUiChannel(
                    index = index,
                    categoryLabel = categoryLabel,
                    xtreamRepository = xtreamRepository,
                    epgRepository = epgRepository,
                    epgUrl = epgUrlProvider(),
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
                    epgUrl = epgUrlProvider(),
                    favoriteIds = favoriteIds,
                )
            }

    private fun loadChannels(categoryId: String, autoPreviewFirstChannel: Boolean = false) {
        val categoryLabel = _uiState.value.categories.firstOrNull { it.id == categoryId }?.label ?: "Live TV"
        loadChannelPage(categoryId = categoryId, selectedCategoryId = categoryId, categoryLabel = categoryLabel, replace = true, autoPreviewFirstChannel = autoPreviewFirstChannel)
    }

    private fun loadChannelPage(
        categoryId: String?,
        selectedCategoryId: String,
        categoryLabel: String,
        replace: Boolean,
        autoPreviewFirstChannel: Boolean = false,
    ) {
        if (replace) cancelChannelsLoad(clearChannels = true)
        if (replace) loadedChannelsCategoryId = selectedCategoryId
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

            runCatchingNonCancellation {
                val searchQuery = _uiState.value.channelSearchQuery.trim()
                val filteredCategoryIds = _uiState.value.activeCategoryFilterCode?.let {
                    _uiState.value.visibleCategories
                        .asSequence()
                        .filterNot { category -> category.id in SpecialLiveCategoryIds }
                        .map { category -> category.id }
                        .toList()
                }.orEmpty()
                val orderedAllCategories = _uiState.value.visibleCategories
                    .filterNot { category -> category.id in SpecialLiveCategoryIds }
                val page = if (categoryId == null && searchQuery.isBlank()) {
                    loadAllChannelsInCategoryOrder(
                        categories = orderedAllCategories,
                        offset = startOffset,
                        limit = LiveItemsPageSize,
                    )
                } else if (categoryId == null && filteredCategoryIds.isNotEmpty()) {
                    catalogRepository.getLiveChannelsByCategoryIdsPage(
                        categoryIds = filteredCategoryIds,
                        query = searchQuery,
                        offset = startOffset,
                        limit = LiveItemsPageSize,
                    )
                } else if (searchQuery.isNotBlank()) {
                    catalogRepository.searchLiveChannelsPage(categoryId, searchQuery, startOffset, LiveItemsPageSize)
                } else if (categoryId == null) {
                    catalogRepository.getAllLiveChannelsPage(startOffset, LiveItemsPageSize)
                } else {
                    catalogRepository.getLiveChannelsPage(categoryId, startOffset, LiveItemsPageSize)
                }
                val matchingCount = if (categoryId == null && filteredCategoryIds.isNotEmpty()) {
                    catalogRepository.countLiveChannelsByCategoryIds(filteredCategoryIds, searchQuery)
                } else {
                    catalogRepository.countLiveChannels(categoryId, searchQuery)
                }
                val visiblePage = page
                    .filter { stream -> playerSettings.allowsContent(stream.name, categoryLabel) }
                    .mapIndexed { index, stream ->
                        stream.toUiChannel(
                            index = startOffset + index,
                            categoryLabel = stream.categoryName.ifBlank { categoryLabel },
                            xtreamRepository = xtreamRepository,
                            epgRepository = epgRepository,
                            epgUrl = epgUrlProvider(),
                            favoriteIds = favoriteIds,
                        )
                    }
                val merged = (previousChannels + visiblePage).distinctBy { it.streamId }
                PageLoadResult(items = merged, rawPageSize = page.size, matchingCount = matchingCount)
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
                        matchingChannelCount = result.matchingCount,
                        focusedChannelId = state.focusedChannelId ?: result.items.firstOrNull()?.streamId,
                        selectedChannelId = if (replace && autoPreviewFirstChannel) {
                            result.items.firstOrNull()?.streamId
                        } else {
                            state.selectedChannelId
                        },
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

    private suspend fun loadAllChannelsInCategoryOrder(
        categories: List<LiveTvCategory>,
        offset: Int,
        limit: Int,
    ): List<LocalLiveChannel> {
        if (categories.isEmpty()) {
            return catalogRepository.getAllLiveChannelsPage(offset, limit)
        }
        var remainingOffset = offset
        val result = mutableListOf<LocalLiveChannel>()
        for (category in categories) {
            val categoryCount = category.count
            if (categoryCount != null && remainingOffset >= categoryCount) {
                remainingOffset -= categoryCount
                continue
            }
            val pageLimit = limit - result.size
            if (pageLimit <= 0) break
            val page = catalogRepository.getLiveChannelsPage(category.id, remainingOffset, pageLimit)
            result += page
            remainingOffset = 0
            if (result.size >= limit) break
        }
        return result
    }

    private fun cancelChannelsLoad(clearChannels: Boolean) {
        channelsJob?.cancel()
        channelsJob = null
        loadedChannelsCategoryId = null
        _uiState.update { state ->
            state.copy(
                itemsLoading = false,
                channelsLoading = false,
                nextPageLoading = false,
                channels = if (clearChannels) emptyList() else state.channels,
                currentOffset = if (clearChannels) 0 else state.currentOffset,
                hasMoreItems = if (clearChannels) false else state.hasMoreItems,
            )
        }
    }

    private fun catalogTotalCount(): Int? =
        localCategories.sumOf { it.count }.takeIf { it > 0 }
}

private fun LiveSortMode.comparator(): Comparator<LiveTvChannel> = when (this) {
    LiveSortMode.DEFAULT -> compareBy { it.number.toIntOrNull() ?: Int.MAX_VALUE }
    LiveSortMode.NAME_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    LiveSortMode.NAME_DESC -> compareByDescending<LiveTvChannel> { it.name.lowercase() }
    LiveSortMode.FAVORITES_FIRST -> compareByDescending<LiveTvChannel> { it.isFavorite }
}.thenBy { it.streamId }

private data class PageLoadResult<T>(
    val items: List<T>,
    val rawPageSize: Int,
    val matchingCount: Int,
)

private fun List<LiveTvChannel>.filterBySearch(query: String): List<LiveTvChannel> {
    val cleanQuery = query.trim()
    if (cleanQuery.isBlank()) return this
    return filter { channel -> channel.name.contains(cleanQuery, ignoreCase = true) }
}

private const val LiveItemsPageSize = 96
private const val EpgCategoryScanPageSize = 500
private const val InitialCategoryLimit = 20
private val EpgRefreshMinAgeMs = TimeUnit.HOURS.toMillis(1)
private const val FavoriteLiveCategoryId = "__favorites_live__"
private const val HistoryLiveCategoryId = "__history_live__"
internal const val AllLiveCategoryId = "__all_live__"
internal val SpecialLiveCategoryIds = setOf(AllLiveCategoryId, FavoriteLiveCategoryId, HistoryLiveCategoryId)

private fun LiveTvCategory.toFilterEntry(): CatalogCategoryFilterEntry =
    CatalogCategoryFilterEntry(
        id = id,
        label = label,
        count = count,
        special = id in SpecialLiveCategoryIds,
    )

private fun List<LiveTvCategory>.withSpecialCategories(
    allCount: Int?,
    favoriteCount: Int,
    historyCount: Int,
    historySignals: List<CategoryHistorySignal> = emptyList(),
): List<LiveTvCategory> =
    buildList {
        add(
            LiveTvCategory(
                id = AllLiveCategoryId,
                label = "ALL",
                count = allCount,
                kind = LiveTvCategoryKind.Generic,
            ),
        )
        if (favoriteCount > 0) {
            add(LiveTvCategory(FavoriteLiveCategoryId, "Favoris", favoriteCount, LiveTvCategoryKind.Generic))
        }
        if (historyCount > 0) {
            add(LiveTvCategory(HistoryLiveCategoryId, "Historique", historyCount, LiveTvCategoryKind.Generic))
        }
        addAll(
            filterNot { it.id in SpecialLiveCategoryIds || AllCategoryPolicy.isEquivalent(it.label) }
                .sortedByHistorySignals(historySignals) { it.id },
        )
    }

private suspend fun <T> runCatchingNonCancellation(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }

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
        number = (index + 1).toString().padStart(3, '0'),
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
    epgUrl: String,
    favoriteIds: Set<Int>,
): LiveTvChannel {
    val displayName = name.cleanedChannelName()
    val epgPrograms = epgRepository.loadPrograms(epgUrl, epgChannelId, name).map {
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
        number = (index + 1).toString().padStart(3, '0'),
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
