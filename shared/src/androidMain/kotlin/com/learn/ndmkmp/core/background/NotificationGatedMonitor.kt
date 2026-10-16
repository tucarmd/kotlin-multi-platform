package com.learn.ndmkmp.core.background

import android.Manifest
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.learn.ndmkmp.core.permission.AndroidPermissions

/**
 * Week 3 · Day 14 — the Monitor tab's [DeviceMonitor] on Android: the process-wide [LoopDeviceMonitor] plus ONE extra
 * step before the Foreground Service starts — ask for `POST_NOTIFICATIONS` (runtime permission on API 33+).
 *
 * ```
 *  startForegroundService()
 *    ├─ API < 33 or already granted ──────────────────────────► monitor.startForegroundService()
 *    └─ not granted ─► RequestPermission(POST_NOTIFICATIONS) ─► (any answer) ─► monitor.startForegroundService()
 * ```
 *
 * Any result starts the service: if denied the service STILL runs (notification hidden from the shade, still listed
 * in Task Manager) — never block the user. Same behaviour as the old `MainActivity.startMonitorService`.
 *
 * Why a wrapper and not inside [LoopDeviceMonitor] / [AndroidPlatformBackground]: those are process-wide and must not
 * hold an Activity; a permission launcher is bound to ONE Activity and must be registered before it is STARTED.
 * `by monitor` = Kotlin interface delegation: every other member forwards to the shared monitor unchanged.
 */
class NotificationGatedMonitor(
    private val activity: ComponentActivity,
    private val monitor: LoopDeviceMonitor,
) : DeviceMonitor by monitor {

    private val requestNotificationThenStart = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        Log.d(TAG, "POST_NOTIFICATIONS granted=$granted")
        monitor.startForegroundService()
    }

    override fun startForegroundService() {
        if (AndroidPermissions.needsNotificationPermission(activity)) {
            requestNotificationThenStart.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            monitor.startForegroundService()
        }
    }

    private companion object {
        const val TAG = "Monitor"
    }
}
