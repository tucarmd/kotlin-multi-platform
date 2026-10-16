package dev.pocketnotes.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.pocketnotes.db.NotesDb
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * [App 2 · B3/B4] iOS side of the database: the file lives in the app's Library/Application Support folder.
 *
 * Like the Android driver, NativeSqliteDriver creates the tables on first launch and runs the .sqm migrations
 * when the stored version is older than NotesDb.Schema.version.
 *
 * TRAP: this driver links against the system SQLite. Both the Gradle framework (linkerOpts) and the Xcode target
 * (OTHER_LDFLAGS in iosApp/project.yml) need -lsqlite3, otherwise the app fails to link with `_sqlite3_*` errors.
 *
 * Called by: Koin, when SqlNoteRepository asks for a SqlDriver.
 */
fun createDriver(): SqlDriver = NativeSqliteDriver(NotesDb.Schema, "notes.db")

actual val platformModule: Module = module {
    single<SqlDriver> { createDriver() }
}
