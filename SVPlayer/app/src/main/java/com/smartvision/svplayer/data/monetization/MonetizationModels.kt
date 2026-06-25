package com.smartvision.svplayer.data.monetization

import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.data.activation.RemoteActivationStatus
import com.smartvision.svplayer.data.activation.StoredActivationState

enum class MonetizationStatus {
    PREMIUM_ACTIVE,
    TRIAL_ACTIVE,
    TRIAL_EXPIRED,
    LICENSE_EXPIRED,
    FREE_WITH_ADS,
}

enum class PlayerContentType {
    LIVE_TV,
    MOVIE,
    SERIES,
}

sealed interface PlayerAdPlan {
    data class StartDirectly(val reason: String) : PlayerAdPlan

    data class ShowPreRoll(
        val requestId: String,
        val adTagUrl: String,
    ) : PlayerAdPlan
}

data class AdFrequencySnapshot(
    val lastAdTimestamp: Long = 0L,
    val adsSeenToday: Int = 0,
    val counterDate: String = "",
)

fun StoredActivationState.monetizationStatus(): MonetizationStatus? =
    resolveMonetizationStatus(
        activationType = activationType,
        licenseStatus = licenseStatus,
        trialStatus = trialStatus,
        freeWithAdsStatus = freeWithAdsStatus,
    )

fun RemoteActivationStatus.monetizationStatus(): MonetizationStatus? =
    resolveMonetizationStatus(
        activationType = activationType,
        licenseStatus = licenseStatus,
        trialStatus = trialStatus,
        freeWithAdsStatus = freeWithAdsStatus,
    )

fun resolveMonetizationStatus(
    activationType: String?,
    licenseStatus: String?,
    trialStatus: String?,
    freeWithAdsStatus: String?,
    debugOverride: MonetizationStatus? = debugMonetizationStatus(),
): MonetizationStatus? {
    debugOverride?.let { return it }
    return when {
        freeWithAdsStatus == "active" ||
            activationType == "free_ads" -> MonetizationStatus.FREE_WITH_ADS

        licenseStatus == "active" ||
            activationType == "smartvision_code" ||
            activationType == "own_xtream" -> MonetizationStatus.PREMIUM_ACTIVE

        trialStatus == "active" ||
            trialStatus == "pending_xtream" ||
            activationType == "trial_demo" ||
            activationType == "trial_pending_xtream" -> MonetizationStatus.TRIAL_ACTIVE

        licenseStatus == "expired" -> MonetizationStatus.LICENSE_EXPIRED
        trialStatus == "expired" -> MonetizationStatus.TRIAL_EXPIRED
        else -> null
    }
}

private fun debugMonetizationStatus(): MonetizationStatus? {
    if (!BuildConfig.DEBUG) return null
    return runCatching {
        MonetizationStatus.valueOf(BuildConfig.DEBUG_MONETIZATION_STATUS.trim().uppercase())
    }.getOrNull()
}
