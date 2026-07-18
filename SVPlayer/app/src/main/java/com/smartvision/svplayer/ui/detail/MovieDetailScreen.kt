package com.smartvision.svplayer.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
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
import com.smartvision.svplayer.data.tmdb.TmdbPersonCredit
import com.smartvision.svplayer.data.tmdb.TmdbRepository
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
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
        get() = tmdbMetadata?.runtimeMinutes?.takeIf { it > 0 }?.let(::formatRuntimeMinutes)
            ?: duration?.durationLabel()

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
    private var tmdbMetadataJob: Job? = null

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

    fun loadTmdbMetadata(language: String, includeAdult: Boolean, enabled: Boolean) {
        if (!enabled) {
            tmdbMetadataJob?.cancel()
            lastTmdbRequestKey = null
            _uiState.update { it.copy(tmdbMetadata = null, tmdbLoading = false) }
            return
        }
        val current = _uiState.value
        if (current.loading || current.title.isBlank()) return
        val requestKey = listOf(movieId, current.title, current.releaseDate.orEmpty(), language, includeAdult).joinToString("|")
        if (requestKey == lastTmdbRequestKey) return
        lastTmdbRequestKey = requestKey
        tmdbMetadataJob?.cancel()
        tmdbMetadataJob = viewModelScope.launch {
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
                        tmdbMetadata = metadata,
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
        settings.tmdbApiEnabled,
    ) {
        if (!state.loading) {
            viewModel.loadTmdbMetadata(
                language = settings.language,
                includeAdult = !settings.parentalControlEnabled,
                enabled = settings.tmdbApiEnabled,
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
    val currentTabFocusRequester = androidx.compose.runtime.remember { FocusRequester() }
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
            currentTabFocusRequester = currentTabFocusRequester,
            contentDownFocusRequester = playFocusRequester,
            onContentDown = { runCatching { playFocusRequester.requestFocus() } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DetailDimens.ScreenPadding)
                .padding(top = DetailDimens.HeaderTop),
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DetailDimens.ScreenPadding)
                .padding(top = 86.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(34.dp),
            verticalAlignment = Alignment.Top,
        ) {
            MovieDetailInfo(
                state = state,
                onWatchMovie = onWatchMovie,
                onRetry = onRetry,
                onFavorite = onFavorite,
                playFocusRequester = playFocusRequester,
                headerFocusRequester = currentTabFocusRequester,
                modifier = Modifier.width(720.dp),
            )
            Spacer(Modifier.weight(1f))
            MoviePosterPanel(
                state = state,
                modifier = Modifier.width(300.dp),
            )
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
    headerFocusRequester: FocusRequester,
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
                bringIntoViewOnFocus = false,
                modifier = Modifier
                    .focusProperties { up = headerFocusRequester }
                    .width(168.dp)
                    .height(DetailDimens.ActionHeight),
            )
            DetailActionButton(
                text = if (state.isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                icon = Icons.Default.FavoriteBorder,
                onClick = onFavorite,
                modifier = Modifier
                    .focusProperties { up = headerFocusRequester }
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

        MovieCastStrip(
            people = state.tmdbMetadata?.castMembers.orEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(194.dp),
        )
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
    }
}

@Composable
private fun MovieCastStrip(
    people: List<TmdbPersonCredit>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text("Cast", color = SmartVisionColors.TextPrimary, style = DetailTitleStyle)
        Spacer(Modifier.height(8.dp))
        if (people.isEmpty()) {
            Text("Actor photos unavailable", color = SmartVisionColors.TextSecondary, style = DetailMetaStyle)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 12.dp),
            ) {
                items(people.take(10), key = { "${it.name}:${it.role.orEmpty()}" }) { person ->
                    val focusState = rememberTvFocusState()
                    Column(
                        modifier = Modifier
                            .width(94.dp)
                            .tvFocusTarget(
                                state = focusState,
                                focusedScale = 1.06f,
                                glowColor = SmartVisionColors.Primary,
                                cornerRadius = 6.dp,
                            )
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (focusState.isFocused) {
                                    SmartVisionColors.Primary.copy(alpha = 0.20f)
                                } else {
                                    Color.Transparent
                                },
                            )
                            .focusable(),
                    ) {
                        AsyncImage(
                            model = person.profileUrl,
                            contentDescription = person.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(94.dp)
                                .height(110.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.35f)),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            person.name,
                            color = SmartVisionColors.TextPrimary,
                            style = DetailMetaStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
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
    val clean = trim()
    val colonParts = clean.split(':').mapNotNull(String::toLongOrNull)
    val minutes = when {
        colonParts.size == 3 -> colonParts[0] * 60 + colonParts[1] + colonParts[2] / 60
        colonParts.size == 2 -> colonParts[0] * 60 + colonParts[1]
        clean.toLongOrNull() != null -> {
            val numeric = clean.toLong()
            if (numeric >= 300) numeric / 60 else numeric
        }
        else -> return clean
    }
    return formatRuntimeMinutes(minutes.toInt())
}

private fun formatRuntimeMinutes(totalMinutes: Int): String {
    val safeMinutes = totalMinutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val minutes = safeMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
