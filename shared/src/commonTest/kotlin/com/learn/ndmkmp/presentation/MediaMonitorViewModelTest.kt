package com.learn.ndmkmp.presentation

import com.learn.ndmkmp.fake.FakeDeviceMonitor
import com.learn.ndmkmp.fake.FakeMediaPicker
import com.learn.ndmkmp.presentation.media.MediaStrings
import com.learn.ndmkmp.presentation.media.MediaViewModel
import com.learn.ndmkmp.presentation.monitor.MonitorStrings
import com.learn.ndmkmp.presentation.monitor.MonitorViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MediaMonitorViewModelTest : MainDispatcherTest() {

    @Test
    fun media_captureAsksFirst_thenShowsImage_thirdCallIsCancelled() = runTest(dispatcher) {
        val vm = MediaViewModel(FakeMediaPicker())
        assertEquals(MediaStrings.REQUEST_CAMERA, vm.state.value.captureButtonText)

        vm.onCaptureClick()
        advanceUntilIdle()
        assertTrue(vm.state.value.cameraGranted)
        assertEquals(MediaStrings.CAPTURE, vm.state.value.captureButtonText)
        val image = assertNotNull(vm.state.value.image)
        assertEquals('B'.code.toByte(), image.preview!![0])       // fake picture is a BMP
        assertEquals("camera", vm.state.value.infoRows.first().value)

        vm.onPickClick(); advanceUntilIdle()
        vm.onPickClick(); advanceUntilIdle()                        // FakeMediaPicker: every 3rd call = cancel
        assertEquals(MediaStrings.PICK_CANCELLED, vm.state.value.message)
        vm.clear()
    }

    @Test
    fun monitor_refreshFillsRows_appendReloadsLogs() = runTest(dispatcher) {
        val vm = MonitorViewModel(FakeDeviceMonitor(supportsForegroundService = false))
        advanceUntilIdle()
        assertEquals(MonitorStrings.STOPPED, vm.state.value.runningText)
        assertTrue(vm.state.value.currentRows.isEmpty())

        vm.refresh()
        advanceUntilIdle()
        assertEquals(7, vm.state.value.currentRows.size)

        vm.appendNow()
        advanceUntilIdle()
        assertTrue(vm.state.value.logLines.isNotEmpty())
        vm.clear()
    }
}
