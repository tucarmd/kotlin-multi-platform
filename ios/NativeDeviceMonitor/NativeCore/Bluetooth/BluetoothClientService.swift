import CoreBluetooth
import Foundation

/// Day 2 — real BLE scan + connect/disconnect on iOS. Day 3 — discover + read + decode. Day 4 — notify + cleanup on disconnect.
/// Task 004 — write characteristic + read the MTU.
/// `NativeCore/` layer: does NOT import SwiftUI.
///
/// No auto-reconnect yet. See docs/README-HOW-TO-IMPLEMENT.md §5–§6, §8.4 and docs/tasks/003-gatt-read-notify.md, 004-write-mtu.md.
/// Mirrors `android/.../nativecore/bluetooth/BluetoothClientService.kt`.
///
/// ```
///  start()  ── create CBCentralManager (once, when the Scanner tab opens)
///
///  startScan(filter)
///    │  central.scanForPeripherals(withServices: nil, AllowDuplicates: true)
///    ▼
///  didDiscover (main queue)
///    │  filter.matches(name)? no → hiddenCount++
///    │  found[peripheral.identifier] = BluetoothDeviceInfo(...)   <- Dictionary = dedupe
///    │  peripherals[identifier] = peripheral                      <- KEEP A STRONG REF, trap (1)
///    ▼
///  devices: [BluetoothDeviceInfo]  ──@Observable──►  ScannerView
///    └── after scanTimeout (10 s) stopScan() by itself
///
///  connect(device)
///    │  stopScan() → central.connect(peripheral) + schedule connectTimeout 15 s   <- trap (2)
///    ▼
///  didConnect        → .connected(name)
///  didFailToConnect  → .error(...)
///  didDisconnect     → .disconnected (manual) | .error(...) (dropped)
///
///  centralManagerDidUpdateState(!= .poweredOn)
///    └─ iOS does NOT call didDisconnect when the radio goes off → clean the list ourselves, .error("Bluetooth turned off")
///
///  ── Day 3 ───────────────────────────────────────────────────────────────────────────
///  didConnect → peripheral.delegate = bridge → discoverServices(nil)
///    didDiscoverServices → discoverCharacteristics(nil, for: each service)     <- iOS: 2 levels, Android: 1 call
///    didDiscoverCharacteristicsFor (all N services) → services: [GattServiceInfo]
///  read(chr) → pendingReads.insert → readValue(for:) → didUpdateValueFor → lastRead + received
///  ── Day 4 ───────────────────────────────────────────────────────────────────────────
///  setNotify(on) → setNotifyValue(true) (iOS writes CCCD 2902 ITSELF) → didUpdateNotificationStateFor → isNotifying
///  didUpdateValueFor (not in pendingReads) → GattValue(.notify) → received (max 50)
///  disconnect() → setNotifyValue(false) on everything enabled → cancelPeripheralConnection → clearGattState()
///  signal lost / radio off ─────────────────────────────────────────► clearGattState() + .error (red)
///  ── Task 004 ────────────────────────────────────────────────────────────────────────
///  didConnect / discovery done → mtu = maximumWriteValueLength(.withoutResponse) + 3   <- iOS negotiates itself, no API to request
///  write(chr, bytes, withResponse: true)  → writeValue(.withResponse)    → didWriteValueFor → GattValue(.write) → received
///  write(chr, bytes, withResponse: false) → writeValue(.withoutResponse) → (NO callback) → GattValue(.write) right away
/// ```
///
/// Unlike Android on Day 3–4: CoreBluetooth **queues** GATT operations itself (no `gattOps` needed) and **writes the CCCD itself**;
/// but read and notify arrive on **the same callback** `didUpdateValueFor` → we must remember what we just asked for.
///
/// ## 3 differences from Android, all of them traps
/// 1. **You must keep a strong reference to `CBPeripheral`.** CoreBluetooth only holds it weakly — let go and the connection
///    is cancelled **silently**, the UI is stuck on `connecting`. (Android: `connectGatt` returns a `BluetoothGatt` you hold yourself.)
/// 2. **`connect` has no timeout.** Android gives up after ~30 s; iOS waits forever → set your own timer.
/// 3. **You can only scan when `state == .poweredOn`.** Call earlier: a purple "API MISUSE" warning, 0 results.
///
/// `@MainActor` + `queue: nil` ⇒ every delegate call arrives on the main queue ⇒ assigning state is safe, no
/// thread hopping like Android (`onScanResult` there runs on a Binder thread).
@MainActor
@Observable
final class BluetoothClientService {

