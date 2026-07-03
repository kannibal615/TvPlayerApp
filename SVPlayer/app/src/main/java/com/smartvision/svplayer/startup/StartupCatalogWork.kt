package com.smartvision.svplayer.startup

import com.smartvision.svplayer.core.config.PlaylistSource

enum class StartupCatalogWorkKind {
    None,
    Synchronize,
    LoadLocal,
}

data class StartupCatalogWorkRequest(
    val kind: StartupCatalogWorkKind = StartupCatalogWorkKind.None,
    val source: PlaylistSource = PlaylistSource.Xtream,
    val requestedAtMs: Long = 0L,
) {
    val active: Boolean
        get() = kind != StartupCatalogWorkKind.None

    companion object {
        val None = StartupCatalogWorkRequest()
    }
}
