package com.smartvision.svplayer.data.remote

import com.smartvision.svplayer.core.config.XtreamCredentials
import com.smartvision.svplayer.core.config.XtreamCredentialsProvider
import com.smartvision.svplayer.data.remote.dto.XtreamCategoryDto
import com.smartvision.svplayer.data.remote.dto.XtreamLiveStreamDto
import com.smartvision.svplayer.data.remote.dto.XtreamMovieDto
import com.smartvision.svplayer.data.remote.dto.XtreamMovieInfoDto
import com.smartvision.svplayer.data.remote.dto.XtreamSeriesDto
import com.smartvision.svplayer.data.remote.dto.XtreamSeriesInfoDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class XtreamApiClient(
    private val api: XtreamApiService,
    private val credentialsProvider: XtreamCredentialsProvider,
) {
    suspend fun getLiveCategories(): List<XtreamCategoryDto> = withContext(Dispatchers.IO) {
        val credentials = configuredCredentials()
        api.getCategories(
            username = credentials.username,
            password = credentials.password,
            action = "get_live_categories",
        )
    }

    suspend fun getLiveStreams(categoryId: String): List<XtreamLiveStreamDto> = withContext(Dispatchers.IO) {
        val credentials = configuredCredentials()
        api.getLiveStreams(
            username = credentials.username,
            password = credentials.password,
            categoryId = categoryId,
        )
    }

    suspend fun getMovieCategories(): List<XtreamCategoryDto> = withContext(Dispatchers.IO) {
        val credentials = configuredCredentials()
        api.getCategories(
            username = credentials.username,
            password = credentials.password,
            action = "get_vod_categories",
        )
    }

    suspend fun getMovies(categoryId: String): List<XtreamMovieDto> = withContext(Dispatchers.IO) {
        val credentials = configuredCredentials()
        api.getMovies(
            username = credentials.username,
            password = credentials.password,
            categoryId = categoryId,
        )
    }

    suspend fun getMovieInfo(movieId: Int): XtreamMovieInfoDto = withContext(Dispatchers.IO) {
        val credentials = configuredCredentials()
        api.getMovieInfo(
            username = credentials.username,
            password = credentials.password,
            movieId = movieId,
        )
    }

    suspend fun getSeriesCategories(): List<XtreamCategoryDto> = withContext(Dispatchers.IO) {
        val credentials = configuredCredentials()
        api.getCategories(
            username = credentials.username,
            password = credentials.password,
            action = "get_series_categories",
        )
    }

    suspend fun getSeries(categoryId: String): List<XtreamSeriesDto> = withContext(Dispatchers.IO) {
        val credentials = configuredCredentials()
        api.getSeries(
            username = credentials.username,
            password = credentials.password,
            categoryId = categoryId,
        )
    }

    suspend fun getSeriesInfo(seriesId: Int): XtreamSeriesInfoDto = withContext(Dispatchers.IO) {
        val credentials = configuredCredentials()
        api.getSeriesInfo(
            username = credentials.username,
            password = credentials.password,
            seriesId = seriesId,
        )
    }

    private fun configuredCredentials(): XtreamCredentials {
        val credentials = credentialsProvider.current()
        check(credentials.isConfigured) {
            "Identifiants Xtream absents dans local.properties"
        }
        return credentials
    }
}
