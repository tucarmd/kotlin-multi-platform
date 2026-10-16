package dev.tipsplit.di

import dev.tipsplit.core.TipCore
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * [App 1 · B4] Koin wiring. Same shape in every practice app: commonModule + platformModule + initKoin().
 *
 * Why Koin for a one-class app: so the shape is already familiar when App 2 adds a database driver
 * that really differs per platform.
 *
 * [Bài 5 · Koin: commonModule + platformModule] commonModule lists what is the same everywhere (TipCore).
 * platformModule lists what each platform must provide itself. initKoin() starts both, and each app adds its own
 * extras through `config` (Android adds appModule with SplitViewModel).
 */
val commonModule = module {
    // One TipCore for the whole app: Android rotation and iOS re-renders keep the same state.
    singleOf(::TipCore)
}

/**
 * Things that differ per platform. Empty in App 1 on purpose: see androidMain / iosMain.
 *
 * [Bài 4 · expect / actual] `expect` = "every target must supply this". The compiler then requires an `actual`
 * with the same name and package in androidMain (PlatformModule.android.kt) and iosMain (PlatformModule.ios.kt);
 * delete one and that target stops compiling. commonMain code uses platformModule without knowing which one it got.
 */
expect val platformModule: Module

/** Android: `initKoin { androidContext(this); modules(appModule) }` in TipSplitApp. */
fun initKoin(config: KoinAppDeclaration) {
    startKoin {
        config()
        modules(commonModule, platformModule)
    }
}

/** iOS has nothing to configure. Swift sees this as `doInitKoin()` (Objective-C reserves the "init" prefix). */
fun initKoin() = initKoin {}
