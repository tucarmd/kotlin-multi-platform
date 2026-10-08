package com.learn.nativedevicemonitor.nativecore.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Week 3 · Day 14 — Foreground Service that runs the [BackgroundMonitor] loop while the app is in the background.
 *
 * ```
 *  BackgroundMonitor.startForegroundService(ctx)
 *    └─► ContextCompat.startForegroundService(Intent)
 *          └─► onStartCommand
 *                ├─ createChannel("monitor")                       (API 26+ requires a channel)
 *                ├─ ServiceCompat.startForeground(1, notification, DATA_SYNC)   ← within 5–10 s, or crash
 *                ├─ BackgroundMonitor.start(…, FOREGROUND_SERVICE)
 *                └─ return START_STICKY      (OS kills the process → the system recreates the service, intent = null)
 *  Stop / notification "Stop" ─► stopService ─► onDestroy ─► BackgroundMonitor.stopLoop()
 *  Android 15+: dataSync running over 6 h/day ─► onTimeout ─► stopSelf()
 * ```
 *
 * A foreground service ≠ "secretly running in the background": the user ALWAYS sees the notification and can stop it in Task Manager
 * (Android 13+). That's the price for Android not killing the process.
 *
 * ⚠️ **Force stop stops the service; it does not restart itself — Android by design, no workaround.**
 * Not used: AlarmManager revival, auto-start on BOOT_COMPLETED, 2 processes watching each other… (plan: don't bypass the OS).
 *
 * Why `dataSync`: Android 14+ requires a type for every FGS. Reading device state + writing a file fits
 * "sync / process data" best; `connectedDevice` is only valid while actually holding a BLE connection.
 */
class MonitorService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null   // started service, binding not allowed

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {     // "Stop" button on the notification
            BackgroundMonitor.stopLoop()
            stopSelf()
            return START_NOT_STICKY
        }
        // intent == null = recreated by the system after a process kill (START_STICKY) → use the default interval.
        val interval = intent?.getLongExtra(EXTRA_INTERVAL_MS, BackgroundMonitor.DEFAULT_INTERVAL_MS)
            ?: BackgroundMonitor.DEFAULT_INTERVAL_MS
        Log.d(TAG, "onStartCommand restarted=${intent == null} interval=$interval")

        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(interval),
            // API < 29 ignores the type parameter; ServiceCompat handles the version if-else.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
        )
        BackgroundMonitor.start(this, interval, MonitorMode.FOREGROUND_SERVICE)
        return START_STICKY
    }

    /** Android 15+ (API 35): `dataSync` FGS is limited to 6 h / 24 h. Not stopping when it runs out → the system throws. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.w(TAG, "onTimeout fgsType=$fgsType — dataSync quota exhausted, stopping")
        BackgroundMonitor.stopLoop()
        stopSelf()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        BackgroundMonitor.stopLoop()
        super.onDestroy()
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Device monitor", NotificationManager.IMPORTANCE_LOW)
            .apply { description = "Shows while the background device monitor is running" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)   // harmless to call again
    }

    private fun buildNotification(intervalMs: Long): Notification {
        // Tap the notification → reopen the app (the package's own launcher intent, so no MainActivity import from ui/).
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, MonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Monitoring device status…")
            .setContentText("Writing a record every ${intervalMs / 1000} s to monitor.jsonl")
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            // Android 12+ delays FGS notifications ~10 s; IMMEDIATE so the learner sees it right away.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val TAG = "MonitorService"
        const val EXTRA_INTERVAL_MS = "intervalMs"
        private const val ACTION_STOP = "com.learn.nativedevicemonitor.monitor.STOP"
        private const val CHANNEL_ID = "monitor"
        private const val NOTIFICATION_ID = 1
    }
}
