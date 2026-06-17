package com.smartvision.svplayer.core.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.smartvision.svplayer.core.config.BuildConfigXtreamCredentialsProvider
import com.smartvision.svplayer.data.local.SVDatabase
import com.smartvision.svplayer.data.remote.XtreamApiService
import com.smartvision.svplayer.data.remote.XtreamUrlFactory
import com.smartvision.svplayer.data.repository.DefaultCatalogRepository
import com.smartvision.svplayer.data.repository.DefaultSettingsRepository
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.domain.repository.SettingsRepository
import com.smartvision.svplayer.domain.usecase.BuildPlaybackRequestUseCase
import com.smartvision.svplayer.domain.usecase.SavePlaybackProgressUseCase
import com.smartvision.svplayer.domain.usecase.SynchronizeCatalogUseCase
import com.smartvision.svplayer.domain.usecase.ToggleFavoriteUseCase
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val Context.settingsDataStore by preferencesDataStore(name = "svplayer_settings")

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val credentialsProvider = BuildConfigXtreamCredentialsProvider()
    private val database = SVDatabase.build(appContext)
    private val urlFactory = XtreamUrlFactory(credentialsProvider)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(credentialsProvider.current().retrofitBaseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(XtreamApiService::class.java)

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
}
