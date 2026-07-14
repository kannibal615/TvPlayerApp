package com.smartvision.svplayer.domain.parental

data class ParentalFilterCounts(
    val folders: Int = 0,
    val items: Int = 0,
)

data class ParentalHiddenFolder(
    val stableKey: String,
    val section: String,
    val folderId: String,
    val name: String,
    val hiddenCount: Int,
)

enum class ParentalHiddenContentType {
    Channel,
    Movie,
    Series,
    Episode,
}

data class ParentalHiddenItem(
    val stableKey: String,
    val type: ParentalHiddenContentType,
    val contentId: String,
    val title: String,
    val folderName: String,
    val imageUrl: String?,
    val secondaryLabel: String,
    val duration: String?,
)

interface ParentalCatalogRepository {
    suspend fun counts(profileId: String, keywords: List<String>): ParentalFilterCounts
    suspend fun folders(profileId: String, keywords: List<String>, offset: Int, limit: Int): List<ParentalHiddenFolder>
    suspend fun items(
        profileId: String,
        keywords: List<String>,
        folder: ParentalHiddenFolder,
        offset: Int,
        limit: Int,
    ): List<ParentalHiddenItem>
    suspend fun itemCount(profileId: String, keywords: List<String>, folder: ParentalHiddenFolder): Int
    suspend fun hiddenStableKeys(profileId: String, keywords: List<String>): Set<String>
    suspend fun deleteProfileSnapshot(profileId: String)
}
