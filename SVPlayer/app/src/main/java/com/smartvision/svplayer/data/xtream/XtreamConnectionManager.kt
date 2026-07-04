package com.smartvision.svplayer.data.xtream

import android.content.Context
import com.google.gson.JsonParseException
import com.smartvision.svplayer.core.config.XtreamAccountManager
import com.smartvision.svplayer.data.anomaly.AnomalyReporter
import com.smartvision.svplayer.data.remote.XtreamConnectionApi
import java.io.EOFException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException

enum class XtreamConnectionStatus {
    UNKNOWN,
    NOT_CONFIGURED,
    CONNECTED,
    NETWORK_ERROR,
    INVALID_CREDENTIALS,
    INVALID_RESPONSE,
    UNKNOWN_ERROR,
}

data class XtreamConnectionState(
    val status: XtreamConnectionStatus = XtreamConnectionStatus.UNKNOWN,
    val checking: Boolean = false,
    val message: String = "",
    val technicalDetail: String = "",
    val serverUrl: String = "",
    val checkedAt: Long = 0L,
    val confirmedFailure: Boolean = false,
    val attempt: Int = 0,
    val maxAttempts: Int = 0,
) {
    val isConnected: Boolean = status == XtreamConnectionStatus.CONNECTED
    val blocksCatalog: Boolean =
        confirmedFailure && (
            status == XtreamConnectionStatus.NETWORK_ERROR ||
                status == XtreamConnectionStatus.INVALID_CREDENTIALS ||
                status == XtreamConnectionStatus.INVALID_RESPONSE ||
                status == XtreamConnectionStatus.UNKNOWN_ERROR
        )
    val blocksCatalogForNavigation: Boolean = blocksCatalog

    val shouldRetryInBackground: Boolean = confirmedFailure && status == XtreamConnectionStatus.NETWORK_ERROR
}

