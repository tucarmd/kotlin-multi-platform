package com.learn.nativedevicemonitor.nativecore.bluetooth

/**
 * Day 3 + Day 4 — snapshot of the GATT tree after connect + read / notified values. Pure functions, no Context,
 * **does not hold** `BluetoothGattCharacteristic` (same reason [BluetoothDeviceInfo] doesn't hold `BluetoothDevice`).
 *
 * ```
 *  Peripheral (GATT server)
 *   └─ GattServiceInfo  180F Battery
 *        └─ GattCharacteristicInfo  2A19 Battery Level   properties = 0x12 → READ + NOTIFY
 *              ├─ read()        → GattValue(source = READ,   hex "5A", decoded "90 %")
 *              └─ setNotify(on) → GattValue(source = NOTIFY, …) every time the peripheral pushes
 *   └─ GattServiceInfo  FFE0 (dongle CarMD)
 *        └─ GattCharacteristicInfo  FFE1   properties = 0x1C → WRITE + WRITE NO RESP + NOTIFY
 *              └─ write(bytes)  → GattValue(source = WRITE,  hex "41 54 5A 0D", decoded "\"ATZ\\r\"")   (task 004)
 * ```
 *
 * UUIDs stay as short strings (`2A19`, `FFE1`) exactly as [BluetoothDecoder.shortUuid] — the same string as iOS
 * `CBUUID.uuidString`, so rules + tests are identical on both sides. Mirrors `ios/NativeDeviceMonitor/NativeCore/Bluetooth/GattModel.swift`.
 */

/**
 * A characteristic's properties = a **1-byte bitmask** declared by the peripheral. Android (`PROPERTY_*`) and iOS
 * (`CBCharacteristicProperties`) use **the same bit values** from the Bluetooth spec → one shared parser.
 *
 * ```
 *  bit  0x02 READ · 0x04 WRITE_NO_RESPONSE · 0x08 WRITE · 0x10 NOTIFY · 0x20 INDICATE
 *  vd 0x12 = 0001 0010 = NOTIFY + READ
 * ```
 */
data class GattProperties(
    val read: Boolean = false,
    val write: Boolean = false,
    val writeNoResponse: Boolean = false,
    val notify: Boolean = false,
    val indicate: Boolean = false,
) {
    /** Notify and indicate are both enabled via CCCD `2902` — indicate adds an ACK (slower, more reliable). */
    val canSubscribe: Boolean get() = notify || indicate

    /** Writable by either type. UART dongles (`FFE1`) usually declare both. */
    val canWrite: Boolean get() = write || writeNoResponse

    /** `READ NOTIFY` — label printed next to the characteristic, like nRF Connect's Properties column. */
    val labels: List<String>
        get() = buildList {
            if (read) add("READ")
            if (write) add("WRITE")
            if (writeNoResponse) add("WRITE NO RESP")
            if (notify) add("NOTIFY")
            if (indicate) add("INDICATE")
        }

    companion object {
        const val READ = 0x02
        const val WRITE_NO_RESPONSE = 0x04
        const val WRITE = 0x08
        const val NOTIFY = 0x10
        const val INDICATE = 0x20

        fun of(raw: Int) = GattProperties(
            read = raw and READ != 0,
            write = raw and WRITE != 0,
            writeNoResponse = raw and WRITE_NO_RESPONSE != 0,
            notify = raw and NOTIFY != 0,
            indicate = raw and INDICATE != 0,
        )
    }
}

data class GattCharacteristicInfo(
    val serviceUuid: String,
    val uuid: String,
    val properties: GattProperties,
    /** CCCD enabled (peripheral confirmed) — the UI turns the Notify button into Stop. */
    val isNotifying: Boolean = false,
) {
    /** Unique key within one connection. The same characteristic UUID may live in 2 different services (rare) → combine both. */
    val key: String get() = "$serviceUuid/$uuid"
}

data class GattServiceInfo(
    val uuid: String,
    val characteristics: List<GattCharacteristicInfo>,
)

/** Where the value came from — all in one "Received Data" box: read = we asked, notify = the peripheral pushed, write = we sent. */
enum class GattValueSource { READ, NOTIFY, WRITE }

/**
 * One batch of received bytes. Stores [hex] + [decoded] (computed once on arrival) instead of a `ByteArray`:
 * `ByteArray` has no value-based `equals` → the `data class` compares wrong, StateFlow/Compose redraw needlessly.
 */
data class GattValue(
    val serviceUuid: String,
    val uuid: String,
    val hex: String,
    val decoded: String,
    val source: GattValueSource,
    /** Wall-clock time (`System.currentTimeMillis`) — only for printing `12:03:04.123`, not for measuring intervals. */
    val atMs: Long,
)

/** Max lines in *Received Data*. Notify at 20 packets/second without a cap → the list bloats, the UI slows down. */
const val RECEIVED_LOG_MAX = 50

/** Append at the END, drop the OLDEST lines past [max]. Always returns a new List (StateFlow compares with equals). */
fun <T> appendCapped(list: List<T>, item: T, max: Int = RECEIVED_LOG_MAX): List<T> =
    (list + item).takeLast(max)

