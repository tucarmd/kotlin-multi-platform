package dev.devicepulse.core

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.devicepulse.data.SqlHistoryRepository
import dev.devicepulse.db.PulseDb
import dev.devicepulse.sensor.AndroidBatterySensor
import dev.devicepulse.sensor.AndroidLocationSensor
import dev.devicepulse.sensor.AndroidNetworkSensor
import dev.devicepulse.sensor.LatencyProbe
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

/** Builds the core with the Android sensors, OkHttp and the Android SQLite driver. Call once, from Application. */
fun createPulseCore(context: Context): PulseCore {
    val app = context.applicationContext
    return PulseCore(
        battery = AndroidBatterySensor(app),
        location = AndroidLocationSensor(app),
        network = AndroidNetworkSensor(app),
        probe = LatencyProbe(HttpClient(OkHttp) { install(HttpTimeout) { requestTimeoutMillis = 10_000 } }),
        history = SqlHistoryRepository(AndroidSqliteDriver(PulseDb.Schema, app, "pulse.db")),
    )
}
