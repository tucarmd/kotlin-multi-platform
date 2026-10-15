package dev.pocketnotes

import app.cash.turbine.test
import dev.pocketnotes.core.NotesCore
import dev.pocketnotes.model.Note
import dev.pocketnotes.model.NotesState
import dev.pocketnotes.model.RelativeTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * [App 2 · B7] NotesCore with a FakeNoteRepository. No database: the fake emits after each write, like SQLDelight's asFlow().
 *
 * The core runs in runTest's `backgroundScope` (it plays viewModelScope), so `advanceTimeBy` drives the 5 s undo
 * timer on virtual time: the test does not really wait 5 seconds.
 */
class NotesCoreTest {

    private val start = Instant.parse("2026-10-15T12:00:00Z")

    /** A clock the test moves by hand. */
    private class TestClock(var now: Instant) : Clock {
        override fun now(): Instant = now
    }

    private val clock = TestClock(start)

    private fun note(id: Long, title: String, pinned: Boolean = false, minutesAgo: Int = 0) =
        Note(id, title, body = "", pinned = pinned, updatedAt = start - minutesAgo.minutes)

    private fun TestScope.core(repository: FakeNoteRepository, scope: CoroutineScope = backgroundScope) =
        NotesCore(repository, scope, clock, TimeZone.UTC).also { runCurrent() }

    private fun NotesCore.ready(): NotesState.Ready = assertIs<NotesState.Ready>(state.value)

    private fun NotesCore.titles(): List<String> = ready().items.map { it.note.title }

    @Test
    fun `state goes from loading to ready with notes sorted`() = runTest {
        val repo = FakeNoteRepository(note(1, "Old", minutesAgo = 30), note(2, "Pinned", pinned = true, minutesAgo = 60))
        val core = NotesCore(repo, backgroundScope, clock, TimeZone.UTC)
        core.state.test {
            assertEquals(NotesState.Loading, awaitItem())
            val ready = assertIs<NotesState.Ready>(awaitItem())
            assertEquals(listOf("Pinned", "Old"), ready.items.map { it.note.title })
            assertEquals(RelativeTime.MinutesAgo(30), ready.items[1].edited)
            assertTrue(ready.hasNotes)
        }
    }

    @Test
    fun `search filters without diacritics and keeps hasNotes true`() = runTest {
        val core = core(FakeNoteRepository(note(1, "Ghi chú"), note(2, "Shopping")))
        core.search("ghi chu")
        runCurrent()
        assertEquals(listOf("Ghi chú"), core.titles())
        core.search("nothing like this")
        runCurrent()
        assertEquals(emptyList(), core.titles())
        assertTrue(core.ready().hasNotes) // "no match" and not "no notes yet"
    }

    @Test
    fun `saving a new note inserts it at the top`() = runTest {
        val core = core(FakeNoteRepository(note(1, "Old", minutesAgo = 5)))
        core.save(id = 0, title = "New", body = "text", pinned = false)
        runCurrent()
        assertEquals(listOf("New", "Old"), core.titles())
    }

    @Test
    fun `saving an empty new note creates nothing`() = runTest {
        val repo = FakeNoteRepository()
        val core = core(repo)
        core.save(id = 0, title = " ", body = "", pinned = true)
        runCurrent()
        assertEquals(emptyList(), repo.notes.value)
        assertEquals(false, core.ready().hasNotes)
    }

    @Test
    fun `delete then undo puts the note back in the same place`() = runTest {
        val core = core(FakeNoteRepository(note(1, "A", minutesAgo = 1), note(2, "B", minutesAgo = 2), note(3, "C", minutesAgo = 3)))
        core.delete(2)
        runCurrent()
        assertEquals(listOf("A", "C"), core.titles())
        assertEquals("B", core.ready().recentlyDeleted?.title)

        core.undoDelete()
        runCurrent()
        assertEquals(listOf("A", "B", "C"), core.titles())
        assertNull(core.ready().recentlyDeleted)
        assertEquals(2, core.find(2)?.id) // same id as before
    }

    @Test
    fun `the undo offer disappears after 5 seconds and the delete stays`() = runTest {
        val core = core(FakeNoteRepository(note(1, "A")))
        core.delete(1)
        advanceTimeBy(4.9.seconds)
        assertEquals("A", core.ready().recentlyDeleted?.title)
        advanceTimeBy(0.2.seconds)
        assertNull(core.ready().recentlyDeleted)
        core.undoDelete() // too late: nothing to undo
        runCurrent()
        assertEquals(emptyList(), core.titles())
    }

    @Test
    fun `toggling the pin moves a note up without changing its time`() = runTest {
        val core = core(FakeNoteRepository(note(1, "New"), note(2, "Old", minutesAgo = 90)))
        core.togglePin(2)
        runCurrent()
        assertEquals(listOf("Old", "New"), core.titles())
        assertEquals(start - 90.minutes, core.find(2)?.updatedAt)
    }

    @Test
    fun `refreshClock updates the relative time labels`() = runTest {
        val core = core(FakeNoteRepository(note(1, "A")))
        assertEquals(RelativeTime.JustNow, core.ready().items.single().edited)
        clock.now = start + 2.hours
        core.refreshClock()
        runCurrent()
        assertEquals(RelativeTime.HoursAgo(2), core.ready().items.single().edited)
    }

    @Test
    fun `a database error becomes the Failed state instead of a crash`() = runTest {
        val broken = object : dev.pocketnotes.data.NoteRepository by FakeNoteRepository() {
            override fun observeAll(): Flow<List<Note>> = flow { error("disk I/O error") }
        }
        val core = NotesCore(broken, backgroundScope, clock, TimeZone.UTC)
        runCurrent()
        assertEquals(NotesState.Failed, core.state.value)
    }
}
