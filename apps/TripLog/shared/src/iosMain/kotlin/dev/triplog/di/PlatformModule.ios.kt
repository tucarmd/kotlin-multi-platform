package dev.triplog.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.triplog.db.TripDb
import dev.triplog.platform.IosLocationStream
import dev.triplog.platform.LocationStream
import org.koin.core.module.Module
import org.koin.dsl.module

/** [App 4 · B3/B4] iOS bindings: system SQLite through NativeSqliteDriver, GPS through CLLocationManager. */
actual val platformModule: Module = module {
    single<SqlDriver> { NativeSqliteDriver(TripDb.Schema, "triplog.db") }
    single<LocationStream> { IosLocationStream() }
}
