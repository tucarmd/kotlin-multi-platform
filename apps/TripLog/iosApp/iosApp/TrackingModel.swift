import Foundation
import Observation
import Shared

/// [App 4 · B6] TrackingModel: bridges the Kotlin TripRecorder to SwiftUI. The iOS twin of RecordViewModel.
///
/// Why @Observable: SwiftUI redraws only the views that read a property that changed.
/// The Kotlin StateFlow is read with `for await` because SKIE turns it into an AsyncSequence.
/// No logic here: every action forwards to TripRecorder, which is tested in shared/commonTest.
///
/// PLATFORM: iOS has no foreground service. Recording continues in the background because IosLocationStream sets
/// allowsBackgroundLocationUpdates and Info.plist declares the `location` background mode.
///
/// Used by: RecordView. Uses: TripRecorder from `Shared`.
@MainActor @Observable
final class TrackingModel {
    private(set) var state: RecordState
    private let recorder: TripRecorder

    init(recorder: TripRecorder) {
        self.recorder = recorder
        self.state = recorder.state.value
        Task { [weak self] in
            for await state in recorder.state { self?.state = state }
        }
    }

    /// The live trip while recording or paused, nil otherwise.
    var trip: LiveTrip? {
        switch onEnum(of: state) {
        case .recording(let recording): recording.trip
        case .paused(let paused): paused.trip
        case .idle, .saving: nil
        }
    }

    func start() { recorder.start() }
    func pause() { recorder.pause() }
    func resume() { recorder.resume() }
    func stop() { recorder.stop() }
}
