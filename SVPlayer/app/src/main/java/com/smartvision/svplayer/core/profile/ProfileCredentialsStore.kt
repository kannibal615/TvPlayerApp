package com.smartvision.svplayer.core.profile

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

data class StoredProfileCredentials(
    val xtreamHost: String = "",
    val xtreamUsername: String = "",
    val xtreamPassword: String = "",
    val m3uUrl: String = "",
    val epgUrl: String = "",
)

class ProfileCredentialsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "smartvision_profile_credentials",
        Context.MODE_PRIVATE,
    )

    fun put(profileId: String, credentials: StoredProfileCredentials) {
        val payload = JSONObject()
            .put("xtream_host", credentials.xtreamHost)
            .put("xtream_username", credentials.xtreamUsername)
            .put("xtream_password", credentials.xtreamPassword)
            .put("m3u_url", credentials.m3uUrl)
            .put("epg_url", credentials.epgUrl)
            .toString()
            .toByteArray(StandardCharsets.UTF_8)
        preferences.edit().putString(profileId, encrypt(profileId, payload)).apply()
    }

    fun get(profileId: String): StoredProfileCredentials? {
        val encoded = preferences.getString(profileId, null) ?: return null
        return runCatching {
            val payload = JSONObject(String(decrypt(profileId, encoded), StandardCharsets.UTF_8))
            StoredProfileCredentials(
                xtreamHost = payload.optString("xtream_host"),
                xtreamUsername = payload.optString("xtream_username"),
                xtreamPassword = payload.optString("xtream_password"),
                m3uUrl = payload.optString("m3u_url"),
                epgUrl = payload.optString("epg_url"),
            )
        }.getOrNull()
    }

    fun delete(profileId: String) {
        preferences.edit().remove(profileId).apply()
    }

    private fun encrypt(profileId: String, plain: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        cipher.updateAAD(profileId.toByteArray(StandardCharsets.UTF_8))
        val encrypted = cipher.doFinal(plain)
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(profileId: String, encoded: String): ByteArray {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        require(combined.size > IV_BYTES)
        val iv = combined.copyOfRange(0, IV_BYTES)
        val encrypted = combined.copyOfRange(IV_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(profileId.toByteArray(StandardCharsets.UTF_8))
        return cipher.doFinal(encrypted)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "smartvision_profile_credentials_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
