package com.smartvision.svplayer.data.monetization

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MonetizationStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `daily counter resets when local date changes`() = runBlocking {
        val clock = MutableClock(Instant.parse("2026-06-24T10:00:00Z"))
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            temporaryFolder.newFile("monetization.preferences_pb")
        }
        val store = MonetizationStore(dataStore, clock, ZoneOffset.UTC)

        store.recordAdStarted()
        assertEquals(1, store.frequencySnapshot().adsSeenToday)

        clock.current = Instant.parse("2026-06-25T00:01:00Z")
        val nextDay = store.frequencySnapshot()
        assertEquals(0, nextDay.adsSeenToday)
        assertEquals("2026-06-25", nextDay.counterDate)
    }
}

private class MutableClock(
    var current: Instant,
    private val currentZone: ZoneId = ZoneOffset.UTC,
) : Clock() {
    override fun getZone(): ZoneId = currentZone
    override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)
    override fun instant(): Instant = current
}
