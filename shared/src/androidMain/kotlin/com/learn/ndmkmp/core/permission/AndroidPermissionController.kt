package com.learn.ndmkmp.core.permission

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.learn.ndmkmp.core.bluetooth.AndroidBluetoothRadio
import com.learn.ndmkmp.core.bluetooth.BluetoothRadioState
import com.learn.ndmkmp.ui.permission.PermissionStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * P3 — the real [PermissionController] on Android. In the old app all of this lived in `MainActivity`
 * (state + launchers + radio receiver); here it is one class the Activity hands to `App()`.
 *
 * ```
 *  AndroidPermissionController(activity)          ← constructed in MainActivity.onCreate (before STARTED)
 *   ├─ launchers: requestPermissions (RequestMultiplePermissions) · enableBluetooth (StartActivityForResult)
 *   ├─ request(group) → markAsked → launch → callback → lastResult "BLUETOOTH_SCAN=true" → refresh()
 *   ├─ refresh()      → statusOfGroup per group + radioState      (App() calls it on every resume)
 *   └─ lifecycle      onStart: observeRadio (ACTION_STATE_CHANGED) · onStop: unregister
 * ```
 *
 * Why the launchers are registered in the constructor: `registerForActivityResult` must be called before the
 * Activity is STARTED — calling it later (e.g. inside a Composable click) crashes with `IllegalStateException`.
 * That is why `createAndroidServices(activity)` runs in `onCreate`, before `setContent`.
 *
 * Holds the Activity: lives exactly as long as it (one controller per Activity instance; rotation creates a new one).
 */
class AndroidPermissionController(private val activity: ComponentActivity) : PermissionController {

    override val groups: List<PermissionGroup> = permissionGroups()

    private val _states = MutableStateFlow(readStates())
    override val states: StateFlow<Map<String, PermissionGroupState>> = _states.asStateFlow()

    private val _lastResult = MutableStateFlow("")
    override val lastResult: StateFlow<String> = _lastResult.asStateFlow()

    // Bluetooth radio on/off. Read in refresh() (resume) + the broadcast between onStart/onStop.
    private val _radio = MutableStateFlow(AndroidBluetoothRadio.radioState(activity))
    override val radio: StateFlow<BluetoothRadioState> = _radio.asStateFlow()

    // RequestMultiplePermissions (plural): request a SET of permissions in one chain of dialogs —
    // BLE needs SCAN + CONNECT; two separate calls and the system may swallow the second.
    private val requestPermissions = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        _lastResult.value = results.entries.joinToString(", ") {
            "${it.key.substringAfterLast('.')}=${it.value}"
        }
        Log.d(TAG, "permission result: ${_lastResult.value}")
        // A Boolean isn't enough: re-read shouldShowRequestPermissionRationale to tell DENIED from PERMANENTLY_DENIED
        refresh()
    }

    // System dialog "turn on Bluetooth?" — the result is only logged, the real state is re-read from the radio.
    private val enableBluetooth = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        Log.d(TAG, "enable bluetooth result=${result.resultCode}")
        _radio.value = AndroidBluetoothRadio.radioState(activity)
    }

    private var stopObservingRadio: (() -> Unit)? = null

    init {
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            // Listen to radio on/off only while the screen is visible — onStop unregisters, no leaked receiver.
            // App() turns OFF into ble.onRadioOff() (scan/GATT don't report errors when the radio goes away).
            override fun onStart(owner: LifecycleOwner) {
                stopObservingRadio = AndroidBluetoothRadio.observeRadio(activity) { _radio.value = it }
            }

            override fun onStop(owner: LifecycleOwner) {
                stopObservingRadio?.invoke()   // pairs with onStart — forget to unregister and the receiver holds the Activity (leak)
                stopObservingRadio = null
            }
        })
    }

    /**
     * Request one permission group. Order MATTERS: markAsked FIRST, launch AFTER — reversed, if the app is killed while
     * the dialog is showing the "asked" flag is lost → next time it looks never asked (see [AndroidPermissions] doc).
     */
    override fun request(group: PermissionGroup) {
        if (group.runtimePermissions.isEmpty()) {
            _lastResult.value = PermissionStrings.NONE_NEEDED   // e.g. BLE Connect on API ≤ 30
            return
        }
        AndroidPermissions.markAsked(activity, group.runtimePermissions)
        requestPermissions.launch(group.runtimePermissions.toTypedArray())
    }

    /** Request everything at once — the system chains the dialogs together. */
    override fun requestAll() {
        val all = groups.flatMap { it.runtimePermissions }.distinct()
        if (all.isEmpty()) return
        AndroidPermissions.markAsked(activity, all)
        requestPermissions.launch(all.toTypedArray())
    }

    override fun openSettings() = AndroidPermissions.openAppSettings(activity)

    override fun resetAskedFlags() {
        AndroidPermissions.resetAskedFlags(activity)
        _lastResult.value = ""
        refresh()
    }

    /** API 31+: launching ACTION_REQUEST_ENABLE without BLUETOOTH_CONNECT is a SecurityException (crash) → guard first. */
    override fun turnOnBluetooth() {
        if (!isBleGranted) {
            Toast.makeText(activity, PermissionStrings.READY_NO_PERMISSION, Toast.LENGTH_SHORT).show()
            return
        }
        enableBluetooth.launch(AndroidBluetoothRadio.enableIntent())
    }

    /**
     * Re-read the state from the system. Called on every resume, NOT cached: the user toggles a permission in Settings
     * and comes back — Android fires no event, the only way to notice is to read again. The radio too: the user may
     * turn Bluetooth off while the app is in the background (the receiver is unregistered then).
     */
    override fun refresh() {
        _states.value = readStates()
        _radio.value = AndroidBluetoothRadio.radioState(activity)
    }

    private fun readStates(): Map<String, PermissionGroupState> =
        groups.associate { it.id to AndroidPermissions.statusOfGroup(activity, it) }

    private companion object {
        const val TAG = "PermissionController"
    }
}
