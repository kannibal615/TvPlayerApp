package com.smartvision.svplayer.data.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.smartvision.svplayer.BuildConfig
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class PrivacyConsentManager(context: Context) {
    private val consentInformation =
        if (BuildConfig.GOOGLE_ADS_APPLICATION_ID.isBlank()) {
            null
        } else {
            UserMessagingPlatform.getConsentInformation(context.applicationContext)
        }
    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    suspend fun refreshSilently(activity: Activity): Boolean {
        if (!BuildConfig.ADS_RUNTIME_CONFIGURED) return false
        val consentInfo = consentInformation ?: return false
        val updated = suspendCancellableCoroutine { continuation ->
            val parameters = ConsentRequestParameters.Builder().build()
            consentInfo.requestConsentInfoUpdate(
                activity,
                parameters,
                {
                    if (continuation.isActive) continuation.resume(true)
                },
                { error ->
                    Log.w(TAG, "Mise a jour consentement impossible: ${error.message}")
                    if (continuation.isActive) continuation.resume(false)
                },
            )
        }
        updatePrivacyOptionState()
        return updated
    }

    suspend fun ensureConsentForAd(activity: Activity): Boolean {
        if (!BuildConfig.ADS_RUNTIME_CONFIGURED) return false
        val consentInfo = consentInformation ?: return true
        refreshSilently(activity)
        if (consentInfo.canRequestAds()) return true
        suspendCancellableCoroutine { continuation ->
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                if (error != null) {
                    Log.w(TAG, "Formulaire de consentement indisponible: ${error.message}")
                }
                updatePrivacyOptionState()
                if (continuation.isActive) {
                    continuation.resume(consentInfo.canRequestAds())
                }
            }
        }
        return consentInfo.canRequestAds()
    }

    suspend fun showPrivacyOptions(activity: Activity): Boolean =
        suspendCancellableCoroutine { continuation ->
            if (consentInformation == null) {
                if (continuation.isActive) continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
                if (error != null) {
                    Log.w(TAG, "Options de confidentialite indisponibles: ${error.message}")
                }
                updatePrivacyOptionState()
                if (continuation.isActive) continuation.resume(error == null)
            }
        }

    private fun updatePrivacyOptionState() {
        _privacyOptionsRequired.value =
            consentInformation?.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private companion object {
        const val TAG = "SmartVisionConsent"
    }
}
