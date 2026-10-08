import Foundation

/// Minimal internal log: prints to the Xcode console as `[Tag] message`.
///
/// This phase needs no in-memory log store or Log Viewer screen, just `AppLog.d/e`.
/// Internal logs are in English like everything else (Rule 4 is about UI text).
enum AppLog {
    static func d(_ tag: String, _ message: String) { print("[\(tag)] \(message)") }
    static func e(_ tag: String, _ message: String) { print("[\(tag)] ERROR \(message)") }
}
