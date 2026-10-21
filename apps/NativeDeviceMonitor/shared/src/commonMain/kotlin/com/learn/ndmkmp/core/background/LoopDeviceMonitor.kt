package com.learn.ndmkmp.core.background

import com.learn.ndmkmp.core.device.DeviceStatus
import com.learn.ndmkmp.core.logging.FileLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Day 12 — the 3 providers read once → one record. Android: BatteryManager / ConnectivityManager / BluetoothManager · iOS: UIDevice / NWPathMonitor / CBCentralManager. */
fun interface DeviceStatusSource {
    /** `source` is left null — the caller stamps who wrote it. */
    suspend fun collect(): DeviceStatus
}

/**
 * Day 14 — what each OS offers beyond the in-app loop. Android: Foreground Service + WorkManager.
 * iOS: no FGS ([supportsForegroundService] = false), `BGAppRefreshTask` for the periodic part.
 */
interface PlatformBackground {
    val supportsForegroundService: Boolean
    val periodicState: Flow<String?>

    /** Start the OS service that will call [LoopDeviceMonitor.runLoop] with FOREGROUND_SERVICE. */
    fun startForegroundService(intervalMs: Long) = Unit
    /** Stop that service (the loop is already cancelled). */
    fun stopForegroundService() = Unit
    fun schedulePeriodic()
    fun cancelPeriodic()
}

/**
 * P7 — the real [DeviceMonitor], common to both platforms: the loop, the log file and the status live here;
 * only "read the device" ([DeviceStatusSource]) and "OS background tricks" ([PlatformBackground]) are per platform.
 *
 * ```
 *  start(10 s)                 ──► runLoop(IN_APP_LOOP)
 *  startForegroundService()    ──► background.startForegroundService ──► (Android service) ──► runLoop(FOREGROUND_SERVICE)
 *  runLoop: while (active) { collect → source=loop|fgs → logger.append → count++ → delay(interval) }
 *  stop()                      ──► job.cancel() (+ stopForegroundService when the FGS owned it)
 * ```
 * One instance per process (the FGS / worker / BG task must see THE SAME loop and file): held by the platform's
 * `AppServices` factory. The OS still decides what lives in the background — no tricks to bypass it.
 */
class LoopDeviceMonitor(
    private val source: DeviceStatusSource,
    val logger: FileLogger,
    private val background: PlatformBackground,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : DeviceMonitor {

    override val status = MutableStateFlow(MonitorStatus())
    override val periodicState: Flow<String?> get() = background.periodicState
    override val supportsForegroundService: Boolean get() = background.supportsForegroundService

    private var job: Job? = null

    override suspend fun collect(): DeviceStatus = source.collect()

    override suspend fun collectAndSave(source: String): DeviceStatus? {
        val record = collect().copy(source = source)
        return if (logger.append(record)) record else null
    }

    override fun start(intervalMs: Long) {
        runLoop(MonitorMode.IN_APP_LOOP, intervalMs)
    }

    override fun startForegroundService() {
        if (job?.isActive == true && status.value.mode == MonitorMode.IN_APP_LOOP) stopLoop()   // switch mechanism
        background.startForegroundService(status.value.intervalMs)
    }

    /** `false` = already running (pressing Start twice does NOT double the loop). Called by the UI and the Android FGS. */
    fun runLoop(mode: MonitorMode, intervalMs: Long = DEFAULT_MONITOR_INTERVAL_MS): Boolean {
        if (job?.isActive == true) return false
        val tag = if (mode == MonitorMode.FOREGROUND_SERVICE) "fgs" else "loop"
        status.value = MonitorStatus(isRunning = true, mode = mode, intervalMs = intervalMs)
        job = scope.launch {
            while (isActive) {
                val record = collectAndSave(tag)
                status.update {
                    it.copy(
                        count = if (record != null) it.count + 1 else it.count,
                        lastRecord = record ?: it.lastRecord,
                        lastError = if (record == null) logger.lastError else null,
                    )
                }
                delay(intervalMs)   // cancellation point: stop() cuts right here, no waiting out the interval
            }
        }
        return true
    }

    override fun stop() {
        val wasService = status.value.mode == MonitorMode.FOREGROUND_SERVICE
        stopLoop()
        if (wasService) background.stopForegroundService()
    }

    /** Cancel the coroutine only — the Android service's onDestroy calls this (calling [stop] would stop it again). */
    fun stopLoop() {
        job?.cancel()
        job = null
        status.update { it.copy(isRunning = false) }
    }

    override fun schedulePeriodic() = background.schedulePeriodic()
    override fun cancelPeriodic() = background.cancelPeriodic()

    override suspend fun getLogs(): LogReadResult = logger.readLogs()
    override suspend fun clearLogs(): Boolean = logger.clearLogs()
}
