package dev.obdlink.di

import org.koin.dsl.module

/**
 * [App 5 · B3/B4] Nothing to bind on Android: Kable gets the application Context by itself through
 * androidx.startup (see its merged AndroidManifest). Runtime permissions are asked in androidApp/ui/BlePermission.kt.
 */
actual val platformModule = module { }
