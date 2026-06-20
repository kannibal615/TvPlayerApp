package com.smartvision.svplayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView

class SplashActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val openHome = Runnable {
        if (!isFinishing) {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

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

        val root = FrameLayout(this).apply {
            setBackgroundColor(getColor(R.color.splash_background_color))
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.splash)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = getString(R.string.app_name)
        }

        root.addView(
            logo,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER,
            ).apply {
                val horizontalInset = (resources.displayMetrics.widthPixels * 0.22f).toInt()
                val verticalInset = (resources.displayMetrics.heightPixels * 0.06f).toInt()
                setMargins(horizontalInset, verticalInset, horizontalInset, verticalInset)
            },
        )
        setContentView(root)

        handler.postDelayed(openHome, SplashDurationMillis)
    }

    override fun onDestroy() {
        handler.removeCallbacks(openHome)
        super.onDestroy()
    }

    private companion object {
        const val SplashDurationMillis = 1_000L
    }
}
