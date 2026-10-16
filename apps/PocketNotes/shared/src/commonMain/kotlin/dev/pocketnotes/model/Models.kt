package dev.pocketnotes.model

import kotlin.time.Instant

/**
 * [App 2 · B1] Plain data the whole app passes around. No logic lives here: rules/ decides, core/ stores, data/ persists.
 *
 * Why kotlin.time.Instant and not kotlinx.datetime.Instant: since Kotlin 2.1.20 the standard library has its own
 * Instant and Clock, and kotlinx-datetime 0.7+ builds on them (its old Instant is deprecated). kotlinx-datetime is
 * still needed for TimeZone / LocalDate, which "yesterday" depends on (see rules/Time.kt).
 */

/** One note as the app sees it. [id] is 0 only for a note that was never saved. */
data class Note(
    val id: Long,
    val title: String,
    val body: String,
    val pinned: Boolean,
    /** Last time the text changed. Pinning does not touch it, so pinning does not reorder the list. */
    val updatedAt: Instant,
)

/** What the editor hands back when the user leaves it. [id] = 0 means "new note". */
data class NoteDraft(
    val id: Long,
    val title: String,
    val body: String,
    val pinned: Boolean,
)

/**
 * What saving a draft should do to the database. Decided by the pure rule `planSave`, carried out by NotesCore.
 *
 * TRAP: sealed **class**, not sealed interface: Objective-C keeps subclass info only for classes,
 * so SKIE's `onEnum(of:)` gives Swift a proper enum. Same lesson as SplitResult in App 1.
 */
sealed class SaveAction {
    /** Nothing changed (or a new note was left empty): no write, so updatedAt does not jump. */
    data object Skip : SaveAction()
    data class Insert(val note: Note) : SaveAction()
    data class Update(val note: Note) : SaveAction()
    /** An existing note whose title and body were both cleared. */
    data class Delete(val id: Long) : SaveAction()
}

/**
 * "Edited 5 min ago" without any text: each UI maps a case to its own string (Labels.kt / Labels.swift).
 *
 * TRAP: no case is called `Date`: Swift would see `RelativeTime.Date` next to Foundation's `Date` in every view file.
 */
sealed class RelativeTime {
    /** Less than one minute ago, or in the future (phone clock moved back). */
    data object JustNow : RelativeTime()
    data class MinutesAgo(val minutes: Int) : RelativeTime()
    /** Earlier today, at least one hour ago. */
    data class HoursAgo(val hours: Int) : RelativeTime()
    /** The previous calendar day in the phone's time zone, even if that was only 2 hours ago. */
    data object Yesterday : RelativeTime()
    /** 2..6 calendar days ago. */
    data class DaysAgo(val days: Int) : RelativeTime()
    /** A week or more ago: show the date itself. [month] is 1..12. */
    data class OnDate(val year: Int, val month: Int, val day: Int) : RelativeTime()
}

/** One line of the list: the note plus how long ago it was edited, computed by the core with the current time. */
data class NoteItem(val note: Note, val edited: RelativeTime)

/**
 * Everything the list screen needs. Exactly one of three cases.
 * No exception ever reaches the UI: a database failure becomes [Failed].
 */
sealed class NotesState {
    /** Before the first read from the database. */
    data object Loading : NotesState()

    /** The database could not be read. */
    data object Failed : NotesState()

    data class Ready(
        /** The search text exactly as typed. */
        val query: String,
        /** Notes that match [query], already sorted: pinned first, then newest first. */
        val items: List<NoteItem>,
        /** False when the database is empty: lets the UI tell "no notes yet" from "no match for the search". */
        val hasNotes: Boolean,
        /** The note deleted less than 5 s ago, kept so "Undo" can put it back. null when there is nothing to undo. */
        val recentlyDeleted: Note?,
    ) : NotesState()
}
