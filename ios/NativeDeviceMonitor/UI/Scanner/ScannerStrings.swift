import Foundation

/// Day 2 — Scanner screen strings (Rule 4 · Rule 5.3). Mirrors `android/.../ui/scanner/ScannerStrings.kt`
/// — same wording so 2 screenshots side by side can be compared line by line.
enum ScannerStrings {
    static let title = "Scanner"
    /// Blue header per the mockup — Android gets it from the TopAppBar (`AppStrings.APP_TITLE`).
    static let screenTitle = "Native Device Monitor"
    static let hint = "Turn on a BLE peripheral first (nRF Connect > Advertiser on another phone, or an ESP32). "
        + "Scan stops automatically after 10 seconds."

    // MARK: - Not ready
    static let notReadyTitle = "Not ready"
    static let notReadyPermission = "Bluetooth permission is missing — grant it on the Permissions tab first."
    static let notReadyRadioOff = "Bluetooth is off — turn it on, then come back."
    static let notReadyUnsupported = "This device has no Bluetooth adapter."
    static let openPermissionsButton = "Open Permissions"
    static let simulatorNote = "Simulator has no Bluetooth — run on a real iPhone to scan."

    // MARK: - Mockup Learning Plan §4
    static let deviceNameLabel = "Device Name"
    static let searchPlaceholder = "Search device…"
    static let scanButton = "Scan Bluetooth"
    static let stopScanButton = "Stop scan"
    static let devicesTitle = "Discovered Devices"
    /// `3 of 7` when the Device Name box is filtering.
    static func matchCountFormat(_ shown: Int, _ total: Int) -> String { "\(shown) of \(total)" }
    static let noDevices = "No devices yet — tap Scan Bluetooth"
    static func noMatchFormat(_ query: String) -> String { "No device name contains \"\(query)\"" }
    static let unknownDevice = "Unknown device"
    static let connectedTag = "CONNECTED"
    static func rssiFormat(_ rssi: Int) -> String { "\(rssi) dBm" }
    static func scanningFormat(_ count: Int) -> String { "Scanning… \(count) found" }
    static let connectButton = "Connect"
    static let disconnectButton = "Disconnect"

    // MARK: - Status
    static func statusFormat(_ label: String) -> String { "Status: \(label)" }
    static let stateIdle = "Idle — select a device, then Connect"
    static let stateConnecting = "Connecting…"
    static let stateDisconnected = "Disconnected"
    static func stateConnectedFormat(_ name: String) -> String { "Connected to \(name)" }

    // MARK: - GATT (Day 3 + Day 4)
    static let gattTitle = "GATT"
    static let gattNotConnected = "Connect a device to list its services and characteristics."
    static let gattDiscovering = "Discovering services…"
    static let gattEmpty = "No services found."
    static func servicesCountFormat(_ services: Int, _ characteristics: Int) -> String {
        "\(services) services · \(characteristics) characteristics"
    }
    /// `180F Battery` — unknown name → just the UUID.
    static func uuidWithName(_ uuid: String, _ name: String?) -> String { name.map { "\(uuid) \($0)" } ?? uuid }
    static let targetTag = "★ target"
    static func readTargetFormat(_ label: String?) -> String { "Read target: " + (label ?? "none (nothing readable)") }
    static func notifyTargetFormat(_ label: String?) -> String { "Notify target: " + (label ?? "none (nothing notifiable)") }
    static let readButton = "Read"
    static let notifyButton = "Notify"
    static let stopNotifyButton = "Stop"
    static func lastReadTitleFormat(_ label: String) -> String { "Last read: \(label)" }
    static func rawFormatLine(_ hex: String) -> String { "raw:   " + (hex.isEmpty ? "(empty)" : hex) }
    static func decodedFormatLine(_ value: String) -> String { "value: \(value)" }

