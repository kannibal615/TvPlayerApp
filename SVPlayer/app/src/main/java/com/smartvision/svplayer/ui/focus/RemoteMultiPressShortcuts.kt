package com.smartvision.svplayer.ui.focus

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Modifier.remoteMultiPressShortcuts(
    enabled: Boolean = true,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    onHeader: () -> Unit,
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val state = remember { RemoteMultiPressState() }
    onPreviewKeyEvent { event ->
        if (!enabled || event.type != KeyEventType.KeyDown || event.nativeKeyEvent.repeatCount > 0) {
            return@onPreviewKeyEvent false
        }
        val direction = when (event.key) {
            Key.DirectionUp -> RemoteDirection.Up
            Key.DirectionDown -> RemoteDirection.Down
            else -> {
                state.reset()
                return@onPreviewKeyEvent false
            }
        }
        val now = SystemClock.elapsedRealtime()
        state.count = if (state.direction == direction && now - state.lastPressAt <= MultiPressWindowMs) {
            state.count + 1
        } else {
            1
        }
        state.direction = direction
        state.lastPressAt = now
        state.pending?.cancel()
        if (state.count >= 2) {
            state.pending = scope.launch {
                delay(MultiPressWindowMs)
                val count = state.count
                val resolvedDirection = state.direction
                state.reset(cancelPending = false)
                when {
                    resolvedDirection == RemoteDirection.Up && count >= 3 -> onHeader()
                    resolvedDirection == RemoteDirection.Up -> onStart()
                    resolvedDirection == RemoteDirection.Down -> onEnd()
                }
            }
            true
        } else {
            false
        }
    }
}

private class RemoteMultiPressState {
    var direction: RemoteDirection? = null
    var count: Int = 0
    var lastPressAt: Long = 0L
    var pending: Job? = null

    fun reset(cancelPending: Boolean = true) {
        if (cancelPending) pending?.cancel()
        pending = null
        direction = null
        count = 0
        lastPressAt = 0L
    }
}

private enum class RemoteDirection { Up, Down }

private const val MultiPressWindowMs = 320L
