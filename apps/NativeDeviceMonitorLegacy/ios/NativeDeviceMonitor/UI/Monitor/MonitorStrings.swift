import Foundation

/// Monitor screen strings (Week 3). Mirrors `android/.../ui/monitor/MonitorStrings.kt` — same wording where the meaning is the same.
enum MonitorStrings {
    static let title = "Monitor"
    static let hint = "Week 3 — device providers, JSONL file logger and background monitor. "
        + "Start the loop, send the app to the background / kill it, come back and read the log."

    // MARK: Device status
    static let statusTitle = "Device status (now)"
    static let refresh = "Refresh status"
    static let appendNow = "Append to log"
    static let notReadYet = "Tap Refresh status to read battery, network and Bluetooth."
    static let nullNote = "null = could not be measured (Wi-Fi switch has no public API on iOS; Simulator has no battery)."

    // MARK: Monitor loop
    static let monitorTitle = "Background monitor"
    static let startLoop = "Start in-app loop (10 s)"
    static let stop = "Stop"
    static let running = "RUNNING"
    static let stopped = "STOPPED"
    static let loopHint = "iOS suspends the app a few seconds after it goes to the background — the loop pauses and resumes when you come back."
    static let noFgsHint = "iOS has no foreground service. Background time is granted by the system (BGAppRefresh below)."
    static func runningLine(intervalSec: Int64, count: Int) -> String { "in-app loop · every \(intervalSec) s · \(count) records this run" }

    // MARK: BGAppRefresh
    static let refreshTitle = "BGAppRefresh (system decides)"
    static let scheduleNow = "Schedule refresh now"
    static let refreshHint = "Scheduled automatically when the app goes to the background. Earliest 15 min — "
        + "iOS picks the real time, may never run. Swiping the app away stops it. Does not run on the Simulator."
    static func scheduleResult(_ s: String?) -> String { "Last submit: \(s ?? "—")" }
    static func lastRun(_ s: String?) -> String { "Last run (this session): \(s ?? "—")" }
    static let lldbTitle = "Trigger now (real iPhone, Xcode paused, app in background):"

    // MARK: Logs
    static let logsTitle = "Logs (monitor.jsonl)"
    static let reload = "Reload logs"
    static let clear = "Clear logs"
    static let empty = "No records yet."
    static func summary(_ count: Int, _ corrupt: Int) -> String {
        "\(count) records" + (corrupt > 0 ? " · \(corrupt) corrupt line(s) skipped" : "")
    }
    static func showingLast(_ n: Int) -> String { "Newest first · showing last \(n)" }
    static let cleared = "Log cleared."
    static let clearFailed = "Could not clear the log file."
    static let appendFailed = "Could not write to the log file."
}
