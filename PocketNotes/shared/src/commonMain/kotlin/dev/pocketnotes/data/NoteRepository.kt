package dev.pocketnotes.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import dev.pocketnotes.db.NotesDb
import dev.pocketnotes.model.Note
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Instant

/**
 * [App 2 · B3] NoteRepository — the only door to stored notes.
 *
 * Why an interface: NotesCore is tested with a FakeNoteRepository (a MutableStateFlow, no database, runs in
 * milliseconds on JVM and iOS), and swapping SQLDelight for something else later only changes [SqlNoteRepository].
 *
 * Called by: NotesCore. Implemented by: SqlNoteRepository (app), FakeNoteRepository (commonTest).
 */
interface NoteRepository {
    /** Every stored note, unsorted. Emits again by itself after any insert / upsert / delete. */
    fun observeAll(): Flow<List<Note>>

    /** Stores a new note and returns the id the database gave it. [Note.id] is ignored. */
    suspend fun insert(note: Note): Long

    /** Writes [note] under its own id: updates it, or re-creates it after a delete (Undo). */
    suspend fun upsert(note: Note)

    suspend fun delete(id: Long)
}

/**
 * [App 2 · B3] NoteRepository on SQLDelight. The driver comes from platformModule (Android / iOS differ only there).
 *
 * Calls: NoteQueries, generated from commonMain/sqldelight/.../Note.sq.
 */
class SqlNoteRepository(
    driver: SqlDriver,
    // Injectable so NoteRepositoryTest could pin it; the app uses the default.
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : NoteRepository {

    private val queries = NotesDb(driver).noteQueries

    /**
     * `asFlow()` re-runs selectAll whenever the noteRecord table changes, so the UI never has to "reload" after a save.
     * TEST: NoteRepositoryTest.`observeAll emits again after every change`
     */
    override fun observeAll(): Flow<List<Note>> =
        queries.selectAll().asFlow()
            // TRAP: mapToList needs a dispatcher. Without it the query runs on the collector's thread,
            // which is the main thread on both platforms.
            .mapToList(dispatcher)
            .map { rows ->
                rows.map { row ->
                    Note(
                        id = row.id,
                        title = row.title,
                        body = row.body,
                        pinned = row.pinned != 0L,
                        updatedAt = Instant.fromEpochMilliseconds(row.updatedAtMillis),
                    )
                }
            }

    override suspend fun insert(note: Note): Long = withContext(dispatcher) {
        // TRAP: last_insert_rowid() belongs to the connection. Running both statements in one transaction keeps
        // them on the same connection, so another write cannot slip in between and return the wrong id.
        queries.transactionWithResult {
            queries.insert(note.title, note.body, note.updatedAt.toEpochMilliseconds(), note.pinned.toLong())
            queries.lastInsertedId().executeAsOne()
        }
    }

    override suspend fun upsert(note: Note) {
        withContext(dispatcher) {
            queries.upsert(note.id, note.title, note.body, note.updatedAt.toEpochMilliseconds(), note.pinned.toLong())
        }
    }

    override suspend fun delete(id: Long) {
        withContext(dispatcher) { queries.deleteById(id) }
    }

    private fun Boolean.toLong(): Long = if (this) 1L else 0L
}
