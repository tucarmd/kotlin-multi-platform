package dev.pocketnotes.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.pocketnotes.db.NotesDb
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * [App 2 · B3/B4] Android side of the database: the file lives in the app's private data folder.
 *
 * Why the Schema is passed in: AndroidSqliteDriver creates the tables on first launch and, when the stored
 * version (PRAGMA user_version) is older than NotesDb.Schema.version, runs the .sqm migrations (1.sqm: add `pinned`).
 *
 * Called by: Koin, when SqlNoteRepository asks for a SqlDriver.
 */
fun createDriver(context: Context): SqlDriver =
    // PLATFORM: needs a Context to find the data folder; iOS (NativeSqliteDriver) does not.
    AndroidSqliteDriver(NotesDb.Schema, context, "notes.db")

actual val platformModule: Module = module {
    single<SqlDriver> { createDriver(androidContext()) }
}
