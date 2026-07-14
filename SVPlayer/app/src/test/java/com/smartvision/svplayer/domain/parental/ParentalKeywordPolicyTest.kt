package com.smartvision.svplayer.domain.parental

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentalKeywordPolicyTest {
    @Test
    fun `migrates legacy delimiters while preserving order`() {
        assertEquals(
            listOf("adults", "porn", "XXX", "violence"),
            ParentalKeywordPolicy.parseJsonOrLegacy(null, " adults; porn, XXX | violence "),
        )
    }

    @Test
    fun `json preserves phrases and ignores case insensitive duplicates`() {
        val original = listOf("Adult content", "Crime, violence", "adult CONTENT")
        val encoded = ParentalKeywordPolicy.serialize(original)

        assertEquals(
            listOf("Adult content", "Crime, violence"),
            ParentalKeywordPolicy.parseJsonOrLegacy(encoded, null),
        )
    }

    @Test
    fun `normalization trims collapses spaces and drops blanks`() {
        assertEquals(
            listOf("adult content", "xxx"),
            ParentalKeywordPolicy.normalize(listOf("  adult   content ", "", " xxx ", "XXX")),
        )
    }

    @Test
    fun `storage limit is enforced on serialized value`() {
        assertTrue(ParentalKeywordPolicy.fitsStorage(listOf("adult", "violence")))
        assertFalse(ParentalKeywordPolicy.fitsStorage(listOf("x".repeat(ParentalKeywordPolicy.MaxSerializedLength))))
    }

    @Test
    fun `invalid json falls back to legacy value`() {
        assertEquals(
            listOf("adult", "xxx"),
            ParentalKeywordPolicy.parseJsonOrLegacy("not-json", "adult; xxx"),
        )
    }
}
