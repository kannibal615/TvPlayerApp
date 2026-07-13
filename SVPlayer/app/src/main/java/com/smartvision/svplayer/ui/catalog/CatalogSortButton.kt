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
import androidx.compose.material.icons.filled.Sort
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import kotlinx.coroutines.delay

@Composable
fun CatalogSortButton(options: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val buttonRequester = remember { FocusRequester() }
    Row(
        modifier = Modifier.height(34.dp).width(154.dp)
            .background(if (focused) SmartVisionColors.Primary.copy(alpha = 0.72f) else Color(0xB40B1B31), RoundedCornerShape(7.dp))
            .border(1.dp, if (focused) SmartVisionColors.CyanAccent else SmartVisionColors.Border, RoundedCornerShape(7.dp))
            .focusRequester(buttonRequester).onFocusChanged { focused = it.isFocused }.focusable().clickable { open = true }
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(Icons.Default.Sort, null, tint = SmartVisionColors.TextPrimary)
        Text(options.getOrNull(selectedIndex) ?: "Trier", color = SmartVisionColors.TextPrimary, maxLines = 1)
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
