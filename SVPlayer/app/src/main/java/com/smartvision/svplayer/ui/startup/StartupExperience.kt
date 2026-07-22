package com.smartvision.svplayer.ui.startup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
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
        val progressSize = maxHeight * 0.088f
        val progressTopOffset = maxHeight * 0.61f

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = progressTopOffset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StartupProgressRing(
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
private fun StartupProgressRing(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "startup-spinner")
    val rotationDegrees by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "startup-spinner-rotation",
    )
    Canvas(
        modifier = modifier.graphicsLayer {
            // Status updates must not rebuild or restart this continuous loading motion.
            rotationZ = rotationDegrees
        },
    ) {
        val stroke = size.minDimension * 0.038f
        val sweep = 92f
        drawArc(
            color = Color(0xFF17335F).copy(alpha = 0.70f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = Color(0xFF159DFF).copy(alpha = 0.22f),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(width = stroke * 1.8f, cap = StrokeCap.Round),
        )
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF125BFF),
                    Color(0xFF16D9FF),
                    Color.White,
                    Color(0xFF125BFF),
                ),
            ),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

private fun StartupStage.label(strings: SmartVisionStrings): String = when (this) {
    StartupStage.Initializing -> strings.startupInitializing
    StartupStage.CheckingActivation -> strings.startupCheckingActivation
    StartupStage.PreparingHome -> strings.startupPreparingHome
    StartupStage.Starting -> strings.startupStarting
}
