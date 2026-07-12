@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.smartvision.svplayer.ui.player

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.anomaly.AnomalyReporter
import com.smartvision.svplayer.data.behavior.BehaviorContent
import com.smartvision.svplayer.data.behavior.BehaviorReporter
import com.smartvision.svplayer.data.models.XtreamSeriesEpisode
import com.smartvision.svplayer.data.monetization.IdleVastAdLoader
import com.smartvision.svplayer.data.monetization.IdleVastCreative
import com.smartvision.svplayer.data.monetization.MonetizationManager
import com.smartvision.svplayer.data.monetization.PlayerAdPlan
import com.smartvision.svplayer.data.monetization.PlayerContentType
import com.smartvision.svplayer.data.monetization.smartVisionMediaSourceFactory
import com.smartvision.svplayer.data.playlist.EpgProgram
import com.smartvision.svplayer.data.playlist.EpgRepository
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.UserContentType
import com.smartvision.svplayer.data.repository.XtreamRepository
import com.smartvision.svplayer.data.xtream.XtreamConnectionManager
import com.smartvision.svplayer.domain.access.PremiumFeatureGateResult
import com.smartvision.svplayer.domain.model.LiveChannel
import com.smartvision.svplayer.domain.model.Movie
import com.smartvision.svplayer.domain.model.Episode
import com.smartvision.svplayer.domain.model.TvSeries
import com.smartvision.svplayer.domain.model.PlaybackKind
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.domain.usecase.BuildPlaybackRequestUseCase
import com.smartvision.svplayer.media.MediaCenterFileType
import com.smartvision.svplayer.media.MediaCenterPlayback
import com.smartvision.svplayer.media.MediaCenterSource
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

enum class FullScreenContentKind {
    Live,
    Movie,
    Episode,
    LocalMedia,
}

private const val MinimumLiveHistoryMs = 5_000L
private const val PlayerReleaseDelayMs = 80L

