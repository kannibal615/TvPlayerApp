package com.smartvision.svplayer.data.tmdb

class TmdbImageResolver(
    private val secureBaseUrl: String = "https://image.tmdb.org/t/p/",
) {
    fun posterUrl(path: String?): String? = imageUrl(path, "w500")

    fun backdropUrl(path: String?): String? = imageUrl(path, "w780")

    fun logoUrl(path: String?): String? = imageUrl(path, "w500")

    private fun imageUrl(path: String?, size: String): String? {
        val normalized = path?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized
        }
        return secureBaseUrl.trimEnd('/') + "/" + size + "/" + normalized.trimStart('/')
    }
}
