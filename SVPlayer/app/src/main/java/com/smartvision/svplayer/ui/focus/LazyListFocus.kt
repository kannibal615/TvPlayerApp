package com.smartvision.svplayer.ui.focus

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.withFrameNanos

/** Waits for a lazy-list item to be laid out before its FocusRequester is used. */
suspend fun LazyListState.awaitItemVisible(index: Int, maxFrames: Int = 8): Boolean {
    repeat(maxFrames) {
        if (layoutInfo.visibleItemsInfo.any { item -> item.index == index }) return true
        withFrameNanos { }
    }
    return layoutInfo.visibleItemsInfo.any { item -> item.index == index }
}