internal val LiveAspectModes = listOf(
    LiveAspectMode("Auto", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    LiveAspectMode("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    LiveAspectMode("16:9", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH),
    LiveAspectMode("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    LiveAspectMode("Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
)

data class FullScreenPlayback(
    val streamId: Int,
    val contentType: String,
    val title: String,
    val subtitle: String,
    val url: String,
    val fallbackUrl: String,
    val badge: String,
    val status: String,
    val infoPills: List<String>,
    val imageUrl: String? = null,
    val categoryId: String? = null,
    val overlayRightText: String = "",
    val parentContentId: Int? = null,
    val previousItem: AdjacentPlayback? = null,
    val nextItem: AdjacentPlayback? = null,
    val nextEpisode: NextEpisodePlayback? = null,
    val seriesEpisodes: List<PlayerEpisodeItem> = emptyList(),
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val resumePositionMs: Long = 0L,
    val epgPrograms: List<FullScreenEpgProgram> = emptyList(),
)

data class FullScreenEpgProgram(
    val title: String,
    val description: String,
    val timeRange: String,
    val isCurrent: Boolean,
    val startMillis: Long? = null,
    val stopMillis: Long? = null,
)

data class AdjacentPlayback(
    val streamId: Int,
    val title: String,
    val label: String,
)

data class NextEpisodePlayback(
    val episodeId: Int,
    val title: String,
    val label: String,
    val thumbnailUrl: String? = null,
)

data class PlayerEpisodeItem(
    val episodeId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val label: String,
    val title: String,
    val duration: String?,
    val description: String?,
    val thumbnailUrl: String?,
)

private enum class PlayerOverlayMenu {
    None,
    Subtitles,
    Epg,
    Settings,
}

internal data class LiveAspectMode(
    val label: String,
    val resizeMode: Int,
)

private data class SubtitleTrackOption(
    val id: String,
    val label: String,
    val group: Tracks.Group,
    val trackIndex: Int,
)

class FullScreenPlayerViewModel(
    private val contentId: Int,
    private val kind: FullScreenContentKind,
    private val xtreamRepository: XtreamRepository,
    private val epgRepository: EpgRepository,
    private val catalogRepository: CatalogRepository,
    private val userContentRepository: UserContentRepository,
    private val buildPlaybackRequest: BuildPlaybackRequestUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(resolvePlayback(contentId, kind, xtreamRepository, epgRepository))
    val uiState: StateFlow<FullScreenPlayback> = _uiState
    private val saveProgressMutex = Mutex()

    init {
        viewModelScope.launch {
            val stored = userContentRepository.getProgress(kind.storageType(), contentId)
            if (kind != FullScreenContentKind.Live && stored != null && stored.positionMs > 0L) {
                _uiState.value = _uiState.value.copy(resumePositionMs = stored.positionMs)
            }
            val localPlayback = buildPlaybackRequest(kind.playbackKind(), contentId.toString())
            if (localPlayback != null) {
                _uiState.value = _uiState.value.copy(
                    title = localPlayback.title.cleanTitle(),
                    subtitle = localPlayback.subtitle,
                    url = localPlayback.url,
                    resumePositionMs = if (kind == FullScreenContentKind.Live) {
                        _uiState.value.resumePositionMs
                    } else {
                        localPlayback.resumePositionMs
                    },
                )
            }
            if (kind == FullScreenContentKind.Live) {
                refreshLivePlaybackFromLocalCatalog()
            } else if (kind == FullScreenContentKind.Movie || kind == FullScreenContentKind.Episode) {
                refreshVodPlaybackFromLocalCatalog()
            }
        }
    }

    suspend fun saveProgress(positionMs: Long, durationMs: Long): Boolean {
        if (kind == FullScreenContentKind.Live && positionMs < MinimumLiveHistoryMs) {
            return false
        }
        if (kind != FullScreenContentKind.Live && (positionMs <= 1_000L || durationMs <= 0L)) {
            return false
        }
        saveProgressMutex.withLock {
            userContentRepository.savePlaybackProgress(
                contentType = kind.storageType(),
                contentId = contentId,
                positionMs = positionMs,
                durationMs = durationMs,
                title = _uiState.value.title,
                subtitle = _uiState.value.subtitle,
                imageUrl = _uiState.value.imageUrl,
                parentContentId = _uiState.value.parentContentId,
            )
        }
        return true
    }

    private fun resolvePlayback(
        contentId: Int,
        kind: FullScreenContentKind,
        xtreamRepository: XtreamRepository,
        epgRepository: EpgRepository,
    ): FullScreenPlayback =
        when (kind) {
            FullScreenContentKind.Live -> resolveLive(contentId, xtreamRepository, epgRepository)
            FullScreenContentKind.Movie -> resolveMovie(contentId, xtreamRepository)
            FullScreenContentKind.Episode -> resolveEpisode(contentId, xtreamRepository)
            FullScreenContentKind.LocalMedia -> error("Local media playback uses LocalMediaPlayerRoute.")
        }

    private fun resolveLive(
        streamId: Int,
        xtreamRepository: XtreamRepository,
        epgRepository: EpgRepository,
    ): FullScreenPlayback =
        xtreamRepository.getCachedLiveStream(streamId)?.let { stream ->
            val categoryName = xtreamRepository.getCachedCategories()
                .firstOrNull { it.id == stream.categoryId }
                ?.name
                ?: "Live TV"
            val epgPrograms = epgRepository.loadPrograms(stream.epgChannelId, stream.name).toFullScreenPrograms()
            val previous = xtreamRepository.getCachedPreviousLiveStream(streamId)
            val next = xtreamRepository.getCachedNextLiveStream(streamId)
            FullScreenPlayback(
                streamId = stream.streamId,
                contentType = UserContentType.Live,
                title = stream.name.cleanTitle(),
                subtitle = categoryName,
                url = xtreamRepository.buildLiveStreamUrl(stream),
                fallbackUrl = xtreamRepository.buildLiveStreamFallbackUrl(stream.streamId),
                badge = "LIVE",
                status = epgPrograms.firstOrNull { it.isCurrent }?.title ?: epgPrograms.firstOrNull()?.title ?: "Direct",
                infoPills = listOf("16+", "HD", "5.1"),
                imageUrl = stream.streamIcon,
                categoryId = stream.categoryId,
                overlayRightText = stream.number.takeIf { it > 0 }?.toLiveChannelNumber(stream.streamId).orEmpty(),
                previousItem = previous?.let {
                    AdjacentPlayback(
                        streamId = it.streamId,
                        title = it.name.cleanTitle(),
                        label = it.number.toLiveChannelNumber(it.streamId),
                    )
                },
                nextItem = next?.let {
                    AdjacentPlayback(
                        streamId = it.streamId,
                        title = it.name.cleanTitle(),
                        label = it.number.toLiveChannelNumber(it.streamId),
                    )
                },
                epgPrograms = epgPrograms,
            )
        } ?: FullScreenPlayback(
            streamId = streamId,
            contentType = UserContentType.Live,
            title = "Chaine $streamId",
            subtitle = "Live TV",
            url = xtreamRepository.buildLiveStreamUrl(streamId),
            fallbackUrl = xtreamRepository.buildLiveStreamFallbackUrl(streamId),
            badge = "LIVE",
            status = "Direct",
            infoPills = listOf("16+", "HD", "5.1"),
        )

    private suspend fun refreshLivePlaybackFromLocalCatalog() {
        val current = catalogRepository.getLiveChannelById(contentId) ?: return
        val previous = catalogRepository.getPreviousLiveChannel(contentId)
        val next = catalogRepository.getNextLiveChannel(contentId)
        val epgPrograms = epgRepository.loadPrograms(current.epgChannelId, current.name).toFullScreenPrograms()
        _uiState.value = current.toFullScreenPlayback(
            previous = previous,
            next = next,
            epgPrograms = epgPrograms,
            fallback = _uiState.value,
        )
    }

    private suspend fun refreshVodPlaybackFromLocalCatalog() {
        when (kind) {
            FullScreenContentKind.Movie -> {
                val movie = catalogRepository.getMovieById(contentId) ?: return
                val previous = catalogRepository.getPreviousMovie(contentId)
                val next = catalogRepository.getNextMovie(contentId)
                _uiState.value = movie.toFullScreenPlayback(previous, next, _uiState.value)
            }
            FullScreenContentKind.Episode -> {
                val episode = catalogRepository.getEpisodeById(contentId) ?: return
                val episodes = catalogRepository.getSeriesEpisodes(episode.seriesId)
                    .sortedWith(compareBy<Episode> { it.seasonNumber }.thenBy { it.episodeNumber })
                val currentIndex = episodes.indexOfFirst { it.episodeId == episode.episodeId }
                val previous = episodes.getOrNull(currentIndex - 1)
                val next = episodes.getOrNull(currentIndex + 1)
                val series = catalogRepository.getSeriesByIds(listOf(episode.seriesId)).firstOrNull()
                _uiState.value = episode.toFullScreenPlayback(series, episodes, previous, next, _uiState.value)
            }
            else -> Unit
        }
    }

    private fun resolveMovie(
        movieId: Int,
        xtreamRepository: XtreamRepository,
    ): FullScreenPlayback =
        xtreamRepository.getCachedMovie(movieId)?.let { movie ->
            val categoryName = xtreamRepository.getCachedMovieCategories()
                .firstOrNull { it.id == movie.categoryId }
                ?.name
                ?: "Films"
            val previous = xtreamRepository.getCachedPreviousMovie(movieId)
            val next = xtreamRepository.getCachedNextMovie(movieId)
            FullScreenPlayback(
                streamId = movie.streamId,
                contentType = UserContentType.Movie,
                title = movie.title.cleanTitle(),
                subtitle = categoryName,
                url = xtreamRepository.buildMovieStreamUrl(movie),
                fallbackUrl = xtreamRepository.buildMovieStreamUrl(movie.streamId),
                badge = "VOD",
                status = "Film",
                infoPills = listOf("HD", movie.containerExtension.uppercase()).distinct(),
                imageUrl = movie.posterUrl,
                categoryId = movie.categoryId,
                overlayRightText = movie.added.extractReleaseYear() ?: "VOD",
                previousItem = previous?.let {
                    AdjacentPlayback(
                        streamId = it.streamId,
                        title = it.title.cleanTitle(),
                        label = "Film precedent",
                    )
                },
                nextItem = next?.let {
                    AdjacentPlayback(
                        streamId = it.streamId,
                        title = it.title.cleanTitle(),
                        label = "Film suivant",
                    )
                },
            )
        } ?: FullScreenPlayback(
            streamId = movieId,
            contentType = UserContentType.Movie,
            title = "Film $movieId",
            subtitle = "Films",
            url = xtreamRepository.buildMovieStreamUrl(movieId),
            fallbackUrl = xtreamRepository.buildMovieStreamUrl(movieId),
            badge = "VOD",
            status = "Film",
            infoPills = listOf("HD", "VOD"),
        )

    private fun resolveEpisode(
        episodeId: Int,
        xtreamRepository: XtreamRepository,
    ): FullScreenPlayback =
        xtreamRepository.getCachedEpisode(episodeId)?.let { episode ->
            val series = xtreamRepository.getCachedSeries(episode.seriesId)
            val seriesDetails = xtreamRepository.getCachedSeriesDetails(episode.seriesId)
            val seriesName = seriesDetails?.title?.cleanTitle()
                ?: series?.title?.cleanTitle()
                ?: "Serie ${episode.seriesId}"
            val seriesCover = seriesDetails?.coverUrl ?: series?.coverUrl
            val previousEpisode = xtreamRepository.getCachedPreviousEpisode(episodeId)
            val nextEpisode = xtreamRepository.getCachedNextEpisode(episodeId)
            val allEpisodes = xtreamRepository.getCachedSeriesEpisodes(episode.seriesId)
                .sortedWith(compareBy<XtreamSeriesEpisode> { it.seasonNumber }.thenBy { it.episodeNumber })
            FullScreenPlayback(
                streamId = episode.episodeId,
                contentType = UserContentType.Episode,
                title = seriesName,
                subtitle = "${episode.seasonEpisodeLabel()} - ${episode.title.cleanTitle()}",
                url = xtreamRepository.buildEpisodeStreamUrl(episode),
                fallbackUrl = xtreamRepository.buildEpisodeStreamUrl(episode.episodeId),
                badge = "SERIE",
                status = episode.duration ?: "Episode",
                infoPills = listOf("HD", episode.containerExtension.uppercase()).distinct(),
                imageUrl = seriesCover,
                categoryId = series?.categoryId,
                overlayRightText = episode.seasonEpisodeLabel(),
                parentContentId = episode.seriesId,
                seasonNumber = episode.seasonNumber,
                episodeNumber = episode.episodeNumber,
                previousItem = previousEpisode?.let {
                    AdjacentPlayback(
                        streamId = it.episodeId,
                        title = it.title.cleanTitle(),
                        label = it.seasonEpisodeLabel(),
                    )
                },
                nextItem = nextEpisode?.let {
                    AdjacentPlayback(
                        streamId = it.episodeId,
                        title = it.title.cleanTitle(),
                        label = it.seasonEpisodeLabel(),
                    )
                },
                nextEpisode = nextEpisode?.let {
                    NextEpisodePlayback(
                        episodeId = it.episodeId,
                        title = it.title.cleanTitle(),
                        label = it.seasonEpisodeLabel(),
                        thumbnailUrl = seriesCover,
                    )
                },
                seriesEpisodes = allEpisodes.map { item ->
                    PlayerEpisodeItem(
                        episodeId = item.episodeId,
                        seasonNumber = item.seasonNumber,
                        episodeNumber = item.episodeNumber,
                        label = item.seasonEpisodeLabel(),
                        title = item.title.cleanTitle(),
                        duration = item.duration,
                        description = item.plot?.cleanTitle(),
                        thumbnailUrl = seriesCover,
                    )
                },
            )
        } ?: FullScreenPlayback(
            streamId = episodeId,
            contentType = UserContentType.Episode,
            title = "Episode $episodeId",
            subtitle = "Series",
            url = xtreamRepository.buildEpisodeStreamUrl(episodeId),
            fallbackUrl = xtreamRepository.buildEpisodeStreamUrl(episodeId),
            badge = "SERIE",
            status = "Episode",
            infoPills = listOf("HD", "SERIE"),
        )
}

@Composable
fun FullScreenPlayerRoute(
    streamId: Int,
    kind: FullScreenContentKind = FullScreenContentKind.Live,
    onBack: () -> Unit,
    onBackWithCurrentContent: (Int) -> Unit = { onBack() },
    onPlayLive: (Int) -> Unit = {},
    onPlayMovie: (Int) -> Unit = {},
    onPlayEpisode: (Int) -> Unit = {},
    recorderAccess: PremiumFeatureGateResult? = null,
    strings: SmartVisionStrings? = null,
    onRecorderLocked: () -> Unit = {},
) {
    val container = LocalAppContainer.current
    val viewModel: FullScreenPlayerViewModel = viewModel(
        key = "fullscreen-${kind.name.lowercase()}-$streamId",
        factory = viewModelFactory {
            FullScreenPlayerViewModel(
                contentId = streamId,
                kind = kind,
                xtreamRepository = container.xtreamRepository,
                epgRepository = container.epgRepository,
                catalogRepository = container.catalogRepository,
                userContentRepository = container.userContentRepository,
                buildPlaybackRequest = container.buildPlaybackRequest,
            )
        },
    )
    val playback by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(playback.streamId, playback.contentType, playback.title) {
        container.anomalyReporter.setCurrentContext(
            "player kind=${kind.name} contentType=${playback.contentType} streamId=${playback.streamId} title=${playback.title}",
        )
    }
    DisposableEffect(Unit) {
        onDispose { container.anomalyReporter.setCurrentContext(null) }
    }
    FullScreenPlayerScreen(
        playback = playback,
        contentKind = kind,
        monetizationManager = container.monetizationManager,
        idleVastAdLoader = container.idleVastAdLoader,
        anomalyReporter = container.anomalyReporter,
        xtreamConnectionManager = container.xtreamConnectionManager,
        behaviorReporter = container.behaviorReporter,
        adRequestTimeoutSeconds = container.adConfigProvider.current().requestTimeoutSeconds,
        onBack = onBack,
        onBackWithCurrentContent = onBackWithCurrentContent,
        onPlayLive = onPlayLive,
        onPlayMovie = onPlayMovie,
        onPlayEpisode = onPlayEpisode,
        onProgressSnapshot = viewModel::saveProgress,
        recorderAccess = recorderAccess,
        strings = strings,
        onRecorderLocked = onRecorderLocked,
    )
}

@Composable
fun LocalMediaPlayerRoute(
    mediaFileId: Long,
    strings: SmartVisionStrings,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    var loading by remember(mediaFileId) { mutableStateOf(true) }
    var playback by remember(mediaFileId) { mutableStateOf<MediaCenterPlayback?>(null) }
    var error by remember(mediaFileId) { mutableStateOf<String?>(null) }

    LaunchedEffect(mediaFileId) {
        loading = true
        error = null
        playback = runCatching { container.mediaRepository.getPlayback(mediaFileId) }
            .onFailure { error = it.message ?: strings.mediaPlaybackUnavailable }
            .getOrNull()
        if (playback == null && error == null) {
            error = strings.mediaPlaybackUnavailable
        }
        loading = false
    }

    when {
        loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = SmartVisionColors.Primary, strokeWidth = 3.dp)
            }
        }
        playback == null -> {
            LocalMediaErrorScreen(
                message = error ?: strings.mediaPlaybackUnavailable,
                backLabel = strings.back,
                onBack = onBack,
            )
        }
        playback?.mediaType == MediaCenterFileType.Photo -> {
            LocalPhotoViewerScreen(
                playback = playback!!,
                strings = strings,
                onBack = onBack,
            )
        }
        playback?.mediaType == MediaCenterFileType.Video || playback?.mediaType == MediaCenterFileType.Audio -> {
            FullScreenPlayerScreen(
                playback = playback!!.toFullScreenPlayback(),
                contentKind = FullScreenContentKind.LocalMedia,
                monetizationManager = container.monetizationManager,
                idleVastAdLoader = container.idleVastAdLoader,
                anomalyReporter = container.anomalyReporter,
                xtreamConnectionManager = container.xtreamConnectionManager,
                behaviorReporter = container.behaviorReporter,
                adRequestTimeoutSeconds = container.adConfigProvider.current().requestTimeoutSeconds,
                onBack = onBack,
                onBackWithCurrentContent = { onBack() },
                strings = strings,
                onProgressSnapshot = { _, _ -> false },
            )
        }
        else -> {
            LocalMediaErrorScreen(
                message = strings.mediaPlaybackUnavailable,
                backLabel = strings.back,
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun FullScreenPlayerScreen(
    playback: FullScreenPlayback,
    contentKind: FullScreenContentKind,
    monetizationManager: MonetizationManager,
    idleVastAdLoader: IdleVastAdLoader,
    anomalyReporter: AnomalyReporter,
    xtreamConnectionManager: XtreamConnectionManager,
    behaviorReporter: BehaviorReporter,
    adRequestTimeoutSeconds: Long,
    onBack: () -> Unit,
    onBackWithCurrentContent: (Int) -> Unit,
    onPlayLive: (Int) -> Unit = {},
    onPlayMovie: (Int) -> Unit = {},
    onPlayEpisode: (Int) -> Unit = {},
    onProgressSnapshot: suspend (positionMs: Long, durationMs: Long) -> Boolean,
    recorderAccess: PremiumFeatureGateResult? = null,
    strings: SmartVisionStrings? = null,
    onRecorderLocked: () -> Unit = {},
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val coroutineScope = rememberCoroutineScope()
    val latestUrl by rememberUpdatedState(playback.url)
    val latestFallbackUrl by rememberUpdatedState(playback.fallbackUrl)
    val playFocusRequester = remember { FocusRequester() }
    val episodesButtonFocusRequester = remember { FocusRequester() }
    val brightnessButtonFocusRequester = remember { FocusRequester() }
    val progressFocusRequester = remember { FocusRequester() }
    val settingsButtonFocusRequester = remember { FocusRequester() }
    var overlayVisible by remember { mutableStateOf(true) }
    var overlayTick by remember { mutableIntStateOf(0) }
    var focusPlayWhenOverlayShows by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var buffering by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var fallbackTried by remember(playback.url) { mutableStateOf(false) }
    var stalledRefreshCount by remember(playback.url) { mutableIntStateOf(0) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedPositionMs by remember { mutableLongStateOf(0L) }
    var activeMenu by remember { mutableStateOf(PlayerOverlayMenu.None) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var liveAspectMode by remember { mutableStateOf(LiveAspectModes.first()) }
    var subtitleTracks by remember { mutableStateOf<List<SubtitleTrackOption>>(emptyList()) }
    var selectedSubtitleId by remember { mutableStateOf<String?>(null) }
    var episodesPanelVisible by remember(playback.streamId) { mutableStateOf(false) }
    var brightnessMode by remember(playback.streamId) { mutableStateOf(false) }
    var brightnessValue by remember(playback.streamId) { mutableStateOf(50f) }
    var vodFocusedControlIndex by remember(playback.streamId) { mutableIntStateOf(3) }
    var vodProgressFocused by remember(playback.streamId) { mutableStateOf(false) }
    var liveFocusedControlIndex by remember(playback.streamId) { mutableIntStateOf(3) }
    var liveProgressFocused by remember(playback.streamId) { mutableStateOf(false) }
    var liveSeekable by remember(playback.streamId) { mutableStateOf(false) }
    var liveWindowDurationMs by remember(playback.streamId) { mutableLongStateOf(0L) }
    var liveWindowIsDynamic by remember(playback.streamId) { mutableStateOf(false) }
    var liveWindowIsLive by remember(playback.streamId) { mutableStateOf(false) }
    var wallClockMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var adjacentNavigationPending by remember(playback.streamId) { mutableStateOf(false) }
    var nextEpisodeCountdown by remember { mutableStateOf<Int?>(null) }
    var nextEpisodeDismissed by remember(playback.streamId) { mutableStateOf(false) }
    var adGateActive by remember(playback.streamId) { mutableStateOf(false) }
    var adStarted by remember(playback.streamId) { mutableStateOf(false) }
    var activeAdRequestId by remember(playback.streamId) { mutableStateOf<String?>(null) }
    var activeAdCreative by remember(playback.streamId) { mutableStateOf<IdleVastCreative?>(null) }
    var firedAdTrackingEvents by remember(playback.streamId) { mutableStateOf(emptySet<String>()) }
    var adPositionMs by remember(playback.streamId) { mutableLongStateOf(0L) }
    var adDurationMs by remember(playback.streamId) { mutableLongStateOf(0L) }
    var exiting by remember(playback.streamId) { mutableStateOf(false) }
    var playbackCompletedReported by remember(playback.streamId) { mutableStateOf(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val exitStopDone = remember(playback.streamId) { AtomicBoolean(false) }
    val releaseScheduled = remember(playback.streamId) { AtomicBoolean(false) }
    val adSkipDelayMs = 20_000L
    val favoriteIds by remember(playback.contentType, container.userContentRepository) {
        container.userContentRepository.observeFavoriteIds(playback.contentType)
    }.collectAsStateWithLifecycle(emptySet())
    val isFavorite = playback.streamId in favoriteIds

    val playerView = remember {
        PlayerView(context).apply {
            useController = false
            resizeMode = liveAspectMode.resizeMode
            isFocusable = true
            isFocusableInTouchMode = true
        }
    }
    val mediaSourceFactory = remember(context) { smartVisionMediaSourceFactory(context) }
    val player = remember(mediaSourceFactory) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
            playWhenReady = true
        }
    }

    fun showOverlay(requestPlayFocus: Boolean = false) {
        if (adGateActive) return
        val wasVisible = overlayVisible
        overlayVisible = true
        overlayTick += 1
        if (!wasVisible || requestPlayFocus) {
            focusPlayWhenOverlayShows = requestPlayFocus ||
                playback.contentType == UserContentType.Live ||
                playback.contentType == UserContentType.Movie ||
                playback.contentType == UserContentType.Episode
            if (
                playback.contentType == UserContentType.Movie ||
                playback.contentType == UserContentType.Episode
            ) {
                vodFocusedControlIndex = 3
                vodProgressFocused = false
            } else if (playback.contentType == UserContentType.Live) {
                liveFocusedControlIndex = 3
                liveProgressFocused = false
            }
        }
    }

    fun prepareMedia(mediaItem: MediaItem, resumeContent: Boolean = true) {
        fallbackTried = false
        errorText = null
        buffering = true
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(mediaItem)
        player.prepare()
        if (resumeContent && playback.resumePositionMs > 0L) {
            player.seekTo(playback.resumePositionMs)
        }
        player.playWhenReady = true
        nextEpisodeCountdown = null
        nextEpisodeDismissed = false
        episodesPanelVisible = false
        brightnessMode = false
    }

    fun prepareContentWithoutAd() {
        prepareMedia(MediaItem.fromUri(playback.url))
    }

    fun refreshStalledPlayback() {
        if (adGateActive || exiting) return
        if (playback.contentType == UserContentType.LocalMedia) {
            buffering = false
            errorText = strings?.mediaPlaybackUnavailable ?: "Fichier local indisponible"
            showOverlay(requestPlayFocus = true)
            return
        }
        if (stalledRefreshCount >= 2) {
            buffering = false
            errorText = "Connexion Xtream indisponible"
            showOverlay(requestPlayFocus = true)
            xtreamConnectionManager.markPlaybackUnavailable(
                source = "player_buffering",
                contentType = playback.contentType,
                streamId = playback.streamId,
                detail = "Flux bloque en buffering apres ${stalledRefreshCount + 1} tentatives",
            )
            coroutineScope.launch {
                xtreamConnectionManager.verifyQuick("player_buffering")
            }
            return
        }
        stalledRefreshCount += 1
        val resumeAt = player.currentPosition.coerceAtLeast(0L)
        anomalyReporter.reportAsync(
            anomalyType = "PLAYER_BUFFERING_RETRY",
            message = "Relance automatique du flux bloque en buffering",
            context = "contentType=${playback.contentType} streamId=${playback.streamId} attempt=$stalledRefreshCount position=$resumeAt",
        )
        player.stop()
        player.prepare()
        if (resumeAt > 0L && player.duration.validDurationMs() > 0L) {
            player.seekTo(resumeAt)
        }
        player.playWhenReady = true
    }

    fun playAdjacent(item: AdjacentPlayback?) {
        val target = item ?: return
        if (adjacentNavigationPending) return
        adjacentNavigationPending = true
        when (playback.contentType) {
            UserContentType.Live -> onPlayLive(target.streamId)
            UserContentType.Movie -> onPlayMovie(target.streamId)
            UserContentType.Episode -> onPlayEpisode(target.streamId)
            else -> Unit
        }
    }

    fun playerExitContext(step: String, extra: String = ""): String =
        listOf(
            "step=$step",
            "elapsed=${SystemClock.elapsedRealtime()}",
            "contentType=${playback.contentType}",
            "streamId=${playback.streamId}",
            "categoryId=${playback.categoryId ?: "none"}",
            "position=$positionMs",
            "duration=$durationMs",
            "adGate=$adGateActive",
            extra,
        ).filter { it.isNotBlank() }.joinToString(" ")

    fun behaviorContent(
        source: String,
        position: Long = positionMs,
        duration: Long = durationMs,
    ): BehaviorContent {
        val durationSeconds = duration.validDurationMs().takeIf { it > 0L }?.div(1_000L)
        val positionSeconds = position.coerceAtLeast(0L).div(1_000L)
        val score = when {
            durationSeconds != null && durationSeconds > 0 -> ((positionSeconds.toDouble() / durationSeconds.toDouble()) * 100).toInt().coerceIn(0, 100)
            playback.contentType == UserContentType.Live && position >= MinimumLiveHistoryMs -> 55
            else -> 25
        }
        return BehaviorContent(
            contentType = playback.contentType.toBehaviorContentType(),
            contentId = playback.streamId.toString(),
            title = playback.title,
            categoryId = playback.categoryId,
            categoryLabel = playback.subtitle,
            durationSeconds = durationSeconds,
            positionSeconds = positionSeconds,
            engagementScore = score,
            sourceScreen = "PLAYER",
            tags = playback.infoPills + playback.badge + playback.status,
            context = mapOf(
                "source" to source,
                "kind" to contentKind.name,
                "badge" to playback.badge,
            ),
        )
    }

    fun reportPlayerExitStep(
        step: String,
        extra: String = "",
        anomalyType: String = "PLAYER_EXIT_STEP",
    ) {
        if (anomalyType in SuppressedPlayerAnomalyTypes) return
        anomalyReporter.setCurrentContext(playerExitContext(step, extra))
        anomalyReporter.reportAsync(
            anomalyType = anomalyType,
            message = "Player exit $step",
            context = playerExitContext(step, extra),
        )
    }

    fun releasePlayerBeforeNavigation(source: String) {
        if (!releaseScheduled.compareAndSet(false, true)) return
        val releaseContext = playerExitContext("exit_release", "source=$source")
        reportPlayerExitStep("before_exit_release", "source=$source")
        runCatching {
            playerView.player = null
            player.clearVideoSurface()
            player.release()
            anomalyReporter.setCurrentContext(releaseContext)
        }.onFailure { error ->
            anomalyReporter.reportAsync(
                anomalyType = "PLAYER_RELEASE_ERROR",
                message = error.message ?: "Erreur release player avant navigation",
                throwable = error,
                context = releaseContext,
            )
        }
    }

    fun exitPlayer(source: String) {
        if (exiting) return
        exiting = true
        val snapshotPosition = player.currentPosition.coerceAtLeast(0L)
        val snapshotDuration = player.duration.validDurationMs()
        reportPlayerExitStep(
            step = "begin",
            extra = "source=$source snapshotPosition=$snapshotPosition snapshotDuration=$snapshotDuration",
            anomalyType = "PLAYER_EXIT_BEGIN",
        )
        runCatching {
            if (exitStopDone.compareAndSet(false, true)) {
                reportPlayerExitStep("before_pause", "source=$source")
                player.pause()
                reportPlayerExitStep("after_pause", "source=$source")
                reportPlayerExitStep("before_stop", "source=$source")
                player.stop()
                reportPlayerExitStep("after_stop", "source=$source")
            }
        }.onFailure { error ->
            anomalyReporter.reportAsync(
                anomalyType = "PLAYER_EXIT_ERROR",
                message = error.message ?: "Erreur sortie player",
                throwable = error,
                context = "contentType=${playback.contentType} streamId=${playback.streamId} source=$source",
            )
        }
        coroutineScope.launch {
            behaviorReporter.reportAsync(
                coroutineScope,
                "PLAYBACK_PROGRESS",
                behaviorContent(source = source, position = snapshotPosition, duration = snapshotDuration),
            )
            if (!adGateActive) {
                runCatching {
                    if (playback.contentType == UserContentType.Live && snapshotPosition < MinimumLiveHistoryMs) {
                        reportPlayerExitStep(
                            step = "save_skipped_short_live",
                            extra = "source=$source snapshotPosition=$snapshotPosition snapshotDuration=$snapshotDuration",
                            anomalyType = "PLAYER_PROGRESS_SAVE_SKIPPED_SHORT_LIVE",
                        )
                    } else {
                        reportPlayerExitStep(
                            step = "before_save_progress",
                            extra = "source=$source snapshotPosition=$snapshotPosition snapshotDuration=$snapshotDuration",
                        )
                        val saved = onProgressSnapshot(snapshotPosition, snapshotDuration)
                        reportPlayerExitStep(
                            step = if (saved) "save_done" else "save_skipped_invalid",
                            extra = "source=$source snapshotPosition=$snapshotPosition snapshotDuration=$snapshotDuration saved=$saved",
                            anomalyType = if (saved) "PLAYER_PROGRESS_SAVE_DONE" else "PLAYER_PROGRESS_SAVE_SKIPPED_INVALID",
                        )
                    }
                }.onFailure { error ->
                    anomalyReporter.reportAsync(
                        anomalyType = "PLAYER_EXIT_ERROR",
                        message = error.message ?: "Erreur sauvegarde historique",
                        throwable = error,
                        context = "contentType=${playback.contentType} streamId=${playback.streamId} categoryId=${playback.categoryId ?: "none"} source=$source step=save_progress",
                    )
                    reportPlayerExitStep(
                        step = "save_failed",
                        extra = "source=$source snapshotPosition=$snapshotPosition snapshotDuration=$snapshotDuration error=${error.javaClass.simpleName}",
                        anomalyType = "PLAYER_PROGRESS_SAVE_FAILED",
                    )
                }
            }
            releasePlayerBeforeNavigation(source)
            reportPlayerExitStep("before_onBack", "source=$source")
            onBackWithCurrentContent(playback.streamId)
        }
    }

    fun handleBack(source: String) {
        when {
            adGateActive -> Unit
            episodesPanelVisible -> {
                episodesPanelVisible = false
                overlayVisible = true
                focusPlayWhenOverlayShows = false
                overlayTick += 1
                coroutineScope.launch {
                    delay(120)
                    runCatching { episodesButtonFocusRequester.requestFocus() }
                }
            }
            brightnessMode -> {
                brightnessMode = false
                showOverlay(requestPlayFocus = false)
                coroutineScope.launch {
                    delay(120)
                    runCatching { brightnessButtonFocusRequester.requestFocus() }
                }
            }
            nextEpisodeCountdown != null -> {
                nextEpisodeCountdown = null
                nextEpisodeDismissed = true
                showOverlay(requestPlayFocus = true)
            }
            activeMenu == PlayerOverlayMenu.Epg || activeMenu == PlayerOverlayMenu.Settings -> {
                activeMenu = PlayerOverlayMenu.None
                showOverlay(requestPlayFocus = false)
                coroutineScope.launch {
                    delay(120)
                    runCatching { settingsButtonFocusRequester.requestFocus() }
                }
            }
            else -> exitPlayer(source)
        }
    }

    fun hideVodOverlayToPlayer() {
        overlayVisible = false
        brightnessMode = false
        vodProgressFocused = false
        focusPlayWhenOverlayShows = false
        playerView.post { runCatching { playerView.requestFocus() } }
    }

    fun seekVodBy(deltaMs: Long) {
        if (!player.isCurrentMediaItemSeekable) return
        val upperBound = durationMs.takeIf { it > 0L } ?: player.duration.validDurationMs()
        val target = if (upperBound > 0L) {
            (player.currentPosition + deltaMs).coerceIn(0L, upperBound)
        } else {
            (player.currentPosition + deltaMs).coerceAtLeast(0L)
        }
        player.seekTo(target)
        positionMs = target
    }

    fun refreshLiveCapabilities() {
        if (playback.contentType != UserContentType.Live) return
        val timeline = player.currentTimeline
        val window = if (!timeline.isEmpty && player.currentMediaItemIndex in 0 until timeline.windowCount) {
            timeline.getWindow(player.currentMediaItemIndex, Timeline.Window())
        } else {
            null
        }
        liveWindowDurationMs = window?.durationMs?.takeIf { it > 0L } ?: player.duration.validDurationMs()
        liveWindowIsDynamic = window?.isDynamic == true
        liveWindowIsLive = window?.isLive == true
        val seekCommandAvailable = player.availableCommands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
        val usableWindow = liveWindowDurationMs > 0L && (liveWindowIsLive || liveWindowIsDynamic || window != null)
        liveSeekable = player.isCurrentMediaItemSeekable && seekCommandAvailable && usableWindow
        if (!liveSeekable) liveProgressFocused = false
    }

    fun seekLiveBy(deltaMs: Long) {
        if (!liveSeekable) return
        val upperBound = liveWindowDurationMs.takeIf { it > 0L }
            ?: player.duration.validDurationMs().takeIf { it > 0L }
            ?: return
        val target = (player.currentPosition + deltaMs).coerceIn(0L, upperBound)
        player.seekTo(target)
        positionMs = target
    }

    fun toggleLivePlayback() {
        if (player.isPlaying) {
            player.pause()
        } else {
            val upperBound = liveWindowDurationMs.takeIf { it > 0L } ?: player.duration.validDurationMs()
            if (upperBound > 0L && player.currentPosition !in 0L..upperBound) {
                player.seekToDefaultPosition()
            }
            player.play()
        }
    }

    fun hideLiveOverlayToPlayer() {
        overlayVisible = false
        brightnessMode = false
        liveProgressFocused = false
        focusPlayWhenOverlayShows = false
        playerView.post { runCatching { playerView.requestFocus() } }
    }

    fun handleLiveChannelKey(keyCode: Int, repeatCount: Int = 0): Boolean {
        if (playback.contentType != UserContentType.Live) return false
        if (adGateActive) return true
        if (activeMenu != PlayerOverlayMenu.None) return false
        if (!overlayVisible) {
            if (keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP || keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN) {
                playAdjacent(if (keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP) playback.previousItem else playback.nextItem)
                showOverlay()
                return true
            }
            return false
        }
        if (keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN) {
            hideLiveOverlayToPlayer()
            return true
        }
        if (brightnessMode) {
            return when (keyCode) {
                AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                    brightnessValue = (brightnessValue - 5f).coerceIn(0f, 100f)
                    showOverlay(requestPlayFocus = false)
                    true
                }
                AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                    brightnessValue = (brightnessValue + 5f).coerceIn(0f, 100f)
                    showOverlay(requestPlayFocus = false)
                    true
                }
                AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER -> {
                    brightnessMode = false
                    liveFocusedControlIndex = 0
                    showOverlay(requestPlayFocus = false)
                    true
                }
                else -> false
            }
        }
        if (liveProgressFocused) {
            return when (keyCode) {
                AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                    seekLiveBy(-10_000L)
                    showOverlay(requestPlayFocus = false)
                    true
                }
                AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                    seekLiveBy(10_000L)
                    showOverlay(requestPlayFocus = false)
                    true
                }
                else -> false
            }
        }
        fun moveLiveFocus(direction: Int) {
            val enabled = buildList {
                add(0)
                if (playback.previousItem != null) add(1)
                if (liveSeekable) add(2)
                add(3)
                if (liveSeekable) add(4)
                if (playback.nextItem != null) add(5)
                add(6)
            }
            val current = enabled.indexOf(liveFocusedControlIndex).takeIf { it >= 0 } ?: enabled.indexOf(3)
            liveFocusedControlIndex = enabled[(current + direction).coerceIn(0, enabled.lastIndex)]
        }
        return when (keyCode) {
            AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                moveLiveFocus(-1)
                showOverlay(requestPlayFocus = false)
                true
            }
            AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                moveLiveFocus(1)
                showOverlay(requestPlayFocus = false)
                true
            }
            AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                if (liveSeekable) {
                    liveProgressFocused = true
                    coroutineScope.launch {
                        delay(20)
                        runCatching { progressFocusRequester.requestFocus() }
                    }
                    showOverlay(requestPlayFocus = false)
                }
                true
            }
            AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER -> {
                if (repeatCount > 0) return true
                when (liveFocusedControlIndex) {
                    0 -> brightnessMode = true
                    1 -> playAdjacent(playback.previousItem)
                    2 -> seekLiveBy(-10_000L)
                    3 -> toggleLivePlayback()
                    4 -> seekLiveBy(10_000L)
                    5 -> playAdjacent(playback.nextItem)
                    6 -> exitPlayer("live_exit_fullscreen_key")
                }
                if (!adjacentNavigationPending) showOverlay(requestPlayFocus = false)
                true
            }
            else -> false
        }
    }

    fun handleVodOverlayKey(keyCode: Int, repeatCount: Int = 0): Boolean {
        val isVod = playback.contentType == UserContentType.Movie ||
            playback.contentType == UserContentType.Episode
        if (!isVod || adGateActive || episodesPanelVisible || activeMenu != PlayerOverlayMenu.None) return false
        if (!overlayVisible) {
            if (keyCode in overlayKeyCodes) {
                vodFocusedControlIndex = 3
                showOverlay(requestPlayFocus = true)
                return true
            }
            return false
        }
        if (keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN) {
            hideVodOverlayToPlayer()
            return true
        }
        if (brightnessMode) {
            return when (keyCode) {
                AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                    brightnessValue = (brightnessValue - 5f).coerceIn(0f, 100f)
                    showOverlay(requestPlayFocus = false)
                    true
                }
                AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                    brightnessValue = (brightnessValue + 5f).coerceIn(0f, 100f)
                    showOverlay(requestPlayFocus = false)
                    true
                }
                AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                AndroidKeyEvent.KEYCODE_ENTER,
                -> {
                    brightnessMode = false
                    vodFocusedControlIndex = 0
                    showOverlay(requestPlayFocus = false)
                    true
                }
                else -> false
            }
        }
        if (vodProgressFocused) {
            return when (keyCode) {
                AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                    seekVodBy(-10_000L)
                    showOverlay(requestPlayFocus = false)
                    true
                }
                AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                    seekVodBy(10_000L)
                    showOverlay(requestPlayFocus = false)
                    true
                }
                else -> false
            }
        }
        fun moveVodFocus(direction: Int) {
            val enabled = buildList {
                add(0)
                if (playback.previousItem != null) add(1)
                if (player.isCurrentMediaItemSeekable) add(2)
                add(3)
                if (player.isCurrentMediaItemSeekable) add(4)
                if (playback.nextItem != null) add(5)
                add(6)
            }
            val current = enabled.indexOf(vodFocusedControlIndex).takeIf { it >= 0 } ?: enabled.indexOf(3)
            vodFocusedControlIndex = enabled[(current + direction).coerceIn(0, enabled.lastIndex)]
        }
        return when (keyCode) {
            AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                moveVodFocus(-1)
                showOverlay(requestPlayFocus = false)
                true
            }
            AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                moveVodFocus(1)
                showOverlay(requestPlayFocus = false)
                true
            }
            AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                if (player.isCurrentMediaItemSeekable) {
                    vodProgressFocused = true
                    coroutineScope.launch {
                        delay(20)
                        runCatching { progressFocusRequester.requestFocus() }
                    }
                    showOverlay(requestPlayFocus = false)
                }
                true
            }
            AndroidKeyEvent.KEYCODE_DPAD_CENTER,
            AndroidKeyEvent.KEYCODE_ENTER,
            -> {
                if (repeatCount > 0) return true
                when (vodFocusedControlIndex) {
                    0 -> brightnessMode = true
                    1 -> playAdjacent(playback.previousItem)
                    2 -> seekVodBy(-10_000L)
                    3 -> if (player.isPlaying) player.pause() else player.play()
                    4 -> seekVodBy(10_000L)
                    5 -> playAdjacent(playback.nextItem)
                    6 -> exitPlayer("vod_exit_fullscreen_key")
                }
                if (!adjacentNavigationPending) showOverlay(requestPlayFocus = false)
                true
            }
            else -> false
        }
    }

    BackHandler {
        handleBack("back_handler")
    }

    LaunchedEffect(playback.url, playback.resumePositionMs, contentKind) {
        adGateActive = false
        adStarted = false
        activeAdRequestId = null
        activeAdCreative = null
        firedAdTrackingEvents = emptySet()
        adPositionMs = 0L
        adDurationMs = 0L
        if (contentKind == FullScreenContentKind.LocalMedia) {
            prepareContentWithoutAd()
            return@LaunchedEffect
        }
        var resolvedPlan: PlayerAdPlan? = null
        monetizationManager.maybeShowPlayerAdThenStartPlayback(
            contentType = contentKind.toPlayerContentType(),
            onContinue = { resolvedPlan = it },
        )
        when (val plan = resolvedPlan ?: PlayerAdPlan.StartDirectly("decision indisponible")) {
            is PlayerAdPlan.StartDirectly -> {
                prepareContentWithoutAd()
            }

            is PlayerAdPlan.ShowPreRoll -> {
                if (BuildConfig.DEBUG && BuildConfig.DEBUG_FORCE_AD_FAILURE) {
                    monetizationManager.onAdFailed(plan.requestId, "echec debug force")
                    prepareContentWithoutAd()
                    return@LaunchedEffect
                }
                adGateActive = true
                overlayVisible = false
                firedAdTrackingEvents = emptySet()
                val creative = idleVastAdLoader.load(plan.adTagUrl)
                val mediaUrl = creative?.mediaUrl
                if (mediaUrl.isNullOrBlank()) {
                    monetizationManager.onAdFailed(plan.requestId, "creative VAST indisponible")
                    adGateActive = false
                    activeAdRequestId = null
                    prepareContentWithoutAd()
                    return@LaunchedEffect
                }
                activeAdCreative = creative
                activeAdRequestId = plan.requestId
                monetizationManager.onAdLoaded(plan.requestId)
                prepareMedia(MediaItem.fromUri(mediaUrl), resumeContent = false)
            }
        }
    }

    fun finishAdAndStartContent(skipped: Boolean) {
        val requestId = activeAdRequestId ?: return
        val creative = activeAdCreative
        activeAdRequestId = null
        activeAdCreative = null
        adGateActive = false
        adStarted = false
        firedAdTrackingEvents = emptySet()
        adPositionMs = 0L
        adDurationMs = 0L
        coroutineScope.launch {
            idleVastAdLoader.ping(
                if (skipped) {
                    creative?.trackingUrls?.get("skip").orEmpty() +
                        creative?.trackingUrls?.get("close").orEmpty() +
                        creative?.trackingUrls?.get("closeLinear").orEmpty()
                } else {
                    creative?.trackingUrls?.get("complete").orEmpty()
                },
            )
            monetizationManager.onAdCompleted(requestId)
        }
        prepareContentWithoutAd()
    }

    LaunchedEffect(activeAdRequestId, adStarted) {
        val requestId = activeAdRequestId ?: return@LaunchedEffect
        if (adStarted) return@LaunchedEffect
        delay(adRequestTimeoutSeconds.coerceAtLeast(3) * 1_000L)
        if (activeAdRequestId == requestId && !adStarted) {
            monetizationManager.onAdFailed(requestId, "delai de chargement depasse")
            activeAdRequestId = null
            adGateActive = false
            activeAdCreative = null
            adPositionMs = 0L
            adDurationMs = 0L
            prepareContentWithoutAd()
        }
    }

    LaunchedEffect(activeAdRequestId, adStarted, activeAdCreative) {
        val creative = activeAdCreative ?: return@LaunchedEffect
        while (activeAdRequestId != null && adStarted && adGateActive) {
            val duration = player.duration.takeIf { it > 0L } ?: 0L
            adPositionMs = player.currentPosition.coerceAtLeast(0L)
            adDurationMs = duration
            if (duration > 0L) {
                val progress = player.currentPosition.toDouble() / duration.toDouble()
                val event = when {
                    progress >= 0.75 && "thirdQuartile" !in firedAdTrackingEvents -> "thirdQuartile"
                    progress >= 0.50 && "midpoint" !in firedAdTrackingEvents -> "midpoint"
                    progress >= 0.25 && "firstQuartile" !in firedAdTrackingEvents -> "firstQuartile"
                    else -> null
                }
                if (event != null) {
                    firedAdTrackingEvents = firedAdTrackingEvents + event
                    idleVastAdLoader.ping(creative.trackingUrls[event].orEmpty())
                }
            }
            delay(250)
        }
    }

    LaunchedEffect(overlayVisible, overlayTick) {
        if (overlayVisible && activeMenu == PlayerOverlayMenu.None && !brightnessMode && nextEpisodeCountdown == null) {
            if (focusPlayWhenOverlayShows) {
                delay(120)
                runCatching { playFocusRequester.requestFocus() }
                focusPlayWhenOverlayShows = false
            }
            delay(4_800)
            overlayVisible = false
            vodProgressFocused = false
            liveProgressFocused = false
        }
    }

    LaunchedEffect(playback.streamId, playback.contentType) {
        while (true) {
            wallClockMs = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    LaunchedEffect(nextEpisodeCountdown) {
        val countdown = nextEpisodeCountdown ?: return@LaunchedEffect
        if (countdown > 0) {
            delay(1_000)
            if (nextEpisodeCountdown == countdown) nextEpisodeCountdown = countdown - 1
        } else {
            playback.nextEpisode?.episodeId?.let(onPlayEpisode)
        }
    }

    LaunchedEffect(player, playback.url) {
        var tick = 0
        while (true) {
            if (!adGateActive && !exiting) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.validDurationMs()
                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
                val remainingMs = durationMs - positionMs
                if (
                    playback.contentType == UserContentType.Episode &&
                    playback.nextEpisode != null &&
                    durationMs > 0L &&
                    remainingMs in 1L..10_000L &&
                    !nextEpisodeDismissed &&
                    nextEpisodeCountdown == null
                ) {
                    nextEpisodeCountdown = ((remainingMs + 999L) / 1_000L).toInt().coerceIn(1, 10)
                    overlayVisible = true
                    activeMenu = PlayerOverlayMenu.None
                }
                if (tick % 10 == 0) {
                    onProgressSnapshot(positionMs, durationMs)
                }
            }
            tick += 1
            delay(500)
        }
    }

    LaunchedEffect(buffering, playback.url, adGateActive, exiting) {
        if (!buffering || adGateActive || exiting) return@LaunchedEffect
        val startPosition = player.currentPosition.coerceAtLeast(0L)
        delay(12_000L)
        val currentPosition = player.currentPosition.coerceAtLeast(0L)
        val stillStalled = buffering &&
            !adGateActive &&
            !exiting &&
            kotlin.math.abs(currentPosition - startPosition) < 1_500L
        if (stillStalled) {
            anomalyReporter.reportAsync(
                anomalyType = "PLAYER_BUFFERING_STALLED",
                message = "Flux bloque en buffering",
                context = "contentType=${playback.contentType} streamId=${playback.streamId} position=$currentPosition startPosition=$startPosition attempt=${stalledRefreshCount + 1}",
            )
            refreshStalledPlayback()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                isPlaying = isPlayingValue
                val requestId = activeAdRequestId
                val creative = activeAdCreative
                if (adGateActive && requestId != null && creative != null && isPlayingValue && !adStarted) {
                    adStarted = true
                    adPositionMs = player.currentPosition.coerceAtLeast(0L)
                    adDurationMs = player.duration.validDurationMs()
                    buffering = false
                    overlayVisible = false
                    coroutineScope.launch {
                        idleVastAdLoader.ping(creative.impressionUrls)
                        idleVastAdLoader.ping(creative.trackingUrls["start"].orEmpty())
                        monetizationManager.onAdStarted(requestId)
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_READY) {
                    errorText = null
                    stalledRefreshCount = 0
                    refreshLiveCapabilities()
                }
                if (playbackState == Player.STATE_ENDED && adGateActive) {
                    buffering = true
                    finishAdAndStartContent(skipped = false)
                    return
                }
                if (playbackState == Player.STATE_ENDED && !adGateActive && !playbackCompletedReported) {
                    playbackCompletedReported = true
                    behaviorReporter.reportAsync(coroutineScope, "PLAYBACK_COMPLETED", behaviorContent("ended", player.currentPosition, player.duration))
                }
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                refreshLiveCapabilities()
            }

            override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
                refreshLiveCapabilities()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                refreshLiveCapabilities()
            }

            override fun onTracksChanged(tracks: Tracks) {
                subtitleTracks = tracks.groups
                    .filter { it.type == C.TRACK_TYPE_TEXT }
                    .flatMapIndexed { groupIndex, group ->
                        (0 until group.length).mapNotNull { trackIndex ->
                            if (!group.isTrackSupported(trackIndex)) return@mapNotNull null
                            val format = group.getTrackFormat(trackIndex)
                            SubtitleTrackOption(
                                id = "$groupIndex:$trackIndex",
                                label = format.label
                                    ?: format.language?.uppercase()
                                    ?: "Sous-titres ${trackIndex + 1}",
                                group = group,
                                trackIndex = trackIndex,
                            )
                        }
                    }
                selectedSubtitleId = subtitleTracks.firstOrNull { option ->
                    option.group.isTrackSelected(option.trackIndex)
                }?.id
            }

            override fun onPlayerError(error: PlaybackException) {
                anomalyReporter.reportAsync(
                    anomalyType = "PLAYER_ERROR",
                    message = error.message ?: error.errorCodeName,
                    throwable = error,
                    context = "contentType=${playback.contentType} streamId=${playback.streamId} fallbackTried=$fallbackTried adGate=$adGateActive",
                )
                behaviorReporter.reportAsync(coroutineScope, "PLAYER_ERROR", behaviorContent("error"))
                val adRequestId = activeAdRequestId
                if (adRequestId != null && adGateActive) {
                    coroutineScope.launch {
                        monetizationManager.onAdFailed(adRequestId, error.message.orEmpty())
                    }
                    activeAdRequestId = null
                    adGateActive = false
                    activeAdCreative = null
                    firedAdTrackingEvents = emptySet()
                    adPositionMs = 0L
                    adDurationMs = 0L
                    prepareContentWithoutAd()
                    return
                }
                val fallback = latestFallbackUrl
                if (!fallbackTried && fallback.isNotBlank() && fallback != latestUrl) {
                    fallbackTried = true
                    errorText = null
                    buffering = true
                    player.stop()
                    player.clearMediaItems()
                    player.setMediaItem(MediaItem.fromUri(fallback))
                    player.prepare()
                    player.playWhenReady = true
                    return
                }

                buffering = false
                errorText = if (playback.contentType == UserContentType.LocalMedia) {
                    strings?.mediaPlaybackUnavailable ?: "Fichier local indisponible"
                } else {
                    "Flux indisponible"
                }
                showOverlay(requestPlayFocus = true)
            }
        }
        player.addListener(listener)
        onDispose {
            runCatching {
                player.removeListener(listener)
                playerView.player = null
                if (releaseScheduled.compareAndSet(false, true)) {
                    val releaseBeginContext = playerExitContext("release_begin")
                    val releaseDoneContext = playerExitContext("release_done")
                    anomalyReporter.setCurrentContext(releaseBeginContext)
                    anomalyReporter.reportAsync(
                        anomalyType = "PLAYER_RELEASE_BEGIN",
                        message = "Player release begin",
                        context = releaseBeginContext,
                    )
                    mainHandler.postDelayed(
                        {
                            runCatching {
                                player.clearVideoSurface()
                                player.release()
                                anomalyReporter.setCurrentContext(releaseDoneContext)
                            }.onFailure { error ->
                                anomalyReporter.reportAsync(
                                    anomalyType = "PLAYER_RELEASE_ERROR",
                                    message = error.message ?: "Erreur release player differe",
                                    throwable = error,
                                    context = releaseDoneContext,
                                )
                            }
                        },
                        PlayerReleaseDelayMs,
                    )
                }
            }.onFailure { error ->
                anomalyReporter.reportAsync(
                    anomalyType = "PLAYER_RELEASE_ERROR",
                    message = error.message ?: "Erreur release player",
                    throwable = error,
                    context = "contentType=${playback.contentType} streamId=${playback.streamId}",
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .onPreviewKeyEvent { event ->
                val keyCode = event.nativeKeyEvent.keyCode
                if (event.nativeKeyEvent.action == AndroidKeyEvent.ACTION_DOWN &&
                    handleLiveChannelKey(keyCode, event.nativeKeyEvent.repeatCount)
                ) {
                    return@onPreviewKeyEvent true
                }
                if (event.nativeKeyEvent.action == AndroidKeyEvent.ACTION_DOWN &&
                    handleVodOverlayKey(keyCode, event.nativeKeyEvent.repeatCount)
                ) {
                    return@onPreviewKeyEvent true
                }
                if (event.nativeKeyEvent.action == AndroidKeyEvent.ACTION_DOWN && keyCode in overlayKeyCodes) {
                    showOverlay()
                }
                false
            },
    ) {
        AndroidView(
            factory = {
                playerView.apply {
                    this.player = player
                    isFocusable = !adGateActive
                    isFocusableInTouchMode = !adGateActive
                    setOnKeyListener { _, keyCode, event ->
                        when {
                            keyCode == AndroidKeyEvent.KEYCODE_BACK &&
                                event.action == AndroidKeyEvent.ACTION_UP &&
                                adGateActive -> true

                            keyCode == AndroidKeyEvent.KEYCODE_BACK && event.action == AndroidKeyEvent.ACTION_UP -> {
                                handleBack("player_view_back")
                                true
                            }

                            event.action == AndroidKeyEvent.ACTION_DOWN && handleLiveChannelKey(keyCode, event.repeatCount) -> true

                            event.action == AndroidKeyEvent.ACTION_DOWN && handleVodOverlayKey(keyCode, event.repeatCount) -> true

                            event.action == AndroidKeyEvent.ACTION_DOWN &&
                                keyCode in overlayKeyCodes &&
                                !adGateActive -> {
                                showOverlay()
                                false
                            }

                            else -> false
                        }
                    }
                    if (!adGateActive) requestFocus()
                }
            },
            update = {
                it.player = player
                it.resizeMode = liveAspectMode.resizeMode
                it.isFocusable = !adGateActive
                it.isFocusableInTouchMode = !adGateActive
                if (adGateActive) it.clearFocus()
                it.setOnKeyListener { _, keyCode, event ->
                    when {
                        keyCode == AndroidKeyEvent.KEYCODE_BACK &&
                            event.action == AndroidKeyEvent.ACTION_UP &&
                            adGateActive -> true

                        keyCode == AndroidKeyEvent.KEYCODE_BACK && event.action == AndroidKeyEvent.ACTION_UP -> {
                            handleBack("player_view_back_update")
                            true
                        }

                        event.action == AndroidKeyEvent.ACTION_DOWN && handleLiveChannelKey(keyCode, event.repeatCount) -> {
                            true
                        }

                        event.action == AndroidKeyEvent.ACTION_DOWN && handleVodOverlayKey(keyCode, event.repeatCount) -> {
                            true
                        }

                        event.action == AndroidKeyEvent.ACTION_DOWN && keyCode in overlayKeyCodes -> {
                            showOverlay()
                            false
                        }

                        else -> false
                    }
                }
            },
            modifier = Modifier.matchParentSize(),
        )

        if (brightnessValue < 50f) {
            val dimAlpha = ((50f - brightnessValue) / 50f * 0.72f).coerceIn(0f, 0.72f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = dimAlpha)),
            )
        }

        if (buffering && !adGateActive) {
            CircularProgressIndicator(
                color = SmartVisionColors.Primary,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(46.dp),
            )
        }

        if (adGateActive) {
            FullScreenAdOverlay(
                positionMs = adPositionMs,
                durationMs = adDurationMs,
                skipDelayMs = adSkipDelayMs,
                onSkip = { finishAdAndStartContent(skipped = true) },
            )
        }

        if (overlayVisible && !adGateActive && !episodesPanelVisible) {
            if (playback.contentType == UserContentType.Live) {
                val liveDuration = liveWindowDurationMs.takeIf { it > 0L } ?: durationMs
                val liveAtEdge = !liveSeekable || (liveDuration - positionMs) <= 10_000L
                LiveFullscreenControlsOverlay(
                    playback = playback,
                    isPlaying = isPlaying,
                    errorText = errorText,
                    positionMs = positionMs,
                    durationMs = liveDuration,
                    wallClockMs = wallClockMs,
                    isSeekable = liveSeekable,
                    isAtLiveEdge = liveAtEdge,
                    focusedControlIndex = liveFocusedControlIndex,
                    progressFocused = liveProgressFocused,
                    onFocusedControlChange = {
                        liveProgressFocused = false
                        liveFocusedControlIndex = it
                    },
                    onProgressFocused = { liveProgressFocused = true },
                    playFocusRequester = playFocusRequester,
                    brightnessFocusRequester = brightnessButtonFocusRequester,
                    progressFocusRequester = progressFocusRequester,
                    brightnessMode = brightnessMode,
                    brightnessValue = brightnessValue,
                    epgUnavailableLabel = strings?.liveTvEpgUnavailable ?: "EPG unavailable",
                    nextLabel = strings?.playerLiveNextLabel ?: "Up next",
                    directLabel = strings?.liveTvDirect ?: "Live",
                    onOpenBrightness = {
                        brightnessMode = true
                        showOverlay(requestPlayFocus = false)
                    },
                    onChangeBrightness = { delta ->
                        brightnessValue = (brightnessValue + delta).coerceIn(0f, 100f)
                        showOverlay(requestPlayFocus = false)
                    },
                    onCloseBrightness = {
                        brightnessMode = false
                        showOverlay(requestPlayFocus = false)
                        coroutineScope.launch {
                            delay(120)
                            runCatching { brightnessButtonFocusRequester.requestFocus() }
                        }
                    },
                    onPreviousChannel = { playAdjacent(playback.previousItem) },
                    onSeekBack = {
                        seekLiveBy(-10_000L)
                        showOverlay(requestPlayFocus = false)
                    },
                    onPlayPause = {
                        toggleLivePlayback()
                        showOverlay(requestPlayFocus = false)
                    },
                    onSeekForward = {
                        seekLiveBy(10_000L)
                        showOverlay(requestPlayFocus = false)
                    },
                    onNextChannel = { playAdjacent(playback.nextItem) },
                    onExitFullscreen = { exitPlayer("live_exit_fullscreen_button") },
                )
            } else if (
                playback.contentType == UserContentType.Movie ||
                playback.contentType == UserContentType.Episode
            ) {
                val vodSubtitle = if (playback.contentType == UserContentType.Episode) {
                    val season = playback.seasonNumber
                    val episode = playback.episodeNumber
                    if (season != null && episode != null) {
                        "${strings?.playerSeasonLabel ?: "Season"} $season · " +
                            "${strings?.playerEpisodeLabel ?: "Episode"} $episode"
                    } else {
                        playback.overlayRightText.takeIf { it.isNotBlank() }
                    }
                } else {
                    null
                }
                VodFullscreenControlsOverlay(
                    title = playback.title,
                    subtitle = vodSubtitle,
                    isPlaying = isPlaying,
                    errorText = errorText,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    canSeek = player.isCurrentMediaItemSeekable,
                    hasPrevious = playback.previousItem != null,
                    hasNext = playback.nextItem != null,
                    focusedControlIndex = vodFocusedControlIndex,
                    progressFocused = vodProgressFocused,
                    onFocusedControlChange = {
                        vodProgressFocused = false
                        vodFocusedControlIndex = it
                    },
                    onProgressFocused = { vodProgressFocused = true },
                    onSeekBy = { deltaMs ->
                        seekVodBy(deltaMs)
                        showOverlay(requestPlayFocus = false)
                    },
                    playFocusRequester = playFocusRequester,
                    brightnessFocusRequester = brightnessButtonFocusRequester,
                    progressFocusRequester = progressFocusRequester,
                    brightnessMode = brightnessMode,
                    brightnessValue = brightnessValue,
                    onOpenBrightness = {
                        brightnessMode = true
                        showOverlay(requestPlayFocus = false)
                    },
                    onChangeBrightness = { delta ->
                        brightnessValue = (brightnessValue + delta).coerceIn(0f, 100f)
                        showOverlay(requestPlayFocus = false)
                    },
                    onCloseBrightness = {
                        brightnessMode = false
                        showOverlay(requestPlayFocus = false)
                        coroutineScope.launch {
                            delay(120)
                            runCatching { brightnessButtonFocusRequester.requestFocus() }
                        }
                    },
                    onPlayPrevious = { playAdjacent(playback.previousItem) },
                    onSeekBack = {
                        seekVodBy(-10_000L)
                        showOverlay()
                    },
                    onPlayPause = {
                        if (player.isPlaying) player.pause() else player.play()
                        showOverlay()
                    },
                    onSeekForward = {
                        seekVodBy(10_000L)
                        showOverlay()
                    },
                    onPlayNext = { playAdjacent(playback.nextItem) },
                    onExitFullscreen = { exitPlayer("vod_exit_fullscreen_button") },
                )
            } else {
                FullPlayerOverlay(
                    playback = playback,
                    isPlaying = isPlaying,
                    errorText = errorText,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    bufferedPositionMs = bufferedPositionMs,
                    isFavorite = isFavorite,
                    playFocusRequester = playFocusRequester,
                    episodesButtonFocusRequester = episodesButtonFocusRequester,
                    brightnessMode = brightnessMode,
                    brightnessValue = brightnessValue,
                    onPlayPause = {
                        if (player.isPlaying) {
                            player.pause()
                        } else {
                            player.play()
                        }
                        showOverlay()
                    },
                    onSeekBack = {
                        if (player.isCurrentMediaItemSeekable) {
                            player.seekTo((player.currentPosition - 15_000L).coerceAtLeast(0L))
                        }
                        showOverlay()
                    },
                    onSeekForward = {
                        if (player.isCurrentMediaItemSeekable) {
                            player.seekTo(player.currentPosition + 15_000L)
                        }
                        showOverlay()
                    },
                    onSeekBy = { deltaMs ->
                        if (player.isCurrentMediaItemSeekable) {
                            player.seekTo((player.currentPosition + deltaMs).coerceIn(0L, durationMs.coerceAtLeast(0L)))
                        }
                        showOverlay()
                    },
                    onPlayPrevious = {
                        playAdjacent(playback.previousItem)
                    },
                    onPlayNext = {
                        playAdjacent(playback.nextItem)
                    },
                    onOpenEpisodes = {
                        if (playback.contentType == UserContentType.Episode && playback.seriesEpisodes.isNotEmpty()) {
                            episodesPanelVisible = true
                            brightnessMode = false
                            overlayVisible = false
                        }
                    },
                    onToggleFavorite = {
                        coroutineScope.launch {
                            container.userContentRepository.toggleFavorite(playback.contentType, playback.streamId)
                        }
                        showOverlay(requestPlayFocus = false)
                    },
                    onOpenBrightness = {
                        brightnessMode = true
                        showOverlay(requestPlayFocus = false)
                    },
                    onChangeBrightness = { delta ->
                        brightnessValue = (brightnessValue + delta).coerceIn(0f, 100f)
                        showOverlay(requestPlayFocus = false)
                    },
                    onOpenSettings = {
                        activeMenu = PlayerOverlayMenu.Settings
                        showOverlay()
                    },
                    onCloseBrightness = {
                        brightnessMode = false
                        showOverlay(requestPlayFocus = false)
                    },
                    contextLabel = if (playback.contentType == UserContentType.Episode) {
                        strings?.playerSeriesContext ?: "Series | Episode"
                    } else {
                        strings?.playerMovieContext ?: "Movie"
                    },
                )
            }
        }

        if (episodesPanelVisible && playback.contentType == UserContentType.Episode) {
            EpisodesSidePanel(
                playback = playback,
                onEpisode = { episodeId -> onPlayEpisode(episodeId) },
                onClose = {
                    episodesPanelVisible = false
                    overlayVisible = true
                    focusPlayWhenOverlayShows = false
                    overlayTick += 1
                    coroutineScope.launch {
                        delay(120)
                        runCatching { episodesButtonFocusRequester.requestFocus() }
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }

        if (activeMenu == PlayerOverlayMenu.Subtitles) {
            PlayerOptionMenu(
                title = "Sous-titres",
                options = listOf("Desactives") + subtitleTracks.map { it.label },
                selectedIndex = selectedSubtitleId?.let { id -> subtitleTracks.indexOfFirst { it.id == id } + 1 } ?: 0,
                onSelect = { index ->
                    val builder = player.trackSelectionParameters.buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    if (index == 0) {
                        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        selectedSubtitleId = null
                    } else {
                        val option = subtitleTracks[index - 1]
                        builder
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .setOverrideForType(
                                TrackSelectionOverride(option.group.mediaTrackGroup, listOf(option.trackIndex)),
                            )
                        selectedSubtitleId = option.id
                    }
                    player.trackSelectionParameters = builder.build()
                    activeMenu = PlayerOverlayMenu.None
                    showOverlay()
                },
                onClose = { activeMenu = PlayerOverlayMenu.None },
            )
        }

        if (activeMenu == PlayerOverlayMenu.Epg && playback.contentType == UserContentType.Live) {
            LiveTvEpgSidePanel(
                programs = playback.epgPrograms,
                onClose = {
                    activeMenu = PlayerOverlayMenu.None
                    showOverlay(requestPlayFocus = false)
                    coroutineScope.launch {
                        delay(120)
                        runCatching { settingsButtonFocusRequester.requestFocus() }
                    }
                },
            )
        }

        if (activeMenu == PlayerOverlayMenu.Settings && playback.contentType == UserContentType.Live) {
            LiveTvSettingsPanel(
                selectedAspectMode = liveAspectMode,
                audioSummary = "Default",
                subtitleSummary = selectedSubtitleId
                    ?.let { id -> subtitleTracks.firstOrNull { it.id == id }?.label }
                    ?: "Off",
                onSelectAspectMode = { mode ->
                    liveAspectMode = mode
                    playerView.resizeMode = mode.resizeMode
                    showOverlay(requestPlayFocus = true)
                },
                onOpenSubtitles = {
                    activeMenu = PlayerOverlayMenu.Subtitles
                },
                onClose = {
                    activeMenu = PlayerOverlayMenu.None
                    showOverlay(requestPlayFocus = false)
                    coroutineScope.launch {
                        delay(120)
                        runCatching { settingsButtonFocusRequester.requestFocus() }
                    }
                },
            )
        } else if (activeMenu == PlayerOverlayMenu.Settings) {
            val speeds = listOf(0.75f, 1f, 1.25f, 1.5f)
            PlayerOptionMenu(
                title = "Vitesse de lecture",
                options = speeds.map { "${it}x" },
                selectedIndex = speeds.indexOf(playbackSpeed).coerceAtLeast(1),
                onSelect = { index ->
                    playbackSpeed = speeds[index]
                    player.setPlaybackSpeed(playbackSpeed)
                    activeMenu = PlayerOverlayMenu.None
                    showOverlay()
                },
                onClose = { activeMenu = PlayerOverlayMenu.None },
            )
        }

        nextEpisodeCountdown?.let { countdown ->
            playback.nextEpisode?.let { next ->
                NextEpisodeCard(
                    nextEpisode = next,
                    countdown = countdown,
                    onPlayNow = { onPlayEpisode(next.episodeId) },
                    onCancel = {
                        nextEpisodeCountdown = null
                        nextEpisodeDismissed = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 68.dp, bottom = 216.dp),
                )
            }
        }
    }
}

@Composable
private fun FullScreenAdOverlay(
    positionMs: Long,
    durationMs: Long,
    skipDelayMs: Long,
    onSkip: () -> Unit,
) {
    val skipFocusRequester = remember { FocusRequester() }
    val showSkipButton = durationMs >= skipDelayMs
    val skipEnabled = showSkipButton && positionMs >= skipDelayMs
    val remainingMs = if (durationMs > 0L) (durationMs - positionMs).coerceAtLeast(0L) else 0L
    val skipCountdownSeconds = ((skipDelayMs - positionMs).coerceAtLeast(0L) + 999L) / 1_000L
    val timerText = if (durationMs > 0L) remainingMs.formatPlaybackTime() else "--:--"
    val elapsedText = if (durationMs > 0L) positionMs.formatPlaybackTime() else "--:--"
    val totalText = if (durationMs > 0L) durationMs.formatPlaybackTime() else "--:--"
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    LaunchedEffect(skipEnabled) {
        if (skipEnabled) {
            delay(80)
            runCatching { skipFocusRequester.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.30f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.42f),
                    ),
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 26.dp, end = 32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xD6071123))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Publicite",
                color = SmartVisionColors.TextSecondary,
                style = PlayerMetaStyle,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = timerText,
                color = Color.White,
                style = PlayerTitleStyle,
                fontWeight = FontWeight.Black,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 42.dp, vertical = 30.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$elapsedText / $totalText",
                    color = Color.White,
                    style = PlayerMetaStyle,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Video publicitaire",
                    color = SmartVisionColors.TextSecondary,
                    style = PlayerMetaStyle,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.20f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(Color(0xFFFFD447)),
                )
            }
        }

        if (showSkipButton) {
            TvButton(
                text = if (skipEnabled) "Passer" else "Passer dans ${skipCountdownSeconds.coerceAtLeast(1L)}",
                onClick = onSkip,
                enabled = skipEnabled,
                leadingIcon = Icons.Default.SkipNext,
                focusRequester = skipFocusRequester,
                variant = if (skipEnabled) TvButtonVariant.Primary else TvButtonVariant.Secondary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 66.dp)
                    .height(44.dp)
                    .width(176.dp),
            )
        }
    }
}

