package com.smartvision.svplayer.core.config

const val AdminProfileAvatarId: String = "admin"

val ProfileAvatarPresetIds: List<String> = listOf(
    "classic_compass",
    "classic_mountain",
    "classic_wave",
    "classic_leaf",
    "classic_planet",
    "classic_lighthouse",
    "classic_owl",
    "classic_sunrise",
)

val KidsProfileAvatarPresetIds: List<String> = listOf(
    "kid_robot",
    "kid_bear",
    "kid_moon",
    "kid_rainbow",
    "kid_dinosaur",
    "kid_star",
    "kid_sun",
    "kid_cube",
)

private val LegacyClassicAvatarIds = mapOf(
    "aurora" to "classic_compass",
    "ocean" to "classic_wave",
    "sunset" to "classic_sunrise",
    "emerald" to "classic_leaf",
    "violet" to "classic_planet",
    "coral" to "classic_lighthouse",
    "steel" to "classic_mountain",
    "gold" to "classic_owl",
    "rose" to "classic_lighthouse",
    "midnight" to "classic_mountain",
)

private val LegacyKidsAvatarIds = mapOf(
    "kids_sky" to "kid_robot",
    "kids_star" to "kid_star",
    "kids_mint" to "kid_dinosaur",
    "kids_coral" to "kid_bear",
    "kids_sun" to "kid_sun",
)

fun canonicalProfileAvatarId(avatarId: String, type: ProfileType): String = when (type) {
    ProfileType.ADMIN -> when (avatarId) {
        AdminProfileAvatarId -> AdminProfileAvatarId
        in ProfileAvatarPresetIds -> avatarId
        in LegacyClassicAvatarIds -> LegacyClassicAvatarIds.getValue(avatarId)
        else -> AdminProfileAvatarId
    }
    ProfileType.KIDS -> when (avatarId) {
        in KidsProfileAvatarPresetIds -> avatarId
        in LegacyKidsAvatarIds -> LegacyKidsAvatarIds.getValue(avatarId)
        else -> KidsProfileAvatarPresetIds.first()
    }
    ProfileType.NORMAL -> when (avatarId) {
        in ProfileAvatarPresetIds -> avatarId
        in LegacyClassicAvatarIds -> LegacyClassicAvatarIds.getValue(avatarId)
        else -> ProfileAvatarPresetIds.first()
    }
}

fun defaultProfileAvatarId(type: ProfileType, stableKey: String? = null): String = when (type) {
    ProfileType.ADMIN -> AdminProfileAvatarId
    ProfileType.KIDS -> stableKey?.stableAvatarFrom(KidsProfileAvatarPresetIds)
        ?: KidsProfileAvatarPresetIds.random()
    ProfileType.NORMAL -> stableKey?.stableAvatarFrom(ProfileAvatarPresetIds)
        ?: ProfileAvatarPresetIds.first()
}

fun profileAvatarIdForName(value: String): String = value.stableAvatarFrom(ProfileAvatarPresetIds)

private fun String.stableAvatarFrom(avatarIds: List<String>): String {
    val key = trim().ifBlank { "profile" }
    val index = key.lowercase().hashCode().and(Int.MAX_VALUE).mod(avatarIds.size)
    return avatarIds[index]
}
