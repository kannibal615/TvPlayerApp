package com.smartvision.svplayer.data.monetization

import org.junit.Assert.assertEquals
import org.junit.Test

class MonetizationStatusTest {
    @Test
    fun `premium has priority over expired trial`() {
        assertEquals(
            MonetizationStatus.PREMIUM_ACTIVE,
            resolveMonetizationStatus(
                activationType = "smartvision_code",
                licenseStatus = "active",
                trialStatus = "expired",
                freeWithAdsStatus = "inactive",
                debugOverride = null,
            ),
        )
    }

    @Test
    fun `active free mode is restored without expiration popup`() {
        assertEquals(
            MonetizationStatus.FREE_WITH_ADS,
            resolveMonetizationStatus(
                activationType = "free_ads",
                licenseStatus = "expired",
                trialStatus = "expired",
                freeWithAdsStatus = "active",
                debugOverride = null,
            ),
        )
    }

    @Test
    fun `license and trial expiration stay distinguishable`() {
        assertEquals(
            MonetizationStatus.LICENSE_EXPIRED,
            resolveMonetizationStatus(null, "expired", "used", "inactive", null),
        )
        assertEquals(
            MonetizationStatus.TRIAL_EXPIRED,
            resolveMonetizationStatus(null, "inactive", "expired", "inactive", null),
        )
    }
}
