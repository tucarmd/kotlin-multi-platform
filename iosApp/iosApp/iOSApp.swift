import SwiftUI
import Shared

/// 📱 iOS shell, counterpart of Android MainActivity: the whole app is one window showing ContentView.
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
