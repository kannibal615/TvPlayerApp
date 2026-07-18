@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.smartvision.svplayer.ui.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class LivePlaybackSession(context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
        playWhenReady = true
    }

    var streamId: Int? = null
        private set
    var primaryUrl: String = ""
        private set

    fun playPreview(streamId: Int, url: String) {
        if (this.streamId == streamId && primaryUrl == url && player.mediaItemCount > 0) {
            player.playWhenReady = true
            return
        }
        this.streamId = streamId
        primaryUrl = url
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    fun matches(streamId: Int, url: String): Boolean =
        this.streamId == streamId && primaryUrl == url && player.mediaItemCount > 0

    fun playFallback(streamId: Int, primaryUrl: String, fallbackUrl: String) {
        this.streamId = streamId
        this.primaryUrl = primaryUrl
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(fallbackUrl))
        player.prepare()
        player.playWhenReady = true
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        streamId = null
        primaryUrl = ""
    }

    fun release() {
        player.release()
    }
}
