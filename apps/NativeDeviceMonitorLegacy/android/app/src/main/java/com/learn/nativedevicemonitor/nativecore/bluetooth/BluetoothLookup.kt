package com.learn.nativedevicemonitor.nativecore.bluetooth

/**
 * Day 2 (device information) — static lookups so the device card is readable by eye, like nRF Connect.
 * Pure functions, no Context, **no network calls** — small hand-copied tables, enough for devices common in testing.
 *
 * ```
 *  manufacturer data  4C 00 02 15 …   → company id 0x004C (little-endian) → "Apple"
 *  service UUID       0000180F-…-FB   → shortUuid "180F"                  → "Battery"
 *  rssi               -58 dBm         → BleSignal.STRONG (3 bars, green)
 *  lastSeen           7 s ago         → isStale = true (card fades while scanning)
 * ```
 *
 * Mirrors `ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothLookup.swift` — same tables, same thresholds.
 * Full Bluetooth SIG tables: https://www.bluetooth.com/specifications/assigned-numbers/
 */
object BluetoothLookup {

    /**
     * Company identifier (first 2 bytes of manufacturer data). Only vendors common around a desk;
     * not in the table → `null`, the UI just shows the hex number as before.
     */
    private val companies = mapOf(
        0x0002 to "Intel",
        0x0006 to "Microsoft",
        0x000A to "Qualcomm",
        0x000D to "Texas Instruments",
        0x000F to "Broadcom",
        0x004C to "Apple",
        0x0059 to "Nordic Semiconductor",
        0x0075 to "Samsung",
        0x0087 to "Garmin",
        0x00E0 to "Google",
        0x0171 to "Amazon",
        0x02E5 to "Espressif",
        0x038F to "Xiaomi",
    )

    /**
     * Short service UUID (exactly the string [BluetoothDecoder.shortUuid] returns) → name.
     * `FFE0` isn't a SIG standard — it's the HM-10 style UART port the CarMD dongle uses.
     */
    private val services = mapOf(
        "1800" to "Generic Access",
        "1801" to "Generic Attribute",
        "180A" to "Device Information",
        "180D" to "Heart Rate",
        "180F" to "Battery",
        "1812" to "HID",
        "181A" to "Environmental Sensing",
        "FE59" to "Nordic DFU",
        "FEAA" to "Eddystone",
        "FD6F" to "Exposure Notification",
        "FFE0" to "UART (CarMD dongle)",
        "6E400001-B5A3-F393-E0A9-E50E24DCCA9E" to "Nordic UART",
    )

    /**
     * Day 3 — short characteristic UUID → name, so the GATT list is readable by eye. `FFE1` is the UART data slot
     * (both write and notify) in service `FFE0` of the CarMD / HM-10 dongle.
     */
    private val characteristics = mapOf(
        "2A00" to "Device Name",
        "2A01" to "Appearance",
        "2A04" to "Connection Parameters",
        "2A05" to "Service Changed",
        "2A19" to "Battery Level",
        "2A24" to "Model Number",
        "2A25" to "Serial Number",
        "2A26" to "Firmware Revision",
        "2A27" to "Hardware Revision",
        "2A28" to "Software Revision",
        "2A29" to "Manufacturer Name",
        "2A37" to "Heart Rate Measurement",
        "2A38" to "Body Sensor Location",
        "2AA6" to "Central Address Resolution",
        "FFE1" to "UART data (CarMD dongle)",
        "6E400002-B5A3-F393-E0A9-E50E24DCCA9E" to "Nordic UART RX (write)",
        "6E400003-B5A3-F393-E0A9-E50E24DCCA9E" to "Nordic UART TX (notify)",
    )

    fun companyName(id: Int): String? = companies[id]

    /** Accepts lowercase too — iOS/Android uppercase in some places, not in others. */
    fun serviceName(shortUuid: String): String? = services[shortUuid.uppercase()]

    fun characteristicName(shortUuid: String): String? = characteristics[shortUuid.uppercase()]

    /**
     * No packet for more than 5 s **while scanning** → treat it as gone / stopped advertising, the card fades.
     * 5 s rather than 10 s: each scan only lasts 10 s, a 10 s threshold would never get to fade.
     * After the scan nothing can be concluded (we stopped listening, the device didn't vanish) → UI only applies it while scanning.
     */
    const val STALE_AFTER_MS = 5_000L

    fun isStale(lastSeenMs: Long, nowMs: Long): Boolean = nowMs - lastSeenMs > STALE_AFTER_MS

    // --- Headphones / speakers (*Hide audio devices* filter) — details: docs/bluetooth-audio-devices.md ----------

    const val APPLE_COMPANY_ID = 0x004C

    /** Type byte in Apple manufacturer data: `0x07` = proximity pairing (AirPods/Beats popup). `0x12` Find My is NOT audio. */
    const val APPLE_PROXIMITY_PAIRING = 0x07

    /** Services only LE Audio devices have: ASCS · PACS · CAP · BASS. */
    val LE_AUDIO_SERVICES = setOf("184E", "1850", "1853", "184F")

    /** Google Fast Pair — Android earbuds advertise it for the quick-pair popup (trackers use it too; never a dongle). */
    const val FAST_PAIR_SERVICE = "FE2C"

    /**
     * Appearance = 10 bit category + 6 bit sub-category → `value shr 6` ra category.
     * `0x021` Audio Sink (loa, 0x0840…) · `0x022` Audio Source (0x0880…) · `0x025` Wearable Audio (earbud/headset/headphones, 0x0940…).
     */
    fun isAudioAppearance(value: Int): Boolean = (value shr 6) in setOf(0x021, 0x022, 0x025)
}

/**
 * Signal strength → number of bars, like nRF Connect's signal icon.
 * Thresholds: ≥ -60 strong (same desk) · -60…-80 medium (same room) · < -80 weak (about to drop, connect likely fails).
 */
enum class BleSignal(val bars: Int) {
    STRONG(3),
    MEDIUM(2),
    WEAK(1);

    companion object {
        fun of(rssi: Int): BleSignal = when {
            rssi >= -60 -> STRONG
            rssi >= -80 -> MEDIUM
            else -> WEAK
        }
    }
}
