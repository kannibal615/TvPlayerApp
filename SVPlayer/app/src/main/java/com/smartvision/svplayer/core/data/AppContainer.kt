package com.smartvision.svplayer.core.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.data.anomaly.AnomalyApiService
import com.smartvision.svplayer.data.anomaly.AnomalyReporter
import com.smartvision.svplayer.data.activation.ActivationApiService
import com.smartvision.svplayer.data.activation.ActivationRepository
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
import com.smartvision.svplayer.data.remote.XtreamApiClient
import com.smartvision.svplayer.data.remote.XtreamApiService
import com.smartvision.svplayer.data.remote.XtreamUrlFactory
import com.smartvision.svplayer.data.repository.DefaultCatalogRepository
import com.smartvision.svplayer.data.repository.DefaultSettingsRepository
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.XtreamRepository
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
    val startupCatalogWork: StateFlow<StartupCatalogWorkRequest> = _startupCatalogWork
    val accountManager = XtreamAccountManager(appContext)
    private val credentialsProvider = accountManager
    private val database = SVDatabase.build(appContext)
    private val urlFactory = XtreamUrlFactory(credentialsProvider)

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            val target = credentialsProvider.current().normalizedHost.toHttpUrlOrNull()
            if (target == null) {
                chain.proceed(request)
            } else {
                val redirected = request.url.newBuilder()
                    .scheme(target.scheme)
                    .host(target.host)
                    .port(target.port)
                    .build()
                chain.proceed(request.newBuilder().url(redirected).build())
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

    val activationRepository: ActivationRepository = ActivationRepository(
        appContext = appContext,
        api = activationApi,
        dataStore = appContext.activationDataStore,
        accountManager = accountManager,
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

    val homeSlidesRepository = HomeSlidesRepository(homeSlidesApi)
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
    )

    val userContentRepository: UserContentRepository = UserContentRepository(
        favoriteDao = database.favoriteDao(),
        progressDao = database.progressDao(),
        mediaDao = database.mediaDao(),
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
    )
    val homeContentRepository = HomeContentRepository(
        catalogRepository = catalogRepository,
        accountManager = accountManager,
        syncStateDao = database.syncStateDao(),
    )
    val syncStateDao = database.syncStateDao()

    val settingsRepository: SettingsRepository =
        DefaultSettingsRepository(appContext, appContext.settingsDataStore, database)

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

    private fun activationBaseUrl(): String =
        BuildConfig.ACTIVATION_BASE_URL.ifBlank { "https://smartvisions.net/" }
}
