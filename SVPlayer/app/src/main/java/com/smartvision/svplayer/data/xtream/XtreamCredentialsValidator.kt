package com.smartvision.svplayer.data.xtream

import com.smartvision.svplayer.data.remote.XtreamApiService
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.HttpException

class XtreamCredentialsValidator(private val api: XtreamApiService) {
    suspend fun validate(host: String, username: String, password: String): XtreamCredentialsValidationResult {
        val normalizedHost = normalizeHost(host)
            ?: return XtreamCredentialsValidationResult.Failure("Invalid server URL.")
        if (username.isBlank()) return XtreamCredentialsValidationResult.Failure("Username is required.")
        if (password.isBlank()) return XtreamCredentialsValidationResult.Failure("Password is required.")
        return try {
            val response = withTimeout(VALIDATION_TIMEOUT_MS) {
                api.getAccount(username.trim(), password.trim(), normalizedHost)
            }
            val userInfo = response.userInfo
                ?: return XtreamCredentialsValidationResult.Failure("Invalid response from the Xtream server.")
            when (userInfo.status.orEmpty().trim().lowercase()) {
                "active" -> XtreamCredentialsValidationResult.Success(normalizedHost)
                "expired" -> XtreamCredentialsValidationResult.Failure("This Xtream account has expired.")
                else -> XtreamCredentialsValidationResult.Failure("Invalid or inactive Xtream credentials.")
            }
        } catch (_: TimeoutCancellationException) {
            XtreamCredentialsValidationResult.Failure("The Xtream server timed out.")
        } catch (_: SocketTimeoutException) {
            XtreamCredentialsValidationResult.Failure("The Xtream server timed out.")
        } catch (_: UnknownHostException) {
            XtreamCredentialsValidationResult.Failure("Xtream server not found.")
        } catch (_: IOException) {
            XtreamCredentialsValidationResult.Failure("Unable to reach the Xtream server.")
        } catch (error: HttpException) {
            XtreamCredentialsValidationResult.Failure(
                if (error.code() == 401 || error.code() == 403) "Invalid Xtream credentials."
                else "Xtream server error (${error.code()}).",
            )
        } catch (_: Throwable) {
            XtreamCredentialsValidationResult.Failure("Unable to validate these Xtream credentials.")
        }
    }

    private fun normalizeHost(value: String): String? {
        val candidate = value.trim().trimEnd('/').let {
            if (it.startsWith("http://", true) || it.startsWith("https://", true)) it else "http://$it"
        }
        return candidate.toHttpUrlOrNull()?.newBuilder()?.encodedPath("/")?.query(null)?.fragment(null)?.build()?.toString()?.trimEnd('/')
    }

    private companion object {
        const val VALIDATION_TIMEOUT_MS = 15_000L
    }
}

sealed interface XtreamCredentialsValidationResult {
    data class Success(val normalizedHost: String) : XtreamCredentialsValidationResult
    data class Failure(val message: String) : XtreamCredentialsValidationResult
}
