package com.smartvision.svplayer.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.models.XtreamMovieCategory
import com.smartvision.svplayer.data.models.XtreamMovieStream
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
    private val userContentRepository: UserContentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MoviesScreenState())
    val uiState: StateFlow<MoviesScreenState> = _uiState.asStateFlow()

    private var moviesJob: Job? = null
    private var favoriteIds: Set<Int> = emptySet()

    init {
        observeFavorites()
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
                    val visibleCategories = categories.withFavorites(favoriteIds.size)
                    val initialCategoryId = if (favoriteIds.isNotEmpty()) {
                        FavoriteMovieCategoryId
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
                    loadFavoriteMovies()
                } else {
                    loadMovies(categories.first().id)
                }
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
        if (category.id == FavoriteMovieCategoryId) {
            loadFavoriteMovies()
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
        viewModelScope.launch {
            userContentRepository.toggleFavorite(UserContentType.Movie, movie.streamId)
        }
    }

    fun retryCurrentCategory() {
        val categoryId = _uiState.value.selectedCategoryId
        if (categoryId == null) {
            loadCategories()
        } else if (categoryId == FavoriteMovieCategoryId) {
            loadFavoriteMovies()
        } else {
            loadMovies(categoryId)
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            userContentRepository.observeFavoriteIds(UserContentType.Movie).collect { ids ->
                favoriteIds = ids
                _uiState.update { state ->
                    val refreshedMovies = if (state.selectedCategoryId == FavoriteMovieCategoryId) {
                        favoriteMovies()
                    } else {
                        state.movies.map { it.copy(isFavorite = it.streamId in ids) }
                    }
                    state.copy(
                        categories = state.categories.withFavorites(ids.size),
                        movies = refreshedMovies,
                        focusedMovieId = state.focusedMovieId
                            ?.takeIf { focusedId -> refreshedMovies.any { it.streamId == focusedId } }
                            ?: refreshedMovies.firstOrNull()?.streamId,
                        selectedMovieId = state.selectedMovieId
                            ?.takeIf { selectedId -> refreshedMovies.any { it.streamId == selectedId } },
                    )
                }
            }
        }
    }

    private fun loadFavoriteMovies() {
        moviesJob?.cancel()
        val movies = favoriteMovies()
        _uiState.update { state ->
            state.copy(
                moviesLoading = false,
                errorMessage = null,
                selectedCategoryId = FavoriteMovieCategoryId,
                categories = state.categories.withFavorites(favoriteIds.size),
                movies = movies,
                focusedMovieId = movies.firstOrNull()?.streamId,
                selectedMovieId = null,
            )
        }
    }

    private fun favoriteMovies(): List<MovieItemUi> =
        xtreamRepository.getCachedMovies()
            .filter { it.streamId in favoriteIds }
            .sortedBy { it.title }
            .mapIndexed { index, movie ->
                val categoryLabel = xtreamRepository.getCachedMovieCategories()
                    .firstOrNull { it.id == movie.categoryId }
                    ?.name
                    ?: "Favoris"
                movie.toUiMovie(index, categoryLabel, xtreamRepository, favoriteIds)
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
                    movie.toUiMovie(index, categoryLabel, xtreamRepository, favoriteIds)
                }
            }.onSuccess { movies ->
                _uiState.update { state ->
                    state.copy(
                        moviesLoading = false,
                        categories = state.categories.map {
                            if (it.id == categoryId) it.copy(count = movies.size) else it
                        }.withFavorites(favoriteIds.size),
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

private const val FavoriteMovieCategoryId = "__favorites_movies__"

private fun List<MovieCategoryUi>.withFavorites(count: Int): List<MovieCategoryUi> =
    listOf(
        MovieCategoryUi(
            id = FavoriteMovieCategoryId,
            label = "Favoris",
            count = count,
        ),
    ) + filterNot { it.id == FavoriteMovieCategoryId }

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
    favoriteIds: Set<Int>,
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
        isFavorite = streamId in favoriteIds,
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
