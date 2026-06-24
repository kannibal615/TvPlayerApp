package com.smartvision.svplayer.data.monetization

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdRequestGateTest {
    @Test
    fun `only one concurrent request is reserved`() {
        val gate = AdRequestGate()
        assertTrue(gate.reserve("first", 1_000L))
        assertFalse(gate.reserve("second", 1_001L))
    }

    @Test
    fun `impression is recorded only once`() {
        val gate = AdRequestGate()
        gate.reserve("request", 1_000L)
        assertTrue(gate.markStarted("request"))
        assertFalse(gate.markStarted("request"))
    }

    @Test
    fun `failure releases reservation and stale loads expire`() {
        val gate = AdRequestGate(timeoutMillis = 100L)
        gate.reserve("failed", 1_000L)
        assertTrue(gate.release("failed"))
        assertTrue(gate.reserve("retry", 1_001L))
        gate.expireStale(1_101L)
        assertFalse(gate.hasPending)
    }
}
