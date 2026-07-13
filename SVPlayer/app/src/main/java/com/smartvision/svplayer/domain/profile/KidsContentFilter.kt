package com.smartvision.svplayer.domain.profile

import java.text.Normalizer
import java.util.Locale

enum class KidsCategoryClassification {
    SAFE_KIDS_CATEGORY,
    AMBIGUOUS_CATEGORY,
    NOT_KIDS_CATEGORY,
}

enum class KidsContentClassification {
    SAFE_KIDS_CONTENT,
    AMBIGUOUS_CONTENT,
    NOT_KIDS_CONTENT,
}

enum class KidsDecisionSource {
    SAFE_CATEGORY,
    CHANNEL_ALLOWLIST,
    AGE_RATING,
    GENRE_AND_RATING,
    ITEM_SCORE,
    MANUAL_OVERRIDE,
    ADULT_EXCLUSION,
}

enum class KidsContentKind(val storageName: String) {
    LIVE_CHANNEL("live"),
    MOVIE("movie"),
    SERIES("series"),
}

data class KidsCategoryDecision(
    val classification: KidsCategoryClassification,
    val score: Int,
    val source: KidsDecisionSource,
    val reason: String,
) {
    val safe: Boolean
        get() = classification == KidsCategoryClassification.SAFE_KIDS_CATEGORY
}

data class KidsContentDecision(
    val classification: KidsContentClassification,
    val score: Int,
    val source: KidsDecisionSource,
    val reason: String,
    val inheritedCategoryId: String? = null,
) {
    val allowed: Boolean
        get() = classification == KidsContentClassification.SAFE_KIDS_CONTENT
}

data class KidsContentMetadata(
    val kind: KidsContentKind,
    val title: String?,
    val description: String? = null,
    val genres: String? = null,
    val ageRating: String? = null,
    val profileAge: Int? = null,
    val manualOverride: Boolean? = null,
)

/**
 * Pure, allocation-conscious rules engine. Category and item decisions are intentionally
 * separate: compact category tokens such as USAKIDS are never accepted in a movie title.
 */
class KidsContentFilter {
    fun evaluateCategory(name: String?): KidsCategoryDecision {
        val text = name.toSignals()
        if (text.normalized.isBlank()) return categoryDecision(0, "CATEGORY_EMPTY")
        if (text.containsExplicitAdultMarker()) {
            return KidsCategoryDecision(
                classification = KidsCategoryClassification.NOT_KIDS_CATEGORY,
                score = AdultRejectionScore,
                source = KidsDecisionSource.ADULT_EXCLUSION,
                reason = "CATEGORY_ADULT_EXCLUSION",
            )
        }

        val compactKids = text.tokens.any(::isCompactCategoryKidsToken)
        val strongTerm = text.containsAnyTerm(CategoryStrongTerms)
        val strongPhrase = text.containsAnyPhrase(CategoryStrongPhrases)
        val localizedCartoonPhrase = text.containsAnyPhrase(LocalizedCartoonPhrases)
        val localizedCartoonsWithCountry =
            text.tokens.any { it == "cartoon" || it == "cartoons" } &&
                text.tokens.any { it in CountryCategoryTokens }

        if (compactKids || strongTerm || strongPhrase || localizedCartoonPhrase || localizedCartoonsWithCountry) {
            val reason = when {
                compactKids -> "CATEGORY_COMPACT_KIDS"
                localizedCartoonPhrase -> "CATEGORY_LOCALIZED_CARTOONS"
                localizedCartoonsWithCountry -> "CATEGORY_CARTOONS_COUNTRY"
                strongPhrase -> "CATEGORY_STRONG_PHRASE"
                else -> "CATEGORY_STRONG_TERM"
            }
            return KidsCategoryDecision(
                classification = KidsCategoryClassification.SAFE_KIDS_CATEGORY,
                score = StrongCategoryScore,
                source = KidsDecisionSource.SAFE_CATEGORY,
                reason = reason,
            )
        }

        var score = 0
        val reasons = mutableListOf<String>()
        if (text.tokens.any { it in CategoryAmbiguousTerms }) {
            score += AmbiguousCategoryScore
            reasons += "AMBIGUOUS_KIDS_TERM"
        }
        if (text.tokens.any { it in GenericCartoonTerms }) {
            score += GenericCartoonScore
            reasons += "GENERIC_CARTOON_OR_ANIMATION"
        }
        if (text.tokens.any { it in FamilyTerms }) {
            score += FamilyCategoryScore
            reasons += "FAMILY_ONLY"
        }
        return categoryDecision(score, reasons.joinToString("+").ifBlank { "NO_KIDS_SIGNAL" })
    }

