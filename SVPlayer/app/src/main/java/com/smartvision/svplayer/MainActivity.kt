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
import com.smartvision.svplayer.data.diagnostics.PerformanceDiagnosticRecorder
import com.smartvision.svplayer.startup.StartupCatalogWorkKind
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
        // PERF_DIAG: startup timing/memory marker, safe no-op outside releaseDiagnostic.
        PerformanceDiagnosticRecorder.recordMemory(
            sheet = PerformanceDiagnosticRecorder.SHEET_STARTUP_STEPS,
            event = "main_on_create",
            fields = mapOf("message" to "MainActivity launcher with system splash theme"),
        )
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
            // PERF_DIAG: separates first Compose frame from the real startup work.
            PerformanceDiagnosticRecorder.record(
                sheet = PerformanceDiagnosticRecorder.SHEET_STARTUP_STEPS,
                event = "first_compose_frame_ready",
                fields = mapOf("delayBeforeStartupChecksMs" to FirstFrameStartupDelayMillis),
            )
            delay(FirstFrameStartupDelayMillis)
            val containerStart = SystemClock.elapsedRealtime()
            val container = withContext(Dispatchers.Default) {
                (application as SVPlayerApplication).appContainer
            }
            // PERF_DIAG: AppContainer lazy init can explain blank/grey startup gaps.
            PerformanceDiagnosticRecorder.recordDuration(
                sheet = PerformanceDiagnosticRecorder.SHEET_STARTUP_STEPS,
                event = "app_container_ready",
                startedAtMs = containerStart,
            )
            val startupChecksStart = SystemClock.elapsedRealtime()
            runStartupChecks(
                container = container,
                startedAtMs = startupStartedAt,
                updateStatus = { state -> startupState = state },
            )
            PerformanceDiagnosticRecorder.recordDuration(
                sheet = PerformanceDiagnosticRecorder.SHEET_STARTUP_STEPS,
                event = "startup_checks_complete",
                startedAtMs = startupChecksStart,
            )
            setTheme(R.style.Theme_SVPlayer)
            window.setBackgroundDrawable(ColorDrawable(AndroidColor.rgb(2, 7, 20)))
            // PERF_DIAG: marks the exact handoff where the splash window background is replaced.
            PerformanceDiagnosticRecorder.record(
                sheet = PerformanceDiagnosticRecorder.SHEET_STARTUP_STEPS,
                event = "theme_switched_to_app",
                fields = mapOf("background" to "#020714"),
            )
            appContainer = container
            startupComplete = true
            Log.i(TAG_STARTUP, "startup complete: rendering AppNavigation")
            PerformanceDiagnosticRecorder.record(
                sheet = PerformanceDiagnosticRecorder.SHEET_STARTUP_STEPS,
                event = "rendering_app_navigation",
            )
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
                modifier = Modifier.fillMaxSize(),
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
            val progressTopOffset = maxHeight * SplashProgressTopRatio

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = progressTopOffset),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StartupProgressBar(
                    progress = state.progress,
                    modifier = Modifier
                        .padding(start = 30.dp)
                        .width(progressWidth)
                        .height(progressHeight),
                )
                Spacer(Modifier.height(statusTopMargin))
                Text(
                    text = state.statusLine(),
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
        startedAtMs: Long,
        updateStatus: (StartupProgressState) -> Unit,
    ) {
        var totalSteps = startupStepCount()
        suspend fun update(label: String, step: Int, total: Int = totalSteps) {
            Log.i(TAG_STARTUP, "startup status: $label")
            // PERF_DIAG: one row per visible splash status, with memory at that moment.
            PerformanceDiagnosticRecorder.recordMemory(
                sheet = PerformanceDiagnosticRecorder.SHEET_STARTUP_STEPS,
                event = "startup_status",
                fields = mapOf(
                    "label" to label,
                    "currentStep" to step,
                    "totalSteps" to total,
                    "progress" to startupProgress(step, total),
                ),
            )
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
            val activationStart = SystemClock.elapsedRealtime()
            runCatching { container.activationRepository.checkStatus() }
                .onSuccess {
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                        event = "activation_status_loaded",
                        startedAtMs = activationStart,
                    )
                }
                .onFailure { error ->
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_ERRORS,
                        event = "activation_status_failed",
                        startedAtMs = activationStart,
                        error = error,
                    )
                }
            val source = container.accountManager.activePlaylistSource.value
            PerformanceDiagnosticRecorder.record(
                sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                event = "active_playlist_source",
                fields = mapOf("source" to source.storageValue),
            )
            val connectionReady = if (source == PlaylistSource.Xtream) {
                update("Verification du serveur Xtream...", 4)
                val checkStart = SystemClock.elapsedRealtime()
                container.xtreamConnectionManager.verifyQuick("splash").isConnected.also { connected ->
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                        event = "xtream_quick_check",
                        startedAtMs = checkStart,
                        fields = mapOf("connected" to connected),
                    )
                }
            } else {
                update("Verification du lien M3U...", 4)
                container.accountManager.m3uUrl.value.isNotBlank().also { configured ->
                    PerformanceDiagnosticRecorder.record(
                        sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                        event = "m3u_link_check",
                        fields = mapOf("configured" to configured),
                    )
                }
            }
            val shouldSync = connectionReady && shouldRunStartupCatalogSync(container)
            val catalogCounts = runCatching { container.catalogRepository.getCatalogContentCounts() }.getOrNull()
            val hasLocalCatalog = when (source) {
                PlaylistSource.Xtream -> catalogCounts?.hasAnyContent == true
                PlaylistSource.M3u -> (catalogCounts?.live ?: 0) > 0
            }
            val startupWork = when {
                shouldSync -> StartupCatalogWorkKind.Synchronize
                connectionReady && hasLocalCatalog -> StartupCatalogWorkKind.LoadLocal
                else -> StartupCatalogWorkKind.None
            }
            if (startupWork == StartupCatalogWorkKind.None) {
                container.clearStartupCatalogWork(container.startupCatalogWork.value.requestedAtMs)
            } else {
                container.requestStartupCatalogWork(startupWork)
            }
            PerformanceDiagnosticRecorder.record(
                sheet = PerformanceDiagnosticRecorder.SHEET_STARTUP_STEPS,
                event = "startup_sync_decision",
                fields = mapOf(
                    "source" to source.storageValue,
                    "connectionReady" to connectionReady,
                    "shouldSync" to shouldSync,
                    "startupWork" to startupWork.name,
                    "localLive" to (catalogCounts?.live ?: 0),
                    "localMovies" to (catalogCounts?.movies ?: 0),
                    "localSeries" to (catalogCounts?.series ?: 0),
                    "totalSteps" to totalSteps,
                ),
            )
            update(
                label = "Verification derniere synchronisation... ${if (shouldSync) "KO" else "OK"}",
                step = 5,
                total = totalSteps,
            )

            var step = 6
            update("Chargement Home...", step++, totalSteps)
            val homePreloadStart = SystemClock.elapsedRealtime()
            runCatching { preloadHomeContent(container) }
                .onSuccess {
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                        event = "startup_home_preloaded",
                        startedAtMs = homePreloadStart,
                    )
                }
                .onFailure { error ->
                    PerformanceDiagnosticRecorder.recordDuration(
                        sheet = PerformanceDiagnosticRecorder.SHEET_ERRORS,
                        event = "startup_home_preload_failed",
                        startedAtMs = homePreloadStart,
                        error = error,
                    )
                }
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

    private fun startupProgress(step: Int, total: Int): Float =
        (step.toFloat() / total.toFloat()).coerceIn(MinimumProgressScale, 1f)

    private fun startupStepCount(): Int = 7

    private suspend fun preloadHomeContent(container: AppContainer) {
        val progressStart = SystemClock.elapsedRealtime()
        val recentProgress = container.userContentRepository.getRecentProgressSnapshot(limit = 10)
        PerformanceDiagnosticRecorder.recordDuration(
            sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
            event = "startup_home_recent_progress",
            startedAtMs = progressStart,
            fields = mapOf("items" to recentProgress.size, "limit" to 10),
        )
        val slidesStart = SystemClock.elapsedRealtime()
        runCatching { container.homeSlidesRepository.refresh() }
            .onSuccess { slides ->
                PerformanceDiagnosticRecorder.recordDuration(
                    sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                    event = "startup_home_slides",
                    startedAtMs = slidesStart,
                    fields = mapOf("items" to slides.size),
                )
            }
            .onFailure { error ->
                PerformanceDiagnosticRecorder.recordDuration(
                    sheet = PerformanceDiagnosticRecorder.SHEET_ERRORS,
                    event = "startup_home_slides_failed",
                    startedAtMs = slidesStart,
                    error = error,
                )
            }
        if (container.accountManager.activePlaylistSource.value == PlaylistSource.Xtream &&
            container.accountManager.accounts.value.isNotEmpty()
        ) {
            // PERF_FIX: do not block the splash on Xtream detail/backdrop calls.
            // Home consumes any in-memory trend cache immediately, then refreshes missing details after first render.
            val snapshot = container.homeContentRepository.getLastCachedTrendingSnapshot()
            PerformanceDiagnosticRecorder.record(
                sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                event = "startup_home_trending_deferred",
                fields = mapOf(
                    "cachedMovies" to snapshot?.movies.orEmpty().size,
                    "cachedSeries" to snapshot?.series.orEmpty().size,
                    "reason" to "deferred_after_home_render",
                ),
            )
        } else {
            container.homeContentRepository.cacheEmptyTrending()
            PerformanceDiagnosticRecorder.record(
                sheet = PerformanceDiagnosticRecorder.SHEET_LOADED_DATA,
                event = "startup_home_trending_empty",
                fields = mapOf("reason" to "no_xtream_source_or_account"),
            )
        }
    }

    private suspend fun shouldRunStartupCatalogSync(container: AppContainer): Boolean {
        val source = container.accountManager.activePlaylistSource.value
        if (source == PlaylistSource.Xtream && !container.accountManager.current().isConfigured) return false
        if (source == PlaylistSource.M3u && container.accountManager.m3uUrl.value.isBlank()) return false
        val settings = container.settingsRepository.settings.first()
        val policy = SyncFrequencyPolicy.from(settings.syncFrequency)
        PerformanceDiagnosticRecorder.record(
            sheet = PerformanceDiagnosticRecorder.SHEET_STARTUP_STEPS,
            event = "sync_policy_loaded",
            fields = mapOf(
                "syncFrequency" to settings.syncFrequency,
                "runOnStartup" to policy.runOnStartup,
                "repeatHours" to (policy.repeatHours ?: ""),
            ),
        )
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
        private const val SplashProgressWidthRatio = 0.32f
        private const val SplashProgressHeightRatio = 0.008f
        private const val SplashProgressTopRatio = 0.50f
        private const val SplashStatusTopMarginRatio = 0.014f
        private val MinimumProgressHeight: Dp = 5.dp
        private const val MinimumSplashDurationMillis = 2_400L
        private const val StatusStepPauseMillis = 80L
        private const val ProgressStepMillis = 180L
        private const val MinimumProgressScale = 0.03f
        private const val FirstFrameStartupDelayMillis = 60L
        private const val StartupTickerMillis = 1_000L
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
    fun statusLine(): String {
        val percent = (progress * 100f).toInt().coerceIn(0, 100)
        return "$percent%  $label"
    }

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
