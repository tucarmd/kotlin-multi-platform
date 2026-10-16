import SwiftUI
import Shared

/// [App 1 · B4] App entry: starts Koin once, then builds the one model the screen needs.
/// Android does the same in TipSplitApp.kt.
///
/// [Bài 6 · hai điểm khởi động] This @main struct is iOS's entry point, the twin of MainActivity + TipSplitApp.
/// [Bài 5 · Koin] `doInitKoin()` is Kotlin's `initKoin()` renamed for Objective-C. It starts the SAME
/// commonModule + platformModule (iosMain) as Android, so `tipCore()` returns the one shared TipCore.
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
