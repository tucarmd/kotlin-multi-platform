package dev.devicepulse.android

import android.app.Application
import dev.devicepulse.core.PulseCore
import dev.devicepulse.core.createPulseCore

class DevicePulseApp : Application() {
    /** Built once; every screen reads sensors and history through it. */
    lateinit var core: PulseCore
        private set

    override fun onCreate() {
        super.onCreate()
        core = createPulseCore(this)
    }
}
