package dev.devicepulse.di

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.devicepulse.data.HistoryRepository
import dev.devicepulse.data.SqlHistoryRepository
import dev.devicepulse.db.PulseDb
import dev.devicepulse.sensor.BatterySensor
import dev.devicepulse.sensor.IosBatterySensor
import dev.devicepulse.sensor.IosLocationSensor
import dev.devicepulse.sensor.IosNetworkSensor
import dev.devicepulse.sensor.LocationSensor
import dev.devicepulse.sensor.NetworkSensor
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<BatterySensor> { IosBatterySensor() }
    single<LocationSensor> { IosLocationSensor() }
    single<NetworkSensor> { IosNetworkSensor() }
    single { HttpClient(Darwin) { install(HttpTimeout) { requestTimeoutMillis = 10_000 } } }
    single<HistoryRepository> { SqlHistoryRepository(NativeSqliteDriver(PulseDb.Schema, "pulse.db")) }
}
