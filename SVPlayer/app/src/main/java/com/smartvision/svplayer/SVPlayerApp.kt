package com.smartvision.svplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.designsystem.TvScaffold
import com.smartvision.svplayer.core.navigation.SVRoute
import com.smartvision.svplayer.domain.model.PlaybackKind
import com.smartvision.svplayer.feature.account.AccountRoute
import com.smartvision.svplayer.feature.home.HomeRoute
import com.smartvision.svplayer.feature.live.LiveRoute
import com.smartvision.svplayer.feature.movies.MoviesRoute
import com.smartvision.svplayer.feature.player.PlayerRoute
import com.smartvision.svplayer.feature.series.SeriesRoute
import com.smartvision.svplayer.feature.settings.SettingsRoute
import kotlinx.coroutines.launch

@Composable
fun SVPlayerApp(navController: NavHostController = rememberNavController()) {
    val container = LocalAppContainer.current
    val syncStatus by container.catalogRepository.syncStatus.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = SVRoute.fromRoute(backStack?.destination?.route)

    NavHost(navController = navController, startDestination = SVRoute.Home.route) {
        composable(SVRoute.Home.route) {
            AppScaffold(currentRoute, syncStatus, navController) {
                HomeRoute(
                    openLive = { navController.navigateSingleTop(SVRoute.Live) },
                    openMovies = { navController.navigateSingleTop(SVRoute.Movies) },
                    openSeries = { navController.navigateSingleTop(SVRoute.Series) },
                )
            }
        }
        composable(SVRoute.Live.route) {
            AppScaffold(currentRoute, syncStatus, navController) {
                LiveRoute(openPlayer = { navController.navigate(SVRoute.player(PlaybackKind.Live.routeName, it.toString())) })
            }
        }
        composable(SVRoute.Movies.route) {
            AppScaffold(currentRoute, syncStatus, navController) {
                MoviesRoute(openPlayer = { navController.navigate(SVRoute.player(PlaybackKind.Movie.routeName, it.toString())) })
            }
        }
        composable(SVRoute.Series.route) {
            AppScaffold(currentRoute, syncStatus, navController) {
                SeriesRoute(openPlayer = { navController.navigate(SVRoute.player(PlaybackKind.Episode.routeName, it.toString())) })
            }
        }
        composable(SVRoute.Account.route) {
            AppScaffold(currentRoute, syncStatus, navController) {
                AccountRoute()
            }
        }
        composable(SVRoute.Settings.route) {
            AppScaffold(currentRoute, syncStatus, navController) {
                SettingsRoute()
            }
        }
        composable(SVRoute.Player.route) { entry ->
            val kind = PlaybackKind.fromRoute(entry.arguments?.getString("kind"))
            val id = entry.arguments?.getString("id").orEmpty()
            PlayerRoute(kind = kind, id = id, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun AppScaffold(
    currentRoute: SVRoute,
    syncStatus: com.smartvision.svplayer.domain.model.SyncStatus,
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    TvScaffold(
        currentRoute = currentRoute,
        syncStatus = syncStatus,
        onNavigate = { navController.navigateSingleTop(it) },
        onSync = {
            scope.launch {
                container.synchronizeCatalog()
            }
        },
        onSettings = { navController.navigateSingleTop(SVRoute.Settings) },
        content = content,
    )
}

private fun NavHostController.navigateSingleTop(route: SVRoute) {
    navigate(route.route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(SVRoute.Home.route) {
            saveState = true
        }
    }
}
