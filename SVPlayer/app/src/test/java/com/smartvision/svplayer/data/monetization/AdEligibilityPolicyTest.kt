package com.smartvision.svplayer.data.monetization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdEligibilityPolicyTest {
    private val config = AdConfig(adTagUrl = "https://example.test/vast")
    private val now = 10_000_000L

    @Test
    fun `premium and active trial never receive ads`() {
        assertEquals(
            "utilisateur Premium: pub ignoree",
            reason(MonetizationStatus.PREMIUM_ACTIVE),
        )
        assertEquals(
            "essai actif: pub ignoree",
            reason(MonetizationStatus.TRIAL_ACTIVE),
        )
    }

    @Test
    fun `free user receives an eligible preroll`() {
        assertNull(reason(MonetizationStatus.FREE_WITH_ADS))
    }

    @Test
    fun `minimum interval blocks rapid channel changes`() {
        assertEquals(
            "intervalle 30 minutes non atteint",
            reason(
                status = MonetizationStatus.FREE_WITH_ADS,
                frequency = AdFrequencySnapshot(lastAdTimestamp = now - 2 * 60_000L),
            ),
        )
    }

    @Test
    fun `daily cap blocks the fourth ad`() {
        assertEquals(
            "limite journaliere atteinte",
            reason(
                status = MonetizationStatus.FREE_WITH_ADS,
                frequency = AdFrequencySnapshot(adsSeenToday = 3),
            ),
        )
    }

    @Test
    fun `premium transition disables ads immediately`() {
        assertNull(reason(MonetizationStatus.FREE_WITH_ADS))
        assertEquals(
            "utilisateur Premium: pub ignoree",
            reason(MonetizationStatus.PREMIUM_ACTIVE),
        )
    }

    private fun reason(
        status: MonetizationStatus,
        frequency: AdFrequencySnapshot = AdFrequencySnapshot(),
    ): String? =
        AdEligibilityPolicy.refusalReason(
            status = status,
            contentType = PlayerContentType.LIVE_TV,
            config = config,
            frequency = frequency,
            nowMillis = now,
            runtimeConfigured = true,
            requestAlreadyPending = false,
        )
}
