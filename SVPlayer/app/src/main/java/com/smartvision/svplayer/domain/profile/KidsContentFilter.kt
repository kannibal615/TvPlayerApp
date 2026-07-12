package com.smartvision.svplayer.domain.profile

import java.text.Normalizer
import java.util.Locale

class KidsContentFilter {
    fun isAllowed(vararg values: String?): Boolean {
        val normalized = values.filterNotNull().joinToString(" ") { normalize(it) }
        if (normalized.isBlank()) return false
        if (EXCLUSION_TERMS.any { normalized.containsTerm(it) }) return false
        if (STRONG_TERMS.any { normalized.containsTerm(it) }) return true
        val mediumMatches = MEDIUM_TERMS.count { normalized.containsTerm(it) }
        val ageMatch = AGE_TERMS.any { normalized.containsTerm(it) }
        return ageMatch || mediumMatches >= 2
    }

    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
        .lowercase(Locale.ROOT)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    private fun String.containsTerm(term: String): Boolean =
        this == term || startsWith("$term ") || endsWith(" $term") || contains(" $term ")

    companion object {
        val STRONG_TERMS = setOf(
            "kids", "kid", "children", "child", "junior", "preschool",
            "enfant", "enfants", "jeunesse", "dessin anime", "dessins animes",
            "bambini", "bambino", "ragazzi", "ninos", "nino", "infantil",
            "kinder", "cocuk", "atfal", "اطفال", "أطفال", "صغار",
            "كرتون", "رسوم متحركة",
        )

        val MEDIUM_TERMS = setOf(
            "cartoon", "cartoons", "animation", "animated", "family", "family kids",
            "familial", "famille", "toy", "toys", "school", "educational",
        )

        val AGE_TERMS = setOf(
            "all ages", "tout public", "tv y", "tv y7", "g rated", "u rated", "0+", "3+", "6+", "7+",
        )

        val EXCLUSION_TERMS = setOf(
            "adult", "adults", "adulte", "adultes",
            "sex", "sexe", "sexual", "sexuel", "sexuelle",
            "porn", "porno", "pornography", "pornographie", "xxx", "xx",
            "erotic", "erotique", "hentai",
            "crime", "crimes", "criminal", "criminel",
            "horror", "horreur", "gore",
            "war", "warfare", "guerre",
            "violence", "violent", "violente", "violence extreme", "horror gore",
            "casino", "gambling", "18+", "rated r", "tv ma",
        )
    }
}
