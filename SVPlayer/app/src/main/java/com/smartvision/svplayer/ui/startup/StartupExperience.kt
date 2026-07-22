package com.smartvision.svplayer.ui.startup

import android.content.Context
import android.graphics.drawable.Animatable
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.smartvision.svplayer.R
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings

@Composable
fun StartupExperience(
    phase: StartupVisualPhase,
    progress: StartupProgressSnapshot,
    strings: SmartVisionStrings,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.startup_cinema_background),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(R.drawable.smartvision_logo_wide),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 200.dp)
                .width(374.dp)
                .height(95.dp),
        )

        AnimatedVisibility(
            visible = phase == StartupVisualPhase.Loading,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(140)),
            modifier = Modifier.fillMaxSize(),
        ) {
            StartupLoadingOverlay(
                status = progress.stage.label(strings),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun StartupLoadingOverlay(
    status: String,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val progressSize = maxHeight * 0.088f
        val progressTopOffset = maxHeight * 0.61f

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = progressTopOffset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DecorativeStartupSpinner(
                modifier = Modifier
                    .width(progressSize)
                    .height(progressSize),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = status,
                color = Color(0xFFE7ECF7).copy(alpha = 0.88f),
                fontSize = 17.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DecorativeStartupSpinner(modifier: Modifier = Modifier) {
    AndroidView(
        factory = ::StartupSpinnerView,
        modifier = modifier,
    )
}

private class StartupSpinnerView(context: Context) : ImageView(context) {
    init {
        setImageResource(R.drawable.startup_spinner_animated)
        scaleType = ScaleType.FIT_CENTER
        contentDescription = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (drawable as? Animatable)?.start()
    }

    override fun onDetachedFromWindow() {
        (drawable as? Animatable)?.stop()
        super.onDetachedFromWindow()
    }
}

private fun StartupStage.label(strings: SmartVisionStrings): String = when (this) {
    StartupStage.Initializing -> strings.startupInitializing
    StartupStage.CheckingActivation -> strings.startupCheckingActivation
    StartupStage.PreparingHome -> strings.startupPreparingHome
    StartupStage.Starting -> strings.startupStarting
}
