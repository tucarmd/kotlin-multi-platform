package dev.obdlink.android.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * [App 5 · B5] Runtime permissions needed to scan and connect, by API level.
 *
 * PLATFORM: Android ≤ 30 (S-1) needs ACCESS_FINE_LOCATION to SEE any BLE scan result (no error, just an empty list);
 * API 31+ needs BLUETOOTH_SCAN + BLUETOOTH_CONNECT and no location (manifest: neverForLocation).
 * iOS has neither: CoreBluetooth shows its own prompt on first use.
 *
 * Example: blePermissions(30) → [ACCESS_FINE_LOCATION]; blePermissions(34) → [BLUETOOTH_SCAN, BLUETOOTH_CONNECT].
 */
fun blePermissions(sdk: Int = Build.VERSION.SDK_INT): Array<String> =
    if (sdk >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

/**
 * [App 5 · B5] Permission + radio state for the Connect tab. Asking is UI work (it needs an Activity),
 * so it lives here and not in the shared core.
 */
class BleAccess internal constructor(
    private val permissionGranted: () -> Boolean,
    private val radioOn: () -> Boolean,
    private val askPermission: () -> Unit,
    private val askRadioOn: () -> Unit,
) {
    /** Observable: reading it from a composable recomposes when the user answers. */
    val granted: Boolean get() = permissionGranted()
    val bluetoothOn: Boolean get() = radioOn()

    /** Android shows the dialog only if it still may (it stays silent after "Don't ask again"). */
    fun requestPermission() = askPermission()

    /** System dialog "Allow OBD Link to turn on Bluetooth?". */
    fun requestBluetoothOn() = askRadioOn()
}

@Composable
fun rememberBleAccess(): BleAccess {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasAll(context)) }
    var radioOn by remember { mutableStateOf(isRadioOn(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasAll(context)
        radioOn = isRadioOn(context)
    }
    val enableLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        radioOn = isRadioOn(context)
    }
    // The user can toggle Bluetooth or permissions in Settings and come back: re-read on every resume.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = hasAll(context)
        radioOn = isRadioOn(context)
    }

    return remember(permissionLauncher, enableLauncher) {
        BleAccess(
            permissionGranted = { granted },
            radioOn = { radioOn },
            askPermission = { permissionLauncher.launch(blePermissions()) },
            askRadioOn = {
                // TRAP: on API 31+ launching ACTION_REQUEST_ENABLE without BLUETOOTH_CONNECT throws SecurityException.
                if (hasAll(context)) {
                    enableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                } else {
                    permissionLauncher.launch(blePermissions())
                }
            },
        )
    }
}

private fun hasAll(context: Context): Boolean =
    blePermissions().all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

private fun isRadioOn(context: Context): Boolean =
    context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
