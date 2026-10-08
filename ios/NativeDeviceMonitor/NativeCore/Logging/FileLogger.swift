import Foundation

/// Log read result: valid records + number of broken lines skipped. Mirrors `LogReadResult` on Android.
struct LogReadResult: Equatable, Sendable {
    var records: [DeviceStatus]
    var corruptLines: Int
    /// IO error. `nil` = read fine.
    var error: String? = nil
}

/// Week 3 · Day 12–13 — write / read / clear a JSONL file. Mirrors `android/.../nativecore/logging/FileLogger.kt`.
///
/// ```
///  append(status)  ──► no file yet → write(to:) · exists → FileHandle.seekToEnd → write(contentsOf:)
///  readLogs()      ──► no file yet → [] · split("\n") → JSONDecoder per line (broken → corruptLines += 1)
///  clearLogs()     ──► removeItem (doesn't exist → ignore)
/// ```
///
/// `actor` = Swift's version of `@Synchronized`: the monitor loop, BGAppRefresh and the "Append" button calling at once →
/// the actor queues each call → two lines never interleave. Calls need `await`.
///
/// Location: `Library/Application Support/monitor.jsonl` — private to the app, not visible in the Files app,
/// included in iCloud backup. Simulator: `xcrun simctl get_app_container booted com.learn.NativeDeviceMonitor data`.
actor FileLogger {

    static let fileName = "monitor.jsonl"

    /// 1 instance for the whole app (like `FileLogger.shared(context)` on Android).
    static let shared: FileLogger = {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        return FileLogger(url: dir.appendingPathComponent(fileName))
    }()

    /// Takes a URL → unit tests pass a temp directory.
    nonisolated let url: URL     // let + Sendable: readable without await (even from the test module)
    private(set) var lastError: String?

    init(url: URL) { self.url = url }

    /// `true` = written. On error → remember `lastError`, the monitor carries on next tick.
    @discardableResult
    func append(_ status: DeviceStatus) -> Bool {
        do {
            let line = Data((try status.toJson() + "\n").utf8)
            let fm = FileManager.default
            if fm.fileExists(atPath: url.path) {
                let handle = try FileHandle(forWritingTo: url)
                defer { try? handle.close() }      // forget to close = leaked file descriptor, eventually "Too many open files"
                try handle.seekToEnd()
                try handle.write(contentsOf: line)
            } else {
                // Application Support does NOT exist on a fresh install → create the directory first.
                try fm.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
                try line.write(to: url, options: .atomic)
            }
            return true
        } catch {
            lastError = error.localizedDescription
            AppLog.e("FileLogger", "append: \(error)")
            return false
        }
    }

    func readLogs() -> LogReadResult {
        guard FileManager.default.fileExists(atPath: url.path) else { return LogReadResult(records: [], corruptLines: 0) }
        let text: String
        do {
            text = try String(contentsOf: url, encoding: .utf8)
        } catch {
            return LogReadResult(records: [], corruptLines: 0, error: error.localizedDescription)
        }
        var corrupt = 0
        let records: [DeviceStatus] = text
            .split(separator: "\n", omittingEmptySubsequences: true)   // empty lines skipped, not counted as broken
            .compactMap { line in
                do { return try DeviceStatus.fromJson(line) } catch { corrupt += 1; return nil }
            }
        return LogReadResult(records: records, corruptLines: corrupt)
    }

    @discardableResult
    func clearLogs() -> Bool {
        do {
            if FileManager.default.fileExists(atPath: url.path) { try FileManager.default.removeItem(at: url) }
            return true
        } catch {
            lastError = error.localizedDescription
            return false
        }
    }
}