    static let shared = BluetoothClientService()

    /// 10 s — matches the Android constant.
    private static let scanTimeout: TimeInterval = 10
    /// iOS doesn't give up on connect → set our own. 15 s is enough for slow peripherals without making the user wait long.
    private static let connectTimeout: TimeInterval = 15

    // MARK: - State pushed to the UI

    private(set) var devices: [BluetoothDeviceInfo] = []
    private(set) var isScanning = false
    private(set) var connection: BluetoothConnectionState = .idle
    /// id of the device being / just connected — the View uses it to tag the right row CONNECTED.
    private(set) var connectedId: UUID?
    /// Raw status line for the learner to read by eye.
    private(set) var lastEvent = ""
    /// Devices that advertised but were dropped by the filter — the View shows "N other devices hidden".
    private(set) var hiddenCount = 0

    // MARK: - State Day 3 + Day 4 (GATT)

    /// Service → characteristic tree of the connected device. Empty until discovery finishes / after disconnect.
    private(set) var services: [GattServiceInfo] = []
    private(set) var isDiscovering = false
    /// Most recent Read (Day 3 exercise: raw bytes + decoded). Kept after disconnect so it can still be read.
    private(set) var lastRead: GattValue?
    /// The *Received Data* box: every read + notify value, oldest on top, at most `receivedLogMax` lines.
    private(set) var received: [GattValue] = []
    /// Read / subscribe / discover errors (don't drop the connection) — the View shows them in red in the GATT block.
    private(set) var gattError: String?
    /// Task 004 — MTU of the current connection. 23 until the real value can be read; disconnect → back to 23.
    private(set) var mtu = attDefaultMtu

    // MARK: - Internals

    private var central: CBCentralManager?
    private var delegateBridge: CentralDelegate?

    /// Dictionary = dedupe. `didDiscover` fires **for every advertising packet** — appending directly gives hundreds of duplicate rows.
    private var found: [UUID: BluetoothDeviceInfo] = [:]
    /// First-seen order — a Dictionary has no order; like LinkedHashMap on Android.
    private var order: [UUID] = []
    private var scanFilter = BluetoothScanFilter.none
    private var hidden: Set<UUID> = []
    /// id → (packet count, time of first packet) to estimate the interval like nRF
    private var packetStats: [UUID: (count: Int, first: Date)] = [:]
    /// **Must** be kept separately: see trap (1).
    private var peripherals: [UUID: CBPeripheral] = [:]
    private var connectedPeripheral: CBPeripheral?

    private var scanTimeoutTask: Task<Void, Never>?
    private var connectTimeoutTask: Task<Void, Never>?
    /// Tells "we tapped Disconnect" apart from "signal lost" — both arrive on the same callback.
    private var userRequestedDisconnect = false

    private var peripheralBridge: PeripheralDelegate?
    /// Services still waiting for `didDiscoverCharacteristicsFor` — the "Discovering…" spinner stops once all arrive.
    private var pendingServiceCount = 0
    /// Key `service/char` we just called `readValue` on — to tell read from notify in `didUpdateValueFor`.
    private var pendingReads: Set<String> = []
    /// Key → bytes being written *with response*. `didWriteValueFor` doesn't return the bytes → keep them to print in *Received Data*.
    private var pendingWrites: [String: Data] = [:]

    private init() {}

    // MARK: - Startup

