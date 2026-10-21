import SwiftUI

/// Entry point — 5 tabs: **Permissions** (Day 1) · **Scanner** (Day 2) · **Media** + **API** (Day 10) · **Monitor** (Week 3). Mirrors `android/.../MainActivity.kt`.
///
/// ```
///  NativeDeviceMonitorApp
///   └─ WindowGroup
///       └─ TabView(selection: $tab)
///           ├─ NavigationStack { PermissionView() }                      tag .permissions
///           ├─ NavigationStack { ScannerView(onOpenPermissions: …) }     tag .scanner
///           ├─ NavigationStack { MediaView() }                           tag .media   (Day 10)
///           ├─ NavigationStack { ApiView() }                             tag .api     (Day 10)
///           └─ NavigationStack { MonitorView() }                         tag .monitor
///   .backgroundTask(.appRefresh(id))  ← iOS wakes the app in the background → BackgroundMonitor.handleAppRefresh()
///   scenePhase == .background         → BackgroundMonitor.scheduleAppRefresh()
/// ```
///
/// Unlike Android: no Activity holding launchers/permission state — services are `@Observable` singletons
/// (`PermissionService.shared`, `BluetoothClientService.shared`), every View reading them sees the same instance.
@main
struct NativeDeviceMonitorApp: App {

    enum Tab: Hashable { case permissions, scanner, media, api, monitor }

    /// Open the Scanner tab directly when launched with `-openScanner` — `simctl`/`devicectl` can't tap tabs,
    /// this flag is for taking acceptance screenshots from the command line.
    @State private var tab: Tab = {
        let args = ProcessInfo.processInfo.arguments
        if args.contains("-openMonitor") || args.contains("-openPomodoro") { return .monitor }
        if args.contains("-openMedia") { return .media }
        if args.contains("-openApi") { return .api }
        return args.contains("-openScanner") ? .scanner : .permissions
    }()

    @Environment(\.scenePhase) private var scenePhase

    /// Week 3: start `NWPathMonitor` as soon as the app opens — the first path arrives tens of ms later; created lazily on
    /// Refresh, the first read was always `wifiConnected = false` (seen on the Simulator).
    init() {
        _ = NetworkProvider.shared
        AppHeader.apply()   // blue header + white text on every screen, before the first View is drawn
    }

    var body: some Scene {
        WindowGroup {
            TabView(selection: $tab) {
                NavigationStack { PermissionView() }
                    .tabItem { Label(AppStrings.tabPermissions, systemImage: "lock.shield") }
                    .tag(Tab.permissions)
                NavigationStack { ScannerView(onOpenPermissions: { tab = .permissions }) }
                    .tabItem { Label(AppStrings.tabScanner, systemImage: "antenna.radiowaves.left.and.right") }
                    .tag(Tab.scanner)
                NavigationStack { MediaView() }
                    .tabItem { Label(AppStrings.tabMedia, systemImage: "camera") }
                    .tag(Tab.media)
                NavigationStack { ApiView() }
                    .tabItem { Label(AppStrings.tabApi, systemImage: "network") }
                    .tag(Tab.api)
                NavigationStack { MonitorView() }
                    .tabItem { Label(AppStrings.tabMonitor, systemImage: "waveform.path.ecg") }
                    .tag(Tab.monitor)
            }
            .tint(AppColor.primary)
        }
        // Week 3 · Day 14: going to the background → ask iOS for one background run (no guaranteed time).
        .onChange(of: scenePhase) { _, phase in
            if phase == .background { BackgroundMonitor.shared.scheduleAppRefresh() }
        }
        // iOS calls this closure when it lets the app run BGAppRefresh (~30 s max). The id must be in
        // Info.plist `BGTaskSchedulerPermittedIdentifiers`, missing it crashes at registration.
        .backgroundTask(.appRefresh(BackgroundMonitor.refreshTaskId)) {
            await BackgroundMonitor.shared.handleAppRefresh()
        }
    }
}