@Composable
private fun LocalPhotoViewerScreen(
    playback: MediaCenterPlayback,
    strings: SmartVisionStrings,
    onBack: () -> Unit,
) {
    val backFocusRequester = remember { FocusRequester() }
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { backFocusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AsyncImage(
            model = playback.uri,
            contentDescription = playback.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 46.dp, vertical = 42.dp),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(92.dp)
                .background(Color.Black.copy(alpha = 0.66f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)))
                .padding(horizontal = 28.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playback.displayName.cleanTitle(),
                    color = Color.White,
                    style = PlayerTitleStyle.copy(fontSize = 24.sp, lineHeight = 29.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = playback.relativePath,
                    color = Color.White.copy(alpha = 0.64f),
                    style = PlayerMetaStyle.copy(fontSize = 13.sp, lineHeight = 17.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TvButton(
                text = strings.back,
                onClick = onBack,
                variant = TvButtonVariant.Secondary,
                focusRequester = backFocusRequester,
                modifier = Modifier
                    .width(132.dp)
                    .height(42.dp),
            )
        }
    }
}

@Composable
private fun LocalMediaErrorScreen(
    message: String,
    backLabel: String,
    onBack: () -> Unit,
) {
    val backFocusRequester = remember { FocusRequester() }
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { backFocusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xDD07101E))
                .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(14.dp))
                .padding(horizontal = 26.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message,
                color = Color.White,
                style = PlayerMetaStyle.copy(fontSize = 16.sp, lineHeight = 21.sp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            TvButton(
                text = backLabel,
                onClick = onBack,
                variant = TvButtonVariant.Secondary,
                focusRequester = backFocusRequester,
                modifier = Modifier
                    .width(132.dp)
                    .height(42.dp),
            )
        }
    }
}

