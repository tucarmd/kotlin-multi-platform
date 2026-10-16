package dev.pocketnotes

import dev.pocketnotes.data.NoteRepository
import dev.pocketnotes.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * [App 2 · B7] In-memory NoteRepository for NotesCoreTest. Behaves like the SQL one (emits after each write)
 * without a database, so the core tests run in milliseconds on the JVM and on the iOS Simulator.
 */
class FakeNoteRepository(vararg initial: Note) : NoteRepository {
    val notes = MutableStateFlow(initial.toList())
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeAll(): Flow<List<Note>> = notes

    override suspend fun insert(note: Note): Long {
        val id = nextId++
        notes.update { it + note.copy(id = id) }
        return id
    }

    override suspend fun upsert(note: Note) {
        notes.update { list -> list.filterNot { it.id == note.id } + note }
    }

    override suspend fun delete(id: Long) {
        notes.update { list -> list.filterNot { it.id == id } }
    }
}
