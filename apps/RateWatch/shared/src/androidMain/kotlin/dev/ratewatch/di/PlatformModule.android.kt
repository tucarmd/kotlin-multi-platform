package dev.ratewatch.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.ratewatch.data.createHttpClient
import dev.ratewatch.db.RateDb
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * [App 3 · B3/B4] Android bindings: OkHttp engine + Android SQLite driver.
 *
 * Why only these two: they are the only pieces that need Android APIs (OkHttp is JVM-only, the driver needs a Context).
 * PLATFORM: the app also needs <uses-permission android:name="android.permission.INTERNET" /> in AndroidManifest.xml,
 * or every request fails with SecurityException; iOS needs nothing for HTTPS.
 */
actual val platformModule: Module = module {
    single { httpClient() }
    single<SqlDriver> { AndroidSqliteDriver(RateDb.Schema, androidContext(), "rates.db") }
}

/** The common client setup on top of OkHttp, the engine with the best Android support. */
private fun httpClient(): HttpClient = createHttpClient(OkHttp.create())
