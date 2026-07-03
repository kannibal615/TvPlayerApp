package com.smartvision.svplayer.data.playlist

import com.smartvision.svplayer.data.local.entity.CategoryEntity
import com.smartvision.svplayer.data.local.entity.LiveStreamEntity
import com.smartvision.svplayer.domain.model.MediaSection
import java.util.Locale
import java.util.zip.CRC32
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

class M3uPlaylistClient(
    private val okHttpClient: OkHttpClient,
) {
    suspend fun fetch(url: String): M3uPlaylist = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "audio/x-mpegurl,application/vnd.apple.mpegurl,text/plain,*/*")
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Lien M3U indisponible (${response.code}).")
            }
            parse(response.body?.string().orEmpty(), url)
        }
    }

    fun parse(content: String, baseUrl: String? = null): M3uPlaylist {
        val channels = mutableListOf<M3uChannel>()
        var pendingInfo: ExtInf? = null
        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                when {
                    line.startsWith("#EXTINF", ignoreCase = true) -> {
                        pendingInfo = parseExtInf(line)
                    }
                    !line.startsWith("#") -> {
                        val info = pendingInfo
                        pendingInfo = null
                        if (info != null && line.startsWith("http", ignoreCase = true)) {
                            channels += M3uChannel(
                                id = stableId("${info.tvgId}|${info.name}|$line"),
                                number = channels.size + 1,
                                name = info.name.ifBlank { "Chaine ${channels.size + 1}" },
                                group = info.group.ifBlank { "M3U" },
                                logoUrl = normalizeM3uLogoUrl(info.logoUrl, baseUrl),
                                epgChannelId = info.tvgId.ifBlank { info.name },
                                streamUrl = line,
                            )
                        }
                    }
                }
            }
        return M3uPlaylist(channels)
    }

    private fun parseExtInf(line: String): ExtInf {
        val attributes = AttributeRegex.findAll(line).associate {
            it.groupValues[1].lowercase(Locale.ROOT) to it.groupValues[2].trim()
        }
        val fallbackName = line.substringAfter(",", "").trim()
        return ExtInf(
            tvgId = attributes["tvg-id"].orEmpty(),
            name = attributes["tvg-name"].orEmpty().ifBlank { fallbackName },
            group = attributes["group-title"].orEmpty(),
            logoUrl = attributes["tvg-logo"]?.takeIf { it.isNotBlank() },
        )
    }

    private fun stableId(value: String): Int {
        val crc = CRC32()
        crc.update(value.toByteArray(Charsets.UTF_8))
        return (crc.value and 0x7FFFFFFF).toInt().takeIf { it != 0 } ?: 1
    }

    private fun normalizeM3uLogoUrl(rawUrl: String?, baseUrl: String?): String? {
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
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw

        val base = baseUrl?.toHttpUrlOrNull() ?: return when {
            raw.startsWith("//") -> "http:$raw"
            raw.startsWith("www.") -> "http://$raw"
            else -> raw
        }
        return base.resolve(raw)?.toString() ?: raw
    }

    private companion object {
        val AttributeRegex = Regex("([A-Za-z0-9_-]+)=\"([^\"]*)\"")
    }
}

data class M3uPlaylist(
    val channels: List<M3uChannel>,
) {
    val categories: List<CategoryEntity> =
        channels.groupBy { it.categoryId }
            .map { (id, items) -> CategoryEntity(id = id, type = MediaSection.Live.storageName, name = items.first().group) }
}

data class M3uChannel(
    val id: Int,
    val number: Int,
    val name: String,
    val group: String,
    val logoUrl: String?,
    val epgChannelId: String,
    val streamUrl: String,
) {
    val categoryId: String = "m3u_" + group.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_").trim('_')
        .ifBlank { "general" }

    fun toEntity(): LiveStreamEntity =
        LiveStreamEntity(
            streamId = id,
            number = number,
            name = name,
            categoryId = categoryId,
            logoUrl = logoUrl,
            epgChannelId = epgChannelId,
            directStreamUrl = streamUrl,
            source = "m3u",
        )
}

private data class ExtInf(
    val tvgId: String,
    val name: String,
    val group: String,
    val logoUrl: String?,
)
