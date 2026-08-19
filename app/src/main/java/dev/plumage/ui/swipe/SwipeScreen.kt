package dev.plumage.ui.swipe

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.plumage.domain.model.Post
import dev.plumage.domain.model.SortMode
import dev.plumage.ui.common.EmptyState
import dev.plumage.ui.common.PillButton
import dev.plumage.ui.common.RoundActionButton
import dev.plumage.ui.common.SegmentedRow
import dev.plumage.ui.common.SquareActionButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SwipeScreen(
    onOpenCollections: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: SwipeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val cardState = remember { SwipeCardState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.topPost?.id) { cardState.reset() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding()
                .padding(horizontal = 16.dp)
                .graphicsLayer {
                    alpha = if (state.searching) 0f else 1f
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchTrigger(
                    query = state.query,
                    onClick = viewModel::openSearch,
                    modifier = Modifier.weight(2f)
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SquareActionButton(
                        icon = Icons.Rounded.GridView,
                        contentDescription = "Collections",
                        onClick = onOpenCollections,
                        modifier = Modifier.weight(1f),
                        container = Color.Transparent
                    )
                    SquareActionButton(
                        icon = Icons.Rounded.Tune,
                        contentDescription = "Settings",
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f),
                        container = Color.Transparent
                    )
                }
            }

            SegmentedRow(
                options = SortMode.entries.map { it.label },
                selectedIndex = SortMode.entries.indexOf(state.sort),
                onSelect = { viewModel.onSortChange(SortMode.entries[it]) },
                modifier = Modifier.padding(top = 10.dp)
            )

            if (state.droppedTags.isNotEmpty()) {
                TagBudgetNotice(state.droppedTags)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    !state.hasSearched -> EmptyState(
                        title = "Start swiping",
                        body = "Search a tag above. Right keeps it in a collection named " +
                            "after that search, left buries it for good.",
                        action = {
                            PillButton("Browse everything", onClick = viewModel::browseEverything)
                        }
                    )

                    state.topPost == null && state.loading ->
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

                    state.topPost == null -> EmptyState(
                        title = "Nothing left here",
                        body = if (state.exhausted) {
                            "You have been through everything matching that search. " +
                                "Try a different tag, or clear your swipe history in Settings."
                        } else {
                            "No posts came back for that search. Check the spelling, or " +
                                "pick a tag from the suggestions as you type."
                        }
                    )

                    else -> {
                        state.nextPost?.let { next ->
                            UnderCard(progress = cardState.dragProgress) {
                                PostCard(post = next, fullBleed = false)
                            }
                        }
                        val top = state.topPost!!
                        SwipeCard(
                            state = cardState,
                            enabled = !state.loading || state.deck.isNotEmpty(),
                            scope = scope,
                            onDecided = { kept -> if (kept) viewModel.keep() else viewModel.bury() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PostCard(post = top, fullBleed = true)
                        }
                    }
                }
            }

            ActionBar(
                canUndo = state.canUndo,
                enabled = state.topPost != null,
                onBury = { scope.launch { cardState.commit(false) { viewModel.bury() } } },
                onUndo = viewModel::undo,
                onKeep = { scope.launch { cardState.commit(true) { viewModel.keep() } } }
            )
        }

        if (state.searching) {
            SearchOverlay(
                onBack = viewModel::closeSearch,
                viewModel = viewModel
            )
        }

        state.error?.let { message ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                action = {
                    Text(
                        "Dismiss",
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable { viewModel.dismissError() }
                            .clip(RoundedCornerShape(8.dp)),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            ) { Text(message) }
            LaunchedEffect(message) {
                delay(4000)
                viewModel.dismissError()
            }
        }
    }
}

@Composable
private fun SearchOverlay(
    onBack: () -> Unit,
    viewModel: SwipeViewModel
) {
    val state by viewModel.state.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Animates the "fall" from the top of the screen to the bottom (top of keyboard).
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateIn = true
        focusRequester.requestFocus()
    }

    val fallOffset by animateDpAsState(
        targetValue = if (animateIn) 0.dp else (-600).dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "search_fall"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .windowInsetsPadding(WindowInsets.ime)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp) // Space for the bar
            ) {
                if (suggestions.isEmpty() && state.query.isBlank()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        }
                    }
                }
                items(suggestions, key = { it.name }) { suggestion ->
                    SuggestionRow(suggestion) {
                        viewModel.applySuggestion(suggestion.name)
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .graphicsLayer { translationY = fallOffset.toPx() }
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 18.dp).size(22.dp)
                )
                TextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text("Search tags") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboard?.hide()
                        viewModel.search()
                        onBack()
                    }),
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@Composable
private fun PostCard(post: Post, fullBleed: Boolean) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(post.aspectRatio.coerceIn(0.62f, 1.1f))
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(post.sampleUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Artwork by ${post.artistLabel}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (fullBleed) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    CardChip(post.artistLabel)
                    CardChip("${post.score}")
                }
            }
        }
    }
}

@Composable
private fun CardChip(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TagBudgetNotice(dropped: List<String>) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
    ) {
        Text(
            text = "e926 allows six tags per search. Ignored: ${dropped.joinToString(", ")}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun ActionBar(
    canUndo: Boolean,
    enabled: Boolean,
    onBury: () -> Unit,
    onUndo: () -> Unit,
    onKeep: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundActionButton(
            icon = Icons.Rounded.Close,
            contentDescription = "Bury",
            onClick = onBury,
            container = MaterialTheme.colorScheme.surfaceVariant,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            enabled = enabled
        )
        Box(Modifier.padding(horizontal = 14.dp)) {
            RoundActionButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Undo last swipe",
                onClick = onUndo,
                container = MaterialTheme.colorScheme.tertiary,
                tint = MaterialTheme.colorScheme.onTertiary,
                enabled = canUndo,
                size = 56
            )
        }
        RoundActionButton(
            icon = Icons.Rounded.Check,
            contentDescription = "Keep",
            onClick = onKeep,
            container = MaterialTheme.colorScheme.primaryContainer,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            enabled = enabled
        )
    }
}
