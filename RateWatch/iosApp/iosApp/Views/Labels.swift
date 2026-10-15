import Foundation
import Shared

/// [App 3 · B6] Localised text helpers. Keys match androidApp/src/main/res/values*/strings.xml.
func L(_ key: String) -> String { NSLocalizedString(key, comment: "") }
func L(_ key: String, _ args: CVarArg...) -> String { String(format: L(key), arguments: args) }

/// [App 3 · B6] Core enums → messages. Their Android twin is Labels.kt.
/// SKIE turns each Kotlin enum into a Swift enum, so `switch` must cover every case.
extension RefreshError {
    var label: String {
        switch self {
        case .offline: L("error_offline")
        case .timeout: L("error_timeout")
        case .serverError: L("error_server")
        case .badResponse: L("error_bad_response")
        case .unknownBase: L("error_unknown_base")
        case .unknown: L("error_unknown")
        }
    }
}

extension AmountError {
    var label: String {
        switch self {
        case .notNumeric: L("amount_not_numeric")
        case .tooLarge: L("amount_too_large")
        }
    }
}

/// Age(2, hours) → "Updated 2 h ago"; nil (nothing saved) → "Not downloaded yet".
func updatedText(_ age: Age?) -> String {
    guard let age else { return L("updated_never") }
    // PLATFORM: Kotlin Long arrives as Int64; String(describing:) avoids a %lld vs %@ format mismatch.
    let amount = String(describing: age.amount)
    switch age.unit {
    case .justNow: return L("updated_just_now")
    case .minutes: return L("updated_minutes", amount)
    case .hours: return L("updated_hours", amount)
    case .days: return L("updated_days", amount)
    }
}
