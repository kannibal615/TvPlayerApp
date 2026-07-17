package com.smartvision.svplayer.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.data.models.XtreamMovieCategory
import com.smartvision.svplayer.data.models.XtreamMovieStream
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.UserContentType
import com.smartvision.svplayer.data.repository.XtreamRepository
import com.smartvision.svplayer.data.tmdb.TmdbMatcher
import com.smartvision.svplayer.data.tmdb.TmdbRepository
import com.smartvision.svplayer.domain.model.Category
import com.smartvision.svplayer.domain.model.CategoryHistorySignal
import com.smartvision.svplayer.domain.model.Movie
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.domain.model.sortedByHistorySignals
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.domain.repository.SettingsRepository
import com.smartvision.svplayer.ui.settings.allowsContent
import com.smartvision.svplayer.ui.catalog.AllCategoryPolicy
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

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
    val backdropUrl: String? = null,
    val categoryLabel: String,
    val containerExtension: String,
    val genre: String?,
    val rating: String?,
    val year: String?,
    val duration: String?,
    val plot: String?,
    val director: String? = null,
    val cast: String? = null,
    val streamUrl: String,
    val isFavorite: Boolean = false,
    val externalMetadataLoaded: Boolean = false,
) {
    val subtitle: String =
        listOfNotNull(genre, rating?.let { "$it/10" }, year).joinToString(" | ").ifBlank { categoryLabel }
}

enum class MovieSortMode(val label: String) {
    DEFAULT("Ordre par defaut"), TITLE_ASC("Titre A - Z"), TITLE_DESC("Titre Z - A"),
    NEWEST("Date de sortie"), RATING("Mieux notes"), DURATION_ASC("Duree croissante"), DURATION_DESC("Duree decroissante"),
}

