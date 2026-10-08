import Foundation

/// Week 3 · Day 12 — one device-state record, using the plan's exact field names (section 9 *Background Device Monitor*).
/// Mirrors `android/.../nativecore/device/DeviceStatus.kt`.
///
/// Fields that **can't be read** are `nil` → written to JSON as `null`, NOT guessed as `false`:
/// - `wifiEnabled`: iOS has **no public API** to read the Wi-Fi switch → always `null` (Android can read `isWifiEnabled`).
/// - `battery`: the Simulator returns `-1` → `null`.
/// - `bluetoothEnabled`: without Bluetooth permission the app may not create a `CBCentralManager` to ask → `null`.
///
/// Synthesized `Codable` encoding skips `nil` fields (uses `encodeIfPresent`) → hand-written `encode(to:)` keeps `null`
/// to match the plan's format. `source` is the opposite: `nil` is omitted entirely (like Android).
struct DeviceStatus: Codable, Equatable, Sendable {
    /// ISO 8601 with offset, e.g. `2026-10-05T09:30:00+07:00`.
    var timestamp: String
    var battery: Int?
    var wifiEnabled: Bool?
    var wifiConnected: Bool
    var cellularAvailable: Bool
    var bluetoothEnabled: Bool?
    var bluetoothConnected: Bool?
    /// Added beyond the plan: which mechanism wrote this line — `loop` · `manual` · `bgRefresh`. For filling in the Day 14 behaviour table.
    var source: String?

    private enum CodingKeys: String, CodingKey {
        case timestamp, battery, wifiEnabled, wifiConnected, cellularAvailable, bluetoothEnabled, bluetoothConnected, source
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(timestamp, forKey: .timestamp)
        try c.encode(battery, forKey: .battery)                    // encode(Optional) → ghi `null` khi nil
        try c.encode(wifiEnabled, forKey: .wifiEnabled)
        try c.encode(wifiConnected, forKey: .wifiConnected)
        try c.encode(cellularAvailable, forKey: .cellularAvailable)
        try c.encode(bluetoothEnabled, forKey: .bluetoothEnabled)
        try c.encode(bluetoothConnected, forKey: .bluetoothConnected)
        try c.encodeIfPresent(source, forKey: .source)
    }

    /// One JSONL line (no `\n`). Fixed key order (`sortedKeys`) → 2 identical writes give 2 identical lines.
    func toJson() throws -> String {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys, .withoutEscapingSlashes]
        return String(decoding: try encoder.encode(self), as: UTF8.self)
    }

    /// A broken line (not JSON, missing `timestamp`) → `throw`; `FileLogger.readLogs` catches and counts it.
    static func fromJson(_ line: some StringProtocol) throws -> DeviceStatus {
        try JSONDecoder().decode(DeviceStatus.self, from: Data(line.utf8))
    }

    /// Timestamp format like Android's `ISO_OFFSET_DATE_TIME` truncated to seconds: `2026-10-05T09:30:00+07:00`.
    static func timestamp(_ date: Date = .now) -> String {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        f.timeZone = .current
        return f.string(from: date)
    }
}
