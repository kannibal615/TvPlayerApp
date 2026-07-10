package com.smartvision.svplayer.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.smartvision.svplayer.data.local.SVDatabase
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.domain.repository.SettingsRepository
import com.smartvision.svplayer.startup.StartupStateStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultSettingsRepository(
    context: Context,
    private val dataStore: DataStore<Preferences>,
    private val database: SVDatabase,
) : SettingsRepository {
    private val startupStateStore = StartupStateStore(context)

    override val settings: Flow<PlayerSettings> =
        dataStore.data.map { preferences ->
            PlayerSettings(
                displaySize = preferences[DISPLAY_SIZE] ?: "Normal",
                language = preferences[LANGUAGE] ?: "English",
                syncFrequency = preferences[SYNC_FREQUENCY] ?: "24h",
                autostartEnabled = preferences[AUTOSTART_ENABLED] ?: startupStateStore.isAutostartEnabled(),
                backgroundSyncEnabled = preferences[BACKGROUND_SYNC_ENABLED] ?: startupStateStore.isBackgroundSyncEnabled(),
                focusStyle = preferences[FOCUS_STYLE] ?: "Default",
                focusColor = preferences[FOCUS_COLOR] ?: "White",
                focusEffect = preferences[FOCUS_EFFECT] ?: "Frame",
                focusBackground = preferences[FOCUS_BACKGROUND] ?: "BlueTransparent",
                focusSelectedColor = preferences[FOCUS_SELECTED_COLOR] ?: "CyanNeon",
                focusActiveColor = preferences[FOCUS_ACTIVE_COLOR] ?: "ElectricBlue",
                focusParentColor = preferences[FOCUS_PARENT_COLOR] ?: "White",
                animationsEnabled = preferences[ANIMATIONS] ?: true,
                videoRatio = preferences[VIDEO_RATIO] ?: "Fit",
                bufferMode = preferences[BUFFER_MODE] ?: "Standard",
                retryEnabled = preferences[RETRY] ?: true,
                parentalControlEnabled = preferences[PARENTAL_CONTROL_ENABLED] ?: false,
                parentalPin = preferences[PARENTAL_PIN] ?: "",
                parentalKeywords = preferences[PARENTAL_KEYWORDS] ?: "adults; porn; xxx",
            )
        }

    override suspend fun setDisplaySize(value: String) {
        dataStore.edit { it[DISPLAY_SIZE] = value }
    }

    override suspend fun setLanguage(value: String) {
        dataStore.edit { it[LANGUAGE] = value }
    }

    override suspend fun setSyncFrequency(value: String) {
        dataStore.edit { it[SYNC_FREQUENCY] = value }
    }

    override suspend fun setAutostartEnabled(value: Boolean) {
        startupStateStore.setAutostartEnabled(value)
        dataStore.edit { it[AUTOSTART_ENABLED] = value }
    }

    override suspend fun setBackgroundSyncEnabled(value: Boolean) {
        startupStateStore.setBackgroundSyncEnabled(value)
        dataStore.edit { it[BACKGROUND_SYNC_ENABLED] = value }
    }

    override suspend fun setFocusStyle(value: String) {
        dataStore.edit { it[FOCUS_STYLE] = value }
    }

    override suspend fun setFocusColor(value: String) {
        dataStore.edit { it[FOCUS_COLOR] = value }
    }

    override suspend fun setFocusEffect(value: String) {
        dataStore.edit { it[FOCUS_EFFECT] = value }
    }

    override suspend fun setFocusBackground(value: String) {
        dataStore.edit { it[FOCUS_BACKGROUND] = value }
    }

    override suspend fun setFocusSelectedColor(value: String) {
        dataStore.edit { it[FOCUS_SELECTED_COLOR] = value }
    }

    override suspend fun setFocusActiveColor(value: String) {
        dataStore.edit { it[FOCUS_ACTIVE_COLOR] = value }
    }

    override suspend fun setFocusParentColor(value: String) {
        dataStore.edit { it[FOCUS_PARENT_COLOR] = value }
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

    override suspend fun setParentalControlEnabled(value: Boolean) {
        dataStore.edit { it[PARENTAL_CONTROL_ENABLED] = value }
    }

    override suspend fun setParentalPin(value: String) {
        dataStore.edit { it[PARENTAL_PIN] = value.filter(Char::isDigit).take(8) }
    }

    override suspend fun setParentalKeywords(value: String) {
        dataStore.edit { it[PARENTAL_KEYWORDS] = value.take(500) }
    }

    override suspend fun resetParentalControl() {
        dataStore.edit {
            it[PARENTAL_CONTROL_ENABLED] = false
            it[PARENTAL_PIN] = ""
            it[PARENTAL_KEYWORDS] = "adults; porn; xxx"
        }
    }

    override suspend fun clearLocalData() {
        database.clearAllTables()
    }

    private companion object {
        val DISPLAY_SIZE = stringPreferencesKey("display_size")
        val LANGUAGE = stringPreferencesKey("language")
        val SYNC_FREQUENCY = stringPreferencesKey("sync_frequency")
        val AUTOSTART_ENABLED = booleanPreferencesKey("autostart_enabled")
        val BACKGROUND_SYNC_ENABLED = booleanPreferencesKey("background_sync_enabled")
        val FOCUS_STYLE = stringPreferencesKey("focus_style")
        val FOCUS_COLOR = stringPreferencesKey("focus_color")
        val FOCUS_EFFECT = stringPreferencesKey("focus_effect")
        val FOCUS_BACKGROUND = stringPreferencesKey("focus_background")
        val FOCUS_SELECTED_COLOR = stringPreferencesKey("focus_selected_color")
        val FOCUS_ACTIVE_COLOR = stringPreferencesKey("focus_active_color")
        val FOCUS_PARENT_COLOR = stringPreferencesKey("focus_parent_color")
        val ANIMATIONS = booleanPreferencesKey("animations_enabled")
        val VIDEO_RATIO = stringPreferencesKey("video_ratio")
        val BUFFER_MODE = stringPreferencesKey("buffer_mode")
        val RETRY = booleanPreferencesKey("retry_enabled")
        val PARENTAL_CONTROL_ENABLED = booleanPreferencesKey("parental_control_enabled")
        val PARENTAL_PIN = stringPreferencesKey("parental_pin")
        val PARENTAL_KEYWORDS = stringPreferencesKey("parental_keywords")
    }
}
