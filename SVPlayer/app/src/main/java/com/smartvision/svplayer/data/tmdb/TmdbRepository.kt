package com.smartvision.svplayer.data.tmdb

import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.data.local.dao.MediaDao
import com.smartvision.svplayer.data.local.entity.TmdbContentMappingEntity
import com.smartvision.svplayer.data.local.entity.TmdbMovieMetadataEntity
import com.smartvision.svplayer.data.local.entity.TmdbSeriesMetadataEntity
import com.smartvision.svplayer.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class TmdbRepository(
    private val api: TmdbApiService,
    private val mediaDao: MediaDao,
    private val accountManager: XtreamAccountManager,
    private val settingsRepository: SettingsRepository,
    readAccessToken: String,
) {
    private val tokenConfigured = readAccessToken.isNotBlank()
    private val imageResolver = TmdbImageResolver()
    private val seasonPosterCache = mutableMapOf<SeasonPosterCacheKey, String?>()
    private var lastCleanupAt: Long = 0L

    val isConfigured: Boolean
        get() = tokenConfigured

    suspend fun getCachedMovieMetadata(
        contentId: Int,
        language: String,
    ): TmdbMovieMetadata? = withContext(Dispatchers.IO) {
        if (!TmdbAccessPolicy.canUse(settingsRepository.settings.first().tmdbApiEnabled)) return@withContext null
        cachedMovieEntity(contentId, language.toTmdbLanguage())?.toTmdbMovieMetadata()
    }

    suspend fun getCachedSeriesMetadata(
        contentId: Int,
        language: String,
    ): TmdbSeriesMetadata? = withContext(Dispatchers.IO) {
        if (!TmdbAccessPolicy.canUse(settingsRepository.settings.first().tmdbApiEnabled)) return@withContext null
        cachedSeriesEntity(contentId, language.toTmdbLanguage())?.toTmdbSeriesMetadata()
    }

    suspend fun enrichMovie(
        contentId: Int,
        title: String,
        year: String?,
        language: String,
        includeAdult: Boolean,
    ): TmdbMovieMetadata? = withContext(Dispatchers.IO) {
        if (!TmdbAccessPolicy.canUse(settingsRepository.settings.first().tmdbApiEnabled)) return@withContext null
        val tmdbLanguage = language.toTmdbLanguage()
        val cached = cachedMovieEntity(contentId, tmdbLanguage)
        if (cached != null && cached.isFresh()) return@withContext cached.toTmdbMovieMetadata()
        if (!TmdbAccessPolicy.canRequestNetwork(settingsRepository.settings.first().tmdbApiEnabled, tokenConfigured)) {
            return@withContext cached?.toTmdbMovieMetadata()
        }

        val match = resolveMovieMatch(
            contentId = contentId,
            title = title,
            year = year,
            language = tmdbLanguage,
            includeAdult = includeAdult,
        ) ?: return@withContext cached?.toTmdbMovieMetadata()

        val details = runCatching {
            api.getMovieDetails(
                movieId = match.tmdbId,
                language = tmdbLanguage,
                includeImageLanguage = tmdbLanguage.toImageLanguageQuery(),
            )
        }.getOrNull() ?: return@withContext cached?.toTmdbMovieMetadata()

        val entity = details.toEntity(tmdbLanguage, imageResolver) ?: return@withContext cached?.toTmdbMovieMetadata()
        mediaDao.upsertTmdbMovieMetadata(entity)
        cleanupStaleMetadataIfNeeded()
        entity.toTmdbMovieMetadata()
    }

    suspend fun enrichSeries(
        contentId: Int,
        title: String,
        year: String?,
        language: String,
        includeAdult: Boolean,
    ): TmdbSeriesMetadata? = withContext(Dispatchers.IO) {
        if (!TmdbAccessPolicy.canUse(settingsRepository.settings.first().tmdbApiEnabled)) return@withContext null
        val tmdbLanguage = language.toTmdbLanguage()
        val cached = cachedSeriesEntity(contentId, tmdbLanguage)
        if (cached != null && cached.isFresh()) return@withContext cached.toTmdbSeriesMetadata()
        if (!TmdbAccessPolicy.canRequestNetwork(settingsRepository.settings.first().tmdbApiEnabled, tokenConfigured)) {
            return@withContext cached?.toTmdbSeriesMetadata()
        }

        val match = resolveSeriesMatch(
            contentId = contentId,
            title = title,
            year = year,
            language = tmdbLanguage,
            includeAdult = includeAdult,
        ) ?: return@withContext cached?.toTmdbSeriesMetadata()

        val details = runCatching {
            api.getSeriesDetails(
                seriesId = match.tmdbId,
                language = tmdbLanguage,
                includeImageLanguage = tmdbLanguage.toImageLanguageQuery(),
            )
        }.getOrNull() ?: return@withContext cached?.toTmdbSeriesMetadata()

        val entity = details.toEntity(tmdbLanguage, imageResolver) ?: return@withContext cached?.toTmdbSeriesMetadata()
        mediaDao.upsertTmdbSeriesMetadata(entity)
        cleanupStaleMetadataIfNeeded()
        entity.toTmdbSeriesMetadata()
    }

    suspend fun getSeriesSeasonPoster(
        contentId: Int,
        seasonNumber: Int,
        language: String,
    ): String? = withContext(Dispatchers.IO) {
        if (seasonNumber < 0) return@withContext null
        val settings = settingsRepository.settings.first()
        if (!TmdbAccessPolicy.canRequestNetwork(settings.tmdbApiEnabled, tokenConfigured)) return@withContext null
        val tmdbId = mediaDao.getTmdbContentMapping(activeProfileId(), "series", contentId)?.tmdbId
            ?: return@withContext null
        val tmdbLanguage = language.toTmdbLanguage()
        val cacheKey = SeasonPosterCacheKey(tmdbId, seasonNumber, tmdbLanguage)
        synchronized(seasonPosterCache) {
            if (seasonPosterCache.containsKey(cacheKey)) return@withContext seasonPosterCache[cacheKey]
        }
        val posterUrl = runCatching {
            api.getSeriesSeasonDetails(
                seriesId = tmdbId,
                seasonNumber = seasonNumber,
                language = tmdbLanguage,
            ).posterUrl(imageResolver)
        }.getOrNull()
        synchronized(seasonPosterCache) { seasonPosterCache[cacheKey] = posterUrl }
        posterUrl
    }

    private suspend fun cachedMovieEntity(contentId: Int, language: String): TmdbMovieMetadataEntity? {
        val tmdbId = mediaDao.getTmdbContentMapping(activeProfileId(), "movie", contentId)?.tmdbId ?: return null
        return mediaDao.getTmdbMovieMetadata(tmdbId, language)
            ?: mediaDao.getAnyTmdbMovieMetadata(tmdbId)
    }

    private suspend fun cachedSeriesEntity(contentId: Int, language: String): TmdbSeriesMetadataEntity? {
        val tmdbId = mediaDao.getTmdbContentMapping(activeProfileId(), "series", contentId)?.tmdbId ?: return null
        return mediaDao.getTmdbSeriesMetadata(tmdbId, language)
            ?: mediaDao.getAnyTmdbSeriesMetadata(tmdbId)
    }

    private suspend fun cleanupStaleMetadataIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastCleanupAt < CleanupIntervalMs) return
        val minUpdatedAt = now - MetadataRetentionMs
        mediaDao.deleteStaleTmdbMovieMetadata(minUpdatedAt)
        mediaDao.deleteStaleTmdbSeriesMetadata(minUpdatedAt)
        lastCleanupAt = now
    }

    private suspend fun resolveMovieMatch(
        contentId: Int,
        title: String,
        year: String?,
        language: String,
        includeAdult: Boolean,
    ): TmdbResolvedMatch? {
        val existing = mediaDao.getTmdbContentMapping(activeProfileId(), "movie", contentId)
        existing?.tmdbId?.takeIf { existing.confidence >= StrongMappingConfidence }?.let { tmdbId ->
            return TmdbResolvedMatch(tmdbId = tmdbId, confidence = existing.confidence)
        }
        val queryYear = TmdbMatcher.extractYear(year, title)
        val best = bestMovieMatch(title, queryYear, language, includeAdult)
            ?: existing?.tmdbId?.let { TmdbResolvedMatch(tmdbId = it, confidence = existing.confidence) }
            ?: return null
        persistMapping(
            contentType = "movie",
            contentId = contentId,
            tmdbId = best.tmdbId,
            mediaType = "movie",
            title = best.title,
            originalTitle = best.originalTitle,
            year = best.year,
            confidence = best.confidence,
            language = language,
            adult = best.adult,
        )
        return best
    }

    private suspend fun resolveSeriesMatch(
        contentId: Int,
        title: String,
        year: String?,
        language: String,
        includeAdult: Boolean,
    ): TmdbResolvedMatch? {
        val existing = mediaDao.getTmdbContentMapping(activeProfileId(), "series", contentId)
        existing?.tmdbId?.takeIf { existing.confidence >= StrongMappingConfidence }?.let { tmdbId ->
            return TmdbResolvedMatch(tmdbId = tmdbId, confidence = existing.confidence)
        }
        val queryYear = TmdbMatcher.extractYear(year, title)
        val best = bestSeriesMatch(title, queryYear, language, includeAdult)
            ?: existing?.tmdbId?.let { TmdbResolvedMatch(tmdbId = it, confidence = existing.confidence) }
            ?: return null
        persistMapping(
            contentType = "series",
            contentId = contentId,
            tmdbId = best.tmdbId,
            mediaType = "tv",
            title = best.title,
            originalTitle = best.originalTitle,
            year = best.year,
            confidence = best.confidence,
            language = language,
            adult = best.adult,
        )
        return best
    }

    private suspend fun bestMovieMatch(
        title: String,
        year: String?,
        language: String,
        includeAdult: Boolean,
    ): TmdbResolvedMatch? {
        val years = listOf(year?.toIntOrNull(), null).distinct()
        var best: TmdbResolvedMatch? = null
        for (queryTitle in TmdbMatcher.searchTitleCandidates(title)) {
            for (searchYear in years) {
                val candidate = runCatching {
                    api.searchMovies(
                        query = queryTitle,
                        language = language,
                        includeAdult = includeAdult,
                        primaryReleaseYear = searchYear,
                    ).results.orEmpty()
                }.getOrElse { emptyList() }
                    .mapNotNull { candidate ->
                        val id = candidate.id ?: return@mapNotNull null
                        val confidence = TmdbMatcher.scoreMovie(queryTitle, year, candidate, includeAdult)
                        if (confidence < TmdbMatcher.MinimumConfidence) return@mapNotNull null
                        TmdbResolvedMatch(
                            tmdbId = id,
                            confidence = confidence,
                            title = candidate.title,
                            originalTitle = candidate.originalTitle,
                            year = TmdbMatcher.extractYear(candidate.releaseDate),
                            adult = candidate.adult == true,
                        )
                    }
                    .maxByOrNull { it.confidence }
                if (candidate != null && (best == null || candidate.confidence > best.confidence)) {
                    best = candidate
                    if (candidate.confidence >= 100) return candidate
                }
            }
        }
        return best
    }

    private suspend fun bestSeriesMatch(
        title: String,
        year: String?,
        language: String,
        includeAdult: Boolean,
    ): TmdbResolvedMatch? {
        val years = listOf(year?.toIntOrNull(), null).distinct()
        var best: TmdbResolvedMatch? = null
        for (queryTitle in TmdbMatcher.searchTitleCandidates(title)) {
            for (searchYear in years) {
                val candidate = runCatching {
                    api.searchSeries(
                        query = queryTitle,
                        language = language,
                        includeAdult = includeAdult,
                        firstAirDateYear = searchYear,
                    ).results.orEmpty()
                }.getOrElse { emptyList() }
                    .mapNotNull { candidate ->
                        val id = candidate.id ?: return@mapNotNull null
                        val confidence = TmdbMatcher.scoreSeries(queryTitle, year, candidate, includeAdult)
                        if (confidence < TmdbMatcher.MinimumConfidence) return@mapNotNull null
                        TmdbResolvedMatch(
                            tmdbId = id,
                            confidence = confidence,
                            title = candidate.name,
                            originalTitle = candidate.originalName,
                            year = TmdbMatcher.extractYear(candidate.firstAirDate),
                            adult = candidate.adult == true,
                        )
                    }
                    .maxByOrNull { it.confidence }
                if (candidate != null && (best == null || candidate.confidence > best.confidence)) {
                    best = candidate
                    if (candidate.confidence >= 100) return candidate
                }
            }
        }
        return best
    }

    private suspend fun persistMapping(
        contentType: String,
        contentId: Int,
        tmdbId: Int,
        mediaType: String,
        title: String?,
        originalTitle: String?,
        year: String?,
        confidence: Int,
        language: String,
        adult: Boolean,
    ) {
        val now = System.currentTimeMillis()
        val profileId = activeProfileId()
        val existing = mediaDao.getTmdbContentMapping(profileId, contentType, contentId)
        mediaDao.upsertTmdbContentMapping(
            TmdbContentMappingEntity(
                profileId = profileId,
                contentType = contentType,
                contentId = contentId,
                tmdbId = tmdbId,
                mediaType = mediaType,
                matchedTitle = title,
                originalTitle = originalTitle,
                matchedYear = year,
                confidence = confidence,
                matchSource = "tmdb_search",
                language = language,
                adult = adult,
                lastError = null,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    private fun activeProfileId(): String =
        accountManager.activeProfileIdOrDefault()

    private data class TmdbResolvedMatch(
        val tmdbId: Int,
        val confidence: Int,
        val title: String? = null,
        val originalTitle: String? = null,
        val year: String? = null,
        val adult: Boolean = false,
    )
}

private data class SeasonPosterCacheKey(
    val tmdbId: Int,
    val seasonNumber: Int,
    val language: String,
)

internal fun TmdbSeriesSeasonDetailsDto.posterUrl(imageResolver: TmdbImageResolver = TmdbImageResolver()): String? =
    imageResolver.posterUrl(posterPath)

internal object TmdbAccessPolicy {
    fun canUse(apiEnabled: Boolean): Boolean = apiEnabled

    fun canRequestNetwork(apiEnabled: Boolean, tokenConfigured: Boolean): Boolean =
        apiEnabled && tokenConfigured
}

private fun String.toTmdbLanguage(): String =
    if (equals("Francais", ignoreCase = true) || startsWith("fr", ignoreCase = true)) {
        "fr-FR"
    } else {
        "en-US"
    }

private fun String.toImageLanguageQuery(): String {
    val short = substringBefore('-').lowercase()
    return "$short,en,null"
}

private fun TmdbMovieDetailsDto.toEntity(
    language: String,
    imageResolver: TmdbImageResolver,
): TmdbMovieMetadataEntity? {
    val tmdbId = id ?: return null
    val logoPath = images?.logos.bestLogoPath(language)
    val castMembers = credits.castMembers(imageResolver)
    val directors = credits.directors(imageResolver)
    val videoItems = videos.videoItems(language)
    val recommendations = recommendations?.results.toMovieRecommendations(imageResolver)
    return TmdbMovieMetadataEntity(
        tmdbId = tmdbId,
        language = language,
        title = title.blankToNull() ?: originalTitle.blankToNull() ?: "Movie $tmdbId",
        originalTitle = originalTitle.blankToNull(),
        overview = overview.blankToNull(),
        posterPath = posterPath.blankToNull(),
        posterUrl = imageResolver.posterUrl(posterPath),
        backdropPath = backdropPath.blankToNull(),
        backdropUrl = imageResolver.backdropUrl(backdropPath),
        logoPath = logoPath,
        logoUrl = imageResolver.logoUrl(logoPath),
        releaseDate = releaseDate.blankToNull(),
        runtimeMinutes = runtime?.takeIf { it > 0 },
        genres = genres.joinNames(),
        voteAverage = voteAverage?.takeIf { it > 0.0 },
        voteCount = voteCount?.takeIf { it > 0 },
        popularity = popularity,
        cast = castMembers.summaryNames(),
        castJson = castMembers.toTmdbJson(),
        director = directors.summaryNames(),
        directorJson = directors.toTmdbJson(),
        trailerKey = videoItems.firstOrNull { it.type.equals("Trailer", ignoreCase = true) }?.key
            ?: videoItems.firstOrNull()?.key,
        videosJson = videoItems.toTmdbJson(),
        recommendationsJson = recommendations.toTmdbJson(),
        collectionName = collection?.name.blankToNull(),
        certification = releaseDates.certification(language),
        providersSummary = watchProviders.providerSummary(language),
        homepage = homepage.blankToNull(),
        status = status.blankToNull(),
        adult = adult == true,
        originCountry = originCountry.joinNonBlank(),
        originalLanguage = originalLanguage.blankToNull(),
        updatedAt = System.currentTimeMillis(),
    )
}

private fun TmdbSeriesDetailsDto.toEntity(
    language: String,
    imageResolver: TmdbImageResolver,
): TmdbSeriesMetadataEntity? {
    val tmdbId = id ?: return null
    val logoPath = images?.logos.bestLogoPath(language)
    val castMembers = credits.castMembers(imageResolver)
    val creators = createdBy?.mapNotNull { person ->
        person.name.blankToNull()?.let { name ->
            TmdbPersonCredit(
                name = name,
                role = "Creator",
                profileUrl = imageResolver.profileUrl(person.profilePath),
            )
        }
    }.orEmpty().distinctBy { it.name }
    val videoItems = videos.videoItems(language)
    val recommendations = recommendations?.results.toSeriesRecommendations(imageResolver)
    return TmdbSeriesMetadataEntity(
        tmdbId = tmdbId,
        language = language,
        name = name.blankToNull() ?: originalName.blankToNull() ?: "Series $tmdbId",
        originalName = originalName.blankToNull(),
        overview = overview.blankToNull(),
        posterPath = posterPath.blankToNull(),
        posterUrl = imageResolver.posterUrl(posterPath),
        backdropPath = backdropPath.blankToNull(),
        backdropUrl = imageResolver.backdropUrl(backdropPath),
        logoPath = logoPath,
        logoUrl = imageResolver.logoUrl(logoPath),
        firstAirDate = firstAirDate.blankToNull(),
        episodeRunTimeMinutes = episodeRunTime?.firstOrNull { it > 0 },
        seasonsCount = numberOfSeasons?.takeIf { it > 0 },
        genres = genres.joinNames(),
        voteAverage = voteAverage?.takeIf { it > 0.0 },
        voteCount = voteCount?.takeIf { it > 0 },
        popularity = popularity,
        cast = castMembers.summaryNames(),
        castJson = castMembers.toTmdbJson(),
        createdBy = creators.summaryNames(),
        createdByJson = creators.toTmdbJson(),
        trailerKey = videoItems.firstOrNull { it.type.equals("Trailer", ignoreCase = true) }?.key
            ?: videoItems.firstOrNull()?.key,
        videosJson = videoItems.toTmdbJson(),
        recommendationsJson = recommendations.toTmdbJson(),
        certification = contentRatings.certification(language),
        providersSummary = watchProviders.providerSummary(language),
        homepage = homepage.blankToNull(),
        status = status.blankToNull(),
        adult = adult == true,
        originCountry = originCountry.joinNonBlank(),
        originalLanguage = originalLanguage.blankToNull(),
        updatedAt = System.currentTimeMillis(),
    )
}

private fun String?.blankToNull(): String? = this?.trim()?.takeIf { it.isNotBlank() }

private fun List<TmdbGenreDto>?.joinNames(): String? =
    this?.mapNotNull { it.name.blankToNull() }?.distinct()?.takeIf { it.isNotEmpty() }?.joinToString(", ")

private fun List<String>?.joinNonBlank(): String? =
    this?.mapNotNull { it.blankToNull() }?.distinct()?.takeIf { it.isNotEmpty() }?.joinToString(", ")

private fun TmdbCreditsDto?.castMembers(imageResolver: TmdbImageResolver): List<TmdbPersonCredit> =
    this?.cast
        ?.sortedBy { it.order ?: Int.MAX_VALUE }
        ?.mapNotNull { cast ->
            cast.name.blankToNull()?.let { name ->
                TmdbPersonCredit(
                    name = name,
                    role = cast.character.blankToNull(),
                    profileUrl = imageResolver.profileUrl(cast.profilePath),
                )
            }
        }
        ?.distinctBy { it.name }
        ?.take(12)
        .orEmpty()

private fun TmdbCreditsDto?.directors(imageResolver: TmdbImageResolver): List<TmdbPersonCredit> =
    this?.crew
        ?.filter { it.job.equals("Director", ignoreCase = true) || it.job.equals("Series Director", ignoreCase = true) }
        ?.mapNotNull { crew ->
            crew.name.blankToNull()?.let { name ->
                TmdbPersonCredit(
                    name = name,
                    role = crew.job.blankToNull(),
                    profileUrl = imageResolver.profileUrl(crew.profilePath),
                )
            }
        }
        ?.distinctBy { it.name }
        ?.take(6)
        .orEmpty()

private fun TmdbVideosDto?.videoItems(language: String): List<TmdbVideoItem> {
    val short = language.substringBefore('-')
    return this?.results
        ?.filter { it.site.equals("YouTube", ignoreCase = true) && it.key.isNullOrBlank().not() }
        ?.sortedWith(
            compareByDescending<TmdbVideoDto> { it.type.equals("Trailer", ignoreCase = true) }
                .thenByDescending { it.type.equals("Teaser", ignoreCase = true) }
                .thenByDescending { it.official == true }
                .thenByDescending { it.language.equals(short, ignoreCase = true) },
        )
        ?.mapNotNull { video ->
            val key = video.key.blankToNull() ?: return@mapNotNull null
            TmdbVideoItem(
                key = key,
                name = video.name.blankToNull() ?: video.type.blankToNull() ?: "Trailer",
                type = video.type.blankToNull() ?: "Video",
                official = video.official == true,
                language = video.language.blankToNull(),
            )
        }
        ?.distinctBy { it.key }
        ?.take(8)
        .orEmpty()
}

private fun List<TmdbMovieSearchResultDto>?.toMovieRecommendations(
    imageResolver: TmdbImageResolver,
): List<TmdbRecommendation> =
    this.orEmpty()
        .mapNotNull { item ->
            val tmdbId = item.id ?: return@mapNotNull null
            val title = item.title.blankToNull() ?: item.originalTitle.blankToNull() ?: return@mapNotNull null
            TmdbRecommendation(
                tmdbId = tmdbId,
                title = title,
                posterUrl = imageResolver.posterUrl(item.posterPath),
                backdropUrl = imageResolver.backdropUrl(item.backdropPath),
                releaseDate = item.releaseDate.blankToNull(),
                voteAverage = item.voteAverage?.takeIf { it > 0.0 },
            )
        }
        .take(12)

private fun List<TmdbSeriesSearchResultDto>?.toSeriesRecommendations(
    imageResolver: TmdbImageResolver,
): List<TmdbRecommendation> =
    this.orEmpty()
        .mapNotNull { item ->
            val tmdbId = item.id ?: return@mapNotNull null
            val title = item.name.blankToNull() ?: item.originalName.blankToNull() ?: return@mapNotNull null
            TmdbRecommendation(
                tmdbId = tmdbId,
                title = title,
                posterUrl = imageResolver.posterUrl(item.posterPath),
                backdropUrl = imageResolver.backdropUrl(item.backdropPath),
                releaseDate = item.firstAirDate.blankToNull(),
                voteAverage = item.voteAverage?.takeIf { it > 0.0 },
            )
        }
        .take(12)

private fun List<TmdbPersonCredit>.summaryNames(): String? =
    map { it.name }.distinct().take(6).takeIf { it.isNotEmpty() }?.joinToString(", ")

private val tmdbRepositoryGson = com.google.gson.Gson()

private fun Any.toTmdbJson(): String = tmdbRepositoryGson.toJson(this)

private fun TmdbMovieMetadataEntity.isFresh(): Boolean =
    System.currentTimeMillis() - updatedAt <= MetadataFreshMs

private fun TmdbSeriesMetadataEntity.isFresh(): Boolean =
    System.currentTimeMillis() - updatedAt <= MetadataFreshMs

private fun List<TmdbImageDto>?.bestLogoPath(language: String): String? {
    val short = language.substringBefore('-')
    return this
        ?.sortedWith(
            compareByDescending<TmdbImageDto> { it.language.equals(short, ignoreCase = true) }
                .thenByDescending { it.language.equals("en", ignoreCase = true) }
                .thenBy { it.language ?: "" },
        )
        ?.firstOrNull()
        ?.filePath
        .blankToNull()
}

private fun TmdbReleaseDatesDto?.certification(language: String): String? {
    val region = language.regionCode()
    return this?.results
        ?.sortedByDescending { it.country.equals(region, ignoreCase = true) }
        ?.flatMap { it.releaseDates.orEmpty() }
        ?.firstOrNull { !it.certification.isNullOrBlank() }
        ?.certification
        .blankToNull()
}

private fun TmdbContentRatingsDto?.certification(language: String): String? {
    val region = language.regionCode()
    return this?.results
        ?.firstOrNull { it.country.equals(region, ignoreCase = true) }
        ?.rating
        .blankToNull()
        ?: this?.results?.firstOrNull { !it.rating.isNullOrBlank() }?.rating.blankToNull()
}

private fun TmdbWatchProvidersDto?.providerSummary(language: String): String? {
    val results = this?.results.orEmpty()
    val region = results[language.regionCode()] ?: results["US"] ?: results.values.firstOrNull() ?: return null
    val providers = listOf(region.flatrate, region.free, region.ads, region.rent, region.buy)
        .filterNotNull()
        .flatten()
        .mapNotNull { it.providerName.blankToNull() }
        .distinct()
        .take(6)
    return providers.takeIf { it.isNotEmpty() }?.joinToString(", ")
}

private fun String.regionCode(): String =
    substringAfter('-', "US").uppercase().ifBlank { "US" }

private const val MetadataFreshMs = 7L * 24L * 60L * 60L * 1_000L
private const val MetadataRetentionMs = 90L * 24L * 60L * 60L * 1_000L
private const val CleanupIntervalMs = 24L * 60L * 60L * 1_000L
private const val StrongMappingConfidence = 85
