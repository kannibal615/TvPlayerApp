package com.smartvision.svplayer.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.models.XtreamMovieCategory
import com.smartvision.svplayer.data.models.XtreamMovieStream
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.UserContentType
import com.smartvision.svplayer.data.repository.XtreamRepository
import com.smartvision.svplayer.domain.model.Category
import com.smartvision.svplayer.domain.model.CategoryHistorySignal
import com.smartvision.svplayer.domain.model.Movie
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.domain.model.sortedByHistorySignals
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.domain.repository.LocalCatalogSnapshot
import com.smartvision.svplayer.domain.repository.SettingsRepository
import com.smartvision.svplayer.ui.settings.allowsContent
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
    private val catalogRepository: CatalogRepository,
    private val userContentRepository: UserContentRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MoviesScreenState())
    val uiState: StateFlow<MoviesScreenState> = _uiState.asStateFlow()

    private var moviesJob: Job? = null
    private var favoriteIds: Set<Int> = emptySet()
    private var historyProgress: List<PlaybackProgressEntity> = emptyList()
    private var historyCategorySignals: List<CategoryHistorySignal> = emptyList()
    private var playerSettings = PlayerSettings()
    private var localCategories: List<Category> = emptyList()
    private var localMovies: List<Movie> = emptyList()

    init {
        observeSettings()
        observeFavorites()
        observeHistory()
        if (!restoreCachedCatalog()) {
            loadCategories()
        }
    }

    fun loadCategories() {
        moviesJob?.cancel()
        viewModelScope.launch {
            catalogRepository.getCachedMovieCatalogSnapshot()?.let { snapshot ->
                applyCatalogSnapshot(snapshot)
                return@launch
            }
            _uiState.value = MoviesScreenState(categoriesLoading = true)
            runCatching {
                catalogRepository.getMovieCatalogSnapshot()
            }.onSuccess { snapshot ->
                applyCatalogSnapshot(snapshot)
            }.onFailure { error ->
                _uiState.value = MoviesScreenState(
                    categoriesLoading = false,
                    errorMessage = error.userMessage("Impossible de charger les categories Films."),
                )
            }
        }
    }

    private fun restoreCachedCatalog(): Boolean {
        val snapshot = catalogRepository.getCachedMovieCatalogSnapshot() ?: return false
        applyCatalogSnapshot(snapshot)
        return true
    }

    private fun applyCatalogSnapshot(snapshot: LocalCatalogSnapshot<Movie>) {
        localCategories = snapshot.categories
        localMovies = snapshot.items
        val categories = localCategories.map { it.toUiCategory() }
            .filter { category -> playerSettings.allowsContent(category.label) }
        if (categories.isEmpty()) {
            _uiState.value = MoviesScreenState(
                categoriesLoading = false,
                errorMessage = "Aucune categorie Films retournee par Xtream.",
            )
            return
        }
        _uiState.update {
            val visibleCategories = categories.withSpecialCategories(
                allCount = localMovies.size.takeIf { it > 0 },
                favoriteCount = favoriteIds.size,
                historyCount = historyProgress.size,
                historySignals = historyCategorySignals,
            )
            it.copy(
                categoriesLoading = false,
                categories = visibleCategories,
                selectedCategoryId = HistoryMovieCategoryId,
                errorMessage = null,
            )
        }
        loadHistoryMovies()
    }

    fun selectCategory(category: MovieCategoryUi) {
        val current = _uiState.value
        if (current.selectedCategoryId == category.id && (current.movies.isNotEmpty() || current.moviesLoading)) {
            return
        }
        if (category.id == FavoriteMovieCategoryId) {
            loadFavoriteMovies()
            return
        }
        if (category.id == HistoryMovieCategoryId) {
            loadHistoryMovies()
            return
        }
        if (category.id == AllMovieCategoryId) {
            loadAllMovies()
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
        } else if (categoryId == HistoryMovieCategoryId) {
            loadHistoryMovies()
        } else if (categoryId == AllMovieCategoryId) {
            loadAllMovies()
        } else {
            loadMovies(categoryId)
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
            userContentRepository.observeFavoriteIds(UserContentType.Movie).collect { ids ->
                favoriteIds = ids
                _uiState.update { state ->
                    val refreshedMovies = if (state.selectedCategoryId == FavoriteMovieCategoryId) {
                        favoriteMovies()
                    } else {
                        state.movies.map { it.copy(isFavorite = it.streamId in ids) }
                    }
                    state.copy(
                        categories = state.categories.withSpecialCategories(
                            allCount = localMovies.size.takeIf { it > 0 },
                            favoriteCount = ids.size,
                            historyCount = historyProgress.size,
                            historySignals = historyCategorySignals,
                        ),
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

    private fun observeHistory() {
        viewModelScope.launch {
            userContentRepository.observeHistory(UserContentType.Movie).collect { progress ->
                historyProgress = progress.map { userContentRepository.enrichProgress(it) }
                    .distinctBy { it.contentId }
                historyCategorySignals = userContentRepository.resolveCategorySignals(historyProgress)
                _uiState.update { state ->
                    val historyMovies = historyMovies()
                    state.copy(
                        categories = state.categories.withSpecialCategories(
                            allCount = localMovies.size.takeIf { it > 0 },
                            favoriteCount = favoriteIds.size,
                            historyCount = historyProgress.size,
                            historySignals = historyCategorySignals,
                        ),
                        movies = if (state.selectedCategoryId == HistoryMovieCategoryId) historyMovies else state.movies,
                        focusedMovieId = if (state.selectedCategoryId == HistoryMovieCategoryId) historyMovies.firstOrNull()?.streamId else state.focusedMovieId,
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
                categories = state.categories.withSpecialCategories(
                    allCount = localMovies.size.takeIf { it > 0 },
                    favoriteCount = favoriteIds.size,
                    historyCount = historyProgress.size,
                    historySignals = historyCategorySignals,
                ),
                movies = movies,
                focusedMovieId = movies.firstOrNull()?.streamId,
                selectedMovieId = null,
            )
        }
    }

    private fun loadHistoryMovies() {
        moviesJob?.cancel()
        val movies = historyMovies()
        _uiState.update { state ->
            state.copy(
                moviesLoading = false,
                errorMessage = null,
                selectedCategoryId = HistoryMovieCategoryId,
                categories = state.categories.withSpecialCategories(
                    allCount = localMovies.size.takeIf { it > 0 },
                    favoriteCount = favoriteIds.size,
                    historyCount = historyProgress.size,
                    historySignals = historyCategorySignals,
                ),
                movies = movies,
                focusedMovieId = movies.firstOrNull()?.streamId,
                selectedMovieId = null,
            )
        }
    }

    private fun loadAllMovies() {
        moviesJob?.cancel()
        moviesJob = viewModelScope.launch {
            val categories = _uiState.value.categories
                .filterNot { it.id in SpecialMovieCategoryIds }
                .sortedByHistorySignals(historyCategorySignals) { it.id }
            val collected = mutableListOf<MovieItemUi>()
            val seen = mutableSetOf<Int>()
            _uiState.update {
                it.copy(
                    moviesLoading = true,
                    errorMessage = null,
                    selectedCategoryId = AllMovieCategoryId,
                    movies = emptyList(),
                    focusedMovieId = null,
                    selectedMovieId = null,
                )
            }
            runCatching {
                categories.forEach { category ->
                    localMovies.filter { it.categoryId == category.id }.forEach { movie ->
                        if (!seen.add(movie.streamId)) return@forEach
                        if (!playerSettings.allowsContent(movie.title, movie.rating, category.label)) return@forEach
                        collected += movie.toUiMovie(collected.size, category.label, xtreamRepository, favoriteIds)
                    }
                    _uiState.update { state ->
                        state.copy(
                            categories = state.categories.withSpecialCategories(
                                allCount = collected.size,
                                favoriteCount = favoriteIds.size,
                                historyCount = historyProgress.size,
                                historySignals = historyCategorySignals,
                            ),
                            movies = collected.toList(),
                            focusedMovieId = state.focusedMovieId ?: collected.firstOrNull()?.streamId,
                        )
                    }
                }
                collected.toList()
            }.onSuccess { movies ->
                _uiState.update { state ->
                    state.copy(
                        moviesLoading = false,
                        categories = state.categories.withSpecialCategories(
                            allCount = movies.size,
                            favoriteCount = favoriteIds.size,
                            historyCount = historyProgress.size,
                            historySignals = historyCategorySignals,
                        ),
                        movies = movies,
                        focusedMovieId = movies.firstOrNull()?.streamId,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        moviesLoading = false,
                        movies = collected.toList(),
                        focusedMovieId = collected.firstOrNull()?.streamId,
                        selectedMovieId = null,
                        errorMessage = error.userMessage("Impossible de charger tous les films Xtream."),
                    )
                }
            }
        }
    }

    private fun historyMovies(): List<MovieItemUi> =
        historyProgress.mapIndexedNotNull { index, progress ->
            val id = progress.contentId.toIntOrNull() ?: return@mapIndexedNotNull null
            if (!playerSettings.allowsContent(progress.title, progress.subtitle)) return@mapIndexedNotNull null
            MovieItemUi(
                streamId = id,
                number = (index + 1).toString().padStart(3, '0'),
                title = progress.title ?: "Film $id",
                posterUrl = progress.imageUrl,
                categoryLabel = "Historique",
                containerExtension = "mp4",
                rating = null,
                year = null,
                streamUrl = xtreamRepository.buildMovieStreamUrl(id),
                isFavorite = id in favoriteIds,
            )
        }

    private fun favoriteMovies(): List<MovieItemUi> =
        localMovies
            .filter { it.streamId in favoriteIds }
            .filter { movie ->
                val categoryLabel = localCategories
                    .firstOrNull { category -> category.id == movie.categoryId }
                    ?.name
                playerSettings.allowsContent(movie.title, movie.rating, categoryLabel)
            }
            .sortedBy { it.title }
            .mapIndexed { index, movie ->
                val categoryLabel = localCategories
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
                localMovies.filter { it.categoryId == categoryId }
                    .filter { movie -> playerSettings.allowsContent(movie.title, movie.rating, categoryLabel) }
                    .mapIndexed { index, movie ->
                    movie.toUiMovie(index, categoryLabel, xtreamRepository, favoriteIds)
                }
            }.onSuccess { movies ->
                _uiState.update { state ->
                    state.copy(
                        moviesLoading = false,
                        categories = state.categories.map {
                            if (it.id == categoryId) it.copy(count = movies.size) else it
                        }.withSpecialCategories(
                            allCount = localMovies.size.takeIf { it > 0 },
                            favoriteCount = favoriteIds.size,
                            historyCount = historyProgress.size,
                            historySignals = historyCategorySignals,
                        ),
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

    fun deleteHistoryMovie(movie: MovieItemUi) {
        viewModelScope.launch {
            userContentRepository.deleteProgress(UserContentType.Movie, movie.streamId)
        }
    }
}

private const val FavoriteMovieCategoryId = "__favorites_movies__"
private const val HistoryMovieCategoryId = "__history_movies__"
private const val AllMovieCategoryId = "__all_movies__"
private val SpecialMovieCategoryIds = setOf(AllMovieCategoryId, FavoriteMovieCategoryId, HistoryMovieCategoryId)

private fun List<MovieCategoryUi>.withSpecialCategories(
    allCount: Int?,
    favoriteCount: Int,
    historyCount: Int,
    historySignals: List<CategoryHistorySignal> = emptyList(),
): List<MovieCategoryUi> =
    listOf(
        MovieCategoryUi(
            id = AllMovieCategoryId,
            label = "ALL",
            count = allCount,
        ),
        MovieCategoryUi(
            id = FavoriteMovieCategoryId,
            label = "Favoris",
            count = favoriteCount,
        ),
        MovieCategoryUi(
            id = HistoryMovieCategoryId,
            label = "Historique",
            count = historyCount,
        ),
    ) + filterNot { it.id in SpecialMovieCategoryIds }
        .sortedByHistorySignals(historySignals) { it.id }

private fun Category.toUiCategory(): MovieCategoryUi =
    MovieCategoryUi(
        id = id,
        label = name,
        count = count,
    )

private fun XtreamMovieCategory.toUiCategory(): MovieCategoryUi =
    MovieCategoryUi(
        id = id,
        label = name,
        count = count,
    )

private fun Movie.toUiMovie(
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
        year = year?.take(4)?.takeIf { it.all(Char::isDigit) },
        streamUrl = xtreamRepository.buildMovieStreamUrl(streamId),
        isFavorite = streamId in favoriteIds,
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
