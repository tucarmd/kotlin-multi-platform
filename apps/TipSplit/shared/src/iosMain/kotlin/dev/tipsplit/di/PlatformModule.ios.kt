package dev.tipsplit.di

import org.koin.dsl.module

/**
 * [App 1 · B3/B4] Nothing iOS-specific yet. App 2 binds the SQLite driver here.
 *
 * [Bài 4 · actual] The iOS answer to `expect val platformModule` in commonMain/di/Koin.kt. One file covers both
 * iosArm64 and iosSimulatorArm64 (Bài 3 · Hierarchy). Code here may import Apple APIs (platform.Foundation…).
 * [Bài 12 · SQLDelight] App 2 (Pocket Notes) puts `NativeSqliteDriver(…)` here.
 */
actual val platformModule = module { }
