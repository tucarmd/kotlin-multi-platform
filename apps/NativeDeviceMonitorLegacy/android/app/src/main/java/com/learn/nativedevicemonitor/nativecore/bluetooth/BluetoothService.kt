package com.learn.nativedevicemonitor.nativecore.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Day 1 (BLE Step 1) — 3 states of the phone's Bluetooth RADIO, separate from permissions.
 *
 * Permissions (Day 1) only answer "is the app ALLOWED to use Bluetooth". Whether the radio is ON
 * is a different question — all permissions granted but the user turned Bluetooth off → scan still finds 0 devices.
 */
enum class BluetoothRadioState {
    /** Device has no Bluetooth (some emulators / old tablets) — `BluetoothManager.adapter == null`. */
    UNSUPPORTED,
    OFF,
    ON,
}

/**
 * The final answer for Day 2: can Scan be tapped yet, and if not, EXACTLY what's blocking it.
 *
 * The check order is deliberate — report first what the user can fix first:
 * no radio → nothing to do · missing permission → Request · radio off → Turn on.
 */
enum class BleReadiness {
    READY,
    UNSUPPORTED,
    MISSING_PERMISSION,
    RADIO_OFF,
}

/**
 * Day 1 (BLE Step 1) — read / observe the Bluetooth radio state. Knows nothing about Compose (Rule 5.4).
 *
 * ```
 *  radioState(ctx) ──► BluetoothManager.adapter ──► null → UNSUPPORTED
 *                                              └──► isEnabled → ON | OFF
 *
 *  observeRadio(ctx) ──► BroadcastReceiver(ACTION_STATE_CHANGED) ──► onChange(state)
 *                        (user toggles Bluetooth in Quick Settings → the app knows immediately, no waiting for onResume)
 * ```
 *
 * Day 2 (scan) calls [readiness] before `startScan()` — no screen re-derives it.
 */
object BluetoothService {

    private const val TAG = "BluetoothService"

    /**
     * `adapter.isEnabled` needs NO runtime permission (even on API 31+), so it can be read even before the user
     * grants BLUETOOTH_CONNECT — the screen always shows the correct ON/OFF.
     */
    fun radioState(context: Context): BluetoothRadioState {
        // getSystemService(BluetoothManager) instead of BluetoothAdapter.getDefaultAdapter() (deprecated since API 31)
        val adapter = ContextCompat.getSystemService(context, BluetoothManager::class.java)?.adapter
            ?: return BluetoothRadioState.UNSUPPORTED
        return if (adapter.isEnabled) BluetoothRadioState.ON else BluetoothRadioState.OFF
    }

    /** Combine permissions + radio into one answer. Pure function (no Context) → readable and testable. */
    fun readiness(radio: BluetoothRadioState, permissionsGranted: Boolean): BleReadiness = when {
        radio == BluetoothRadioState.UNSUPPORTED -> BleReadiness.UNSUPPORTED
        !permissionsGranted -> BleReadiness.MISSING_PERMISSION
        radio == BluetoothRadioState.OFF -> BleReadiness.RADIO_OFF
        else -> BleReadiness.READY
    }

    /**
     * Intent that opens the system dialog "App wants to turn on Bluetooth" — the app may NOT turn the radio on itself
     * (`adapter.enable()` is locked since API 33), it can only ask the user.
     *
     * ⚠️ API 31+: launching this Intent without BLUETOOTH_CONNECT throws SecurityException →
     * the UI layer must disable the button until granted (see `MainActivity.turnOnBluetooth`).
     */
    fun enableIntent(): Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    /**
     * Observe radio on/off. Returns an unregister function — call in onStart, unregister in onStop.
     *
     * Why a receiver and not just re-reading in onResume like permissions: pulling down Quick Settings to toggle
     * Bluetooth does NOT take the Activity through onPause/onResume on most devices → the screen would be wrong.
     * Compared to TS: like `addEventListener` returning a `removeEventListener` function.
     */
    fun observeRadio(context: Context, onChange: (BluetoothRadioState) -> Unit): () -> Unit {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val raw = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                // Ignore TURNING_ON / TURNING_OFF: transitional states, only report once settled
                val state = when (raw) {
                    BluetoothAdapter.STATE_ON -> BluetoothRadioState.ON
                    BluetoothAdapter.STATE_OFF -> BluetoothRadioState.OFF
                    else -> return
                }
                Log.d(TAG, "radio -> $state")
                onChange(state)
            }
        }
        // SYSTEM broadcasts still reach a NOT_EXPORTED receiver; this flag blocks other apps from spoofing.
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        return { context.unregisterReceiver(receiver) }
    }
}
