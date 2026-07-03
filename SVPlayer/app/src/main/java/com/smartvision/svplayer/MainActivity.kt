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
import com.smartvision.svplayer.core.data.AppContainer
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.data.diagnostics.PerformanceDiagnosticRecorder
import com.smartvision.svplayer.ui.navigation.AppNavigation
import com.smartvision.svplayer.ui.theme.SmartVisionTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

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
            update("Verification de l'activation locale...", 2)
            val localActivationStart = SystemClock.elapsedRealtime()
            val localActivation = runCatching { container.activationRepository.localState.first() }.getOrNull()
            container.cacheStartupActivationState(localActivation)
            PerformanceDiagnosticRecorder.record(
                sheet = PerformanceDiagnosticRecorder.SHEET_STARTUP_STEPS,
                event = "startup_local_activation_loaded",
                fields = mapOf(
                    "durationMs" to (SystemClock.elapsedRealtime() - localActivationStart),
                    "activated" to (localActivation?.activated ?: false),
                    "hasDeviceId" to !localActivation?.deviceId.isNullOrBlank(),
                ),
            )
            update("Preparation de l'accueil...", 3)
            container.clearStartupCatalogWork(container.startupCatalogWork.value.requestedAtMs)
            update(
                label = "Demarrage en cours...",
                step = totalSteps,
                total = totalSteps,
            )
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

    private fun startupStepCount(): Int = 3

    companion object {
        private const val TAG = "SmartVisionFocus"
        private const val TAG_STARTUP = "SVStartup"
        const val ACTION_SHOW_XTREAM_CONNECTION_ALERT = "com.smartvision.svplayer.SHOW_XTREAM_CONNECTION_ALERT"
        private const val REQUEST_NOTIFICATIONS = 7041
        private const val SplashProgressWidthRatio = 0.30f
        private const val SplashProgressHeightRatio = 0.010f
        private const val SplashProgressTopRatio = 0.43f
        private const val SplashStatusTopMarginRatio = 0.014f
        private val MinimumProgressHeight: Dp = 5.dp
        private const val MinimumSplashDurationMillis = 900L
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
