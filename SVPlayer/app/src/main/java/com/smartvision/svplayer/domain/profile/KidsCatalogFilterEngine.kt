package com.smartvision.svplayer.domain.profile

import java.security.MessageDigest

data class KidsCategoryInput(
    val id: String,
    val name: String,
)

data class KidsItemInput<T>(
    val value: T,
    val id: String,
    val categoryId: String?,
    val metadata: KidsContentMetadata,
)

data class CachedKidsCategoryDecision(
    val categoryId: String,
    val normalizedName: String,
    val metadataFingerprint: String,
    val ruleVersion: Int,
    val decision: KidsCategoryDecision,
)

data class CachedKidsItemDecision(
    val contentId: String,
    val categoryId: String?,
    val metadataFingerprint: String,
    val ruleVersion: Int,
    val decision: KidsContentDecision,
)

data class KidsFilterMetrics(
    val categoriesProcessed: Int = 0,
    val categoriesEvaluated: Int = 0,
    val categoryCacheHits: Int = 0,
    val safeCategories: Int = 0,
    val processedItems: Int = 0,
    val inheritedItems: Int = 0,
    val individuallyAnalyzedItems: Int = 0,
    val itemCacheHits: Int = 0,
    val ambiguousItems: Int = 0,
    val rejectedItems: Int = 0,
    val durationMs: Long = 0L,
) {
    val cacheHits: Int
        get() = categoryCacheHits + itemCacheHits

    operator fun plus(other: KidsFilterMetrics): KidsFilterMetrics = KidsFilterMetrics(
        categoriesProcessed = categoriesProcessed + other.categoriesProcessed,
        categoriesEvaluated = categoriesEvaluated + other.categoriesEvaluated,
        categoryCacheHits = categoryCacheHits + other.categoryCacheHits,
        safeCategories = safeCategories + other.safeCategories,
        processedItems = processedItems + other.processedItems,
        inheritedItems = inheritedItems + other.inheritedItems,
        individuallyAnalyzedItems = individuallyAnalyzedItems + other.individuallyAnalyzedItems,
        itemCacheHits = itemCacheHits + other.itemCacheHits,
        ambiguousItems = ambiguousItems + other.ambiguousItems,
        rejectedItems = rejectedItems + other.rejectedItems,
        durationMs = durationMs + other.durationMs,
    )
}

data class KidsCategoryBatchResult(
    val decisions: Map<String, KidsCategoryDecision>,
    val cacheUpdates: List<CachedKidsCategoryDecision>,
    val metrics: KidsFilterMetrics,
)

data class KidsItemBatchResult<T>(
    val acceptedItems: List<T>,
    val acceptedCategoryIds: Set<String>,
    val cacheUpdates: List<CachedKidsItemDecision>,
    val metrics: KidsFilterMetrics,
)

