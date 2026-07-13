package com.smartvision.svplayer.ui.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.models.XtreamSeriesCategory
import com.smartvision.svplayer.data.models.XtreamSeriesEpisode
import com.smartvision.svplayer.data.models.XtreamSeriesStream
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.UserContentType
import com.smartvision.svplayer.data.repository.XtreamRepository
import com.smartvision.svplayer.data.tmdb.TmdbRepository
import com.smartvision.svplayer.domain.model.Category
import com.smartvision.svplayer.domain.model.CategoryHistorySignal
import com.smartvision.svplayer.domain.model.Episode
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.domain.model.TvSeries
import com.smartvision.svplayer.domain.model.sortedByHistorySignals
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.domain.repository.SettingsRepository
import com.smartvision.svplayer.ui.settings.allowsContent
import com.smartvision.svplayer.ui.catalog.AllCategoryPolicy
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeriesCategoryUi(
    val id: String,
    val label: String,
    val count: Int?,
)

data class SeriesItemUi(
    val seriesId: Int,
    val number: String,
    val title: String,
    val coverUrl: String?,
    val backdropUrl: String? = null,
    val categoryLabel: String,
    val plot: String?,
    val genre: String?,
    val releaseDate: String?,
    val rating: String?,
    val episodeRunTime: String?,
    val seasonsCount: Int? = null,
    val episodesCount: Int? = null,
    val preferredEpisodeId: Int? = null,
    val createdBy: String? = null,
    val cast: String? = null,
    val isFavorite: Boolean = false,
) {
    val subtitle: String =
        listOfNotNull(releaseDate?.take(4), genre, rating).joinToString(" | ").ifBlank { categoryLabel }
}

enum class SeriesSortMode(val label: String) {
    DEFAULT("Ordre par defaut"), TITLE_ASC("Titre A - Z"), TITLE_DESC("Titre Z - A"), NEWEST("Date de sortie"), RATING("Mieux notees"),
}

data class SeriesEpisodeUi(
    val episodeId: Int,
    val seriesId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val duration: String?,
    val plot: String?,
    val streamUrl: String,
    val progressPercent: Int = 0,
    val resumePositionMs: Long = 0L,
) {
    val number: String = "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
}

data class SeriesScreenState(
    val categoriesLoading: Boolean = true,
    val itemsLoading: Boolean = false,
    val seriesLoading: Boolean = false,
    val nextPageLoading: Boolean = false,
    val hasMoreItems: Boolean = false,
    val currentOffset: Int = 0,
    val episodesLoading: Boolean = false,
    val errorMessage: String? = null,
    val categories: List<SeriesCategoryUi> = emptyList(),
    val selectedCategoryId: String? = null,
    val contentSearchQuery: String = "",
    val series: List<SeriesItemUi> = emptyList(),
    val focusedSeriesId: Int? = null,
    val selectedSeriesId: Int? = null,
    val episodes: List<SeriesEpisodeUi> = emptyList(),
    val selectedPreviewEpisodeId: Int? = null,
    val sortMode: SeriesSortMode = SeriesSortMode.DEFAULT,
) {
    val displayedSeries: List<SeriesItemUi>
        get() = series.sortedWith(sortMode.comparator())
    val selectedCategory: SeriesCategoryUi?
        get() = categories.firstOrNull { it.id == selectedCategoryId }

    val selectedSeries: SeriesItemUi?
        get() = series.firstOrNull { it.seriesId == selectedSeriesId }

    val firstEpisode: SeriesEpisodeUi?
        get() = episodes.firstOrNull()

    val selectedPreviewEpisode: SeriesEpisodeUi?
        get() {
            selectedPreviewEpisodeId?.let { selectedId ->
                episodes.firstOrNull { it.episodeId == selectedId }?.let { return it }
            }
            val preferredEpisodeId = selectedSeries?.preferredEpisodeId
            return episodes.firstOrNull { it.episodeId == preferredEpisodeId } ?: firstEpisode
        }
}