private enum class RecordingDurationChoice {
    ProgramEnd,
    Minutes30,
    Minutes60,
    Minutes120,
}

private data class RecordingDialogSelection(
    val durationMs: Long,
    val programTitle: String?,
    val programStartMs: Long?,
    val programStopMs: Long?,
)

@Composable
private fun LiveRecordDialog(
    playback: FullScreenPlayback,
    strings: SmartVisionStrings,
    onDismiss: () -> Unit,
    onStart: (RecordingDialogSelection) -> Unit,
) {
    val firstFocusRequester = remember { FocusRequester() }
    val currentProgram = playback.epgPrograms.firstOrNull { it.isCurrent } ?: playback.epgPrograms.firstOrNull()
    val programEndAvailable = currentProgram?.stopMillis?.let { it > System.currentTimeMillis() } == true
    var selectedChoice by remember(playback.streamId, programEndAvailable) {
        mutableStateOf(if (programEndAvailable) RecordingDurationChoice.ProgramEnd else RecordingDurationChoice.Minutes60)
    }

    LaunchedEffect(Unit) {
        delay(120)
        runCatching { firstFocusRequester.requestFocus() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xF0061020))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(18.dp))
                .padding(24.dp),
        ) {
            Text(
                text = strings.recorderTitle,
                color = Color.White,
                style = PlayerTitleStyle.copy(fontSize = 28.sp, lineHeight = 34.sp),
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = strings.recorderSubtitle,
                color = Color.White.copy(alpha = 0.68f),
                style = PlayerMetaStyle.copy(fontSize = 14.sp, lineHeight = 19.sp),
            )
            Spacer(Modifier.height(16.dp))
            RecordInfoCard(
                label = strings.recorderCurrentProgram,
                value = currentProgram?.let {
                    listOf(it.timeRange, it.title)
                        .filter { value -> value.isNotBlank() }
                        .joinToString("  |  ")
                }
                    ?: playback.title,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = strings.recorderManualDuration,
                color = Color.White,
                style = PlayerMetaStyle.copy(fontSize = 15.sp, lineHeight = 19.sp),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (programEndAvailable) {
                    RecorderChoiceButton(
                        label = strings.recorderUntilProgramEnd,
                        selected = selectedChoice == RecordingDurationChoice.ProgramEnd,
                        focusRequester = firstFocusRequester,
                        onClick = { selectedChoice = RecordingDurationChoice.ProgramEnd },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecorderChoiceButton(
                        label = strings.recorderDuration30,
                        selected = selectedChoice == RecordingDurationChoice.Minutes30,
                        focusRequester = if (!programEndAvailable) firstFocusRequester else null,
                        onClick = { selectedChoice = RecordingDurationChoice.Minutes30 },
                        modifier = Modifier.weight(1f),
                    )
                    RecorderChoiceButton(
                        label = strings.recorderDuration60,
                        selected = selectedChoice == RecordingDurationChoice.Minutes60,
                        onClick = { selectedChoice = RecordingDurationChoice.Minutes60 },
                        modifier = Modifier.weight(1f),
                    )
                    RecorderChoiceButton(
                        label = strings.recorderDuration120,
                        selected = selectedChoice == RecordingDurationChoice.Minutes120,
                        onClick = { selectedChoice = RecordingDurationChoice.Minutes120 },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvButton(
                    text = strings.recorderStartRecording,
                    onClick = { onStart(selectedChoice.toRecordingSelection(currentProgram)) },
                    variant = TvButtonVariant.Primary,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                )
                TvButton(
                    text = strings.recorderClose,
                    onClick = onDismiss,
                    variant = TvButtonVariant.Secondary,
                    modifier = Modifier
                        .width(126.dp)
                        .height(44.dp),
                )
            }
        }
    }
}

private fun RecordingDurationChoice.toRecordingSelection(program: FullScreenEpgProgram?): RecordingDialogSelection {
    val now = System.currentTimeMillis()
    val durationMs = when (this) {
        RecordingDurationChoice.ProgramEnd -> program?.stopMillis?.minus(now) ?: 60 * 60 * 1000L
        RecordingDurationChoice.Minutes30 -> 30 * 60 * 1000L
        RecordingDurationChoice.Minutes60 -> 60 * 60 * 1000L
        RecordingDurationChoice.Minutes120 -> 120 * 60 * 1000L
    }.coerceAtLeast(60_000L)
    return RecordingDialogSelection(
        durationMs = durationMs,
        programTitle = program?.title,
        programStartMs = program?.startMillis,
        programStopMs = program?.stopMillis,
    )
}

@Composable
private fun RecordInfoCard(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.58f),
            style = PlayerMetaStyle.copy(fontSize = 12.sp, lineHeight = 16.sp),
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            style = PlayerMetaStyle.copy(fontSize = 15.sp, lineHeight = 20.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecorderChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    TvButton(
        text = label,
        onClick = onClick,
        selected = selected,
        variant = if (selected) TvButtonVariant.Primary else TvButtonVariant.Secondary,
        focusRequester = focusRequester,
        modifier = modifier.height(42.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
    )
}

@Composable
private fun FullPlayerOverlay(
    playback: FullScreenPlayback,
    isPlaying: Boolean,
    errorText: String?,
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    isFavorite: Boolean,
    playFocusRequester: FocusRequester,
    episodesButtonFocusRequester: FocusRequester,
    brightnessMode: Boolean,
    brightnessValue: Float,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onOpenEpisodes: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenBrightness: () -> Unit,
    onChangeBrightness: (Float) -> Unit,
    onOpenSettings: () -> Unit,
    onCloseBrightness: () -> Unit,
    contextLabel: String,
) {
    val isSeriesContent = playback.contentType == UserContentType.Episode
    val hasPrevious = playback.previousItem != null
    val hasNext = playback.nextItem != null
    PremiumPlayerOverlayFrame(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 40.dp, end = 40.dp, bottom = 24.dp)
                .premiumPlayerGlassSurface()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 14.dp),
        ) {
            PremiumPlayerContextPill(
                label = contextLabel,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = playback.title,
                color = Color.White,
                style = PlayerTitleStyle.copy(fontSize = 29.sp, lineHeight = 34.sp),
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.64f),
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = if (isSeriesContent) playback.subtitle.replace(" - ", " - ") else playback.subtitle,
                color = Color.White.copy(alpha = 0.82f),
                style = PlayerMetaStyle.copy(fontSize = 17.sp, lineHeight = 22.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.64f),
            )
            Spacer(Modifier.height(16.dp))
            MovieSeriesProgressBar(
                positionMs = positionMs,
                durationMs = durationMs,
                bufferedPositionMs = bufferedPositionMs,
                onSeekBy = onSeekBy,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.weight(2.2f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (hasPrevious) {
                        PlayerControlButton(
                            label = if (isSeriesContent) "Episode precedent" else "Titre precedent",
                            icon = Icons.Default.SkipPrevious,
                            onClick = onPlayPrevious,
                            width = 46.dp,
                            height = 46.dp,
                            iconSize = 25.dp,
                        )
                        Spacer(Modifier.width(42.dp))
                    }
                    PlayerControlButton(
                        label = "- 15 sec",
                        icon = Icons.Default.Replay10,
                        onClick = onSeekBack,
                        width = 48.dp,
                        height = 48.dp,
                        iconSize = 24.dp,
                    )
                    Spacer(Modifier.width(46.dp))
                    PlayerControlButton(
                        label = if (isPlaying) "Pause" else "Lecture",
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        onClick = onPlayPause,
                        focusRequester = playFocusRequester,
                        primary = true,
                        width = 72.dp,
                        height = 72.dp,
                        iconSize = 34.dp,
                    )
                    Spacer(Modifier.width(46.dp))
                    PlayerControlButton(
                        label = "+ 15 sec",
                        icon = Icons.Default.Forward10,
                        onClick = onSeekForward,
                        width = 48.dp,
                        height = 48.dp,
                        iconSize = 24.dp,
                    )
                    if (hasNext) {
                        Spacer(Modifier.width(42.dp))
                        PlayerControlButton(
                            label = if (isSeriesContent) "Episode suivant" else "Titre suivant",
                            icon = Icons.Default.SkipNext,
                            onClick = onPlayNext,
                            width = 46.dp,
                            height = 46.dp,
                            iconSize = 25.dp,
                        )
                    }
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    if (brightnessMode) {
                        PlayerBrightnessSlider(
                            value = brightnessValue,
                            onChange = onChangeBrightness,
                            onClose = onCloseBrightness,
                            modifier = Modifier.width(420.dp),
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isSeriesContent) {
                                PlayerUtilityIconButton(
                                    icon = Icons.Default.List,
                                    contentDescription = "Autres episodes",
                                    focusRequester = episodesButtonFocusRequester,
                                    onClick = onOpenEpisodes,
                                )
                            }
                            PlayerUtilityIconButton(
                                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favori",
                                selected = isFavorite,
                                onClick = onToggleFavorite,
                            )
                            PlayerUtilityIconButton(
                                icon = Icons.Default.Brightness7,
                                contentDescription = "Luminosite",
                                onClick = onOpenBrightness,
                            )
                            PlayerUtilityIconButton(
                                icon = Icons.Default.Settings,
                                contentDescription = "Parametres",
                                onClick = onOpenSettings,
                            )
                            PlayerUtilityIconButton(
                                icon = Icons.Default.Fullscreen,
                                contentDescription = "Plein ecran",
                                onClick = { },
                            )
                        }
                    }
                }
            }

        }

        errorText?.let { message ->
            Text(
                text = message,
                color = Color.White,
                style = PlayerMetaStyle,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.66f))
                    .border(BorderStroke(1.dp, SmartVisionColors.Error), RoundedCornerShape(6.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun MovieSeriesProgressBar(
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    onSeekBy: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val safeDuration = durationMs.coerceAtLeast(1L)
    val progress = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val buffered = (bufferedPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(progress, 1f)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = positionMs.formatPlaybackTime(),
            color = Color.White,
            style = PlayerMetaStyle.copy(fontSize = 16.sp, lineHeight = 20.sp),
            modifier = Modifier.width(78.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .tvFocusTarget(
                    state = focusState,
                    pressed = pressed,
                    focusedScale = 1.005f,
                    glowColor = PlayerNeonBlue,
                    cornerRadius = 20.dp,
                )
                .clip(RoundedCornerShape(20.dp))
                .background(if (focusState.isFocused) PlayerNeonBlue.copy(alpha = 0.10f) else Color.Transparent)
                .border(
                    BorderStroke(if (focusState.isFocused) 1.5.dp else 0.dp, PlayerNeonBlue.copy(alpha = 0.72f)),
                    RoundedCornerShape(20.dp),
                )
                .focusable(interactionSource = interactionSource)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            onSeekBy(-15_000L)
                            true
                        }
                        Key.DirectionRight -> {
                            onSeekBy(15_000L)
                            true
                        }
                        else -> false
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.28f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(buffered)
                    .height(6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.34f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(PlayerNeonBlue),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(BorderStroke(3.dp, PlayerNeonBlue), CircleShape),
                )
            }
        }
        Text(
            text = durationMs.formatPlaybackTime(),
            color = Color.White,
            style = PlayerMetaStyle.copy(fontSize = 16.sp, lineHeight = 20.sp),
            textAlign = TextAlign.End,
            modifier = Modifier.width(88.dp),
        )
    }
}

