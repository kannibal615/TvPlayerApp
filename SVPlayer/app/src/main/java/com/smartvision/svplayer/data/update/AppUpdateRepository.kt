package com.smartvision.svplayer.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.smartvision.svplayer.BuildConfig
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class AppUpdateRepository(
    private val appContext: Context,
    private val api: AppUpdateApiService,
    private val okHttpClient: OkHttpClient,
) {
    suspend fun checkForUpdate(): AppUpdateInfo? {
        try {
            val response = api.checkUpdate(
                versionCode = BuildConfig.VERSION_CODE,
                versionName = BuildConfig.VERSION_NAME,
            )
            if (!response.success) {
                throw AppUpdateException(response.error ?: "Verification de mise a jour indisponible.")
            }
            if (!response.updateAvailable || response.latestVersionCode <= BuildConfig.VERSION_CODE) {
                return null
            }
            val apkUrl = response.apkUrl?.takeIf { it.startsWith("https://") }
                ?: throw AppUpdateException("URL de mise a jour invalide.")

            return AppUpdateInfo(
                versionCode = response.latestVersionCode,
                versionName = response.latestVersionName ?: response.latestVersionCode.toString(),
                apkUrl = apkUrl,
                apkSha256 = response.apkSha256,
                apkSize = response.apkSize,
                mandatory = response.mandatory,
                releaseNotes = response.releaseNotes,
            )
        } catch (error: Throwable) {
            throw error
        }
    }

    suspend fun downloadApk(update: AppUpdateInfo): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(update.apkUrl)
            .get()
            .build()
        try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw AppUpdateException("Telechargement impossible (${response.code}).")
            }
            val body = response.body ?: throw AppUpdateException("APK indisponible.")
            val updatesDir = File(appContext.cacheDir, "updates").apply {
                if (!exists()) mkdirs()
            }
            val apkFile = File(updatesDir, "smartvision-update-${update.versionCode}.apk")
            body.byteStream().use { input ->
                apkFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val expectedHash = update.apkSha256?.trim()?.lowercase()
            if (!expectedHash.isNullOrBlank() && sha256(apkFile) != expectedHash) {
                apkFile.delete()
                throw AppUpdateException("APK de mise a jour invalide.")
            }
            apkFile
        } catch (error: Throwable) {
            throw error
        }
    }

    fun openInstaller(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !appContext.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${appContext.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(settingsIntent)
            throw AppUpdateException("Autorisez l installation depuis SmartVision puis relancez la mise a jour.")
        }

        val apkUri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            apkFile,
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(installIntent)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val apkSha256: String?,
    val apkSize: Long?,
    val mandatory: Boolean,
    val releaseNotes: String?,
)

class AppUpdateException(message: String) : Exception(message)