class SeriesViewModel(
    private val xtreamRepository: XtreamRepository,
    private val catalogRepository: CatalogRepository,
    private val userContentRepository: UserContentRepository,
    private val settingsRepository: SettingsRepository,
    private val tmdbRepository: TmdbRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SeriesScreenState())
    val uiState: StateFlow<SeriesScreenState> = _uiState.asStateFlow()

    private var seriesJob: Job? = null
    private var episodesJob: Job? = null
    private var metadataJob: Job? = null
    private var favoriteIds: Set<Int> = emptySet()
    private var historyProgress: List<PlaybackProgressEntity> = emptyList()
    private var historyCategorySignals: List<CategoryHistorySignal> = emptyList()
    private var playerSettings = PlayerSettings()
    private var localCategories: List<Category> = emptyList()
    private var observedCatalogRevision: Long? = null

    init {
        observeCatalogRevision()
        observeSettings()
        observeFavorites()
        observeHistory()
        loadCategories()
    }

    fun setSortMode(mode: SeriesSortMode) {
        _uiState.update { it.copy(sortMode = mode) }
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
        seriesJob?.cancel()
        episodesJob?.cancel()
        metadataJob?.cancel()
        localCategories = emptyList()
        _uiState.value = SeriesScreenState(categoriesLoading = true)
        loadCategories()
    }

    fun loadCategories() {
        seriesJob?.cancel()
        episodesJob?.cancel()
        metadataJob?.cancel()
        val cachedCategories = catalogRepository.getCachedSeriesCategories()
        if (!cachedCategories.isNullOrEmpty()) {
            applyCategories(cachedCategories)
            return
        }
        viewModelScope.launch {
            _uiState.value = SeriesScreenState(categoriesLoading = true)
            var initialApplied = false
            runCatching { catalogRepository.getInitialSeriesCategoriesSnapshot(InitialCategoryLimit) }
                .onSuccess { categories ->
                    if (categories.isNotEmpty()) {
                        initialApplied = true
                        applyCategories(categories)
                    }
                }
            runCatching { catalogRepository.getSeriesCategoriesSnapshot() }
                .onSuccess { categories -> applyCategories(categories) }
                .onFailure { error ->
                    if (!initialApplied) {
                        _uiState.value = SeriesScreenState(
                            categoriesLoading = false,
                            errorMessage = error.userMessage("Impossible de charger les categories Series."),
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
            _uiState.value = SeriesScreenState(
                categoriesLoading = false,
                errorMessage = "Aucune categorie Series retournee par Xtream.",
            )
            return
        }
        _uiState.update {
            val visibleCategories = categories.withSpecialCategories(
                allCount = catalogTotalCount(),
                favoriteCount = favoriteIds.size,
                historyCount = historySeries().size,
                historySignals = historyCategorySignals,
            )
            val initialCategory = visibleCategories.initialCategoryForPlaylist()
            it.copy(
                categoriesLoading = false,
                categories = visibleCategories,
                selectedCategoryId = initialCategory?.id,
                errorMessage = null,
            )
        }
        when (_uiState.value.selectedCategoryId) {
            HistorySeriesCategoryId -> loadHistorySeries()
            FavoriteSeriesCategoryId -> loadFavoriteSeries()
            AllSeriesCategoryId, null -> loadAllSeries()
            else -> _uiState.value.selectedCategoryId?.let(::loadSeries)
        }
    }

    fun selectCategory(category: SeriesCategoryUi) {
        val current = _uiState.value
        if (current.selectedCategoryId == category.id && (current.series.isNotEmpty() || current.seriesLoading)) {
            return
        }
        if (category.id == FavoriteSeriesCategoryId) {
            loadFavoriteSeries()
            return
        }
        if (category.id == HistorySeriesCategoryId) {
            loadHistorySeries()
            return
        }
        if (category.id == AllSeriesCategoryId) {
            loadAllSeries()
            return
        }
        loadSeries(category.id)
    }

    fun updateContentSearchQuery(query: String) {
        val cleanQuery = query.trim()
        val current = _uiState.value
        if (current.contentSearchQuery == cleanQuery) return
        _uiState.update { it.copy(contentSearchQuery = cleanQuery) }
        when (current.selectedCategoryId) {
            FavoriteSeriesCategoryId -> loadFavoriteSeries()
            HistorySeriesCategoryId -> loadHistorySeries()
            AllSeriesCategoryId, null -> loadAllSeries()
            else -> current.selectedCategoryId?.let(::loadSeries)
        }
    }

    fun focusSeries(series: SeriesItemUi) {
        _uiState.update { it.copy(focusedSeriesId = series.seriesId) }
    }

    fun activateSeries(series: SeriesItemUi): Int? {
        val state = _uiState.value
        if (state.selectedSeriesId == series.seriesId) {
            _uiState.update { it.copy(focusedSeriesId = series.seriesId, errorMessage = null) }
            return state.selectedPreviewEpisode?.episodeId
        }
        selectSeries(series)
        return null
    }

    fun selectSeries(series: SeriesItemUi) {
        if (_uiState.value.selectedSeriesId == series.seriesId && _uiState.value.episodes.isNotEmpty()) {
            _uiState.update { it.copy(focusedSeriesId = series.seriesId, errorMessage = null) }
            return
        }
        val resumeEpisodeId = historyProgress.firstOrNull { progress ->
            progress.parentContentId?.toIntOrNull() == series.seriesId
        }?.contentId?.toIntOrNull()
        _uiState.update {
            it.copy(
                selectedSeriesId = series.seriesId,
                focusedSeriesId = series.seriesId,
                episodes = emptyList(),
                selectedPreviewEpisodeId = resumeEpisodeId ?: series.preferredEpisodeId,
                errorMessage = null,
            )
        }
        loadEpisodes(series.seriesId)
    }

    fun selectPreviewEpisode(episodeId: Int) {
        if (_uiState.value.episodes.none { it.episodeId == episodeId }) return
        _uiState.update { it.copy(selectedPreviewEpisodeId = episodeId, errorMessage = null) }
    }

    fun toggleFavorite(series: SeriesItemUi) {
        viewModelScope.launch {
            userContentRepository.toggleFavorite(UserContentType.Series, series.seriesId)
        }
    }

    fun retryCurrentCategory() {
        val categoryId = _uiState.value.selectedCategoryId
        if (categoryId == null) {
            loadCategories()
        } else if (categoryId == FavoriteSeriesCategoryId) {
            loadFavoriteSeries()
        } else if (categoryId == HistorySeriesCategoryId) {
            loadHistorySeries()
        } else if (categoryId == AllSeriesCategoryId) {
            loadAllSeries()
        } else {
            loadSeries(categoryId)
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
            userContentRepository.observeFavoriteIds(UserContentType.Series).collect { ids ->
                favoriteIds = ids
                val favoriteSelection = if (_uiState.value.selectedCategoryId == FavoriteSeriesCategoryId) {
                    favoriteSeries(_uiState.value.contentSearchQuery)
                } else {
                    null
                }
                _uiState.update { state ->
                    val refreshedSeries = favoriteSelection
                        ?: state.series.map { it.copy(isFavorite = it.seriesId in ids) }
                    state.copy(
                        categories = state.categories.withSpecialCategories(
                            allCount = catalogTotalCount(),
                            favoriteCount = ids.size,
                            historyCount = historySeries().size,
                            historySignals = historyCategorySignals,
                        ),
                        series = refreshedSeries,
                        focusedSeriesId = state.focusedSeriesId
                            ?.takeIf { focusedId -> refreshedSeries.any { it.seriesId == focusedId } }
                            ?: refreshedSeries.firstOrNull()?.seriesId,
                        selectedSeriesId = state.selectedSeriesId
                            ?.takeIf { selectedId -> refreshedSeries.any { it.seriesId == selectedId } },
                        episodes = state.episodes.takeIf { state.selectedSeriesId != null && refreshedSeries.any { series -> series.seriesId == state.selectedSeriesId } }
                            ?: emptyList(),
                    )
                }
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            userContentRepository.observeHistory(UserContentType.Episode).collect { progress ->
                historyProgress = progress.map { userContentRepository.enrichProgress(it) }
                    .distinctBy { it.parentContentId ?: it.contentId }
                historyCategorySignals = userContentRepository.resolveCategorySignals(historyProgress)
                _uiState.update { state ->
                    val history = historySeries(state.contentSearchQuery)
                    state.copy(
                        categories = state.categories.withSpecialCategories(
                            allCount = catalogTotalCount(),
                            favoriteCount = favoriteIds.size,
                            historyCount = history.size,
                            historySignals = historyCategorySignals,
                        ),
                        series = if (state.selectedCategoryId == HistorySeriesCategoryId) history else state.series,
                        focusedSeriesId = if (state.selectedCategoryId == HistorySeriesCategoryId) history.firstOrNull()?.seriesId else state.focusedSeriesId,
                    )
                }
            }
        }
    }

    private fun loadFavoriteSeries() {
        seriesJob?.cancel()
        episodesJob?.cancel()
        metadataJob?.cancel()
        seriesJob = viewModelScope.launch {
            val series = favoriteSeries(_uiState.value.contentSearchQuery)
            _uiState.update { state ->
                state.copy(
                    itemsLoading = false,
                    seriesLoading = false,
                    nextPageLoading = false,
                    hasMoreItems = false,
                    currentOffset = series.size,
                    episodesLoading = false,
                    errorMessage = null,
                    selectedCategoryId = FavoriteSeriesCategoryId,
                    categories = state.categories.withSpecialCategories(
                        allCount = catalogTotalCount(),
                        favoriteCount = favoriteIds.size,
                        historyCount = historySeries().size,
                        historySignals = historyCategorySignals,
                    ),
                    series = series,
                    focusedSeriesId = series.firstOrNull()?.seriesId,
                    selectedSeriesId = null,
                    episodes = emptyList(),
                )
            }
        }
    }

    private fun loadHistorySeries() {
        seriesJob?.cancel()
        episodesJob?.cancel()
        metadataJob?.cancel()
        val series = historySeries(_uiState.value.contentSearchQuery)
        _uiState.update { state ->
            state.copy(
                seriesLoading = false,
                itemsLoading = false,
                nextPageLoading = false,
                hasMoreItems = false,
                currentOffset = series.size,
                episodesLoading = false,
                errorMessage = null,
                selectedCategoryId = HistorySeriesCategoryId,
                categories = state.categories.withSpecialCategories(
                    allCount = catalogTotalCount(),
                    favoriteCount = favoriteIds.size,
                    historyCount = series.size,
                    historySignals = historyCategorySignals,
                ),
                series = series,
                focusedSeriesId = series.firstOrNull()?.seriesId,
                selectedSeriesId = null,
                episodes = emptyList(),
            )
        }
    }

    private fun loadAllSeries() {
        loadSeriesPage(categoryId = null, selectedCategoryId = AllSeriesCategoryId, categoryLabel = "Series", replace = true)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.categoriesLoading || state.itemsLoading || state.nextPageLoading || !state.hasMoreItems) return
        val selectedCategoryId = state.selectedCategoryId ?: return
        if (selectedCategoryId in setOf(FavoriteSeriesCategoryId, HistorySeriesCategoryId)) return
        val categoryId = selectedCategoryId.takeUnless { it == AllSeriesCategoryId }
        val categoryLabel = state.selectedCategory?.label ?: "Series"
        loadSeriesPage(
            categoryId = categoryId,
            selectedCategoryId = selectedCategoryId,
            categoryLabel = categoryLabel,
            query = state.contentSearchQuery,
            replace = false,
        )
    }

    private fun historySeries(query: String = ""): List<SeriesItemUi> =
        historyProgress.asSequence()
            .mapNotNull { progress ->
                val episodeId = progress.contentId.toIntOrNull() ?: return@mapNotNull null
                val seriesId = progress.parentContentId?.toIntOrNull()
                    ?: return@mapNotNull null
                Triple(seriesId, progress, episodeId)
            }
            .distinctBy { it.first }
            .mapIndexed { index, (seriesId, progress, episodeId) ->
                val title = progress.title ?: "Serie $seriesId"
                if (!playerSettings.allowsContent(title, progress.subtitle)) {
                    return@mapIndexed null
                }
                SeriesItemUi(
                    seriesId = seriesId,
                    number = (index + 1).toString().padStart(3, '0'),
                    title = title,
                    coverUrl = progress.imageUrl,
                    categoryLabel = "Historique",
                    plot = null,
                    genre = null,
                    releaseDate = null,
                    rating = null,
                    episodeRunTime = null,
                    preferredEpisodeId = episodeId,
                    isFavorite = seriesId in favoriteIds,
                )
            }
            .toList()
            .filterNotNull()
            .filter { it.matchesSearch(query) }

    private suspend fun favoriteSeries(query: String = ""): List<SeriesItemUi> =
        catalogRepository.getSeriesByIds(favoriteIds.toList())
            .filter { series ->
                playerSettings.allowsContent(series.title, series.plot, series.genre, series.categoryName)
            }
            .sortedBy { it.title }
            .mapIndexed { index, series ->
                series.toUiSeries(index, series.categoryName.ifBlank { "Favoris" }, favoriteIds)
            }
            .filter { it.matchesSearch(query) }

    private fun loadSeries(categoryId: String) {
        val categoryLabel = _uiState.value.categories.firstOrNull { it.id == categoryId }?.label ?: "Series"
        loadSeriesPage(categoryId = categoryId, selectedCategoryId = categoryId, categoryLabel = categoryLabel, replace = true)
    }

    private fun loadSeriesPage(
        categoryId: String?,
        selectedCategoryId: String,
        categoryLabel: String,
        query: String = _uiState.value.contentSearchQuery,
        replace: Boolean,
    ) {
        if (replace) seriesJob?.cancel()
        if (replace) episodesJob?.cancel()
        seriesJob = viewModelScope.launch {
            val startOffset = if (replace) 0 else _uiState.value.currentOffset
            val previousSeries = if (replace) emptyList() else _uiState.value.series
            _uiState.update {
                it.copy(
                    itemsLoading = replace,
                    seriesLoading = replace,
                    nextPageLoading = !replace,
                    episodesLoading = false,
                    errorMessage = null,
                    selectedCategoryId = selectedCategoryId,
                    series = if (replace) emptyList() else it.series,
                    episodes = if (replace) emptyList() else it.episodes,
                    focusedSeriesId = if (replace) null else it.focusedSeriesId,
                    selectedSeriesId = if (replace) null else it.selectedSeriesId,
                )
            }
            runCatching {
                val page = if (query.isNotBlank()) {
                    catalogRepository.searchSeriesPage(categoryId, query, startOffset, SeriesItemsPageSize)
                } else if (categoryId == null) {
                    catalogRepository.getAllSeriesPage(startOffset, SeriesItemsPageSize)
                } else {
                    catalogRepository.getSeriesPage(categoryId, startOffset, SeriesItemsPageSize)
                }
                val visiblePage = page
                    .filter { series -> playerSettings.allowsContent(series.title, series.plot, series.genre, categoryLabel) }
                    .mapIndexed { index, series ->
                        series.toUiSeries(startOffset + index, series.categoryName.ifBlank { categoryLabel }, favoriteIds)
                    }
                PageLoadResult(items = (previousSeries + visiblePage).distinctBy { it.seriesId }, rawPageSize = page.size)
            }.onSuccess { result ->
                _uiState.update { state ->
                    state.copy(
                        itemsLoading = false,
                        seriesLoading = false,
                        nextPageLoading = false,
                        hasMoreItems = result.rawPageSize == SeriesItemsPageSize,
                        currentOffset = startOffset + result.rawPageSize,
                        categories = state.categories.withSpecialCategories(
                            allCount = catalogTotalCount(),
                            favoriteCount = favoriteIds.size,
                            historyCount = historySeries().size,
                            historySignals = historyCategorySignals,
                        ),
                        series = result.items,
                        focusedSeriesId = state.focusedSeriesId ?: result.items.firstOrNull()?.seriesId,
                        errorMessage = null,
                    )
                }
                loadVisibleMetadata(result.items)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        itemsLoading = false,
                        seriesLoading = false,
                        nextPageLoading = false,
                        series = if (replace) emptyList() else it.series,
                        focusedSeriesId = if (replace) null else it.focusedSeriesId,
                        selectedSeriesId = if (replace) null else it.selectedSeriesId,
                        episodes = if (replace) emptyList() else it.episodes,
                        errorMessage = error.userMessage("Impossible de charger les series Xtream."),
                    )
                }
            }
        }
    }

    private fun loadVisibleMetadata(series: List<SeriesItemUi>) {
        metadataJob?.cancel()
        metadataJob = viewModelScope.launch {
            series.take(20).chunked(4).forEach { batch ->
                val metadata = batch.map { item ->
                    async {
                        val tmdb = tmdbRepository.getCachedSeriesMetadata(item.seriesId, playerSettings.language)
                        val episodes = runCatching {
                            catalogRepository.getSeriesEpisodes(item.seriesId)
                        }.getOrDefault(emptyList())
                        item.seriesId to CachedSeriesMetadata(
                            seasons = episodes.map { it.seasonNumber }.filter { it > 0 }.distinct().size,
                            episodes = episodes.size,
                            title = tmdb?.name.nonBlank(),
                            coverUrl = tmdb?.posterUrl.nonBlank(),
                            backdropUrl = tmdb?.backdropUrl.nonBlank(),
                            plot = tmdb?.overview.nonBlank(),
                            genre = tmdb?.genres.nonBlank(),
                            releaseDate = tmdb?.firstAirDate?.take(4)?.takeIf { it.all(Char::isDigit) },
                            rating = tmdb?.voteAverage?.takeIf { it > 0.0 }?.formatRating(),
                            episodeRunTime = tmdb?.episodeRunTimeMinutes?.takeIf { it > 0 }?.toString(),
                            createdBy = tmdb?.createdBy.nonBlank(),
                            cast = tmdb?.cast.nonBlank(),
                        )
                    }
                }.awaitAll().toMap()
                _uiState.update { state ->
                    state.copy(
                        series = state.series.map { item ->
                            metadata[item.seriesId]?.let { meta ->
                                item.copy(
                                    title = meta.title ?: item.title,
                                    coverUrl = meta.coverUrl ?: item.coverUrl,
                                    backdropUrl = meta.backdropUrl ?: item.backdropUrl,
                                    plot = meta.plot ?: item.plot,
                                    genre = meta.genre ?: item.genre,
                                    releaseDate = meta.releaseDate ?: item.releaseDate,
                                    rating = meta.rating ?: item.rating,
                                    episodeRunTime = meta.episodeRunTime ?: item.episodeRunTime,
                                    seasonsCount = meta.seasons.takeIf { it > 0 },
                                    episodesCount = meta.episodes.takeIf { it > 0 },
                                    createdBy = meta.createdBy ?: item.createdBy,
                                    cast = meta.cast ?: item.cast,
                                )
                            } ?: item
                        },
                    )
                }
            }
        }
    }

    private fun loadEpisodes(seriesId: Int) {
        episodesJob?.cancel()
        episodesJob = viewModelScope.launch {
            _uiState.update { it.copy(episodesLoading = true) }
            runCatching {
                val localEpisodes = catalogRepository.getSeriesEpisodes(seriesId)
                val episodes = if (localEpisodes.isNotEmpty()) {
                    localEpisodes.map { episode -> episode.toUiEpisode(xtreamRepository) }
                } else {
                    xtreamRepository.getSeriesEpisodes(seriesId)
                        .map { episode -> episode.toUiEpisode(xtreamRepository) }
                }
                episodes
                    .filter { episode -> playerSettings.allowsContent(episode.title, episode.plot) }
                    .map { episode ->
                    val progress = historyProgress.firstOrNull { item ->
                        item.contentId.toIntOrNull() == episode.episodeId
                    }
                    episode.copy(
                        progressPercent = progress?.let { item ->
                            if (item.durationMs > 0L) {
                                ((item.positionMs * 100L) / item.durationMs).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }
                        } ?: 0,
                        resumePositionMs = progress?.positionMs ?: 0L,
                    )
                }
            }.onSuccess { episodes ->
                _uiState.update {
                    if (it.selectedSeriesId == seriesId) {
                        val preferredId = it.selectedPreviewEpisodeId
                            ?.takeIf { episodeId -> episodes.any { episode -> episode.episodeId == episodeId } }
                            ?: episodes.firstOrNull()?.episodeId
                        it.copy(
                            episodesLoading = false,
                            episodes = episodes,
                            selectedPreviewEpisodeId = preferredId,
                        )
                    } else {
                        it.copy(episodesLoading = false)
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        episodesLoading = false,
                        errorMessage = error.userMessage("Impossible de charger les episodes."),
                    )
                }
            }
        }
    }

    fun deleteHistorySeries(series: SeriesItemUi) {
        val progress = historyProgress.firstOrNull { item ->
            item.parentContentId?.toIntOrNull() == series.seriesId
        } ?: return
        val episodeId = progress.contentId.toIntOrNull() ?: return
        viewModelScope.launch {
            userContentRepository.deleteProgress(UserContentType.Episode, episodeId)
        }
    }

    private fun catalogTotalCount(): Int? =
        localCategories.sumOf { it.count }.takeIf { it > 0 }
}

