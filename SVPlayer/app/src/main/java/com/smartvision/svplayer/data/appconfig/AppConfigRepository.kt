package com.smartvision.svplayer.data.appconfig

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.data.monetization.MonetizationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AppConfigRepository(
    private val api: AppConfigApiService,
    private val activationRepository: ActivationRepository,
    private val dataStore: DataStore<Preferences>,
) {
    val acceptedConsentVersion: Flow<String> =
        dataStore.data.map { it[ACCEPTED_CONSENT_VERSION].orEmpty() }

    suspend fun loadConfig(): AppRuntimeConfig {
        val access = currentAccess()
        val response = runCatching {
            api.getAppConfig(
                deviceId = access.deviceId,
                publicDeviceCode = access.publicDeviceCode,
            )
        }.getOrNull()
        if (response?.success != true) {
            return defaultRuntimeConfig()
        }
        val consent = response.consent?.toDomain() ?: defaultConsentConfig()
        val features = response.features
            .mapNotNull { it.toDomainOrNull() }
            .ifEmpty { defaultFeatureAccess() }
        return AppRuntimeConfig(
            consent = consent,
            features = features,
            trending = response.trending?.toDomain() ?: defaultTrendingConfig(),
            acceptedConsentVersion = response.acceptedConsentVersion,
        )
    }

    suspend fun acceptConsent(version: String) {
        val access = currentAccess()
        runCatching {
            api.acceptConsent(
                AcceptConsentRequest(
                    deviceId = access.deviceId,
                    publicDeviceCode = access.publicDeviceCode,
                    consentVersion = version,
                    appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                )
            )
        }
        dataStore.edit { it[ACCEPTED_CONSENT_VERSION] = version }
    }

    fun isFeatureAllowed(
        config: AppRuntimeConfig,
        featureKey: String,
        status: MonetizationStatus?,
    ): Boolean {
        val feature = config.features.firstOrNull { it.key == featureKey }
            ?: defaultFeatureAccess().firstOrNull { it.key == featureKey }
            ?: return true
        return when (status) {
            MonetizationStatus.PREMIUM_ACTIVE -> feature.premium
            MonetizationStatus.TRIAL_ACTIVE -> feature.trial
            MonetizationStatus.FREE_WITH_ADS -> feature.freeAds
            MonetizationStatus.TRIAL_EXPIRED,
            MonetizationStatus.LICENSE_EXPIRED,
            null -> feature.freeAds
        }
    }

    private companion object {
        val ACCEPTED_CONSENT_VERSION = stringPreferencesKey("accepted_consent_version_v2")
    }

    private suspend fun currentAccess(): AppConfigDeviceAccess {
        val createdDeviceId = activationRepository.getOrCreateDeviceId()
        val state = activationRepository.localState.first()
        return AppConfigDeviceAccess(
            deviceId = state.deviceId.ifBlank { createdDeviceId },
            publicDeviceCode = state.publicDeviceCode,
        )
    }
}

data class AppRuntimeConfig(
    val consent: ConsentConfig = defaultConsentConfig(),
    val features: List<FeatureAccess> = defaultFeatureAccess(),
    val trending: TrendingConfig = defaultTrendingConfig(),
    val acceptedConsentVersion: String? = null,
)

data class ConsentConfig(
    val version: String,
    val title: String,
    val body: String,
    val variables: Map<String, String>,
)

data class FeatureAccess(
    val key: String,
    val label: String,
    val premium: Boolean,
    val trial: Boolean,
    val freeAds: Boolean,
)

data class TrendingConfig(
    val requireLandscapeImage: Boolean,
    val excludeAdult: Boolean,
    val useRatingFilter: Boolean,
    val minimumRating: Float,
    val candidateLimit: Int,
    val sectionLimit: Int,
)

private data class AppConfigDeviceAccess(
    val deviceId: String,
    val publicDeviceCode: String,
)

