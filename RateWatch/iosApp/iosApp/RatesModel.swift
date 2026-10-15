import Foundation
import Observation
import Shared

/// [App 3 · B6] RatesModel: bridges the Kotlin RatesCore to SwiftUI. The iOS twin of RatesViewModel on Android.
///
/// Why @Observable: SwiftUI redraws only the views that read a property that changed.
/// Both Kotlin StateFlows are read with `for await` because SKIE turns them into AsyncSequences.
/// No logic here: every action forwards to RatesCore, which is tested in shared/commonTest.
///
/// Used by: RootView, RatesView, ConvertView. Uses: RatesCore from `Shared`.
@MainActor @Observable
final class RatesModel {
    private(set) var state: RatesState
    private(set) var convert: ConvertState
    private let core: RatesCore

    init(core: RatesCore) {
        self.core = core
        self.state = core.state.value
        self.convert = core.convert.value
        Task { [weak self] in
            for await state in core.state { self?.state = state }
        }
        Task { [weak self] in
            for await convert in core.convert { self?.convert = convert }
        }
    }

    func start() { core.start() }
    func refresh() { core.refresh() }

    /// For `.refreshable`: SwiftUI keeps the pull spinner until this async function returns.
    /// PLATFORM: Android's PullToRefreshBox reads `refreshing` directly; SwiftUI needs an `async` closure instead,
    /// so we poll the published state. The first sleep gives the core time to switch `refreshing` on.
    func refreshAndWait() async {
        core.refresh()
        try? await Task.sleep(for: .milliseconds(150))
        while state.refreshing {
            try? await Task.sleep(for: .milliseconds(100))
        }
    }

    func selectBase(_ code: String) { core.selectBase(code: code) }
    func toggleFavorite(_ code: String) { core.toggleFavorite(code: code) }
    func onAmountChanged(_ text: String) { core.onAmountChanged(text: text) }
    func onFromSelected(_ code: String) { core.onFromSelected(code: code) }
    func onToSelected(_ code: String) { core.onToSelected(code: code) }
    func swapCurrencies() { core.swapCurrencies() }
}
