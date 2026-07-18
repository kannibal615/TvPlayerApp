package com.smartvision.svplayer.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HomePreviewSessionIdTest {
    @Test
    fun `same content in different rows gets distinct preview sessions`() {
        val continueSession = homePreviewSessionId("continue", "movie:42")
        val trendingSession = homePreviewSessionId("trending-movies", "movie:42")

        assertEquals("continue:movie:42", continueSession)
        assertEquals("trending-movies:movie:42", trendingSession)
        assertNotEquals(continueSession, trendingSession)
    }
}
