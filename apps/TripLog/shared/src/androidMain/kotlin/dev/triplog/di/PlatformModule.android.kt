package dev.triplog.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.triplog.db.TripDb
import dev.triplog.platform.AndroidLocationStream
import dev.triplog.platform.LocationStream
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/** [App 4 · B3/B4] Android bindings: SQLite through AndroidSqliteDriver, GPS through LocationManager. */
actual val platformModule: Module = module {
    single<SqlDriver> { AndroidSqliteDriver(TripDb.Schema, androidContext(), "triplog.db") }
    single<LocationStream> { AndroidLocationStream(androidContext()) }
}
