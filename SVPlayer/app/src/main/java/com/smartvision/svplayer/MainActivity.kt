package com.smartvision.svplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.View
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.data.AppContainer
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.sync.SyncFrequencyPolicy
import com.smartvision.svplayer.ui.navigation.AppNavigation
import com.smartvision.svplayer.ui.theme.SmartVisionTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        Log.i(TAG_STARTUP, "onCreate: MainActivity launcher with system splash theme")
        requestNotificationPermissionIfNeeded()
        handleIntent(intent)
        setContent {
            SmartVisionTheme {
                StartupGate()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        try {
            super.dispatchKeyEvent(event)
        } catch (error: IllegalStateException) {
            if (error.message?.contains("FocusRequester is not initialized") == true) {
                Log.w(TAG, "Focus search ignored uninitialized requester for keyCode=${event.keyCode}")
                true
            } else {
                throw error
            }
        }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_SHOW_XTREAM_CONNECTION_ALERT) {
            (application as SVPlayerApplication).appContainer.xtreamConnectionManager.requestAlert()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
    }

    @Composable
    private fun StartupGate() {
        var appContainer by remember { mutableStateOf<AppContainer?>(null) }
        var startupComplete by remember { mutableStateOf(false) }
        var statusLabel by remember { mutableStateOf("Initialisation en cours...") }
        var progress by remember { mutableFloatStateOf(MinimumProgressScale) }

        LaunchedEffect(Unit) {
            withFrameNanos { }
            setTheme(R.style.Theme_SVPlayer)
            delay(FirstFrameStartupDelayMillis)
            val container = withContext(Dispatchers.Default) {
                (application as SVPlayerApplication).appContainer
            }
            runStartupChecks(
                container = container,
                updateStatus = { label, targetProgress ->
                    statusLabel = label
                    progress = targetProgress
                },
            )
            appContainer = container
            startupComplete = true
            Log.i(TAG_STARTUP, "startup complete: rendering AppNavigation")
        }

        val readyContainer = appContainer
        if (startupComplete && readyContainer != null) {
            CompositionLocalProvider(LocalAppContainer provides readyContainer) {
                AppNavigation()
            }
        } else {
            StartupSplashScreen(
                statusLabel = statusLabel,
                progress = progress,
            )
        }
    }

    @Composable
    private fun StartupSplashScreen(statusLabel: String, progress: Float) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            Image(
                painter = painterResource(R.drawable.smartvision_splash_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            StartupLoadingOverlay(
                statusLabel = statusLabel,
                progress = progress,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }

    @Composable
    private fun StartupLoadingOverlay(
        statusLabel: String,
        progress: Float,
        modifier: Modifier = Modifier,
    ) {
        BoxWithConstraints(modifier = modifier) {
            val logoWidth = maxWidth * SplashLogoWidthRatio
            val logoHeight = logoWidth * LogoAspectRatio
            val progressWidth = maxWidth * SplashProgressWidthRatio
            val progressHeight = (maxHeight * SplashProgressHeightRatio).coerceAtLeast(MinimumProgressHeight)
            val progressTopMargin = maxHeight * SplashProgressTopMarginRatio
            val statusTopMargin = maxHeight * SplashStatusTopMarginRatio
            val logoAlpha by rememberInfiniteTransition(label = "startupLogoPulse").animateFloat(
                initialValue = LogoPulseMinAlpha,
                targetValue = LogoPulseMaxAlpha,
                animationSpec = infiniteRepeatable(
                    animation = tween(LogoPulseMillis.toInt(), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "startupLogoAlpha",
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
                StartupProgressBar(
                    progress = progress,
                    modifier = Modifier
                        .width(progressWidth)
                        .height(progressHeight),
                )
                Spacer(Modifier.height(statusTopMargin))
                Text(
                    text = statusLabel,
                    color = Color(0xFFDBEAFE).copy(alpha = 0.86f),
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    @Composable
    private fun StartupProgressBar(progress: Float, modifier: Modifier = Modifier) {
        val animatedProgress by animateFloatAsState(
            targetValue = progress.coerceIn(MinimumProgressScale, 1f),
            animationSpec = tween(ProgressStepMillis.toInt(), easing = FastOutSlowInEasing),
            label = "startupProgress",
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

    private suspend fun runStartupChecks(
        container: AppContainer,
        updateStatus: (String, Float) -> Unit,
    ) {
        val startedAt = SystemClock.elapsedRealtime()
        var totalSteps = startupStepCount(PlaylistSource.Xtream, shouldSync = true)
        suspend fun update(label: String, step: Int, total: Int = totalSteps) {
            Log.i(TAG_STARTUP, "startup status: $label")
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
            totalSteps = startupStepCount(source, shouldSync)
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

            update("Chargement des categories (LIVE TV)...", step++, totalSteps)
            runCatching {
                container.catalogRepository.getLiveCategoriesSnapshot()
                container.catalogRepository.getAllLiveChannelsPage(offset = 0, limit = StartupLivePageLimit)
            }
            if (source == PlaylistSource.Xtream) {
                update("Chargement des categories (FILMS)...", step++, totalSteps)
                runCatching {
                    container.catalogRepository.getMovieCategoriesSnapshot()
                    container.catalogRepository.getAllMoviesPage(offset = 0, limit = StartupMoviePageLimit)
                }
                update("Chargement des categories (SERIES)...", step++, totalSteps)
                runCatching {
                    container.catalogRepository.getSeriesCategoriesSnapshot()
                    container.catalogRepository.getAllSeriesPage(offset = 0, limit = StartupSeriesPageLimit)
                }
            }
            update("Chargement Home...", step++, totalSteps)
            runCatching { preloadHomeContent(container) }
            update("Demarrage en cours...", totalSteps, totalSteps)
        }.onFailure { error ->
            Log.w(TAG_STARTUP, "startup checks failed: ${error.javaClass.simpleName}", error)
            updateStatus("Demarrage en cours...", 1f)
        }
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        if (elapsed < MinimumSplashDurationMillis) {
            delay(MinimumSplashDurationMillis - elapsed)
        }
    }

    private fun startupProgress(step: Int, total: Int): Float =
        (step.toFloat() / total.toFloat()).coerceIn(MinimumProgressScale, 1f)

    private fun startupStepCount(source: PlaylistSource, shouldSync: Boolean): Int {
        var total = 5
        if (shouldSync) total += 2
        total += 1
        if (source == PlaylistSource.Xtream) total += 2
        total += 1
        return total + 1
    }

    private suspend fun preloadHomeContent(container: AppContainer) {
        container.userContentRepository.getRecentProgressSnapshot(limit = 10)
        runCatching { container.homeSlidesRepository.refresh() }
        if (container.accountManager.activePlaylistSource.value == PlaylistSource.Xtream &&
            container.accountManager.accounts.value.isNotEmpty()
        ) {
            container.homeContentRepository.preloadTrending()
        } else {
            container.homeContentRepository.cacheEmptyTrending()
        }
    }

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

    companion object {
        private const val TAG = "SmartVisionFocus"
        private const val TAG_STARTUP = "SVStartup"
        const val ACTION_SHOW_XTREAM_CONNECTION_ALERT = "com.smartvision.svplayer.SHOW_XTREAM_CONNECTION_ALERT"
        private const val REQUEST_NOTIFICATIONS = 7041
        private const val LogoAspectRatio = 248f / 980f
        private const val SplashLogoWidthRatio = 0.54f
        private const val SplashProgressWidthRatio = 0.36f
        private const val SplashProgressHeightRatio = 0.008f
        private const val SplashProgressTopMarginRatio = 0.018f
        private const val SplashStatusTopMarginRatio = 0.014f
        private val MinimumProgressHeight: Dp = 5.dp
        private const val LogoPulseMinAlpha = 0.86f
        private const val LogoPulseMaxAlpha = 1.0f
        private const val LogoPulseMillis = 760L
        private const val MinimumSplashDurationMillis = 2_400L
        private const val StatusStepPauseMillis = 80L
        private const val ProgressStepMillis = 180L
        private const val MinimumProgressScale = 0.03f
        private const val FirstFrameStartupDelayMillis = 60L
        private const val StartupLivePageLimit = 96
        private const val StartupMoviePageLimit = 72
        private const val StartupSeriesPageLimit = 72
    }
}
