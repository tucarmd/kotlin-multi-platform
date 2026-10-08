import CoreBluetooth

/// Day 1 (BLE Step 1) — Bluetooth RADIO state, separate from permissions.
///
/// Permissions (`PermissionService`) answer "is the app ALLOWED to use Bluetooth". Whether the radio is ON
/// is a different question — permission granted but the user turned Bluetooth off in Control Center → scan still finds 0 devices.
/// Mirrors `android/.../nativecore/bluetooth/BluetoothService.kt` (Android only has ON/OFF/UNSUPPORTED;
/// iOS folds the permission into `CBManagerState` so it also has `.unauthorized`).
enum BluetoothRadioState: Equatable {
    /// `CBCentralManager` not created yet (on purpose — see `start()`), or the stack is starting up.
    case unknown
    /// Device doesn't support BLE. The Simulator does NOT land here — it pretends to be `.poweredOn`.
    case unsupported
    case unauthorized
    case poweredOff
    case poweredOn
}

/// The verdict for Day 2: can Scan be tapped yet, and if not, EXACTLY what's blocking it. Same 4 cases as Android.
enum BleReadiness: Equatable {
    case ready
    case unsupported
    case missingPermission
    case radioOff
}

/// Day 1 (BLE Step 1) — read / observe the Bluetooth radio. Knows nothing about SwiftUI (Rule 5.4).
///
/// ```
///  start() ── permission != .allowedAlways ──► do NOT create the manager (creating it shows the permission dialog!)
///          └─ permission OK ──► CBCentralManager(delegate) ──► centralManagerDidUpdateState
///                                                          └─► radio = .poweredOn / .poweredOff ...
///  The delegate keeps being called every time the user toggles Bluetooth in Control Center → the screen updates itself.
///  (Android has to register a BroadcastReceiver; iOS gets it for free via the delegate.)
/// ```
@MainActor
@Observable
final class BluetoothService {

    static let shared = BluetoothService()

    private(set) var radio: BluetoothRadioState = .unknown

    /// Keep the manager + delegate alive: let them go out of scope and the delegate is never called.
    private var observer: RadioObserver?

    private init() {}

    /// Start observing the radio. Calling it again is harmless (no-op if an observer exists).
    ///
    /// Why check the permission BEFORE creating the manager: creating `CBCentralManager` while permission is still
    /// `.notDetermined` = iOS shows the permission dialog right away → the user opens the screen and gets asked without knowing why.
    /// Requesting permission is the job of the Request button on the Bluetooth card (`PermissionService`), not here.
    func start() {
        guard CBManager.authorization == .allowedAlways else {
            radio = CBManager.authorization == .notDetermined ? .unknown : .unauthorized
            return
        }
        guard observer == nil else { return }
        observer = RadioObserver(showPowerAlert: false) { [weak self] state in
            self?.radio = state
            AppLog.d("Bluetooth", "radio -> \(state)")
        }
    }

    /// Combine permission + radio into one answer. Pure function → readable and testable.
    static func readiness(radio: BluetoothRadioState, permissionGranted: Bool) -> BleReadiness {
        if radio == .unsupported { return .unsupported }
        if !permissionGranted || radio == .unauthorized { return .missingPermission }
        if radio != .poweredOn { return .radioOff }       // .poweredOff / .unknown can't scan yet either
        return .ready
    }

    /// iOS does NOT let apps turn Bluetooth on, nor deep-link to the Bluetooth page in Settings.
    /// The only official route: create a manager with `ShowPowerAlertKey = true` → iOS shows the alert
    /// "Turn On Bluetooth to Allow … to Connect" with a built-in Settings button. Equivalent to
    /// `ACTION_REQUEST_ENABLE` on Android (but Android can turn it on directly, iOS only points the way).
    func showSystemPowerAlert() {
        // Temporary manager: the alert only shows when a manager is CREATED while the radio is off. Kept until the next call.
        powerAlertObserver = RadioObserver(showPowerAlert: true) { _ in }
    }

    private var powerAlertObserver: RadioObserver?
}

/// Objective-C style delegate bridge → closure. Separate from `BluetoothService` so the service doesn't
/// have to inherit from `NSObject` (and doesn't mix `@Observable` with an Obj-C class).
private final class RadioObserver: NSObject, CBCentralManagerDelegate {

    private var manager: CBCentralManager?
    private let onChange: @MainActor (BluetoothRadioState) -> Void

    init(showPowerAlert: Bool, onChange: @escaping @MainActor (BluetoothRadioState) -> Void) {
        self.onChange = onChange
        super.init()
        // queue: nil = the delegate runs on the main queue → safe to change state the UI is reading.
        manager = CBCentralManager(
            delegate: self,
            queue: nil,
            options: [CBCentralManagerOptionShowPowerAlertKey: showPowerAlert]
        )
    }

    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        let state: BluetoothRadioState = switch central.state {
        case .poweredOn: .poweredOn
        case .poweredOff: .poweredOff
        case .unauthorized: .unauthorized
        case .unsupported: .unsupported
        case .unknown, .resetting: .unknown       // transitional, says nothing yet
        @unknown default: .unknown
        }
        // queue nil ⇒ we really are on the main thread; assumeIsolated tells the compiler so.
        MainActor.assumeIsolated { onChange(state) }
    }
}
