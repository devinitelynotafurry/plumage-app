package dev.plumage.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.plumage.data.prefs.SettingsRepository
import dev.plumage.data.repo.CollectionRepository
import dev.plumage.data.repo.PostRepository
import dev.plumage.domain.model.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val posts: PostRepository,
    private val collections: CollectionRepository
) : ViewModel() {

    val settings: StateFlow<Settings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    val seenCount: StateFlow<Int> = posts.observeSeenCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setUsername(value: String) = viewModelScope.launch { settingsRepo.setUsername(value) }
    fun setBlockedTags(value: String) = viewModelScope.launch { settingsRepo.setBlockedTags(value) }
    fun setFilterAi(value: Boolean) = viewModelScope.launch { settingsRepo.setFilterAi(value) }
    fun setDynamicColor(value: Boolean) = viewModelScope.launch { settingsRepo.setDynamicColor(value) }

    fun forgetSwipeHistory() = viewModelScope.launch { posts.clearSeenHistory() }

    fun eraseEverything() = viewModelScope.launch {
        posts.clearSeenHistory()
        collections.clearAll()
        settingsRepo.clearAll()
    }
}
