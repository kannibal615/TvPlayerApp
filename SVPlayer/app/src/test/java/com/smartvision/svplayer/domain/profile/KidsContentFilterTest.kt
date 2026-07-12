package com.smartvision.svplayer.domain.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KidsContentFilterTest {
    private val filter = KidsContentFilter()

    @Test
    fun `normalizes accents and multilingual kids terms`() {
        assertTrue(filter.isAllowed("Dessins animés"))
        assertTrue(filter.isAllowed("Niños"))
        assertTrue(filter.isAllowed("رسوم متحركة"))
        assertTrue(filter.isAllowed("Çocuk TV"))
    }

    @Test
    fun `explicit exclusion wins over a kids match`() {
        assertFalse(filter.isAllowed("Kids", "Adult XXX"))
        assertFalse(filter.isAllowed("Cartoons 18+"))
    }

    @Test
    fun `medium evidence requires corroboration`() {
        assertFalse(filter.isAllowed("Family"))
        assertTrue(filter.isAllowed("Family animation"))
    }

    @Test
    fun `unknown content is denied`() {
        assertFalse(filter.isAllowed("General entertainment"))
        assertFalse(filter.isAllowed(null, ""))
    }
}
