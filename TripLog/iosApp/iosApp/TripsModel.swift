import Foundation
import Observation
import Shared

/// [App 4 · B6] TripsModel: the saved trips from the Kotlin TripLibrary. The iOS twin of TripsViewModel.
///
/// Used by: TripsView, TripDetailView. Uses: TripLibrary from `Shared`.
@MainActor @Observable
final class TripsModel {
    private(set) var trips: [Trip] = []
    private let library: TripLibrary

    init(library: TripLibrary) {
        self.library = library
        self.trips = library.trips.value
        Task { [weak self] in
            for await list in library.trips { self?.trips = list }
        }
    }

    func delete(_ trip: Trip) { library.delete(tripId: trip.id) }

    /// Loads the trip and writes its GPX next to it, ready for ShareLink. Nil when the trip is gone.
    /// TRAP: SKIE turns the Kotlin `suspend fun` into `async throws`; TripLibrary.load never throws on purpose,
    /// because a Kotlin exception escaping into Swift would crash the app.
    func load(_ tripId: Int64) async -> LoadedTrip? {
        guard let detail = try? await library.load(tripId: tripId) else { return nil }
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(library.gpxFileName(detail: detail))
        try? library.gpx(detail: detail).write(to: url, atomically: true, encoding: .utf8)
        // PLATFORM: Kotlin List<Double> arrives as [KotlinDouble] (boxed). Convert once here, not in the view.
        let speeds = library.speeds(detail: detail).map { $0.doubleValue }
        return LoadedTrip(detail: detail, speeds: speeds, gpxURL: url)
    }
}

/// Everything TripDetailView needs once loading has finished.
struct LoadedTrip {
    let detail: TripDetail
    let speeds: [Double]
    let gpxURL: URL
}
