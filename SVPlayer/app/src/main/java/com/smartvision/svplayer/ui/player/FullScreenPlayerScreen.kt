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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.UserContentType
import com.smartvision.svplayer.data.repository.XtreamRepository
import com.smartvision.svplayer.domain.model.PlaybackKind
import com.smartvision.svplayer.domain.usecase.BuildPlaybackRequestUseCase
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

enum class FullScreenContentKind {
    Live,
    Movie,
    Episode,
}

private const val MinimumLiveHistoryMs = 5_000L
private const val PlayerReleaseDelayMs = 80L

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
    val resumePositionMs: Long = 0L,
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
)

private enum class PlayerOverlayMenu {
    None,
    Subtitles,
    Settings,
}

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
    private val userContentRepository: UserContentRepository,
    private val buildPlaybackRequest: BuildPlaybackRequestUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(resolvePlayback(contentId, kind, xtreamRepository))
    val uiState: StateFlow<FullScreenPlayback> = _uiState
    private val saveProgressMutex = Mutex()

    init {
        viewModelScope.launch {
            val stored = userContentRepository.getProgress(kind.storageType(), contentId)
            if (kind != FullScreenContentKind.Live && stored != null && stored.positionMs > 0L) {
                _uiState.value = _uiState.value.copy(resumePositionMs = stored.positionMs)
            }
            val localPlayback = buildPlaybackRequest(kind.playbackKind(), contentId.toString())
            if (localPlayback != null && _uiState.value.title.isGeneratedTitleFor(kind, contentId)) {
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
    ): FullScreenPlayback =
        when (kind) {
            FullScreenContentKind.Live -> resolveLive(contentId, xtreamRepository)
            FullScreenContentKind.Movie -> resolveMovie(contentId, xtreamRepository)
            FullScreenContentKind.Episode -> resolveEpisode(contentId, xtreamRepository)
        }

    private fun resolveLive(
        streamId: Int,
        xtreamRepository: XtreamRepository,
    ): FullScreenPlayback =
        xtreamRepository.getCachedLiveStream(streamId)?.let { stream ->
            val categoryName = xtreamRepository.getCachedCategories()
                .firstOrNull { it.id == stream.categoryId }
                ?.name
                ?: "Live TV"
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
                status = "Direct",
                infoPills = listOf("16+", "HD", "5.1"),
                imageUrl = stream.streamIcon,
                categoryId = stream.categoryId,
                overlayRightText = stream.number.toString().padStart(3, '0'),
                previousItem = previous?.let {
                    AdjacentPlayback(
                        streamId = it.streamId,
                        title = it.name.cleanTitle(),
                        label = it.number.toString().padStart(3, '0'),
                    )
                },
                nextItem = next?.let {
                    AdjacentPlayback(
                        streamId = it.streamId,
                        title = it.name.cleanTitle(),
                        label = it.number.toString().padStart(3, '0'),
                    )
                },
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
            val seriesName = series?.title?.cleanTitle() ?: "Series"
            val seriesCover = series?.coverUrl
            val previousEpisode = xtreamRepository.getCachedPreviousEpisode(episodeId)
            val nextEpisode = xtreamRepository.getCachedNextEpisode(episodeId)
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
    onPlayLive: (Int) -> Unit = {},
    onPlayMovie: (Int) -> Unit = {},
    onPlayEpisode: (Int) -> Unit = {},
) {
    val container = LocalAppContainer.current
    val viewModel: FullScreenPlayerViewModel = viewModel(
        key = "fullscreen-${kind.name.lowercase()}-$streamId",
        factory = viewModelFactory {
            FullScreenPlayerViewModel(
                contentId = streamId,
                kind = kind,
                xtreamRepository = container.xtreamRepository,
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
        behaviorReporter = container.behaviorReporter,
        adRequestTimeoutSeconds = container.adConfigProvider.current().requestTimeoutSeconds,
        onBack = onBack,
        onPlayLive = onPlayLive,
        onPlayMovie = onPlayMovie,
        onPlayEpisode = onPlayEpisode,
        onProgressSnapshot = viewModel::saveProgress,
    )
}

@Composable
private fun FullScreenPlayerScreen(
    playback: FullScreenPlayback,
    contentKind: FullScreenContentKind,
    monetizationManager: MonetizationManager,
    idleVastAdLoader: IdleVastAdLoader,
    anomalyReporter: AnomalyReporter,
    behaviorReporter: BehaviorReporter,
    adRequestTimeoutSeconds: Long,
    onBack: () -> Unit,
    onPlayLive: (Int) -> Unit,
    onPlayMovie: (Int) -> Unit,
    onPlayEpisode: (Int) -> Unit,
    onProgressSnapshot: suspend (positionMs: Long, durationMs: Long) -> Boolean,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val latestUrl by rememberUpdatedState(playback.url)
    val latestFallbackUrl by rememberUpdatedState(playback.fallbackUrl)
    val playFocusRequester = remember { FocusRequester() }
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
    var subtitleTracks by remember { mutableStateOf<List<SubtitleTrackOption>>(emptyList()) }
    var selectedSubtitleId by remember { mutableStateOf<String?>(null) }
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

    val playerView = remember {
        PlayerView(context).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
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
            focusPlayWhenOverlayShows = true
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
    }

    fun prepareContentWithoutAd() {
        prepareMedia(MediaItem.fromUri(playback.url))
    }

    fun refreshStalledPlayback() {
        if (adGateActive || exiting || stalledRefreshCount >= 2) return
        stalledRefreshCount += 1
        val resumeAt = player.currentPosition.coerceAtLeast(0L)
        anomalyReporter.reportAsync(
            anomalyType = "Relance buffering",
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
        when (playback.contentType) {
            UserContentType.Live -> onPlayLive(target.streamId)
            UserContentType.Movie -> onPlayMovie(target.streamId)
            UserContentType.Episode -> onPlayEpisode(target.streamId)
            else -> Unit
        }
    }

    fun handleLiveChannelKey(keyCode: Int): Boolean {
        if (playback.contentType != UserContentType.Live) return false
        if (keyCode != AndroidKeyEvent.KEYCODE_DPAD_UP && keyCode != AndroidKeyEvent.KEYCODE_DPAD_DOWN) return false
        if (adGateActive) return true
        if (!overlayVisible) {
            showOverlay(requestPlayFocus = true)
            return true
        }
        val target = if (keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP) {
            playback.previousItem
        } else {
            playback.nextItem
        }
        playAdjacent(target)
        showOverlay()
        return true
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
            onBack()
        }
    }

    BackHandler {
        if (adGateActive) {
            Unit
        } else {
            exitPlayer("back_handler")
        }
    }

    LaunchedEffect(playback.url, playback.resumePositionMs, contentKind) {
        adGateActive = false
        adStarted = false
        activeAdRequestId = null
        activeAdCreative = null
        firedAdTrackingEvents = emptySet()
        adPositionMs = 0L
        adDurationMs = 0L
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
        if (overlayVisible && activeMenu == PlayerOverlayMenu.None && nextEpisodeCountdown == null) {
            if (focusPlayWhenOverlayShows) {
                delay(120)
                playFocusRequester.requestFocus()
                focusPlayWhenOverlayShows = false
            }
            delay(4_800)
            overlayVisible = false
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
                anomalyType = "buffering bloqué",
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
                }
                if (playbackState == Player.STATE_ENDED && adGateActive) {
                    buffering = true
                    finishAdAndStartContent(skipped = false)
                    return
                }
                if (playbackState == Player.STATE_ENDED && playback.nextEpisode != null && !nextEpisodeDismissed) {
                    nextEpisodeCountdown = 3
                    overlayVisible = true
                    activeMenu = PlayerOverlayMenu.None
                }
                if (playbackState == Player.STATE_ENDED && !adGateActive && !playbackCompletedReported) {
                    playbackCompletedReported = true
                    behaviorReporter.reportAsync(coroutineScope, "PLAYBACK_COMPLETED", behaviorContent("ended", player.currentPosition, player.duration))
                }
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
                errorText = "Flux indisponible"
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
                if (event.nativeKeyEvent.action == AndroidKeyEvent.ACTION_DOWN && handleLiveChannelKey(keyCode)) {
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
                                exitPlayer("player_view_back")
                                true
                            }

                            event.action == AndroidKeyEvent.ACTION_DOWN && handleLiveChannelKey(keyCode) -> true

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
                it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                it.isFocusable = !adGateActive
                it.isFocusableInTouchMode = !adGateActive
                if (adGateActive) it.clearFocus()
                it.setOnKeyListener { _, keyCode, event ->
                    when {
                        keyCode == AndroidKeyEvent.KEYCODE_BACK &&
                            event.action == AndroidKeyEvent.ACTION_UP &&
                            adGateActive -> true

                        keyCode == AndroidKeyEvent.KEYCODE_BACK && event.action == AndroidKeyEvent.ACTION_UP -> {
                            exitPlayer("player_view_back_update")
                            true
                        }

                        event.action == AndroidKeyEvent.ACTION_DOWN && handleLiveChannelKey(keyCode) -> {
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

        if (overlayVisible && !adGateActive) {
            FullPlayerOverlay(
                playback = playback,
                isPlaying = isPlaying,
                errorText = errorText,
                positionMs = positionMs,
                durationMs = durationMs,
                bufferedPositionMs = bufferedPositionMs,
                playFocusRequester = playFocusRequester,
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
                        player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
                    }
                    showOverlay()
                },
                onSeekForward = {
                    if (player.isCurrentMediaItemSeekable) {
                        player.seekTo(player.currentPosition + 10_000L)
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
                onOpenSubtitles = {
                    activeMenu = PlayerOverlayMenu.Subtitles
                    showOverlay()
                },
                onOpenSettings = {
                    activeMenu = PlayerOverlayMenu.Settings
                    showOverlay()
                },
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

        if (activeMenu == PlayerOverlayMenu.Settings) {
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
                        .align(Alignment.CenterEnd)
                        .padding(end = 42.dp),
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
            skipFocusRequester.requestFocus()
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
private fun FullPlayerOverlay(
    playback: FullScreenPlayback,
    isPlaying: Boolean,
    errorText: String?,
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    playFocusRequester: FocusRequester,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val showProgress = playback.contentType != UserContentType.Live
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        PlayerTopGlassBar(
            playback = playback,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 24.dp, end = 24.dp, top = 18.dp),
        )

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

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 44.dp, end = 44.dp, bottom = 28.dp)
                .fillMaxWidth()
                .height(if (showProgress) 166.dp else 132.dp)
                .clip(PlayerGlassShape)
                .background(PlayerGlassBackground)
                .border(BorderStroke(1.dp, PlayerGlassBorder), PlayerGlassShape)
                .padding(horizontal = 28.dp, vertical = if (showProgress) 14.dp else 18.dp),
        ) {
            if (showProgress) {
                PlayerProgressBar(
                    playback = playback,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    bufferedPositionMs = bufferedPositionMs,
                    modifier = Modifier.fillMaxWidth(),
                    onSeekBy = onSeekBy,
                )
                Spacer(Modifier.height(12.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerControlButton("Sous-titres", Icons.Default.Subtitles, onOpenSubtitles)
                PlayerControlButton("- 10 sec", Icons.Default.Replay10, onSeekBack)
                PlayerControlButton(
                    label = if (isPlaying) "Pause" else "Lecture",
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    onClick = onPlayPause,
                    focusRequester = playFocusRequester,
                    primary = true,
                    width = 96.dp,
                    height = 96.dp,
                    iconSize = 34.dp,
                )
                PlayerControlButton("+ 10 sec", Icons.Default.Forward10, onSeekForward)
                PlayerControlButton("Parametres", Icons.Default.Settings, onOpenSettings)
            }
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
            .height(76.dp)
            .clip(PlayerGlassShape)
            .background(PlayerGlassBackground)
            .border(BorderStroke(1.dp, PlayerGlassBorder), PlayerGlassShape)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerLogo()
        PlayerVerticalSeparator()
        PlaybackPill(playback.badge)
        PlayerVerticalSeparator()
        Text(
            text = playback.title,
            color = Color.White,
            style = PlayerTitleStyle.copy(fontSize = 21.sp, lineHeight = 25.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.15f),
        )
        PlayerVerticalSeparator()
        Text(
            text = playback.subtitle,
            color = Color.White.copy(alpha = 0.9f),
            style = PlayerTitleStyle.copy(fontSize = 20.sp, lineHeight = 24.sp),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.9f),
        )
        Text(
            text = playback.overlayRightText.ifBlank { playback.status },
            color = Color.White.copy(alpha = 0.78f),
            style = PlayerTitleStyle.copy(fontSize = 20.sp, lineHeight = 24.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.width(104.dp),
        )
    }
}

@Composable
private fun PlayerVerticalSeparator() {
    Box(
        modifier = Modifier
            .padding(horizontal = 22.dp)
            .width(1.dp)
            .height(30.dp)
            .background(Color.White.copy(alpha = 0.24f)),
    )
}

@Composable
private fun PlayerLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.smartvision_logo_wide),
        contentDescription = "SmartVision",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(178.dp)
            .height(44.dp),
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
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(badgeColor),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, style = PlayerMetaStyle.copy(fontSize = 15.sp, lineHeight = 18.sp), fontWeight = FontWeight.Bold)
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
            .height(34.dp)
            .tvFocusTarget(
                state = focusState,
                enabled = hasDuration,
                pressed = pressed,
                focusedScale = 1.01f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = 18.dp,
            )
            .clip(RoundedCornerShape(18.dp))
            .background(if (focusState.isFocused) Color(0xA6111F36) else Color.Transparent)
            .border(
                BorderStroke(if (focusState.isFocused) 2.dp else 1.dp, if (focusState.isFocused) PlayerNeonBlue else Color.Transparent),
                RoundedCornerShape(18.dp),
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
        Text(startText, color = Color.White, style = PlayerMetaStyle.copy(fontSize = 13.sp, lineHeight = 16.sp))
        Spacer(Modifier.width(10.dp))
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(10.dp),
        ) {
            val thumbOffset = (maxWidth - 10.dp) * progress
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
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
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(PlayerNeonBlue),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(endText, color = Color.White, style = PlayerMetaStyle.copy(fontSize = 13.sp, lineHeight = 16.sp))
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
        firstFocusRequester.requestFocus()
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
        playNowFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .width(340.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xF20A1425))
            .border(BorderStroke(1.dp, SmartVisionColors.Primary), RoundedCornerShape(8.dp))
            .padding(18.dp),
    ) {
        Text(
            text = "Episode suivant dans ${countdown.coerceAtLeast(0)}",
            color = SmartVisionColors.CyanAccent,
            style = PlayerMetaStyle,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = "${nextEpisode.label}  ${nextEpisode.title}",
            color = Color.White,
            style = PlayerTitleStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TvButton(
                text = "Lire maintenant",
                onClick = onPlayNow,
                leadingIcon = Icons.Default.SkipNext,
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
    width: Dp = 68.dp,
    height: Dp = 68.dp,
    iconSize: Dp = 23.dp,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = CircleShape
    val backgroundColor by animateColorAsState(
        targetValue = when {
            primary && focusState.isFocused -> PlayerNeonBlue.copy(alpha = 0.24f)
            primary -> Color.Black.copy(alpha = 0.32f)
            focusState.isFocused -> PlayerNeonBlue.copy(alpha = 0.16f)
            else -> Color.White.copy(alpha = 0.05f)
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "playerControlBackground",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            focusState.isFocused || primary -> PlayerNeonBlue
            else -> Color.White.copy(alpha = 0.22f)
        },
        animationSpec = tween(SmartVisionDimensions.FocusAnimationMillis),
        label = "playerControlBorder",
    )

    Column(
        modifier = modifier
            .width(width)
            .height(height + 28.dp)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                pressed = pressed,
                focusedScale = if (primary) 1.08f else 1.04f,
                glowColor = PlayerNeonBlue,
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
        Spacer(Modifier.height(7.dp))
        Text(
            text = label,
            color = if (focusState.isFocused || primary) Color.White else SmartVisionColors.TextSecondary,
            style = PlayerMetaStyle.copy(fontSize = 13.sp, lineHeight = 16.sp),
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

private val PlayerTitleStyle = TextStyle(
    fontSize = 16.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.sp,
)

private val PlayerMetaStyle = TextStyle(
    fontSize = 10.sp,
    lineHeight = 13.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
)

private val PlayerTinyStyle = TextStyle(
    fontSize = 8.sp,
    lineHeight = 10.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
)

private val PlayerGlassShape = RoundedCornerShape(28.dp)
private val PlayerGlassBackground = Color(0x66040E20)
private val PlayerGlassBorder = Color.White.copy(alpha = 0.22f)
private val PlayerNeonBlue = Color(0xFF0A84FF)

private fun String.cleanTitle(): String =
    replace(Regex("\\s+"), " ")
        .replace(" FHD", "", ignoreCase = true)
        .replace(" HD", "", ignoreCase = true)
        .replace(" SD", "", ignoreCase = true)
        .trim()
        .ifBlank { "Live TV" }

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
    }

private fun FullScreenContentKind.playbackKind(): PlaybackKind =
    when (this) {
        FullScreenContentKind.Live -> PlaybackKind.Live
        FullScreenContentKind.Movie -> PlaybackKind.Movie
        FullScreenContentKind.Episode -> PlaybackKind.Episode
    }

private fun String.isGeneratedTitleFor(kind: FullScreenContentKind, id: Int): Boolean {
    val normalized = trim()
    return when (kind) {
        FullScreenContentKind.Live -> normalized.equals("Chaine $id", ignoreCase = true) ||
            normalized.matches(Regex("(?i)^chaine\\s+\\d+$"))
        FullScreenContentKind.Movie -> normalized.equals("Film $id", ignoreCase = true)
        FullScreenContentKind.Episode -> normalized.equals("Episode $id", ignoreCase = true)
    }
}

private fun FullScreenContentKind.toPlayerContentType(): PlayerContentType =
    when (this) {
        FullScreenContentKind.Live -> PlayerContentType.LIVE_TV
        FullScreenContentKind.Movie -> PlayerContentType.MOVIE
        FullScreenContentKind.Episode -> PlayerContentType.SERIES
    }

private fun String.toBehaviorContentType(): String =
    when (this) {
        UserContentType.Live -> "LIVE_TV"
        UserContentType.Movie -> "MOVIE"
        UserContentType.Episode -> "EPISODE"
        UserContentType.Series -> "SERIES"
        else -> "UNKNOWN"
    }

private fun Long.validDurationMs(): Long =
    takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L

private fun Long.formatPlaybackTime(): String {
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
