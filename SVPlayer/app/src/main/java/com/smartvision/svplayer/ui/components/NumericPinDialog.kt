package com.smartvision.svplayer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay

private object PinDialogDimensions {
    val dialogWidth = 368.dp
    val dialogPaddingHorizontal = 28.dp
    val dialogPaddingVertical = 20.dp
    val dialogRadius = 18.dp
    val iconContainer = 42.dp
    val icon = 21.dp
    val pinDot = 22.dp
    val pinDotSpacing = 14.dp
    val dividerWidth = 292.dp
    val keyWidth = 85.dp
    val keyHeight = 38.dp
    val keySpacingHorizontal = 7.dp
    val keySpacingVertical = 5.dp
    val keyRadius = 7.dp
    val actionWidth = 136.dp
    val actionHeight = 44.dp
    val actionSpacing = 12.dp
}

@Composable
fun NumericPinDialog(
    title: String,
    strings: SmartVisionStrings,
    requireConfirmation: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Boolean,
) {
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var errorTrigger by remember { mutableIntStateOf(0) }
    val keyRequesters = remember { List(12) { FocusRequester() } }
    val cancelRequester = remember { FocusRequester() }
    val applyRequester = remember { FocusRequester() }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(firstPin) {
        delay(80)
        runCatching { keyRequesters.first().requestFocus() }
    }
    LaunchedEffect(pin.length) {
        if (pin.length == 4) {
            runCatching { applyRequester.requestFocus() }
        }
    }
    LaunchedEffect(errorTrigger) {
        if (errorTrigger == 0) return@LaunchedEffect
        listOf(-7f, 7f, -5f, 5f, 0f).forEach { target ->
            shakeOffset.animateTo(target, tween(durationMillis = 55))
        }
        runCatching { keyRequesters.first().requestFocus() }
    }

    fun appendDigit(digit: String) {
        if (pin.length < 4) pin += digit
        error = null
    }
    fun reject(message: String) {
        error = message
        pin = ""
        errorTrigger++
    }
    fun validate() {
        when {
            requireConfirmation && firstPin == null -> {
                firstPin = pin
                pin = ""
                error = null
            }
            requireConfirmation && firstPin != pin -> reject(strings.pinsDoNotMatch)
            !onSubmit(pin) -> reject(strings.pinIncorrect)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(PinDialogDimensions.dialogWidth)
                    .clip(RoundedCornerShape(PinDialogDimensions.dialogRadius))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0C1B2D), SmartVisionColors.Surface.copy(alpha = 0.99f)),
                        ),
                    )
                    .border(
                        BorderStroke(1.5.dp, SmartVisionColors.CyanAccent.copy(alpha = 0.88f)),
                        RoundedCornerShape(PinDialogDimensions.dialogRadius),
                    )
                    .padding(
                        horizontal = PinDialogDimensions.dialogPaddingHorizontal,
                        vertical = PinDialogDimensions.dialogPaddingVertical,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                /* Box(
                    modifier = Modifier
                        .size(PinDialogDimensions.iconContainer)
                        .clip(CircleShape)
                        .background(SmartVisionColors.CyanAccent.copy(alpha = 0.12f))
                        .border(BorderStroke(1.dp, SmartVisionColors.CyanAccent.copy(alpha = 0.72f)), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = SmartVisionColors.CyanAccent,
                        modifier = Modifier.size(PinDialogDimensions.icon),
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    text = title,
                    color = SmartVisionColors.TextPrimary,
                    style = SmartVisionType.TitleS,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ) */

Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Start,
    modifier = Modifier.fillMaxWidth(),
) {
    Box(
        modifier = Modifier
            .size(PinDialogDimensions.iconContainer)
            .clip(CircleShape)
            .background(SmartVisionColors.CyanAccent.copy(alpha = 0.12f))
            .border(
                BorderStroke(1.dp, SmartVisionColors.CyanAccent.copy(alpha = 0.72f)),
                CircleShape
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = SmartVisionColors.CyanAccent,
            modifier = Modifier.size(PinDialogDimensions.icon),
        )
    }

    Spacer(Modifier.width(8.dp))

    Text(
        text = title,
        color = SmartVisionColors.TextPrimary,
        style = SmartVisionType.TitleS,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start,
    )
}





                Spacer(Modifier.height(5.dp))
                /* Text(
                    text = if (firstPin == null) {
                        if (requireConfirmation) strings.newPin else strings.enterPin
                    } else strings.confirmPin,
                    color = SmartVisionColors.CyanAccent.copy(alpha = 0.92f),
                    style = SmartVisionType.Caption,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp)) */
                Row(
                    modifier = Modifier.graphicsLayer { translationX = shakeOffset.value.dp.toPx() },
                    horizontalArrangement = Arrangement.spacedBy(PinDialogDimensions.pinDotSpacing),
                ) {
                    repeat(4) { index -> PinDot(filled = index < pin.length) }
                }
                if (error != null) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = error.orEmpty(),
                        color = SmartVisionColors.Error,
                        style = SmartVisionType.Caption,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(12.dp))
                PinDivider()
                Spacer(Modifier.height(11.dp))

                val labels = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "CLR")
                labels.chunked(3).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(PinDialogDimensions.keySpacingHorizontal)) {
                        row.forEachIndexed { columnIndex, label ->
                            val index = rowIndex * 3 + columnIndex
                            val downTarget = if (rowIndex < 3) keyRequesters[index + 3] else if (columnIndex == 0) cancelRequester else applyRequester
                            PinKey(
                                text = label,
                                isBackspace = index == 9,
                                onClick = when (index) {
                                    9 -> ({ pin = pin.dropLast(1); error = null })
                                    11 -> ({ pin = ""; error = null })
                                    else -> ({ appendDigit(label) })
                                },
                                focusRequester = keyRequesters[index],
                                modifier = Modifier.focusProperties {
                                    if (columnIndex > 0) left = keyRequesters[index - 1]
                                    if (columnIndex < 2) right = keyRequesters[index + 1]
                                    if (rowIndex > 0) up = keyRequesters[index - 3]
                                    down = downTarget
                                },
                            )
                        }
                    }
                    if (rowIndex < 3) Spacer(Modifier.height(PinDialogDimensions.keySpacingVertical))
                }

                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(PinDialogDimensions.actionSpacing)) {
                    PinActionButton(
                        text = strings.cancel,
                        primary = false,
                        enabled = true,
                        onClick = onDismiss,
                        focusRequester = cancelRequester,
                        modifier = Modifier.focusProperties {
                            right = applyRequester
                            up = keyRequesters[9]
                        },
                    )
                    PinActionButton(
                        text = strings.apply,
                        primary = true,
                        enabled = pin.length == 4,
                        onClick = ::validate,
                        focusRequester = applyRequester,
                        modifier = Modifier.focusProperties {
                            left = cancelRequester
                            up = keyRequesters[11]
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PinDot(filled: Boolean) {
    val fill by animateColorAsState(
        targetValue = if (filled) SmartVisionColors.CyanAccent else Color.Transparent,
        animationSpec = tween(120),
        label = "pinDotFill",
    )
    Box(
        Modifier
            .size(PinDialogDimensions.pinDot)
            .clip(CircleShape)
            .background(fill.copy(alpha = if (filled) 0.88f else 0f))
            .border(BorderStroke(1.5.dp, Color(0xFFEAF7FF)), CircleShape),
    )
}

@Composable
private fun PinDivider() {
    Box(
        Modifier
            .width(PinDialogDimensions.dividerWidth)
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, SmartVisionColors.Border, SmartVisionColors.CyanAccent, SmartVisionColors.Border, Color.Transparent),
                ),
            ),
    )
}

