package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.smartvision.svplayer.data.local.entity.ParentalFilterSnapshotEntity

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

    @Query("SELECT * FROM parental_filter_snapshots WHERE profileId = :profileId LIMIT 1")
    suspend fun getSnapshot(profileId: String): ParentalFilterSnapshotEntity?

    @Query("SELECT COUNT(*) FROM (SELECT section, folderId FROM parental_hidden_items WHERE profileId = :profileId GROUP BY section, folderId)")
    suspend fun countSnapshotFolders(profileId: String): Int

    @Query("SELECT COUNT(*) FROM parental_hidden_items WHERE profileId = :profileId")
    suspend fun countSnapshotItems(profileId: String): Int

    @Query(
        "SELECT section, folderId, folderName, COUNT(*) AS hiddenCount FROM parental_hidden_items " +
            "WHERE profileId = :profileId GROUP BY section, folderId, folderName " +
            "ORDER BY section, folderName LIMIT :limit OFFSET :offset",
    )
    suspend fun loadSnapshotFolders(profileId: String, offset: Int, limit: Int): List<ParentalHiddenFolderRow>

    @Query(
        "SELECT contentType, contentId, title, folderName, imageUrl, secondaryLabel, duration " +
            "FROM parental_hidden_items WHERE profileId = :profileId AND section = :section AND folderId = :folderId " +
            "ORDER BY title, contentType, contentId LIMIT :limit OFFSET :offset",
    )
    suspend fun loadSnapshotItems(
        profileId: String,
        section: String,
        folderId: String,
        offset: Int,
        limit: Int,
    ): List<ParentalHiddenItemRow>

    @Query(
        "SELECT COUNT(*) FROM parental_hidden_items " +
            "WHERE profileId = :profileId AND section = :section AND folderId = :folderId",
    )
    suspend fun countSnapshotItems(profileId: String, section: String, folderId: String): Int

    @Query("SELECT contentType || ':' || contentId FROM parental_hidden_items WHERE profileId = :profileId")
    suspend fun loadSnapshotStableKeys(profileId: String): List<String>

    @Query("DELETE FROM parental_hidden_items WHERE profileId = :profileId")
    suspend fun deleteItemsByProfile(profileId: String)

    @Query("DELETE FROM parental_filter_snapshots WHERE profileId = :profileId")
    suspend fun deleteSnapshotByProfile(profileId: String)
}
