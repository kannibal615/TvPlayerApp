package com.smartvision.svplayer.ui.catalog

import java.util.Locale

enum class StreamingBrand(
    val key: String,
    val displayName: String,
) {
    Netflix("netflix", "NETFLIX"),
    Prime("prime", "PRIME"),
    Apple("apple", "APPLE"),
    Disney("disney", "DISNEY"),

    ;

    fun groupId(section: String): String = "__brand_${section}_${key}__"
}

data class StreamingCategoryGroups<T>(
    val groups: Map<StreamingBrand, List<T>>,
    val remaining: List<T>,
)

object StreamingCategoryGroupPolicy {
    private val brandPatterns = StreamingBrand.entries.associateWith { brand ->
        Regex("(?<![\\p{L}\\p{N}])${Regex.escape(brand.displayName)}(?![\\p{L}\\p{N}])")
    }

    fun brandFor(label: String): StreamingBrand? {
        val normalized = label.uppercase(Locale.ROOT)
        return StreamingBrand.entries.firstOrNull { brand -> brandPatterns.getValue(brand).containsMatchIn(normalized) }
    }

    fun toggleExpanded(current: StreamingBrand?, requested: StreamingBrand): StreamingBrand? =
        requested.takeUnless { current == requested }

    fun <T> group(
        categories: List<T>,
        labelOf: (T) -> String,
    ): StreamingCategoryGroups<T> {
        val grouped = linkedMapOf<StreamingBrand, MutableList<T>>()
        val remaining = mutableListOf<T>()
        categories.forEach { category ->
            val brand = brandFor(labelOf(category))
            if (brand == null) {
                remaining += category
            } else {
                grouped.getOrPut(brand) { mutableListOf() } += category
            }
        }
        return StreamingCategoryGroups(
            groups = StreamingBrand.entries.mapNotNull { brand ->
                grouped[brand]?.takeIf { it.isNotEmpty() }?.let { brand to it.toList() }
            }.toMap(linkedMapOf()),
            remaining = remaining,
        )
    }
}
