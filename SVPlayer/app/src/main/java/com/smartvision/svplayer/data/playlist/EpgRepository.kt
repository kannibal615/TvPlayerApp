package com.smartvision.svplayer.data.playlist

import android.content.Context
import android.util.Xml
import java.io.StringReader
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
    private val preferences = context.getSharedPreferences("smartvision_epg_cache", Context.MODE_PRIVATE)
    @Volatile private var cache: Map<String, List<EpgProgram>> = loadCache()

    suspend fun synchronize(url: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            if (url.isBlank()) return@runCatching 0
            val request = Request.Builder().url(url).header("Accept", "application/xml,text/xml,*/*").build()
            val xml = okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("URL EPG indisponible (${response.code}).")
                response.body?.string().orEmpty()
            }
            val parsed = parseXmltv(xml)
            cache = parsed
            persist(parsed)
            parsed.values.sumOf { it.size }
        }
    }

    fun loadPrograms(channelId: String?, channelName: String): List<EpgProgram> {
        val keys = listOfNotNull(channelId, channelName)
            .flatMap { key -> listOf(key, normalizeKey(key)) }
            .filter { it.isNotBlank() }
        return keys.asSequence()
            .mapNotNull { cache[it] }
            .firstOrNull()
            .orEmpty()
            .sortedBy { it.startMillis ?: Long.MAX_VALUE }
    }

    private fun parseXmltv(xml: String): Map<String, List<EpgProgram>> {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))
        val programs = mutableMapOf<String, MutableList<EpgProgram>>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "programme") {
                val channel = parser.getAttributeValue(null, "channel").orEmpty()
                val start = parser.getAttributeValue(null, "start")
                val stop = parser.getAttributeValue(null, "stop")
                val program = readProgramme(parser, channel, start, stop)
                if (channel.isNotBlank() && program.title.isNotBlank()) {
                    programs.getOrPut(channel) { mutableListOf() } += program
                    programs.getOrPut(normalizeKey(channel)) { mutableListOf() } += program
                }
            }
            event = parser.next()
        }
        return programs
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
        val lines = programs.entries.flatMap { (channel, items) ->
            items.take(MaxProgramsPerChannel).map { program ->
                listOf(
                    channel,
                    program.title,
                    program.description,
                    program.startLabel,
                    program.stopLabel,
                    program.startMillis?.toString().orEmpty(),
                    program.stopMillis?.toString().orEmpty(),
                ).joinToString("\t") { it.replace("\t", " ").replace("\n", " ") }
            }
        }
        preferences.edit().putString(KEY_PROGRAMS, lines.joinToString("\n")).apply()
    }

    private fun loadCache(): Map<String, List<EpgProgram>> =
        preferences.getString(KEY_PROGRAMS, "").orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size < 7) return@mapNotNull null
                parts[0] to EpgProgram(
                    channelId = parts[0],
                    title = parts[1],
                    description = parts[2],
                    startLabel = parts[3],
                    stopLabel = parts[4],
                    startMillis = parts[5].toLongOrNull(),
                    stopMillis = parts[6].toLongOrNull(),
                )
            }
            .groupBy({ it.first }, { it.second })

    private companion object {
        const val KEY_PROGRAMS = "programs"
        const val MaxProgramsPerChannel = 30
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

private val XmltvDateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
private val DisplayTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
