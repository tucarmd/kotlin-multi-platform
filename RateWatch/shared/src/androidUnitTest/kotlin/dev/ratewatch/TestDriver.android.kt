package dev.ratewatch

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ratewatch.db.RateDb

/** JVM: SQLite through JDBC (sqlite-driver). The Android driver needs a Context, which a JVM unit test does not have. */
actual fun inMemoryRateDbDriver(): SqlDriver =
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { RateDb.Schema.create(it) }
