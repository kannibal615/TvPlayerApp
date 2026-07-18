package com.smartvision.svplayer.domain.profile

import java.util.Locale

data class ContentPrefixOption(
    val code: String,
    val englishLabel: String,
    val frenchLabel: String,
) {
    fun displayLabel(locale: Locale = Locale.getDefault()): String =
        if (locale.language == Locale.FRENCH.language) frenchLabel else englishLabel
}

object ContentPrefixPolicy {
    private val delimitedPrefix = Regex(
        pattern = """^\s*(?:\[([A-Z]{1,3}|[a-z])]\s*|([A-Z]{2,3})\s*(?:-|:|\|)\s*)""",
    )

    val predefinedOptions: List<ContentPrefixOption> = listOf(
        ContentPrefixOption("FR", "French / France", "Français / France"),
        ContentPrefixOption("AR", "Arabic", "Arabe"),
        ContentPrefixOption("EN", "English", "Anglais"),
        ContentPrefixOption("UK", "United Kingdom", "Royaume-Uni"),
        ContentPrefixOption("US", "United States", "États-Unis"),
        ContentPrefixOption("ES", "Spanish / Spain", "Espagnol / Espagne"),
        ContentPrefixOption("PT", "Portuguese / Portugal", "Portugais / Portugal"),
        ContentPrefixOption("DE", "German / Germany", "Allemand / Allemagne"),
        ContentPrefixOption("IT", "Italian / Italy", "Italien / Italie"),
        ContentPrefixOption("NL", "Dutch / Netherlands", "Néerlandais / Pays-Bas"),
        ContentPrefixOption("BE", "Belgium", "Belgique"),
        ContentPrefixOption("TR", "Turkish / Türkiye", "Turc / Turquie"),
        ContentPrefixOption("PL", "Polish / Poland", "Polonais / Pologne"),
        ContentPrefixOption("RO", "Romanian / Romania", "Roumain / Roumanie"),
        ContentPrefixOption("GR", "Greek / Greece", "Grec / Grèce"),
        ContentPrefixOption("RU", "Russian / Russia", "Russe / Russie"),
        ContentPrefixOption("IN", "India", "Inde"),
        ContentPrefixOption("PK", "Pakistan", "Pakistan"),
        ContentPrefixOption("AF", "Africa", "Afrique"),
        ContentPrefixOption("EU", "Europe", "Europe"),
        ContentPrefixOption("AS", "Asia", "Asie"),
        ContentPrefixOption("X", "X", "X"),
    )

    fun detect(title: String?): String? {
        val match = delimitedPrefix.find(title.orEmpty()) ?: return null
        return (match.groups[1]?.value ?: match.groups[2]?.value)
            ?.uppercase(Locale.ROOT)
            ?.takeIf { it.length in 1..3 }
    }

    fun normalizeCode(code: String): String? =
        code.trim().uppercase(Locale.ROOT).takeIf { normalized ->
            normalized.length in 1..3 && normalized.all(Char::isLetter)
        }

    fun normalize(codes: Iterable<String>): Set<String> =
        codes.mapNotNull(::normalizeCode).toSortedSet()

    fun accepts(title: String?, selectedCodes: Set<String>): Boolean {
        val normalizedSelection = normalize(selectedCodes)
        if (normalizedSelection.isEmpty()) return true
        val detectedCode = detect(title) ?: return false
        return detectedCode in normalizedSelection
    }

    fun optionsWithDetected(
        detectedCodes: Set<String>,
        selectedCodes: Set<String> = emptySet(),
        locale: Locale = Locale.getDefault(),
    ): List<ContentPrefixOption> {
        val predefinedByCode = predefinedOptions.associateBy(ContentPrefixOption::code)
        val normalizedSelection = normalize(selectedCodes)
        return (predefinedOptions + normalize(detectedCodes + selectedCodes)
            .filterNot(predefinedByCode::containsKey)
            .map { code -> ContentPrefixOption(code, code, code) })
            .distinctBy(ContentPrefixOption::code)
            .sortedWith(
                compareByDescending<ContentPrefixOption> { it.code in normalizedSelection }
                    .thenBy { it.displayLabel(locale) }
                    .thenBy { it.code },
            )
    }
}