    /// Create the `CBCentralManager` (once). Called when the Scanner tab opens.
    /// Not created in `init`: creating the manager while permission is still `.notDetermined` makes iOS show the permission dialog immediately.
    func start() {
        guard central == nil else { return }
        let bridge = CentralDelegate(owner: self)
        delegateBridge = bridge
        central = CBCentralManager(delegate: bridge, queue: nil)
    }

    // MARK: - Scan

    func startScan(filter: BluetoothScanFilter = .none) {
        guard let central else {
            lastEvent = "Bluetooth is not initialised"
            return
        }
        // Trap (3): scanning before .poweredOn doesn't throw, it just silently returns nothing.
        guard central.state == .poweredOn else {
            lastEvent = "Bluetooth is not powered on (state=\(central.state.rawValue))"
            return
        }
        guard !isScanning else { return }

        found.removeAll()
        order.removeAll()
        // Keep the connected peripheral — removing it drops the strong ref, the connection dies silently (trap 1)
        peripherals = connectedPeripheral.map { [$0.identifier: $0] } ?? [:]
        devices = []
        scanFilter = filter           // CoreBluetooth has no name filter → filter in didDiscover
        hidden.removeAll()
        hiddenCount = 0
        packetStats.removeAll()
        isScanning = true
        lastEvent = filter.isOff ? "Scanning…" : "Scanning (only \(filter.label))…"

        // withServices: nil = see EVERYTHING (filtering by service UUID is the only way to scan in the background).
        // AllowDuplicates = true → receive EVERY packet so RSSI stays live + packets/interval are counted like nRF. Costs more battery —
        // only on during the manual 10 s scan.
        central.scanForPeripherals(withServices: nil,
                                   options: [CBCentralManagerScanOptionAllowDuplicatesKey: true])

        scanTimeoutTask?.cancel()
        scanTimeoutTask = Task { [weak self] in
            try? await Task.sleep(for: .seconds(Self.scanTimeout))
            guard !Task.isCancelled else { return }
            self?.stopScan()
        }
    }

    func stopScan() {
        scanTimeoutTask?.cancel()
        scanTimeoutTask = nil
        guard isScanning else { return }
        isScanning = false
        central?.stopScan()
        lastEvent = "Scan finished: \(found.count) devices"
    }

    // MARK: - Connect / disconnect

    func connect(_ device: BluetoothDeviceInfo) {
        guard let central, let peripheral = peripherals[device.id] else {
            connection = .error(message: "Device is gone — scan again")
            return
        }
        stopScan()                          // scanning in parallel makes connecting slow and flaky
        // Connected to device A and CONNECT tapped on row B: Day 2 keeps only 1 connection → cancel A first.
        // A's didDisconnect arrives AFTER B is already "connecting" → filtered in handleDisconnected by identifier.
        if let old = connectedPeripheral, old.identifier != peripheral.identifier {
            AppLog.d("BleClient", "switch device: drop \(old.identifier)")
            central.cancelPeripheralConnection(old)
        }
        clearGattState()
        lastRead = nil                      // new device → new log
        received = []
        userRequestedDisconnect = false
        connectedPeripheral = peripheral    // keep a strong ref BEFORE connecting — trap (1)
        connectedId = peripheral.identifier
        connection = .connecting
        lastEvent = "Connecting to \(device.name ?? device.id.uuidString)…"
        central.connect(peripheral)

        // Trap (2): iOS waits forever → give up ourselves
        connectTimeoutTask?.cancel()
        connectTimeoutTask = Task { [weak self] in
            try? await Task.sleep(for: .seconds(Self.connectTimeout))
            // Check the id too: if the user moved on to another device, the old device's timer must not cancel the new one
            guard !Task.isCancelled, let self, case .connecting = self.connection,
                  self.connectedId == peripheral.identifier else { return }
            self.central?.cancelPeripheralConnection(peripheral)
            self.connection = .error(message: "Connect timed out after \(Int(Self.connectTimeout))s")
        }
    }

