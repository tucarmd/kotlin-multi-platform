import SwiftUI
import Shared

func L(_ key: String) -> String { NSLocalizedString(key, comment: "") }
func L(_ key: String, _ arg: String) -> String { String(format: L(key), arg) }

enum Tone {
    case good, warn, bad

    var color: Color {
        switch self {
        case .good: Color(red: 0.18, green: 0.49, blue: 0.20)
        case .warn: Color(red: 0.70, green: 0.42, blue: 0.0)
        case .bad: .red
        }
    }
}

/// A rule result ready to draw: its label and how alarming it is. The rule itself runs in the shared core.
struct Grade {
    let label: String
    let tone: Tone
}

extension BatteryLevel {
    var grade: Grade {
        switch self {
        case .critical: Grade(label: L("grade_critical"), tone: .bad)
        case .low: Grade(label: L("grade_low"), tone: .warn)
        case .ok: Grade(label: L("grade_ok"), tone: .good)
        }
    }
}

extension FixQuality {
    var grade: Grade {
        switch self {
        case .good: Grade(label: L("grade_good"), tone: .good)
        case .fair: Grade(label: L("grade_fair"), tone: .warn)
        case .poor: Grade(label: L("grade_poor"), tone: .bad)
        }
    }
}

extension LatencyGrade {
    var grade: Grade {
        switch self {
        case .fast: Grade(label: L("grade_fast"), tone: .good)
        case .ok: Grade(label: L("grade_ok"), tone: .warn)
        case .slow: Grade(label: L("grade_slow"), tone: .bad)
        }
    }
}

extension ChargingState {
    var label: String {
        switch self {
        case .charging: L("charging_charging")
        case .discharging: L("charging_discharging")
        case .full: L("charging_full")
        case .unknown: L("charging_unknown")
        }
    }
}

extension ConnectionType {
    var label: String {
        switch self {
        case .wifi: L("conn_wifi")
        case .cellular: L("conn_cellular")
        case .ethernet: L("conn_ethernet")
        case .none: L("conn_none")
        case .unknown: L("conn_unknown")
        }
    }
}

extension SensorKind {
    var title: String {
        switch self {
        case .battery: L("sensor_battery")
        case .location: L("sensor_location")
        case .network: L("sensor_network")
        }
    }

    var emoji: String {
        switch self {
        case .battery: "🔋"
        case .location: "📍"
        case .network: "📶"
        }
    }

    var rule: String {
        switch self {
        case .battery: L("rule_battery")
        case .location: L("rule_location")
        case .network: L("rule_network")
        }
    }
}

extension Double {
    /// POSIX locale keeps the decimal point a dot, so coordinates read the same in every language.
    func fixed(_ decimals: Int) -> String {
        String(format: "%.\(decimals)f", locale: Locale(identifier: "en_US_POSIX"), self)
    }
}

extension KotlinInstant {
    var clockText: String {
        let date = Date(timeIntervalSince1970: Double(toEpochMilliseconds()) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss · dd/MM/yyyy"
        return formatter.string(from: date)
    }
}

func latencyText(_ latency: Reading<KotlinLong>?) -> String {
    guard let latency else { return L("not_measured") }
    switch onEnum(of: latency) {
    case .loading: return L("measuring")
    case .available(let ms): return "\(ms.value.int64Value) ms"
    case .error(let e): return L("state_error", e.message)
    case .permissionDenied, .unsupported: return L("not_measured")
    }
}

func isLoading<T>(_ reading: Reading<T>?) -> Bool {
    guard let reading else { return false }
    if case .loading = onEnum(of: reading) { return true }
    return false
}
