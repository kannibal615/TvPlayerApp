package com.smartvision.svplayer.data.playlist

import android.content.Context
import android.util.Log
import android.util.Xml
import java.io.File
import java.io.Reader
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser

class EpgRepository(
    context: Context,
    private val okHttpClient: OkHttpClient,
) {
    private val cacheFile = File(context.filesDir, "smartvision_epg_cache.tsv")
    @Volatile private var cache: Map<String, List<EpgProgram>> = emptyMap()
    @Volatile private var cacheLoaded: Boolean = false

    init {
        deleteLegacySharedPreferenceCache(context)
    }

    suspend fun synchronize(url: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            if (url.isBlank()) return@runCatching 0
            logMemory("epg_sync_start")
            val request = Request.Builder().url(url).header("Accept", "application/xml,text/xml,*/*").build()
            val parsed = okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("URL EPG indisponible (${response.code}).")
                val body = response.body ?: error("URL EPG vide.")
                body.charStream().use { parseXmltv(it) }
            }
            cache = withLookupAliases(parsed)
            cacheLoaded = true
            persist(parsed)
            parsed.values.sumOf { it.size }.also { count ->
                logMemory("epg_sync_success", "channels=${parsed.size} programs=$count")
            }
        }.onFailure { error ->
            if (error is OutOfMemoryError) {
                cache = emptyMap()
                cacheLoaded = true
                runCatching { cacheFile.delete() }
            }
            logMemory("epg_sync_error", "error=${error.javaClass.simpleName}")
        }
    }

    fun loadPrograms(channelId: String?, channelName: String): List<EpgProgram> {
        ensureCacheLoaded()
        val keys = listOfNotNull(channelId, channelName)
            .flatMap { key -> listOf(key, normalizeKey(key)) }
            .filter { it.isNotBlank() }
        return keys.asSequence()
            .mapNotNull { cache[it] }
            .firstOrNull()
            .orEmpty()
            .sortedBy { it.startMillis ?: Long.MAX_VALUE }
    }

    fun hasPrograms(channelId: String?, channelName: String): Boolean {
        ensureCacheLoaded()
        return listOfNotNull(channelId, channelName)
            .flatMap { key -> listOf(key, normalizeKey(key)) }
            .filter { it.isNotBlank() }
            .any { key -> cache[key]?.isNotEmpty() == true }
    }

    private fun parseXmltv(reader: Reader): Map<String, List<EpgProgram>> {
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
                val items = channel.takeIf { it.isNotBlank() }?.let { programs.getOrPut(it) { mutableListOf() } }
                if (items == null ||
                    items.size >= MaxProgramsPerChannel ||
                    programs.size > MaxCachedChannels ||
                    totalPrograms >= MaxCachedPrograms
                ) {
                    skipCurrentTag(parser)
                } else {
                    val program = readProgramme(parser, channel, start, stop)
                    if (program.title.isNotBlank()) {
                        items += program
                        totalPrograms++
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
        return EpgProgram(
            channelId = channel,
            title = title,
            description = description,
            startLabel = start?.toDisplayTime().orEmpty(),
            stopLabel = stop?.toDisplayTime().orEmpty(),
            startMillis = start?.toEpochMillis(),
            stopMillis = stop?.toEpochMillis(),
        )
    }

    private fun persist(programs: Map<String, List<EpgProgram>>) {
        cacheFile.parentFile?.mkdirs()
        cacheFile.bufferedWriter().use { writer ->
            programs.entries
                .asSequence()
                .take(MaxCachedChannels)
                .forEach { (channel, items) ->
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

    private fun ensureCacheLoaded() {
        if (cacheLoaded) return
        synchronized(this) {
            if (cacheLoaded) return
            val loaded = loadCache()
            cache = withLookupAliases(loaded)
            cacheLoaded = true
        }
    }

    private fun loadCache(): Map<String, List<EpgProgram>> {
        if (!cacheFile.exists()) return emptyMap()
        val programs = linkedMapOf<String, MutableList<EpgProgram>>()
        cacheFile.useLines { lines ->
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

    private fun withLookupAliases(programs: Map<String, List<EpgProgram>>): Map<String, List<EpgProgram>> {
        if (programs.isEmpty()) return emptyMap()
        val indexed = LinkedHashMap<String, List<EpgProgram>>(programs.size * 2)
        programs.forEach { (channel, items) ->
            indexed[channel] = items
            normalizeKey(channel).takeIf { it.isNotBlank() }?.let { indexed[it] = items }
        }
        return indexed
    }

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
        Log.i(
            "SVEpgMemory",
            "stage=$stage usedMb=$usedMb freeMb=$freeMb totalMb=$totalMb maxMb=$maxMb $details".trim(),
        )
    }

    private fun deleteLegacySharedPreferenceCache(context: Context) {
        runCatching {
            File(context.applicationInfo.dataDir, "shared_prefs/smartvision_epg_cache.xml").delete()
        }
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
}

private fun String.toEpochMillis(): Long? =
    runCatching {
        XmltvDateFormat.parse(take(14))?.time
    }.getOrNull()

private fun String.toDisplayTime(): String =
    toEpochMillis()?.let { DisplayTimeFormat.format(it) }.orEmpty()

private fun normalizeKey(value: String): String =
    value.lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
        .replace(Regex("[^a-z0-9]+"), "")

private fun String.toCacheField(): String =
    replace("\t", " ")
        .replace("\n", " ")
        .replace("\r", " ")

private val XmltvDateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
private val DisplayTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
