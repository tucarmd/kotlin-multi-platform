package com.learn.ndmkmp.platform

import com.learn.ndmkmp.core.bluetooth.BluetoothRadioState
import com.learn.ndmkmp.core.permission.PermissionController
import com.learn.ndmkmp.core.permission.PermissionGroup
import com.learn.ndmkmp.core.permission.PermissionGroupId
import com.learn.ndmkmp.core.permission.PermissionGroupState
import com.learn.ndmkmp.core.permission.PermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** iOS authorization as Apple reports it (`CBManager.authorization`, `AVCaptureDevice.authorizationStatus`). */
enum class NativeAuthorization { NOT_DETERMINED, GRANTED, DENIED, RESTRICTED }

/**
 * P12a — what the Swift side does for permissions (`iosApp/.../Platform/IosPermissionNative.swift`).
 * Plain calls + callbacks, no Flow / suspend: easy to implement in Swift, the state lives in [IosPermissionController].
 * Every callback must arrive on the main thread.
 */
interface IosPermissionNative {
    fun bluetoothAuthorization(): NativeAuthorization
    fun cameraAuthorization(): NativeAuthorization
    /** Bluetooth has no `requestAccess` — creating a `CBCentralManager` shows the dialog; [done] once answered. */
    fun requestBluetooth(done: () -> Unit)
    fun requestCamera(done: (Boolean) -> Unit)
    fun openSettings()
    /** Start observing the radio (only once Bluetooth is granted — creating the manager earlier shows the dialog). */
    fun startRadio(onChange: (BluetoothRadioState) -> Unit)
    /** iOS can't turn Bluetooth on: a manager with ShowPowerAlert = the system "Turn On Bluetooth" alert. */
    fun showPowerAlert()
}

/**
 * P12a — [PermissionController] for iOS. Same 4 states as Android, mapped from Apple's answer:
 *
 * ```
 *  NOT_DETERMINED ─► NOT_REQUESTED        (dialog will show)
 *  GRANTED        ─► GRANTED
 *  DENIED         ─► PERMANENTLY_DENIED   (iOS asks exactly once — only Settings left)
 *  RESTRICTED     ─► PERMANENTLY_DENIED   (Screen Time / MDM)
 * ```
 * iOS has ONE Bluetooth prompt, so BLE Scan and BLE Connect share it (both cards show the same state). No "asked"
 * flags to keep: iOS remembers NOT_DETERMINED itself (Android needs SharedPreferences for that).
 */
class IosPermissionController(private val native: IosPermissionNative) : PermissionController {

    override val groups: List<PermissionGroup> = listOf(
        PermissionGroup(PermissionGroupId.BLE_SCAN, listOf("NSBluetoothAlwaysUsageDescription")),
        PermissionGroup(PermissionGroupId.BLE_CONNECT, listOf("(same iOS prompt as BLE Scan)")),
        PermissionGroup(PermissionGroupId.CAMERA, listOf("NSCameraUsageDescription")),
    )

    private val _states = MutableStateFlow(read())
    override val states: StateFlow<Map<String, PermissionGroupState>> = _states.asStateFlow()

    private val _lastResult = MutableStateFlow("")
    override val lastResult: StateFlow<String> = _lastResult.asStateFlow()

    private val _radio = MutableStateFlow(BluetoothRadioState.OFF)
    override val radio: StateFlow<BluetoothRadioState> = _radio.asStateFlow()

    private var radioStarted = false

    init {
        startRadioIfGranted()
    }

    override fun request(group: PermissionGroup) {
        when (group.id) {
            PermissionGroupId.CAMERA -> native.requestCamera { granted ->
                refresh()
                _lastResult.value = "camera -> ${if (granted) "GRANTED" else "DENIED"}"
            }
            else -> native.requestBluetooth {
                refresh()
                _lastResult.value = "bluetooth -> ${native.bluetoothAuthorization()}"
            }
        }
    }

    /** iOS queues permission dialogs — fire two at once and the second is swallowed → one after another. */
    override fun requestAll() {
        native.requestBluetooth {
            native.requestCamera {
                refresh()
                _lastResult.value = "bluetooth -> ${native.bluetoothAuthorization()}, camera -> ${native.cameraAuthorization()}"
            }
        }
    }

    override fun openSettings() = native.openSettings()

    /** Nothing to reset on iOS: no app-side flags. The system side: delete the app or Reset Location & Privacy. */
    override fun resetAskedFlags() {
        _lastResult.value = "iOS keeps no asked-flags — delete the app (or Reset Location & Privacy) to start over"
    }

    override fun turnOnBluetooth() = native.showPowerAlert()

    override fun refresh() {
        _states.value = read()
        startRadioIfGranted()
    }

    private fun startRadioIfGranted() {
        if (radioStarted || native.bluetoothAuthorization() != NativeAuthorization.GRANTED) return
        radioStarted = true
        native.startRadio { _radio.value = it }
    }

    private fun read(): Map<String, PermissionGroupState> {
        val bluetooth = native.bluetoothAuthorization().toStatus()
        val camera = native.cameraAuthorization().toStatus()
        return groups.associate { g ->
            val status = if (g.id == PermissionGroupId.CAMERA) camera else bluetooth
            g.id to PermissionGroupState(status, g.runtimePermissions.associateWith { status })
        }
    }

    private fun NativeAuthorization.toStatus() = when (this) {
        NativeAuthorization.NOT_DETERMINED -> PermissionStatus.NOT_REQUESTED
        NativeAuthorization.GRANTED -> PermissionStatus.GRANTED
        NativeAuthorization.DENIED, NativeAuthorization.RESTRICTED -> PermissionStatus.PERMANENTLY_DENIED
    }
}
