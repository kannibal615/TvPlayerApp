package com.smartvision.svplayer.ui.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.data.activation.ActivationException
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.data.activation.ActivationSession
import com.smartvision.svplayer.data.activation.ActivationStatus
import com.smartvision.svplayer.data.activation.StoredActivationState
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

enum class ActivationOfferMode {
    TrialAvailable,
    FreeWithAds,
    Blocked,
}

enum class ActivationUiError {
    InvalidLicenseCode,
    LicenseActivationFailed,
    FreeWithAdsUnavailable,
    NetworkUnavailable,
    TrialUnavailable,
    SessionUnavailable,
    StatusUnavailable,
    DeviceBlocked,
}

enum class ActivationAction {
    Trial,
    FreeWithAds,
    License,
}

data class ActivationUiState(
    val checking: Boolean = true,
    val localStateReady: Boolean = false,
    val initialAccessResolved: Boolean = false,
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
    val offerMode: ActivationOfferMode = ActivationOfferMode.TrialAvailable,
    val activationBusy: Boolean = false,
    val activeAction: ActivationAction? = null,
    val statusLabel: String = "Verification de l activation...",
    val error: ActivationUiError? = null,
    val backendUrl: String = BuildConfig.ACTIVATION_BASE_URL,
) {
    val canRevealInitialSurface: Boolean
        get() = localStateReady && initialAccessResolved

    val hasSession: Boolean = shortCode.isNotBlank() && qrUrl.isNotBlank()
    val shouldShowLicenseKey: Boolean = activationType == "trial_demo" || activationType == "free_ads" || freeWithAdsStatus == "active"
    val purchaseUrl: String =
        backendUrl.trimEnd('/') + "/account/?source=tv&intent=license&" +
            "device=$publicDeviceCode" +
            "&plan=year_1"
    val xtreamSetupUrl: String =
        backendUrl.trimEnd('/') + "/xtream?device=" + publicDeviceCode.ifBlank { deviceId }
}

