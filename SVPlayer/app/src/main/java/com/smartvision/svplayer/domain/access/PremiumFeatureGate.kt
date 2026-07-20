package com.smartvision.svplayer.domain.access

import com.smartvision.svplayer.data.appconfig.AppRuntimeConfig
import com.smartvision.svplayer.data.appconfig.FeatureAccess
import com.smartvision.svplayer.data.monetization.MonetizationStatus

enum class PremiumFeature(
    val key: String,
    val defaultLabel: String,
    val defaultPremium: Boolean = true,
    val defaultTrial: Boolean = true,
    val defaultFreeAds: Boolean = false,
) {
    RECORDER("recorder", "Recorder"),
    MEDIA_CENTER("media_center", "Media Center"),
    MEDIA_FILE_MANAGEMENT("media_file_management", "Media file management"),
    MEDIA_PHONE_TRANSFER("media_phone_transfer", "Phone transfer"),
    MULTI_PROFILE("multi_profile", "Multi-profile"),
    YOUTUBE("youtube", "YouTube"),
    PARENTAL_CONTROL("parental_control", "Parental control"),
    REPLAY("replay", "Replay"),
    ADVANCED_FAVORITES("advanced_favorites", "Advanced favorites"),
    MULTI_SCREEN("multi_screen", "Multi-screen", defaultTrial = false),
    LOCAL_CACHE("local_cache", "Download or local cache", defaultTrial = false),
}

enum class PremiumFeatureGateState {
    Allowed,
    LockedPremiumVisible,
    BlockedExpired,
    SourceUnsupported,
    ConfigDisabled,
}

enum class PremiumFeatureGateReason {
    PremiumRequired,
    TrialUpgradeRequired,
    LicenseExpired,
    TrialExpired,
    SourceUnsupported,
    DisabledByConfig,
}

data class PremiumFeatureGateResult(
    val feature: PremiumFeature,
    val state: PremiumFeatureGateState,
    val reason: PremiumFeatureGateReason? = null,
) {
    val allowed: Boolean
        get() = state == PremiumFeatureGateState.Allowed

    val locked: Boolean
        get() = !allowed

    val showPremiumCrown: Boolean
        get() = state == PremiumFeatureGateState.LockedPremiumVisible

    val showDisabledControl: Boolean
        get() = state != PremiumFeatureGateState.ConfigDisabled

    val shouldShowUpgradePrompt: Boolean
        get() = reason == PremiumFeatureGateReason.PremiumRequired ||
            reason == PremiumFeatureGateReason.TrialUpgradeRequired ||
            reason == PremiumFeatureGateReason.LicenseExpired ||
            reason == PremiumFeatureGateReason.TrialExpired
}

object PremiumFeatureGate {
    fun evaluate(
        config: AppRuntimeConfig,
        feature: PremiumFeature,
        status: MonetizationStatus?,
        sourceSupported: Boolean = true,
    ): PremiumFeatureGateResult {
        if (!sourceSupported) {
            return PremiumFeatureGateResult(
                feature = feature,
                state = PremiumFeatureGateState.SourceUnsupported,
                reason = PremiumFeatureGateReason.SourceUnsupported,
            )
        }

        return evaluate(
            access = config.featureAccessFor(feature),
            feature = feature,
            status = status,
        )
    }

    fun evaluate(
        access: FeatureAccess,
        feature: PremiumFeature,
        status: MonetizationStatus?,
    ): PremiumFeatureGateResult =
        when (status) {
            MonetizationStatus.PREMIUM_ACTIVE -> {
                if (access.premium) allowed(feature) else disabledByConfig(feature)
            }

            MonetizationStatus.TRIAL_ACTIVE -> {
                when {
                    access.trial -> allowed(feature)
                    access.premium -> locked(feature, PremiumFeatureGateReason.TrialUpgradeRequired)
                    else -> disabledByConfig(feature)
                }
            }

            MonetizationStatus.FREE_WITH_ADS,
            null -> {
                when {
                    access.freeAds -> allowed(feature)
                    access.premium || access.trial -> locked(feature, PremiumFeatureGateReason.PremiumRequired)
                    else -> disabledByConfig(feature)
                }
            }

            MonetizationStatus.LICENSE_EXPIRED -> PremiumFeatureGateResult(
                feature = feature,
                state = PremiumFeatureGateState.BlockedExpired,
                reason = PremiumFeatureGateReason.LicenseExpired,
            )

            MonetizationStatus.TRIAL_EXPIRED -> PremiumFeatureGateResult(
                feature = feature,
                state = PremiumFeatureGateState.BlockedExpired,
                reason = PremiumFeatureGateReason.TrialExpired,
            )
        }

    private fun allowed(feature: PremiumFeature): PremiumFeatureGateResult =
        PremiumFeatureGateResult(feature, PremiumFeatureGateState.Allowed)

    private fun locked(
        feature: PremiumFeature,
        reason: PremiumFeatureGateReason,
    ): PremiumFeatureGateResult =
        PremiumFeatureGateResult(feature, PremiumFeatureGateState.LockedPremiumVisible, reason)

    private fun disabledByConfig(feature: PremiumFeature): PremiumFeatureGateResult =
        PremiumFeatureGateResult(
            feature = feature,
            state = PremiumFeatureGateState.ConfigDisabled,
            reason = PremiumFeatureGateReason.DisabledByConfig,
        )
}

fun AppRuntimeConfig.featureAccessFor(feature: PremiumFeature): FeatureAccess =
    features.firstOrNull { it.key == feature.key }
        ?: FeatureAccess(
            key = feature.key,
            label = feature.defaultLabel,
            premium = feature.defaultPremium,
            trial = feature.defaultTrial,
            freeAds = feature.defaultFreeAds,
        )