/** Category-first catalog pipeline. Safe categories never invoke [itemEvaluator]. */
class KidsCatalogFilterEngine(
    private val filter: KidsContentFilter = KidsContentFilter(),
    categoryEvaluator: ((String?) -> KidsCategoryDecision)? = null,
    itemEvaluator: ((KidsContentMetadata) -> KidsContentDecision)? = null,
) {
    private val evaluateCategory = categoryEvaluator ?: filter::evaluateCategory
    private val evaluateItem = itemEvaluator ?: filter::evaluateItem

    fun evaluateCategories(
        categories: List<KidsCategoryInput>,
        cached: Map<String, CachedKidsCategoryDecision> = emptyMap(),
    ): KidsCategoryBatchResult {
        val startedAt = System.nanoTime()
        val decisions = LinkedHashMap<String, KidsCategoryDecision>(categories.size)
        val updates = ArrayList<CachedKidsCategoryDecision>()
        var evaluated = 0
        var cacheHits = 0
        var safe = 0

        categories.forEach { category ->
            val normalizedName = filter.normalize(category.name)
            val fingerprint = KidsFilterFingerprint.category(category.name)
            val cachedDecision = cached[category.id]
                ?.takeIf {
                    it.ruleVersion == KidsContentFilter.RuleVersion &&
                        it.normalizedName == normalizedName &&
                        it.metadataFingerprint == fingerprint
                }
            val decision = if (cachedDecision != null) {
                cacheHits += 1
                cachedDecision.decision
            } else {
                evaluated += 1
                evaluateCategory(category.name).also {
                    updates += CachedKidsCategoryDecision(
                        categoryId = category.id,
                        normalizedName = normalizedName,
                        metadataFingerprint = fingerprint,
                        ruleVersion = KidsContentFilter.RuleVersion,
                        decision = it,
                    )
                }
            }
            decisions[category.id] = decision
            if (decision.safe) safe += 1
        }

        return KidsCategoryBatchResult(
            decisions = decisions,
            cacheUpdates = updates,
            metrics = KidsFilterMetrics(
                categoriesProcessed = categories.size,
                categoriesEvaluated = evaluated,
                categoryCacheHits = cacheHits,
                safeCategories = safe,
                durationMs = elapsedMillis(startedAt),
            ),
        )
    }

    fun <T> filterItems(
        items: List<KidsItemInput<T>>,
        categoryDecisions: Map<String, KidsCategoryDecision>,
        cached: Map<String, CachedKidsItemDecision> = emptyMap(),
    ): KidsItemBatchResult<T> {
        val startedAt = System.nanoTime()
        val accepted = ArrayList<T>(items.size)
        val acceptedCategoryIds = linkedSetOf<String>()
        val updates = ArrayList<CachedKidsItemDecision>()
        var inherited = 0
        var individuallyAnalyzed = 0
        var cacheHits = 0
        var ambiguous = 0
        var rejected = 0

        items.forEach { item ->
            val categoryDecision = item.categoryId?.let(categoryDecisions::get)
            val manualOverride = item.metadata.manualOverride
            val decision = when {
                manualOverride != null -> {
                    individuallyAnalyzed += 1
                    evaluateItem(item.metadata)
                }
                categoryDecision?.safe == true && filter.isStructuredAdultRating(item.metadata.ageRating) -> {
                    KidsContentDecision(
                        classification = KidsContentClassification.NOT_KIDS_CONTENT,
                        score = -200,
                        source = KidsDecisionSource.ADULT_EXCLUSION,
                        reason = "INHERITED_ITEM_ADULT_RATING",
                    )
                }
                categoryDecision?.safe == true -> {
                    inherited += 1
                    filter.inheritedDecision(requireNotNull(item.categoryId), categoryDecision)
                }
                else -> {
                    val fingerprint = KidsFilterFingerprint.item(item.categoryId, item.metadata)
                    val cachedDecision = cached[item.id]
                        ?.takeIf {
                            it.ruleVersion == KidsContentFilter.RuleVersion &&
                                it.categoryId == item.categoryId &&
                                it.metadataFingerprint == fingerprint
                        }
                    if (cachedDecision != null) {
                        cacheHits += 1
                        cachedDecision.decision
                    } else {
                        individuallyAnalyzed += 1
                        evaluateItem(item.metadata).also {
                            updates += CachedKidsItemDecision(
                                contentId = item.id,
                                categoryId = item.categoryId,
                                metadataFingerprint = fingerprint,
                                ruleVersion = KidsContentFilter.RuleVersion,
                                decision = it,
                            )
                        }
                    }
                }
            }

            if (decision.allowed) {
                accepted += item.value
                item.categoryId?.let(acceptedCategoryIds::add)
            } else {
                rejected += 1
                if (decision.classification == KidsContentClassification.AMBIGUOUS_CONTENT) ambiguous += 1
            }
        }

        return KidsItemBatchResult(
            acceptedItems = accepted,
            acceptedCategoryIds = acceptedCategoryIds,
            cacheUpdates = updates,
            metrics = KidsFilterMetrics(
                processedItems = items.size,
                inheritedItems = inherited,
                individuallyAnalyzedItems = individuallyAnalyzed,
                itemCacheHits = cacheHits,
                ambiguousItems = ambiguous,
                rejectedItems = rejected,
                durationMs = elapsedMillis(startedAt),
            ),
        )
    }
}

object KidsFilterFingerprint {
    fun category(name: String): String = sha256(name.trim())

    fun item(categoryId: String?, metadata: KidsContentMetadata): String = sha256(
        listOf(
            categoryId.orEmpty(),
            metadata.kind.storageName,
            metadata.title.orEmpty(),
            metadata.description.orEmpty(),
            metadata.genres.orEmpty(),
            metadata.ageRating.orEmpty(),
            metadata.profileAge?.toString().orEmpty(),
            metadata.manualOverride?.toString().orEmpty(),
        ).joinToString("\u0000"),
    )

    fun source(type: String, vararg stableParts: String): String =
        "$type:${sha256(stableParts.joinToString("\u0000") { it.trim() })}"

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}

private fun elapsedMillis(startedAtNanos: Long): Long =
    ((System.nanoTime() - startedAtNanos) / 1_000_000L).coerceAtLeast(0L)
