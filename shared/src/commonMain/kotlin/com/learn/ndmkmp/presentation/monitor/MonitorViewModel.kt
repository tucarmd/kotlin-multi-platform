package com.learn.ndmkmp.presentation.monitor

import com.learn.ndmkmp.core.background.DeviceMonitor
import com.learn.ndmkmp.core.background.LogReadResult
import com.learn.ndmkmp.core.background.MonitorStatus
import com.learn.ndmkmp.core.device.DeviceStatus
import com.learn.ndmkmp.presentation.InfoRow
import com.learn.ndmkmp.presentation.SharedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Records shown on screen — the file may have thousands of lines, drawing them all stutters. */
private const val LOG_PREVIEW = 30

data class MonitorUiState(
    val status: MonitorStatus = MonitorStatus(),
    val supportsForegroundService: Boolean = false,
    /** Periodic task line: "State: ENQUEUED" / "State: not scheduled". */
    val workStateText: String = MonitorStrings.workState(null),
    /** Rows of the last Refresh (7 fields, `null` written literally), empty = not read yet. */
    val currentRows: List<InfoRow> = emptyList(),
    val logs: LogReadResult = LogReadResult(emptyList(), 0),
    val message: String = "",
) {
    val isRunning: Boolean get() = status.isRunning
    /** Android: WorkManager · iOS: BGAppRefresh — the platform is told apart by its foreground-service support. */
    val workTitle: String get() = if (supportsForegroundService) MonitorStrings.WORK_TITLE else MonitorStrings.WORK_TITLE_IOS
    val workHint: String get() = if (supportsForegroundService) MonitorStrings.WORK_HINT else MonitorStrings.WORK_HINT_IOS
    val loopHint: String get() = if (supportsForegroundService) MonitorStrings.LOOP_HINT else MonitorStrings.LOOP_HINT_IOS
    val runningText: String get() = if (status.isRunning) MonitorStrings.RUNNING else MonitorStrings.STOPPED
    val runningLine: String get() =
        MonitorStrings.runningLine(MonitorStrings.modeLabel(status.mode?.name), status.intervalMs / 1000, status.count)
    val lastRecordLine: String? get() = status.lastRecord?.let { "last: ${it.toJson()}" }
    val logSummary: String get() = MonitorStrings.summary(logs.records.size, logs.corruptLines)
    val showingLastText: String get() = MonitorStrings.showingLast(minOf(LOG_PREVIEW, logs.records.size))
    /** Newest first, at most [LOG_PREVIEW] — one terminal line per record. */
    val logLines: List<String> get() = logs.records.takeLast(LOG_PREVIEW).asReversed().map(::logLine)
}

/**
 * Week 3 — Monitor tab: providers, JSONL FileLogger, background loop. No device logic here either: everything goes
 * through [DeviceMonitor], its suspend functions do their own I/O off the main thread.
 */
class MonitorViewModel(private val monitor: DeviceMonitor) : SharedViewModel() {

    private val _state = MutableStateFlow(
        MonitorUiState(status = monitor.status.value, supportsForegroundService = monitor.supportsForegroundService),
    )
    val state: StateFlow<MonitorUiState> = _state.asStateFlow()

    init {
        monitor.status.onEach { s -> _state.update { it.copy(status = s) } }.launchIn(scope)
        // First open + every loop tick (count changes) → re-read the file so the list updates by itself.
        monitor.status.distinctUntilChangedBy { it.count }.onEach { reloadLogs() }.launchIn(scope)
        monitor.periodicState.onEach { w -> _state.update { it.copy(workStateText = MonitorStrings.workState(w)) } }.launchIn(scope)
    }

    fun refresh() {
        scope.launch {
            val s = monitor.collect()
            _state.update { it.copy(currentRows = statusRows(s)) }
        }
    }

    fun appendNow() {
        scope.launch {
            val saved = monitor.collectAndSave(source = "manual")
            _state.update {
                it.copy(
                    currentRows = saved?.let(::statusRows) ?: it.currentRows,
                    message = if (saved == null) MonitorStrings.APPEND_FAILED else "",
                )
            }
            reloadLogs()
        }
    }

    fun startLoop() = monitor.start()
    fun startForegroundService() = monitor.startForegroundService()
    fun stop() = monitor.stop()
    fun schedulePeriodic() = monitor.schedulePeriodic()
    fun cancelPeriodic() = monitor.cancelPeriodic()

    fun reloadLogs() {
        scope.launch {
            val logs = monitor.getLogs()
            _state.update { it.copy(logs = logs) }
        }
    }

    fun clearLogs() {
        scope.launch {
            val ok = monitor.clearLogs()
            _state.update { it.copy(message = if (ok) MonitorStrings.CLEARED else MonitorStrings.CLEAR_FAILED) }
            reloadLogs()
        }
    }
}

/** The record's 7 fields — `null` shows literally "null" so the learner sees which field wasn't measured. */
internal fun statusRows(s: DeviceStatus): List<InfoRow> = listOf(
    InfoRow("timestamp", s.timestamp),
    InfoRow("battery", (s.battery?.let { "$it %" } ?: "null")),
    InfoRow("wifiEnabled", s.wifiEnabled.toString()),
    InfoRow("wifiConnected", s.wifiConnected.toString()),
    InfoRow("cellularAvailable", s.cellularAvailable.toString()),
    InfoRow("bluetoothEnabled", s.bluetoothEnabled.toString()),
    InfoRow("bluetoothConnected", s.bluetoothConnected.toString()),
)

private fun logLine(r: DeviceStatus) =
    "${r.timestamp.substringAfter('T').take(8)} [${r.source ?: "?"}] " +
        "bat=${r.battery ?: "-"} wifi=${r.wifiConnected} cell=${r.cellularAvailable} " +
        "bt=${r.bluetoothEnabled}/${r.bluetoothConnected}"
