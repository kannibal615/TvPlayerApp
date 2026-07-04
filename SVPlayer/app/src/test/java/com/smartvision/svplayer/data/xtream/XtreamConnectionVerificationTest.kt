package com.smartvision.svplayer.data.xtream

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamConnectionVerificationTest {
    @Test
    fun `two transient network errors followed by success never blocks catalog`() = runTest {
        val pendingStates = mutableListOf<XtreamConnectionState>()
        val results = ArrayDeque(
            listOf(
                failure(XtreamConnectionStatus.NETWORK_ERROR),
                failure(XtreamConnectionStatus.NETWORK_ERROR),
                success(),
            ),
        )

        val result = runConfirmedXtreamVerification(
            maxAttempts = 3,
            retryDelayMillis = 1,
            checkOnce = { results.removeFirst() },
            onPendingFailure = pendingStates::add,
            delayBeforeRetry = {},
        )

        assertTrue(result.isConnected)
        assertFalse(result.confirmedFailure)
        assertFalse(result.blocksCatalog)
        assertEquals(2, pendingStates.size)
        assertTrue(pendingStates.all { it.checking && !it.confirmedFailure && !it.blocksCatalog })
    }

    @Test
    fun `three network errors confirm failure and block catalog`() = runTest {
        val pendingStates = mutableListOf<XtreamConnectionState>()
        val results = ArrayDeque(
            listOf(
                failure(XtreamConnectionStatus.NETWORK_ERROR),
                failure(XtreamConnectionStatus.NETWORK_ERROR),
                failure(XtreamConnectionStatus.NETWORK_ERROR),
            ),
        )

        val result = runConfirmedXtreamVerification(
            maxAttempts = 3,
            retryDelayMillis = 1,
            checkOnce = { results.removeFirst() },
            onPendingFailure = pendingStates::add,
            delayBeforeRetry = {},
        )

        assertEquals(XtreamConnectionStatus.NETWORK_ERROR, result.status)
        assertTrue(result.confirmedFailure)
        assertTrue(result.blocksCatalog)
        assertEquals(3, result.attempt)
        assertEquals(2, pendingStates.size)
        assertTrue(pendingStates.all { !it.blocksCatalog })
    }

    @Test
    fun `invalid credentials are confirmed before blocking catalog`() = runTest {
        val pendingStates = mutableListOf<XtreamConnectionState>()

        val result = runConfirmedXtreamVerification(
            maxAttempts = 3,
            retryDelayMillis = 1,
            checkOnce = { failure(XtreamConnectionStatus.INVALID_CREDENTIALS) },
            onPendingFailure = pendingStates::add,
            delayBeforeRetry = {},
        )

        assertEquals(XtreamConnectionStatus.INVALID_CREDENTIALS, result.status)
        assertTrue(result.confirmedFailure)
        assertTrue(result.blocksCatalog)
        assertTrue(pendingStates.all { !it.confirmedFailure && !it.blocksCatalog })
    }

    private fun success(): XtreamConnectionState =
        XtreamConnectionState(
            status = XtreamConnectionStatus.CONNECTED,
            message = "ok",
            serverUrl = "http://example.test",
        )

    private fun failure(status: XtreamConnectionStatus): XtreamConnectionState =
        XtreamConnectionState(
            status = status,
            message = "failed",
            serverUrl = "http://example.test",
        )
}
