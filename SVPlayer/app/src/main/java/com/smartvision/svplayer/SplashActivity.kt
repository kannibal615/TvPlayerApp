package com.smartvision.svplayer

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

class SplashActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
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
            setImageResource(R.drawable.smartvision_splash_full)
            scaleType = ImageView.ScaleType.FIT_XY
        }
        val displayHeight = resources.displayMetrics.heightPixels
        val markSize = (displayHeight * 0.38f).toInt()
        val wordmarkWidth = (displayHeight * 0.76f).toInt()
        val wordmarkHeight = (wordmarkWidth * WordmarkAspectRatio).toInt()
        val haloSize = (markSize * 1.72f).toInt()

        val halo = View(this).apply {
            alpha = 0.46f
            scaleX = 1f
            scaleY = 1f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = haloSize * 0.48f
                colors = intArrayOf(
                    Color.argb(112, 0, 210, 255),
                    Color.argb(34, 0, 116, 255),
                    Color.TRANSPARENT,
                )
            }
        }

        val mark = ImageView(this).apply {
            setImageResource(R.drawable.smartvision_mark)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = getString(R.string.app_name)
        }
        val wordmark = ImageView(this).apply {
            setImageResource(R.drawable.smartvision_wordmark)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = null
        }
        val logoGroup = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            alpha = LogoPulseMinAlpha
            scaleX = 1f
            scaleY = 1f
            translationY = 0f
            addView(
                mark,
                LinearLayout.LayoutParams(markSize, markSize),
            )
            addView(
                wordmark,
                LinearLayout.LayoutParams(wordmarkWidth, wordmarkHeight).apply {
                    topMargin = -(displayHeight * 0.035f).toInt()
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
            halo,
            FrameLayout.LayoutParams(
                haloSize,
                haloSize,
                Gravity.CENTER,
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
                    root.post { startSplashAnimation(logoGroup, halo) }
                    return true
                }
            },
        )
    }

    private fun startSplashAnimation(logoGroup: View, halo: View) {
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
        halo.animate()
            .alpha(0.72f)
            .scaleX(1.04f)
            .scaleY(1.04f)
            .setDuration(HaloRevealMillis)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        handler.postDelayed(openHome, SplashDurationMillis)
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
        super.onDestroy()
    }

    private companion object {
        const val WordmarkAspectRatio = 340f / 1400f
        const val LogoPulseMinAlpha = 0.08f
        const val LogoPulseMaxAlpha = 0.26f
        const val LogoPulseMillis = 760L
        const val HaloRevealMillis = 720L
        const val SplashDurationMillis = 3_400L
    }
}
