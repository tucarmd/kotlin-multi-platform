import SwiftUI
import Shared

@main
struct iOSApp: App {
    @State private var model: PulseModel
    @State private var permission = LocationPermission()

    init() {
        doInitKoin()                                  // Koin.kt: initKoin() — same modules as Android
        _model = State(initialValue: PulseModel(core: pulseCore()))
    }

    var body: some Scene {
        WindowGroup {
            RootView(model: model, permission: permission)
        }
    }
}
