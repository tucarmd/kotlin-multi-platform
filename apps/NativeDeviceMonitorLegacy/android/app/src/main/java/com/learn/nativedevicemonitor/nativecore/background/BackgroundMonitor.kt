package com.learn.nativedevicemonitor.nativecore.background

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.learn.nativedevicemonitor.nativecore.device.DeviceStatus
import com.learn.nativedevicemonitor.nativecore.device.DeviceStatusCollector
import com.learn.nativedevicemonitor.nativecore.logging.FileLogger
import com.learn.nativedevicemonitor.nativecore.logging.LogReadResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** What keeps the loop running — each mechanism lives/dies differently when the app goes to the background (Day 14 table). */
enum class MonitorMode {
    /** Day 13 v1: a coroutine inside the app process. Nothing keeps the process alive in the background. */
    IN_APP_LOOP,

    /** Day 14: the same loop running inside a Foreground Service — has a notification, Android keeps the process. */
    FOREGROUND_SERVICE,
}

/** The plan's `getStatus()`. Exposed as [BackgroundMonitor.status] (`StateFlow`) so the UI redraws on every tick. */
data class MonitorStatus(
    val isRunning: Boolean = false,
    val mode: MonitorMode? = null,
    val intervalMs: Long = BackgroundMonitor.DEFAULT_INTERVAL_MS,
    /** Records the loop wrote since this start (excludes worker / Append now). */
    val count: Int = 0,
    val lastRecord: DeviceStatus? = null,
    val lastError: String? = null,
)

/**
 * Week 3 · Day 13–14 — BackgroundMonitor: `start(interval)` · `stop()` · `getStatus()` · `getLogs()` · `clearLogs()`.
 *
 * ```
 *  start(ctx, 10 s, IN_APP_LOOP | FOREGROUND_SERVICE)
 *    │  job already running → ignore (pressing Start twice does NOT double the loop)
 *    ▼
 *  scope.launch(Dispatchers.Default) {
 *     while (isActive) {
 *        collect   DeviceStatusCollector.collect(ctx)      ← 3 provider
 *        record    status.copy(source = "loop"|"fgs")
 *        save      FileLogger.append  (Dispatchers.IO)
 *        count++ → _status (StateFlow) → UI
 *        delay(interval)                                   ← cancellation point: stop() cuts right here
 *     }
 *  }
 *  stop() → job.cancel() (+ stopService when running as FGS)
 *
 *  ── background mechanisms (Day 14) ────────────────────────────────────────────────────
 *  startForegroundService ─► MonitorService.onStartCommand ─► startForeground(notification) ─► start(…, FGS)
 *  schedulePeriodic       ─► WorkManager unique periodic 15′ ─► MonitorWorker.doWork ─► collectAndSave("worker")
 * ```
 *
 * Why an `object` (process-wide singleton): the Activity, `MonitorService` and `MonitorWorker` must see
 * THE SAME loop + the same status. The Activity is destroyed on rotation / backgrounding, a singleton is not.
 * Holds no `Context` at all — functions that need one take it as a parameter (always `applicationContext`).
 *
 * ⚠️ No tricks to "get around" the OS here (plan: *do not bypass platform restrictions*):
 * - Force stop (Settings › Force stop / `adb shell am force-stop`) kills the process + cancels both FGS and scheduled work
 *   → nothing runs again until the user opens the app. That's Android by design.
 * - OS kills the process in the background: IN_APP_LOOP dies for good; FGS (START_STICKY) is recreated a few seconds later.
 * - WorkManager minimum is 15 minutes, Doze can delay it further — the interval is NOT exact.
 */
object BackgroundMonitor {

    private const val TAG = "Monitor"
    const val DEFAULT_INTERVAL_MS = 10_000L

    /** Unique work name — re-enqueueing the same name with KEEP doesn't create a second one. */
    const val PERIODIC_WORK_NAME = "monitor-periodic"

    /** WorkManager doesn't allow periodic work shorter than 15 minutes (`PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS`). */
    const val PERIODIC_INTERVAL_MIN = 15L

    // SupervisorJob: one failing tick doesn't kill the whole scope. Default (not Main): collect + IO don't block the UI.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _status = MutableStateFlow(MonitorStatus())
    val status: StateFlow<MonitorStatus> = _status.asStateFlow()

    fun getStatus(): MonitorStatus = _status.value

