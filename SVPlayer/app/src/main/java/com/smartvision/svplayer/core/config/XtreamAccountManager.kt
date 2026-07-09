package com.smartvision.svplayer.core.config

import android.content.Context
import java.util.UUID
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

data class PlaylistProfile(
    val id: String,
    val name: String,
    val source: PlaylistSource,
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
    val status: PlaylistProfileStatus = PlaylistProfileStatus.NotConfigured,
) {
    val isConfigured: Boolean
        get() = when (source) {
            PlaylistSource.Xtream -> xtreamHost.isNotBlank() && xtreamUsername.isNotBlank() && xtreamPassword.isNotBlank()
            PlaylistSource.M3u -> m3uUrl.isNotBlank()
        }
}

class XtreamAccountManager(context: Context) : XtreamCredentialsProvider {
    private val preferences = context.getSharedPreferences("xtream_accounts", Context.MODE_PRIVATE)
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
        val normalized = profile.copy(
            id = id,
            name = profile.name.trim(),
            avatarId = profile.avatarId.ifBlank { profileAvatarIdForName(profile.name.ifBlank { id }) },
            avatarColorHex = profile.avatarColorHex.ifBlank { avatarColorForName(profile.name.ifBlank { id }) },
            xtreamHost = profile.xtreamHost.trim().trimEnd('/'),
            xtreamUsername = profile.xtreamUsername.trim(),
            xtreamPassword = profile.xtreamPassword.trim(),
            m3uUrl = profile.m3uUrl.trim(),
            epgUrl = profile.epgUrl.trim(),
            createdAt = profile.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now,
        )
        require(normalized.name.isNotBlank()) { "Le nom du profil est obligatoire." }
        require(normalized.isConfigured) { "La source du profil est incomplete." }
        require(_profiles.value.none { it.id != id && it.name.equals(normalized.name, ignoreCase = true) }) {
            "Un profil porte deja ce nom."
        }
        val wasEmpty = _profiles.value.isEmpty()
        val existingWasActive = _activeProfileId.value == id
        _profiles.value = (_profiles.value.filterNot { it.id == id } + normalized)
            .sortedBy { it.createdAt }
        if (_activeProfileId.value == null || wasEmpty) {
            _activeProfileId.value = id
        }
        refreshProfileStatuses()
        if (existingWasActive || _activeProfileId.value == id) {
            activeProfile()?.let(::applyProfileToLegacyState)
        }
        persist()
        return id
    }

    @Synchronized
    fun activateProfile(profileId: String) {
        val profile = _profiles.value.firstOrNull { it.id == profileId }
            ?: throw IllegalArgumentException("Profil introuvable.")
        if (_activeProfileId.value == profile.id) return
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
        _profiles.value = _profiles.value.filterNot { it.id == profileId }
        if (_activeProfileId.value == profileId) {
            _activeProfileId.value = _profiles.value.firstOrNull()?.id
        }
        activeProfile()?.let(::applyProfileToLegacyState) ?: clearLegacySourceState()
        refreshProfileStatuses()
        persist()
    }

    @Synchronized
    fun markActiveProfileSynced(syncAt: Long = System.currentTimeMillis()) {
        val activeId = _activeProfileId.value ?: return
        _profiles.value = _profiles.value.map { profile ->
            if (profile.id == activeId) {
                profile.copy(lastSyncAt = syncAt, updatedAt = syncAt)
            } else {
                profile
            }
        }
        refreshProfileStatuses()
        persist()
    }

    fun activeProfile(): PlaylistProfile? =
        _profiles.value.firstOrNull { it.id == _activeProfileId.value }

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
            avatarId = profileAvatarIdForName("Profil principal"),
            avatarColorHex = avatarColorForName("Profil principal"),
            xtreamHost = activeAccount?.host.orEmpty(),
            xtreamUsername = activeAccount?.username.orEmpty(),
            xtreamPassword = activeAccount?.password.orEmpty(),
            m3uUrl = _m3uUrl.value,
            epgUrl = _epgUrl.value.ifBlank { activeAccount?.epgUrl.orEmpty() },
            createdAt = now,
            updatedAt = now,
        )
        _profiles.value = listOf(profile)
        _activeProfileId.value = profile.id
        refreshProfileStatuses()
    }

    private fun ensureLegacyProfileExists() {
        if (_profiles.value.isEmpty()) {
            migrateLegacyProfileIfNeeded()
        }
        refreshProfileStatuses()
    }

    private fun applyProfileToLegacyState(profile: PlaylistProfile) {
        _activePlaylistSource.value = profile.source
        _epgUrl.value = profile.epgUrl
        _m3uUrl.value = profile.m3uUrl
        if (profile.source == PlaylistSource.Xtream && profile.xtreamHost.isNotBlank()) {
            val account = XtreamAccount(
                id = profile.id,
                name = profile.name,
                host = profile.xtreamHost,
                username = profile.xtreamUsername,
                password = profile.xtreamPassword,
                epgUrl = profile.epgUrl,
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
                !profile.isConfigured -> PlaylistProfileStatus.NotConfigured
                profile.id == activeId -> PlaylistProfileStatus.Active
                else -> PlaylistProfileStatus.Inactive
            }
            profile.copy(status = status)
        }
    }

    private fun persist() {
        val json = JSONArray().apply {
            _accounts.value.forEach { account ->
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
            PlaylistProfile(
                id = item.getString("id"),
                name = item.optString("name"),
                source = PlaylistSource.fromStorage(item.optString("source")),
                avatarId = item.optString("avatar_id").ifBlank {
                    profileAvatarIdForName(item.optString("name").ifBlank { item.getString("id") })
                },
                avatarColorHex = item.optString("avatar_color_hex").ifBlank {
                    avatarColorForName(item.optString("name").ifBlank { item.getString("id") })
                },
                xtreamHost = item.optString("xtream_host"),
                xtreamUsername = item.optString("xtream_username"),
                xtreamPassword = item.optString("xtream_password"),
                m3uUrl = item.optString("m3u_url"),
                epgUrl = item.optString("epg_url"),
                createdAt = item.optLong("created_at", System.currentTimeMillis()),
                updatedAt = item.optLong("updated_at", System.currentTimeMillis()),
                lastSyncAt = item.takeIf { it.has("last_sync_at") && !it.isNull("last_sync_at") }?.optLong("last_sync_at"),
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
                        .put("avatar_id", profile.avatarId.ifBlank { profileAvatarIdForName(profile.name) })
                        .put("avatar_color_hex", profile.avatarColorHex.ifBlank { avatarColorForName(profile.name) })
                        .put("xtream_host", profile.xtreamHost)
                        .put("xtream_username", profile.xtreamUsername)
                        .put("xtream_password", profile.xtreamPassword)
                        .put("m3u_url", profile.m3uUrl)
                        .put("epg_url", profile.epgUrl)
                        .put("created_at", profile.createdAt)
                        .put("updated_at", profile.updatedAt)
                        .put("last_sync_at", profile.lastSyncAt)
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
    }

    private fun PlaylistSource.isAvailable(): Boolean =
        when (this) {
            PlaylistSource.Xtream -> current().isConfigured
            PlaylistSource.M3u -> _m3uUrl.value.isNotBlank()
        }
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
