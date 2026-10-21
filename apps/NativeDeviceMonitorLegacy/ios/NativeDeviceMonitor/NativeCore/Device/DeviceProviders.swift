import Foundation
import Network
import UIKit

/// Week 3 · Day 12 — 3 providers + the collector. Mirrors `android/.../nativecore/device/DeviceProviders.kt`.
///
/// ```
///  DeviceStatusCollector.collect()
///    ├─ BatteryProvider.percent()       → UIDevice.batteryLevel (0…1, Simulator = -1 → nil)
///    ├─ NetworkProvider.shared.snapshot → NWPathMonitor (runs continuously on its own queue, keeps the latest path)
///    ├─ BluetoothProvider.snapshot()    → BluetoothService.radio + BluetoothClientService.connection
///    └─ DeviceStatus(timestamp ISO 8601, …)
/// ```
///
/// Unlike Android: the network can NOT be read once — iOS only lets you listen to `NWPathMonitor`, so `NetworkProvider` is
/// a singleton that starts the monitor on creation and remembers the last path. The very first read right after creation may have no path yet.
enum BatteryProvider {

    /// `isBatteryMonitoringEnabled` must be turned on first, otherwise it's always `-1`. Call on main (UIKit).
    @MainActor
    static func percent() -> Int? {
        UIDevice.current.isBatteryMonitoringEnabled = true
        let level = UIDevice.current.batteryLevel
        return level < 0 ? nil : Int((level * 100).rounded())
    }
}

struct NetworkSnapshot: Equatable, Sendable {
    /// iOS has no public API to read the Wi-Fi switch → always `nil` (field kept for the same format as Android).
    var wifiEnabled: Bool? = nil
    var wifiConnected = false
    var cellularAvailable = false
}

/// `@unchecked Sendable` + `NSLock`: `pathUpdateHandler` runs on its own queue, `snapshot` is read from main/other Tasks.
final class NetworkProvider: @unchecked Sendable {

    static let shared = NetworkProvider()

    private let monitor = NWPathMonitor()
    private let lock = NSLock()
    private var latest = NetworkSnapshot()

    private init() {
        monitor.pathUpdateHandler = { [weak self] path in
            // `.satisfied` = there's a route to the network. `usesInterfaceType` = that route goes over Wi-Fi / cellular.
            // Like Android `activeNetwork`: Wi-Fi + 4G both present → the path prefers Wi-Fi → cellular = false.
            let ok = path.status == .satisfied
            let snap = NetworkSnapshot(
                wifiConnected: ok && path.usesInterfaceType(.wifi),
                cellularAvailable: ok && path.usesInterfaceType(.cellular)
            )
            self?.lock.withLock { self?.latest = snap }
        }
        monitor.start(queue: DispatchQueue(label: "NetworkProvider"))
    }

    var snapshot: NetworkSnapshot { lock.withLock { latest } }
}

struct BluetoothSnapshot: Equatable, Sendable {
    var enabled: Bool?
    var connected: Bool?
}

enum BluetoothProvider {

    /// Read from the app's 2 existing services — iOS has no "who is the system connected to over GATT" API like Android's
    /// `getConnectedDevices(GATT)` for third-party apps (`retrieveConnectedPeripherals` needs the service UUIDs up front).
    @MainActor
    static func snapshot() -> BluetoothSnapshot {
        let enabled: Bool? = switch BluetoothService.shared.radio {
        case .poweredOn: true
        case .poweredOff: false
        case .unknown, .unauthorized, .unsupported: nil     // no permission / manager not created → unknown
        }
        let connected: Bool = if case .connected = BluetoothClientService.shared.connection { true } else { false }
        return BluetoothSnapshot(enabled: enabled, connected: enabled == false ? false : connected)
    }
}

enum DeviceStatusCollector {

    @MainActor
    static func collect(source: String? = nil) -> DeviceStatus {
        let network = NetworkProvider.shared.snapshot
        let bluetooth = BluetoothProvider.snapshot()
        return DeviceStatus(
            timestamp: DeviceStatus.timestamp(),
            battery: BatteryProvider.percent(),
            wifiEnabled: network.wifiEnabled,
            wifiConnected: network.wifiConnected,
            cellularAvailable: network.cellularAvailable,
            bluetoothEnabled: bluetooth.enabled,
            bluetoothConnected: bluetooth.connected,
            source: source
        )
    }
}
