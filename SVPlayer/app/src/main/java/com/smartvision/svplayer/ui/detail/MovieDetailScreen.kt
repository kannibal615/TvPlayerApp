package com.smartvision.svplayer.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.behavior.BehaviorContent
import com.smartvision.svplayer.data.models.XtreamMovieDetails
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.UserContentType
import com.smartvision.svplayer.data.repository.XtreamRepository
import com.smartvision.svplayer.data.tmdb.TmdbMovieMetadata
import com.smartvision.svplayer.data.tmdb.TmdbRepository
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class MovieDetailUiState(
    val movieId: Int,
    val title: String = "Film",
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val categoryLabel: String = "Films",
    val plot: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val rating: String? = null,
    val duration: String? = null,
    val director: String? = null,
    val cast: String? = null,
    val extension: String = "mp4",
    val isFavorite: Boolean = false,
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val tmdbMetadata: TmdbMovieMetadata? = null,
    val tmdbLoading: Boolean = false,
) {
    val displayTitle: String
        get() = tmdbMetadata?.title.nonBlank() ?: title

    val displayPosterUrl: String?
        get() = tmdbMetadata?.posterUrl.nonBlank() ?: posterUrl

    val displayBackdropUrl: String?
        get() = tmdbMetadata?.backdropUrl.nonBlank() ?: backdropUrl

    val displayPlot: String?
        get() = tmdbMetadata?.overview.nonBlank() ?: plot

    val displayGenre: String?
        get() = tmdbMetadata?.genres.nonBlank() ?: genre

    val displayReleaseDate: String?
        get() = tmdbMetadata?.releaseDate.nonBlank() ?: releaseDate

    val displayRating: String?
        get() = tmdbMetadata?.voteAverage?.takeIf { it > 0.0 }?.formatRating() ?: rating

    val displayDurationLabel: String?
        get() = tmdbMetadata?.runtimeMinutes?.takeIf { it > 0 }?.let { "$it min" } ?: duration?.durationLabel()

    val displayDirector: String?
        get() = tmdbMetadata?.director.nonBlank() ?: director

    val displayCast: String?
        get() = tmdbMetadata?.cast.nonBlank() ?: cast

    val isTmdbEnriched: Boolean
        get() = tmdbMetadata != null

    val backgroundUrl: String?
        get() = displayBackdropUrl ?: displayPosterUrl

    val year: String?
        get() = displayReleaseDate?.take(4)?.takeIf { it.all(Char::isDigit) }
}

class MovieDetailViewModel(
    private val movieId: Int,
    private val xtreamRepository: XtreamRepository,
    private val userContentRepository: UserContentRepository,
    private val tmdbRepository: TmdbRepository,
) : ViewModel() {
    private val initialMovie = xtreamRepository.getCachedMovie(movieId)
    private val initialCategory = initialMovie?.categoryId?.let { categoryId ->
        xtreamRepository.getCachedMovieCategories().firstOrNull { it.id == categoryId }?.name
    } ?: "Films"

    private val _uiState = MutableStateFlow(
        MovieDetailUiState(
            movieId = movieId,
            title = initialMovie?.title?.cleanDetailTitle() ?: "Film $movieId",
            posterUrl = initialMovie?.posterUrl,
            categoryLabel = initialCategory,
            rating = initialMovie?.rating,
            extension = initialMovie?.containerExtension ?: "mp4",
        ),
    )
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()
    private var lastTmdbRequestKey: String? = null

    init {
        observeFavorite()
        loadDetails()
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            userContentRepository.toggleFavorite(UserContentType.Movie, movieId)
        }
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, errorMessage = null) }
            runCatching { xtreamRepository.getMovieDetails(movieId) }
                .onSuccess { details ->
                    val current = _uiState.value
                    _uiState.value = details.toUiState(
                        categoryLabel = details.categoryId?.let { categoryId ->
                            xtreamRepository.getCachedMovieCategories().firstOrNull { it.id == categoryId }?.name
                        } ?: initialCategory,
                        isFavorite = current.isFavorite,
                    ).copy(
                        tmdbMetadata = current.tmdbMetadata,
                        tmdbLoading = current.tmdbLoading,
                    )
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            errorMessage = error.message ?: "Details film indisponibles.",
                        )
                    }
                }
        }
    }

    fun loadTmdbMetadata(language: String, includeAdult: Boolean) {
        val current = _uiState.value
        if (current.loading || current.title.isBlank()) return
        val requestKey = listOf(movieId, current.title, current.releaseDate.orEmpty(), language, includeAdult).joinToString("|")
        if (requestKey == lastTmdbRequestKey) return
        lastTmdbRequestKey = requestKey
        viewModelScope.launch {
            _uiState.update { it.copy(tmdbLoading = true) }
            runCatching {
                tmdbRepository.enrichMovie(
                    contentId = movieId,
                    title = current.title,
                    year = current.releaseDate,
                    language = language,
                    includeAdult = includeAdult,
                )
            }.onSuccess { metadata ->
                _uiState.update {
                    it.copy(
                        tmdbMetadata = metadata ?: it.tmdbMetadata,
                        tmdbLoading = false,
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(tmdbLoading = false) }
            }
        }
    }

    private fun observeFavorite() {
        viewModelScope.launch {
            userContentRepository.observeFavoriteIds(UserContentType.Movie).collect { ids ->
                _uiState.update { it.copy(isFavorite = movieId in ids) }
            }
        }
    }
}

