import SwiftUI
import Shared

/// [App 1 · B4] App entry: starts Koin once, then builds the one model the screen needs.
/// Android does the same in TipSplitApp.kt.
@main
struct iOSApp: App {
    @State private var model: SplitModel

    init() {
        doInitKoin()                                  // Koin.kt: initKoin(), same modules as Android
        _model = State(initialValue: SplitModel(core: tipCore()))  // KoinIos.kt: tipCore()
    }

    var body: some Scene {
        WindowGroup {
            SplitView(model: model)
        }
    }
}
