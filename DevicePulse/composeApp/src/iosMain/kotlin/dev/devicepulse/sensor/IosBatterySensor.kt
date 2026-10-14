package dev.devicepulse.sensor

import dev.devicepulse.model.BatteryInfo
import dev.devicepulse.model.ChargingState
import dev.devicepulse.model.Reading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryLevelDidChangeNotification
import platform.UIKit.UIDeviceBatteryState
import platform.UIKit.UIDeviceBatteryStateDidChangeNotification
import kotlin.math.roundToInt

class IosBatterySensor : BatterySensor {

    override fun observe(): Flow<Reading<BatteryInfo>> = callbackFlow {
        val device = UIDevice.currentDevice
        device.batteryMonitoringEnabled = true

        fun read(): Reading<BatteryInfo> {
            // The simulator has no battery and reports -1.
            val level = device.batteryLevel
            if (level < 0f) return Reading.Unsupported
            val charging = when (device.batteryState) {
                UIDeviceBatteryState.UIDeviceBatteryStateCharging -> ChargingState.Charging
                UIDeviceBatteryState.UIDeviceBatteryStateUnplugged -> ChargingState.Discharging
                UIDeviceBatteryState.UIDeviceBatteryStateFull -> ChargingState.Full
                else -> ChargingState.Unknown
            }
            return Reading.Available(BatteryInfo((level * 100).roundToInt(), charging))
        }

        trySend(read())
        val center = NSNotificationCenter.defaultCenter
        val observers = listOf(UIDeviceBatteryLevelDidChangeNotification, UIDeviceBatteryStateDidChangeNotification)
            .map { name -> center.addObserverForName(name, null, NSOperationQueue.mainQueue) { _ -> trySend(read()) } }
        awaitClose {
            observers.forEach { center.removeObserver(it) }
            device.batteryMonitoringEnabled = false
        }
    }.distinctUntilChanged()
}
