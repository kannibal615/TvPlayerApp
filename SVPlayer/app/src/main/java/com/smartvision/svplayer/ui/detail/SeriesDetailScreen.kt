package com.smartvision.svplayer.ui.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tv
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.behavior.BehaviorContent
import com.smartvision.svplayer.data.models.XtreamSeriesDetails
import com.smartvision.svplayer.data.models.XtreamSeriesEpisode
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.UserContentType
import com.smartvision.svplayer.data.repository.XtreamRepository
import com.smartvision.svplayer.data.tmdb.TmdbSeriesMetadata
import com.smartvision.svplayer.data.tmdb.TmdbRepository
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SeriesDetailEpisodeUi(
    val episodeId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val duration: String?,
    val plot: String?,
) {
    val code: String = "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
}

data class SeriesDetailUiState(
    val seriesId: Int,
    val title: String = "Serie",
    val coverUrl: String? = null,
    val backdropUrl: String? = null,
    val categoryLabel: String = "Series",
    val plot: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val rating: String? = null,
    val episodeRunTime: String? = null,
    val cast: String? = null,
    val episodes: List<SeriesDetailEpisodeUi> = emptyList(),
    val selectedSeason: Int = 1,
    val isFavorite: Boolean = false,
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val tmdbMetadata: TmdbSeriesMetadata? = null,
    val tmdbLoading: Boolean = false,
) {
    val displayTitle: String
        get() = tmdbMetadata?.name.nonBlank() ?: title

    val displayCoverUrl: String?
        get() = tmdbMetadata?.posterUrl.nonBlank() ?: coverUrl

    val displayBackdropUrl: String?
        get() = tmdbMetadata?.backdropUrl.nonBlank() ?: backdropUrl

    val displayPlot: String?
        get() = tmdbMetadata?.overview.nonBlank() ?: plot

    val displayGenre: String?
        get() = tmdbMetadata?.genres.nonBlank() ?: genre

    val displayReleaseDate: String?
        get() = tmdbMetadata?.firstAirDate.nonBlank() ?: releaseDate

    val displayRating: String?
        get() = tmdbMetadata?.voteAverage?.takeIf { it > 0.0 }?.formatRating() ?: rating

    val displayEpisodeRunTime: String?
        get() = tmdbMetadata?.episodeRunTimeMinutes?.takeIf { it > 0 }?.toString() ?: episodeRunTime

    val displayCast: String?
        get() = tmdbMetadata?.cast.nonBlank() ?: cast

    val displayCreator: String?
        get() = tmdbMetadata?.createdBy.nonBlank()

    val isTmdbEnriched: Boolean
        get() = tmdbMetadata != null

    val backgroundUrl: String?
        get() = displayBackdropUrl ?: displayCoverUrl

    val seasons: List<Int>
        get() = episodes.map { it.seasonNumber }.filter { it > 0 }.distinct().ifEmpty { listOf(1) }

    val visibleEpisodes: List<SeriesDetailEpisodeUi>
        get() = episodes.filter { it.seasonNumber == selectedSeason }.ifEmpty { episodes }

    val firstEpisode: SeriesDetailEpisodeUi?
        get() = visibleEpisodes.firstOrNull() ?: episodes.firstOrNull()

    val year: String?
        get() = displayReleaseDate?.take(4)?.takeIf { it.all(Char::isDigit) }
}

