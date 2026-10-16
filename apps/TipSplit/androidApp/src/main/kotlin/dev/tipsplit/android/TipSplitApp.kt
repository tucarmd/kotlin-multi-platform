package dev.tipsplit.android

import android.app.Application
import dev.tipsplit.android.ui.SplitViewModel
import dev.tipsplit.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** [App 1 · B4] Android-only bindings, added on top of shared's commonModule + platformModule. */
val appModule = module {
    viewModelOf(::SplitViewModel)
}

/**
 * [App 1 · B4] Starts Koin once, before any screen exists.
 * Why in Application and not MainActivity: the Activity is recreated on rotation; Koin must start only once.
 */
class TipSplitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@TipSplitApp)
            modules(appModule)
        }
    }
}
