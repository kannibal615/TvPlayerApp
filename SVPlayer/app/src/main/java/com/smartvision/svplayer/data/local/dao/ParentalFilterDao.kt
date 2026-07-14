package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

data class ParentalHiddenFolderRow(
    val section: String,
    val folderId: String,
    val folderName: String,
    val hiddenCount: Int,
)

data class ParentalHiddenItemRow(
    val contentType: String,
    val contentId: String,
    val title: String,
    val folderName: String,
    val imageUrl: String?,
    val secondaryLabel: String,
    val duration: String?,
)

@Dao
interface ParentalFilterDao {
    @RawQuery
    suspend fun countHiddenFolders(query: SupportSQLiteQuery): Int

    @RawQuery
    suspend fun countHiddenItems(query: SupportSQLiteQuery): Int

    @RawQuery
    suspend fun loadHiddenFolders(query: SupportSQLiteQuery): List<ParentalHiddenFolderRow>

    @RawQuery
    suspend fun loadHiddenItems(query: SupportSQLiteQuery): List<ParentalHiddenItemRow>
}
