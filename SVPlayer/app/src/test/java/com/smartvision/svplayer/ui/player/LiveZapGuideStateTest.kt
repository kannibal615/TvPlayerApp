package com.smartvision.svplayer.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveZapGuideStateTest {
    @Test
    fun aNewZapKeepsTheGuideVisibleAndInvalidatesThePreviousTimeout() {
        val state = LiveZapGuideState()
        state.updateChannels(
            listOf(LiveZapGuideItem(streamId = 10, title = "Ten", imageUrl = null)),
        )

        val firstRequest = state.show(streamId = 10)
        val secondRequest = state.show(streamId = 11)
        state.hide(firstRequest)

        assertTrue(state.visible)
        state.hide(secondRequest)
        assertFalse(state.visible)
    }
}
