package com.smartvision.svplayer.data.tmdb

import kotlin.math.roundToInt

object TmdbMatcher {
    const val MinimumConfidence = 68

    private val bracketRegex = Regex("""[\[(].*?[\])]""")
    private val yearRegex = Regex("""(19|20)\d{2}""")
    private val qualityRegex = Regex(
        """\b(4k|uhd|fhd|hd|hdr|dv|webrip|web-dl|bluray|brrip|x264|x265|h264|h265|hevc|multi|vostfr|vf|truefrench)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val catalogPrefixRegex = Regex("""^\s*([A-Z0-9]{2,6})\s*[-:|]\s+(.+)$""")
    private val leadingCatalogBracketRegex = Regex("""^\s*[\[(]([A-Z]{2,6})[\])]\s*(?:[-:|]\s*)?(.+)$""")
    private val trailingCatalogBracketRegex = Regex("""^(.+?)\s*[\[(]([A-Z]{2,6})[\])]\s*$""")
    private val parenthesizedYearRegex = Regex("""\s*\((?:19|20)\d{2}\)\s*""")
    private val punctuationRegex = Regex("""[^a-z0-9]+""")

    fun extractYear(vararg values: String?): String? =
        values.firstNotNullOfOrNull { value -> yearRegex.find(value.orEmpty())?.value }

    fun cleanTitle(value: String): String =
        value
            .lowercase()
            .replace(bracketRegex, " ")
            .replace(yearRegex, " ")
            .replace(qualityRegex, " ")
            .replace(punctuationRegex, " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    fun cleanDisplayTitle(value: String): String =
        stripCatalogBracketCodes(stripCatalogPrefix(value))
            .replace(parenthesizedYearRegex, " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    fun searchTitleCandidates(value: String): List<String> {
        val fullTitle = cleanTitle(value)
        val withoutCatalogPrefix = stripCatalogPrefix(value)
            .takeIf { it != value }
            ?.let(::cleanTitle)
            ?.takeIf(String::isNotBlank)
        return listOfNotNull(withoutCatalogPrefix, fullTitle.takeIf(String::isNotBlank)).distinct()
    }

    private fun stripCatalogPrefix(value: String): String =
        catalogPrefixRegex
            .matchEntire(value)
            ?.groupValues
            ?.getOrNull(2)
            ?: value

    private fun stripCatalogBracketCodes(value: String): String {
        val withoutLeading = leadingCatalogBracketRegex
            .matchEntire(value)
            ?.takeIf { it.groupValues[1] in CatalogBracketCodes }
            ?.groupValues
            ?.getOrNull(2)
            ?: value
        return trailingCatalogBracketRegex
            .matchEntire(withoutLeading)
            ?.takeIf { it.groupValues[2] in CatalogBracketCodes }
            ?.groupValues
            ?.getOrNull(1)
            ?: withoutLeading
    }

    fun scoreMovie(
        queryTitle: String,
        queryYear: String?,
        candidate: TmdbMovieSearchResultDto,
        includeAdult: Boolean,
    ): Int {
        if (!includeAdult && candidate.adult == true) return 0
        val candidateTitles = listOf(candidate.title, candidate.originalTitle)
        return scoreCommon(
            queryTitle = queryTitle,
            queryYear = queryYear,
            candidateTitles = candidateTitles,
            candidateDate = candidate.releaseDate,
            popularity = candidate.popularity,
            voteCount = candidate.voteCount,
        )
    }

    fun scoreSeries(
        queryTitle: String,
        queryYear: String?,
        candidate: TmdbSeriesSearchResultDto,
        includeAdult: Boolean,
    ): Int {
        if (!includeAdult && candidate.adult == true) return 0
        val candidateTitles = listOf(candidate.name, candidate.originalName)
        return scoreCommon(
            queryTitle = queryTitle,
            queryYear = queryYear,
            candidateTitles = candidateTitles,
            candidateDate = candidate.firstAirDate,
            popularity = candidate.popularity,
            voteCount = candidate.voteCount,
        )
    }

    private fun scoreCommon(
        queryTitle: String,
        queryYear: String?,
        candidateTitles: List<String?>,
        candidateDate: String?,
        popularity: Double?,
        voteCount: Int?,
    ): Int {
        val cleanQuery = cleanTitle(queryTitle)
        if (cleanQuery.isBlank()) return 0
        val cleanCandidateTitles = candidateTitles.mapNotNull { it?.let(::cleanTitle)?.takeIf(String::isNotBlank) }
        if (cleanCandidateTitles.isEmpty()) return 0

        val titleScore = when {
            cleanCandidateTitles.any { it == cleanQuery } -> 70
            cleanCandidateTitles.any { it.contains(cleanQuery) || cleanQuery.contains(it) } -> 58
            else -> cleanCandidateTitles.maxOf { tokenOverlapScore(cleanQuery, it) }
        }

        if (titleScore < 28) return titleScore

        val candidateYear = extractYear(candidateDate)
        val yearScore = when {
            queryYear.isNullOrBlank() -> 0
            candidateYear == queryYear -> 20
            candidateYear != null && kotlin.math.abs(candidateYear.toInt() - queryYear.toInt()) == 1 -> 8
            candidateYear != null -> -8
            else -> 0
        }
        val popularityScore = when {
            (voteCount ?: 0) >= 500 -> 5
            (voteCount ?: 0) >= 100 -> 3
            (popularity ?: 0.0) >= 25.0 -> 3
            else -> 0
        }
        return (titleScore + yearScore + popularityScore).coerceIn(0, 100)
    }

    private fun tokenOverlapScore(left: String, right: String): Int {
        val leftTokens = left.split(' ').filter { it.length > 1 }.toSet()
        val rightTokens = right.split(' ').filter { it.length > 1 }.toSet()
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0
        val shared = leftTokens.intersect(rightTokens).size
        val coverage = shared.toDouble() / leftTokens.size.toDouble()
        return (coverage * 48.0).roundToInt()
    }
}

private val CatalogBracketCodes = setOf(
    "AR",
    "BE",
    "BR",
    "DE",
    "EN",
    "ES",
    "FR",
    "IT",
    "JP",
    "KR",
    "MULTI",
    "NL",
    "PL",
    "PT",
    "RU",
    "TR",
    "VF",
    "VOSTFR",
)
