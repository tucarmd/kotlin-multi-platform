package dev.pocketnotes.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.pocketnotes.android.R

/**
 * [App 2 · B5] Entry composable of the editor. Holds the draft (title, body, pin) and saves it when the user leaves.
 *
 * Why the draft lives here and not in the core: it is only "what is in the text fields right now". The core gets
 * one save() when the user leaves, and the rule planSave decides whether that means insert, update, delete or nothing.
 *
 * [id] = 0 means a new note.
 */
@Composable
fun NoteEditRoute(viewModel: NotesViewModel, id: Long, onDone: () -> Unit) {
    // Read once: the editor must not be overwritten by the list flow while the user types.
    val original = remember(id) { viewModel.find(id) }
    // rememberSaveable: the typed text survives rotation and the app being killed in the background.
    var title by rememberSaveable { mutableStateOf(original?.title.orEmpty()) }
    var body by rememberSaveable { mutableStateOf(original?.body.orEmpty()) }
    var pinned by rememberSaveable { mutableStateOf(original?.pinned ?: false) }
    var left by remember { mutableStateOf(false) }

    // TRAP: save on an explicit "leave" (Back button, back gesture), not in a DisposableEffect's onDispose.
    // onDispose also runs on rotation: a new note would be inserted once per rotation.
    // TRAP: `left` makes leaving happen once. A double tap on Back would otherwise insert the new note twice.
    val leave = {
        if (!left) {
            left = true
            viewModel.save(id, title, body, pinned)
            onDone()
        }
    }
    BackHandler(onBack = leave)

    NoteEditScreen(
        title = title,
        body = body,
        pinned = pinned,
        canDelete = id != 0L,
        onTitleChange = { title = it },
        onBodyChange = { body = it },
        onTogglePin = { pinned = !pinned },
        onBack = leave,
        onDelete = {
            if (!left) {
                left = true
                viewModel.delete(id) // the list then shows "Note deleted · Undo"
                onDone()
            }
        },
    )
}

/** [App 2 · B5] Title field, body field, pin and delete buttons. Plain values in, lambdas out: previewable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    title: String,
    body: String,
    pinned: Boolean,
    canDelete: Boolean,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onTogglePin: () -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    // Borderless fields: the editor should look like a page, not a form.
    val plain = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = stringResource(if (pinned) R.string.unpin else R.string.pin),
                            tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    if (canDelete) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 4.dp)) {
            TextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = { Text(stringResource(R.string.title_hint)) },
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                singleLine = true,
                colors = plain,
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = body,
                onValueChange = onBodyChange,
                placeholder = { Text(stringResource(R.string.body_hint)) },
                colors = plain,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteEditPreview() {
    MaterialTheme {
        NoteEditScreen(
            title = "Ghi chú đầu tiên",
            body = "Mua sữa, trứng\nGọi cho mẹ",
            pinned = true,
            canDelete = true,
            onTitleChange = {}, onBodyChange = {}, onTogglePin = {}, onBack = {}, onDelete = {},
        )
    }
}