    fun evaluateItem(metadata: KidsContentMetadata): KidsContentDecision {
        metadata.manualOverride?.let { allowed ->
            return KidsContentDecision(
                classification = if (allowed) {
                    KidsContentClassification.SAFE_KIDS_CONTENT
                } else {
                    KidsContentClassification.NOT_KIDS_CONTENT
                },
                score = if (allowed) AllowlistScore else AdultRejectionScore,
                source = KidsDecisionSource.MANUAL_OVERRIDE,
                reason = if (allowed) "MANUAL_ALLOW" else "MANUAL_DENY",
            )
        }

        val title = metadata.title.toSignals()
        val description = metadata.description.toSignals()
        val genres = metadata.genres.toSignals()
        if (isStructuredAdultRating(metadata.ageRating) ||
            title.containsExplicitAdultMarker() ||
            description.containsExplicitAdultMarker() ||
            genres.containsExplicitAdultMarker()
        ) {
            return KidsContentDecision(
                classification = KidsContentClassification.NOT_KIDS_CONTENT,
                score = AdultRejectionScore,
                source = KidsDecisionSource.ADULT_EXCLUSION,
                reason = "ITEM_ADULT_EXCLUSION",
            )
        }

        if (metadata.kind == KidsContentKind.LIVE_CHANNEL && title.matchesControlledChannelAlias()) {
            return KidsContentDecision(
                classification = KidsContentClassification.SAFE_KIDS_CONTENT,
                score = AllowlistScore,
                source = KidsDecisionSource.CHANNEL_ALLOWLIST,
                reason = "CHANNEL_ALLOWLIST",
            )
        }

        var score = 0
        var ageScore = 0
        var genreScore = 0
        val reasons = mutableListOf<String>()
        when (classifyAgeRating(metadata.ageRating, metadata.profileAge ?: DefaultMaximumKidsAge)) {
            RatingClassification.SAFE -> {
                ageScore = SafeAgeRatingScore
                reasons += "SAFE_AGE_RATING"
            }
            RatingClassification.COMPATIBLE -> {
                ageScore = CompatibleAgeRatingScore
                reasons += "COMPATIBLE_AGE_RATING"
            }
            RatingClassification.UNKNOWN -> Unit
            RatingClassification.ADULT -> error("Adult ratings are handled before scoring")
        }
        score += ageScore

        val hasOfficialKidsGenre = genres.containsAnyTerm(OfficialKidsGenreTerms) ||
            genres.containsAnyPhrase(OfficialKidsGenrePhrases)
        val hasAnimationGenre = genres.tokens.any { it in AnimationGenreTerms }
        val hasFamilyGenre = genres.tokens.any { it in FamilyTerms }
        genreScore = when {
            hasAnimationGenre && hasFamilyGenre -> AnimationFamilyScore
            hasOfficialKidsGenre -> OfficialKidsGenreScore
            hasAnimationGenre -> AnimationOnlyScore
            hasFamilyGenre -> FamilyOnlyScore
            else -> 0
        }
        if (genreScore > 0) {
            score += genreScore
            reasons += when {
                hasAnimationGenre && hasFamilyGenre -> "ANIMATION_AND_FAMILY"
                hasOfficialKidsGenre -> "OFFICIAL_KIDS_GENRE"
                hasAnimationGenre -> "ANIMATION_ONLY"
                else -> "FAMILY_ONLY"
            }
        }

        if (description.containsAnyTerm(ItemKidsTerms) || description.containsAnyPhrase(ItemKidsPhrases)) {
            score += DescriptionKidsScore
            reasons += "DESCRIPTION_KIDS_TERM"
        }
        if (title.containsAnyTerm(ItemKidsTerms) || title.containsAnyPhrase(ItemKidsPhrases)) {
            score += TitleKidsScore
            reasons += "TITLE_KIDS_TERM"
        }

        val hasImportantNegative = title.containsImportantNegativeMarker() ||
            description.containsImportantNegativeMarker() ||
            genres.containsImportantNegativeMarker()
        if (hasImportantNegative) {
            score += ImportantNegativeScore
            reasons += "IMPORTANT_NEGATIVE_SIGNAL"
        }

        val classification = when {
            score >= SafeContentThreshold -> KidsContentClassification.SAFE_KIDS_CONTENT
            score >= AmbiguousContentThreshold -> KidsContentClassification.AMBIGUOUS_CONTENT
            else -> KidsContentClassification.NOT_KIDS_CONTENT
        }
        val source = when {
            ageScore > 0 && genreScore > 0 -> KidsDecisionSource.GENRE_AND_RATING
            ageScore >= SafeAgeRatingScore -> KidsDecisionSource.AGE_RATING
            else -> KidsDecisionSource.ITEM_SCORE
        }
        return KidsContentDecision(
            classification = classification,
            score = score,
            source = source,
            reason = reasons.joinToString("+").ifBlank { "NO_SUFFICIENT_SIGNAL" },
        )
    }

