package com.smartvision.svplayer.core.config

import android.content.Context
import com.smartvision.svplayer.core.profile.ProfileCredentialsStore
import com.smartvision.svplayer.core.profile.StoredProfileCredentials
import com.smartvision.svplayer.domain.profile.CatalogSyncFingerprint
import com.smartvision.svplayer.domain.profile.ContentPrefixPolicy
import java.util.UUID
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class XtreamAccount(
    val id: String,
    val name: String,
    val host: String,
    val username: String,
    val password: String,
    val epgUrl: String = "",
)

enum class PlaylistSource(val storageValue: String) {
    Xtream("xtream"),
    M3u("m3u");

    companion object {
        fun fromStorage(value: String?): PlaylistSource =
            entries.firstOrNull { it.storageValue == value } ?: Xtream
    }
}

enum class PlaylistProfileStatus(val storageValue: String) {
    Active("active"),
    Inactive("inactive"),
    Error("error"),
    NotConfigured("not_configured");

    companion object {
        fun fromStorage(value: String?): PlaylistProfileStatus =
            entries.firstOrNull { it.storageValue == value } ?: NotConfigured
    }
}

enum class ProfileType(val storageValue: String) {
    ADMIN("admin"),
    NORMAL("normal"),
    KIDS("kids");

    companion object {
        fun fromStorage(value: String?, fallback: ProfileType = NORMAL): ProfileType =
            entries.firstOrNull { it.storageValue == value } ?: fallback
    }
}

enum class CredentialsMode(val storageValue: String) {
    SHARED_WITH_ADMIN("shared_with_admin"),
    CUSTOM("custom");

    companion object {
        fun fromStorage(value: String?): CredentialsMode =
            entries.firstOrNull { it.storageValue == value } ?: CUSTOM
    }
}

data class PlaylistProfile(
    val id: String,
    val name: String,
    val source: PlaylistSource,
    val type: ProfileType = ProfileType.NORMAL,
    val credentialsMode: CredentialsMode = CredentialsMode.CUSTOM,
    val isLocked: Boolean = false,
    val avatarId: String = "",
    val avatarColorHex: String = "",
    val xtreamHost: String = "",
    val xtreamUsername: String = "",
    val xtreamPassword: String = "",
    val m3uUrl: String = "",
    val epgUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncAt: Long? = null,
    val selectedContentPrefixes: Set<String> = emptySet(),
    val detectedContentPrefixes: Set<String> = emptySet(),
    val lastCatalogFingerprint: String = "",
    val status: PlaylistProfileStatus = PlaylistProfileStatus.NotConfigured,
) {
    val isConfigured: Boolean
        get() = credentialsMode == CredentialsMode.SHARED_WITH_ADMIN || when (source) {
            PlaylistSource.Xtream -> xtreamHost.isNotBlank() && xtreamUsername.isNotBlank() && xtreamPassword.isNotBlank()
            PlaylistSource.M3u -> m3uUrl.isNotBlank()
        }
}

class XtreamAccountManager(context: Context) : XtreamCredentialsProvider {
    private val preferences = context.getSharedPreferences("xtream_accounts", Context.MODE_PRIVATE)
    private val credentialsStore = ProfileCredentialsStore(context)
    private val _accounts = MutableStateFlow(loadAccounts())
    val accounts: StateFlow<List<XtreamAccount>> = _accounts.asStateFlow()

    private val _activeAccountId = MutableStateFlow(
        preferences.getString(KEY_ACTIVE, null)
            ?.takeIf { id -> _accounts.value.any { it.id == id } }
            ?: _accounts.value.firstOrNull()?.id,
    )
    val activeAccountId: StateFlow<String?> = _activeAccountId.asStateFlow()

    private val _epgUrl = MutableStateFlow(preferences.getString(KEY_EPG_URL, "").orEmpty())
    val epgUrl: StateFlow<String> = _epgUrl.asStateFlow()

    private val _m3uUrl = MutableStateFlow(preferences.getString(KEY_M3U_URL, "").orEmpty())
    val m3uUrl: StateFlow<String> = _m3uUrl.asStateFlow()

    private val _activePlaylistSource = MutableStateFlow(
        PlaylistSource.fromStorage(preferences.getString(KEY_ACTIVE_PLAYLIST_SOURCE, null)),
    )
    val activePlaylistSource: StateFlow<PlaylistSource> = _activePlaylistSource.asStateFlow()

    private val _profiles = MutableStateFlow(loadProfiles())
    val profiles: StateFlow<List<PlaylistProfile>> = _profiles.asStateFlow()

