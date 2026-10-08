package com.learn.nativedevicemonitor.ui.monitor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.nativedevicemonitor.nativecore.background.BackgroundMonitor
import com.learn.nativedevicemonitor.nativecore.device.DeviceStatus
import com.learn.nativedevicemonitor.nativecore.device.DeviceStatusCollector
import com.learn.nativedevicemonitor.nativecore.logging.LogReadResult
import com.learn.nativedevicemonitor.ui.common.AppCard
import com.learn.nativedevicemonitor.ui.common.AppHintText
import com.learn.nativedevicemonitor.ui.common.AppPrimaryButton
import com.learn.nativedevicemonitor.ui.common.AppSecondaryButton
import com.learn.nativedevicemonitor.ui.theme.AppColor
import com.learn.nativedevicemonitor.ui.theme.AppSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Records shown on screen — the file may have thousands of lines, drawing them all stutters. */
private const val LOG_PREVIEW = 30

/**
 * Week 3 · Day 12–15 — Monitor tab: drive the providers, FileLogger and BackgroundMonitor from the UI.
 *
 * ```
 *  MonitorScreen
 *   └─ Column(verticalScroll)
 *       ├─ AppCard "Device status (now)"   [Refresh status] [Append to log]   ← Day 12
 *       ├─ AppCard "Background monitor"     RUNNING/STOPPED · mode · count · last record
 *       │                                    [Start in-app loop] [Start foreground service] [Stop]   ← Day 13–14
 *       ├─ AppCard "WorkManager"            State: ENQUEUED…  [Schedule] [Cancel]                   ← Day 14
 *       └─ AppCard "Logs"                   N records · M corrupt  [Reload] [Clear] + latest 30 lines  ← Day 13
 * ```
 *
 * The UI contains NO device logic (Day 11): everything goes through `BackgroundMonitor` / `DeviceStatusCollector`.
 * Reading the file + providers is IO → `withContext(Dispatchers.IO)`, never blocking the main thread.
 *
 * @param onStartForegroundService the Activity requests `POST_NOTIFICATIONS` (API 33+) before starting — permission
 *   launchers must be registered in the Activity, not in a Composable (same as the Permissions tab).
 */
