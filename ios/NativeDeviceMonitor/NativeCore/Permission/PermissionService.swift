import AVFoundation
import CoreBluetooth
import UIKit

/// Day 1 — read / request runtime permissions on iOS. Knows NOTHING about SwiftUI.
///
/// Rule 5: `NativeCore/` does not import `UI/`. Only Apple frameworks are used here, so the Scanner screen
/// (Day 2) reuses exactly this service, nobody rewrites the state logic.
///
/// ## Why the iOS version does NOT need `UserDefaults` while the Android one needs `SharedPreferences`
///
/// This is the most valuable difference of Day 1, not a matter of syntax.
///
/// Android can only answer 2 questions, and those 2 are **not enough** to tell "never asked" from
/// "denied twice" — both give `(DENIED, rationale = false)`. So `PermissionService.kt`
/// has to record its own "I've asked for this permission" flag in SharedPreferences.
///
/// iOS answers directly with **one** API:
///
/// ```
///   CBManager.authorization                      -> .notDetermined | .allowedAlways | .denied | .restricted
///   AVCaptureDevice.authorizationStatus(for:)    -> .notDetermined | .authorized | .denied | .restricted
/// ```
///
/// `.notDetermined` is a state the system keeps itself → the app doesn't have to remember anything. One less state
/// file, and one less class of bug — "flag out of sync with the system" (e.g. the user clears app data on Android).
///
/// In exchange iOS loses something else: **you may ask exactly once**. See `PermissionStatus.swift`.
///
/// ## Why an `@Observable` class and not an `enum` of static functions
///
/// Permission state is data that **changes over time** (the user turns it off in Settings), and the screen
/// must redraw when it changes. `@Observable` (iOS 17+) lets SwiftUI track which properties are read
/// in `body` — equivalent to `StateFlow` in Kotlin, without having to declare `collectAsState`.
///
/// `@MainActor`: every property here is read by a View's `body`, so pin it to the main actor to
/// avoid sprinkling `DispatchQueue.main.async` — like Compose requiring state changes on the main thread.
@MainActor
@Observable
final class PermissionService {

    /// One shared instance: permissions are state of the **whole app**, not of one screen.
    /// If each screen kept its own copy, screen B would still show the old state after screen A requested.
    static let shared = PermissionService()

    /// group id (`PermissionGroup.id`) -> state. Keyed by string id rather than array index:
    /// adding/removing groups doesn't shift other groups' states.
    private(set) var states: [String: PermissionGroupState] = [:]

    /// Raw result of the latest action — shown in the UI in a monospaced font to read by eye.
    private(set) var lastResult: String = ""

    /// Keeps the probe alive while waiting for the system to answer.
    ///
    /// **Must be kept**: if the `CBCentralManager` is released midway the delegate is never
    /// called, and the permission dialog may disappear. A common trap when writing
    /// `let m = CBCentralManager(...)` inside a function and letting it go out of scope.
    private var bluetoothProbe: BluetoothPermissionProbe?

    private init() {}

    // MARK: - Reading state

    /// Re-read the state of both groups. Called every time the app returns to the foreground.
    ///
    /// Why re-read instead of caching: the user may leave the app → go to Settings and turn the permission off → come back.
    /// The app gets no event for that; the only way is to read again on return.
    func refreshAll() async {
        var next: [String: PermissionGroupState] = [:]
        for group in PermissionGroups.all {
            next[group.id] = await state(of: group)
        }
        states = next
    }

    private func state(of group: PermissionGroup) async -> PermissionGroupState {
        let status: PermissionStatus
        switch group.kind {
        case .bluetooth: status = Self.bluetoothStatus()
        case .camera: status = Self.cameraStatus()
        }
        return PermissionGroupState.from([group.id: status])
    }

    /// `CBManager.authorization` is a **static** property — readable WITHOUT creating a
    /// `CBCentralManager`, so reading the state doesn't trigger the dialog. Important: reading it via
    /// an instance (`centralManager.authorization`, the deprecated one) means merely creating the manager
    /// already shows the dialog — the app asks as soon as the screen opens, the user has no idea why.
    private static func bluetoothStatus() -> PermissionStatus {
        switch CBManager.authorization {
        case .notDetermined: return .notDetermined
        case .allowedAlways: return .granted
        case .denied: return .denied
        case .restricted: return .restricted
        @unknown default: return .denied    // Apple adds a new case -> treat as not granted, never blindly assume granted
        }
    }

