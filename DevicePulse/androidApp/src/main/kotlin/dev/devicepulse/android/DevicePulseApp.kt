package dev.devicepulse.android

import android.app.Application
import dev.devicepulse.android.ui.PulseViewModel
import dev.devicepulse.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Android-only bindings, on top of shared's commonModule + platformModule. */
val appModule = module {
    viewModelOf(::PulseViewModel)
}

class DevicePulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@DevicePulseApp)
            modules(appModule)
        }
    }
}
