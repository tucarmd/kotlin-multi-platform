package dev.devicepulse.di

import dev.devicepulse.core.PulseCore
import org.koin.mp.KoinPlatform

/**
 * Swift cannot call Koin's `get<T>()` (it is reified), so iOS asks for the core here.
 * Call after `doInitKoin()`. Swift (via SKIE): `pulseCore()`.
 */
fun pulseCore(): PulseCore = KoinPlatform.getKoin().get()
