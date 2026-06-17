package com.smartvision.svplayer.core.config

import com.smartvision.svplayer.BuildConfig

data class XtreamCredentials(
    val host: String,
    val username: String,
    val password: String,
) {
    val isConfigured: Boolean =
        host.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    val normalizedHost: String =
        host.trim().trimEnd('/')

    val retrofitBaseUrl: String =
        (normalizedHost.ifBlank { "http://127.0.0.1" }) + "/"

    val maskedUsername: String =
        when {
            username.length <= 4 -> "****"
            else -> username.take(2) + "****" + username.takeLast(2)
        }
}

interface XtreamCredentialsProvider {
    fun current(): XtreamCredentials
}

class BuildConfigXtreamCredentialsProvider : XtreamCredentialsProvider {
    override fun current(): XtreamCredentials =
        XtreamCredentials(
            host = BuildConfig.XTREAM_HOST,
            username = BuildConfig.XTREAM_USERNAME,
            password = BuildConfig.XTREAM_PASSWORD,
        )
}
