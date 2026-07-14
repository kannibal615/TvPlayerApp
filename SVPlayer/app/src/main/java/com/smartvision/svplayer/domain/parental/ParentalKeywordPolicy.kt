package com.smartvision.svplayer.domain.parental

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

object ParentalKeywordPolicy {
    const val MaxSerializedLength = 500
    val DefaultKeywords = listOf("adults", "porn", "xxx")

    fun parseLegacy(value: String): List<String> = normalize(
        value.split(',', ';', '\n', '\r', '|'),
    )

    fun parseJsonOrLegacy(json: String?, legacy: String?): List<String> {
        val parsed = json
            ?.takeIf { it.isNotBlank() }
            ?.let { raw -> runCatching { parentalKeywordGson.fromJson<List<String>>(raw, parentalKeywordListType) }.getOrNull() }
        return normalize(parsed ?: parseLegacy(legacy.orEmpty().ifBlank { DefaultKeywords.joinToString("; ") }))
    }

    fun normalize(values: Iterable<String>): List<String> {
        val seen = linkedSetOf<String>()
        return values.mapNotNull { raw ->
            val clean = raw.trim().replace(Regex("\\s+"), " ")
            if (clean.isBlank()) return@mapNotNull null
            val key = clean.lowercase(Locale.ROOT)
            if (!seen.add(key)) return@mapNotNull null
            clean
        }
    }

    fun serialize(values: Iterable<String>): String = parentalKeywordGson.toJson(normalize(values))

    fun legacyValue(values: Iterable<String>): String = normalize(values).joinToString("; ")

    fun fitsStorage(values: Iterable<String>): Boolean = serialize(values).length <= MaxSerializedLength

    fun normalizedForMatching(values: Iterable<String>): List<String> = normalize(values).map { it.lowercase(Locale.ROOT) }
}

private val parentalKeywordGson = Gson()
private val parentalKeywordListType = object : TypeToken<List<String>>() {}.type
