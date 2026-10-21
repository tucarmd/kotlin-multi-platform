package com.learn.ndmkmp.platform

import com.learn.ndmkmp.core.background.DeviceStatusSource
import com.learn.ndmkmp.core.background.LoopDeviceMonitor
import com.learn.ndmkmp.core.background.PlatformBackground
import com.learn.ndmkmp.core.bluetooth.BleClient
import com.learn.ndmkmp.core.bluetooth.BluetoothConnectionState
import com.learn.ndmkmp.core.bluetooth.BluetoothRadioState
import com.learn.ndmkmp.core.device.DeviceStatus
import com.learn.ndmkmp.core.logging.FileLogger
import com.learn.ndmkmp.core.permission.PermissionController
import com.learn.ndmkmp.core.permission.PermissionGroupId
import com.learn.ndmkmp.isoLocalTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import kotlin.time.Clock

/**
 * P12c — the Swift half of the Monitor tab (`iosApp/.../Platform/DeviceNative.swift`).
 * Battery / network need UIKit + NWPathMonitor; BGAppRefresh needs BGTaskScheduler. Called on the main thread.
 */
interface IosDeviceNative {
    /** 0…100, -1 = unknown (Simulator: battery monitoring returns -1). */
    fun batteryPercent(): Int
    fun wifiConnected(): Boolean
    fun cellularAvailable(): Boolean
    /** Submit a `BGAppRefreshTaskRequest` (earliest in 15 min). Returns a line for the UI: "SCHEDULED …" or the error. */
    fun scheduleRefresh(): String
    fun cancelRefresh()
}

/**
 * P12c — one record from the iPhone. Bluetooth is not read again here: the radio comes from the permission controller
 * (its `CBCentralManager` already observes it), "connected" from the BLE client — iOS has no API to list what the
 * SYSTEM is connected to.
 */
class IosDeviceStatusSource(
    private val native: IosDeviceNative,
    private val permissions: PermissionController,
    private val ble: BleClient,
) : DeviceStatusSource {

    // UIDevice is UIKit → main thread.
    override suspend fun collect(): DeviceStatus = withContext(Dispatchers.Main) {
        val granted = permissions.states.value[PermissionGroupId.BLE_SCAN]?.isGranted == true
        val enabled: Boolean? = when {
            !granted -> null                                         // no permission → can't know
            permissions.radio.value == BluetoothRadioState.UNSUPPORTED -> null
            else -> permissions.radio.value == BluetoothRadioState.ON
        }
        DeviceStatus(
            timestamp = isoLocalTimestamp(Clock.System.now().toEpochMilliseconds()),
            battery = native.batteryPercent().takeIf { it >= 0 },
            wifiEnabled = null,                                      // iOS has no public API for the Wi-Fi switch
            wifiConnected = native.wifiConnected(),
            cellularAvailable = native.cellularAvailable(),
            bluetoothEnabled = enabled,
            bluetoothConnected = if (enabled == false) false else ble.connection.value is BluetoothConnectionState.Connected,
        )
    }
}

/** P12c — iOS has no foreground service; the periodic part is BGAppRefresh (iOS decides when, ≥ 15 min, maybe never). */
class IosPlatformBackground(private val native: IosDeviceNative) : PlatformBackground {
    override val supportsForegroundService = false

    private val state = MutableStateFlow<String?>(null)
    override val periodicState: Flow<String?> = state

    override fun schedulePeriodic() {
        state.value = native.scheduleRefresh()
    }

    override fun cancelPeriodic() {
        native.cancelRefresh()
        state.value = null
    }
}

/** The ONE monitor of the process: the screen and the BG task (Swift `IosPlatform`) share the loop and the file. */
fun iosDeviceMonitor(native: IosDeviceNative, permissions: PermissionController, ble: BleClient) = LoopDeviceMonitor(
    source = IosDeviceStatusSource(native, permissions, ble),
    // Documents = private to the app, kept until uninstall; visible in Files if UIFileSharingEnabled is set.
    logger = FileLogger(Path(documentsDir() + "/" + FileLogger.FILE_NAME)),
    background = IosPlatformBackground(native),
)

private fun documentsDir(): String =
    NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
