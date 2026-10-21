package com.learn.ndmkmp.core.bluetooth

import kotlinx.coroutines.flow.StateFlow

/** Bluetooth radio of the phone. */
enum class BluetoothRadioState {
    /** No Bluetooth hardware (some emulators, the iOS Simulator). */
    UNSUPPORTED,
    OFF,
    ON,
}

/** One answer to "can we scan now?" — the Scanner shows this instead of a dead Scan button. */
enum class BleReadiness {
    READY,
    UNSUPPORTED,
    MISSING_PERMISSION,
    RADIO_OFF,
}

/** Order matters: no hardware beats no permission beats radio off. Pure → same on both platforms. */
fun bleReadiness(radio: BluetoothRadioState, permissionsGranted: Boolean): BleReadiness = when {
    radio == BluetoothRadioState.UNSUPPORTED -> BleReadiness.UNSUPPORTED
    !permissionsGranted -> BleReadiness.MISSING_PERMISSION
    radio == BluetoothRadioState.OFF -> BleReadiness.RADIO_OFF
    else -> BleReadiness.READY
}

/**
 * Everything the Scanner tab needs from BLE — the contract P4 implements twice:
 * androidMain (`BluetoothGatt`, own op queue, CCCD write) and iosMain (CoreBluetooth via Kotlin/Native, 15 s connect timeout).
 *
 * ```
 *  scan ─► devices ─► connect(address) ─► connection = Connected ─► services (discovered)
 *                                         read / setNotify / write ─► lastRead · received · gattError · mtu
 * ```
 * State is exposed as `StateFlow` so Compose collects it the same way on both platforms. P2: [FakeBleClient].
 */
interface BleClient {
    val devices: StateFlow<List<BluetoothDeviceInfo>>
    val isScanning: StateFlow<Boolean>
    val connection: StateFlow<BluetoothConnectionState>
    /** Address (Android MAC) / identifier (iOS UUID) of the device being connected or connected. */
    val connectedAddress: StateFlow<String?>
    /** Last thing that happened, one line — shown under the scan controls. */
    val lastEvent: StateFlow<String>
    /** How many devices the scan filter hid (so "0 devices" is not a mystery). */
    val hiddenCount: StateFlow<Int>
    val services: StateFlow<List<GattServiceInfo>>
    val isDiscovering: StateFlow<Boolean>
    val lastRead: StateFlow<GattValue?>
    /** Notified + written values, newest last, capped at [RECEIVED_LOG_MAX]. */
    val received: StateFlow<List<GattValue>>
    val gattError: StateFlow<String?>
    val mtu: StateFlow<Int>

    fun startScan(filter: BluetoothScanFilter = BluetoothScanFilter.NONE)
    fun stopScan()
    fun connect(address: String)
    fun disconnect()
    fun read(characteristic: GattCharacteristicInfo)
    fun setNotify(characteristic: GattCharacteristicInfo, enabled: Boolean)
    fun write(characteristic: GattCharacteristicInfo, bytes: ByteArray, withResponse: Boolean)
    fun clearReceived()
    /** Radio turned off — scan/GATT report no error by themselves, so the owner tells the client to clean up. */
    fun onRadioOff()
    /** Stop scanning + close GATT (leaking the client slot → Android status 133 on every later connect). */
    fun release()
}