    fun inheritedDecision(categoryId: String, category: KidsCategoryDecision): KidsContentDecision =
        KidsContentDecision(
            classification = KidsContentClassification.SAFE_KIDS_CONTENT,
            score = category.score,
            source = KidsDecisionSource.SAFE_CATEGORY,
            reason = "SAFE_CATEGORY_INHERITANCE",
            inheritedCategoryId = categoryId,
        )

    fun isStructuredAdultRating(value: String?): Boolean =
        classifyAgeRating(value, DefaultMaximumKidsAge) == RatingClassification.ADULT

    fun normalize(value: String): String = normalizeKidsText(value)

    private fun categoryDecision(score: Int, reason: String): KidsCategoryDecision =
        KidsCategoryDecision(
            classification = when {
                score >= SafeCategoryThreshold -> KidsCategoryClassification.SAFE_KIDS_CATEGORY
                score >= AmbiguousCategoryThreshold -> KidsCategoryClassification.AMBIGUOUS_CATEGORY
                else -> KidsCategoryClassification.NOT_KIDS_CATEGORY
            },
            score = score,
            source = KidsDecisionSource.ITEM_SCORE,
            reason = reason,
        )

    private fun classifyAgeRating(value: String?, maximumAge: Int): RatingClassification {
        val canonical = value.canonicalAgeRating()
        if (canonical.isBlank()) return RatingClassification.UNKNOWN
        if (canonical in AdultRatings) return RatingClassification.ADULT
        if (canonical in SafeRatings) return RatingClassification.SAFE
        val numericAge = NumericAgeRating.matchEntire(canonical)?.groupValues?.getOrNull(1)?.toIntOrNull()
        return if (numericAge != null && numericAge <= maximumAge && numericAge in CompatibleAges) {
            RatingClassification.COMPATIBLE
        } else {
            RatingClassification.UNKNOWN
        }
    }

    private enum class RatingClassification { SAFE, COMPATIBLE, ADULT, UNKNOWN }

