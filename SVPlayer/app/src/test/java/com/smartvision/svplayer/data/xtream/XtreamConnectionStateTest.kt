package com.smartvision.svplayer.data.xtream

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamConnectionStateTest {
    @Test
    fun `connected state does not block catalog while quick check is refreshing`() {
        val state = XtreamConnectionState(
            status = XtreamConnectionStatus.CONNECTED,
            checking = true,
        )

        assertFalse(state.blocksCatalogForNavigation)
    }

    @Test
    fun `unknown state blocks catalog while quick check is running`() {
        val state = XtreamConnectionState(
            status = XtreamConnectionStatus.UNKNOWN,
            checking = true,
        )

        assertTrue(state.blocksCatalogForNavigation)
    }

    @Test
    fun `connection error blocks catalog navigation`() {
        val state = XtreamConnectionState(
            status = XtreamConnectionStatus.NETWORK_ERROR,
            checking = false,
        )

        assertTrue(state.blocksCatalogForNavigation)
    }
}
