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
    fun `unknown state does not block catalog while quick check is running`() {
        val state = XtreamConnectionState(
            status = XtreamConnectionStatus.UNKNOWN,
            checking = true,
        )

        assertFalse(state.blocksCatalogForNavigation)
    }

    @Test
    fun `unconfirmed connection error does not block catalog navigation`() {
        val state = XtreamConnectionState(
            status = XtreamConnectionStatus.NETWORK_ERROR,
            checking = false,
        )

        assertFalse(state.blocksCatalogForNavigation)
    }

    @Test
    fun `confirmed connection error blocks catalog navigation`() {
        val state = XtreamConnectionState(
            status = XtreamConnectionStatus.NETWORK_ERROR,
            checking = false,
            confirmedFailure = true,
        )

        assertTrue(state.blocksCatalogForNavigation)
    }
}
