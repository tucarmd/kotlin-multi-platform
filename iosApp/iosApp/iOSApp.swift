import SwiftUI
import Shared

/// Entry point. Since R2 the UI is SwiftUI (no more Compose `MainViewController`); the logic is the shared Kotlin
/// `AppViewModel` held by `AppModel`.
///
/// ```
///  iOSApp
///   └─ WindowGroup
///       └─ RootView(model)          TabView, one SwiftUI View per tab
///   scenePhase == .active → vm.onResume()   back from Settings: re-read permissions (same as Android onResume)
/// ```
@main
struct iOSApp: App {
    @StateObject private var model = AppModel()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            RootView(model: model)
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { model.vm.onResume() }
        }
    }
}
