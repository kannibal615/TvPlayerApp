package com.smartvision.svplayer.ui.catalog

import com.smartvision.svplayer.ui.live.LiveSortMode
import com.smartvision.svplayer.ui.live.LiveTvChannel
import com.smartvision.svplayer.ui.live.LiveTvUiState
import com.smartvision.svplayer.ui.movies.MovieItemUi
import com.smartvision.svplayer.ui.movies.MovieSortMode
import com.smartvision.svplayer.ui.movies.MoviesScreenState
import com.smartvision.svplayer.ui.series.SeriesItemUi
import com.smartvision.svplayer.ui.series.SeriesScreenState
import com.smartvision.svplayer.ui.series.SeriesSortMode
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogSortStateTest {
    @Test fun `movie title sort is stable`() {
        val items = listOf(movie(3, "Beta"), movie(2, "Alpha"), movie(1, "Alpha"))
        assertEquals(listOf(1, 2, 3), MoviesScreenState(movies = items, sortMode = MovieSortMode.TITLE_ASC).displayedMovies.map { it.streamId })
    }

    @Test fun `series rating sort keeps id tie breaker`() {
        val items = listOf(series(3, "7.0"), series(2, "9.0"), series(1, "9.0"))
        assertEquals(listOf(1, 2, 3), SeriesScreenState(series = items, sortMode = SeriesSortMode.RATING).displayedSeries.map { it.seriesId })
    }

    @Test fun `live favorites sort before provider order`() {
        val items = listOf(channel(1, false), channel(2, true))
        assertEquals(listOf(2, 1), LiveTvUiState(channels = items, sortMode = LiveSortMode.FAVORITES_FIRST).displayedChannels.map { it.streamId })
    }

    private fun movie(id: Int, title: String) = MovieItemUi(id, id.toString(), title, null, null, "", "mp4", null, null, null, null, null, null, null, "")
    private fun series(id: Int, rating: String) = SeriesItemUi(id, id.toString(), "S$id", null, null, "", null, null, null, rating, null)
    private fun channel(id: Int, favorite: Boolean) = LiveTvChannel(id, id.toString(), "", null, "C$id", "", "", "", 0f, "", "", "", streamUrl = "", fallbackStreamUrl = "", isFavorite = favorite)
}
