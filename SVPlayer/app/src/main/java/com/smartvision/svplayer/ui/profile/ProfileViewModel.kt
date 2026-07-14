package com.smartvision.svplayer.ui.profile

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.core.config.PlaylistProfile
import com.smartvision.svplayer.core.config.PlaylistSource
import com.smartvision.svplayer.core.config.XtreamAccount
import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.data.activation.ActivationRepository
import com.smartvision.svplayer.data.activation.StoredActivationState
import com.smartvision.svplayer.data.monetization.MonetizationStatus
import com.smartvision.svplayer.data.monetization.monetizationStatus
import com.smartvision.svplayer.data.repository.emptyAccountProfile
import com.smartvision.svplayer.domain.model.AccountProfile
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val activationRepository: ActivationRepository,
    private val accountManager: XtreamAccountManager,
    catalogRepository: CatalogRepository,
) : ViewModel() {
    private val transient = MutableStateFlow(ProfileTransientState())

    private val legacyPlaylistState = combine(
        accountManager.accounts,
        accountManager.activeAccountId,
        accountManager.epgUrl,
        accountManager.m3uUrl,
        accountManager.activePlaylistSource,
    ) { accounts, activeAccountId, epgUrl, m3uUrl, activePlaylistSource ->
        ProfilePlaylistState(
            accounts = accounts,
            activeAccountId = activeAccountId.orEmpty(),
            epgUrl = epgUrl,
            m3uUrl = m3uUrl,
            activePlaylistSource = activePlaylistSource,
            playlistProfiles = emptyList(),
            activePlaylistProfileId = "",
        )
    }

    private val playlistState = combine(
        legacyPlaylistState,
        accountManager.profiles,
        accountManager.activeProfileId,
    ) { playlist, profiles, activeProfileId ->
        playlist.copy(
            playlistProfiles = profiles,
            activePlaylistProfileId = activeProfileId.orEmpty(),
        )
    }

    private val baseState = combine(
        activationRepository.localState,
        playlistState,
        catalogRepository.observeAccount(),
    ) { activation, playlist, account ->
        ProfileBaseState(
            activation = activation,
            accounts = playlist.accounts,
            activeAccountId = playlist.activeAccountId,
            account = account,
            epgUrl = playlist.epgUrl,
            m3uUrl = playlist.m3uUrl,
            activePlaylistSource = playlist.activePlaylistSource,
            playlistProfiles = playlist.playlistProfiles,
            activePlaylistProfileId = playlist.activePlaylistProfileId,
        )
    }

    val uiState = combine(
        baseState,
        transient,
    ) { base, transient ->
        val activation = base.activation
        val accounts = base.accounts
        val account = base.account
        val activeAccountId = base.activeAccountId
        val activeAccount = accounts.firstOrNull { it.id == activeAccountId } ?: accounts.firstOrNull()
        runCatching {
            buildProfileState(
                activation,
                accounts,
                activeAccountId,
                activeAccount,
                account,
                base.epgUrl,
                base.m3uUrl,
                base.activePlaylistSource,
                base.playlistProfiles,
                base.activePlaylistProfileId,
                transient,
            )
        }.getOrElse {
            ProfileUiState(
                deviceId = activation.deviceId,
                refreshing = transient.refreshing,
                licenseCode = transient.licenseCode,
                submittingLicense = transient.submittingLicense,
                errorMessage = "Impossible d'afficher toutes les informations du compte. Actualisez le statut.",
                qrDialog = transient.qrDialog,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProfileUiState(),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            transient.update { it.copy(refreshing = true, errorMessage = null) }
            try {
                runCatching {
                    activationRepository.getOrCreateDeviceId()
                    activationRepository.checkStatus()
                }.onFailure { error ->
                    transient.update {
                        it.copy(errorMessage = error.userMessage("Impossible de verifier le compte."))
                    }
                }
            } finally {
                transient.update { it.copy(refreshing = false) }
            }
        }
    }

    fun showLicenseQr() {
        viewModelScope.launch {
            val device = activationRepository.currentDisplayDevice()
            transient.update {
                it.copy(
                    qrDialog = ProfileQrState(
                        title = "Acheter ou prolonger la licence",
                        subtitle = "Scannez ce QR code avec votre telephone pour choisir une licence SmartVision. Le portail associera l'achat a cet appareil.",
                        url = "${activationBaseUrl()}account/?source=tv&intent=license&${tvDeviceQuery(device.publicDeviceCode, device.deviceId)}&plan=year_1",
                        allowsLicenseEntry = true,
                    ),
                )
            }
        }
    }

    fun updateLicenseCode(value: String) {
        transient.update {
            it.copy(
                licenseCode = value.replace(Regex("[\\s-]+"), "").uppercase().take(10),
                errorMessage = null,
            )
        }
    }

    fun activateLicense() {
        val code = transient.value.licenseCode
        if (!Regex("^[A-Z0-9]{10}$").matches(code)) {
            transient.update {
                it.copy(errorMessage = "Le code licence doit contenir exactement 10 caracteres.")
            }
            return
        }
        viewModelScope.launch {
            transient.update { it.copy(submittingLicense = true, errorMessage = null) }
            runCatching { activationRepository.activateLicense(code) }
                .onSuccess {
                    transient.update {
                        it.copy(
                            submittingLicense = false,
                            licenseCode = "",
                            qrDialog = null,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    transient.update {
                        it.copy(
                            submittingLicense = false,
                            errorMessage = error.userMessage("Activation de la licence impossible."),
                        )
                    }
                }
        }
    }

    fun showXtreamSetupQr() {
        transient.update {
            it.copy(
                qrDialog = ProfileQrState(
                    title = "Configuration Xtream",
                    subtitle = "Generation d'un lien securise pour renseigner ou remplacer les identifiants Xtream de cette TV.",
                    url = "",
                    loading = true,
                ),
            )
        }
        viewModelScope.launch {
            runCatching { activationRepository.createPlaylistSetupSession() }
                .onSuccess { session ->
                    transient.update {
                        it.copy(
                            qrDialog = ProfileQrState(
                                title = "Configurer Xtream sur telephone",
                                subtitle = "Scannez le QR code, saisissez host, utilisateur et mot de passe. La TV recevra les identifiants chiffres automatiquement.",
                                url = session.qrUrl,
                                code = session.shortCode,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    transient.update {
                        it.copy(
                            qrDialog = ProfileQrState(
                                title = "Configuration Xtream",
                                subtitle = "Le lien de configuration n'a pas pu etre genere.",
                                url = "",
                                error = error.userMessage("Lien de configuration indisponible."),
                            ),
                        )
                    }
                }
        }
    }

    fun showXtreamShopQr() {
        viewModelScope.launch {
            val device = activationRepository.currentDisplayDevice()
            transient.update {
                it.copy(
                    qrDialog = ProfileQrState(
                        title = "Acheter ou prolonger Xtream",
                        subtitle = "Scannez ce QR code pour continuer sur le portail. Le parcours Xtream sera finalise sur mobile, pas avec la telecommande.",
                        url = "${activationBaseUrl()}account/?source=tv&intent=xtream&${tvDeviceQuery(device.publicDeviceCode, device.deviceId)}",
                    ),
                )
            }
        }
    }

    fun dismissQr() {
        transient.update { it.copy(qrDialog = null, errorMessage = null) }
    }

    fun saveEpgUrl(url: String) {
        accountManager.updateEpgUrl(url)
    }

    fun saveM3uUrl(url: String) {
        accountManager.updateM3uUrl(url)
    }

    fun selectPlaylistSource(source: PlaylistSource) {
        accountManager.selectPlaylistSource(source)
    }
}

private data class ProfileBaseState(
    val activation: StoredActivationState,
    val accounts: List<XtreamAccount>,
    val activeAccountId: String,
    val account: AccountProfile,
    val epgUrl: String,
    val m3uUrl: String,
    val activePlaylistSource: PlaylistSource,
    val playlistProfiles: List<PlaylistProfile>,
    val activePlaylistProfileId: String,
)

private data class ProfilePlaylistState(
    val accounts: List<XtreamAccount>,
    val activeAccountId: String,
    val epgUrl: String,
    val m3uUrl: String,
    val activePlaylistSource: PlaylistSource,
    val playlistProfiles: List<PlaylistProfile>,
    val activePlaylistProfileId: String,
)

data class ProfileUiState(
    val deviceId: String = "",
    val publicDeviceCode: String = "",
    val activationStatusLabel: String = "Verification",
    val licenseExpiresAt: String = "",
    val usageMode: UsageMode = UsageMode.Unknown,
    val xtreamHost: String = "",
    val xtreamUsername: String = "",
    val epgUrl: String = "",
    val m3uUrl: String = "",
    val activePlaylistSource: PlaylistSource = PlaylistSource.Xtream,
    val playlistProfiles: List<PlaylistProfile> = emptyList(),
    val activePlaylistProfileId: String = "",
    val xtreamExpiresAt: String = "",
    val xtreamConnections: String = "",
    val hasXtream: Boolean = false,
    val xtreamAccounts: List<XtreamAccount> = emptyList(),
    val activeXtreamAccountId: String = "",
    val account: AccountProfile = emptyAccountProfile(),
    val refreshing: Boolean = false,
    val licenseCode: String = "",
    val submittingLicense: Boolean = false,
    val errorMessage: String? = null,
    val qrDialog: ProfileQrState? = null,
) {
    val tvCode: String = publicDeviceCode.ifBlank { "Generation..." }
    val activeXtreamAccount: XtreamAccount?
        get() = xtreamAccounts.firstOrNull { it.id == activeXtreamAccountId } ?: xtreamAccounts.firstOrNull()
}

data class ProfileQrState(
    val title: String,
    val subtitle: String,
    val url: String,
    val code: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val allowsLicenseEntry: Boolean = false,
)

private data class ProfileTransientState(
    val refreshing: Boolean = false,
    val licenseCode: String = "",
    val submittingLicense: Boolean = false,
    val errorMessage: String? = null,
    val qrDialog: ProfileQrState? = null,
)

enum class UsageMode(
    val label: String,
    val description: String,
    val renewalHint: String,
    val primaryCta: String,
    val color: Color,
) {
    Trial(
        label = "ESSAI GRATUIT",
        description = "Essai gratuit 7 jours sans publicite",
        renewalHint = "Acheter une licence avant la fin de l'essai pour rester sans publicite.",
        primaryCta = "Passer Premium",
        color = SmartVisionColors.CyanAccent,
    ),
    Premium(
        label = "PREMIUM",
        description = "Licence SmartVision payante active",
        renewalHint = "Prolongez depuis le portail si la licence approche de l'expiration.",
        primaryCta = "Prolonger",
        color = SmartVisionColors.Success,
    ),
    FreeAds(
        label = "GRATUIT AVEC PUBS",
        description = "Acces gratuit finance par la publicite",
        renewalHint = "Acheter une licence supprime les publicites.",
        primaryCta = "Supprimer les pubs",
        color = SmartVisionColors.Warning,
    ),
    Unknown(
        label = "COMPTE",
        description = "Statut de compte en verification",
        renewalHint = "Actualisez le statut si les informations semblent incorrectes.",
        primaryCta = "Acheter licence",
        color = SmartVisionColors.TextSecondary,
    ),
}

private fun buildProfileState(
    activation: StoredActivationState,
    accounts: List<XtreamAccount>,
    activeAccountId: String,
    activeAccount: XtreamAccount?,
    account: AccountProfile,
    epgUrl: String,
    m3uUrl: String,
    activePlaylistSource: PlaylistSource,
    playlistProfiles: List<PlaylistProfile>,
    activePlaylistProfileId: String,
    transient: ProfileTransientState,
): ProfileUiState {
    val usageMode = when (activation.monetizationStatus()) {
        MonetizationStatus.TRIAL_ACTIVE -> UsageMode.Trial
        MonetizationStatus.PREMIUM_ACTIVE -> UsageMode.Premium
        MonetizationStatus.FREE_WITH_ADS -> UsageMode.FreeAds
        MonetizationStatus.TRIAL_EXPIRED,
        MonetizationStatus.LICENSE_EXPIRED,
        null,
        -> UsageMode.Unknown
    }
    val activationStatusLabel = when (activation.monetizationStatus()) {
        MonetizationStatus.TRIAL_ACTIVE -> "Essai gratuit actif"
        MonetizationStatus.PREMIUM_ACTIVE -> "Premium actif"
        MonetizationStatus.FREE_WITH_ADS -> "Gratuit avec pubs"
        MonetizationStatus.TRIAL_EXPIRED -> "Essai expire"
        MonetizationStatus.LICENSE_EXPIRED -> "Licence expiree"
        null -> "Verification"
    }
    val hasXtream = activeAccount != null
    val xtreamUsername = account.usernameMasked.takeIf { it.isNotBlank() }
        ?: activeAccount?.username?.masked().orEmpty()
    val connections = if (account.activeConnections != null || account.maxConnections != null) {
        "${account.activeConnections ?: 0}/${account.maxConnections ?: 0}"
    } else {
        ""
    }
    return ProfileUiState(
        deviceId = activation.deviceId,
        publicDeviceCode = activation.publicDeviceCode,
        activationStatusLabel = activationStatusLabel,
        licenseExpiresAt = activation.expiresAt.orEmpty(),
        usageMode = usageMode,
        xtreamHost = account.host.ifBlank { activeAccount?.host.orEmpty() },
        xtreamUsername = xtreamUsername,
        epgUrl = epgUrl.ifBlank { activeAccount?.epgUrl.orEmpty() },
        m3uUrl = m3uUrl,
        activePlaylistSource = activePlaylistSource,
        playlistProfiles = playlistProfiles,
        activePlaylistProfileId = activePlaylistProfileId,
        xtreamExpiresAt = account.expirationDate.orEmpty(),
        xtreamConnections = connections,
        hasXtream = hasXtream,
        xtreamAccounts = accounts,
        activeXtreamAccountId = activeAccountId,
        account = account,
        refreshing = transient.refreshing,
        licenseCode = transient.licenseCode,
        submittingLicense = transient.submittingLicense,
        errorMessage = transient.errorMessage,
        qrDialog = transient.qrDialog,
    )
}

