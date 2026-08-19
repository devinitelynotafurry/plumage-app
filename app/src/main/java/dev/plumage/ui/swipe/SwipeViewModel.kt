package dev.plumage.ui.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.plumage.data.prefs.SettingsRepository
import dev.plumage.data.repo.CollectionRepository
import dev.plumage.data.repo.PostRepository
import dev.plumage.domain.QueryBuilder
import dev.plumage.domain.model.Post
import dev.plumage.domain.model.Settings
import dev.plumage.domain.model.SortMode
import dev.plumage.domain.model.TagSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SwipeUiState(
    val query: String = "",
    val activeSearch: String = "",
    val sort: SortMode = SortMode.NEWEST,
    val deck: List<Post> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val exhausted: Boolean = false,
    val droppedTags: List<String> = emptyList(),
    val hasSearched: Boolean = false,
    val canUndo: Boolean = false,
    val lastKeptInto: String? = null,
    val searching: Boolean = false
) {
    val topPost: Post? get() = deck.firstOrNull()
    val nextPost: Post? get() = deck.getOrNull(1)
}

private data class UndoEntry(val post: Post, val kept: Boolean, val collection: String)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SwipeViewModel @Inject constructor(
    private val posts: PostRepository,
    private val collections: CollectionRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SwipeUiState())
    val state: StateFlow<SwipeUiState> = _state.asStateFlow()

    private val activeToken = MutableStateFlow("")
    private val undoStack = ArrayDeque<UndoEntry>()

    /**
     * flatMapLatest is doing the important work here: every keystroke supersedes the
     * previous lookup, so a slow response for "fo" can never land after the response
     * for "fox" and repopulate the dropdown with stale suggestions.
     */
    val suggestions: StateFlow<List<TagSuggestion>> = activeToken
        .debounce(SUGGESTION_DEBOUNCE_MS)
        .distinctUntilChanged()
        .flatMapLatest { token ->
            if (token.isBlank()) {
                flowOf(posts.getPopularTags())
            } else if (token.length < PostRepository.MIN_AUTOCOMPLETE_CHARS) {
                flowOf(emptyList())
            } else {
                flowOf(posts.suggestTags(token))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val seenCount: StateFlow<Int> = posts.observeSeenCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            val saved = settingsRepo.lastQuery.first()
            val sort = settingsRepo.sortMode.first()
            _state.value = _state.value.copy(query = saved, sort = sort)
        }
    }

    fun onQueryChange(value: String) {
        _state.value = _state.value.copy(query = value)
        activeToken.value = value.substringAfterLast(' ').trim()
    }

    /** Replaces the token being typed, leaving earlier tags in the field alone. */
    fun applySuggestion(tag: String) {
        val current = _state.value.query
        val head = current.substringBeforeLast(' ', missingDelimiterValue = "")
        val negated = current.substringAfterLast(' ').startsWith("-")
        val chosen = if (negated) "-$tag" else tag
        val rebuilt = if (head.isBlank()) "$chosen " else "$head $chosen "
        _state.value = _state.value.copy(query = rebuilt)
        activeToken.value = ""
    }

    fun onSortChange(sort: SortMode) {
        if (sort == _state.value.sort) return
        _state.value = _state.value.copy(sort = sort)
        viewModelScope.launch { settingsRepo.setSortMode(sort) }
        if (_state.value.hasSearched) search(_state.value.activeSearch, restart = true)
    }

    fun browseEverything() = search("", restart = true)

    fun search(raw: String = _state.value.query, restart: Boolean = true) {
        val trimmed = raw.trim()
        viewModelScope.launch {
            settingsRepo.setLastQuery(trimmed)
            undoStack.clear()
            _state.value = _state.value.copy(
                query = trimmed,
                activeSearch = trimmed,
                deck = if (restart) emptyList() else _state.value.deck,
                hasSearched = true,
                canUndo = false,
                error = null,
                exhausted = false
            )
            activeToken.value = ""
            load(startPage = if (restart) 1 else null)
        }
    }

    fun keep() {
        val post = _state.value.topPost ?: return
        val collection = QueryBuilder.collectionNameFor(_state.value.activeSearch)
        undoStack.addLast(UndoEntry(post, kept = true, collection = collection))
        advance()
        _state.value = _state.value.copy(lastKeptInto = collection, canUndo = true)
        viewModelScope.launch {
            collections.save(post, collection)
            posts.markSeen(post.id, kept = true)
        }
    }

    fun bury() {
        val post = _state.value.topPost ?: return
        undoStack.addLast(
            UndoEntry(post, kept = false, QueryBuilder.collectionNameFor(_state.value.activeSearch))
        )
        advance()
        _state.value = _state.value.copy(canUndo = true)
        viewModelScope.launch { posts.markSeen(post.id, kept = false) }
    }

    fun undo() {
        val entry = undoStack.removeLastOrNull() ?: return
        _state.value = _state.value.copy(
            deck = listOf(entry.post) + _state.value.deck,
            canUndo = undoStack.isNotEmpty(),
            exhausted = false
        )
        viewModelScope.launch {
            posts.unmarkSeen(entry.post.id)
            if (entry.kept) collections.remove(entry.post.id, entry.collection)
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }

    fun openSearch() {
        _state.value = _state.value.copy(searching = true)
        activeToken.value = _state.value.query.substringAfterLast(' ').trim()
    }

    fun closeSearch() {
        _state.value = _state.value.copy(searching = false)
    }

    private fun advance() {
        val remaining = _state.value.deck.drop(1)
        _state.value = _state.value.copy(deck = remaining)
        if (remaining.size <= REFILL_THRESHOLD && !_state.value.loading && !_state.value.exhausted) {
            viewModelScope.launch { load(startPage = null) }
        }
    }

    private suspend fun load(startPage: Int?) {
        if (_state.value.loading) return
        _state.value = _state.value.copy(loading = true)
        val settings: Settings = settingsRepo.settings.first()
        runCatching {
            posts.fetch(
                rawQuery = _state.value.activeSearch,
                sort = _state.value.sort,
                settings = settings,
                startPage = startPage
            )
        }.onSuccess { page ->
            val existing = _state.value.deck.map { it.id }.toSet()
            _state.value = _state.value.copy(
                deck = _state.value.deck + page.posts.filterNot { it.id in existing },
                loading = false,
                exhausted = page.exhausted && page.posts.isEmpty(),
                droppedTags = page.droppedTags,
                error = null
            )
        }.onFailure { t ->
            _state.value = _state.value.copy(
                loading = false,
                error = t.message ?: "Could not reach e926."
            )
        }
    }

    companion object {
        const val SUGGESTION_DEBOUNCE_MS = 350L
        const val REFILL_THRESHOLD = 4
    }
}
