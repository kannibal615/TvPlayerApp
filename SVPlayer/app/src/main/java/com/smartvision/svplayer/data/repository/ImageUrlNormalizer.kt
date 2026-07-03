package com.smartvision.svplayer.data.repository

internal fun normalizeCatalogImageUrl(rawUrl: String?, baseHost: String?): String? {
    val raw = rawUrl
        ?.trim()
        ?.replace("\\/", "/")
        ?.replace(" ", "%20")
        ?.takeIf { it.isNotBlank() }
        ?: return null
    if (raw.equals("null", ignoreCase = true) ||
        raw.equals("n/a", ignoreCase = true) ||
        raw.equals("none", ignoreCase = true) ||
        raw == "0"
    ) {
        return null
    }

    val base = baseHost
        ?.trim()
        ?.trimEnd('/')
        ?.takeIf { it.isNotBlank() }
        ?.let { if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it" }

    return when {
        raw.startsWith("http://") || raw.startsWith("https://") -> raw
        raw.startsWith("//") -> {
            val scheme = base?.substringBefore("://", missingDelimiterValue = "http") ?: "http"
            "$scheme:$raw"
        }
        raw.startsWith("/") && base != null -> base + raw
        raw.startsWith("www.") -> "http://$raw"
        base != null -> "$base/${raw.trimStart('/')}"
        else -> raw
    }
}
