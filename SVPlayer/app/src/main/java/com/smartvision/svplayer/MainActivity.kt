package com.smartvision.svplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.ui.navigation.AppNavigation
import com.smartvision.svplayer.ui.theme.SmartVisionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setBackgroundDrawableResource(R.drawable.splash_background)
        Log.i(TAG, "onCreate: splash window background applied before AppNavigation")
        requestNotificationPermissionIfNeeded()
        handleIntent(intent)
        setContent {
            Log.i(TAG, "setContent: rendering AppNavigation")
            SmartVisionTheme {
                val appContainer = (application as SVPlayerApplication).appContainer
                CompositionLocalProvider(LocalAppContainer provides appContainer) {
                    AppNavigation()
                }
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

    companion object {
        private const val TAG = "SmartVisionFocus"
        const val ACTION_SHOW_XTREAM_CONNECTION_ALERT = "com.smartvision.svplayer.SHOW_XTREAM_CONNECTION_ALERT"
        private const val REQUEST_NOTIFICATIONS = 7041
    }
}
