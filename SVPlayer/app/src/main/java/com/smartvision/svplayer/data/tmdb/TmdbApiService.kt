package com.smartvision.svplayer.data.tmdb

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    @GET("configuration")
    suspend fun getConfiguration(): TmdbConfigurationResponse

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("language") language: String,
        @Query("include_adult") includeAdult: Boolean,
        @Query("primary_release_year") primaryReleaseYear: Int? = null,
    ): TmdbSearchResponse<TmdbMovieSearchResultDto>

    @GET("search/tv")
    suspend fun searchSeries(
        @Query("query") query: String,
        @Query("language") language: String,
        @Query("include_adult") includeAdult: Boolean,
        @Query("first_air_date_year") firstAirDateYear: Int? = null,
    ): TmdbSearchResponse<TmdbSeriesSearchResultDto>

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String,
        @Query("append_to_response") appendToResponse: String = MovieAppendToResponse,
        @Query("include_image_language") includeImageLanguage: String? = null,
    ): TmdbMovieDetailsDto

    @GET("tv/{series_id}")
    suspend fun getSeriesDetails(
        @Path("series_id") seriesId: Int,
        @Query("language") language: String,
        @Query("append_to_response") appendToResponse: String = SeriesAppendToResponse,
        @Query("include_image_language") includeImageLanguage: String? = null,
    ): TmdbSeriesDetailsDto

    companion object {
        const val MovieAppendToResponse = "credits,videos,images,release_dates,watch/providers,recommendations"
        const val SeriesAppendToResponse = "credits,videos,images,content_ratings,watch/providers,recommendations"
    }
}

data class TmdbConfigurationResponse(
    @SerializedName("images") val images: TmdbConfigurationImagesDto?,
)

data class TmdbConfigurationImagesDto(
    @SerializedName("secure_base_url") val secureBaseUrl: String?,
    @SerializedName("poster_sizes") val posterSizes: List<String>?,
    @SerializedName("backdrop_sizes") val backdropSizes: List<String>?,
    @SerializedName("logo_sizes") val logoSizes: List<String>?,
    @SerializedName("profile_sizes") val profileSizes: List<String>?,
)

data class TmdbSearchResponse<T>(
    @SerializedName("results") val results: List<T>?,
)

data class TmdbMovieSearchResultDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("title") val title: String?,
    @SerializedName("original_title") val originalTitle: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("adult") val adult: Boolean?,
    @SerializedName("popularity") val popularity: Double?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("vote_count") val voteCount: Int?,
)

data class TmdbSeriesSearchResultDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("original_name") val originalName: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("adult") val adult: Boolean?,
    @SerializedName("popularity") val popularity: Double?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("vote_count") val voteCount: Int?,
)

data class TmdbMovieDetailsDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("title") val title: String?,
    @SerializedName("original_title") val originalTitle: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("runtime") val runtime: Int?,
    @SerializedName("genres") val genres: List<TmdbGenreDto>?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("vote_count") val voteCount: Int?,
    @SerializedName("popularity") val popularity: Double?,
    @SerializedName("belongs_to_collection") val collection: TmdbCollectionDto?,
    @SerializedName("homepage") val homepage: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("adult") val adult: Boolean?,
    @SerializedName("origin_country") val originCountry: List<String>?,
    @SerializedName("original_language") val originalLanguage: String?,
    @SerializedName("credits") val credits: TmdbCreditsDto?,
    @SerializedName("videos") val videos: TmdbVideosDto?,
    @SerializedName("images") val images: TmdbImagesDto?,
    @SerializedName("release_dates") val releaseDates: TmdbReleaseDatesDto?,
    @SerializedName("watch/providers") val watchProviders: TmdbWatchProvidersDto?,
    @SerializedName("recommendations") val recommendations: TmdbSearchResponse<TmdbMovieSearchResultDto>?,
)

data class TmdbSeriesDetailsDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("original_name") val originalName: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int?,
    @SerializedName("genres") val genres: List<TmdbGenreDto>?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("vote_count") val voteCount: Int?,
    @SerializedName("popularity") val popularity: Double?,
    @SerializedName("created_by") val createdBy: List<TmdbPersonDto>?,
    @SerializedName("homepage") val homepage: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("adult") val adult: Boolean?,
    @SerializedName("origin_country") val originCountry: List<String>?,
    @SerializedName("original_language") val originalLanguage: String?,
    @SerializedName("credits") val credits: TmdbCreditsDto?,
    @SerializedName("videos") val videos: TmdbVideosDto?,
    @SerializedName("images") val images: TmdbImagesDto?,
    @SerializedName("content_ratings") val contentRatings: TmdbContentRatingsDto?,
    @SerializedName("watch/providers") val watchProviders: TmdbWatchProvidersDto?,
    @SerializedName("recommendations") val recommendations: TmdbSearchResponse<TmdbSeriesSearchResultDto>?,
)

data class TmdbGenreDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
)

data class TmdbCollectionDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
)

data class TmdbCreditsDto(
    @SerializedName("cast") val cast: List<TmdbCastDto>?,
    @SerializedName("crew") val crew: List<TmdbCrewDto>?,
)

data class TmdbCastDto(
    @SerializedName("name") val name: String?,
    @SerializedName("character") val character: String?,
    @SerializedName("profile_path") val profilePath: String?,
    @SerializedName("order") val order: Int?,
)

data class TmdbCrewDto(
    @SerializedName("name") val name: String?,
    @SerializedName("job") val job: String?,
    @SerializedName("profile_path") val profilePath: String?,
)

data class TmdbPersonDto(
    @SerializedName("name") val name: String?,
    @SerializedName("profile_path") val profilePath: String?,
)

data class TmdbVideosDto(
    @SerializedName("results") val results: List<TmdbVideoDto>?,
)

data class TmdbVideoDto(
    @SerializedName("key") val key: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("site") val site: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("official") val official: Boolean?,
    @SerializedName("iso_639_1") val language: String?,
)

data class TmdbImagesDto(
    @SerializedName("logos") val logos: List<TmdbImageDto>?,
)

data class TmdbImageDto(
    @SerializedName("file_path") val filePath: String?,
    @SerializedName("iso_639_1") val language: String?,
)

data class TmdbReleaseDatesDto(
    @SerializedName("results") val results: List<TmdbReleaseCountryDto>?,
)

data class TmdbReleaseCountryDto(
    @SerializedName("iso_3166_1") val country: String?,
    @SerializedName("release_dates") val releaseDates: List<TmdbReleaseDateDto>?,
)

data class TmdbReleaseDateDto(
    @SerializedName("certification") val certification: String?,
)

data class TmdbContentRatingsDto(
    @SerializedName("results") val results: List<TmdbContentRatingDto>?,
)

data class TmdbContentRatingDto(
    @SerializedName("iso_3166_1") val country: String?,
    @SerializedName("rating") val rating: String?,
)

data class TmdbWatchProvidersDto(
    @SerializedName("results") val results: Map<String, TmdbWatchProviderRegionDto>?,
)

data class TmdbWatchProviderRegionDto(
    @SerializedName("flatrate") val flatrate: List<TmdbWatchProviderDto>?,
    @SerializedName("free") val free: List<TmdbWatchProviderDto>?,
    @SerializedName("ads") val ads: List<TmdbWatchProviderDto>?,
    @SerializedName("rent") val rent: List<TmdbWatchProviderDto>?,
    @SerializedName("buy") val buy: List<TmdbWatchProviderDto>?,
)

data class TmdbWatchProviderDto(
    @SerializedName("provider_name") val providerName: String?,
)
