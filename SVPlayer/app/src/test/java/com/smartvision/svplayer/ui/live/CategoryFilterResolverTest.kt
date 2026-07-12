package com.smartvision.svplayer.ui.live

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryFilterResolverTest {
    @Test fun `parser recognizes supported delimiters`() {
        val cases = mapOf(
            "|AR| WEYYAK" to "AR", "AR | WEYYAK" to "AR", "[AR] WEYYAK" to "AR",
            "(AR) WEYYAK" to "AR", "ar | weyyak" to "AR", "|EU| FRANCE 4K" to "EU",
            "IT | CINEMA" to "IT", "UK | NEWS" to "UK", "GB | NEWS" to "GB",
            "TR-SPORT" to "TR", "AR_ WEYYAK" to "AR", "AR: WEYYAK" to "AR",
        )
        cases.forEach { (input, expected) -> assertEquals(input, expected, CategoryCodeParser.parse(input)) }
    }

    @Test fun `parser rejects ordinary words and blanks`() {
        listOf("MOVIES", "SPORT", "", "   ").forEach { assertNull(it, CategoryCodeParser.parse(it)) }
    }

    @Test fun `custom mappings win over ISO and aliases are normalized`() {
        assertEquals("Monde arabe", CategoryFilterResolver.resolve("AR", Locale.FRENCH).displayName)
        assertEquals("GB", CategoryFilterResolver.resolve("UK", Locale.FRENCH).normalizedCode)
        assertEquals("Royaume-Uni", CategoryFilterResolver.resolve("GB", Locale.FRENCH).displayName)
        assertEquals(CategoryFilterType.UNKNOWN, CategoryFilterResolver.resolve("ZZ").type)
    }

    @Test fun `filters are unique ordered and filter original categories`() {
        val categories = listOf(
            category("1", "|IT| CINEMA"), category("2", "IT | SPORT"),
            category("3", "|AR| WEYYAK"), category("4", "MOVIES"), category("5", "|ZZ| TEST"),
        )
        val filters = CategoryFilterResolver.buildFilters(categories, Locale.FRENCH)
        assertEquals(listOf("AR", "IT", "ZZ"), filters.map { it.identity.normalizedCode })
        assertEquals(2, filters.first { it.identity.normalizedCode == "IT" }.categoryCount)
        assertEquals(listOf("1", "2"), CategoryFilterResolver.filterCategories(categories, "IT").map { it.id })
        assertEquals(categories, CategoryFilterResolver.filterCategories(categories, null))
    }

    @Test fun `playlist change naturally rebuilds available filters`() {
        val before = CategoryFilterResolver.buildFilters(listOf(category("1", "|FR| NEWS")), Locale.FRENCH)
        val after = CategoryFilterResolver.buildFilters(listOf(category("2", "|TR| SPORT")), Locale.FRENCH)
        assertEquals(listOf("FR"), before.map { it.identity.normalizedCode })
        assertEquals(listOf("TR"), after.map { it.identity.normalizedCode })
    }

    @Test fun `active filter moves first while all category remains available`() {
        val all = category(AllLiveCategoryId, "ALL")
        val categories = listOf(all, category("1", "|DE| NEWS"), category("2", "|FR| CINEMA"))
        val filters = CategoryFilterResolver.buildFilters(categories, Locale.FRENCH)
        val ordered = orderFiltersForBar(filters, "FR")

        assertEquals("FR", ordered.first().identity.normalizedCode)
        assertEquals(listOf(AllLiveCategoryId, "2"), CategoryFilterResolver.filterCategories(categories, "FR").map { it.id })
        assertTrue(CategoryFilterResolver.filterCategories(categories, "FR").first().id == AllLiveCategoryId)
    }

    private fun category(id: String, label: String) =
        LiveTvCategory(id, label, 1, LiveTvCategoryKind.Generic)
}
