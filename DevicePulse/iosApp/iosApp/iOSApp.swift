import SwiftUI
import Shared

@main
struct iOSApp: App {
    // Built once, like DevicePulseApp.core on Android. createPulseCore() comes from shared/iosMain.
    @State private var model = PulseModel(core: createPulseCore())
    @State private var permission = LocationPermission()

    var body: some Scene {
        WindowGroup {
            RootView(model: model, permission: permission)
        }
    }
}
