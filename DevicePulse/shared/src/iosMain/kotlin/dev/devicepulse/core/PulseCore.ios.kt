package dev.devicepulse.core

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.devicepulse.data.SqlHistoryRepository
import dev.devicepulse.db.PulseDb
import dev.devicepulse.sensor.IosBatterySensor
import dev.devicepulse.sensor.IosLocationSensor
import dev.devicepulse.sensor.IosNetworkSensor
import dev.devicepulse.sensor.LatencyProbe
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout

/** Builds the core with the iOS sensors, the Darwin engine and the native SQLite driver. Swift (via SKIE): `createPulseCore()`. */
fun createPulseCore(): PulseCore = PulseCore(
    battery = IosBatterySensor(),
    location = IosLocationSensor(),
    network = IosNetworkSensor(),
    probe = LatencyProbe(HttpClient(Darwin) { install(HttpTimeout) { requestTimeoutMillis = 10_000 } }),
    history = SqlHistoryRepository(NativeSqliteDriver(PulseDb.Schema, "pulse.db")),
)
