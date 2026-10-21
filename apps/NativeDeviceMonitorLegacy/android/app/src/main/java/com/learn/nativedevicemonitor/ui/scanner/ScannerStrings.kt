package com.learn.nativedevicemonitor.ui.scanner

/**
 * Day 2 — Scanner screen strings (Rule 4: UI in English · Rule 5.3: Kotlin `object`, no strings.xml).
 * Scan + connect only. Labels follow the Learning Plan §4 mockup (Device Name · Scan Bluetooth · …).
 */
object ScannerStrings {
    const val HINT = "Turn on a BLE peripheral first (nRF Connect > Advertiser on another phone, or an ESP32). " +
        "Scan stops automatically after 10 seconds."

    // --- Not ready ---
    const val NOT_READY_TITLE = "Not ready"
    const val NOT_READY_PERMISSION = "Bluetooth permission is missing — grant BLE Scan and BLE Connect first."
    const val NOT_READY_RADIO_OFF = "Bluetooth is off — turn it on, then come back."
    const val NOT_READY_UNSUPPORTED = "This device has no Bluetooth adapter."
    const val OPEN_PERMISSIONS_BUTTON = "Open Permissions"

    // --- Mockup Learning Plan §4 ---
    const val DEVICE_NAME_LABEL = "Device Name"
    const val SEARCH_PLACEHOLDER = "Search device…"
    const val SCAN_BUTTON = "Scan Bluetooth"
    const val STOP_SCAN_BUTTON = "Stop scan"
    const val DEVICES_TITLE = "Discovered Devices"
    /** `3 of 7` when the Device Name box is filtering. */
    fun matchCountFormat(shown: Int, total: Int) = "$shown of $total"
    const val NO_DEVICES = "No devices yet — tap Scan Bluetooth"
    fun noMatchFormat(query: String) = "No device name contains \"$query\""
    const val UNKNOWN_DEVICE = "Unknown device"
    const val CONNECTED_TAG = "CONNECTED"
    fun rssiFormat(rssi: Int) = "$rssi dBm"
    fun scanningFormat(count: Int) = "Scanning… $count found"
    const val CONNECT_BUTTON = "Connect"
    const val DISCONNECT_BUTTON = "Disconnect"

    // --- Status ---
    fun statusFormat(label: String) = "Status: $label"
    const val STATE_IDLE = "Idle — select a device, then Connect"
    const val STATE_CONNECTING = "Connecting…"
    const val STATE_DISCONNECTED = "Disconnected"
    fun stateConnectedFormat(name: String) = "Connected to $name"

    // --- GATT (Day 3 + Day 4) ---
    const val GATT_TITLE = "GATT"
    const val GATT_NOT_CONNECTED = "Connect a device to list its services and characteristics."
    const val GATT_DISCOVERING = "Discovering services…"
    const val GATT_EMPTY = "No services found."
    fun servicesCountFormat(services: Int, characteristics: Int) = "$services services · $characteristics characteristics"
    /** `180F Battery` — unknown name → just the UUID. */
    fun uuidWithName(uuid: String, name: String?) = name?.let { "$uuid $it" } ?: uuid
    const val TARGET_TAG = "★ target"
    fun readTargetFormat(label: String?) = "Read target: " + (label ?: "none (nothing readable)")
    fun notifyTargetFormat(label: String?) = "Notify target: " + (label ?: "none (nothing notifiable)")
    const val READ_BUTTON = "Read"
    const val NOTIFY_BUTTON = "Notify"
    const val STOP_NOTIFY_BUTTON = "Stop"
    fun lastReadTitleFormat(label: String) = "Last read: $label"
    fun rawFormatLine(hex: String) = "raw:   " + hex.ifEmpty { "(empty)" }
    fun decodedFormatLine(value: String) = "value: $value"

