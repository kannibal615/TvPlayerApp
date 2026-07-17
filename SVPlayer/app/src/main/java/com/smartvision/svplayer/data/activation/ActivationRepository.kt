package com.smartvision.svplayer.data.activation

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.ProfileType
import com.smartvision.svplayer.core.config.WebPlaylistDelivery
import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.data.playlist.EpgRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

class ActivationRepository(
    private val appContext: Context,
    private val api: ActivationApiService,
    private val dataStore: DataStore<Preferences>,
    private val accountManager: XtreamAccountManager,
    private val epgRepository: EpgRepository,
) {
    val localState: Flow<StoredActivationState> =
        dataStore.data.map { preferences ->
            StoredActivationState(
                deviceId = preferences[DEVICE_ID].orEmpty(),
                publicDeviceCode = preferences[PUBLIC_DEVICE_CODE].orEmpty(),
                status = preferences[STATUS] ?: ActivationStatus.Pending.value,
                activated = preferences[ACTIVATED] ?: false,
                expiresAt = preferences[EXPIRES_AT],
                activationType = preferences[ACTIVATION_TYPE],
                licenseStatus = preferences[LICENSE_STATUS],
                trialStatus = preferences[TRIAL_STATUS],
                freeWithAdsStatus = preferences[FREE_WITH_ADS_STATUS],
                playlistConfigured = preferences[PLAYLIST_CONFIGURED] ?: false,
            )
        }

    suspend fun getOrCreateDeviceId(): String {
        val existing = dataStore.data.first()[DEVICE_ID]
        if (!existing.isNullOrBlank()) {
            return existing
        }

        val generated = UUID.randomUUID().toString()
        dataStore.edit { preferences ->
            preferences[DEVICE_ID] = generated
        }
        return generated
    }

    suspend fun getOrCreateLocalPublicCode(): String {
        val existing = dataStore.data.first()[PUBLIC_DEVICE_CODE]
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val code = DeviceIdentityProvider.create(appContext, BuildConfig.VERSION_NAME).localPublicCode
        dataStore.edit { preferences ->
            if (preferences[PUBLIC_DEVICE_CODE].isNullOrBlank()) {
                preferences[PUBLIC_DEVICE_CODE] = code
            }
        }
        return code
    }

    suspend fun registerDevice(): RemoteActivationStatus {
        val identity = DeviceIdentityProvider.create(appContext, BuildConfig.VERSION_NAME)
        dataStore.edit { preferences ->
            if (preferences[PUBLIC_DEVICE_CODE].isNullOrBlank()) {
                preferences[PUBLIC_DEVICE_CODE] = identity.localPublicCode
            }
        }
        val response = api.registerDevice(
            RegisterDeviceRequest(
                platform = "android_tv",
                androidIdHash = identity.androidIdHash,
                deviceFingerprintHash = identity.fingerprintHash,
                localPublicDeviceCode = identity.localPublicCode,
                appPackage = identity.appPackage,
                appVersion = identity.appVersion,
                deviceManufacturer = identity.manufacturer,
                deviceModel = identity.model,
            ),
        )

        if (!response.success) {
            throw ActivationException(response.error ?: "Enregistrement appareil refuse.")
        }

        val serverDeviceId = response.serverDeviceId ?: response.legacyDeviceId
        val publicDeviceCode = response.publicDeviceCode?.takeIf { it.isNotBlank() } ?: identity.localPublicCode
        if (serverDeviceId.isNullOrBlank()) {
            throw ActivationException("Reponse appareil incomplete.")
        }

        dataStore.edit { preferences ->
            preferences[DEVICE_ID] = serverDeviceId
            preferences[PUBLIC_DEVICE_CODE] = publicDeviceCode
            preferences[STATUS] = response.activationStatus ?: ActivationStatus.Pending.value
            preferences[ACTIVATED] = response.activationStatus == ActivationStatus.Active.value
            preferences[PLAYLIST_CONFIGURED] = response.playlistConfigured
            if (response.expiresAt.isNullOrBlank()) {
                preferences.remove(EXPIRES_AT)
            } else {
                preferences[EXPIRES_AT] = response.expiresAt
            }
            if (response.activationType.isNullOrBlank()) {
                preferences.remove(ACTIVATION_TYPE)
            } else {
                preferences[ACTIVATION_TYPE] = response.activationType
            }
            if (response.licenseStatus.isNullOrBlank()) preferences.remove(LICENSE_STATUS) else preferences[LICENSE_STATUS] = response.licenseStatus
            if (response.trialStatus.isNullOrBlank()) preferences.remove(TRIAL_STATUS) else preferences[TRIAL_STATUS] = response.trialStatus
            if (response.freeWithAdsStatus.isNullOrBlank()) {
                preferences.remove(FREE_WITH_ADS_STATUS)
            } else {
                preferences[FREE_WITH_ADS_STATUS] = response.freeWithAdsStatus
            }
            if (preferences[DEVICE_TOKEN].isNullOrBlank()) {
                response.deviceToken?.takeIf { it.isNotBlank() }?.let { preferences[DEVICE_TOKEN] = it }
            }
        }

        return RemoteActivationStatus(
            status = ActivationStatus.fromValue(response.activationStatus),
            activated = response.activationStatus == ActivationStatus.Active.value,
            deviceId = serverDeviceId,
            publicDeviceCode = publicDeviceCode,
            expiresAt = response.expiresAt,
            activationType = response.activationType,
            licenseStatus = response.licenseStatus,
            trialStatus = response.trialStatus,
            freeWithAdsStatus = response.freeWithAdsStatus,
            playlistConfigured = response.playlistConfigured,
        )
    }

    suspend fun createSession(): ActivationSession {
        val deviceId = ensureRegisteredDeviceId()
        val response = api.createActivationSession(
            CreateActivationSessionRequest(
                deviceId = deviceId,
                deviceName = deviceName(),
                appVersion = BuildConfig.VERSION_NAME,
            ),
        )

        if (!response.success) {
            throw ActivationException(response.error ?: "Session d activation refusee.")
        }

        val shortCode = response.shortCode.orEmpty()
        val qrUrl = response.qrUrl.orEmpty()
        val expiresAt = response.expiresAt.orEmpty()

        if (shortCode.isBlank() || qrUrl.isBlank() || expiresAt.isBlank()) {
            throw ActivationException("Reponse activation incomplete.")
        }

        dataStore.edit { preferences ->
            preferences[DEVICE_ID] = response.deviceId ?: deviceId
            preferences[STATUS] = ActivationStatus.Pending.value
            preferences[ACTIVATED] = false
            preferences[EXPIRES_AT] = expiresAt
            preferences.remove(ACTIVATION_TYPE)
            response.deviceToken?.takeIf { it.isNotBlank() }?.let { preferences[DEVICE_TOKEN] = it }
        }

        return ActivationSession(
            deviceId = response.deviceId ?: deviceId,
            shortCode = shortCode,
            qrUrl = qrUrl,
            expiresAt = expiresAt,
            pollingIntervalSeconds = response.pollingInterval?.coerceAtLeast(1) ?: 5,
        )
    }

    suspend fun createPlaylistSetupSession(): ActivationSession {
        val deviceId = ensureRegisteredDeviceId()
        val deviceToken = dataStore.data.first()[DEVICE_TOKEN].orEmpty()
        if (deviceToken.isBlank()) {
            throw ActivationException("Lien de configuration indisponible. Generez une nouvelle activation.")
        }

        val response = api.createPlaylistSetupSession(
            PlaylistSetupSessionRequest(
                deviceId = deviceId,
                deviceToken = deviceToken,
            ),
        )

        if (!response.success) {
            throw ActivationException(response.error ?: "Lien de configuration refuse.")
        }

        val shortCode = response.shortCode.orEmpty()
        val qrUrl = response.qrUrl.orEmpty()
        val expiresAt = response.expiresAt.orEmpty()
        if (shortCode.isBlank() || qrUrl.isBlank() || expiresAt.isBlank()) {
            throw ActivationException("Reponse configuration incomplete.")
        }

        return ActivationSession(
            deviceId = response.deviceId ?: deviceId,
            shortCode = shortCode,
            qrUrl = qrUrl,
            expiresAt = expiresAt,
            pollingIntervalSeconds = response.pollingInterval?.coerceAtLeast(1) ?: 5,
        )
    }

    suspend fun getDeviceToken(): String = dataStore.data.first()[DEVICE_TOKEN].orEmpty()

    suspend fun clearPlaylistConfig(profile: PlaylistProfile): Boolean {
        val preferences = dataStore.data.first()
        val deviceId = preferences[DEVICE_ID].orEmpty()
        val deviceToken = preferences[DEVICE_TOKEN].orEmpty()
        if (deviceId.isBlank() || deviceToken.isBlank()) {
            throw ActivationException("Session appareil indisponible.")
        }

        val response = api.clearPlaylistConfig(
            ClearPlaylistConfigRequest(
                deviceId = deviceId,
                deviceToken = deviceToken,
                host = profile.xtreamHost.takeIf { it.isNotBlank() },
                username = profile.xtreamUsername.takeIf { it.isNotBlank() },
                password = profile.xtreamPassword.takeIf { it.isNotBlank() },
                epgUrl = profile.epgUrl.takeIf { it.isNotBlank() },
                m3uUrl = profile.m3uUrl.takeIf { it.isNotBlank() },
            ),
        )

        if (!response.success) {
            throw ActivationException(response.error ?: "Suppression playlist refusee.")
        }
        if (response.cleared || !response.playlistConfigured) {
            dataStore.edit { updated ->
                updated[PLAYLIST_CONFIGURED] = response.playlistConfigured
            }
        }
        return response.cleared
    }

    suspend fun checkStatus(): RemoteActivationStatus {
        val deviceId = ensureRegisteredDeviceId()
        val deviceToken = dataStore.data.first()[DEVICE_TOKEN]
        val response = try {
            api.getDeviceStatus(deviceId, deviceToken)
        } catch (error: HttpException) {
            if (error.code() == 404) {
                return registerDevice()
            }
            throw error
        }

        val resolved = persistStatusResponse(response, "Statut activation indisponible.")

        response.playlistConfig?.let { importPlaylistConfig(it) }

        return resolved
    }

    suspend fun activateLicense(licenseCode: String): RemoteActivationStatus {
        val device = currentDeviceAccess()
        val response = api.activateLicense(
            ActivateLicenseRequest(
                deviceId = device.deviceId,
                publicDeviceCode = device.publicDeviceCode,
                licenseCode = licenseCode,
            ),
        )
        return persistStatusResponse(response, "Activation licence refusee.")
    }

    suspend fun startTrial(): RemoteActivationStatus {
        val device = currentDeviceAccess()
        val response = api.startTrial(
            DeviceAccessRequest(
                deviceId = device.deviceId,
                publicDeviceCode = device.publicDeviceCode,
            ),
        )
        return persistStatusResponse(response, "Essai gratuit refuse.")
    }

    suspend fun finalizeTrialAfterPlaylistConfigured(): RemoteActivationStatus {
        val device = currentDeviceAccess()
        val response = api.startTrial(
            DeviceAccessRequest(
                deviceId = device.deviceId,
                publicDeviceCode = device.publicDeviceCode,
                playlistConfigured = true,
            ),
        )
        return persistStatusResponse(response, "Finalisation de l essai refusee.")
    }

    suspend fun enableFreeWithAds(): RemoteActivationStatus {
        val device = currentDeviceAccess()
        val response = api.enableFreeWithAds(
            DeviceAccessRequest(
                deviceId = device.deviceId,
                publicDeviceCode = device.publicDeviceCode,
            ),
        )
        return persistStatusResponse(response, "Mode gratuit indisponible.")
    }

    private suspend fun persistStatusResponse(
        response: DeviceStatusResponse,
        defaultError: String,
    ): RemoteActivationStatus {
        if (!response.success) {
            val businessStatus = ActivationStatus.fromValue(response.status)
            if (businessStatus == ActivationStatus.Expired && response.trialStatus == "expired") {
                return persistResolvedStatus(response, businessStatus)
            }
            throw ActivationException(response.error ?: defaultError)
        }
        val status = ActivationStatus.fromValue(response.status)
        return persistResolvedStatus(response, status)
    }

    private suspend fun persistResolvedStatus(
        response: DeviceStatusResponse,
        status: ActivationStatus,
    ): RemoteActivationStatus {
        val deviceId = response.serverDeviceId ?: response.legacyDeviceId ?: ensureRegisteredDeviceId()
        val publicDeviceCode = response.publicDeviceCode ?: dataStore.data.first()[PUBLIC_DEVICE_CODE].orEmpty()
        dataStore.edit { preferences ->
            preferences[DEVICE_ID] = deviceId
            if (publicDeviceCode.isNotBlank()) preferences[PUBLIC_DEVICE_CODE] = publicDeviceCode
            preferences[STATUS] = status.value
            preferences[ACTIVATED] = response.activated
            preferences[PLAYLIST_CONFIGURED] = response.playlistConfigured
            if (response.expiresAt.isNullOrBlank()) preferences.remove(EXPIRES_AT) else preferences[EXPIRES_AT] = response.expiresAt
            if (response.activationType.isNullOrBlank()) preferences.remove(ACTIVATION_TYPE) else preferences[ACTIVATION_TYPE] = response.activationType
            if (response.licenseStatus.isNullOrBlank()) preferences.remove(LICENSE_STATUS) else preferences[LICENSE_STATUS] = response.licenseStatus
            if (response.trialStatus.isNullOrBlank()) preferences.remove(TRIAL_STATUS) else preferences[TRIAL_STATUS] = response.trialStatus
            if (response.freeWithAdsStatus.isNullOrBlank()) preferences.remove(FREE_WITH_ADS_STATUS) else preferences[FREE_WITH_ADS_STATUS] = response.freeWithAdsStatus
        }
        return RemoteActivationStatus(
            status = status,
            activated = response.activated,
            deviceId = deviceId,
            publicDeviceCode = publicDeviceCode,
            expiresAt = response.expiresAt,
            activationType = response.activationType,
            licenseStatus = response.licenseStatus,
            trialStatus = response.trialStatus,
            freeWithAdsStatus = response.freeWithAdsStatus,
            playlistConfigured = response.playlistConfigured,
        )
    }

    suspend fun publishProfileInventory(profiles: List<PlaylistProfile>) {
        val preferences = dataStore.data.first()
        val deviceId = preferences[DEVICE_ID].orEmpty()
        val deviceToken = preferences[DEVICE_TOKEN].orEmpty()
        if (deviceId.isBlank() || deviceToken.isBlank()) return
        val response = api.syncDeviceProfiles(
            DeviceProfilesRequest(
                deviceId = deviceId,
                deviceToken = deviceToken,
                profiles = profiles
                    .filter { it.type != ProfileType.KIDS }
                    .map { profile ->
                        DeviceProfileSummary(
                            profileId = profile.id,
                            name = profile.name,
                            type = profile.type.storageValue,
                        )
                    },
            ),
        )
        if (!response.success) throw ActivationException(response.error ?: "Publication des profils refusee.")
    }

    private suspend fun importPlaylistConfig(playlist: PlaylistConfigResponse) {
        val configId = playlist.configId.orEmpty()
        if (configId.isNotBlank() && dataStore.data.first()[LAST_PLAYLIST_CONFIG_ID] == configId) return
        val targetedDelivery = playlist.targetProfileIds.isNotEmpty() || !playlist.newProfileName.isNullOrBlank()
        if (targetedDelivery) {
            val appliedIds = accountManager.applyWebPlaylistDelivery(
                WebPlaylistDelivery(
                    targetProfileIds = playlist.targetProfileIds,
                    newProfileName = playlist.newProfileName.orEmpty(),
                    providedFields = playlist.providedFields.toSet(),
                    xtreamHost = playlist.host.orEmpty(),
                    xtreamUsername = playlist.username.orEmpty(),
                    xtreamPassword = playlist.password.orEmpty(),
                    m3uUrl = playlist.m3uUrl.orEmpty(),
                    epgUrl = playlist.epgUrl.orEmpty(),
                ),
            )
            appliedIds.mapNotNull { id ->
                accountManager.profiles.value.firstOrNull { it.id == id }
                    ?.let(accountManager::resolvedProfile)
                    ?.epgUrl
                    ?.takeIf { it.isNotBlank() }
            }.distinct().forEach { epgUrl ->
                epgRepository.synchronizeIfStale(epgUrl, EPG_IMPORT_REFRESH_MIN_AGE_MS)
            }
            if (configId.isNotBlank()) dataStore.edit { it[LAST_PLAYLIST_CONFIG_ID] = configId }
            return
        }
        val importedProfileId = accountManager.upsertWebPlaylistProfile(
            sourceHint = playlist.sourceHint(),
            xtreamHost = playlist.host.orEmpty(),
            xtreamUsername = playlist.username.orEmpty(),
            xtreamPassword = playlist.password.orEmpty(),
            m3uUrl = playlist.m3uUrl.orEmpty(),
            epgUrl = playlist.epgUrl.orEmpty(),
            epgProvided = "epg" in playlist.providedFields || playlist.providedFields.isEmpty(),
        )
        if (importedProfileId != null) {
            if (configId.isNotBlank()) dataStore.edit { it[LAST_PLAYLIST_CONFIG_ID] = configId }
            return
        }

        playlist.epgUrl?.trim()?.takeIf { it.isNotBlank() }?.let(accountManager::updateEpgUrl)
        playlist.m3uUrl?.trim()?.takeIf { it.isNotBlank() }?.let {
            accountManager.updateM3uUrl(it)
            accountManager.selectPlaylistSource(PlaylistSource.M3u)
        }
        if (configId.isNotBlank()) dataStore.edit { it[LAST_PLAYLIST_CONFIG_ID] = configId }
    }

    private suspend fun ensureRegisteredDeviceId(): String {
        val stored = dataStore.data.first()[DEVICE_ID]
        val publicCode = dataStore.data.first()[PUBLIC_DEVICE_CODE]
        return if (!stored.isNullOrBlank() && !publicCode.isNullOrBlank()) {
            stored
        } else {
            registerDevice().deviceId
        }
    }

    private suspend fun currentDeviceAccess(): DeviceAccess {
        val registered = registerDevice()
        if (registered.deviceId.isBlank() || registered.publicDeviceCode.isBlank()) {
            throw ActivationException("Identifiant appareil indisponible.")
        }
        return DeviceAccess(registered.deviceId, registered.publicDeviceCode)
    }

    private fun deviceName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val model = Build.MODEL.orEmpty()
        return listOf(manufacturer, model)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Android TV" }
    }

    private companion object {
        val DEVICE_ID = stringPreferencesKey("activation_device_id")
        val STATUS = stringPreferencesKey("activation_status")
        val ACTIVATED = booleanPreferencesKey("activation_activated")
        val EXPIRES_AT = stringPreferencesKey("activation_expires_at")
        val ACTIVATION_TYPE = stringPreferencesKey("activation_type")
        val LICENSE_STATUS = stringPreferencesKey("activation_license_status")
        val TRIAL_STATUS = stringPreferencesKey("activation_trial_status")
        val DEVICE_TOKEN = stringPreferencesKey("activation_device_token")
        val PUBLIC_DEVICE_CODE = stringPreferencesKey("activation_public_device_code")
        val FREE_WITH_ADS_STATUS = stringPreferencesKey("activation_free_with_ads_status")
        val PLAYLIST_CONFIGURED = booleanPreferencesKey("activation_playlist_configured")
        val LAST_PLAYLIST_CONFIG_ID = stringPreferencesKey("activation_last_playlist_config_id")
        const val EPG_IMPORT_REFRESH_MIN_AGE_MS = 60 * 60 * 1_000L
    }
}

