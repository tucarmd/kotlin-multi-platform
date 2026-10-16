package dev.devicepulse.di

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.devicepulse.data.HistoryRepository
import dev.devicepulse.data.SqlHistoryRepository
import dev.devicepulse.db.PulseDb
import dev.devicepulse.sensor.AndroidBatterySensor
import dev.devicepulse.sensor.AndroidLocationSensor
import dev.devicepulse.sensor.AndroidNetworkSensor
import dev.devicepulse.sensor.BatterySensor
import dev.devicepulse.sensor.LocationSensor
import dev.devicepulse.sensor.NetworkSensor
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<BatterySensor> { AndroidBatterySensor(androidContext()) }
    single<LocationSensor> { AndroidLocationSensor(androidContext()) }
    single<NetworkSensor> { AndroidNetworkSensor(androidContext()) }
    single { HttpClient(OkHttp) { install(HttpTimeout) { requestTimeoutMillis = 10_000 } } }
    single<HistoryRepository> {
        SqlHistoryRepository(AndroidSqliteDriver(PulseDb.Schema, androidContext(), "pulse.db"))
    }
}
