package com.smartvision.svplayer.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.smartvision.svplayer.data.local.SVDatabase
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val database: SVDatabase,
) : SettingsRepository {
    override val settings: Flow<PlayerSettings> =
        dataStore.data.map { preferences ->
            PlayerSettings(
                displaySize = preferences[DISPLAY_SIZE] ?: "Normal",
                animationsEnabled = preferences[ANIMATIONS] ?: true,
                videoRatio = preferences[VIDEO_RATIO] ?: "Fit",
                bufferMode = preferences[BUFFER_MODE] ?: "Standard",
                retryEnabled = preferences[RETRY] ?: true,
            )
        }

    override suspend fun setDisplaySize(value: String) {
        dataStore.edit { it[DISPLAY_SIZE] = value }
    }

    override suspend fun setAnimationsEnabled(value: Boolean) {
        dataStore.edit { it[ANIMATIONS] = value }
    }

    override suspend fun setVideoRatio(value: String) {
        dataStore.edit { it[VIDEO_RATIO] = value }
    }

    override suspend fun setBufferMode(value: String) {
        dataStore.edit { it[BUFFER_MODE] = value }
    }

    override suspend fun setRetryEnabled(value: Boolean) {
        dataStore.edit { it[RETRY] = value }
    }

    override suspend fun clearLocalData() {
        database.clearAllTables()
    }

    private companion object {
        val DISPLAY_SIZE = stringPreferencesKey("display_size")
        val ANIMATIONS = booleanPreferencesKey("animations_enabled")
        val VIDEO_RATIO = stringPreferencesKey("video_ratio")
        val BUFFER_MODE = stringPreferencesKey("buffer_mode")
        val RETRY = booleanPreferencesKey("retry_enabled")
    }
}