    private val _activeProfileId = MutableStateFlow(
        preferences.getString(KEY_ACTIVE_PROFILE, null)
            ?.takeIf { id -> _profiles.value.any { it.id == id } }
            ?: _profiles.value.firstOrNull()?.id,
    )
    val activeProfileId: StateFlow<String?> = _activeProfileId.asStateFlow()

    init {
        migrateLegacyProfileIfNeeded()
        migrateProfileRolesIfNeeded()
        if (_activeProfileId.value == null) {
            _activeProfileId.value = _profiles.value.firstOrNull()?.id
        }
        activeProfile()?.let(::applyProfileToLegacyState)
        persist()
    }

    override fun current(): XtreamCredentials {
        val account = _accounts.value.firstOrNull { it.id == _activeAccountId.value }
            ?: _accounts.value.firstOrNull()
        return XtreamCredentials(
            host = account?.host.orEmpty(),
            username = account?.username.orEmpty(),
            password = account?.password.orEmpty(),
        )
    }

    @Synchronized
    fun upsert(account: XtreamAccount): String {
        val id = account.id.ifBlank { UUID.randomUUID().toString() }
        val normalized = account.copy(
            id = id,
            name = account.name.trim().ifBlank { account.username.trim() },
            host = account.host.trim().trimEnd('/'),
            username = account.username.trim(),
            password = account.password.trim(),
            epgUrl = account.epgUrl.trim(),
        )
        require(normalized.host.isNotBlank() && normalized.username.isNotBlank() && normalized.password.isNotBlank()) {
            "Hote, utilisateur et mot de passe sont obligatoires."
        }
        _accounts.value = (_accounts.value.filterNot { it.id == id } + normalized)
            .sortedBy { it.name.lowercase() }
        if (_activeAccountId.value == null) _activeAccountId.value = id
        if (normalized.epgUrl.isNotBlank()) _epgUrl.value = normalized.epgUrl
        ensureLegacyProfileExists()
        persist()
        return id
    }

    @Synchronized
    fun updateEpgUrl(url: String) {
        val normalizedUrl = url.trim()
        _epgUrl.value = normalizedUrl
        val activeId = _activeAccountId.value
        if (activeId != null) {
            _accounts.value = _accounts.value.map { account ->
                if (account.id == activeId) account.copy(epgUrl = normalizedUrl) else account
            }
        }
        ensureLegacyProfileExists()
        persist()
    }

    @Synchronized
    fun updateM3uUrl(url: String) {
        _m3uUrl.value = url.trim()
        if (_m3uUrl.value.isNotBlank() && !_activePlaylistSource.value.isAvailable()) {
            _activePlaylistSource.value = PlaylistSource.M3u
        }
        ensureLegacyProfileExists()
        persist()
    }

    @Synchronized
    fun selectPlaylistSource(source: PlaylistSource) {
        _activePlaylistSource.value = source
        ensureLegacyProfileExists()
        persist()
    }

    @Synchronized
    fun delete(accountId: String) {
        _accounts.value = _accounts.value.filterNot { it.id == accountId }
        if (_activeAccountId.value == accountId) {
            _activeAccountId.value = _accounts.value.firstOrNull()?.id
        }
        ensureLegacyProfileExists()
        persist()
    }

    @Synchronized
    fun select(accountId: String) {
        require(_accounts.value.any { it.id == accountId }) { "Compte Xtream introuvable." }
        _activeAccountId.value = accountId
        if (_profiles.value.any { it.id == accountId }) {
            activateProfile(accountId)
            return
        }
        ensureLegacyProfileExists()
        persist()
    }

