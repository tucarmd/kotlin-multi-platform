import CoreBluetooth
import Foundation
import Shared

/// R5d — thin CoreBluetooth wrapper in Swift. Implements Kotlin `IosBleNative`: runs commands, reports raw events to
/// Kotlin `IosBleClient` (which keeps the list, timers, read-vs-notify and cleanup rules). Ported from the old app's
/// `BluetoothClientService.swift` — only the CoreBluetooth-specific part stays here.
///
/// ```
///  prepare()      CBCentralManager(queue: nil) — only once Bluetooth is granted (earlier = permission dialog)
///  startScan()    scanForPeripherals(nil, AllowDuplicates) ─► didDiscover ─► events.onDiscovered(id, name, rssi, adv…)
///  connect(id)    peripherals[id] (STRONG ref — trap 1) ─► didConnect ─► discoverServices ─► discoverCharacteristics
///                 ─► events.onServices(…, done) — 2 levels on iOS, 1 call on Android
///  read / notify  ─► didUpdateValueFor (same callback for both) ─► events.onValue
///  write          .withResponse ─► didWriteValueFor ─► events.onWritten · .withoutResponse: no callback
/// ```
/// `queue: nil` ⇒ every delegate call arrives on the main queue, which is where Kotlin expects its events.
final class BleNative: NSObject, IosBleNative {

    private var events: IosBleEvents?
    private var central: CBCentralManager?
    /// CoreBluetooth only holds peripherals weakly — let go and the connection is cancelled SILENTLY (trap 1).
    private var peripherals: [String: CBPeripheral] = [:]
    private var connected: CBPeripheral?
    /// Services still waiting for their characteristics — `done` once all have answered.
    private var pendingServices = 0

    func setEvents(events: IosBleEvents) { self.events = events }

    func prepare() {
        guard central == nil, CBManager.authorization == .allowedAlways else { return }
        central = CBCentralManager(delegate: self, queue: nil)
    }

    func startScan() {
        guard let central, central.state == .poweredOn else { return }
        // Keep only the connected peripheral (dropping it would kill the connection — trap 1)
        peripherals = connected.map { [$0.identifier.uuidString: $0] } ?? [:]
        // nil services = see everything; AllowDuplicates = every packet → live RSSI + packet count (manual 10 s only)
        central.scanForPeripherals(withServices: nil, options: [CBCentralManagerScanOptionAllowDuplicatesKey: true])
    }

    func stopScan() { central?.stopScan() }

    func connect(id: String) -> Bool {
        guard let central, let peripheral = peripherals[id] else { return false }
        connected = peripheral                          // strong ref BEFORE connecting
        pendingServices = 0
        central.connect(peripheral)
        return true
    }

    func cancelConnection(id: String) {
        guard let peripheral = peripherals[id] ?? (connected?.identifier.uuidString == id ? connected : nil) else { return }
        central?.cancelPeripheralConnection(peripheral)
    }

    func read(serviceUuid: String, uuid: String) {
        guard let ch = characteristic(serviceUuid, uuid) else { return }
        connected?.readValue(for: ch)
    }

    func setNotify(serviceUuid: String, uuid: String, enabled: Bool) {
        guard let ch = characteristic(serviceUuid, uuid) else { return }
        connected?.setNotifyValue(enabled, for: ch)      // iOS writes the CCCD 2902 itself
    }

    /// Writing .withResponse to a WRITE-NO-RESP-only characteristic (or vice versa) is DROPPED silently by iOS —
    /// the shared `planWrite` already picks the type from the properties.
    func write(serviceUuid: String, uuid: String, data: Data, withResponse: Bool) {
        guard let ch = characteristic(serviceUuid, uuid) else { return }
        connected?.writeValue(data, for: ch, type: withResponse ? .withResponse : .withoutResponse)
    }

    /// iOS negotiates the MTU itself; `.withoutResponse` = MTU − 3 (`.withResponse` says 512 because iOS splits long writes).
    func maxWriteWithoutResponse() -> Int32 {
        Int32(connected?.maximumWriteValueLength(for: .withoutResponse) ?? 0)
    }

    // MARK: - helpers

    /// Looked up each time: CBCharacteristic objects die with the connection.
    private func characteristic(_ serviceUuid: String, _ uuid: String) -> CBCharacteristic? {
        connected?.services?.first { $0.uuid.uuidString == serviceUuid }?
            .characteristics?.first { $0.uuid.uuidString == uuid }
    }