private fun SeriesSortMode.comparator(): Comparator<SeriesItemUi> = when (this) {
    SeriesSortMode.DEFAULT -> compareBy { it.number.toIntOrNull() ?: Int.MAX_VALUE }
    SeriesSortMode.TITLE_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
    SeriesSortMode.TITLE_DESC -> compareByDescending<SeriesItemUi> { it.title.lowercase(Locale.ROOT) }
    SeriesSortMode.NEWEST -> compareByDescending<SeriesItemUi> { it.releaseDate?.take(4)?.toIntOrNull() ?: Int.MIN_VALUE }
    SeriesSortMode.RATING -> compareByDescending<SeriesItemUi> { it.rating?.replace(',', '.')?.toDoubleOrNull() ?: Double.NEGATIVE_INFINITY }
}.thenBy { it.seriesId }

private data class PageLoadResult<T>(
    val items: List<T>,
    val rawPageSize: Int,
)

private data class CachedSeriesMetadata(
    val seasons: Int,
    val episodes: Int,
    val title: String?,
    val coverUrl: String?,
    val backdropUrl: String?,
    val plot: String?,
    val genre: String?,
    val releaseDate: String?,
    val rating: String?,
    val episodeRunTime: String?,
    val createdBy: String?,
    val cast: String?,
)