@Composable
private fun PlayerUtilityIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    selected: Boolean = false,
) {
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(46.dp)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.08f,
                glowColor = focusStyle.accent,
                cornerRadius = 50.dp,
            )
            .clip(CircleShape)
            .background(
                when {
                    focusState.isFocused -> focusStyle.background
                    selected -> focusStyle.selectedBackground
                    else -> Color.Transparent
                },
            )
            .border(
                BorderStroke(
                    if (focusState.isFocused || selected) 1.5.dp else 0.dp,
                    if (selected) focusStyle.selectedAccent else focusStyle.accent,
                ),
                CircleShape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) Color(0xFFFF3B45) else Color.White,
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
internal fun PlayerBrightnessSlider(
    value: Float,
    onChange: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sliderFocusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val normalizedValue = (value / 100f).coerceIn(0f, 1f)

    LaunchedEffect(Unit) {
        delay(100)
        runCatching { sliderFocusRequester.requestFocus() }
    }

    Row(
        modifier = modifier
            .height(36.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .focusRequester(sliderFocusRequester)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onChange(-5f)
                        true
                    }
                    Key.DirectionRight -> {
                        onChange(5f)
                        true
                    }
                    Key.Enter, Key.DirectionCenter, Key.Back -> {
                        onClose()
                        true
                    }
                    else -> false
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.WbSunny,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(18.dp),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.10f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(normalizedValue)
                    .height(3.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(PlayerNeonBlue.copy(alpha = 0.95f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(normalizedValue.coerceAtLeast(0.01f))
                    .height(24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (focused) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(PlayerNeonBlue.copy(alpha = 0.10f)),
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(PlayerNeonBlue.copy(alpha = 0.18f)),
                    )
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(PlayerNeonBlue.copy(alpha = 0.28f)),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(BorderStroke(2.dp, PlayerNeonBlue), CircleShape),
                )
            }
        }

        Icon(
            imageVector = Icons.Outlined.WbSunny,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun EpisodesSidePanel(
    playback: FullScreenPlayback,
    onEpisode: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val seasons = remember(playback.seriesEpisodes) {
        playback.seriesEpisodes.map { it.seasonNumber }.filter { it > 0 }.distinct().ifEmpty { listOf(1) }
    }
    var selectedSeason by remember(playback.streamId) {
        mutableStateOf(playback.seriesEpisodes.firstOrNull { it.episodeId == playback.streamId }?.seasonNumber ?: seasons.first())
    }
    val episodes = playback.seriesEpisodes.filter { it.seasonNumber == selectedSeason }.ifEmpty { playback.seriesEpisodes }
    val listState = rememberLazyListState()
    val currentIndex = episodes.indexOfFirst { it.episodeId == playback.streamId }.coerceAtLeast(0)
    val firstFocusRequester = remember { FocusRequester() }

    BackHandler(onBack = onClose)
    LaunchedEffect(selectedSeason, playback.streamId) {
        if (episodes.isNotEmpty()) {
            listState.scrollToItem(currentIndex.coerceAtMost(episodes.lastIndex))
            delay(100)
            runCatching { firstFocusRequester.requestFocus() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(0.29f)
            .background(Color.Black.copy(alpha = 0.88f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)))
            .padding(start = 26.dp, end = 14.dp, top = 54.dp, bottom = 28.dp)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionLeft || event.key == Key.Back)) {
                    onClose()
                    true
                } else {
                    false
                }
            },
    ) {
        Text(
            text = "Autres episodes",
            color = Color.White,
            style = PlayerTitleStyle.copy(fontSize = 28.sp, lineHeight = 34.sp),
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(24.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(seasons) { season ->
                SeasonChip(
                    text = "Saison $season",
                    selected = season == selectedSeason,
                    onClick = { selectedSeason = season },
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(episodes, key = { it.episodeId }) { episode ->
                EpisodePanelRow(
                    episode = episode,
                    isCurrent = episode.episodeId == playback.streamId,
                    focusRequester = if (episode.episodeId == episodes.getOrNull(currentIndex)?.episodeId) firstFocusRequester else null,
                    onClick = { onEpisode(episode.episodeId) },
                )
            }
        }
    }
}

@Composable
private fun SeasonChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .height(44.dp)
            .width(132.dp)
            .tvFocusTarget(state = focusState, pressed = pressed, cornerRadius = 7.dp, glowColor = PlayerNeonBlue)
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected || focusState.isFocused) PlayerNeonBlue.copy(alpha = 0.78f) else Color.Transparent)
            .border(BorderStroke(1.dp, if (selected || focusState.isFocused) PlayerNeonBlue else Color.White.copy(alpha = 0.18f)), RoundedCornerShape(7.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = Color.White, style = PlayerMetaStyle, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EpisodePanelRow(
    episode: PlayerEpisodeItem,
    isCurrent: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = 1.02f,
                cornerRadius = 8.dp,
                glowColor = PlayerNeonBlue,
            )
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    focusState.isFocused -> PlayerNeonBlue.copy(alpha = 0.84f)
                    isCurrent -> PlayerNeonBlue.copy(alpha = 0.28f)
                    else -> Color.Transparent
                },
            )
            .border(
                BorderStroke(
                    if (focusState.isFocused || isCurrent) 2.dp else 1.dp,
                    if (focusState.isFocused || isCurrent) PlayerNeonBlue else Color.Transparent,
                ),
                RoundedCornerShape(8.dp),
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EpisodeThumbnail(episode.thumbnailUrl)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.label,
                color = Color.White,
                style = PlayerMetaStyle.copy(fontSize = 13.sp, lineHeight = 16.sp),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = episode.title,
                color = Color.White,
                style = PlayerMetaStyle.copy(fontSize = 18.sp, lineHeight = 21.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = episode.description.orEmpty(),
                color = Color.White.copy(alpha = 0.72f),
                style = PlayerTinyStyle.copy(fontSize = 12.sp, lineHeight = 15.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = episode.duration.orEmpty(),
            color = Color.White.copy(alpha = 0.86f),
            style = PlayerMetaStyle.copy(fontSize = 13.sp, lineHeight = 16.sp),
            textAlign = TextAlign.End,
            modifier = Modifier.width(48.dp),
        )
    }
}

@Composable
private fun EpisodeThumbnail(imageUrl: String?) {
    Box(
        modifier = Modifier
            .width(96.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF0B2139))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)), RoundedCornerShape(5.dp)),
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
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = 0.76f), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun PlayerTopGlassBar(
    playback: FullScreenPlayback,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(PlayerNeonBlue.copy(alpha = 0.08f), PlayerGlassShape)
            .border(BorderStroke(1.dp, PlayerGlassGlowBorder), PlayerGlassShape)
            .padding(1.dp)
            .clip(PlayerGlassShape)
            .background(PlayerGlassBackground)
            .border(BorderStroke(1.dp, PlayerGlassBorder), PlayerGlassShape)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerLogo()
        PlayerVerticalSeparator()
        PlaybackPill(playback.badge)
        PlayerVerticalSeparator()
        Text(
            text = playback.title,
            color = Color.White,
            style = PlayerTitleStyle.copy(fontSize = 17.sp, lineHeight = 21.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        PlayerVerticalSeparator()
        Text(
            text = playback.overlayRightText.ifBlank { playback.status },
            color = Color.White.copy(alpha = 0.78f),
            style = PlayerTitleStyle.copy(fontSize = 16.sp, lineHeight = 20.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.width(84.dp),
        )
    }
}

@Composable
private fun PlayerVerticalSeparator() {
    Box(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .width(1.dp)
            .height(24.dp)
            .background(Color.White.copy(alpha = 0.18f)),
    )
}

@Composable
private fun PlayerLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.smartvision_logo_wide),
        contentDescription = "SmartVision",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(138.dp)
            .height(34.dp),
    )
}

