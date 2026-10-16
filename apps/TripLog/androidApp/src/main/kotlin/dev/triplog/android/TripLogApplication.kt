package dev.triplog.android

import android.app.Application
import dev.triplog.android.ui.RecordViewModel
import dev.triplog.android.ui.TripsViewModel
import dev.triplog.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** [App 4 · B4] Android-only bindings, added on top of shared's commonModule + platformModule. */
val appModule = module {
    viewModelOf(::RecordViewModel)
    viewModelOf(::TripsViewModel)
}

/**
 * [App 4 · B4] Starts Koin once, before any Activity or Service exists.
 *
 * Why in Application: TrackingService can be created without the Activity (e.g. its notification's Stop button),
 * and both must find the same TripRecorder single.
 */
class TripLogApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@TripLogApplication)
            modules(appModule)
        }
    }
}
