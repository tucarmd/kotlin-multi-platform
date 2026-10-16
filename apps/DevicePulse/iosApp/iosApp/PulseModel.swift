import Foundation
import Observation
import Shared

/// The iOS twin of PulseViewModel on Android: the shared PulseMonitor does the work,
/// this class only republishes its two StateFlows so SwiftUI redraws.
@MainActor @Observable
final class PulseModel {
    private(set) var state: DashboardState
    private(set) var snapshots: [Snapshot] = []
    private let monitor: PulseMonitor

    init(core: PulseCore) {
        let monitor = core.startMonitor()
        self.monitor = monitor
        self.state = monitor.state.value
        // SKIE turns each StateFlow into an AsyncSequence. Collecting keeps the flow subscribed,
        // so the sensors run while the app is on screen.
        Task { [weak self] in
            for await state in monitor.state { self?.state = state }
        }
        Task { [weak self] in
            for await list in monitor.snapshots { self?.snapshots = list }
        }
    }

    func measureLatency() { monitor.measureLatency() }
    func refreshLocation() { monitor.refreshLocation() }
    func saveSnapshot() { monitor.saveSnapshot() }
    func clearHistory() { monitor.clearHistory() }
}
