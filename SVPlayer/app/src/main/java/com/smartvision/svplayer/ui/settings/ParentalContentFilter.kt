package com.smartvision.svplayer.ui.settings

import com.smartvision.svplayer.domain.model.PlayerSettings
import com.smartvision.svplayer.domain.parental.ParentalKeywordPolicy
import java.util.Locale

fun PlayerSettings.parentalKeywordsList(): List<String> =
    ParentalKeywordPolicy.normalizedForMatching(
        parentalKeywordValues.ifEmpty { ParentalKeywordPolicy.parseLegacy(parentalKeywords) },
    )

fun PlayerSettings.allowsContent(vararg values: String?): Boolean {
    if (!parentalControlEnabled) return true
    val keywords = parentalKeywordsList()
    if (keywords.isEmpty()) return true
    val haystack = values
        .filterNotNull()
        .joinToString(" ")
        .lowercase(Locale.ROOT)
    return keywords.none { keyword -> haystack.contains(keyword) }
}
