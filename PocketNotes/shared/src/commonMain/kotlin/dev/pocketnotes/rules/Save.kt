package dev.pocketnotes.rules

import dev.pocketnotes.model.Note
import dev.pocketnotes.model.NoteDraft
import dev.pocketnotes.model.SaveAction
import kotlin.time.Instant

/**
 * [App 2 · B1] What happens when the user leaves the editor, as a pure function.
 *
 * Why a rule and not an `if` in the core: "empty note is not created" and "opening a note without changing it does
 * not move it to the top" are the two behaviours people notice first. As a pure function each is a one-line test.
 *
 * Called by: NotesCore.save. Calls: nothing.
 */

/**
 * Decides how to store [draft]. [existing] is the saved version of the same note, or null for a new note.
 *
 * Examples:
 * new + blank title and body → Skip · new + "Milk" → Insert(updatedAt = now) ·
 * existing, nothing changed → Skip · existing, only the pin changed → Update (updatedAt kept) ·
 * existing, text changed → Update(updatedAt = now) · existing, title and body cleared → Delete(id).
 */
fun planSave(existing: Note?, draft: NoteDraft, now: Instant): SaveAction {
    val blank = draft.title.isBlank() && draft.body.isBlank()
    if (existing == null) {
        // TEST: RulesTest.`a new note left empty is not created`
        return if (blank) SaveAction.Skip else SaveAction.Insert(
            Note(id = 0, title = draft.title, body = draft.body, pinned = draft.pinned, updatedAt = now),
        )
    }
    if (blank) return SaveAction.Delete(existing.id)

    val textChanged = existing.title != draft.title || existing.body != draft.body
    return when {
        textChanged -> SaveAction.Update(
            existing.copy(title = draft.title, body = draft.body, pinned = draft.pinned, updatedAt = now),
        )
        // TRAP: only the pin changed: keep updatedAt, otherwise pinning a note would also make it the "newest".
        existing.pinned != draft.pinned -> SaveAction.Update(existing.copy(pinned = draft.pinned))
        else -> SaveAction.Skip
    }
}
