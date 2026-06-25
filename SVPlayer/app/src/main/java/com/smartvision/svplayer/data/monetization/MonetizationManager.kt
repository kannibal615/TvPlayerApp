package com.smartvision.svplayer.data.monetization

import android.util.Log
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.data.activation.ActivationRepository
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MonetizationManager(
    private val activationRepository: ActivationRepository,
    private val store: MonetizationStore,
    private val configProvider: AdConfigProvider,
    private val eventReporter: AdsEventReporter,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val mutex = Mutex()
    private val requestGate = AdRequestGate()
    private var lastKnownStatus: MonetizationStatus? = null
    private var pendingContentType: PlayerContentType? = null

    suspend fun synchronizeStatus(): MonetizationStatus? {
        val status = activationRepository.localState.first().monetizationStatus()
        if (status != null) {
            store.updateStatus(status)
            if (lastKnownStatus != status) {
                when (status) {
                    MonetizationStatus.FREE_WITH_ADS -> Log.i(TAG, "Passage en FREE_WITH_ADS")
                    MonetizationStatus.PREMIUM_ACTIVE -> Log.i(TAG, "Passage en Premium")
                    else -> Unit
                }
                lastKnownStatus = status
            }
            Log.i(TAG, "Statut monetisation actuel: $status")
        }
        return status
    }

    suspend fun maybeShowPlayerAdThenStartPlayback(
        contentType: PlayerContentType,
        onContinue: (PlayerAdPlan) -> Unit,
    ) {
        val status = synchronizeStatus()
        val config = configProvider.refresh()
        val now = clock.millis()
        val plan = mutex.withLock {
            requestGate.expireStale(now)
            val frequency = store.frequencySnapshot()
            val reason = AdEligibilityPolicy.refusalReason(
                status = status,
                contentType = contentType,
                config = config,
                frequency = frequency,
                nowMillis = now,
                runtimeConfigured = BuildConfig.ADS_RUNTIME_CONFIGURED || config.adTagUrl.isNotBlank(),
                requestAlreadyPending = requestGate.hasPending,
            )
            if (reason != null) {
                Log.i(TAG, "Pub refusee: $reason")
                PlayerAdPlan.StartDirectly(reason)
            } else {
                val requestId = UUID.randomUUID().toString()
                requestGate.reserve(requestId, now)
                pendingContentType = contentType
                Log.i(TAG, "Utilisateur FREE_WITH_ADS: pub autorisee pour $contentType")
                PlayerAdPlan.ShowPreRoll(
                    requestId = requestId,
                    adTagUrl = config.adTagUrl,
                )
            }
        }
        onContinue(plan)
    }

    suspend fun idleLivePreviewAdTagUrl(): String? {
        val status = synchronizeStatus()
        val config = configProvider.refresh()
        return config.adTagUrl.takeIf {
            status == MonetizationStatus.FREE_WITH_ADS &&
                config.adsEnabled &&
                config.adsOnlyInsidePlayer &&
                config.showAdBeforeLiveStream &&
                it.isNotBlank()
        }
    }

    suspend fun onAdStarted(requestId: String) {
        val contentType = mutex.withLock {
            if (!requestGate.markStarted(requestId)) return
            store.recordAdStarted()
            pendingContentType ?: PlayerContentType.LIVE_TV
        }
        eventReporter.reportStarted(contentType)
        Log.i(TAG, "Pub affichee")
    }

    suspend fun onIdleLivePreviewAdStarted() {
        eventReporter.reportStarted(PlayerContentType.LIVE_TV)
    }

    suspend fun onAdCompleted(requestId: String) {
        mutex.withLock {
            if (requestGate.release(requestId)) {
                pendingContentType = null
                Log.i(TAG, "Pub terminee")
                Log.i(TAG, "Video lancee apres pub")
            }
        }
    }

    suspend fun onAdFailed(requestId: String, reason: String) {
        mutex.withLock {
            if (requestGate.release(requestId)) {
                pendingContentType = null
                Log.w(TAG, "Pub echouee: $reason; lecture autorisee")
                Log.i(TAG, "Video lancee apres echec pub")
            }
        }
    }

    private companion object {
        const val TAG = "SmartVisionAds"
    }
}

internal class AdRequestGate(
    private val timeoutMillis: Long = 90_000L,
) {
    private var reservation: Reservation? = null
    val hasPending: Boolean get() = reservation != null

    fun reserve(requestId: String, nowMillis: Long): Boolean {
        if (reservation != null) return false
        reservation = Reservation(requestId, nowMillis)
        return true
    }

    fun markStarted(requestId: String): Boolean {
        val current = reservation ?: return false
        if (current.requestId != requestId || current.impressionRecorded) return false
        reservation = current.copy(impressionRecorded = true)
        return true
    }

    fun release(requestId: String): Boolean {
        if (reservation?.requestId != requestId) return false
        reservation = null
        return true
    }

    fun expireStale(nowMillis: Long) {
        val current = reservation ?: return
        if (nowMillis - current.createdAt >= timeoutMillis) {
            reservation = null
        }
    }

    private data class Reservation(
        val requestId: String,
        val createdAt: Long,
        val impressionRecorded: Boolean = false,
    )
}

object AdEligibilityPolicy {
    fun refusalReason(
        status: MonetizationStatus?,
        contentType: PlayerContentType,
        config: AdConfig,
        frequency: AdFrequencySnapshot,
        nowMillis: Long,
        runtimeConfigured: Boolean,
        requestAlreadyPending: Boolean,
    ): String? {
        if (!config.adsEnabled) return "pubs desactivees"
        if (!config.adsOnlyInsidePlayer) return "configuration player invalide"
        if (!runtimeConfigured || config.adTagUrl.isBlank()) return "regie publicitaire non configuree"
        when (status) {
            MonetizationStatus.PREMIUM_ACTIVE -> return "utilisateur Premium: pub ignoree"
            MonetizationStatus.TRIAL_ACTIVE -> return "essai actif: pub ignoree"
            MonetizationStatus.TRIAL_EXPIRED,
            MonetizationStatus.LICENSE_EXPIRED,
            null,
            -> return "statut utilisateur non eligible"
            MonetizationStatus.FREE_WITH_ADS -> Unit
        }
        val contextAllowed = when (contentType) {
            PlayerContentType.LIVE_TV -> config.showAdBeforeLiveStream
            PlayerContentType.MOVIE -> config.showAdBeforeMovie
            PlayerContentType.SERIES -> config.showAdBeforeSeriesEpisode
        }
        if (!contextAllowed) return "contexte non autorise"
        if (requestAlreadyPending) return "requete publicitaire deja en cours"
        if (frequency.adsSeenToday >= config.maxAdsPerDay) return "limite journaliere atteinte"
        val minimumInterval = config.minMinutesBetweenAds * 60_000L
        if (frequency.lastAdTimestamp > 0L &&
            nowMillis - frequency.lastAdTimestamp < minimumInterval
        ) {
            return "intervalle ${config.minMinutesBetweenAds} minutes non atteint"
        }
        return null
    }
}
