package com.smartvision.svplayer.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import kotlinx.coroutines.delay

@Composable
fun CatalogSortButton(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    focusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
) {
    var open by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val internalButtonRequester = remember { FocusRequester() }
    val buttonRequester = focusRequester ?: internalButtonRequester
    val active = selectedIndex > 0
    val backgroundColor = when {
        focused -> SmartVisionColors.Primary.copy(alpha = 0.78f)
        active -> SmartVisionColors.Primary.copy(alpha = 0.48f)
        else -> Color(0xB40B1B31)
    }
    val borderColor = when {
        focused -> SmartVisionColors.CyanAccent
        active -> SmartVisionColors.Primary
        else -> SmartVisionColors.Border
    }
    Row(
        modifier = Modifier.height(30.dp).width(30.dp)
            .background(backgroundColor, RoundedCornerShape(7.dp))
            .border(1.dp, borderColor, RoundedCornerShape(7.dp))
            .focusRequester(buttonRequester)
            .onPreviewKeyEvent { event ->
                val confirmationKey = event.key == Key.DirectionCenter ||
                    event.key == Key.Enter ||
                    event.key == Key.NumPadEnter
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft && leftFocusRequester != null -> {
                        runCatching { leftFocusRequester.requestFocus() }
                        true
                    }
                    confirmationKey && event.type == KeyEventType.KeyDown -> true
                    confirmationKey && event.type == KeyEventType.KeyUp -> {
                        open = true
                        true
                    }
                    else -> false
                }
            }
            .onFocusChanged { focused = it.isFocused }.focusable().clickable { open = true }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.FilterList,
            contentDescription = options.getOrNull(selectedIndex) ?: "Sort",
            tint = if (active || focused) Color.White else SmartVisionColors.TextPrimary,
        )
    }
    if (open) {
        val firstRequester = remember { FocusRequester() }
        Dialog(onDismissRequest = { open = false; runCatching { buttonRequester.requestFocus() } }) {
            LazyColumn(
                Modifier.width(410.dp).background(Color(0xFA07101E), RoundedCornerShape(12.dp))
                    .border(1.dp, SmartVisionColors.Border, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(options) { index, label ->
                    var itemFocused by remember { mutableStateOf(false) }
                    Row(
                        Modifier.fillMaxWidth().height(46.dp)
                            .then(if (index == selectedIndex) Modifier.focusRequester(firstRequester) else Modifier)
                            .background(if (index == selectedIndex) SmartVisionColors.Primary.copy(alpha = 0.65f) else Color(0xB40B1B31), RoundedCornerShape(8.dp))
                            .border(1.dp, if (itemFocused) SmartVisionColors.CyanAccent else SmartVisionColors.Border, RoundedCornerShape(8.dp))
                            .onPreviewKeyEvent { event ->
                                val confirmationKey = event.key == Key.DirectionCenter ||
                                    event.key == Key.Enter ||
                                    event.key == Key.NumPadEnter
                                when {
                                    !confirmationKey -> false
                                    event.type == KeyEventType.KeyDown -> true
                                    event.type == KeyEventType.KeyUp -> {
                                        onSelected(index)
                                        open = false
                                        runCatching { buttonRequester.requestFocus() }
                                        true
                                    }
                                    else -> false
                                }
                            }
                            .onFocusChanged { itemFocused = it.isFocused }.focusable().clickable {
                                onSelected(index); open = false; runCatching { buttonRequester.requestFocus() }
                            }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { Text(label, color = SmartVisionColors.TextPrimary, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        LaunchedEffect(Unit) { delay(80); runCatching { firstRequester.requestFocus() } }
    }
}
