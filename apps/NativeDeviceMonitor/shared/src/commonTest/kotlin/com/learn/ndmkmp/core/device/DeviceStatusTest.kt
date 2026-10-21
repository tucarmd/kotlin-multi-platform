package com.learn.ndmkmp.core.device

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Week 3 · Day 12 — DeviceStatus ↔ JSON. The JSON cases from the old FileLoggerTest (file I/O cases move with FileLogger in P7).
 * Runs on the Android host JVM and on the iOS simulator — same test, both platforms.
 */
class DeviceStatusTest {

    private fun sample(source: String? = "loop") = DeviceStatus(
        timestamp = "2026-10-05T09:30:00+07:00",
        battery = 82,
        wifiEnabled = true,
        wifiConnected = true,
        cellularAvailable = false,
        bluetoothEnabled = true,
        bluetoothConnected = null,
        source = source,
    )

    @Test
    fun json_hasPlanFieldNames_andNullForUnmeasured() {
        val json = sample().toJson()
        listOf("timestamp", "battery", "wifiEnabled", "wifiConnected", "cellularAvailable", "bluetoothEnabled")
            .forEach { assertTrue(json.contains("\"$it\""), "missing $it in $json") }
        assertTrue(json.contains("\"bluetoothConnected\":null"), json)
    }

    @Test
    fun json_roundTrip() {
        val s = sample()
        assertEquals(s, DeviceStatus.fromJson(s.toJson()))
    }

    @Test
    fun json_withoutSource_omitsField_andParsesBack() {
        val s = sample(source = null)
        assertFalse(s.toJson().contains("source"))
        assertEquals(s, DeviceStatus.fromJson(s.toJson()))
    }

    @Test
    fun json_shortLine_missingKeysBecomeNullOrFalse() {
        val s = DeviceStatus.fromJson("""{"timestamp":"2026-10-05T09:30:00+07:00","extra":1}""")
        assertNull(s.battery)
        assertFalse(s.wifiConnected)
        assertNull(s.source)
    }

    @Test
    fun json_brokenLine_throws() {
        assertFailsWith<IllegalArgumentException> { DeviceStatus.fromJson("{not json") }
        assertFailsWith<IllegalArgumentException> { DeviceStatus.fromJson("""{"battery":50}""") }   // no timestamp
    }
}