    companion object {
        const val RuleVersion = 2
        const val DefaultMaximumKidsAge = 12

        private const val SafeCategoryThreshold = 80
        private const val AmbiguousCategoryThreshold = 40
        private const val SafeContentThreshold = 80
        private const val AmbiguousContentThreshold = 50
        private const val AllowlistScore = 100
        private const val StrongCategoryScore = 80
        private const val AmbiguousCategoryScore = 40
        private const val GenericCartoonScore = 35
        private const val FamilyCategoryScore = 15
        private const val SafeAgeRatingScore = 80
        private const val CompatibleAgeRatingScore = 50
        private const val AnimationFamilyScore = 80
        private const val OfficialKidsGenreScore = 70
        private const val AnimationOnlyScore = 25
        private const val FamilyOnlyScore = 20
        private const val DescriptionKidsScore = 20
        private const val TitleKidsScore = 10
        private const val ImportantNegativeScore = -80
        private const val AdultRejectionScore = -200

        private val CategoryStrongTerms = normalizedSetOf(
            "kids", "kid", "children", "child", "baby", "toddler", "preschool",
            "enfant", "enfants", "bébé", "tout-petit", "tout-petits", "préscolaire", "maternelle",
            "comptine", "comptines", "éveil",
            "أطفال", "الأطفال", "للأطفال", "طفل", "صغار", "الصغار", "للصغار", "كرتون", "براعم",
            "atfal", "lilatfal", "baraem",
            "niños", "niñas", "infantil", "preescolar",
            "crianças", "criança",
            "bambini", "bambino", "infanzia", "prescolare",
            "çocuk", "çocuklar",
            "kinder", "kind", "vorschule", "kinderprogramm", "kindersender",
            "kinderen", "tekenfilm", "kinderprogramma",
            "dzieci", "dziecko", "bajki",
            "дети", "детский", "мультфильм", "мультфильмы",
            "copii", "copil",
            "παιδιά", "παιδικό",
        )
        private val CategoryStrongPhrases = normalizedListOf(
            "children's", "kids animation", "children animation", "nursery rhymes", "kids learning",
            "educational for kids", "dessin animé", "dessins animés", "animation jeunesse",
            "programme jeunesse", "programmes jeunesse",
            "رسوم متحركة", "برامج أطفال", "قناة أطفال", "أناشيد أطفال", "تعليم الأطفال", "حكايات أطفال",
            "al atfal", "baramij atfal", "anasheed atfal", "arabic kids", "arabic cartoons",
            "programa infantil", "para niños", "programa infantil", "para crianças",
            "programma per bambini", "okul öncesi", "çocuk kanalı", "çocuk programı",
            "program dla dzieci", "для детей", "program pentru copii",
        )
        private val LocalizedCartoonPhrases = normalizedListOf(
            "dibujos animados", "desenhos animados", "cartoni animati", "cartone animato",
            "çizgi film", "zeichentrick", "desene animate", "κινούμενα σχέδια",
        )
        private val CategoryAmbiguousTerms = normalizedSetOf("jeunesse", "jeugd", "junior")
        private val GenericCartoonTerms = normalizedSetOf("cartoon", "cartoons", "animation", "animated")
        private val FamilyTerms = normalizedSetOf("family", "famille", "familial")
        private val CountryCategoryTokens = normalizedSetOf(
            "us", "usa", "uk", "gb", "fr", "france", "ar", "arabic", "es", "spain", "espana",
            "pt", "portugal", "it", "italia", "tr", "turkiye", "de", "deutschland", "nl", "pl",
            "ru", "ro", "gr",
        )
        private val ItemKidsTerms = normalizedSetOf(
            "kids", "kid", "children", "child", "enfant", "enfants", "أطفال", "اطفال",
            "niños", "niñas", "crianças", "bambini", "çocuk", "kinder", "dzieci", "дети", "copii", "παιδιά",
        )
        private val ItemKidsPhrases = normalizedListOf(
            "for kids", "pour enfants", "للأطفال", "para niños", "para crianças", "per bambini", "для детей",
        )
        private val OfficialKidsGenreTerms = normalizedSetOf("kids", "children", "preschool", "jeunesse", "infantil")
        private val OfficialKidsGenrePhrases = normalizedListOf("for kids", "pour enfants", "para niños", "per bambini")
        private val AnimationGenreTerms = normalizedSetOf("animation", "animated", "cartoon", "cartoons")

        private val SafeRatings = setOf("TVY", "TVY7", "G", "GRATED", "U", "URATED", "0+", "3+", "6+", "7+")
        private val AdultRatings = setOf("16+", "18+", "TVMA", "R", "NC17")
        private val CompatibleAges = setOf(10, 12)
        private val NumericAgeRating = Regex("^(\\d{1,2})\\+$")
    }
}

private data class TextSignals(
    val raw: String,
    val normalized: String,
    val tokens: Set<String>,
    val padded: String,
) {
    fun containsAnyTerm(terms: Set<String>): Boolean = tokens.any(terms::contains)

    fun containsAnyPhrase(phrases: List<String>): Boolean = phrases.any(::containsPhrase)

    fun containsPhrase(phrase: String): Boolean = padded.contains(" $phrase ")

    fun containsExplicitAdultMarker(): Boolean =
        AdultAgeTextPattern.containsMatchIn(raw) ||
            containsAnyTerm(KidsContentFilterAdultLexicon.terms) ||
            containsAnyPhrase(KidsContentFilterAdultLexicon.phrases)

    fun containsImportantNegativeMarker(): Boolean =
        containsAnyTerm(KidsContentFilterAdultLexicon.importantNegativeTerms) ||
            containsAnyPhrase(KidsContentFilterAdultLexicon.importantNegativePhrases)

    fun matchesControlledChannelAlias(): Boolean =
        KidsContentFilterChannelLexicon.aliases.any(::containsPhrase)
}

