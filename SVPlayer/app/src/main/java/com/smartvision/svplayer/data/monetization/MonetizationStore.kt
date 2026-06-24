package com.smartvision.svplayer.data.monetization

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first

class MonetizationStore(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock = Clock.systemUTC(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    suspend fun updateStatus(status: MonetizationStatus) {
        val now = clock.millis()
        dataStore.edit { preferences ->
            val previous = preferences[USER_MONETIZATION_STATUS]
            preferences[USER_MONETIZATION_STATUS] = status.name
            if (status == MonetizationStatus.FREE_WITH_ADS &&
                previous != MonetizationStatus.FREE_WITH_ADS.name &&
                preferences[FREE_WITH_ADS_ACCEPTED_AT] == null
            ) {
                preferences[FREE_WITH_ADS_ACCEPTED_AT] = now
            }
        }
    }

    suspend fun frequencySnapshot(): AdFrequencySnapshot {
        resetDailyCounterIfNeeded()
        val preferences = dataStore.data.first()
        return AdFrequencySnapshot(
            lastAdTimestamp = preferences[LAST_AD_TIMESTAMP] ?: 0L,
            adsSeenToday = preferences[ADS_SEEN_TODAY] ?: 0,
            counterDate = preferences[ADS_COUNTER_DATE].orEmpty(),
        )
    }

    suspend fun recordAdStarted() {
        val now = clock.millis()
        val today = today()
        dataStore.edit { preferences ->
            if (preferences[ADS_COUNTER_DATE] != today) {
                preferences[ADS_COUNTER_DATE] = today
                preferences[ADS_SEEN_TODAY] = 0
            }
            preferences[LAST_AD_TIMESTAMP] = now
            preferences[ADS_SEEN_TODAY] = (preferences[ADS_SEEN_TODAY] ?: 0) + 1
        }
    }

    private suspend fun resetDailyCounterIfNeeded() {
        val today = today()
        dataStore.edit { preferences ->
            if (preferences[ADS_COUNTER_DATE] != today) {
                preferences[ADS_COUNTER_DATE] = today
                preferences[ADS_SEEN_TODAY] = 0
            }
        }
    }

    private fun today(): String =
        LocalDate.now(clock.withZone(zoneId)).toString()

    private companion object {
        val USER_MONETIZATION_STATUS = stringPreferencesKey("user_monetization_status")
        val FREE_WITH_ADS_ACCEPTED_AT = longPreferencesKey("free_with_ads_accepted_at")
        val LAST_AD_TIMESTAMP = longPreferencesKey("last_ad_timestamp")
        val ADS_SEEN_TODAY = intPreferencesKey("ads_seen_today")
        val ADS_COUNTER_DATE = stringPreferencesKey("ads_counter_date")
    }
}