class XtreamConnectionManager(
    context: Context,
    private val accountManager: XtreamAccountManager,
    private val apiClient: XtreamConnectionApi,
    private val anomalyReporter: AnomalyReporter,
) {
    private val appContext = context.applicationContext
    private val notifier = XtreamConnectionNotifier(appContext)
    private val prefs = appContext.getSharedPreferences("smartvision_xtream_connection", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(XtreamConnectionState())
    val state: StateFlow<XtreamConnectionState> = _state

    private val _alertRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val alertRequests: SharedFlow<Unit> = _alertRequests
    private val verificationMutex = Mutex()
    private var lastConnectedAccountSignature: String = ""

    suspend fun verifyQuick(source: String): XtreamConnectionState = withContext(Dispatchers.IO) {
        verificationMutex.withLock {
            verifyQuickLocked(source)
        }
    }

    private suspend fun verifyQuickLocked(source: String): XtreamConnectionState {
        val credentials = accountManager.current()
        if (!credentials.isConfigured) {
            val notConfigured = XtreamConnectionState(
                status = XtreamConnectionStatus.NOT_CONFIGURED,
                checking = false,
                message = "Aucun compte Xtream configure.",
                serverUrl = credentials.normalizedHost,
                checkedAt = System.currentTimeMillis(),
            )
            _state.value = notConfigured
            notifier.clear()
            return notConfigured
        }

        _state.value = _state.value.copy(
            checking = true,
            message = "Verification de la connexion Xtream...",
            serverUrl = credentials.normalizedHost,
            confirmedFailure = false,
            attempt = 0,
            maxAttempts = CONNECTION_CHECK_ATTEMPTS,
        )

        val checked = runConfirmedXtreamVerification(
            maxAttempts = CONNECTION_CHECK_ATTEMPTS,
            retryDelayMillis = CONNECTION_CHECK_RETRY_DELAY_MS,
            checkOnce = { verifyAccountOnce(credentials.normalizedHost) },
            onPendingFailure = { pendingState ->
                _state.value = pendingState
            },
        )

        _state.value = checked
        if (checked.isConnected) {
            lastConnectedAccountSignature = credentials.connectionSignature()
            notifier.clear()
        } else if (checked.blocksCatalog) {
            lastConnectedAccountSignature = ""
            reportFailureIfNeeded(checked, source)
            notifier.showIssue()
            if (source.shouldRequestForegroundAlert()) {
                requestAlert()
            }
        }
        return checked
    }

    private suspend fun verifyAccountOnce(serverUrl: String): XtreamConnectionState =
        try {
            withTimeout(ACCOUNT_CHECK_TIMEOUT_MS) {
                val account = apiClient.getAccount()
                val userInfo = account.userInfo
                    ?: throw InvalidXtreamResponseException("user_info absent")
                val status = userInfo.status.orEmpty().trim().lowercase()
                if (status != "active") {
                    throw InvalidXtreamCredentialsException("Compte Xtream non actif: ${userInfo.status ?: "unknown"}")
                }
                XtreamConnectionState(
                    status = XtreamConnectionStatus.CONNECTED,
                    checking = false,
                    message = "Connexion Xtream valide.",
                    serverUrl = serverUrl,
                    checkedAt = System.currentTimeMillis(),
                )
            }
        } catch (error: Throwable) {
            if (error is CancellationException && error !is TimeoutCancellationException) {
                throw error
            }
            error.toConnectionState(serverUrl)
        }

    fun hasFreshConnectedState(maxAgeMillis: Long = FRESH_CONNECTED_WINDOW_MS): Boolean {
        val current = _state.value
        if (!current.isConnected || current.checking || current.checkedAt <= 0L) return false
        if (System.currentTimeMillis() - current.checkedAt > maxAgeMillis) return false
        return lastConnectedAccountSignature == accountManager.current().connectionSignature()
    }

    fun requestAlert() {
        if (_state.value.blocksCatalog) {
            _alertRequests.tryEmit(Unit)
        }
    }

    fun markPlaybackUnavailable(
        source: String,
        contentType: String,
        streamId: Int,
        detail: String,
    ): XtreamConnectionState {
        val credentials = accountManager.current()
        val state = XtreamConnectionState(
            status = XtreamConnectionStatus.NETWORK_ERROR,
            checking = true,
            message = "Verification Xtream en arriere-plan apres incident de lecture...",
            technicalDetail = detail.take(180),
            serverUrl = credentials.normalizedHost,
            checkedAt = System.currentTimeMillis(),
            confirmedFailure = false,
            attempt = 0,
            maxAttempts = CONNECTION_CHECK_ATTEMPTS,
        )
        _state.value = state
        anomalyReporter.reportAsync(
            anomalyType = "PLAYER_STREAM_UNAVAILABLE",
            message = "Flux Xtream bloque en lecture.",
            context = listOf(
                "source=$source",
                "server=${state.serverUrl}",
                "contentType=$contentType",
                "streamId=$streamId",
                "detail=${state.technicalDetail}",
            ).joinToString(" | "),
        )
        return state
    }

    private fun reportFailureIfNeeded(state: XtreamConnectionState, source: String) {
        val now = System.currentTimeMillis()
        val key = "${state.serverUrl}|${state.status}|${state.message}"
        val lastKey = prefs.getString(KEY_LAST_ANOMALY, "")
        val lastAt = prefs.getLong(KEY_LAST_ANOMALY_AT, 0L)
        if (key == lastKey && now - lastAt < ANOMALY_DEDUP_WINDOW_MS) return

        prefs.edit()
            .putString(KEY_LAST_ANOMALY, key)
            .putLong(KEY_LAST_ANOMALY_AT, now)
            .apply()

        anomalyReporter.reportAsync(
            anomalyType = "XTREAM_FAILED",
            message = state.message,
            context = listOf(
                "source=$source",
                "server=${state.serverUrl}",
                "errorType=${state.status.name}",
                "detail=${state.technicalDetail}",
            ).joinToString(" | "),
        )
    }

    private fun Throwable.toConnectionState(serverUrl: String): XtreamConnectionState {
        val status = when (this) {
            is InvalidXtreamCredentialsException -> XtreamConnectionStatus.INVALID_CREDENTIALS
            is InvalidXtreamResponseException -> XtreamConnectionStatus.INVALID_RESPONSE
            is TimeoutCancellationException,
            is SocketTimeoutException,
            is UnknownHostException,
            is IOException -> XtreamConnectionStatus.NETWORK_ERROR
            is HttpException -> when (code()) {
                401, 403 -> XtreamConnectionStatus.INVALID_CREDENTIALS
                in 500..599 -> XtreamConnectionStatus.NETWORK_ERROR
                else -> XtreamConnectionStatus.INVALID_RESPONSE
            }
            is JsonParseException,
            is EOFException -> XtreamConnectionStatus.INVALID_RESPONSE
            is IllegalStateException -> XtreamConnectionStatus.NOT_CONFIGURED
            else -> XtreamConnectionStatus.UNKNOWN_ERROR
        }
        val message = when (status) {
            XtreamConnectionStatus.NETWORK_ERROR -> "Serveur Xtream inaccessible ou timeout reseau."
            XtreamConnectionStatus.INVALID_CREDENTIALS -> "Identifiants Xtream invalides, expires ou desactives."
            XtreamConnectionStatus.INVALID_RESPONSE -> "Reponse Xtream vide, invalide ou incompatible."
            XtreamConnectionStatus.NOT_CONFIGURED -> "Aucun compte Xtream configure."
            XtreamConnectionStatus.UNKNOWN_ERROR -> "Erreur Xtream non identifiee."
            else -> localizedMessage ?: javaClass.simpleName
        }
        return XtreamConnectionState(
            status = status,
            checking = false,
            message = message,
            technicalDetail = localizedMessage ?: javaClass.simpleName,
            serverUrl = serverUrl,
            checkedAt = System.currentTimeMillis(),
        )
    }

    private companion object {
        const val ACCOUNT_CHECK_TIMEOUT_MS = 8_000L
        const val CONNECTION_CHECK_ATTEMPTS = 3
        const val CONNECTION_CHECK_RETRY_DELAY_MS = 1_200L
        const val ANOMALY_DEDUP_WINDOW_MS = 15 * 60 * 1_000L
        const val FRESH_CONNECTED_WINDOW_MS = 30_000L
        const val KEY_LAST_ANOMALY = "last_anomaly_key"
        const val KEY_LAST_ANOMALY_AT = "last_anomaly_at"
    }
}

internal suspend fun runConfirmedXtreamVerification(
    maxAttempts: Int,
    retryDelayMillis: Long,
    checkOnce: suspend () -> XtreamConnectionState,
    onPendingFailure: (XtreamConnectionState) -> Unit,
    delayBeforeRetry: suspend (Long) -> Unit = { delay(it) },
): XtreamConnectionState {
    require(maxAttempts > 0) { "maxAttempts must be positive" }
    var lastFailure: XtreamConnectionState? = null
    for (attempt in 1..maxAttempts) {
        val result = checkOnce().copy(
            attempt = attempt,
            maxAttempts = maxAttempts,
        )
        if (result.isConnected || result.status == XtreamConnectionStatus.NOT_CONFIGURED) {
            return result.copy(
                checking = false,
                confirmedFailure = false,
            )
        }

        lastFailure = result
        if (attempt < maxAttempts) {
            onPendingFailure(
                result.copy(
                    checking = true,
                    confirmedFailure = false,
                    message = "Verification de la connexion Xtream... tentative $attempt/$maxAttempts",
                ),
            )
            delayBeforeRetry(retryDelayMillis)
        }
    }
    return requireNotNull(lastFailure).copy(
        checking = false,
        confirmedFailure = true,
        attempt = maxAttempts,
        maxAttempts = maxAttempts,
    )
}

private fun com.smartvision.svplayer.core.config.XtreamCredentials.connectionSignature(): String =
    "${normalizedHost}|$username|${password.hashCode()}"

private fun String.shouldRequestForegroundAlert(): Boolean =
    this == "splash" ||
        this == "startup" ||
        this == "home_startup_sync" ||
        this == "manual_sync" ||
        this == "user_retry" ||
        this == "credentials_edit" ||
        this == "player_buffering"

private class InvalidXtreamCredentialsException(message: String) : RuntimeException(message)
private class InvalidXtreamResponseException(message: String) : RuntimeException(message)