@Composable
fun MovieDetailRoute(
    movieId: Int,
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
    onLicenseKey: () -> Unit,
    showLicenseKey: Boolean,
    hasNewNotifications: Boolean,
    notificationBadgeCount: Int,
    onWatchMovie: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = PlayerSettings(),
    )
    val viewModel: MovieDetailViewModel = viewModel(
        key = "movie-detail-$movieId",
        factory = viewModelFactory {
            MovieDetailViewModel(
                movieId = movieId,
                xtreamRepository = container.xtreamRepository,
                userContentRepository = container.userContentRepository,
                tmdbRepository = container.tmdbRepository,
            )
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val behaviorScope = rememberCoroutineScope()
    LaunchedEffect(state.movieId, state.categoryLabel) {
        container.behaviorReporter.report(
            "CONTENT_OPENED",
            state.toBehaviorContent("DETAIL"),
        )
    }
    LaunchedEffect(
        state.loading,
        state.movieId,
        state.title,
        state.releaseDate,
        settings.language,
        settings.parentalControlEnabled,
    ) {
        if (!state.loading) {
            viewModel.loadTmdbMetadata(
                language = settings.language,
                includeAdult = !settings.parentalControlEnabled,
            )
        }
    }
    MovieDetailScreen(
        state = state,
        currentRoute = currentRoute,
        tabs = tabs,
        onNavigate = onNavigate,
        onSync = onSync,
        onSettings = onSettings,
        onProfile = onProfile,
        onNotifications = onNotifications,
        onLicenseKey = onLicenseKey,
        showLicenseKey = showLicenseKey,
        hasNewNotifications = hasNewNotifications,
        notificationBadgeCount = notificationBadgeCount,
        onRetry = viewModel::loadDetails,
        onWatchMovie = {
            onWatchMovie(state.movieId)
        },
        onFavorite = {
            container.behaviorReporter.reportAsync(
                behaviorScope,
                if (state.isFavorite) "FAVORITE_REMOVED" else "FAVORITE_ADDED",
                state.toBehaviorContent("DETAIL"),
            )
            viewModel.toggleFavorite()
        },
        modifier = modifier,
    )
}

@Composable
private fun MovieDetailScreen(
    state: MovieDetailUiState,
    currentRoute: String,
    tabs: List<HomeHeaderTab>,
    onNavigate: (String) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
    onLicenseKey: () -> Unit,
    showLicenseKey: Boolean,
    hasNewNotifications: Boolean,
    notificationBadgeCount: Int,
    onRetry: () -> Unit,
    onWatchMovie: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playFocusRequester = androidx.compose.runtime.remember { FocusRequester() }
    LaunchedEffect(state.movieId) {
        delay(120)
        runCatching { playFocusRequester.requestFocus() }
    }

    DetailBackground(
        imageUrl = state.backgroundUrl,
        modifier = modifier,
    ) {
        DetailHeader(
            currentRoute = currentRoute,
            tabs = tabs,
            onNavigate = onNavigate,
            onSync = onSync,
            onSettings = onSettings,
            onProfile = onProfile,
            onNotifications = onNotifications,
            onLicenseKey = onLicenseKey,
            showLicenseKey = showLicenseKey,
            hasNewNotifications = hasNewNotifications,
            notificationBadgeCount = notificationBadgeCount,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DetailDimens.ScreenPadding)
                .padding(top = DetailDimens.HeaderTop),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DetailDimens.ScreenPadding)
                .padding(top = 86.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(34.dp),
                ) {
                    MovieDetailInfo(
                        state = state,
                        onWatchMovie = onWatchMovie,
                        onRetry = onRetry,
                        onFavorite = onFavorite,
                        playFocusRequester = playFocusRequester,
                        modifier = Modifier.width(720.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    MoviePosterPanel(
                        state = state,
                        modifier = Modifier
                            .width(330.dp)
                            .align(Alignment.Top),
                    )
                }
            }
            item {
                DetailVideoSection(videos = state.tmdbMetadata?.videos.orEmpty())
            }
            item {
                DetailPeopleSection(title = "Cast", people = state.tmdbMetadata?.castMembers.orEmpty())
            }
            item {
                DetailPeopleSection(title = "Director", people = state.tmdbMetadata?.directors.orEmpty())
            }
            item {
                DetailUserRatingSection(
                    contentKey = "movie:${state.movieId}",
                    tmdbRating = state.displayRating,
                    voteCount = state.tmdbMetadata?.voteCount,
                )
            }
            item {
                DetailRecommendationsSection(recommendations = state.tmdbMetadata?.recommendations.orEmpty())
            }
        }
    }
}

