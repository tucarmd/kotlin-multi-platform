import CoreBluetooth
import Foundation

/// Day 2 — one BLE device **seen while scanning**. A data snapshot, does NOT hold the `CBPeripheral`.
///
/// Mirrors `android/.../BluetoothDeviceInfo.kt`. One **very important** difference from Android:
///
/// ```
///   Android : id = MAC address  (AA:BB:CC:DD:EE:FF)  — the same on every Android phone
///   iOS     : id = UUID assigned by THIS PHONE        — another phone sees a different UUID
/// ```
///
/// iOS **doesn't let apps read the MAC** of a BLE device (privacy). Instead each iPhone assigns
/// a stable `identifier` *of its own*. In practice: you **cannot** store this id on a server and
/// reuse it on another phone — that's why the real CarMD app stores the MAC from **Android** and pairs
/// by name/advertising on iOS (see `knowledge/carmd-core/`).
///
/// `Identifiable` so SwiftUI's `ForEach`/`List` can use it directly, no `id:` needed.
struct BluetoothDeviceInfo: Identifiable, Equatable {
    /// `CBPeripheral.identifier` — unique key, only valid on this phone.
    let id: UUID
    /// nil when the device doesn't advertise a name → UI shows "Unknown device", does NOT skip the row.
    let name: String?
    /// Signal strength, dBm. Closer to 0 is closer (-40 very close, -95 about to drop).
    let rssi: Int
    /// Day 2: latest advertising packet — the View shows it when the card is tapped to expand (nRF Scanner style).
    var advertisement: BluetoothAdvertisement? = nil
    /// When the latest packet arrived → "Last seen: 3s ago" + fade the card when the device goes quiet (`BluetoothLookup.isStale`).
    var lastSeen = Date()
}

/// Day 2 (nRF-style) — advertising packet contents like nRF Connect's Scanner. Mirrors the Kotlin `BluetoothAdvertisement`.
///
/// ```
///  advertisementData: [String: Any]
///   ├─ CBAdvertisementDataIsConnectable      → isConnectable
///   ├─ CBAdvertisementDataTxPowerLevelKey    → txPower
///   ├─ CBAdvertisementDataServiceUUIDsKey    → serviceUuids
///   ├─ CBAdvertisementDataManufacturerDataKey→ first 2 bytes = company id (little-endian) + data
///   └─ CBAdvertisementDataServiceDataKey     → uuid: bytes
/// ```
/// **iOS does NOT allow** reading: MAC, flags, raw bytes, bond state — CoreBluetooth has already split the packet into a dictionary.
/// To compare byte by byte with the dongle use the Android build (Raw) or nRF Connect.
struct BluetoothAdvertisement: Equatable {
    let isConnectable: Bool?
    let txPower: Int?
    let serviceUuids: [String]
    let manufacturerData: [String]
    let serviceData: [String]
    let packetCount: Int
    /// An estimate — iOS merges / drops packets, especially with the screen off.
    let intervalMs: Int?

    // Signs of "this is headphones / a speaker" for the *Hide audio devices* filter (docs/bluetooth-audio-devices.md).
    // iOS exposes neither Appearance nor device class like Android → only these 2 fields + `serviceUuids`.

    /// UUID of the service data, e.g. `FE2C` (Google Fast Pair).
    var serviceDataUuids: [String] = []
    /// Type byte in Apple manufacturer data (`0x004C`); `0x07` = AirPods/Beats proximity pairing.
    var appleType: UInt8? = nil
}

/// Day 2 — connection state. An `enum` with associated values = a `sealed interface` in Kotlin:
/// `connected` carries the name, `error` carries the reason.
///
/// ```
///   idle ──scan──► (list) ──connect()──► connecting ──► connected(name)
///     ▲                                           │               │
///     │                                           │ failure       │ disconnect() / signal lost
///     └────────────  disconnected ◄────────────────┴───────────────┘
///                          ▲
///                      error(message)
/// ```
///
/// `switch` over this enum is **exhaustive** — add a new case and forget to handle it in the View → build error.
enum BluetoothConnectionState: Equatable {
    case idle
    /// `central.connect` called, waiting for `didConnect`. iOS has **no timeout** here (unlike Android ~30 s) —
    /// it can hang forever if the device is silent, so `BluetoothClientService` sets its own timer.
    case connecting
    case connected(name: String)
    /// Disconnected **because we tapped** — separate from `.error` so the UI doesn't scare the user with red text.
    case disconnected
    case error(message: String)
}
