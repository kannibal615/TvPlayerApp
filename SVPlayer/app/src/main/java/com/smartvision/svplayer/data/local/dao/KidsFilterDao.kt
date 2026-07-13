package com.smartvision.svplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartvision.svplayer.data.local.entity.KidsCategoryDecisionEntity
import com.smartvision.svplayer.data.local.entity.KidsItemDecisionEntity

@Dao
interface KidsFilterDao {
    @Query(
        "SELECT * FROM kids_category_decisions " +
            "WHERE sourceKey = :sourceKey AND contentType = :contentType",
    )
    suspend fun getCategoryDecisions(sourceKey: String, contentType: String): List<KidsCategoryDecisionEntity>

    @Query(
        "SELECT * FROM kids_item_decisions " +
            "WHERE sourceKey = :sourceKey AND contentType = :contentType AND contentId IN (:contentIds)",
    )
    suspend fun getItemDecisions(
        sourceKey: String,
        contentType: String,
        contentIds: List<String>,
    ): List<KidsItemDecisionEntity>

    @Upsert
    suspend fun upsertCategoryDecisions(decisions: List<KidsCategoryDecisionEntity>)

    @Upsert
    suspend fun upsertItemDecisions(decisions: List<KidsItemDecisionEntity>)

    @Query("DELETE FROM kids_category_decisions WHERE ruleVersion != :ruleVersion")
    suspend fun deleteObsoleteCategoryRules(ruleVersion: Int)

    @Query("DELETE FROM kids_item_decisions WHERE ruleVersion != :ruleVersion")
    suspend fun deleteObsoleteItemRules(ruleVersion: Int)
}
