package com.smartvision.svplayer.ui.home

import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.mock.HomeVisualStyle
import com.smartvision.svplayer.domain.repository.CatalogContentCounts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLoadPolicyTest {
    @Test
    fun rejectsResultsFromAnOldProfileOrCatalogRevision() {
        val token = HomeLoadToken(profileId = "walid", catalogRevision = 12L)

        assertTrue(shouldApplyHomeLoadResult(token, "walid", 12L))
        assertFalse(shouldApplyHomeLoadResult(token, "nouran", 12L))
        assertFalse(shouldApplyHomeLoadResult(token, "walid", 13L))
    }

    @Test
    fun hidesAllProfileScopedContentUntilTheActiveProfileStateArrives() {
        val stale = HomeUiState(
            profileId = "walid",
            continueWatching = listOf(
                ContinueItem(
                    id = "live:1",
                    title = "Walid channel",
                    meta = "",
                    remaining = "",
                    progress = 0f,
                    visualStyle = HomeVisualStyle.Signal,
                ),
            ),
            catalogCounts = CatalogContentCounts(live = 100, movies = 200, series = 300),
            continueWatchingLoading = false,
            trendingLoading = false,
            catalogCountsLoading = false,
        )

        val visible = stale.visibleForProfile("nouran")

        assertEquals("nouran", visible.profileId)
        assertTrue(visible.continueWatching.isEmpty())
        assertEquals(CatalogContentCounts(), visible.catalogCounts)
        assertTrue(visible.continueWatchingLoading)
        assertTrue(visible.trendingLoading)
        assertTrue(visible.catalogCountsLoading)
    }
}
