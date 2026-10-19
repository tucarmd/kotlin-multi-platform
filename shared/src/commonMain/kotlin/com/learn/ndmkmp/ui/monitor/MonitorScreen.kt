package com.learn.ndmkmp.ui.monitor

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.ndmkmp.presentation.InfoRow
import com.learn.ndmkmp.presentation.monitor.MonitorStrings
import com.learn.ndmkmp.presentation.monitor.MonitorViewModel
import com.learn.ndmkmp.ui.common.AppCard
import com.learn.ndmkmp.ui.common.AppHintText
import com.learn.ndmkmp.ui.common.AppPrimaryButton
import com.learn.ndmkmp.ui.common.AppSecondaryButton
import com.learn.ndmkmp.ui.theme.AppColor
import com.learn.ndmkmp.ui.theme.AppSpacing

/**
 * Week 3 · Day 12–15 — Monitor tab. Draws [MonitorViewModel.state].
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
 */
@Composable
fun MonitorScreen(vm: MonitorViewModel) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.contentBackground)
            .verticalScroll(rememberScrollState())
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        AppHintText(MonitorStrings.HINT, bullet = false)

        AppCard(title = MonitorStrings.STATUS_TITLE) {
            if (state.currentRows.isEmpty()) AppHintText(MonitorStrings.NOT_READ_YET) else KeyValueTable(state.currentRows)
            AppHintText(MonitorStrings.NULL_NOTE)
            AppPrimaryButton(MonitorStrings.REFRESH, onClick = vm::refresh)
            AppSecondaryButton(MonitorStrings.APPEND_NOW, onClick = vm::appendNow)
        }

        AppCard(title = MonitorStrings.MONITOR_TITLE) {
            Text(
                state.runningText,
                color = if (state.isRunning) AppColor.success else AppColor.textSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            Text(state.runningLine, fontSize = 13.sp, color = AppColor.textPrimary)
            state.lastRecordLine?.let {
                Text(it, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AppColor.textPrimary, lineHeight = 14.sp)
            }
            state.status.lastError?.let { Text(it, color = AppColor.danger, fontSize = 13.sp) }
            AppHintText(MonitorStrings.LOOP_HINT)
            AppHintText(MonitorStrings.FGS_HINT)
            AppPrimaryButton(MonitorStrings.START_LOOP, onClick = vm::startLoop, enabled = !state.isRunning)
            if (state.supportsForegroundService) {
                AppPrimaryButton(MonitorStrings.START_FGS, onClick = vm::startForegroundService, enabled = !state.isRunning)
            }
            AppPrimaryButton(MonitorStrings.STOP, onClick = vm::stop, enabled = state.isRunning, danger = true)
        }

        AppCard(title = MonitorStrings.WORK_TITLE) {
            Text(state.workStateText, fontSize = 13.sp, color = AppColor.textPrimary)
            AppHintText(MonitorStrings.WORK_HINT)
            AppPrimaryButton(MonitorStrings.SCHEDULE, onClick = vm::schedulePeriodic)
            AppSecondaryButton(MonitorStrings.CANCEL, onClick = vm::cancelPeriodic)
        }

        AppCard(title = MonitorStrings.LOGS_TITLE) {
            Text(state.logSummary, fontSize = 13.sp, color = AppColor.textPrimary)
            state.logs.error?.let { Text(it, color = AppColor.danger, fontSize = 13.sp) }
            if (state.message.isNotEmpty()) Text(state.message, color = AppColor.textSecondary, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                AppSecondaryButton(MonitorStrings.RELOAD, onClick = vm::reloadLogs, modifier = Modifier.weight(1f))
                AppPrimaryButton(MonitorStrings.CLEAR, onClick = vm::clearLogs, modifier = Modifier.weight(1f), danger = true)
            }
            if (state.logLines.isEmpty()) {
                AppHintText(MonitorStrings.EMPTY)
            } else {
                AppHintText(state.showingLastText, bullet = false)
                LogTerminal(state.logLines)
            }
        }
    }
}

@Composable
private fun KeyValueTable(rows: List<InfoRow>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEach { (k, v) ->
            Row(Modifier.fillMaxWidth()) {
                Text(k, fontSize = 13.sp, color = AppColor.textSecondary, modifier = Modifier.weight(1f))
                Text(v, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = AppColor.textPrimary)
            }
        }
    }
}

/** Terminal-style box like *Received Data* on the Scanner — read the raw log by eye. */
@Composable
private fun LogTerminal(lines: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColor.terminalBackground, RoundedCornerShape(8.dp))
            .padding(AppSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        lines.forEach {
            Text(it, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AppColor.terminalText, lineHeight = 14.sp)
        }
    }
}
