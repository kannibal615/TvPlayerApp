package com.smartvision.svplayer

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.smartvision.svplayer.core.data.AppContainer
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.sync.SyncFrequencyPolicy
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val splashScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var root: FrameLayout
    private var launched = false
    private var animationStarted = false
    private val openHome = Runnable { launchHome() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(2, 8, 23))
        }
        val backdrop = ImageView(this).apply {
            setImageResource(R.drawable.smartvision_splash_bg)
            scaleType = ImageView.ScaleType.FIT_XY
        }
        val displayWidth = resources.displayMetrics.widthPixels
        val displayHeight = resources.displayMetrics.heightPixels
        val logoWidth = (displayWidth * SplashLogoWidthRatio).toInt()
        val logoHeight = (logoWidth * LogoAspectRatio).toInt()
        val progressWidth = (displayWidth * SplashProgressWidthRatio).toInt()

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.smartvision_logo_wide)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = getString(R.string.app_name)
        }
        val progressFill = View(this).apply {
            scaleX = 0f
            pivotX = 0f
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(29, 118, 255), Color.rgb(31, 221, 255)),
            ).apply {
                cornerRadius = displayHeight * 0.012f
            }
        }
        val progressTrack = FrameLayout(this).apply {
            alpha = 0f
            background = GradientDrawable().apply {
                cornerRadius = displayHeight * 0.012f
                setColor(Color.argb(150, 13, 29, 54))
            }
            addView(
                progressFill,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        val statusText = TextView(this).apply {
            text = "Initialisation en cours..."
            setTextColor(Color.argb(220, 219, 234, 254))
            textSize = 13f
            gravity = Gravity.CENTER
            alpha = 0f
            includeFontPadding = false
        }
        val logoGroup = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            alpha = LogoPulseMinAlpha
            scaleX = 1f
            scaleY = 1f
            translationY = 0f
            addView(
                logo,
                LinearLayout.LayoutParams(logoWidth, logoHeight),
            )
            addView(
                progressTrack,
                LinearLayout.LayoutParams(progressWidth, (displayHeight * 0.008f).toInt().coerceAtLeast(5)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = (displayHeight * 0.018f).toInt()
                },
            )
            addView(
                statusText,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = (displayHeight * 0.014f).toInt()
                },
            )
        }

        root.addView(
            backdrop,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        root.addView(
            logoGroup,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            ),
        )
        setContentView(root)

        root.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (root.viewTreeObserver.isAlive) {
                        root.viewTreeObserver.removeOnPreDrawListener(this)
                    }
                    root.post { startSplashAnimation(logoGroup, progressTrack, progressFill, statusText) }
                    return true
                }
            },
        )
    }

    private fun startSplashAnimation(logoGroup: View, progressTrack: View, progressFill: View, statusText: TextView) {
        if (animationStarted || launched || isFinishing) return
        animationStarted = true
        logoGroup.startAnimation(
            AlphaAnimation(LogoPulseMinAlpha, LogoPulseMaxAlpha).apply {
                duration = LogoPulseMillis
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            },
        )
        handler.postDelayed(
            {
                if (launched || isFinishing) return@postDelayed
                progressTrack.animate()
                    .alpha(1f)
                    .setDuration(LoadingFadeMillis)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
                statusText.animate()
                    .alpha(1f)
                    .setDuration(LoadingFadeMillis)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            },
            LoadingRevealDelayMillis,
        )
        splashScope.launch {
            runStartupChecks(statusText, progressFill)
        }
    }

    private suspend fun runStartupChecks(statusText: TextView, progressFill: View) {
        val startedAt = SystemClock.elapsedRealtime()
        val container = (application as SVPlayerApplication).appContainer
        var totalSteps = StartupStepsWithSync
        suspend fun update(label: String, step: Int, total: Int = totalSteps) {
            statusText.text = label
            animateStartupProgress(progressFill, step, total)
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

    private fun animateStartupProgress(progressFill: View, step: Int, total: Int) {
        val target = (step.toFloat() / total.toFloat()).coerceIn(MinimumProgressScale, 1f)
        progressFill.animate()
            .scaleX(target)
            .setDuration(ProgressStepMillis)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
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

    private fun launchHome() {
        if (launched || isFinishing) return
        launched = true
        handler.removeCallbacks(openHome)
        root.clearAnimation()
        root.animate().cancel()
        root.alpha = 1f
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(0, 0)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(openHome)
        splashScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val LogoAspectRatio = 248f / 980f
        const val SplashLogoWidthRatio = 0.54f
        const val SplashProgressWidthRatio = 0.36f
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