    func disconnect() {
        guard let central, let peripheral = connectedPeripheral else { return }
        // Day 4 — clean up subscriptions BEFORE disconnecting. CoreBluetooth queues the CCCD = 0 write before the disconnect;
        // if the link drops before it's sent that's fine: an unbonded peripheral resets CCCD on disconnect.
        let subscribed = services.flatMap(\.characteristics).filter(\.isNotifying)
        for info in subscribed {
            if let ch = cbCharacteristic(info) { peripheral.setNotifyValue(false, for: ch) }
        }
        pendingReads.removeAll()
        // Set the flag FIRST: the callback may arrive immediately, set it after and it reads the old flag and misreports "unexpected".
        userRequestedDisconnect = true
        lastEvent = subscribed.isEmpty ? "Disconnecting…" : "Unsubscribed \(subscribed.count) · disconnecting…"
        AppLog.d("BleClient", "disconnect unsubscribed=\(subscribed.map(\.uuid))")
        central.cancelPeripheralConnection(peripheral)
    }

    // MARK: - Day 3: read

    /// Read once. The result arrives in `handleValue`. No queue needed like Android — CoreBluetooth queues itself.
    func read(_ info: GattCharacteristicInfo) {
        guard let peripheral = connectedPeripheral, let ch = cbCharacteristic(info) else {
            gattError = "Not connected"
            return
        }
        gattError = nil
        pendingReads.insert(info.key)
        peripheral.readValue(for: ch)
    }

    // MARK: - Day 4: notify

    /// Turn notify on / off. iOS merges Android's 2 steps (enable locally + write CCCD `2902`) into 1 call.
    /// A characteristic without a CCCD → the error comes back in `didUpdateNotificationStateFor`.
    func setNotify(_ info: GattCharacteristicInfo, _ enabled: Bool) {
        guard let peripheral = connectedPeripheral, let ch = cbCharacteristic(info) else {
            gattError = "Not connected"
            return
        }
        gattError = nil
        peripheral.setNotifyValue(enabled, for: ch)
    }

    func clearReceived() {
        received = []
    }

    // MARK: - Task 004: write + MTU

    /// Write `bytes`. The View already checked the rules via `planWrite` (writable? valid hex? ≤ MTU − 3?) before calling.
    /// No queue needed like Android — CoreBluetooth queues itself.
    ///
    /// **iOS trap:** writing `.withResponse` to a characteristic that only has WRITE NO RESP (or vice versa) → CoreBluetooth
    /// prints a warning and **drops the write**, no callback. `planWrite` picks the right type from the properties so we avoid it.
    func write(_ info: GattCharacteristicInfo, _ bytes: Data, withResponse: Bool) {
        guard let peripheral = connectedPeripheral, let ch = cbCharacteristic(info) else {
            gattError = "Not connected"
            return
        }
        gattError = nil
        if withResponse {
            pendingWrites[info.key] = bytes
            peripheral.writeValue(bytes, for: ch, type: .withResponse)       // result arrives in didWriteValueFor
        } else {
            // No ACK → no callback. Sending many packets back to back requires waiting for `canSendWriteWithoutResponse`
            // (peripheralIsReady(toSendWriteWithoutResponse:)) — the app sends 1 hand-typed command at a time so not needed yet.
            peripheral.writeValue(bytes, for: ch, type: .withoutResponse)
            logWrite(ch, bytes)
        }
    }

    private func logWrite(_ ch: CBCharacteristic, _ bytes: Data) {
        let uuid = ch.uuid.uuidString
        let value = GattValue(serviceUuid: ch.service?.uuid.uuidString ?? "", uuid: uuid,
                              hex: BluetoothDecoder.toHex(bytes), decoded: BluetoothDecoder.decode(uuid, bytes),
                              source: .write, at: Date())
        AppLog.d("BleClient", "WRITE \(uuid) raw=[\(value.hex)] decoded=\(value.decoded)")
        received = appendCapped(received, value)
        lastEvent = "Wrote \(bytes.count) bytes to \(uuid)"
    }

