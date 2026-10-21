import Foundation

/// Day 3 + Day 4 — snapshot of the GATT tree after connect + read / notified values. Pure functions,
/// **does not hold** `CBService` / `CBCharacteristic` (same reason `BluetoothDeviceInfo` doesn't hold `CBPeripheral`).
///
/// ```
///  Peripheral (GATT server)
///   └─ GattServiceInfo  180F Battery
///        └─ GattCharacteristicInfo  2A19 Battery Level   properties = 0x12 → READ + NOTIFY
///              ├─ read()        → GattValue(source: .read,   hex "5A", decoded "90 %")
///              └─ setNotify(on) → GattValue(source: .notify, …) every time the peripheral pushes
///   └─ GattServiceInfo  FFE0 (dongle CarMD)
///        └─ GattCharacteristicInfo  FFE1   properties = 0x1C → WRITE + WRITE NO RESP + NOTIFY
///              └─ write(bytes)  → GattValue(source: .write,  hex "41 54 5A 0D", decoded "\"ATZ\\r\"")   (task 004)
/// ```
///
/// UUIDs are `CBUUID.uuidString` strings (`2A19`, `FFE1`, 128-bit in full) — the same string as Android's `shortUuid`,
/// so rules + tests are identical. Mirrors `android/.../nativecore/bluetooth/GattModel.kt`.

/// Properties = a **1-byte bitmask**. `CBCharacteristicProperties.rawValue` and Android's `PROPERTY_*` use
/// **the same bit values** from the Bluetooth spec → one shared parser.
///
/// ```
///  bit  0x02 READ · 0x04 WRITE_NO_RESPONSE · 0x08 WRITE · 0x10 NOTIFY · 0x20 INDICATE
///  vd 0x12 = 0001 0010 = NOTIFY + READ
/// ```
struct GattProperties: Equatable {
    var read = false
    var write = false
    var writeNoResponse = false
    var notify = false
    var indicate = false

    static let readBit = 0x02
    static let writeNoResponseBit = 0x04
    static let writeBit = 0x08
    static let notifyBit = 0x10
    static let indicateBit = 0x20

    static func of(_ raw: Int) -> GattProperties {
        GattProperties(read: raw & readBit != 0,
                       write: raw & writeBit != 0,
                       writeNoResponse: raw & writeNoResponseBit != 0,
                       notify: raw & notifyBit != 0,
                       indicate: raw & indicateBit != 0)
    }

    /// Notify and indicate are both enabled via CCCD `2902` — indicate adds an ACK (slower, more reliable).
    var canSubscribe: Bool { notify || indicate }

    /// Writable by either type. UART dongles (`FFE1`) usually declare both.
    var canWrite: Bool { write || writeNoResponse }

    /// `READ NOTIFY` — label printed next to the characteristic, like nRF Connect's Properties column.
    var labels: [String] {
        var out: [String] = []
        if read { out.append("READ") }
        if write { out.append("WRITE") }
        if writeNoResponse { out.append("WRITE NO RESP") }
        if notify { out.append("NOTIFY") }
        if indicate { out.append("INDICATE") }
        return out
    }
}

struct GattCharacteristicInfo: Equatable, Identifiable {
    let serviceUuid: String
    let uuid: String
    let properties: GattProperties
    /// CCCD enabled (peripheral confirmed via `didUpdateNotificationStateFor`) — the UI turns the Notify button into Stop.
    var isNotifying = false

    /// Unique key within one connection — the same characteristic UUID may live in 2 different services (rare).
    var key: String { "\(serviceUuid)/\(uuid)" }
    var id: String { key }
}

struct GattServiceInfo: Equatable, Identifiable {
    let uuid: String
    var characteristics: [GattCharacteristicInfo]
    var id: String { uuid }
}

/// Where the value came from — read = we asked, notify = the peripheral pushed, write = we sent.
enum GattValueSource: String {
    case read = "READ"
    case notify = "NOTIFY"
    case write = "WRITE"
}

/// One batch of received bytes. Stores `hex` + `decoded` (computed once on arrival) — the View only prints, never re-decodes on each draw.
struct GattValue: Equatable, Identifiable {
    let id = UUID()
    let serviceUuid: String
    let uuid: String
    let hex: String
    let decoded: String
    let source: GattValueSource
    let at: Date
}

/// Max lines in *Received Data*. Notify at 20 packets/second without a cap → the array bloats, the View slows down.
let receivedLogMax = 50

