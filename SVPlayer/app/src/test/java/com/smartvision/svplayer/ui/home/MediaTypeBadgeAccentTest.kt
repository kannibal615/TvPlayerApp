package com.smartvision.svplayer.ui.home

import com.smartvision.svplayer.ui.theme.SmartVisionColors
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaTypeBadgeAccentTest {
    @Test
    fun `maps media types to home category accents`() {
        assertEquals(SmartVisionColors.CyanAccent, mediaTypeBadgeAccent("LIVE"))
        assertEquals(SmartVisionColors.Warning, mediaTypeBadgeAccent("FILM"))
        assertEquals(SmartVisionColors.Primary, mediaTypeBadgeAccent("SERIE"))
    }

    @Test
    fun `uses secondary text color for unknown media type`() {
        assertEquals(SmartVisionColors.TextSecondary, mediaTypeBadgeAccent("MEDIA"))
    }
}
