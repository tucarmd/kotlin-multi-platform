package dev.pocketnotes.di

import dev.pocketnotes.data.NoteRepository
import dev.pocketnotes.data.SqlNoteRepository
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * [App 2 · B4] Koin wiring. Same shape as App 1: commonModule + platformModule + initKoin().
 *
 * New in App 2: platformModule is no longer empty. It binds the SqlDriver, the only thing that differs between
 * Android and iOS. Everything above the driver (repository, core) is common code.
 *
 * NotesCore is NOT registered here: it needs a CoroutineScope owned by the UI side
 * (Android: viewModelScope in NotesViewModel; iOS: MainScope in notesCore()).
 */
val commonModule = module {
    // One repository (one database connection) for the whole app.
    single<NoteRepository> { SqlNoteRepository(driver = get()) }
}

/** Binds `SqlDriver`: AndroidSqliteDriver on Android, NativeSqliteDriver on iOS. See PlatformModule.*.kt. */
expect val platformModule: Module

/** Android: `initKoin { androidContext(this); modules(appModule) }` in PocketNotesApp. */
fun initKoin(config: KoinAppDeclaration) {
    startKoin {
        config()
        modules(commonModule, platformModule)
    }
}

/** iOS has nothing to configure. Swift sees this as `doInitKoin()` (Objective-C reserves the "init" prefix). */
fun initKoin() = initKoin {}