    @Synchronized
    fun upsertProfile(profile: PlaylistProfile): String {
        val now = System.currentTimeMillis()
        val id = profile.id.ifBlank { UUID.randomUUID().toString() }
        val existing = _profiles.value.firstOrNull { it.id == id }
        val normalizedType = when {
            existing?.type == ProfileType.ADMIN -> ProfileType.ADMIN
            profile.type == ProfileType.ADMIN && _profiles.value.any { it.id != id && it.type == ProfileType.ADMIN } ->
                throw IllegalArgumentException("Un profil administrateur existe deja.")
            _profiles.value.isEmpty() -> ProfileType.ADMIN
            else -> profile.type
        }
        val normalizedCredentialsMode = if (normalizedType == ProfileType.ADMIN) {
            CredentialsMode.CUSTOM
        } else {
            profile.credentialsMode
        }
        val requestedAvatarId = profile.avatarId.ifBlank { existing?.avatarId.orEmpty() }
        val normalized = profile.copy(
            id = id,
            name = profile.name.trim(),
            type = normalizedType,
            credentialsMode = normalizedCredentialsMode,
            avatarId = if (requestedAvatarId.isBlank()) {
                defaultProfileAvatarId(
                    type = normalizedType,
                    stableKey = id.takeIf { existing != null || normalizedType != ProfileType.KIDS },
                )
            } else {
                canonicalProfileAvatarId(requestedAvatarId, normalizedType)
            },
            avatarColorHex = profile.avatarColorHex.ifBlank { avatarColorForName(profile.name.ifBlank { id }) },
            xtreamHost = if (normalizedCredentialsMode == CredentialsMode.CUSTOM) profile.xtreamHost.trim().trimEnd('/') else "",
            xtreamUsername = if (normalizedCredentialsMode == CredentialsMode.CUSTOM) profile.xtreamUsername.trim() else "",
            xtreamPassword = if (normalizedCredentialsMode == CredentialsMode.CUSTOM) profile.xtreamPassword.trim() else "",
            m3uUrl = if (normalizedCredentialsMode == CredentialsMode.CUSTOM) profile.m3uUrl.trim() else "",
            epgUrl = if (normalizedCredentialsMode == CredentialsMode.CUSTOM) profile.epgUrl.trim() else "",
            selectedContentPrefixes = ContentPrefixPolicy.normalize(profile.selectedContentPrefixes),
            detectedContentPrefixes = ContentPrefixPolicy.normalize(
                profile.detectedContentPrefixes.ifEmpty { existing?.detectedContentPrefixes.orEmpty() },
            ),
            lastCatalogFingerprint = profile.lastCatalogFingerprint.ifBlank {
                existing?.lastCatalogFingerprint.orEmpty()
            },
            createdAt = profile.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now,
        )
        require(normalized.name.isNotBlank()) { "Le nom du profil est obligatoire." }
        require(normalized.isConfigured) { "La source du profil est incomplete." }
        if (normalized.credentialsMode == CredentialsMode.SHARED_WITH_ADMIN) {
            require(adminProfile()?.isConfigured == true) { "Le profil administrateur n'est pas configure." }
        }
        require(_profiles.value.none { it.id != id && it.name.equals(normalized.name, ignoreCase = true) }) {
            "Un profil porte deja ce nom."
        }
        if (normalized.credentialsMode == CredentialsMode.CUSTOM) {
            credentialsStore.put(id, normalized.toStoredCredentials())
        } else {
            credentialsStore.delete(id)
        }
        val wasEmpty = _profiles.value.isEmpty()
        val existingWasActive = _activeProfileId.value == id
        _profiles.value = (_profiles.value.filterNot { it.id == id } + normalized)
            .sortedBy { it.createdAt }
        if (_activeProfileId.value == null || wasEmpty) {
            _activeProfileId.value = id
        }
        refreshProfileStatuses()
        val activeUsesAdminCredentials = activeProfile()?.credentialsMode == CredentialsMode.SHARED_WITH_ADMIN
        if (existingWasActive || _activeProfileId.value == id || (normalized.type == ProfileType.ADMIN && activeUsesAdminCredentials)) {
            activeProfile()?.let(::applyProfileToLegacyState)
        }
        persist()
        return id
    }

