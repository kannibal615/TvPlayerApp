package com.smartvision.svplayer.ui.catalog

import java.text.Normalizer
import java.util.Locale

object AllCategoryPolicy {
    private val equivalentNames = setOf(
        "all",
        "all channels",
        "all movies",
        "all series",
        "tous",
        "toutes",
        "toutes les chaines",
        "tous les films",
        "toutes les series",
    )

    fun isEquivalent(label: String): Boolean = normalize(label) in equivalentNames

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
        .lowercase(Locale.ROOT)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}