private const val SeriesItemsPageSize = 72
private const val InitialCategoryLimit = 20
private const val FavoriteSeriesCategoryId = "__favorites_series__"
private const val HistorySeriesCategoryId = "__history_series__"
private const val AllSeriesCategoryId = "__all_series__"
private val SpecialSeriesCategoryIds = setOf(AllSeriesCategoryId, FavoriteSeriesCategoryId, HistorySeriesCategoryId)

private fun List<SeriesCategoryUi>.withSpecialCategories(
    allCount: Int?,
    favoriteCount: Int,
    historyCount: Int,
    historySignals: List<CategoryHistorySignal> = emptyList(),
): List<SeriesCategoryUi> =
    listOf(
        SeriesCategoryUi(
            id = AllSeriesCategoryId,
            label = "ALL",
            count = allCount,
        ),
        SeriesCategoryUi(
            id = FavoriteSeriesCategoryId,
            label = "Favoris",
            count = favoriteCount,
        ),
        SeriesCategoryUi(
            id = HistorySeriesCategoryId,
            label = "Historique",
            count = historyCount,
        ),
    ) + filterNot { it.id in SpecialSeriesCategoryIds || AllCategoryPolicy.isEquivalent(it.label) }
        .sortedByHistorySignals(historySignals) { it.id }

