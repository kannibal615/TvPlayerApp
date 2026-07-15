package com.smartvision.svplayer.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncFrequencyPolicyTest {
    @Test
    fun `24h and 48h create periodic policies`() {
        assertEquals(24L, SyncFrequencyPolicy.from("24h").repeatHours)
        assertEquals(48L, SyncFrequencyPolicy.from("48h").repeatHours)
    }

    @Test
    fun `startup frequency runs on app start without periodic worker`() {
        val policy = SyncFrequencyPolicy.from("A chaque demarrage")

        assertNull(policy.repeatHours)
        assertTrue(policy.runOnStartup)
    }

    @Test
    fun `manual and never disable automatic sync`() {
        listOf("Manuelle", "Jamais").forEach { value ->
            val policy = SyncFrequencyPolicy.from(value)

            assertNull(policy.repeatHours)
            assertFalse(policy.runOnStartup)
        }
    }

    @Test
    fun `missing catalog or last success requires synchronization`() {
        assertTrue(SyncFrequencyPolicy.isSynchronizationDue("Manuelle", lastSyncAt = 100L, hasLocalCatalog = false, nowMs = 200L))
        assertTrue(SyncFrequencyPolicy.isSynchronizationDue("Manuelle", lastSyncAt = null, hasLocalCatalog = true, nowMs = 200L))
    }

    @Test
    fun `periodic policy uses the last successful synchronization timestamp`() {
        val hour = 60L * 60L * 1000L
        assertFalse(SyncFrequencyPolicy.isSynchronizationDue("24h", lastSyncAt = 10L * hour, hasLocalCatalog = true, nowMs = 33L * hour))
        assertTrue(SyncFrequencyPolicy.isSynchronizationDue("24h", lastSyncAt = 10L * hour, hasLocalCatalog = true, nowMs = 34L * hour))
    }

    @Test
    fun `startup policy is due while manual policy preserves a populated catalog`() {
        assertTrue(SyncFrequencyPolicy.isSynchronizationDue("A chaque demarrage", 100L, true, 200L))
        assertFalse(SyncFrequencyPolicy.isSynchronizationDue("Manuelle", 100L, true, 200L))
    }
}
