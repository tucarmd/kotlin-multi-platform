import Foundation

/// Day 1 — ONE permission group = one app FEATURE, not one OS constant.
///
/// This phase only has BLE + Camera (no Notifications yet).
/// Mirrors `android/.../nativecore/permission/PermissionGroup.kt`.
///
/// - Parameters:
///   - id: key for looking up the state in `PermissionService.states`.
///   - kind: which iOS permission is behind this group — `PermissionService` `switch`es on it.
///   - infoPlistKey: the key that must be declared in Info.plist, also shown in the UI. Missing it makes the app **crash
///     the moment it requests the permission** (not a denied result) — a classic iOS trap, so make it visible.
///
/// The text shown in the UI (label / why) isn't here — `UI/Permission/PermissionStrings.swift` looks it up by `id`.
struct PermissionGroup: Identifiable {
    let id: String
    let kind: PermissionKind
    let infoPlistKey: String
}

/// iOS permissions this app touches. Each case = a different framework, a different way to ask.
enum PermissionKind {
    /// `CBManager.authorization` + creating a `CBCentralManager` to trigger the dialog.
    case bluetooth
    /// `AVCaptureDevice.authorizationStatus(for: .video)`
    case camera
}

/// The iOS app's permission table.
///
/// ## Why iOS has 2 groups while Android has 3
///
/// ```
///   Android (3 groups)                  iOS (2 groups)
///   ---------------------------         ------------------------------
///   ble_scan     BLUETOOTH_SCAN   ─┐
///                                  ├──> bluetooth   ONE permission for both scanning and connecting
///   ble_connect  BLUETOOTH_CONNECT ─┘
///   camera       CAMERA           ────> camera
/// ```
///
/// Android 12 split the Bluetooth permission in two; iOS didn't — one `NSBluetoothAlwaysUsageDescription` line,
/// one dialog. And **iOS doesn't involve the Location permission** like Android API ≤ 30.
///
/// A `let` constant, not a function of the OS version like Kotlin: deployment target 17.0, no branches to take.
enum PermissionGroups {
    static let bluetooth = "bluetooth"
    static let camera = "camera"

    static let all: [PermissionGroup] = [
        PermissionGroup(id: bluetooth, kind: .bluetooth, infoPlistKey: "NSBluetoothAlwaysUsageDescription"),
        PermissionGroup(id: camera, kind: .camera, infoPlistKey: "NSCameraUsageDescription"),
    ]
}
