package com.learn.ndmkmp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.learn.ndmkmp.core.background.DeviceMonitor
import com.learn.ndmkmp.core.bluetooth.BleClient
import com.learn.ndmkmp.core.bluetooth.BluetoothRadioState
import com.learn.ndmkmp.core.bluetooth.bleReadiness
import com.learn.ndmkmp.core.media.MediaPicker
import com.learn.ndmkmp.core.network.ApiService
import com.learn.ndmkmp.core.permission.PermissionController
import com.learn.ndmkmp.core.permission.PermissionGroupId
import com.learn.ndmkmp.fake.FakeApiService
import com.learn.ndmkmp.fake.FakeBleClient
import com.learn.ndmkmp.fake.FakeDeviceMonitor
import com.learn.ndmkmp.fake.FakeMediaPicker
import com.learn.ndmkmp.fake.FakePermissionController
import com.learn.ndmkmp.ui.api.ApiScreen
import com.learn.ndmkmp.ui.common.AppStrings
import com.learn.ndmkmp.ui.media.MediaScreen
import com.learn.ndmkmp.ui.monitor.MonitorScreen
import com.learn.ndmkmp.ui.permission.PermissionScreen
import com.learn.ndmkmp.ui.scanner.ScannerScreen
import com.learn.ndmkmp.ui.theme.AppColor
import com.learn.ndmkmp.ui.theme.AppTheme

/**
 * Everything platform-specific the 5 tabs need, as interfaces. The ONLY place that decides fake vs real:
 * P2 = all fakes; P3–P7 swap one line each for the Android / iOS implementation.
 */
class AppServices(
    val permissions: PermissionController,
    val ble: BleClient,
    val media: MediaPicker,
    val api: ApiService,
    val monitor: DeviceMonitor,
) {
    companion object {
        fun fake() = AppServices(
            permissions = FakePermissionController(),
            ble = FakeBleClient(),
            media = FakeMediaPicker(),
            api = FakeApiService(),
            monitor = FakeDeviceMonitor(supportsForegroundService = platformName().startsWith("Android")),
        )
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    PERMISSIONS(AppStrings.TAB_PERMISSIONS, Icons.Filled.Lock),
    SCANNER(AppStrings.TAB_SCANNER, Icons.Filled.Search),
    MEDIA(AppStrings.TAB_MEDIA, Icons.Filled.Face),
    API(AppStrings.TAB_API, Icons.AutoMirrored.Filled.Send),
    MONITOR(AppStrings.TAB_MONITOR, Icons.Filled.DateRange),
}

/**
 * The whole app — drawn by Android (`MainActivity`) and iOS (`MainViewController`) alike.
 *
 * ```
 *  Scaffold
 *   ├─ topBar     CenterAlignedTopAppBar "Native Device Monitor" (CarMD blue)
 *   ├─ content    one of the 5 screens
 *   └─ bottomBar  NavigationBar: Permissions · Scanner · Media · API · Monitor
 * ```
 *
 * The old Android app kept this in `MainActivity` (it also held the permission launchers). Here it is common code:
 * the platform parts sit behind [AppServices].
 *
 * @param startTab tab name to open first (`monitor`, `scanner`…) — for screenshots from the command line:
 *        Android `adb shell am start … --es tab monitor`, iOS `xcrun simctl launch … -tab monitor`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(startTab: String? = null, services: AppServices = remember { AppServices.fake() }) {
    var tab by rememberSaveable {
        mutableStateOf(Tab.entries.firstOrNull { it.name.equals(startTab, ignoreCase = true) } ?: Tab.PERMISSIONS)
    }

    val permissionStates by services.permissions.states.collectAsState()
    val lastResult by services.permissions.lastResult.collectAsState()
    val radio by services.permissions.radio.collectAsState()
    // Same rule as PermissionController.isBleGranted, but read from the collected state → recomposes when it changes.
    val bleGranted = listOf(PermissionGroupId.BLE_SCAN, PermissionGroupId.BLE_CONNECT).all { permissionStates[it]?.isGranted == true }
    val readiness = bleReadiness(radio, bleGranted)

    // Radio switched off mid-session → scan/GATT give no error by themselves, so tell the client to clean up.
    LaunchedEffect(radio) {
        if (radio == BluetoothRadioState.OFF) services.ble.onRadioOff()
    }

    AppTheme {
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
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = null) },
                            // iPhone tab items are narrower than Android's → "Permissions" wrapped to 2 lines; keep 1 line
                            label = { Text(t.label, maxLines = 1, softWrap = false, fontSize = 11.sp) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    Tab.PERMISSIONS -> PermissionScreen(
                        groups = services.permissions.groups,
                        states = permissionStates,
                        lastResult = lastResult,
                        onRequest = services.permissions::request,
                        onOpenSettings = services.permissions::openSettings,
                        onRequestAll = services.permissions::requestAll,
                        onResetAskedFlags = services.permissions::resetAskedFlags,
                        radioState = radio,
                        bleReadiness = readiness,
                        onTurnOnBluetooth = services.permissions::turnOnBluetooth,
                    )
                    Tab.SCANNER -> ScannerScreen(
                        service = services.ble,
                        readiness = readiness,
                        onOpenPermissions = { tab = Tab.PERMISSIONS },
                    )
                    Tab.MEDIA -> MediaScreen(services.media)
                    Tab.API -> ApiScreen(services.api)
                    Tab.MONITOR -> MonitorScreen(services.monitor)
                }
            }
        }
    }
}
