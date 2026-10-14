package dev.devicepulse.core

import dev.devicepulse.data.HistoryRepository
import dev.devicepulse.sensor.BatterySensor
import dev.devicepulse.sensor.LatencyProbe
import dev.devicepulse.sensor.LocationSensor
import dev.devicepulse.sensor.NetworkSensor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

/**
 * Wires the core together once per app. Each platform builds it with `createPulseCore(...)`
 * (androidMain / iosMain), which picks the sensors, the Ktor engine and the SQLite driver.
 */
class PulseCore(
    private val battery: BatterySensor,
    private val location: LocationSensor,
    private val network: NetworkSensor,
    private val probe: LatencyProbe,
    val history: HistoryRepository,
) {
    fun startMonitor(scope: CoroutineScope): PulseMonitor =
        PulseMonitor(battery, location, network, probe, history, scope)

    // Not "newMonitor": Objective-C reserves the "new" prefix, so Swift would see doNewMonitor().
    /** For Swift, which cannot build a CoroutineScope. Call [PulseMonitor.close] when the screen goes away. */
    fun startMonitor(): PulseMonitor = startMonitor(MainScope())
}
