import SwiftUI
import Shared

/// [App 4 · B4] App entry: starts Koin once, then builds the models the screens need.
/// Android does the same in TripLogApplication.kt.
///
/// Why the models live here and not in a view: TrackingModel must exist as long as the app does, so a trip keeps
/// recording while the user browses old trips or the app is in the background.
@main
struct iOSApp: App {
    @State private var tracking: TrackingModel
    @State private var trips: TripsModel
    @State private var permission = LocationPermission()

    init() {
        doInitKoin()                                                   // Koin.kt: initKoin(), same modules as Android
        _tracking = State(initialValue: TrackingModel(recorder: tripRecorder()))  // KoinIos.kt: tripRecorder()
        _trips = State(initialValue: TripsModel(library: tripLibrary()))          // KoinIos.kt: tripLibrary()
    }

    var body: some Scene {
        WindowGroup {
            NavigationStack {
                RecordView(model: tracking, trips: trips, permission: permission)
            }
        }
    }
}
