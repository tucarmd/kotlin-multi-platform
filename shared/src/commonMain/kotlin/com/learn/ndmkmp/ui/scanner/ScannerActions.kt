package com.learn.ndmkmp.ui.scanner

// 📍 P1 note: pure rules (no Compose import) even though the folder says ui/ — they were the Scanner screen's helpers
// in the old app. In P8 they move to presentation/scanner/ next to ScannerViewModel. Tests: commonTest/ui/scanner/.

import com.learn.ndmkmp.core.bluetooth.BluetoothAdvertisement
import com.learn.ndmkmp.core.bluetooth.BluetoothConnectionState
import com.learn.ndmkmp.core.bluetooth.BluetoothDeviceInfo
import com.learn.ndmkmp.core.bluetooth.BluetoothLookup

/**
 * Scanner screen rules per Learning Plan §4 mockup — split out of the Composable so they can be unit tested without
 * a device, and so the UI has no business `if/else`.
 *
 * ```
 *  Device Name: [ cmd      ]  ──► matchesName(device.name, query) ──► list keeps only devices with "cmd" in the name
 *  Hide unknown devices [●]   ──► isUnknownDevice(name)   ┐ isDeviceShown(): combined with the Device Name box,
 *  Hide audio devices   [●]   ──► isAudioDevice(adv)      ┘ the selected / connected device is never hidden by a switch
 *  [ Scan Bluetooth ]
 *  Discovered Devices  (tap a row = select)
 *  [ Connect ] [ Disconnect ] ◄── scannerButtonsOf(selected device, state) ──► enabled / disabled / spinner
 * ```
 *
 * The Device Name box **filters the list already seen** (filters as you type), it doesn't change how scanning works — unlike the
 * "CarMD tools only" switch (filters during the scan, in `BluetoothScanFilter`). This is exactly the Day 01 exercise
 * "filter a `List<Device>` with a lambda" brought into a real UI.
 *
 * Mirrors `ios/NativeDeviceMonitor/UI/Scanner/ScannerActions.swift`.
 */

/**
 * Empty box = show everything. With text, **unnamed** devices are hidden (no name to match).
 * "Contains" match + case-insensitive: typing `cmd` finds `CMD-3C71BF…`.
 */
fun matchesName(name: String?, query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return name?.contains(q, ignoreCase = true) == true
}

// --- 2 list filter switches: Hide unknown devices · Hide audio devices ------------------------------------

/** No name to recognise it by → *Hide unknown devices* hides it. A whitespace-only name counts as unnamed too. */
fun isUnknownDevice(name: String?): Boolean = name.isNullOrBlank()

/**
 * Headphones / speakers / earbuds — they do send BLE packets (quick-pair popup, battery) but aren't GATT devices the app needs.
 * Matching **1** sign is enough; matching none → NOT audio (better to miss one than wrongly hide a dongle).
 *
 * ```
 *  Appearance 0x084x / 0x088x / 0x094x ─┐
 *  Service LE Audio 184E 1850 1853 184F ┤
 *  Service data FE2C (Fast Pair) ───────┼─► true
 *  Apple 0x004C, type byte 0x07 ────────┤
 *  bluetoothClass AUDIO_VIDEO ──────────┘
 * ```
 * No guessing by name ("Buds", "JBL"…): too easy to hide the wrong thing. Details: `docs/bluetooth-audio-devices.md`.
 */
fun isAudioDevice(adv: BluetoothAdvertisement?): Boolean {
    if (adv == null) return false
    return adv.isAudioClass ||
        adv.appearance?.let(BluetoothLookup::isAudioAppearance) == true ||
        adv.serviceUuids.any { it in BluetoothLookup.LE_AUDIO_SERVICES } ||
        BluetoothLookup.FAST_PAIR_SERVICE in adv.serviceDataUuids ||
        adv.appleType == BluetoothLookup.APPLE_PROXIMITY_PAIRING
}

/**
 * Whether a row is shown = the Device Name box **and** the 2 switches (combined).
 * [isPinned] = selected / connected device: the switches NEVER hide it (otherwise the Disconnect button is gone).
 * The Device Name box still applies to every device — the user typed it, so they immediately see why it disappeared.
 */
fun isDeviceShown(
    device: BluetoothDeviceInfo,
    query: String,
    hideUnknown: Boolean,
    hideAudio: Boolean,
    isPinned: Boolean,
): Boolean {
    if (!matchesName(device.name, query)) return false
    if (isPinned) return true
    if (hideUnknown && isUnknownDevice(device.name)) return false
    if (hideAudio && isAudioDevice(device.advertisement)) return false
    return true
}

/** State of the 2 buttons under the list. A `data class` so tests compare with `assertEquals`. */
data class ScannerButtons(
    val connectEnabled: Boolean,
    /** Connecting → the Connect button shows a spinner (and is disabled). */
    val connecting: Boolean,
    val disconnectEnabled: Boolean,
)

/**
 * ```
 *  Connecting ─────────────────────────────────────────► Connect ⏳ (disabled) · Disconnect disabled
 *  Connected + that same device selected ──────────────► Connect disabled  · Disconnect enabled
 *  Connected + a DIFFERENT device selected ────────────► Connect enabled (service drops the old one) · Disconnect enabled
 *  nothing selected / not ready / "non-connectable" ───► Connect disabled
 * ```
 *
 * @param hasSelection a row has been tapped (and it's still in the list).
 * @param selectedIsConnectable advertising flag of the selected device. `null` (unreadable on Android < 8) =
 *   **let them try** — wrongly blocking a connectable device is worse than letting the user tap and see an error.
 * @param selectedIsConnectedDevice whether the selected device is the one in `connectedAddress`.
 * @param ready permissions + radio ready (`BleReadiness.READY`).
 */
fun scannerButtonsOf(
    hasSelection: Boolean,
    selectedIsConnectable: Boolean?,
    selectedIsConnectedDevice: Boolean,
    connection: BluetoothConnectionState,
    ready: Boolean,
): ScannerButtons {
    val isConnecting = connection == BluetoothConnectionState.Connecting
    val isConnected = connection is BluetoothConnectionState.Connected
    val alreadyOnThisDevice = isConnected && selectedIsConnectedDevice
    return ScannerButtons(
        connectEnabled = ready && hasSelection && selectedIsConnectable != false && !isConnecting && !alreadyOnThisDevice,
        connecting = isConnecting,
        disconnectEnabled = isConnected,
    )
}