    private func snapshot(_ peripheral: CBPeripheral) -> [GattServiceInfo] {
        (peripheral.services ?? []).map { s in
            GattServiceInfo(uuid: s.uuid.uuidString, characteristics: (s.characteristics ?? []).map { ch in
                GattCharacteristicInfo(serviceUuid: s.uuid.uuidString, uuid: ch.uuid.uuidString,
                                       properties: GattProperties.companion.of(raw: Int32(ch.properties.rawValue)),
                                       isNotifying: ch.isNotifying)
            })
        }
    }

    private func id(_ p: CBPeripheral) -> String { p.identifier.uuidString }
}

// MARK: - CBCentralManagerDelegate

extension BleNative: CBCentralManagerDelegate {

    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if central.state != .poweredOn {
            // iOS sends no didDisconnect when the radio goes off → the old peripherals are dead, Kotlin cleans its state.
            peripherals.removeAll()
            connected = nil
        }
        events?.onPoweredOn(on: central.state == .poweredOn)
    }

    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral,
                        advertisementData adv: [String: Any], rssi RSSI: NSNumber) {
        let key = id(peripheral)
        peripherals[key] = peripheral
        let serviceData = adv[CBAdvertisementDataServiceDataKey] as? [CBUUID: Data] ?? [:]
        let unknown = IosBleEventsCompanion.shared.UNKNOWN
        events?.onDiscovered(
            id: key,
            // peripheral.name may be nil while the packet still carries a name → try both
            name: peripheral.name ?? adv[CBAdvertisementDataLocalNameKey] as? String,
            rssi: RSSI.int32Value,
            connectable: (adv[CBAdvertisementDataIsConnectable] as? NSNumber).map { $0.boolValue ? 1 : 0 } ?? unknown,
            txPower: (adv[CBAdvertisementDataTxPowerLevelKey] as? NSNumber)?.int32Value ?? unknown,
            serviceUuids: (adv[CBAdvertisementDataServiceUUIDsKey] as? [CBUUID] ?? []).map(\.uuidString),
            manufacturerData: adv[CBAdvertisementDataManufacturerDataKey] as? Data,
            serviceDataUuids: serviceData.keys.map(\.uuidString),
            serviceDataValues: serviceData.keys.map { serviceData[$0]! }
        )
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        events?.onConnected(id: id(peripheral), name: peripheral.name ?? id(peripheral))
        guard peripheral === connected else { return }
        // Delegate BEFORE asking — otherwise the discovery result arrives with nobody listening.
        peripheral.delegate = self
        peripheral.discoverServices(nil)
    }

    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        if peripheral === connected { connected = nil }
        events?.onFailedToConnect(id: id(peripheral), message: error?.localizedDescription ?? "Failed to connect")
    }

    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        if peripheral === connected { connected = nil }
        // CBError 6 = connection timed out (out of range) · 7 = the peripheral disconnected on purpose
        events?.onDisconnected(id: id(peripheral), message: error?.localizedDescription)
    }
}

// MARK: - CBPeripheralDelegate

extension BleNative: CBPeripheralDelegate {

    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        if let error {
            events?.onGattError(id: id(peripheral), message: "Service discovery failed: \(error.localizedDescription)")
            return
        }
        let services = peripheral.services ?? []
        pendingServices = services.count
        if services.isEmpty { events?.onServices(id: id(peripheral), services: [], done: true); return }
        // Level 2: iOS asks "which characteristics" per service
        services.forEach { peripheral.discoverCharacteristics(nil, for: $0) }
    }

    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        if let error {
            events?.onGattError(id: id(peripheral),
                                message: "Characteristics of \(service.uuid.uuidString) failed: \(error.localizedDescription)")
        }
        pendingServices -= 1
        // Draw each service as it arrives — devices with many services show the tree growing right away
        events?.onServices(id: id(peripheral), services: snapshot(peripheral), done: pendingServices <= 0)
    }

    /// The peripheral changed its GATT table (firmware update…) → discover again.
    func peripheral(_ peripheral: CBPeripheral, didModifyServices invalidatedServices: [CBService]) {
        peripheral.discoverServices(nil)
    }

    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        events?.onValue(id: id(peripheral), serviceUuid: characteristic.service?.uuid.uuidString ?? "",
                        uuid: characteristic.uuid.uuidString, data: characteristic.value,
                        error: error?.localizedDescription)
    }

    func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: Error?) {
        events?.onNotifyState(id: id(peripheral), serviceUuid: characteristic.service?.uuid.uuidString ?? "",
                              uuid: characteristic.uuid.uuidString, isNotifying: characteristic.isNotifying,
                              error: error?.localizedDescription)
    }

    func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?) {
        events?.onWritten(id: id(peripheral), serviceUuid: characteristic.service?.uuid.uuidString ?? "",
                          uuid: characteristic.uuid.uuidString, error: error?.localizedDescription)
    }
}
