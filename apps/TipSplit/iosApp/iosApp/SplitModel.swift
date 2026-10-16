import Foundation
import Observation
import Shared

/// [App 1 · B6] SplitModel: bridges the Kotlin TipCore to SwiftUI. The iOS twin of SplitViewModel on Android.
///
/// Why @Observable: SwiftUI redraws only the views that read a property that changed.
/// The Kotlin StateFlow is read with `for await` because SKIE turns it into an AsyncSequence.
/// No logic here: every action forwards to TipCore, which is tested in shared/commonTest.
///
/// Used by: SplitView. Uses: TipCore from `Shared`.
///
/// [Bài 8 · ViewModel] iOS has no androidx ViewModel; an @Observable class owned by the App (@State in iOSApp)
/// plays the same role. [Bài 4] `import Shared` is the framework built from shared/ (baseName = "Shared").
@MainActor @Observable
final class SplitModel {
    private(set) var state: SplitState
    let tipOptions: [Int]
    private let core: TipCore

    init(core: TipCore) {
        self.core = core
        self.state = core.state.value
        // TRAP: Kotlin List<Int> arrives as [KotlinInt] (boxed numbers). Convert once here, not in every view.
        self.tipOptions = core.tipOptions.map { $0.intValue }
        Task { [weak self] in
            for await state in core.state { self?.state = state }
        }
    }

    func onBillChanged(_ text: String) { core.onBillChanged(text: text) }
    // PLATFORM: Kotlin Int is 32-bit, so Swift sees Int32 and needs an explicit conversion.
    func onTipSelected(_ percent: Int) { core.onTipSelected(percent: Int32(percent)) }
    func addPerson() { core.addPerson() }
    func removePerson() { core.removePerson() }
    func onRoundUpToggled() { core.onRoundUpToggled() }
    func reset() { core.reset() }
}
