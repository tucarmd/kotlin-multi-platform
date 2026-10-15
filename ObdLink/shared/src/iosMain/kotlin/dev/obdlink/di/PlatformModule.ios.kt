package dev.obdlink.di

import org.koin.dsl.module

/**
 * [App 5 · B3/B4] Nothing to bind on iOS: Kable owns the CBCentralManager.
 * PLATFORM: iOS asks for Bluetooth permission itself the first time a scan starts, using the text from
 * NSBluetoothAlwaysUsageDescription in Info.plist. Without that key the app is killed on the first scan.
 */
actual val platformModule = module { }
