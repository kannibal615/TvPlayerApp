package com.smartvision.svplayer.core.config

val ProfileAvatarPresetIds: List<String> = listOf(
    "aurora",
    "ocean",
    "sunset",
    "emerald",
    "violet",
    "coral",
    "steel",
    "gold",
    "rose",
    "midnight",
)

val KidsProfileAvatarPresetIds: List<String> = listOf(
    "kids_sky",
    "kids_star",
    "kids_mint",
    "kids_coral",
    "kids_sun",
)

fun profileAvatarIdForName(value: String): String {
    val key = value.trim().ifBlank { "profile" }
    val index = kotlin.math.abs(key.lowercase().hashCode()).mod(ProfileAvatarPresetIds.size)
    return ProfileAvatarPresetIds[index]
}
