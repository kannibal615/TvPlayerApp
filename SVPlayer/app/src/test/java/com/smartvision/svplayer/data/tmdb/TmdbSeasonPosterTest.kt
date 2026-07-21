package com.smartvision.svplayer.data.tmdb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbSeasonPosterTest {
    @Test
    fun resolvesRelativeSeasonPosterPath() {
        val dto = TmdbSeriesSeasonDetailsDto(id = 1, seasonNumber = 2, posterPath = "/season-two.jpg")

        assertEquals("https://image.tmdb.org/t/p/w500/season-two.jpg", dto.posterUrl())
    }

    @Test
    fun missingSeasonPosterFallsBackAtUiLayer() {
        val dto = TmdbSeriesSeasonDetailsDto(id = 1, seasonNumber = 2, posterPath = null)

        assertNull(dto.posterUrl())
    }
}
