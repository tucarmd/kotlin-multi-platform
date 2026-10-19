package com.learn.ndmkmp.core.bluetooth

/**
 * Day 2 — one BLE device **seen while scanning**. A data snapshot, does NOT hold
 * the real `android.bluetooth.BluetoothDevice`.
 *
 * Why a separate class instead of passing `BluetoothDevice` straight to the UI:
 * - `BluetoothDevice` is a system object, reading `.name` needs the `BLUETOOTH_CONNECT` permission
 *   (API 31+) → a Composable reading it by accident throws `SecurityException` mid-draw.
 * - It is **not** a `data class` so it has no value-based `equals` → Compose compares by
 *   reference, the list flickers every time the scan returns new results.
 * - Rule 5.4: `ui/` shouldn't depend on Bluetooth framework types.
 *
 * `address` (MAC, e.g. `AA:BB:CC:DD:EE:FF`) is the **unique key** — the name may be null or change,
 * RSSI changes constantly. Dedupe in `BluetoothClientService` goes by address.
 *
 * *Compared to TS:* this is a plain DTO; `data class` gives `equals`/`copy` that TS has to hand-write.
 */
// 📍 P1 note: `address` is a plain String on purpose. The old Android class held `android.bluetooth.BluetoothDevice`,
// which commonMain can't see. Android puts the MAC here, iOS (P12) the CBPeripheral UUID — same field, both platforms.
data class BluetoothDeviceInfo(
    val address: String,
    /** null when the device doesn't advertise a name — UI shows "Unknown device", does NOT skip the row. */
    val name: String?,
    /** Signal strength, dBm. Closer to 0 is stronger (-40 very close, -95 about to drop). */
    val rssi: Int,
    /** Day 2: latest advertising packet — the UI shows it when the card is tapped to expand (nRF Scanner style). */
    val advertisement: BluetoothAdvertisement? = null,
    /**
     * When the latest packet arrived, in `SystemClock.elapsedRealtime()` (ms since boot) — the same clock
     * as `ScanResult.timestampNanos`. NOT `System.currentTimeMillis()`: changing the device time skews it.
     */
    val lastSeenMs: Long = 0L,
)

/**
 * Day 2 (nRF-style) — advertising packet contents, like nRF Connect's Scanner screen.
 *
 * ```
 *  ScanResult
 *   ├─ isConnectable · txPower · timestampNanos       ← measured / read by the system (API 26+)
 *   ├─ device.bondState                              ← paired with this phone yet?
 *   └─ scanRecord (raw bytes ≤ 31 bytes, + scan response)
 *        ├─ advertiseFlags        0x06 = LE General Discoverable + BR/EDR not supported
 *        ├─ serviceUuids          e.g. FFE0 — the CarMD dongle may NOT advertise a UUID (that's why Ionic filters by name)
 *        ├─ manufacturerSpecificData  2-byte company id (0x004C = Apple, 0x0059 = Nordic) + data
 *        ├─ serviceData            uuid → bytes
 *        └─ bytes                  raw — paste into nRF "Raw data" to compare byte by byte
 * ```
 * Every field is nullable/empty: if the device doesn't advertise a field it's absent, not an error.
 */
data class BluetoothAdvertisement(
    val isConnectable: Boolean?,
    val txPower: Int?,
    /** "Bonded" / "Bonding" / "Not bonded". */
    val bondState: String,
    val flags: Int?,
    val serviceUuids: List<String>,
    /** `0x004C Apple: 02 15 …` — one line per company, with the vendor name if [BluetoothLookup] knows it. */
    val manufacturerData: List<String>,
    /** `FFE0: 01 02` — one line per service. */
    val serviceData: List<String>,
    val rawHex: String,
    /** Packets received since the scan started — nRF shows it next to the interval. */
    val packetCount: Int,
    /** Average gap between 2 packets (ms), null with only 1 packet. Only an ESTIMATE: the system merges/drops packets. */
    val intervalMs: Long?,
    /** `BluetoothDevice.type`: "LE only" / "Dual mode" / "Classic" / "Unknown". iOS doesn't expose this field. */
    val deviceType: String = "Unknown",
    /** `BluetoothDevice.addressType` (API 35+): "Public" / "Random". null on older Android. */
    val addressType: String? = null,

    // --- Signs of "this is headphones / a speaker" for the *Hide audio devices* filter (see docs/bluetooth-audio-devices.md) ---

    /** Appearance (AD type `0x19`) self-declared in the packet, e.g. `0x0941` Earbud. null = not sent (most devices). */
    val appearance: Int? = null,
    /** Short UUID of the service data, e.g. `FE2C` (Google Fast Pair). */
    val serviceDataUuids: List<String> = emptyList(),
    /** First byte of Apple manufacturer data (`0x004C`) = packet type; `0x07` = AirPods/Beats proximity pairing. */
    val appleType: Int? = null,
    /** `bluetoothClass.majorDeviceClass == AUDIO_VIDEO` — the system only knows once paired / dual-mode. iOS has none. */
    val isAudioClass: Boolean = false,
)

/**
 * Day 2 — connection state. A `sealed interface` rather than an `enum` because 2 branches
 * **carry data**: `Connected` needs a name to show, `Error` needs a reason.
 *
 * ```
 *   Idle ──startScan──► (scan) ──connect()──► Connecting ──► Connected(name)
 *     ▲                                            │              │
 *     │                                            │ failure      │ disconnect() / signal lost
 *     └──────────── Disconnected ◄──────────────────┴──────────────┘
 *                        ▲
 *                     Error(message)
 * ```
 *
 * `when` over a `sealed` type is **exhaustive**: add a new branch and forget to handle it in the UI → **build error**,
 * not a runtime bug. That's why there is no hand-typed `String status`.
 *
 * *Compared to TS:* equivalent to a discriminated union `{ kind: 'connected', name: string } | ...`.
 */
sealed interface BluetoothConnectionState {

    /** Nothing yet — the screen just opened. */
    data object Idle : BluetoothConnectionState

    /** `connectGatt` called, waiting for `onConnectionStateChange`. Can sit here 5–30 s if the device is silent. */
    data object Connecting : BluetoothConnectionState

    data class Connected(val name: String) : BluetoothConnectionState

    /** Disconnected **because we tapped** — distinct from [Error] so the UI doesn't scare the user with red text. */
    data object Disconnected : BluetoothConnectionState

    /**
     * Unexpected disconnect/failure. [message] includes Android's raw `status` because that's the only thing
     * you can look up while debugging (133 = generic error, 8 = signal lost, 19 = device disconnected on purpose).
     */
    data class Error(val message: String) : BluetoothConnectionState
}
