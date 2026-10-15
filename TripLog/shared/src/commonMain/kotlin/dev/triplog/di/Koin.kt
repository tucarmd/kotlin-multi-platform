package dev.triplog.di

import dev.triplog.core.TripLibrary
import dev.triplog.core.TripRecorder
import dev.triplog.data.SqlTripRepository
import dev.triplog.data.TripRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * [App 4 · B4] Koin wiring. Same shape in every practice app: commonModule + platformModule + initKoin().
 *
 * Why TripRecorder is a `single`: Android's TrackingService and Record screen, and iOS's TrackingModel, must all
 * talk to the SAME recorder. Two recorders would mean two GPS streams and two half-trips.
 */
val commonModule = module {
    // App-wide scope for the recorder and the library. Never cancelled: it lives as long as the process.
    // TRAP: not viewModelScope. A ViewModel scope dies on back-navigation and would stop a trip mid-way.
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<TripRepository> { SqlTripRepository(get()) }
    single { TripRecorder(location = get(), repository = get(), scope = get()) }
    single { TripLibrary(repository = get(), scope = get()) }
}

/** Binds the SQLite driver and the LocationStream for the current platform. */
expect val platformModule: Module

/** Android: `initKoin { androidContext(this); modules(appModule) }` in TripLogApplication. */
fun initKoin(config: KoinAppDeclaration) {
    startKoin {
        config()
        modules(commonModule, platformModule)
    }
}

/** iOS has nothing to configure. Swift sees this as `doInitKoin()` (Objective-C reserves the "init" prefix). */
fun initKoin() = initKoin {}
