import SwiftUI
import Shared

/// [App 4 · B6] The Record screen. Draws `model.state` and fires actions; computes nothing itself.
/// Its Android twin is RecordScreen.kt, with the same sections in the same order.
struct RecordView: View {
    let model: TrackingModel
    let trips: TripsModel
    let permission: LocationPermission

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                RouteMap(points: model.trip?.points ?? [], followUser: true)
                    .frame(height: 260)
                if permission.denied {
                    banner(L("permission_location_denied")) {
                        Button(L("open_settings")) { permission.openSettings() }
                    }
                }
                if let problem = model.trip?.problem {
                    banner(problem.label) { EmptyView() }
                }
                if let trip = model.trip {
                    liveStats(trip)
                } else if case .idle = onEnum(of: model.state) {
                    Text(L("idle_hint")).font(.body).foregroundStyle(.secondary)
                }
                controls
            }
            .padding()
        }
        .navigationTitle(L("app_name"))
        .toolbar {
            NavigationLink {
                TripsView(model: trips)
            } label: {
                Label(L("trips"), systemImage: "list.bullet")
            }
        }
    }

    @ViewBuilder
    private func liveStats(_ trip: LiveTrip) -> some View {
        Text(durationText(trip.elapsedMillis))
            .font(.system(size: 56, weight: .bold, design: .rounded))
            .monospacedDigit()
        if case .paused = onEnum(of: model.state) {
            Text(L("paused")).foregroundStyle(.orange)
        }
        // PLATFORM: Kotlin Double? arrives as KotlinDouble? (boxed); unwrap with .doubleValue.
        let speedNow = trip.currentSpeedMps.map { speedText($0.doubleValue) } ?? "–"
        let accuracy = trip.lastAccuracyMeters.flatMap { $0.doubleValue < 10_000 ? $0.doubleValue : nil }
            .map { L("accuracy_value", formatFixed(value: $0, decimals: 0)) } ?? "–"
        HStack(spacing: 12) {
            StatTile(label: L("distance"), value: distanceText(trip.distanceMeters))
            StatTile(label: L("speed_now"), value: speedNow)
        }
        HStack(spacing: 12) {
            StatTile(label: L("speed_avg"), value: speedText(trip.averageSpeedMps))
            StatTile(label: L("accuracy"), value: accuracy)
        }
        if trip.droppedFixes > 0 {
            Text(L("dropped_fixes", "\(trip.droppedFixes)")).font(.footnote).foregroundStyle(.secondary)
        }
    }

    /// One button row per state. SKIE's `onEnum(of:)` makes RecordState a Swift enum, so this `switch`
    /// stops compiling if a case is added in Kotlin later.
    @ViewBuilder
    private var controls: some View {
        HStack(spacing: 12) {
            switch onEnum(of: model.state) {
            case .idle:
                Button(L("start")) { model.start() }
                    .buttonStyle(.borderedProminent)
            case .recording:
                Button(L("pause")) { model.pause() }
                    .buttonStyle(.bordered)
                Button(L("stop"), role: .destructive) { model.stop() }
                    .buttonStyle(.borderedProminent)
            case .paused:
                Button(L("resume")) { model.resume() }
                    .buttonStyle(.borderedProminent)
                Button(L("stop"), role: .destructive) { model.stop() }
                    .buttonStyle(.borderedProminent)
            case .saving:
                ProgressView()
                Text(L("saving"))
            }
        }
        .controlSize(.large)
        .frame(maxWidth: .infinity)
    }

    private func banner<Action: View>(_ text: String, @ViewBuilder action: () -> Action) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(text)
            action()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(.red.opacity(0.12), in: RoundedRectangle(cornerRadius: 12))
    }
}
