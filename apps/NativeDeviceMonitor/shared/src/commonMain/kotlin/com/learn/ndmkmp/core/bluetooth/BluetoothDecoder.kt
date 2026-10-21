package com.learn.ndmkmp.core.bluetooth

/**
 * Day 2 + Day 3 — turn BLE data into human-readable text. Pure functions, no Context.
 *
 * Day 2: short UUIDs, Appearance, hex of the advertising packet. Day 3: [decode] characteristic bytes → value.
 * Write (task 004): the opposite direction — text the user types → bytes, [fromHex] / [fromText].
 */
object BluetoothDecoder {

    /** Bluetooth SIG base UUID: a 16-bit UUID `XXXX` is really `0000XXXX-0000-1000-8000-00805F9B34FB`. */
    private const val BASE_SUFFIX = "-0000-1000-8000-00805F9B34FB"

    /**
     * `0000FFE0-0000-1000-8000-00805F9B34FB` → `FFE0` — matches how nRF Connect shows short UUIDs.
     * Custom UUIDs (self-assigned 128-bit) stay as-is, uppercased.
     */
    fun shortUuid(uuid: String): String {
        // String in, not java.util.UUID: commonMain has no JVM. Android passes `uuid.toString()`, iOS `CBUUID.UUIDString`.
        val s = uuid.uppercase()
        return if (s.startsWith("0000") && s.endsWith(BASE_SUFFIX)) s.substring(4, 8) else s
    }

    /**
     * Read Appearance (AD type `0x19`, 2 bytes little-endian) from the raw advertising packet bytes.
     * `ScanRecord` has no getter for this field (`advertisingDataMap` only from API 33) → walk it ourselves.
     *
     * ```
     *  02 01 06   03 19 41 09   05 09 42 75 64 73
     *  └ flags ┘  └ appearance ┘ └── name "Buds" ─┘      each block: [len][type][len-1 data bytes]
     *                   0x0941 = Earbud
     * ```
     * Hit len = 0 (trailing padding) or a truncated block → stop, return null.
     */
    fun appearanceOf(bytes: ByteArray): Int? {
        var i = 0
        while (i < bytes.size) {
            val len = bytes[i].toInt() and 0xFF
            // the block spans bytes[i .. i+len] → the last byte must still be inside the array
            if (len == 0 || i + len >= bytes.size) return null
            val type = bytes[i + 1].toInt() and 0xFF
            if (type == AD_TYPE_APPEARANCE && len >= 3) {
                return (bytes[i + 2].toInt() and 0xFF) or ((bytes[i + 3].toInt() and 0xFF) shl 8)
            }
            i += len + 1
        }
        return null
    }

    private const val AD_TYPE_APPEARANCE = 0x19

    /** `byteArrayOf(0x02, 0x15)` → `02 15`. `and 0xFF` because Kotlin's Byte is signed: 0xFF is -1. No `"%02X".format` in commonMain. */
    fun toHex(bytes: ByteArray): String = bytes.joinToString(" ") { hex(it.toInt() and 0xFF, 2) }

    /** 0x0941 → "0941" with [digits] = 4. Uppercase, zero-padded. */
    private fun hex(value: Long, digits: Int): String = value.toString(16).uppercase().padStart(digits, '0')
    private fun hex(value: Int, digits: Int): String = hex(value.toLong(), digits)

    // --- Day 3: bytes → value ---------------------------------------------------------------------------

    /**
     * Read / notified bytes → text for humans. Known UUID → decode per spec, unknown → a safe guess.
     *
     * ```
     *  2A19 Battery Level      5A          → "90 %"       uint8
     *  2A29 Manufacturer Name  4E 6F 72 …  → "Nordic"     UTF-8
     *  2A37 Heart Rate         00 48       → "72 bpm"     flags bit0 = 0 → uint8 · 1 → uint16 LE
     *  (unknown)               41 54 0D    → "\"AT\\r\""    all printable → treat as text (UART dongles answer in ASCII)
     *  (unknown)               34 12       → "4660 (uint16 LE)"   1 / 2 / 4 bytes → little-endian number
     *  (unknown)               01 02 03    → "—"                  give up, hex only
     * ```
     * **Little-endian**: BLE sends the LOW byte first — `34 12` is `0x1234` = 4660, not `0x3412`.
     */
    fun decode(uuid: String, bytes: ByteArray): String {
        if (bytes.isEmpty()) return "(empty)"
        return when (uuid.uppercase()) {
            "2A19" -> "${bytes[0].toInt() and 0xFF} %"
            in UTF8_CHARACTERISTICS -> bytes.decodeToString().trimEnd('\u0000')
            "2A37" -> heartRateOf(bytes)
            "2A01" -> "0x" + hex(uintLe(bytes, 2), 4)
            else -> guess(bytes)
        }
    }