private object KidsContentFilterAdultLexicon {
    val terms = normalizedSetOf(
        "adult", "adults", "adulte", "xxx", "porn", "porno", "erotic", "érotique", "mature",
        "uncensored", "hentai",
    )
    val phrases = normalizedListOf(
        "adults only", "non censuré", "tv ma", "nc 17", "interdit aux moins de 18 ans",
        "adult animation", "anime adult",
    )
    val importantNegativeTerms = normalizedSetOf(
        "horror", "horreur", "gore", "slasher", "murder", "meurtre", "drug", "drogue",
        "gambling", "casino", "betting",
    )
    val importantNegativePhrases = normalizedListOf(
        "violence extrême", "true crime", "adult animation", "anime adult",
    )
}

private object KidsContentFilterChannelLexicon {
    val aliases = normalizedListOf(
        "disney junior", "disney channel", "nick jr", "nickelodeon", "cartoon network", "cartoonito",
        "boomerang", "babytv", "cbeebies", "cbbc", "pbs kids", "gulli", "tiji", "piwi+", "canal j",
        "télétoon", "okoo", "m6 kid", "boing", "dreamworks", "jimjam", "spacetoon", "majid kids",
        "jeem tv", "baraem", "toyor al janah", "طيور الجنة", "سبيستون", "ماجد للأطفال", "براعم",
    )
}

private fun String?.toSignals(): TextSignals {
    val raw = this.orEmpty()
    val normalized = normalizeKidsText(raw)
    return TextSignals(
        raw = raw,
        normalized = normalized,
        tokens = if (normalized.isBlank()) emptySet() else normalized.split(' ').toSet(),
        padded = " $normalized ",
    )
}

private fun isCompactCategoryKidsToken(token: String): Boolean {
    if (token == "kids") return false
    val kidsIndex = token.indexOf("kids")
    if (kidsIndex < 0 || token.indexOf("kids", kidsIndex + 1) >= 0) return false
    val prefix = token.substring(0, kidsIndex)
    val suffix = token.substring(kidsIndex + 4).trimEnd { it.isDigit() }
    return (prefix.isEmpty() || prefix in CompactCategoryPrefixes) &&
        (suffix.isEmpty() || suffix in CompactCategorySuffixes) &&
        (prefix.isNotEmpty() || suffix.isNotEmpty())
}

private val CompactCategoryPrefixes = normalizedSetOf(
    "us", "usa", "uk", "gb", "fr", "ar", "arabic", "es", "pt", "it", "tr", "de", "nl",
    "pl", "ru", "ro", "gr",
)
private val CompactCategorySuffixes = CompactCategoryPrefixes + normalizedSetOf(
    "france", "spain", "portugal", "italia", "turkiye", "deutschland",
)

private fun String?.canonicalAgeRating(): String = this.orEmpty()
    .trim()
    .uppercase(Locale.ROOT)
    .replace(KidsTextPatterns.ageRatingSeparators, "")

private fun normalizeKidsText(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
    .lowercase(Locale.ROOT)
    .replace(KidsTextPatterns.combiningMarks, "")
    .replace(KidsTextPatterns.separators, " ")
    .replace(KidsTextPatterns.unsupportedText, " ")
    .trim()
    .replace(KidsTextPatterns.multipleSpaces, " ")

private fun normalizedSetOf(vararg values: String): Set<String> = values.mapTo(linkedSetOf(), ::normalizeKidsText)

private fun normalizedListOf(vararg values: String): List<String> = values.map(::normalizeKidsText).distinct()

private object KidsTextPatterns {
    val combiningMarks = Regex("\\p{M}+")
    val separators = Regex("[|_\\-/:.()\\[\\]{}'’]+")
    val unsupportedText = Regex("[^\\p{L}\\p{N}+]+")
    val multipleSpaces = Regex("\\s+")
    val ageRatingSeparators = Regex("[\\s_\\-]")
}

private val AdultAgeTextPattern = Regex("(?i)(^|[^\\p{L}\\p{N}])(?:16|18)\\s*\\+($|[^\\p{L}\\p{N}])")
