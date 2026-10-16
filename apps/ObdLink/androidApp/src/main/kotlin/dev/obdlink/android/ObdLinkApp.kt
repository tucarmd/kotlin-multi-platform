package dev.obdlink.android

import android.app.Application
import dev.obdlink.android.ui.ObdViewModel
import dev.obdlink.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** [App 5 · B4] Android-only bindings, added on top of shared's bleModule + obdModule + platformModule. */
val appModule = module {
    viewModelOf(::ObdViewModel)
}

/**
 * [App 5 · B4] Starts Koin once, before any screen exists.
 * Why in Application and not MainActivity: the Activity is recreated on rotation; Koin (and the BLE link held by
 * the single ObdCore) must survive that.
 */
class ObdLinkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@ObdLinkApp)
            modules(appModule)
        }
    }
}
