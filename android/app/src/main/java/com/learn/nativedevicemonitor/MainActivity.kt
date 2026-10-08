package com.learn.nativedevicemonitor

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.learn.nativedevicemonitor.nativecore.background.BackgroundMonitor
import com.learn.nativedevicemonitor.nativecore.bluetooth.BluetoothClientService
import com.learn.nativedevicemonitor.nativecore.bluetooth.BluetoothRadioState
import com.learn.nativedevicemonitor.nativecore.bluetooth.BluetoothService
import com.learn.nativedevicemonitor.nativecore.permission.PermissionGroup
import com.learn.nativedevicemonitor.nativecore.permission.PermissionGroupId
import com.learn.nativedevicemonitor.nativecore.permission.PermissionGroupState
import com.learn.nativedevicemonitor.nativecore.permission.PermissionService
import com.learn.nativedevicemonitor.nativecore.permission.permissionGroups
import com.learn.nativedevicemonitor.ui.api.ApiScreen
import com.learn.nativedevicemonitor.ui.common.AppStrings
import com.learn.nativedevicemonitor.ui.media.MediaScreen
import com.learn.nativedevicemonitor.ui.monitor.MonitorScreen
import com.learn.nativedevicemonitor.ui.permission.PermissionScreen
import com.learn.nativedevicemonitor.ui.permission.PermissionStrings
import com.learn.nativedevicemonitor.ui.scanner.ScannerScreen
import com.learn.nativedevicemonitor.ui.theme.AppColor
import com.learn.nativedevicemonitor.ui.theme.AppTheme

/**
 * Entry point — 1 Activity, 5 tab: **Permissions** (Day 1) · **Scanner** (Day 2) · **Media** + **API** (Day 10) · **Monitor** (Week 3).
 *
 * ```
 *  MainActivity
 *   ├─ state: permissionStates (map) · radioState · bleClient (lazy)
 *   ├─ launchers: requestPermissions (RequestMultiplePermissions) · enableBluetooth (StartActivityForResult)
 *   ├─ onStart   → BluetoothService.observeRadio   (radio on/off → radioState, off → bleClient.onRadioOff)
 *   ├─ onResume  → refreshPermissionStates + radioState   (the user may have just changed them in Settings)
 *   ├─ onStop    → unregister receiver ·  onDestroy → bleClient.release()
 *   └─ setContent: Scaffold(TopAppBar, NavigationBar)
 *        ├─ tab PERMISSIONS → PermissionScreen(states, radio, readiness, lambdas)
 *        ├─ tab SCANNER     → ScannerScreen(bleClient, readiness)
 *        ├─ tab MEDIA       → MediaScreen()     (Camera / Photo Picker launchers live inside the screen — Day 10)
 *        ├─ tab API         → ApiScreen()       (Day 10)
 *        └─ tab MONITOR     → MonitorScreen(onStartForegroundService)   (asks for POST_NOTIFICATIONS here)
 * ```
 *
 * Why permission state + launchers live in the Activity and not in a Composable: `registerForActivityResult`
 * must be registered before the Activity is STARTED — calling it inside a Composable crashes with `IllegalStateException`.
 *
 * Why tabs use one state variable instead of Navigation Compose: the screens are siblings, no
 * back stack, no arguments — a navigation library would be overkill. A pushed screen (Device detail) is when it's needed.
 */
class MainActivity : ComponentActivity() {

    private val permissionGroupList: List<PermissionGroup> = permissionGroups()

    // mutableStateMapOf: an "observable" map — changing one key redraws only that card.
    private val permissionStates = mutableStateMapOf<String, PermissionGroupState>()
    private var permissionLastResult by mutableStateOf("")

    // Bluetooth radio on/off. Read in onResume + listen to the broadcast in onStart/onStop.
    private var radioState by mutableStateOf(BluetoothRadioState.OFF)
    private var stopObservingRadio: (() -> Unit)? = null

