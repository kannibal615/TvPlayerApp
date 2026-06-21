package com.smartvision.svplayer.core.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.data.activation.ActivationApiService
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.data.local.SVDatabase
import com.smartvision.svplayer.data.remote.XtreamApiClient
import com.smartvision.svplayer.data.remote.XtreamApiService
import com.smartvision.svplayer.data.remote.XtreamUrlFactory
import com.smartvision.svplayer.data.repository.DefaultCatalogRepository
import com.smartvision.svplayer.data.repository.DefaultSettingsRepository
import com.smartvision.svplayer.data.repository.UserContentRepository
import com.smartvision.svplayer.data.repository.XtreamRepository
import com.smartvision.svplayer.data.update.AppUpdateApiService
import com.smartvision.svplayer.data.update.AppUpdateRepository
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.domain.repository.SettingsRepository
import com.smartvision.svplayer.domain.usecase.BuildPlaybackRequestUseCase
import com.smartvision.svplayer.domain.usecase.SavePlaybackProgressUseCase
import com.smartvision.svplayer.domain.usecase.SynchronizeCatalogUseCase
import com.smartvision.svplayer.domain.usecase.ToggleFavoriteUseCase
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val Context.settingsDataStore by preferencesDataStore(name = "svplayer_settings")
private val Context.activationDataStore by preferencesDataStore(name = "smartvision_activation")

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
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

    private val activationRetrofit = Retrofit.Builder()
        .baseUrl(activationBaseUrl())
        .client(activationOkHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val activationApi = activationRetrofit.create(ActivationApiService::class.java)
    private val appUpdateApi = activationRetrofit.create(AppUpdateApiService::class.java)

    val activationRepository: ActivationRepository = ActivationRepository(
        api = activationApi,
        dataStore = appContext.activationDataStore,
        accountManager = accountManager,
    )

    val appUpdateRepository: AppUpdateRepository = AppUpdateRepository(
        appContext = appContext,
        api = appUpdateApi,
        okHttpClient = activationOkHttpClient,
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

    val catalogRepository: CatalogRepository = DefaultCatalogRepository(
        api = api,
        credentialsProvider = credentialsProvider,
        urlFactory = urlFactory,
        categoryDao = database.categoryDao(),
        mediaDao = database.mediaDao(),
        profileDao = database.profileDao(),
        favoriteDao = database.favoriteDao(),
        progressDao = database.progressDao(),
        syncStateDao = database.syncStateDao(),
    )

    val settingsRepository: SettingsRepository =
        DefaultSettingsRepository(appContext.settingsDataStore, database)

    val synchronizeCatalog = SynchronizeCatalogUseCase(catalogRepository)
    val toggleFavorite = ToggleFavoriteUseCase(catalogRepository)
    val buildPlaybackRequest = BuildPlaybackRequestUseCase(catalogRepository)
    val savePlaybackProgress = SavePlaybackProgressUseCase(catalogRepository)

    private fun activationBaseUrl(): String =
        BuildConfig.ACTIVATION_BASE_URL.ifBlank { "https://app.smartvisions.net/" }
}
