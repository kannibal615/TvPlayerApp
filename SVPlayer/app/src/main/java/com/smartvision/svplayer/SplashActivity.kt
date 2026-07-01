package com.smartvision.svplayer

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.data.AppContainer
import com.smartvision.svplayer.sync.SyncFrequencyPolicy
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SplashActivity : ComponentActivity() {
    private var launched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.statusBarColor = AndroidColor.BLACK
        window.navigationBarColor = AndroidColor.BLACK
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        setContent {
            var statusLabel by remember { mutableStateOf("Initialisation en cours...") }
            var progress by remember { mutableFloatStateOf(MinimumProgressScale) }

            LaunchedEffect(Unit) {
                runStartupChecks { label, targetProgress ->
                    statusLabel = label
                    progress = targetProgress
                }
            }

            SplashVideoScreen(
                statusLabel = statusLabel,
                progress = progress,
            )
        }
    }

    @Composable
    private fun SplashVideoScreen(statusLabel: String, progress: Float) {
        var loadingVisible by remember { mutableStateOf(false) }
        val loadingAlpha by animateFloatAsState(
            targetValue = if (loadingVisible) 1f else 0f,
            animationSpec = tween(LoadingFadeMillis.toInt(), easing = FastOutSlowInEasing),
            label = "splashLoadingAlpha",
        )

        LaunchedEffect(Unit) {
            delay(LoadingRevealDelayMillis)
            loadingVisible = true
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            SplashVideoBackground(Modifier.fillMaxSize())
            SplashLoadingOverlay(
                statusLabel = statusLabel,
                progress = progress,
                loadingAlpha = loadingAlpha,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }

    @Composable
    private fun SplashVideoBackground(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val player = remember {
            ExoPlayer.Builder(context)
                .build()
                .apply {
                    volume = 0f
                    repeatMode = Player.REPEAT_MODE_ALL
                    playWhenReady = true
                    setMediaItem(MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(R.raw.splash_wave_animation)))
                    prepare()
                }
        }

        DisposableEffect(player) {
            onDispose {
                player.release()
            }
        }

        AndroidView(
            modifier = modifier.background(Color.Black),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(AndroidColor.BLACK)
                    setKeepContentOnPlayerReset(true)
                    isFocusable = false
                    isFocusableInTouchMode = false
                    isClickable = false
                    isLongClickable = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                    this.player = player
                }
            },
            update = { playerView ->
                if (playerView.player !== player) {
                    playerView.player = player
                }
            },
        )
    }

    @Composable
    private fun SplashLoadingOverlay(
        statusLabel: String,
        progress: Float,
        loadingAlpha: Float,
        modifier: Modifier = Modifier,
    ) {
        BoxWithConstraints(modifier = modifier) {
            val logoWidth = maxWidth * SplashLogoWidthRatio
            val logoHeight = logoWidth * LogoAspectRatio
            val progressWidth = maxWidth * SplashProgressWidthRatio
            val progressHeight = (maxHeight * SplashProgressHeightRatio).coerceAtLeast(MinimumProgressHeight)
            val progressTopMargin = maxHeight * SplashProgressTopMarginRatio
            val statusTopMargin = maxHeight * SplashStatusTopMarginRatio
            val logoAlpha by rememberInfiniteTransition(label = "splashLogoPulse").animateFloat(
                initialValue = LogoPulseMinAlpha,
                targetValue = LogoPulseMaxAlpha,
                animationSpec = infiniteRepeatable(
                    animation = tween(LogoPulseMillis.toInt(), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "splashLogoAlpha",
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.smartvision_logo_wide),
                    contentDescription = getString(R.string.app_name),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(logoWidth)
                        .height(logoHeight)
                        .alpha(logoAlpha),
                )
                Spacer(Modifier.height(progressTopMargin))
                SplashProgressBar(
                    progress = progress,
                    modifier = Modifier
                        .width(progressWidth)
                        .height(progressHeight)
                        .alpha(loadingAlpha),
                )
                Spacer(Modifier.height(statusTopMargin))
                Text(
                    text = statusLabel,
                    color = Color(0xFFDBEAFE).copy(alpha = 0.86f),
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(loadingAlpha),
                )
            }
        }
    }

    @Composable
    private fun SplashProgressBar(progress: Float, modifier: Modifier = Modifier) {
        val animatedProgress by animateFloatAsState(
            targetValue = progress.coerceIn(MinimumProgressScale, 1f),
            animationSpec = tween(ProgressStepMillis.toInt(), easing = FastOutSlowInEasing),
            label = "splashProgress",
        )
        val shape = RoundedCornerShape(percent = 50)

        Box(
            modifier = modifier
                .clip(shape)
                .background(Color(0x960D1D36)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(shape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1D76FF),
                                Color(0xFF1FDDFF),
                            ),
                        ),
                    ),
            )
        }
    }

    private suspend fun runStartupChecks(updateStatus: (String, Float) -> Unit) {
        val startedAt = SystemClock.elapsedRealtime()
        val container = (application as SVPlayerApplication).appContainer
        var totalSteps = StartupStepsWithSync
        suspend fun update(label: String, step: Int, total: Int = totalSteps) {
            updateStatus(label, startupProgress(step, total))
            delay(StatusStepPauseMillis)
        }

        runCatching {
            update("Initialisation en cours...", 1)
            update("Verification de la licence...", 2)
            update("Verification de l'activation...", 3)
            runCatching { container.activationRepository.checkStatus() }
            val source = container.accountManager.activePlaylistSource.value
            val connectionReady = if (source == PlaylistSource.Xtream) {
                update("Verification du serveur Xtream...", 4)
                container.xtreamConnectionManager.verifyQuick("splash").isConnected
            } else {
                update("Verification du lien M3U...", 4)
                container.accountManager.m3uUrl.value.isNotBlank()
            }
            val shouldSync = connectionReady && shouldRunStartupCatalogSync(container)
            val shouldSyncEpg = container.accountManager.epgUrl.value.isNotBlank()
            totalSteps = (if (shouldSync) StartupStepsWithSync else StartupStepsWithoutSync) + if (shouldSyncEpg) EpgStartupSteps else 0
            update(
                label = "Verification derniere synchronisation... ${if (shouldSync) "KO" else "OK"}",
                step = 5,
                total = totalSteps,
            )

            var step = 6
            if (shouldSync) {
                update(
                    if (source == PlaylistSource.M3u) "Synchronisation M3U en cours..." else "Synchronisation Xtream en cours...",
                    step++,
                    totalSteps,
                )
                container.xtreamRepository.clearCaches()
                container.catalogRepository.invalidateLocalCatalogCache()
                val syncResult = runCatching { container.synchronizeCatalog().getOrThrow() }
                update(
                    label = if (syncResult.isSuccess) {
                        if (source == PlaylistSource.M3u) "Chargement catalogue M3U termine." else "Chargement catalogue Xtream termine."
                    } else {
                        "Synchronisation catalogue indisponible."
                    },
                    step = step++,
                    total = totalSteps,
                )
            }

            if (shouldSyncEpg) {
                update("Synchronisation EPG en cours...", step++, totalSteps)
                val epgResult = runCatching { container.epgRepository.synchronize(container.accountManager.epgUrl.value).getOrThrow() }
                update(
                    if (epgResult.isSuccess) "Chargement EPG termine." else "Chargement EPG indisponible.",
                    step++,
                    totalSteps,
                )
            }

            update("Chargement des donnees (HOME)...", step++, totalSteps)
            preloadHomeData(container)
            update("Chargement des donnees (LIVE TV)...", step++, totalSteps)
            runCatching { container.catalogRepository.getLiveCatalogSnapshot() }
            update("Chargement des donnees (FILMS)...", step++, totalSteps)
            runCatching { container.catalogRepository.getMovieCatalogSnapshot() }
            update("Chargement des donnees (SERIES)...", step++, totalSteps)
            runCatching { container.catalogRepository.getSeriesCatalogSnapshot() }
            update("Demarrage en cours...", totalSteps, totalSteps)
        }
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        if (elapsed < MinimumSplashDurationMillis) {
            delay(MinimumSplashDurationMillis - elapsed)
        }
        launchHome()
    }

    private suspend fun preloadHomeData(container: AppContainer) = coroutineScope {
        val slides = async { runCatching { container.homeSlidesRepository.refresh() } }
        val progress = async { runCatching { container.userContentRepository.getRecentProgressSnapshot() } }
        slides.await()
        progress.await()
        Unit
    }

    private fun startupProgress(step: Int, total: Int): Float =
        (step.toFloat() / total.toFloat()).coerceIn(MinimumProgressScale, 1f)

    private suspend fun shouldRunStartupCatalogSync(container: AppContainer): Boolean {
        val source = container.accountManager.activePlaylistSource.value
        if (source == PlaylistSource.Xtream && !container.accountManager.current().isConfigured) return false
        if (source == PlaylistSource.M3u && container.accountManager.m3uUrl.value.isBlank()) return false
        val settings = container.settingsRepository.settings.first()
        val policy = SyncFrequencyPolicy.from(settings.syncFrequency)
        if (policy.runOnStartup) return true
        val repeatHours = policy.repeatHours ?: return false
        val lastSync = container.syncStateDao.get()?.lastSync ?: return true
        return System.currentTimeMillis() - lastSync >= TimeUnit.HOURS.toMillis(repeatHours)
    }

    private fun launchHome() {
        if (launched || isFinishing) return
        launched = true
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION),
        )
        overridePendingTransition(0, 0)
        finish()
    }

    private companion object {
        const val LogoAspectRatio = 248f / 980f
        const val SplashLogoWidthRatio = 0.54f
        const val SplashProgressWidthRatio = 0.36f
        const val SplashProgressHeightRatio = 0.008f
        const val SplashProgressTopMarginRatio = 0.018f
        const val SplashStatusTopMarginRatio = 0.014f
        val MinimumProgressHeight: Dp = 5.dp
        const val LogoPulseMinAlpha = 0.86f
        const val LogoPulseMaxAlpha = 1.0f
        const val LogoPulseMillis = 760L
        const val LoadingRevealDelayMillis = 820L
        const val LoadingFadeMillis = 260L
        const val MinimumSplashDurationMillis = 2_400L
        const val StatusStepPauseMillis = 80L
        const val ProgressStepMillis = 180L
        const val MinimumProgressScale = 0.03f
        const val StartupStepsWithoutSync = 10
        const val StartupStepsWithSync = 12
        const val EpgStartupSteps = 2
    }
}