    /**
     * Start the loop. `false` = already running (does nothing).
     * `@Synchronized`: the UI and `MonitorService.onStartCommand` may call it at almost the same time.
     */
    @Synchronized
    fun start(context: Context, intervalMs: Long = DEFAULT_INTERVAL_MS, mode: MonitorMode = MonitorMode.IN_APP_LOOP): Boolean {
        if (job?.isActive == true) {
            Log.d(TAG, "start ignored — already running (${_status.value.mode})")
            return false
        }
        val appContext = context.applicationContext
        val source = if (mode == MonitorMode.FOREGROUND_SERVICE) "fgs" else "loop"
        _status.value = MonitorStatus(isRunning = true, mode = mode, intervalMs = intervalMs)
        Log.d(TAG, "start mode=$mode interval=${intervalMs}ms")
        job = scope.launch {
            while (isActive) {
                val record = collectAndSave(appContext, source)
                _status.update {
                    it.copy(
                        count = if (record != null) it.count + 1 else it.count,
                        lastRecord = record ?: it.lastRecord,
                        lastError = if (record == null) FileLogger.shared(appContext).lastError else null,
                    )
                }
                delay(intervalMs)   // suspend function checks for cancellation → stop() stops IMMEDIATELY, no waiting out the interval
            }
        }
        return true
    }

    /** Stop the loop. When running as FGS also stop the service (the notification disappears). */
    @Synchronized
    fun stop(context: Context) {
        val wasService = _status.value.mode == MonitorMode.FOREGROUND_SERVICE
        stopLoop()
        if (wasService) context.stopService(Intent(context, MonitorService::class.java))
    }

    /** Cancel the coroutine only — `MonitorService.onDestroy` calls this (calling [stop] would stopService again). */
    @Synchronized
    fun stopLoop() {
        if (job?.isActive == true) Log.d(TAG, "stop after ${_status.value.count} records")
        job?.cancel()
        job = null
        _status.update { it.copy(isRunning = false) }
    }

    /**
     * collect → record → save ONCE. Shared by the loop, the worker and the "Append now" button.
     * Returns the written record, `null` if writing the file failed.
     */
    suspend fun collectAndSave(context: Context, source: String): DeviceStatus? = withContext(Dispatchers.IO) {
        val record = DeviceStatusCollector.collect(context).copy(source = source)
        val ok = FileLogger.shared(context).append(record)
        Log.d(TAG, "tick source=$source ok=$ok ${record.toJson()}")
        if (ok) record else null
    }

    /** Read the file — blocking, call inside `withContext(Dispatchers.IO)`. */
    fun getLogs(context: Context): LogReadResult = FileLogger.shared(context).readLogs()

    fun clearLogs(context: Context): Boolean = FileLogger.shared(context).clearLogs()

    // ── Day 14: Foreground Service ──────────────────────────────────────────────────────────

    /**
     * Must be called while the app is in the FOREGROUND (button tap): Android 12+ forbids starting an FGS from the background
     * (`ForegroundServiceStartNotAllowedException`). `startForegroundService` gives the service 5–10 s to call
     * `startForeground`; miss it and the app crashes ANR-like (`ForegroundServiceDidNotStartInTimeException`).
     */
    fun startForegroundService(context: Context, intervalMs: Long = DEFAULT_INTERVAL_MS) {
        if (job?.isActive == true && _status.value.mode == MonitorMode.IN_APP_LOOP) stopLoop()  // switch mechanism
        val intent = Intent(context, MonitorService::class.java)
            .putExtra(MonitorService.EXTRA_INTERVAL_MS, intervalMs)
        ContextCompat.startForegroundService(context, intent)
    }

    // ── Day 14: WorkManager ─────────────────────────────────────────────────────────────────

    /**
     * Schedule [MonitorWorker] every 15 minutes. Survives the app being killed AND reboot (WorkManager keeps its own DB).
     * KEEP: if already scheduled keep it, tapping many times doesn't add more.
     */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<MonitorWorker>(PERIODIC_INTERVAL_MIN, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
        Log.d(TAG, "periodic work scheduled every ${PERIODIC_INTERVAL_MIN}min")
    }

    fun cancelPeriodic(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
        Log.d(TAG, "periodic work cancelled")
    }

    /** Work state for the UI: `ENQUEUED` / `RUNNING` / `CANCELLED` / null (not scheduled). */
    fun periodicState(context: Context): Flow<WorkInfo.State?> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(PERIODIC_WORK_NAME)
            .map { infos -> infos.firstOrNull()?.state }
}
