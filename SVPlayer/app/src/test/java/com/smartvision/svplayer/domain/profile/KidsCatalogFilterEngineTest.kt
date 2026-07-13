package com.smartvision.svplayer.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KidsCatalogFilterEngineTest {
    private val filter = KidsContentFilter()

    @Test
    fun `safe categories are evaluated once and 245 items inherit without individual scoring`() {
        var categoryCalls = 0
        var itemCalls = 0
        val engine = KidsCatalogFilterEngine(
            filter = filter,
            categoryEvaluator = {
                categoryCalls += 1
                filter.evaluateCategory(it)
            },
            itemEvaluator = {
                itemCalls += 1
                filter.evaluateItem(it)
            },
        )
        val categories = listOf(
            KidsCategoryInput("fr", "KIDS France 150"),
            KidsCategoryInput("usa", "USAKIDS 95"),
        )
        val categoryBatch = engine.evaluateCategories(categories)
        val items = buildList {
            repeat(150) { index -> add(item("fr-$index", "fr", "Programme $index")) }
            repeat(95) { index -> add(item("usa-$index", "usa", "Channel $index", KidsContentKind.LIVE_CHANNEL)) }
        }
        val result = engine.filterItems(items, categoryBatch.decisions)

        assertEquals(2, categoryCalls)
        assertEquals(0, itemCalls)
        assertEquals(245, result.acceptedItems.size)
        assertEquals(245, result.metrics.inheritedItems)
        assertEquals(0, result.metrics.individuallyAnalyzedItems)
    }

    @Test
    fun `unchanged decisions are shared from cache and only changed metadata is rescored`() {
        val engine = KidsCatalogFilterEngine(filter)
        val categories = listOf(KidsCategoryInput("general", "General entertainment"))
        val firstCategories = engine.evaluateCategories(categories)
        val firstItems = listOf(
            item("1", "general", "Learning Hour", ageRating = "G"),
            item("2", "general", "Family Story", genres = "Family"),
            item("3", "general", "Animated Family", genres = "Animation, Family"),
        )
        val first = engine.filterItems(firstItems, firstCategories.decisions)
        assertEquals(3, first.metrics.individuallyAnalyzedItems)

        val categoryCache = firstCategories.cacheUpdates.associateBy { it.categoryId }
        val itemCache = first.cacheUpdates.associateBy { it.contentId }
        val secondCategories = engine.evaluateCategories(categories, categoryCache)
        val second = engine.filterItems(firstItems, secondCategories.decisions, itemCache)
        assertEquals(1, secondCategories.metrics.categoryCacheHits)
        assertEquals(3, second.metrics.itemCacheHits)
        assertEquals(0, second.metrics.individuallyAnalyzedItems)

        val changedItems = firstItems.map { input ->
            if (input.id == "2") input.copy(metadata = input.metadata.copy(description = "Updated metadata")) else input
        }
        val third = engine.filterItems(changedItems, secondCategories.decisions, itemCache)
        assertEquals(2, third.metrics.itemCacheHits)
        assertEquals(1, third.metrics.individuallyAnalyzedItems)
    }

    @Test
    fun `rule version mismatch invalidates category and item cache`() {
        val engine = KidsCatalogFilterEngine(filter)
        val categories = listOf(KidsCategoryInput("general", "General entertainment"))
        val firstCategories = engine.evaluateCategories(categories)
        val items = listOf(item("1", "general", "Learning Hour", ageRating = "G"))
        val firstItems = engine.filterItems(items, firstCategories.decisions)
        val staleCategories = firstCategories.cacheUpdates
            .map { it.copy(ruleVersion = KidsContentFilter.RuleVersion - 1) }
            .associateBy { it.categoryId }
        val staleItems = firstItems.cacheUpdates
            .map { it.copy(ruleVersion = KidsContentFilter.RuleVersion - 1) }
            .associateBy { it.contentId }

        val categoriesAfterRuleChange = engine.evaluateCategories(categories, staleCategories)
        val itemsAfterRuleChange = engine.filterItems(items, categoriesAfterRuleChange.decisions, staleItems)
        assertEquals(1, categoriesAfterRuleChange.metrics.categoriesEvaluated)
        assertEquals(0, categoriesAfterRuleChange.metrics.categoryCacheHits)
        assertEquals(1, itemsAfterRuleChange.metrics.individuallyAnalyzedItems)
        assertEquals(0, itemsAfterRuleChange.metrics.itemCacheHits)
    }

    @Test
    fun `another Kids profile reuses source decisions without a full pass`() {
        val firstProfileEngine = KidsCatalogFilterEngine(filter)
        val categories = listOf(
            KidsCategoryInput("kids", "KIDS France 150"),
            KidsCategoryInput("general", "General entertainment"),
        )
        val firstCategories = firstProfileEngine.evaluateCategories(categories)
        val uncertainItems = listOf(item("general-1", "general", "Learning Hour", ageRating = "G"))
        val firstItems = firstProfileEngine.filterItems(uncertainItems, firstCategories.decisions)

        val secondProfileEngine = KidsCatalogFilterEngine(filter)
        val secondCategories = secondProfileEngine.evaluateCategories(
            categories,
            firstCategories.cacheUpdates.associateBy { it.categoryId },
        )
        val secondItems = secondProfileEngine.filterItems(
            uncertainItems,
            secondCategories.decisions,
            firstItems.cacheUpdates.associateBy { it.contentId },
        )

        assertEquals(2, secondCategories.metrics.categoryCacheHits)
        assertEquals(0, secondCategories.metrics.categoriesEvaluated)
        assertEquals(1, secondItems.metrics.itemCacheHits)
        assertEquals(0, secondItems.metrics.individuallyAnalyzedItems)
    }

    @Test
    fun `structured adult rating overrides safe category without invoking full scorer`() {
        var itemCalls = 0
        val engine = KidsCatalogFilterEngine(
            filter = filter,
            itemEvaluator = {
                itemCalls += 1
                filter.evaluateItem(it)
            },
        )
        val categoryBatch = engine.evaluateCategories(listOf(KidsCategoryInput("kids", "KIDS France")))
        val result = engine.filterItems(
            listOf(
                item("adult-rated", "kids", "Unexpected entry", ageRating = "18+"),
            ),
            categoryBatch.decisions,
        )

        assertTrue(result.acceptedItems.isEmpty())
        assertEquals(0, itemCalls)
        assertEquals(1, result.metrics.rejectedItems)
    }

    private fun item(
        id: String,
        categoryId: String,
        title: String,
        kind: KidsContentKind = KidsContentKind.MOVIE,
        genres: String? = null,
        ageRating: String? = null,
    ): KidsItemInput<String> = KidsItemInput(
        value = id,
        id = id,
        categoryId = categoryId,
        metadata = KidsContentMetadata(
            kind = kind,
            title = title,
            genres = genres,
            ageRating = ageRating,
        ),
    )
}
