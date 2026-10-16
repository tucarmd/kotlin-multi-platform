import SwiftUI
import Shared

/// [App 3 · B6] Two tabs sharing one RatesModel. The iOS twin of App.kt (NavigationBar + NavHost).
struct RootView: View {
    let model: RatesModel
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        TabView {
            RatesView(model: model)
                .tabItem { Label(L("tab_rates"), systemImage: "list.bullet") }
            ConvertView(model: model)
                .tabItem { Label(L("tab_convert"), systemImage: "arrow.left.arrow.right") }
        }
        .tint(Color(red: 0, green: 0.47, blue: 0.42))
        // Every time the app becomes active: update "… ago" and auto-refresh if older than 1 h.
        // PLATFORM: Android does the same with LifecycleResumeEffect in App.kt.
        .onChange(of: scenePhase, initial: true) { _, phase in
            if phase == .active { model.start() }
        }
    }
}
