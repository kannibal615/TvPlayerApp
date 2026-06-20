package com.smartvision.svplayer.core.config

import android.content.Context
import com.smartvision.svplayer.BuildConfig
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
)

class XtreamAccountManager(context: Context) : XtreamCredentialsProvider {
    private val preferences = context.getSharedPreferences("xtream_accounts", Context.MODE_PRIVATE)
    private val _accounts = MutableStateFlow(loadAccounts().ifEmpty { buildConfigAccount() })
    val accounts: StateFlow<List<XtreamAccount>> = _accounts.asStateFlow()

    private val _activeAccountId = MutableStateFlow(
        preferences.getString(KEY_ACTIVE, null)
            ?.takeIf { id -> _accounts.value.any { it.id == id } }
            ?: _accounts.value.firstOrNull()?.id,
    )
    val activeAccountId: StateFlow<String?> = _activeAccountId.asStateFlow()

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
        )
        require(normalized.host.isNotBlank() && normalized.username.isNotBlank() && normalized.password.isNotBlank()) {
            "Hote, utilisateur et mot de passe sont obligatoires."
        }
        _accounts.value = (_accounts.value.filterNot { it.id == id } + normalized)
            .sortedBy { it.name.lowercase() }
        if (_activeAccountId.value == null) _activeAccountId.value = id
        persist()
        return id
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
                        .put("password", account.password),
                )
            }
        }
        preferences.edit()
            .putString(KEY_ACCOUNTS, json.toString())
            .putString(KEY_ACTIVE, _activeAccountId.value)
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
            )
        }.filter { it.host.isNotBlank() && it.username.isNotBlank() && it.password.isNotBlank() }
    }.getOrDefault(emptyList())

    private fun buildConfigAccount(): List<XtreamAccount> {
        if (BuildConfig.XTREAM_HOST.isBlank() || BuildConfig.XTREAM_USERNAME.isBlank() || BuildConfig.XTREAM_PASSWORD.isBlank()) {
            return emptyList()
        }
        return listOf(
            XtreamAccount(
                id = "build_config",
                name = "Compte principal",
                host = BuildConfig.XTREAM_HOST,
                username = BuildConfig.XTREAM_USERNAME,
                password = BuildConfig.XTREAM_PASSWORD,
            ),
        )
    }

    private companion object {
        const val KEY_ACCOUNTS = "accounts_json"
        const val KEY_ACTIVE = "active_account_id"
    }
}
