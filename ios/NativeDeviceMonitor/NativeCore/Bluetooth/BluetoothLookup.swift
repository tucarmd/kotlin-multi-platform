import Foundation

/// Day 2 (device information) — static lookups so the device card is readable by eye, like nRF Connect.
/// Pure functions, **no network calls**. Mirrors `android/.../nativecore/bluetooth/BluetoothLookup.kt` — same tables, same thresholds.
///
/// ```
///  manufacturer data  4C 00 02 15 …   → company id 0x004C (little-endian) → "Apple"
///  service UUID       CBUUID "180F"   → "Battery"
///  rssi               -58 dBm         → BleSignal.strong (3 bars, green)
///  lastSeen           7 s ago         → isStale = true (card fades while scanning)
/// ```
/// Full Bluetooth SIG tables: https://www.bluetooth.com/specifications/assigned-numbers/
enum BluetoothLookup {

    /// Company identifier (first 2 bytes of manufacturer data). Not in the table → nil, the UI just shows the hex number.
    private static let companies: [UInt16: String] = [
        0x0002: "Intel",
        0x0006: "Microsoft",
        0x000A: "Qualcomm",
        0x000D: "Texas Instruments",
        0x000F: "Broadcom",
        0x004C: "Apple",
        0x0059: "Nordic Semiconductor",
        0x0075: "Samsung",
        0x0087: "Garmin",
        0x00E0: "Google",
        0x0171: "Amazon",
        0x02E5: "Espressif",
        0x038F: "Xiaomi",
    ]

    /// `CBUUID.uuidString` (16-bit printed short `180F`, 128-bit printed in full) → name.
    /// `FFE0` isn't a SIG standard — it's the HM-10 style UART port the CarMD dongle uses.
    private static let services: [String: String] = [
        "1800": "Generic Access",
        "1801": "Generic Attribute",
        "180A": "Device Information",
        "180D": "Heart Rate",
        "180F": "Battery",
        "1812": "HID",
        "181A": "Environmental Sensing",
        "FE59": "Nordic DFU",
        "FEAA": "Eddystone",
        "FD6F": "Exposure Notification",
        "FFE0": "UART (CarMD dongle)",
        "6E400001-B5A3-F393-E0A9-E50E24DCCA9E": "Nordic UART",
    ]

    static func companyName(_ id: UInt16) -> String? { companies[id] }

    static func serviceName(_ uuid: String) -> String? { services[uuid.uppercased()] }

    /// Day 3 — characteristic UUID → name, so the GATT list is readable by eye. `FFE1` is the UART data slot
    /// (both write and notify) in service `FFE0` of the CarMD / HM-10 dongle. Same table as Android.
    private static let characteristics: [String: String] = [
        "2A00": "Device Name",
        "2A01": "Appearance",
        "2A04": "Connection Parameters",
        "2A05": "Service Changed",
        "2A19": "Battery Level",
        "2A24": "Model Number",
        "2A25": "Serial Number",
        "2A26": "Firmware Revision",
        "2A27": "Hardware Revision",
        "2A28": "Software Revision",
        "2A29": "Manufacturer Name",
        "2A37": "Heart Rate Measurement",
        "2A38": "Body Sensor Location",
        "2AA6": "Central Address Resolution",
        "FFE1": "UART data (CarMD dongle)",
        "6E400002-B5A3-F393-E0A9-E50E24DCCA9E": "Nordic UART RX (write)",
        "6E400003-B5A3-F393-E0A9-E50E24DCCA9E": "Nordic UART TX (notify)",
    ]

    static func characteristicName(_ uuid: String) -> String? { characteristics[uuid.uppercased()] }

    /// No packet for more than 5 s **while scanning** → the card fades. 5 s rather than 10 s: each scan only
    /// lasts 10 s, a 10 s threshold would never get to fade. After the scan the View doesn't apply this rule.
    static let staleAfter: TimeInterval = 5

    // MARK: - Tai nghe / loa (filter *Hide audio devices*) — docs/bluetooth-audio-devices.md
    // Android also has Appearance + device class; iOS can't read those 2 so they're not here.

    static let appleCompanyId: UInt16 = 0x004C
    /// Type byte in Apple manufacturer data: `0x07` = proximity pairing (AirPods/Beats popup). `0x12` Find My is NOT audio.
    static let appleProximityPairing: UInt8 = 0x07
    /// Services only LE Audio devices have: ASCS · PACS · CAP · BASS.
    static let leAudioServices: Set<String> = ["184E", "1850", "1853", "184F"]
    /// Google Fast Pair — Android earbuds advertise it for the quick-pair popup.
    static let fastPairService = "FE2C"

    static func isStale(lastSeen: Date, now: Date) -> Bool {
        now.timeIntervalSince(lastSeen) > staleAfter
    }
}

/// Signal strength → number of bars. Thresholds: ≥ -60 strong · -60…-80 medium · < -80 weak (connect likely fails).
enum BleSignal: Int {
    case weak = 1
    case medium = 2
    case strong = 3

    var bars: Int { rawValue }

    static func of(_ rssi: Int) -> BleSignal {
        if rssi >= -60 { return .strong }
        if rssi >= -80 { return .medium }
        return .weak
    }
}
