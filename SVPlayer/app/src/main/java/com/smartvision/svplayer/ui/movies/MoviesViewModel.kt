package com.smartvision.svplayer.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.models.XtreamMovieCategory
import com.smartvision.svplayer.data.models.XtreamMovieStream
import com.smartvision.svplayer.data.repository.XtreamRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MovieCategoryUi(
    val id: String,
    val label: String,
    val count: Int?,
)

data class MovieItemUi(
    val streamId: Int,
    val number: String,
    val title: String,
    val posterUrl: String?,
    val categoryLabel: String,
    val containerExtension: String,
    val rating: String?,
    val year: String?,
    val streamUrl: String,
    val isFavorite: Boolean = false,
) {
    val subtitle: String =
        listOfNotNull(year, rating, containerExtension.uppercase()).joinToString(" | ").ifBlank { categoryLabel }
}

data class MoviesScreenState(
    val categoriesLoading: Boolean = true,
    val moviesLoading: Boolean = false,
    val errorMessage: String? = null,
    val categories: List<MovieCategoryUi> = emptyList(),
    val selectedCategoryId: String? = null,
    val movies: List<MovieItemUi> = emptyList(),
    val focusedMovieId: Int? = null,
    val selectedMovieId: Int? = null,
) {
    val selectedCategory: MovieCategoryUi?
        get() = categories.firstOrNull { it.id == selectedCategoryId }

    val selectedMovie: MovieItemUi?
        get() = movies.firstOrNull { it.streamId == selectedMovieId }
}

class MoviesViewModel(
    private val xtreamRepository: XtreamRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MoviesScreenState())
    val uiState: StateFlow<MoviesScreenState> = _uiState.asStateFlow()

    private var moviesJob: Job? = null

    init {
        loadCategories()
    }

    fun loadCategories() {
        moviesJob?.cancel()
        viewModelScope.launch {
            _uiState.value = MoviesScreenState(categoriesLoading = true)
            runCatching {
                xtreamRepository.getMovieCategories().map { it.toUiCategory() }
            }.onSuccess { categories ->
                if (categories.isEmpty()) {
                    _uiState.value = MoviesScreenState(
                        categoriesLoading = false,
                        errorMessage = "Aucune categorie Films retournee par Xtream.",
                    )
                    return@onSuccess
                }
                _uiState.update {
                    it.copy(
                        categoriesLoading = false,
                        categories = categories,
                        selectedCategoryId = categories.first().id,
                        errorMessage = null,
                    )
                }
                loadMovies(categories.first().id)
            }.onFailure { error ->
                _uiState.value = MoviesScreenState(
                    categoriesLoading = false,
                    errorMessage = error.userMessage("Impossible de charger les categories Films."),
                )
            }
        }
    }

    fun selectCategory(category: MovieCategoryUi) {
        if (_uiState.value.selectedCategoryId == category.id && _uiState.value.movies.isNotEmpty()) {
            return
        }
        loadMovies(category.id)
    }

    fun focusMovie(movie: MovieItemUi) {
        _uiState.update { it.copy(focusedMovieId = movie.streamId) }
    }

    fun activateMovie(movie: MovieItemUi): Boolean {
        val openFullPlayer = _uiState.value.selectedMovieId == movie.streamId
        _uiState.update {
            it.copy(
                selectedMovieId = movie.streamId,
                focusedMovieId = movie.streamId,
                errorMessage = null,
            )
        }
        return openFullPlayer
    }

    fun toggleFavorite(movie: MovieItemUi) {
        _uiState.update { state ->
            state.copy(
                movies = state.movies.map {
                    if (it.streamId == movie.streamId) it.copy(isFavorite = !it.isFavorite) else it
                },
            )
        }
    }

    fun retryCurrentCategory() {
        val categoryId = _uiState.value.selectedCategoryId
        if (categoryId == null) {
            loadCategories()
        } else {
            loadMovies(categoryId)
        }
    }

    private fun loadMovies(categoryId: String) {
        moviesJob?.cancel()
        moviesJob = viewModelScope.launch {
            val categoryLabel = _uiState.value.categories.firstOrNull { it.id == categoryId }?.label ?: "Films"
            _uiState.update {
                it.copy(
                    moviesLoading = true,
                    errorMessage = null,
                    selectedCategoryId = categoryId,
                    movies = emptyList(),
                    focusedMovieId = null,
                    selectedMovieId = null,
                )
            }
            runCatching {
                xtreamRepository.getMovies(categoryId).mapIndexed { index, movie ->
                    movie.toUiMovie(index, categoryLabel, xtreamRepository)
                }
            }.onSuccess { movies ->
                _uiState.update { state ->
                    state.copy(
                        moviesLoading = false,
                        categories = state.categories.map {
                            if (it.id == categoryId) it.copy(count = movies.size) else it
                        },
                        movies = movies,
                        focusedMovieId = movies.firstOrNull()?.streamId,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        moviesLoading = false,
                        movies = emptyList(),
                        focusedMovieId = null,
                        selectedMovieId = null,
                        errorMessage = error.userMessage("Impossible de charger les films Xtream."),
                    )
                }
            }
        }
    }
}

private fun XtreamMovieCategory.toUiCategory(): MovieCategoryUi =
    MovieCategoryUi(
        id = id,
        label = name,
        count = count,
    )

private fun XtreamMovieStream.toUiMovie(
    index: Int,
    categoryLabel: String,
    xtreamRepository: XtreamRepository,
): MovieItemUi =
    MovieItemUi(
        streamId = streamId,
        number = (number.takeIf { it > 0 } ?: (index + 1)).toString().padStart(3, '0'),
        title = title.cleanTitle(),
        posterUrl = posterUrl,
        categoryLabel = categoryLabel,
        containerExtension = containerExtension,
        rating = rating?.takeIf { it.isNotBlank() },
        year = added?.take(4)?.takeIf { it.all(Char::isDigit) },
        streamUrl = xtreamRepository.buildMovieStreamUrl(this),
    )

private fun String.cleanTitle(): String =
    replace(Regex("\\s+"), " ")
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" 4K", "", ignoreCase = true)
        .trim()
        .ifBlank { "Film" }

private fun Throwable.userMessage(fallback: String): String =
    when (this) {
        is IllegalStateException -> message ?: fallback
        else -> fallback
    }
