package com.smartvision.svplayer.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.svplayer.domain.repository.CatalogRepository
import com.smartvision.svplayer.data.repository.emptyAccountProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class AccountViewModel(repository: CatalogRepository) : ViewModel() {
    val account = repository.observeAccount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyAccountProfile())
}
