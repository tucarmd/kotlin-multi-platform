package dev.pocketnotes

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import dev.pocketnotes.data.SqlNoteRepository
import dev.pocketnotes.db.NotesDb
import dev.pocketnotes.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

/**
 * [App 2 · B7] SqlNoteRepository against a real SQLite engine (in memory, on the JVM).
 *
 * Why androidUnitTest and not commonTest: JdbcSqliteDriver is JVM-only. The SQL is the same file the phones run,
 * so this checks Note.sq, 1.sqm and the row ↔ Note mapping without an emulator.
 */
class NoteRepositoryTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    private val at = Instant.fromEpochMilliseconds(1_760_000_000_000)

    @AfterTest
    fun closeDriver() = driver.close()

    private fun freshRepository(): SqlNoteRepository {
        NotesDb.Schema.create(driver)
        return SqlNoteRepository(driver)
    }

    @Test
    fun `insert returns the new id and the note reads back the same`() = runTest {
        val repo = freshRepository()
        val id = repo.insert(Note(0, "Ghi chú", "Nội dung", pinned = true, updatedAt = at))
        assertEquals(listOf(Note(id, "Ghi chú", "Nội dung", true, at)), repo.observeAll().first())
    }

    @Test
    fun `observeAll emits again after every change`() = runTest {
        val repo = freshRepository()
        repo.observeAll().test {
            assertEquals(0, awaitItem().size)
            val id = repo.insert(Note(0, "A", "", false, at))
            assertEquals(1, awaitItem().size)
            repo.upsert(Note(id, "A2", "", false, at))
            assertEquals("A2", awaitItem().single().title)
            repo.delete(id)
            assertEquals(0, awaitItem().size)
        }
    }

    @Test
    fun `upsert after delete restores the note with its old id`() = runTest {
        val repo = freshRepository()
        val id = repo.insert(Note(0, "Keep me", "", false, at))
        val saved = repo.observeAll().first().single()
        repo.delete(id)
        repo.upsert(saved)
        assertEquals(listOf(saved), repo.observeAll().first())
    }

    @Test
    fun `migration from schema 1 keeps old notes and adds pinned`() = runTest {
        // Version 1 of the app, written by hand: the same table without the pinned column.
        driver.execute(
            null,
            "CREATE TABLE noteRecord (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, " +
                "body TEXT NOT NULL, updatedAtMillis INTEGER NOT NULL)",
            0,
        )
        driver.execute(null, "INSERT INTO noteRecord(title, body, updatedAtMillis) VALUES ('Old note', 'from v1', 1000)", 0)

        assertEquals(2L, NotesDb.Schema.version)
        NotesDb.Schema.migrate(driver, oldVersion = 1, newVersion = NotesDb.Schema.version)

        val repo = SqlNoteRepository(driver)
        val old = repo.observeAll().first().single()
        assertEquals("Old note", old.title)
        assertFalse(old.pinned)

        // The new column is really usable after the migration
        repo.upsert(old.copy(pinned = true))
        assertEquals(true, repo.observeAll().first().single().pinned)
    }
}
