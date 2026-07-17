package com.smartvision.svplayer.data.playlist

import android.content.Context
import android.util.Log
import android.util.Xml
import com.smartvision.svplayer.data.network.NetworkActivityTracker
import com.smartvision.svplayer.data.network.NetworkActivityType
import java.io.File
import java.io.Reader
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser

class EpgRepository(
    context: Context,
    private val okHttpClient: OkHttpClient,
    private val networkActivityTracker: NetworkActivityTracker,
) {
    private val cacheDirectory = File(context.filesDir, "smartvision_epg")
    private val caches = ConcurrentHashMap<String, Map<String, List<EpgProgram>>>()
    private val loadedSources = ConcurrentHashMap.newKeySet<String>()

    init {
        deleteLegacySharedCache(context)
    }

    suspend fun synchronize(url: String): Result<Int> = withContext(Dispatchers.IO) {
        val sourceKey = url.sourceKey()
        val work = networkActivityTracker.begin(
            id = "epg-${System.currentTimeMillis()}",
            title = "EPG",
            type = NetworkActivityType.Epg,
            message = "Downloading EPG",
            source = "EPG",
            progressPercent = 0,
        )
        runCatching {
            if (url.isBlank()) {
                work.complete("No EPG URL")
                return@runCatching 0
            }
            logMemory("epg_sync_start")
            val request = Request.Builder().url(url).header("Accept", "application/xml,text/xml,*/*").build()
            val parsed = okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("URL EPG indisponible (${response.code}).")
                val body = response.body ?: error("URL EPG vide.")
                work.update(
                    status = com.smartvision.svplayer.data.network.NetworkActivityStatus.Importing,
                    message = "Parsing EPG",
                    progressPercent = 75,
                )
                body.charStream().use { parseXmltv(it, System.currentTimeMillis()) }
            }
            caches[sourceKey] = withLookupAliases(parsed)
            loadedSources += sourceKey
            persist(sourceKey, parsed)
            markSynchronized(sourceKey)
            parsed.values.sumOf { it.size }.also { count ->
                logMemory("epg_sync_success", "channels=${parsed.size} programs=$count")
                work.update(currentItems = count, progressPercent = 100)
                work.complete("EPG ready")
            }
        }.onFailure { error ->
            if (error is OutOfMemoryError) {
                caches.remove(sourceKey)
                loadedSources += sourceKey
                runCatching { cacheFile(sourceKey).delete() }
            }
            logMemory("epg_sync_error", "error=${error.javaClass.simpleName}")
            work.fail(error.message ?: error.javaClass.simpleName)
        }
    }

    suspend fun synchronizeIfStale(url: String, minAgeMs: Long): Result<Int> = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext Result.success(0)
        val sourceKey = url.sourceKey()
        if (isFresh(sourceKey, minAgeMs)) return@withContext Result.success(0)
        synchronize(url)
    }

    fun loadPrograms(url: String, channelId: String?, channelName: String): List<EpgProgram> {
        if (url.isBlank()) return emptyList()
        val sourceKey = url.sourceKey()
        ensureCacheLoaded(sourceKey)
        val keys = listOfNotNull(channelId, channelName)
            .flatMap { key -> listOf(key, normalizeKey(key)) }
            .filter { it.isNotBlank() }
        val programs = keys.asSequence()
            .mapNotNull { caches[sourceKey]?.get(it) }
            .firstOrNull()
            .orEmpty()
        return currentAndUpcomingPrograms(programs, System.currentTimeMillis())
    }

    fun hasPrograms(url: String, channelId: String?, channelName: String): Boolean =
        loadPrograms(url, channelId, channelName).isNotEmpty()

    private fun parseXmltv(reader: Reader, nowMillis: Long): Map<String, List<EpgProgram>> {
        val parser = Xml.newPullParser()
        parser.setInput(reader)
        val programs = linkedMapOf<String, MutableList<EpgProgram>>()
        var totalPrograms = 0
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "programme") {
                val channel = parser.getAttributeValue(null, "channel").orEmpty()
                val start = parser.getAttributeValue(null, "start")
                val stop = parser.getAttributeValue(null, "stop")
                if (channel.isBlank() || (channel !in programs && programs.size >= MaxCachedChannels)) {
                    skipCurrentTag(parser)
                } else {
                    val program = readProgramme(parser, channel, start, stop)
                    if (program.title.isNotBlank() && program.isCurrentOrUpcoming(nowMillis)) {
                        val items = programs.getOrPut(channel) { mutableListOf() }
                        items += program
                        items.sortBy { it.startMillis }
                        if (items.size > MaxProgramsPerChannel) {
                            items.removeAt(items.lastIndex)
                        } else {
                            totalPrograms++
                        }
                        if (totalPrograms >= MaxCachedPrograms) break
                    }
                }
            }
            event = parser.next()
        }
        return programs.mapValues { (_, items) -> items.toList() }
    }

    private fun readProgramme(
        parser: XmlPullParser,
        channel: String,
        start: String?,
        stop: String?,
    ): EpgProgram {
        var title = ""
        var description = ""
        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "programme")) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "title" -> title = parser.nextText().trim()
                    "desc" -> description = parser.nextText().trim()
                }
            }
            event = parser.next()
        }
        val startMillis = parseXmltvEpochMillis(start)
        val stopMillis = parseXmltvEpochMillis(stop)
        return EpgProgram(
            channelId = channel,
            title = title,
            description = description,
            startLabel = startMillis?.toDisplayTime().orEmpty(),
            stopLabel = stopMillis?.toDisplayTime().orEmpty(),
            startMillis = startMillis,
            stopMillis = stopMillis,
        )
    }

    private fun persist(sourceKey: String, programs: Map<String, List<EpgProgram>>) {
        cacheDirectory.mkdirs()
        cacheFile(sourceKey).bufferedWriter().use { writer ->
            programs.entries.asSequence().take(MaxCachedChannels).forEach { (channel, items) ->
                items.take(MaxProgramsPerChannel).forEach { program ->
                    writer.appendLine(
                        listOf(
                            channel,
                            program.title,
                            program.description,
                            program.startLabel,
                            program.stopLabel,
                            program.startMillis?.toString().orEmpty(),
                            program.stopMillis?.toString().orEmpty(),
                        ).joinToString("\t") { it.toCacheField() },
                    )
                }
            }
        }
    }

    private fun ensureCacheLoaded(sourceKey: String) {
        if (sourceKey in loadedSources) return
        synchronized(loadedSources) {
            if (sourceKey in loadedSources) return
            caches[sourceKey] = withLookupAliases(loadCache(sourceKey))
            loadedSources += sourceKey
        }
    }

    private fun loadCache(sourceKey: String): Map<String, List<EpgProgram>> {
        val file = cacheFile(sourceKey)
        if (!file.exists()) return emptyMap()
        val programs = linkedMapOf<String, MutableList<EpgProgram>>()
        file.useLines { lines ->
            lines.take(MaxCachedPrograms).forEach { line ->
                val parts = line.split('\t', limit = 7)
                if (parts.size < 7) return@forEach
                val channel = parts[0].takeIf { it.isNotBlank() } ?: return@forEach
                val items = programs.getOrPut(channel) { mutableListOf() }
                if (items.size >= MaxProgramsPerChannel || programs.size > MaxCachedChannels) return@forEach
                items += EpgProgram(
                    channelId = channel,
                    title = parts[1],
                    description = parts[2],
                    startLabel = parts[3],
                    stopLabel = parts[4],
                    startMillis = parts[5].toLongOrNull(),
                    stopMillis = parts[6].toLongOrNull(),
                )
            }
        }
        return programs.mapValues { (_, items) -> items.toList() }
    }

    private fun isFresh(sourceKey: String, minAgeMs: Long): Boolean {
        val file = metadataFile(sourceKey)
        if (!file.exists()) return false
        val lastSuccessAt = file.readText().trim().toLongOrNull() ?: return false
        return System.currentTimeMillis() - lastSuccessAt < minAgeMs
    }

    private fun markSynchronized(sourceKey: String) {
        cacheDirectory.mkdirs()
        metadataFile(sourceKey).writeText(System.currentTimeMillis().toString())
    }

    private fun withLookupAliases(programs: Map<String, List<EpgProgram>>): Map<String, List<EpgProgram>> {
        if (programs.isEmpty()) return emptyMap()
        val indexed = LinkedHashMap<String, List<EpgProgram>>(programs.size * 2)
        programs.forEach { (channel, items) ->
            indexed[channel] = items
            normalizeKey(channel).takeIf { it.isNotBlank() }?.let { indexed[it] = items }
        }
        return indexed
    }

    private fun cacheFile(sourceKey: String) = File(cacheDirectory, "$sourceKey.tsv")
    private fun metadataFile(sourceKey: String) = File(cacheDirectory, "$sourceKey.meta")

    private fun skipCurrentTag(parser: XmlPullParser) {
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return
            }
        }
    }

    private fun logMemory(stage: String, details: String = "") {
        val runtime = Runtime.getRuntime()
        val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / BytesInMb
        val freeMb = runtime.freeMemory() / BytesInMb
        val totalMb = runtime.totalMemory() / BytesInMb
        val maxMb = runtime.maxMemory() / BytesInMb
        Log.i("SVEpgMemory", "stage=$stage usedMb=$usedMb freeMb=$freeMb totalMb=$totalMb maxMb=$maxMb $details".trim())
    }

    private fun deleteLegacySharedCache(context: Context) {
        runCatching { File(context.filesDir, "smartvision_epg_cache.tsv").delete() }
        runCatching { File(context.filesDir, "smartvision_epg_cache.meta").delete() }
        runCatching { File(context.applicationInfo.dataDir, "shared_prefs/smartvision_epg_cache.xml").delete() }
    }

    private companion object {
        const val BytesInMb = 1024L * 1024L
        const val MaxProgramsPerChannel = 8
        const val MaxCachedChannels = 8_000
        const val MaxCachedPrograms = 64_000
    }
}

