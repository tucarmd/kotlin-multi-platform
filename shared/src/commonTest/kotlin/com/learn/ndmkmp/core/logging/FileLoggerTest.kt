package com.learn.ndmkmp.core.logging

import com.learn.ndmkmp.core.device.DeviceStatus
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readLine
import kotlinx.io.writeString
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** P7 — the old FileLoggerTest (JUnit TemporaryFolder) ported: a temp file per test, same cases, JVM + iOS. */
class FileLoggerTest {

    private val dir = Path(SystemTemporaryDirectory, "ndm-test-${Random.nextLong().toULong()}")
    private val file = Path(dir, FileLogger.FILE_NAME)
    private val log = FileLogger(file)

    @AfterTest
    fun cleanUp() {
        if (SystemFileSystem.exists(file)) SystemFileSystem.delete(file)
        if (SystemFileSystem.exists(dir)) SystemFileSystem.delete(dir)
    }

    private fun sample(ts: String = "2026-10-05T09:30:00+07:00") = DeviceStatus(
        timestamp = ts, battery = 82, wifiEnabled = true, wifiConnected = true, cellularAvailable = false,
        bluetoothEnabled = true, bluetoothConnected = null, source = "loop",
    )

    private fun rawAppend(text: String) = SystemFileSystem.sink(file, append = true).buffered().use { it.writeString(text) }

    private fun lineCount() = SystemFileSystem.source(file).buffered().use { s -> generateSequence { s.readLine() }.count() }

    @Test
    fun read_missingFile_isEmpty_notError() = runTest {
        val r = log.readLogs()
        assertEquals(0, r.records.size)
        assertEquals(0, r.corruptLines)
        assertNull(r.error)
    }

    @Test
    fun append3_writes3Lines_inOrder() = runTest {
        repeat(3) { i -> assertTrue(log.append(sample(ts = "2026-10-05T09:30:0$i+07:00"))) }
        assertEquals(3, lineCount())
        assertEquals(listOf("00", "01", "02"), log.readLogs().records.map { it.timestamp.substring(17, 19) })
    }

    @Test
    fun read_skipsCorruptLines_andCountsThem() = runTest {
        log.append(sample())
        rawAppend("not json\n")
        rawAppend("{\"battery\":1}\n")      // valid JSON but missing timestamp → broken
        rawAppend("\n")                      // empty line: skipped, not counted as broken
        log.append(sample())
        val r = log.readLogs()
        assertEquals(2, r.records.size)
        assertEquals(2, r.corruptLines)
    }

    @Test
    fun clear_emptiesFile_andAppendStillWorks() = runTest {
        log.append(sample())
        assertTrue(log.clearLogs())
        assertEquals(0, log.readLogs().records.size)
        log.append(sample())
        assertEquals(1, log.readLogs().records.size)
    }

    @Test
    fun clear_missingFile_isOk() = runTest {
        assertTrue(log.clearLogs())
    }
}
