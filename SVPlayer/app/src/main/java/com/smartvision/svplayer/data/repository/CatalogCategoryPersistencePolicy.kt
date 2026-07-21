package com.smartvision.svplayer.data.repository

internal object CatalogCategoryPersistencePolicy {
    fun shouldReplace(usableCategoryCount: Int, retainedItemCount: Int): Boolean =
        usableCategoryCount > 0 || retainedItemCount == 0
}