    @Synchronized
    fun upsertWebPlaylistProfile(
        sourceHint: PlaylistSource?,
        xtreamHost: String,
        xtreamUsername: String,
        xtreamPassword: String,
        m3uUrl: String,
        epgUrl: String,
        epgProvided: Boolean = true,
    ): String? {
        val existing = _profiles.value.firstOrNull {
            it.name.equals(WebPlaylistProfileName, ignoreCase = true)
        }
        val normalizedHost = xtreamHost.trim().trimEnd('/').ifBlank { existing?.xtreamHost.orEmpty() }
        val normalizedUsername = xtreamUsername.trim().ifBlank { existing?.xtreamUsername.orEmpty() }
        val normalizedPassword = xtreamPassword.trim().ifBlank { existing?.xtreamPassword.orEmpty() }
        val normalizedM3u = m3uUrl.trim().ifBlank { existing?.m3uUrl.orEmpty() }
        val normalizedEpg = if (epgProvided) epgUrl.trim() else existing?.epgUrl.orEmpty()
        val hasXtream = normalizedHost.isNotBlank() &&
            normalizedUsername.isNotBlank() &&
            normalizedPassword.isNotBlank()
        val hasM3u = normalizedM3u.isNotBlank()
        if (!hasXtream && !hasM3u) return null

        val source = when {
            sourceHint == PlaylistSource.M3u && hasM3u -> PlaylistSource.M3u
            sourceHint == PlaylistSource.Xtream && hasXtream -> PlaylistSource.Xtream
            existing?.source == PlaylistSource.M3u && hasM3u -> PlaylistSource.M3u
            hasXtream -> PlaylistSource.Xtream
            else -> PlaylistSource.M3u
        }
        val sourceChanged = existing == null ||
            existing.source != source ||
            existing.xtreamHost.trim().trimEnd('/') != normalizedHost ||
            existing.xtreamUsername.trim() != normalizedUsername ||
            existing.xtreamPassword.trim() != normalizedPassword ||
            existing.m3uUrl.trim() != normalizedM3u
        val now = System.currentTimeMillis()
        val profileId = upsertProfile(
            PlaylistProfile(
                id = existing?.id.orEmpty(),
                name = WebPlaylistProfileName,
                source = source,
                avatarId = existing?.avatarId ?: defaultProfileAvatarId(ProfileType.NORMAL, WebPlaylistProfileName),
                avatarColorHex = existing?.avatarColorHex ?: avatarColorForName(WebPlaylistProfileName),
                xtreamHost = normalizedHost,
                xtreamUsername = normalizedUsername,
                xtreamPassword = normalizedPassword,
                m3uUrl = normalizedM3u,
                epgUrl = normalizedEpg,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                lastSyncAt = if (sourceChanged) null else existing?.lastSyncAt,
                selectedContentPrefixes = existing?.selectedContentPrefixes.orEmpty(),
                detectedContentPrefixes = existing?.detectedContentPrefixes.orEmpty(),
                lastCatalogFingerprint = if (sourceChanged) "" else existing?.lastCatalogFingerprint.orEmpty(),
            ),
        )
        return profileId
    }

    @Synchronized
    fun applyWebPlaylistDelivery(delivery: WebPlaylistDelivery): List<String> {
        val appliedIds = mutableListOf<String>()
        val xtreamProvided = "xtream" in delivery.providedFields
        val m3uProvided = "m3u" in delivery.providedFields
        val epgProvided = "epg" in delivery.providedFields

        delivery.targetProfileIds.distinct().forEach { targetId ->
            val existing = _profiles.value.firstOrNull { it.id == targetId && isEligibleWebPlaylistTarget(it) }
                ?: return@forEach
            val resolved = resolvedProfile(existing)
            val source = when {
                xtreamProvided -> PlaylistSource.Xtream
                m3uProvided -> PlaylistSource.M3u
                else -> resolved.source
            }
            val updated = existing.copy(
                source = source,
                credentialsMode = CredentialsMode.CUSTOM,
                xtreamHost = if (xtreamProvided) delivery.xtreamHost else resolved.xtreamHost,
                xtreamUsername = if (xtreamProvided) delivery.xtreamUsername else resolved.xtreamUsername,
                xtreamPassword = if (xtreamProvided) delivery.xtreamPassword else resolved.xtreamPassword,
                m3uUrl = if (m3uProvided) delivery.m3uUrl else resolved.m3uUrl,
                epgUrl = if (epgProvided) delivery.epgUrl else resolved.epgUrl,
                lastSyncAt = if (
                    source != resolved.source ||
                    (xtreamProvided && (
                        delivery.xtreamHost.trim().trimEnd('/') != resolved.xtreamHost.trim().trimEnd('/') ||
                            delivery.xtreamUsername.trim() != resolved.xtreamUsername.trim() ||
                            delivery.xtreamPassword.trim() != resolved.xtreamPassword.trim()
                        )) ||
                    (m3uProvided && delivery.m3uUrl.trim() != resolved.m3uUrl.trim())
                ) null else existing.lastSyncAt,
            )
            appliedIds += upsertProfile(updated)
        }

        val requestedNewName = delivery.newProfileName.trim()
        if (requestedNewName.isNotBlank() && (xtreamProvided || m3uProvided)) {
            val uniqueName = uniqueWebProfileName(requestedNewName)
            val source = if (m3uProvided && !xtreamProvided) PlaylistSource.M3u else PlaylistSource.Xtream
            appliedIds += upsertProfile(
                PlaylistProfile(
                    id = "",
                    name = uniqueName,
                    source = source,
                    type = ProfileType.NORMAL,
                    credentialsMode = CredentialsMode.CUSTOM,
                    avatarId = defaultProfileAvatarId(ProfileType.NORMAL, uniqueName),
                    avatarColorHex = avatarColorForName(uniqueName),
                    xtreamHost = delivery.xtreamHost,
                    xtreamUsername = delivery.xtreamUsername,
                    xtreamPassword = delivery.xtreamPassword,
                    m3uUrl = delivery.m3uUrl,
                    epgUrl = if (epgProvided) delivery.epgUrl else "",
                    lastSyncAt = null,
                ),
            )
        }
        return appliedIds
    }

