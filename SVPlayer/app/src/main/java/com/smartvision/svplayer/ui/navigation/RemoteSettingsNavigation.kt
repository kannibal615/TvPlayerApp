package com.smartvision.svplayer.ui.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object RemoteSettingsNavigation {
    private val _requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requests = _requests.asSharedFlow()

    fun requestOpenSettings() {
        _requests.tryEmit(Unit)
    }
}
