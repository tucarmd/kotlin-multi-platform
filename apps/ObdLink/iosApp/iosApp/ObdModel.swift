import Foundation
import Observation
import Shared

/// [App 5 · B6] ObdModel: bridges the Kotlin ObdCore to SwiftUI. The iOS twin of ObdViewModel on Android.
///
/// Why @Observable: SwiftUI redraws only the views that read a property that changed (the gauges redraw
/// several times a second; the Connect tab does not).
/// The Kotlin StateFlow is read with `for await` because SKIE turns it into an AsyncSequence.
/// No logic here: every action forwards to ObdCore, which is tested in shared/commonTest.
///
/// Used by: RootView and the four tab views. Uses: ObdCore from `Shared`.
@MainActor @Observable
final class ObdModel {
    private(set) var state: ObdState
    private let core: ObdCore

    init(core: ObdCore) {
        self.core = core
        self.state = core.state.value
        Task { [weak self] in
            // PLATFORM: ObdCore publishes from a Kotlin background thread; this Task is @MainActor, so the
            // assignment below always happens on the main thread, as SwiftUI requires.
            for await state in core.state { self?.state = state }
        }
    }

    func scan() { core.scan() }
    func stopScan() { core.stopScan() }
    func connect(_ dongle: Dongle) { core.connect(dongle: dongle) }
    func disconnect() { core.disconnect() }
    func reset() { core.reset() }
    func startLive() { core.startLive() }
    func stopLive() { core.stopLive() }
    func readCodes() { core.readCodes() }
    func requestClearCodes() { core.requestClearCodes() }
    func confirmClearCodes() { core.confirmClearCodes() }
    func cancelClearCodes() { core.cancelClearCodes() }
    func sendConsole(_ text: String) { core.sendConsole(typed: text) }
    func clearConsole() { core.clearConsole() }
}