    /// iOS negotiates the MTU **automatically** on connect (iPhones usually get 185 or more — depends on the model and what the peripheral supports).
    /// There's no API to request it like Android's `requestMtu` → only read it back: `maximumWriteValueLength(.withoutResponse)` = MTU − 3.
    /// (`.withResponse` returns 512 — iOS splits "long writes" itself — so it can't be used to infer the MTU.)
    private func refreshMtu(_ peripheral: CBPeripheral) {
        let newMtu = peripheral.maximumWriteValueLength(for: .withoutResponse) + attHeaderBytes
        guard newMtu != mtu else { return }
        mtu = newMtu
        AppLog.d("BleClient", "mtu=\(newMtu)")
    }

    /// Snapshot → real object. Looked up each time (a few dozen elements) instead of keeping a map: `CBCharacteristic` dies with the connection.
    private func cbCharacteristic(_ info: GattCharacteristicInfo) -> CBCharacteristic? {
        connectedPeripheral?.services?
            .first { $0.uuid.uuidString == info.serviceUuid }?
            .characteristics?
            .first { $0.uuid.uuidString == info.uuid }
    }

    private func keyOf(_ ch: CBCharacteristic) -> String {
        "\(ch.service?.uuid.uuidString ?? "")/\(ch.uuid.uuidString)"
    }

    /// Day 4 — clear everything that belongs to the old connection. Keep `lastRead` + `received` so the user can still read them after disconnect.
    private func clearGattState() {
        services = []
        isDiscovering = false
        pendingServiceCount = 0
        pendingReads.removeAll()
        pendingWrites.removeAll()
        gattError = nil
        mtu = attDefaultMtu            // MTU belongs to one connection — the next connection negotiates again
    }

    // MARK: - Delegates call back in here

    fileprivate func handleStateChange(_ state: CBManagerState) {
        guard state != .poweredOn else { return }
        // iOS does **not** call didDisconnectPeripheral when the radio goes off — the old CBPeripheral becomes useless.
        // Without cleaning up ourselves the UI is stuck on "Scanning…" / "Connected".
        let hadSomething = isScanning || connectedPeripheral != nil
        scanTimeoutTask?.cancel()
        connectTimeoutTask?.cancel()
        isScanning = false
        found.removeAll()
        order.removeAll()
        hidden.removeAll()
        hiddenCount = 0
        peripherals.removeAll()
        devices = []
        connectedPeripheral = nil
        clearGattState()
        if hadSomething {
            connection = .error(message: "Bluetooth turned off")
            lastEvent = "Bluetooth turned off — list cleared"
        }
    }

    fileprivate func handleDiscovered(_ peripheral: CBPeripheral, advertisement: [String: Any], rssi: NSNumber) {
        let id = peripheral.identifier
        // peripheral.name may be nil while the advertising packet still has a name → try both sources
        let name = peripheral.name ?? advertisement[CBAdvertisementDataLocalNameKey] as? String
        guard scanFilter.matches(name) else {
            if hidden.insert(id).inserted { hiddenCount = hidden.count }
            return
        }
        found[id] = BluetoothDeviceInfo(id: id, name: name, rssi: rssi.intValue,
                                        advertisement: advertisementOf(id, advertisement))
        peripherals[id] = peripheral
        if !order.contains(id) { order.append(id) }
        // Keep FIRST-SEEN ORDER, don't sort by RSSI: each row has its own CONNECT button, sorting by signal makes
        // rows jump around several times a second → your finger hits the wrong device. (nRF doesn't sort by default either.)
        devices = order.compactMap { found[$0] }
    }

