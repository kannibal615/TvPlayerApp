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
import com.smartvision.svplayer.domain.model.CategoryHistorySignal
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.domain.model.sortedByHistorySignals
import com.smartvision.svplayer.domain.repository.SettingsRepository
import com.smartvision.svplayer.ui.settings.allowsContent
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
    val categoryLabel: String,
    val plot: String?,
    val genre: String?,
    val releaseDate: String?,
    val rating: String?,
    val episodeRunTime: String?,
    val seasonsCount: Int? = null,
    val episodesCount: Int? = null,
    val isFavorite: Boolean = false,
) {
    val subtitle: String =
        listOfNotNull(releaseDate?.take(4), genre, rating).joinToString(" | ").ifBlank { categoryLabel }
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
) {
    val number: String = "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
}

data class SeriesScreenState(
    val categoriesLoading: Boolean = true,
    val seriesLoading: Boolean = false,
    val episodesLoading: Boolean = false,
    val errorMessage: String? = null,
    val categories: List<SeriesCategoryUi> = emptyList(),
    val selectedCategoryId: String? = null,
    val series: List<SeriesItemUi> = emptyList(),
    val focusedSeriesId: Int? = null,
    val selectedSeriesId: Int? = null,
    val episodes: List<SeriesEpisodeUi> = emptyList(),
) {
    val selectedCategory: SeriesCategoryUi?
        get() = categories.firstOrNull { it.id == selectedCategoryId }

    val selectedSeries: SeriesItemUi?
        get() = series.firstOrNull { it.seriesId == selectedSeriesId }

    val firstEpisode: SeriesEpisodeUi?
        get() = episodes.firstOrNull()
}