private fun List<SeriesCategoryUi>.initialCategoryForPlaylist(): SeriesCategoryUi? =
    firstOrNull { it.id == AllSeriesCategoryId && (it.count ?: 0) > 0 }
        ?: firstOrNull { it.id !in setOf(FavoriteSeriesCategoryId, HistorySeriesCategoryId) && (it.count ?: 0) > 0 }
        ?: firstOrNull()

private fun Category.toUiCategory(): SeriesCategoryUi =
    SeriesCategoryUi(
        id = id,
        label = name,
        count = count,
    )

private fun XtreamSeriesCategory.toUiCategory(): SeriesCategoryUi =
    SeriesCategoryUi(
        id = id,
        label = name,
        count = count,
    )

private fun TvSeries.toUiSeries(
    index: Int,
    categoryLabel: String,
    favoriteIds: Set<Int>,
): SeriesItemUi =
    SeriesItemUi(
        seriesId = seriesId,
        number = (number.takeIf { it > 0 } ?: (index + 1)).toString().padStart(3, '0'),
        title = title.cleanTitle(),
        coverUrl = posterUrl,
        categoryLabel = categoryLabel,
        plot = plot,
        genre = genre,
        releaseDate = year,
        rating = rating,
        episodeRunTime = null,
        seasonsCount = seasonsCount,
        isFavorite = seriesId in favoriteIds,
    )

