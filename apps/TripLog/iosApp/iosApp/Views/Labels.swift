import Foundation
import Shared

/// [App 4 · B6] Localised text helpers. Keys match androidApp/src/main/res/values*/strings.xml.
func L(_ key: String) -> String { NSLocalizedString(key, comment: "") }
func L(_ key: String, _ arg: String) -> String { String(format: L(key), arg) }

/// Shared formatters, so both platforms print the same digits. Kotlin Long arrives as Int64.
func distanceText(_ meters: Double) -> String { formatDistance(meters: meters) }
func durationText(_ millis: Int64) -> String { formatDuration(millis: millis) }
func speedText(_ metersPerSecond: Double) -> String { L("kmh", formatSpeedKmh(metersPerSecond: metersPerSecond)) }

/// Epoch millis → "15 Oct 2026 at 08:30" in the phone's language.
func dateText(_ millis: Int64) -> String {
    Date(timeIntervalSince1970: Double(millis) / 1000).formatted(date: .abbreviated, time: .shortened)
}

/// [App 4 · B6] Core enum → message. Its Android twin is Labels.kt.
/// SKIE turns the Kotlin enum into a Swift enum, so `switch` must cover every case.
extension LocationProblem {
    var label: String {
        switch self {
        case .permissionDenied: L("problem_permission")
        case .locationOff: L("problem_location_off")
        }
    }
}
