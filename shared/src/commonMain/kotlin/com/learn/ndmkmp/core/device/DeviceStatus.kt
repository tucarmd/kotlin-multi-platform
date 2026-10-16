package com.learn.ndmkmp.core.device

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Week 3 · Day 12 — one device-state record, using the plan's exact field names (section 9 *Background Device Monitor*):
 *
 * ```json
 * { "timestamp": "2026-09-11T10:30:00", "battery": 82, "wifiEnabled": true, "wifiConnected": true,
 *   "cellularAvailable": true, "bluetoothEnabled": true, "bluetoothConnected": false }
 * ```
 *
 * Fields that **can't be read** are `null` (written to JSON as `null`), NOT guessed as `false`:
 * - `bluetoothConnected` needs `BLUETOOTH_CONNECT` on API 31+ → permission not granted = `null`.
 * - `battery` rarely fails but `BatteryManager` may return `Int.MIN_VALUE` on odd emulators.
 * `false` means "measured, it's off" — mix the two meanings and reading the log leads to wrong conclusions.
 *
 * *KMP:* the old Android app used `org.json` (Android-only) and iOS used `Codable`. Here one `@Serializable`
 * class serves both — the same JSONL file format as before.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeviceStatus(
    /** ISO 8601 local time with offset, e.g. `2026-10-05T09:30:00+07:00`. Required: missing = broken line. */
    val timestamp: String,
    // Defaults below only matter when READING an old/short line (missing key → null / false, like org.json's opt*).
    val battery: Int? = null,
    val wifiEnabled: Boolean? = null,
    val wifiConnected: Boolean = false,
    val cellularAvailable: Boolean = false,
    val bluetoothEnabled: Boolean? = null,
    val bluetoothConnected: Boolean? = null,
    /**
     * Added beyond the plan: which mechanism wrote this line — `loop` · `fgs` · `worker` · `manual`
     * (iOS adds `bgRefresh`). Without this field filling in the Day 14 behaviour table means guessing from timestamps.
     * Omitted from the JSON when null (NEVER), unlike the fields above which are written as `null`.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val source: String? = null,
) {

    /** One JSONL line. Keys come out in declaration order → the file reads in the plan's order by eye. */
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {

        // encodeDefaults = true → `"bluetoothConnected":null` is still written; only `source` opts out (NEVER).
        // ignoreUnknownKeys → a newer app version adding a field doesn't break reading old logs.
        private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

        /**
         * Parse one line. A broken line (not JSON, missing `timestamp`) → throws `SerializationException`
         * (an `IllegalArgumentException`); FileLogger.readLogs (P7) catches and counts it, no crash.
         */
        fun fromJson(line: String): DeviceStatus = json.decodeFromString(serializer(), line)
    }
}
