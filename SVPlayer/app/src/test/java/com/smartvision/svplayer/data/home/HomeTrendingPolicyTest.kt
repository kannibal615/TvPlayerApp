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
        val artwork: String? = null,
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
    fun `movie trends require a runtime strictly greater than eighty minutes`() {
        assertFalse(HomeTrendingPolicy.isEligibleMovieDuration("80 min"))
        assertFalse(HomeTrendingPolicy.isEligibleMovieDuration("01:20:00"))
        assertTrue(HomeTrendingPolicy.isEligibleMovieDuration("81 min"))
        assertTrue(HomeTrendingPolicy.isEligibleMovieDuration("01:20:01"))
        assertFalse(HomeTrendingPolicy.isEligibleMovieDuration(null as String?))
        assertFalse(HomeTrendingPolicy.isEligibleMovieDuration("unknown"))
    }

    @Test
    fun `duration filtering backfills candidates without changing ranking`() {
        val ranked = listOf(
            "unknown" to null,
            "boundary" to 4_800_000L,
            "first" to 5_400_000L,
            "short" to 3_000_000L,
            "second" to 4_800_001L,
            "third" to 6_000_000L,
        )

        val selected = HomeTrendingPolicy.selectEligibleMoviesPreservingOrder(
            candidates = ranked,
            durationMsOf = { it.second },
            limit = 3,
        )

        assertEquals(listOf("first", "second", "third"), selected.map { it.first })
    }

    @Test
    fun `selection replaces duplicate artwork with the next distinct candidate`() {
        val rated = listOf(
            Item(1, 9.5f, 300, 2025, artwork = "HTTPS://img.example/poster.jpg?size=large"),
            Item(2, 9.0f, 200, 2024, artwork = "https://img.example/poster.jpg?size=small"),
            Item(3, 8.5f, 100, 2023, artwork = "https://img.example/other.jpg"),
        )
        val newest = listOf(
            Item(4, null, 500, 2025, artwork = "https://img.example/latest.jpg"),
        )

        val selected = select(rated, newest, 3)

        assertEquals(listOf(1, 3, 4), selected.map(Item::id))
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
            artworkKeyOf = Item::artwork,
            limit = limit,
        )
}
