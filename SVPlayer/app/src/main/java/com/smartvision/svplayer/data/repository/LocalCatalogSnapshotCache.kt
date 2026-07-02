package com.smartvision.svplayer.data.repository

import com.smartvision.svplayer.domain.model.Category
import com.smartvision.svplayer.domain.model.LiveChannel
import com.smartvision.svplayer.domain.model.Movie
import com.smartvision.svplayer.domain.model.TvSeries
import com.smartvision.svplayer.domain.repository.LocalCatalogSnapshot

internal class LocalCatalogSnapshotCache {
    private var liveSnapshot: LocalCatalogSnapshot<LiveChannel>? = null
    private var movieSnapshot: LocalCatalogSnapshot<Movie>? = null
    private var seriesSnapshot: LocalCatalogSnapshot<TvSeries>? = null
    private var liveCategories: List<Category>? = null
    private var movieCategories: List<Category>? = null
    private var seriesCategories: List<Category>? = null
    private val livePages = mutableMapOf<PageKey, List<LiveChannel>>()
    private val moviePages = mutableMapOf<PageKey, List<Movie>>()
    private val seriesPages = mutableMapOf<PageKey, List<TvSeries>>()

    fun getLive(): LocalCatalogSnapshot<LiveChannel>? =
        liveSnapshot?.copy(fromCache = true)

    fun getMovies(): LocalCatalogSnapshot<Movie>? =
        movieSnapshot?.copy(fromCache = true)

    fun getSeries(): LocalCatalogSnapshot<TvSeries>? =
        seriesSnapshot?.copy(fromCache = true)

    fun getLiveCategories(): List<Category>? =
        liveCategories

    fun getMovieCategories(): List<Category>? =
        movieCategories

    fun getSeriesCategories(): List<Category>? =
        seriesCategories

    fun putLiveCategories(categories: List<Category>): List<Category> {
        liveCategories = categories
        return categories
    }

    fun putMovieCategories(categories: List<Category>): List<Category> {
        movieCategories = categories
        return categories
    }

    fun putSeriesCategories(categories: List<Category>): List<Category> {
        seriesCategories = categories
        return categories
    }

    fun getLivePage(categoryId: String?, offset: Int, limit: Int): List<LiveChannel>? =
        livePages[PageKey(categoryId, offset, limit)]

    fun getMoviePage(categoryId: String?, offset: Int, limit: Int): List<Movie>? =
        moviePages[PageKey(categoryId, offset, limit)]

    fun getSeriesPage(categoryId: String?, offset: Int, limit: Int): List<TvSeries>? =
        seriesPages[PageKey(categoryId, offset, limit)]

    fun putLivePage(categoryId: String?, offset: Int, limit: Int, items: List<LiveChannel>): List<LiveChannel> {
        livePages[PageKey(categoryId, offset, limit)] = items
        return items
    }

    fun putMoviePage(categoryId: String?, offset: Int, limit: Int, items: List<Movie>): List<Movie> {
        moviePages[PageKey(categoryId, offset, limit)] = items
        return items
    }

    fun putSeriesPage(categoryId: String?, offset: Int, limit: Int, items: List<TvSeries>): List<TvSeries> {
        seriesPages[PageKey(categoryId, offset, limit)] = items
        return items
    }

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
        liveCategories = null
        movieCategories = null
        seriesCategories = null
        livePages.clear()
        moviePages.clear()
        seriesPages.clear()
    }
}

private data class PageKey(
    val categoryId: String?,
    val offset: Int,
    val limit: Int,
)
