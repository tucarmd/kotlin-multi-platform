package dev.pocketnotes.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.pocketnotes.core.NotesCore
import dev.pocketnotes.data.NoteRepository
import dev.pocketnotes.model.Note

/**
 * [App 2 · B5] Thin Android wrapper around the shared NotesCore.
 *
 * Why it builds the core itself instead of getting it from Koin: NotesCore needs a CoroutineScope, and
 * `viewModelScope` only exists inside a ViewModel. It survives rotation and is cancelled when the Activity finishes,
 * so the core's database collection and undo timer stop on their own (no close() needed on Android).
 *
 * No logic here: every function forwards to NotesCore, which is tested in shared/commonTest.
 * Its iOS twin is NotesModel.swift.
 */
class NotesViewModel(repository: NoteRepository) : ViewModel() {
    private val core = NotesCore(repository, viewModelScope)
    val state = core.state

    fun search(text: String) = core.search(text)
    fun refreshClock() = core.refreshClock()
    fun find(id: Long): Note? = core.find(id)
    fun save(id: Long, title: String, body: String, pinned: Boolean) = core.save(id, title, body, pinned)
    fun togglePin(id: Long) = core.togglePin(id)
    fun delete(id: Long) = core.delete(id)
    fun undoDelete() = core.undoDelete()
    fun dismissUndo() = core.dismissUndo()
}
