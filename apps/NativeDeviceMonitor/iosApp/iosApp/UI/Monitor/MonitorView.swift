import SwiftUI
import Shared

/// Week 3 — Monitor tab in SwiftUI: providers, JSONL log, background loop. Draws `MonitorViewModel.state`.
/// Mirrors Android `ui/monitor/MonitorScreen.kt`. No foreground-service button on iOS (`supportsForegroundService`).
///
/// ```
///  MonitorView
///   └─ ScrollView > VStack
///       ├─ AppCard "Device status (now)"   [Refresh status] [Append to log]
///       ├─ AppCard "Background monitor"     RUNNING/STOPPED · mode · count · last record · [Start loop] [Stop]
///       ├─ AppCard "BGAppRefresh"           State… [Schedule] [Cancel]
///       └─ AppCard "Logs"                   N records · [Reload] [Clear] + latest 30 lines
/// ```
struct MonitorView: View {
    private let vm: MonitorViewModel
    @StateObject private var model: Observed<MonitorUiState>

    init(vm: MonitorViewModel) {
        self.vm = vm
        _model = StateObject(wrappedValue: Observed(vm.state))
    }

    private var strings: MonitorStrings { MonitorStrings.shared }

    var body: some View {
        let state = model.value
        ScrollView {
            VStack(alignment: .leading, spacing: AppSpacing.md) {
                AppHintText(text: strings.HINT, showBullet: false)

                AppCard(title: strings.STATUS_TITLE) {
                    if state.currentRows.isEmpty {
                        AppHintText(text: strings.NOT_READ_YET)
                    } else {
                        InfoTable(rows: state.currentRows)
                    }
                    AppHintText(text: strings.NULL_NOTE)
                    AppPrimaryButton(title: strings.REFRESH) { vm.refresh() }
                    AppSecondaryButton(title: strings.APPEND_NOW) { vm.appendNow() }
                }

                AppCard(title: strings.MONITOR_TITLE) {
                    Text(state.runningText)
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(state.isRunning ? AppColor.success : AppColor.textSecondary)
                    Text(state.runningLine).font(.system(size: 13)).foregroundStyle(AppColor.textPrimary)
                    if let last = state.lastRecordLine {
                        Text(last).font(.system(size: 11, design: .monospaced)).foregroundStyle(AppColor.textPrimary)
                    }
                    if let error = state.status.lastError {
                        Text(error).font(.system(size: 13)).foregroundStyle(AppColor.danger)
                    }
                    AppHintText(text: state.loopHint)
                    AppPrimaryButton(title: strings.START_LOOP, action: { vm.startLoop() }, isEnabled: !state.isRunning)
                    if state.supportsForegroundService {
                        AppPrimaryButton(title: strings.START_FGS, action: { vm.startForegroundService() }, isEnabled: !state.isRunning)
                    }
                    AppSecondaryButton(title: strings.STOP, action: { vm.stop() }, isEnabled: state.isRunning)
                }

                AppCard(title: state.workTitle) {
                    Text(state.workStateText).font(.system(size: 13)).foregroundStyle(AppColor.textPrimary)
                    AppHintText(text: state.workHint)
                    AppPrimaryButton(title: strings.SCHEDULE) { vm.schedulePeriodic() }
                    AppSecondaryButton(title: strings.CANCEL) { vm.cancelPeriodic() }
                }

                AppCard(title: strings.LOGS_TITLE) {
                    Text(state.logSummary).font(.system(size: 13)).foregroundStyle(AppColor.textPrimary)
                    if let error = state.logs.error {
                        Text(error).font(.system(size: 13)).foregroundStyle(AppColor.danger)
                    }
                    if !state.message.isEmpty {
                        Text(state.message).font(.system(size: 13)).foregroundStyle(AppColor.textSecondary)
                    }
                    HStack(spacing: AppSpacing.sm) {
                        AppSecondaryButton(title: strings.RELOAD) { vm.reloadLogs() }
                        AppPrimaryButton(title: strings.CLEAR) { vm.clearLogs() }
                    }
                    if state.logLines.isEmpty {
                        AppHintText(text: strings.EMPTY)
                    } else {
                        AppHintText(text: state.showingLastText, showBullet: false)
                        TerminalBox(lines: state.logLines)
                    }
                }
            }
            .padding(AppSpacing.md)
        }
        .background(AppColor.contentBackground)
    }
}