class SeriesDetailViewModel(
    private val seriesId: Int,
    private val xtreamRepository: XtreamRepository,
    private val userContentRepository: UserContentRepository,
    private val tmdbRepository: TmdbRepository,
) : ViewModel() {
    private val initialSeries = xtreamRepository.getCachedSeries(seriesId)
    private val initialCategory = initialSeries?.categoryId?.let { categoryId ->
        xtreamRepository.getCachedSeriesCategories().firstOrNull { it.id == categoryId }?.name
    } ?: "Series"

    private val _uiState = MutableStateFlow(
        SeriesDetailUiState(
            seriesId = seriesId,
            title = initialSeries?.title?.cleanSeriesTitle() ?: "Serie $seriesId",
            coverUrl = initialSeries?.coverUrl,
            categoryLabel = initialCategory,
            plot = initialSeries?.plot,
            genre = initialSeries?.genre,
            releaseDate = initialSeries?.releaseDate,
            rating = initialSeries?.rating,
            episodeRunTime = initialSeries?.episodeRunTime,
        ),
    )
    val uiState: StateFlow<SeriesDetailUiState> = _uiState.asStateFlow()
    private var lastTmdbRequestKey: String? = null

    init {
        observeFavorite()
        loadDetails()
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            userContentRepository.toggleFavorite(UserContentType.Series, seriesId)
        }
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, errorMessage = null) }
            runCatching {
                val details = xtreamRepository.getSeriesDetails(seriesId)
                val episodes = xtreamRepository.getSeriesEpisodes(seriesId)
                details to episodes
            }.onSuccess { (details, episodes) ->
                val current = _uiState.value
                val categoryLabel = details.categoryId?.let { categoryId ->
                    xtreamRepository.getCachedSeriesCategories().firstOrNull { it.id == categoryId }?.name
                } ?: initialCategory
                val episodeItems = episodes.map { it.toDetailEpisode() }
                _uiState.value = details.toUiState(
                    categoryLabel = categoryLabel,
                    episodes = episodeItems,
                    selectedSeason = episodeItems.firstOrNull()?.seasonNumber ?: 1,
                    isFavorite = current.isFavorite,
                ).copy(
                    tmdbMetadata = current.tmdbMetadata,
                    tmdbLoading = current.tmdbLoading,
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        errorMessage = error.message ?: "Details serie indisponibles.",
                    )
                }
            }
        }
    }

    fun selectSeason(season: Int) {
        _uiState.update { it.copy(selectedSeason = season) }
    }

    fun loadTmdbMetadata(language: String, includeAdult: Boolean) {
        val current = _uiState.value
        if (current.loading || current.title.isBlank()) return
        val requestKey = listOf(seriesId, current.title, current.releaseDate.orEmpty(), language, includeAdult).joinToString("|")
        if (requestKey == lastTmdbRequestKey) return
        lastTmdbRequestKey = requestKey
        viewModelScope.launch {
            _uiState.update { it.copy(tmdbLoading = true) }
            runCatching {
                tmdbRepository.enrichSeries(
                    contentId = seriesId,
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
            userContentRepository.observeFavoriteIds(UserContentType.Series).collect { ids ->
                _uiState.update { it.copy(isFavorite = seriesId in ids) }
            }
        }
    }
}

@Composable
fun SeriesDetailRoute(
    seriesId: Int,
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
    onWatchEpisode: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = PlayerSettings(),
    )
    val viewModel: SeriesDetailViewModel = viewModel(
        key = "series-detail-$seriesId",
        factory = viewModelFactory {
            SeriesDetailViewModel(
                seriesId = seriesId,
                xtreamRepository = container.xtreamRepository,
                userContentRepository = container.userContentRepository,
                tmdbRepository = container.tmdbRepository,
            )
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val behaviorScope = rememberCoroutineScope()
    LaunchedEffect(state.seriesId, state.categoryLabel) {
        container.behaviorReporter.report(
            "CONTENT_OPENED",
            state.toBehaviorContent("DETAIL"),
        )
    }
    LaunchedEffect(
        state.loading,
        state.seriesId,
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
    SeriesDetailScreen(
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
        onSeason = viewModel::selectSeason,
        onFavorite = {
            container.behaviorReporter.reportAsync(
                behaviorScope,
                if (state.isFavorite) "FAVORITE_REMOVED" else "FAVORITE_ADDED",
                state.toBehaviorContent("DETAIL"),
            )
            viewModel.toggleFavorite()
        },
        onWatchEpisode = { episodeId ->
            onWatchEpisode(episodeId)
        },
        modifier = modifier,
    )
}

@Composable
private fun SeriesDetailScreen(
    state: SeriesDetailUiState,
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
    onSeason: (Int) -> Unit,
    onFavorite: () -> Unit,
    onWatchEpisode: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstEpisodeFocusRequester = remember { FocusRequester() }
    val playFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    LaunchedEffect(state.seriesId) {
        listState.scrollToItem(0)
        delay(120)
        runCatching { playFocusRequester.requestFocus() }
    }

    DetailBackground(imageUrl = state.backgroundUrl, modifier = modifier) {
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
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DetailDimens.ScreenPadding)
                .padding(top = 82.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    SeriesHeroInfo(
                        state = state,
                        onWatchEpisode = { state.firstEpisode?.episodeId?.let(onWatchEpisode) },
                        onRetry = onRetry,
                        onFavorite = onFavorite,
                        playFocusRequester = playFocusRequester,
                        modifier = Modifier.width(720.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    SeriesCoverFrame(
                        imageUrl = state.displayCoverUrl,
                        title = state.displayTitle,
                        modifier = Modifier
                            .width(330.dp)
                            .aspectRatio(0.68f),
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    SeriesSeasonPanel(
                        state = state,
                        onSeason = onSeason,
                        modifier = Modifier.width(520.dp),
                    )
                    SeriesEpisodeList(
                        episodes = state.visibleEpisodes,
                        loading = state.loading,
                        errorMessage = state.errorMessage,
                        firstEpisodeFocusRequester = firstEpisodeFocusRequester,
                        onRetry = onRetry,
                        onEpisode = { episode -> onWatchEpisode(episode.episodeId) },
                        modifier = Modifier
                            .weight(1f)
                            .height(300.dp),
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
                DetailPeopleSection(title = "Creators", people = state.tmdbMetadata?.creators.orEmpty())
            }
            item {
                DetailUserRatingSection(
                    contentKey = "series:${state.seriesId}",
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
private fun SeriesHeroInfo(
    state: SeriesDetailUiState,
    onWatchEpisode: () -> Unit,
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOfNotNull(state.year, state.displayGenre, state.displayEpisodeRunTime?.let { "$it min" }).take(3).forEach { meta ->
                Text(
                    text = meta,
                    color = SmartVisionColors.TextSecondary,
                    style = DetailBodyStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("|", color = SmartVisionColors.TextSecondary, style = DetailBodyStyle)
            }
            state.displayRating?.let { DetailBadge(text = "$it/10") }
            DetailBadge(text = "${state.episodes.size} episodes")
            if (state.isTmdbEnriched) {
                DetailBadge(text = "TMDB", color = Color(0xFF18253A))
            }
            state.tmdbMetadata?.certification.nonBlank()?.let { DetailBadge(text = it, color = Color(0xFF18253A)) }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = state.displayPlot ?: "Serie disponible depuis le catalogue Xtream.",
            color = SmartVisionColors.TextSecondary,
            style = DetailBodyStyle,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(650.dp),
        )
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailActionButton(
                text = "Reprendre",
                icon = Icons.Default.PlayArrow,
                onClick = onWatchEpisode,
                primary = true,
                focusRequester = playFocusRequester,
                bringIntoViewOnFocus = false,
                modifier = Modifier
                    .width(162.dp)
                    .height(DetailDimens.ActionHeight),
            )
            DetailActionButton(
                text = if (state.isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                icon = Icons.Default.FavoriteBorder,
                onClick = onFavorite,
                modifier = Modifier
                    .width(190.dp)
                    .height(DetailDimens.ActionHeight),
            )
            if (state.errorMessage != null) {
                DetailActionButton(
                    text = "Reessayer",
                    icon = Icons.Default.Sync,
                    onClick = onRetry,
                    modifier = Modifier
                        .width(132.dp)
                        .height(DetailDimens.ActionHeight),
                )
            }
        }
    }
}

@Composable
private fun SeriesSeasonPanel(
    state: SeriesDetailUiState,
    onSeason: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.seasons.take(5).forEach { season ->
                SeasonTab(
                    text = "Saison $season",
                    selected = season == state.selectedSeason,
                    onClick = { onSeason(season) },
                    modifier = Modifier
                        .width(78.dp)
                        .height(30.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SeriesCoverFrame(
                imageUrl = state.displayCoverUrl,
                title = state.displayTitle,
                modifier = Modifier
                    .width(238.dp)
                    .aspectRatio(1.75f),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Saison ${state.selectedSeason}",
                    color = SmartVisionColors.TextPrimary,
                    style = DetailTitleStyle,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${state.visibleEpisodes.size} episodes",
                    color = SmartVisionColors.TextSecondary,
                    style = DetailMetaStyle,
                    maxLines = 1,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.displayPlot ?: state.categoryLabel,
                    color = SmartVisionColors.TextSecondary,
                    style = DetailMetaStyle,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    DetailBadge(text = state.categoryLabel)
                    state.displayCreator?.let { DetailBadge(text = it, color = Color(0xFF18253A)) }
                }
            }
        }
    }
}


@Composable
private fun SeriesEpisodeList(
    episodes: List<SeriesDetailEpisodeUi>,
    loading: Boolean,
    errorMessage: String?,
    firstEpisodeFocusRequester: FocusRequester,
    onRetry: () -> Unit,
    onEpisode: (SeriesDetailEpisodeUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Episodes",
            color = SmartVisionColors.TextPrimary,
            style = DetailTitleStyle,
            maxLines = 1,
        )
        Spacer(Modifier.height(7.dp))
        when {
            loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SmartVisionColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
            }

            errorMessage != null && episodes.isEmpty() -> DetailActionButton(
                text = "Reessayer",
                icon = Icons.Default.Sync,
                onClick = onRetry,
                modifier = Modifier
                    .width(136.dp)
                    .height(DetailDimens.ActionHeight),
            )

            episodes.isEmpty() -> Text(
                text = "Aucun episode",
                color = SmartVisionColors.TextSecondary,
                style = DetailBodyStyle,
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(episodes, key = { it.episodeId }) { episode ->
                    DetailEpisodeRow(
                        episode = episode,
                        focusRequester = if (episode.episodeId == episodes.first().episodeId) {
                            firstEpisodeFocusRequester
                        } else {
                            null
                        },
                        onClick = { onEpisode(episode) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(6.dp)
    val background by animateColorAsState(
        targetValue = if (selected || focusState.isFocused) SmartVisionColors.Primary else Color(0xA0101A2B),
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "seasonTabBackground",
    )
    val border by animateColorAsState(
        targetValue = if (focusState.isFocused) SmartVisionColors.FocusWhite else Color.Transparent,
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "seasonTabBorder",
    )

    Box(
        modifier = modifier
            .detailBringIntoViewOnFocus()
            .tvFocusTarget(
                state = focusState,
                pressed = pressed,
                focusedScale = 1.05f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = 6.dp,
            )
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .clip(shape)
            .background(background)
            .border(BorderStroke(if (focusState.isFocused) 2.dp else 1.dp, border), shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = DetailMetaStyle,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun DetailEpisodeRow(
    episode: SeriesDetailEpisodeUi,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val active = focusState.isFocused
    val shape = RoundedCornerShape(6.dp)

    Row(
        modifier = modifier
            .detailBringIntoViewOnFocus()
            .fillMaxWidth()
            .height(34.dp)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.025f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = 6.dp,
            )
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .clip(shape)
            .background(if (active) SmartVisionColors.PrimaryDark.copy(alpha = 0.86f) else Color(0x90101A2B))
            .border(
                BorderStroke(if (active) 2.dp else 1.dp, if (active) SmartVisionColors.FocusWhite else Color.White.copy(alpha = 0.07f)),
                shape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (active) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(
                text = episode.episodeNumber.toString(),
                color = SmartVisionColors.TextSecondary,
                style = DetailMetaStyle,
                modifier = Modifier.width(18.dp),
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = episode.title,
            color = Color.White,
            style = DetailMetaStyle,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = episode.duration.orEmpty(),
            color = SmartVisionColors.TextSecondary,
            style = DetailMetaStyle,
            maxLines = 1,
        )
    }
}

@Composable
private fun SeriesCoverFrame(
    imageUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.46f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = SmartVisionColors.TextSecondary,
                modifier = Modifier.size(54.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.74f)))),
        )
        Text(
            text = title,
            color = Color.White,
            style = DetailBodyStyle,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        )
    }
}

private fun XtreamSeriesDetails.toUiState(
    categoryLabel: String,
    episodes: List<SeriesDetailEpisodeUi>,
    selectedSeason: Int,
    isFavorite: Boolean,
): SeriesDetailUiState =
    SeriesDetailUiState(
        seriesId = seriesId,
        title = title.cleanSeriesTitle(),
        coverUrl = coverUrl,
        backdropUrl = backdropUrl,
        categoryLabel = categoryLabel,
        plot = plot,
        genre = genre,
        releaseDate = releaseDate,
        rating = rating,
        episodeRunTime = episodeRunTime,
        cast = cast,
        episodes = episodes,
        selectedSeason = selectedSeason,
        isFavorite = isFavorite,
        loading = false,
    )

private fun XtreamSeriesEpisode.toDetailEpisode(): SeriesDetailEpisodeUi =
    SeriesDetailEpisodeUi(
        episodeId = episodeId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        title = title.cleanSeriesTitle(),
        duration = duration,
        plot = plot,
    )

private fun SeriesDetailUiState.toBehaviorContent(sourceScreen: String, episodeId: Int? = null): BehaviorContent =
    BehaviorContent(
        contentType = if (episodeId == null) "SERIES" else "EPISODE",
        contentId = (episodeId ?: seriesId).toString(),
        title = displayTitle,
        categoryLabel = categoryLabel,
        durationSeconds = displayEpisodeRunTime?.toLongOrNull()?.times(60),
        engagementScore = if (loading) 25 else 45,
        sourceScreen = sourceScreen,
        tags = listOfNotNull(displayGenre, year, "episodes_${episodes.size}"),
        context = mapOf(
            "series_id" to seriesId.toString(),
            "season" to selectedSeason.toString(),
            "rating" to (displayRating ?: "-"),
        ),
    )

private fun String?.nonBlank(): String? = this?.trim()?.takeIf { it.isNotBlank() }

private fun Double.formatRating(): String = String.format(Locale.US, "%.1f", this)

private fun String.cleanSeriesTitle(): String =
    replace(Regex("\\s+"), " ")
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" 4K", "", ignoreCase = true)
        .trim()
        .ifBlank { "Serie" }
