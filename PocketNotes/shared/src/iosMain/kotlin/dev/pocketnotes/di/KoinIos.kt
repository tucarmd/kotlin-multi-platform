package dev.pocketnotes.di

import dev.pocketnotes.core.NotesCore
import kotlinx.coroutines.MainScope
import org.koin.mp.KoinPlatform

/**
 * [App 2 · B4] Door into the core for Swift.
 *
 * TRAP: Swift cannot call Koin's `get<T>()` because it is `reified` (the type only exists at Kotlin compile time),
 * and it cannot build a Kotlin CoroutineScope either. So this function does both: repository from Koin,
 * a fresh MainScope for the core. The caller owns the result and must call `close()` when done.
 *
 * Call after `doInitKoin()`. Swift (via SKIE): `notesCore()`.
 */
fun notesCore(): NotesCore = NotesCore(repository = KoinPlatform.getKoin().get(), scope = MainScope())
