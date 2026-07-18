package com.smartvision.svplayer.domain.profile

import com.smartvision.svplayer.core.config.PlaylistProfile
import java.security.MessageDigest

object CatalogSyncFingerprint {
    private const val PolicyVersion = 1

    fun create(profile: PlaylistProfile): String {
        val payload = listOf(
            PolicyVersion.toString(),
            profile.source.storageValue,
            profile.type.storageValue,
            profile.xtreamHost.trim().trimEnd('/'),
            profile.xtreamUsername.trim(),
            profile.xtreamPassword,
            profile.m3uUrl.trim(),
            ContentPrefixPolicy.normalize(profile.selectedContentPrefixes).joinToString(","),
        ).joinToString("\u001f")
        return MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }
}
