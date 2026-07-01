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

    init {
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
        persist()
    }

    @Synchronized
    fun updateM3uUrl(url: String) {
        _m3uUrl.value = url.trim()
        if (_m3uUrl.value.isNotBlank() && !_activePlaylistSource.value.isAvailable()) {
            _activePlaylistSource.value = PlaylistSource.M3u
        }
        persist()
    }

    @Synchronized
    fun selectPlaylistSource(source: PlaylistSource) {
        _activePlaylistSource.value = source
        persist()
    }

    @Synchronized
    fun delete(accountId: String) {
        _accounts.value = _accounts.value.filterNot { it.id == accountId }
        if (_activeAccountId.value == accountId) {
            _activeAccountId.value = _accounts.value.firstOrNull()?.id
        }
        persist()
    }

    @Synchronized
    fun select(accountId: String) {
        require(_accounts.value.any { it.id == accountId }) { "Compte Xtream introuvable." }
        _activeAccountId.value = accountId
        persist()
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

    private companion object {
        const val KEY_ACCOUNTS = "accounts_json"
        const val KEY_ACTIVE = "active_account_id"
        const val KEY_EPG_URL = "epg_url"
        const val KEY_M3U_URL = "m3u_url"
        const val KEY_ACTIVE_PLAYLIST_SOURCE = "active_playlist_source"
        const val LEGACY_BUILD_CONFIG_ACCOUNT = "build_config"
    }

    private fun PlaylistSource.isAvailable(): Boolean =
        when (this) {
            PlaylistSource.Xtream -> current().isConfigured
            PlaylistSource.M3u -> _m3uUrl.value.isNotBlank()
        }
}