    private fun uniqueWebProfileName(requestedName: String): String {
        return uniqueProfileName(requestedName, _profiles.value.map { it.name }, WebPlaylistProfileName)
    }

    @Synchronized
    fun activateProfile(profileId: String) {
        val profile = _profiles.value.firstOrNull { it.id == profileId }
            ?: throw IllegalArgumentException("Profil introuvable.")
        if (_activeProfileId.value == profile.id) {
            applyProfileToLegacyState(profile)
            return
        }
        _activeProfileId.value = profile.id
        applyProfileToLegacyState(profile)
        refreshProfileStatuses()
        persist()
    }

    @Synchronized
    fun selectProfile(profileId: String) = activateProfile(profileId)

    @Synchronized
    fun updateActiveProfileSource(source: PlaylistSource) {
        val activeId = _activeProfileId.value
        val now = System.currentTimeMillis()
        _activePlaylistSource.value = source
        if (activeId != null) {
            _profiles.value = _profiles.value.map { profile ->
                if (profile.id == activeId) profile.copy(source = source, updatedAt = now) else profile
            }
            refreshProfileStatuses()
        } else {
            ensureLegacyProfileExists()
        }
        activeProfile()?.let(::applyProfileToLegacyState)
        persist()
    }

    @Synchronized
    fun deleteProfile(profileId: String) {
        requireProfileDeletionAllowed(_profiles.value.firstOrNull { it.id == profileId })
        _profiles.value = _profiles.value.filterNot { it.id == profileId }
        credentialsStore.delete(profileId)
        if (_activeProfileId.value == profileId) {
            _activeProfileId.value = _profiles.value.firstOrNull()?.id
        }
        activeProfile()?.let(::applyProfileToLegacyState) ?: clearLegacySourceState()
        refreshProfileStatuses()
        persist()
    }

    @Synchronized
    fun markActiveProfileSynced(syncAt: Long = System.currentTimeMillis()) {
        markProfileSynced(_activeProfileId.value ?: return, syncAt)
    }

    @Synchronized
    fun markProfileSynced(profileId: String, syncAt: Long = System.currentTimeMillis()) {
        _profiles.value = _profiles.value.map { profile ->
            if (profile.id == profileId) {
                profile.copy(
                    lastSyncAt = syncAt,
                    updatedAt = syncAt,
                    lastCatalogFingerprint = CatalogSyncFingerprint.create(resolvedProfile(profile)),
                )
            } else {
                profile
            }
        }
        refreshProfileStatuses()
        persist()
    }

    @Synchronized
    fun recordDetectedContentPrefixes(profileId: String, codes: Set<String>) {
        val normalized = ContentPrefixPolicy.normalize(codes)
        if (normalized.isEmpty()) return
        var changed = false
        _profiles.value = _profiles.value.map { profile ->
            if (profile.id != profileId) return@map profile
            val merged = profile.detectedContentPrefixes + normalized
            if (merged == profile.detectedContentPrefixes) profile else {
                changed = true
                profile.copy(detectedContentPrefixes = merged)
            }
        }
        if (changed) persist()
    }

    fun isCatalogCurrent(profile: PlaylistProfile): Boolean {
        val resolved = resolvedProfile(profile)
        return profile.lastCatalogFingerprint.isNotBlank() &&
            profile.lastCatalogFingerprint == CatalogSyncFingerprint.create(resolved)
    }

    fun activeProfile(): PlaylistProfile? =
        _profiles.value.firstOrNull { it.id == _activeProfileId.value }

    fun adminProfile(): PlaylistProfile? =
        _profiles.value.firstOrNull { it.type == ProfileType.ADMIN }

