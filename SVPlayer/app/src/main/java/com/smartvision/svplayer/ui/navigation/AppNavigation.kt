package com.smartvision.svplayer.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.ui.detail.MovieDetailRoute
import com.smartvision.svplayer.ui.detail.SeriesDetailRoute
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.home.HomeScreen
import com.smartvision.svplayer.ui.live.LiveTvScreen
import com.smartvision.svplayer.ui.movies.MoviesScreen
import com.smartvision.svplayer.ui.player.FullScreenContentKind
import com.smartvision.svplayer.ui.player.FullScreenPlayerRoute
import com.smartvision.svplayer.ui.series.SeriesScreen
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: AppRoute.Home.route
    val syncCatalog = {
        scope.launch {
            container.synchronizeCatalog()
        }
        Unit
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(AppRoute.Home.route) {
            HomeScreen(
                currentRoute = currentRoute,
                tabs = headerTabs,
                onNavigate = { route -> navController.navigateSingleTop(route) },
                onSync = syncCatalog,
                onSettings = { navController.navigateSingleTop(AppRoute.Settings.route) },
                onContentClick = { item -> navController.navigateFromContinueItem(item) },
            )
        }
        composable(AppRoute.Live.route) {
            LiveTvScreen(
                currentRoute = currentRoute,
                tabs = headerTabs,
                onNavigate = { route -> navController.navigateSingleTop(route) },
                onSync = syncCatalog,
                onSettings = { navController.navigateSingleTop(AppRoute.Settings.route) },
                onWatch = { channelId -> navController.navigate("player/$channelId") },
            )
        }
        composable(AppRoute.Movies.route) {
            MoviesScreen(
                currentRoute = currentRoute,
                tabs = headerTabs,
                onNavigate = { route -> navController.navigateSingleTop(route) },
                onSync = syncCatalog,
                onSettings = { navController.navigateSingleTop(AppRoute.Settings.route) },
                onOpenMovieDetails = { movieId -> navController.navigate("movie_detail/$movieId") },
                onWatchMovie = { movieId -> navController.navigate("movie_player/$movieId") },
            )
        }
        composable(AppRoute.Series.route) {
            SeriesScreen(
                currentRoute = currentRoute,
                tabs = headerTabs,
                onNavigate = { route -> navController.navigateSingleTop(route) },
                onSync = syncCatalog,
                onSettings = { navController.navigateSingleTop(AppRoute.Settings.route) },
                onOpenSeriesDetails = { seriesId -> navController.navigate("series_detail/$seriesId") },
                onWatchEpisode = { episodeId -> navController.navigate("episode_player/$episodeId") },
            )
        }
        composable(AppRoute.Settings.route) {
            PlaceholderRouteScreen("Parametres", "Route settings prete. Aucun stockage ou compte n'est branche.")
        }
        composable(AppRoute.SyncSettings.route) {
            PlaceholderRouteScreen("Synchronisation", "Action mock. Pas d'appel API Xtream dans cette tache.")
        }
        composable("player/{channelId}") { entry ->
            val channelId = entry.arguments?.getString("channelId")?.toIntOrNull()
            if (channelId == null) {
                PlaceholderRouteScreen("Lecture live", "Chaine introuvable.")
            } else {
                FullScreenPlayerRoute(
                    streamId = channelId,
                    kind = FullScreenContentKind.Live,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        composable("movie_player/{movieId}") { entry ->
            val movieId = entry.arguments?.getString("movieId")?.toIntOrNull()
            if (movieId == null) {
                PlaceholderRouteScreen("Lecture film", "Film introuvable.")
            } else {
                FullScreenPlayerRoute(
                    streamId = movieId,
                    kind = FullScreenContentKind.Movie,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        composable("movie_detail/{movieId}") { entry ->
            val movieId = entry.arguments?.getString("movieId")?.toIntOrNull()
            if (movieId == null) {
                PlaceholderRouteScreen("Detail film", "Film introuvable.")
            } else {
                MovieDetailRoute(
                    movieId = movieId,
                    currentRoute = AppRoute.Movies.route,
                    tabs = headerTabs,
                    onNavigate = { route -> navController.navigateSingleTop(route) },
                    onSync = syncCatalog,
                    onSettings = { navController.navigateSingleTop(AppRoute.Settings.route) },
                    onWatchMovie = { id -> navController.navigate("movie_player/$id") },
                )
            }
        }
        composable("episode_player/{episodeId}") { entry ->
            val episodeId = entry.arguments?.getString("episodeId")?.toIntOrNull()
            if (episodeId == null) {
                PlaceholderRouteScreen("Lecture serie", "Episode introuvable.")
            } else {
                FullScreenPlayerRoute(
                    streamId = episodeId,
                    kind = FullScreenContentKind.Episode,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        composable("series_detail/{seriesId}") { entry ->
            val seriesId = entry.arguments?.getString("seriesId")?.toIntOrNull()
            if (seriesId == null) {
                PlaceholderRouteScreen("Detail serie", "Serie introuvable.")
            } else {
                SeriesDetailRoute(
                    seriesId = seriesId,
                    currentRoute = AppRoute.Series.route,
                    tabs = headerTabs,
                    onNavigate = { route -> navController.navigateSingleTop(route) },
                    onSync = syncCatalog,
                    onSettings = { navController.navigateSingleTop(AppRoute.Settings.route) },
                    onWatchEpisode = { episodeId -> navController.navigate("episode_player/$episodeId") },
                )
            }
        }
    }
}

@Composable
private fun PlaceholderRouteScreen(
    title: String,
    description: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        SmartVisionColors.PrimaryDark.copy(alpha = 0.34f),
                        SmartVisionColors.Background,
                        Color(0xFF01040C),
                    ),
                    radius = 1300f,
                ),
            )
            .padding(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleL,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Body,
            )
        }
    }
}

private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(AppRoute.Home.route) {
            saveState = true
        }
    }
}

private fun NavHostController.navigateFromContinueItem(item: ContinueItem) {
    val parts = item.id.split(":", limit = 2)
    if (parts.size != 2) return
    val id = parts[1].toIntOrNull() ?: return
    when (parts[0]) {
        "live" -> navigate("player/$id")
        "movie" -> navigate("movie_player/$id")
        "episode" -> navigate("episode_player/$id")
        "series" -> navigate("series_detail/$id")
    }
}

private enum class AppRoute(val route: String) {
    Home("home"),
    Live("live_tv"),
    Movies("movies"),
    Series("series"),
    Settings("settings"),
    SyncSettings("sync/settings"),
}

private val headerTabs = listOf(
    HomeHeaderTab("Accueil", AppRoute.Home.route),
    HomeHeaderTab("Live TV", AppRoute.Live.route),
    HomeHeaderTab("Films", AppRoute.Movies.route),
    HomeHeaderTab("Series", AppRoute.Series.route),
)