@Composable
private fun PinKey(
    text: String,
    isBackspace: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background by animateColorAsState(
        if (focused) Color(0xFF173A55) else if (pressed) Color(0xFF132E45) else Color(0xFF0E2033),
        tween(100), label = "pinKeyBackground",
    )
    val border by animateColorAsState(
        if (focused) SmartVisionColors.CyanAccent else SmartVisionColors.CyanAccent.copy(alpha = 0.42f),
        tween(100), label = "pinKeyBorder",
    )
    Box(
        modifier = modifier
            .width(PinDialogDimensions.keyWidth)
            .height(PinDialogDimensions.keyHeight)
            .graphicsLayer {
                scaleX = if (pressed) 0.98f else 1f
                scaleY = if (pressed) 0.98f else 1f
            }
            .onFocusChanged { focused = it.isFocused }
            .then(Modifier.focusRequester(focusRequester))
            .clip(RoundedCornerShape(PinDialogDimensions.keyRadius))
            .background(background)
            .border(BorderStroke(if (focused) 1.5.dp else 1.dp, border), RoundedCornerShape(PinDialogDimensions.keyRadius))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        if (isBackspace) {
            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
        } else {
            Text(text, color = Color.White, style = SmartVisionType.Label, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PinActionButton(
    text: String,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background by animateColorAsState(
        when {
            !enabled -> SmartVisionColors.SurfaceElevated.copy(alpha = 0.52f)
            primary && focused -> Color(0xFF3595FF)
            primary -> SmartVisionColors.Primary
            focused -> Color(0xFF173A55)
            else -> Color(0xFF0E2033)
        },
        tween(100), label = "pinActionBackground",
    )
    val border = if (focused) SmartVisionColors.CyanAccent else SmartVisionColors.Border
    Box(
        modifier = modifier
            .width(PinDialogDimensions.actionWidth)
            .height(PinDialogDimensions.actionHeight)
            .graphicsLayer {
                scaleX = if (pressed) 0.98f else 1f
                scaleY = if (pressed) 0.98f else 1f
            }
            .onFocusChanged { focused = it.isFocused }
            .then(Modifier.focusRequester(focusRequester))
            .clip(RoundedCornerShape(PinDialogDimensions.keyRadius))
            .background(background)
            .border(BorderStroke(if (focused) 1.5.dp else 1.dp, border), RoundedCornerShape(PinDialogDimensions.keyRadius))
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(enabled = enabled, interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else SmartVisionColors.TextSecondary.copy(alpha = 0.48f),
            style = SmartVisionType.Label,
            textAlign = TextAlign.Center,
        )
    }
}
