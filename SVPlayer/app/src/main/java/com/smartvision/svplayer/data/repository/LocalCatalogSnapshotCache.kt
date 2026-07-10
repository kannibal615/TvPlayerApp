package com.smartvision.svplayer.data.repository

import com.smartvision.svplayer.domain.model.Category
import com.smartvision.svplayer.domain.model.LiveChannel
import com.smartvision.svplayer.domain.model.Movie
import com.smartvision.svplayer.domain.model.TvSeries
import com.smartvision.svplayer.domain.repository.LocalCatalogSnapshot

internal class LocalCatalogSnapshotCache {
    private var ownerProfileId: String? = null
    private var liveSnapshot: LocalCatalogSnapshot<LiveChannel>? = null
    private var movieSnapshot: LocalCatalogSnapshot<Movie>? = null
    private var seriesSnapshot: LocalCatalogSnapshot<TvSeries>? = null
    private var liveCategories: List<Category>? = null
    private var movieCategories: List<Category>? = null
    private var seriesCategories: List<Category>? = null
    private val livePages = mutableMapOf<PageKey, List<LiveChannel>>()
    private val moviePages = mutableMapOf<PageKey, List<Movie>>()
    private val seriesPages = mutableMapOf<PageKey, List<TvSeries>>()

    fun getLive(profileId: String): LocalCatalogSnapshot<LiveChannel>? =
        liveSnapshot.takeIf { isOwnedBy(profileId) }
            ?.copy(fromCache = true)

    fun getMovies(profileId: String): LocalCatalogSnapshot<Movie>? =
        movieSnapshot.takeIf { isOwnedBy(profileId) }
            ?.copy(fromCache = true)

    fun getSeries(profileId: String): LocalCatalogSnapshot<TvSeries>? =
        seriesSnapshot.takeIf { isOwnedBy(profileId) }
            ?.copy(fromCache = true)

    fun getLiveCategories(profileId: String): List<Category>? =
        liveCategories.takeIf { isOwnedBy(profileId) }

    fun getMovieCategories(profileId: String): List<Category>? =
        movieCategories.takeIf { isOwnedBy(profileId) }

    fun getSeriesCategories(profileId: String): List<Category>? =
        seriesCategories.takeIf { isOwnedBy(profileId) }

    fun putLiveCategories(profileId: String, categories: List<Category>): List<Category> {
        prepareFor(profileId)
        liveCategories = categories
        return categories
    }

    fun putMovieCategories(profileId: String, categories: List<Category>): List<Category> {
        prepareFor(profileId)
        movieCategories = categories
        return categories
    }

    fun putSeriesCategories(profileId: String, categories: List<Category>): List<Category> {
        prepareFor(profileId)
        seriesCategories = categories
        return categories
    }

    fun getLivePage(profileId: String, categoryId: String?, offset: Int, limit: Int): List<LiveChannel>? =
        livePages[PageKey(categoryId, offset, limit)].takeIf { isOwnedBy(profileId) }

    fun getMoviePage(profileId: String, categoryId: String?, offset: Int, limit: Int): List<Movie>? =
        moviePages[PageKey(categoryId, offset, limit)].takeIf { isOwnedBy(profileId) }

    fun getSeriesPage(profileId: String, categoryId: String?, offset: Int, limit: Int): List<TvSeries>? =
        seriesPages[PageKey(categoryId, offset, limit)].takeIf { isOwnedBy(profileId) }

    fun putLivePage(profileId: String, categoryId: String?, offset: Int, limit: Int, items: List<LiveChannel>): List<LiveChannel> {
        prepareFor(profileId)
        livePages[PageKey(categoryId, offset, limit)] = items
        return items
    }

    fun putMoviePage(profileId: String, categoryId: String?, offset: Int, limit: Int, items: List<Movie>): List<Movie> {
        prepareFor(profileId)
        moviePages[PageKey(categoryId, offset, limit)] = items
        return items
    }

    fun putSeriesPage(profileId: String, categoryId: String?, offset: Int, limit: Int, items: List<TvSeries>): List<TvSeries> {
        prepareFor(profileId)
        seriesPages[PageKey(categoryId, offset, limit)] = items
        return items
    }

    fun putLive(profileId: String, snapshot: LocalCatalogSnapshot<LiveChannel>): LocalCatalogSnapshot<LiveChannel> {
        prepareFor(profileId)
        liveSnapshot = snapshot.copy(fromCache = false)
        return snapshot.copy(fromCache = false)
    }

    fun putMovies(profileId: String, snapshot: LocalCatalogSnapshot<Movie>): LocalCatalogSnapshot<Movie> {
        prepareFor(profileId)
        movieSnapshot = snapshot.copy(fromCache = false)
        return snapshot.copy(fromCache = false)
    }

    fun putSeries(profileId: String, snapshot: LocalCatalogSnapshot<TvSeries>): LocalCatalogSnapshot<TvSeries> {
        prepareFor(profileId)
        seriesSnapshot = snapshot.copy(fromCache = false)
        return snapshot.copy(fromCache = false)
    }

    fun invalidate() {
        ownerProfileId = null
        clearValues()
    }

    private fun isOwnedBy(profileId: String): Boolean =
        ownerProfileId == profileId

    private fun prepareFor(profileId: String) {
        if (ownerProfileId != profileId) {
            clearValues()
            ownerProfileId = profileId
        }
    }

    private fun clearValues() {
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
