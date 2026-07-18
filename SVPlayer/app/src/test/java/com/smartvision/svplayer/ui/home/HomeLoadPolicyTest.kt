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
    fun allowsProfileTransitionWhileContinueWatchingFinishesAsynchronously() {
        val state = HomeUiState(
            profileId = "kids",
            continueWatchingLoading = true,
            trendingLoading = false,
            catalogCountsLoading = false,
            catalogRevision = 9L,
            loadedCatalogRevision = 9L,
            syncInProgress = false,
        )

        assertTrue(
            isHomeContentReady(
                state,
                activeProfileId = "kids",
                expectedCatalogRevision = 9L,
            ),
        )
    }

    @Test
    fun keepsProfileTransitionBlockedUntilCatalogStateIsCurrent() {
        val ready = HomeUiState(
            profileId = "kids",
            continueWatchingLoading = false,
            trendingLoading = false,
            catalogCountsLoading = false,
            catalogRevision = 9L,
            loadedCatalogRevision = 9L,
            syncInProgress = false,
        )

        assertFalse(isHomeContentReady(ready.copy(profileId = "admin"), "kids", 9L))
        assertFalse(isHomeContentReady(ready.copy(syncInProgress = true), "kids", 9L))
        assertFalse(isHomeContentReady(ready.copy(loadedCatalogRevision = 8L), "kids", 9L))
        assertFalse(isHomeContentReady(ready.copy(trendingLoading = true), "kids", 9L))
        assertFalse(isHomeContentReady(ready.copy(catalogCountsLoading = true), "kids", 9L))
        assertFalse(isHomeContentReady(ready, "kids", 10L))
    }

    @Test
    fun rejectsResultsFromAnOldProfileOrCatalogRevision() {
        val token = HomeLoadToken(profileId = "walid", catalogRevision = 12L)

        assertTrue(shouldApplyHomeLoadResult(token, "walid", 12L))
        assertFalse(shouldApplyHomeLoadResult(token, "nouran", 12L))
        assertFalse(shouldApplyHomeLoadResult(token, "walid", 13L))
    }

    @Test
    fun rejectsCancelledOrSupersededAttemptEvenWhenProfileAndRevisionMatch() {
        val token = HomeLoadToken(profileId = "kids", catalogRevision = 9L)

        assertFalse(
            shouldApplyHomeLoadAttempt(token, 1L, 4L, "kids", 9L, 2L, 4L),
        )
        assertFalse(
            shouldApplyHomeLoadAttempt(token, 2L, 3L, "kids", 9L, 2L, 4L),
        )
        assertTrue(
            shouldApplyHomeLoadAttempt(token, 2L, 4L, "kids", 9L, 2L, 4L),
        )
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

    @Test
    fun adminCountsAreNeverRenderedAfterKidsHomeBecomesReady() {
        val adminCounts = CatalogContentCounts(live = 2_958, movies = 20_790, series = 10_022)
        val kidsCounts = CatalogContentCounts(live = 677, movies = 2_404, series = 1_322)
        val adminReady = HomeUiState(
            profileId = "admin",
            catalogCounts = adminCounts,
            trendingLoading = false,
            catalogCountsLoading = false,
            catalogRevision = 8L,
            loadedCatalogRevision = 8L,
        )

        val switchingToKids = adminReady.visibleForProfile("kids")
        assertEquals(CatalogContentCounts(), switchingToKids.catalogCounts)
        assertTrue(switchingToKids.catalogCountsLoading)
        assertFalse(isHomeContentReady(switchingToKids, "kids", 8L))

        val kidsReady = adminReady.copy(
            profileId = "kids",
            catalogCounts = kidsCounts,
            catalogRevision = 9L,
            loadedCatalogRevision = 9L,
        ).visibleForProfile("kids")
        assertTrue(isHomeContentReady(kidsReady, "kids", 9L))
        assertEquals(kidsCounts, kidsReady.catalogCounts)
        assertFalse(kidsReady.catalogCounts == adminCounts)
    }
}