    fun resolvedProfile(profile: PlaylistProfile): PlaylistProfile {
        if (profile.credentialsMode != CredentialsMode.SHARED_WITH_ADMIN) return profile
        val admin = adminProfile() ?: return profile
        return profile.copy(
            source = admin.source,
            xtreamHost = admin.xtreamHost,
            xtreamUsername = admin.xtreamUsername,
            xtreamPassword = admin.xtreamPassword,
            m3uUrl = admin.m3uUrl,
            epgUrl = admin.epgUrl,
        )
    }

    fun activeProfileIdOrDefault(): String =
        _activeProfileId.value ?: DefaultProfileId

    private fun migrateLegacyProfileIfNeeded() {
        if (_profiles.value.isNotEmpty()) return
        val activeAccount = _accounts.value.firstOrNull { it.id == _activeAccountId.value }
            ?: _accounts.value.firstOrNull()
        val hasM3u = _m3uUrl.value.isNotBlank()
        if (activeAccount == null && !hasM3u) return
        val source = when {
            _activePlaylistSource.value == PlaylistSource.M3u && hasM3u -> PlaylistSource.M3u
            activeAccount != null -> PlaylistSource.Xtream
            else -> PlaylistSource.M3u
        }
        val now = System.currentTimeMillis()
        val profile = PlaylistProfile(
            id = UUID.randomUUID().toString(),
            name = "Profil principal",
            source = source,
            type = ProfileType.ADMIN,
            credentialsMode = CredentialsMode.CUSTOM,
            avatarId = AdminProfileAvatarId,
            avatarColorHex = avatarColorForName("Profil principal"),
            xtreamHost = activeAccount?.host.orEmpty(),
            xtreamUsername = activeAccount?.username.orEmpty(),
            xtreamPassword = activeAccount?.password.orEmpty(),
            m3uUrl = _m3uUrl.value,
            epgUrl = _epgUrl.value.ifBlank { activeAccount?.epgUrl.orEmpty() },
            createdAt = now,
            updatedAt = now,
        )
        credentialsStore.put(profile.id, profile.toStoredCredentials())
        _profiles.value = listOf(profile)
        _activeProfileId.value = profile.id
        refreshProfileStatuses()
    }

    private fun migrateProfileRolesIfNeeded() {
        _profiles.value = normalizeProfileRoles(_profiles.value)
    }

    private fun ensureLegacyProfileExists() {
        if (_profiles.value.isEmpty()) {
            migrateLegacyProfileIfNeeded()
        }
        refreshProfileStatuses()
    }

    private fun applyProfileToLegacyState(profile: PlaylistProfile) {
        val resolved = resolvedProfile(profile)
        _activePlaylistSource.value = resolved.source
        _epgUrl.value = resolved.epgUrl
        _m3uUrl.value = resolved.m3uUrl
        if (resolved.source == PlaylistSource.Xtream && resolved.xtreamHost.isNotBlank()) {
            val account = XtreamAccount(
                id = profile.id,
                name = profile.name,
                host = resolved.xtreamHost,
                username = resolved.xtreamUsername,
                password = resolved.xtreamPassword,
                epgUrl = resolved.epgUrl,
            )
            _accounts.value = listOf(account)
            _activeAccountId.value = account.id
        } else {
            _accounts.value = emptyList()
            _activeAccountId.value = null
        }
    }

    private fun clearLegacySourceState() {
        _accounts.value = emptyList()
        _activeAccountId.value = null
        _m3uUrl.value = ""
        _epgUrl.value = ""
        _activePlaylistSource.value = PlaylistSource.Xtream
    }

    private fun refreshProfileStatuses() {
        val activeId = _activeProfileId.value
        _profiles.value = _profiles.value.map { profile ->
            val status = when {
                !resolvedProfile(profile).isConfigured -> PlaylistProfileStatus.NotConfigured
                profile.id == activeId -> PlaylistProfileStatus.Active
                else -> PlaylistProfileStatus.Inactive
            }
            profile.copy(status = status)
        }
    }

    private fun persist() {
        val json = JSONArray().apply {
            if (_profiles.value.isEmpty()) _accounts.value.forEach { account ->
                put(
                    JSONObject()
                        .put("id", account.id)
                        .put("name", account.name)
                        .put("host", account.host)
                        .put("username", account.username)
                        .put("password", account.password)
                        .put("epg_url", account.epgUrl),
                )
            }
        }
        preferences.edit()
            .putString(KEY_ACCOUNTS, json.toString())
            .putString(KEY_ACTIVE, _activeAccountId.value)
            .putString(KEY_EPG_URL, _epgUrl.value)
            .putString(KEY_M3U_URL, _m3uUrl.value)
            .putString(KEY_ACTIVE_PLAYLIST_SOURCE, _activePlaylistSource.value.storageValue)
            .putString(KEY_PROFILES, profilesToJson(_profiles.value).toString())
            .putString(KEY_ACTIVE_PROFILE, _activeProfileId.value)
            .apply()
    }

