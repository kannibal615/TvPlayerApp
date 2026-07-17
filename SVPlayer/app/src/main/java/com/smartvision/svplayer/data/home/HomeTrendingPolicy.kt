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
        artworkKeyOf: (T) -> String? = { null },
        limit: Int = SectionLimit,
    ): List<T> {
        if (limit <= 0) return emptyList()
        val rankedRated = ratedCandidates
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
            .toList()
        val usedIds = mutableSetOf<Int>()
        val usedArtwork = mutableSetOf<String>()
        val rated = rankedRated.takeUniqueVisualItems(
            limit = limit,
            idOf = idOf,
            artworkKeyOf = artworkKeyOf,
            usedIds = usedIds,
            usedArtwork = usedArtwork,
        )
        if (rated.size >= limit) return rated
        val rankedNewest = newestCandidates
            .asSequence()
            .filter(allowed)
            .filterNot { idOf(it) in usedIds }
            .distinctBy(idOf)
            .sortedWith(
                compareByDescending<T>(addedAtOf)
                    .thenByDescending { yearOf(it) ?: 0 }
                    .thenBy(idOf),
            )
            .toList()
        val newest = rankedNewest.takeUniqueVisualItems(
            limit = limit - rated.size,
            idOf = idOf,
            artworkKeyOf = artworkKeyOf,
            usedIds = usedIds,
            usedArtwork = usedArtwork,
        )
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
        artworkKeyOf: (T) -> String? = { null },
        minimumRating: Float = MinimumSeriesRating,
        limit: Int = SectionLimit,
    ): List<T> {
        if (limit <= 0) return emptyList()
        val comparator = compareByDescending<T> { ratingOf(it) ?: 0f }
            .thenByDescending(addedAtOf)
            .thenByDescending { yearOf(it) ?: 0 }
            .thenBy(idOf)
        val rankedRecent = recentCandidates
            .asSequence()
            .filter(allowed)
            .distinctBy(idOf)
            .filter { (ratingOf(it) ?: 0f).isFinite() && (ratingOf(it) ?: 0f) >= minimumRating }
            .sortedWith(comparator)
            .toList()
        val usedIds = mutableSetOf<Int>()
        val usedArtwork = mutableSetOf<String>()
        val recent = rankedRecent.takeUniqueVisualItems(
            limit = limit,
            idOf = idOf,
            artworkKeyOf = artworkKeyOf,
            usedIds = usedIds,
            usedArtwork = usedArtwork,
        )
        if (recent.size >= limit) return recent
        val rankedFallback = ratedCandidates
            .asSequence()
            .filter(allowed)
            .filterNot { idOf(it) in usedIds }
            .distinctBy(idOf)
            .filter { (ratingOf(it) ?: 0f).isFinite() && (ratingOf(it) ?: 0f) >= minimumRating }
            .sortedWith(comparator)
            .toList()
        val bestRatedFallback = rankedFallback.takeUniqueVisualItems(
            limit = limit - recent.size,
            idOf = idOf,
            artworkKeyOf = artworkKeyOf,
            usedIds = usedIds,
            usedArtwork = usedArtwork,
        )
        return recent + bestRatedFallback
    }

    fun artworkIdentity(value: String?): String? =
        value
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.substringBefore('#')
            ?.substringBefore('?')
            ?.trimEnd('/')
            ?.lowercase(Locale.ROOT)
            ?.takeIf(String::isNotEmpty)

    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
        .lowercase(Locale.ROOT)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    private val AdultPattern = Regex(
        "(^|[^a-z0-9])(adult|adults|adulte|adultes|porn|porno|pornographie|pornographies|xxx|erotic|erotics|erotique|erotiques|hentai|sex|sexe|sexuel|sexuelle|sexuels|sexuelles|sexy|18\\+)([^a-z0-9]|$)",
    )

    private fun <T> List<T>.takeUniqueVisualItems(
        limit: Int,
        idOf: (T) -> Int,
        artworkKeyOf: (T) -> String?,
        usedIds: MutableSet<Int>,
        usedArtwork: MutableSet<String>,
    ): List<T> {
        if (limit <= 0) return emptyList()
        val selected = ArrayList<T>(limit)
        for (item in this) {
            val id = idOf(item)
            if (id in usedIds) continue
            val artwork = artworkIdentity(artworkKeyOf(item))
            if (artwork != null && artwork in usedArtwork) continue
            usedIds += id
            if (artwork != null) usedArtwork += artwork
            selected += item
            if (selected.size == limit) break
        }
        return selected
    }
}
