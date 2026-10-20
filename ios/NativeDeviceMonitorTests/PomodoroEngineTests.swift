import Foundation
import Testing
@testable import NativeDeviceMonitor

/// MVVM lesson 01 — Model rules. Time is a plain `Date` we pass in, so "25 minutes later" is `t0 + 25 * 60`.
struct PomodoroEngineTests {

    private let t0 = Date(timeIntervalSince1970: 1_800_000_000)

    @Test func freshStateIsIdleFocus25Minutes() {
        let s = PomodoroState()
        #expect(s.phase == .focus)
        #expect(s.status == .idle)
        #expect(s.remaining == 25 * 60)
    }

    @Test func startSetsEndDateFromRemaining() {
        let s = PomodoroEngine.start(PomodoroState(), now: t0)
        #expect(s.status == .running(endDate: t0 + 25 * 60))
    }

    @Test func startWhileRunningChangesNothing() {
        let running = PomodoroEngine.start(PomodoroState(), now: t0)
        #expect(PomodoroEngine.start(running, now: t0 + 60) == running)
    }

    @Test func tickRefreshesRemaining() {
        let running = PomodoroEngine.start(PomodoroState(), now: t0)
        let r = PomodoroEngine.tick(running, now: t0 + 61)
        #expect(r.state.remaining == 25 * 60 - 61)
        #expect(r.completedFocusAt == nil)
    }

    @Test func pauseKeepsRemainingAndResumeMovesEndDate() {
        let running = PomodoroEngine.start(PomodoroState(), now: t0)
        let paused = PomodoroEngine.pause(running, now: t0 + 600)          // 10 min in
        #expect(paused.status == .paused)
        #expect(paused.remaining == 15 * 60)
        let resumed = PomodoroEngine.start(paused, now: t0 + 3600)          // resume an hour later
        #expect(resumed.status == .running(endDate: t0 + 3600 + 15 * 60))
    }

    @Test func pausedTimerDoesNotMoveOnTick() {
        let paused = PomodoroEngine.pause(PomodoroEngine.start(PomodoroState(), now: t0), now: t0 + 60)
        #expect(PomodoroEngine.tick(paused, now: t0 + 9999).state == paused)
    }

    @Test func focusEndsGoesToIdleBreakAndReportsEndDate() {
        let running = PomodoroEngine.start(PomodoroState(), now: t0)
        // Came back 2 hours later: still ONE finished focus, stamped at its real end time.
        let r = PomodoroEngine.tick(running, now: t0 + 7200)
        #expect(r.state == PomodoroState(phase: .rest, status: .idle, remaining: 5 * 60))
        #expect(r.completedFocusAt == t0 + 25 * 60)
    }

    @Test func breakEndsGoesToFocusWithoutCounting() {
        let rest = PomodoroEngine.start(PomodoroState(phase: .rest, status: .idle, remaining: 5 * 60), now: t0)
        let r = PomodoroEngine.tick(rest, now: t0 + 5 * 60)
        #expect(r.state.phase == .focus)
        #expect(r.completedFocusAt == nil)
    }

    @Test func skipGoesToNextPhaseIdleFull() {
        let running = PomodoroEngine.start(PomodoroState(), now: t0)
        #expect(PomodoroEngine.skip(running) == PomodoroState(phase: .rest, status: .idle, remaining: 5 * 60))
    }

    @Test func resetGoesBackToFreshFocus() {
        let rest = PomodoroState(phase: .rest, status: .paused, remaining: 42)
        #expect(PomodoroEngine.reset(rest) == PomodoroState())
    }

    @Test func countsOnlyTheSameCalendarDay() {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(identifier: "Asia/Ho_Chi_Minh")!
        let day = cal.date(from: DateComponents(year: 2026, month: 10, day: 13, hour: 12))!
        let dates = [
            cal.date(from: DateComponents(year: 2026, month: 10, day: 12, hour: 23, minute: 59))!,  // yesterday
            cal.date(from: DateComponents(year: 2026, month: 10, day: 13, hour: 0, minute: 1))!,
            cal.date(from: DateComponents(year: 2026, month: 10, day: 13, hour: 22))!,
        ]
        #expect(PomodoroEngine.countOnSameDay(dates, as: day, calendar: cal) == 2)
    }
}
