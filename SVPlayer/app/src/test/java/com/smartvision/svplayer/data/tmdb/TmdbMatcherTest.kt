package com.smartvision.svplayer.data.tmdb

import org.junit.Assert.assertEquals
import org.junit.Test

class TmdbMatcherTest {
    @Test
    fun `search candidates remove catalog prefix before fallback to original`() {
        assertEquals(
            listOf("after the storm", "ex after the storm"),
            TmdbMatcher.searchTitleCandidates("EX - After the Storm (2016)"),
        )
    }

    @Test
    fun `search candidates do not strip normal mixed case movie titles`() {
        assertEquals(
            listOf("up an adventure"),
            TmdbMatcher.searchTitleCandidates("Up - An Adventure"),
        )
    }

    @Test
    fun `year extraction ignores xtream added timestamp and uses title year`() {
        assertEquals(
            "2019",
            TmdbMatcher.extractYear("1783123456", "EX - Three Days and a Life (2019)"),
        )
    }

    @Test
    fun `display title removes catalog prefix and parenthesized year`() {
        assertEquals(
            "After the Storm",
            TmdbMatcher.cleanDisplayTitle("EX - After the Storm (2016)"),
        )
    }
}