private fun RemoteConsentConfig.toDomain(): ConsentConfig =
    ConsentConfig(
        version = version?.takeIf { it.isNotBlank() } ?: "2026-06-28",
        title = title?.takeIf { it.isNotBlank() } ?: "Privacy Policy and Terms of Use",
        body = body?.takeIf { it.isNotBlank() } ?: defaultConsentConfig().body,
        variables = variables,
    )

private fun RemoteFeatureAccess.toDomainOrNull(): FeatureAccess? {
    val safeKey = key.trim().takeIf { it.isNotBlank() } ?: return null
    return FeatureAccess(
        key = safeKey,
        label = label.trim().ifBlank { safeKey },
        premium = premium,
        trial = trial,
        freeAds = freeAds,
    )
}

private fun RemoteTrendingConfig.toDomain(): TrendingConfig {
    val defaults = defaultTrendingConfig()
    return TrendingConfig(
        requireLandscapeImage = requireLandscapeImage ?: defaults.requireLandscapeImage,
        excludeAdult = excludeAdult ?: defaults.excludeAdult,
        useRatingFilter = useRatingFilter ?: defaults.useRatingFilter,
        minimumRating = minimumRating?.coerceIn(0f, 10f) ?: defaults.minimumRating,
        candidateLimit = candidateLimit?.coerceIn(10, 100) ?: defaults.candidateLimit,
        sectionLimit = sectionLimit?.coerceIn(1, 20) ?: defaults.sectionLimit,
    )
}

private fun defaultRuntimeConfig(): AppRuntimeConfig =
    AppRuntimeConfig(defaultConsentConfig(), defaultFeatureAccess(), defaultTrendingConfig())

fun defaultTrendingConfig(): TrendingConfig =
    TrendingConfig(
        requireLandscapeImage = true,
        excludeAdult = true,
        useRatingFilter = false,
        minimumRating = 9.0f,
        candidateLimit = 50,
        sectionLimit = 10,
    )

private fun defaultConsentConfig(): ConsentConfig =
    ConsentConfig(
        version = "2026-06-28",
        title = "Privacy Policy and Terms of Use",
        body = """
            **SmartVision Player** is a commercial IPTV media player developed and operated by **ONETECCOM**.

            **SmartVision Player does not provide IPTV content, TV channels, movies, series, IPTV subscriptions or playlists.**

            Users are solely responsible for the content, links, playlists and Xtream credentials they add. A SmartVision Player licence gives access only to the application or specific playback features.

            For questions about licences, payments or support, contact **support@smartvisions.net**.
        """.trimIndent(),
        variables = mapOf(
            "app_name" to "SmartVision Player",
            "company" to "ONETECCOM",
            "site" to "smartvisions.net",
            "support_email" to "support@smartvisions.net",
        ),
    )

private fun defaultFeatureAccess(): List<FeatureAccess> =
    listOf(
        FeatureAccess("youtube", "YouTube", premium = true, trial = true, freeAds = false),
        FeatureAccess("parental_control", "Parental control", premium = true, trial = true, freeAds = false),
        FeatureAccess("replay", "Replay", premium = true, trial = true, freeAds = false),
        FeatureAccess("advanced_favorites", "Favoris avances", premium = true, trial = true, freeAds = false),
        FeatureAccess("multi_screen", "Multi-ecran", premium = true, trial = false, freeAds = false),
        FeatureAccess("local_cache", "Telechargement ou cache local", premium = true, trial = false, freeAds = false),
        FeatureAccess("recorder", "Recorder", premium = true, trial = true, freeAds = false),
        FeatureAccess("media_center", "Media Center menu", premium = true, trial = true, freeAds = false),
        FeatureAccess("media_file_management", "Media file management", premium = true, trial = true, freeAds = false),
        FeatureAccess("media_phone_transfer", "Phone transfer", premium = true, trial = true, freeAds = false),
        FeatureAccess("multi_profile", "Multi-profile", premium = true, trial = true, freeAds = false),
    )
