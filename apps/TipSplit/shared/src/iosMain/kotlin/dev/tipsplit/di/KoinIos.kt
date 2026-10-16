package dev.tipsplit.di

import dev.tipsplit.core.TipCore
import org.koin.mp.KoinPlatform

/**
 * [App 1 · B4] Door into the core for Swift.
 *
 * TRAP: Swift cannot call Koin's `get<T>()` because it is `reified` (the type only exists at Kotlin compile time).
 * So each type Swift needs gets a plain function here. Call after `doInitKoin()`. Swift (via SKIE): `tipCore()`.
 *
 * [Bài 3 · Source set] Why iosMain and not commonMain: only Swift needs this door. Putting it here keeps it out of
 * the Android build, and Swift still sees it because iosMain is compiled into the `Shared` framework.
 */
fun tipCore(): TipCore = KoinPlatform.getKoin().get()
