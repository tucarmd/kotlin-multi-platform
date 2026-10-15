package dev.ratewatch.di

import dev.ratewatch.core.RatesCore
import org.koin.mp.KoinPlatform

/**
 * [App 3 · B4] Door into the core for Swift.
 *
 * TRAP: Swift cannot call Koin's `get<T>()` because it is `reified` (the type only exists at Kotlin compile time).
 * So each type Swift needs gets a plain function here. Call after `doInitKoin()`. Swift (via SKIE): `ratesCore()`.
 */
fun ratesCore(): RatesCore = KoinPlatform.getKoin().get()
