package com.learn.ndmkmp.core.device

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.learn.ndmkmp.core.background.DeviceStatusSource
import com.learn.ndmkmp.isoLocalTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Week 3 · Day 12 — 3 providers, each returns the CURRENT state (read once, no listener kept). Ported from the old
 * app's `DeviceProviders.kt`; the loop / logger around them is common ([com.learn.ndmkmp.core.background.LoopDeviceMonitor]).
 *
 * ```
 *  AndroidDeviceStatusSource.collect()
 *    ├─ BatteryProvider.percent    → BatteryManager.BATTERY_PROPERTY_CAPACITY
 *    ├─ NetworkProvider.snapshot   → ConnectivityManager.activeNetwork + WifiManager.isWifiEnabled
 *    ├─ BluetoothProvider.snapshot → adapter.isEnabled + getConnectedDevices(GATT)
 *    └─ DeviceStatus(timestamp ISO 8601 = common isoLocalTimestamp, …)
 * ```
 *
 * Holds only `applicationContext`: the source is called from 3 places with different lifetimes (screen,
 * Foreground Service, WorkManager worker) — an Activity here would leak.
 */
class AndroidDeviceStatusSource(context: Context) : DeviceStatusSource {

    private val appContext = context.applicationContext

    override suspend fun collect(): DeviceStatus = withContext(Dispatchers.IO) {
        val network = NetworkProvider.snapshot(appContext)
        val bluetooth = BluetoothProvider.snapshot(appContext)
        DeviceStatus(
            // Seconds precision (yyyy-MM-dd'T'HH:mm:ssXXX): milliseconds don't help when the interval is in seconds.
            timestamp = isoLocalTimestamp(System.currentTimeMillis()),
            battery = BatteryProvider.percent(appContext),
            wifiEnabled = network.wifiEnabled,
            wifiConnected = network.wifiConnected,
            cellularAvailable = network.cellularAvailable,
            bluetoothEnabled = bluetooth.enabled,
            bluetoothConnected = bluetooth.connected,
        )
    }
}

object BatteryProvider {

    /**
     * Battery percent 0–100. No permission needed. Emulator: change it with `adb shell dumpsys battery set level 15`.
     * `Int.MIN_VALUE` = the device doesn't support this property → `null`.
     */
    fun percent(context: Context): Int? {
        val bm = ContextCompat.getSystemService(context, BatteryManager::class.java) ?: return null
        val value = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (value == Int.MIN_VALUE || value < 0) null else value
    }
}

/** Network result: [wifiEnabled] = the Wi-Fi switch; [wifiConnected]/[cellularAvailable] = network usable right now. */
data class NetworkSnapshot(
    val wifiEnabled: Boolean?,
    val wifiConnected: Boolean,
    val cellularAvailable: Boolean,
)

object NetworkProvider {

    /**
     * Needs `ACCESS_NETWORK_STATE` (install-time, declared in the Manifest — no dialog).
     *
     * Only looks at **activeNetwork** (the network the system is using): Wi-Fi + SIM both on → Android prefers Wi-Fi →
     * `cellularAvailable = false` even though the SIM has signal. It means "which path the app is using", not "does the SIM have signal".
     * Knowing whether the SIM really has signal needs `TelephonyManager` + the `READ_PHONE_STATE` permission — out of scope.
     *
     * `NET_CAPABILITY_VALIDATED` = Android successfully reached the internet (café Wi-Fi before login = false).
     */
    fun snapshot(context: Context): NetworkSnapshot {
        val cm = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        // WifiManager must come from applicationContext — from an Activity it triggers lint warning "WifiManagerLeak".
        val wifi = ContextCompat.getSystemService(context.applicationContext, WifiManager::class.java)
        return NetworkSnapshot(
            wifiEnabled = wifi?.isWifiEnabled,
            wifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true && validated,
            cellularAvailable = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true && validated,
        )
    }
}

data class BluetoothSnapshot(val enabled: Boolean?, val connected: Boolean?)

object BluetoothProvider {

    /**
     * `enabled`: `adapter.isEnabled` — no runtime permission needed (like `AndroidBluetoothRadio.radioState`).
     * Device without Bluetooth → `null`.
     *
     * `connected`: asks the SYSTEM `getConnectedDevices(GATT)` instead of reading the app's `AndroidBleClient` —
     * the Foreground Service / worker run with no screen open, the UI's BLE client may not exist yet.
     * The result includes other apps' GATT connections. API 31+ needs `BLUETOOTH_CONNECT`; not granted → `null`
     * (calling anyway is a `SecurityException`).
     */
    fun snapshot(context: Context): BluetoothSnapshot {
        val manager = ContextCompat.getSystemService(context, BluetoothManager::class.java)
        val adapter = manager?.adapter ?: return BluetoothSnapshot(enabled = null, connected = null)
        val enabled = adapter.isEnabled
        val canAskConnected = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
        val connected = when {
            !enabled -> false                     // radio off means definitely no connection
            !canAskConnected -> null
            else -> try {
                manager.getConnectedDevices(BluetoothProfile.GATT).isNotEmpty()
            } catch (_: SecurityException) {
                null                              // the user revoked the permission midway
            }
        }
        return BluetoothSnapshot(enabled, connected)
    }
}
