package dev.plumage.ui.viewer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.plumage.data.repo.CollectionRepository
import dev.plumage.domain.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViewerUiState(
    val post: Post? = null,
    val exporting: Boolean = false,
    val message: String? = null,
    val deleted: Boolean = false
)

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val collections: CollectionRepository,
    private val exporter: MediaExporter
) : ViewModel() {

    private val _state = MutableStateFlow(ViewerUiState())
    val state: StateFlow<ViewerUiState> = _state.asStateFlow()

    fun load(collectionName: String, postId: Long) = viewModelScope.launch {
        val post = collections.observeCollection(collectionName).first()
            .firstOrNull { it.id == postId }
        _state.value = _state.value.copy(post = post)
    }

    fun export(context: Context) {
        val post = _state.value.post ?: return
        _state.value = _state.value.copy(exporting = true)
        viewModelScope.launch {
            val result = exporter.export(context, post.fileUrl, post.id, post.ext)
            _state.value = _state.value.copy(
                exporting = false,
                message = when (result) {
                    is MediaExporter.Result.Saved -> "Saved to Pictures/Plumage."
                    is MediaExporter.Result.Failed -> result.reason
                }
            )
        }
    }

    /**
     * Removes the post from this collection only. The seen ledger is deliberately
     * left alone: the user has already ruled on this post, and un-burying it here
     * would put it back in the deck unannounced.
     */
    fun delete(collectionName: String) {
        val post = _state.value.post ?: return
        viewModelScope.launch {
            collections.remove(post.id, collectionName)
            _state.value = _state.value.copy(deleted = true)
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
