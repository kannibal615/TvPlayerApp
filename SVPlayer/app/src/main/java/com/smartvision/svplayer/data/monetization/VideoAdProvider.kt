package com.smartvision.svplayer.data.monetization

import android.content.Context
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.exoplayer.ima.ImaAdsLoader
import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory
import com.smartvision.svplayer.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface VideoAdProvider {
    val adsLoader: ImaAdsLoader
    val forceFailureForDebug: Boolean

    fun bindRequest(requestId: String, callbacks: VideoAdCallbacks)
    fun attachPlayer(player: Player)
    fun detachPlayer(player: Player)
    fun clearRequest(requestId: String)
}

data class VideoAdCallbacks(
    val onStarted: () -> Unit,
    val onFinished: () -> Unit,
    val onFailed: (String) -> Unit,
)

class ImaVideoAdProvider(
    context: Context,
    private val monetizationManager: MonetizationManager,
) : VideoAdProvider {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var activeRequest: ActiveRequest? = null

    override val forceFailureForDebug: Boolean =
        BuildConfig.DEBUG && BuildConfig.DEBUG_FORCE_AD_FAILURE

    override val adsLoader: ImaAdsLoader = ImaAdsLoader.Builder(context.applicationContext)
        .setImaSdkSettings(
            ImaSdkFactory.getInstance().createImaSdkSettings().apply {
                playerType = "SmartVision Android TV"
                playerVersion = BuildConfig.VERSION_NAME
            },
        )
        .setFocusSkipButtonWhenAvailable(true)
        .setEnableContinuousPlayback(false)
        .setAdPreloadTimeoutMs(8_000)
        .setVastLoadTimeoutMs(8_000)
        .setMediaLoadTimeoutMs(8_000)
        .setDebugModeEnabled(BuildConfig.DEBUG)
        .setAdEventListener(::handleAdEvent)
        .setAdErrorListener(::handleAdError)
        .build()

    override fun bindRequest(requestId: String, callbacks: VideoAdCallbacks) {
        activeRequest = ActiveRequest(requestId, callbacks)
    }

    override fun attachPlayer(player: Player) {
        adsLoader.setPlayer(player)
    }

    override fun detachPlayer(player: Player) {
        adsLoader.setPlayer(null)
    }

    override fun clearRequest(requestId: String) {
        if (activeRequest?.requestId == requestId) {
            activeRequest = null
        }
    }

    private fun handleAdEvent(event: AdEvent) {
        val request = activeRequest ?: return
        when (event.type) {
            AdEvent.AdEventType.STARTED -> {
                if (!request.started) {
                    activeRequest = request.copy(started = true)
                    scope.launch { monetizationManager.onAdStarted(request.requestId) }
                    request.callbacks.onStarted()
                }
            }

            AdEvent.AdEventType.COMPLETED,
            AdEvent.AdEventType.SKIPPED,
            AdEvent.AdEventType.ALL_ADS_COMPLETED,
            AdEvent.AdEventType.CONTENT_RESUME_REQUESTED,
            -> finishRequest(request)

            else -> Unit
        }
    }

    private fun handleAdError(event: AdErrorEvent) {
        val request = activeRequest ?: return
        val message = event.error?.message ?: "Erreur IMA inconnue"
        Log.w(TAG, message)
        activeRequest = null
        scope.launch { monetizationManager.onAdFailed(request.requestId, message) }
        request.callbacks.onFailed(message)
    }

    private fun finishRequest(request: ActiveRequest) {
        if (activeRequest?.requestId != request.requestId) return
        activeRequest = null
        scope.launch { monetizationManager.onAdCompleted(request.requestId) }
        request.callbacks.onFinished()
    }

    private data class ActiveRequest(
        val requestId: String,
        val callbacks: VideoAdCallbacks,
        val started: Boolean = false,
    )

    private companion object {
        const val TAG = "SmartVisionIma"
    }
}
