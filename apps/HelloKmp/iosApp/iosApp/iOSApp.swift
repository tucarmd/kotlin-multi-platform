import SwiftUI
import Shared

/// [App 0 · 1.1] App entry, the twin of MainActivity. For now it just shows a string from shared/ to prove
/// Shared.framework is linked; step 1.4 replaces the body with HelloView.
///
/// A top-level Kotlin function in Scaffold.kt shows up in Swift as a static method of `ScaffoldKt`.
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            Text(ScaffoldKt.scaffoldCheck())
        }
    }
}
