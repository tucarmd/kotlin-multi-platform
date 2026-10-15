import Foundation
import Shared

/// [App 2 · B6] Localised text helpers. Keys match androidApp/src/main/res/values*/strings.xml.
func L(_ key: String) -> String { NSLocalizedString(key, comment: "") }
func L(_ key: String, _ args: CVarArg...) -> String { String(format: L(key), arguments: args) }

/// [App 2 · B6] Core value → text. Its Android twin is Labels.kt.
/// SKIE's `onEnum(of:)` turns the Kotlin sealed class into a Swift enum, so `switch` must cover every case.
extension RelativeTime {
    var label: String {
        switch onEnum(of: self) {
        case .justNow: L("just_now")
        // PLATFORM: Kotlin Int arrives as Int32, which is what %d expects.
        case .minutesAgo(let t): L("minutes_ago", t.minutes)
        case .hoursAgo(let t): L("hours_ago", t.hours)
        case .yesterday: L("yesterday")
        case .daysAgo(let t): L("days_ago", t.days)
        // Day / month order lives in the string itself: en "%2$d/%3$d/%1$d", vi "%3$d/%2$d/%1$d".
        case .onDate(let d): L("on_date", d.year, d.month, d.day)
        }
    }
}