    // System dialog "turn on Bluetooth?" — the result is only logged, the real state is re-read from the radio.
    private val enableBluetooth = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        Log.d(TAG, "enable bluetooth result=${result.resultCode}")
        radioState = BluetoothService.radioState(this)
    }

    // RequestMultiplePermissions (plural): request a SET of permissions in one chain of dialogs —
    // BLE needs SCAN + CONNECT; two separate calls and the system may swallow the second.
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        permissionLastResult = results.entries.joinToString(", ") {
            "${it.key.substringAfterLast('.')}=${it.value}"
        }
        Log.d(TAG, "permission result: $permissionLastResult")
        // A Boolean isn't enough: re-read shouldShowRequestPermissionRationale to tell DENIED from PERMANENTLY_DENIED
        refreshPermissionStates()
    }

    // Week 3 · Day 14: API 33+ must request POST_NOTIFICATIONS for the Foreground Service notification to show.
    // Any result starts the service: if denied the service STILL runs (notification hidden) — never block the user.
    private val requestNotificationThenStartService = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        Log.d(TAG, "POST_NOTIFICATIONS granted=$granted")
        BackgroundMonitor.startForegroundService(this)
    }

    // Lazy: no service needed until the Scanner tab opens. applicationContext — the service outlives the screen.
    private var bleClientCreated = false
    private val bleClient by lazy {
        bleClientCreated = true
        BluetoothClientService(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshPermissionStates()
        setContent { AppTheme { AppRoot() } }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AppRoot() {
        // rememberSaveable: rotating keeps the same tab
        // `--es tab monitor` (adb am start) opens a tab directly — for acceptance screenshots from the command line, like `-openScanner` on iOS.
        var tab by rememberSaveable {
            mutableStateOf(Tab.entries.firstOrNull { it.name.equals(intent.getStringExtra("tab"), true) } ?: Tab.PERMISSIONS)
        }
        val readiness = BluetoothService.readiness(radioState, isBleGranted())

        Scaffold(
            containerColor = AppColor.contentBackground,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(AppStrings.APP_TITLE) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppColor.primary,
                        titleContentColor = Color.White,
                    ),
                )
            },
            bottomBar = {
                NavigationBar(containerColor = AppColor.surface) {
                    NavigationBarItem(
                        selected = tab == Tab.PERMISSIONS,
                        onClick = { tab = Tab.PERMISSIONS },
                        icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        label = { Text(AppStrings.TAB_PERMISSIONS) },
                    )
                    NavigationBarItem(
                        selected = tab == Tab.SCANNER,
                        onClick = { tab = Tab.SCANNER },
                        icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        label = { Text(AppStrings.TAB_SCANNER) },
                    )
                    NavigationBarItem(
                        selected = tab == Tab.MEDIA,
                        onClick = { tab = Tab.MEDIA },
                        icon = { Icon(Icons.Filled.Face, contentDescription = null) },
                        label = { Text(AppStrings.TAB_MEDIA) },
                    )
                    NavigationBarItem(
                        selected = tab == Tab.API,
                        onClick = { tab = Tab.API },
                        icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                        label = { Text(AppStrings.TAB_API) },
                    )
                    NavigationBarItem(
                        selected = tab == Tab.MONITOR,
                        onClick = { tab = Tab.MONITOR },
                        icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                        label = { Text(AppStrings.TAB_MONITOR) },
                    )
                }
            },
        ) { padding ->
            val modifier = Modifier.padding(padding)
            when (tab) {
                Tab.PERMISSIONS -> Box(modifier) {
                    PermissionScreen(
                        groups = permissionGroupList,
                        states = permissionStates,
                        lastResult = permissionLastResult,
                        onRequest = { requestGroup(it) },
                        onOpenSettings = { PermissionService.openAppSettings(this@MainActivity) },
                        onRequestAll = { requestAllPermissions() },
                        onResetAskedFlags = {
                            PermissionService.resetAskedFlags(this@MainActivity)
                            permissionLastResult = ""
                            refreshPermissionStates()
                        },
                        radioState = radioState,
                        bleReadiness = readiness,
                        onTurnOnBluetooth = { turnOnBluetooth() },
                    )
                }

                Tab.SCANNER -> Box(modifier) {
                    ScannerScreen(
                        service = bleClient,
                        readiness = readiness,
                        onOpenPermissions = { tab = Tab.PERMISSIONS },
                    )
                }

                Tab.MEDIA -> Box(modifier) { MediaScreen() }

                Tab.API -> Box(modifier) { ApiScreen() }

                Tab.MONITOR -> Box(modifier) {
                    MonitorScreen(onStartForegroundService = { startMonitorService() })
                }
            }
        }
    }

    /**
     * Request one permission group. Order MATTERS: markAsked FIRST, launch AFTER — reversed, if the app is killed while
     * the dialog is showing the "asked" flag is lost → next time it looks never asked (see PermissionService doc).
     */
    private fun requestGroup(group: PermissionGroup) {
        if (group.runtimePermissions.isEmpty()) {
            permissionLastResult = PermissionStrings.NONE_NEEDED   // e.g. BLE Connect on API ≤ 30
            return
        }
        PermissionService.markAsked(this, group.runtimePermissions)
        requestPermissions.launch(group.runtimePermissions.toTypedArray())
    }

    /** Request everything at once — the system chains the dialogs together. */
    private fun requestAllPermissions() {
        val all = permissionGroupList.flatMap { it.runtimePermissions }.distinct()
        if (all.isEmpty()) return
        PermissionService.markAsked(this, all)
        requestPermissions.launch(all.toTypedArray())
    }

    /**
     * Re-read the state from the system. Called in onResume, NOT cached: the user toggles a permission in Settings
     * and comes back — Android fires no event, the only way to notice is to read again.
     */
    private fun refreshPermissionStates() {
        permissionGroupList.forEach { group ->
            permissionStates[group.id] = PermissionService.statusOfGroup(this, group)
        }
    }

    /** Week 3 · Day 14: request the notification permission (API 33+, not granted) → callback starts the service; otherwise start right away. */
    private fun startMonitorService() {
        if (PermissionService.needsNotificationPermission(this)) {
            requestNotificationThenStartService.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            BackgroundMonitor.startForegroundService(this)
        }
    }

    /** BLE ready = scan group AND connect group are both GRANTED (only one of them and scanning still throws SecurityException). */
    private fun isBleGranted(): Boolean =
        permissionStates[PermissionGroupId.BLE_SCAN]?.isGranted == true &&
            permissionStates[PermissionGroupId.BLE_CONNECT]?.isGranted == true

    /** API 31+: launching ACTION_REQUEST_ENABLE without BLUETOOTH_CONNECT is a SecurityException (crash) → guard first. */
    private fun turnOnBluetooth() {
        if (!isBleGranted()) {
            Toast.makeText(this, PermissionStrings.READY_NO_PERMISSION, Toast.LENGTH_SHORT).show()
            return
        }
        enableBluetooth.launch(BluetoothService.enableIntent())
    }

    override fun onStart() {
        super.onStart()
        // Listen to radio on/off only while the screen is visible — onStop unregisters, no leaked receiver.
        // Radio OFF → tell bleClient to clean up (scan/GATT don't report errors when the radio goes away). Only when the
        // service already exists, so we don't create it just because the user toggled Bluetooth on the Permissions tab.
        stopObservingRadio = BluetoothService.observeRadio(this) { state ->
            radioState = state
            if (state == BluetoothRadioState.OFF && bleClientCreated) bleClient.onRadioOff()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
        radioState = BluetoothService.radioState(this)   // the user may turn Bluetooth off while the app is in the background
    }

    override fun onStop() {
        super.onStop()
        stopObservingRadio?.invoke()      // pairs with onStart — forget to unregister and the receiver holds the Activity (leak)
        stopObservingRadio = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop scanning + close GATT. Without close the client slot leaks → every later connect returns status=133.
        if (bleClientCreated) bleClient.release()
    }

    private enum class Tab { PERMISSIONS, SCANNER, MEDIA, API, MONITOR }

    private companion object {
        const val TAG = "MainActivity"
    }
}
