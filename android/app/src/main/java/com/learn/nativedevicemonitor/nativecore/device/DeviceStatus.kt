package com.learn.nativedevicemonitor.nativecore.device

import org.json.JSONObject

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
 * *Compared to Swift:* `data class` ↔ `struct DeviceStatus: Codable` (iOS synthesizes encode/decode,
 * Kotlin uses `org.json` that ships with Android, no extra library).
 */
data class DeviceStatus(
    /** ISO 8601 local time with offset, e.g. `2026-10-05T09:30:00+07:00`. */
    val timestamp: String,
    val battery: Int?,
    val wifiEnabled: Boolean?,
    val wifiConnected: Boolean,
    val cellularAvailable: Boolean,
    val bluetoothEnabled: Boolean?,
    val bluetoothConnected: Boolean?,
    /**
     * Added beyond the plan: which mechanism wrote this line — `loop` · `fgs` · `worker` · `manual`
     * (iOS adds `bgRefresh`). Without this field filling in the Day 14 behaviour table means guessing from timestamps.
     */
    val source: String? = null,
) {

    /** One JSONL line. `JSONObject` keeps put order → the file reads in the plan's order by eye. */
    fun toJson(): String = JSONObject()
        .put("timestamp", timestamp)
        .put("battery", battery ?: JSONObject.NULL)
        .put("wifiEnabled", wifiEnabled ?: JSONObject.NULL)
        .put("wifiConnected", wifiConnected)
        .put("cellularAvailable", cellularAvailable)
        .put("bluetoothEnabled", bluetoothEnabled ?: JSONObject.NULL)
        .put("bluetoothConnected", bluetoothConnected ?: JSONObject.NULL)
        .apply { if (source != null) put("source", source) }
        .toString()

    companion object {

        /**
         * Parse one line. A broken line (not JSON, missing `timestamp`) → throws `org.json.JSONException`;
         * [com.learn.nativedevicemonitor.nativecore.logging.FileLogger.readLogs] catches and counts it, no crash.
         */
        fun fromJson(line: String): DeviceStatus {
            val o = JSONObject(line)
            return DeviceStatus(
                timestamp = o.getString("timestamp"),   // required — missing means a broken line
                battery = o.optIntOrNull("battery"),
                wifiEnabled = o.optBoolOrNull("wifiEnabled"),
                wifiConnected = o.optBoolean("wifiConnected", false),
                cellularAvailable = o.optBoolean("cellularAvailable", false),
                bluetoothEnabled = o.optBoolOrNull("bluetoothEnabled"),
                bluetoothConnected = o.optBoolOrNull("bluetoothConnected"),
                source = if (o.isNull("source")) null else o.optString("source"),
            )
        }

        // `optInt` returns 0 when missing/null → can't tell "0 %" from "not measured" → hand-written variant returning null.
        private fun JSONObject.optIntOrNull(key: String): Int? =
            if (isNull(key)) null else optInt(key)

        private fun JSONObject.optBoolOrNull(key: String): Boolean? =
            if (isNull(key)) null else optBoolean(key)
    }
}
