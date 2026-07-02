package com.smartvision.svplayer.ui.home

import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.launch

@Composable
fun ContinueWatchingRow(
    title: String,
    items: List<ContinueItem>,
    onItemClick: (ContinueItem) -> Unit,
    modifier: Modifier = Modifier,
    showViewAll: Boolean = false,
    viewAllText: String = "View all",
    onViewAll: () -> Unit = {},
    lazyListState: LazyListState? = null,
    firstItemFocusRequester: FocusRequester? = null,
    onDownFromRow: (() -> Unit)? = null,
    onUpFromRow: (() -> Unit)? = null,
    enablePreview: Boolean = false,
    resumeOverlayText: String = "Resume playback",
    blocked: Boolean = false,
    onBlockedClick: () -> Unit = {},
) {
    val fallbackRowState = rememberLazyListState()
    val rowState = lazyListState ?: fallbackRowState
    val scope = rememberCoroutineScope()
    var focusedPreviewId by remember { mutableStateOf<String?>(null) }
    Column(modifier = modifier) {
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
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = SmartVisionDimensions.HomeRowEdgePadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                val cardWidth = if (enablePreview && focusedPreviewId == item.id) {
                    SmartVisionDimensions.HomeContentPreviewCardWidth
                } else {
                    SmartVisionDimensions.HomeContentCardWidth
                }
                ContentProgressCard(
                    item = item,
                    onClick = { if (blocked) onBlockedClick() else onItemClick(item) },
                    focusRequester = if (index == 0) firstItemFocusRequester else null,
                    onFocused = {
                        Log.i(HomeRowLogTag, "focused row=$title index=$index id=${item.id}")
                        scope.launch {
                            runCatching { rowState.animateScrollToItem(index) }
                                .onSuccess { Log.i(HomeRowLogTag, "scrollToFocused row=$title index=$index") }
                                .onFailure { Log.w(HomeRowLogTag, "scrollToFocusedFailed row=$title index=$index", it) }
                        }
                    },
                    onFocusChanged = { focused ->
                        if (enablePreview) {
                            focusedPreviewId = if (focused) item.id else focusedPreviewId?.takeUnless { it == item.id }
                        }
                    },
                    onDown = onDownFromRow,
                    onUp = onUpFromRow,
                    enablePreview = enablePreview,
                    resumeOverlayText = resumeOverlayText,
                    blocked = blocked,
                    modifier = Modifier
                        .width(cardWidth)
                        .height(SmartVisionDimensions.HomeContentCardHeight),
                )
            }
            if (showViewAll) {
                item(key = "view_all_$title") {
                    ViewAllButton(
                        text = viewAllText,
                        onClick = { if (blocked) onBlockedClick() else onViewAll() },
                        onDown = onDownFromRow,
                        onUp = onUpFromRow,
                        modifier = Modifier
                            .width(78.dp)
                            .height(SmartVisionDimensions.HomeContentCardHeight),
                    )
                }
            }
        }
    }
}

private const val HomeRowLogTag = "SVHomeFocus"

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
                focusedScale = 1.045f,
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
                        else -> false
                    }
                }
            }
            .tvFocusTarget(
                state = focusState,
                pressed = pressed,
                focusedScale = 1.045f,
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
