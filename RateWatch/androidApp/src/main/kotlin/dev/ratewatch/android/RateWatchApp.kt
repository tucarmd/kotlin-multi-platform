package dev.ratewatch.android

import android.app.Application
import dev.ratewatch.android.ui.RatesViewModel
import dev.ratewatch.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** [App 3 · B4] Android-only bindings, added on top of shared's commonModule + platformModule. */
val appModule = module {
    viewModelOf(::RatesViewModel)
}

/**
 * [App 3 · B4] Starts Koin once, before any screen exists.
 * Why in Application and not MainActivity: the Activity is recreated on rotation; Koin must start only once.
 * androidContext is needed here because platformModule builds the SQLite driver from it.
 */
class RateWatchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@RateWatchApp)
            modules(appModule)
        }
    }
}