    /// Advertising dictionary → snapshot like the nRF Scanner device card.
    private func advertisementOf(_ id: UUID, _ adv: [String: Any]) -> BluetoothAdvertisement {
        let now = Date()
        let old = packetStats[id] ?? (0, now)
        let count = old.count + 1
        packetStats[id] = (count, old.first)
        let interval = count > 1 ? Int(now.timeIntervalSince(old.first) * 1000) / (count - 1) : nil

        var mfr: [String] = []
        var appleType: UInt8?
        if let data = adv[CBAdvertisementDataManufacturerDataKey] as? Data, data.count >= 2 {
            // company id is little-endian: byte 0 is the LOW byte (0x4C 0x00 = 0x004C Apple)
            let company = UInt16(data[data.startIndex]) | UInt16(data[data.startIndex + 1]) << 8
            // Unlike Android: iOS Data STILL has the 2 company id bytes in front → the type byte sits at index 2
            if company == BluetoothLookup.appleCompanyId, data.count >= 3 { appleType = data[data.startIndex + 2] }
            let companyName = BluetoothLookup.companyName(company).map { " \($0)" } ?? ""
            mfr.append(String(format: "0x%04X", company) + companyName + ": " + BluetoothDecoder.toHex(data.dropFirst(2)))
        }
        let serviceDataDict = adv[CBAdvertisementDataServiceDataKey] as? [CBUUID: Data] ?? [:]
        let serviceData = serviceDataDict
            .map { "\($0.key.uuidString): \(BluetoothDecoder.toHex($0.value))" }
            .sorted()
        return BluetoothAdvertisement(
            isConnectable: (adv[CBAdvertisementDataIsConnectable] as? NSNumber)?.boolValue,
            txPower: (adv[CBAdvertisementDataTxPowerLevelKey] as? NSNumber)?.intValue,
            serviceUuids: (adv[CBAdvertisementDataServiceUUIDsKey] as? [CBUUID] ?? []).map(\.uuidString),
            manufacturerData: mfr,
            serviceData: serviceData,
            packetCount: count,
            intervalMs: interval,
            serviceDataUuids: serviceDataDict.keys.map(\.uuidString).sorted(),
            appleType: appleType
        )
    }

    /// Whether the callback belongs to the device we're tracking. No → it's the old device just replaced (see `connect`), ignore.
    private func isCurrent(_ peripheral: CBPeripheral) -> Bool {
        peripheral.identifier == connectedPeripheral?.identifier
    }

    fileprivate func handleConnected(_ peripheral: CBPeripheral) {
        guard isCurrent(peripheral) else {
            central?.cancelPeripheralConnection(peripheral)   // the old device connected before being cancelled → cancel again
            return
        }
        connectTimeoutTask?.cancel()
        let name = peripheral.name ?? peripheral.identifier.uuidString
        connection = .connected(name: name)
        lastEvent = "Connected to \(name)"
        refreshMtu(peripheral)
        startDiscovery(peripheral)
    }

    // MARK: - Day 3: discover (delegate callbacks)

    private func startDiscovery(_ peripheral: CBPeripheral) {
        if peripheralBridge == nil { peripheralBridge = PeripheralDelegate(owner: self) }
        // Set the delegate BEFORE asking — without a delegate the discovery result arrives… with nobody listening.
        peripheral.delegate = peripheralBridge
        isDiscovering = true
        gattError = nil
        lastEvent = "Discovering services…"
        peripheral.discoverServices(nil)    // nil = all. A real app should pass the [CBUUID] it needs, faster + saves battery
    }

    fileprivate func handleServicesDiscovered(_ peripheral: CBPeripheral, _ error: Error?) {
        guard isCurrent(peripheral) else { return }
        if let error {
            isDiscovering = false
            gattError = "Service discovery failed: \(error.localizedDescription)"
            return
        }
        let found = peripheral.services ?? []
        pendingServiceCount = found.count
        if found.isEmpty {
            isDiscovering = false
            services = []
            return
        }
        // Level 2: iOS splits "which services" and "which characteristics per service" into 2 rounds
        for service in found { peripheral.discoverCharacteristics(nil, for: service) }
    }