    /** Device Name · Model · Serial · Firmware · Hardware · Software · Manufacturer — the spec says UTF-8 strings. */
    private val UTF8_CHARACTERISTICS = setOf("2A00", "2A24", "2A25", "2A26", "2A27", "2A28", "2A29")

    private fun heartRateOf(bytes: ByteArray): String {
        val flags = bytes[0].toInt() and 0xFF
        val wide = flags and 0x01 != 0
        if (bytes.size < if (wide) 3 else 2) return "—"
        val bpm = if (wide) uintLe(bytes.copyOfRange(1, 3), 2) else bytes[1].toInt() and 0xFF
        return "$bpm bpm"
    }

    private fun guess(bytes: ByteArray): String {
        val printable = bytes.all { b -> val c = b.toInt() and 0xFF; c in 0x20..0x7E || c == 0x0D || c == 0x0A || c == 0x09 }
        if (printable) {
            val text = bytes.decodeToString()   // all bytes are printable ASCII here, so UTF-8 decode = ASCII
               .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t")
            return "\"$text\""
        }
        return when (bytes.size) {
            1 -> "${bytes[0].toInt() and 0xFF} (uint8)"
            2 -> "${uintLe(bytes, 2)} (uint16 LE)"
            4 -> "${uintLe(bytes, 4)} (uint32 LE)"
            else -> "—"
        }
    }

    // --- Write: text → bytes ----------------------------------------------------------------------------

    /**
     * Hex string the user typed → bytes. Accepts many styles, because people copy from nRF / Logcat / docs:
     *
     * ```
     *  "41 54 5A 0D"   "41545A0D"   "0x41,0x54,0x5A,0x0D"   "41-54-5a-0d"   → 41 54 5A 0D
     *  ""  · "4"  (odd, 1 char)  · "4G" (not hex)                             → null
     * ```
     * Strip `0x`, spaces, `,` `-` `:` then cut into 2-char pairs. null = invalid, the UI shows red.
     */
    fun fromHex(text: String): ByteArray? {
        val clean = text.replace("0x", "", ignoreCase = true).filterNot { it.isWhitespace() || it in ",-:" }
        if (clean.isEmpty() || clean.length % 2 != 0) return null
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toIntOrNull(16)?.toByte() ?: return null
        }
    }

    /**
     * Typed text → UTF-8 bytes. The input field can't type Enter as `\r` → allow **escapes** like in code:
     * `ATZ\r` (5 chars `A T Z \ r`) → `41 54 5A 0D`. Understands `\r` `\n` `\t` `\\`. Empty → null.
     *
     * ELM327 dongles (CarMD) only run a command when they see `\r` (0x0D) — without it the dongle waits and never answers.
     */
    fun fromText(text: String): ByteArray? {
        if (text.isEmpty()) return null
        val out = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\\' && i + 1 < text.length) {
                when (text[i + 1]) {
                    'r' -> { out.append('\r'); i += 2; continue }
                    'n' -> { out.append('\n'); i += 2; continue }
                    't' -> { out.append('\t'); i += 2; continue }
                    '\\' -> { out.append('\\'); i += 2; continue }
                }
            }
            out.append(c)
            i++
        }
        return out.toString().encodeToByteArray()
    }

    /** Combine the first [count] bytes as little-endian. Long so uint32 doesn't overflow into a negative like Int. */
    private fun uintLe(bytes: ByteArray, count: Int): Long {
        var v = 0L
        for (i in 0 until minOf(count, bytes.size)) v = v or ((bytes[i].toLong() and 0xFF) shl (8 * i))
        return v
    }
}
