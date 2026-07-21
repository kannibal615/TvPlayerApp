package com.smartvision.svplayer.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogCategoryPersistencePolicyTest {
    @Test
    fun `preserves folders when a partial sync keeps media without usable categories`() {
        assertFalse(CatalogCategoryPersistencePolicy.shouldReplace(usableCategoryCount = 0, retainedItemCount = 1))
    }

    @Test
    fun `replaces folders when a usable category snapshot exists`() {
        assertTrue(CatalogCategoryPersistencePolicy.shouldReplace(usableCategoryCount = 1, retainedItemCount = 1))
    }

    @Test
    fun `clears folders when the section contains no retained media`() {
        assertTrue(CatalogCategoryPersistencePolicy.shouldReplace(usableCategoryCount = 0, retainedItemCount = 0))
    }
}
