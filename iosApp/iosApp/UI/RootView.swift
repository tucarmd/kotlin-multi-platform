import SwiftUI
import Shared

/// The app frame: 5 tabs from the shared `AppTab` list, icons are SF Symbols (Android uses Material icons).
/// Mirrors Android `App.kt`.
///
/// ```
///  TabView(selection: model.tab)
///   ├─ Permissions   PermissionView(vm.permissions)
///   ├─ Scanner       ScannerView(vm.scanner)      [Open Permissions] → select(.permissions)
///   ├─ Media         MediaView(vm.media)
///   ├─ API           ApiView(vm.api)
///   └─ Monitor       MonitorView(vm.monitor)
///  Each View wraps its ViewModel's `state` in `Observed` (StateFlow → @Published).
/// ```
struct RootView: View {
    @ObservedObject var model: AppModel

    var body: some View {
        TabView(selection: Binding(get: { model.tab }, set: { model.select($0) })) {
            ForEach(model.vm.tabs, id: \.self) { tab in
                NavigationStack {
                    screen(for: tab)
                        .navigationTitle(model.vm.title)
                        .navigationBarTitleDisplayMode(.inline)
                        .toolbarBackground(AppColor.primary, for: .navigationBar)
                        .toolbarBackground(.visible, for: .navigationBar)
                        .toolbarColorScheme(.dark, for: .navigationBar)
                }
                .tabItem { Label(tab.label, systemImage: icon(for: tab)) }
                .tag(tab)
            }
        }
        .tint(AppColor.primary)
    }

    @ViewBuilder
    private func screen(for tab: AppTab) -> some View {
        switch tab {
        case .permissions: PermissionView(vm: model.vm.permissions)
        case .scanner: ScannerView(vm: model.vm.scanner, onOpenPermissions: { model.select(.permissions) })
        case .media: MediaView(vm: model.vm.media)
        case .api: ApiView(vm: model.vm.api)
        case .monitor: MonitorView(vm: model.vm.monitor)
        }
    }

    private func icon(for tab: AppTab) -> String {
        switch tab {
        case .permissions: "lock.shield"
        case .scanner: "antenna.radiowaves.left.and.right"
        case .media: "camera"
        case .api: "network"
        case .monitor: "waveform.path.ecg"
        }
    }
}
