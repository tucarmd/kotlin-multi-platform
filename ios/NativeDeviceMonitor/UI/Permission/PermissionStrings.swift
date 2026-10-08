import Foundation

/// Day 1 — Permissions screen strings (Rule 4: UI in English · Rule 5.3: `enum` + `static let`).
/// Mirrors `android/.../ui/permission/PermissionStrings.kt`. No hard-coded strings in the View.
enum PermissionStrings {
    static let title = "Permissions"
    static let hint = "Tap Request to trigger the system dialog. Deny it once, then tap again — "
        + "on iOS the dialog never comes back, so the button must turn into Open Settings."
    static func lastResultFormat(_ result: String) -> String { "Last result: \(result)" }

    // Permission groups — looked up by PermissionGroup.id
    static let groupBluetooth = "Bluetooth"
    static let groupBluetoothWhy = "Scan for and connect to BLE devices (Scanner tab)."
    static let groupCamera = "Camera"
    static let groupCameraWhy = "Practice only: a second permission to test granted / denied."

    static let statusGranted = "GRANTED"
    static let statusNotRequested = "NOT REQUESTED"
    static let statusDenied = "DENIED"
    static let statusRestricted = "RESTRICTED"
    static let requestButton = "Request permission"
    static let openSettingsButton = "Open app settings"
    static let deniedHint = "Denied. iOS will not ask again — turn it on in Settings."
    static let restrictedHint = "Restricted by Screen Time or a device management profile. "
        + "This cannot be changed in app settings."

    static let debugTitle = "Debug"
    static let debugHintQueue = "Request all asks the dialogs one after another — iOS queues them, so answer each one."
    static let debugHintReset = "To test from scratch again: delete the app and reinstall, or Settings > General > "
        + "Transfer or Reset iPhone > Reset > Reset Location & Privacy."
    static let requestAllButton = "Request all"

    // Card "Bluetooth radio"
    static let radioTitle = "Bluetooth radio"
    static let radioHint = "Permission says the app MAY use Bluetooth. The radio must also be ON "
        + "before the Scanner can scan. Toggle Bluetooth in Control Center — this card updates live."
    static let radioOn = "RADIO ON"
    static let radioOff = "RADIO OFF"
    static let radioUnknown = "RADIO UNKNOWN"
    static let radioUnauthorized = "NO PERMISSION"
    static let radioUnsupported = "NO BLUETOOTH"
    static let readyYes = "Ready to scan: YES"
    static let readyNoPermission = "Ready to scan: NO — grant Bluetooth above first"
    static let readyRadioOff = "Ready to scan: NO — Bluetooth is off"
    static let readyUnsupported = "Ready to scan: NO — this device has no Bluetooth"
    static let showAlertButton = "Ask iOS to turn on Bluetooth"
    static let simulatorNote = "Simulator has no real Bluetooth radio — this card is not a real result here. "
        + "Test ON/OFF on a real iPhone."

    /// Unknown id → show the id itself: add a group and forget its label and you see it on screen right away.
    static func label(_ groupId: String) -> String {
        switch groupId {
        case PermissionGroups.bluetooth: groupBluetooth
        case PermissionGroups.camera: groupCamera
        default: groupId
        }
    }

    static func why(_ groupId: String) -> String {
        switch groupId {
        case PermissionGroups.bluetooth: groupBluetoothWhy
        case PermissionGroups.camera: groupCameraWhy
        default: ""
        }
    }
}
