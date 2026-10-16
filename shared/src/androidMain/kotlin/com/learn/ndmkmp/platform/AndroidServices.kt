package com.learn.ndmkmp.platform

import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.learn.ndmkmp.AppServices
import com.learn.ndmkmp.core.background.AndroidMonitorRuntime
import com.learn.ndmkmp.core.background.NotificationGatedMonitor
import com.learn.ndmkmp.core.bluetooth.AndroidBleClient
import com.learn.ndmkmp.core.media.AndroidMediaPicker
import com.learn.ndmkmp.core.network.KtorApiService
import com.learn.ndmkmp.core.permission.AndroidPermissionController

/**
 * The Android side of [AppServices] — the only place that picks real vs fake on Android. Since P7 every service is real.
 * Must be called in `MainActivity.onCreate` BEFORE the Activity is STARTED: the permission / camera launchers
 * (`registerForActivityResult`) have to be registered that early.
 */
fun createAndroidServices(activity: ComponentActivity): AppServices {
    // applicationContext — the BLE client outlives a single screen; holding the Activity would leak it.
    val ble = AndroidBleClient(activity.applicationContext)
    activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
        // Stop scanning + close GATT. Without close the client slot leaks → every later connect returns status=133.
        override fun onDestroy(owner: LifecycleOwner) = ble.release()
    })
    return AppServices(
        permissions = AndroidPermissionController(activity),   // P3 ✅
        ble = ble,                                              // P4 ✅
        media = AndroidMediaPicker(activity),                   // P5 ✅
        api = KtorApiService(),                                 // P6 ✅
        // P7 ✅ one loop per process (shared with MonitorService / MonitorWorker) + POST_NOTIFICATIONS before the FGS
        monitor = NotificationGatedMonitor(activity, AndroidMonitorRuntime.monitor(activity)),
    )
}
