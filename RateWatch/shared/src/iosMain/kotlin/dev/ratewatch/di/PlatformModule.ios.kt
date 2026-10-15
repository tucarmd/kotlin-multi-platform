package dev.ratewatch.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.ratewatch.data.createHttpClient
import dev.ratewatch.db.RateDb
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * [App 3 · B3/B4] iOS bindings: Darwin engine (NSURLSession) + native SQLite driver.
 *
 * PLATFORM: NativeSqliteDriver links against the system libsqlite3. The static Shared framework does not carry it,
 * so both shared/build.gradle.kts (linkerOpts) and iosApp/project.yml (OTHER_LDFLAGS) add `-lsqlite3`.
 */
actual val platformModule: Module = module {
    single { httpClient() }
    single<SqlDriver> { NativeSqliteDriver(RateDb.Schema, "rates.db") }
}

/** The common client setup on top of Darwin (NSURLSession), so iOS proxies, VPNs and ATS rules apply. */
private fun httpClient(): HttpClient = createHttpClient(Darwin.create())
