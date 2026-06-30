package com.smartvision.svplayer.data.repository

import com.smartvision.svplayer.domain.model.LiveChannel
import com.smartvision.svplayer.domain.model.Movie
import com.smartvision.svplayer.domain.model.TvSeries
import com.smartvision.svplayer.domain.repository.LocalCatalogSnapshot

internal class LocalCatalogSnapshotCache {
    private var liveSnapshot: LocalCatalogSnapshot<LiveChannel>? = null
    private var movieSnapshot: LocalCatalogSnapshot<Movie>? = null
    private var seriesSnapshot: LocalCatalogSnapshot<TvSeries>? = null

    fun getLive(): LocalCatalogSnapshot<LiveChannel>? =
        liveSnapshot?.copy(fromCache = true)

    fun getMovies(): LocalCatalogSnapshot<Movie>? =
        movieSnapshot?.copy(fromCache = true)

    fun getSeries(): LocalCatalogSnapshot<TvSeries>? =
        seriesSnapshot?.copy(fromCache = true)

    fun putLive(snapshot: LocalCatalogSnapshot<LiveChannel>): LocalCatalogSnapshot<LiveChannel> {
        liveSnapshot = snapshot.copy(fromCache = false)
        return snapshot.copy(fromCache = false)
    }

    fun putMovies(snapshot: LocalCatalogSnapshot<Movie>): LocalCatalogSnapshot<Movie> {
        movieSnapshot = snapshot.copy(fromCache = false)
        return snapshot.copy(fromCache = false)
    }

    fun putSeries(snapshot: LocalCatalogSnapshot<TvSeries>): LocalCatalogSnapshot<TvSeries> {
        seriesSnapshot = snapshot.copy(fromCache = false)
        return snapshot.copy(fromCache = false)
    }

    fun invalidate() {
        liveSnapshot = null
        movieSnapshot = null
        seriesSnapshot = null
    }
}
