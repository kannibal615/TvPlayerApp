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
}