    private fun loadAccounts(): List<XtreamAccount> = runCatching {
        val json = JSONArray(preferences.getString(KEY_ACCOUNTS, "[]"))
        (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            XtreamAccount(
                id = item.getString("id"),
                name = item.optString("name"),
                host = item.optString("host"),
                username = item.optString("username"),
                password = item.optString("password"),
                epgUrl = item.optString("epg_url"),
            )
        }.filter {
            it.id != LEGACY_BUILD_CONFIG_ACCOUNT &&
                it.host.isNotBlank() && it.username.isNotBlank() && it.password.isNotBlank()
        }
    }.getOrDefault(emptyList())

    private fun loadProfiles(): List<PlaylistProfile> = runCatching {
        val json = JSONArray(preferences.getString(KEY_PROFILES, "[]"))
        (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            val profileId = item.getString("id")
            val stored = credentialsStore.get(profileId)
            val legacy = StoredProfileCredentials(
                xtreamHost = item.optString("xtream_host"),
                xtreamUsername = item.optString("xtream_username"),
                xtreamPassword = item.optString("xtream_password"),
                m3uUrl = item.optString("m3u_url"),
                epgUrl = item.optString("epg_url"),
            )
            val credentials = stored ?: legacy.also {
                if (it.hasValues()) credentialsStore.put(profileId, it)
            }
            val profileType = ProfileType.fromStorage(
                item.optString("profile_type"),
                fallback = if (index == 0) ProfileType.ADMIN else ProfileType.NORMAL,
            )
            PlaylistProfile(
                id = profileId,
                name = item.optString("name"),
                source = PlaylistSource.fromStorage(item.optString("source")),
                type = profileType,
                credentialsMode = CredentialsMode.fromStorage(item.optString("credentials_mode")),
                isLocked = item.optBoolean("is_locked", false),
                avatarId = canonicalProfileAvatarId(
                    avatarId = item.optString("avatar_id").ifBlank {
                        defaultProfileAvatarId(profileType, profileId)
                    },
                    type = profileType,
                ),
                avatarColorHex = item.optString("avatar_color_hex").ifBlank {
                    avatarColorForName(item.optString("name").ifBlank { item.getString("id") })
                },
                xtreamHost = credentials.xtreamHost,
                xtreamUsername = credentials.xtreamUsername,
                xtreamPassword = credentials.xtreamPassword,
                m3uUrl = credentials.m3uUrl,
                epgUrl = credentials.epgUrl,
                createdAt = item.optLong("created_at", System.currentTimeMillis()),
                updatedAt = item.optLong("updated_at", System.currentTimeMillis()),
                lastSyncAt = item.takeIf { it.has("last_sync_at") && !it.isNull("last_sync_at") }?.optLong("last_sync_at"),
                selectedContentPrefixes = item.optStringSet("selected_content_prefixes"),
                detectedContentPrefixes = item.optStringSet("detected_content_prefixes"),
                lastCatalogFingerprint = item.optString("last_catalog_fingerprint"),
                status = PlaylistProfileStatus.fromStorage(item.optString("status")),
            )
        }.filter { it.id.isNotBlank() && it.name.isNotBlank() }
    }.getOrDefault(emptyList())

    private fun profilesToJson(profiles: List<PlaylistProfile>): JSONArray =
        JSONArray().apply {
            profiles.forEach { profile ->
                put(
                    JSONObject()
                        .put("id", profile.id)
                        .put("name", profile.name)
                        .put("source", profile.source.storageValue)
                        .put("profile_type", profile.type.storageValue)
                        .put("credentials_mode", profile.credentialsMode.storageValue)
                        .put("is_locked", profile.isLocked)
                        .put(
                            "avatar_id",
                            canonicalProfileAvatarId(
                                avatarId = profile.avatarId.ifBlank {
                                    defaultProfileAvatarId(profile.type, profile.id)
                                },
                                type = profile.type,
                            ),
                        )
                        .put("avatar_color_hex", profile.avatarColorHex.ifBlank { avatarColorForName(profile.name) })
                        .put("created_at", profile.createdAt)
                        .put("updated_at", profile.updatedAt)
                        .put("last_sync_at", profile.lastSyncAt)
                        .put("selected_content_prefixes", JSONArray(profile.selectedContentPrefixes.sorted()))
                        .put("detected_content_prefixes", JSONArray(profile.detectedContentPrefixes.sorted()))
                        .put("last_catalog_fingerprint", profile.lastCatalogFingerprint)
                        .put("status", profile.status.storageValue),
                )
            }
        }