private fun XtreamSeriesStream.toUiSeries(
    index: Int,
    categoryLabel: String,
    favoriteIds: Set<Int>,
): SeriesItemUi =
    SeriesItemUi(
        seriesId = seriesId,
        number = (number.takeIf { it > 0 } ?: (index + 1)).toString().padStart(3, '0'),
        title = title.cleanTitle(),
        coverUrl = coverUrl,
        categoryLabel = categoryLabel,
        plot = plot,
        genre = genre,
        releaseDate = releaseDate,
        rating = rating,
        episodeRunTime = episodeRunTime,
        isFavorite = seriesId in favoriteIds,
    )

private fun SeriesItemUi.matchesSearch(query: String): Boolean {
    if (query.isBlank()) return true
    return title.contains(query, ignoreCase = true) ||
        genre?.contains(query, ignoreCase = true) == true ||
        releaseDate?.contains(query, ignoreCase = true) == true
}

private fun Episode.toUiEpisode(xtreamRepository: XtreamRepository): SeriesEpisodeUi =
    SeriesEpisodeUi(
        episodeId = episodeId,
        seriesId = seriesId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        title = title.cleanTitle(),
        duration = duration,
        plot = plot,
        streamUrl = xtreamRepository.buildEpisodeStreamUrl(episodeId, containerExtension),
    )

private fun XtreamSeriesEpisode.toUiEpisode(xtreamRepository: XtreamRepository): SeriesEpisodeUi =
    SeriesEpisodeUi(
        episodeId = episodeId,
        seriesId = seriesId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        title = title.cleanTitle(),
        duration = duration,
        plot = plot,
        streamUrl = xtreamRepository.buildEpisodeStreamUrl(this),
    )

private fun String.cleanTitle(): String =
    replace(Regex("\\s+"), " ")
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" 4K", "", ignoreCase = true)
        .trim()
        .ifBlank { "Series" }

private fun String?.nonBlank(): String? = this?.trim()?.takeIf { it.isNotBlank() }

private fun Double.formatRating(): String = String.format(Locale.US, "%.1f", this)

private fun Throwable.userMessage(fallback: String): String =
    when (this) {
        is IllegalStateException -> message ?: fallback
        else -> fallback
    }
