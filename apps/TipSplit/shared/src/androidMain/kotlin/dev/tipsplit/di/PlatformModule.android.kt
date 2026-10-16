package dev.tipsplit.di

import org.koin.dsl.module

/**
 * [App 1 · B3/B4] Nothing Android-specific yet. App 2 binds the SQLite driver here.
 *
 * [Bài 4 · actual] The Android answer to `expect val platformModule` in commonMain/di/Koin.kt.
 * Code in androidMain may use the Android SDK (Context, BatteryManager…); Tip Split just does not need any yet.
 * [Bài 12 · SQLDelight] App 2 (Pocket Notes) puts `AndroidSqliteDriver(…, androidContext())` here.
 */
actual val platformModule = module { }
