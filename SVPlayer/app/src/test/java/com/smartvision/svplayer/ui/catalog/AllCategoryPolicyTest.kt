package com.smartvision.svplayer.ui.catalog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AllCategoryPolicyTest {
    @Test fun `recognizes localized all category variants`() {
        assertTrue(AllCategoryPolicy.isEquivalent("ALL"))
        assertTrue(AllCategoryPolicy.isEquivalent("Toutes les cha\u00eenes"))
        assertTrue(AllCategoryPolicy.isEquivalent("Tous les films"))
        assertTrue(AllCategoryPolicy.isEquivalent("Toutes les s\u00e9ries"))
        assertFalse(AllCategoryPolicy.isEquivalent("Nouveaux films"))
    }
}
