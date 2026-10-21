import Foundation
import Testing
@testable import NativeDeviceMonitor

/// MVVM lesson 01 — ViewModel tests. No screen, no waiting: the VM gets an in-memory store and a fake clock
/// (`clock.now += 25 * 60` = 25 minutes pass instantly). This is the payoff of injecting dependencies through `init`.
@MainActor
struct PomodoroViewModelTests {

    /// A clock we move by hand. `final class` so the VM's closure and the test see the same value.
    final class FakeClock {
        var now = Date(timeIntervalSince1970: 1_800_000_000)
    }

    private let clock = FakeClock()
    private let store = InMemoryPomodoroSessionStore()

    private func makeVM() -> PomodoroViewModel {
        PomodoroViewModel(store: store, now: { [clock] in clock.now })
    }

    @Test func startsAt25MinutesWithStartButton() {
        let vm = makeVM()
        #expect(vm.timeText == "25:00")
        #expect(vm.primaryTitle == PomodoroStrings.start)
        #expect(vm.phaseTitle == PomodoroStrings.focus)
        #expect(vm.progress == 0)
        #expect(!vm.canReset)
    }

    @Test func primaryButtonCyclesStartPauseResume() {
        let vm = makeVM()
        vm.primaryTapped()
        #expect(vm.primaryTitle == PomodoroStrings.pause)
        #expect(vm.isRunning)
        clock.now += 1
        vm.primaryTapped()
        #expect(vm.primaryTitle == PomodoroStrings.resume)
        #expect(vm.timeText == "24:59")
    }

    @Test func timeTextRoundsUp() {
        let vm = makeVM()
        let started = clock.now
        vm.primaryTapped()
        clock.now = started + 0.4      // 24:59.6 left → still "25:00"
        vm.tick()
        #expect(vm.timeText == "25:00")
        clock.now = started + 60       // exactly 24:00 left
        vm.tick()
        #expect(vm.timeText == "24:00")
        clock.now = started + 60.5     // 23:59.5 left → "24:00"
        vm.tick()
        #expect(vm.timeText == "24:00")
    }

    @Test func finishingFocusSavesAndCountsAndSwitchesToBreak() {
        let vm = makeVM()
        vm.primaryTapped()
        clock.now += 25 * 60
        vm.tick()
        #expect(vm.phaseTitle == PomodoroStrings.rest)
        #expect(vm.timeText == "05:00")
        #expect(vm.primaryTitle == PomodoroStrings.start)
        #expect(vm.todayCount == 1)
        #expect(store.dates.count == 1)
    }

    @Test func skipDoesNotCount() {
        let vm = makeVM()
        vm.primaryTapped()
        vm.skip()
        #expect(vm.phaseTitle == PomodoroStrings.rest)
        #expect(vm.todayCount == 0)
    }

    @Test func resetReturnsToFreshFocus() {
        let vm = makeVM()
        vm.primaryTapped()
        clock.now += 100
        vm.reset()
        #expect(vm.timeText == "25:00")
        #expect(!vm.isRunning)
        #expect(!vm.canReset)
    }

    @Test func comingBackFromBackgroundCatchesUp() {
        let vm = makeVM()
        vm.primaryTapped()
        clock.now += 3 * 60          // phone locked 3 minutes, no ticks happened
        vm.appBecameActive()
        #expect(vm.timeText == "22:00")
    }

    @Test func todayCountIgnoresOtherDays() {
        store.recordCompletedFocus(at: clock.now - 2 * 24 * 3600)
        store.recordCompletedFocus(at: clock.now)
        #expect(makeVM().todayCount == 1)
    }
}
