package dev.triplog.di

import dev.triplog.core.TripLibrary
import dev.triplog.core.TripRecorder
import org.koin.mp.KoinPlatform

/*
 * [App 4 · B4] Doors into the core for Swift.
 *
 * TRAP: Swift cannot call Koin's `get<T>()` because it is `reified` (the type only exists at Kotlin compile time).
 * So each type Swift needs gets a plain function here. Call after `doInitKoin()`.
 */

/** The one TripRecorder of the app (Koin single). Swift (via SKIE): `tripRecorder()`. */
fun tripRecorder(): TripRecorder = KoinPlatform.getKoin().get()

/** The saved trips. Swift (via SKIE): `tripLibrary()`. */
fun tripLibrary(): TripLibrary = KoinPlatform.getKoin().get()