    private static func cameraStatus() -> PermissionStatus {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .notDetermined: return .notDetermined
        case .authorized: return .granted
        case .denied: return .denied
        case .restricted: return .restricted
        @unknown default: return .denied
        }
    }

    // MARK: - Requesting

    /// Request one group's permission then re-read the state.
    ///
    /// There's no "if already denied, stop" branch: calling it anyway is harmless (iOS returns the old result,
    /// shows nothing). The blocking happens in the UI — the Request button is replaced by Open Settings, see
    /// `PermissionStatus.canRequest`.
    func request(_ group: PermissionGroup) async {
        switch group.kind {
        case .bluetooth: await requestBluetooth()
        case .camera: _ = await AVCaptureDevice.requestAccess(for: .video)
        }
        await refreshAll()
        let status = states[group.id]?.status
        lastResult = "\(group.id) -> \(String(describing: status ?? .notDetermined))"
        AppLog.d("Permission", "request \(group.id) -> \(String(describing: status))")
    }

    /// Request both groups, one after another.
    ///
    /// **Must be sequential (`for` + `await`), never parallel**: iOS queues permission dialogs,
    /// fire 2 at once and the user only sees the first, the second is silently swallowed.
    /// On Android `requestPermissions` takes an array so they can be combined — here the 2 platforms differ.
    func requestAll() async {
        for group in PermissionGroups.all {
            await request(group)
        }
        lastResult = "Requested all \(PermissionGroups.all.count) groups"
    }

    /// Bluetooth has **no** `requestAccess` function like Camera.
    ///
    /// The only way to trigger the dialog is to **start using CoreBluetooth**: create a `CBCentralManager`.
    /// iOS only asks the first time the app touches BLE. So here we create a probe, wait for the delegate to report
    /// a state (meaning the user has answered), then throw it away.
    ///
    /// The probe could be dropped and `BluetoothClientService` (Day 2) left to trigger the dialog — the probe is kept so
    /// the Permissions screen can request without creating the scanning service.
    private func requestBluetooth() async {
        guard CBManager.authorization == .notDetermined else { return }  // already answered → creating a manager is pointless
        let probe = BluetoothPermissionProbe()
        bluetoothProbe = probe           // keep it alive while waiting
        await probe.start()
        bluetoothProbe = nil
    }

    // MARK: - Settings

    /// Open the Settings page **of this app** — the only way out of `.denied`.
    ///
    /// `UIApplication.openSettingsURLString` is a system-provided URL (`app-settings:`), not a
    /// hand-made string. Mirrors `ACTION_APPLICATION_DETAILS_SETTINGS` on Android, but iOS **doesn't
    /// allow** deep-linking further to the exact switch row — only to the app's page.
    func openAppSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
        AppLog.d("Permission", "openAppSettings")
    }

    // Android also has a `resetAskedFlags()` function to clear the SharedPreferences flags after running
    // `adb shell pm reset-permissions`. iOS has NO equivalent because there's no flag to clear
    // (see the doc at the top of the file). To test from scratch: delete and reinstall the app, or
    // Settings > General > Transfer or Reset iPhone > Reset > Reset Location & Privacy.
}

/// A sacrificial object that triggers the Bluetooth permission dialog, then waits for the system to answer.
///
/// Must be an `NSObject` + delegate because `CBCentralManagerDelegate` is an Objective-C style API with no
/// async/await version yet. `withCheckedContinuation` is the bridge: it turns "call me back later" into `await`
/// — equivalent to `suspendCancellableCoroutine` in Kotlin.
private final class BluetoothPermissionProbe: NSObject, CBCentralManagerDelegate {

    private var manager: CBCentralManager?
    private var continuation: CheckedContinuation<Void, Never>?

    /// Create the manager (→ iOS shows the dialog) and wait until the delegate reports a definite state.
    func start() async {
        await withCheckedContinuation { (cont: CheckedContinuation<Void, Never>) in
            continuation = cont
            // queue: nil = callbacks on the main queue, matching PermissionService's @MainActor.
            // Once created the dialog appears immediately if the permission is .notDetermined.
            manager = CBCentralManager(delegate: self, queue: nil)
        }
        manager = nil
    }

    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        // `.unknown` is the initial state, says nothing yet — skip, wait for the next call.
        // `.resetting` too: the Bluetooth stack is restarting.
        guard central.state != .unknown, central.state != .resetting else { return }
        // Resume only ONCE: the delegate keeps being called when the user toggles Bluetooth,
        // resuming a second time crashes ("continuation misuse").
        continuation?.resume()
        continuation = nil
        AppLog.d("Permission", "bluetooth probe state = \(central.state.rawValue)")
    }
}
