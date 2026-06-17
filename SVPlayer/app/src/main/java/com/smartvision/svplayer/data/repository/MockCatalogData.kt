package com.smartvision.svplayer.data.repository

import com.smartvision.svplayer.domain.model.AccountProfile
import com.smartvision.svplayer.domain.model.Category
import com.smartvision.svplayer.domain.model.Episode
import com.smartvision.svplayer.domain.model.LiveChannel
import com.smartvision.svplayer.domain.model.MediaSection
import com.smartvision.svplayer.domain.model.Movie
import com.smartvision.svplayer.domain.model.TvSeries

object MockCatalogData {
    val liveCategories = listOf(
        Category("sport", "Sport", MediaSection.Live, 132),
        Category("news", "Actualites", MediaSection.Live, 86),
        Category("cinema", "Cinema", MediaSection.Live, 105),
        Category("entertainment", "Divertissement", MediaSection.Live, 142),
        Category("documentary", "Documentaire", MediaSection.Live, 96),
        Category("kids", "Jeunesse", MediaSection.Live, 78),
        Category("music", "Musique", MediaSection.Live, 64),
        Category("international", "International", MediaSection.Live, 118),
    )

    val liveChannels = listOf(
        LiveChannel(1001, 1, "beIN Sports 1", "sport", "Sport", null, "Ligue des Champions", "18:00 - 20:00"),
        LiveChannel(1002, 2, "Canal+ Sport", "sport", "Sport", null, "Top 14", "18:30 - 20:30"),
        LiveChannel(1003, 3, "L'Equipe", "sport", "Sport", null, "L'Equipe du Soir", "19:00 - 20:00"),
        LiveChannel(1004, 4, "RMC Sport 1", "sport", "Sport", null, "Premier League", "18:45 - 20:45"),
        LiveChannel(1005, 5, "Eurosport 1", "sport", "Sport", null, "Cyclisme", "17:30 - 19:30"),
        LiveChannel(1006, 6, "France Info", "news", "Actualites", null, "Journal", "En direct"),
        LiveChannel(1007, 7, "Arte", "documentary", "Documentaire", null, "Grand format", "En direct"),
    )

    val movieCategories = listOf(
        Category("all", "Toutes les categories", MediaSection.Movies, 245),
        Category("new", "Nouveautes", MediaSection.Movies, 44),
        Category("favorites", "Favoris", MediaSection.Movies, 0),
        Category("action", "Action", MediaSection.Movies, 68),
        Category("drama", "Drame", MediaSection.Movies, 55),
        Category("family", "Famille", MediaSection.Movies, 36),
    )

    val movies = listOf(
        Movie(2001, 1, "Interstellar", "action", "Action", null, "2014", "Science-fiction", "8.7", "2h49", "Un voyage spatial spectaculaire pour sauver l'humanite.", "mp4"),
        Movie(2002, 2, "Inception", "action", "Action", null, "2010", "Thriller", "8.8", "2h28", "Un voleur manipule les reves pour un dernier contrat.", "mp4"),
        Movie(2003, 3, "Le Mans 66", "drama", "Drame", null, "2019", "Sport", "8.1", "2h32", "Deux passionnes defient Ferrari au Mans.", "mp4"),
        Movie(2004, 4, "Paddington 2", "family", "Famille", null, "2017", "Famille", "7.8", "1h43", "Une aventure chaleureuse pour toute la famille.", "mp4"),
    )

    val seriesCategories = listOf(
        Category("all", "Toutes les series", MediaSection.Series, 138),
        Category("new", "Nouveautes", MediaSection.Series, 29),
        Category("favorites", "Favoris", MediaSection.Series, 0),
        Category("drama", "Drame", MediaSection.Series, 46),
        Category("crime", "Crime", MediaSection.Series, 34),
        Category("sci-fi", "Science-fiction", MediaSection.Series, 25),
    )

    val series = listOf(
        TvSeries(3001, 1, "Dark Matter", "sci-fi", "Science-fiction", null, "2024", "Science-fiction", "7.7", 1, "Un physicien est projete dans une version alternative de sa vie."),
        TvSeries(3002, 2, "The Bureau", "drama", "Drame", null, "2015", "Espionnage", "8.7", 5, "Agents, identites et tensions geopolitiques."),
        TvSeries(3003, 3, "Sherlock", "crime", "Crime", null, "2010", "Crime", "9.1", 4, "Une lecture moderne du detective londonien."),
    )

    val episodes = listOf(
        Episode(4001, 3001, 1, 1, "Episode 1", "mp4", "52 min", "Premiere rupture entre deux realites."),
        Episode(4002, 3001, 1, 2, "Episode 2", "mp4", "50 min", "Les choix commencent a diverger."),
        Episode(4003, 3002, 1, 1, "Episode 1", "mp4", "54 min", "Retour au service."),
    )

    val account = AccountProfile(
        id = "debug",
        name = "Profil local debug",
        host = "Configure dans local.properties",
        usernameMasked = "****",
        status = "Debug",
        expirationDate = null,
        activeConnections = null,
        maxConnections = null,
        lastSync = null,
        liveCount = liveChannels.size,
        movieCount = movies.size,
        seriesCount = series.size,
    )
}
