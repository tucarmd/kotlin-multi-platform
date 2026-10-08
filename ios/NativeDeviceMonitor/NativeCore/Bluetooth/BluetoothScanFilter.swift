import Foundation

/// Day 2 (nRF-style) — filter devices during scan by NAME, like the CarMD Ionic app but the names are a PARAMETER.
/// Mirrors `BluetoothScanFilter.kt`.
///
/// ```
///  didDiscover ─► name = peripheral.name ?? advertisementData[LocalName]
///                  └─ filter.matches(name)?  yes → found[id]   ·   no → hidden.insert(id)
/// ```
/// Ionic: `local_plugin/cordova-plugin-ble-central-master/src/ios/BLECentralPlugin.m:663` hard-codes the filter
/// `CarMD2` / `CMD-` in native (risk #8, bluetooth 05). Here the View toggles it, the service takes it as a parameter.
/// Case-insensitive comparison (like Ionic JS `toUpperCase()`).
struct BluetoothScanFilter: Equatable {
    var names: [String] = []
    var prefixes: [String] = []

    /// No rules = see everything, including unnamed devices (debug mode, this app's default).
    var isOff: Bool { names.isEmpty && prefixes.isEmpty }

    func matches(_ name: String?) -> Bool {
        if isOff { return true }
        // No name means it can't be a CarMD tool — Ionic drops it too
        guard let n = name?.trimmingCharacters(in: .whitespaces), !n.isEmpty else { return false }
        return names.contains { $0.caseInsensitiveCompare(n) == .orderedSame }
            || prefixes.contains { n.lowercased().hasPrefix($0.lowercased()) }
    }

    /// `CarMD2 · CMD-*` — printed in the UI so the tester knows what is being filtered.
    var label: String { (names + prefixes.map { "\($0)*" }).joined(separator: " · ") }

    /// Default = exactly the name set the Ionic app uses (`src/app/configs/app.config.ts:157` + `CMD-`).
    static let carmdTools = BluetoothScanFilter(names: ["CarMD2"], prefixes: ["CMD-"])
    static let none = BluetoothScanFilter()
}
