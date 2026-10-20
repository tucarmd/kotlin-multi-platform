import Foundation
import Observation

/// MVVM lesson 01 — the **ViewModel**: holds the screen's state and turns it into ready-to-show values
/// (`timeText`, `progress`, button title). The View only reads these and calls the 4 intents below.
///
/// ```
///  PomodoroView ──reads──▶ timeText · progress · phaseTitle · primaryTitle · todayText
///        │       ──calls──▶ primaryTapped() · reset() · skip() · tick() · appBecameActive()
///        ▼
///  PomodoroViewModel ──▶ PomodoroEngine (rules)   ──▶ PomodoroSessionStore (saved sessions)
/// ```
///
/// - `import Observation`, **not SwiftUI** → unit tests don't need a screen.
/// - Dependencies come in through `init` (store + clock) → tests pass an in-memory store and a fake clock.
/// - `@MainActor`: SwiftUI reads these properties on the main thread, so they are only ever changed there.
@MainActor
@Observable
final class PomodoroViewModel {

    private(set) var state = PomodoroState()
    private(set) var todayCount = 0

    private let store: PomodoroSessionStore
    private let now: () -> Date
    private let calendar: Calendar

    init(store: PomodoroSessionStore, now: @escaping () -> Date = Date.init, calendar: Calendar = .current) {
        self.store = store
        self.now = now
        self.calendar = calendar
        refreshTodayCount()
    }

    // MARK: - Output (what the View shows)

    /// "25:00". Rounded UP: 24:59.4 left still reads 25:00, like every kitchen timer.
    var timeText: String {
        let seconds = Int(state.remaining.rounded(.up))
        return String(format: "%02d:%02d", seconds / 60, seconds % 60)
    }

    /// 0 = phase just started, 1 = phase done.
    var progress: Double { 1 - state.remaining / state.phase.duration }

    var phaseTitle: String { state.phase == .focus ? PomodoroStrings.focus : PomodoroStrings.rest }

    var isFocus: Bool { state.phase == .focus }

    var isRunning: Bool {
        if case .running = state.status { return true }
        return false
    }

    var primaryTitle: String {
        switch state.status {
        case .idle: PomodoroStrings.start
        case .running: PomodoroStrings.pause
        case .paused: PomodoroStrings.resume
        }
    }

    var canReset: Bool { state != PomodoroState() }

    var todayText: String { PomodoroStrings.today(todayCount) }

    // MARK: - Intents (what the View calls)

    func primaryTapped() {
        state = isRunning ? PomodoroEngine.pause(state, now: now()) : PomodoroEngine.start(state, now: now())
    }

    func reset() { state = PomodoroEngine.reset(state) }

    func skip() { state = PomodoroEngine.skip(state) }

    /// Called by the View's loop while running. A finished focus is saved and the counter refreshed.
    func tick() {
        let result = PomodoroEngine.tick(state, now: now())
        state = result.state
        if let finished = result.completedFocusAt {
            store.recordCompletedFocus(at: finished)
            refreshTodayCount()
        }
    }

    /// Back from the background / lock screen: catch up at once (the loop was suspended) and
    /// re-count "today" — the day may have changed overnight.
    func appBecameActive() {
        tick()
        refreshTodayCount()
    }

    private func refreshTodayCount() {
        todayCount = PomodoroEngine.countOnSameDay(store.completedFocusDates(), as: now(), calendar: calendar)
    }
}
