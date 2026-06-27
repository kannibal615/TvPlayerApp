package com.smartvision.svplayer.domain.model

data class CategoryHistorySignal(
    val categoryId: String,
    val updatedAt: Long,
)

fun <T> List<T>.sortedByHistorySignals(
    signals: List<CategoryHistorySignal>,
    idOf: (T) -> String,
): List<T> {
    if (signals.isEmpty()) return this
    val scores = signals
        .filter { it.categoryId.isNotBlank() }
        .groupBy { it.categoryId }
        .mapValues { (_, entries) ->
            CategoryScore(
                count = entries.size,
                newest = entries.maxOf { it.updatedAt },
            )
        }
    if (scores.isEmpty()) return this
    return withIndex()
        .sortedWith(
            compareByDescending<IndexedValue<T>> { scores[idOf(it.value)]?.count ?: 0 }
                .thenByDescending { scores[idOf(it.value)]?.newest ?: Long.MIN_VALUE }
                .thenBy { it.index },
        )
        .map { it.value }
}

private data class CategoryScore(
    val count: Int,
    val newest: Long,
)
