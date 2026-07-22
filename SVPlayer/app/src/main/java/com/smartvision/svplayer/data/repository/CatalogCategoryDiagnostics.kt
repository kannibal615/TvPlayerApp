package com.smartvision.svplayer.data.repository

import android.util.Log
import com.smartvision.svplayer.domain.model.Category
import com.smartvision.svplayer.domain.model.MediaSection

internal object CatalogCategoryDiagnostics {
    private const val Tag = "SVCatalogCategories"

    fun repositoryRead(
        section: MediaSection,
        profileId: String,
        source: String,
        categories: List<Category>,
        configured: Boolean,
    ) {
        Log.i(
            Tag,
            "event=repository_read section=${section.storageName} profile=${profileToken(profileId)} " +
                "source=$source configured=$configured categories=${categories.size} " +
                "nonEmpty=${categories.count { it.count > 0 }}",
        )
    }

    fun roomSnapshot(
        section: MediaSection,
        profileId: String,
        source: String,
        roomRows: Int,
        returnedRows: Int,
    ) {
        Log.i(
            Tag,
            "event=room_snapshot section=${section.storageName} profile=${profileToken(profileId)} " +
                "source=$source roomRows=$roomRows returnedRows=$returnedRows",
        )
    }

    fun snapshotWrite(
        section: MediaSection,
        profileId: String,
        incomingCategories: Int,
        persistedCategories: Int,
        mediaItems: Int,
    ) {
        Log.i(
            Tag,
            "event=snapshot_write section=${section.storageName} profile=${profileToken(profileId)} " +
                "incomingCategories=$incomingCategories persistedCategories=$persistedCategories mediaItems=$mediaItems",
        )
    }

    fun rejectedSnapshot(
        section: MediaSection,
        profileId: String,
        incomingCategories: Int,
        mediaItems: Int,
    ) {
        Log.w(
            Tag,
            "event=snapshot_rejected section=${section.storageName} profile=${profileToken(profileId)} " +
                "incomingCategories=$incomingCategories mediaItems=$mediaItems",
        )
    }

    fun viewModelProjection(
        section: MediaSection,
        source: String,
        rawCategories: Int,
        allowedCategories: Int,
        stateCategories: Int,
        renderedCategories: Int,
        parentalEnabled: Boolean,
    ) {
        Log.i(
            Tag,
            "event=viewmodel_projection section=${section.storageName} source=$source raw=$rawCategories " +
                "allowed=$allowedCategories state=$stateCategories rendered=$renderedCategories " +
                "parentalEnabled=$parentalEnabled",
        )
    }

    fun platformGrouping(
        section: MediaSection,
        source: String,
        providerCategories: Int,
        platformFolders: Int,
        groupedCategories: Int,
        remainingCategories: Int,
        renderedCategories: Int,
        expanded: Boolean,
    ) {
        Log.i(
            Tag,
            "event=platform_grouping section=${section.storageName} source=$source providers=$providerCategories " +
                "platforms=$platformFolders grouped=$groupedCategories remaining=$remainingCategories " +
                "rendered=$renderedCategories expanded=$expanded",
        )
    }

    fun allCategoryOrder(
        section: MediaSection,
        source: String,
        providerCategories: Int,
        platformFolders: Int,
        groupedCategories: Int,
        orderedCategories: Int,
        offset: Int,
        limit: Int,
        returnedItems: Int,
        fallback: Boolean,
    ) {
        Log.i(
            Tag,
            "event=all_category_order section=${section.storageName} source=$source providers=$providerCategories " +
                "platforms=$platformFolders grouped=$groupedCategories ordered=$orderedCategories " +
                "offset=$offset limit=$limit returned=$returnedItems fallback=$fallback",
        )
    }

    private fun profileToken(profileId: String): String = profileId.hashCode().toUInt().toString(16)
}
