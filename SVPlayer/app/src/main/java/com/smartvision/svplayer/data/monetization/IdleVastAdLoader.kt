package com.smartvision.svplayer.data.monetization

import java.io.StringReader
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource

data class IdleVastCreative(
    val mediaUrl: String,
    val impressionUrls: List<String>,
    val trackingUrls: Map<String, List<String>>,
)

class IdleVastAdLoader(
    private val client: OkHttpClient,
) {
    suspend fun load(tagUrl: String): IdleVastCreative? = withContext(Dispatchers.IO) {
        loadRecursive(tagUrl, depth = 0)
    }

    suspend fun ping(urls: List<String>) = withContext(Dispatchers.IO) {
        urls.distinct().forEach { rawUrl ->
            runCatching {
                client.newCall(
                    Request.Builder()
                        .url(rawUrl.withCacheBuster())
                        .header("User-Agent", SMARTVISION_AD_USER_AGENT)
                        .get()
                        .build(),
                )
                    .execute()
                    .use { }
            }
        }
    }

    private fun loadRecursive(tagUrl: String, depth: Int): IdleVastCreative? {
        if (depth > 3) return null
        val xml = client.newCall(
            Request.Builder()
                .url(tagUrl.withCacheBuster())
                .header("User-Agent", SMARTVISION_AD_USER_AGENT)
                .header("Accept", "application/xml,text/xml,*/*")
                .get()
                .build(),
        ).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.string().orEmpty()
        }
        if (xml.isBlank()) return null

        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(InputSource(StringReader(xml)))

        val wrapperUrl = document.elements("VASTAdTagURI").firstOrNull()?.textContent?.trim().orEmpty()
        if (wrapperUrl.isNotBlank()) {
            val wrapped = loadRecursive(wrapperUrl, depth + 1) ?: return null
            return wrapped.copy(
                impressionUrls = document.elements("Impression").texts() + wrapped.impressionUrls,
                trackingUrls = mergeTracking(document.trackingUrls(), wrapped.trackingUrls),
            )
        }

        val mediaUrl = document.elements("MediaFile")
            .filterIsInstance<Element>()
            .filter { it.getAttribute("delivery").isBlank() || it.getAttribute("delivery") == "progressive" }
            .sortedBy { if (it.getAttribute("type").equals("video/mp4", ignoreCase = true)) 0 else 1 }
            .firstOrNull {
                it.getAttribute("type").equals("video/mp4", ignoreCase = true) ||
                    it.getAttribute("type").equals("video/webm", ignoreCase = true)
            }
            ?.textContent
            ?.trim()
            .orEmpty()
        if (mediaUrl.isBlank()) return null

        return IdleVastCreative(
            mediaUrl = mediaUrl,
            impressionUrls = document.elements("Impression").texts(),
            trackingUrls = document.trackingUrls(),
        )
    }
}

private fun org.w3c.dom.Document.elements(localName: String): List<Node> {
    val nodes = getElementsByTagNameNS("*", localName)
    return buildList(nodes.length) {
        for (index in 0 until nodes.length) add(nodes.item(index))
    }
}

private fun List<Node>.texts(): List<String> =
    mapNotNull { it.textContent?.trim()?.takeIf(String::isNotBlank) }

private fun org.w3c.dom.Document.trackingUrls(): Map<String, List<String>> =
    elements("Tracking")
        .filterIsInstance<Element>()
        .mapNotNull { element ->
            val event = element.getAttribute("event").trim()
            val url = element.textContent?.trim().orEmpty()
            if (event.isBlank() || url.isBlank()) null else event to url
        }
        .groupBy({ it.first }, { it.second })

private fun mergeTracking(
    first: Map<String, List<String>>,
    second: Map<String, List<String>>,
): Map<String, List<String>> =
    (first.keys + second.keys).associateWith { key ->
        first[key].orEmpty() + second[key].orEmpty()
    }

private fun String.withCacheBuster(): String {
    val value = UUID.randomUUID().toString().replace("-", "").take(8)
    return replace("[CACHEBUSTING]", value, ignoreCase = true)
        .replace("[CACHE_BUSTING]", value, ignoreCase = true)
        .replace("%%CACHEBUSTER%%", value, ignoreCase = true)
}
