package com.learn.ndmkmp.core.background

import com.learn.ndmkmp.core.device.DeviceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** What keeps the loop running — each mechanism lives/dies differently when the app goes to the background (Day 14 table). */
enum class MonitorMode {
    /** Day 13 v1: a coroutine inside the app process. Nothing keeps the process alive in the background. */
    IN_APP_LOOP,

    /** Day 14 (Android only): the same loop inside a Foreground Service — has a notification, Android keeps the process. */
    FOREGROUND_SERVICE,
}

const val DEFAULT_MONITOR_INTERVAL_MS = 10_000L

/** The plan's `getStatus()`, exposed as a `StateFlow` so the UI redraws on every tick. */
data class MonitorStatus(
    val isRunning: Boolean = false,
    val mode: MonitorMode? = null,
    val intervalMs: Long = DEFAULT_MONITOR_INTERVAL_MS,
    /** Records the loop wrote since this start (excludes periodic task / Append now). */
    val count: Int = 0,
    val lastRecord: DeviceStatus? = null,
    val lastError: String? = null,
)

/** Log read result: valid records + number of broken lines skipped (the UI reports it instead of silently swallowing). */
data class LogReadResult(
    val records: List<DeviceStatus>,
    val corruptLines: Int,
    /** I/O error (disk full, file locked…). `null` = read fine. */
    val error: String? = null,
)

/**
 * Week 3 — the Monitor tab's whole contract: providers (battery / network / Bluetooth) + JSONL logger + background loop.
 *
 * ```
 *  collect()                → DeviceStatus now (3 providers)
 *  start / stop             → in-app loop every intervalMs, status.count++ per record
 *  startForegroundService() → Android FGS · iOS: not available (supportsForegroundService = false)
 *  schedulePeriodic()       → Android WorkManager 15′ · iOS BGAppRefreshTask (system decides when)
 *  getLogs / clearLogs      → JSONL file
 * ```
 * P2: [com.learn.ndmkmp.fake.FakeDeviceMonitor] · P7: real per-platform implementations.
 */
interface DeviceMonitor {
    val status: StateFlow<MonitorStatus>

    /** Periodic task state as text (`ENQUEUED`, `RUNNING`… / iOS `SCHEDULED`); null = not scheduled. */
    val periodicState: Flow<String?>

    val supportsForegroundService: Boolean

    suspend fun collect(): DeviceStatus

    /** Collect + append to the log with [source] (`manual`, `loop`, …). null = write failed. */
    suspend fun collectAndSave(source: String): DeviceStatus?

    fun start(intervalMs: Long = DEFAULT_MONITOR_INTERVAL_MS)
    fun startForegroundService()
    fun stop()
    fun schedulePeriodic()
    fun cancelPeriodic()

    suspend fun getLogs(): LogReadResult
    suspend fun clearLogs(): Boolean
}