class SeriesViewModel(
    private val xtreamRepository: XtreamRepository,
    private val userContentRepository: UserContentRepository,
    private val settingsRepository: SettingsRepository,
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

    init {
        observeSettings()
        observeFavorites()
        observeHistory()
        loadCategories()
    }

    fun loadCategories() {
        seriesJob?.cancel()
        episodesJob?.cancel()
        metadataJob?.cancel()
        viewModelScope.launch {
            _uiState.value = SeriesScreenState(categoriesLoading = true)
            runCatching {
                xtreamRepository.getSeriesCategories().map { it.toUiCategory() }
            }.onSuccess { categories ->
                if (categories.isEmpty()) {
                    _uiState.value = SeriesScreenState(
                        categoriesLoading = false,
                        errorMessage = "Aucune categorie Series retournee par Xtream.",
                    )
                    return@onSuccess
                }
                _uiState.update {
                    val visibleCategories = categories.withSpecialCategories(
                        allCount = xtreamRepository.getCachedSeriesList().size.takeIf { it > 0 },
                        favoriteCount = favoriteIds.size,
                        historyCount = historySeries().size,
                        historySignals = historyCategorySignals,
                    )
                    it.copy(
                        categoriesLoading = false,
                        categories = visibleCategories,
                        selectedCategoryId = HistorySeriesCategoryId,
                        errorMessage = null,
                    )
                }
                loadHistorySeries()
            }.onFailure { error ->
                _uiState.value = SeriesScreenState(
                    categoriesLoading = false,
                    errorMessage = error.userMessage("Impossible de charger les categories Series."),
                )
            }
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

    fun focusSeries(series: SeriesItemUi) {
        _uiState.update { it.copy(focusedSeriesId = series.seriesId) }
    }

    fun activateSeries(series: SeriesItemUi): Int? {
        val state = _uiState.value
        if (state.selectedSeriesId == series.seriesId) {
            _uiState.update { it.copy(focusedSeriesId = series.seriesId, errorMessage = null) }
            return state.firstEpisode?.episodeId
        }
        selectSeries(series)
        return null
    }

    fun selectSeries(series: SeriesItemUi) {
        if (_uiState.value.selectedSeriesId == series.seriesId && _uiState.value.episodes.isNotEmpty()) {
            _uiState.update { it.copy(focusedSeriesId = series.seriesId, errorMessage = null) }
            return
        }
        _uiState.update {
            it.copy(
                selectedSeriesId = series.seriesId,
                focusedSeriesId = series.seriesId,
                episodes = emptyList(),
                errorMessage = null,
            )
        }
        loadEpisodes(series.seriesId)
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
                    retryCurrentCategory()
                }
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            userContentRepository.observeFavoriteIds(UserContentType.Series).collect { ids ->
                favoriteIds = ids
                _uiState.update { state ->
                    val refreshedSeries = if (state.selectedCategoryId == FavoriteSeriesCategoryId) {
                        favoriteSeries()
                    } else {
                        state.series.map { it.copy(isFavorite = it.seriesId in ids) }
                    }
                    state.copy(
                        categories = state.categories.withSpecialCategories(
                            allCount = xtreamRepository.getCachedSeriesList().size.takeIf { it > 0 },
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
                    val history = historySeries()
                    state.copy(
                        categories = state.categories.withSpecialCategories(
                            allCount = xtreamRepository.getCachedSeriesList().size.takeIf { it > 0 },
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
        val series = favoriteSeries()
        _uiState.update { state ->
            state.copy(
                seriesLoading = false,
                episodesLoading = false,
                errorMessage = null,
                selectedCategoryId = FavoriteSeriesCategoryId,
                categories = state.categories.withSpecialCategories(
                    allCount = xtreamRepository.getCachedSeriesList().size.takeIf { it > 0 },
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

    private fun loadHistorySeries() {
        seriesJob?.cancel()
        episodesJob?.cancel()
        metadataJob?.cancel()
        val series = historySeries()
        _uiState.update { state ->
            state.copy(
                seriesLoading = false,
                episodesLoading = false,
                errorMessage = null,
                selectedCategoryId = HistorySeriesCategoryId,
                categories = state.categories.withSpecialCategories(
                    allCount = xtreamRepository.getCachedSeriesList().size.takeIf { it > 0 },
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
        seriesJob?.cancel()
        episodesJob?.cancel()
        metadataJob?.cancel()
        seriesJob = viewModelScope.launch {
            val categories = _uiState.value.categories
                .filterNot { it.id in SpecialSeriesCategoryIds }
                .sortedByHistorySignals(historyCategorySignals) { it.id }
            val collected = mutableListOf<SeriesItemUi>()
            val seen = mutableSetOf<Int>()
            _uiState.update {
                it.copy(
                    seriesLoading = true,
                    episodesLoading = false,
                    errorMessage = null,
                    selectedCategoryId = AllSeriesCategoryId,
                    series = emptyList(),
                    episodes = emptyList(),
                    focusedSeriesId = null,
                    selectedSeriesId = null,
                )
            }
            runCatching {
                categories.forEach { category ->
                    xtreamRepository.getSeries(category.id).forEach { series ->
                        if (!seen.add(series.seriesId)) return@forEach
                        if (!playerSettings.allowsContent(series.title, series.plot, series.genre, category.label)) return@forEach
                        collected += series.toUiSeries(collected.size, category.label, favoriteIds)
                    }
                    _uiState.update { state ->
                        state.copy(
                            categories = state.categories.withSpecialCategories(
                                allCount = collected.size,
                                favoriteCount = favoriteIds.size,
                                historyCount = historySeries().size,
                                historySignals = historyCategorySignals,
                            ),
                            series = collected.toList(),
                            focusedSeriesId = state.focusedSeriesId ?: collected.firstOrNull()?.seriesId,
                        )
                    }
                }
                collected.toList()
            }.onSuccess { series ->
                _uiState.update { state ->
                    state.copy(
                        seriesLoading = false,
                        categories = state.categories.withSpecialCategories(
                            allCount = series.size,
                            favoriteCount = favoriteIds.size,
                            historyCount = historySeries().size,
                            historySignals = historyCategorySignals,
                        ),
                        series = series,
                        focusedSeriesId = series.firstOrNull()?.seriesId,
                        errorMessage = null,
                    )
                }
                loadVisibleMetadata(series)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        seriesLoading = false,
                        series = collected.toList(),
                        focusedSeriesId = collected.firstOrNull()?.seriesId,
                        selectedSeriesId = null,
                        episodes = emptyList(),
                        errorMessage = error.userMessage("Impossible de charger toutes les series Xtream."),
                    )
                }
            }
        }
    }

    private fun historySeries(): List<SeriesItemUi> =
        historyProgress.asSequence()
            .mapNotNull { progress ->
                val episodeId = progress.contentId.toIntOrNull() ?: return@mapNotNull null
                val seriesId = progress.parentContentId?.toIntOrNull()
                    ?: xtreamRepository.getCachedEpisode(episodeId)?.seriesId
                    ?: return@mapNotNull null
                Triple(seriesId, progress, episodeId)
            }
            .distinctBy { it.first }
            .mapIndexed { index, (seriesId, progress, _) ->
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
                    isFavorite = seriesId in favoriteIds,
                )
            }
            .toList()
            .filterNotNull()

    private fun favoriteSeries(): List<SeriesItemUi> =
        xtreamRepository.getCachedSeriesList()
            .filter { it.seriesId in favoriteIds }
            .filter { series ->
                val categoryLabel = xtreamRepository.getCachedSeriesCategories()
                    .firstOrNull { category -> category.id == series.categoryId }
                    ?.name
                playerSettings.allowsContent(series.title, series.plot, series.genre, categoryLabel)
            }
            .sortedBy { it.title }
            .mapIndexed { index, series ->
                val categoryLabel = xtreamRepository.getCachedSeriesCategories()
                    .firstOrNull { it.id == series.categoryId }
                    ?.name
                    ?: "Favoris"
                series.toUiSeries(index, categoryLabel, favoriteIds)
            }

    private fun loadSeries(categoryId: String) {
        seriesJob?.cancel()
        episodesJob?.cancel()
        seriesJob = viewModelScope.launch {
            val categoryLabel = _uiState.value.categories.firstOrNull { it.id == categoryId }?.label ?: "Series"
            _uiState.update {
                it.copy(
                    seriesLoading = true,
                    episodesLoading = false,
                    errorMessage = null,
                    selectedCategoryId = categoryId,
                    series = emptyList(),
                    episodes = emptyList(),
                    focusedSeriesId = null,
                    selectedSeriesId = null,
                )
            }
            runCatching {
                xtreamRepository.getSeries(categoryId)
                    .filter { series -> playerSettings.allowsContent(series.title, series.plot, series.genre, categoryLabel) }
                    .mapIndexed { index, series ->
                    series.toUiSeries(index, categoryLabel, favoriteIds)
                }
            }.onSuccess { series ->
                _uiState.update { state ->
                    state.copy(
                        seriesLoading = false,
                        categories = state.categories.map {
                            if (it.id == categoryId) it.copy(count = series.size) else it
                        }.withSpecialCategories(
                            allCount = xtreamRepository.getCachedSeriesList().size.takeIf { it > 0 },
                            favoriteCount = favoriteIds.size,
                            historyCount = historySeries().size,
                            historySignals = historyCategorySignals,
                        ),
                        series = series,
                        focusedSeriesId = series.firstOrNull()?.seriesId,
                        errorMessage = null,
                    )
                }
                loadVisibleMetadata(series)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        seriesLoading = false,
                        series = emptyList(),
                        focusedSeriesId = null,
                        selectedSeriesId = null,
                        episodes = emptyList(),
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
                        val episodes = runCatching {
                            xtreamRepository.getSeriesEpisodes(item.seriesId)
                        }.getOrDefault(emptyList())
                        item.seriesId to Pair(
                            episodes.map { it.seasonNumber }.filter { it > 0 }.distinct().size,
                            episodes.size,
                        )
                    }
                }.awaitAll().toMap()
                _uiState.update { state ->
                    state.copy(
                        series = state.series.map { item ->
                            metadata[item.seriesId]?.let { (seasons, episodes) ->
                                item.copy(
                                    seasonsCount = seasons.takeIf { it > 0 },
                                    episodesCount = episodes.takeIf { it > 0 },
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
                xtreamRepository.getSeriesEpisodes(seriesId)
                    .filter { episode -> playerSettings.allowsContent(episode.title, episode.plot) }
                    .map { episode ->
                    episode.toUiEpisode(xtreamRepository)
                }
            }.onSuccess { episodes ->
                _uiState.update {
                    if (it.selectedSeriesId == seriesId) {
                        it.copy(episodesLoading = false, episodes = episodes)
                    } else {
                        it.copy(episodesLoading = false)
                    }
                }
            }.onFailure { error ->
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
}

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
    ) + filterNot { it.id in SpecialSeriesCategoryIds }
        .sortedByHistorySignals(historySignals) { it.id }

private fun XtreamSeriesCategory.toUiCategory(): SeriesCategoryUi =
    SeriesCategoryUi(
        id = id,
        label = name,
        count = count,
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

private fun Throwable.userMessage(fallback: String): String =
    when (this) {
        is IllegalStateException -> message ?: fallback
        else -> fallback
    }
