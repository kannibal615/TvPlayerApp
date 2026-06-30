package com.smartvision.svplayer.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.monetization.resolveMonetizationStatus
import com.smartvision.svplayer.data.xtream.XtreamConnectionState
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.startup.BackgroundSyncScheduler
import com.smartvision.svplayer.sync.CatalogSyncScheduler
import com.smartvision.svplayer.sync.SyncFrequencyPolicy
import com.smartvision.svplayer.ui.activation.ActivationScreen
import com.smartvision.svplayer.ui.activation.ActivationViewModel
import com.smartvision.svplayer.ui.activation.XtreamQrSetupPanel
import com.smartvision.svplayer.ui.appconfig.AppConfigViewModel
import com.smartvision.svplayer.ui.appconfig.ConsentDialog
import com.smartvision.svplayer.ui.detail.MovieDetailRoute
import com.smartvision.svplayer.ui.detail.SeriesDetailRoute
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.home.HomeScreen
import com.smartvision.svplayer.ui.home.HomeCollectionsScreen
import com.smartvision.svplayer.ui.home.HomeCollectionKind
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.i18n.smartVisionStrings
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
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.TvFocusStyles
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import com.smartvision.svplayer.ui.update.AppUpdateDialog
import com.smartvision.svplayer.ui.update.AppUpdateViewModel
import com.smartvision.svplayer.ui.youtube.YoutubeScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
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
    val appConfigViewModel: AppConfigViewModel = viewModel(
        factory = viewModelFactory {
            AppConfigViewModel(container.appConfigRepository)
        },
    )
    val activationState by activationViewModel.uiState.collectAsStateWithLifecycle()
    val appUpdateState by appUpdateViewModel.uiState.collectAsStateWithLifecycle()
    val appConfigState by appConfigViewModel.uiState.collectAsStateWithLifecycle()
    val playerSettings by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = PlayerSettings(),
    )
    val strings = smartVisionStrings(playerSettings.language)
    val focusStyle = remember(
        playerSettings.focusStyle,
        playerSettings.focusColor,
        playerSettings.focusEffect,
        playerSettings.focusBackground,
    ) {
        TvFocusStyles.fromKeys(
            playerSettings.focusStyle,
            playerSettings.focusColor,
            playerSettings.focusEffect,
            playerSettings.focusBackground,
        )
    }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: AppRoute.Home.route
    var showExitConfirmation by remember { mutableStateOf(false) }
    var showLicensePurchaseQr by remember { mutableStateOf(false) }
    var showXtreamConnectionDialog by remember { mutableStateOf(false) }
    var showXtreamSetupDialog by remember { mutableStateOf(false) }
    var premiumLicenseCode by remember { mutableStateOf("") }
    val xtreamConnectionState by container.xtreamConnectionManager.state.collectAsStateWithLifecycle()
    val xtreamCatalogBlocked = xtreamConnectionState.blocksCatalogForNavigation
    val context = LocalContext.current
    val activity = context as? Activity
    val monetizationStatus = resolveMonetizationStatus(
        activationType = activationState.activationType,
        licenseStatus = activationState.licenseStatus,
        trialStatus = activationState.trialStatus,
        freeWithAdsStatus = activationState.freeWithAdsStatus,
    )
    val youtubeAllowed = container.appConfigRepository.isFeatureAllowed(
        config = appConfigState.config,
        featureKey = "youtube",
        status = monetizationStatus,
    )
    val parentalControlAllowed = container.appConfigRepository.isFeatureAllowed(
        config = appConfigState.config,
        featureKey = "parental_control",
        status = monetizationStatus,
    )
    val tabs = remember(youtubeAllowed, strings, xtreamCatalogBlocked) {
        headerTabs(strings).map { tab ->
            when {
                tab.route == AppRoute.Youtube.route -> tab.copy(locked = !youtubeAllowed)
                tab.route.isXtreamCatalogRoute() -> tab.copy(warning = xtreamCatalogBlocked)
                else -> tab
            }
        }
    }
    val navigateFromHeader: (String) -> Unit = { route ->
        if (route.isXtreamCatalogRoute() && xtreamCatalogBlocked) {
            showXtreamConnectionDialog = true
        } else if (route == AppRoute.Youtube.route && !youtubeAllowed) {
            showLicensePurchaseQr = true
        } else {
            navController.navigateSingleTop(route)
        }
    }
    LaunchedEffect(activity) {
        activity?.let { container.privacyConsentManager.refreshSilently(it) }
    }
    LaunchedEffect(currentRoute) {
        container.anomalyReporter.setCurrentRoute(currentRoute)
    }
    LaunchedEffect(Unit) {
        container.xtreamConnectionManager.alertRequests.collect {
            showXtreamConnectionDialog = true
        }
    }
    LaunchedEffect(
        activationState.activationType,
        activationState.licenseStatus,
        activationState.trialStatus,
        activationState.freeWithAdsStatus,
    ) {
        container.monetizationManager.synchronizeStatus()
    }
    val syncCatalog: suspend () -> Result<Unit> = {
        val connection = container.xtreamConnectionManager.verifyQuick("manual_sync")
        if (!connection.isConnected) {
            showXtreamConnectionDialog = connection.blocksCatalog
            Result.failure(IllegalStateException("Connexion Xtream indisponible"))
        } else {
            runCatching {
                container.xtreamRepository.clearCaches()
                container.catalogRepository.invalidateLocalCatalogCache()
                container.synchronizeCatalog().getOrThrow()
            }
        }
    }
    val launchSyncCatalog = {
        scope.launch { syncCatalog() }
        Unit
    }
    LaunchedEffect(activationState.activated, appConfigState.loading, parentalControlAllowed, playerSettings.parentalControlEnabled, playerSettings.parentalPin, playerSettings.parentalKeywords) {
        if (!appConfigState.loading &&
            activationState.activated &&
            !parentalControlAllowed &&
            (playerSettings.parentalControlEnabled || playerSettings.parentalPin.isNotBlank() || playerSettings.parentalKeywords != "adults; porn; xxx")
        ) {
            container.settingsRepository.resetParentalControl()
            container.xtreamRepository.clearCaches()
            container.catalogRepository.invalidateLocalCatalogCache()
            runCatching { container.synchronizeCatalog() }
        }
    }

    if (appConfigState.consentRequired) {
        CompositionLocalProvider(LocalTvFocusStyle provides focusStyle) {
            Box(Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(R.drawable.smartvision_splash_bg),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99020714)),
                )
                ConsentDialog(
                    consent = appConfigState.config.consent,
                    onAccept = appConfigViewModel::acceptConsent,
                )
            }
        }
        return
    }

    val xtreamAccounts by container.accountManager.accounts.collectAsStateWithLifecycle()
    val xtreamAccountSignature = remember(xtreamAccounts) {
        xtreamAccounts.joinToString("|") { account ->
            "${account.id}:${account.host}:${account.username}:${account.password.hashCode()}"
        }
    }
    var lastXtreamAccountSignature by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(activationState.activationType, xtreamAccountSignature) {
        if (activationState.activationType == "trial_pending_xtream" && xtreamAccounts.isNotEmpty()) {
            runCatching { container.activationRepository.finalizeTrialAfterPlaylistConfigured() }
            activationViewModel.checkNow()
        }
    }

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
            title = strings.configureXtreamTitle,
            onManualAccount = { account ->
                val accountId = container.accountManager.upsert(account)
                container.accountManager.select(accountId)
                container.xtreamRepository.clearCaches()
                container.catalogRepository.invalidateLocalCatalogCache()
                container.xtreamRepository.getLiveCategories()
                container.synchronizeCatalog()
                if (activationState.activationType == "trial_pending_xtream") {
                    runCatching { container.activationRepository.finalizeTrialAfterPlaylistConfigured() }
                    activationViewModel.checkNow()
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
    LaunchedEffect(playerSettings.backgroundSyncEnabled) {
        BackgroundSyncScheduler.applyPeriodicSync(context, playerSettings.backgroundSyncEnabled)
    }
    LaunchedEffect(activationState.activated, xtreamAccountSignature, playerSettings.syncFrequency) {
        if (activationState.activated && xtreamAccounts.isNotEmpty()) {
            val previousSignature = lastXtreamAccountSignature
            lastXtreamAccountSignature = xtreamAccountSignature
            val accountChanged = previousSignature != null && previousSignature != xtreamAccountSignature
            val firstAccountCheck = previousSignature == null
            val startupSync = firstAccountCheck &&
                SyncFrequencyPolicy.from(playerSettings.syncFrequency).runOnStartup
            val shouldVerifyXtream = accountChanged || firstAccountCheck || startupSync
            val shouldSyncCatalog = accountChanged || startupSync
            val startupAlreadyHandled = firstAccountCheck && container.xtreamConnectionManager.hasFreshConnectedState()
            if (shouldVerifyXtream && !startupAlreadyHandled) {
                val connection = container.xtreamConnectionManager.verifyQuick("startup")
                if (!connection.isConnected) {
                    showXtreamConnectionDialog = connection.blocksCatalogForNavigation
                    return@LaunchedEffect
                }
                if (shouldSyncCatalog) {
                    runCatching {
                        container.xtreamRepository.clearCaches()
                        container.catalogRepository.invalidateLocalCatalogCache()
                        container.synchronizeCatalog().getOrThrow()
                    }
                }
            }
        }
    }
    LaunchedEffect(activationState.activated, xtreamAccountSignature, xtreamConnectionState.status) {
        if (activationState.activated && xtreamAccounts.isNotEmpty() && xtreamConnectionState.shouldRetryInBackground) {
            while (isActive && container.xtreamConnectionManager.state.value.shouldRetryInBackground) {
                delay(60_000L)
                val connection = container.xtreamConnectionManager.verifyQuick("background_retry")
                if (connection.isConnected) {
                    showXtreamConnectionDialog = false
                    break
                }
            }
        }
    }
    LaunchedEffect(activationState.activated) {
        if (activationState.activated) {
            while (isActive) {
                appUpdateViewModel.checkForUpdate()
                delay(30 * 60 * 1_000L)
            }
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
    val updateNotificationCount = if (appUpdateState.update != null) 1 else 0
    val hasNewNotifications = notificationBadgeState.hasUnread || updateNotificationCount > 0
    val notificationBadgeCount = notificationBadgeState.unreadCount + updateNotificationCount

    CompositionLocalProvider(LocalTvFocusStyle provides focusStyle) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(AppRoute.Home.route) {
            HomeScreen(
                currentRoute = currentRoute,
                tabs = tabs,
                onNavigate = navigateFromHeader,
                onSync = launchSyncCatalog,
                onSettings = { navController.navigateSingleTop(AppRoute.Settings.route) },
                onProfile = { navController.navigateSingleTop(AppRoute.Profile.route) },
                onNotifications = { navController.navigateSingleTop(AppRoute.Notifications.route) },
                onLicenseKey = { showLicensePurchaseQr = true },
                showLicenseKey = activationState.shouldShowLicenseKey,
                hasNewNotifications = hasNewNotifications,
                notificationBadgeCount = notificationBadgeCount,
                strings = strings,
                xtreamCatalogBlocked = xtreamCatalogBlocked,
                onXtreamBlocked = { showXtreamConnectionDialog = true },
                onContentClick = { item ->
                    if (xtreamCatalogBlocked) {
                        showXtreamConnectionDialog = true
                    } else {
                        navController.navigateFromContinueItem(item)
                    }
                },
                onContinueViewAll = {
                    if (xtreamCatalogBlocked) showXtreamConnectionDialog = true else navController.navigate(AppRoute.ContinueWatching.route)
                },
                onTrendingViewAll = {
                    if (xtreamCatalogBlocked) showXtreamConnectionDialog = true else navController.navigate(AppRoute.Trending.route)
                },
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
                onItemClick = { item ->
                    if (xtreamCatalogBlocked) showXtreamConnectionDialog = true else navController.navigateFromContinueItem(item)
                },
            )
        }
        composable(AppRoute.Trending.route) {
            HomeCollectionsScreen(
                kind = HomeCollectionKind.Trending,
                onBack = { navController.popBackStack() },
                onItemClick = { item ->
                    if (xtreamCatalogBlocked) showXtreamConnectionDialog = true else navController.navigateFromContinueItem(item)
                },
            )
        }
        composable(AppRoute.Live.route) {
            if (xtreamCatalogBlocked) {
                LaunchedEffect(Unit) { showXtreamConnectionDialog = true }
                PlaceholderRouteScreen("Live TV", "Connexion Xtream indisponible.")
            } else LiveTvScreen(
                currentRoute = currentRoute,
                tabs = tabs,
                onNavigate = navigateFromHeader,
                onSync = launchSyncCatalog,
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
            if (xtreamCatalogBlocked) {
                LaunchedEffect(Unit) { showXtreamConnectionDialog = true }
                PlaceholderRouteScreen("Movies", "Connexion Xtream indisponible.")
            } else MoviesScreen(
                currentRoute = currentRoute,
                tabs = tabs,
                onNavigate = navigateFromHeader,
                onSync = launchSyncCatalog,
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
            if (xtreamCatalogBlocked) {
                LaunchedEffect(Unit) { showXtreamConnectionDialog = true }
                PlaceholderRouteScreen("Series", "Connexion Xtream indisponible.")
            } else SeriesScreen(
                currentRoute = currentRoute,
                tabs = tabs,
                onNavigate = navigateFromHeader,
                onSync = launchSyncCatalog,
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
            if (!youtubeAllowed) {
                LaunchedEffect(Unit) { showLicensePurchaseQr = true }
                PlaceholderRouteScreen(strings.youtubePremiumTitle, strings.youtubePremiumSubtitle)
            } else {
                YoutubeScreen(
                    currentRoute = currentRoute,
                    tabs = tabs,
                    onNavigate = navigateFromHeader,
                    onSync = launchSyncCatalog,
                    onSettings = { navController.navigateSingleTop(AppRoute.Settings.route) },
                    onProfile = { navController.navigateSingleTop(AppRoute.Profile.route) },
                    onNotifications = { navController.navigateSingleTop(AppRoute.Notifications.route) },
                    onLicenseKey = { showLicensePurchaseQr = true },
                    showLicenseKey = activationState.shouldShowLicenseKey,
                    hasNewNotifications = hasNewNotifications,
                    notificationBadgeCount = notificationBadgeCount,
                )
            }
        }
        composable(AppRoute.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                updateState = appUpdateState,
                onCheckForUpdate = { appUpdateViewModel.checkForUpdate(revealDialog = true) },
                onSyncCatalog = launchSyncCatalog,
                parentalControlAllowed = parentalControlAllowed,
                onLockedFeature = { showLicensePurchaseQr = true },
            )
        }
        composable(AppRoute.Notifications.route) {
            NotificationsRoute(
                onBack = { navController.popBackStack() },
                onNotificationsSeen = notificationBadgeViewModel::clearUnread,
                updateNotification = appUpdateState.update,
                onOpenUpdate = {
                    appUpdateViewModel.openFromNotification()
                },
            )
        }
        composable("player/{channelId}") { entry ->
            val channelId = entry.arguments?.getString("channelId")?.toIntOrNull()
            if (xtreamCatalogBlocked) {
                LaunchedEffect(Unit) { showXtreamConnectionDialog = true }
                PlaceholderRouteScreen("Lecture live", "Connexion Xtream indisponible.")
            } else if (channelId == null) {
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
            if (xtreamCatalogBlocked) {
                LaunchedEffect(Unit) { showXtreamConnectionDialog = true }
                PlaceholderRouteScreen("Lecture film", "Connexion Xtream indisponible.")
            } else if (movieId == null) {
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
            if (xtreamCatalogBlocked) {
                LaunchedEffect(Unit) { showXtreamConnectionDialog = true }
                PlaceholderRouteScreen("Detail film", "Connexion Xtream indisponible.")
            } else if (movieId == null) {
                PlaceholderRouteScreen("Detail film", "Film introuvable.")
            } else {
                MovieDetailRoute(
                    movieId = movieId,
                    currentRoute = AppRoute.Movies.route,
                    tabs = tabs,
                    onNavigate = navigateFromHeader,
                    onSync = launchSyncCatalog,
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
            if (xtreamCatalogBlocked) {
                LaunchedEffect(Unit) { showXtreamConnectionDialog = true }
                PlaceholderRouteScreen("Lecture serie", "Connexion Xtream indisponible.")
            } else if (episodeId == null) {
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
            if (xtreamCatalogBlocked) {
                LaunchedEffect(Unit) { showXtreamConnectionDialog = true }
                PlaceholderRouteScreen("Detail serie", "Connexion Xtream indisponible.")
            } else if (seriesId == null) {
                PlaceholderRouteScreen("Detail serie", "Serie introuvable.")
            } else {
                SeriesDetailRoute(
                    seriesId = seriesId,
                    currentRoute = AppRoute.Series.route,
                    tabs = tabs,
                    onNavigate = navigateFromHeader,
                    onSync = launchSyncCatalog,
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
            title = strings.premiumPurchaseTitle,
            subtitle = strings.premiumPurchaseSubtitle,
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

    if (showXtreamConnectionDialog) {
        XtreamConnectionAlertDialog(
            state = xtreamConnectionState,
            onEditCredentials = {
                showXtreamSetupDialog = true
            },
            onRetry = {
                scope.launch {
                    val connection = container.xtreamConnectionManager.verifyQuick("user_retry")
                    if (connection.isConnected) {
                        showXtreamConnectionDialog = false
                    }
                }
            },
            onContinue = {
                showXtreamConnectionDialog = false
            },
        )
    }

    if (showXtreamSetupDialog) {
        Dialog(onDismissRequest = { showXtreamSetupDialog = false }) {
            Box(
                modifier = Modifier
                    .width(1100.dp)
                    .height(640.dp),
            ) {
                XtreamQrSetupPanel(
                    activationRepository = container.activationRepository,
                    title = strings.configureXtreamTitle,
                    onManualAccount = { account ->
                        val accountId = container.accountManager.upsert(account)
                        container.accountManager.select(accountId)
                        container.xtreamRepository.clearCaches()
                        container.catalogRepository.invalidateLocalCatalogCache()
                        val connection = container.xtreamConnectionManager.verifyQuick("credentials_edit")
                        if (connection.isConnected) {
                            showXtreamSetupDialog = false
                            showXtreamConnectionDialog = false
                            container.synchronizeCatalog().getOrThrow()
                        } else {
                            throw IllegalStateException(connection.message)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    val update = appUpdateState.update
    if (update != null && appUpdateState.shouldShowDialog) {
        AppUpdateDialog(
            update = update,
            installing = appUpdateState.installing,
            errorMessage = appUpdateState.errorMessage,
            strings = strings,
            onInstall = { appUpdateViewModel.installUpdate(context) },
            onDismiss = appUpdateViewModel::dismiss,
        )
    }
    }

}

@Composable
private fun XtreamConnectionAlertDialog(
    state: XtreamConnectionState,
    onEditCredentials: () -> Unit,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
) {
    val retryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        retryFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onContinue) {
        Column(
            modifier = Modifier
                .width(720.dp)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF142033), Color(0xFF07101D))),
                    RoundedCornerShape(16.dp),
                )
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(16.dp))
                .padding(horizontal = 34.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Connexion Xtream indisponible",
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleS,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "L'application n'arrive pas a se connecter au serveur Xtream associe a votre compte. Vos chaines, films et series sont temporairement indisponibles. Vous pouvez modifier vos identifiants, reessayer la connexion ou continuer sur l'application.",
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Body,
                textAlign = TextAlign.Center,
            )
            if (state.message.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.message,
                    color = SmartVisionColors.Warning,
                    style = SmartVisionType.Caption,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TvButton(
                    text = "Modifier les identifiants",
                    onClick = onEditCredentials,
                    variant = TvButtonVariant.Secondary,
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    modifier = Modifier.height(44.dp),
                )
                Spacer(Modifier.width(10.dp))
                TvButton(
                    text = if (state.checking) "Verification..." else "Reessayer la connexion",
                    onClick = onRetry,
                    enabled = !state.checking,
                    focusRequester = retryFocusRequester,
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    modifier = Modifier.height(44.dp),
                )
                Spacer(Modifier.width(10.dp))
                TvButton(
                    text = "Aller sur l'application",
                    onClick = onContinue,
                    variant = TvButtonVariant.Text,
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    modifier = Modifier.height(44.dp),
                )
            }
        }
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

private fun String.isXtreamCatalogRoute(): Boolean =
    this == AppRoute.Live.route || this == AppRoute.Movies.route || this == AppRoute.Series.route

private enum class AppRoute(val route: String) {
    Home("home"),
    Live("live_tv"),
    Movies("movies"),
    Series("series"),
    Youtube("youtube"),
    Settings("settings"),
    Profile("profile"),
    Notifications("notifications"),
    ContinueWatching("continue_watching"),
    Trending("trending"),
}

private fun headerTabs(strings: SmartVisionStrings) = listOf(
    HomeHeaderTab(strings.home, AppRoute.Home.route),
    HomeHeaderTab(strings.liveTv, AppRoute.Live.route),
    HomeHeaderTab(strings.movies, AppRoute.Movies.route),
    HomeHeaderTab(strings.series, AppRoute.Series.route),
    HomeHeaderTab(strings.youtube, AppRoute.Youtube.route, useYoutubeLogo = true),
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