class ActivationViewModel(
    private val repository: ActivationRepository,
    private val initialLocalState: StoredActivationState? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialLocalState.toActivationUiState())
    val uiState: StateFlow<ActivationUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var pendingInitialLocalState: StoredActivationState? = initialLocalState

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
                    error = ActivationUiError.InvalidLicenseCode,
                    statusLabel = "Code licence invalide",
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activationBusy = true,
                    activeAction = ActivationAction.License,
                    checking = false,
                    error = null,
                    statusLabel = "Activation de la licence...",
                )
            }
            runCatching { repository.activateLicense(normalized) }
                .onSuccess { status -> applyAccessStatus(status) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            activationBusy = false,
                            activeAction = null,
                            checking = false,
                            error = error.toActivationUiError(ActivationUiError.LicenseActivationFailed),
                            statusLabel = "Activation refusee",
                        )
                    }
                }
        }
    }

    fun startTrial() {
        viewModelScope.launch {
            startTrialFromUserAction()
        }
    }

    fun continueFreeWithAds() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activationBusy = true,
                    activeAction = ActivationAction.FreeWithAds,
                    checking = false,
                    error = null,
                    statusLabel = "Activation du mode gratuit...",
                )
            }
            runCatching { repository.enableFreeWithAds() }
                .onSuccess { status -> applyAccessStatus(status) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            activationBusy = false,
                            activeAction = null,
                            error = error.toActivationUiError(ActivationUiError.FreeWithAdsUnavailable),
                            statusLabel = "Mode gratuit indisponible",
                        )
                    }
                }
        }
    }

    private fun start() {
        pollingJob?.cancel()
        viewModelScope.launch {
            val localPublicCode = repository.getOrCreateLocalPublicCode()
            val cached = pendingInitialLocalState ?: repository.localState.first()
            pendingInitialLocalState = null
            val cachedMonetization = cached.monetizationStatus()
            val cachedHasAccess =
                cached.activated &&
                    ActivationStatus.fromValue(cached.status) == ActivationStatus.Active &&
                    cachedMonetization.hasRuntimeAccess()
            val accessWasAlreadyResolved = _uiState.value.initialAccessResolved
            val canKeepCurrentSurface = cachedHasAccess || accessWasAlreadyResolved
            _uiState.update {
                it.copy(
                    localStateReady = true,
                    initialAccessResolved = canKeepCurrentSurface,
                    checking = !cachedHasAccess,
                    activated = cachedHasAccess,
                    activationBusy = false,
                    activeAction = null,
                    blocked = ActivationStatus.fromValue(cached.status) == ActivationStatus.Blocked,
                    deviceId = cached.deviceId,
                    publicDeviceCode = cached.publicDeviceCode.ifBlank { localPublicCode },
                    expiresAt = cached.expiresAt.orEmpty(),
                    activationType = cached.activationType,
                    licenseStatus = cached.licenseStatus,
                    trialStatus = cached.trialStatus,
                    freeWithAdsStatus = cached.freeWithAdsStatus,
                    playlistConfigured = cached.playlistConfigured,
                    offerMode = resolveActivationOfferMode(
                        status = ActivationStatus.fromValue(cached.status),
                        trialStatus = cached.trialStatus,
                        licenseStatus = cached.licenseStatus,
                    ),
                    error = null,
                    statusLabel = if (cachedHasAccess) {
                        cachedMonetization.accessStatusLabel()
                    } else {
                        "Initialisation de l appareil..."
                    },
                )
            }

            val registration = runCatching { repository.registerDevice() }
                .onFailure { error ->
                    if (cachedHasAccess) {
                        _uiState.update {
                            it.copy(
                                checking = false,
                                initialAccessResolved = true,
                                activated = true,
                                activationBusy = false,
                                activeAction = null,
                                blocked = false,
                                error = null,
                                statusLabel = cachedMonetization.accessStatusLabel(),
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                checking = false,
                                initialAccessResolved = true,
                                activated = false,
                                error = ActivationUiError.NetworkUnavailable,
                                statusLabel = "Connexion impossible",
                            )
                        }
                    }
                }
                .getOrNull()
                ?: return@launch

            val status = runCatching { repository.checkStatus() }.getOrElse { registration }
            val handled = applyAccessStatus(
                status = status,
                resolveInitialAccess = canKeepCurrentSurface,
            )
            if (handled) {
                _uiState.update { it.copy(initialAccessResolved = true) }
                return@launch
            }

            startTrialAutomatically()
        }
    }

    private suspend fun startTrialFromUserAction() {
        _uiState.update {
            it.copy(
                activationBusy = true,
                activeAction = ActivationAction.Trial,
                checking = false,
                error = null,
                statusLabel = "Demarrage de l essai gratuit...",
            )
        }
        runCatching { repository.startTrial() }
            .onSuccess { status -> applyAccessStatus(status) }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        activationBusy = false,
                        activeAction = null,
                        checking = false,
                        offerMode = ActivationOfferMode.TrialAvailable,
                        error = error.toActivationUiError(ActivationUiError.TrialUnavailable),
                        statusLabel = "Essai gratuit indisponible",
                    )
                }
            }
    }

    private suspend fun startTrialAutomatically() {
        _uiState.update {
            it.copy(
                checking = true,
                creatingSession = false,
                polling = false,
                activationBusy = true,
                activeAction = ActivationAction.Trial,
                offerMode = ActivationOfferMode.TrialAvailable,
                error = null,
                statusLabel = "Preparation de l essai gratuit...",
            )
        }
        runCatching { repository.startTrial() }
            .onSuccess { status -> applyAccessStatus(status) }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        checking = false,
                        initialAccessResolved = true,
                        creatingSession = false,
                        polling = false,
                        activationBusy = false,
                        activeAction = null,
                        activated = false,
                        offerMode = ActivationOfferMode.TrialAvailable,
                        error = error.toActivationUiError(ActivationUiError.TrialUnavailable),
                        statusLabel = "Essai gratuit indisponible",
                    )
                }
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
                error = null,
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
                        initialAccessResolved = true,
                        creatingSession = false,
                        polling = false,
                        error = error.toActivationUiError(ActivationUiError.SessionUnavailable),
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
                    error = null,
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
                        initialAccessResolved = true,
                        creatingSession = false,
                        polling = it.hasSession,
                        error = error.toActivationUiError(ActivationUiError.StatusUnavailable),
                        statusLabel = if (it.hasSession) "En attente de validation" else "Activation indisponible",
                    )
                }
            }
    }

    private fun applyAccessStatus(
        status: com.smartvision.svplayer.data.activation.RemoteActivationStatus,
        resolveInitialAccess: Boolean = true,
    ): Boolean {
        val monetizationStatus = status.monetizationStatus()
        val debugStatusForced =
            BuildConfig.DEBUG && BuildConfig.DEBUG_MONETIZATION_STATUS.isNotBlank()
        when {
            status.status == ActivationStatus.Blocked -> {
                pollingJob?.cancel()
                _uiState.update {
                    it.copy(
                        checking = false,
                        initialAccessResolved = resolveInitialAccess,
                        creatingSession = false,
                        polling = false,
                        activationBusy = false,
                        activeAction = null,
                        activated = false,
                        blocked = true,
                        deviceId = status.deviceId.ifBlank { it.deviceId },
                        publicDeviceCode = status.publicDeviceCode.ifBlank { it.publicDeviceCode },
                        activationType = null,
                        licenseStatus = status.licenseStatus,
                        trialStatus = status.trialStatus,
                        freeWithAdsStatus = status.freeWithAdsStatus,
                        playlistConfigured = status.playlistConfigured,
                        offerMode = ActivationOfferMode.Blocked,
                        statusLabel = "Appareil bloque",
                        error = ActivationUiError.DeviceBlocked,
                    )
                }
                return true
            }

            status.status == ActivationStatus.Expired ||
                monetizationStatus == MonetizationStatus.TRIAL_EXPIRED ||
                monetizationStatus == MonetizationStatus.LICENSE_EXPIRED -> {
                pollingJob?.cancel()
                _uiState.update {
                    it.copy(
                        checking = false,
                        initialAccessResolved = resolveInitialAccess,
                        creatingSession = false,
                        polling = false,
                        activationBusy = false,
                        activeAction = null,
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
                        offerMode = ActivationOfferMode.FreeWithAds,
                        statusLabel = if (monetizationStatus == MonetizationStatus.LICENSE_EXPIRED) {
                            "Licence Premium expiree"
                        } else {
                            "Essai gratuit termine"
                        },
                        error = null,
                    )
                }
                return true
            }

            status.activated && status.status == ActivationStatus.Active ||
                debugStatusForced && (
                    monetizationStatus == MonetizationStatus.PREMIUM_ACTIVE ||
                        monetizationStatus == MonetizationStatus.TRIAL_ACTIVE ||
                        monetizationStatus == MonetizationStatus.FREE_WITH_ADS
                    ) -> {
                pollingJob?.cancel()
                _uiState.update {
                    it.copy(
                        checking = false,
                        initialAccessResolved = resolveInitialAccess,
                        creatingSession = false,
                        polling = false,
                        activationBusy = false,
                        activeAction = null,
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
                        offerMode = ActivationOfferMode.TrialAvailable,
                        error = null,
                        statusLabel = monetizationStatus.accessStatusLabel(),
                    )
                }
                return true
            }

            else -> {
                _uiState.update {
                    it.copy(
                        checking = false,
                        initialAccessResolved = resolveInitialAccess,
                        creatingSession = false,
                        polling = true,
                        activationBusy = false,
                        activeAction = null,
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
                        offerMode = ActivationOfferMode.TrialAvailable,
                        statusLabel = "En attente de licence",
                        error = null,
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

private fun StoredActivationState?.toActivationUiState(): ActivationUiState {
    if (this == null) return ActivationUiState()
    val monetization = monetizationStatus()
    val hasAccess = activated &&
        ActivationStatus.fromValue(status) == ActivationStatus.Active &&
        monetization.hasRuntimeAccess()
    return ActivationUiState(
        localStateReady = true,
        initialAccessResolved = hasAccess,
        checking = !hasAccess,
        activated = hasAccess,
        blocked = ActivationStatus.fromValue(status) == ActivationStatus.Blocked,
        deviceId = deviceId,
        publicDeviceCode = publicDeviceCode,
        expiresAt = expiresAt.orEmpty(),
        activationType = activationType,
        licenseStatus = licenseStatus,
        trialStatus = trialStatus,
        freeWithAdsStatus = freeWithAdsStatus,
        playlistConfigured = playlistConfigured,
        offerMode = resolveActivationOfferMode(
            status = ActivationStatus.fromValue(status),
            trialStatus = trialStatus,
            licenseStatus = licenseStatus,
        ),
        activationBusy = false,
        activeAction = null,
        statusLabel = if (hasAccess) {
            monetization.accessStatusLabel()
        } else {
            "Initialisation de l appareil..."
        },
        error = null,
    )
}

private fun MonetizationStatus?.hasRuntimeAccess(): Boolean =
    this == MonetizationStatus.PREMIUM_ACTIVE ||
        this == MonetizationStatus.TRIAL_ACTIVE ||
        this == MonetizationStatus.FREE_WITH_ADS

private fun MonetizationStatus?.accessStatusLabel(): String =
    if (this == MonetizationStatus.FREE_WITH_ADS) {
        "Mode gratuit avec pubs"
    } else {
        "Activation validee"
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
        error = null,
    )

internal fun resolveActivationOfferMode(
    status: ActivationStatus,
    trialStatus: String?,
    licenseStatus: String?,
): ActivationOfferMode =
    when {
        status == ActivationStatus.Blocked -> ActivationOfferMode.Blocked
        trialStatus == "expired" || licenseStatus == "expired" -> ActivationOfferMode.FreeWithAds
        else -> ActivationOfferMode.TrialAvailable
    }

private fun Throwable.toActivationUiError(defaultError: ActivationUiError): ActivationUiError =
    when (this) {
        is IOException, is HttpException -> ActivationUiError.NetworkUnavailable
        is ActivationException -> defaultError
        else -> defaultError
    }
