import SwiftUI
import Shared

/// Owns the shared `AppViewModel` for the whole app — the iOS counterpart of `MainActivity` holding it on Android.
///
/// ```
///  iOSApp ──@StateObject──► AppModel
///                             ├─ vm: AppViewModel(services: iosServices)   ← Kotlin (shared/)
///                             │     ├─ permissions · scanner · media · api · monitor   (tab ViewModels)
///                             │     └─ tab: StateFlow<AppTab>
///                             └─ @Published tab                             ← mirrored for TabView(selection:)
/// ```
///
/// Kotlin ViewModels have no `ViewModelStore` on iOS → whoever creates them must call `clear()` (here: `deinit`),
/// otherwise their coroutines (scan clock, monitor collectors) keep running.
@MainActor
final class AppModel: ObservableObject {
    let vm: AppViewModel
    @Published private(set) var tab: AppTab
    private var task: Task<Void, Never>?

    init() {
        // `xcrun simctl launch booted com.learn.ndmkmp -tab monitor` opens a tab directly (screenshots), like `--es tab` on Android.
        let vm = AppViewModel(services: IosServicesKt.iosServices, startTab: Self.launchArgument("-tab"))
        self.vm = vm
        tab = vm.tab.value
        task = Task { [weak self] in
            for await next in vm.tab {
                self?.tab = next
            }
        }
    }

    func select(_ tab: AppTab) {
        vm.selectTab(tab: tab)
    }

    deinit {
        task?.cancel()
        vm.clear()
    }

    private static func launchArgument(_ name: String) -> String? {
        let args = ProcessInfo.processInfo.arguments
        guard let i = args.firstIndex(of: name), i + 1 < args.count else { return nil }
        return args[i + 1]
    }
}
