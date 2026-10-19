import AVFoundation
import CoreBluetooth
import UIKit
import Shared

/// R5a — the Apple-SDK half of permissions, in Swift. Implements the Kotlin `IosPermissionNative` protocol; the state
/// (4 statuses, last result, radio flow) lives in Kotlin `IosPermissionController`. Ported from the old app's
/// `PermissionService.swift` + `BluetoothService.swift`.
///
/// ```
///  read     CBManager.authorization (STATIC — reading it does NOT show the dialog)
///           AVCaptureDevice.authorizationStatus(for: .video)
///  request  Bluetooth: create a CBCentralManager → iOS shows the dialog → wait for a definite state
///           Camera:    AVCaptureDevice.requestAccess
///  radio    CBCentralManager delegate → .poweredOn / .poweredOff … every time the user toggles Control Center
/// ```
final class PermissionNative: NSObject, IosPermissionNative {

    /// Keep the probe / observers alive: a released CBCentralManager never calls its delegate.
    private var probe: CentralStateObserver?
    private var radio: CentralStateObserver?
    private var powerAlert: CentralStateObserver?

    func bluetoothAuthorization() -> NativeAuthorization {
        let auth: NativeAuthorization = switch CBManager.authorization {
        case .notDetermined: .notDetermined
        case .allowedAlways: .granted
        case .denied: .denied
        case .restricted: .restricted
        @unknown default: .denied      // a new Apple case → treat as not granted, never assume granted
        }
        return auth
    }

    func cameraAuthorization() -> NativeAuthorization {
        let auth: NativeAuthorization = switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .notDetermined: .notDetermined
        case .authorized: .granted
        case .denied: .denied
        case .restricted: .restricted
        @unknown default: .denied
        }
        return auth
    }

    /// Bluetooth has no `requestAccess`: the only trigger is creating a `CBCentralManager`.
    func requestBluetooth(done: @escaping () -> Void) {
        guard CBManager.authorization == .notDetermined else { done(); return }   // already answered → nothing to show
        var answered = false
        probe = CentralStateObserver(showPowerAlert: false) { [weak self] state in
            // .unknown / .resetting say nothing yet — wait for the user's answer. The delegate keeps firing on every
            // radio toggle → answer only once.
            guard state != .unknown, state != .resetting, !answered else { return }
            answered = true
            done()
            // Don't free the CBCentralManager inside its own delegate callback — let it go on the next main-loop turn.
            DispatchQueue.main.async { self?.probe = nil }
        }
    }

    func requestCamera(done: @escaping (KotlinBoolean) -> Void) {
        AVCaptureDevice.requestAccess(for: .video) { granted in
            // The completion runs on an arbitrary queue — Kotlin state is written on main.
            DispatchQueue.main.async { done(KotlinBoolean(bool: granted)) }
        }
    }

    /// `app-settings:` — iOS only allows opening the app's own page, not a specific switch.
    func openSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }

    func startRadio(onChange: @escaping (BluetoothRadioState) -> Void) {
        guard radio == nil else { return }
        radio = CentralStateObserver(showPowerAlert: false) { state in
            let mapped: BluetoothRadioState = switch state {
            case .poweredOn: .on
            case .unsupported: .unsupported
            default: .off          // poweredOff · unauthorized · unknown · resetting → can't scan
            }
            onChange(mapped)
        }
    }

    /// The alert only shows when a manager is CREATED while the radio is off — so make a new one each time.
    func showPowerAlert() {
        powerAlert = CentralStateObserver(showPowerAlert: true) { _ in }
    }
}

/// Objective-C delegate → closure. `queue: nil` = callbacks on the main queue.
final class CentralStateObserver: NSObject, CBCentralManagerDelegate {
    private var manager: CBCentralManager?
    private let onChange: (CBManagerState) -> Void

    init(showPowerAlert: Bool, onChange: @escaping (CBManagerState) -> Void) {
        self.onChange = onChange
        super.init()
        manager = CBCentralManager(delegate: self, queue: nil,
                                   options: [CBCentralManagerOptionShowPowerAlertKey: showPowerAlert])
    }

    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        onChange(central.state)
    }
}
