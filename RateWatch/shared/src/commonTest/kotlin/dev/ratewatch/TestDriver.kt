package dev.ratewatch

import app.cash.sqldelight.db.SqlDriver

/**
 * [Phụ lục P1 · DB trong RAM] A fresh, empty, in-memory RateDb for one test. Nothing touches the disk.
 *
 * expect/actual works in TEST source sets too: commonTest declares it, androidUnitTest and iosTest implement it,
 * so SqlRateRepositoryTest runs once in commonTest and checks the SQL on BOTH platforms' SQLite.
 */
expect fun inMemoryRateDbDriver(): SqlDriver