    private companion object {
        const val KEY_ACCOUNTS = "accounts_json"
        const val KEY_ACTIVE = "active_account_id"
        const val KEY_EPG_URL = "epg_url"
        const val KEY_M3U_URL = "m3u_url"
        const val KEY_ACTIVE_PLAYLIST_SOURCE = "active_playlist_source"
        const val KEY_PROFILES = "playlist_profiles_json"
        const val KEY_ACTIVE_PROFILE = "active_playlist_profile_id"
        const val LEGACY_BUILD_CONFIG_ACCOUNT = "build_config"
        const val DefaultProfileId = "default"
        const val WebPlaylistProfileName = "PlaylistWeb"
    }

    private fun PlaylistSource.isAvailable(): Boolean =
        when (this) {
            PlaylistSource.Xtream -> current().isConfigured
            PlaylistSource.M3u -> _m3uUrl.value.isNotBlank()
        }
}

private fun JSONObject.optStringSet(key: String): Set<String> {
    val values = optJSONArray(key) ?: return emptySet()
    return buildSet {
        repeat(values.length()) { index ->
            values.optString(index).takeIf(String::isNotBlank)?.let(::add)
        }
    }
}

data class WebPlaylistDelivery(
    val targetProfileIds: List<String>,
    val newProfileName: String,
    val providedFields: Set<String>,
    val xtreamHost: String,
    val xtreamUsername: String,
    val xtreamPassword: String,
    val m3uUrl: String,
    val epgUrl: String,
)

internal fun isEligibleWebPlaylistTarget(profile: PlaylistProfile): Boolean = profile.type != ProfileType.KIDS

internal fun uniqueProfileName(requestedName: String, existingNames: List<String>, fallback: String): String {
    val base = requestedName.trim().ifBlank { fallback }
    val names = existingNames.map { it.lowercase(Locale.ROOT) }.toSet()
    if (base.lowercase(Locale.ROOT) !in names) return base
    var suffix = 2
    while ("$base ($suffix)".lowercase(Locale.ROOT) in names) suffix++
    return "$base ($suffix)"
}

private fun PlaylistProfile.toStoredCredentials(): StoredProfileCredentials = StoredProfileCredentials(
    xtreamHost = xtreamHost,
    xtreamUsername = xtreamUsername,
    xtreamPassword = xtreamPassword,
    m3uUrl = m3uUrl,
    epgUrl = epgUrl,
)

private fun StoredProfileCredentials.hasValues(): Boolean =
    xtreamHost.isNotBlank() || xtreamUsername.isNotBlank() || xtreamPassword.isNotBlank() ||
        m3uUrl.isNotBlank() || epgUrl.isNotBlank()

internal fun normalizeProfileRoles(profiles: List<PlaylistProfile>): List<PlaylistProfile> {
    if (profiles.isEmpty()) return profiles
    val ordered = profiles.sortedBy { it.createdAt }
    val selectedAdminId = ordered.firstOrNull { it.type == ProfileType.ADMIN }?.id ?: ordered.first().id
    return ordered.map { profile ->
        when {
            profile.id == selectedAdminId -> profile.copy(
                type = ProfileType.ADMIN,
                credentialsMode = CredentialsMode.CUSTOM,
            )
            profile.type == ProfileType.ADMIN -> profile.copy(type = ProfileType.NORMAL)
            else -> profile
        }
    }
}

internal fun requireProfileDeletionAllowed(profile: PlaylistProfile?) {
    require(profile != null) { "Profil introuvable." }
    require(profile.type != ProfileType.ADMIN) { "Le profil administrateur ne peut pas etre supprime." }
}

private val ProfileAvatarPalette = listOf(
    "#1D7AF3",
    "#D9352A",
    "#7B3FF2",
    "#009B72",
    "#E08A00",
    "#C2255C",
    "#2F80ED",
    "#6C8E00",
)

private fun avatarColorForName(value: String): String {
    val key = value.trim().ifBlank { "profile" }
    val index = kotlin.math.abs(key.lowercase().hashCode()).mod(ProfileAvatarPalette.size)
    return ProfileAvatarPalette[index]
}
