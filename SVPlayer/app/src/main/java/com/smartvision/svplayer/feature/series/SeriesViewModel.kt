package com.smartvision.svplayer.feature.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.domain.model.Category
import com.smartvision.svplayer.domain.model.Episode
import com.smartvision.svplayer.domain.model.TvSeries
import com.smartvision.svplayer.domain.repository.CatalogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SeriesUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val series: List<TvSeries> = emptyList(),
    val selectedSeriesId: Int? = null,
    val episodes: List<Episode> = emptyList(),
    val loadingEpisodes: Boolean = false,
) {
    val selectedSeries: TvSeries?
        get() = series.firstOrNull { it.seriesId == selectedSeriesId } ?: series.firstOrNull()
}

@OptIn(ExperimentalCoroutinesApi::class)
class SeriesViewModel(
    private val repository: CatalogRepository,
) : ViewModel() {
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val selectedSeriesId = MutableStateFlow<Int?>(null)
    private val episodes = MutableStateFlow<List<Episode>>(emptyList())
    private val loadingEpisodes = MutableStateFlow(false)

    val uiState = combine(
        repository.observeSeriesCategories(),
        selectedCategoryId.flatMapLatest { repository.observeSeries(it) },
        selectedCategoryId,
        selectedSeriesId,
        episodes,
        loadingEpisodes,
    ) { categories, series, categoryId, seriesId, episodeList, loading ->
        SeriesUiState(
            categories = categories,
            selectedCategoryId = categoryId ?: categories.firstOrNull()?.id,
            series = series,
            selectedSeriesId = seriesId ?: series.firstOrNull()?.seriesId,
            episodes = episodeList,
            loadingEpisodes = loading,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SeriesUiState())

    init {
        viewModelScope.launch {
            val categories = repository.observeSeriesCategories().filter { it.isNotEmpty() }.first()
            selectedCategoryId.value = categories.first().id
        }
    }

    fun selectCategory(category: Category) {
        selectedCategoryId.value = category.id
        selectedSeriesId.value = null
        episodes.value = emptyList()
    }

    fun selectSeries(series: TvSeries) {
        selectedSeriesId.value = series.seriesId
        episodes.value = emptyList()
    }

    fun loadEpisodes(series: TvSeries) {
        selectedSeriesId.value = series.seriesId
        viewModelScope.launch {
            loadingEpisodes.value = true
            episodes.value = repository.getSeriesEpisodes(series.seriesId)
            loadingEpisodes.value = false
        }
    }

    fun toggleFavorite(series: TvSeries) {
        viewModelScope.launch {
            repository.toggleFavorite("series", series.seriesId.toString())
        }
    }
}
