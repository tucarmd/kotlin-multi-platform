import Foundation

/// Scanner screen rules per Learning Plan §4 mockup — split out of the View so they can be tested without an iPhone,
/// and so the View has no business `if/else`. Mirrors `android/.../ui/scanner/ScannerActions.kt`.
///
/// ```
///  Device Name: [ cmd      ]  ──► matchesName(device.name, query) ──► list keeps only devices with "cmd" in the name
///  Hide unknown devices [●]   ──► isUnknownDevice(name)   ┐ isDeviceShown(): combined with the Device Name box,
///  Hide audio devices   [●]   ──► isAudioDevice(adv)      ┘ the selected / connected device is never hidden by a switch
///  [ Scan Bluetooth ]
///  Discovered Devices  (tap a row = select)
///  [ Connect ] [ Disconnect ] ◄── scannerButtonsOf(selected device, state) ──► enabled / disabled / spinner
/// ```
///
/// The Device Name box **filters the list already seen** (filters as you type), it doesn't change how scanning works — unlike the
/// "CarMD tools only" switch (filters during the scan, in `BluetoothScanFilter`). This is exactly the Day 02 exercise
/// "map/filter an array with a closure" brought into a real UI.

/// Empty box = show everything. With text, **unnamed** devices are hidden (no name to match).
/// "Contains" match + case-insensitive: typing `cmd` finds `CMD-3C71BF…`.
func matchesName(_ name: String?, query: String) -> Bool {
    let q = query.trimmingCharacters(in: .whitespaces)
    if q.isEmpty { return true }
    return name?.localizedCaseInsensitiveContains(q) == true
}

// MARK: - 2 list filter switches: Hide unknown devices · Hide audio devices

/// No name to recognise it by → *Hide unknown devices* hides it. A whitespace-only name counts as unnamed too.
func isUnknownDevice(_ name: String?) -> Bool {
    (name ?? "").trimmingCharacters(in: .whitespaces).isEmpty
}

/// Headphones / speakers / earbuds — they do send BLE packets (quick-pair popup, battery) but aren't GATT devices the app needs.
/// Matching **1** sign is enough; matching none → NOT audio (better to miss one than wrongly hide a dongle).
///
/// ```
///  Service LE Audio 184E 1850 1853 184F ┐
///  Service data FE2C (Fast Pair) ───────┼─► true
///  Apple 0x004C, type byte 0x07 ────────┘
/// ```
/// Fewer signs than Android (no Appearance, device class) → iOS recognises fewer headphones — not compensated by guessing names.
func isAudioDevice(_ adv: BluetoothAdvertisement?) -> Bool {
    guard let adv else { return false }
    return adv.serviceUuids.contains { BluetoothLookup.leAudioServices.contains($0.uppercased()) }
        || adv.serviceDataUuids.contains { $0.uppercased() == BluetoothLookup.fastPairService }
        || adv.appleType == BluetoothLookup.appleProximityPairing
}

/// Whether a row is shown = the Device Name box **and** the 2 switches (combined).
/// `isPinned` = selected / connected device: the switches NEVER hide it (otherwise the Disconnect button is gone).
func isDeviceShown(
    _ device: BluetoothDeviceInfo,
    query: String,
    hideUnknown: Bool,
    hideAudio: Bool,
    isPinned: Bool
) -> Bool {
    guard matchesName(device.name, query: query) else { return false }
    if isPinned { return true }
    if hideUnknown && isUnknownDevice(device.name) { return false }
    if hideAudio && isAudioDevice(device.advertisement) { return false }
    return true
}

/// State of the 2 buttons under the list.
struct ScannerButtons: Equatable {
    let connectEnabled: Bool
    /// Connecting → the Connect button shows a spinner (and is disabled).
    let connecting: Bool
    let disconnectEnabled: Bool
}

/// ```
///  connecting ─────────────────────────────────────────► Connect ⏳ (disabled) · Disconnect disabled
///  connected + that same device selected ──────────────► Connect disabled  · Disconnect enabled
///  connected + a DIFFERENT device selected ────────────► Connect enabled (service drops the old one) · Disconnect enabled
///  nothing selected / not ready / "non-connectable" ───► Connect disabled
/// ```
/// - Parameters:
///   - hasSelection: a row has been tapped (and it's still in the list).
///   - selectedIsConnectable: advertising flag of the selected device. `nil` = **let them try**, don't block wrongly.
///   - selectedIsConnectedDevice: whether the selected device is the one in `connectedId`.
///   - ready: permission + radio ready (`BleReadiness.ready`).
func scannerButtonsOf(
    hasSelection: Bool,
    selectedIsConnectable: Bool?,
    selectedIsConnectedDevice: Bool,
    connection: BluetoothConnectionState,
    ready: Bool
) -> ScannerButtons {
    let isConnecting = connection == .connecting
    let isConnected: Bool = if case .connected = connection { true } else { false }
    let alreadyOnThisDevice = isConnected && selectedIsConnectedDevice
    return ScannerButtons(
        connectEnabled: ready && hasSelection && selectedIsConnectable != false && !isConnecting && !alreadyOnThisDevice,
        connecting: isConnecting,
        disconnectEnabled: isConnected
    )
}
