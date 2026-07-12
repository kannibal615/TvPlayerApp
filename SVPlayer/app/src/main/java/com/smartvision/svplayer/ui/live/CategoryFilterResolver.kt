package com.smartvision.svplayer.ui.live

import java.util.Locale

enum class CategoryFilterType { COUNTRY, REGION, LANGUAGE_GROUP, UNKNOWN }

data class CategoryFilterIdentity(
    val sourceCode: String,
    val normalizedCode: String,
    val displayName: String,
    val type: CategoryFilterType,
    val flagKey: String?,
    val priority: Int,
)

data class CategoryFilter(
    val identity: CategoryFilterIdentity,
    val categoryCount: Int,
)

object CategoryCodeParser {
    // A code is accepted only when it is explicitly delimited at the beginning.
    private val delimitedCode = Regex(
        """^\s*(?:[|\[(]\s*)?([A-Za-z]{2})(?=\s*[|\])]|\s+[-_|:]?\s*|\s*[-_:]\s*)""",
    )

    fun parse(categoryName: String): String? {
        val trimmed = categoryName.trim()
        if (trimmed.isEmpty()) return null
        val match = delimitedCode.find(trimmed) ?: return null
        val code = match.groupValues[1].uppercase(Locale.ROOT)
        val consumed = match.range.last + 1
        if (consumed >= trimmed.length) return null
        return code
    }
}

object CategoryFilterResolver {
    private data class Rule(
        val englishName: String,
        val frenchName: String,
        val type: CategoryFilterType,
        val flagKey: String?,
        val normalizedCode: String,
    )

    private val rules = mapOf(
        "AR" to Rule("Arab world", "Monde arabe", CategoryFilterType.LANGUAGE_GROUP, "ARAB", "AR"),
        "EU" to Rule("Europe", "Europe", CategoryFilterType.REGION, "EU", "EU"),
        "AS" to Rule("Asia", "Asie", CategoryFilterType.REGION, "ASIA", "AS"),
        "AF" to Rule("Africa", "Afrique", CategoryFilterType.REGION, "AFRICA", "AF"),
        "UK" to Rule("United Kingdom", "Royaume-Uni", CategoryFilterType.COUNTRY, "GB", "GB"),
        "GB" to Rule("United Kingdom", "Royaume-Uni", CategoryFilterType.COUNTRY, "GB", "GB"),
    )

    fun resolve(sourceCode: String, locale: Locale = Locale.getDefault()): CategoryFilterIdentity {
        val code = sourceCode.trim().uppercase(Locale.ROOT)
        val custom = rules[code]
        if (custom != null) {
            return CategoryFilterIdentity(
                sourceCode = code,
                normalizedCode = custom.normalizedCode,
                displayName = if (locale.language == Locale.FRENCH.language) custom.frenchName else custom.englishName,
                type = custom.type,
                flagKey = custom.flagKey,
                priority = if (custom.type == CategoryFilterType.COUNTRY) 20 else 10,
            )
        }
        if (code in Locale.getISOCountries()) {
            val countryLocale = Locale.Builder().setRegion(code).build()
            return CategoryFilterIdentity(
                sourceCode = code,
                normalizedCode = code,
                displayName = countryLocale.getDisplayCountry(locale).ifBlank { code },
                type = CategoryFilterType.COUNTRY,
                flagKey = code,
                priority = 20,
            )
        }
        return CategoryFilterIdentity(code, code, code, CategoryFilterType.UNKNOWN, null, 30)
    }

    fun buildFilters(
        categories: List<LiveTvCategory>,
        locale: Locale = Locale.getDefault(),
    ): List<CategoryFilter> {
        val firstSeen = linkedMapOf<String, Pair<CategoryFilterIdentity, Int>>()
        categories.asSequence()
            .filterNot { it.id in SpecialLiveCategoryIds }
            .forEach { category ->
                val code = CategoryCodeParser.parse(category.label) ?: return@forEach
                val identity = resolve(code, locale)
                val current = firstSeen[identity.normalizedCode]
                firstSeen[identity.normalizedCode] = identity to ((current?.second ?: 0) + 1)
            }
        return firstSeen.values
            .map { (identity, count) -> CategoryFilter(identity, count) }
            .sortedBy { it.identity.priority }
    }

    fun filterCategories(categories: List<LiveTvCategory>, normalizedCode: String?): List<LiveTvCategory> {
        if (normalizedCode == null) return categories
        return categories.filter { category ->
            val code = CategoryCodeParser.parse(category.label) ?: return@filter false
            resolve(code, Locale.ROOT).normalizedCode == normalizedCode
        }
    }
}

object FlagResolver {
    fun visual(identity: CategoryFilterIdentity): String = when (identity.flagKey) {
        "ARAB" -> "☾"
        "EU" -> "🇪🇺"
        "ASIA" -> "🌏"
        "AFRICA" -> "🌍"
        null -> identity.sourceCode
        else -> unicodeFlag(identity.flagKey) ?: identity.sourceCode
    }

    private fun unicodeFlag(countryCode: String): String? {
        if (countryCode.length != 2 || countryCode.any { it !in 'A'..'Z' }) return null
        return countryCode.map { char -> Character.toChars(0x1F1E6 + (char - 'A')).concatToString() }
            .joinToString("")
    }
}
