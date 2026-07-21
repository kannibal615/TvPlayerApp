package com.smartvision.svplayer

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.smartvision.svplayer.core.data.AppContainer
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.data.diagnostics.PerformanceDiagnosticRecorder
import com.smartvision.svplayer.ui.i18n.smartVisionStrings
import com.smartvision.svplayer.ui.navigation.AppNavigation
import com.smartvision.svplayer.ui.navigation.RemoteSettingsNavigation
import com.smartvision.svplayer.ui.startup.StartupExperience
import com.smartvision.svplayer.ui.startup.StartupMinimumLogoOnlyMillis
import com.smartvision.svplayer.ui.startup.StartupProgressSnapshot
import com.smartvision.svplayer.ui.startup.StartupStage
import com.smartvision.svplayer.ui.startup.StartupVisualPhase
import com.smartvision.svplayer.ui.startup.shouldRevealStartupLoading
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

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.isSettingsShortcut()) {
            Log.d(
                TAG_REMOTE_KEYS,
                "keyCode=${event.keyCode} key=${KeyEvent.keyCodeToString(event.keyCode)} " +
                    "action=${event.action} deviceId=${event.deviceId}",
            )
            if (event.action == KeyEvent.ACTION_UP) {
                RemoteSettingsNavigation.requestOpenSettings()
            }
            return true
        }
        return try {
            super.dispatchKeyEvent(event)
        } catch (error: IllegalStateException) {
            if (error.message?.contains("FocusRequester is not initialized") == true) {
                Log.w(TAG, "Focus search ignored uninitialized requester for keyCode=${event.keyCode}")
                true
            } else {
                throw error
            }
        }
    }

    private fun KeyEvent.isSettingsShortcut(): Boolean =
        keyCode == KeyEvent.KEYCODE_SETTINGS ||
            keyCode == KeyEvent.KEYCODE_MENU ||
            keyCode == KeyEvent.KEYCODE_MEDIA_TOP_MENU

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
        var startupLanguage by remember { mutableStateOf("English") }
        var visualPhase by remember { mutableStateOf(StartupVisualPhase.LogoOnly) }
        var startupProgress by remember { mutableStateOf(StartupProgressSnapshot()) }
        var initialSurfaceReady by remember { mutableStateOf(false) }
        val strings = smartVisionStrings(startupLanguage)

        LaunchedEffect(startupComplete) {
            if (!startupComplete) {
                while (!startupComplete) {
                    val elapsed = SystemClock.elapsedRealtime() - startupStartedAt
                    if (shouldRevealStartupLoading(elapsed, startupComplete, startupProgress)) {
                        visualPhase = StartupVisualPhase.Loading
                        break
                    }
                    delay(50)
                }
                if (
                    !startupComplete &&
                    shouldRevealStartupLoading(
                        elapsedMillis = SystemClock.elapsedRealtime() - startupStartedAt,
                        startupComplete = startupComplete,
                        progress = startupProgress,
                    )
                ) {
                    visualPhase = StartupVisualPhase.Loading
                }
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
            startupLanguage = runCatching {
                container.settingsRepository.settings.first().language
            }.getOrDefault("English")
            val startupChecksStart = SystemClock.elapsedRealtime()
            runStartupChecks(
                container = container,
                updateStatus = { state -> startupProgress = state },
            )
            PerformanceDiagnosticRecorder.recordDuration(
                sheet = PerformanceDiagnosticRecorder.SHEET_STARTUP_STEPS,
                event = "startup_checks_complete",
                startedAtMs = startupChecksStart,
            )
            appContainer = container
            val surfacePreloadStartedAt = SystemClock.elapsedRealtime()
            while (
                !initialSurfaceReady &&
                SystemClock.elapsedRealtime() - surfacePreloadStartedAt < InitialSurfacePreloadTimeoutMillis
            ) {
                delay(16)
            }
            Log.i(
                TAG_STARTUP,
                "initial surface preloaded: ready=$initialSurfaceReady durationMs=${SystemClock.elapsedRealtime() - surfacePreloadStartedAt}",
            )
            PerformanceDiagnosticRecorder.recordDuration(
                sheet = PerformanceDiagnosticRecorder.SHEET_STARTUP_STEPS,
                event = "initial_surface_preloaded",
                startedAtMs = surfacePreloadStartedAt,
                fields = mapOf("ready" to initialSurfaceReady),
            )
            startupProgress = StartupProgressSnapshot(
                stage = StartupStage.Starting,
                completedSteps = startupStepCount(),
                totalSteps = startupStepCount(),
            )
            if (visualPhase == StartupVisualPhase.Loading) {
                delay(CompletedProgressHoldMillis)
            }
            val elapsed = SystemClock.elapsedRealtime() - startupStartedAt
            if (elapsed < StartupMinimumLogoOnlyMillis) {
                delay(StartupMinimumLogoOnlyMillis - elapsed)
            }
            visualPhase = StartupVisualPhase.TransitionOut
            setTheme(R.style.Theme_SVPlayer)
            window.setBackgroundDrawable(ColorDrawable(AndroidColor.rgb(2, 7, 20)))
            // PERF_DIAG: marks the exact handoff where the splash window background is replaced.
            PerformanceDiagnosticRecorder.record(
                sheet = PerformanceDiagnosticRecorder.SHEET_STARTUP_STEPS,
                event = "theme_switched_to_app",
                fields = mapOf("background" to "#020714"),
            )
            startupComplete = true
            Log.i(TAG_STARTUP, "startup complete: rendering AppNavigation")
            PerformanceDiagnosticRecorder.record(
                sheet = PerformanceDiagnosticRecorder.SHEET_STARTUP_STEPS,
                event = "rendering_app_navigation",
            )
        }

        val readyContainer = appContainer
        if (readyContainer != null) {
            CompositionLocalProvider(LocalAppContainer provides readyContainer) {
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppOpaqueBackground),
                        onInitialSurfaceReady = { initialSurfaceReady = true },
                    )
                    if (!startupComplete) {
                        StartupExperience(
                            phase = visualPhase,
                            progress = startupProgress,
                            strings = strings,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        } else {
            StartupExperience(
                phase = visualPhase,
                progress = startupProgress,
                strings = strings,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private suspend fun runStartupChecks(
        container: AppContainer,
        updateStatus: (StartupProgressSnapshot) -> Unit,
    ) {
        val totalSteps = startupStepCount()
        fun update(stage: StartupStage, completedSteps: Int) {
            Log.i(TAG_STARTUP, "startup stage: $stage")
            val progress = StartupProgressSnapshot(
                stage = stage,
                completedSteps = completedSteps,
                totalSteps = totalSteps,
            )
            // PERF_DIAG: one row per visible splash status, with memory at that moment.
            PerformanceDiagnosticRecorder.recordMemory(
                sheet = PerformanceDiagnosticRecorder.SHEET_STARTUP_STEPS,
                event = "startup_status",
                fields = mapOf(
                    "stage" to stage.name,
                    "completedSteps" to completedSteps,
                    "totalSteps" to totalSteps,
                    "progress" to progress.progress,
                ),
            )
            updateStatus(progress)
        }

        runCatching {
            update(StartupStage.Initializing, 0)
            update(StartupStage.CheckingActivation, 1)
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
            update(StartupStage.PreparingHome, 2)
            container.clearStartupCatalogWork(container.startupCatalogWork.value.requestedAtMs)
            update(StartupStage.PreparingHome, totalSteps - 1)
        }.onFailure { error ->
            Log.w(TAG_STARTUP, "startup checks failed: ${error.javaClass.simpleName}", error)
            updateStatus(StartupProgressSnapshot(StartupStage.PreparingHome, totalSteps - 1, totalSteps))
        }
    }

    private fun startupStepCount(): Int = 4

    companion object {
        private const val TAG = "SmartVisionFocus"
        private const val TAG_STARTUP = "SVStartup"
        private const val TAG_REMOTE_KEYS = "SVRemoteKeys"
        const val ACTION_SHOW_XTREAM_CONNECTION_ALERT = "com.smartvision.svplayer.SHOW_XTREAM_CONNECTION_ALERT"
        private const val REQUEST_NOTIFICATIONS = 7041
        private const val FirstFrameStartupDelayMillis = 60L
        private const val InitialSurfacePreloadTimeoutMillis = 2_500L
        private const val CompletedProgressHoldMillis = 140L
        private val AppOpaqueBackground = Color(0xFF020714)
    }
}
