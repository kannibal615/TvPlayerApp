package com.smartvision.svplayer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryHistoryPolicyTest {
    @Test
    fun `sorts categories by watched count then recency`() {
        val categories = listOf("sports", "movies", "kids", "news")
        val signals = listOf(
            CategoryHistorySignal(categoryId = "movies", updatedAt = 20),
            CategoryHistorySignal(categoryId = "sports", updatedAt = 50),
            CategoryHistorySignal(categoryId = "movies", updatedAt = 10),
            CategoryHistorySignal(categoryId = "kids", updatedAt = 80),
        )

        val sorted = categories.sortedByHistorySignals(signals) { it }

        assertEquals(listOf("movies", "kids", "sports", "news"), sorted)
    }

    @Test
    fun `keeps original order when there are no signals`() {
        val categories = listOf("all", "favorites", "history", "sports")

        val sorted = categories.sortedByHistorySignals(emptyList()) { it }

        assertEquals(categories, sorted)
    }
}
