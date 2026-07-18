package com.smartvision.svplayer.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentPrefixPolicyTest {
    @Test
    fun detectsEverySupportedPrefixSyntaxAtTheStartOfTheTitle() {
        assertEquals("FR", ContentPrefixPolicy.detect("FR - Eurosport"))
        assertEquals("AR", ContentPrefixPolicy.detect("AR:Al Jazeera"))
        assertEquals("EN", ContentPrefixPolicy.detect("EN | BBC News"))
        assertEquals("UK", ContentPrefixPolicy.detect("[UK] Sky Sports"))
        assertEquals("X", ContentPrefixPolicy.detect("[X] Private channel"))
        assertEquals("X", ContentPrefixPolicy.detect("[x] Private channel"))
    }

    @Test
    fun rejectsFoldersWordsAndNonUppercaseCodes() {
        assertNull(ContentPrefixPolicy.detect("Sports / FR - Eurosport"))
        assertNull(ContentPrefixPolicy.detect("FR Eurosport"))
        assertNull(ContentPrefixPolicy.detect("fr - Eurosport"))
        assertNull(ContentPrefixPolicy.detect("F - Eurosport"))
        assertNull(ContentPrefixPolicy.detect("X - Eurosport"))
        assertNull(ContentPrefixPolicy.detect("[fr] Eurosport"))
        assertNull(ContentPrefixPolicy.detect("FRENCH - Eurosport"))
    }

    @Test
    fun emptySelectionKeepsEverythingAndMultipleSelectionUsesOr() {
        assertTrue(ContentPrefixPolicy.accepts("No prefix", emptySet()))
        assertTrue(ContentPrefixPolicy.accepts("FR - Film", setOf("FR", "AR")))
        assertTrue(ContentPrefixPolicy.accepts("AR:Film", setOf("FR", "AR")))
        assertFalse(ContentPrefixPolicy.accepts("EN | Film", setOf("FR", "AR")))
        assertFalse(ContentPrefixPolicy.accepts("No prefix", setOf("FR", "AR")))
    }

    @Test
    fun detectedCodesExtendThePredefinedRegistryWithoutDuplicates() {
        val options = ContentPrefixPolicy.optionsWithDetected(setOf("fr", "ZZ"))
        assertEquals(1, options.count { it.code == "FR" })
        assertEquals("ZZ", options.single { it.code == "ZZ" }.englishLabel)
    }

    @Test
    fun selectedCodesAreNormalizedIncludedAndSortedFirst() {
        val options = ContentPrefixPolicy.optionsWithDetected(
            detectedCodes = setOf("ZZ"),
            selectedCodes = setOf("zz", "x"),
        )

        assertEquals(listOf("X", "ZZ"), options.take(2).map { it.code })
        assertEquals(setOf("X", "ZZ"), ContentPrefixPolicy.normalize(setOf("x", "zz")))
    }
}
