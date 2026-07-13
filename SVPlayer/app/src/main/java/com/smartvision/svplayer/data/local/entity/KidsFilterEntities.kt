package com.smartvision.svplayer.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "kids_category_decisions",
    primaryKeys = ["sourceKey", "contentType", "categoryId"],
    indices = [
        Index(value = ["sourceKey", "contentType", "decision"]),
        Index(value = ["ruleVersion"]),
    ],
)
data class KidsCategoryDecisionEntity(
    val sourceKey: String,
    val contentType: String,
    val categoryId: String,
    val normalizedName: String,
    val decision: String,
    val score: Int,
    val source: String,
    val reason: String,
    val ruleVersion: Int,
    val metadataFingerprint: String,
    val updatedAt: Long,
)

@Entity(
    tableName = "kids_item_decisions",
    primaryKeys = ["sourceKey", "contentType", "contentId"],
    indices = [
        Index(value = ["sourceKey", "contentType", "categoryId"]),
        Index(value = ["sourceKey", "contentType", "allowed"]),
        Index(value = ["ruleVersion"]),
    ],
)
data class KidsItemDecisionEntity(
    val sourceKey: String,
    val contentType: String,
    val contentId: String,
    val categoryId: String?,
    val allowed: Boolean,
    val decision: String,
    val score: Int,
    val source: String,
    val reason: String,
    val inheritedCategoryId: String?,
    val ruleVersion: Int,
    val metadataFingerprint: String,
    val updatedAt: Long,
)
