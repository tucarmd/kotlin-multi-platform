package com.learn.ndmkmp.core.background

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * P7 · Day 14 — what Android offers beyond the in-app loop: a Foreground Service + WorkManager. Ported from the
 * "Day 14" half of the old app's `BackgroundMonitor` (the loop half is now common [LoopDeviceMonitor]).
 *
 * ```
 *  startForegroundService ─► MonitorService.onStartCommand ─► startForeground(notification) ─► runLoop(FOREGROUND_SERVICE)
 *  schedulePeriodic       ─► WorkManager unique periodic 15′ ─► MonitorWorker.doWork ─► collectAndSave("worker")
 * ```
 *
 * ⚠️ No tricks to "get around" the OS here (plan: *do not bypass platform restrictions*):
 * - Force stop (Settings › Force stop / `adb shell am force-stop`) kills the process + cancels both FGS and scheduled work
 *   → nothing runs again until the user opens the app. That's Android by design.
 * - OS kills the process in the background: IN_APP_LOOP dies for good; FGS (START_STICKY) is recreated a few seconds later.
 * - WorkManager minimum is 15 minutes, Doze can delay it further — the interval is NOT exact.
 */
class AndroidPlatformBackground(context: Context) : PlatformBackground {

    private val appContext = context.applicationContext

    override val supportsForegroundService: Boolean = true

    /** Work state for the UI: `ENQUEUED` / `RUNNING` / `CANCELLED` / null (not scheduled). */
    override val periodicState: Flow<String?> =
        WorkManager.getInstance(appContext)
            .getWorkInfosForUniqueWorkFlow(PERIODIC_WORK_NAME)
            .map { infos -> infos.firstOrNull()?.state?.name }

    /**
     * Must be called while the app is in the FOREGROUND (button tap): Android 12+ forbids starting an FGS from the background
     * (`ForegroundServiceStartNotAllowedException`). `startForegroundService` gives the service 5–10 s to call
     * `startForeground`; miss it and the app crashes ANR-like (`ForegroundServiceDidNotStartInTimeException`).
     */
    override fun startForegroundService(intervalMs: Long) {
        val intent = Intent(appContext, MonitorService::class.java)
            .putExtra(MonitorService.EXTRA_INTERVAL_MS, intervalMs)
        ContextCompat.startForegroundService(appContext, intent)
    }

    /** The loop is already cancelled by [LoopDeviceMonitor.stop]; this removes the service + its notification. */
    override fun stopForegroundService() {
        appContext.stopService(Intent(appContext, MonitorService::class.java))
    }

    /**
     * Schedule [MonitorWorker] every 15 minutes. Survives the app being killed AND reboot (WorkManager keeps its own DB).
     * KEEP: if already scheduled keep it, tapping many times doesn't add more.
     */
    override fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<MonitorWorker>(PERIODIC_INTERVAL_MIN, TimeUnit.MINUTES).build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
        Log.d(TAG, "periodic work scheduled every ${PERIODIC_INTERVAL_MIN}min")
    }

    override fun cancelPeriodic() {
        WorkManager.getInstance(appContext).cancelUniqueWork(PERIODIC_WORK_NAME)
        Log.d(TAG, "periodic work cancelled")
    }

    companion object {
        private const val TAG = "Monitor"

        /** Unique work name — re-enqueueing the same name with KEEP doesn't create a second one. */
        const val PERIODIC_WORK_NAME = "monitor-periodic"

        /** WorkManager doesn't allow periodic work shorter than 15 minutes (`PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS`). */
        const val PERIODIC_INTERVAL_MIN = 15L
    }
}
