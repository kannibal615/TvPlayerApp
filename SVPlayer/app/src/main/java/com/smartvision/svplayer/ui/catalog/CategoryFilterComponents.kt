package com.smartvision.svplayer.ui.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class FilterBarItem(val code: String?, val identity: CategoryFilterIdentity?, val count: Int)

@Composable
internal fun CategoryFilterBar(
    filters: List<CategoryFilter>,
    activeFilterCode: String?,
    strings: SmartVisionStrings,
    activeFilterFocusRequester: FocusRequester,
    categoryListFocusRequester: FocusRequester,
    filterButtonFocusRequester: FocusRequester,
    onApplyFilter: (String?) -> Unit,
) {
    val items = remember(filters, activeFilterCode, strings.liveTvCategoryFilterAll) {
        val orderedFilters = orderFiltersForBar(filters, activeFilterCode)
        listOf(FilterBarItem(null, null, 0)) + orderedFilters.map {
            FilterBarItem(it.identity.normalizedCode, it.identity, it.categoryCount)
        }
    }
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(activeFilterCode, items) {
        val activeIndex = items.indexOfFirst { it.code == activeFilterCode }
        if (activeIndex >= 0) state.animateScrollToItem(activeIndex)
    }

    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(SmartVisionColors.Border.copy(alpha = 0.7f)))
        Spacer(Modifier.height(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.canScrollBackward) {
                CategoryFilterLeftArrowChip(
                    categoryListFocusRequester = categoryListFocusRequester,
                    filterButtonFocusRequester = filterButtonFocusRequester,
                    onClick = { scope.launch { state.animateScrollToItem((state.firstVisibleItemIndex - VisibleFilterStep).coerceAtLeast(0)) } },
                )
            }
            LazyRow(
                state = state,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 1.dp),
            ) {
                itemsIndexed(items, key = { _, item -> item.code ?: "__all_filters__" }) { index, item ->
                    val selected = item.code == activeFilterCode
                    CategoryFilterChip(
                        label = item.identity?.displayName ?: strings.liveTvCategoryFilterAll,
                        visual = item.identity?.let(FlagResolver::visual),
                        count = item.count,
                        selected = selected,
                        modifier = if (selected) Modifier.focusRequester(activeFilterFocusRequester) else Modifier,
                        categoryListFocusRequester = categoryListFocusRequester,
                        filterButtonFocusRequester = filterButtonFocusRequester,
                        onFocused = { scope.launch { state.animateScrollToItem(index) } },
                        onClick = { onApplyFilter(item.code) },
                    )
                }
            }
        }
    }
}

internal fun orderFiltersForBar(filters: List<CategoryFilter>, activeFilterCode: String?): List<CategoryFilter> =
    filters.sortedBy { filter -> if (filter.identity.normalizedCode == activeFilterCode) 0 else 1 }

@Composable
internal fun CategoryFilterIconButton(
    strings: SmartVisionStrings,
    expanded: Boolean,
    focusRequester: FocusRequester,
    activeFilterFocusRequester: FocusRequester,
    categoryListFocusRequester: FocusRequester,
    headerFocusRequester: FocusRequester,
    onToggle: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.size(30.dp).clip(RoundedCornerShape(7.dp))
            .background(if (focused) SmartVisionColors.Primary.copy(alpha = 0.24f) else Color.Transparent)
            .border(1.dp, if (focused) SmartVisionColors.CyanAccent else SmartVisionColors.Border, RoundedCornerShape(7.dp))
            .focusRequester(focusRequester)
            .focusProperties {
                down = if (expanded) activeFilterFocusRequester else categoryListFocusRequester
                up = headerFocusRequester
            }
            .onFocusChanged { focused = it.isFocused }.focusable()
            .clickable(remember { MutableInteractionSource() }, null, onClick = onToggle)
            .semantics { contentDescription = strings.liveTvCategoryFilterOpen },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.FilterList, strings.liveTvCategoryFilterOpen, tint = SmartVisionColors.TextPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    visual: String?,
    count: Int,
    selected: Boolean,
    modifier: Modifier,
    categoryListFocusRequester: FocusRequester,
    filterButtonFocusRequester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val focusStyle = LocalTvFocusStyle.current
    var focused by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(false) }
    LaunchedEffect(focused) {
        showTooltip = false
        if (focused && visual != null) { delay(650); showTooltip = true }
    }
    Box {
        Row(
            modifier.height(30.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (selected) SmartVisionColors.Primary.copy(alpha = 0.72f) else Color(0xB40B1B31))
                .border(BorderStroke(if (focused) focusStyle.borderWidth else 1.dp, if (focused) focusStyle.accent else SmartVisionColors.Border), RoundedCornerShape(7.dp))
                .focusProperties { down = categoryListFocusRequester; up = filterButtonFocusRequester }
                .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocused() }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        event.key in setOf(Key.Enter, Key.NumPadEnter, Key.DirectionCenter)
                    ) {
                        onClick()
                        true
                    } else {
                        false
                    }
                }
                .focusable().clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
                .semantics { contentDescription = "$label, $count"; this.selected = selected }
                .padding(horizontal = if (visual == null) 10.dp else 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (label == "Arabic" || label == "Arabe") {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_filter_ar),
                    contentDescription = label,
                    modifier = Modifier.size(23.dp),
                )
            } else {
                Text(visual ?: label, color = SmartVisionColors.TextPrimary, fontSize = if (visual == null) 11.sp else 17.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (showTooltip) {
            Text(label, color = SmartVisionColors.TextPrimary, fontSize = 10.sp, maxLines = 1,
                modifier = Modifier.align(Alignment.BottomCenter).background(Color(0xF20A1323), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp))
        }
    }
}

@Composable
private fun CategoryFilterLeftArrowChip(enabled: Boolean = true, categoryListFocusRequester: FocusRequester, filterButtonFocusRequester: FocusRequester, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xB40B1B31))
        .border(1.dp, if (focused) SmartVisionColors.CyanAccent else SmartVisionColors.Border, RoundedCornerShape(7.dp))
        .focusProperties { down = categoryListFocusRequester; up = filterButtonFocusRequester }
        .onFocusChanged { focused = it.isFocused }.focusable(enabled).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.ChevronLeft, null, tint = SmartVisionColors.TextPrimary.copy(alpha = if (enabled) 1f else 0.3f))
    }
}

private const val VisibleFilterStep = 4

@Composable
internal fun CategoryFilterEmptyState(message: String, allLabel: String, focusRequester: FocusRequester, upFocusRequester: FocusRequester, onShowAll: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, color = SmartVisionColors.TextSecondary, fontSize = 13.sp)
        Box(Modifier.focusRequester(focusRequester).focusProperties { up = upFocusRequester }.clip(RoundedCornerShape(8.dp))
            .background(SmartVisionColors.Primary.copy(alpha = 0.65f)).focusable().clickable(onClick = onShowAll).padding(horizontal = 16.dp, vertical = 9.dp)) {
            Text(allLabel, color = SmartVisionColors.TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}
