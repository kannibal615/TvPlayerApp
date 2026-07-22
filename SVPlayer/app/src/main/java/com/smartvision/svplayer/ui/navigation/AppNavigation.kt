package com.smartvision.svplayer.ui.navigation

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.ProfileType
import com.smartvision.svplayer.core.profile.ProfilePermissions
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.data.mock.ContinueItem
import com.smartvision.svplayer.data.monetization.resolveMonetizationStatus
import com.smartvision.svplayer.data.xtream.XtreamConnectionState
import com.smartvision.svplayer.domain.access.PremiumFeature
import com.smartvision.svplayer.domain.access.PremiumFeatureGate
import com.smartvision.svplayer.domain.access.PremiumFeatureGateResult
import com.smartvision.svplayer.domain.access.PremiumFeatureGateState
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.domain.model.ParentalControlScope
import com.smartvision.svplayer.domain.model.SyncStatus
import com.smartvision.svplayer.domain.repository.CatalogContentCounts
import com.smartvision.svplayer.startup.BackgroundSyncScheduler
import com.smartvision.svplayer.sync.CatalogSyncScheduler
import com.smartvision.svplayer.ui.activation.ActivationScreen
import com.smartvision.svplayer.ui.activation.ActivationViewModel
import com.smartvision.svplayer.ui.activation.XtreamQrSetupPanel
import com.smartvision.svplayer.ui.appconfig.AppConfigViewModel
import com.smartvision.svplayer.ui.appconfig.ConsentDialog
import com.smartvision.svplayer.ui.detail.MovieDetailRoute
import com.smartvision.svplayer.ui.detail.SeriesDetailRoute
import com.smartvision.svplayer.ui.home.HomeHeaderFocusTarget
import com.smartvision.svplayer.ui.home.HomeHeaderTab
import com.smartvision.svplayer.ui.home.HomeScreen
import com.smartvision.svplayer.ui.home.HomeViewModel
import com.smartvision.svplayer.ui.home.visibleForProfile
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.i18n.smartVisionStrings
import com.smartvision.svplayer.ui.live.LiveTvScreen
import com.smartvision.svplayer.ui.media.MediaScreen
import com.smartvision.svplayer.ui.movies.MoviesScreen
import com.smartvision.svplayer.ui.notifications.NotificationBadgeViewModel
import com.smartvision.svplayer.ui.notifications.NotificationsRoute
import com.smartvision.svplayer.ui.player.FullScreenContentKind
import com.smartvision.svplayer.ui.player.FullScreenPlayerRoute
import com.smartvision.svplayer.ui.player.LivePlaybackSession
import com.smartvision.svplayer.ui.player.LocalMediaPlayerRoute
import com.smartvision.svplayer.ui.profile.ProfileRoute
import com.smartvision.svplayer.ui.profile.ProfilePickerScreen
import com.smartvision.svplayer.ui.profile.ProfileSelectionRequest
import com.smartvision.svplayer.ui.profile.SmartVisionQrDialog
import com.smartvision.svplayer.ui.profile.canDisplayGlobalProfilePicker
import com.smartvision.svplayer.ui.profile.canRevealProfilePickerAfterHome
import com.smartvision.svplayer.ui.profile.canStartProfileSelectionFromPicker
import com.smartvision.svplayer.ui.profile.canCompleteProfileSelection
import com.smartvision.svplayer.ui.series.SeriesScreen
import com.smartvision.svplayer.ui.settings.SettingsScreen
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.components.TvConfirmationDialog
import com.smartvision.svplayer.ui.components.TvDialogSurface
import com.smartvision.svplayer.ui.components.TvDialogTone
import com.smartvision.svplayer.ui.focus.LocalTvFocusStyle
import com.smartvision.svplayer.ui.focus.LocalTvAnimationsEnabled
import com.smartvision.svplayer.ui.focus.TvFocusStyles
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.LocalLoadingColor
import com.smartvision.svplayer.ui.theme.SmartVisionLoadingColors
import com.smartvision.svplayer.ui.theme.SmartVisionType
import com.smartvision.svplayer.ui.update.AppUpdateDialog
import com.smartvision.svplayer.ui.update.AppUpdateViewModel
import com.smartvision.svplayer.ui.youtube.YoutubeScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
    onInitialSurfaceReady: () -> Unit = {},
    initialSurfaceVisible: Boolean = true,
) {
    val container = LocalAppContainer.current
    val latestInitialSurfaceReady by rememberUpdatedState(onInitialSurfaceReady)
    val scope = rememberCoroutineScope()
    val activationViewModel: ActivationViewModel = viewModel(
        factory = viewModelFactory {
            ActivationViewModel(
                repository = container.activationRepository,
                initialLocalState = container.startupActivationState,
            )
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
    val parentalControlScope by container.settingsRepository.parentalControlScope.collectAsStateWithLifecycle(
        initialValue = ParentalControlScope(),
    )
    val strings = smartVisionStrings(playerSettings.language)
    val focusStyle = remember(
        playerSettings.focusStyle,
        playerSettings.focusColor,
        playerSettings.focusEffect,
        playerSettings.focusBackground,
        playerSettings.focusSelectedColor,
        playerSettings.focusActiveColor,
        playerSettings.focusParentColor,
        playerSettings.focusHaloDistance,
        playerSettings.focusHaloColor,
        playerSettings.focusHaloOpacity,
    ) {
        TvFocusStyles.fromKeys(
            playerSettings.focusStyle,
            playerSettings.focusColor,
            playerSettings.focusEffect,
            playerSettings.focusBackground,
            playerSettings.focusSelectedColor,
            playerSettings.focusActiveColor,
            playerSettings.focusParentColor,
            playerSettings.focusHaloDistance,
            playerSettings.focusHaloColor,
            playerSettings.focusHaloOpacity,
        )
    }
    val loadingColor = remember(playerSettings.loadingColor) {
        SmartVisionLoadingColors.fromKey(playerSettings.loadingColor)
    }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: AppRoute.Home.route
    val homeRouteIsActive = backStack?.destination?.route == AppRoute.Home.route
    var showExitConfirmation by remember { mutableStateOf(false) }
    var showLicensePurchaseQr by remember { mutableStateOf(false) }
    var showXtreamConnectionDialog by remember { mutableStateOf(false) }
    var showXtreamSetupDialog by remember { mutableStateOf(false) }
    var premiumLicenseCode by remember { mutableStateOf("") }
    var liveReturnFocusChannelId by remember { mutableStateOf<Int?>(null) }
    var movieReturnFocusId by remember { mutableStateOf<Int?>(null) }
    var seriesReturnFocusId by remember { mutableStateOf<Int?>(null) }
    var episodeReturnFocusSeriesId by remember { mutableStateOf<Int?>(null) }
    var episodeDetailReturnFocusId by remember { mutableStateOf<Int?>(null) }
    var homeHeaderFocusRequest by remember { mutableStateOf(0) }
    var homeHeaderFocusTarget by remember { mutableStateOf(HomeHeaderFocusTarget.CurrentTab) }
    var profilePickerCompleted by remember { mutableStateOf(false) }
    var openProfilePickerAfterHome by remember { mutableStateOf(false) }
    var profileSelectionRequest by remember { mutableStateOf<ProfileSelectionRequest?>(null) }
    var profileSelectionRequestCounter by remember { mutableStateOf(0L) }
    var profileActivationCompletedRequestId by remember { mutableStateOf<Long?>(null) }
    var profileSwitchActivationJob by remember { mutableStateOf<Job?>(null) }
    var homeProfileAvatarBounds by remember { mutableStateOf<Rect?>(null) }
    val activePlaylistSource by container.accountManager.activePlaylistSource.collectAsStateWithLifecycle()
    val playlistProfiles by container.accountManager.profiles.collectAsStateWithLifecycle()
    val configuredPickerProfiles = remember(playlistProfiles) {
        playlistProfiles.filter { it.isConfigured }
    }
    val profilePickerWanted = !profilePickerCompleted && configuredPickerProfiles.isNotEmpty()
    val activeProfileId by container.accountManager.activeProfileId.collectAsStateWithLifecycle()
    val homeViewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            HomeViewModel(
                userContentRepository = container.userContentRepository,
                catalogRepository = container.catalogRepository,
                homeSlidesRepository = container.homeSlidesRepository,
                homeContentRepository = container.homeContentRepository,
                accountManager = container.accountManager,
                settingsRepository = container.settingsRepository,
            )
        },
    )
    val rawHomeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val authoritativeCatalogRevision by container.catalogRepository.catalogRevision.collectAsStateWithLifecycle()
    val coordinatedHomeState = rawHomeState.visibleForProfile(activeProfileId.orEmpty())
    val activeProfile = remember(playlistProfiles, activeProfileId) {
        playlistProfiles.firstOrNull { it.id == activeProfileId }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var appInForeground by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            appInForeground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val showProfilePicker = canDisplayGlobalProfilePicker(
        pickerWanted = profilePickerWanted,
        homeIsActive = homeRouteIsActive,
        openRequested = openProfilePickerAfterHome,
        waitingForFirstRoute = backStack == null,
    )
    val maskHomeBeforeProfileSelection =
        (profilePickerWanted || openProfilePickerAfterHome) && profileSelectionRequest == null

    LaunchedEffect(profilePickerWanted, activationState.localStateReady) {
        if (!profilePickerWanted && activationState.localStateReady) {
            withFrameNanos { }
            latestInitialSurfaceReady()
        }
    }
    val profilePermissions = remember(activeProfile?.type) {
        ProfilePermissions.forType(activeProfile?.type ?: ProfileType.ADMIN)
    }
    val xtreamConnectionState by container.xtreamConnectionManager.state.collectAsStateWithLifecycle()
    val xtreamCatalogBlocked = activePlaylistSource == PlaylistSource.Xtream && xtreamConnectionState.blocksCatalogForNavigation
    val xtreamCatalogVisualBlocked = activePlaylistSource == PlaylistSource.Xtream && xtreamConnectionState.blocksCatalog
    val context = LocalContext.current
    val activity = context as? Activity
    val livePlaybackSession = remember(context) { LivePlaybackSession(context) }
    var moviePlayerEnterBounds by remember { mutableStateOf<Rect?>(null) }
    var seriesPlayerEnterBounds by remember { mutableStateOf<Rect?>(null) }
    DisposableEffect(livePlaybackSession) {
        onDispose { livePlaybackSession.release() }
    }
    LaunchedEffect(currentRoute) {
        if (currentRoute != AppRoute.Live.route && !currentRoute.startsWith("player/")) {
            livePlaybackSession.stop()
        }
    }
    val monetizationStatus = resolveMonetizationStatus(
        activationType = activationState.activationType,
        licenseStatus = activationState.licenseStatus,
        trialStatus = activationState.trialStatus,
        freeWithAdsStatus = activationState.freeWithAdsStatus,
    )
    val loadedYoutubeAllowed = container.appConfigRepository.isFeatureAllowed(
        config = appConfigState.config,
        featureKey = "youtube",
        status = monetizationStatus,
    )
    val youtubeAllowed = if (appConfigState.loading) true else loadedYoutubeAllowed
    val loadedMediaCenterGate = PremiumFeatureGate.evaluate(
        config = appConfigState.config,
        feature = PremiumFeature.MEDIA_CENTER,
        status = monetizationStatus,
    )
    val mediaCenterGate = if (appConfigState.loading) {
        PremiumFeatureGateResult(PremiumFeature.MEDIA_CENTER, PremiumFeatureGateState.Allowed)
    } else {
        loadedMediaCenterGate
    }
    val recorderGate = PremiumFeatureGate.evaluate(
        config = appConfigState.config,
        feature = PremiumFeature.RECORDER,
        status = monetizationStatus,
    )
    val loadedMediaPhoneTransferGate = PremiumFeatureGate.evaluate(
        config = appConfigState.config,
        feature = PremiumFeature.MEDIA_PHONE_TRANSFER,
        status = monetizationStatus,
    )
    val mediaPhoneTransferGate = if (appConfigState.loading) {
        PremiumFeatureGateResult(PremiumFeature.MEDIA_PHONE_TRANSFER, PremiumFeatureGateState.Allowed)
    } else {
        loadedMediaPhoneTransferGate
    }
    val loadedMultiProfileGate = PremiumFeatureGate.evaluate(
        config = appConfigState.config,
        feature = PremiumFeature.MULTI_PROFILE,
        status = monetizationStatus,
    )
    val multiProfileGate = if (appConfigState.loading) {
        PremiumFeatureGateResult(PremiumFeature.MULTI_PROFILE, PremiumFeatureGateState.Allowed)
    } else {
        loadedMultiProfileGate
    }
    val parentalControlAllowed = container.appConfigRepository.isFeatureAllowed(
        config = appConfigState.config,
        featureKey = "parental_control",
        status = monetizationStatus,
    )
    val tabs = remember(youtubeAllowed, mediaCenterGate, strings, xtreamCatalogVisualBlocked, activeProfile?.type) {
        headerTabs(strings).mapNotNull { tab ->
            when {
                activeProfile?.type == ProfileType.KIDS && tab.route == AppRoute.Media.route -> null
                tab.route == AppRoute.Youtube.route -> tab.copy(locked = !youtubeAllowed)
                tab.route == AppRoute.Media.route && !mediaCenterGate.showDisabledControl -> null
                tab.route == AppRoute.Media.route -> tab.copy(locked = mediaCenterGate.locked)
                tab.route.isXtreamCatalogRoute() -> tab.copy(warning = xtreamCatalogVisualBlocked)
                else -> tab
            }
        }
    }
    fun navigateHomeWithHeaderFocus(target: HomeHeaderFocusTarget = HomeHeaderFocusTarget.CurrentTab) {
        homeHeaderFocusTarget = target
        homeHeaderFocusRequest += 1
        navController.navigate(AppRoute.Home.route) {
            popUpTo(AppRoute.Home.route) { inclusive = false }
            launchSingleTop = true
            restoreState = true
        }
    }
    val navigateFromHeader: (String) -> Unit = { route ->
        if (!route.isAllowedFor(profilePermissions)) {
            navigateHomeWithHeaderFocus()
        } else if (route.isXtreamCatalogRoute() && xtreamCatalogBlocked) {
            showXtreamConnectionDialog = true
        } else if (route == AppRoute.Youtube.route && !youtubeAllowed) {
            if (profilePermissions.canAccessPremium) showLicensePurchaseQr = true
        } else if (route == AppRoute.Media.route && !mediaCenterGate.allowed) {
            if (mediaCenterGate.shouldShowUpgradePrompt) {
                showLicensePurchaseQr = true
            }
        } else if (route == AppRoute.Home.route) {
            navigateHomeWithHeaderFocus()
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
    LaunchedEffect(currentRoute) {
        RemoteSettingsNavigation.requests.collect {
            if (currentRoute != AppRoute.Settings.route) {
                navController.navigateSingleTop(AppRoute.Settings.route)
            }
        }
    }
    val openProfilePickerFromHome: () -> Unit = {
        if (profileSelectionRequest == null && !openProfilePickerAfterHome) {
            openProfilePickerAfterHome = true
            if (!homeRouteIsActive) {
                navController.navigate(AppRoute.Home.route) {
                    popUpTo(AppRoute.Home.route) { inclusive = false }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
    LaunchedEffect(profilePickerWanted, backStack, homeRouteIsActive, profileSelectionRequest) {
        if (
            profilePickerWanted &&
            backStack != null &&
            !homeRouteIsActive &&
            profileSelectionRequest == null &&
            !openProfilePickerAfterHome
        ) {
            openProfilePickerFromHome()
        }
    }
    LaunchedEffect(openProfilePickerAfterHome, currentRoute, appInForeground) {
        if (
            !canRevealProfilePickerAfterHome(
                openRequested = openProfilePickerAfterHome,
                homeIsActive = homeRouteIsActive,
                appInForeground = appInForeground,
            )
        ) {
            return@LaunchedEffect
        }
        withFrameNanos { }
        if (
            navController.currentDestination?.route != AppRoute.Home.route ||
            !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        ) {
            return@LaunchedEffect
        }
        profileSelectionRequest = null
        profileActivationCompletedRequestId = null
        profilePickerCompleted = false
        openProfilePickerAfterHome = false
    }
    val onProfileAction: () -> Unit = {
        if (!profilePermissions.canManageProfiles) {
            openProfilePickerFromHome()
        } else {
            navigateFromHeader(AppRoute.Profile.route)
        }
    }
    LaunchedEffect(Unit) {
        container.xtreamConnectionManager.alertRequests.collect {
            if (container.xtreamConnectionManager.state.value.blocksCatalog) {
                showXtreamConnectionDialog = true
            }
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
        if (container.accountManager.activePlaylistSource.value == PlaylistSource.Xtream) {
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
    LaunchedEffect(activationState.activated, appConfigState.loading, parentalControlAllowed, parentalControlScope.enabled, playerSettings.parentalPin, playerSettings.parentalKeywords) {
        if (!appConfigState.loading &&
            activationState.activated &&
            !parentalControlAllowed &&
            (parentalControlScope.enabled || playerSettings.parentalPin.isNotBlank() || playerSettings.parentalKeywords != "adults; porn; xxx")
        ) {
            container.settingsRepository.resetParentalControl()
            container.xtreamRepository.clearCaches()
            container.catalogRepository.invalidateLocalCatalogCache()
            runCatching { container.synchronizeCatalog() }
        }
    }

    if (appConfigState.consentRequired) {
        CompositionLocalProvider(
            LocalTvFocusStyle provides focusStyle,
            LocalTvAnimationsEnabled provides playerSettings.animationsEnabled,
            LocalLoadingColor provides loadingColor,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF020714)),
            ) {
                ConsentDialog(
                    consent = appConfigState.config.consent,
                    onAccept = appConfigViewModel::acceptConsent,
                )
            }
        }
        return
    }

    val xtreamAccounts by container.accountManager.accounts.collectAsStateWithLifecycle()
    val m3uUrl by container.accountManager.m3uUrl.collectAsStateWithLifecycle()
    val hasPlayableSource = xtreamAccounts.isNotEmpty() || m3uUrl.isNotBlank()
    LaunchedEffect(
        activationState.localStateReady,
        activationState.checking,
        activationState.activated,
        hasPlayableSource,
        currentRoute,
    ) {
        Log.i(
            TAG_STARTUP,
            "state localReady=${activationState.localStateReady} checking=${activationState.checking} " +
                "activated=${activationState.activated} playableSource=$hasPlayableSource route=$currentRoute",
        )
    }
    if (!activationState.localStateReady) {
        // PERF_FIX: avoid the visible grey/blank frame between the splash handoff and Home.
        // ActivationViewModel only needs a very short local cache read here; keep a real app background.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF01040C),
                            Color(0xFF061426),
                            Color(0xFF01040C),
                        ),
                    ),
                ),
        )
        return
    }
    val xtreamAccountSignature = remember(xtreamAccounts) {
        xtreamAccounts.joinToString("|") { account ->
            "${account.id}:${account.host}:${account.username}:${account.password.hashCode()}"
        }
    }
    val m3uProfileSignature = remember(activeProfileId, m3uUrl) {
        "${activeProfileId.orEmpty()}:$m3uUrl"
    }
    var lastXtreamAccountSignature by remember { mutableStateOf<String?>(null) }
    var lastXtreamProfileId by remember { mutableStateOf<String?>(null) }
    var lastM3uProfileSignature by remember { mutableStateOf<String?>(null) }
    var lastM3uProfileId by remember { mutableStateOf<String?>(null) }
    val startProfileActivation: (String) -> Job = { profileId ->
        val profileChanged = container.accountManager.activeProfileId.value != profileId
        if (profileChanged) {
            profileSwitchActivationJob?.cancel()
            container.accountManager.activateProfile(profileId)
            container.xtreamRepository.clearCaches()
            container.homeContentRepository.invalidateTrending()
        }
        scope.launch {
            if (profileChanged) {
                // Profile cards only switch the active local scope. Network catalog
                // work is evaluated and launched by Home after the picker is gone.
                container.catalogRepository.clearCatalogForProfileSwitch()
            }
        }.also { profileSwitchActivationJob = it }
    }
    val requestProfileSelection: (String) -> Unit = requestProfileSelection@{ profileId ->
        if (
            !canStartProfileSelectionFromPicker(
                homeIsActive = homeRouteIsActive,
                appInForeground = appInForeground,
                selectionInProgress = profileSelectionRequest != null,
            )
        ) {
            return@requestProfileSelection
        }
        val request = ProfileSelectionRequest(
            requestId = ++profileSelectionRequestCounter,
            profileId = profileId,
        )
        profileSelectionRequest = request
        profileActivationCompletedRequestId = null
        profilePickerCompleted = false
        runCatching {
            startProfileActivation(profileId)
        }.onSuccess { activationJob ->
            scope.launch {
                activationJob.join()
                if (
                    profileSelectionRequest == request &&
                    container.accountManager.activeProfileId.value == request.profileId
                ) {
                    profileActivationCompletedRequestId = request.requestId
                }
            }
        }.onFailure {
            if (profileSelectionRequest == request) {
                profileSelectionRequest = null
            }
        }
    }
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
                strings = strings,
                onDismiss = { showExitConfirmation = false },
                onExit = { activity?.finishAffinity() },
            )
        }
        return
    }

    if (!hasPlayableSource) {
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
                strings = strings,
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
        if (activationState.activated && activePlaylistSource == PlaylistSource.Xtream && xtreamAccounts.isNotEmpty()) {
            val previousSignature = lastXtreamAccountSignature
            val previousProfileId = lastXtreamProfileId
            lastXtreamAccountSignature = xtreamAccountSignature
            lastXtreamProfileId = activeProfileId
            val accountChanged = previousSignature != null && previousSignature != xtreamAccountSignature
            val firstAccountCheck = previousSignature == null
            val sameProfileSourceChanged = accountChanged && previousProfileId == activeProfileId
            val shouldVerifyXtream = accountChanged || firstAccountCheck
            val shouldSyncCatalog = accountChanged && sameProfileSourceChanged
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
    LaunchedEffect(activationState.activated, activePlaylistSource, m3uProfileSignature, playerSettings.syncFrequency) {
        if (activationState.activated && activePlaylistSource == PlaylistSource.M3u && m3uUrl.isNotBlank()) {
            val previousSignature = lastM3uProfileSignature
            val previousProfileId = lastM3uProfileId
            lastM3uProfileSignature = m3uProfileSignature
            lastM3uProfileId = activeProfileId
            val sourceChanged = previousSignature != null && previousSignature != m3uProfileSignature
            val sameProfileSourceChanged = sourceChanged && previousProfileId == activeProfileId
            val shouldSyncCatalog = sourceChanged && sameProfileSourceChanged
            if (shouldSyncCatalog) {
                runCatching {
                    container.xtreamRepository.clearCaches()
                    container.catalogRepository.invalidateLocalCatalogCache()
                    container.synchronizeCatalog().getOrThrow()
                }
            }
        }
    }
    LaunchedEffect(activationState.activated, xtreamAccountSignature, xtreamConnectionState.status) {
        if (activationState.activated && activePlaylistSource == PlaylistSource.Xtream && xtreamAccounts.isNotEmpty() && xtreamConnectionState.shouldRetryInBackground) {
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
        if (showLicensePurchaseQr && (!activationState.shouldShowLicenseKey || !profilePermissions.canAccessPremium)) {
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
    LaunchedEffect(appInForeground) {
        if (appInForeground) notificationBadgeViewModel.refresh()
    }
    val hasNewNotifications = notificationBadgeState.hasUnread
    val notificationBadgeCount = notificationBadgeState.unreadCount

    CompositionLocalProvider(
        LocalTvFocusStyle provides focusStyle,
        LocalTvAnimationsEnabled provides playerSettings.animationsEnabled,
        LocalLoadingColor provides loadingColor,
    ) {
    val routeAllowedForProfile = currentRoute.isAllowedFor(profilePermissions)
    LaunchedEffect(currentRoute, profilePermissions) {
        if (!routeAllowedForProfile) {
            navController.navigate(AppRoute.Home.route) {
                launchSingleTop = true
                popUpTo(AppRoute.Home.route) { inclusive = false }
            }
        }
    }
    if (!routeAllowedForProfile) {
        Box(Modifier.fillMaxSize().background(SmartVisionColors.Background))
        return@CompositionLocalProvider
    }
    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
        modifier = modifier
            .fillMaxSize()
            .background(SmartVisionColors.Background),
    ) {
        composable(AppRoute.Home.route) {
            key(activeProfileId) {
                HomeScreen(
                    viewModel = homeViewModel,
                    state = coordinatedHomeState,
                    activeProfile = activeProfile,
                    currentRoute = currentRoute,
                    tabs = tabs,
                    onNavigate = navigateFromHeader,
                    onSync = launchSyncCatalog,
                    onSettings = { navigateFromHeader(AppRoute.Settings.route) },
                    onProfile = onProfileAction,
                    onNotifications = { navigateFromHeader(AppRoute.Notifications.route) },
                    onLicenseKey = { showLicensePurchaseQr = true },
                    showLicenseKey = activationState.shouldShowLicenseKey,
                    hasNewNotifications = hasNewNotifications,
                    notificationBadgeCount = notificationBadgeCount,
                    headerFocusRequest = homeHeaderFocusRequest,
                    headerFocusTarget = homeHeaderFocusTarget,
                    visibleToUser = !showProfilePicker && !openProfilePickerAfterHome && !maskHomeBeforeProfileSelection,
                    onProfileAvatarBoundsChanged = { bounds ->
                        homeProfileAvatarBounds = bounds
                    },
                    strings = strings,
                    xtreamCatalogBlocked = xtreamCatalogVisualBlocked,
                    xtreamCatalogNavigationBlocked = xtreamCatalogBlocked,
                    onXtreamBlocked = { showXtreamConnectionDialog = true },
                    onContentClick = { item ->
                        if (xtreamCatalogBlocked) {
                            showXtreamConnectionDialog = true
                        } else {
                            moviePlayerEnterBounds = null
                            seriesPlayerEnterBounds = null
                            navController.navigateFromContinueItem(item)
                        }
                    },
                    onTrendingContentClick = { item ->
                        if (xtreamCatalogBlocked) {
                            showXtreamConnectionDialog = true
                        } else {
                            moviePlayerEnterBounds = null
                            seriesPlayerEnterBounds = null
                            navController.navigateFromTrendingItem(item)
                        }
                    },
                )
            }
        }
        composable(AppRoute.Profile.route) {
            ProfileRoute(
                strings = strings,
                onBack = { navigateHomeWithHeaderFocus(HomeHeaderFocusTarget.Profile) },
                onSettings = { navController.navigateSingleTop(AppRoute.Settings.route) },
                currentRoute = currentRoute,
                tabs = tabs,
                onNavigate = navigateFromHeader,
                onSync = launchSyncCatalog,
                onNotifications = { navController.navigateSingleTop(AppRoute.Notifications.route) },
                onLicenseKey = { showLicensePurchaseQr = true },
                showLicenseKey = activationState.shouldShowLicenseKey,
                hasNewNotifications = hasNewNotifications,
                notificationBadgeCount = notificationBadgeCount,
                multiProfileAccess = multiProfileGate,
                onLockedFeature = {
                    if (multiProfileGate.shouldShowUpgradePrompt) {
                        showLicensePurchaseQr = true
                    }
                },
                onSyncCatalog = syncCatalog,
                onActivationChanged = activationViewModel::checkNow,
                startDestination = com.smartvision.svplayer.ui.profile.ProfileAreaDestination.INFO,
                onOpenInfo = {},
                onOpenManage = { navController.navigateSingleTop(AppRoute.ManageProfiles.route) },
                onRequestGlobalProfilePicker = openProfilePickerFromHome,
            )
        }
        composable(AppRoute.ManageProfiles.route) {
            ProfileRoute(
                strings = strings,
                onBack = { navController.navigateSingleTop(AppRoute.Profile.route) },
                onSettings = { navController.navigateSingleTop(AppRoute.Settings.route) },
                currentRoute = currentRoute,
                tabs = tabs,
                onNavigate = navigateFromHeader,
                onSync = launchSyncCatalog,
                onNotifications = { navController.navigateSingleTop(AppRoute.Notifications.route) },
                onLicenseKey = { showLicensePurchaseQr = true },
                showLicenseKey = activationState.shouldShowLicenseKey,
                hasNewNotifications = hasNewNotifications,
                notificationBadgeCount = notificationBadgeCount,
                multiProfileAccess = multiProfileGate,
                onLockedFeature = {
                    if (multiProfileGate.shouldShowUpgradePrompt) showLicensePurchaseQr = true
                },
                onSyncCatalog = syncCatalog,
                onActivationChanged = activationViewModel::checkNow,
                startDestination = com.smartvision.svplayer.ui.profile.ProfileAreaDestination.MANAGE,
                onOpenInfo = { navController.navigateSingleTop(AppRoute.Profile.route) },
                onOpenManage = {},
                onRequestGlobalProfilePicker = openProfilePickerFromHome,
            )
        }
        composable(AppRoute.Live.route) {
            if (xtreamCatalogBlocked) {
                LaunchedEffect(Unit) { showXtreamConnectionDialog = true }
                PlaceholderRouteScreen("Live TV", "Connexion Xtream indisponible.")
            } else LiveTvScreen(
                strings = strings,
                currentRoute = currentRoute,
                tabs = tabs,
                onNavigate = navigateFromHeader,
                onSync = launchSyncCatalog,
                onSettings = { navigateFromHeader(AppRoute.Settings.route) },
                onProfile = onProfileAction,
                onNotifications = { navigateFromHeader(AppRoute.Notifications.route) },
                onLicenseKey = { showLicensePurchaseQr = true },
                showLicenseKey = activationState.shouldShowLicenseKey,
                hasNewNotifications = hasNewNotifications,
                notificationBadgeCount = notificationBadgeCount,
                returnFocusChannelId = liveReturnFocusChannelId,
                onReturnFocusConsumed = { liveReturnFocusChannelId = null },
                playbackSession = livePlaybackSession,
                onWatch = { channelId -> navController.navigate("player/$channelId") },
            )
        }
        composable(AppRoute.Movies.route) {
            if (xtreamCatalogBlocked) {
                LaunchedEffect(Unit) { showXtreamConnectionDialog = true }
                PlaceholderRouteScreen("Movies", "Connexion Xtream indisponible.")
            } else MoviesScreen(
                strings = strings,
                currentRoute = currentRoute,
                tabs = tabs,
                onNavigate = navigateFromHeader,
                onSync = launchSyncCatalog,
                onSettings = { navigateFromHeader(AppRoute.Settings.route) },
                onProfile = onProfileAction,
                onNotifications = { navigateFromHeader(AppRoute.Notifications.route) },
                onLicenseKey = { showLicensePurchaseQr = true },
                showLicenseKey = activationState.shouldShowLicenseKey,
                hasNewNotifications = hasNewNotifications,
                notificationBadgeCount = notificationBadgeCount,
                returnFocusMovieId = movieReturnFocusId,
                onReturnFocusConsumed = { movieReturnFocusId = null },
                onOpenMovieDetails = { movieId -> navController.navigate("movie_detail/$movieId") },
                onWatchMovie = { movieId ->
                    moviePlayerEnterBounds = null
                    navController.navigate("movie_player/$movieId")
                },
            )
        }
        composable(AppRoute.Series.route) {
            if (xtreamCatalogBlocked) {
                LaunchedEffect(Unit) { showXtreamConnectionDialog = true }
                PlaceholderRouteScreen("Series", "Connexion Xtream indisponible.")
            } else SeriesScreen(
                strings = strings,
                currentRoute = currentRoute,
                tabs = tabs,
                onNavigate = navigateFromHeader,
                onSync = launchSyncCatalog,
                onSettings = { navigateFromHeader(AppRoute.Settings.route) },
                onProfile = onProfileAction,
                onNotifications = { navigateFromHeader(AppRoute.Notifications.route) },
                onLicenseKey = { showLicensePurchaseQr = true },
                showLicenseKey = activationState.shouldShowLicenseKey,
                hasNewNotifications = hasNewNotifications,
                notificationBadgeCount = notificationBadgeCount,
                returnFocusSeriesId = seriesReturnFocusId,
                onReturnFocusConsumed = { seriesReturnFocusId = null },
                onOpenSeriesDetails = { seriesId -> navController.navigate("series_detail/$seriesId") },
                onWatchEpisode = { episodeId, seriesId ->
                    episodeReturnFocusSeriesId = seriesId
                    seriesPlayerEnterBounds = null
                    navController.navigate("episode_player/$episodeId")
                },
            )
        }
        composable(AppRoute.Media.route) {
            if (!mediaCenterGate.showDisabledControl) {
                PlaceholderRouteScreen(strings.media, strings.mediaDisabledByAdmin)
            } else {
                MediaScreen(
                    currentRoute = currentRoute,
                    tabs = tabs,
                    onNavigate = navigateFromHeader,
                    onSync = launchSyncCatalog,
                    onSettings = { navigateFromHeader(AppRoute.Settings.route) },
                    onProfile = onProfileAction,
                    onNotifications = { navigateFromHeader(AppRoute.Notifications.route) },
                    onLicenseKey = { showLicensePurchaseQr = true },
                    showLicenseKey = activationState.shouldShowLicenseKey,
                    hasNewNotifications = hasNewNotifications,
                    notificationBadgeCount = notificationBadgeCount,
                    strings = strings,
                    access = mediaCenterGate,
                    transferAccess = mediaPhoneTransferGate,
                    onPlayFile = { mediaFileId -> navController.navigate("media_player/$mediaFileId") },
                    onLockedFeature = {
                        if (mediaCenterGate.shouldShowUpgradePrompt || mediaPhoneTransferGate.shouldShowUpgradePrompt) {
                            showLicensePurchaseQr = true
                        }
                    },
                )
            }
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
                    onSettings = { navigateFromHeader(AppRoute.Settings.route) },
                    onProfile = onProfileAction,
                    onNotifications = { navigateFromHeader(AppRoute.Notifications.route) },
                    onLicenseKey = { showLicensePurchaseQr = true },
                    showLicenseKey = activationState.shouldShowLicenseKey,
                    hasNewNotifications = hasNewNotifications,
                    notificationBadgeCount = notificationBadgeCount,
                )
            }
        }
        composable(AppRoute.Settings.route) {
            SettingsScreen(
                onBack = { navigateHomeWithHeaderFocus(HomeHeaderFocusTarget.Settings) },
                currentRoute = currentRoute,
                tabs = tabs,
                onNavigate = navigateFromHeader,
                onSync = launchSyncCatalog,
                onSettings = {},
                onProfile = onProfileAction,
                onNotifications = { navigateFromHeader(AppRoute.Notifications.route) },
                onLicenseKey = { showLicensePurchaseQr = true },
                showLicenseKey = activationState.shouldShowLicenseKey,
                hasNewNotifications = hasNewNotifications,
                notificationBadgeCount = notificationBadgeCount,
                updateState = appUpdateState,
                onCheckForUpdate = { appUpdateViewModel.checkForUpdate(revealDialog = true) },
            )
        }
        composable(AppRoute.Notifications.route) {
            NotificationsRoute(
                currentRoute = currentRoute,
                tabs = tabs,
                onNavigate = navigateFromHeader,
                onSync = launchSyncCatalog,
                onSettings = { navigateFromHeader(AppRoute.Settings.route) },
                onProfile = onProfileAction,
                onLicenseKey = { showLicensePurchaseQr = true },
                showLicenseKey = activationState.shouldShowLicenseKey,
                hasNewNotifications = hasNewNotifications,
                notificationBadgeCount = notificationBadgeCount,
                onBack = {
                    homeHeaderFocusTarget = HomeHeaderFocusTarget.Notifications
                    homeHeaderFocusRequest += 1
                    navController.popBackStack()
                },
                onNotificationsChanged = notificationBadgeViewModel::refresh,
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
                    onBackWithCurrentContent = { currentChannelId ->
                        liveReturnFocusChannelId = currentChannelId
                        navController.popBackStack()
                    },
                    onPlayLive = { nextChannelId ->
                        navController.navigate("player/$nextChannelId") {
                            popUpTo("player/{channelId}") { inclusive = true }
                        }
                    },
                    recorderAccess = recorderGate,
                    strings = strings,
                    livePlaybackSession = livePlaybackSession,
                    onRecorderLocked = {
                        if (recorderGate.shouldShowUpgradePrompt) {
                            showLicensePurchaseQr = true
                        }
                    },
                )
            }
        }
        composable("media_player/{mediaFileId}") { entry ->
            val mediaFileId = entry.arguments?.getString("mediaFileId")?.toLongOrNull()
            if (!mediaCenterGate.allowed || mediaFileId == null) {
                PlaceholderRouteScreen(strings.media, strings.mediaPlaybackUnavailable)
            } else {
                LocalMediaPlayerRoute(
                    mediaFileId = mediaFileId,
                    strings = strings,
                    onBack = { navController.popBackStack() },
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
                    onBackWithCurrentContent = { currentMovieId ->
                        movieReturnFocusId = currentMovieId
                        navController.popBackStack()
                    },
                    onPlayMovie = { nextMovieId ->
                        moviePlayerEnterBounds = null
                        navController.navigate("movie_player/$nextMovieId") {
                            popUpTo("movie_player/{movieId}") { inclusive = true }
                        }
                    },
                    strings = strings,
                    enterFromBounds = moviePlayerEnterBounds,
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
                KidsContentRouteGuard(
                    kidsMode = activeProfile?.type == ProfileType.KIDS,
                    contentKey = "movie:$movieId:${activeProfileId.orEmpty()}",
                    isAllowed = { container.catalogRepository.getMovieById(movieId) != null },
                    onDenied = { navController.navigateSingleTop(AppRoute.Home.route) },
                ) {
                    MovieDetailRoute(
                    movieId = movieId,
                    currentRoute = AppRoute.Movies.route,
                    tabs = tabs,
                    onNavigate = navigateFromHeader,
                    onSync = launchSyncCatalog,
                    onSettings = { navigateFromHeader(AppRoute.Settings.route) },
                    onProfile = onProfileAction,
                    onNotifications = { navigateFromHeader(AppRoute.Notifications.route) },
                    onLicenseKey = { showLicensePurchaseQr = true },
                    showLicenseKey = activationState.shouldShowLicenseKey,
                    hasNewNotifications = hasNewNotifications,
                    notificationBadgeCount = notificationBadgeCount,
                    onWatchMovie = { id ->
                        movieReturnFocusId = id
                        moviePlayerEnterBounds = null
                        navController.navigate("movie_player/$id")
                    },
                )
            }
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
                    onBackWithCurrentContent = {
                        episodeReturnFocusSeriesId?.let { seriesId ->
                            seriesReturnFocusId = seriesId
                        }
                        episodeReturnFocusSeriesId = null
                        navController.popBackStack()
                    },
                    onPlayEpisode = { nextEpisodeId ->
                        seriesPlayerEnterBounds = null
                        navController.navigate("episode_player/$nextEpisodeId") {
                            popUpTo("episode_player/{episodeId}") { inclusive = true }
                        }
                    },
                    onOpenSeriesDetails = { seriesId ->
                        seriesReturnFocusId = seriesId
                        episodeReturnFocusSeriesId = null
                        navController.navigate("series_detail/$seriesId") {
                            popUpTo("episode_player/{episodeId}") { inclusive = true }
                        }
                    },
                    strings = strings,
                    enterFromBounds = seriesPlayerEnterBounds,
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
                KidsContentRouteGuard(
                    kidsMode = activeProfile?.type == ProfileType.KIDS,
                    contentKey = "series:$seriesId:${activeProfileId.orEmpty()}",
                    isAllowed = { container.catalogRepository.getSeriesByIds(listOf(seriesId)).isNotEmpty() },
                    onDenied = { navController.navigateSingleTop(AppRoute.Home.route) },
                ) {
                    SeriesDetailRoute(
                    seriesId = seriesId,
                    returnFocusEpisodeId = episodeDetailReturnFocusId,
                    onReturnFocusConsumed = { episodeDetailReturnFocusId = null },
                    currentRoute = AppRoute.Series.route,
                    tabs = tabs,
                    onNavigate = navigateFromHeader,
                    onSync = launchSyncCatalog,
                    onSettings = { navigateFromHeader(AppRoute.Settings.route) },
                    onProfile = onProfileAction,
                    onNotifications = { navigateFromHeader(AppRoute.Notifications.route) },
                    onLicenseKey = { showLicensePurchaseQr = true },
                    showLicenseKey = activationState.shouldShowLicenseKey,
                    hasNewNotifications = hasNewNotifications,
                    notificationBadgeCount = notificationBadgeCount,
                    onWatchEpisode = { episodeId ->
                        episodeReturnFocusSeriesId = seriesId
                        episodeDetailReturnFocusId = episodeId
                        seriesPlayerEnterBounds = null
                        navController.navigate("episode_player/$episodeId")
                    },
                    )
                }
            }
        }
    }

    val playerRouteActive = currentRoute.startsWith("player/") ||
        currentRoute.startsWith("movie_player/") ||
        currentRoute.startsWith("episode_player/") ||
        currentRoute.startsWith("media_player/")
    val routeOwnsBackHandler = currentRoute in setOf(
        AppRoute.Settings.route,
        AppRoute.Profile.route,
        AppRoute.ManageProfiles.route,
        AppRoute.Notifications.route,
        AppRoute.Youtube.route,
    )
    BackHandler(enabled = !playerRouteActive && !routeOwnsBackHandler) {
        if (currentRoute == AppRoute.Settings.route) {
            navigateHomeWithHeaderFocus(HomeHeaderFocusTarget.Settings)
        } else if (currentRoute == AppRoute.Profile.route) {
            navigateHomeWithHeaderFocus(HomeHeaderFocusTarget.Profile)
        } else if (currentRoute != AppRoute.Home.route) {
            val popped = navController.popBackStack()
            if (!popped) {
                navController.navigateSingleTop(AppRoute.Home.route)
            }
        } else {
            showExitConfirmation = true
        }
    }
    BackHandler(enabled = openProfilePickerAfterHome) {
        // The Home staging frame is not interactive. Back is handled by the
        // global picker as soon as it becomes visible.
    }

    if (maskHomeBeforeProfileSelection) {
        Box(Modifier.fillMaxSize().background(SmartVisionColors.Background))
    }

    if (showProfilePicker) {
        val activationReadyRequestId = profileSelectionRequest
            ?.takeIf { request ->
                homeRouteIsActive && canCompleteProfileSelection(
                    request = request,
                    completedRequestId = profileActivationCompletedRequestId,
                    activeProfileId = activeProfileId,
                    appInForeground = appInForeground,
                )
            }
            ?.requestId
        BackHandler {
            if (profileSelectionRequest == null) {
                showExitConfirmation = true
            }
        }
        ProfilePickerScreen(
            profiles = configuredPickerProfiles,
            activeProfileId = activeProfileId,
            multiProfileAccess = multiProfileGate,
            selectionRequestId = profileSelectionRequest?.requestId,
            selectionLoadingProfileId = profileSelectionRequest?.profileId,
            activationReadyRequestId = activationReadyRequestId,
            homeProfileAvatarBounds = homeProfileAvatarBounds,
            onSelectionTransitionFinished = { requestId, profileId ->
                val request = profileSelectionRequest
                if (
                    request?.requestId == requestId &&
                    request.profileId == profileId &&
                    homeRouteIsActive &&
                    canCompleteProfileSelection(
                        request = request,
                        completedRequestId = profileActivationCompletedRequestId,
                        activeProfileId = activeProfileId,
                        appInForeground = appInForeground,
                    )
                ) {
                    profilePickerCompleted = true
                    profileSelectionRequest = null
                    profileActivationCompletedRequestId = null
                }
            },
            onLockedFeature = {
                if (multiProfileGate.shouldShowUpgradePrompt) {
                    showLicensePurchaseQr = true
                }
            },
            onVerifyPin = container.settingsRepository::verifyParentalPin,
            onFirstFrameReady = latestInitialSurfaceReady,
            initialContentVisible = initialSurfaceVisible,
            onSelectProfile = requestProfileSelection,
            onSaveProfile = { profile ->
                val wasActiveProfile =
                    profile.id.isNotBlank() &&
                        profile.id == container.accountManager.activeProfileId.value
                container.accountManager.upsertProfile(profile)
                if (wasActiveProfile) {
                    container.xtreamRepository.clearCaches()
                    scope.launch {
                        container.catalogRepository.clearCatalogForProfileSwitch()
                    }
                }
            },
        )
    }

    if (showExitConfirmation) {
        ExitConfirmationDialog(
            strings = strings,
            onDismiss = { showExitConfirmation = false },
            onChangeProfile = if (showProfilePicker) {
                null
            } else {
                {
                    showExitConfirmation = false
                    openProfilePickerFromHome()
                }
            },
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

private const val TAG_STARTUP = "SVStartup"

@Composable
private fun XtreamConnectionAlertDialog(
    state: XtreamConnectionState,
    onEditCredentials: () -> Unit,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
) {
    val retryFocusRequester = remember { FocusRequester() }
    val continueFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state.checking) {
        withFrameNanos { }
        delay(80)
        runCatching {
            if (state.checking) continueFocusRequester.requestFocus() else retryFocusRequester.requestFocus()
        }
    }

    TvDialogSurface(
        title = "Connexion Xtream indisponible",
        onDismiss = onContinue,
        width = 720.dp,
        tone = TvDialogTone.Destructive,
        icon = Icons.Default.Warning,
    ) {
        Text(
            text = "L'application n'arrive pas a se connecter au serveur Xtream associe a votre compte. Vos chaines, films et series sont temporairement indisponibles. Vous pouvez modifier vos identifiants, reessayer la connexion ou continuer sur l'application.",
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.Body,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.message.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.message,
                color = SmartVisionColors.Warning,
                style = SmartVisionType.Caption,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
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
                focusRequester = continueFocusRequester,
                contentPadding = PaddingValues(horizontal = 18.dp),
                modifier = Modifier.height(44.dp),
            )
        }
    }
}

@Composable
private fun ExitConfirmationDialog(
    strings: SmartVisionStrings,
    onDismiss: () -> Unit,
    onExit: () -> Unit,
    onChangeProfile: (() -> Unit)? = null,
) {
    TvConfirmationDialog(
        title = strings.exitAppTitle,
        message = strings.exitAppMessage,
        confirmText = strings.exitAppAction,
        cancelText = strings.cancel,
        tone = TvDialogTone.Warning,
        icon = Icons.Default.ExitToApp,
        onDismiss = onDismiss,
        onConfirm = onExit,
        secondaryText = strings.changeProfile.takeIf { onChangeProfile != null },
        onSecondary = onChangeProfile,
    )
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

private fun String.isAllowedFor(permissions: ProfilePermissions): Boolean {
    val baseRoute = substringBefore('/')
    return when (baseRoute) {
        AppRoute.Settings.route -> permissions.canAccessSettings
        AppRoute.Notifications.route -> permissions.canAccessNotifications
        AppRoute.Media.route -> permissions.canAccessMedia
        AppRoute.Profile.route -> permissions.canManageProfiles
        else -> true
    }
}

private fun CatalogContentCounts.hasAnyContent(): Boolean =
    live > 0 || movies > 0 || series > 0

private fun NavHostController.navigateFromTrendingItem(item: ContinueItem) {
    val parts = item.id.split(":", limit = 2)
    if (parts.size != 2) return
    val id = parts[1].toIntOrNull() ?: return
    when (parts[0]) {
        "movie" -> navigate("movie_detail/$id")
        "series" -> navigate("series_detail/$id")
    }
}

@Composable
private fun KidsContentRouteGuard(
    kidsMode: Boolean,
    contentKey: String,
    isAllowed: suspend () -> Boolean,
    onDenied: () -> Unit,
    content: @Composable () -> Unit,
) {
    var checked by remember(contentKey, kidsMode) { mutableStateOf(!kidsMode) }
    var allowed by remember(contentKey, kidsMode) { mutableStateOf(!kidsMode) }
    LaunchedEffect(contentKey, kidsMode) {
        if (kidsMode) {
            allowed = runCatching { isAllowed() }.getOrDefault(false)
            checked = true
            if (!allowed) onDenied()
        }
    }
    if (checked && allowed) {
        content()
    } else {
        PlaceholderRouteScreen("Kids profile", "Content unavailable for this profile.")
    }
}

private enum class AppRoute(val route: String) {
    Home("home"),
    Live("live_tv"),
    Movies("movies"),
    Series("series"),
    Media("media"),
    Youtube("youtube"),
    Settings("settings"),
    Profile("profile"),
    ManageProfiles("profile/manage"),
    Notifications("notifications"),
}

private fun headerTabs(strings: SmartVisionStrings) = listOf(
    HomeHeaderTab(strings.home, AppRoute.Home.route, R.drawable.ic_header_home),
    HomeHeaderTab(strings.liveTv, AppRoute.Live.route, R.drawable.ic_header_live_tv),
    HomeHeaderTab(strings.movies, AppRoute.Movies.route, R.drawable.ic_header_movies),
    HomeHeaderTab(strings.series, AppRoute.Series.route, R.drawable.ic_header_series),
    HomeHeaderTab(
        label = "YouTube",
        route = AppRoute.Youtube.route,
        iconRes = R.drawable.ic_header_youtube,
    ),
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
