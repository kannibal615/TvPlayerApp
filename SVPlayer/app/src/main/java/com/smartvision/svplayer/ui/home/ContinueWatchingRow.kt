package com.smartvision.svplayer.ui.home

import android.util.Log
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.smartvision.svplayer.data.diagnostics.PerformanceDiagnosticRecorder
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ContinueWatchingRow(
    title: String,
    items: List<ContinueItem>,
    onItemClick: (ContinueItem) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState? = null,
    firstItemFocusRequester: FocusRequester? = null,
    onDownFromRow: (() -> Unit)? = null,
    onUpFromRow: (() -> Unit)? = null,
    enablePreview: Boolean = false,
    resumeOverlayText: String = "Resume playback",
    blocked: Boolean = false,
    blockedMessage: String = "Connection unavailable",
    onBlockedClick: () -> Unit = {},
    previewController: HomePreviewController,
) {
    val fallbackRowState = rememberLazyListState()
    val rowState = lazyListState ?: fallbackRowState
    val rowScope = rememberCoroutineScope()
    val internalFirstItemFocusRequester = remember { FocusRequester() }
    val resolvedFirstItemFocusRequester = firstItemFocusRequester ?: internalFirstItemFocusRequester
    var focusedItemId by remember { mutableStateOf<String?>(null) }
    var focusedPreviewId by remember { mutableStateOf<String?>(null) }
    var anchorJob by remember { mutableStateOf<Job?>(null) }
    val totalFocusableItems = items.size
    val itemsSignature = remember(items) {
        items.joinToString("|") { it.id }
    }
    val lastItemFocusRequester = remember(itemsSignature) { FocusRequester() }

    DisposableEffect(rowState) {
        onDispose { anchorJob?.cancel() }
    }

    LaunchedEffect(itemsSignature) {
        focusedItemId = null
        focusedPreviewId = null
        // PERF_DIAG: row reset marker for diagnosing delayed section display and stale scroll state.
        PerformanceDiagnosticRecorder.record(
            sheet = PerformanceDiagnosticRecorder.SHEET_ROW_FOCUS,
            event = "home_row_items_signature",
            fields = mapOf(
                "row" to title,
                "items" to items.size,
                "totalFocusableItems" to totalFocusableItems,
            ),
        )
        if (rowState.firstVisibleItemIndex != 0 || rowState.firstVisibleItemScrollOffset != 0) {
            rowState.scrollToItem(0)
        }
    }

    LaunchedEffect(enablePreview, focusedItemId) {
        focusedPreviewId = null
        val pendingId = focusedItemId ?: return@LaunchedEffect
        if (!enablePreview) return@LaunchedEffect
        // PERF_FIX: do not transform portrait cards to landscape while the user is still moving fast.
        delay(HomeCardPreviewTransformDelayMillis)
        if (focusedItemId == pendingId) {
            focusedPreviewId = pendingId
        }
    }

    fun anchorFocusedItem(index: Int) {
        if (index < 0 || totalFocusableItems <= 0) return
        val targetIndex = index
        // PERF_DIAG: captures end-of-list math that can cause right-side clipping.
        PerformanceDiagnosticRecorder.record(
            sheet = PerformanceDiagnosticRecorder.SHEET_ROW_FOCUS,
            event = "home_row_anchor_decision",
            fields = mapOf(
                "row" to title,
                "focusedIndex" to index,
                "targetFirstIndex" to targetIndex,
                "visibleCount" to rowState.layoutInfo.visibleItemsInfo.size,
                "firstVisibleItemIndex" to rowState.firstVisibleItemIndex,
                "viewportStartOffset" to rowState.layoutInfo.viewportStartOffset,
                "viewportEndOffset" to rowState.layoutInfo.viewportEndOffset,
            ),
        )
        if (rowState.firstVisibleItemIndex == targetIndex && rowState.firstVisibleItemScrollOffset == 0) return
        anchorJob?.cancel()
        anchorJob = rowScope.launch {
            try {
                rowState.animateScrollToItem(targetIndex)
                Log.i(HomeRowLogTag, "anchor row=$title focused=$index first=$targetIndex")
                PerformanceDiagnosticRecorder.record(
                    sheet = PerformanceDiagnosticRecorder.SHEET_ROW_FOCUS,
                    event = "home_row_anchor_done",
                    fields = mapOf(
                        "row" to title,
                        "focusedIndex" to index,
                        "targetFirstIndex" to targetIndex,
                        "firstVisibleItemIndex" to rowState.firstVisibleItemIndex,
                        "firstVisibleItemScrollOffset" to rowState.firstVisibleItemScrollOffset,
                    ),
                )
            } catch (cancellation: CancellationException) {
                PerformanceDiagnosticRecorder.record(
                    sheet = PerformanceDiagnosticRecorder.SHEET_ROW_FOCUS,
                    event = "home_row_anchor_canceled",
                    fields = mapOf("row" to title, "focusedIndex" to index),
                )
                throw cancellation
            } catch (error: Throwable) {
                Log.w(HomeRowLogTag, "anchor failed row=$title focused=$index", error)
                PerformanceDiagnosticRecorder.record(
                    sheet = PerformanceDiagnosticRecorder.SHEET_ERRORS,
                    event = "home_row_anchor_failed",
                    fields = mapOf("row" to title, "focusedIndex" to index),
                    error = error,
                )
            }
        }
    }

    fun wrapFocus(targetIndex: Int, targetRequester: FocusRequester) {
        if (items.isEmpty()) return
        rowScope.launch {
            rowState.scrollToItem(targetIndex.coerceIn(items.indices))
            withFrameNanos { }
            runCatching { targetRequester.requestFocus() }
        }
    }

    Column(modifier = modifier.height(SmartVisionDimensions.HomeContentRowHeight)) {
        Text(
            text = title,
            color = SmartVisionColors.TextPrimary,
            style = SmartVisionType.HomeSectionTitle,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(
            state = rowState,
            modifier = Modifier
                .fillMaxWidth()
                .height(SmartVisionDimensions.HomeContentCardHeight),
            contentPadding = PaddingValues(horizontal = SmartVisionDimensions.HomeRowEdgePadding),
            horizontalArrangement = Arrangement.spacedBy(SmartVisionDimensions.HomeContentCardSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                ContentProgressCard(
                    item = item,
                    onClick = { if (blocked) onBlockedClick() else onItemClick(item) },
                    focusRequester = when {
                        index == 0 -> resolvedFirstItemFocusRequester
                        index == items.lastIndex -> lastItemFocusRequester
                        else -> null
                    },
                    onFocused = {
                        Log.i(HomeRowLogTag, "focused row=$title index=$index id=${item.id}")
                        // PERF_DIAG: one row per focused card, including width target and visible LazyRow state.
                        PerformanceDiagnosticRecorder.record(
                            sheet = PerformanceDiagnosticRecorder.SHEET_ROW_FOCUS,
                            event = "home_row_item_focused",
                            fields = mapOf(
                                "row" to title,
                                "index" to index,
                                "id" to item.id,
                                "title" to item.title,
                                "previewMode" to item.previewMode.name,
                                "hasPreviewUrl" to !item.previewUrl.isNullOrBlank(),
                                "targetCardWidthDp" to SmartVisionDimensions.HomeContentPreviewCardWidth.value,
                                "transformDelayMs" to HomeCardPreviewTransformDelayMillis,
                                "firstVisibleItemIndex" to rowState.firstVisibleItemIndex,
                                "visibleItems" to rowState.layoutInfo.visibleItemsInfo.size,
                            ),
                        )
                        // Let LazyRow perform its native minimal reveal. Forcing every
                        // focused item to index 0 made each D-pad step animate the whole row.
                    },
                    onFocusChanged = { focused ->
                        if (enablePreview) {
                            focusedItemId = if (focused) item.id else focusedItemId?.takeUnless { it == item.id }
                            if (!focused && focusedPreviewId == item.id) {
                                focusedPreviewId = null
                            }
                        }
                        PerformanceDiagnosticRecorder.record(
                            sheet = PerformanceDiagnosticRecorder.SHEET_ROW_FOCUS,
                            event = "home_row_item_focus_changed",
                            fields = mapOf("row" to title, "index" to index, "id" to item.id, "focused" to focused),
                        )
                    },
                    onDown = onDownFromRow,
                    onUp = onUpFromRow,
                    onLeft = if (index == 0) {
                        { wrapFocus(items.lastIndex, if (items.size == 1) resolvedFirstItemFocusRequester else lastItemFocusRequester) }
                    } else {
                        null
                    },
                    onRight = if (index == items.lastIndex) {
                        { wrapFocus(0, resolvedFirstItemFocusRequester) }
                    } else {
                        null
                    },
                    enablePreview = enablePreview && focusedPreviewId == item.id,
                    resumeOverlayText = resumeOverlayText,
                    blocked = blocked,
                    blockedMessage = blockedMessage,
                    previewController = previewController,
                    modifier = Modifier
                        .width(SmartVisionDimensions.HomeContentPreviewCardWidth)
                        .height(SmartVisionDimensions.HomeContentCardHeight),
                )
            }
            item(key = "tail_spacer_$title") {
                Spacer(
                    modifier = Modifier
                        .width(SmartVisionDimensions.HomeContentPreviewCardWidth * 3)
                        .height(SmartVisionDimensions.HomeContentCardHeight),
                )
            }
        }
    }
}

private const val HomeRowLogTag = "SVHomeFocus"
private const val HomeCardPreviewTransformDelayMillis = 550L

@Composable
private fun RowChevronButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(SmartVisionDimensions.HomeContentRadius)
    val focusStyle = LocalTvFocusStyle.current

    Box(
        modifier = modifier
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .tvFocusTarget(
                state = focusState,
                pressed = pressed,
                focusedScale = 1.0f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = SmartVisionDimensions.HomeContentRadius,
            )
            .clip(shape)
            .background(SmartVisionColors.SurfaceElevated.copy(alpha = 0.8f))
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else 1.dp,
                    if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border,
                ),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = SmartVisionColors.TextPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ViewAllButton(
    text: String,
    onClick: () -> Unit,
    onDown: (() -> Unit)? = null,
    onUp: (() -> Unit)? = null,
    blockLeft: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(SmartVisionDimensions.HomeContentRadius)
    val focusStyle = LocalTvFocusStyle.current

    Column(
        modifier = modifier
            .zIndex(if (focusState.isFocused) 2f else 0f)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when {
                        event.key == Key.DirectionDown && onDown != null -> {
                            onDown()
                            true
                        }
                        event.key == Key.DirectionUp && onUp != null -> {
                            onUp()
                            true
                        }
                        event.key == Key.DirectionLeft && blockLeft -> true
                        else -> false
                    }
                }
            }
            .tvFocusTarget(
                state = focusState,
                pressed = pressed,
                focusedScale = 1.0f,
                glowColor = SmartVisionColors.Primary,
                cornerRadius = SmartVisionDimensions.HomeContentRadius,
            )
            .clip(shape)
            .background(SmartVisionColors.SurfaceElevated.copy(alpha = 0.82f))
            .border(
                BorderStroke(
                    if (focusState.isFocused) focusStyle.borderWidth else 1.dp,
                    if (focusState.isFocused) focusStyle.accent else SmartVisionColors.Border,
                ),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(PaddingValues(horizontal = 8.dp, vertical = 10.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = SmartVisionColors.TextPrimary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = text,
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Caption,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
