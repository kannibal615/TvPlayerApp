package com.smartvision.svplayer.data.remote

import com.smartvision.svplayer.data.remote.dto.XtreamAccountDto
import com.smartvision.svplayer.data.remote.dto.XtreamCategoryDto
import com.smartvision.svplayer.data.remote.dto.XtreamLiveStreamDto
import com.smartvision.svplayer.data.remote.dto.XtreamMovieDto
import com.smartvision.svplayer.data.remote.dto.XtreamMovieInfoDto
import com.smartvision.svplayer.data.remote.dto.XtreamSeriesDto
import com.smartvision.svplayer.data.remote.dto.XtreamSeriesInfoDto
import retrofit2.http.GET
import retrofit2.http.Query

interface XtreamApiService {
    @GET("player_api.php")
    suspend fun getAccount(
        @Query("username") username: String,
        @Query("password") password: String,
    ): XtreamAccountDto

    @GET("player_api.php")
    suspend fun getCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String,
    ): List<XtreamCategoryDto>

    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String? = null,
    ): List<XtreamLiveStreamDto>

    @GET("player_api.php")
    suspend fun getMovies(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams",
        @Query("category_id") categoryId: String? = null,
    ): List<XtreamMovieDto>

    @GET("player_api.php")
    suspend fun getMovieInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_info",
        @Query("vod_id") movieId: Int,
    ): XtreamMovieInfoDto

    @GET("player_api.php")
    suspend fun getSeries(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series",
        @Query("category_id") categoryId: String? = null,
    ): List<XtreamSeriesDto>

    @GET("player_api.php")
    suspend fun getSeriesInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: Int,
    ): XtreamSeriesInfoDto
}
