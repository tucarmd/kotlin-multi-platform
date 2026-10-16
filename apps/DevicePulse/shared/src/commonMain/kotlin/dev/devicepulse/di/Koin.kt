package dev.devicepulse.di

import dev.devicepulse.core.PulseCore
import dev.devicepulse.sensor.LatencyProbe
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    singleOf(::LatencyProbe)
    singleOf(::PulseCore)
}

/** Binds the three sensors, the HttpClient engine and the SQLDelight driver for the current platform. */
expect val platformModule: Module

/** Android: `initKoin { androidContext(this); modules(appModule) }`. */
fun initKoin(config: KoinAppDeclaration) {
    startKoin {
        config()
        modules(commonModule, platformModule)
    }
}

/** iOS has nothing to configure. Swift sees this as `doInitKoin()` (Objective-C reserves the "init" prefix). */
fun initKoin() = initKoin {}