/// Append at the END, drop the OLDEST lines past `max`.
func appendCapped<T>(_ list: [T], _ item: T, max: Int = receivedLogMax) -> [T] {
    Array((list + [item]).suffix(max))
}

// MARK: - Target characteristic (Day 3 target: "can locate a target characteristic")

/// Priority when picking a characteristic to **Read** as a test: battery → manufacturer → device name → the first READ one.
let readTargetPriority = ["2A19", "2A29", "2A00"]

/// Priority when picking a characteristic to **Notify**: CarMD dongle UART `FFE1` → Nordic UART TX → Heart Rate →
/// Battery → the first notify/indicate one.
let notifyTargetPriority = ["FFE1", "6E400003-B5A3-F393-E0A9-E50E24DCCA9E", "2A37", "2A19"]

/// Priority when picking a characteristic to **Write**: CarMD dongle UART `FFE1` (same slot as notify — HM-10 dongles use
/// 1 characteristic for both directions) → Nordic UART **RX** `6E400002` (app → device) → the first writable one.
let writeTargetPriority = ["FFE1", "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"]

func readTargetOf(_ services: [GattServiceInfo]) -> GattCharacteristicInfo? {
    pickTarget(services, priority: readTargetPriority) { $0.properties.read }
}

func notifyTargetOf(_ services: [GattServiceInfo]) -> GattCharacteristicInfo? {
    pickTarget(services, priority: notifyTargetPriority) { $0.properties.canSubscribe }
}

func writeTargetOf(_ services: [GattServiceInfo]) -> GattCharacteristicInfo? {
    pickTarget(services, priority: writeTargetPriority) { $0.properties.canWrite }
}

private func pickTarget(_ services: [GattServiceInfo], priority: [String],
                        usable: (GattCharacteristicInfo) -> Bool) -> GattCharacteristicInfo? {
    let candidates = services.flatMap(\.characteristics).filter(usable)
    for wanted in priority {
        if let hit = candidates.first(where: { $0.uuid.caseInsensitiveCompare(wanted) == .orderedSame }) { return hit }
    }
    return candidates.first
}

// MARK: - MTU (task 004)

/// MTU = the maximum size of **1 ATT packet** both sides agree on. Before negotiation the spec mandates **23**.
///
/// ```
///  ┌──────── 1 ATT packet = MTU bytes ────┐
///  │ opcode 1 │ handle 2 │  data MTU-3     │     MTU 23  → 20 data bytes / packet
///  └──────────┴──────────┴─────────────────┘     MTU 247 → 244 bytes (fits 1 Data Length Extension packet of 251)
/// ```
/// iOS negotiates the MTU **itself** on connect (no API to request it) — the app only reads it back via `maximumWriteValueLength`.
let attDefaultMtu = 23

/// Android requests this MTU after discovery. The same constant on both sides keeps tests identical.
let attRequestedMtu = 247

/// The first 3 bytes of every packet are opcode + handle — not our data.
let attHeaderBytes = 3

func maxPayloadOf(_ mtu: Int) -> Int { max(mtu - attHeaderBytes, 0) }

// MARK: - Write plan (task 004)

/// Result of checking the Write box **before** sending. The View uses it for the preview / red text and to disable Send —
/// the rules live here (testable), the View just prints.
enum WritePlan: Equatable {
    /// `withResponse` = write with ACK (peripheral confirms). false = "fire and forget", faster.
    case ok(Data, withResponse: Bool)
    case invalid(String)
}

/// All the rules of the Send button:
/// 1. the characteristic must have WRITE or WRITE NO RESP
/// 2. the input converts to bytes (`BluetoothDecoder.fromHex` / `fromText`)
/// 3. byte count ≤ `maxPayload` (= `MTU − 3`) — no splitting into packets, too long is simply blocked to keep it simple
/// 4. has WRITE → write **with response** (know for sure the peripheral got it); only NO RESP → without response
func planWrite(_ input: String, asHex: Bool, properties: GattProperties, maxPayload: Int) -> WritePlan {
    guard properties.canWrite else { return .invalid("This characteristic is not writable") }
    guard !input.isEmpty else { return .invalid("Type something to send") }
    guard let bytes = asHex ? BluetoothDecoder.fromHex(input) : BluetoothDecoder.fromText(input) else {
        return .invalid("Invalid hex — use pairs like 41 54 5A 0D")
    }
    guard bytes.count <= maxPayload else {
        return .invalid("\(bytes.count) bytes > max \(maxPayload) for MTU \(maxPayload + attHeaderBytes)")
    }
    return .ok(bytes, withResponse: properties.write)
}
