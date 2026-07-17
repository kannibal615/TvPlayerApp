package com.smartvision.svplayer.data.playlist

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpgProgramPolicyTest {
    private val now = 1_700_000_000_000L

    @Test
    fun `only current and upcoming programs are returned in chronological order`() {
        val programs = listOf(
            program("future", now + 60_000, now + 120_000),
            program("ended", now - 120_000, now - 60_000),
            program("current", now - 60_000, now + 60_000),
            program("unknown", null, null),
        )

        assertEquals(
            listOf("current", "future"),
            currentAndUpcomingPrograms(programs, now).map { it.title },
        )
    }

    @Test
    fun `program ending exactly now is no longer current`() {
        assertEquals(emptyList<EpgProgram>(), currentAndUpcomingPrograms(listOf(program("ended", now - 1, now)), now))
    }

    @Test
    fun `xmltv offset is converted to the correct instant`() {
        assertEquals(
            Instant.parse("2026-07-17T10:30:00Z").toEpochMilli(),
            parseXmltvEpochMillis("20260717123000 +0200"),
        )
    }

    @Test
    fun `invalid xmltv date is rejected`() {
        assertNull(parseXmltvEpochMillis("not-a-date"))
    }

    private fun program(title: String, start: Long?, stop: Long?) = EpgProgram(
        channelId = "channel",
        title = title,
        description = "",
        startLabel = "",
        stopLabel = "",
        startMillis = start,
        stopMillis = stop,
    )
}

