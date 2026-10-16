import SwiftUI
import Shared

/// [App 5 · B4] App entry: starts Koin once, then builds the one model every tab shares.
/// Android does the same in ObdLinkApp.kt.
@main
struct iOSApp: App {
    @State private var model: ObdModel

    init() {
        doInitKoin()                                         // Koin.kt: initKoin(), same modules as Android
        _model = State(initialValue: ObdModel(core: obdCore()))  // KoinIos.kt: obdCore()
    }

    var body: some Scene {
        WindowGroup {
            RootView(model: model)
        }
    }
}
