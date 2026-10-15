package dev.ratewatch.di

import dev.ratewatch.core.RatesCore
import dev.ratewatch.data.RateApi
import dev.ratewatch.data.RateRepository
import dev.ratewatch.data.SqlRateRepository
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * [App 3 · B4] Koin wiring. Same shape in every practice app: commonModule + platformModule + initKoin().
 *
 * Why Koin pays off here: RatesCore needs a repository, which needs a RateApi (needs an HttpClient) and a SqlDriver.
 * The HttpClient engine and the driver differ per platform (platformModule); everything above them is common.
 *
 * Called by: RateWatchApp (Android), iOSApp.swift via doInitKoin(). Calls: platformModule.
 */
val commonModule = module {
    single { RateApi(get()) }
    single<RateRepository> { SqlRateRepository(get(), get()) }
    // One RatesCore for the whole app: both tabs, Android rotation and iOS re-renders share the same state.
    // Not singleOf(::RatesCore): that would ask Koin for the Clock and CoroutineScope too; the defaults are what we want.
    single { RatesCore(get()) }
}

/** HttpClient (OkHttp / Darwin engine) and SqlDriver (Android / native SQLite). See androidMain / iosMain. */
expect val platformModule: Module

/** Android: `initKoin { androidContext(this); modules(appModule) }` in RateWatchApp. */
fun initKoin(config: KoinAppDeclaration) {
    startKoin {
        config()
        modules(commonModule, platformModule)
    }
}

/** iOS has nothing to configure. Swift sees this as `doInitKoin()` (Objective-C reserves the "init" prefix). */
fun initKoin() = initKoin {}