data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String,
    val startLabel: String,
    val stopLabel: String,
    val startMillis: Long?,
    val stopMillis: Long?,
) {
    val timeRange: String
        get() = listOf(startLabel, stopLabel).filter { it.isNotBlank() }.joinToString(" - ").ifBlank { "EPG" }

    internal fun isCurrentOrUpcoming(nowMillis: Long): Boolean {
        val start = startMillis ?: return false
        return if (start > nowMillis) true else stopMillis?.let { it > nowMillis } == true
    }
}

internal fun currentAndUpcomingPrograms(programs: List<EpgProgram>, nowMillis: Long): List<EpgProgram> =
    programs.filter { it.isCurrentOrUpcoming(nowMillis) }.sortedBy { it.startMillis }

internal fun parseXmltvEpochMillis(value: String?): Long? {
    val normalized = value?.trim().orEmpty()
    if (normalized.length < 14) return null
    val dateTime = normalized.take(14)
    val offset = Regex("[+-]\\d{4}").find(normalized.drop(14))?.value
    return runCatching {
        if (offset != null) {
            OffsetDateTime.parse(dateTime + offset, XmltvOffsetFormatter).toInstant().toEpochMilli()
        } else {
            LocalDateTime.parse(dateTime, XmltvLocalFormatter).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }.getOrNull()
}

private fun Long.toDisplayTime(): String =
    java.time.Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(DisplayTimeFormatter)

private fun normalizeKey(value: String): String =
    value.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").replace(Regex("[^a-z0-9]+"), "")

private fun String.sourceKey(): String = MessageDigest.getInstance("SHA-256")
    .digest(trim().toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
    .take(24)

private fun String.toCacheField(): String = replace("\t", " ").replace("\n", " ").replace("\r", " ")

private val XmltvOffsetFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssxx", Locale.US)
private val XmltvLocalFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.US)
private val DisplayTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
