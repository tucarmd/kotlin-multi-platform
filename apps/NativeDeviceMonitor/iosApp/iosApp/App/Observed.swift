import SwiftUI
import Shared

/// The bridge between a Kotlin `StateFlow` and SwiftUI — the Swift twin of Compose's `collectAsState()`.
///
/// ```
///  Kotlin ViewModel                      Swift
///  val state: StateFlow<ApiUiState>  ──► SKIE: SkieSwiftStateFlow<ApiUiState> (an AsyncSequence)
///                                        Observed: for await v in flow { value = v }   → @Published
///                                        View: @StateObject var api = Observed(vm.state)  → redraws on every value
/// ```
///
/// SKIE (Gradle plugin in `shared/build.gradle.kts`) is what makes `for await` work: without it Swift only sees an
/// Objective-C `Kotlinx_coroutines_coreStateFlow` with a callback-based `collect`.
@MainActor
final class Observed<T>: ObservableObject {
    @Published private(set) var value: T
    private var task: Task<Void, Never>?

    init(_ flow: SkieSwiftStateFlow<T>) {
        value = flow.value
        // Kotlin writes state on Dispatchers.Main → values arrive on the main thread, safe to publish.
        task = Task { [weak self] in
            for await next in flow {
                self?.value = next
            }
        }
    }

    deinit {
        task?.cancel()
    }
}