    // MARK: - Write + MTU (task 004)
    static func writeTargetFormat(_ label: String?) -> String { "Write target: " + (label ?? "none (nothing writable)") }
    static func mtuFormat(_ mtu: Int, _ max: Int) -> String { "MTU \(mtu) · max \(max) bytes per packet" }
    static let writeButton = "Write"
    static let writeTitle = "Write"
    static func writeToFormat(_ label: String, _ withResponse: Bool) -> String {
        "To \(label) · " + (withResponse ? "with response" : "without response")
    }
    static let modeText = "Text"
    static let modeHex = "Hex"
    static let writePlaceholderText = "ATZ\\r"
    static let writePlaceholderHex = "41 54 5A 0D"
    /// Common ELM327 commands: reset · ask version · engine RPM (PID 0C). Tap = fill the box, not sent yet.
    static let writePresets = ["ATZ\\r", "ATI\\r", "010C\\r"]
    static let writeEscapeHint = "Text: type \\r \\n \\t for CR / LF / tab — ELM327 dongles only answer after \\r."
    static let sendButton = "Send"
    static func writePreviewFormat(_ hex: String, _ count: Int) -> String { "→ \(hex) (\(count) bytes)" }

    // MARK: - Received Data
    static let receivedTitle = "Received Data"
    /// Empty box: tells the user what to do to get data.
    static let receivedPlaceholder = "— nothing yet: connect, then Read or Notify a characteristic"
    static let clearButton = "Clear"
    static func receivedCountFormat(_ count: Int, _ max: Int) -> String { "\(count) / \(max)" }
    /// `12:03:04.123 NOTIFY FFE1  41 54 0D → "AT\r"`
    static func receivedLineFormat(_ time: String, _ source: String, _ uuid: String, _ hex: String, _ decoded: String) -> String {
        "\(time) \(source) \(uuid)  \(hex) → \(decoded)"
    }

    // MARK: - Filter + device information (nRF Scanner style)
    static let filterToggle = "CarMD tools only"
    static func filterOnFormat(_ label: String) -> String { "Showing only \(label)" }
    static let filterOff = "Showing every BLE device"
    static func hiddenFormat(_ count: Int) -> String { "\(count) other devices hidden by the filter" }
    static let hideUnknownToggle = "Hide unknown devices"
    static let hideAudioToggle = "Hide audio devices (headphones, speakers)"
    /// The list has devices but the 2 switches hide them all.
    static func allHiddenFormat(_ count: Int) -> String { "All \(count) devices are hidden — turn a filter off" }
    static let tapToExpand = "Tap a device to select it · tap again for advertising data. Faded rows have not been heard for 5 s."
    static let iosLimits = "iOS hides MAC, flags, raw bytes, bond state and device type — use the Android build or nRF Connect for those."
    static func connectableFormat(_ value: Bool?) -> String {
        "Connectable: " + (value.map { $0 ? "yes" : "no" } ?? "n/a")
    }
    static func txPowerFormat(_ dbm: Int?) -> String { "TX power: " + (dbm.map { "\($0) dBm" } ?? "n/a") }
    static func intervalFormat(_ ms: Int?, _ packets: Int) -> String {
        "Interval: " + (ms.map { "~\($0) ms" } ?? "n/a") + " · \(packets) packets"
    }
    static func lastSeenFormat(_ secondsAgo: Int) -> String {
        "Last seen: " + (secondsAgo <= 0 ? "just now" : "\(secondsAgo)s ago")
    }
    /// `Services: 180F Battery, FFE0 UART (CarMD dongle)` — names from `BluetoothLookup`.
    static func servicesFormat(_ uuids: [String], nameOf: (String) -> String?) -> String {
        let parts = uuids.map { u in nameOf(u).map { "\(u) \($0)" } ?? u }
        return "Services: " + (parts.isEmpty ? "none" : parts.joined(separator: ", "))
    }
    static func manufacturerFormat(_ line: String) -> String { "Manufacturer \(line)" }
    static func serviceDataFormat(_ line: String) -> String { "Service data \(line)" }
}
