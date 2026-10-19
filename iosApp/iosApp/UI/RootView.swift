import SwiftUI
import Shared

/// The app frame: 5 tabs from the shared `AppTab` list, icons are SF Symbols (Android uses Material icons).
/// Mirrors Android `App.kt`.
///
/// ```
///  TabView(selection: model.tab)
///   ├─ Permissions   (R3)
///   ├─ Scanner       (R3)
///   ├─ Media         (R3)
///   ├─ API           ApiView(vm.api)      ← R2: first tab in SwiftUI
///   └─ Monitor       (R3)
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
        case .api: ApiView(vm: model.vm.api)
        default: PendingTabView(tab: tab)
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

/// R2 placeholder — the SwiftUI version of this tab comes in R3.
private struct PendingTabView: View {
    let tab: AppTab

    var body: some View {
        VStack(spacing: AppSpacing.sm) {
            Text(tab.label).font(.system(size: 17, weight: .semibold)).foregroundStyle(AppColor.textPrimary)
            AppHintText(text: "SwiftUI version of this tab comes in R3 (shared ViewModel already done in R1).", showBullet: false)
        }
        .padding(AppSpacing.lg)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppColor.contentBackground)
    }
}
