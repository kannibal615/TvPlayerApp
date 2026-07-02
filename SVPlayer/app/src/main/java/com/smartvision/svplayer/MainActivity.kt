package com.smartvision.svplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.data.AppContainer
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.domain.model.SyncStatus
import com.smartvision.svplayer.sync.SyncFrequencyPolicy
import com.smartvision.svplayer.ui.navigation.AppNavigation
import com.smartvision.svplayer.ui.theme.SmartVisionTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
        val startupStartedAt = remember { SystemClock.elapsedRealtime() }
        var nowMs by remember { mutableLongStateOf(startupStartedAt) }
        var startupState by remember {
            mutableStateOf(StartupProgressState(startedAtMs = startupStartedAt))
        }

        LaunchedEffect(startupComplete) {
            while (!startupComplete) {
                nowMs = SystemClock.elapsedRealtime()
                delay(StartupTickerMillis)
            }
        }

        LaunchedEffect(Unit) {
            withFrameNanos { }
            delay(FirstFrameStartupDelayMillis)
            val container = withContext(Dispatchers.Default) {
                (application as SVPlayerApplication).appContainer
            }
            runStartupChecks(
                container = container,
                startedAtMs = startupStartedAt,
                updateStatus = { state -> startupState = state },
            )
            setTheme(R.style.Theme_SVPlayer)
            window.setBackgroundDrawable(ColorDrawable(AndroidColor.rgb(2, 7, 20)))
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
                state = startupState,
                nowMs = nowMs,
            )
        }
    }

    @Composable
    private fun StartupSplashScreen(state: StartupProgressState, nowMs: Long) {
        Box(modifier = Modifier.fillMaxSize()) {
            StartupLoadingOverlay(
                state = state,
                nowMs = nowMs,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }

    @Composable
    private fun StartupLoadingOverlay(
        state: StartupProgressState,
        nowMs: Long,
        modifier: Modifier = Modifier,
    ) {
        BoxWithConstraints(modifier = modifier) {
            val progressWidth = maxWidth * SplashProgressWidthRatio
            val progressHeight = (maxHeight * SplashProgressHeightRatio).coerceAtLeast(MinimumProgressHeight)
            val statusTopMargin = maxHeight * SplashStatusTopMarginRatio
            val progressTopOffset = maxHeight / 2 + SystemSplashLogoHeight / 2 + (maxHeight * SplashProgressTopMarginRatio)
            val elapsedMs = (nowMs - state.startedAtMs).coerceAtLeast(0L)
            val etaText = estimateStartupEta(elapsedMs, state.progress)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = progressTopOffset),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StartupProgressBar(
                    progress = state.progress,
                    modifier = Modifier
                        .width(progressWidth)
                        .height(progressHeight),
                )
                Spacer(Modifier.height(statusTopMargin))
                Text(
                    text = state.label,
                    color = Color(0xFFDBEAFE).copy(alpha = 0.86f),
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(DiagnosticLineTopMargin))
                Text(
                    text = state.summaryLine(),
                    color = Color(0xFFB7D7FF).copy(alpha = 0.82f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Temps: ${formatDuration(elapsedMs)} | Restant: $etaText",
                    color = Color(0xFFB7D7FF).copy(alpha = 0.76f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                )
                state.sectionsLine()?.let { details ->
                    Text(
                        text = details,
                        color = Color(0xFF9FC8FF).copy(alpha = 0.72f),
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
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
        startedAtMs: Long,
        updateStatus: (StartupProgressState) -> Unit,
    ) {
        var totalSteps = startupStepCount(PlaylistSource.Xtream, shouldSync = true)
        suspend fun update(label: String, step: Int, total: Int = totalSteps) {
            Log.i(TAG_STARTUP, "startup status: $label")
            updateStatus(
                StartupProgressState(
                    label = label,
                    progress = startupProgress(step, total),
                    currentStep = step,
                    totalSteps = total,
                    startedAtMs = startedAtMs,
                ),
            )
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
                val syncStep = step - 1
                val syncResult = collectCatalogSyncStartupProgress(
                    container = container,
                    startedAtMs = startedAtMs,
                    step = syncStep,
                    totalSteps = totalSteps,
                    fallbackLabel = if (source == PlaylistSource.M3u) {
                        "Synchronisation M3U en cours..."
                    } else {
                        "Synchronisation Xtream en cours..."
                    },
                    updateStatus = updateStatus,
                )
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
            updateStatus(
                StartupProgressState(
                    label = "Demarrage en cours...",
                    progress = 1f,
                    currentStep = totalSteps,
                    totalSteps = totalSteps,
                    startedAtMs = startedAtMs,
                ),
            )
        }
        val elapsed = SystemClock.elapsedRealtime() - startedAtMs
        if (elapsed < MinimumSplashDurationMillis) {
            delay(MinimumSplashDurationMillis - elapsed)
        }
    }

    private suspend fun collectCatalogSyncStartupProgress(
        container: AppContainer,
        startedAtMs: Long,
        step: Int,
        totalSteps: Int,
        fallbackLabel: String,
        updateStatus: (StartupProgressState) -> Unit,
    ): Result<Unit> = coroutineScope {
        val syncJob = async { runCatching { container.synchronizeCatalog().getOrThrow() } }
        val statusJob = launch {
            container.catalogRepository.syncStatus.collect { status ->
                val state = status.toStartupProgressState(
                    fallbackLabel = fallbackLabel,
                    startedAtMs = startedAtMs,
                    step = step,
                    totalSteps = totalSteps,
                ) ?: return@collect
                updateStatus(state)
            }
        }
        val result = syncJob.await()
        statusJob.cancelAndJoin()
        result
    }

    private fun startupProgress(step: Int, total: Int): Float =
        (step.toFloat() / total.toFloat()).coerceIn(MinimumProgressScale, 1f)

    private fun startupProgressForStep(step: Int, total: Int, stepFraction: Float): Float =
        (((step - 1).toFloat() + stepFraction.coerceIn(0f, 1f)) / total.toFloat())
            .coerceIn(MinimumProgressScale, 1f)

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

    private fun SyncStatus.toStartupProgressState(
        fallbackLabel: String,
        startedAtMs: Long,
        step: Int,
        totalSteps: Int,
    ): StartupProgressState? = when (this) {
        SyncStatus.Idle -> null
        is SyncStatus.Running -> StartupProgressState(
            label = message.ifBlank { fallbackLabel },
            progress = startupProgressForStep(step, totalSteps, percent.toFloat() / 100f),
            currentStep = step,
            totalSteps = totalSteps,
            completedItems = completedItems.takeIf { totalItems > 0 },
            totalItems = totalItems.takeIf { it > 0 },
            sections = catalogProgress.toStartupSections(),
            startedAtMs = startedAtMs,
        )
        is SyncStatus.Success -> StartupProgressState(
            label = message.ifBlank { fallbackLabel },
            progress = startupProgressForStep(step, totalSteps, 1f),
            currentStep = step,
            totalSteps = totalSteps,
            sections = catalogProgress.toStartupSections(),
            startedAtMs = startedAtMs,
        )
        is SyncStatus.Error -> StartupProgressState(
            label = message.ifBlank { fallbackLabel },
            progress = startupProgressForStep(step, totalSteps, 1f),
            currentStep = step,
            totalSteps = totalSteps,
            sections = catalogProgress.toStartupSections(),
            startedAtMs = startedAtMs,
        )
    }

    private fun SyncStatus.CatalogProgress.toStartupSections(): List<StartupSectionProgress> =
        listOf(
            live.toStartupSection("Live"),
            movies.toStartupSection("Films"),
            series.toStartupSection("Series"),
        )

    private fun SyncStatus.SyncSectionProgress.toStartupSection(label: String): StartupSectionProgress =
        StartupSectionProgress(
            label = label,
            currentItems = currentItems,
            previousItems = previousItems,
            percent = percent,
            completed = completed,
        )

    companion object {
        private const val TAG = "SmartVisionFocus"
        private const val TAG_STARTUP = "SVStartup"
        const val ACTION_SHOW_XTREAM_CONNECTION_ALERT = "com.smartvision.svplayer.SHOW_XTREAM_CONNECTION_ALERT"
        private const val REQUEST_NOTIFICATIONS = 7041
        private const val SplashProgressWidthRatio = 0.36f
        private const val SplashProgressHeightRatio = 0.008f
        private const val SplashProgressTopMarginRatio = 0.018f
        private const val SplashStatusTopMarginRatio = 0.014f
        private val SystemSplashLogoHeight: Dp = 142.dp
        private val MinimumProgressHeight: Dp = 5.dp
        private val DiagnosticLineTopMargin: Dp = 6.dp
        private const val MinimumSplashDurationMillis = 2_400L
        private const val StatusStepPauseMillis = 80L
        private const val ProgressStepMillis = 180L
        private const val MinimumProgressScale = 0.03f
        private const val FirstFrameStartupDelayMillis = 60L
        private const val StartupTickerMillis = 1_000L
        private const val StartupLivePageLimit = 96
        private const val StartupMoviePageLimit = 72
        private const val StartupSeriesPageLimit = 72
    }
}

private data class StartupProgressState(
    val label: String = "Initialisation en cours...",
    val progress: Float = 0.03f,
    val currentStep: Int = 1,
    val totalSteps: Int = 1,
    val completedItems: Int? = null,
    val totalItems: Int? = null,
    val sections: List<StartupSectionProgress> = emptyList(),
    val startedAtMs: Long,
) {
    fun summaryLine(): String {
        val percent = (progress * 100f).toInt().coerceIn(0, 100)
        val processed = if (completedItems != null && totalItems != null) {
            "$completedItems / $totalItems elements"
        } else {
            "-- elements"
        }
        val remaining = if (completedItems != null && totalItems != null) {
            (totalItems - completedItems).coerceAtLeast(0).toString()
        } else {
            "--"
        }
        return "$percent% | Etape $currentStep / $totalSteps | Traite: $processed | Restant: $remaining"
    }

    fun sectionsLine(): String? =
        sections
            .filter { it.currentItems > 0 || it.previousItems > 0 || it.completed }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" | ") { it.displayText() }
}

private data class StartupSectionProgress(
    val label: String,
    val currentItems: Int,
    val previousItems: Int,
    val percent: Int,
    val completed: Boolean,
) {
    fun displayText(): String {
        val previous = previousItems.takeIf { it > 0 }?.toString() ?: "--"
        val remaining = if (previousItems > 0) {
            (previousItems - currentItems).coerceAtLeast(0).toString()
        } else {
            "--"
        }
        val marker = if (completed) "OK" else "$percent%"
        return "$label $marker: $currentItems/$previous reste $remaining"
    }
}

private fun estimateStartupEta(elapsedMs: Long, progress: Float): String {
    val safeProgress = progress.coerceIn(0f, 1f)
    if (safeProgress >= 0.995f) return "00:00"
    if (safeProgress <= 0.05f) return "Calcul..."
    val remainingMs = ((elapsedMs / safeProgress) - elapsedMs).toLong().coerceAtLeast(0L)
    return formatDuration(remainingMs)
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}
