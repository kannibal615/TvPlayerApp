package com.smartvision.svplayer.ui.appconfig

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smartvision.svplayer.R
import com.smartvision.svplayer.data.appconfig.ConsentConfig
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.launch
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

    LaunchedEffect(Unit) {
        runCatching { scrollFocusRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.bg_consent_neon),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f)),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.88f)
                    .fillMaxHeight(0.86f)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xEE0B1930),
                                Color(0xEE06101F),
                            ),
                        ),
                        RoundedCornerShape(15.dp),
                    )
                    .border(BorderStroke(1.dp, SmartVisionColors.Primary.copy(alpha = 0.55f)), RoundedCornerShape(15.dp))
                    .padding(horizontal = 34.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ConsentHeader()
                Spacer(Modifier.height(10.dp))
                ConsentLegalPanel(
                    scrollState = scrollState,
                    scrollFocusRequester = scrollFocusRequester,
                    canAccept = canAccept,
                    onScrollKey = { delta ->
                        scope.launch {
                            scrollState.animateScrollTo((scrollState.value + delta).coerceIn(0, scrollState.maxValue))
                        }
                    },
                    onMoveToAccept = {
                        runCatching { acceptFocusRequester.requestFocus() }
                    },
                    body = consent.body.applyConsentVariables(consent.variables),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                   Spacer(Modifier.height(10.dp))

                    ConsentBottomActions(
                    canAccept = canAccept,
                    focusRequester = acceptFocusRequester.takeIf { canAccept },
                    onMoveToScroll = {
                        scope.launch {
                            runCatching { scrollFocusRequester.requestFocus() }
                            scrollState.animateScrollTo((scrollState.maxValue - 72).coerceAtLeast(0))
                        }
                    },
                    onClick = onAccept,
                )
            }
        }
    }
}

@Composable
private fun ConsentHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp),
        ) {
            Text(
    text = buildAnnotatedString {
        pushStyle(
            SpanStyle(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
        )
        append("Welcome to ")
        pop()

        pushStyle(
            SpanStyle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF2F6FF),
                        Color(0xFFE7EEFB)
                    )
                ),
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
        )
        append("Smart")
        pop()

        pushStyle(
            SpanStyle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF20E8FF),
                        Color(0xFF0A9DFF),
                        Color(0xFF006BFF)
                    )
                ),
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
        )
        append("Vision")
        pop()

        pushStyle(
            SpanStyle(
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp
            )
        )
        append(" Player")
        pop()
    },
    style = TextStyle(
        fontSize = 28.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.35f),
            offset = Offset(1.2f, 1.8f),
            blurRadius = 3.5f
        )
    ),
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth(),
)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "To continue, please review and accept our Privacy Policy and Terms of Use.",
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Body.copy(fontSize = 13.sp, lineHeight = 20.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ConsentLegalPanel(
    scrollState: androidx.compose.foundation.ScrollState,
    scrollFocusRequester: FocusRequester,
    canAccept: Boolean,
    onScrollKey: (Int) -> Unit,
    onMoveToAccept: () -> Unit,
    body: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color(0xAA06101F), RoundedCornerShape(15.dp))
            .border(BorderStroke(1.dp, Color(0xFF1E4E86).copy(alpha = 0.65f)), RoundedCornerShape(15.dp)),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusRequester(scrollFocusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionDown -> {
                                if (canAccept && scrollState.value >= scrollState.maxValue - 8) {
                                    onMoveToAccept()
                                } else {
                                    onScrollKey(72)
                                }
                            }
                            Key.DirectionUp -> onScrollKey(-72)
                            Key.PageDown -> onScrollKey(260)
                            Key.PageUp -> onScrollKey(-260)
                            else -> return@onPreviewKeyEvent false
                        }
                        true
                    }
                    .verticalScroll(scrollState)
                    .focusable()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Text(
                    text = "PRIVACY POLICY AND TERMS OF USE",
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Body.copy(fontSize = 13.sp, lineHeight = 16.sp),
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = body.toBoldAnnotatedString(),
                    color = SmartVisionColors.TextSecondary,
                    style = SmartVisionType.Body.copy(fontSize = 10.sp, lineHeight = 16.sp),
                )
            }
            ConsentScrollbar(scrollState = scrollState)
        }
    }
}

@Composable
private fun ConsentBottomActions(
    canAccept: Boolean,
    focusRequester: FocusRequester?,
    onMoveToScroll: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConsentAcceptButton(
            enabled = canAccept,
            focusRequester = focusRequester,
            onMoveToScroll = onMoveToScroll,
            onClick = onClick,
        )

        Spacer(Modifier.width(18.dp))

        Text(
            text = if (canAccept) {
                ""
            } else {
                "Scroll to the bottom..."
            },
            color = if (canAccept) {
                SmartVisionColors.CyanAccent
            } else {
                SmartVisionColors.TextSecondary
            },
            style = SmartVisionType.Label.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun ConsentAcceptButton(
    enabled: Boolean,
    focusRequester: FocusRequester?,
    onMoveToScroll: () -> Unit,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(12.dp)
    val background = if (enabled) {
        Brush.horizontalGradient(
            listOf(
                SmartVisionColors.Primary.copy(alpha = 0.96f),
                SmartVisionColors.PrimaryDark.copy(alpha = 0.96f),
                SmartVisionColors.CyanAccent.copy(alpha = 0.82f),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xAA0B1930),
                Color(0xAA06101F),
            ),
        )
    }
    val borderColor = when {
        focusState.isFocused && enabled -> SmartVisionColors.FocusWhite
        enabled -> SmartVisionColors.CyanAccent.copy(alpha = 0.74f)
        else -> SmartVisionColors.CyanAccent.copy(alpha = 0.36f)
    }

    Row(
        modifier = Modifier
            .width(170.dp)
            .height(40.dp)
            .tvFocusTarget(
                state = focusState,
                focusRequester = focusRequester,
                enabled = enabled,
                pressed = pressed,
                focusedScale = 1.035f,
                glowColor = SmartVisionColors.CyanAccent,
                cornerRadius = 10.dp,
            )
            .clip(shape)
            .background(background)
            .border(BorderStroke(if (focusState.isFocused && enabled) 2.dp else 1.dp, borderColor), shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                    onMoveToScroll()
                    true
                } else {
                    false
                }
            }
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!enabled) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = SmartVisionColors.TextSecondary.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = "Accept",
            color = if (enabled) Color.White else SmartVisionColors.TextSecondary.copy(alpha = 0.72f),
            style = SmartVisionType.Label.copy(fontSize = 15.sp, lineHeight = 18.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ConsentScrollbar(scrollState: androidx.compose.foundation.ScrollState) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .width(18.dp)
            .fillMaxHeight()
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(50)),
        ) {
            val trackHeightPx = with(density) { maxHeight.toPx() }
            val minThumbPx = with(density) { 44.dp.toPx() }
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
                    .width(6.dp)
                    .height(with(density) { thumbHeightPx.toDp() })
                    .background(SmartVisionColors.CyanAccent.copy(alpha = 0.90f), RoundedCornerShape(50)),
            )
        }
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
