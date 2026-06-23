package com.smartvision.svplayer.data.activation

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale

data class DeviceIdentity(
    val androidIdHash: String,
    val fingerprintHash: String,
    val appPackage: String,
    val appVersion: String,
    val manufacturer: String,
    val model: String,
)

object DeviceIdentityProvider {
    fun create(context: Context, appVersion: String): DeviceIdentity {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        ).orEmpty()
        val packageName = context.packageName.orEmpty()
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        val fingerprintSource = listOf(
            androidId,
            packageName,
            manufacturer,
            model,
            Build.BRAND.orEmpty(),
            Build.DEVICE.orEmpty(),
        )
            .joinToString("|") { it.trim().lowercase(Locale.US) }

        return DeviceIdentity(
            androidIdHash = sha256(androidId.ifBlank { fingerprintSource }),
            fingerprintHash = sha256(fingerprintSource),
            appPackage = packageName,
            appVersion = appVersion,
            manufacturer = manufacturer,
            model = model,
        )
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
