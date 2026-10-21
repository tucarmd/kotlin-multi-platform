package com.learn.ndmkmp.core.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Week 3 · Day 14 — WorkManager: writes 1 record each time the system lets it run (periodic, ≥ 15 minutes).
 *
 * ```
 *  AndroidPlatformBackground.schedulePeriodic ─► WorkManager (own DB, survives kill + reboot)
 *      … JobScheduler picks the time (≥ 15′, Doze delays further) …
 *  doWork() ─► AndroidMonitorRuntime.monitor(ctx).collectAndSave("worker") ─► success | retry
 * ```
 *
 * Versus a Foreground Service:
 * | | FGS | WorkManager |
 * |---|---|---|
 * | Interval | anything (10 s) | ≥ 15 minutes, not exact |
 * | Notification | required | no |
 * | App killed by OS | START_STICKY recreates it | still runs on schedule (new process) |
 * | Force stop | stops | **schedule cancelled** until the user opens the app again |
 * | Reboot | does not auto-run | runs again (WorkManager registers BOOT_COMPLETED itself) |
 *
 * `CoroutineWorker` (not `Worker`): `doWork` is `suspend`, runs on `Dispatchers.Default`, cancellable.
 * ~10 minutes per run — plenty for 1 record. WorkManager builds this class by reflection (default factory) →
 * the constructor must stay `(Context, WorkerParameters)`.
 */
class MonitorWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val record = AndroidMonitorRuntime.monitor(applicationContext).collectAndSave(source = "worker")
        // retry = WorkManager retries with backoff (default exponential 30 s) — only for transient errors (IO).
        return if (record != null) Result.success() else Result.retry()
    }
}
