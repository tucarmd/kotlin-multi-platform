import Foundation
import Shared

/// [App 5 · B6] Localised text helpers. Keys match androidApp/src/main/res/values*/strings.xml.
func L(_ key: String) -> String { NSLocalizedString(key, comment: "") }
func L(_ key: String, _ args: CVarArg...) -> String { String(format: L(key), arguments: args) }

/// [App 5 · B6] Core enums and states → text. Its Android twin is Labels.kt.
/// SKIE turns Kotlin enums into Swift enums and sealed classes into `onEnum(of:)`, so every `switch` is exhaustive.
extension ObdError {
    var label: String {
        switch self {
        case .bluetoothOff: L("error_bluetooth_off")
        case .scanFailed: L("error_scan_failed")
        case .connectFailed: L("error_connect_failed")
        case .setupFailed: L("error_setup_failed")
        case .reconnectFailed: L("error_reconnect_failed")
        }
    }
}

extension CodesFailure {
    var label: String {
        switch self {
        case .noAnswer: L("codes_failed_no_answer")
        case .rejected: L("codes_failed_rejected")
        }
    }
}

extension DtcSystem {
    var label: String {
        switch self {
        case .powertrain: L("system_powertrain")
        case .chassis: L("system_chassis")
        case .body: L("system_body")
        case .network: L("system_network")
        }
    }
}

extension Dongle {
    /// Advertised name, or a placeholder when the dongle advertises none.
    var displayName: String { name.isEmpty ? L("dongle_unnamed") : name }
}

extension ConnectionState {
    /// One line describing the connection, e.g. "Reconnecting 2/3…".
    var label: String {
        switch onEnum(of: self) {
        case .idle: L("state_idle")
        case .scanning: L("state_scanning")
        case .connecting(let s): L("state_connecting", s.dongle.displayName)
        case .initializing(let s): L("state_initializing", s.dongle.displayName)
        case .ready(let s): L("state_ready", s.dongle.displayName)
        case .polling(let s): L("state_polling", s.dongle.displayName)
        // PLATFORM: Kotlin Int is Int32 in Swift; %ld in Localizable.strings expects a 64-bit Int.
        case .reconnecting(let s): L("state_reconnecting", Int(s.attempt), Int(s.maxAttempts))
        case .error(let s): s.reason.label
        }
    }
}

extension ConsoleKind {
    /// Console notes are stored as a kind + the command; the sentence is built here.
    func format(_ text: String) -> String {
        switch self {
        case .sent: "> \(text)"
        case .received: text
        case .noAnswer: L("console_no_answer", text)
        case .invalidInput: L("console_invalid", text)
        case .clearBlocked: L("console_clear_blocked", text)
        case .notConnected: L("console_not_connected", text)
        }
    }
}
