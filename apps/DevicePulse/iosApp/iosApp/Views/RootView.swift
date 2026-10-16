import SwiftUI
import Shared

enum Route: Hashable {
    case detail(SensorKind)
    case history
}

struct RootView: View {
    let model: PulseModel
    let permission: LocationPermission
    @State private var path: [Route] = []

    var body: some View {
        NavigationStack(path: $path) {
            DashboardView(model: model, onGrantLocation: permission.request, open: { path.append($0) })
                .navigationDestination(for: Route.self) { route in
                    switch route {
                    case .detail(let sensor):
                        DetailView(sensor: sensor, model: model, onGrantLocation: permission.request)
                    case .history:
                        HistoryView(model: model)
                    }
                }
        }
        .tint(Color(red: 0.24, green: 0.35, blue: 1.0))
        .task { if permission.canPrompt { permission.request() } }
        // Same as Android: after a grant, make the location flow re-subscribe.
        .onChange(of: permission.granted) { _, granted in
            if granted { model.refreshLocation() }
        }
    }
}
