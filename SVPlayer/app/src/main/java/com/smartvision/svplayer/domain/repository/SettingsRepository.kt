package com.smartvision.svplayer.domain.repository

import com.smartvision.svplayer.domain.model.PlayerSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<PlayerSettings>

    suspend fun setDisplaySize(value: String)
    suspend fun setLanguage(value: String)
    suspend fun setSyncFrequency(value: String)
    suspend fun setAutostartEnabled(value: Boolean)
    suspend fun setBackgroundSyncEnabled(value: Boolean)
    suspend fun setFocusStyle(value: String)
    suspend fun setFocusColor(value: String)
    suspend fun setFocusEffect(value: String)
    suspend fun setFocusBackground(value: String)
    suspend fun setAnimationsEnabled(value: Boolean)
    suspend fun setVideoRatio(value: String)
    suspend fun setBufferMode(value: String)
    suspend fun setRetryEnabled(value: Boolean)
    suspend fun setParentalControlEnabled(value: Boolean)
    suspend fun setParentalPin(value: String)
    suspend fun setParentalKeywords(value: String)
    suspend fun resetParentalControl()
    suspend fun clearLocalData()
}
