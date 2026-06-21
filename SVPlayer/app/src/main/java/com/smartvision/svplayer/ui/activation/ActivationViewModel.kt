package com.smartvision.svplayer.ui.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.data.activation.ActivationException
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.data.activation.ActivationSession
import com.smartvision.svplayer.data.activation.ActivationStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class ActivationUiState(
    val checking: Boolean = true,
    val creatingSession: Boolean = false,
    val polling: Boolean = false,
    val activated: Boolean = false,
    val blocked: Boolean = false,
    val deviceId: String = "",
    val shortCode: String = "",
    val qrUrl: String = "",
    val expiresAt: String = "",
    val pollingIntervalSeconds: Int = 5,
    val activationType: String? = null,
    val statusLabel: String = "Verification de l activation...",
    val errorMessage: String? = null,
    val backendUrl: String = BuildConfig.ACTIVATION_BASE_URL,
) {
    val hasSession: Boolean = shortCode.isNotBlank() && qrUrl.isNotBlank()
    val shouldShowLicenseKey: Boolean = activationType == "trial_demo" || activationType == "free_ads"
}

class ActivationViewModel(
    private val repository: ActivationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActivationUiState())
    val uiState: StateFlow<ActivationUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        start()
    }

    fun retry() {
        start()
    }

    fun refreshSession() {
        viewModelScope.launch {
            createSessionAndStartPolling()
        }
    }

    fun checkNow() {
        viewModelScope.launch {
            checkRemoteStatus(updateLoading = true)
        }
    }

    private fun start() {
        pollingJob?.cancel()
        viewModelScope.launch {
            val deviceId = repository.getOrCreateDeviceId()
            val cached = repository.localState.first()
            _uiState.update {
                it.copy(
                    checking = true,
                    activated = cached.activated && cached.status == ActivationStatus.Active.value,
                    deviceId = deviceId,
                    expiresAt = cached.expiresAt.orEmpty(),
                    activationType = cached.activationType,
                    errorMessage = null,
                )
            }

            val status = runCatching { repository.checkStatus() }.getOrNull()
            if (status?.activated == true && status.status == ActivationStatus.Active) {
                _uiState.update {
                    it.copy(
                        checking = false,
                        activated = true,
                        polling = false,
                        expiresAt = status.expiresAt.orEmpty(),
                        activationType = status.activationType,
                        statusLabel = "Activation validee",
                        errorMessage = null,
                    )
                }
                return@launch
            }

            if (status?.status == ActivationStatus.Blocked) {
                _uiState.update {
                    it.copy(
                        checking = false,
                        activated = false,
                        blocked = true,
                        polling = false,
                        activationType = null,
                        statusLabel = "Appareil bloque",
                        errorMessage = "Cet appareil est bloque. Contactez le support SmartVision.",
                    )
                }
                return@launch
            }

            createSessionAndStartPolling()
        }
    }

    private suspend fun createSessionAndStartPolling() {
        pollingJob?.cancel()
        _uiState.update {
            it.copy(
                checking = false,
                creatingSession = true,
                polling = false,
                blocked = false,
                activationType = null,
                errorMessage = null,
                statusLabel = "Generation du code d activation...",
            )
        }

        runCatching { repository.createSession() }
            .onSuccess { session ->
                _uiState.update { it.withSession(session) }
                startPolling(session.pollingIntervalSeconds)
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        checking = false,
                        creatingSession = false,
                        polling = false,
                        errorMessage = error.userMessage("Impossible de creer la session d activation."),
                        statusLabel = "Activation indisponible",
                    )
                }
            }
    }

    private fun startPolling(intervalSeconds: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive && !_uiState.value.activated) {
                delay(intervalSeconds.coerceAtLeast(1) * 1000L)
                checkRemoteStatus(updateLoading = false)
            }
        }
    }

    private suspend fun checkRemoteStatus(updateLoading: Boolean) {
        if (updateLoading) {
            _uiState.update {
                it.copy(
                    checking = true,
                    errorMessage = null,
                    statusLabel = "Verification du statut...",
                )
            }
        }

        runCatching { repository.checkStatus() }
            .onSuccess { status ->
                when {
                    status.activated && status.status == ActivationStatus.Active -> {
                        pollingJob?.cancel()
                        _uiState.update {
                            it.copy(
                                checking = false,
                                creatingSession = false,
                                polling = false,
                                activated = true,
                                blocked = false,
                                expiresAt = status.expiresAt.orEmpty(),
                                activationType = status.activationType,
                                errorMessage = null,
                                statusLabel = "Activation validee",
                            )
                        }
                    }
                    status.status == ActivationStatus.Blocked -> {
                        pollingJob?.cancel()
                        _uiState.update {
                            it.copy(
                                checking = false,
                                creatingSession = false,
                                polling = false,
                                activated = false,
                                blocked = true,
                                activationType = null,
                                statusLabel = "Appareil bloque",
                                errorMessage = "Cet appareil est bloque. Contactez le support SmartVision.",
                            )
                        }
                    }
                    status.status == ActivationStatus.Expired -> {
                        pollingJob?.cancel()
                        _uiState.update {
                            it.copy(
                                checking = false,
                                creatingSession = false,
                                polling = false,
                                activated = false,
                                blocked = false,
                                activationType = null,
                                statusLabel = "Activation expiree",
                                errorMessage = "L activation de cet appareil a expire. Generez un nouveau code.",
                            )
                        }
                    }
                    else -> {
                        _uiState.update {
                            it.copy(
                                checking = false,
                                creatingSession = false,
                                polling = true,
                                activated = false,
                                blocked = false,
                                activationType = null,
                                statusLabel = "En attente de validation",
                                errorMessage = null,
                            )
                        }
                    }
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        checking = false,
                        creatingSession = false,
                        polling = it.hasSession,
                        errorMessage = error.userMessage("Statut activation indisponible."),
                        statusLabel = if (it.hasSession) "En attente de validation" else "Activation indisponible",
                    )
                }
            }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }
}

private fun ActivationUiState.withSession(session: ActivationSession): ActivationUiState =
    copy(
        checking = false,
        creatingSession = false,
        polling = true,
        activated = false,
        blocked = false,
        deviceId = session.deviceId,
        shortCode = session.shortCode,
        qrUrl = session.qrUrl,
        expiresAt = session.expiresAt,
        pollingIntervalSeconds = session.pollingIntervalSeconds,
        activationType = null,
        statusLabel = "En attente de validation",
        errorMessage = null,
    )

private fun Throwable.userMessage(defaultMessage: String): String =
    when (this) {
        is ActivationException -> message ?: defaultMessage
        is IOException -> "Connexion au serveur d activation impossible."
        is HttpException -> "Serveur d activation indisponible (${code()})."
        else -> defaultMessage
    }
