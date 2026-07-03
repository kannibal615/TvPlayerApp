package com.smartvision.svplayer.data.repository

import com.smartvision.svplayer.core.config.XtreamCredentials
import com.smartvision.svplayer.data.local.entity.CategoryEntity
import com.smartvision.svplayer.data.local.entity.EpisodeEntity
import com.smartvision.svplayer.data.local.entity.LiveStreamEntity
import com.smartvision.svplayer.data.local.entity.MovieEntity
import com.smartvision.svplayer.data.local.entity.ProfileEntity
import com.smartvision.svplayer.data.local.entity.SeriesEntity
import com.smartvision.svplayer.data.remote.dto.XtreamAccountDto
import com.smartvision.svplayer.data.remote.dto.XtreamCategoryDto
import com.smartvision.svplayer.data.remote.dto.XtreamEpisodeDto
import com.smartvision.svplayer.data.remote.dto.XtreamLiveStreamDto
import com.smartvision.svplayer.data.remote.dto.XtreamMovieDto
import com.smartvision.svplayer.data.remote.dto.XtreamSeriesDto
import com.smartvision.svplayer.domain.model.AccountProfile
import com.smartvision.svplayer.domain.model.Category
import com.smartvision.svplayer.domain.model.Episode
import com.smartvision.svplayer.domain.model.LiveChannel
import com.smartvision.svplayer.domain.model.MediaSection
import com.smartvision.svplayer.domain.model.Movie
import com.smartvision.svplayer.domain.model.TvSeries
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun CategoryEntity.toDomain(type: MediaSection, count: Int): Category =
    Category(id = id, name = name, type = type, count = count)

fun XtreamCategoryDto.toEntity(type: MediaSection): CategoryEntity? {
    val safeId = id?.takeIf { it.isNotBlank() } ?: return null
    return CategoryEntity(id = safeId, type = type.storageName, name = name.orEmpty().ifBlank { "Sans categorie" })
}

fun XtreamLiveStreamDto.toEntity(imageBaseHost: String? = null): LiveStreamEntity? {
    val safeId = streamId ?: return null
    return LiveStreamEntity(
        streamId = safeId,
        number = number ?: safeId,
        name = name.orEmpty().ifBlank { "Chaine $safeId" },
        categoryId = categoryId,
        logoUrl = normalizeCatalogImageUrl(icon, imageBaseHost),
        epgChannelId = epgChannelId,
    )
}

fun XtreamMovieDto.toEntity(imageBaseHost: String? = null): MovieEntity? {
    val safeId = streamId ?: return null
    return MovieEntity(
        streamId = safeId,
        number = number ?: safeId,
        title = name.orEmpty().ifBlank { "Film $safeId" },
        categoryId = categoryId,
        posterUrl = normalizeCatalogImageUrl(icon, imageBaseHost),
        year = added?.take(4),
        genre = null,
        rating = ratingFiveBased ?: rating,
        duration = null,
        plot = null,
        containerExtension = containerExtension.orEmpty().ifBlank { "mp4" },
    )
}

fun XtreamSeriesDto.toEntity(imageBaseHost: String? = null): SeriesEntity? {
    val safeId = seriesId ?: return null
    return SeriesEntity(
        seriesId = safeId,
        number = number ?: safeId,
        title = name.orEmpty().ifBlank { "Serie $safeId" },
        categoryId = categoryId,
        posterUrl = normalizeCatalogImageUrl(cover, imageBaseHost),
        year = releaseDate?.take(4),
        genre = genre,
        rating = rating,
        seasonsCount = null,
        plot = plot,
    )
}

fun XtreamEpisodeDto.toEntity(seriesId: Int, seasonNumber: Int): EpisodeEntity? {
    val safeId = id?.toIntOrNull() ?: return null
    return EpisodeEntity(
        episodeId = safeId,
        seriesId = seriesId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber ?: safeId,
        title = title.orEmpty().ifBlank { "Episode $safeId" },
        containerExtension = containerExtension.orEmpty().ifBlank { "mp4" },
        duration = info?.duration,
        plot = info?.plot,
    )
}

fun LiveStreamEntity.toDomain(categoryName: String, imageBaseHost: String? = null): LiveChannel =
    LiveChannel(
        streamId = streamId,
        number = number,
        name = name,
        categoryId = categoryId,
        categoryName = categoryName,
        logoUrl = normalizeCatalogImageUrl(logoUrl, imageBaseHost),
        currentProgram = categoryName,
        timeRange = null,
        epgChannelId = epgChannelId,
        directStreamUrl = directStreamUrl,
    )

fun MovieEntity.toDomain(categoryName: String, imageBaseHost: String? = null): Movie =
    Movie(
        streamId = streamId,
        number = number,
        title = title,
        categoryId = categoryId,
        categoryName = categoryName,
        posterUrl = normalizeCatalogImageUrl(posterUrl, imageBaseHost),
        year = year,
        genre = genre,
        rating = rating,
        duration = duration,
        plot = plot,
        containerExtension = containerExtension,
    )

fun SeriesEntity.toDomain(categoryName: String, imageBaseHost: String? = null): TvSeries =
    TvSeries(
        seriesId = seriesId,
        number = number,
        title = title,
        categoryId = categoryId,
        categoryName = categoryName,
        posterUrl = normalizeCatalogImageUrl(posterUrl, imageBaseHost),
        year = year,
        genre = genre,
        rating = rating,
        seasonsCount = seasonsCount,
        plot = plot,
    )

fun EpisodeEntity.toDomain(): Episode =
    Episode(
        episodeId = episodeId,
        seriesId = seriesId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        title = title,
        containerExtension = containerExtension,
        duration = duration,
        plot = plot,
    )

fun XtreamAccountDto.toProfileEntity(credentials: XtreamCredentials, now: Long): ProfileEntity =
    ProfileEntity(
        id = "debug",
        name = "Profil SmartVision",
        host = credentials.normalizedHost,
        usernameMasked = credentials.maskedUsername,
        status = userInfo?.status.orEmpty().ifBlank { "Inconnu" },
        expirationDate = userInfo?.expirationDate?.toLongOrNull()?.let { formatUnixSeconds(it) },
        activeConnections = userInfo?.activeConnections?.toIntOrNull(),
        maxConnections = userInfo?.maxConnections?.toIntOrNull(),
        lastSync = now,
    )

fun ProfileEntity.toDomain(liveCount: Int, movieCount: Int, seriesCount: Int): AccountProfile =
    AccountProfile(
        id = id,
        name = name,
        host = host,
        usernameMasked = usernameMasked,
        status = status,
        expirationDate = expirationDate,
        activeConnections = activeConnections,
        maxConnections = maxConnections,
        lastSync = lastSync?.let { formatDateTime(it) },
        liveCount = liveCount,
        movieCount = movieCount,
        seriesCount = seriesCount,
    )

fun formatDateTime(value: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(value))

private fun formatUnixSeconds(value: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(value * 1000L))