@Composable
private fun PlayerInfoCard(
    playback: FullScreenPlayback,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xB4071123))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), RoundedCornerShape(6.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlaybackPill(playback.badge)
            Spacer(Modifier.width(10.dp))
            Text(
                text = playback.title,
                color = Color.White,
                style = PlayerTitleStyle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = playback.subtitle,
                color = SmartVisionColors.TextSecondary,
                style = PlayerMetaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = playback.status,
                color = SmartVisionColors.TextSecondary,
                style = PlayerMetaStyle,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            playback.infoPills.take(3).forEach { pill ->
                InfoPill(pill)
            }
        }
    }
}

@Composable
private fun PlaybackPill(text: String) {
    val badgeColor = when (text) {
        "LIVE" -> Color(0xFFEF233C)
        "VOD" -> Color(0xFFFF8A00)
        "SERIE" -> PlayerNeonBlue
        else -> SmartVisionColors.Primary
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeColor.copy(alpha = 0.16f))
            .border(BorderStroke(1.dp, badgeColor.copy(alpha = 0.72f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(badgeColor),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, style = PlayerMetaStyle.copy(fontSize = 12.sp, lineHeight = 15.sp), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color.Black.copy(alpha = 0.36f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = Color.White, style = PlayerTinyStyle, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlayerProgressBar(
    playback: FullScreenPlayback,
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    modifier: Modifier = Modifier,
    onSeekBy: (Long) -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hasDuration = durationMs > 0L
    val progress = if (hasDuration) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }
    val bufferedProgress = if (hasDuration) {
        (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(progress, 1f)
    } else {
        1f
    }
    val startText = if (hasDuration) positionMs.formatPlaybackTime() else playback.status
    val endText = if (hasDuration) durationMs.formatPlaybackTime() else "Direct"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .tvFocusTarget(
                state = focusState,
                enabled = hasDuration,
                pressed = pressed,
                focusedScale = 1.01f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = 14.dp,
            )
            .clip(RoundedCornerShape(14.dp))
            .background(if (focusState.isFocused) Color(0xA6111F36) else Color.Transparent)
            .border(
                BorderStroke(if (focusState.isFocused) 2.dp else 1.dp, if (focusState.isFocused) PlayerNeonBlue else Color.Transparent),
                RoundedCornerShape(14.dp),
            )
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != AndroidKeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                        onSeekBy(-10_000L)
                        true
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                        onSeekBy(10_000L)
                        true
                    }
                    else -> false
                }
            }
            .clickable(
                enabled = hasDuration,
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
            )
            .focusable(enabled = hasDuration, interactionSource = interactionSource)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(startText, color = Color.White, style = PlayerMetaStyle.copy(fontSize = 12.sp, lineHeight = 15.sp))
        Spacer(Modifier.width(10.dp))
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
        ) {
            val thumbOffset = (maxWidth - 10.dp) * progress
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.22f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(bufferedProgress)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.18f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(PlayerNeonBlue),
                )
            }
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(PlayerNeonBlue),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(endText, color = Color.White, style = PlayerMetaStyle.copy(fontSize = 12.sp, lineHeight = 15.sp))
    }
}

