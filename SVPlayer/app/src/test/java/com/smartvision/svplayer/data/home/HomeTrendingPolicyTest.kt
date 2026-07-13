package com.smartvision.svplayer.data.home

import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.mock.HomeVisualStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTrendingPolicyTest {
    private data class Item(
        val id: Int,
        val rating: Float?,
        val addedAt: Long,
        val year: Int?,
        val adult: Boolean = false,
    )

    @Test
    fun `selection prioritizes ratings then uses stable tie breakers`() {
        val items = listOf(
            Item(3, 8.5f, 100, 2024),
            Item(2, 9.0f, 100, 2023),
            Item(1, 9.0f, 200, 2022),
        )
        val selected = select(items, emptyList(), 3)
        assertEquals(listOf(1, 2, 3), selected.map(Item::id))
    }

    @Test
    fun `selection fills missing rated content with newest without duplicates or adult content`() {
        val rated = listOf(Item(1, 9f, 10, 2020), Item(9, 10f, 99, 2025, adult = true))
        val newest = listOf(Item(1, null, 500, 2025), Item(2, null, 400, 2024), Item(3, null, 300, 2023))
        val selected = select(rated, newest, 3)
        assertEquals(listOf(1, 2, 3), selected.map(Item::id))
    }

    @Test
    fun `novelty and adult detection normalize accents and case`() {
        assertTrue(HomeTrendingPolicy.isNoveltyCategory("  R\u00C9CEMMENT   AJOUT\u00C9S "))
        assertTrue(HomeTrendingPolicy.isNoveltyCategory("New Releases"))
        assertTrue(HomeTrendingPolicy.containsAdultMarker("Films \u00C9rotiques"))
        assertFalse(HomeTrendingPolicy.containsAdultMarker("Films famille"))
    }

    @Test
    fun `prepared preview never replaces the card image`() {
        val item = ContinueItem(
            id = "movie-42",
            title = "Stable image",
            meta = "Movie",
            remaining = "",
            progress = 0f,
            visualStyle = HomeVisualStyle.Cinema,
            imageUrl = "https://catalog.example/poster.jpg",
        )
        val prepared = HomeTrendingPreparedPreview(
            contentType = "movie",
            contentId = 42,
            posterUrl = "https://metadata.example/new-poster.jpg",
            backdropUrl = "https://metadata.example/backdrop.jpg",
            durationLabel = "1h 30m",
            durationMs = 5_400_000L,
            previewUrl = "https://stream.example/preview.ts",
            previewStartPositionMs = 10_000L,
            previewFallbackStartPositionMs = 0L,
            sampleLabel = null,
            backdropAvailable = true,
            previewAvailable = true,
        )

        val result = prepared.applyTo(item)

        assertEquals(item.imageUrl, result.imageUrl)
        assertEquals(prepared.backdropUrl, result.previewImageUrl)
        assertTrue(result.previewPrepared)
    }

    private fun select(rated: List<Item>, newest: List<Item>, limit: Int): List<Item> =
        HomeTrendingPolicy.selectDeterministic(
            ratedCandidates = rated,
            newestCandidates = newest,
            idOf = Item::id,
            ratingOf = Item::rating,
            addedAtOf = Item::addedAt,
            yearOf = Item::year,
            allowed = { !it.adult },
            limit = limit,
        )
}
