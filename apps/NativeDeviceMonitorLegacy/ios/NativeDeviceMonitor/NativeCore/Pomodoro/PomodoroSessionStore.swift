import Foundation

/// MVVM lesson 01 — where finished focus sessions are kept. The ViewModel only knows this **protocol** (the "socket"):
/// the app plugs in `UserDefaultsPomodoroSessionStore`, tests and `#Preview` plug in `InMemoryPomodoroSessionStore`.
/// Moving to a file / SwiftData later = one new type, the ViewModel and View don't change.
protocol PomodoroSessionStore {
    func completedFocusDates() -> [Date]
    func recordCompletedFocus(at date: Date)
}

/// Real store: an array of timestamps in UserDefaults — survives closing the app.
final class UserDefaultsPomodoroSessionStore: PomodoroSessionStore {

    private let defaults: UserDefaults
    private let key = "pomodoro.completedFocus"
    /// Only "today" is shown — keep the last 200 so the array can't grow forever.
    private let keepLast = 200

    init(defaults: UserDefaults = .standard) { self.defaults = defaults }

    func completedFocusDates() -> [Date] {
        (defaults.array(forKey: key) as? [Double] ?? []).map(Date.init(timeIntervalSince1970:))
    }

    func recordCompletedFocus(at date: Date) {
        let all = completedFocusDates().map(\.timeIntervalSince1970) + [date.timeIntervalSince1970]
        defaults.set(Array(all.suffix(keepLast)), forKey: key)
    }
}

/// Fake for tests and previews — lives in memory, gone when the process ends.
final class InMemoryPomodoroSessionStore: PomodoroSessionStore {
    private(set) var dates: [Date]

    init(dates: [Date] = []) { self.dates = dates }

    func completedFocusDates() -> [Date] { dates }
    func recordCompletedFocus(at date: Date) { dates.append(date) }
}