@Composable
fun MonitorScreen(onStartForegroundService: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val status by BackgroundMonitor.status.collectAsState()
    val workState by remember { BackgroundMonitor.periodicState(context) }.collectAsState(initial = null)

    var current by remember { mutableStateOf<DeviceStatus?>(null) }
    var logs by remember { mutableStateOf(LogReadResult(emptyList(), 0)) }
    var message by remember { mutableStateOf("") }

    fun reloadLogs() = scope.launch {
        logs = withContext(Dispatchers.IO) { BackgroundMonitor.getLogs(context) }
    }

    // First time the tab opens + every loop tick (count changes) → re-read the file so the list updates by itself.
    LaunchedEffect(status.count) { reloadLogs() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.contentBackground)
            .verticalScroll(rememberScrollState())
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        AppHintText(MonitorStrings.HINT, bullet = false)

        // ── Day 12: providers ─────────────────────────────────────────────
        AppCard(title = MonitorStrings.STATUS_TITLE) {
            val shown = current
            if (shown == null) AppHintText(MonitorStrings.NOT_READ_YET) else StatusTable(shown)
            AppHintText(MonitorStrings.NULL_NOTE)
            AppPrimaryButton(MonitorStrings.REFRESH, onClick = {
                scope.launch { current = withContext(Dispatchers.IO) { DeviceStatusCollector.collect(context) } }
            })
            AppSecondaryButton(MonitorStrings.APPEND_NOW, onClick = {
                scope.launch {
                    val saved = BackgroundMonitor.collectAndSave(context, source = "manual")
                    if (saved != null) current = saved
                    message = if (saved == null) MonitorStrings.APPEND_FAILED else ""
                    reloadLogs()
                }
            })
        }

        // ── Day 13–14: the loop ───────────────────────────────────────────
        AppCard(title = MonitorStrings.MONITOR_TITLE) {
            StateChip(
                text = if (status.isRunning) MonitorStrings.RUNNING else MonitorStrings.STOPPED,
                on = status.isRunning,
            )
            Text(
                MonitorStrings.runningLine(
                    MonitorStrings.modeLabel(status.mode?.name),
                    status.intervalMs / 1000,
                    status.count,
                ),
                fontSize = 13.sp,
                color = AppColor.textPrimary,
            )
            status.lastRecord?.let { Mono("last: ${it.toJson()}") }
            status.lastError?.let { Text(it, color = AppColor.danger, fontSize = 13.sp) }
            AppHintText(MonitorStrings.LOOP_HINT)
            AppHintText(MonitorStrings.FGS_HINT)
            AppPrimaryButton(
                MonitorStrings.START_LOOP,
                onClick = { BackgroundMonitor.start(context) },
                enabled = !status.isRunning,
            )
            AppPrimaryButton(
                MonitorStrings.START_FGS,
                onClick = onStartForegroundService,
                enabled = !status.isRunning,
            )
            AppPrimaryButton(
                MonitorStrings.STOP,
                onClick = { BackgroundMonitor.stop(context) },
                enabled = status.isRunning,
                danger = true,
            )
        }

        // ── Day 14: WorkManager ───────────────────────────────────────────
        AppCard(title = MonitorStrings.WORK_TITLE) {
            Text(MonitorStrings.workState(workState?.name), fontSize = 13.sp, color = AppColor.textPrimary)
            AppHintText(MonitorStrings.WORK_HINT)
            AppPrimaryButton(MonitorStrings.SCHEDULE, onClick = { BackgroundMonitor.schedulePeriodic(context) })
            AppSecondaryButton(MonitorStrings.CANCEL, onClick = { BackgroundMonitor.cancelPeriodic(context) })
        }

        // ── Day 13: logs ──────────────────────────────────────────────────
        AppCard(title = MonitorStrings.LOGS_TITLE) {
            Text(MonitorStrings.summary(logs.records.size, logs.corruptLines), fontSize = 13.sp, color = AppColor.textPrimary)
            logs.error?.let { Text(it, color = AppColor.danger, fontSize = 13.sp) }
            if (message.isNotEmpty()) Text(message, color = AppColor.textSecondary, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                AppSecondaryButton(MonitorStrings.RELOAD, onClick = { reloadLogs() }, modifier = Modifier.weight(1f))
                AppPrimaryButton(
                    MonitorStrings.CLEAR,
                    onClick = {
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { BackgroundMonitor.clearLogs(context) }
                            message = if (ok) MonitorStrings.CLEARED else MonitorStrings.CLEAR_FAILED
                            reloadLogs()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    danger = true,
                )
            }
            if (logs.records.isEmpty()) {
                AppHintText(MonitorStrings.EMPTY)
            } else {
                AppHintText(MonitorStrings.showingLast(minOf(LOG_PREVIEW, logs.records.size)), bullet = false)
                LogTerminal(logs.records.takeLast(LOG_PREVIEW).asReversed())
            }
        }
    }
}

/** Table of the record's 7 fields — `null` shows literally "null" so the learner sees which field wasn't measured. */
@Composable
private fun StatusTable(s: DeviceStatus) {
    val rows = listOf(
        "timestamp" to s.timestamp,
        "battery" to (s.battery?.let { "$it %" } ?: "null"),
        "wifiEnabled" to s.wifiEnabled.toString(),
        "wifiConnected" to s.wifiConnected.toString(),
        "cellularAvailable" to s.cellularAvailable.toString(),
        "bluetoothEnabled" to s.bluetoothEnabled.toString(),
        "bluetoothConnected" to s.bluetoothConnected.toString(),
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEach { (k, v) ->
            Row(Modifier.fillMaxWidth()) {
                Text(k, fontSize = 13.sp, color = AppColor.textSecondary, modifier = Modifier.weight(1f))
                Text(v, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = AppColor.textPrimary)
            }
        }
    }
}

@Composable
private fun StateChip(text: String, on: Boolean) {
    Text(
        text = text,
        color = if (on) AppColor.success else AppColor.textSecondary,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
    )
}

@Composable
private fun Mono(text: String) {
    Text(text, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AppColor.textPrimary, lineHeight = 14.sp)
}

/** Terminal-style box like *Received Data* on the Scanner — read the raw log by eye. */
@Composable
private fun LogTerminal(records: List<DeviceStatus>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColor.terminalBackground, RoundedCornerShape(8.dp))
            .padding(AppSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        records.forEach { r ->
            Text(
                "${r.timestamp.substringAfter('T').take(8)} [${r.source ?: "?"}] " +
                    "bat=${r.battery ?: "-"} wifi=${r.wifiConnected} cell=${r.cellularAvailable} " +
                    "bt=${r.bluetoothEnabled}/${r.bluetoothConnected}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = AppColor.terminalText,
                lineHeight = 14.sp,
            )
        }
    }
}
