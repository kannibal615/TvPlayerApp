package com.smartvision.svplayer.ui.appconfig

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.smartvision.svplayer.data.appconfig.ConsentConfig
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlin.math.roundToInt

@Composable
fun ConsentDialog(
    consent: ConsentConfig,
    onAccept: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val scrollFocusRequester = remember { FocusRequester() }
    val acceptFocusRequester = remember { FocusRequester() }
    val canAccept by remember {
        derivedStateOf { scrollState.maxValue == 0 || scrollState.value >= scrollState.maxValue - 8 }
    }

    LaunchedEffect(canAccept) {
        if (canAccept) runCatching { acceptFocusRequester.requestFocus() }
    }
    LaunchedEffect(Unit) {
        runCatching { scrollFocusRequester.requestFocus() }
    }

    Dialog(onDismissRequest = {}) {
        Column(
            modifier = Modifier
                .width(1080.dp)
                .height(500.dp)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF111C2E), Color(0xFF07101F))),
                    RoundedCornerShape(12.dp),
                )
                .border(BorderStroke(1.dp, SmartVisionColors.Primary.copy(alpha = 0.55f)), RoundedCornerShape(12.dp))
                .padding(20.dp),
        ) {
            Text(
                text = consent.title,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .focusRequester(scrollFocusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            val delta = when (event.key) {
                                Key.DirectionDown -> 72
                                Key.DirectionUp -> -72
                                Key.PageDown -> 260
                                Key.PageUp -> -260
                                else -> return@onPreviewKeyEvent false
                            }
                            scope.launch {
                                scrollState.animateScrollTo((scrollState.value + delta).coerceIn(0, scrollState.maxValue))
                            }
                            true
                        }
                        .background(Color(0x99071221), RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(8.dp))
                        .verticalScroll(scrollState)
                        .focusable()
                        .padding(12.dp),
                ) {
                    Text(
                        text = consent.body.applyConsentVariables(consent.variables).toBoldAnnotatedString(),
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Body,
                    )
                }
                Spacer(Modifier.width(10.dp))
                ConsentScrollbar(scrollState = scrollState)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (canAccept) "You can now accept." else "Scroll to the bottom to enable Accept.",
                    color = if (canAccept) SmartVisionColors.CyanAccent else SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Caption,
                    modifier = Modifier.weight(1f),
                )
                TvButton(
                    text = "Accept",
                    onClick = onAccept,
                    enabled = canAccept,
                    focusRequester = acceptFocusRequester.takeIf { canAccept },
                    variant = if (canAccept) TvButtonVariant.Primary else TvButtonVariant.Secondary,
                    modifier = Modifier.height(44.dp),
                )
            }
        }
    }
}

@Composable
private fun ConsentScrollbar(scrollState: androidx.compose.foundation.ScrollState) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier
            .width(5.dp)
            .fillMaxHeight()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50)),
    ) {
        val trackHeightPx = with(density) { maxHeight.toPx() }
        val minThumbPx = with(density) { 36.dp.toPx() }
        val thumbHeightPx = if (scrollState.maxValue <= 0) {
            trackHeightPx
        } else {
            (trackHeightPx * trackHeightPx / (trackHeightPx + scrollState.maxValue)).coerceIn(minThumbPx, trackHeightPx)
        }
        val offsetPx = if (scrollState.maxValue <= 0) {
            0f
        } else {
            (trackHeightPx - thumbHeightPx) * (scrollState.value.toFloat() / scrollState.maxValue.toFloat())
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(0, offsetPx.roundToInt()) }
                .width(5.dp)
                .height(with(density) { thumbHeightPx.toDp() })
                .background(SmartVisionColors.CyanAccent.copy(alpha = 0.82f), RoundedCornerShape(50)),
        )
    }
}

private fun String.applyConsentVariables(variables: Map<String, String>): String {
    var result = this
    variables.forEach { (key, value) ->
        result = result.replace("**?$key?**", "**$value**")
        result = result.replace("?$key?", value)
    }
    return result
}

private fun String.toBoldAnnotatedString() = buildAnnotatedString {
    var cursor = 0
    val regex = Regex("\\*\\*(.+?)\\*\\*")
    regex.findAll(this@toBoldAnnotatedString).forEach { match ->
        append(this@toBoldAnnotatedString.substring(cursor, match.range.first))
        pushStyle(SpanStyle(color = SmartVisionColors.TextPrimary, fontWeight = FontWeight.Bold))
        append(match.groupValues[1])
        pop()
        cursor = match.range.last + 1
    }
    if (cursor < this@toBoldAnnotatedString.length) {
        append(this@toBoldAnnotatedString.substring(cursor))
    }
}