    fileprivate func handleCharacteristicsDiscovered(_ peripheral: CBPeripheral, _ service: CBService, _ error: Error?) {
        guard isCurrent(peripheral) else { return }
        if let error { gattError = "Characteristics of \(service.uuid.uuidString) failed: \(error.localizedDescription)" }
        pendingServiceCount -= 1
        // Draw each service as it arrives, don't wait for all — devices with many services show the list growing right away
        services = (peripheral.services ?? []).map { s in
            GattServiceInfo(uuid: s.uuid.uuidString,
                            characteristics: (s.characteristics ?? []).map { ch in
                                GattCharacteristicInfo(serviceUuid: s.uuid.uuidString, uuid: ch.uuid.uuidString,
                                                       properties: .of(Int(ch.properties.rawValue)),
                                                       isNotifying: ch.isNotifying)
                            })
        }
        guard pendingServiceCount <= 0 else { return }
        isDiscovering = false
        let count = services.reduce(0) { $0 + $1.characteristics.count }
        // Re-read the MTU: at didConnect iOS may not have finished negotiating (still 23), by now it usually has the real value
        refreshMtu(peripheral)
        lastEvent = "Discovered \(services.count) services, \(count) characteristics · MTU \(mtu)"
        for s in services {
            AppLog.d("BleClient", "service \(s.uuid): " + s.characteristics
                .map { "\($0.uuid)[\($0.properties.labels.joined(separator: "|"))]" }.joined(separator: ", "))
        }
    }

    /// The peripheral changed its GATT table (e.g. firmware update) → ask again from scratch.
    fileprivate func handleServicesModified(_ peripheral: CBPeripheral) {
        guard isCurrent(peripheral) else { return }
        startDiscovery(peripheral)
    }

    // MARK: - Day 3 + Day 4: value / notify state (delegate callbacks)

    /// Read and notify arrive on **this same** callback. In `pendingReads` = we just asked → READ, otherwise NOTIFY.
    fileprivate func handleValue(_ peripheral: CBPeripheral, _ ch: CBCharacteristic, _ error: Error?) {
        guard isCurrent(peripheral) else { return }
        let key = keyOf(ch)
        let wasRead = pendingReads.remove(key) != nil
        if let error {
            // Read rejected: "Reading is not permitted" · pairing required: "Authentication is insufficient"
            gattError = "\(wasRead ? "Read" : "Notify") \(ch.uuid.uuidString) failed: \(error.localizedDescription)"
            return
        }
        let data = ch.value ?? Data()
        let uuid = ch.uuid.uuidString
        let value = GattValue(serviceUuid: ch.service?.uuid.uuidString ?? "", uuid: uuid,
                              hex: BluetoothDecoder.toHex(data), decoded: BluetoothDecoder.decode(uuid, data),
                              source: wasRead ? .read : .notify, at: Date())
        AppLog.d("BleClient", "\(value.source.rawValue) \(uuid) raw=[\(value.hex)] decoded=\(value.decoded)")   // "print raw bytes + decoded"
        if wasRead {
            lastRead = value
            lastEvent = "Read \(uuid): \(value.decoded)"
        }
        // Data → event → UI: @Observable sees `received` change → the View draws the new line
        received = appendCapped(received, value)
    }

    fileprivate func handleNotificationState(_ peripheral: CBPeripheral, _ ch: CBCharacteristic, _ error: Error?) {
        guard isCurrent(peripheral) else { return }
        if let error {
            gattError = "Subscribe \(ch.uuid.uuidString) failed: \(error.localizedDescription)"
        } else {
            lastEvent = "Notify \(ch.isNotifying ? "ON" : "OFF"): \(ch.uuid.uuidString)"
        }
        // Always take the REAL isNotifying from CoreBluetooth, don't guess from what we requested
        let key = keyOf(ch)
        services = services.map { s in
            var s = s
            s.characteristics = s.characteristics.map { c in
                var c = c
                if c.key == key { c.isNotifying = ch.isNotifying }
                return c
            }
            return s
        }
    }

    /// Only arrives for `.withResponse` writes: error nil = the peripheral ACKed.
    fileprivate func handleWritten(_ peripheral: CBPeripheral, _ ch: CBCharacteristic, _ error: Error?) {
        guard isCurrent(peripheral) else { return }
        guard let bytes = pendingWrites.removeValue(forKey: keyOf(ch)) else { return }
        if let error {
            // "Writing is not permitted" · "The value's length is invalid" (too long) · pairing required: "Authentication is insufficient"
            gattError = "Write \(ch.uuid.uuidString) failed: \(error.localizedDescription)"
            return
        }
        logWrite(ch, bytes)
    }

