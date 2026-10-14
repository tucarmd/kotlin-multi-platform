package dev.devicepulse.sensor

import dev.devicepulse.model.BatteryInfo
import dev.devicepulse.model.LocationFix
import dev.devicepulse.model.NetworkStatus
import dev.devicepulse.model.Reading
import kotlinx.coroutines.flow.Flow

// Implemented per platform in androidMain / iosMain and wired by createPulseCore().

interface BatterySensor {
    fun observe(): Flow<Reading<BatteryInfo>>
}

interface LocationSensor {
    fun observe(): Flow<Reading<LocationFix>>
}

interface NetworkSensor {
    fun observe(): Flow<Reading<NetworkStatus>>
}
