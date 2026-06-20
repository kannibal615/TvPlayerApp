package com.smartvision.svplayer.ui.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.models.XtreamSeriesCategory
import com.smartvision.svplayer.data.models.XtreamSeriesEpisode
import com.smartvision.svplayer.data.models.XtreamSeriesStream
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.UserContentType
import com.smartvision.svplayer.data.repository.XtreamRepository
import kotlinx.coroutines.Job
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(SeriesScreenState())
    val uiState: StateFlow<SeriesScreenState> = _uiState.asStateFlow()

    private var seriesJob: Job? = null
    private var episodesJob: Job? = null
    private var favoriteIds: Set<Int> = emptySet()

    init {
        observeFavorites()
        loadCategories()
    }

    fun loadCategories() {
        seriesJob?.cancel()
        episodesJob?.cancel()
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
                    val visibleCategories = categories.withFavorites(favoriteIds.size)
                    val initialCategoryId = if (favoriteIds.isNotEmpty()) {
                        FavoriteSeriesCategoryId
                    } else {
                        categories.first().id
                    }
                    it.copy(
                        categoriesLoading = false,
                        categories = visibleCategories,
                        selectedCategoryId = initialCategoryId,
                        errorMessage = null,
                    )
                }
                if (favoriteIds.isNotEmpty()) {
                    loadFavoriteSeries()
                } else {
                    loadSeries(categories.first().id)
                }
            }.onFailure { error ->
                _uiState.value = SeriesScreenState(
                    categoriesLoading = false,
                    errorMessage = error.userMessage("Impossible de charger les categories Series."),
                )
            }
        }
    }

    fun selectCategory(category: SeriesCategoryUi) {
        if (_uiState.value.selectedCategoryId == category.id && _uiState.value.series.isNotEmpty()) {
            return
        }
        if (category.id == FavoriteSeriesCategoryId) {
            loadFavoriteSeries()
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
        } else {
            loadSeries(categoryId)
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
                        categories = state.categories.withFavorites(ids.size),
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

    private fun loadFavoriteSeries() {
        seriesJob?.cancel()
        episodesJob?.cancel()
        val series = favoriteSeries()
        _uiState.update { state ->
            state.copy(
                seriesLoading = false,
                episodesLoading = false,
                errorMessage = null,
                selectedCategoryId = FavoriteSeriesCategoryId,
                categories = state.categories.withFavorites(favoriteIds.size),
                series = series,
                focusedSeriesId = series.firstOrNull()?.seriesId,
                selectedSeriesId = null,
                episodes = emptyList(),
            )
        }
    }

    private fun favoriteSeries(): List<SeriesItemUi> =
        xtreamRepository.getCachedSeriesList()
            .filter { it.seriesId in favoriteIds }
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
                xtreamRepository.getSeries(categoryId).mapIndexed { index, series ->
                    series.toUiSeries(index, categoryLabel, favoriteIds)
                }
            }.onSuccess { series ->
                _uiState.update { state ->
                    state.copy(
                        seriesLoading = false,
                        categories = state.categories.map {
                            if (it.id == categoryId) it.copy(count = series.size) else it
                        }.withFavorites(favoriteIds.size),
                        series = series,
                        focusedSeriesId = series.firstOrNull()?.seriesId,
                        errorMessage = null,
                    )
                }
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

    private fun loadEpisodes(seriesId: Int) {
        episodesJob?.cancel()
        episodesJob = viewModelScope.launch {
            _uiState.update { it.copy(episodesLoading = true) }
            runCatching {
                xtreamRepository.getSeriesEpisodes(seriesId).map { episode ->
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
}

private const val FavoriteSeriesCategoryId = "__favorites_series__"

private fun List<SeriesCategoryUi>.withFavorites(count: Int): List<SeriesCategoryUi> =
    listOf(
        SeriesCategoryUi(
            id = FavoriteSeriesCategoryId,
            label = "Favoris",
            count = count,
        ),
    ) + filterNot { it.id == FavoriteSeriesCategoryId }

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
