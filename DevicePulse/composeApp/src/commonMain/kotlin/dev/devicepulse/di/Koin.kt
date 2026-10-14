package dev.devicepulse.di

import dev.devicepulse.sensor.LatencyProbe
import dev.devicepulse.ui.DashboardViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    singleOf(::LatencyProbe)
    viewModelOf(::DashboardViewModel)
}

/** Binds the three sensors, the HttpClient engine and the SQLDelight driver for the current platform. */
expect val platformModule: Module

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(commonModule, platformModule)
    }
}
