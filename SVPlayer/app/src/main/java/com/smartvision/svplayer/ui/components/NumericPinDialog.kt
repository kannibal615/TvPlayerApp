package com.smartvision.svplayer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay

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
    val firstKeyRequester = remember { FocusRequester() }

    LaunchedEffect(firstPin) {
        delay(80)
        runCatching { firstKeyRequester.requestFocus() }
    }

    TvDialogSurface(title = title, onDismiss = onDismiss, width = 430.dp, icon = Icons.Default.Lock) {
        Text(
            text = if (firstPin == null) {
                if (requireConfirmation) strings.newPin else strings.enterPin
            } else strings.confirmPin,
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Caption,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = buildString {
                repeat(4) { index -> append(if (index < pin.length) "●" else "○") }
            },
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.TitleL,
        )
        error?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = SmartVisionColors.Error, style = SmartVisionType.Caption)
        }
        Spacer(Modifier.height(12.dp))
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { columnIndex, digit ->
                    TvButton(
                        text = digit,
                        onClick = { if (pin.length < 4) pin += digit; error = null },
                        focusRequester = firstKeyRequester.takeIf { rowIndex == 0 && columnIndex == 0 },
                        variant = TvButtonVariant.Secondary,
                        modifier = Modifier.weight(1f).height(46.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvButton(text = "⌫", onClick = { pin = pin.dropLast(1); error = null }, variant = TvButtonVariant.Secondary, modifier = Modifier.weight(1f).height(46.dp))
            TvButton(text = "0", onClick = { if (pin.length < 4) pin += "0"; error = null }, variant = TvButtonVariant.Secondary, modifier = Modifier.weight(1f).height(46.dp))
            TvButton(text = "CLR", onClick = { pin = ""; error = null }, variant = TvButtonVariant.Secondary, modifier = Modifier.weight(1f).height(46.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TvButton(text = strings.cancel, onClick = onDismiss, variant = TvButtonVariant.Secondary, modifier = Modifier.height(42.dp))
            Spacer(Modifier.width(10.dp))
            TvButton(
                text = strings.apply,
                enabled = pin.length == 4,
                onClick = {
                    if (requireConfirmation && firstPin == null) {
                        firstPin = pin
                        pin = ""
                    } else if (requireConfirmation && firstPin != pin) {
                        error = strings.pinsDoNotMatch
                        pin = ""
                    } else if (!onSubmit(pin)) {
                        error = strings.pinIncorrect
                        pin = ""
                    }
                },
                modifier = Modifier.height(42.dp),
            )
        }
    }
}