@Composable
private fun PlayerOptionMenu(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val firstFocusRequester = remember { FocusRequester() }

    LaunchedEffect(title) {
        delay(100)
        runCatching { firstFocusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.36f)),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 42.dp)
                .width(280.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xF20A1425))
                .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
                .padding(18.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                style = PlayerTitleStyle,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(14.dp))
            options.forEachIndexed { index, label ->
                TvButton(
                    text = label,
                    onClick = { onSelect(index) },
                    selected = index == selectedIndex,
                    focusRequester = firstFocusRequester.takeIf { index == selectedIndex.coerceIn(options.indices) },
                    variant = if (index == selectedIndex) TvButtonVariant.Primary else TvButtonVariant.Secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                )
                Spacer(Modifier.height(7.dp))
            }
            TvButton(
                text = "Fermer",
                onClick = onClose,
                variant = TvButtonVariant.Text,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
            )
        }
    }
}

@Composable
private fun NextEpisodeCard(
    nextEpisode: NextEpisodePlayback,
    countdown: Int,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playNowFocusRequester = remember { FocusRequester() }

    LaunchedEffect(nextEpisode.episodeId) {
        delay(100)
        runCatching { playNowFocusRequester.requestFocus() }
    }

    Column(
        modifier = modifier
            .width(580.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xF20A1425))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)), RoundedCornerShape(8.dp))
            .padding(20.dp),
    ) {
        Text(
            text = buildAnnotatedString {
                append("Episode suivant dans ")
                withStyle(SpanStyle(color = PlayerNeonBlue)) {
                    append(countdown.coerceAtLeast(0).toString())
                }
                append(" secondes")
            },
            color = Color.White,
            style = PlayerTitleStyle.copy(fontSize = 20.sp, lineHeight = 25.sp),
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            EpisodeThumbnail(nextEpisode.thumbnailUrl)
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nextEpisode.label,
                    color = Color.White,
                    style = PlayerTitleStyle.copy(fontSize = 18.sp, lineHeight = 22.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = nextEpisode.title,
                    color = Color.White.copy(alpha = 0.78f),
                    style = PlayerMetaStyle.copy(fontSize = 17.sp, lineHeight = 21.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TvButton(
                text = "Voir episode",
                onClick = onPlayNow,
                focusRequester = playNowFocusRequester,
                modifier = Modifier
                    .weight(1.3f)
                    .height(42.dp),
            )
            TvButton(
                text = "Annuler",
                onClick = onCancel,
                variant = TvButtonVariant.Secondary,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
            )
        }
    }
}

@Composable
private fun PlayerControlButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    primary: Boolean = false,
    width: Dp = 48.dp,
    height: Dp = 48.dp,
    iconSize: Dp = 20.dp,
) {
    val focusState = rememberTvFocusState()
    val focusStyle = LocalTvFocusStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = CircleShape
    val outerGlowSize = maxOf(width, height) + if (primary) 12.dp else 8.dp
    val backgroundColor by animateColorAsState(
        targetValue = when {
            primary && focusState.isFocused -> focusStyle.background
            primary -> PlayerButtonBackground.copy(alpha = 0.74f)
            focusState.isFocused -> focusStyle.background
            else -> PlayerButtonBackground.copy(alpha = 0.62f)
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "playerControlBackground",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused -> focusStyle.accent
            primary -> focusStyle.activeAccent
            else -> Color.White.copy(alpha = 0.22f)
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "playerControlBorder",
    )

    Column(
        modifier = modifier
            .width(outerGlowSize + 8.dp)
            .height(outerGlowSize + 16.dp)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = if (primary) 1.06f else 1.035f,
                glowColor = focusStyle.accent,
                cornerRadius = 50.dp,
            )
            .zIndex(if (focusState.isFocused) 3f else 0f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(PaddingValues(horizontal = 2.dp, vertical = 2.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(outerGlowSize)
                .background(
                    if (focusState.isFocused) focusStyle.background else Color.Transparent,
                    shape,
                )
                .padding(if (focusState.isFocused) 5.dp else 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = width, height = height)
                    .clip(shape)
                    .background(backgroundColor)
                    .border(
                        BorderStroke(
                            if (focusState.isFocused || primary) 2.dp else 1.dp,
                            borderColor,
                        ),
                        shape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = if (focusState.isFocused || primary) Color.White else SmartVisionColors.TextSecondary,
            style = PlayerMetaStyle.copy(fontSize = 10.sp, lineHeight = 12.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val overlayKeyCodes = setOf(
    AndroidKeyEvent.KEYCODE_DPAD_UP,
    AndroidKeyEvent.KEYCODE_DPAD_DOWN,
    AndroidKeyEvent.KEYCODE_DPAD_LEFT,
    AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
    AndroidKeyEvent.KEYCODE_DPAD_CENTER,
    AndroidKeyEvent.KEYCODE_ENTER,
)

private val SuppressedPlayerAnomalyTypes = setOf(
    "PLAYER_EXIT_STEP",
    "PLAYER_EXIT",
    "PLAYER_EXIT_BEGIN",
    "PLAYER_RELEASE_DONE",
    "PLAYER_PROGRESS_SAVE_DONE",
)

internal val PlayerTitleStyle = TextStyle(
    fontSize = 16.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.sp,
)

internal val PlayerMetaStyle = TextStyle(
    fontSize = 10.sp,
    lineHeight = 13.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
)

internal val PlayerTinyStyle = TextStyle(
    fontSize = 8.sp,
    lineHeight = 10.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
)

private val PlayerGlassShape = RoundedCornerShape(14.dp)
private val PlayerGlassBackground = Color(0x26071222)
private val PlayerGlassBorder = Color.White.copy(alpha = 0.22f)
internal val PlayerOverlaySurface = Color(0xFF0D0D0D)
internal val PlayerNeonBlue = Color(0xFF1A6AFF)
internal val PlayerFavoriteRed = Color(0xFFFF3B30)
private val PlayerGlassGlowBorder = PlayerNeonBlue.copy(alpha = 0.18f)
private val PlayerButtonBackground = Color(0x990A1B38)

private fun String.cleanTitle(): String =
    replace(Regex("\\s+"), " ")
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" SD", "", ignoreCase = true)
        .trim()
        .ifBlank { "Live TV" }

private fun Int.toLiveChannelNumber(fallbackId: Int): String =
    (takeIf { it > 0 } ?: fallbackId).toString().padStart(3, '0')

private fun List<EpgProgram>.toFullScreenPrograms(): List<FullScreenEpgProgram> {
    if (isEmpty()) return emptyList()
    val now = System.currentTimeMillis()
    val currentIndex = indexOfFirst { program ->
        val start = program.startMillis
        val stop = program.stopMillis
        start != null && stop != null && now in start..stop
    }.takeIf { it >= 0 } ?: 0
    return mapIndexed { index, program ->
        FullScreenEpgProgram(
            title = program.title,
            description = program.description,
            timeRange = program.timeRange,
            isCurrent = index == currentIndex,
            startMillis = program.startMillis,
            stopMillis = program.stopMillis,
        )
    }
}

private fun LiveChannel.toFullScreenPlayback(
    previous: LiveChannel?,
    next: LiveChannel?,
    epgPrograms: List<FullScreenEpgProgram>,
    fallback: FullScreenPlayback,
): FullScreenPlayback {
    val currentProgram = epgPrograms.firstOrNull { it.isCurrent } ?: epgPrograms.firstOrNull()
    return FullScreenPlayback(
        streamId = streamId,
        contentType = UserContentType.Live,
        title = name.cleanTitle(),
        subtitle = categoryName,
        url = directStreamUrl?.takeIf { it.isNotBlank() } ?: fallback.url,
        fallbackUrl = fallback.fallbackUrl,
        badge = "LIVE",
        status = currentProgram?.title ?: currentProgram?.timeRange ?: "Direct",
        infoPills = fallback.infoPills,
        imageUrl = logoUrl?.takeIf { it.isNotBlank() } ?: fallback.imageUrl,
        categoryId = categoryId,
        overlayRightText = number.toLiveChannelNumber(streamId),
        previousItem = previous?.toAdjacentPlayback(),
        nextItem = next?.toAdjacentPlayback(),
        resumePositionMs = fallback.resumePositionMs,
        epgPrograms = epgPrograms,
    )
}

private fun LiveChannel.toAdjacentPlayback(): AdjacentPlayback =
    AdjacentPlayback(
        streamId = streamId,
        title = name.cleanTitle(),
        label = number.toLiveChannelNumber(streamId),
    )

private fun Movie.toFullScreenPlayback(
    previous: Movie?,
    next: Movie?,
    fallback: FullScreenPlayback,
): FullScreenPlayback = fallback.copy(
    streamId = streamId,
    contentType = UserContentType.Movie,
    title = title.cleanTitle(),
    subtitle = categoryName,
    status = duration ?: "Film",
    infoPills = listOf("HD", containerExtension.uppercase()).distinct(),
    imageUrl = posterUrl,
    categoryId = categoryId,
    overlayRightText = year.orEmpty(),
    previousItem = previous?.let {
        AdjacentPlayback(it.streamId, it.title.cleanTitle(), "Film precedent")
    },
    nextItem = next?.let {
        AdjacentPlayback(it.streamId, it.title.cleanTitle(), "Film suivant")
    },
)

private fun Episode.toFullScreenPlayback(
    series: TvSeries?,
    episodes: List<Episode>,
    previous: Episode?,
    next: Episode?,
    fallback: FullScreenPlayback,
): FullScreenPlayback {
    val seriesTitle = series?.title?.cleanTitle() ?: fallback.title
    val seriesImage = series?.posterUrl ?: fallback.imageUrl
    return fallback.copy(
        streamId = episodeId,
        contentType = UserContentType.Episode,
        title = seriesTitle,
        subtitle = "${seasonEpisodeLabel()} - ${title.cleanTitle()}",
        status = duration ?: "Episode",
        infoPills = listOf("HD", containerExtension.uppercase()).distinct(),
        imageUrl = seriesImage,
        categoryId = series?.categoryId ?: fallback.categoryId,
        overlayRightText = seasonEpisodeLabel(),
        parentContentId = seriesId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        previousItem = previous?.let {
            AdjacentPlayback(it.episodeId, it.title.cleanTitle(), it.seasonEpisodeLabel())
        },
        nextItem = next?.let {
            AdjacentPlayback(it.episodeId, it.title.cleanTitle(), it.seasonEpisodeLabel())
        },
        nextEpisode = next?.let {
            NextEpisodePlayback(it.episodeId, it.title.cleanTitle(), it.seasonEpisodeLabel(), seriesImage)
        },
        seriesEpisodes = episodes.map {
            PlayerEpisodeItem(
                episodeId = it.episodeId,
                seasonNumber = it.seasonNumber,
                episodeNumber = it.episodeNumber,
                label = it.seasonEpisodeLabel(),
                title = it.title.cleanTitle(),
                duration = it.duration,
                description = it.plot?.cleanTitle(),
                thumbnailUrl = seriesImage,
            )
        },
    )
}

private fun Episode.seasonEpisodeLabel(): String =
    "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"

private fun String?.extractReleaseYear(): String? =
    this
        ?.let { Regex("(19|20)\\d{2}").find(it)?.value }
        ?.takeIf { year -> year.toIntOrNull()?.let { it in 1900..2100 } == true }

private fun XtreamSeriesEpisode.seasonEpisodeLabel(): String =
    "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"

private fun FullScreenContentKind.storageType(): String =
    when (this) {
        FullScreenContentKind.Live -> UserContentType.Live
        FullScreenContentKind.Movie -> UserContentType.Movie
        FullScreenContentKind.Episode -> UserContentType.Episode
        FullScreenContentKind.LocalMedia -> UserContentType.LocalMedia
    }

private fun FullScreenContentKind.playbackKind(): PlaybackKind =
    when (this) {
        FullScreenContentKind.Live -> PlaybackKind.Live
        FullScreenContentKind.Movie -> PlaybackKind.Movie
        FullScreenContentKind.Episode -> PlaybackKind.Episode
        FullScreenContentKind.LocalMedia -> PlaybackKind.Movie
    }

private fun String.isGeneratedTitleFor(kind: FullScreenContentKind, id: Int): Boolean {
    val normalized = trim()
    return when (kind) {
        FullScreenContentKind.Live -> normalized.equals("Chaine $id", ignoreCase = true) ||
            normalized.matches(Regex("(?i)^chaine\\s+\\d+$"))
        FullScreenContentKind.Movie -> normalized.equals("Film $id", ignoreCase = true)
        FullScreenContentKind.Episode -> normalized.equals("Episode $id", ignoreCase = true) ||
            normalized.equals("Series", ignoreCase = true) ||
            normalized.equals("Serie", ignoreCase = true)
        FullScreenContentKind.LocalMedia -> false
    }
}

private fun FullScreenContentKind.toPlayerContentType(): PlayerContentType =
    when (this) {
        FullScreenContentKind.Live -> PlayerContentType.LIVE_TV
        FullScreenContentKind.Movie -> PlayerContentType.MOVIE
        FullScreenContentKind.Episode -> PlayerContentType.SERIES
        FullScreenContentKind.LocalMedia -> PlayerContentType.MOVIE
    }

private fun String.toBehaviorContentType(): String =
    when (this) {
        UserContentType.Live -> "LIVE_TV"
        UserContentType.Movie -> "MOVIE"
        UserContentType.Episode -> "EPISODE"
        UserContentType.Series -> "SERIES"
        UserContentType.LocalMedia -> "LOCAL_MEDIA"
        else -> "UNKNOWN"
    }

private fun MediaCenterPlayback.toFullScreenPlayback(): FullScreenPlayback {
    val mediaLabel = mediaType.name.uppercase(Locale.US)
    return FullScreenPlayback(
        streamId = id.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
        contentType = UserContentType.LocalMedia,
        title = displayName.cleanTitle(),
        subtitle = listOf(source.localLabel(), relativePath.substringBeforeLast('/', ""))
            .filter { it.isNotBlank() }
            .joinToString("  |  "),
        url = uri.toString(),
        fallbackUrl = uri.toString(),
        badge = mediaLabel,
        status = mimeType ?: "Local media",
        infoPills = listOf(mediaLabel, formatLocalMediaSize(sizeBytes)).filter { it.isNotBlank() },
        overlayRightText = "MEDIA",
    )
}

private fun MediaCenterSource.localLabel(): String =
    when (this) {
        MediaCenterSource.Recording -> "Recordings"
        MediaCenterSource.Import -> "Imports"
        MediaCenterSource.Transfer -> "Transfers"
        MediaCenterSource.Local -> "Local"
    }

private fun formatLocalMediaSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    if (bytes < 1024L) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes / 1024.0
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }
    return String.format(Locale.US, "%.1f %s", value, units[unitIndex])
}

private fun Long.validDurationMs(): Long =
    takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L

internal fun Long.formatPlaybackTime(): String {
    val totalSeconds = (this / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
