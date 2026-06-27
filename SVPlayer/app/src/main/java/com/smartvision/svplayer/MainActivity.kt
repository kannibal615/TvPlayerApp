package com.smartvision.svplayer

import android.os.Bundle
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
        setContent {
            SmartVisionTheme {
                val appContainer = (application as SVPlayerApplication).appContainer
                CompositionLocalProvider(LocalAppContainer provides appContainer) {
                    AppNavigation()
                }
            }
        }
    }
}
