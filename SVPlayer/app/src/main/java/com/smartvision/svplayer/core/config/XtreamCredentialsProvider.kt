package com.smartvision.svplayer.core.config

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
