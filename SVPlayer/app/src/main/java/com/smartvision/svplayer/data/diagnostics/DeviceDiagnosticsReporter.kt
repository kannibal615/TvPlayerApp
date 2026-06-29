package com.smartvision.svplayer.data.diagnostics

import android.content.Context
import android.os.Build
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.startup.StartupStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DeviceDiagnosticsReporter(
    private val appContext: Context,
    private val activationRepository: ActivationRepository,
    private val api: DeviceDiagnosticsApiService,
    private val stateStore: StartupStateStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun syncLatestAsync() {
        reportAutostartAsync()
        reportAutoSyncAsync()
    }

    fun reportAutostartAsync() {
        scope.launch {
            report("autostart", stateStore.autostartSnapshot())
        }
    }

    fun reportAutoSyncAsync() {
        scope.launch {
            report("auto_sync", stateStore.autoSyncSnapshot())
        }
    }

    private suspend fun report(type: String, payload: Map<String, Any?>) {
        val local = activationRepository.localState.first()
        val deviceId = local.deviceId.ifBlank { activationRepository.getOrCreateDeviceId() }
        val publicDeviceCode = local.publicDeviceCode.ifBlank { activationRepository.getOrCreateLocalPublicCode() }
        if (deviceId.isBlank() && publicDeviceCode.isBlank()) return
        runCatching {
            api.upsert(
                DeviceDiagnosticsRequest(
                    deviceId = deviceId,
                    publicDeviceCode = publicDeviceCode,
                    appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    androidVersion = Build.VERSION.RELEASE.orEmpty(),
                    deviceModel = listOf(Build.MANUFACTURER, Build.MODEL).filter { !it.isNullOrBlank() }.joinToString(" "),
                    diagnosticType = type,
                    payload = payload,
                )
            )
        }
    }
}
