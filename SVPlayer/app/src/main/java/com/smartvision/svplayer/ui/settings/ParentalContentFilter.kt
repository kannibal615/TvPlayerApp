package com.smartvision.svplayer.ui.settings

import com.smartvision.svplayer.domain.model.PlayerSettings

fun PlayerSettings.parentalKeywordsList(): List<String> =
    parentalKeywords
        .split(',', ';', '\n', '\r', '|')
        .map { it.trim().lowercase() }
        .filter { it.length >= 2 }
        .distinct()

fun PlayerSettings.allowsContent(vararg values: String?): Boolean {
    if (!parentalControlEnabled) return true
    val keywords = parentalKeywordsList()
    if (keywords.isEmpty()) return true
    val haystack = values
        .filterNotNull()
        .joinToString(" ")
        .lowercase()
    return keywords.none { keyword -> haystack.contains(keyword) }
}
