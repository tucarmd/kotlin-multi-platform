import Foundation
import Shared

/// [App 1 · B6] Localised text helpers. Keys match androidApp/src/main/res/values*/strings.xml.
func L(_ key: String) -> String { NSLocalizedString(key, comment: "") }
func L(_ key: String, _ arg: String) -> String { String(format: L(key), arg) }

/// Kotlin Long (Swift Int64) → "37.000 đ", using the shared formatter so both platforms print the same thing.
func money(_ amount: Int64) -> String { formatMoney(amount: amount) + " đ" }

/// [App 1 · B6] Core enum → message. Its Android twin is Labels.kt.
/// SKIE turns the Kotlin enum into a Swift enum, so `switch` must cover every case.
extension InputError {
    var label: String {
        switch self {
        case .notNumeric: L("error_not_a_number")
        case .notPositive: L("error_not_positive")
        case .tooLarge: L("error_too_large")
        case .noPeople: L("error_no_people")
        case .tooManyPeople: L("error_too_many_people")
        }
    }
}
