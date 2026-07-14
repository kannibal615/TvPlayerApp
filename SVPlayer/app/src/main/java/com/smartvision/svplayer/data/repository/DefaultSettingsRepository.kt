package com.smartvision.svplayer.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.data.local.SVDatabase
import com.smartvision.svplayer.core.profile.ProfilePinManager
import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.domain.parental.ParentalKeywordPolicy
import com.smartvision.svplayer.domain.repository.SettingsRepository
import com.smartvision.svplayer.startup.StartupStateStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DefaultSettingsRepository(
    context: Context,
    private val dataStore: DataStore<Preferences>,
    private val database: SVDatabase,
    private val profilePinManager: ProfilePinManager,
    private val accountManager: XtreamAccountManager,
) : SettingsRepository {
    private val startupStateStore = StartupStateStore(context)
    private val migrationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        migrationScope.launch {
            val preferences = dataStore.data.first()
            val legacyPin = preferences[PARENTAL_PIN].orEmpty()
            if (profilePinManager.migrateLegacyPinIfNeeded(legacyPin)) {
                dataStore.edit { it[PARENTAL_PIN] = CONFIGURED_PIN_MARKER }
            }
            if (preferences[PARENTAL_KEYWORDS_JSON].isNullOrBlank() && !preferences[PARENTAL_KEYWORDS].isNullOrBlank()) {
                val migratedKeywords = ParentalKeywordPolicy.parseLegacy(preferences[PARENTAL_KEYWORDS].orEmpty())
                dataStore.edit { it[PARENTAL_KEYWORDS_JSON] = ParentalKeywordPolicy.serialize(migratedKeywords) }
            }
        }
    }

    override val settings: Flow<PlayerSettings> =
        combine(dataStore.data, accountManager.activeProfileId) { preferences, profileId ->
            val parentalKeywordValues = ParentalKeywordPolicy.parseJsonOrLegacy(
                json = preferences[PARENTAL_KEYWORDS_JSON],
                legacy = preferences[PARENTAL_KEYWORDS],
            )
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
                videoRatio = preferences[profileStringKey(VIDEO_RATIO_KEY, profileId)] ?: preferences[VIDEO_RATIO] ?: "Fit",
                bufferMode = preferences[profileStringKey(BUFFER_MODE_KEY, profileId)] ?: preferences[BUFFER_MODE] ?: "Standard",
                retryEnabled = preferences[profileBooleanKey(RETRY_KEY, profileId)] ?: preferences[RETRY] ?: true,
                parentalControlEnabled = preferences[PARENTAL_CONTROL_ENABLED] ?: false,
                parentalPin = if (profilePinManager.hasPin() || preferences[PARENTAL_PIN].orEmpty().isNotBlank()) {
                    CONFIGURED_PIN_MARKER
                } else {
                    ""
                },
                parentalKeywords = ParentalKeywordPolicy.legacyValue(parentalKeywordValues),
                parentalKeywordValues = parentalKeywordValues,
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
        dataStore.edit { it[profileStringKey(VIDEO_RATIO_KEY, accountManager.activeProfileId.value)] = value }
    }

    override suspend fun setBufferMode(value: String) {
        dataStore.edit { it[profileStringKey(BUFFER_MODE_KEY, accountManager.activeProfileId.value)] = value }
    }

    override suspend fun setRetryEnabled(value: Boolean) {
        dataStore.edit { it[profileBooleanKey(RETRY_KEY, accountManager.activeProfileId.value)] = value }
    }

    override suspend fun setParentalControlEnabled(value: Boolean) {
        dataStore.edit { it[PARENTAL_CONTROL_ENABLED] = value }
    }

    override suspend fun setParentalPin(value: String) {
        profilePinManager.setPin(value)
        dataStore.edit { it[PARENTAL_PIN] = CONFIGURED_PIN_MARKER }
    }

    override fun verifyParentalPin(value: String): Boolean = profilePinManager.verifyPin(value)

    override suspend fun setParentalKeywords(value: String) {
        replaceParentalKeywords(ParentalKeywordPolicy.parseLegacy(value))
    }

    override suspend fun replaceParentalKeywords(values: List<String>) {
        val normalized = ParentalKeywordPolicy.normalize(values)
        require(ParentalKeywordPolicy.fitsStorage(normalized)) { "Parental keyword list is too long" }
        dataStore.edit {
            it[PARENTAL_KEYWORDS] = ParentalKeywordPolicy.legacyValue(normalized)
            it[PARENTAL_KEYWORDS_JSON] = ParentalKeywordPolicy.serialize(normalized)
        }
    }

    override suspend fun resetParentalControl() {
        profilePinManager.clear()
        dataStore.edit {
            it[PARENTAL_CONTROL_ENABLED] = false
            it[PARENTAL_PIN] = ""
            it[PARENTAL_KEYWORDS] = ParentalKeywordPolicy.legacyValue(ParentalKeywordPolicy.DefaultKeywords)
            it[PARENTAL_KEYWORDS_JSON] = ParentalKeywordPolicy.serialize(ParentalKeywordPolicy.DefaultKeywords)
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
        val PARENTAL_KEYWORDS_JSON = stringPreferencesKey("parental_keywords_json")
        const val CONFIGURED_PIN_MARKER = "configured"
        const val VIDEO_RATIO_KEY = "video_ratio"
        const val BUFFER_MODE_KEY = "buffer_mode"
        const val RETRY_KEY = "retry_enabled"
    }

    private fun profileStringKey(base: String, profileId: String?) =
        stringPreferencesKey("${base}_${profileId.orEmpty().ifBlank { "default" }}")

    private fun profileBooleanKey(base: String, profileId: String?) =
        booleanPreferencesKey("${base}_${profileId.orEmpty().ifBlank { "default" }}")
}