    // --- Write + MTU (task 004) ---
    fun writeTargetFormat(label: String?) = "Write target: " + (label ?: "none (nothing writable)")
    fun mtuFormat(mtu: Int, max: Int) = "MTU $mtu · max $max bytes per packet"
    const val WRITE_BUTTON = "Write"
    const val WRITE_TITLE = "Write"
    fun writeToFormat(label: String, withResponse: Boolean) =
        "To $label · " + if (withResponse) "with response" else "without response"
    const val MODE_TEXT = "Text"
    const val MODE_HEX = "Hex"
    const val WRITE_PLACEHOLDER_TEXT = "ATZ\\r"
    const val WRITE_PLACEHOLDER_HEX = "41 54 5A 0D"
    /** Common ELM327 commands: reset · ask version · engine RPM (PID 0C). Tap = fill the box, not sent yet. */
    val WRITE_PRESETS = listOf("ATZ\\r", "ATI\\r", "010C\\r")
    const val WRITE_ESCAPE_HINT = "Text: type \\r \\n \\t for CR / LF / tab — ELM327 dongles only answer after \\r."
    const val SEND_BUTTON = "Send"
    fun writePreviewFormat(hex: String, count: Int) = "→ $hex ($count bytes)"

    // --- Received Data ---
    const val RECEIVED_TITLE = "Received Data"
    /** Empty box: tells the user what to do to get data. */
    const val RECEIVED_PLACEHOLDER = "— nothing yet: connect, then Read or Notify a characteristic"
    const val CLEAR_BUTTON = "Clear"
    fun receivedCountFormat(count: Int, max: Int) = "$count / $max"
    /** `12:03:04.123 NOTIFY FFE1  41 54 0D → "AT\r"` */
    fun receivedLineFormat(time: String, source: String, uuid: String, hex: String, decoded: String) =
        "$time $source $uuid  $hex → $decoded"

    // --- Filter + device information (nRF Scanner style) ---
    const val FILTER_TOGGLE = "CarMD tools only"
    fun filterOnFormat(label: String) = "Showing only $label"
    const val FILTER_OFF = "Showing every BLE device"
    fun hiddenFormat(count: Int) = "$count other devices hidden by the filter"
    const val HIDE_UNKNOWN_TOGGLE = "Hide unknown devices"
    const val HIDE_AUDIO_TOGGLE = "Hide audio devices (headphones, speakers)"
    /** The list has devices but the 2 switches hide them all. */
    fun allHiddenFormat(count: Int) = "All $count devices are hidden — turn a filter off"
    const val TAP_TO_EXPAND = "Tap a device to select it · tap again for advertising data. Faded rows have not been heard for 5 s."
    fun connectableFormat(value: Boolean?) = "Connectable: " + when (value) { true -> "yes"; false -> "no"; null -> "n/a" }
    fun txPowerFormat(dbm: Int?) = "TX power: " + (dbm?.let { "$it dBm" } ?: "n/a")
    fun intervalFormat(ms: Long?, packets: Int) = "Interval: " + (ms?.let { "~$it ms" } ?: "n/a") + " · $packets packets"
    fun lastSeenFormat(secondsAgo: Long) = "Last seen: " + if (secondsAgo <= 0) "just now" else "${secondsAgo}s ago"
    fun deviceTypeFormat(type: String) = "Device type: $type"
    fun addressTypeFormat(type: String?) = "Address type: " + (type ?: "n/a (Android 15+)")
    fun bondFormat(state: String) = "Bond: $state"
    fun flagsFormat(flags: Int?) = "Flags: " + (flags?.let { "0x%02X".format(it) } ?: "n/a")
    /** `Services: 180F Battery, FFE0 UART (CarMD dongle)` — names from `BluetoothLookup`, unknown → just the UUID. */
    fun servicesFormat(uuids: List<String>, nameOf: (String) -> String?) =
        "Services: " + uuids.map { u -> nameOf(u)?.let { "$u $it" } ?: u }.ifEmpty { listOf("none") }.joinToString(", ")
    fun manufacturerFormat(line: String) = "Manufacturer $line"
    fun serviceDataFormat(line: String) = "Service data $line"
    fun rawFormat(hex: String) = "Raw: " + hex.ifEmpty { "(empty)" }
}
