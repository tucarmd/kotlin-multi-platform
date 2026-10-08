import SwiftUI

/// Week 3 · Day 12–15 — Monitor tab. Mirrors `android/.../ui/monitor/MonitorScreen.kt`.
///
/// ```
///  MonitorView
///   └─ ScrollView > VStack
///       ├─ AppCard "Device status (now)"   [Refresh status] [Append to log]           ← Day 12
///       ├─ AppCard "Background monitor"     RUNNING/STOPPED · count · last record
///       │                                    [Start in-app loop] [Stop]                  ← Day 13
///       ├─ AppCard "BGAppRefresh"           last submit · last run · LLDB command          ← Day 14 (Android: FGS + WorkManager)
///       └─ AppCard "Logs"                   N records · M corrupt  [Reload] [Clear] + latest 30 lines
/// ```
///
/// The View holds no device logic (Day 11): everything goes through `BackgroundMonitor.shared` / `DeviceStatusCollector`.
struct MonitorView: View {

    private let monitor = BackgroundMonitor.shared

    @State private var current: DeviceStatus?
    @State private var logs = LogReadResult(records: [], corruptLines: 0)
    @State private var message = ""

    private let preview = 30

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: AppSpacing.md) {
                AppHintText(text: MonitorStrings.hint, showBullet: false)
                statusCard
                monitorCard
                refreshCard
                logsCard
            }
            .padding(AppSpacing.md)
        }
        .background(AppColor.contentBackground)
        .navigationTitle(MonitorStrings.title)
        .navigationBarTitleDisplayMode(.inline)
        // First time + every tick (count changes) → re-read the file so the list updates by itself. Like LaunchedEffect(status.count).
        .task(id: monitor.count) { await reloadLogs() }
    }

    // MARK: - Cards

    private var statusCard: some View {
        AppCard(title: MonitorStrings.statusTitle) {
            if let current { statusTable(current) } else { AppHintText(text: MonitorStrings.notReadYet) }
            AppHintText(text: MonitorStrings.nullNote)
            AppPrimaryButton(title: MonitorStrings.refresh) {
                current = DeviceStatusCollector.collect()
            }
            AppSecondaryButton(title: MonitorStrings.appendNow) {
                Task {
                    let saved = await monitor.collectAndSave(source: "manual")
                    if let saved { current = saved }
                    message = saved == nil ? MonitorStrings.appendFailed : ""
                    await reloadLogs()
                }
            }
        }
    }

    private var monitorCard: some View {
        AppCard(title: MonitorStrings.monitorTitle) {
            Text(monitor.isRunning ? MonitorStrings.running : MonitorStrings.stopped)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(monitor.isRunning ? AppColor.success : AppColor.textSecondary)
            Text(MonitorStrings.runningLine(intervalSec: monitor.interval.components.seconds, count: monitor.count))
                .font(.system(size: 13))
                .foregroundStyle(AppColor.textPrimary)
            if let last = monitor.lastRecord, let json = try? last.toJson() {
                mono("last: \(json)")
            }
            if let err = monitor.lastError {
                Text(err).font(.system(size: 13)).foregroundStyle(AppColor.danger)
            }
            AppHintText(text: MonitorStrings.loopHint)
            AppHintText(text: MonitorStrings.noFgsHint)
            AppPrimaryButton(title: MonitorStrings.startLoop, action: { monitor.start() }, isEnabled: !monitor.isRunning)
            AppSecondaryButton(title: MonitorStrings.stop, action: { monitor.stop() }, isEnabled: monitor.isRunning)
        }
    }

    private var refreshCard: some View {
        AppCard(title: MonitorStrings.refreshTitle) {
            Text(MonitorStrings.scheduleResult(monitor.refreshScheduleResult))
                .font(.system(size: 13)).foregroundStyle(AppColor.textPrimary)
            Text(MonitorStrings.lastRun(monitor.lastRefreshRun))
                .font(.system(size: 13)).foregroundStyle(AppColor.textPrimary)
            AppHintText(text: MonitorStrings.refreshHint)
            AppHintText(text: MonitorStrings.lldbTitle)
            mono(BackgroundMonitor.lldbSimulateCommand)
                .textSelection(.enabled)
            AppSecondaryButton(title: MonitorStrings.scheduleNow) { monitor.scheduleAppRefresh() }
        }
    }

    private var logsCard: some View {
        AppCard(title: MonitorStrings.logsTitle) {
            Text(MonitorStrings.summary(logs.records.count, logs.corruptLines))
                .font(.system(size: 13)).foregroundStyle(AppColor.textPrimary)
            if let err = logs.error {
                Text(err).font(.system(size: 13)).foregroundStyle(AppColor.danger)
            }
            if !message.isEmpty {
                Text(message).font(.system(size: 13)).foregroundStyle(AppColor.textSecondary)
            }
            HStack(spacing: AppSpacing.sm) {
                AppSecondaryButton(title: MonitorStrings.reload) { Task { await reloadLogs() } }
                AppPrimaryButton(title: MonitorStrings.clear) {
                    Task {
                        let ok = await monitor.clearLogs()
                        message = ok ? MonitorStrings.cleared : MonitorStrings.clearFailed
                        await reloadLogs()
                    }
                }
            }
            if logs.records.isEmpty {
                AppHintText(text: MonitorStrings.empty)
            } else {
                AppHintText(text: MonitorStrings.showingLast(min(preview, logs.records.count)), showBullet: false)
                logTerminal(Array(logs.records.suffix(preview).reversed()))
            }
        }
    }

    // MARK: - Pieces

    private func reloadLogs() async { logs = await monitor.getLogs() }

    /// 7 fields — `nil` shows the text "null" so you see which field wasn't measured.
    private func statusTable(_ s: DeviceStatus) -> some View {
        let rows: [(String, String)] = [
            ("timestamp", s.timestamp),
            ("battery", s.battery.map { "\($0) %" } ?? "null"),
            ("wifiEnabled", text(s.wifiEnabled)),
            ("wifiConnected", "\(s.wifiConnected)"),
            ("cellularAvailable", "\(s.cellularAvailable)"),
            ("bluetoothEnabled", text(s.bluetoothEnabled)),
            ("bluetoothConnected", text(s.bluetoothConnected)),
        ]
        return VStack(spacing: 2) {
            ForEach(rows, id: \.0) { k, v in
                HStack {
                    Text(k).font(.system(size: 13)).foregroundStyle(AppColor.textSecondary)
                    Spacer()
                    Text(v).font(.system(size: 13, design: .monospaced)).foregroundStyle(AppColor.textPrimary)
                }
            }
        }
    }

    private func text(_ b: Bool?) -> String { b.map { "\($0)" } ?? "null" }

    private func mono(_ s: String) -> some View {
        Text(s).font(.system(size: 11, design: .monospaced)).foregroundStyle(AppColor.textPrimary)
    }

    /// Terminal-style box like *Received Data* on the Scanner.
    private func logTerminal(_ records: [DeviceStatus]) -> some View {
        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            ForEach(Array(records.enumerated()), id: \.offset) { _, r in
                let time = r.timestamp.split(separator: "T").last.map { String($0.prefix(8)) } ?? r.timestamp
                Text("\(time) [\(r.source ?? "?")] bat=\(r.battery.map(String.init) ?? "-") wifi=\(r.wifiConnected) "
                     + "cell=\(r.cellularAvailable) bt=\(text(r.bluetoothEnabled))/\(text(r.bluetoothConnected))")
                    .font(.system(size: 11, design: .monospaced))
                    .foregroundStyle(AppColor.terminalText)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(AppSpacing.sm)
        .background(AppColor.terminalBackground, in: RoundedRectangle(cornerRadius: 8))
    }
}

#Preview {
    NavigationStack { MonitorView() }
}
