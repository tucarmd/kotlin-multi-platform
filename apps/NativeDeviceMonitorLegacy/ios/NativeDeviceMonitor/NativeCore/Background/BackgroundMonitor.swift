import BackgroundTasks
import Foundation

/// Week 3 · Day 13–14 — `start(interval)` · `stop()` · `getStatus()` · `getLogs()` · `clearLogs()`
/// + schedules `BGAppRefreshTask`. Mirrors `android/.../nativecore/background/BackgroundMonitor.kt`.
///
/// ```
///  start(interval: 10 s)
///    │  task already running → ignore (Start twice does NOT double the loop)
///    ▼
///  Task { while !Task.isCancelled {
///           collect   DeviceStatusCollector.collect(source: "loop")
///           save      await FileLogger.shared.append
///           count += 1 → @Observable → the View redraws
///           try await Task.sleep(for: interval)          ← stop() cancels → sleep throws CancellationError → exits immediately
///  } }
///
///  ── Day 14: app goes to the background ─────────────────────────────────────────────────
///  scenePhase == .background ─► scheduleAppRefresh() ─► BGTaskScheduler.submit(earliestBeginDate: +15′)
///      … iOS picks the moment (usage habits, battery, network) — could be hours, could be never …
///  .backgroundTask(.appRefresh(id)) ─► handleAppRefresh() ─► 1 "bgRefresh" record ─► schedule the next one
/// ```
///
/// ⚠️ **iOS does NOT guarantee a fixed interval.** A few seconds in the background and the app is *suspended*: the `Task` loop above
/// freezes (not dead, not running), back in the foreground it carries on — gaps in the log are the correct behaviour.
/// iOS has no Foreground Service like Android. The user swipes the app away in the App Switcher → BGAppRefresh
/// also does NOT run anymore until the app is opened again. No workarounds (plan: *do not bypass platform restrictions*):
/// no silent audio playback, no fake background location.
@MainActor
@Observable
final class BackgroundMonitor {

    static let shared = BackgroundMonitor()

    /// Must MATCH the item in Info.plist `BGTaskSchedulerPermittedIdentifiers` (project.yml) — off by 1 character and
    /// `submit` throws `.notPermitted`.
    nonisolated static let refreshTaskId = "com.learn.NativeDeviceMonitor.refresh"
    nonisolated static let defaultInterval: Duration = .seconds(10)   // used as a default argument → must be readable outside MainActor
    /// A hint to iOS "don't run earlier than this" — NOT a schedule.
    static let refreshEarliest: TimeInterval = 15 * 60

    // The plan's `getStatus()` = reading the properties below (the View reads them directly, @Observable tracks them).
    private(set) var isRunning = false
    private(set) var interval: Duration = defaultInterval
    private(set) var count = 0
    private(set) var lastRecord: DeviceStatus?
    private(set) var lastError: String?
    /// Result of the latest BGAppRefresh `submit` — the Simulator always reports an error (BGTaskScheduler doesn't run on the Simulator).
    private(set) var refreshScheduleResult: String?
    /// Latest BGAppRefresh that actually ran (read from the log when the app opens — may have been a different process).
    private(set) var lastRefreshRun: String?

    private var task: Task<Void, Never>?

    private init() {}

    /// `false` = already running.
    @discardableResult
    func start(interval: Duration = defaultInterval) -> Bool {
        guard task == nil else {
            AppLog.d("Monitor", "start ignored — already running")
            return false
        }
        self.interval = interval
        count = 0
        isRunning = true
        AppLog.d("Monitor", "start interval=\(interval)")
        task = Task { [weak self] in
            while !Task.isCancelled {
                guard let self else { return }
                let record = await self.collectAndSave(source: "loop")
                if let record { self.count += 1; self.lastRecord = record }
                do { try await Task.sleep(for: interval) } catch { break }   // cancelled → exit the loop immediately
            }
        }
        return true
    }

    func stop() {
        if task != nil { AppLog.d("Monitor", "stop after \(count) records") }
        task?.cancel()
        task = nil
        isRunning = false
    }

    /// collect → record → save ONCE. Shared by the loop, BGAppRefresh and the "Append to log" button.
    @discardableResult
    func collectAndSave(source: String) async -> DeviceStatus? {
        let record = DeviceStatusCollector.collect(source: source)
        let ok = await FileLogger.shared.append(record)
        lastError = ok ? nil : await FileLogger.shared.lastError
        AppLog.d("Monitor", "tick source=\(source) ok=\(ok)")
        return ok ? record : nil
    }

    func getLogs() async -> LogReadResult { await FileLogger.shared.readLogs() }

    @discardableResult
    func clearLogs() async -> Bool { await FileLogger.shared.clearLogs() }

    // MARK: - Day 14: BGTaskScheduler

    /// Called when the app goes to the background. Submitting the same id again = REPLACES the old request (one pending request per id).
    func scheduleAppRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: Self.refreshTaskId)
        request.earliestBeginDate = Date(timeIntervalSinceNow: Self.refreshEarliest)
        do {
            try BGTaskScheduler.shared.submit(request)
            refreshScheduleResult = "Scheduled (earliest \(DeviceStatus.timestamp(request.earliestBeginDate ?? .now)))"
        } catch {
            // Simulator: BGTaskSchedulerErrorDomain code 1 (.unavailable). Real device with Background App Refresh off: also 1.
            refreshScheduleResult = "Submit failed: \(error.localizedDescription)"
        }
        AppLog.d("Monitor", "scheduleAppRefresh → \(refreshScheduleResult ?? "")")
    }

    /// iOS calls this when it lets the app run in the background (~30 s). Schedule the next one FIRST — if cut off mid-way the schedule still exists.
    func handleAppRefresh() async {
        scheduleAppRefresh()
        await collectAndSave(source: "bgRefresh")
        lastRefreshRun = DeviceStatus.timestamp()
    }

    /// Debug on a real device (doesn't run on the Simulator): app in background → Xcode Pause → paste into LLDB:
    /// `e -l objc -- (void)[[BGTaskScheduler sharedScheduler] _simulateLaunchForTaskWithIdentifier:@"com.learn.NativeDeviceMonitor.refresh"]`
    /// → Continue → `handleAppRefresh` runs → the file gets a `"source":"bgRefresh"` line.
    nonisolated static let lldbSimulateCommand =
        "e -l objc -- (void)[[BGTaskScheduler sharedScheduler] _simulateLaunchForTaskWithIdentifier:@\"\(refreshTaskId)\"]"
}
