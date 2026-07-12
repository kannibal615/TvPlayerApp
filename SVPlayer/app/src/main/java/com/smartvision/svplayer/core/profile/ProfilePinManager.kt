package com.smartvision.svplayer.core.profile

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class ProfilePinManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "smartvision_profile_pin",
        Context.MODE_PRIVATE,
    )

    fun hasPin(): Boolean =
        preferences.getString(KEY_HASH, null) != null && preferences.getString(KEY_SALT, null) != null

    fun setPin(pin: String) {
        val normalized = normalize(pin)
        require(normalized.length == PIN_LENGTH) { "Le PIN doit contenir exactement quatre chiffres." }
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val hash = derive(normalized, salt)
        preferences.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val normalized = normalize(pin)
        if (normalized.length != PIN_LENGTH) return false
        val salt = preferences.getString(KEY_SALT, null)?.decodeBase64() ?: return false
        val expected = preferences.getString(KEY_HASH, null)?.decodeBase64() ?: return false
        return MessageDigest.isEqual(expected, derive(normalized, salt))
    }

    fun migrateLegacyPinIfNeeded(legacyPin: String): Boolean {
        if (hasPin()) return false
        val normalized = normalize(legacyPin)
        if (normalized.length < PIN_LENGTH) return false
        setPin(normalized.take(PIN_LENGTH))
        return true
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun normalize(value: String): String = value.filter(Char::isDigit).take(PIN_LENGTH)

    private fun String.decodeBase64(): ByteArray? =
        runCatching { Base64.decode(this, Base64.NO_WRAP) }.getOrNull()

    private companion object {
        const val PIN_LENGTH = 4
        const val SALT_BYTES = 16
        const val ITERATIONS = 120_000
        const val HASH_BITS = 256
        const val KEY_SALT = "pin_salt"
        const val KEY_HASH = "pin_hash"
    }
}
