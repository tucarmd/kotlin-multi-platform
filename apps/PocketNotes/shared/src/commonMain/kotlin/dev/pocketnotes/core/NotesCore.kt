package dev.pocketnotes.core

import dev.pocketnotes.data.NoteRepository
import dev.pocketnotes.model.Note
import dev.pocketnotes.model.NoteDraft
import dev.pocketnotes.model.NoteItem
import dev.pocketnotes.model.NotesState
import dev.pocketnotes.model.SaveAction
import dev.pocketnotes.rules.matches
import dev.pocketnotes.rules.planSave
import dev.pocketnotes.rules.relativeTime
import dev.pocketnotes.rules.sortNotes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * [App 2 · B2] NotesCore: holds the list state and receives every user action. Shared by both UIs.
 *
 * Why it takes a [scope]: the state is a live `combine` over the database flow, and the undo timer is a coroutine.
 * Both must stop when the screen owner goes away. Android passes `viewModelScope`; iOS gets a `MainScope()` from
 * `notesCore()` (iosMain) and calls [close].
 *
 * Why it is not a Koin singleton like App 1's TipCore: its lifetime belongs to that scope, which only the UI side owns.
 *
 * Pattern (same as App 1): actions only change the INPUTS (database, query, recently deleted, clock);
 * the state is always recomputed from them with pure rules, so it can never get out of sync.
 *
 * Called by: NotesViewModel (Android), NotesModel (iOS). Calls: NoteRepository, rules/ (matches, sortNotes,
 * relativeTime, planSave).
 */
class NotesCore(
    private val repository: NoteRepository,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    private val undoWindow: Duration = 5.seconds,
) {
    private val query = MutableStateFlow("")
    private val recentlyDeleted = MutableStateFlow<Note?>(null)

    // TRAP: "now" is an input like the others. Reading the clock inside the rule would make tests flaky,
    // and a list built once would say "just now" forever. The UI calls refreshClock() when the list appears.
    private val now = MutableStateFlow(clock.now())

    // Last list read from the database, unfiltered: find() and save() look notes up here, without a query.
    private val allNotes = MutableStateFlow<List<Note>>(emptyList())

    private var undoTimer: Job? = null

    // TRAP: each write runs on the repository's background dispatcher. Without this lock, "delete" then a quick
    // "undo" can reach SQLite in the wrong order and the note is deleted after it was restored.
    private val writeLock = Mutex()

    /** The list screen state. Android: collectAsStateWithLifecycle(). Swift: `for await` (SKIE). */
    val state: StateFlow<NotesState> = combine(
        repository.observeAll().onEach { allNotes.value = it },
        query,
        recentlyDeleted,
        now,
    ) { notes, q, deleted, time ->
        NotesState.Ready(
            query = q,
            items = sortNotes(notes.filter { matches(it, q) })
                .map { NoteItem(it, relativeTime(it.updatedAt, time, timeZone)) },
            hasNotes = notes.isNotEmpty(),
            recentlyDeleted = deleted,
        ) as NotesState
    }
        // No exception crosses into the UI: a broken database becomes a state the UI can draw.
        .catch { emit(NotesState.Failed) }
        .stateIn(scope, SharingStarted.Eagerly, NotesState.Loading)

    /** Filters the list as the user types. The text is kept as typed so the search field never jumps. */
    fun search(text: String) {
        query.value = text
    }

    /** Recomputes "5 min ago" labels. Call when the list (re)appears. */
    fun refreshClock() {
        now.value = clock.now()
    }

    /** The saved note with this id, or null (new note, or not loaded yet). The editor fills its fields from it. */
    fun find(id: Long): Note? = allNotes.value.firstOrNull { it.id == id }

    /**
     * Stores what the editor holds when the user leaves it. [id] = 0 creates a note.
     * An empty new note is not created; an existing note that was emptied is deleted (with Undo).
     * TEST: NotesCoreTest.`saving an empty new note creates nothing`
     */
    fun save(id: Long, title: String, body: String, pinned: Boolean) {
        val draft = NoteDraft(id = id, title = title, body = body, pinned = pinned)
        when (val action = planSave(existing = find(id).takeIf { id != 0L }, draft = draft, now = clock.now())) {
            SaveAction.Skip -> Unit
            is SaveAction.Insert -> write { repository.insert(action.note) }
            is SaveAction.Update -> write { repository.upsert(action.note) }
            is SaveAction.Delete -> delete(action.id)
        }
    }

    /** Pins or unpins without touching updatedAt (see planSave). */
    fun togglePin(id: Long) {
        val note = find(id) ?: return
        write { repository.upsert(note.copy(pinned = !note.pinned)) }
    }

    /**
     * Deletes the note now and keeps a copy for [undoWindow] (5 s) so [undoDelete] can put it back.
     * Why a copy in memory: undo does not need to read the database, and the note is really gone if the app is killed.
     * TEST: NotesCoreTest.`delete then undo puts the note back in the same place`
     */
    fun delete(id: Long) {
        val note = find(id) ?: return
        undoTimer?.cancel() // an older pending delete becomes final
        recentlyDeleted.value = note
        write { repository.delete(id) }
        undoTimer = scope.launch {
            delay(undoWindow)
            recentlyDeleted.value = null
        }
    }

    /** Puts the last deleted note back with its old id, pin and time, so it lands in the same place in the list. */
    fun undoDelete() {
        val note = recentlyDeleted.value ?: return
        undoTimer?.cancel()
        recentlyDeleted.value = null
        write { repository.upsert(note) }
    }

    /** Hides the Undo offer early (the user swiped the snackbar away). The delete stays. */
    fun dismissUndo() {
        undoTimer?.cancel()
        recentlyDeleted.value = null
    }

    /** Runs database writes one at a time, in the order the user did them (Mutex is first-come, first-served). */
    private fun write(block: suspend () -> Unit) {
        scope.launch { writeLock.withLock { block() } }
    }

    /** iOS only: stops the state flow and the undo timer. Android never calls it; viewModelScope ends by itself. */
    fun close() {
        scope.cancel()
    }
}
