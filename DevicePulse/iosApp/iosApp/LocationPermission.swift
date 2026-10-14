import CoreLocation
import Observation
import UIKit

/// Asking for permission is UI work, so it lives in the app, not the shared core.
/// The core's IosLocationSensor sees the new authorization on its own CLLocationManager.
@MainActor @Observable
final class LocationPermission: NSObject, CLLocationManagerDelegate {
    private let manager: CLLocationManager
    private(set) var status: CLAuthorizationStatus

    override init() {
        let manager = CLLocationManager()
        self.manager = manager
        self.status = manager.authorizationStatus
        super.init()
        manager.delegate = self
    }

    var granted: Bool { status == .authorizedWhenInUse || status == .authorizedAlways }

    /// iOS shows the prompt only once; after "Don't Allow" the answer can only change in Settings.
    var canPrompt: Bool { status == .notDetermined }

    func request() {
        if canPrompt {
            manager.requestWhenInUseAuthorization()
        } else if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        Task { @MainActor in self.status = status }
    }
}
