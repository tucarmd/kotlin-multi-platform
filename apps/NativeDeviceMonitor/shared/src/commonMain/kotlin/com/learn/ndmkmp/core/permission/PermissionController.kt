package com.learn.ndmkmp.core.permission

import com.learn.ndmkmp.core.bluetooth.BluetoothRadioState
import kotlinx.coroutines.flow.StateFlow

/**
 * Day 1 — ONE permission group = one app FEATURE, not one OS constant.
 * Users understand "the app wants to find the dongle", not `BLUETOOTH_SCAN`; the same feature needs a different set of
 * permissions per Android version (API ≤ 30 vs 31+) and a different API on iOS — the UI only sees features.
 *
 * @param runtimePermissions what is requested at runtime on THIS device (Android names; iOS fills in a readable label).
 *        Empty = nothing to request here (e.g. ble_connect on API ≤ 30, or on iOS where one prompt covers BLE).
 * @param installTimeNote related install-time permissions — only shown in the UI, NOT requested.
 */
data class PermissionGroup(
    val id: String,
    val runtimePermissions: List<String>,
    val installTimeNote: String = "",
)

/** id shared by readiness logic and the Permission screen (label lookup). Wrong name = build error. */
object PermissionGroupId {
    const val BLE_SCAN = "ble_scan"
    const val BLE_CONNECT = "ble_connect"
    const val CAMERA = "camera"
}

/**
 * What the Permissions tab needs from the platform. P2: [FakePermissionController].
 * P3: Android (ActivityResult launchers, `shouldShowRequestPermissionRationale`) and iOS (CBManager / AVCaptureDevice).
 */
interface PermissionController {
    val groups: List<PermissionGroup>
    val states: StateFlow<Map<String, PermissionGroupState>>
    /** Last raw result line, e.g. `BLUETOOTH_SCAN=true` — shown so the learner sees what the OS answered. */
    val lastResult: StateFlow<String>
    val radio: StateFlow<BluetoothRadioState>

    fun request(group: PermissionGroup)
    fun requestAll()
    fun openSettings()
    fun resetAskedFlags()
    fun turnOnBluetooth()
    /** Re-read from the OS — called when the app comes back to the foreground (no event fires after Settings). */
    fun refresh()

    /** BLE ready = scan AND connect granted (only one of them and scanning still fails). */
    val isBleGranted: Boolean
        get() = states.value[PermissionGroupId.BLE_SCAN]?.isGranted == true &&
            states.value[PermissionGroupId.BLE_CONNECT]?.isGranted == true
}
