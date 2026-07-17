package com.smartvision.svplayer.ui.home

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
}
