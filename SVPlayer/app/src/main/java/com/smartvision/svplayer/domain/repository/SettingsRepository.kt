package com.smartvision.svplayer.domain.repository

import com.smartvision.svplayer.domain.model.PlayerSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<PlayerSettings>

    suspend fun setDisplaySize(value: String)
    suspend fun setLanguage(value: String)
    suspend fun setSyncFrequency(value: String)
    suspend fun setAnimationsEnabled(value: Boolean)
    suspend fun setVideoRatio(value: String)
    suspend fun setBufferMode(value: String)
    suspend fun setRetryEnabled(value: Boolean)
    suspend fun setParentalControlEnabled(value: Boolean)
    suspend fun setParentalPin(value: String)
    suspend fun setParentalKeywords(value: String)
    suspend fun clearLocalData()
}
