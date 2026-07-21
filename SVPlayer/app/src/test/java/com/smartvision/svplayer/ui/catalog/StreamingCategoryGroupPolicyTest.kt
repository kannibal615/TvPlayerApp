package com.smartvision.svplayer.ui.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamingCategoryGroupPolicyTest {
    @Test fun `brand matching is case insensitive and token delimited`() {
        assertEquals(StreamingBrand.Netflix, StreamingCategoryGroupPolicy.brandFor("|FR| netflix cinema"))
        assertEquals(StreamingBrand.Prime, StreamingCategoryGroupPolicy.brandFor("PRIME VIDEO 4K"))
        assertEquals(StreamingBrand.Apple, StreamingCategoryGroupPolicy.brandFor("[US] APPLE TV+"))
        assertEquals(StreamingBrand.Disney, StreamingCategoryGroupPolicy.brandFor("FR - DISNEY+"))
        assertNull(StreamingCategoryGroupPolicy.brandFor("NETFLIXING"))
        assertNull(StreamingCategoryGroupPolicy.brandFor("PRIMETIME"))
    }

    @Test fun `groups use configured order and do not duplicate categories`() {
        val categories = listOf(
            TestCategory("1", "DISNEY KIDS"),
            TestCategory("2", "NETFLIX PRIME MIX"),
            TestCategory("3", "GENERAL"),
            TestCategory("4", "PRIME VIDEO"),
        )

        val result = StreamingCategoryGroupPolicy.group(categories, TestCategory::label)

        assertEquals(listOf(StreamingBrand.Netflix, StreamingBrand.Prime, StreamingBrand.Disney), result.groups.keys.toList())
        assertEquals(listOf("2"), result.groups.getValue(StreamingBrand.Netflix).map(TestCategory::id))
        assertEquals(listOf("4"), result.groups.getValue(StreamingBrand.Prime).map(TestCategory::id))
        assertEquals(listOf("1"), result.groups.getValue(StreamingBrand.Disney).map(TestCategory::id))
        assertEquals(listOf("3"), result.remaining.map(TestCategory::id))
        assertEquals(categories.size, result.groups.values.sumOf(List<TestCategory>::size) + result.remaining.size)
    }

    @Test fun `empty brands are omitted`() {
        val result = StreamingCategoryGroupPolicy.group(
            categories = listOf(TestCategory("1", "NETFLIX")),
            labelOf = TestCategory::label,
        )

        assertEquals(listOf(StreamingBrand.Netflix), result.groups.keys.toList())
    }

    @Test fun `accordion keeps at most one brand expanded`() {
        assertEquals(
            StreamingBrand.Netflix,
            StreamingCategoryGroupPolicy.toggleExpanded(null, StreamingBrand.Netflix),
        )
        assertEquals(
            StreamingBrand.Prime,
            StreamingCategoryGroupPolicy.toggleExpanded(StreamingBrand.Netflix, StreamingBrand.Prime),
        )
        assertNull(StreamingCategoryGroupPolicy.toggleExpanded(StreamingBrand.Prime, StreamingBrand.Prime))
    }

    private data class TestCategory(val id: String, val label: String)
}
