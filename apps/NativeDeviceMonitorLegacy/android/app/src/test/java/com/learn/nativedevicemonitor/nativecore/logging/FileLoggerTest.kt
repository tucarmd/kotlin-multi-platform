package com.learn.nativedevicemonitor.nativecore.logging

import com.learn.nativedevicemonitor.nativecore.device.DeviceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Week 3 · Day 12–13 — DeviceStatus ↔ JSON + FileLogger append / read / clear / broken lines.
 * Same cases as `ios/NativeDeviceMonitorTests/FileLoggerTests.swift`. Runs on the JVM (temp file), no device needed.
 */
class FileLoggerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun sample(ts: String = "2026-10-05T09:30:00+07:00", source: String? = "loop") = DeviceStatus(
        timestamp = ts,
        battery = 82,
        wifiEnabled = true,
        wifiConnected = true,
        cellularAvailable = false,
        bluetoothEnabled = true,
        bluetoothConnected = null,
        source = source,
    )

    private fun logger(): Pair<FileLogger, File> {
        val file = File(tmp.root, FileLogger.FILE_NAME)
        return FileLogger(file) to file
    }

    @Test
    fun json_hasPlanFieldNames_andNullForUnmeasured() {
        val json = sample().toJson()
        listOf("timestamp", "battery", "wifiEnabled", "wifiConnected", "cellularAvailable", "bluetoothEnabled")
            .forEach { assertTrue("missing $it in $json", json.contains("\"$it\"")) }
        assertTrue(json.contains("\"bluetoothConnected\":null"))
    }

    @Test
    fun json_roundTrip() {
        val s = sample()
        assertEquals(s, DeviceStatus.fromJson(s.toJson()))
    }

    @Test
    fun json_withoutSource_omitsField_andParsesBack() {
        val s = sample(source = null)
        assertTrue(!s.toJson().contains("source"))
        assertEquals(s, DeviceStatus.fromJson(s.toJson()))
    }

    @Test
    fun read_missingFile_isEmpty_notError() {
        val (log, _) = logger()
        val r = log.readLogs()
        assertEquals(0, r.records.size)
        assertEquals(0, r.corruptLines)
        assertNull(r.error)
    }

    @Test
    fun append3_writes3Lines_inOrder() {
        val (log, file) = logger()
        repeat(3) { i -> assertTrue(log.append(sample(ts = "2026-10-05T09:30:0$i+07:00"))) }
        assertEquals(3, file.readLines().size)
        val r = log.readLogs()
        assertEquals(listOf("00", "01", "02"), r.records.map { it.timestamp.substring(17, 19) })
    }

    @Test
    fun read_skipsCorruptLines_andCountsThem() {
        val (log, file) = logger()
        log.append(sample())
        file.appendText("not json\n")
        file.appendText("{\"battery\":1}\n")      // valid JSON but missing timestamp → broken
        file.appendText("\n")                      // empty line: skipped, not counted as broken
        log.append(sample())
        val r = log.readLogs()
        assertEquals(2, r.records.size)
        assertEquals(2, r.corruptLines)
    }

    @Test
    fun clear_emptiesFile_andAppendStillWorks() {
        val (log, _) = logger()
        log.append(sample())
        assertTrue(log.clearLogs())
        assertEquals(0, log.readLogs().records.size)
        log.append(sample())
        assertEquals(1, log.readLogs().records.size)
    }

    @Test
    fun clear_missingFile_isOk() {
        val (log, _) = logger()
        assertTrue(log.clearLogs())
    }
}