    fileprivate func handleFailedToConnect(_ peripheral: CBPeripheral, _ error: Error?) {
        guard isCurrent(peripheral) else { return }
        connectTimeoutTask?.cancel()
        connectedPeripheral = nil
        clearGattState()
        connection = .error(message: error?.localizedDescription ?? "Failed to connect")
        lastEvent = "didFailToConnect: \(error?.localizedDescription ?? "-")"
    }

    fileprivate func handleDisconnected(_ peripheral: CBPeripheral, _ error: Error?) {
        // Without filtering: tap CONNECT on device B → device A's didDisconnect arrives → B gets overwritten with "Disconnected"
        guard isCurrent(peripheral) else {
            AppLog.d("BleClient", "ignore didDisconnect of old \(peripheral.identifier)")
            return
        }
        connectTimeoutTask?.cancel()
        connectedPeripheral = nil
        // Day 4: both paths (manual / signal lost) clear services + subscriptions. The old CBCharacteristics are dead.
        clearGattState()
        if userRequestedDisconnect {
            connection = .disconnected
        } else {
            // CBError 6 = "connection timed out unexpectedly" (out of range) · 7 = device disconnected on purpose
            connection = .error(message: "Unexpected disconnect: \(error?.localizedDescription ?? "unknown")")
        }
        lastEvent = "didDisconnect: \(error?.localizedDescription ?? "user requested")"
        userRequestedDisconnect = false
    }
}

/// Objective-C delegate bridge → `BluetoothClientService`.
///
/// A separate class because `@Observable` doesn't mix cleanly with `NSObject`, and so the service doesn't expose
/// delegate methods publicly. `unowned` not strong: the service holds the bridge, the bridge holding it back = retain cycle.
private final class CentralDelegate: NSObject, CBCentralManagerDelegate {

    private unowned let owner: BluetoothClientService

    init(owner: BluetoothClientService) {
        self.owner = owner
    }

    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        AppLog.d("BleClient", "state = \(central.state.rawValue)")
        // queue nil ⇒ we really are on the main thread; assumeIsolated tells the compiler so.
        MainActor.assumeIsolated { owner.handleStateChange(central.state) }
    }

    func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi RSSI: NSNumber
    ) {
        MainActor.assumeIsolated {
            owner.handleDiscovered(peripheral, advertisement: advertisementData, rssi: RSSI)
        }
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        AppLog.d("BleClient", "didConnect \(peripheral.identifier)")
        MainActor.assumeIsolated { owner.handleConnected(peripheral) }
    }

    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        MainActor.assumeIsolated { owner.handleFailedToConnect(peripheral, error) }
    }

    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        MainActor.assumeIsolated { owner.handleDisconnected(peripheral, error) }
    }
}

/// Day 3 + Day 4 — delegate bridge for the **peripheral** (unlike `CentralDelegate`: central handles scan/connect,
/// peripheral handles GATT). `unowned` for the same reason: the service holds the bridge.
private final class PeripheralDelegate: NSObject, CBPeripheralDelegate {

    private unowned let owner: BluetoothClientService

    init(owner: BluetoothClientService) {
        self.owner = owner
    }

    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        MainActor.assumeIsolated { owner.handleServicesDiscovered(peripheral, error) }
    }

    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        MainActor.assumeIsolated { owner.handleCharacteristicsDiscovered(peripheral, service, error) }
    }

    func peripheral(_ peripheral: CBPeripheral, didModifyServices invalidatedServices: [CBService]) {
        MainActor.assumeIsolated { owner.handleServicesModified(peripheral) }
    }

    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        MainActor.assumeIsolated { owner.handleValue(peripheral, characteristic, error) }
    }

    func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: Error?) {
        MainActor.assumeIsolated { owner.handleNotificationState(peripheral, characteristic, error) }
    }

    func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?) {
        MainActor.assumeIsolated { owner.handleWritten(peripheral, characteristic, error) }
    }
}
