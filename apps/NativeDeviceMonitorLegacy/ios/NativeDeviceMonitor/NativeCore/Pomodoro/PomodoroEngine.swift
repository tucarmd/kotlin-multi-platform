import Foundation

/// MVVM lesson 01 — the **Model** layer of the mini Pomodoro timer. Pure values + pure functions, no SwiftUI, no timer,
/// no `Date()` inside: the current time is always passed in (`now:`), so tests can "fast-forward 25 minutes" in one line.
///
/// ```
///  idle ──start──▶ running(endDate) ──pause──▶ paused ──start──▶ running(endDate')
///    ▲                 │ tick: now ≥ endDate
///    └── next phase ◀──┘   (focus → rest → focus …, the next phase waits for Start)
/// ```
///
/// Why `endDate` instead of `remaining -= 1` every second: iOS suspends the app when it goes to the background,
/// a per-second counter would freeze. With `endDate`, `remaining = endDate - now` is right whenever we look again.
enum PomodoroPhase: Equatable, Sendable {
    case focus, rest

    var duration: TimeInterval {
        switch self {
        case .focus: 25 * 60
        case .rest: 5 * 60
        }
    }

    var next: PomodoroPhase { self == .focus ? .rest : .focus }
}

/// `endDate` lives INSIDE `.running` — an idle or paused timer can't carry a stale end date by accident.
enum PomodoroStatus: Equatable, Sendable {
    case idle
    case running(endDate: Date)
    case paused
}

struct PomodoroState: Equatable, Sendable {
    var phase: PomodoroPhase = .focus
    var status: PomodoroStatus = .idle
    /// Seconds left. While running it is refreshed by `tick` — the truth is `endDate`.
    var remaining: TimeInterval = PomodoroPhase.focus.duration
}

/// Result of a tick: the new state + when a focus session finished (nil = nothing finished this tick).
struct PomodoroTick: Equatable, Sendable {
    var state: PomodoroState
    var completedFocusAt: Date?
}

/// Every rule of the timer. An `enum` with no cases = a namespace, like `AppColor`.
enum PomodoroEngine {

    /// idle / paused → running. Already running → unchanged.
    static func start(_ state: PomodoroState, now: Date) -> PomodoroState {
        if case .running = state.status { return state }
        var s = state
        s.status = .running(endDate: now.addingTimeInterval(state.remaining))
        return s
    }

    /// running → paused, keeping the seconds left. Not running → unchanged.
    static func pause(_ state: PomodoroState, now: Date) -> PomodoroState {
        guard case .running(let endDate) = state.status else { return state }
        var s = state
        s.remaining = max(0, endDate.timeIntervalSince(now))
        s.status = .paused
        return s
    }

    /// Back to a fresh focus session, nothing recorded.
    static func reset(_ state: PomodoroState) -> PomodoroState { PomodoroState() }

    /// Jump to the next phase, idle, full duration. A skipped focus is NOT counted.
    static func skip(_ state: PomodoroState) -> PomodoroState {
        let next = state.phase.next
        return PomodoroState(phase: next, status: .idle, remaining: next.duration)
    }

    /// Refresh `remaining`; when time is up, move to the next phase (idle) and report the finished focus.
    /// The finish time is `endDate`, not `now` — coming back the next morning still counts it for the right day.
    static func tick(_ state: PomodoroState, now: Date) -> PomodoroTick {
        guard case .running(let endDate) = state.status else { return PomodoroTick(state: state) }
        let left = endDate.timeIntervalSince(now)
        if left > 0 {
            var s = state
            s.remaining = left
            return PomodoroTick(state: s)
        }
        let next = state.phase.next
        return PomodoroTick(
            state: PomodoroState(phase: next, status: .idle, remaining: next.duration),
            completedFocusAt: state.phase == .focus ? endDate : nil
        )
    }

    /// How many focus sessions finished on the same calendar day as `day`.
    static func countOnSameDay(_ dates: [Date], as day: Date, calendar: Calendar) -> Int {
        dates.filter { calendar.isDate($0, inSameDayAs: day) }.count
    }
}

extension PomodoroTick {
    init(state: PomodoroState) { self.init(state: state, completedFocusAt: nil) }
}
