package com.smartvision.svplayer.ui.startup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            painter = painterResource(R.drawable.startup_neon_background),
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
                progress = progress,
                status = progress.stage.label(strings),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun StartupLoadingOverlay(
    progress: StartupProgressSnapshot,
    status: String,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val progressWidth = maxWidth * 0.30f
        val progressHeight = (maxHeight * 0.008f).coerceAtLeast(MinimumProgressHeight)
        val progressTopOffset = maxHeight * 0.775f

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = progressTopOffset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StartupProgressBar(
                progress = progress.progress,
                modifier = Modifier
                    .width(progressWidth)
                    .height(progressHeight),
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
private fun StartupProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "startupProgress",
    )
    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xA00A1730)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF126DFF), Color(0xFF20E7F5)),
                    ),
                ),
        )
    }
}

private fun StartupStage.label(strings: SmartVisionStrings): String = when (this) {
    StartupStage.Initializing -> strings.startupInitializing
    StartupStage.CheckingActivation -> strings.startupCheckingActivation
    StartupStage.PreparingHome -> strings.startupPreparingHome
    StartupStage.Starting -> strings.startupStarting
}

private val MinimumProgressHeight: Dp = 5.dp
