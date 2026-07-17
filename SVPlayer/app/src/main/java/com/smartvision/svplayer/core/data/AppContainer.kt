package com.smartvision.svplayer.core.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.data.anomaly.AnomalyApiService
import com.smartvision.svplayer.data.anomaly.AnomalyReporter
import com.smartvision.svplayer.data.activation.ActivationApiService
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.data.activation.StoredActivationState
import com.smartvision.svplayer.data.appconfig.AppConfigApiService
import com.smartvision.svplayer.data.appconfig.AppConfigRepository
import com.smartvision.svplayer.data.behavior.BehaviorApiService
import com.smartvision.svplayer.data.behavior.BehaviorReporter
import com.smartvision.svplayer.data.home.HomeSlidesApiService
import com.smartvision.svplayer.data.home.HomeContentRepository
import com.smartvision.svplayer.data.home.HomeSlidesRepository
import com.smartvision.svplayer.data.diagnostics.DeviceDiagnosticsApiService
import com.smartvision.svplayer.data.diagnostics.DeviceDiagnosticsReporter
import com.smartvision.svplayer.data.local.SVDatabase
import com.smartvision.svplayer.data.parental.RoomParentalCatalogRepository
import com.smartvision.svplayer.data.monetization.MonetizationManager
import com.smartvision.svplayer.data.monetization.MonetizationStore
import com.smartvision.svplayer.data.monetization.IdleVastAdLoader
import com.smartvision.svplayer.data.monetization.PrivacyConsentManager
import com.smartvision.svplayer.data.monetization.AdConfigApiService
import com.smartvision.svplayer.data.monetization.AdsEventReporter
import com.smartvision.svplayer.data.monetization.AdsEventsApiService
import com.smartvision.svplayer.data.monetization.RemoteAdConfigProvider
import com.smartvision.svplayer.data.notifications.NotificationsApiService
import com.smartvision.svplayer.data.notifications.NotificationsRepository
import com.smartvision.svplayer.data.playlist.EpgRepository
import com.smartvision.svplayer.data.playlist.M3uPlaylistClient
import com.smartvision.svplayer.data.private_media.PrivateMediaApiService
import com.smartvision.svplayer.data.private_media.PrivateMediaRepository
import com.smartvision.svplayer.data.remote.XtreamApiClient
import com.smartvision.svplayer.data.remote.XtreamApiService
import com.smartvision.svplayer.data.remote.XtreamUrlFactory
import com.smartvision.svplayer.data.remote.XTREAM_PROFILE_HOST_HEADER
import com.smartvision.svplayer.data.repository.DefaultCatalogRepository
import com.smartvision.svplayer.data.repository.DefaultSettingsRepository
import com.smartvision.svplayer.core.profile.ProfilePinManager
import com.smartvision.svplayer.data.xtream.XtreamCredentialsValidator
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.XtreamRepository
import com.smartvision.svplayer.data.tmdb.TmdbApiService
import com.smartvision.svplayer.data.tmdb.TmdbRepository
import com.smartvision.svplayer.data.update.AppUpdateApiService
import com.smartvision.svplayer.data.update.AppUpdateRepository
import com.smartvision.svplayer.data.xtream.XtreamConnectionManager
import com.smartvision.svplayer.data.youtube.YoutubeApiService
import com.smartvision.svplayer.data.youtube.YoutubeBehaviorApiService
import com.smartvision.svplayer.data.youtube.YoutubeBehaviorReporter
import com.smartvision.svplayer.data.youtube.YoutubeRepository
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.domain.repository.SettingsRepository
import com.smartvision.svplayer.domain.usecase.BuildPlaybackRequestUseCase
import com.smartvision.svplayer.domain.usecase.SavePlaybackProgressUseCase
import com.smartvision.svplayer.domain.usecase.SynchronizeCatalogUseCase
import com.smartvision.svplayer.domain.usecase.ToggleFavoriteUseCase
import com.smartvision.svplayer.media.MediaRepository
import com.smartvision.svplayer.media.MediaStorageManager
import com.smartvision.svplayer.media.transfer.MediaTransferServer
import com.smartvision.svplayer.recorder.RecorderController
import com.smartvision.svplayer.recorder.RecordingEngine
import com.smartvision.svplayer.recorder.RecordingRepository
import com.smartvision.svplayer.startup.StartupStateStore
import com.smartvision.svplayer.startup.StartupCatalogWorkKind
import com.smartvision.svplayer.startup.StartupCatalogWorkRequest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val Context.settingsDataStore by preferencesDataStore(name = "svplayer_settings")
private val Context.activationDataStore by preferencesDataStore(name = "smartvision_activation")
private val Context.monetizationDataStore by preferencesDataStore(name = "smartvision_monetization")
private val Context.appConfigDataStore by preferencesDataStore(name = "smartvision_app_config")

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val _startupCatalogWork = MutableStateFlow(StartupCatalogWorkRequest.None)
    private var startupSyncPolicyEvaluated = false
    @Volatile
    var startupActivationState: StoredActivationState? = null
        private set
    val startupCatalogWork: StateFlow<StartupCatalogWorkRequest> = _startupCatalogWork
    val accountManager = XtreamAccountManager(appContext)
    val profilePinManager = ProfilePinManager(appContext)
    private val credentialsProvider = accountManager
    private val database = SVDatabase.build(appContext)
    private val urlFactory = XtreamUrlFactory(credentialsProvider)

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            val targetHost = request.header(XTREAM_PROFILE_HOST_HEADER)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: credentialsProvider.current().normalizedHost
            val target = targetHost.toHttpUrlOrNull()
            if (target == null) {
                chain.proceed(request.newBuilder().removeHeader(XTREAM_PROFILE_HOST_HEADER).build())
            } else {
                val redirected = request.url.newBuilder()
                    .scheme(target.scheme)
                    .host(target.host)
                    .port(target.port)
                    .build()
                chain.proceed(
                    request.newBuilder()
                        .removeHeader(XTREAM_PROFILE_HOST_HEADER)
                        .url(redirected)
                        .build(),
                )
            }
        }
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://127.0.0.1/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(XtreamApiService::class.java)
    val xtreamCredentialsValidator = XtreamCredentialsValidator(api)
    private val xtreamApiClient = XtreamApiClient(api, credentialsProvider)

    private val activationOkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    val m3uPlaylistClient = M3uPlaylistClient(activationOkHttpClient)
    val epgRepository = EpgRepository(appContext, activationOkHttpClient)

    private val activationRetrofit = Retrofit.Builder()
        .baseUrl(activationBaseUrl())
        .client(activationOkHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val activationApi = activationRetrofit.create(ActivationApiService::class.java)
    private val appUpdateApi = activationRetrofit.create(AppUpdateApiService::class.java)
    private val homeSlidesApi = activationRetrofit.create(HomeSlidesApiService::class.java)
    private val notificationsApi = activationRetrofit.create(NotificationsApiService::class.java)
    private val appConfigApi = activationRetrofit.create(AppConfigApiService::class.java)
    private val privateMediaApi = activationRetrofit.create(PrivateMediaApiService::class.java)
    private val adConfigApi = activationRetrofit.create(AdConfigApiService::class.java)
    private val adsEventsApi = activationRetrofit.create(AdsEventsApiService::class.java)
    private val anomalyApi = activationRetrofit.create(AnomalyApiService::class.java)
    private val deviceDiagnosticsApi = activationRetrofit.create(DeviceDiagnosticsApiService::class.java)
    private val behaviorApi = activationRetrofit.create(BehaviorApiService::class.java)
    private val youtubeBehaviorApi = activationRetrofit.create(YoutubeBehaviorApiService::class.java)
    private val youtubeRetrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/youtube/v3/")
        .client(activationOkHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val youtubeApi = youtubeRetrofit.create(YoutubeApiService::class.java)
    private val tmdbReadAccessToken = BuildConfig.TMDB_READ_ACCESS_TOKEN.trim()
    private val tmdbOkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
                .header("Accept", "application/json")
            if (tmdbReadAccessToken.isNotBlank()) {
                builder.header("Authorization", "Bearer $tmdbReadAccessToken")
            }
            chain.proceed(builder.build())
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    private val tmdbRetrofit = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .client(tmdbOkHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val tmdbApi = tmdbRetrofit.create(TmdbApiService::class.java)

    val activationRepository: ActivationRepository = ActivationRepository(
        appContext = appContext,
        api = activationApi,
        dataStore = appContext.activationDataStore,
        accountManager = accountManager,
        epgRepository = epgRepository,
    )

    val adConfigProvider = RemoteAdConfigProvider(adConfigApi)
    private val adsEventReporter = AdsEventReporter(activationRepository, adsEventsApi)
    val monetizationManager = MonetizationManager(
        activationRepository = activationRepository,
        store = MonetizationStore(appContext.monetizationDataStore),
        configProvider = adConfigProvider,
        eventReporter = adsEventReporter,
    )
    val privacyConsentManager = PrivacyConsentManager(appContext)
    val idleVastAdLoader = IdleVastAdLoader(activationOkHttpClient)

    val appUpdateRepository: AppUpdateRepository = AppUpdateRepository(
        appContext = appContext,
        api = appUpdateApi,
        okHttpClient = activationOkHttpClient,
    )

    val homeSlidesRepository = HomeSlidesRepository(
        appContext = appContext,
        api = homeSlidesApi,
        okHttpClient = activationOkHttpClient,
    )
    val notificationsRepository = NotificationsRepository(
        activationRepository = activationRepository,
        api = notificationsApi,
    )
    val appConfigRepository = AppConfigRepository(
        api = appConfigApi,
        activationRepository = activationRepository,
        dataStore = appContext.appConfigDataStore,
    )
    val anomalyReporter = AnomalyReporter(
        appContext = appContext,
        activationRepository = activationRepository,
        api = anomalyApi,
    )
    val xtreamConnectionManager = XtreamConnectionManager(
        context = appContext,
        accountManager = accountManager,
        apiClient = xtreamApiClient,
        anomalyReporter = anomalyReporter,
    )
    private val startupStateStore = StartupStateStore(appContext)
    val deviceDiagnosticsReporter = DeviceDiagnosticsReporter(
        appContext = appContext,
        activationRepository = activationRepository,
        api = deviceDiagnosticsApi,
        stateStore = startupStateStore,
    )
    val behaviorReporter = BehaviorReporter(
        activationRepository = activationRepository,
        api = behaviorApi,
    )

    val xtreamRepository: XtreamRepository = XtreamRepository(
        apiClient = xtreamApiClient,
        urlFactory = urlFactory,
        accountManager = accountManager,
    )

    val userContentRepository: UserContentRepository = UserContentRepository(
        accountManager = accountManager,
        favoriteDao = database.favoriteDao(),
        progressDao = database.progressDao(),
        mediaDao = database.mediaDao(),
    )
    val mediaStorageManager = MediaStorageManager(appContext)
    val mediaRepository: MediaRepository = MediaRepository(
        dao = database.mediaCenterDao(),
        storageManager = mediaStorageManager,
    )
    val privateMediaRepository = PrivateMediaRepository(privateMediaApi)
    val mediaTransferServer = MediaTransferServer(mediaRepository)
    val recordingRepository = RecordingRepository(database.mediaCenterDao())
    private val recorderOkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
    val recordingEngine = RecordingEngine(recorderOkHttpClient)
    val recorderController = RecorderController(
        context = appContext,
        repository = recordingRepository,
    )

    private val youtubeBehaviorReporter = YoutubeBehaviorReporter(
        activationRepository = activationRepository,
        api = youtubeBehaviorApi,
        dao = database.youtubeDao(),
    )

    val youtubeRepository: YoutubeRepository = YoutubeRepository(
        api = youtubeApi,
        dao = database.youtubeDao(),
        favoriteDao = database.favoriteDao(),
        apiKey = BuildConfig.YOUTUBE_API_KEY,
        behaviorReporter = youtubeBehaviorReporter,
        accountManager = accountManager,
    )
    val tmdbRepository = TmdbRepository(
        api = tmdbApi,
        mediaDao = database.mediaDao(),
        accountManager = accountManager,
        readAccessToken = tmdbReadAccessToken,
    )

    val catalogRepository: CatalogRepository = DefaultCatalogRepository(
        database = database,
        api = api,
        accountManager = accountManager,
        urlFactory = urlFactory,
        m3uPlaylistClient = m3uPlaylistClient,
        epgRepository = epgRepository,
        categoryDao = database.categoryDao(),
        mediaDao = database.mediaDao(),
        profileDao = database.profileDao(),
        favoriteDao = database.favoriteDao(),
        progressDao = database.progressDao(),
        syncStateDao = database.syncStateDao(),
        youtubeDao = database.youtubeDao(),
        kidsFilterDao = database.kidsFilterDao(),
    )
    val settingsRepository: SettingsRepository =
        DefaultSettingsRepository(appContext, appContext.settingsDataStore, database, profilePinManager, accountManager)
    val parentalCatalogRepository = RoomParentalCatalogRepository(database)

    val homeContentRepository = HomeContentRepository(
        catalogRepository = catalogRepository,
        accountManager = accountManager,
        syncStateDao = database.syncStateDao(),
        mediaDao = database.mediaDao(),
        xtreamRepository = xtreamRepository,
        tmdbRepository = tmdbRepository,
        settingsRepository = settingsRepository,
        urlFactory = urlFactory,
    )
    val syncStateDao = database.syncStateDao()

    val synchronizeCatalog = SynchronizeCatalogUseCase(catalogRepository)
    val toggleFavorite = ToggleFavoriteUseCase(catalogRepository)
    val buildPlaybackRequest = BuildPlaybackRequestUseCase(catalogRepository)
    val savePlaybackProgress = SavePlaybackProgressUseCase(catalogRepository)

    fun requestStartupCatalogWork(kind: StartupCatalogWorkKind) {
        _startupCatalogWork.value = StartupCatalogWorkRequest(
            kind = kind,
            source = accountManager.activePlaylistSource.value,
            requestedAtMs = System.currentTimeMillis(),
        )
    }

    fun clearStartupCatalogWork(requestedAtMs: Long) {
        if (_startupCatalogWork.value.requestedAtMs == requestedAtMs) {
            _startupCatalogWork.value = StartupCatalogWorkRequest.None
        }
    }

    @Synchronized
    fun claimStartupSyncPolicyEvaluation(): Boolean {
        if (startupSyncPolicyEvaluated) return false
        startupSyncPolicyEvaluated = true
        return true
    }

    fun cacheStartupActivationState(state: StoredActivationState?) {
        startupActivationState = state
    }

    private fun activationBaseUrl(): String =
        BuildConfig.ACTIVATION_BASE_URL.ifBlank { "https://smartvisions.net/" }
}
