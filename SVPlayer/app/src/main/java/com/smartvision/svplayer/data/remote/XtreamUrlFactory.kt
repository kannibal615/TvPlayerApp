package com.smartvision.svplayer.data.remote

import com.smartvision.svplayer.core.config.XtreamCredentialsProvider

class XtreamUrlFactory(
    private val credentialsProvider: XtreamCredentialsProvider,
) {
    fun live(streamId: Int): String {
        val credentials = credentialsProvider.current()
        return "${credentials.normalizedHost}/live/${credentials.username}/${credentials.password}/$streamId.ts"
    }

    fun liveHls(streamId: Int): String {
        val credentials = credentialsProvider.current()
        return "${credentials.normalizedHost}/live/${credentials.username}/${credentials.password}/$streamId.m3u8"
    }

    fun movie(streamId: Int, extension: String): String {
        val credentials = credentialsProvider.current()
        return "${credentials.normalizedHost}/movie/${credentials.username}/${credentials.password}/$streamId.${extension.ifBlank { "mp4" }}"
    }

    fun episode(episodeId: Int, extension: String): String {
        val credentials = credentialsProvider.current()
        return "${credentials.normalizedHost}/series/${credentials.username}/${credentials.password}/$episodeId.${extension.ifBlank { "mp4" }}"
    }
}
