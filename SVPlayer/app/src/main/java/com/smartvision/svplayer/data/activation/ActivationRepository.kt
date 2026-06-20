package com.smartvision.svplayer.data.activation

import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.smartvision.svplayer.BuildConfig
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ActivationRepository(
    private val api: ActivationApiService,
    private val dataStore: DataStore<Preferences>,
) {
    val localState: Flow<StoredActivationState> =
        dataStore.data.map { preferences ->
            StoredActivationState(
                deviceId = preferences[DEVICE_ID].orEmpty(),
                status = preferences[STATUS] ?: ActivationStatus.Pending.value,
                activated = preferences[ACTIVATED] ?: false,
                expiresAt = preferences[EXPIRES_AT],
                activationType = preferences[ACTIVATION_TYPE],
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

    suspend fun createSession(): ActivationSession {
        val deviceId = getOrCreateDeviceId()
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
        }

        return ActivationSession(
            deviceId = response.deviceId ?: deviceId,
            shortCode = shortCode,
            qrUrl = qrUrl,
            expiresAt = expiresAt,
            pollingIntervalSeconds = response.pollingInterval?.coerceAtLeast(1) ?: 5,
        )
    }

    suspend fun checkStatus(): RemoteActivationStatus {
        val deviceId = getOrCreateDeviceId()
        val response = api.getDeviceStatus(deviceId)

        if (!response.success) {
            throw ActivationException(response.error ?: "Statut activation indisponible.")
        }

        val status = ActivationStatus.fromValue(response.status)
        dataStore.edit { preferences ->
            preferences[DEVICE_ID] = deviceId
            preferences[STATUS] = status.value
            preferences[ACTIVATED] = response.activated
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
        }

        return RemoteActivationStatus(
            status = status,
            activated = response.activated,
            expiresAt = response.expiresAt,
            activationType = response.activationType,
            playlistConfigured = response.playlistConfigured,
        )
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
    }
}

data class StoredActivationState(
    val deviceId: String,
    val status: String,
    val activated: Boolean,
    val expiresAt: String?,
    val activationType: String?,
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
    val expiresAt: String?,
    val activationType: String?,
    val playlistConfigured: Boolean,
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