// --- Target characteristic (Day 3 target: "can locate a target characteristic") -------------------------

/**
 * Priority when picking a characteristic to **Read** as a test: battery (1 byte, easy to decode) → manufacturer → device name.
 * None of those → the first READ characteristic. Nothing readable → null.
 */
val READ_TARGET_PRIORITY = listOf("2A19", "2A29", "2A00")

/**
 * Priority when picking a characteristic to **Notify**: the CarMD dongle UART port (`FFE1`, in service `FFE0`) →
 * Nordic UART TX (common on nRF / ESP32) → Heart Rate → Battery. None → the first notify/indicate one.
 */
val NOTIFY_TARGET_PRIORITY = listOf("FFE1", "6E400003-B5A3-F393-E0A9-E50E24DCCA9E", "2A37", "2A19")

/**
 * Priority when picking a characteristic to **Write**: the CarMD dongle UART port `FFE1` (same characteristic as notify —
 * HM-10 dongles use one slot for both directions) → Nordic UART **RX** `6E400002` (app → device) → the first writable one.
 */
val WRITE_TARGET_PRIORITY = listOf("FFE1", "6E400002-B5A3-F393-E0A9-E50E24DCCA9E")

fun readTargetOf(services: List<GattServiceInfo>): GattCharacteristicInfo? =
    pickTarget(services, READ_TARGET_PRIORITY) { it.properties.read }

fun notifyTargetOf(services: List<GattServiceInfo>): GattCharacteristicInfo? =
    pickTarget(services, NOTIFY_TARGET_PRIORITY) { it.properties.canSubscribe }

fun writeTargetOf(services: List<GattServiceInfo>): GattCharacteristicInfo? =
    pickTarget(services, WRITE_TARGET_PRIORITY) { it.properties.canWrite }

private fun pickTarget(
    services: List<GattServiceInfo>,
    priority: List<String>,
    usable: (GattCharacteristicInfo) -> Boolean,
): GattCharacteristicInfo? {
    val candidates = services.flatMap { it.characteristics }.filter(usable)
    return priority.firstNotNullOfOrNull { wanted -> candidates.firstOrNull { it.uuid.equals(wanted, ignoreCase = true) } }
        ?: candidates.firstOrNull()
}

// --- MTU (task 004) --------------------------------------------------------------------------------------

/**
 * MTU = the maximum size of **1 ATT packet** both sides agree on. Before negotiation the spec mandates **23**.
 *
 * ```
 *  ┌──────── 1 ATT packet = MTU bytes ────┐
 *  │ opcode 1 │ handle 2 │  data MTU-3     │     MTU 23  → 20 data bytes / packet
 *  └──────────┴──────────┴─────────────────┘     MTU 247 → 244 bytes (fits 1 Data Length Extension packet of 251)
 * ```
 */
const val ATT_DEFAULT_MTU = 23

/** Android requests this MTU after discovery. 247 = 251 (largest link-layer packet with DLE) − 4 bytes L2CAP header. */
const val ATT_REQUESTED_MTU = 247

/** The first 3 bytes of every packet are opcode + handle — not our data. */
const val ATT_HEADER_BYTES = 3

fun maxPayloadOf(mtu: Int): Int = (mtu - ATT_HEADER_BYTES).coerceAtLeast(0)

// --- Write plan (task 004) -------------------------------------------------------------------------------

/**
 * Result of checking the Write box **before** sending. The UI uses it for the preview / red text and to disable Send —
 * the rules live here (testable), the UI just prints.
 */
sealed interface WritePlan {
    /** [withResponse] = write with ACK (peripheral confirms). false = "fire and forget" write, faster. */
    class Ok(val bytes: ByteArray, val withResponse: Boolean) : WritePlan

    data class Invalid(val reason: String) : WritePlan
}

/**
 * All the rules of the Send button:
 * 1. the characteristic must have WRITE or WRITE NO RESP
 * 2. the input converts to bytes ([BluetoothDecoder.fromHex] / [BluetoothDecoder.fromText])
 * 3. byte count ≤ [maxPayload] (= `MTU − 3`) — no splitting into packets, too long is simply blocked to keep it simple
 * 4. has WRITE → write **with response** (know for sure the peripheral got it); only NO RESP → without response
 */
fun planWrite(input: String, asHex: Boolean, properties: GattProperties, maxPayload: Int): WritePlan {
    if (!properties.canWrite) return WritePlan.Invalid("This characteristic is not writable")
    if (input.isEmpty()) return WritePlan.Invalid("Type something to send")
    val bytes = if (asHex) BluetoothDecoder.fromHex(input) else BluetoothDecoder.fromText(input)
    if (bytes == null) return WritePlan.Invalid("Invalid hex — use pairs like 41 54 5A 0D")
    if (bytes.size > maxPayload) {
        return WritePlan.Invalid("${bytes.size} bytes > max $maxPayload for MTU ${maxPayload + ATT_HEADER_BYTES}")
    }
    return WritePlan.Ok(bytes, withResponse = properties.write)
}
