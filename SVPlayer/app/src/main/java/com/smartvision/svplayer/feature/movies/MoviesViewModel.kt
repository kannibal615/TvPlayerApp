package com.smartvision.svplayer.feature.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.domain.model.Category
import com.smartvision.svplayer.domain.model.Movie
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

data class MoviesUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val movies: List<Movie> = emptyList(),
    val selectedMovieId: Int? = null,
) {
    val selectedMovie: Movie?
        get() = movies.firstOrNull { it.streamId == selectedMovieId } ?: movies.firstOrNull()
}

@OptIn(ExperimentalCoroutinesApi::class)
class MoviesViewModel(
    private val repository: CatalogRepository,
) : ViewModel() {
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val selectedMovieId = MutableStateFlow<Int?>(null)

    val uiState = combine(
        repository.observeMovieCategories(),
        selectedCategoryId.flatMapLatest { repository.observeMovies(it) },
        selectedCategoryId,
        selectedMovieId,
    ) { categories, movies, categoryId, movieId ->
        MoviesUiState(
            categories = categories,
            selectedCategoryId = categoryId ?: categories.firstOrNull()?.id,
            movies = movies,
            selectedMovieId = movieId ?: movies.firstOrNull()?.streamId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoviesUiState())

    init {
        viewModelScope.launch {
            val categories = repository.observeMovieCategories().filter { it.isNotEmpty() }.first()
            selectedCategoryId.value = categories.first().id
        }
    }

    fun selectCategory(category: Category) {
        selectedCategoryId.value = category.id
        selectedMovieId.value = null
    }

    fun selectMovie(movie: Movie) {
        selectedMovieId.value = movie.streamId
    }

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            repository.toggleFavorite("movie", movie.streamId.toString())
        }
    }
}