data class MoviesScreenState(
    val categoriesLoading: Boolean = true,
    val itemsLoading: Boolean = false,
    val moviesLoading: Boolean = false,
    val nextPageLoading: Boolean = false,
    val hasMoreItems: Boolean = false,
    val currentOffset: Int = 0,
    val errorMessage: String? = null,
    val categories: List<MovieCategoryUi> = emptyList(),
    val selectedCategoryId: String? = null,
    val contentSearchQuery: String = "",
    val movies: List<MovieItemUi> = emptyList(),
    val focusedMovieId: Int? = null,
    val selectedMovieId: Int? = null,
    val sortMode: MovieSortMode = MovieSortMode.DEFAULT,
) {
    val displayedMovies: List<MovieItemUi>
        get() = movies.sortedWith(sortMode.comparator())
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
    private val tmdbRepository: TmdbRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MoviesScreenState())
    val uiState: StateFlow<MoviesScreenState> = _uiState.asStateFlow()

    private var moviesJob: Job? = null
    private val tmdbMetadataJobs = mutableSetOf<Job>()
    private val tmdbEnrichmentSemaphore = Semaphore(TmdbEnrichmentConcurrency)
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

    fun setSortMode(mode: MovieSortMode) {
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
        moviesJob?.cancel()
        cancelTmdbMetadataJobs()
        localCategories = emptyList()
        _uiState.value = MoviesScreenState(categoriesLoading = true)
        loadCategories()
    }

    fun loadCategories() {
        moviesJob?.cancel()
        cancelTmdbMetadataJobs()
        val cachedCategories = catalogRepository.getCachedMovieCategories()
        if (!cachedCategories.isNullOrEmpty()) {
            applyCategories(cachedCategories)
            return
        }
        viewModelScope.launch {
            _uiState.value = MoviesScreenState(categoriesLoading = true)
            var initialApplied = false
            runCatching { catalogRepository.getInitialMovieCategoriesSnapshot(InitialCategoryLimit) }
                .onSuccess { categories ->
                    if (categories.isNotEmpty()) {
                        initialApplied = true
                        applyCategories(categories)
                    }
                }
            runCatching { catalogRepository.getMovieCategoriesSnapshot() }
                .onSuccess { categories -> applyCategories(categories) }
                .onFailure { error ->
                    if (!initialApplied) {
                        _uiState.value = MoviesScreenState(
                            categoriesLoading = false,
                            errorMessage = error.userMessage("Impossible de charger les categories Films."),
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
            _uiState.value = MoviesScreenState(
                categoriesLoading = false,
                errorMessage = "Aucune categorie Films retournee par Xtream.",
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
            val initialCategory = visibleCategories.initialCategoryForPlaylist()
            it.copy(
                categoriesLoading = false,
                categories = visibleCategories,
                selectedCategoryId = initialCategory?.id,
                errorMessage = null,
            )
        }
        when (_uiState.value.selectedCategoryId) {
            HistoryMovieCategoryId -> loadHistoryMovies()
            FavoriteMovieCategoryId -> loadFavoriteMovies()
            AllMovieCategoryId, null -> loadAllMovies()
            else -> _uiState.value.selectedCategoryId?.let(::loadMovies)
        }
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

    fun updateContentSearchQuery(query: String) {
        val cleanQuery = query.trim()
        val current = _uiState.value
        if (current.contentSearchQuery == cleanQuery) return
        _uiState.update { it.copy(contentSearchQuery = cleanQuery) }
        when (current.selectedCategoryId) {
            FavoriteMovieCategoryId -> loadFavoriteMovies()
            HistoryMovieCategoryId -> loadHistoryMovies()
            AllMovieCategoryId, null -> loadAllMovies()
            else -> current.selectedCategoryId?.let(::loadMovies)
        }
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
                val favoriteSelection = if (_uiState.value.selectedCategoryId == FavoriteMovieCategoryId) {
                    favoriteMovies(_uiState.value.contentSearchQuery)
                } else {
                    null
                }
                _uiState.update { state ->
                    val refreshedMovies = favoriteSelection
                        ?: state.movies.map { it.copy(isFavorite = it.streamId in ids) }
                    state.copy(
                        categories = state.categories.withSpecialCategories(
                            allCount = catalogTotalCount(),
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
                favoriteSelection?.let(::loadTmdbMetadataForMovies)
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            userContentRepository.observeHistory(UserContentType.Movie).collect { progress ->
                val enrichedProgress = progress.map { userContentRepository.enrichProgress(it) }
                    .distinctBy { it.contentId }
                val moviesById = catalogRepository
                    .getMoviesByIds(enrichedProgress.mapNotNull { it.contentId.toIntOrNull() })
                    .associateBy { it.streamId }
                historyProgress = enrichedProgress.filter { item ->
                    val movie = item.contentId.toIntOrNull()?.let(moviesById::get)
                    movie?.let {
                        playerSettings.allowsContent(it.title, it.plot, it.genre, it.categoryName)
                    } ?: playerSettings.allowsContent(item.title, item.subtitle)
                }
                historyCategorySignals = userContentRepository.resolveCategorySignals(historyProgress)
                val refreshedHistoryMovies = historyMovies(_uiState.value.contentSearchQuery)
                _uiState.update { state ->
                    state.copy(
                        categories = state.categories.withSpecialCategories(
                            allCount = catalogTotalCount(),
                            favoriteCount = favoriteIds.size,
                            historyCount = historyProgress.size,
                            historySignals = historyCategorySignals,
                        ),
                        movies = if (state.selectedCategoryId == HistoryMovieCategoryId) refreshedHistoryMovies else state.movies,
                        focusedMovieId = if (state.selectedCategoryId == HistoryMovieCategoryId) refreshedHistoryMovies.firstOrNull()?.streamId else state.focusedMovieId,
                    )
                }
                if (_uiState.value.selectedCategoryId == HistoryMovieCategoryId) {
                    loadTmdbMetadataForMovies(refreshedHistoryMovies)
                }
            }
        }
    }

    private fun loadFavoriteMovies() {
        moviesJob?.cancel()
        cancelTmdbMetadataJobs()
        moviesJob = viewModelScope.launch {
            val movies = favoriteMovies(_uiState.value.contentSearchQuery)
            _uiState.update { state ->
                state.copy(
                    itemsLoading = false,
                    moviesLoading = false,
                    nextPageLoading = false,
                    hasMoreItems = false,
                    currentOffset = movies.size,
                    errorMessage = null,
                    selectedCategoryId = FavoriteMovieCategoryId,
                    categories = state.categories.withSpecialCategories(
                        allCount = catalogTotalCount(),
                        favoriteCount = favoriteIds.size,
                        historyCount = historyProgress.size,
                        historySignals = historyCategorySignals,
                    ),
                    movies = movies,
                    focusedMovieId = movies.firstOrNull()?.streamId,
                    selectedMovieId = null,
                )
            }
            loadTmdbMetadataForMovies(movies)
        }
    }

    private fun loadHistoryMovies() {
        moviesJob?.cancel()
        cancelTmdbMetadataJobs()
        val movies = historyMovies(_uiState.value.contentSearchQuery)
        _uiState.update { state ->
            state.copy(
                moviesLoading = false,
                itemsLoading = false,
                nextPageLoading = false,
                hasMoreItems = false,
                currentOffset = movies.size,
                errorMessage = null,
                selectedCategoryId = HistoryMovieCategoryId,
                categories = state.categories.withSpecialCategories(
                    allCount = catalogTotalCount(),
                    favoriteCount = favoriteIds.size,
                    historyCount = historyProgress.size,
                    historySignals = historyCategorySignals,
                ),
                movies = movies,
                focusedMovieId = movies.firstOrNull()?.streamId,
                selectedMovieId = null,
            )
        }
        loadTmdbMetadataForMovies(movies)
    }

    private fun loadAllMovies() {
        loadMoviePage(categoryId = null, selectedCategoryId = AllMovieCategoryId, categoryLabel = "Films", replace = true)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.categoriesLoading || state.itemsLoading || state.nextPageLoading || !state.hasMoreItems) return
        val selectedCategoryId = state.selectedCategoryId ?: return
        if (selectedCategoryId in setOf(FavoriteMovieCategoryId, HistoryMovieCategoryId)) return
        val categoryId = selectedCategoryId.takeUnless { it == AllMovieCategoryId }
        val categoryLabel = state.selectedCategory?.label ?: "Films"
        loadMoviePage(
            categoryId = categoryId,
            selectedCategoryId = selectedCategoryId,
            categoryLabel = categoryLabel,
            query = state.contentSearchQuery,
            replace = false,
        )
    }

    private fun historyMovies(query: String = ""): List<MovieItemUi> =
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
                genre = null,
                rating = null,
                year = null,
                duration = progress.durationMs.takeIf { it > 0L }?.let(::formatDurationMs),
                plot = progress.subtitle,
                streamUrl = xtreamRepository.buildMovieStreamUrl(id),
                isFavorite = id in favoriteIds,
            )
        }.filter { it.matchesSearch(query) }

    private suspend fun favoriteMovies(query: String = ""): List<MovieItemUi> =
        catalogRepository.getMoviesByIds(favoriteIds.toList())
            .filter { movie ->
                playerSettings.allowsContent(movie.title, movie.plot, movie.genre, movie.categoryName)
            }
            .sortedBy { it.title }
            .mapIndexed { index, movie ->
                movie.toUiMovie(index, movie.categoryName.ifBlank { "Favoris" }, xtreamRepository, favoriteIds)
            }
            .filter { it.matchesSearch(query) }

    private fun loadMovies(categoryId: String) {
        val categoryLabel = _uiState.value.categories.firstOrNull { it.id == categoryId }?.label ?: "Films"
        loadMoviePage(categoryId = categoryId, selectedCategoryId = categoryId, categoryLabel = categoryLabel, replace = true)
    }

    private fun loadMoviePage(
        categoryId: String?,
        selectedCategoryId: String,
        categoryLabel: String,
        query: String = _uiState.value.contentSearchQuery,
        replace: Boolean,
    ) {
        if (replace) {
            moviesJob?.cancel()
            cancelTmdbMetadataJobs()
        }
        moviesJob = viewModelScope.launch {
            val startOffset = if (replace) 0 else _uiState.value.currentOffset
            val previousMovies = if (replace) emptyList() else _uiState.value.movies
            _uiState.update {
                it.copy(
                    itemsLoading = replace,
                    moviesLoading = replace,
                    nextPageLoading = !replace,
                    errorMessage = null,
                    selectedCategoryId = selectedCategoryId,
                    movies = if (replace) emptyList() else it.movies,
                    focusedMovieId = if (replace) null else it.focusedMovieId,
                    selectedMovieId = if (replace) null else it.selectedMovieId,
                )
            }
            runCatching {
                val page = if (query.isNotBlank()) {
                    catalogRepository.searchMoviesPage(categoryId, query, startOffset, MovieItemsPageSize)
                } else if (categoryId == null) {
                    catalogRepository.getAllMoviesPage(startOffset, MovieItemsPageSize)
                } else {
                    catalogRepository.getMoviesPage(categoryId, startOffset, MovieItemsPageSize)
                }
                val visiblePage = page
                    .filter { movie -> playerSettings.allowsContent(movie.title, movie.plot, movie.genre, categoryLabel) }
                    .mapIndexed { index, movie ->
                        movie.toUiMovie(
                            index = startOffset + index,
                            categoryLabel = movie.categoryName.ifBlank { categoryLabel },
                            xtreamRepository = xtreamRepository,
                            favoriteIds = favoriteIds,
                        )
                    }
                PageLoadResult(
                    items = (previousMovies + visiblePage).distinctBy { it.streamId },
                    loadedItems = visiblePage,
                    rawPageSize = page.size,
                )
            }.onSuccess { result ->
                _uiState.update { state ->
                    state.copy(
                        itemsLoading = false,
                        moviesLoading = false,
                        nextPageLoading = false,
                        hasMoreItems = result.rawPageSize == MovieItemsPageSize,
                        currentOffset = startOffset + result.rawPageSize,
                        categories = state.categories.withSpecialCategories(
                            allCount = catalogTotalCount(),
                            favoriteCount = favoriteIds.size,
                            historyCount = historyProgress.size,
                            historySignals = historyCategorySignals,
                        ),
                        movies = result.items,
                        focusedMovieId = state.focusedMovieId ?: result.items.firstOrNull()?.streamId,
                        errorMessage = null,
                    )
                }
                loadTmdbMetadataForMovies(result.loadedItems)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        itemsLoading = false,
                        moviesLoading = false,
                        nextPageLoading = false,
                        movies = if (replace) emptyList() else it.movies,
                        focusedMovieId = if (replace) null else it.focusedMovieId,
                        selectedMovieId = if (replace) null else it.selectedMovieId,
                        errorMessage = error.userMessage("Impossible de charger les films Xtream."),
                    )
                }
            }
        }
    }

    private fun loadTmdbMetadataForMovies(movies: List<MovieItemUi>) {
        if (movies.isEmpty()) return
        val job = viewModelScope.launch {
            movies.distinctBy { it.streamId }.chunked(TmdbMetadataUpdateBatchSize).forEach { batch ->
                batch.map { movie ->
                    async {
                        val metadata = try {
                            tmdbEnrichmentSemaphore.withPermit {
                                tmdbRepository.enrichMovie(
                                    contentId = movie.streamId,
                                    title = movie.title,
                                    year = movie.year,
                                    language = playerSettings.language,
                                    includeAdult = !playerSettings.parentalControlEnabled,
                                )
                            }
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            null
                        } ?: return@async null
                        val enrichedMovie = movie.copy(
                            title = metadata.title.nonBlank() ?: movie.title,
                            posterUrl = metadata.posterUrl.nonBlank() ?: movie.posterUrl,
                            backdropUrl = metadata.backdropUrl.nonBlank() ?: movie.backdropUrl,
                            genre = metadata.genres.nonBlank() ?: movie.genre,
                            rating = metadata.voteAverage?.takeIf { it > 0.0 }?.formatRating() ?: movie.rating,
                            year = metadata.releaseDate?.take(4)?.takeIf { it.all(Char::isDigit) } ?: movie.year,
                            duration = metadata.runtimeMinutes?.takeIf { it > 0 }?.let(::formatMinutes) ?: movie.duration,
                            plot = metadata.overview.nonBlank() ?: movie.plot,
                            director = metadata.director.nonBlank() ?: movie.director,
                            cast = metadata.cast.nonBlank() ?: movie.cast,
                            externalMetadataLoaded = true,
                        )
                        // Publish every resolved row immediately. Waiting for all
                        // 12 requests made one slow TMDB lookup keep the other
                        // visible rows on unreliable Xtream metadata for minutes.
                        _uiState.update { state ->
                            if (state.movies.none { it.streamId == movie.streamId }) {
                                state
                            } else {
                                state.copy(
                                    movies = state.movies.map { current ->
                                        if (current.streamId == movie.streamId) enrichedMovie else current
                                    },
                                )
                            }
                        }
                        movie.streamId
                    }
                }.awaitAll()
            }
        }
        tmdbMetadataJobs += job
        job.invokeOnCompletion { tmdbMetadataJobs -= job }
    }

    private fun cancelTmdbMetadataJobs() {
        val jobs = tmdbMetadataJobs.toList()
        tmdbMetadataJobs.clear()
        jobs.forEach { it.cancel() }
    }

    fun deleteHistoryMovie(movie: MovieItemUi) {
        viewModelScope.launch {
            userContentRepository.deleteProgress(UserContentType.Movie, movie.streamId)
        }
    }

    private fun catalogTotalCount(): Int? =
        localCategories.sumOf { it.count }.takeIf { it > 0 }
}

private fun MovieSortMode.comparator(): Comparator<MovieItemUi> = when (this) {
    MovieSortMode.DEFAULT -> compareBy { it.number.toIntOrNull() ?: Int.MAX_VALUE }
    MovieSortMode.TITLE_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
    MovieSortMode.TITLE_DESC -> compareByDescending<MovieItemUi> { it.title.lowercase(Locale.ROOT) }
    MovieSortMode.NEWEST -> compareByDescending<MovieItemUi> { it.year?.take(4)?.toIntOrNull() ?: Int.MIN_VALUE }
    MovieSortMode.RATING -> compareByDescending<MovieItemUi> { it.rating?.replace(',', '.')?.toDoubleOrNull() ?: Double.NEGATIVE_INFINITY }
    MovieSortMode.DURATION_ASC -> compareBy { it.duration.durationMinutes() }
    MovieSortMode.DURATION_DESC -> compareByDescending<MovieItemUi> { it.duration.durationMinutes() }
}.thenBy { it.streamId }

private fun String?.durationMinutes(): Int = this?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() } ?: Int.MAX_VALUE

