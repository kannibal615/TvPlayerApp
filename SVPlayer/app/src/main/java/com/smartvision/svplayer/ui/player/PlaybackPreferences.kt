@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.smartvision.svplayer.ui.player

import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.ui.AspectRatioFrameLayout

internal data class PlaybackBufferConfig(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int,
    val stalledPlaybackTimeoutMs: Long,
)

internal fun playbackBufferConfig(mode: String): PlaybackBufferConfig =
    when (mode.trim().lowercase()) {
        "low latency", "low_latency", "lowlatency" -> PlaybackBufferConfig(
            minBufferMs = 5_000,
            maxBufferMs = 20_000,
            bufferForPlaybackMs = 1_000,
            bufferForPlaybackAfterRebufferMs = 2_000,
            stalledPlaybackTimeoutMs = 10_000L,
        )
        "stable" -> PlaybackBufferConfig(
            minBufferMs = 30_000,
            maxBufferMs = 90_000,
            bufferForPlaybackMs = 5_000,
            bufferForPlaybackAfterRebufferMs = 10_000,
            stalledPlaybackTimeoutMs = 25_000L,
        )
        else -> PlaybackBufferConfig(
            minBufferMs = 15_000,
            maxBufferMs = 50_000,
            bufferForPlaybackMs = 2_500,
            bufferForPlaybackAfterRebufferMs = 5_000,
            stalledPlaybackTimeoutMs = 12_000L,
        )
    }

internal fun buildPlaybackLoadControl(mode: String): DefaultLoadControl {
    val config = playbackBufferConfig(mode)
    return DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            config.minBufferMs,
            config.maxBufferMs,
            config.bufferForPlaybackMs,
            config.bufferForPlaybackAfterRebufferMs,
        )
        .build()
}

internal fun videoResizeMode(videoRatio: String): Int =
    when (videoRatio.trim().lowercase()) {
        "fill" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        "zoom" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }
