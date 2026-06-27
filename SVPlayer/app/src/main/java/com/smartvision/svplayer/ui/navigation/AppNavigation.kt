package com.smartvision.svplayer.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.sync.CatalogSyncScheduler
import com.smartvision.svplayer.sync.SyncFrequencyPolicy
import com.smartvision.svplayer.ui.activation.ActivationScreen
import com.smartvision.svplayer.ui.activation.ActivationViewModel
import com.smartvision.svplayer.ui.activation.XtreamQrSetupPanel
import com.smartvision.svplayer.ui.detail.MovieDetailRoute
import com.smartvision.svplayer.ui.detail.SeriesDetailRoute
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.home.HomeScreen
import com.smartvision.svplayer.ui.home.HomeCollectionsScreen
import com.smartvision.svplayer.ui.home.HomeCollectionKind
import com.smartvision.svplayer.ui.live.LiveTvScreen
import com.smartvision.svplayer.ui.movies.MoviesScreen
import com.smartvision.svplayer.ui.notifications.NotificationBadgeViewModel
import com.smartvision.svplayer.ui.notifications.NotificationsRoute
import com.smartvision.svplayer.ui.player.FullScreenContentKind
import com.smartvision.svplayer.ui.player.FullScreenPlayerRoute
import com.smartvision.svplayer.ui.profile.ProfileRoute
import com.smartvision.svplayer.ui.profile.SmartVisionQrDialog
import com.smartvision.svplayer.ui.series.SeriesScreen
import com.smartvision.svplayer.ui.settings.SettingsScreen
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import com.smartvision.svplayer.ui.update.AppUpdateDialog
import com.smartvision.svplayer.ui.update.AppUpdateViewModel
import com.smartvision.svplayer.ui.youtube.YoutubePlayerScreen
import com.smartvision.svplayer.ui.youtube.YoutubeScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val activationViewModel: ActivationViewModel = viewModel(
        factory = viewModelFactory {
            ActivationViewModel(container.activationRepository)
        },
    )
    val appUpdateViewModel: AppUpdateViewModel = viewModel(
        factory = viewModelFactory {
            AppUpdateViewModel(container.appUpdateRepository)
        },
    )
    val activationState by activationViewModel.uiState.collectAsStateWithLifecycle()
    val appUpdateState by appUpdateViewModel.uiState.collectAsStateWithLifecycle()
    val playerSettings by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = PlayerSettings(),
    )
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: AppRoute.Home.route
    var showExitConfirmation by remember { mutableStateOf(false) }
    var showLicensePurchaseQr by remember { mutableStateOf(false) }
    var premiumLicenseCode by remember { mutableStateOf("") }
    val context = LocalContext.current
    val activity = context as? Activity
    LaunchedEffect(activity) {
        activity?.let { container.privacyConsentManager.refreshSilently(it) }
    }
    LaunchedEffect(currentRoute) {
        container.anomalyReporter.setCurrentRoute(currentRoute)
    }
    LaunchedEffect(
        activationState.activationType,
        activationState.licenseStatus,
        activationState.trialStatus,
        activationState.freeWithAdsStatus,
    ) {
        container.monetizationManager.synchronizeStatus()
    }
    val syncCatalog = {
        scope.launch {
            runCatching {
                container.xtreamRepository.clearCaches()
                container.synchronizeCatalog()
            }
        }
        Unit
    }

    val xtreamAccounts by container.accountManager.accounts.collectAsStateWithLifecycle()
    val xtreamAccountSignature = remember(xtreamAccounts) {
        xtreamAccounts.joinToString("|") { account ->
            "${account.id}:${account.host}:${account.username}:${account.password.hashCode()}"
        }
    }
    var lastXtreamAccountSignature by remember { mutableStateOf<String?>(null) }

    if (!activationState.activated) {
        BackHandler {
            showExitConfirmation = true
        }
        ActivationScreen(
            state = activationState,
            onRetry = activationViewModel::retry,
            onRefreshSession = activationViewModel::refreshSession,
            onCheckNow = activationViewModel::checkNow,
            onActivateLicense = activationViewModel::activateLicense,
            onStartTrial = activationViewModel::startTrial,
            onContinueFreeWithAds = activationViewModel::continueFreeWithAds,
            onShowActivationForm = activationViewModel::showActivationForm,
        )
        if (showExitConfirmation) {
            ExitConfirmationDialog(
                onDismiss = { showExitConfirmation = false },
                onExit = { activity?.finishAffinity() },
            )
        }
        return
    }

    if (xtreamAccounts.isEmpty()) {
        BackHandler {
            showExitConfirmation = true
        }
        XtreamQrSetupPanel(
            activationRepository = container.activationRepository,
            title = "Configurer les identifiants Xtream",
            onManualAccount = { account ->
                val accountId = container.accountManager.upsert(account)
                container.accountManager.select(accountId)
                container.xtreamRepository.clearCaches()
                container.xtreamRepository.getLiveCategories()
                container.synchronizeCatalog()
                if (activationState.activationType == "trial_pending_xtream") {
                    runCatching { container.activationRepository.finalizeTrialAfterPlaylistConfigured() }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (showExitConfirmation) {
            ExitConfirmationDialog(
                onDismiss = { showExitConfirmation = false },
                onExit = { activity?.finishAffinity() },
            )
        }
        return
    }

    LaunchedEffect(playerSettings.syncFrequency) {
        CatalogSyncScheduler.apply(context, playerSettings.syncFrequency)
    }
    LaunchedEffect(activationState.activated, xtreamAccountSignature, playerSettings.syncFrequency) {
        if (activationState.activated && xtreamAccounts.isNotEmpty()) {
            val previousSignature = lastXtreamAccountSignature
            lastXtreamAccountSignature = xtreamAccountSignature
            val accountChanged = previousSignature != null && previousSignature != xtreamAccountSignature
            val startupSync = previousSignature == null &&
                SyncFrequencyPolicy.from(playerSettings.syncFrequency).runOnStartup
            if (accountChanged || startupSync) {
                runCatching {
                    container.xtreamRepository.clearCaches()
                    container.synchronizeCatalog()
                }
            }
        }
    }
    LaunchedEffect(activationState.activated) {
        if (activationState.activated) {
            appUpdateViewModel.checkForUpdate()
        }
    }
    LaunchedEffect(showLicensePurchaseQr, activationState.shouldShowLicenseKey) {
        if (showLicensePurchaseQr && !activationState.shouldShowLicenseKey) {
            showLicensePurchaseQr = false
            premiumLicenseCode = ""
        }
    }
    val notificationBadgeViewModel: NotificationBadgeViewModel = viewModel(
        factory = viewModelFactory {
            NotificationBadgeViewModel(container.notificationsRepository)
        },
    )
    val notificationBadgeState by notificationBadgeViewModel.uiState.collectAsStateWithLifecycle()
    val hasNewNotifications = notificationBadgeState.hasUnread
    val notificationBadgeCount = notificationBadgeState.unreadCount

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
                onProfile = { navController.navigateSingleTop(AppRoute.Profile.route) },
                onNotifications = { navController.navigateSingleTop(AppRoute.Notifications.route) },
                onLicenseKey = { showLicensePurchaseQr = true },
                showLicenseKey = activationState.shouldShowLicenseKey,
                hasNewNotifications = hasNewNotifications,
                notificationBadgeCount = notificationBadgeCount,
                onContentClick = { item -> navController.navigateFromContinueItem(item) },
                onContinueViewAll = { navController.navigate(AppRoute.ContinueWatching.route) },
                onTrendingViewAll = { navController.navigate(AppRoute.Trending.route) },
            )
        }
        composable(AppRoute.Profile.route) {
            ProfileRoute(
                onBack = { navController.popBackStack() },
                onSettings = { navController.navigateSingleTop(AppRoute.Settings.route) },
                onSyncCatalog = syncCatalog,
                onActivationChanged = activationViewModel::checkNow,
            )
        }
        composable(AppRoute.ContinueWatching.route) {
            HomeCollectionsScreen(
                kind = HomeCollectionKind.ContinueWatching,
                onBack = { navController.popBackStack() },
                onItemClick = { item -> navController.navigateFromContinueItem(item) },
            )
        }
        composable(AppRoute.Trending.route) {
            HomeCollectionsScreen(
                kind = HomeCollectionKind.Trending,
                onBack = { navController.popBackStack() },
                onItemClick = { item -> navController.navigateFromContinueItem(item) },
            )
        }
        composable(AppRoute.Live.route) {
            LiveTvScreen(
                currentRoute = currentRoute,
                tabs = headerTabs,
                onNavigate = { route -> navController.navigateSingleTop(route) },
                onSync = syncCatalog,
                onSettings = { navController.navigateSingleTop(AppRoute.Settings.route) },
                onProfile = { navController.navigateSingleTop(AppRoute.Profile.route) },
                onNotifications = { navController.navigateSingleTop(AppRoute.Notifications.route) },
                onLicenseKey = { showLicensePurchaseQr = true },
                showLicenseKey = activationState.shouldShowLicenseKey,
                hasNewNotifications = hasNewNotifications,
                notificationBadgeCount = notificationBadgeCount,
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
                onProfile = { navController.navigateSingleTop(AppRoute.Profile.route) },
                onNotifications = { navController.navigateSingleTop(AppRoute.Notifications.route) },
                onLicenseKey = { showLicensePurchaseQr = true },
                showLicenseKey = activationState.shouldShowLicenseKey,
                hasNewNotifications = hasNewNotifications,
                notificationBadgeCount = notificationBadgeCount,
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
                onProfile = { navController.navigateSingleTop(AppRoute.Profile.route) },
                onNotifications = { navController.navigateSingleTop(AppRoute.Notifications.route) },
                onLicenseKey = { showLicensePurchaseQr = true },
                showLicenseKey = activationState.shouldShowLicenseKey,
                hasNewNotifications = hasNewNotifications,
                notificationBadgeCount = notificationBadgeCount,
                onOpenSeriesDetails = { seriesId -> navController.navigate("series_detail/$seriesId") },
                onWatchEpisode = { episodeId -> navController.navigate("episode_player/$episodeId") },
            )
        }
        composable(AppRoute.Youtube.route) {
            YoutubeScreen(
                currentRoute = currentRoute,
                tabs = headerTabs,
                onNavigate = { route -> navController.navigateSingleTop(route) },
                onSync = syncCatalog,
                onSettings = { navController.navigateSingleTop(AppRoute.Settings.route) },
                onProfile = { navController.navigateSingleTop(AppRoute.Profile.route) },
                onNotifications = { navController.navigateSingleTop(AppRoute.Notifications.route) },
                onLicenseKey = { showLicensePurchaseQr = true },
                showLicenseKey = activationState.shouldShowLicenseKey,
                hasNewNotifications = hasNewNotifications,
                notificationBadgeCount = notificationBadgeCount,
                onOpenYoutubeVideo = { videoId -> navController.navigate("youtube_player/$videoId") },
            )
        }
        composable(AppRoute.YoutubePlayer.route) { backStackEntry ->
            YoutubePlayerScreen(
                videoId = backStackEntry.arguments?.getString("videoId").orEmpty(),
                currentRoute = AppRoute.Youtube.route,
                tabs = headerTabs,
                anomalyReporter = container.anomalyReporter,
                onNavigate = { route -> navController.navigateSingleTop(route) },
                onSync = syncCatalog,
                onSettings = { navController.navigateSingleTop(AppRoute.Settings.route) },
                onProfile = { navController.navigateSingleTop(AppRoute.Profile.route) },
                onNotifications = { navController.navigateSingleTop(AppRoute.Notifications.route) },
                onLicenseKey = { showLicensePurchaseQr = true },
                showLicenseKey = activationState.shouldShowLicenseKey,
                hasNewNotifications = hasNewNotifications,
                notificationBadgeCount = notificationBadgeCount,
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppRoute.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                updateState = appUpdateState,
                onCheckForUpdate = appUpdateViewModel::checkForUpdate,
                onSyncCatalog = syncCatalog,
            )
        }
        composable(AppRoute.Notifications.route) {
            NotificationsRoute(
                onBack = { navController.popBackStack() },
                onNotificationsSeen = notificationBadgeViewModel::clearUnread,
            )
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
                    onPlayLive = { nextChannelId ->
                        navController.navigate("player/$nextChannelId") {
                            popUpTo("player/{channelId}") { inclusive = true }
                        }
                    },
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
                    onPlayMovie = { nextMovieId ->
                        navController.navigate("movie_player/$nextMovieId") {
                            popUpTo("movie_player/{movieId}") { inclusive = true }
                        }
                    },
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
                    onProfile = { navController.navigateSingleTop(AppRoute.Profile.route) },
                    onNotifications = { navController.navigateSingleTop(AppRoute.Notifications.route) },
                    onLicenseKey = { showLicensePurchaseQr = true },
                    showLicenseKey = activationState.shouldShowLicenseKey,
                    hasNewNotifications = hasNewNotifications,
                    notificationBadgeCount = notificationBadgeCount,
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
                    onPlayEpisode = { nextEpisodeId ->
                        navController.navigate("episode_player/$nextEpisodeId") {
                            popUpTo("episode_player/{episodeId}") { inclusive = true }
                        }
                    },
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
                    onProfile = { navController.navigateSingleTop(AppRoute.Profile.route) },
                    onNotifications = { navController.navigateSingleTop(AppRoute.Notifications.route) },
                    onLicenseKey = { showLicensePurchaseQr = true },
                    showLicenseKey = activationState.shouldShowLicenseKey,
                    hasNewNotifications = hasNewNotifications,
                    notificationBadgeCount = notificationBadgeCount,
                    onWatchEpisode = { episodeId -> navController.navigate("episode_player/$episodeId") },
                )
            }
        }
    }

    val playerRouteActive = currentRoute.startsWith("player/") ||
        currentRoute.startsWith("movie_player/") ||
        currentRoute.startsWith("episode_player/")
    BackHandler(enabled = !playerRouteActive) {
        if (currentRoute != AppRoute.Home.route) {
            val popped = navController.popBackStack()
            if (!popped) {
                navController.navigateSingleTop(AppRoute.Home.route)
            }
        } else {
            showExitConfirmation = true
        }
    }

    if (showExitConfirmation) {
        ExitConfirmationDialog(
            onDismiss = { showExitConfirmation = false },
            onExit = { activity?.finishAffinity() },
        )
    }

    if (showLicensePurchaseQr) {
        val deviceQuery = tvDeviceQuery(
            publicDeviceCode = activationState.publicDeviceCode,
            deviceId = activationState.deviceId,
        )
        val purchaseUrl = activationPortalBaseUrl()
            .plus("account/?source=tv&intent=license&")
            .plus(deviceQuery)
            .plus("&plan=year_1")
        SmartVisionQrDialog(
            title = "Passer a SmartVision Premium",
            subtitle = "Scannez ce QR code pour acheter une licence. Premium supprime les publicites et conserve l'acces pendant la duree choisie.",
            qrUrl = purchaseUrl,
            tvCode = activationState.publicDeviceCode.ifBlank { activationState.deviceId.take(8).uppercase() },
            width = 820.dp,
            licenseCode = premiumLicenseCode,
            onLicenseCodeChange = { premiumLicenseCode = it },
            onSubmitLicenseCode = {
                activationViewModel.activateLicense(premiumLicenseCode)
            },
            submittingLicense = activationState.activationBusy,
            error = activationState.errorMessage,
            onDismiss = { showLicensePurchaseQr = false },
        )
    }

    val update = appUpdateState.update
    if (update != null && appUpdateState.shouldShowDialog) {
        AppUpdateDialog(
            update = update,
            installing = appUpdateState.installing,
            errorMessage = appUpdateState.errorMessage,
            onInstall = { appUpdateViewModel.installUpdate(context) },
            onDismiss = appUpdateViewModel::dismiss,
        )
    }

}

@Composable
private fun ExitConfirmationDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit,
) {
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        cancelFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF111C2E), Color(0xFF07101F))),
                    RoundedCornerShape(16.dp),
                )
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(16.dp))
                .padding(horizontal = 36.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Quitter SmartVision ?",
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Êtes-vous sûr de vouloir quitter l’application ?",
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Body,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TvButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    focusRequester = cancelFocusRequester,
                    variant = TvButtonVariant.Secondary,
                    contentPadding = PaddingValues(horizontal = 22.dp),
                    modifier = Modifier.height(44.dp),
                )
                Spacer(Modifier.width(12.dp))
                TvButton(
                    text = "Quitter",
                    onClick = onExit,
                    variant = TvButtonVariant.Exit,
                    contentPadding = PaddingValues(horizontal = 22.dp),
                    modifier = Modifier.height(44.dp),
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
    Youtube("youtube"),
    YoutubePlayer("youtube_player/{videoId}"),
    Settings("settings"),
    Profile("profile"),
    Notifications("notifications"),
    ContinueWatching("continue_watching"),
    Trending("trending"),
}

private val headerTabs = listOf(
    HomeHeaderTab("Accueil", AppRoute.Home.route),
    HomeHeaderTab("Live TV", AppRoute.Live.route),
    HomeHeaderTab("Films", AppRoute.Movies.route),
    HomeHeaderTab("Series", AppRoute.Series.route),
    HomeHeaderTab("YouTube", AppRoute.Youtube.route, useYoutubeLogo = true),
)

private fun activationPortalBaseUrl(): String =
    BuildConfig.ACTIVATION_BASE_URL.ifBlank { "https://smartvisions.net/" }
        .trim()
        .trimEnd('/') + "/"

private fun tvDeviceQuery(publicDeviceCode: String, deviceId: String): String =
    if (publicDeviceCode.isNotBlank()) {
        "device=$publicDeviceCode"
    } else {
        "device_id=$deviceId"
    }
