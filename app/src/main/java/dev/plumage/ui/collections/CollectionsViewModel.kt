package dev.plumage.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.plumage.data.local.CollectionSummary
import dev.plumage.data.repo.CollectionRepository
import dev.plumage.domain.model.Post
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val repository: CollectionRepository
) : ViewModel() {

    val collections: StateFlow<List<CollectionSummary>> = repository.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeCollection(name: String): StateFlow<List<Post>> =
        repository.observeCollection(name)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteCollection(name: String) = viewModelScope.launch {
        repository.removeCollection(name)
    }
}
