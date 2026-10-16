import SwiftUI
import Shared

/// [App 3 · B4] App entry: starts Koin once, then builds the one model both tabs share.
/// Android does the same in RateWatchApp.kt.
@main
struct iOSApp: App {
    @State private var model: RatesModel

    init() {
        doInitKoin()                                                 // Koin.kt: initKoin(), same modules as Android
        _model = State(initialValue: RatesModel(core: ratesCore()))  // KoinIos.kt: ratesCore()
    }

    var body: some Scene {
        WindowGroup {
            RootView(model: model)
        }
    }
}
