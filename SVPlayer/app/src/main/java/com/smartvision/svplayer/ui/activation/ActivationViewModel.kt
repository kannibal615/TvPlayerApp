package com.smartvision.svplayer.ui.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.data.activation.ActivationException
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.data.activation.ActivationSession
import com.smartvision.svplayer.data.activation.ActivationStatus
import com.smartvision.svplayer.data.monetization.MonetizationStatus
import com.smartvision.svplayer.data.monetization.monetizationStatus
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
    val publicDeviceCode: String = "",
    val shortCode: String = "",
    val qrUrl: String = "",
    val expiresAt: String = "",
    val pollingIntervalSeconds: Int = 5,
    val activationType: String? = null,
    val licenseStatus: String? = null,
    val trialStatus: String? = null,
    val freeWithAdsStatus: String? = null,
    val playlistConfigured: Boolean = false,
    val showFreeWithAdsChoice: Boolean = false,
    val activationBusy: Boolean = false,
    val statusLabel: String = "Verification de l activation...",
    val errorMessage: String? = null,
    val backendUrl: String = BuildConfig.ACTIVATION_BASE_URL,
) {
    val hasSession: Boolean = shortCode.isNotBlank() && qrUrl.isNotBlank()
    val shouldShowLicenseKey: Boolean = activationType == "trial_demo" || activationType == "free_ads" || freeWithAdsStatus == "active"
    val purchaseUrl: String =
        backendUrl.trimEnd('/') + "/activation?device=" + publicDeviceCode.ifBlank { deviceId }
    val xtreamSetupUrl: String =
        backendUrl.trimEnd('/') + "/xtream?device=" + publicDeviceCode.ifBlank { deviceId }
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

    fun activateLicense(rawLicenseCode: String) {
        val normalized = rawLicenseCode.replace(Regex("[\\s-]+"), "").uppercase()
        if (!Regex("^[A-Z0-9]{10}$").matches(normalized)) {
            _uiState.update {
                it.copy(
                    errorMessage = "Le code doit contenir exactement 10 caracteres alphanumeriques.",
                    statusLabel = "Code licence invalide",
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activationBusy = true,
                    checking = false,
                    errorMessage = null,
                    statusLabel = "Activation de la licence...",
                )
            }
            runCatching { repository.activateLicense(normalized) }
                .onSuccess { status -> applyAccessStatus(status) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            activationBusy = false,
                            checking = false,
                            errorMessage = error.userMessage("Impossible d activer ce code licence."),
                            statusLabel = "Activation refusee",
                        )
                    }
                }
        }
    }

    fun startTrial() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activationBusy = true,
                    checking = false,
                    errorMessage = null,
                    statusLabel = "Demarrage de l essai gratuit...",
                )
            }
            runCatching { repository.startTrial() }
                .onSuccess { status -> applyAccessStatus(status) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            activationBusy = false,
                            checking = false,
                            showFreeWithAdsChoice = false,
                            errorMessage = error.userMessage("L essai gratuit n est pas disponible."),
                            statusLabel = "Essai gratuit indisponible",
                        )
                    }
                }
        }
    }

    fun continueFreeWithAds() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activationBusy = true,
                    checking = false,
                    errorMessage = null,
                    statusLabel = "Activation du mode gratuit...",
                )
            }
            runCatching { repository.enableFreeWithAds() }
                .onSuccess { status -> applyAccessStatus(status) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            activationBusy = false,
                            errorMessage = error.userMessage("Mode gratuit indisponible."),
                            statusLabel = "Mode gratuit indisponible",
                        )
                    }
                }
        }
    }

    fun showActivationForm() {
        _uiState.update {
            it.copy(
                showFreeWithAdsChoice = false,
                activated = false,
                errorMessage = null,
                statusLabel = "Saisissez une licence ou scannez le QR code",
            )
        }
    }

    private fun start() {
        pollingJob?.cancel()
        viewModelScope.launch {
            val localPublicCode = repository.getOrCreateLocalPublicCode()
            val cached = repository.localState.first()
            _uiState.update {
                it.copy(
                    checking = true,
                    activated = false,
                    deviceId = cached.deviceId,
                    publicDeviceCode = cached.publicDeviceCode.ifBlank { localPublicCode },
                    expiresAt = cached.expiresAt.orEmpty(),
                    activationType = cached.activationType,
                    licenseStatus = cached.licenseStatus,
                    trialStatus = cached.trialStatus,
                    freeWithAdsStatus = cached.freeWithAdsStatus,
                    playlistConfigured = cached.playlistConfigured,
                    showFreeWithAdsChoice = false,
                    errorMessage = null,
                    statusLabel = "Initialisation de l appareil...",
                )
            }

            val registration = runCatching { repository.registerDevice() }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            checking = false,
                            activated = false,
                            errorMessage = error.userMessage("Connexion impossible. Verifiez votre reseau."),
                            statusLabel = "Connexion impossible",
                        )
                    }
                }
                .getOrNull()
                ?: return@launch

            val status = runCatching { repository.checkStatus() }.getOrElse { registration }
            val handled = applyAccessStatus(status)
            if (handled) {
                return@launch
            }

            startPassivePolling()
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

    private fun startPassivePolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive && !_uiState.value.activated && !_uiState.value.showFreeWithAdsChoice) {
                delay(_uiState.value.pollingIntervalSeconds.coerceAtLeast(1) * 1000L)
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
                applyAccessStatus(status)
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

    private fun applyAccessStatus(
        status: com.smartvision.svplayer.data.activation.RemoteActivationStatus,
    ): Boolean {
        val monetizationStatus = status.monetizationStatus()
        when {
            status.status == ActivationStatus.Blocked -> {
                pollingJob?.cancel()
                _uiState.update {
                    it.copy(
                        checking = false,
                        creatingSession = false,
                        polling = false,
                        activationBusy = false,
                        activated = false,
                        blocked = true,
                        deviceId = status.deviceId.ifBlank { it.deviceId },
                        publicDeviceCode = status.publicDeviceCode.ifBlank { it.publicDeviceCode },
                        activationType = null,
                        licenseStatus = status.licenseStatus,
                        trialStatus = status.trialStatus,
                        freeWithAdsStatus = status.freeWithAdsStatus,
                        playlistConfigured = status.playlistConfigured,
                        showFreeWithAdsChoice = false,
                        statusLabel = "Appareil bloque",
                        errorMessage = "Cet appareil est bloque. Contactez le support SmartVision.",
                    )
                }
                return true
            }

            status.status == ActivationStatus.Expired -> {
                pollingJob?.cancel()
                _uiState.update {
                    it.copy(
                        checking = false,
                        creatingSession = false,
                        polling = false,
                        activationBusy = false,
                        activated = false,
                        blocked = false,
                        deviceId = status.deviceId.ifBlank { it.deviceId },
                        publicDeviceCode = status.publicDeviceCode.ifBlank { it.publicDeviceCode },
                        expiresAt = status.expiresAt.orEmpty(),
                        activationType = status.activationType,
                        licenseStatus = status.licenseStatus,
                        trialStatus = status.trialStatus,
                        freeWithAdsStatus = status.freeWithAdsStatus,
                        playlistConfigured = status.playlistConfigured,
                        showFreeWithAdsChoice = true,
                        statusLabel = if (monetizationStatus == MonetizationStatus.LICENSE_EXPIRED) {
                            "Licence Premium expiree"
                        } else {
                            "Essai gratuit termine"
                        },
                        errorMessage = null,
                    )
                }
                return true
            }

            status.activated && status.status == ActivationStatus.Active -> {
                pollingJob?.cancel()
                _uiState.update {
                    it.copy(
                        checking = false,
                        creatingSession = false,
                        polling = false,
                        activationBusy = false,
                        activated = true,
                        blocked = false,
                        deviceId = status.deviceId.ifBlank { it.deviceId },
                        publicDeviceCode = status.publicDeviceCode.ifBlank { it.publicDeviceCode },
                        expiresAt = status.expiresAt.orEmpty(),
                        activationType = status.activationType,
                        licenseStatus = status.licenseStatus,
                        trialStatus = status.trialStatus,
                        freeWithAdsStatus = status.freeWithAdsStatus,
                        playlistConfigured = status.playlistConfigured,
                        showFreeWithAdsChoice = false,
                        errorMessage = null,
                        statusLabel = if (monetizationStatus == MonetizationStatus.FREE_WITH_ADS) {
                            "Mode gratuit avec pubs"
                        } else {
                            "Activation validee"
                        },
                    )
                }
                return true
            }

            else -> {
                _uiState.update {
                    it.copy(
                        checking = false,
                        creatingSession = false,
                        polling = true,
                        activationBusy = false,
                        activated = false,
                        blocked = false,
                        deviceId = status.deviceId.ifBlank { it.deviceId },
                        publicDeviceCode = status.publicDeviceCode.ifBlank { it.publicDeviceCode },
                        expiresAt = status.expiresAt.orEmpty(),
                        activationType = status.activationType,
                        licenseStatus = status.licenseStatus,
                        trialStatus = status.trialStatus,
                        freeWithAdsStatus = status.freeWithAdsStatus,
                        playlistConfigured = status.playlistConfigured,
                        showFreeWithAdsChoice = false,
                        statusLabel = "En attente de licence",
                        errorMessage = null,
                    )
                }
                return false
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
        publicDeviceCode = publicDeviceCode,
        shortCode = session.shortCode,
        qrUrl = session.qrUrl,
        expiresAt = session.expiresAt,
        pollingIntervalSeconds = session.pollingIntervalSeconds,
        activationType = null,
        licenseStatus = null,
        trialStatus = null,
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
