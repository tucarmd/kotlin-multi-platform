import CoreLocation
import Observation
import UIKit

/// [App 4 · B6] Location permission status for the Record screen banner. Same idea as DevicePulse.
///
/// Asking is UI work, so it lives in the app, not the shared core. IosLocationStream also asks on its own when the
/// status is still "not determined", so tapping Start on a fresh install shows the system prompt either way.
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

    /// True after "Don't Allow" (or a parental restriction): only Settings can change it now.
    var denied: Bool { status == .denied || status == .restricted }

    /// PLATFORM: iOS shows the prompt only once; after "Don't Allow" the answer can only change in Settings.
    func openSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        Task { @MainActor in self.status = status }
    }
}
