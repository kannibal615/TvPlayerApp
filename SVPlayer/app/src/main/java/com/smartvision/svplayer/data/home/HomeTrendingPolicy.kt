package com.smartvision.svplayer.data.home

import java.text.Normalizer
import java.util.Locale

object HomeTrendingPolicy {
    const val SectionLimit = 10
    const val CandidateLimit = 120
    const val MinimumSeriesRating = 3f

    private val noveltyTerms = listOf(
        "new movie",
        "new movies",
        "new series",
        "new release",
        "new releases",
        "latest",
        "recently added",
        "nouveau film",
        "nouveaux films",
        "nouvelle serie",
        "nouvelles series",
        "nouveaute",
        "nouveautes",
        "recemment ajoute",
        "recemment ajoutes",
    )

    fun isNoveltyCategory(name: String): Boolean {
        val normalized = normalize(name)
        return noveltyTerms.any { term -> normalized.contains(term) }
    }

    fun containsAdultMarker(vararg values: String?): Boolean =
        values.filterNotNull()
            .map(::normalize)
            .any { AdultPattern.containsMatchIn(it) }

    fun <T> selectDeterministic(
        ratedCandidates: List<T>,
        newestCandidates: List<T>,
        idOf: (T) -> Int,
        ratingOf: (T) -> Float?,
        addedAtOf: (T) -> Long,
        yearOf: (T) -> Int?,
        allowed: (T) -> Boolean,
        limit: Int = SectionLimit,
    ): List<T> {
        if (limit <= 0) return emptyList()
        val rated = ratedCandidates
            .asSequence()
            .filter(allowed)
            .distinctBy(idOf)
            .filter { (ratingOf(it) ?: 0f).isFinite() && (ratingOf(it) ?: 0f) > 0f }
            .sortedWith(
                compareByDescending<T> { ratingOf(it) ?: 0f }
                    .thenByDescending(addedAtOf)
                    .thenByDescending { yearOf(it) ?: 0 }
                    .thenBy(idOf),
            )
            .take(limit)
            .toList()
        if (rated.size >= limit) return rated
        val usedIds = rated.mapTo(mutableSetOf(), idOf)
        val newest = newestCandidates
            .asSequence()
            .filter(allowed)
            .filterNot { idOf(it) in usedIds }
            .distinctBy(idOf)
            .sortedWith(
                compareByDescending<T>(addedAtOf)
                    .thenByDescending { yearOf(it) ?: 0 }
                    .thenBy(idOf),
            )
            .take(limit - rated.size)
            .toList()
        return rated + newest
    }

    fun <T> selectRecentRated(
        recentCandidates: List<T>,
        ratedCandidates: List<T>,
        idOf: (T) -> Int,
        ratingOf: (T) -> Float?,
        addedAtOf: (T) -> Long,
        yearOf: (T) -> Int?,
        allowed: (T) -> Boolean,
        minimumRating: Float = MinimumSeriesRating,
        limit: Int = SectionLimit,
    ): List<T> {
        if (limit <= 0) return emptyList()
        val comparator = compareByDescending<T> { ratingOf(it) ?: 0f }
            .thenByDescending(addedAtOf)
            .thenByDescending { yearOf(it) ?: 0 }
            .thenBy(idOf)
        val recent = recentCandidates
            .asSequence()
            .filter(allowed)
            .distinctBy(idOf)
            .filter { (ratingOf(it) ?: 0f).isFinite() && (ratingOf(it) ?: 0f) >= minimumRating }
            .sortedWith(comparator)
            .take(limit)
            .toList()
        if (recent.size >= limit) return recent
        val usedIds = recent.mapTo(mutableSetOf(), idOf)
        val bestRatedFallback = ratedCandidates
            .asSequence()
            .filter(allowed)
            .filterNot { idOf(it) in usedIds }
            .distinctBy(idOf)
            .filter { (ratingOf(it) ?: 0f).isFinite() && (ratingOf(it) ?: 0f) >= minimumRating }
            .sortedWith(comparator)
            .take(limit - recent.size)
            .toList()
        return recent + bestRatedFallback
    }

    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
        .lowercase(Locale.ROOT)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    private val AdultPattern = Regex(
        "(^|[^a-z0-9])(adult|adults|adulte|adultes|porn|porno|pornographie|pornographies|xxx|erotic|erotics|erotique|erotiques|hentai|sex|sexe|sexuel|sexuelle|sexuels|sexuelles|sexy|18\\+)([^a-z0-9]|$)",
    )
}