private fun PlaylistConfigResponse.sourceHint(): PlaylistSource? =
    when (source?.trim()?.lowercase()) {
        PlaylistSource.Xtream.storageValue -> PlaylistSource.Xtream
        PlaylistSource.M3u.storageValue -> PlaylistSource.M3u
        else -> null
    }

data class StoredActivationState(
    val deviceId: String,
    val publicDeviceCode: String,
    val status: String,
    val activated: Boolean,
    val expiresAt: String?,
    val activationType: String?,
    val licenseStatus: String?,
    val trialStatus: String?,
    val freeWithAdsStatus: String?,
    val playlistConfigured: Boolean,
)

data class ActivationSession(
    val deviceId: String,
    val shortCode: String,
    val qrUrl: String,
    val expiresAt: String,
    val pollingIntervalSeconds: Int,
)

data class RemoteActivationStatus(
    val status: ActivationStatus,
    val activated: Boolean,
    val deviceId: String,
    val publicDeviceCode: String,
    val expiresAt: String?,
    val activationType: String?,
    val licenseStatus: String?,
    val trialStatus: String?,
    val freeWithAdsStatus: String?,
    val playlistConfigured: Boolean,
)

private data class DeviceAccess(
    val deviceId: String,
    val publicDeviceCode: String,
)

enum class ActivationStatus(val value: String) {
    Pending("pending"),
    Active("active"),
    Expired("expired"),
    Blocked("blocked"),
    Unknown("unknown");

    companion object {
        fun fromValue(value: String?): ActivationStatus =
            entries.firstOrNull { it.value == value } ?: Unknown
    }
}

class ActivationException(message: String) : RuntimeException(message)