@Composable
private fun MovieDetailInfo(
    state: MovieDetailUiState,
    onWatchMovie: () -> Unit,
    onRetry: () -> Unit,
    onFavorite: () -> Unit,
    playFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = state.displayTitle,
            color = SmartVisionColors.TextPrimary,
            style = DetailHeroTitleStyle,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOfNotNull(state.year, state.displayGenre, state.displayDurationLabel).take(3).forEach { meta ->
                Text(
                    text = meta,
                    color = SmartVisionColors.TextSecondary,
                    style = DetailBodyStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("•", color = SmartVisionColors.TextSecondary, style = DetailBodyStyle)
            }
            state.displayRating?.let { DetailBadge(text = "$it/10") }
            DetailBadge(text = state.extension.uppercase())
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = state.displayPlot ?: "Film VOD disponible dans ${state.categoryLabel}.",
            color = SmartVisionColors.TextSecondary,
            style = DetailBodyStyle,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(560.dp),
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailActionButton(
                text = "Regarder",
                icon = Icons.Default.PlayArrow,
                onClick = onWatchMovie,
                primary = true,
                focusRequester = playFocusRequester,
                modifier = Modifier
                    .width(168.dp)
                    .height(DetailDimens.ActionHeight),
            )
            DetailActionButton(
                text = if (state.isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                icon = Icons.Default.FavoriteBorder,
                onClick = onFavorite,
                modifier = Modifier
                    .width(184.dp)
                    .height(DetailDimens.ActionHeight),
            )
        }

        Spacer(Modifier.height(18.dp))

        if (state.loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = SmartVisionColors.Primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text("Chargement des details", color = SmartVisionColors.TextSecondary, style = DetailBodyStyle)
            }
        } else if (state.errorMessage != null) {
            DetailActionButton(
                text = "Reessayer",
                icon = Icons.Default.Sync,
                onClick = onRetry,
                modifier = Modifier
                    .width(136.dp)
                    .height(DetailDimens.ActionHeight),
            )
        }

        MovieMetaPanel(state = state)
    }
}

@Composable
private fun MoviePosterPanel(
    state: MovieDetailUiState,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .aspectRatio(0.68f)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.42f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (!state.displayPosterUrl.isNullOrBlank()) {
            AsyncImage(
                model = state.displayPosterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Theaters,
                contentDescription = null,
                tint = SmartVisionColors.TextSecondary,
                modifier = Modifier.size(88.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    ),
                ),
        )
        Text(
            text = state.displayTitle,
            color = Color.White,
            style = DetailTitleStyle,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
        )
    }
}

@Composable
private fun MovieMetaPanel(
    state: MovieDetailUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0x98101A2B))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.13f)), RoundedCornerShape(9.dp))
            .padding(16.dp),
    ) {
        Text("Details", color = SmartVisionColors.TextPrimary, style = DetailTitleStyle)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailBadge(text = state.categoryLabel)
            if (state.isTmdbEnriched) {
                DetailBadge(text = "TMDB", color = Color(0xFF18253A))
            }
            state.displayDirector?.let { DetailBadge(text = it, color = Color(0xFF18253A)) }
            state.tmdbMetadata?.certification.nonBlank()?.let { DetailBadge(text = it, color = Color(0xFF18253A)) }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = state.displayCast ?: "Catalogue Xtream VOD",
            color = SmartVisionColors.TextSecondary,
            style = DetailBodyStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        state.tmdbMetadata?.providersSummary.nonBlank()?.let { providers ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Providers: $providers",
                color = SmartVisionColors.TextSecondary,
                style = DetailMetaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun XtreamMovieDetails.toUiState(
    categoryLabel: String,
    isFavorite: Boolean,
): MovieDetailUiState =
    MovieDetailUiState(
        movieId = movieId,
        title = title.cleanDetailTitle(),
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        categoryLabel = categoryLabel,
        plot = plot,
        genre = genre,
        releaseDate = releaseDate,
        rating = rating,
        duration = duration,
        director = director,
        cast = cast,
        extension = containerExtension,
        isFavorite = isFavorite,
        loading = false,
    )

private fun MovieDetailUiState.toBehaviorContent(sourceScreen: String): BehaviorContent =
    BehaviorContent(
        contentType = "MOVIE",
        contentId = movieId.toString(),
        title = displayTitle,
        categoryLabel = categoryLabel,
        durationSeconds = duration?.toLongOrNull(),
        engagementScore = if (loading) 25 else 45,
        sourceScreen = sourceScreen,
        tags = listOfNotNull(displayGenre, extension, year),
        context = mapOf("rating" to (displayRating ?: "-")),
    )

private fun String?.nonBlank(): String? = this?.trim()?.takeIf { it.isNotBlank() }

private fun Double.formatRating(): String = String.format(Locale.US, "%.1f", this)

private fun String.cleanDetailTitle(): String =
    replace(Regex("\\s+"), " ")
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" 4K", "", ignoreCase = true)
        .trim()
        .ifBlank { "Film" }

private fun String.durationLabel(): String {
    val seconds = toLongOrNull() ?: return this
    if (seconds < 300) return this
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours} h ${minutes} min"
        hours > 0 -> "${hours} h"
        else -> "${minutes} min"
    }
}
