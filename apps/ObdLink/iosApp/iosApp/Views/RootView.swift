import SwiftUI
import Shared

/// [App 5 · B6] Four tabs, same order as the Android NavigationBar (App.kt).
struct RootView: View {
    let model: ObdModel

    var body: some View {
        TabView {
            ConnectView(model: model)
                .tabItem { Label(L("tab_connect"), systemImage: "antenna.radiowaves.left.and.right") }
            LiveView(model: model)
                .tabItem { Label(L("tab_live"), systemImage: "gauge.with.dots.needle.67percent") }
            CodesView(model: model)
                .tabItem { Label(L("tab_codes"), systemImage: "exclamationmark.triangle") }
            ConsoleView(model: model)
                .tabItem { Label(L("tab_console"), systemImage: "terminal") }
        }
    }
}

/// Shown on Live / Codes / Console while no dongle is connected. Twin of ConnectFirst() in Components.kt.
struct ConnectFirstView: View {
    var body: some View {
        ContentUnavailableView(L("connect_first"), systemImage: "car")
    }
}