private data class PageLoadResult<T>(
    val items: List<T>,
    val loadedItems: List<T>,
    val rawPageSize: Int,
)

private const val MovieItemsPageSize = 72
private const val TmdbMetadataUpdateBatchSize = 12
private const val TmdbEnrichmentConcurrency = 2
private const val InitialCategoryLimit = 20
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
    ) + filterNot { it.id in SpecialMovieCategoryIds || AllCategoryPolicy.isEquivalent(it.label) }
        .sortedByHistorySignals(historySignals) { it.id }

private fun List<MovieCategoryUi>.initialCategoryForPlaylist(): MovieCategoryUi? =
    firstOrNull { it.id == AllMovieCategoryId && (it.count ?: 0) > 0 }
        ?: firstOrNull { it.id !in setOf(FavoriteMovieCategoryId, HistoryMovieCategoryId) && (it.count ?: 0) > 0 }
        ?: firstOrNull()

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
        genre = genre?.takeIf { it.isNotBlank() },
        rating = rating?.takeIf { it.isNotBlank() },
        year = TmdbMatcher.extractYear(year, title),
        duration = duration?.takeIf { it.isNotBlank() },
        plot = plot?.takeIf { it.isNotBlank() },
        streamUrl = xtreamRepository.buildMovieStreamUrl(streamId, containerExtension),
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
        genre = null,
        rating = rating?.takeIf { it.isNotBlank() },
        year = TmdbMatcher.extractYear(added, title),
        duration = null,
        plot = null,
        streamUrl = xtreamRepository.buildMovieStreamUrl(this),
        isFavorite = streamId in favoriteIds,
    )

private fun MovieItemUi.matchesSearch(query: String): Boolean {
    if (query.isBlank()) return true
    return title.contains(query, ignoreCase = true) ||
        genre?.contains(query, ignoreCase = true) == true ||
        year?.contains(query, ignoreCase = true) == true
}

private fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val remaining = minutes % 60
    return when {
        hours > 0 && remaining > 0 -> "${hours}h ${remaining}m"
        hours > 0 -> "${hours}h"
        else -> "${remaining}m"
    }
}

private fun formatDurationMs(durationMs: Long): String =
    formatMinutes((durationMs / 60_000L).toInt().coerceAtLeast(1))

private fun String.cleanTitle(): String =
    TmdbMatcher.cleanDisplayTitle(this)
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" 4K", "", ignoreCase = true)
        .trim()
        .ifBlank { "Film" }

private fun String?.nonBlank(): String? = this?.trim()?.takeIf { it.isNotBlank() }

private fun Double.formatRating(): String = String.format(Locale.US, "%.1f", this)

private fun Throwable.userMessage(fallback: String): String =
    when (this) {
        is IllegalStateException -> message ?: fallback
        else -> fallback
    }
