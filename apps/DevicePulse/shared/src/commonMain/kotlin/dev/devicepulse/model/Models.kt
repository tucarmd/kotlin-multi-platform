package dev.devicepulse.model

import kotlin.time.Instant

data class BatteryInfo(val levelPercent: Int, val charging: ChargingState)

enum class ChargingState { Charging, Discharging, Full, Unknown }

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val speedMps: Double?,
    val timestamp: Instant,
)

enum class ConnectionType { Wifi, Cellular, Ethernet, None, Unknown }

data class NetworkStatus(val type: ConnectionType, val isOnline: Boolean)

/**
 * Every sensor reports one of these five states, so no exception ever crosses a sensor boundary.
 *
 * A sealed *class* (not interface) so Swift keeps the type argument: `Reading<BatteryInfo>`, not `any Reading`.
 * With SKIE, Swift switches over it with `onEnum(of:)`.
 */
sealed class Reading<out T : Any> {
    data object Loading : Reading<Nothing>()
    data class Available<out T : Any>(val value: T) : Reading<T>()
    data object PermissionDenied : Reading<Nothing>()
    data object Unsupported : Reading<Nothing>()
    data class Error(val message: String) : Reading<Nothing>()
}

fun <T : Any> Reading<T>.valueOrNull(): T? = (this as? Reading.Available<T>)?.value

enum class SensorKind { Battery, Location, Network }
