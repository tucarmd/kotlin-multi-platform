package dev.obdlink.di

import dev.obdlink.ble.KableElmTransport
import dev.obdlink.core.ObdCore
import dev.obdlink.obd.transport.ElmTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * [App 5 · B4] Koin wiring. Same shape as every practice app (modules + expect platformModule + initKoin), with the
 * modules named after the Gradle modules they come from.
 *
 * Why the transport is bound to the INTERFACE: ObdCore asks for an ElmTransport. Tests build ObdCore with a
 * FakeElmTransport directly; the app gets KableElmTransport from here. Nothing else changes.
 */
val bleModule = module {
    // SupervisorJob: one failed BLE coroutine (e.g. the state watcher) must not cancel the others.
    single<ElmTransport> { KableElmTransport(CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
}

val obdModule = module {
    // One ObdCore for the whole app: the four tabs (and Android rotation) share the same connection.
    // limitedParallelism(1): the core's coroutines never run on two threads at once (see ObdCore KDoc).
    single { ObdCore(transport = get(), scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))) }
}

/**
 * Things that differ per platform. Empty in App 5 on purpose: Kable hides the BLE differences, and asking for the
 * Bluetooth permission is UI work (Android needs an Activity; iOS prompts by itself), so it lives in each app.
 */
expect val platformModule: Module

/** Android: `initKoin { androidContext(this); modules(appModule) }` in ObdLinkApp. */
fun initKoin(config: KoinAppDeclaration) {
    startKoin {
        config()
        modules(bleModule, obdModule, platformModule)
    }
}

/** iOS has nothing to configure. Swift sees this as `doInitKoin()` (Objective-C reserves the "init" prefix). */
fun initKoin() = initKoin {}
