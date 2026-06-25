package com.smartvision.svplayer.data.monetization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RemoteAdConfigTest {
    @Test
    fun `remote frequency settings override local defaults`() {
        val config = RemoteAdConfig(
            success = true,
            adsEnabled = true,
            minMinutesBetweenAds = 1,
            maxAdsPerDay = 50,
            vastTagUrl = "https://example.test/vast",
        ).toAdConfig(AdConfig())

        assertEquals(1, config.minMinutesBetweenAds)
        assertEquals(50, config.maxAdsPerDay)
        assertEquals("https://example.test/vast", config.adTagUrl)
    }

    @Test
    fun `remote flags are applied`() {
        val config = RemoteAdConfig(
            success = true,
            adsEnabled = false,
            showAdBeforeMovie = false,
        ).toAdConfig(AdConfig())

        assertFalse(config.adsEnabled)
        assertFalse(config.showAdBeforeMovie)
    }
}
