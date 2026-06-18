package com.smartvision.svplayer.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.home.HomeScreen
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: AppRoute.Home.route

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
                onSync = { navController.navigateSingleTop(AppRoute.SyncSettings.route) },
                onSettings = { navController.navigateSingleTop(AppRoute.Settings.route) },
                onContentClick = {},
            )
        }
        composable(AppRoute.Live.route) {
            PlaceholderRouteScreen("Live TV", "Route live_tv prête. L'écran Live TV complet reste hors scope.")
        }
        composable(AppRoute.Movies.route) {
            PlaceholderRouteScreen("Films", "Route movies prête. Aucun catalogue API n'est branché.")
        }
        composable(AppRoute.Series.route) {
            PlaceholderRouteScreen("Séries", "Route series prête. Les détails séries viendront plus tard.")
        }
        composable(AppRoute.Settings.route) {
            PlaceholderRouteScreen("Paramètres", "Route settings prête. Aucun stockage ou compte n'est branché.")
        }
        composable(AppRoute.SyncSettings.route) {
            PlaceholderRouteScreen("Synchronisation", "Action mock. Pas d'appel API Xtream dans cette tâche.")
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
    HomeHeaderTab("Séries", AppRoute.Series.route),
)
