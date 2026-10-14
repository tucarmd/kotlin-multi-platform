package dev.devicepulse

import android.app.Application
import dev.devicepulse.di.initKoin
import org.koin.android.ext.koin.androidContext

class DevicePulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@DevicePulseApp) }
    }
}
