package dev.pocketnotes.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pocketnotes.android.R
import dev.pocketnotes.model.Note
import dev.pocketnotes.model.NoteItem
import dev.pocketnotes.model.NotesState
import dev.pocketnotes.model.RelativeTime
import kotlin.time.Instant

/**
 * [App 2 · B5] Entry composable of the list: reads the shared ViewModel and turns its StateFlow into Compose state.
 *
 * Why split Route / Screen: NoteListScreen takes plain values and lambdas, so @Preview can draw it without Koin.
 */
@Composable
fun NoteListRoute(viewModel: NotesViewModel, onOpen: (Long) -> Unit, onNew: () -> Unit) {
    // collectAsStateWithLifecycle stops collecting while the app is in the background.
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Each time the list is shown again (back from the editor, app resumed), "5 min ago" is recomputed.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshClock()
        onPauseOrDispose { }
    }
    NoteListScreen(
        state = state,
        onSearch = viewModel::search,
        onOpen = onOpen,
        onNew = onNew,
        onTogglePin = viewModel::togglePin,
        onDelete = viewModel::delete,
        onUndo = viewModel::undoDelete,
        onDismissUndo = viewModel::dismissUndo,
    )
}

/** [App 2 · B5] Search field + list + "+" button + Undo snackbar. Draws [state] and fires actions; decides nothing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    state: NotesState,
    onSearch: (String) -> Unit,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    onTogglePin: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onUndo: () -> Unit,
    onDismissUndo: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val deleted = (state as? NotesState.Ready)?.recentlyDeleted
    val deletedText = stringResource(R.string.deleted)
    val undoText = stringResource(R.string.undo)

    // The core owns the 5 s timer. When it clears recentlyDeleted, this effect is cancelled
    // and the snackbar disappears with it, so both platforms hide "Undo" at the same moment.
    LaunchedEffect(deleted) {
        if (deleted == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(deletedText, actionLabel = undoText, duration = SnackbarDuration.Indefinite)
        if (result == SnackbarResult.ActionPerformed) onUndo() else onDismissUndo()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNew) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_note)) }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                NotesState.Loading -> Centered { CircularProgressIndicator() }
                NotesState.Failed -> Centered { Text(stringResource(R.string.load_failed), color = MaterialTheme.colorScheme.error) }
                is NotesState.Ready -> {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = onSearch,
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    when {
                        !state.hasNotes -> EmptyState(stringResource(R.string.empty_title), stringResource(R.string.empty_body))
                        state.items.isEmpty() -> EmptyState(stringResource(R.string.no_results, state.query.trim()), null)
                        else -> NoteList(state.items, onOpen, onTogglePin, onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteList(items: List<NoteItem>, onOpen: (Long) -> Unit, onTogglePin: (Long) -> Unit, onDelete: (Long) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        // TRAP: key = note id. Without a key, the swipe state of a deleted row is reused by the row that slides
        // into its place, and that row shows up half-swiped (or gets deleted too).
        items(items, key = { it.note.id }) { item ->
            SwipeToDelete(onDelete = { onDelete(item.note.id) }) {
                NoteRow(item, onClick = { onOpen(item.note.id) }, onTogglePin = { onTogglePin(item.note.id) })
            }
            HorizontalDivider()
        }
    }
}

/**
 * Swipe right-to-left to delete. The row disappears because the database flow emits without it, not because of the swipe.
 *
 * TRAP: plain `remember`, NOT rememberSwipeToDismissBoxState(). That one uses rememberSaveable, and LazyColumn keeps
 * saved state per item key even after the item is gone. After "Undo" the note comes back with the same id, its row
 * restores the "swiped away" state, and onDismiss deletes it a second time. Found by running Undo on an emulator;
 * the core tests cannot see it because it lives in the UI.
 */
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val threshold = SwipeToDismissBoxDefaults.positionalThreshold
    val swipe = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, density, positionalThreshold = threshold) }
    SwipeToDismissBox(
        state = swipe,
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
            }
        },
    ) { content() }
}

@Composable
private fun NoteRow(item: NoteItem, onClick: () -> Unit, onTogglePin: () -> Unit) {
    val note = item.note
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                note.title.ifBlank { stringResource(R.string.untitled) },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (note.body.isNotBlank()) {
                Text(note.body, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(item.edited.label(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        // material-icons-core has no "push pin" or empty star, so the colour tells pinned from not pinned.
        IconButton(onClick = onTogglePin) {
            Icon(
                Icons.Filled.Star,
                contentDescription = stringResource(if (note.pinned) R.string.unpin else R.string.pin),
                tint = if (note.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String?) = Centered {
    Text(title, style = MaterialTheme.typography.titleMedium)
    if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

/** Preview with a hand-made state: one pinned note, one plain note, one with no title. */
@Preview(showBackground = true)
@Composable
private fun NoteListPreview() {
    val at = Instant.parse("2026-10-15T12:00:00Z")
    val items = listOf(
        NoteItem(Note(1, "Ghi chú đầu tiên", "Mua sữa, trứng\nGọi cho mẹ", pinned = true, updatedAt = at), RelativeTime.MinutesAgo(5)),
        NoteItem(Note(2, "Shopping", "Milk, eggs, bread", pinned = false, updatedAt = at), RelativeTime.Yesterday),
        NoteItem(Note(3, "", "A note without a title", pinned = false, updatedAt = at), RelativeTime.OnDate(2026, 9, 1)),
    )
    MaterialTheme {
        NoteListScreen(
            state = NotesState.Ready(query = "", items = items, hasNotes = true, recentlyDeleted = null),
            onSearch = {}, onOpen = {}, onNew = {}, onTogglePin = {}, onDelete = {}, onUndo = {}, onDismissUndo = {},
        )
    }
}
