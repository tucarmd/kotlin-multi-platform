package dev.devicepulse.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dev.devicepulse.model.BatteryInfo
import dev.devicepulse.model.ChargingState
import dev.devicepulse.model.Reading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class AndroidBatterySensor(private val context: Context) : BatterySensor {

    override fun observe(): Flow<Reading<BatteryInfo>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(intent.toReading())
            }
        }
        // ACTION_BATTERY_CHANGED is sticky: registering returns the current value immediately.
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        trySend(sticky?.toReading() ?: Reading.Unsupported)
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    private fun Intent.toReading(): Reading<BatteryInfo> {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return Reading.Unsupported
        val charging = when (getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> ChargingState.Charging
            BatteryManager.BATTERY_STATUS_DISCHARGING,
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargingState.Discharging
            BatteryManager.BATTERY_STATUS_FULL -> ChargingState.Full
            else -> ChargingState.Unknown
        }
        return Reading.Available(BatteryInfo(level * 100 / scale, charging))
    }
}
