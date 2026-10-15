package dev.pocketnotes.android

import android.app.Application
import dev.pocketnotes.android.ui.NotesViewModel
import dev.pocketnotes.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** [App 2 · B4] Android-only bindings, added on top of shared's commonModule + platformModule. */
val appModule = module {
    viewModelOf(::NotesViewModel)
}

/**
 * [App 2 · B4] Starts Koin once, before any screen exists.
 * Why in Application and not MainActivity: the Activity is recreated on rotation; Koin must start only once.
 * androidContext() is what lets platformModule open the SQLite file (AndroidSqliteDriver needs a Context).
 */
class PocketNotesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@PocketNotesApp)
            modules(appModule)
        }
    }
}
