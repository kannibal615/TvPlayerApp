package com.smartvision.svplayer.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KidsContentFilterTest {
    private val filter = KidsContentFilter()

    @Test
    fun `accepts required multilingual and compact kids categories`() {
        listOf(
            "KIDS France 150",
            "USAKIDS 95",
            "UK_KIDS",
            "AR | أطفال",
            "AR | كرتون",
            "للأطفال",
            "ES Infantil",
            "Dibujos Animados",
            "Crianças Portugal",
            "Bambini Italia",
            "Çocuk Türkiye",
            "Kinder Deutschland",
            "Cartoons UK",
        ).forEach { category ->
            assertEquals(category, KidsCategoryClassification.SAFE_KIDS_CATEGORY, filter.evaluateCategory(category).classification)
        }
    }

    @Test
    fun `rejects adult and insufficient categories`() {
        listOf("ADULT KIDS 18+", "KIDS 16+", "XXX Kids", "Adult Animation", "Family Cinema", "Anime")
            .forEach { category ->
                assertFalse(category, filter.evaluateCategory(category).safe)
            }
        assertEquals(
            KidsCategoryClassification.NOT_KIDS_CATEGORY,
            filter.evaluateCategory("Family Cinema").classification,
        )
        assertEquals(
            KidsCategoryClassification.NOT_KIDS_CATEGORY,
            filter.evaluateCategory("Anime").classification,
        )
    }

    @Test
    fun `normalization preserves non latin alphabets and whole words`() {
        assertEquals("ar اطفال", filter.normalize("AR | أَطْفَال"))
        assertEquals("cocuk turkiye", filter.normalize("Çocuk_Türkiye"))
        assertEquals("дети мультфильмы", filter.normalize("Дети / мультфильмы"))

        val kidnapped = filter.evaluateItem(
            KidsContentMetadata(kind = KidsContentKind.MOVIE, title = "Kidnapped"),
        )
        assertEquals(0, kidnapped.score)
        assertFalse(kidnapped.reason.contains("TITLE_KIDS_TERM"))
    }

    @Test
    fun `movie and series scoring requires corroborating signals`() {
        assertTrue(
            filter.evaluateItem(
                KidsContentMetadata(
                    kind = KidsContentKind.MOVIE,
                    title = "Spy Kids",
                    genres = "Family",
                    ageRating = "G",
                ),
            ).allowed,
        )
        assertFalse(filter.evaluateItem(KidsContentMetadata(KidsContentKind.MOVIE, "The Kids Are All Right")).allowed)
        assertFalse(filter.evaluateItem(KidsContentMetadata(KidsContentKind.MOVIE, "Kids", ageRating = "TV-MA")).allowed)
        assertTrue(
            filter.evaluateItem(
                KidsContentMetadata(
                    kind = KidsContentKind.SERIES,
                    title = "Friendly Adventures",
                    genres = "Animation, Family",
                    ageRating = "TV-Y7",
                ),
            ).allowed,
        )
        assertFalse(
            filter.evaluateItem(
                KidsContentMetadata(
                    kind = KidsContentKind.SERIES,
                    title = "Animated Night",
                    genres = "Animation",
                    ageRating = "TV-MA",
                ),
            ).allowed,
        )
        assertFalse(filter.evaluateItem(KidsContentMetadata(KidsContentKind.MOVIE, "Family Story", genres = "Family")).allowed)
    }

    @Test
    fun `adult animation titles are not accepted from animation genre alone`() {
        listOf("South Park", "Family Guy", "Rick and Morty").forEach { title ->
            assertFalse(
                title,
                filter.evaluateItem(
                    KidsContentMetadata(kind = KidsContentKind.SERIES, title = title, genres = "Animation"),
                ).allowed,
            )
        }
    }

    @Test
    fun `channel allowlist uses controlled full aliases`() {
        assertTrue(filter.evaluateItem(KidsContentMetadata(KidsContentKind.LIVE_CHANNEL, "FR | Disney Junior HD")).allowed)
        assertTrue(filter.evaluateItem(KidsContentMetadata(KidsContentKind.LIVE_CHANNEL, "Nick Jr.")).allowed)
        assertFalse(filter.evaluateItem(KidsContentMetadata(KidsContentKind.LIVE_CHANNEL, "Disney")).allowed)
        assertFalse(filter.evaluateItem(KidsContentMetadata(KidsContentKind.LIVE_CHANNEL, "Junior")).allowed)
        assertFalse(filter.evaluateItem(KidsContentMetadata(KidsContentKind.LIVE_CHANNEL, "Nick")).allowed)
    }
}
