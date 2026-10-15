import SwiftUI
import Shared

/// [App 4 · B6] Saved trips, newest first. Swipe left to delete. Its Android twin is TripsScreen.kt.
struct TripsView: View {
    let model: TripsModel

    var body: some View {
        Group {
            if model.trips.isEmpty {
                ContentUnavailableView(L("no_trips"), systemImage: "map")
            } else {
                List {
                    ForEach(model.trips, id: \.id) { trip in
                        NavigationLink {
                            TripDetailView(model: model, tripId: trip.id)
                        } label: {
                            row(trip)
                        }
                    }
                    // TRAP: delete by the Trip object, not by index after the fact: the list may already have
                    // changed (a new trip saved) between the swipe and the Kotlin flow emitting.
                    .onDelete { offsets in offsets.map { model.trips[$0] }.forEach(model.delete) }
                }
            }
        }
        .navigationTitle(L("trips"))
    }

    private func row(_ trip: Trip) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(dateText(trip.startedAtMillis)).font(.headline)
                Text(L("points_count", "\(trip.pointCount)")).font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text(distanceText(trip.distanceMeters)).font(.headline)
                Text(durationText(trip.durationMillis)).font(.caption).monospacedDigit()
            }
        }
    }
}
