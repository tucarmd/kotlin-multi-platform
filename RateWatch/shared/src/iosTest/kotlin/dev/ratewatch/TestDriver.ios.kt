package dev.ratewatch

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import dev.ratewatch.db.RateDb

/** iOS simulator: the same native driver the app uses, without a file. inMemoryDriver() also creates the schema. */
actual fun inMemoryRateDbDriver(): SqlDriver = inMemoryDriver(RateDb.Schema)
