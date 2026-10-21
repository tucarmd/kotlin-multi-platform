package com.learn.ndmkmp.fake

import com.learn.ndmkmp.core.background.DeviceMonitor
import com.learn.ndmkmp.core.background.LogReadResult
import com.learn.ndmkmp.core.background.MonitorMode
import com.learn.ndmkmp.core.background.MonitorStatus
import com.learn.ndmkmp.core.device.DeviceStatus
import com.learn.ndmkmp.isoLocalTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * P2 stand-in for providers + FileLogger + background loop (P7). The "log file" is a list in memory and the battery
 * drains 1 % per record, so the Monitor tab has moving numbers to show. The loop is a real coroutine — Start/Stop and
 * the count behave like the real in-app loop; FGS and the periodic task only flip their labels.
 *
 * @param supportsForegroundService true on Android so the FGS button is visible (iOS has no such thing).
 */
class FakeDeviceMonitor(
    override val supportsForegroundService: Boolean,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) : DeviceMonitor {

    override val status = MutableStateFlow(MonitorStatus())
    override val periodicState = MutableStateFlow<String?>(null)

    private val log = mutableListOf<DeviceStatus>()
    private var battery = 87
    private var loop: Job? = null

    override suspend fun collect() = DeviceStatus(
        timestamp = isoLocalTimestamp(Clock.System.now().toEpochMilliseconds()),
        battery = battery,
        wifiEnabled = true,
        wifiConnected = true,
        cellularAvailable = false,
        bluetoothEnabled = true,
        bluetoothConnected = null,   // "not measured" — the real Android one needs BLUETOOTH_CONNECT
    )

    override suspend fun collectAndSave(source: String): DeviceStatus {
        val record = collect().copy(source = source)
        log += record
        battery = (battery - 1).coerceAtLeast(5)
        return record
    }

    override fun start(intervalMs: Long) = startLoop(MonitorMode.IN_APP_LOOP, "loop", intervalMs)

    override fun startForegroundService() = startLoop(MonitorMode.FOREGROUND_SERVICE, "fgs", status.value.intervalMs)

    private fun startLoop(mode: MonitorMode, source: String, intervalMs: Long) {
        if (loop?.isActive == true) return
        status.value = MonitorStatus(isRunning = true, mode = mode, intervalMs = intervalMs)
        loop = scope.launch {
            while (isActive) {
                val record = collectAndSave(source)
                status.update { it.copy(count = it.count + 1, lastRecord = record) }
                delay(intervalMs)
            }
        }
    }

    override fun stop() {
        loop?.cancel()
        status.update { it.copy(isRunning = false, mode = null) }
    }

    override fun schedulePeriodic() {
        periodicState.value = "ENQUEUED"
    }

    override fun cancelPeriodic() {
        periodicState.value = "CANCELLED"
    }

    override suspend fun getLogs() = LogReadResult(log.toList(), corruptLines = 0)

    override suspend fun clearLogs(): Boolean {
        log.clear()
        return true
    }
}
