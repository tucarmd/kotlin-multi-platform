import SwiftUI
import Shared

/// [App 4 · B6] One trip: map, numbers, speed chart and GPX export. Its Android twin is TripDetailScreen.kt.
///
/// Why ShareLink with a file URL: the share sheet then offers "Save to Files", AirDrop, Mail… with a real
/// `trip-7.gpx` attachment. Android does the same with FileProvider + ACTION_SEND.
struct TripDetailView: View {
    let model: TripsModel
    let tripId: Int64

    private enum Phase { case loading, missing, loaded(LoadedTrip) }
    @State private var phase: Phase = .loading

    var body: some View {
        content
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if case .loaded(let loaded) = phase {
                    ShareLink(item: loaded.gpxURL) {
                        Label(L("share_gpx"), systemImage: "square.and.arrow.up")
                    }
                }
            }
            .task(id: tripId) {
                phase = await model.load(tripId).map(Phase.loaded) ?? .missing
            }
    }

    private var title: String {
        if case .loaded(let loaded) = phase { dateText(loaded.detail.trip.startedAtMillis) } else { "" }
    }

    @ViewBuilder
    private var content: some View {
        switch phase {
        case .loading:
            ProgressView(L("loading"))
        case .missing:
            ContentUnavailableView(L("trip_missing"), systemImage: "map")
        case .loaded(let loaded):
            let trip = loaded.detail.trip
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    RouteMap(points: loaded.detail.points).frame(height: 300)
                    HStack(spacing: 12) {
                        StatTile(label: L("distance"), value: distanceText(trip.distanceMeters))
                        StatTile(label: L("duration"), value: durationText(trip.durationMillis))
                    }
                    HStack(spacing: 12) {
                        StatTile(label: L("speed_avg"), value: speedText(trip.averageSpeedMps))
                        StatTile(label: L("speed_max"), value: speedText(trip.maxSpeedMps))
                    }
                    if loaded.speeds.contains(where: { $0 > 0 }) {
                        Text(L("speed_chart")).font(.headline)
                        SpeedChart(speeds: loaded.speeds)
                    }
                    Text(L("points_count", "\(trip.pointCount)")).font(.caption).foregroundStyle(.secondary)
                }
                .padding()
            }
        }
    }
}
