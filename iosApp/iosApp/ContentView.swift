import UIKit
import SwiftUI
import Shared

/// Bridges UIKit → SwiftUI: shows the Compose UI (`MainViewController` from shared/iosMain) inside a SwiftUI View.
/// `Shared` is the framework built by `./gradlew :shared:embedAndSignAppleFrameworkForXcode` (Xcode build phase).
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/// Full screen, edge to edge: App() handles insets itself (safeDrawingPadding), same as Android.
struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea()
    }
}
