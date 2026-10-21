package com.learn.ndmkmp.core.background

import com.learn.ndmkmp.core.device.DeviceStatus
import com.learn.ndmkmp.core.logging.FileLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** P7 — loop rules shared by both platforms: Start twice ≠ two loops, Stop stops, FGS stop goes through the platform. */
class LoopDeviceMonitorTest {

    private val file = Path(SystemTemporaryDirectory, "ndm-loop-${Random.nextLong().toULong()}.jsonl")

    @AfterTest
    fun cleanUp() {
        if (SystemFileSystem.exists(file)) SystemFileSystem.delete(file)
    }

    private class RecordingBackground : PlatformBackground {
        override val supportsForegroundService = true
        override val periodicState = MutableStateFlow<String?>(null)
        var fgsStopped = 0
        override fun stopForegroundService() { fgsStopped++ }
        override fun schedulePeriodic() { periodicState.value = "ENQUEUED" }
        override fun cancelPeriodic() { periodicState.value = null }
    }

    private val background = RecordingBackground()
    private val monitor = LoopDeviceMonitor({ DeviceStatus(timestamp = "2026-10-16T10:00:00+07:00", battery = 50) }, FileLogger(file), background)

    @Test
    fun startTwice_keepsOneLoop() {
        assertTrue(monitor.runLoop(MonitorMode.IN_APP_LOOP, 60_000))
        assertFalse(monitor.runLoop(MonitorMode.IN_APP_LOOP, 60_000))
        assertTrue(monitor.status.value.isRunning)
        monitor.stop()
        assertFalse(monitor.status.value.isRunning)
        assertEquals(0, background.fgsStopped)          // in-app loop: no service to stop
    }

    @Test
    fun stop_whenFgsOwnedLoop_stopsService() {
        monitor.runLoop(MonitorMode.FOREGROUND_SERVICE, 60_000)
        monitor.stop()
        assertEquals(1, background.fgsStopped)
    }

    @Test
    fun collectAndSave_stampsSource_andWritesLine() = runTest {
        val r = monitor.collectAndSave("manual")
        assertEquals("manual", r?.source)
        assertEquals(listOf("manual"), monitor.getLogs().records.map { it.source })
    }
}
